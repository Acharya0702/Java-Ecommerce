package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.ReviewDTO;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import com.ecommerce.ecommercebackend.security.JwtService;
import com.ecommerce.ecommercebackend.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    // Helper method to get user ID from JWT token
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                String email = jwtService.extractUsername(token);

                // Get user from database by email
                return userRepository.findByEmail(email)
                        .map(User::getId)
                        .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            } catch (Exception e) {
                throw new RuntimeException("Invalid token or user not found: " + e.getMessage());
            }
        }
        return null; // Return null for non-authenticated users
    }

    // Get all reviews for a product
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDTO>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            HttpServletRequest request) {

        Long currentUserId = getUserIdFromRequest(request);
        List<ReviewDTO> reviews = reviewService.getProductReviews(productId, sortBy, currentUserId);
        return ResponseEntity.ok(reviews);
    }

    // Get review statistics for a product
    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<Map<String, Object>> getProductReviewStats(@PathVariable Long productId) {
        Map<String, Object> stats = reviewService.getProductReviewStats(productId);
        return ResponseEntity.ok(stats);
    }

    // Create a new review
    @PostMapping
    public ResponseEntity<?> createReview(
            HttpServletRequest request,
            @RequestBody ReviewDTO.CreateReviewDTO reviewRequest) {

        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            ReviewDTO review = reviewService.createReview(userId, reviewRequest);
            return ResponseEntity.ok(review);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Update a review
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            HttpServletRequest request,
            @PathVariable Long reviewId,
            @RequestBody ReviewDTO.UpdateReviewDTO updateRequest) {

        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            ReviewDTO updatedReview = reviewService.updateReview(reviewId, userId, updateRequest);
            return ResponseEntity.ok(updatedReview);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Delete a review
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            HttpServletRequest request,
            @PathVariable Long reviewId) {

        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            reviewService.deleteReview(reviewId, userId, false);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Vote on review (helpful/unhelpful)
    @PostMapping("/{reviewId}/vote")
    public ResponseEntity<?> voteOnReview(
            HttpServletRequest request,
            @PathVariable Long reviewId,
            @RequestParam boolean helpful) {

        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            ReviewDTO review = reviewService.voteOnReview(reviewId, userId, helpful);
            return ResponseEntity.ok(review);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get user's reviews
    @GetMapping("/my-reviews")
    public ResponseEntity<?> getUserReviews(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            List<ReviewDTO> reviews = reviewService.getUserReviews(userId);
            return ResponseEntity.ok(reviews);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // Check if user can review a product
    @GetMapping("/can-review/{productId}")
    public ResponseEntity<?> canUserReviewProduct(
            HttpServletRequest request,
            @PathVariable Long productId) {

        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            Map<String, Object> canReview = reviewService.canUserReviewProduct(userId, productId);
            return ResponseEntity.ok(canReview);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // Add a test endpoint for debugging
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            String authHeader = request.getHeader("Authorization");

            return ResponseEntity.ok(Map.of(
                    "message", "Review endpoint is working!",
                    "userId", userId,
                    "hasAuthHeader", authHeader != null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}