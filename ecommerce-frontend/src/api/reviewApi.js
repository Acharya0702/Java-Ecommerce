import api from './api';

const reviewApi = {
    // Get reviews for a product
    getProductReviews: (productId, sortBy = 'newest') =>
        api.get(`/reviews/product/${productId}?sortBy=${sortBy}`),

    // Get review statistics for a product
    getProductReviewStats: (productId) =>
        api.get(`/reviews/product/${productId}/stats`),

    // Create a review
    createReview: (reviewData) =>
        api.post('/reviews', reviewData),

    // Update a review
    updateReview: (reviewId, updateData) =>
        api.put(`/reviews/${reviewId}`, updateData),

    // Delete a review
    deleteReview: (reviewId) =>
        api.delete(`/reviews/${reviewId}`),

    // Vote on review
    voteOnReview: (reviewId, helpful) =>
        api.post(`/reviews/${reviewId}/vote?helpful=${helpful}`),

    // Report a review
    reportReview: (reviewId) =>
        api.post(`/reviews/${reviewId}/report`),

    // Get user's reviews
    getUserReviews: () =>
        api.get('/reviews/my-reviews'),

    // Check if user can review a product
    canUserReviewProduct: (productId) =>
        api.get(`/reviews/can-review/${productId}`),

    // Admin: Get unapproved reviews
    getUnapprovedReviews: () =>
        api.get('/reviews/admin/unapproved'),

    // Admin: Moderate review
    moderateReview: (reviewId, approve, response) =>
        api.post(`/reviews/admin/${reviewId}/moderate?approve=${approve}&response=${encodeURIComponent(response || '')}`)
};

export default reviewApi;