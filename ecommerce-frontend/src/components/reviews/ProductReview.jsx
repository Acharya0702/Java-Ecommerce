import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Button, ProgressBar, Alert, Spinner, Tabs, Tab } from 'react-bootstrap';
import { FaStar, FaEdit, FaChartBar, FaFilter } from 'react-icons/fa';
import StarRating from '../common/StartRating';
import ReviewList from './ReviewList';
import ReviewForm from './ReviewForm';
import reviewApi from '../../api/reviewApi';

const ProductReviews = ({ productId, currentUserId }) => {
    const [reviews, setReviews] = useState([]);
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeTab, setActiveTab] = useState('reviews');
    const [showReviewForm, setShowReviewForm] = useState(false);

    useEffect(() => {
        fetchReviews();
        fetchStats();
    }, [productId]);

    const fetchReviews = async () => {
        try {
            setLoading(true);
            const response = await reviewApi.getProductReviews(productId);
            setReviews(response.data);
            setError(null);
        } catch (error) {
            console.error('Error fetching reviews:', error);
            setError('Failed to load reviews');
        } finally {
            setLoading(false);
        }
    };

    const fetchStats = async () => {
        try {
            const response = await reviewApi.getProductReviewStats(productId);
            setStats(response.data);
        } catch (error) {
            console.error('Error fetching stats:', error);
        }
    };

    const handleReviewSubmitted = (newReview) => {
        setReviews(prev => [newReview, ...prev]);
        fetchStats();
        setShowReviewForm(false);
        setActiveTab('reviews');
    };

    const RatingDistribution = () => {
        if (!stats?.ratingDistribution) return null;

        const distribution = stats.ratingDistribution;
        const total = Object.values(distribution).reduce((a, b) => a + b, 0);

        return (
            <div className="rating-distribution">
                {[5, 4, 3, 2, 1].map((rating) => {
                    const count = distribution[rating] || 0;
                    const percentage = total > 0 ? (count / total) * 100 : 0;

                    return (
                        <div key={rating} className="d-flex align-items-center mb-2">
                            <div className="me-2" style={{ width: '30px' }}>
                                <span className="small">{rating} ★</span>
                            </div>
                            <ProgressBar
                                now={percentage}
                                variant={rating >= 4 ? "success" : rating === 3 ? "warning" : "danger"}
                                style={{ flex: 1, height: '8px' }}
                            />
                            <div className="ms-2" style={{ width: '40px', fontSize: '0.85em' }}>
                                {percentage.toFixed(0)}%
                            </div>
                        </div>
                    );
                })}
            </div>
        );
    };

    if (loading) {
        return (
            <Container className="my-4 text-center">
                <Spinner animation="border" />
                <p>Loading reviews...</p>
            </Container>
        );
    }

    return (
        <Container className="my-4">
            {/* Header */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h3>Customer Reviews</h3>
                <Button
                    variant="primary"
                    onClick={() => setShowReviewForm(true)}
                >
                    <FaEdit className="me-2" />
                    Write a Review
                </Button>
            </div>

            {/* Review Form Modal */}
            {showReviewForm && (
                <div className="modal-backdrop" style={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    backgroundColor: 'rgba(0,0,0,0.5)',
                    zIndex: 1040,
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center'
                }}>
                    <div className="bg-white rounded p-4" style={{ width: '90%', maxWidth: '800px', maxHeight: '90vh', overflow: 'auto' }}>
                        <div className="d-flex justify-content-between align-items-center mb-4">
                            <h4>Write Your Review</h4>
                            <Button variant="link" onClick={() => setShowReviewForm(false)}>
                                ✕
                            </Button>
                        </div>
                        <ReviewForm
                            productId={productId}
                            onSuccess={handleReviewSubmitted}
                            onCancel={() => setShowReviewForm(false)}
                        />
                    </div>
                </div>
            )}

            {/* Stats Summary */}
            {stats && (
                <Card className="mb-4">
                    <Card.Body>
                        <Row>
                            <Col md={3} className="text-center mb-3 mb-md-0">
                                <div className="display-4 fw-bold text-primary">
                                    {stats.averageRating || '0.0'}
                                </div>
                                <div className="mb-2">
                                    <StarRating rating={parseFloat(stats.averageRating) || 0} size={24} />
                                </div>
                                <div className="text-muted">
                                    {stats.totalReviews || 0} reviews
                                </div>
                            </Col>

                            <Col md={6} className="mb-3 mb-md-0">
                                <h6 className="mb-3">Rating Distribution</h6>
                                <RatingDistribution />
                            </Col>

                            <Col md={3}>
                                <div className="stats-details">
                                    <div className="mb-2">
                                        <FaStar className="text-success me-2" />
                                        <span className="fw-bold">{stats.totalVerified || 0}</span>
                                        <span className="text-muted ms-1">Verified Purchases</span>
                                    </div>
                                    <div>
                                        <FaChartBar className="text-info me-2" />
                                        <span className="fw-bold">{stats.withImages || 0}</span>
                                        <span className="text-muted ms-1">Reviews with Photos</span>
                                    </div>
                                </div>
                            </Col>
                        </Row>
                    </Card.Body>
                </Card>
            )}

            {/* Tabs */}
            <Tabs
                activeKey={activeTab}
                onSelect={(k) => setActiveTab(k)}
                className="mb-4"
            >
                <Tab eventKey="reviews" title="All Reviews">
                    {error ? (
                        <Alert variant="danger">{error}</Alert>
                    ) : (
                        <ReviewList
                            reviews={reviews}
                            currentUserId={currentUserId}
                            onReviewsUpdate={fetchReviews}
                        />
                    )}
                </Tab>

                <Tab eventKey="verified" title="Verified Purchases">
                    <ReviewList
                        reviews={reviews.filter(r => r.isVerifiedPurchase)}
                        currentUserId={currentUserId}
                        onReviewsUpdate={fetchReviews}
                    />
                </Tab>

                <Tab eventKey="with-photos" title="With Photos">
                    <ReviewList
                        reviews={reviews.filter(r => r.images && r.images.length > 0)}
                        currentUserId={currentUserId}
                        onReviewsUpdate={fetchReviews}
                    />
                </Tab>
            </Tabs>
        </Container>
    );
};

export default ProductReviews;