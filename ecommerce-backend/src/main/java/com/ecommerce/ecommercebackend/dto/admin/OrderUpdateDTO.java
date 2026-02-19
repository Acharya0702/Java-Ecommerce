package com.ecommerce.ecommercebackend.dto.admin;

import com.ecommerce.ecommercebackend.entity.Order;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class OrderUpdateDTO {
    @NotNull(message = "Order status is required")
    private Order.OrderStatus status;

    private String trackingNumber;
    private String shippingMethod;
    private String notes;
}