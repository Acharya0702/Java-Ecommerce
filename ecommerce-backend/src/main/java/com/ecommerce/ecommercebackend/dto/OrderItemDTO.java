// src/main/java/com/ecommerce/ecommercebackend/dto/OrderItemDTO.java
package com.ecommerce.ecommercebackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long id;
    private String productName;
    private String productImage;
    private String sku;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}