package com.ecommerce.ecommercebackend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Long userId;
    private String userName;
    private String userEmail;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    // Statistics
    private Integer helpfulCount;
    private Integer unhelpfulCount;
    private Boolean currentUserVote; // true = helpful, false = unhelpful, null = no vote

    @Data
    public static class CreateReviewDTO {
        private Long productId;
        private Integer rating;
        private String comment;
    }

    @Data
    public static class UpdateReviewDTO {
        private Integer rating;
        private String comment;
    }

    @Data
    public static class VoteReviewDTO {
        private Long reviewId;
        private boolean helpful; // true = helpful, false = unhelpful
    }
}