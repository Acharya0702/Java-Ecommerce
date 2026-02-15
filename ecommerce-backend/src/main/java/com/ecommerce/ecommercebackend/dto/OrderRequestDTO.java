package com.ecommerce.ecommercebackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.ecommerce.ecommercebackend.entity.Order;

@Data
public class OrderRequestDTO {

    @NotNull(message = "Shipping address is required")
    @Valid
    private AddressDTO shippingAddress;

    @Valid
    private AddressDTO billingAddress; // Remove @NotNull annotation

    private Order.PaymentMethod paymentMethod = Order.PaymentMethod.CASH_ON_DELIVERY;

    private String notes;

    // Add this field to determine if billing address should be same as shipping
    private Boolean useShippingForBilling = true;
}