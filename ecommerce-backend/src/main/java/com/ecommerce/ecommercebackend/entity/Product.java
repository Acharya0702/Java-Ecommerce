package com.ecommerce.ecommercebackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    private Integer stockQuantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnore
    @ToString.Exclude
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // ADD THIS - Critical!
    @ToString.Exclude  // ADD THIS
    private Set<CartItem> cartItems = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // ADD THIS - Critical!
    @ToString.Exclude  // ADD THIS
    private Set<OrderItem> orderItems = new HashSet<>();

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
            String namePart = this.name.substring(0, Math.min(3, this.name.length())).toUpperCase();
            String timestamp = String.valueOf(System.currentTimeMillis() % 10000);
            this.sku = namePart + "-" + timestamp;
        }
    }
}