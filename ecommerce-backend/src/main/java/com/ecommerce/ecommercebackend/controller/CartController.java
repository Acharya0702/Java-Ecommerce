package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.CartDTO;
import com.ecommerce.ecommercebackend.dto.CartItemRequest;
import com.ecommerce.ecommercebackend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartDTO> getCart() {
        log.info("GET /cart called");
        CartDTO cart = cartService.getCurrentUserCart();
        log.info("Cart response: {}", cart);
        log.info("Cart items count: {}", cart.getCartItems() != null ? cart.getCartItems().size() : 0);
        return ResponseEntity.ok(cart);
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getCartItemCount() {
        Integer count = cartService.getCartItemCount();
        log.info("Cart item count: {}", count);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/items")
    public ResponseEntity<CartDTO> addToCart(@Valid @RequestBody CartItemRequest request) {
        log.info("POST /cart/items called with productId: {}, quantity: {}",
                request.getProductId(), request.getQuantity());
        CartDTO cart = cartService.addItemToCart(request);
        log.info("After add - Cart items count: {}",
                cart.getCartItems() != null ? cart.getCartItems().size() : 0);
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> updateCartItem(
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {
        log.info("PUT /cart/items/{} called with quantity: {}", itemId, quantity);
        CartDTO cart = cartService.updateCartItem(itemId, quantity);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> removeCartItem(@PathVariable Long itemId) {
        log.info("DELETE /cart/items/{} called", itemId);
        CartDTO cart = cartService.removeItemFromCart(itemId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    public ResponseEntity<CartDTO> clearCart() {
        log.info("DELETE /cart called");
        CartDTO cart = cartService.clearCart();
        return ResponseEntity.ok(cart);
    }
}