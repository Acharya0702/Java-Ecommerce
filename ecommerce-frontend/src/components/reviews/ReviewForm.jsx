import React, { useState, useEffect } from 'react';
import { Form, Button, Alert, Row, Col, Card } from 'react-bootstrap';
import { FaStar, FaCamera, FaTimes } from 'react-icons/fa';
import StarRating from '../common/StartRating';
import reviewApi from '../../api/reviewApi';

const ReviewForm = ({ productId, orderId, onSuccess, onCancel }) => {
    const [formData, setFormData] = useState({
        productId: productId,
        orderId: orderId || null,
        rating: 0,
        title: '',
        comment: '',
        sizeRating: null,
        comfortRating: null,
        qualityRating: null,
        valueRating: null,
        images: new Set()
    });

    const [errors, setErrors] = useState({});
    const [loading, setLoading] = useState(false);
    const [imagePreviews, setImagePreviews] = useState([]);
    const [canReview, setCanReview] = useState(false);
    const [checkingReview, setCheckingReview] = useState(true);

    useEffect(() => {
        checkCanReview();
    }, []);

    const checkCanReview = async () => {
        try {
            const response = await reviewApi.canUserReviewProduct(productId);
            setCanReview(response.data.canReview);
        } catch (error) {
            console.error('Error checking review eligibility:', error);
        } finally {
            setCheckingReview(false);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));

        // Clear error for this field
        if (errors[name]) {
            setErrors(prev => ({
                ...prev,
                [name]: null
            }));
        }
    };

    const handleRatingChange = (rating, field = 'rating') => {
        setFormData(prev => ({
            ...prev,
            [field]: rating
        }));
    };

    const handleImageUpload = (e) => {
        const files = Array.from(e.target.files);

        // Limit to 5 images
        if (formData.images.size + files.length > 5) {
            alert('You can upload maximum 5 images');
            return;
        }

        // Create previews
        const newPreviews = files.map(file => ({
            file,
            preview: URL.createObjectURL(file),
            name: file.name
        }));

        setImagePreviews(prev => [...prev, ...newPreviews]);
        setFormData(prev => ({
            ...prev,
            images: new Set([...prev.images, ...files])
        }));
    };

    const removeImage = (index) => {
        setImagePreviews(prev => prev.filter((_, i) => i !== index));
        const updatedImages = Array.from(formData.images);
        updatedImages.splice(index, 1);
        setFormData(prev => ({
            ...prev,
            images: new Set(updatedImages)
        }));
    };

    const validateForm = () => {
        const newErrors = {};

        if (!formData.rating || formData.rating < 1) {
            newErrors.rating = 'Please select a rating';
        }

        if (!formData.comment.trim()) {
            newErrors.comment = 'Please write your review';
        } else if (formData.comment.trim().length < 10) {
            newErrors.comment = 'Review must be at least 10 characters';
        }

        if (formData.title && formData.title.length > 200) {
            newErrors.title = 'Title cannot exceed 200 characters';
        }

        return newErrors;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        const validationErrors = validateForm();
        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors);
            return;
        }

        setLoading(true);
        try {
            // Convert Set to Array for API
            const submitData = {
                ...formData,
                images: Array.from(formData.images).map(file => file.name) // In real app, upload to server first
            };

            const response = await reviewApi.createReview(submitData);

            // Clean up image previews
            imagePreviews.forEach(preview => URL.revokeObjectURL(preview.preview));

            if (onSuccess) {
                onSuccess(response.data);
            }

            alert('Thank you for your review!');
        } catch (error) {
            console.error('Error submitting review:', error);
            const errorMessage = error.response?.data?.message || 'Failed to submit review';
            setErrors({ submit: errorMessage });
        } finally {
            setLoading(false);
        }
    };

    if (checkingReview) {
        return (
            <Card className="mb-4">
                <Card.Body className="text-center">
                    <div className="spinner-border text-primary" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                    <p className="mt-2">Checking review eligibility...</p>
                </Card.Body>
            </Card>
        );
    }

    if (!canReview) {
        return (
            <Card className="mb-4">
                <Card.Body className="text-center">
                    <Alert variant="info">
                        <h5>You cannot review this product</h5>
                        <p className="mb-0">
                            You need to purchase this product first before leaving a review.
                        </p>
                    </Alert>
                </Card.Body>
            </Card>
        );
    }

    return (
        <Card className="mb-4">
            <Card.Header>
                <h5 className="mb-0">Write a Review</h5>
            </Card.Header>
            <Card.Body>
                {errors.submit && (
                    <Alert variant="danger" dismissible onClose={() => setErrors({})}>
                        {errors.submit}
                    </Alert>
                )}

                <Form onSubmit={handleSubmit}>
                    {/* Overall Rating */}
                    <Form.Group className="mb-4">
                        <Form.Label className="d-block fw-bold mb-3">
                            Overall Rating *
                        </Form.Label>
                        <div className="d-flex align-items-center">
                            <StarRating
                                rating={formData.rating}
                                editable={true}
                                onChange={(rating) => handleRatingChange(rating)}
                                size={28}
                            />
                            <span className="ms-3" style={{ fontSize: '1.1em' }}>
                                {formData.rating > 0 ? (
                                    <>
                                        <strong>{formData.rating}/5</strong>
                                        <span className="text-muted ms-1">
                                            ({formData.rating === 5 ? 'Excellent' :
                                            formData.rating === 4 ? 'Good' :
                                                formData.rating === 3 ? 'Average' :
                                                    formData.rating === 2 ? 'Poor' : 'Very Poor'})
                                        </span>
                                    </>
                                ) : 'Select rating'}
                            </span>
                        </div>
                        {errors.rating && (
                            <Form.Text className="text-danger">{errors.rating}</Form.Text>
                        )}
                    </Form.Group>

                    {/* Title */}
                    <Form.Group className="mb-4">
                        <Form.Label>Review Title</Form.Label>
                        <Form.Control
                            type="text"
                            name="title"
                            value={formData.title}
                            onChange={handleInputChange}
                            placeholder="Summarize your experience (optional)"
                            maxLength={200}
                        />
                        <Form.Text className="text-muted">
                            {formData.title.length}/200 characters
                        </Form.Text>
                        {errors.title && (
                            <Form.Text className="text-danger">{errors.title}</Form.Text>
                        )}
                    </Form.Group>

                    {/* Review Comment */}
                    <Form.Group className="mb-4">
                        <Form.Label>Your Review *</Form.Label>
                        <Form.Control
                            as="textarea"
                            name="comment"
                            value={formData.comment}
                            onChange={handleInputChange}
                            rows={4}
                            placeholder="Share your experience with this product..."
                            required
                        />
                        <Form.Text className="text-muted">
                            Minimum 10 characters. Be specific about what you liked or disliked.
                        </Form.Text>
                        {errors.comment && (
                            <Form.Text className="text-danger">{errors.comment}</Form.Text>
                        )}
                    </Form.Group>

                    {/* Detailed Ratings */}
                    <div className="mb-4">
                        <Form.Label className="d-block fw-bold mb-3">
                            Detailed Ratings (Optional)
                        </Form.Label>
                        <Row>
                            {[
                                { label: 'Size', field: 'sizeRating' },
                                { label: 'Comfort', field: 'comfortRating' },
                                { label: 'Quality', field: 'qualityRating' },
                                { label: 'Value', field: 'valueRating' }
                            ].map((item, index) => (
                                <Col xs={6} md={3} key={index} className="mb-3">
                                    <div className="detailed-rating">
                                        <div className="mb-2">{item.label}</div>
                                        <StarRating
                                            rating={formData[item.field] || 0}
                                            editable={true}
                                            onChange={(rating) => handleRatingChange(rating, item.field)}
                                            size={20}
                                            maxRating={5}
                                        />
                                    </div>
                                </Col>
                            ))}
                        </Row>
                    </div>

                    {/* Image Upload */}
                    <Form.Group className="mb-4">
                        <Form.Label>Add Photos (Optional)</Form.Label>
                        <div className="border rounded p-3 text-center">
                            <input
                                type="file"
                                id="review-images"
                                multiple
                                accept="image/*"
                                onChange={handleImageUpload}
                                style={{ display: 'none' }}
                            />
                            <label htmlFor="review-images" className="btn btn-outline-secondary mb-3">
                                <FaCamera className="me-2" />
                                Upload Photos
                            </label>
                            <p className="text-muted small mb-3">
                                Upload up to 5 photos. JPG, PNG, GIF accepted.
                            </p>

                            {imagePreviews.length > 0 && (
                                <div className="image-previews d-flex flex-wrap gap-2">
                                    {imagePreviews.map((preview, index) => (
                                        <div key={index} className="position-relative" style={{ width: '100px' }}>
                                            <img
                                                src={preview.preview}
                                                alt={`Preview ${index + 1}`}
                                                className="img-thumbnail"
                                                style={{ width: '100px', height: '100px', objectFit: 'cover' }}
                                            />
                                            <Button
                                                variant="danger"
                                                size="sm"
                                                className="position-absolute top-0 end-0"
                                                style={{ transform: 'translate(30%, -30%)' }}
                                                onClick={() => removeImage(index)}
                                            >
                                                <FaTimes />
                                            </Button>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </Form.Group>

                    {/* Verified Purchase Badge */}
                    {orderId && (
                        <Alert variant="success" className="mb-4">
                            <FaStar className="me-2" />
                            <strong>Verified Purchase</strong>
                            <p className="mb-0 mt-1">Your review will be marked as a verified purchase.</p>
                        </Alert>
                    )}

                    {/* Submit Buttons */}
                    <div className="d-flex justify-content-between">
                        <Button
                            variant="outline-secondary"
                            onClick={onCancel}
                            disabled={loading}
                        >
                            Cancel
                        </Button>
                        <Button
                            type="submit"
                            variant="primary"
                            disabled={loading}
                        >
                            {loading ? (
                                <>
                                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                                    Submitting...
                                </>
                            ) : (
                                'Submit Review'
                            )}
                        </Button>
                    </div>
                </Form>
            </Card.Body>
        </Card>
    );
};

export default ReviewForm;