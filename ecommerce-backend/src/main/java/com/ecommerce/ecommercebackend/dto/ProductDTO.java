package com.ecommerce.ecommercebackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String sku;
    private Integer stockQuantity;
    private Long categoryId;
    private String categoryName;
    private String imageUrl;
    private Map<String, String> additionalImages;
    private Map<String, String> specifications;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Calculated fields
    private BigDecimal discountedPrice;
    private Boolean hasDiscount;
    private Boolean inStock;

    // Review rating fields - ADD THESE
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Map<Integer, Long> ratingDistribution;

    // Getter methods for calculated fields
    public BigDecimal getDiscountedPrice() {
        if (discountedPrice != null) {
            return discountedPrice;
        }
        return discountPrice != null ? discountPrice : price;
    }

    public boolean getHasDiscount() {
        if (hasDiscount != null) {
            return hasDiscount;
        }
        return discountPrice != null && discountPrice.compareTo(price) < 0;
    }

    public boolean getInStock() {
        if (inStock != null) {
            return inStock;
        }
        return stockQuantity != null && stockQuantity > 0;
    }

    // Helper methods for rating display
    public String getAverageRatingFormatted() {
        if (averageRating != null) {
            return String.format("%.1f", averageRating);
        }
        return "0.0";
    }

    public int getStarPercentage(int star) {
        if (ratingDistribution != null && ratingDistribution.containsKey(star) && totalReviews != null && totalReviews > 0) {
            long count = ratingDistribution.getOrDefault(star, 0L);
            return (int) ((count * 100) / totalReviews);
        }
        return 0;
    }
}