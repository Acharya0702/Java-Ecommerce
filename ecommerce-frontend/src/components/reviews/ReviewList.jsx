import React, { useState } from 'react';
import { Card, Button, Badge, Dropdown, Form } from 'react-bootstrap';
import { FaStar, FaCheckCircle, FaThumbsUp, FaThumbsDown, FaFlag, FaImages, FaChevronDown } from 'react-icons/fa';
import StarRating from '../common/StartRating';
import reviewApi from '../../api/reviewApi';

const ReviewItem = ({ review, currentUserId, onVote, onReport, onDelete }) => {
    const [helpfulVoted, setHelpfulVoted] = useState(false);
    const [unhelpfulVoted, setUnhelpfulVoted] = useState(false);
    const [showImages, setShowImages] = useState(false);
    const [isEditing, setIsEditing] = useState(false);
    const [editComment, setEditComment] = useState(review.comment);
    const [isUpdating, setIsUpdating] = useState(false);

    const handleVote = async (helpful) => {
        try {
            await reviewApi.voteOnReview(review.id, helpful);

            if (helpful) {
                setHelpfulVoted(true);
                setUnhelpfulVoted(false);
            } else {
                setHelpfulVoted(false);
                setUnhelpfulVoted(true);
            }

            if (onVote) {
                onVote(review.id, helpful);
            }
        } catch (error) {
            console.error('Error voting:', error);
            alert('You can only vote once per review');
        }
    };

    const handleReport = async () => {
        if (window.confirm('Are you sure you want to report this review?')) {
            try {
                await reviewApi.reportReview(review.id);
                if (onReport) {
                    onReport(review.id);
                }
                alert('Review reported. Thank you for your feedback.');
            } catch (error) {
                console.error('Error reporting review:', error);
            }
        }
    };

    const handleDelete = async () => {
        if (window.confirm('Are you sure you want to delete this review?')) {
            try {
                await reviewApi.deleteReview(review.id);
                if (onDelete) {
                    onDelete(review.id);
                }
            } catch (error) {
                console.error('Error deleting review:', error);
            }
        }
    };

    const handleSaveEdit = async () => {
        if (!editComment.trim() || editComment.trim().length < 10) {
            alert('Review must be at least 10 characters');
            return;
        }

        setIsUpdating(true);
        try {
            await reviewApi.updateReview(review.id, { comment: editComment });
            setIsEditing(false);
            review.comment = editComment;
        } catch (error) {
            console.error('Error updating review:', error);
        } finally {
            setIsUpdating(false);
        }
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    };

    const canEditDelete = currentUserId === review.userId;

    return (
        <Card className="mb-3 border">
            <Card.Body>
                {/* Header */}
                <div className="d-flex justify-content-between align-items-start mb-3">
                    <div>
                        <div className="d-flex align-items-center mb-2">
                            <div className="me-3">
                                <div className="bg-light rounded-circle d-flex align-items-center justify-content-center"
                                     style={{ width: '40px', height: '40px' }}>
                                    <span className="fw-bold">
                                        {review.userName?.charAt(0).toUpperCase() || 'U'}
                                    </span>
                                </div>
                            </div>
                            <div>
                                <div className="fw-bold">{review.userName}</div>
                                <div className="text-muted small">
                                    {formatDate(review.createdAt)}
                                    {review.isVerifiedPurchase && (
                                        <>
                                            <span className="mx-2">•</span>
                                            <FaCheckCircle className="text-success me-1" />
                                            <span className="text-success">Verified Purchase</span>
                                        </>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Rating and Actions */}
                    <div className="text-end">
                        <StarRating rating={review.rating} size={16} />
                        <div className="mt-1">
                            {canEditDelete && (
                                <Dropdown className="d-inline-block ms-2">
                                    <Dropdown.Toggle variant="link" size="sm" className="text-muted p-0">
                                        <FaChevronDown />
                                    </Dropdown.Toggle>
                                    <Dropdown.Menu>
                                        <Dropdown.Item onClick={() => setIsEditing(true)}>
                                            Edit
                                        </Dropdown.Item>
                                        <Dropdown.Item onClick={handleDelete} className="text-danger">
                                            Delete
                                        </Dropdown.Item>
                                    </Dropdown.Menu>
                                </Dropdown>
                            )}
                        </div>
                    </div>
                </div>

                {/* Title */}
                {review.title && (
                    <h6 className="fw-bold mb-2">{review.title}</h6>
                )}

                {/* Review Content */}
                {isEditing ? (
                    <div className="mb-3">
                        <Form.Control
                            as="textarea"
                            value={editComment}
                            onChange={(e) => setEditComment(e.target.value)}
                            rows={3}
                        />
                        <div className="mt-2">
                            <Button
                                size="sm"
                                variant="primary"
                                onClick={handleSaveEdit}
                                disabled={isUpdating}
                                className="me-2"
                            >
                                {isUpdating ? 'Saving...' : 'Save'}
                            </Button>
                            <Button
                                size="sm"
                                variant="outline-secondary"
                                onClick={() => {
                                    setIsEditing(false);
                                    setEditComment(review.comment);
                                }}
                            >
                                Cancel
                            </Button>
                        </div>
                    </div>
                ) : (
                    <p className="mb-3" style={{ whiteSpace: 'pre-wrap' }}>
                        {review.comment}
                    </p>
                )}

                {/* Detailed Ratings */}
                {(review.sizeRating || review.comfortRating || review.qualityRating || review.valueRating) && (
                    <div className="mb-3">
                        <div className="row g-2">
                            {review.sizeRating && (
                                <div className="col-auto">
                                    <Badge bg="light" text="dark" className="me-1">
                                        Size: {review.sizeRating}/5
                                    </Badge>
                                </div>
                            )}
                            {review.comfortRating && (
                                <div className="col-auto">
                                    <Badge bg="light" text="dark" className="me-1">
                                        Comfort: {review.comfortRating}/5
                                    </Badge>
                                </div>
                            )}
                            {review.qualityRating && (
                                <div className="col-auto">
                                    <Badge bg="light" text="dark" className="me-1">
                                        Quality: {review.qualityRating}/5
                                    </Badge>
                                </div>
                            )}
                            {review.valueRating && (
                                <div className="col-auto">
                                    <Badge bg="light" text="dark" className="me-1">
                                        Value: {review.valueRating}/5
                                    </Badge>
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {/* Review Images */}
                {review.images && review.images.length > 0 && (
                    <div className="mb-3">
                        <Button
                            variant="outline-secondary"
                            size="sm"
                            onClick={() => setShowImages(!showImages)}
                            className="mb-2"
                        >
                            <FaImages className="me-1" />
                            {showImages ? 'Hide' : 'Show'} Photos ({review.images.length})
                        </Button>

                        {showImages && (
                            <div className="d-flex flex-wrap gap-2 mt-2">
                                {review.images.map((image, index) => (
                                    <img
                                        key={index}
                                        src={image}
                                        alt={`Review ${index + 1}`}
                                        className="img-thumbnail"
                                        style={{ width: '80px', height: '80px', objectFit: 'cover', cursor: 'pointer' }}
                                        onClick={() => window.open(image, '_blank')}
                                    />
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {/* Seller Response */}
                {review.sellerResponse && (
                    <div className="alert alert-info mt-3 mb-3">
                        <div className="fw-bold mb-1">Seller Response:</div>
                        <p className="mb-0" style={{ whiteSpace: 'pre-wrap' }}>
                            {review.sellerResponse}
                        </p>
                    </div>
                )}

                {/* Helpful Votes */}
                <div className="d-flex justify-content-between align-items-center border-top pt-3">
                    <div>
                        <span className="text-muted small me-3">
                            Was this review helpful?
                        </span>
                        <Button
                            variant={helpfulVoted ? "primary" : "outline-primary"}
                            size="sm"
                            className="me-2"
                            onClick={() => handleVote(true)}
                            disabled={helpfulVoted || unhelpfulVoted}
                        >
                            <FaThumbsUp className="me-1" />
                            Helpful ({review.helpfulVotes || 0})
                        </Button>
                        <Button
                            variant={unhelpfulVoted ? "danger" : "outline-danger"}
                            size="sm"
                            onClick={() => handleVote(false)}
                            disabled={helpfulVoted || unhelpfulVoted}
                        >
                            <FaThumbsDown className="me-1" />
                            Not Helpful ({review.unhelpfulVotes || 0})
                        </Button>
                    </div>

                    <Button
                        variant="link"
                        size="sm"
                        className="text-muted"
                        onClick={handleReport}
                        title="Report inappropriate review"
                    >
                        <FaFlag className="me-1" />
                        Report
                    </Button>
                </div>
            </Card.Body>
        </Card>
    );
};

const ReviewList = ({ reviews, currentUserId, onReviewsUpdate }) => {
    const [sortBy, setSortBy] = useState('newest');

    const sortedReviews = [...reviews].sort((a, b) => {
        switch (sortBy) {
            case 'helpful':
                return (b.helpfulVotes || 0) - (a.helpfulVotes || 0);
            case 'rating_high':
                return (b.rating || 0) - (a.rating || 0);
            case 'rating_low':
                return (a.rating || 0) - (b.rating || 0);
            case 'oldest':
                return new Date(a.createdAt) - new Date(b.createdAt);
            default: // newest
                return new Date(b.createdAt) - new Date(a.createdAt);
        }
    });

    const handleVote = (reviewId, helpful) => {
        if (onReviewsUpdate) {
            onReviewsUpdate();
        }
    };

    const handleReport = (reviewId) => {
        if (onReviewsUpdate) {
            onReviewsUpdate();
        }
    };

    const handleDelete = (reviewId) => {
        if (onReviewsUpdate) {
            onReviewsUpdate();
        }
    };

    return (
        <div>
            {/* Sort Controls */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h5 className="mb-0">
                    Customer Reviews ({reviews.length})
                </h5>

                <div className="d-flex align-items-center">
                    <span className="me-2">Sort by:</span>
                    <Form.Select
                        size="sm"
                        style={{ width: 'auto' }}
                        value={sortBy}
                        onChange={(e) => setSortBy(e.target.value)}
                    >
                        <option value="newest">Most Recent</option>
                        <option value="helpful">Most Helpful</option>
                        <option value="rating_high">Highest Rating</option>
                        <option value="rating_low">Lowest Rating</option>
                        <option value="oldest">Oldest First</option>
                    </Form.Select>
                </div>
            </div>

            {/* Reviews List */}
            {sortedReviews.length === 0 ? (
                <div className="text-center py-5">
                    <div className="mb-3">
                        <FaStar size={48} className="text-muted" />
                    </div>
                    <h5>No reviews yet</h5>
                    <p className="text-muted">Be the first to review this product!</p>
                </div>
            ) : (
                <>
                    {sortedReviews.map((review) => (
                        <ReviewItem
                            key={review.id}
                            review={review}
                            currentUserId={currentUserId}
                            onVote={handleVote}
                            onReport={handleReport}
                            onDelete={handleDelete}
                        />
                    ))}
                </>
            )}
        </div>
    );
};

export default ReviewList;