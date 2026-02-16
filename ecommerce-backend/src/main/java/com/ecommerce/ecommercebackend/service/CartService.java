package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.CartDTO;
import com.ecommerce.ecommercebackend.dto.CartItemDTO;
import com.ecommerce.ecommercebackend.dto.CartItemRequest;
import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.repository.CartRepository;
import com.ecommerce.ecommercebackend.repository.CartItemRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    // SIMPLIFIED: Get current user's cart
    @Transactional(readOnly = true)
    public CartDTO getCurrentUserCart() {
        try {
            User currentUser = authService.getCurrentUser();
            log.info("Getting cart for user: {}", currentUser.getId());

            // Get cart or create if not exists
            Cart cart = cartRepository.findByUserId(currentUser.getId())
                    .orElseGet(() -> {
                        log.info("Creating new cart for user: {}", currentUser.getId());
                        return createCartForUser(currentUser.getId());
                    });

            // Get cart items separately to avoid lazy loading issues
            List<CartItem> cartItems = cartItemRepository.findAllByCartIdWithProduct(cart.getId());

            // Build DTO
            return buildCartDTO(cart, cartItems);

        } catch (Exception e) {
            log.error("Error getting cart: {}", e.getMessage(), e);
            // Return empty cart on error
            return buildEmptyCartDTO();
        }
    }

    // SIMPLIFIED: Add item to cart
    @Transactional
    public CartDTO addItemToCart(CartItemRequest request) {
        try {
            User currentUser = authService.getCurrentUser();
            log.info("Adding item to cart - user: {}, product: {}, quantity: {}",
                    currentUser.getId(), request.getProductId(), request.getQuantity());

            // Get or create cart
            Cart cart = cartRepository.findByUserId(currentUser.getId())
                    .orElseGet(() -> createCartForUser(currentUser.getId()));

            // Get product
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + request.getProductId()));

            // Validate
            validateProductForCart(product, request.getQuantity());

            // Check if item already exists in cart
            Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(
                    cart.getId(), request.getProductId());

            if (existingItem.isPresent()) {
                // Update existing item
                CartItem cartItem = existingItem.get();
                updateCartItemQuantity(cartItem, request.getQuantity(), product);
            } else {
                // Create new item
                createNewCartItem(cart, product, request.getQuantity());
            }

            // Recalculate and save cart
            recalculateAndSaveCart(cart);

            // Return updated cart
            List<CartItem> cartItems = cartItemRepository.findAllByCartIdWithProduct(cart.getId());
            return buildCartDTO(cart, cartItems);

        } catch (RuntimeException e) {
            log.error("Error in addItemToCart: {}", e.getMessage());
            throw e; // Re-throw business exceptions
        } catch (Exception e) {
            log.error("Unexpected error in addItemToCart: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add item to cart", e);
        }
    }

    // SIMPLIFIED: Update cart item quantity
    @Transactional
    public CartDTO updateCartItem(Long itemId, Integer quantity) {
        try {
            if (quantity < 1) {
                throw new RuntimeException("Quantity must be at least 1");
            }

            CartItem cartItem = cartItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Cart item not found with ID: " + itemId));

            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Validate stock
            if (product.getStockQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
            }

            // Update item
            cartItem.setQuantity(quantity);
            cartItem.setPrice(product.getDiscountedPrice());
            cartItemRepository.save(cartItem);

            // Recalculate and save cart
            Cart cart = cartItem.getCart();
            recalculateAndSaveCart(cart);

            // Return updated cart
            List<CartItem> cartItems = cartItemRepository.findAllByCartIdWithProduct(cart.getId());
            return buildCartDTO(cart, cartItems);

        } catch (Exception e) {
            log.error("Error updating cart item: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update cart item", e);
        }
    }

    // SIMPLIFIED: Remove item from cart
    @Transactional
    public CartDTO removeItemFromCart(Long itemId) {
        try {
            CartItem cartItem = cartItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Cart item not found with ID: " + itemId));

            Cart cart = cartItem.getCart();
            cartItemRepository.delete(cartItem);

            // Recalculate and save cart
            recalculateAndSaveCart(cart);

            // Return updated cart
            List<CartItem> cartItems = cartItemRepository.findAllByCartIdWithProduct(cart.getId());
            return buildCartDTO(cart, cartItems);

        } catch (Exception e) {
            log.error("Error removing cart item: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to remove cart item", e);
        }
    }

    // SIMPLIFIED: Clear cart
    @Transactional
    public CartDTO clearCart() {
        try {
            User currentUser = authService.getCurrentUser();
            Cart cart = cartRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Cart not found"));

            cartItemRepository.deleteByCartId(cart.getId());

            cart.setTotalItems(0);
            cart.setTotalAmount(BigDecimal.ZERO);
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);

            return buildCartDTO(cart, new ArrayList<>());

        } catch (Exception e) {
            log.error("Error clearing cart: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clear cart", e);
        }
    }

    // SIMPLIFIED: Get cart item count
    @Transactional(readOnly = true)
    public Integer getCartItemCount() {
        try {
            User currentUser = authService.getCurrentUser();
            Cart cart = cartRepository.findByUserId(currentUser.getId())
                    .orElseGet(() -> createCartForUser(currentUser.getId()));

            return cart.getTotalItems();

        } catch (Exception e) {
            log.error("Error getting cart item count: {}", e.getMessage(), e);
            return 0;
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Cart createCartForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setTotalItems(0);
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());

        return cartRepository.save(cart);
    }

    private void validateProductForCart(Product product, Integer requestedQuantity) {
        if (!product.getIsActive()) {
            throw new RuntimeException("Product is not available");
        }

        if (product.getStockQuantity() < requestedQuantity) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
        }
    }

    private void updateCartItemQuantity(CartItem cartItem, Integer additionalQuantity, Product product) {
        int newQuantity = cartItem.getQuantity() + additionalQuantity;

        if (product.getStockQuantity() < newQuantity) {
            throw new RuntimeException("Insufficient stock for requested quantity. Available: " + product.getStockQuantity());
        }

        cartItem.setQuantity(newQuantity);
        cartItem.setPrice(product.getDiscountedPrice());
        cartItemRepository.save(cartItem);
    }

    private void createNewCartItem(Cart cart, Product product, Integer quantity) {
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItem.setPrice(product.getDiscountedPrice());
        cartItem.setCreatedAt(LocalDateTime.now());
        cartItem.setUpdatedAt(LocalDateTime.now());

        cartItemRepository.save(cartItem);
    }

    private void recalculateAndSaveCart(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());

        int totalItems = cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        BigDecimal totalAmount = cartItems.stream()
                .map(item -> {
                    BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalItems(totalItems);
        cart.setTotalAmount(totalAmount);
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);
    }

    private CartDTO buildCartDTO(Cart cart, List<CartItem> cartItems) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());

        if (cart.getUser() != null) {
            dto.setUserId(cart.getUser().getId());
            dto.setUserEmail(cart.getUser().getEmail());
        }

        dto.setTotalAmount(cart.getTotalAmount());
        dto.setTotalItems(cart.getTotalItems());
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());

        // Convert cart items to DTOs
        List<CartItemDTO> cartItemDTOs = new ArrayList<>();
        for (CartItem item : cartItems) {
            CartItemDTO itemDTO = new CartItemDTO();
            itemDTO.setId(item.getId());

            if (item.getProduct() != null) {
                itemDTO.setProductId(item.getProduct().getId());
                itemDTO.setProductName(item.getProduct().getName());
                itemDTO.setProductImageUrl(item.getProduct().getImageUrl());
            }

            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setPrice(item.getPrice());
            itemDTO.setSubtotal(item.getSubtotal());
            itemDTO.setCreatedAt(item.getCreatedAt());

            cartItemDTOs.add(itemDTO);
        }

        dto.setCartItems(cartItemDTOs);
        return dto;
    }

    private CartDTO buildEmptyCartDTO() {
        CartDTO dto = new CartDTO();
        dto.setCartItems(new ArrayList<>());
        dto.setTotalAmount(BigDecimal.ZERO);
        dto.setTotalItems(0);
        return dto;
    }
}