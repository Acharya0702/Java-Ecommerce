package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.ReviewDTO;
import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ReviewRepository;
import com.ecommerce.ecommercebackend.repository.ReviewVoteRepository;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // Create a new review
    public ReviewDTO createReview(Long userId, ReviewDTO.CreateReviewDTO reviewRequest) {
        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate product exists
        Product product = productRepository.findById(reviewRequest.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if user already reviewed this product
        Optional<Review> existingReview = reviewRepository.findByUserIdAndProductId(userId, reviewRequest.getProductId());
        if (existingReview.isPresent()) {
            throw new IllegalArgumentException("You have already reviewed this product");
        }

        // Validate rating
        if (reviewRequest.getRating() == null || reviewRequest.getRating() < 1 || reviewRequest.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Create review
        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(reviewRequest.getRating());
        review.setComment(reviewRequest.getComment());
        review.setCreatedAt(LocalDateTime.now());

        // Save review
        Review savedReview = reviewRepository.save(review);

        // Update product rating statistics
        updateProductRatingStats(product.getId());

        return convertToDTO(savedReview, userId);
    }

    // Get all reviews for a product
    public List<ReviewDTO> getProductReviews(Long productId, String sortBy, Long currentUserId) {
        List<Review> reviews;

        if ("oldest".equals(sortBy)) {
            reviews = reviewRepository.findByProductIdOrderByCreatedAtAsc(productId);
        } else { // default: newest
            reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        }

        return reviews.stream()
                .map(review -> convertToDTO(review, currentUserId))
                .collect(Collectors.toList());
    }

    // Get review statistics for a product
    public Map<String, Object> getProductReviewStats(Long productId) {
        Double averageRating = reviewRepository.findAverageRatingByProductId(productId);
        Long totalReviews = reviewRepository.countByProductId(productId);
        List<Object[]> ratingDistribution = reviewRepository.getRatingDistribution(productId);

        // Convert rating distribution to map
        Map<Integer, Long> distributionMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distributionMap.put(i, 0L);
        }

        if (ratingDistribution != null) {
            for (Object[] row : ratingDistribution) {
                Integer rating = (Integer) row[0];
                Long count = (Long) row[1];
                distributionMap.put(rating, count);
            }
        }

        return Map.of(
                "averageRating", averageRating != null ? String.format("%.1f", averageRating) : "0.0",
                "totalReviews", totalReviews != null ? totalReviews : 0,
                "ratingDistribution", distributionMap
        );
    }

    // Update a review
    public ReviewDTO updateReview(Long reviewId, Long userId, ReviewDTO.UpdateReviewDTO updateRequest) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check if user owns the review
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only update your own reviews");
        }

        // Update fields
        if (updateRequest.getRating() != null) {
            if (updateRequest.getRating() < 1 || updateRequest.getRating() > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }
            review.setRating(updateRequest.getRating());
        }

        if (updateRequest.getComment() != null) {
            review.setComment(updateRequest.getComment());
        }

        Review updatedReview = reviewRepository.save(review);

        // Update product rating stats
        updateProductRatingStats(review.getProduct().getId());

        return convertToDTO(updatedReview, userId);
    }

    // Delete a review
    public void deleteReview(Long reviewId, Long userId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check if user owns the review or is admin
        if (!review.getUser().getId().equals(userId) && !isAdmin) {
            throw new IllegalArgumentException("You can only delete your own reviews");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);

        // Update product rating stats
        updateProductRatingStats(productId);
    }

    // Vote on review (helpful/unhelpful)
    public ReviewDTO voteOnReview(Long reviewId, Long userId, boolean helpful) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Prevent user from voting on their own review
        if (review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot vote on your own review");
        }

        // Check if user already voted
        Optional<ReviewVote> existingVote = reviewVoteRepository.findByUserIdAndReviewId(userId, reviewId);

        if (existingVote.isPresent()) {
            ReviewVote vote = existingVote.get();
            // If same vote type, remove vote
            if ((helpful && vote.getVoteType() == ReviewVote.VoteType.HELPFUL) ||
                    (!helpful && vote.getVoteType() == ReviewVote.VoteType.UNHELPFUL)) {
                reviewVoteRepository.delete(vote);
            } else {
                // Update vote type
                vote.setVoteType(helpful ? ReviewVote.VoteType.HELPFUL : ReviewVote.VoteType.UNHELPFUL);
                reviewVoteRepository.save(vote);
            }
        } else {
            // Create new vote
            ReviewVote vote = new ReviewVote();
            vote.setReview(review);
            vote.setUser(userRepository.findById(userId).orElseThrow());
            vote.setVoteType(helpful ? ReviewVote.VoteType.HELPFUL : ReviewVote.VoteType.UNHELPFUL);
            vote.setCreatedAt(LocalDateTime.now());
            reviewVoteRepository.save(vote);
        }

        return convertToDTO(review, userId);
    }

    // Get user's reviews
    public List<ReviewDTO> getUserReviews(Long userId) {
        List<Review> reviews = reviewRepository.findByUserId(userId);
        return reviews.stream()
                .map(review -> convertToDTO(review, userId))
                .collect(Collectors.toList());
    }

    // Check if user can review a product
    public Map<String, Object> canUserReviewProduct(Long userId, Long productId) {
        // For now, allow anyone to review. You can add purchase verification later.
        boolean hasReviewed = reviewRepository.findByUserIdAndProductId(userId, productId).isPresent();

        return Map.of(
                "canReview", !hasReviewed,
                "hasReviewed", hasReviewed,
                "message", hasReviewed ? "You have already reviewed this product" : "You can review this product"
        );
    }

    // Helper method to update product rating statistics
    private void updateProductRatingStats(Long productId) {
        // In a real application, you might want to cache these statistics
        // or update a denormalized field in the products table
        log.info("Updated rating stats for product: {}", productId);
    }

    // Convert Review entity to DTO with vote counts
    private ReviewDTO convertToDTO(Review review, Long currentUserId) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setProductId(review.getProduct().getId());
        dto.setProductName(review.getProduct().getName());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getFullName() != null ?
                review.getUser().getFullName() : review.getUser().getEmail());
        dto.setUserEmail(review.getUser().getEmail());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());

        // Get vote counts
        Long helpfulCount = reviewVoteRepository.countHelpfulVotes(review.getId());
        Long unhelpfulCount = reviewVoteRepository.countUnhelpfulVotes(review.getId());
        dto.setHelpfulCount(helpfulCount != null ? helpfulCount.intValue() : 0);
        dto.setUnhelpfulCount(unhelpfulCount != null ? unhelpfulCount.intValue() : 0);

        // Get current user's vote if logged in
        if (currentUserId != null) {
            Optional<ReviewVote> userVote = reviewVoteRepository.findByUserIdAndReviewId(currentUserId, review.getId());
            if (userVote.isPresent()) {
                dto.setCurrentUserVote(userVote.get().getVoteType() == ReviewVote.VoteType.HELPFUL);
            }
        }

        return dto;
    }
}