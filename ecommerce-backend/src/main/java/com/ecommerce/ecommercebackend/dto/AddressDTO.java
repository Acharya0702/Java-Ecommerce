package com.ecommerce.ecommercebackend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class AddressDTO {

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zip code is required")
    private String zipCode;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Recipient name is required")
    private String recipientName;
}