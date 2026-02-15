package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Find all reviews for a product
    List<Review> findByProductId(Long productId);

    // Find review by user and product
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    // Find reviews by user
    List<Review> findByUserId(Long userId);

    // Calculate average rating for a product
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    // Count reviews for a product
    Long countByProductId(Long productId);

    // Get rating distribution
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId GROUP BY r.rating")
    List<Object[]> getRatingDistribution(@Param("productId") Long productId);

    // Sort by date (newest first)
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    // Sort by date (oldest first)
    List<Review> findByProductIdOrderByCreatedAtAsc(Long productId);
}