package com.ecommerce.ecommercebackend.dto.admin;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductBulkUpdateDTO {
    private List<Long> productIds;
    private BigDecimal discountPrice;
    private Integer stockAdjustment;
    private Boolean isActive;
}