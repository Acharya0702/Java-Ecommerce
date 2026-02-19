package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.OrderDTO;
import com.ecommerce.ecommercebackend.dto.OrderRequestDTO;
import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import com.ecommerce.ecommercebackend.service.AuthService;
import com.ecommerce.ecommercebackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequestDTO request) {
        try {
            log.info("=== ORDER CREATION REQUEST START ===");
            log.info("Request: {}", request);

            OrderDTO order = orderService.createOrder(request);

            log.info("=== ORDER CREATION SUCCESS ===");
            log.info("Order created: {}, Total: ${}", order.getOrderNumber(), order.getTotalAmount());

            return ResponseEntity.status(HttpStatus.CREATED).body(order);

        } catch (RuntimeException e) {
            log.error("=== ORDER CREATION BUSINESS ERROR ===");
            log.error("Error: {}", e.getMessage(), e);

            // Return specific error message
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Cart not found")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Your cart is empty or not found"));
            } else if (errorMessage.contains("Insufficient stock")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", errorMessage));
            } else if (errorMessage.contains("Product not found")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "One or more products in your cart are no longer available"));
            }

            return ResponseEntity.badRequest()
                    .body(Map.of("error", errorMessage));

        } catch (Exception e) {
            log.error("=== ORDER CREATION UNEXPECTED ERROR ===");
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage(), e);

            // Check for specific common exceptions
            if (e instanceof DataIntegrityViolationException) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Database constraint violation. Please check your data."));
            } else if (e instanceof TransactionSystemException) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Transaction failed. Please try again."));
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create order: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getUserOrders() {
        try {
            List<OrderDTO> orders = orderService.getUserOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching user orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            log.info("Getting order by ID: {}", id);
            OrderDTO order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Error fetching order: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "Order not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error fetching order: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            log.info("Getting order by number: {}", orderNumber);
            OrderDTO order = orderService.getOrderByNumber(orderNumber);
            return ResponseEntity.ok(order);

        } catch (RuntimeException e) {
            log.error("Error fetching order by number: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "Order not found or access denied");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error fetching order by number: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try {
            OrderDTO order = orderService.cancelOrder(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Error cancelling order: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error cancelling order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cancel order"));
        }
    }

    @PostMapping("/debug-create")
    public ResponseEntity<?> debugCreateOrder() {
        try {
            log.info("=== DEBUG ORDER CREATION ===");

            User currentUser = authService.getCurrentUser();
            log.info("Current user: {}", currentUser.getEmail());

            // Test 1: Create a minimal order without cart items
            Order order = new Order();
            order.setUser(currentUser);
            order.setOrderNumber("DEBUG-" + System.currentTimeMillis());
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentMethod(Order.PaymentMethod.CASH_ON_DELIVERY);
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            order.setSubtotal(new BigDecimal("100.00"));
            order.setShippingAmount(new BigDecimal("5.99"));
            order.setTaxAmount(new BigDecimal("10.00"));
            order.setTotalAmount(new BigDecimal("115.99"));

            // Add simple address
            Order.Address shippingAddress = new Order.Address();
            shippingAddress.setStreet("123 Test St");
            shippingAddress.setCity("Test City");
            shippingAddress.setState("TS");
            shippingAddress.setZipCode("12345");
            shippingAddress.setCountry("Test Country");
            shippingAddress.setPhone("123-456-7890");
            shippingAddress.setRecipientName("Test User");
            order.setShippingAddress(shippingAddress);

            order.setBillingAddress(shippingAddress); // Same address

            log.info("Attempting to save order...");
            Order savedOrder = orderRepository.save(order);
            log.info("Order saved successfully: ID={}, Number={}",
                    savedOrder.getId(), savedOrder.getOrderNumber());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", savedOrder.getId(),
                    "orderNumber", savedOrder.getOrderNumber(),
                    "message", "Debug order created successfully"
            ));

        } catch (Exception e) {
            log.error("=== DEBUG ORDER CREATION FAILED ===");
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            log.error("Stack trace:", e);

            // Check for specific exceptions
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Debug failed",
                            "exception", e.getClass().getName(),
                            "message", e.getMessage(),
                            "rootCause", rootCause.getMessage()
                    ));
        }
    }
}