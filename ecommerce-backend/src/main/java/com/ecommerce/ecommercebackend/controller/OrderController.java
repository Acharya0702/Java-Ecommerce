package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.OrderDTO;
import com.ecommerce.ecommercebackend.dto.OrderRequestDTO;
import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class OrderController {

    private final OrderService orderService;

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

            // Return specific error message based on exception
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
            } else if (errorMessage.contains("Cart is empty")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Your cart is empty. Add items before placing an order."));
            }

            return ResponseEntity.badRequest()
                    .body(Map.of("error", errorMessage));

        } catch (Exception e) {
            log.error("=== ORDER CREATION UNEXPECTED ERROR ===");
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create order: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserOrders() {
        try {
            log.info("Fetching all orders for current user");
            List<OrderDTO> orders = orderService.getUserOrders();
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            log.error("Error fetching user orders: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error fetching user orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch orders"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            log.info("Fetching order by ID: {}", id);
            OrderDTO order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Error fetching order {}: {}", id, e.getMessage());

            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found"));
            } else if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to view this order"));
            }

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error fetching order {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order"));
        }
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            log.info("Fetching order by number: {}", orderNumber);
            OrderDTO order = orderService.getOrderByNumber(orderNumber);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Error fetching order by number {}: {}", orderNumber, e.getMessage());

            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found"));
            } else if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to view this order"));
            }

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error fetching order by number {}: {}", orderNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order"));
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try {
            log.info("Cancelling order: {}", id);
            OrderDTO order = orderService.cancelOrder(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Error cancelling order {}: {}", id, e.getMessage());

            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found"));
            } else if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to cancel this order"));
            } else if (e.getMessage().contains("cannot be cancelled")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Order cannot be cancelled at its current status"));
            }

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error cancelling order {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cancel order"));
        }
    }

    // Admin endpoints for order management
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        try {
            log.info("Updating order status: {} to {}", id, request.getStatus());

            // Validate status transition
            if (request.getStatus() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Status is required"));
            }

            // If status is SHIPPED, tracking number is required
            if (request.getStatus() == Order.OrderStatus.SHIPPED &&
                    (request.getTrackingNumber() == null || request.getTrackingNumber().trim().isEmpty())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Tracking number is required when marking order as SHIPPED"));
            }

            OrderDTO order = orderService.updateOrderStatus(id, request.getStatus(), request.getTrackingNumber());

            log.info("Order status updated successfully: {} -> {}", order.getOrderNumber(), order.getStatus());
            return ResponseEntity.ok(order);

        } catch (RuntimeException e) {
            log.error("Error updating order status for {}: {}", id, e.getMessage());

            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found"));
            } else if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to update this order"));
            }

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating order status for {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update order status"));
        }
    }

    // Bulk status update for admin
    @PutMapping("/bulk/status")
    public ResponseEntity<?> bulkUpdateOrderStatus(@Valid @RequestBody BulkStatusUpdateRequest request) {
        try {
            log.info("Bulk updating {} orders to status: {}", request.getOrderIds().size(), request.getStatus());

            if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Order IDs list cannot be empty"));
            }

            List<OrderDTO> updatedOrders = request.getOrderIds().stream()
                    .map(id -> {
                        try {
                            return orderService.updateOrderStatus(id, request.getStatus(), request.getTrackingNumber());
                        } catch (Exception e) {
                            log.error("Failed to update order {}: {}", id, e.getMessage());
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "updatedCount", updatedOrders.size(),
                    "orders", updatedOrders
            ));

        } catch (Exception e) {
            log.error("Error in bulk status update: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to perform bulk status update"));
        }
    }

    // Debug endpoint for testing
    @PostMapping("/debug-create")
    public ResponseEntity<?> debugCreateOrder() {
        try {
            log.info("=== DEBUG ORDER CREATION ===");

            // Use the service to get current user and create a debug order
            // This is just a test endpoint - you might want to remove in production
            OrderDTO debugOrder = orderService.createDebugOrder();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", debugOrder.getId(),
                    "orderNumber", debugOrder.getOrderNumber(),
                    "message", "Debug order created successfully"
            ));

        } catch (Exception e) {
            log.error("=== DEBUG ORDER CREATION FAILED ===");
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            log.error("Stack trace:", e);

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

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Order Service",
                "timestamp", System.currentTimeMillis()
        ));
    }

    // Request DTO classes
    @Data
    public static class UpdateOrderStatusRequest {
        private Order.OrderStatus status;
        private String trackingNumber;
    }

    @Data
    public static class BulkStatusUpdateRequest {
        private List<Long> orderIds;
        private Order.OrderStatus status;
        private String trackingNumber;
    }
}