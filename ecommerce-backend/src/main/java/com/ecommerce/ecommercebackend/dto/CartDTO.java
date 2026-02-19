package com.ecommerce.ecommercebackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    private List<CartItemDTO> cartItems = new ArrayList<>();
    private BigDecimal totalAmount=BigDecimal.ZERO;
    private Integer totalItems=0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean empty = true;

    public boolean isEmpty() {
        return cartItems == null || cartItems.isEmpty();
    }
}
