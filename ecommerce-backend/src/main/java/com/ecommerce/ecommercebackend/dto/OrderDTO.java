package com.ecommerce.ecommercebackend.dto;

import com.ecommerce.ecommercebackend.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userName;
    private String userEmail;
    private List<OrderItemDTO> orderItems = new ArrayList<>();
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;

    // Use top-level AddressDTO instead of inner class
    private AddressDTO shippingAddress;
    private AddressDTO billingAddress;

    private Order.OrderStatus status;
    private Order.PaymentMethod paymentMethod;
    private Order.PaymentStatus paymentStatus;
    private String trackingNumber;
    private String shippingMethod;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    @Data
    public static class OrderItemDTO {
        private Long id;
        private String productName;
        private String productImage;
        private String sku;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}