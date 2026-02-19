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
import java.util.Collections;
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
            log.info("Step 1: Got current user: {}", currentUser.getId());

            // Find cart or create one if it doesn't exist
            Cart cart = cartRepository.findByUserId(currentUser.getId())
                    .orElseGet(() -> {
                        log.info("Step 2: Creating new cart for user: {}", currentUser.getId());
                        Cart newCart = new Cart();
                        newCart.setUser(currentUser);
                        newCart.setTotalAmount(BigDecimal.ZERO);
                        newCart.setTotalItems(0);
                        newCart.setCreatedAt(LocalDateTime.now());
                        newCart.setUpdatedAt(LocalDateTime.now());
                        Cart savedCart = cartRepository.save(newCart);
                        log.info("Step 2a: Cart created with ID: {}", savedCart.getId());
                        return savedCart;
                    });

            log.info("Step 3: Cart found/created with ID: {}, TotalItems: {}", cart.getId(), cart.getTotalItems());

            // Fetch cart items - this might return empty list, that's OK
            List<CartItem> cartItems = cartItemRepository.findAllByCartIdWithProduct(cart.getId());
            log.info("Step 4: Found {} items in cart", cartItems.size());

            // Log each item if any
            for (int i = 0; i < cartItems.size(); i++) {
                CartItem item = cartItems.get(i);
                log.info("Step 4.{}: Item ID: {}, Product: {}, Quantity: {}",
                        i+1, item.getId(),
                        item.getProduct() != null ? item.getProduct().getName() : "null",
                        item.getQuantity());
            }

            // Convert to DTO
            CartDTO cartDTO = convertToDTO(cart, cartItems);
            log.info("Step 5: Converted to DTO, items in DTO: {}", cartDTO.getCartItems().size());

            return cartDTO;

        } catch (Exception e) {
            log.error("ERROR in getCurrentUserCart: {}", e.getMessage(), e);

            // Return an empty cart DTO instead of throwing
            CartDTO emptyCart = new CartDTO();
            emptyCart.setCartItems(Collections.emptyList());
            emptyCart.setTotalAmount(BigDecimal.ZERO);
            emptyCart.setTotalItems(0);
            emptyCart.setEmpty(true);

            // Try to get user info for the response
            try {
                User currentUser = authService.getCurrentUser();
                emptyCart.setUserId(currentUser.getId());
                emptyCart.setUserEmail(currentUser.getEmail());
                log.info("Returning empty cart for user: {}", currentUser.getId());
            } catch (Exception ex) {
                log.error("Could not get user info for empty cart: {}", ex.getMessage());
            }

            return emptyCart;
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
            return convertToDTO(cart, cartItems);

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
            return convertToDTO(cart, cartItems);

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
            return convertToDTO(cart, cartItems);

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

            return convertToDTO(cart, new ArrayList<>());

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

    private CartDTO convertToDTO(Cart cart, List<CartItem> cartItems) {
        CartDTO dto = new CartDTO();

        // Null checks
        if (cart == null) {
            return createEmptyCartDTO();
        }

        dto.setId(cart.getId());
        dto.setUserId(cart.getUser() != null ? cart.getUser().getId() : null);
        dto.setUserEmail(cart.getUser() != null ? cart.getUser().getEmail() : null);
        dto.setTotalAmount(cart.getTotalAmount() != null ? cart.getTotalAmount() : BigDecimal.ZERO);
        dto.setTotalItems(cart.getTotalItems() != null ? cart.getTotalItems() : 0);
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());

        // Handle cart items - even if empty
        List<CartItemDTO> itemDTOs = new ArrayList<>();
        if (cartItems != null && !cartItems.isEmpty()) {
            for (CartItem item : cartItems) {
                CartItemDTO itemDTO = new CartItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
                itemDTO.setProductName(item.getProduct() != null ? item.getProduct().getName() : "Unknown Product");
                itemDTO.setQuantity(item.getQuantity() != null ? item.getQuantity() : 0);
                itemDTO.setPrice(item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
                itemDTO.setSubtotal(item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO);
                itemDTO.setProductImageUrl(item.getProduct() != null ? item.getProduct().getImageUrl() : null);
                itemDTO.setCreatedAt(item.getCreatedAt());
                itemDTOs.add(itemDTO);
            }
        }
        dto.setCartItems(itemDTOs);

        // Set empty flag
        dto.setEmpty(itemDTOs.isEmpty());

        return dto;
    }

    private CartDTO createEmptyCartDTO() {
        CartDTO dto = new CartDTO();
        dto.setCartItems(Collections.emptyList());
        dto.setTotalAmount(BigDecimal.ZERO);
        dto.setTotalItems(0);
        dto.setEmpty(true);
        return dto;
    }
}