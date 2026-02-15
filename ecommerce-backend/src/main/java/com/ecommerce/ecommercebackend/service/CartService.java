package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.CartDTO;
import com.ecommerce.ecommercebackend.dto.CartItemDTO;
import com.ecommerce.ecommercebackend.dto.CartItemRequest;
import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.exception.InsufficientStockException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.CartRepository;
import com.ecommerce.ecommercebackend.repository.CartItemRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public CartDTO getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCartForUser(userId));

        // Ensure cart items are initialized to avoid LazyInitializationException
        if (cart.getCartItems() != null) {
            cart.getCartItems().size(); // Force initialization
        }

        return convertToDTO(cart);
    }

    public CartDTO getCurrentUserCart() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }
        return getCartByUserId(currentUser.getId());
    }

    @Transactional
    public CartDTO addItemToCart(CartItemRequest request) {
        log.info("=== Starting addItemToCart ===");
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }else{
            log.info("Current user: {}", currentUser.getEmail());
        }

        Cart cart = getOrCreateCart(currentUser.getId());
        log.info("Cart ID: {}", cart.getId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        log.info("Product found: {} (ID: {})", product.getName(), product.getId());

        if (!product.getIsActive()) {
            throw new RuntimeException("Product '" + product.getName() + "' is not available");
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for '%s'. Available: %d, Requested: %d",
                            product.getName(), product.getStockQuantity(), request.getQuantity())
            );
        }

        // Use the entity's helper methods for better encapsulation
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(
                cart.getId(), request.getProductId());

        if (existingItem.isPresent()) {
            // Update quantity using entity method
            CartItem cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();

            if (product.getStockQuantity() < newQuantity) {
                throw new InsufficientStockException(
                        String.format("Cannot add %d more of '%s'. Available: %d",
                                request.getQuantity(), product.getName(), product.getStockQuantity())
                );
            }

            cartItem.updateQuantity(newQuantity);
            cartItem.setPrice(product.getDiscountedPrice());
            cartItemRepository.save(cartItem);
            log.info("Updated cart item quantity for product {} in cart {}: new quantity {}",
                    product.getId(), cart.getId(), newQuantity);
        } else {
            // Add new item using cart's helper method
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(product.getDiscountedPrice());

            // Use the cart's helper method to add item and recalculate totals
            cart.addCartItem(cartItem);

            cartItemRepository.save(cartItem);
            log.info("Added new item for product {} to cart {}: quantity {}",
                    product.getId(), cart.getId(), request.getQuantity());
        }

        // The cart's totals are updated via recalculateTotals() in addCartItem
        // But we need to ensure the cart is saved
        Cart updatedCart = cartRepository.save(cart);
        log.info("Cart saved successfully");

        CartDTO dto = convertToDTO(updatedCart);
        log.info("DTO converted successfully");
        return dto;
    }

    @Transactional
    public CartDTO updateCartItem(Long itemId, Integer quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        Product product = cartItem.getProduct();
        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for '%s'. Available: %d, Requested: %d",
                            product.getName(), product.getStockQuantity(), quantity)
            );
        }

        // Use entity method to update quantity
        cartItem.updateQuantity(quantity);
        cartItem.setPrice(product.getDiscountedPrice());
        cartItemRepository.save(cartItem);

        // Recalculate cart totals
        Cart cart = cartItem.getCart();
        cart.recalculateTotals(); // Use the entity's built-in method
        cartRepository.save(cart);

        log.info("Updated cart item {} quantity to {}", itemId, quantity);
        return convertToDTO(cart);
    }

    @Transactional
    public CartDTO removeItemFromCart(Long itemId) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        Cart cart = cartItem.getCart();

        // Use cart's helper method to remove item
        cart.removeCartItem(cartItem);

        // Delete the cart item
        cartItemRepository.delete(cartItem);

        // Cart totals are updated via recalculateTotals() in removeCartItem
        cartRepository.save(cart);

        log.info("Removed cart item {} from cart {}", itemId, cart.getId());
        return convertToDTO(cart);
    }

    @Transactional
    public CartDTO clearCart() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }

        Cart cart = cartRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + currentUser.getId()));

        // Use cart's helper method to clear items
        cart.clearCart();

        // Delete all cart items
        cartItemRepository.deleteByCartId(cart.getId());

        // Cart totals are updated via clearCart() method
        cartRepository.save(cart);

        log.info("Cleared cart for user {}", currentUser.getId());
        return convertToDTO(cart);
    }

    public Integer getCartItemCount() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return 0; // Return 0 for unauthenticated users
        }

        Cart cart = cartRepository.findByUserIdWithItems(currentUser.getId())
                .orElseGet(() -> createCartForUser(currentUser.getId()));

        return cart.getTotalItems() != null ? cart.getTotalItems() : 0;
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCartForUser(userId));
    }

    private Cart createCartForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setTotalItems(0);

        Cart savedCart = cartRepository.save(cart);
        log.info("Created new cart for user {}", userId);

        return savedCart;
    }

    /**
     * Recalculate cart totals - kept for backward compatibility
     * but now delegates to the entity's method
     */
    private void recalculateCartTotals(Cart cart) {
        cart.recalculateTotals();
    }

    private CartDTO convertToDTO(Cart cart) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());

        if (cart.getUser() != null) {
            dto.setUserId(cart.getUser().getId());
            dto.setUserEmail(cart.getUser().getEmail());
        }

        dto.setTotalAmount(cart.getTotalAmount() != null ? cart.getTotalAmount() : BigDecimal.ZERO);
        dto.setTotalItems(cart.getTotalItems() != null ? cart.getTotalItems() : 0);
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());

        if (cart.getCartItems() != null) {
            List<CartItemDTO> cartItemDTOs = cart.getCartItems().stream()
                    .map(this::convertCartItemToDTO)
                    .collect(Collectors.toList());
            dto.setCartItems(cartItemDTOs);
        }

        return dto;
    }

    private CartItemDTO convertCartItemToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());

        if (cartItem.getProduct() != null) {
            dto.setProductId(cartItem.getProduct().getId());
            dto.setProductName(cartItem.getProduct().getName());
            dto.setProductImageUrl(cartItem.getProduct().getImageUrl());
        }

        dto.setQuantity(cartItem.getQuantity());
        dto.setPrice(cartItem.getPrice() != null ? cartItem.getPrice() : BigDecimal.ZERO);
        dto.setSubtotal(cartItem.getSubtotal() != null ? cartItem.getSubtotal() :
                cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        dto.setCreatedAt(cartItem.getCreatedAt());

        return dto;
    }
}