package com.ecommerce.ecommercebackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long id;
    private String productName;
    private String sku;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private String productImage;

    // Getters
    public String getProductImage() {
        return productImage != null ? productImage : "https://picsum.photos/300/200?random=1";
    }
}