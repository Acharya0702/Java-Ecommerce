package com.ecommerce.ecommercebackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "products")
@Data
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "rating_count_1")
    private Integer ratingCount1 = 0;

    @Column(name = "rating_count_2")
    private Integer ratingCount2 = 0;

    @Column(name = "rating_count_3")
    private Integer ratingCount3 = 0;

    @Column(name = "rating_count_4")
    private Integer ratingCount4 = 0;

    @Column(name = "rating_count_5")
    private Integer ratingCount5 = 0;

    private Integer stockQuantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnore
    @ToString.Exclude
    private Category category;

    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, String> additionalImages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, String> specifications;

    private Boolean isActive = true;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ADD THESE RELATIONSHIPS WITH @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private Set<CartItem> cartItems = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private Set<OrderItem> orderItems = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private Set<Review> reviews = new HashSet<>();

    // Helper methods
    public BigDecimal getDiscountedPrice() {
        return discountPrice != null ? discountPrice : price;
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public boolean hasDiscount() {
        return discountPrice != null && discountPrice.compareTo(price) < 0;
    }

    public BigDecimal getDiscountPercentage() {
        if (hasDiscount()) {
            return price.subtract(discountPrice)
                    .divide(price, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    public void decreaseStock(Integer quantity) {
        if (quantity > 0 && this.stockQuantity >= quantity) {
            this.stockQuantity -= quantity;
        } else {
            throw new IllegalArgumentException("Insufficient stock");
        }
    }

    public void increaseStock(Integer quantity) {
        if (quantity > 0) {
            this.stockQuantity += quantity;
        }
    }

    @PrePersist
    @PreUpdate
    private void generateSku() {
        if (this.sku == null || this.sku.isEmpty()) {
            // Generate SKU from name and timestamp
            String namePart = this.name.substring(0, Math.min(3, this.name.length())).toUpperCase();
            String timestamp = String.valueOf(System.currentTimeMillis() % 10000);
            this.sku = namePart + "-" + timestamp;
        }
    }

    public void updateRatingStats(Double newAverage, Map<Integer, Long> distribution) {
        if (newAverage != null) {
            this.averageRating = BigDecimal.valueOf(newAverage);
        }
        if (distribution != null) {
            this.ratingCount1 = distribution.getOrDefault(1, 0L).intValue();
            this.ratingCount2 = distribution.getOrDefault(2, 0L).intValue();
            this.ratingCount3 = distribution.getOrDefault(3, 0L).intValue();
            this.ratingCount4 = distribution.getOrDefault(4, 0L).intValue();
            this.ratingCount5 = distribution.getOrDefault(5, 0L).intValue();
            this.totalReviews = ratingCount1 + ratingCount2 + ratingCount3 + ratingCount4 + ratingCount5;
        }
    }
}