package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {

    // Find vote by user and review
    Optional<ReviewVote> findByUserIdAndReviewId(Long userId, Long reviewId);

    // Count helpful votes for a review
    @Query("SELECT COUNT(v) FROM ReviewVote v WHERE v.review.id = :reviewId AND v.voteType = 'HELPFUL'")
    Long countHelpfulVotes(@Param("reviewId") Long reviewId);

    // Count unhelpful votes for a review
    @Query("SELECT COUNT(v) FROM ReviewVote v WHERE v.review.id = :reviewId AND v.voteType = 'UNHELPFUL'")
    Long countUnhelpfulVotes(@Param("reviewId") Long reviewId);

    // Check if user has voted on a review
    boolean existsByUserIdAndReviewId(Long userId, Long reviewId);

    // Delete vote by user and review
    void deleteByUserIdAndReviewId(Long userId, Long reviewId);
}