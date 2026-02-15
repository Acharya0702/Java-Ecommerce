package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order; // Link to order to verify purchase

    @Column(name = "rating", nullable = false)
    private Integer rating; // 1 to 5

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_verified_purchase")
    private Boolean isVerifiedPurchase = false;

    @Column(name = "is_approved")
    private Boolean isApproved = true; // For admin moderation

    @Column(name = "helpful_votes")
    private Integer helpfulVotes = 0;

    @Column(name = "unhelpful_votes")
    private Integer unhelpfulVotes = 0;

    @Column(name = "reported_count")
    private Integer reportedCount = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Additional fields for review details
    @Column(name = "size_rating")
    private Integer sizeRating; // 1 to 5

    @Column(name = "comfort_rating")
    private Integer comfortRating; // 1 to 5

    @Column(name = "quality_rating")
    private Integer qualityRating; // 1 to 5

    @Column(name = "value_rating")
    private Integer valueRating; // 1 to 5

    // Review images/videos
    @ElementCollection
    @CollectionTable(
            name = "review_images",
            joinColumns = @JoinColumn(name = "review_id")
    )
    @Column(name = "image_url")
    private Set<String> images = new HashSet<>();

    // Response from seller/admin
    @Column(name = "seller_response", columnDefinition = "TEXT")
    private String sellerResponse;

    @Column(name = "response_date")
    private LocalDateTime responseDate;

    // Pre-persist validation
    @PrePersist
    @PreUpdate
    private void validateRating() {
        if (rating != null) {
            if (rating < 1) rating = 1;
            if (rating > 5) rating = 5;
        }
    }

    // Helper methods
    public void markAsVerifiedPurchase() {
        this.isVerifiedPurchase = true;
    }

    public void approve() {
        this.isApproved = true;
    }

    public void disapprove() {
        this.isApproved = false;
    }

    public void incrementHelpfulVotes() {
        this.helpfulVotes++;
    }

    public void incrementUnhelpfulVotes() {
        this.unhelpfulVotes++;
    }

    public void reportReview() {
        this.reportedCount++;
    }

    public void addResponse(String response, boolean fromAdmin) {
        this.sellerResponse = response;
        this.responseDate = LocalDateTime.now();
    }

    public Double getHelpfulnessScore() {
        int totalVotes = helpfulVotes + unhelpfulVotes;
        return totalVotes > 0 ? (double) helpfulVotes / totalVotes : 0.0;
    }

    public boolean isFromVerifiedPurchaser() {
        return isVerifiedPurchase != null && isVerifiedPurchase;
    }

    // Calculate average of all ratings if sub-ratings exist
    public Double getOverallRating() {
        int count = 0;
        double sum = 0.0;

        if (rating != null) {
            sum += rating;
            count++;
        }
        if (sizeRating != null) {
            sum += sizeRating;
            count++;
        }
        if (comfortRating != null) {
            sum += comfortRating;
            count++;
        }
        if (qualityRating != null) {
            sum += qualityRating;
            count++;
        }
        if (valueRating != null) {
            sum += valueRating;
            count++;
        }

        return count > 0 ? sum / count : null;
    }
}