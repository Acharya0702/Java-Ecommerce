// src/main/java/com/ecommerce/ecommercebackend/dto/OrderDTO.java
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

    private AddressDTO shippingAddress;
    private AddressDTO billingAddress;

    private String status;  // Changed from Order.OrderStatus to String
    private String paymentMethod;  // Changed from Order.PaymentMethod to String
    private String paymentStatus;  // Changed from Order.PaymentStatus to String
    private String trackingNumber;
    private String shippingMethod;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
}