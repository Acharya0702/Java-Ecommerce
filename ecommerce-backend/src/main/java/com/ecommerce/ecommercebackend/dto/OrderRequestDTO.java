package com.ecommerce.ecommercebackend.dto;

import com.ecommerce.ecommercebackend.entity.Order;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Data
public class OrderRequestDTO {

    @Valid
    @NotNull(message = "Shipping address is required")
    private AddressDTO shippingAddress;

    @Valid
    @NotNull(message = "Billing address is required")
    private AddressDTO billingAddress;

    @NotNull(message = "Payment method is required")
    private Order.PaymentMethod paymentMethod;

    private String notes;
}