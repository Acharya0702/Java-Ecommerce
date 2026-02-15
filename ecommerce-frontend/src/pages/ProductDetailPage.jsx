import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Badge, Alert, Spinner, Tabs, Tab } from 'react-bootstrap';
import { useParams, useNavigate } from 'react-router-dom';
import { FaStar, FaShoppingCart, FaHeart, FaShare, FaTruck, FaShieldAlt, FaArrowLeft } from 'react-icons/fa';
import { useSelector, useDispatch } from 'react-redux';
import api from '../api/api';
import { addToCart } from '../store/slices/cartSlice';
import StarRating from '../components/common/StartRating';
import ProductReviews from '../components/reviews/ProductReview';

const ProductDetailPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const dispatch = useDispatch();

    const [product, setProduct] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedImage, setSelectedImage] = useState('');
    const [quantity, setQuantity] = useState(1);
    const [addingToCart, setAddingToCart] = useState(false);
    const [activeTab, setActiveTab] = useState('description');

    const { isAuthenticated, user } = useSelector(state => state.auth);

    useEffect(() => {
        fetchProduct();
    }, [id]);

    const fetchProduct = async () => {
        try {
            setLoading(true);
            const response = await api.get(`/products/${id}`);
            setProduct(response.data);
            setSelectedImage(response.data.imageUrl);
            setError(null);
        } catch (err) {
            setError('Failed to load product');
            console.error('Error fetching product:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleAddToCart = async () => {
        if (!isAuthenticated) {
            alert('Please login to add items to cart');
            navigate('/login');
            return;
        }

        if (!product.inStock) {
            alert('This product is out of stock');
            return;
        }

        try {
            setAddingToCart(true);
            for (let i = 0; i < quantity; i++) {
                await dispatch(addToCart(product.id)).unwrap();
            }
            alert(`${quantity} item(s) added to cart!`);
        } catch (err) {
            console.error('Error adding to cart:', err);
            alert('Failed to add to cart');
        } finally {
            setAddingToCart(false);
        }
    };

    if (loading) {
        return (
            <Container className="my-5 text-center">
                <Spinner animation="border" />
                <p>Loading product...</p>
            </Container>
        );
    }

    if (error || !product) {
        return (
            <Container className="my-5">
                <Alert variant="danger">
                    <Button variant="link" onClick={() => navigate(-1)} className="p-0 me-2">
                        <FaArrowLeft />
                    </Button>
                    {error || 'Product not found'}
                </Alert>
            </Container>
        );
    }

    return (
        <Container className="my-5">
            <Button variant="link" onClick={() => navigate(-1)} className="mb-4 p-0">
                <FaArrowLeft className="me-2" />
                Back to Products
            </Button>

            <Row>
                {/* Product Images */}
                <Col lg={6}>
                    <Card className="mb-4">
                        <Card.Body className="p-4">
                            <div className="text-center mb-3">
                                <img
                                    src={selectedImage || product.imageUrl}
                                    alt={product.name}
                                    className="img-fluid rounded"
                                    style={{ maxHeight: '400px', objectFit: 'contain' }}
                                />
                            </div>

                            {/* Additional Images */}
                            {product.additionalImages && Object.values(product.additionalImages).length > 0 && (
                                <div className="d-flex flex-wrap gap-2">
                                    <Button
                                        variant={selectedImage === product.imageUrl ? 'primary' : 'outline-secondary'}
                                        size="sm"
                                        onClick={() => setSelectedImage(product.imageUrl)}
                                    >
                                        Main
                                    </Button>
                                    {Object.entries(product.additionalImages).map(([key, url]) => (
                                        <Button
                                            key={key}
                                            variant={selectedImage === url ? 'primary' : 'outline-secondary'}
                                            size="sm"
                                            onClick={() => setSelectedImage(url)}
                                        >
                                            {key}
                                        </Button>
                                    ))}
                                </div>
                            )}
                        </Card.Body>
                    </Card>
                </Col>

                {/* Product Details */}
                <Col lg={6}>
                    <Card className="mb-4">
                        <Card.Body>
                            <h2 className="mb-2">{product.name}</h2>

                            {/* Rating */}
                            <div className="d-flex align-items-center mb-3">
                                <StarRating rating={product.averageRating || 0} size={20} />
                                <span className="ms-2 text-muted">
                                    ({product.totalReviews || 0} reviews)
                                </span>
                                {product.hasDiscount && (
                                    <Badge bg="danger" className="ms-3">
                                        {product.discountPercentage?.toFixed(0)}% OFF
                                    </Badge>
                                )}
                            </div>

                            {/* Price */}
                            <div className="mb-4">
                                {product.hasDiscount ? (
                                    <div>
                                        <h3 className="text-danger">
                                            ${product.discountedPrice}
                                        </h3>
                                        <h5 className="text-muted text-decoration-line-through">
                                            ${product.price}
                                        </h5>
                                    </div>
                                ) : (
                                    <h3>${product.price}</h3>
                                )}
                            </div>

                            {/* Stock Status */}
                            <div className="mb-4">
                                <Badge bg={product.inStock ? 'success' : 'danger'}>
                                    {product.inStock ? 'In Stock' : 'Out of Stock'}
                                </Badge>
                                {product.inStock && (
                                    <span className="ms-2 text-muted">
                                        {product.stockQuantity > 10 ? 'Ships in 24 hours' : `Only ${product.stockQuantity} left`}
                                    </span>
                                )}
                            </div>

                            {/* SKU */}
                            <div className="mb-4">
                                <small className="text-muted">SKU: {product.sku}</small>
                            </div>

                            {/* Quantity Selector */}
                            <div className="mb-4">
                                <label className="form-label">Quantity:</label>
                                <div className="d-flex align-items-center" style={{ width: '120px' }}>
                                    <Button
                                        variant="outline-secondary"
                                        size="sm"
                                        onClick={() => setQuantity(prev => Math.max(1, prev - 1))}
                                        disabled={quantity <= 1}
                                    >
                                        -
                                    </Button>
                                    <input
                                        type="number"
                                        className="form-control text-center mx-2"
                                        value={quantity}
                                        onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                                        min="1"
                                        max={Math.min(product.stockQuantity, 10)}
                                        style={{ width: '60px' }}
                                    />
                                    <Button
                                        variant="outline-secondary"
                                        size="sm"
                                        onClick={() => setQuantity(prev => Math.min(product.stockQuantity, prev + 1))}
                                        disabled={quantity >= product.stockQuantity || quantity >= 10}
                                    >
                                        +
                                    </Button>
                                </div>
                            </div>

                            {/* Action Buttons */}
                            <div className="d-flex flex-wrap gap-2 mb-4">
                                <Button
                                    variant="primary"
                                    size="lg"
                                    className="flex-grow-1"
                                    onClick={handleAddToCart}
                                    disabled={!product.inStock || addingToCart}
                                >
                                    <FaShoppingCart className="me-2" />
                                    {addingToCart ? 'Adding...' : 'Add to Cart'}
                                </Button>

                                <Button variant="outline-primary" size="lg">
                                    <FaHeart className="me-2" />
                                    Wishlist
                                </Button>

                                <Button variant="outline-secondary" size="lg">
                                    <FaShare className="me-2" />
                                    Share
                                </Button>
                            </div>

                            {/* Shipping Info */}
                            <div className="border-top pt-3">
                                <div className="d-flex align-items-center mb-2">
                                    <FaTruck className="text-muted me-2" />
                                    <span>Free shipping on orders over $50</span>
                                </div>
                                <div className="d-flex align-items-center">
                                    <FaShieldAlt className="text-muted me-2" />
                                    <span>30-day return policy</span>
                                </div>
                            </div>
                        </Card.Body>
                    </Card>
                </Col>
            </Row>

            {/* Product Tabs */}
            <Card className="mb-4">
                <Card.Body>
                    <Tabs
                        activeKey={activeTab}
                        onSelect={(k) => setActiveTab(k)}
                        className="mb-3"
                    >
                        <Tab eventKey="description" title="Description">
                            <div className="p-3">
                                <h5>Product Description</h5>
                                <p style={{ whiteSpace: 'pre-line' }}>
                                    {product.description}
                                </p>
                            </div>
                        </Tab>

                        <Tab eventKey="specifications" title="Specifications">
                            <div className="p-3">
                                <h5>Product Specifications</h5>
                                {product.specifications ? (
                                    <table className="table">
                                        <tbody>
                                        {Object.entries(product.specifications).map(([key, value]) => (
                                            <tr key={key}>
                                                <td style={{ width: '200px' }}><strong>{key}</strong></td>
                                                <td>{value}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                ) : (
                                    <p className="text-muted">No specifications available.</p>
                                )}
                            </div>
                        </Tab>

                        <Tab eventKey="reviews" title="Reviews">
                            <div className="p-3">
                                <ProductReviews
                                    productId={product.id}
                                    currentUserId={user?.id}
                                />
                            </div>
                        </Tab>
                    </Tabs>
                </Card.Body>
            </Card>
        </Container>
    );
};

export default ProductDetailPage;