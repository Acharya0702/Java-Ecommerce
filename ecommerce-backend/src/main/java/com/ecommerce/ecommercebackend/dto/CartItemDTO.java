package com.ecommerce.ecommercebackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity=0;
    private BigDecimal price=BigDecimal.ZERO;
    private BigDecimal subtotal=BigDecimal.ZERO;
    private LocalDateTime createdAt;

    // Helper method
    public BigDecimal calculateSubtotal() {
        return price != null && quantity != null ?
                price.multiply(BigDecimal.valueOf(quantity)) :
                BigDecimal.ZERO;
    }
}
