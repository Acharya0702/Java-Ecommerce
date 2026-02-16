package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.CartDTO;
import com.ecommerce.ecommercebackend.dto.CartItemRequest;
import com.ecommerce.ecommercebackend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<?> getCart() {
        try {
            log.info("GET /cart - fetching cart");
            CartDTO cart = cartService.getCurrentUserCart();
            log.info("GET /cart - success, cart has {} items", cart.getCartItems().size());
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            log.error("GET /cart - error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch cart",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> getCartItemCount() {
        try {
            Integer count = cartService.getCartItemCount();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting cart count: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to get cart count",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    @PostMapping("/items")
    public ResponseEntity<?> addToCart(@Valid @RequestBody CartItemRequest request) {
        try {
            log.info("POST /cart/items - adding product {} with quantity {}",
                    request.getProductId(), request.getQuantity());

            CartDTO cart = cartService.addItemToCart(request);

            log.info("POST /cart/items - success, cart now has {} items",
                    cart.getCartItems().size());

            return ResponseEntity.ok(cart);

        } catch (RuntimeException e) {
            log.error("POST /cart/items - business error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (Exception e) {
            log.error("POST /cart/items - unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateCartItem(
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {
        try {
            CartDTO cart = cartService.updateCartItem(itemId, quantity);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage(), "timestamp", LocalDateTime.now()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update cart item", "timestamp", LocalDateTime.now()));
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long itemId) {
        try {
            CartDTO cart = cartService.removeItemFromCart(itemId);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage(), "timestamp", LocalDateTime.now()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to remove cart item", "timestamp", LocalDateTime.now()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearCart() {
        try {
            CartDTO cart = cartService.clearCart();
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clear cart", "timestamp", LocalDateTime.now()));
        }
    }
}