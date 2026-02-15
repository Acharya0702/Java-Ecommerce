// src/pages/ProductsPage.js
import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Container, Row, Col, Card, Button, Spinner, Alert } from 'react-bootstrap';
import { productApi } from '../api/productApi';
import { addToCart } from '../store/slices/cartSlice'; // Import addToCart
import { toast } from 'react-toastify';

const ProductsPage = () => {
    const navigate = useNavigate();
    const dispatch = useDispatch();
    const [searchParams] = useSearchParams();
    const { user, token } = useSelector((state) => state.auth);
    const [addingToCart, setAddingToCart] = useState({}); // Track which products are being added

    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchProducts();
    }, [searchParams]);

    const fetchProducts = async () => {
        try {
            setLoading(true);
            const category = searchParams.get('category');
            const search = searchParams.get('search');

            let data;
            if (search) {
                data = await productApi.searchProducts(search);
            } else if (category) {
                data = await productApi.getProductsByCategory(category);
            } else {
                data = await productApi.getAllProducts();
            }

            setProducts(data);
            setError(null);
        } catch (err) {
            console.error('Error fetching products:', err);
            setError('Failed to load products. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleAddToCart = async (product) => {
        if (!token) {
            navigate('/login', {
                state: {
                    from: '/products',
                    message: 'Please login to add items to cart'
                }
            });
            return;
        }

        // Set loading state for this specific product
        setAddingToCart(prev => ({ ...prev, [product.id]: true }));

        try {
            // Dispatch the addToCart action
            await dispatch(addToCart(product.id)).unwrap();
            toast.success(`${product.name} added to cart!`);
        } catch (error) {
            console.error('Error adding to cart:', error);
            toast.error(typeof error === 'string' ? error : 'Failed to add item to cart');
        } finally {
            // Clear loading state
            setAddingToCart(prev => ({ ...prev, [product.id]: false }));
        }
    };

    if (loading) {
        return (
            <Container className="text-center py-5">
                <Spinner animation="border" variant="primary" />
                <p className="mt-3">Loading products...</p>
            </Container>
        );
    }

    if (error) {
        return (
            <Container className="py-5">
                <Alert variant="danger">{error}</Alert>
            </Container>
        );
    }

    return (
        <Container className="py-4">
            <h2 className="mb-4">Our Products</h2>

            {!token && (
                <Alert variant="info" className="mb-4">
                    <Alert.Heading>Welcome Guest! 👋</Alert.Heading>
                    <p>
                        You're browsing as a guest.{' '}
                        <Button
                            variant="primary"
                            size="sm"
                            onClick={() => navigate('/login', { state: { from: '/products' } })}
                        >
                            Sign in
                        </Button>{' '}
                        to add items to your cart and track orders.
                    </p>
                </Alert>
            )}

            {products.length === 0 ? (
                <Alert variant="warning">No products found.</Alert>
            ) : (
                <Row>
                    {products.map((product) => (
                        <Col key={product.id} md={4} lg={3} className="mb-4">
                            <Card className="h-100 shadow-sm hover-shadow">
                                <Card.Img
                                    variant="top"
                                    src={product.imageUrl || 'https://via.placeholder.com/300x200'}
                                    alt={product.name}
                                    style={{ height: '200px', objectFit: 'cover' }}
                                />
                                <Card.Body>
                                    <Card.Title>{product.name}</Card.Title>
                                    <Card.Text className="text-muted small">
                                        {product.description?.substring(0, 100)}...
                                    </Card.Text>
                                    <div className="d-flex justify-content-between align-items-center">
                                        <h5 className="text-primary mb-0">${product.price}</h5>
                                        <Button
                                            variant={token ? "primary" : "outline-primary"}
                                            size="sm"
                                            onClick={() => handleAddToCart(product)}
                                            disabled={addingToCart[product.id]} // Disable while adding
                                        >
                                            {addingToCart[product.id] ? (
                                                <>
                                                    <Spinner
                                                        as="span"
                                                        animation="border"
                                                        size="sm"
                                                        role="status"
                                                        aria-hidden="true"
                                                        className="me-2"
                                                    />
                                                    Adding...
                                                </>
                                            ) : (
                                                token ? 'Add to Cart' : 'Login to Buy'
                                            )}
                                        </Button>
                                    </div>
                                </Card.Body>
                                <Card.Footer className="bg-white border-0">
                                    <small className="text-muted">
                                        {product.category}
                                    </small>
                                </Card.Footer>
                            </Card>
                        </Col>
                    ))}
                </Row>
            )}
        </Container>
    );
};

export default ProductsPage;