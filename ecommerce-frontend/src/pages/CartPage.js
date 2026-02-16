import React, { useEffect, useState } from 'react';
import { Container, Row, Col, Card, Table, Button, Alert, Spinner, Form } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { FaTrash, FaShoppingCart, FaArrowLeft, FaPlus, FaMinus } from 'react-icons/fa';
import {
    fetchCart,
    updateCartItem,
    removeCartItem,
    clearCart,
    addToCart
} from '../store/slices/cartSlice';
import { toast } from 'react-toastify';

const CartPage = () => {
    const navigate = useNavigate();
    const dispatch = useDispatch();
    const { user, accessToken } = useSelector((state) => state.auth);
    const { items, totalItems, totalAmount, loading, error } = useSelector((state) => state.cart);

    const [updatingItems, setUpdatingItems] = useState({});
    const [quantities, setQuantities] = useState({});

    useEffect(() => {
        if (accessToken) {
            dispatch(fetchCart());
        }
    }, [accessToken, dispatch]);

    // Initialize quantities from items
    useEffect(() => {
        const initialQuantities = {};
        items.forEach(item => {
            initialQuantities[item.id] = item.quantity;
        });
        setQuantities(initialQuantities);
    }, [items]);

    const handleQuantityChange = async (itemId, newQuantity, productStock = 99) => {
        if (newQuantity < 1) return;
        if (newQuantity > productStock) {
            toast.warning(`Only ${productStock} items available`);
            return;
        }

        // Update local state immediately for better UX
        setQuantities(prev => ({ ...prev, [itemId]: newQuantity }));
        setUpdatingItems(prev => ({ ...prev, [itemId]: true }));

        try {
            await dispatch(updateCartItem({ itemId, quantity: newQuantity })).unwrap();
            toast.success('Cart updated');
        } catch (error) {
            // Revert on error
            setQuantities(prev => ({ ...prev, [itemId]: quantities[itemId] }));
            console.error('Error updating quantity:', error);
            toast.error(typeof error === 'string' ? error : 'Failed to update quantity');
        } finally {
            setUpdatingItems(prev => ({ ...prev, [itemId]: false }));
        }
    };

    const handleRemoveItem = async (itemId) => {
        if (!window.confirm('Are you sure you want to remove this item?')) return;

        setUpdatingItems(prev => ({ ...prev, [itemId]: true }));

        try {
            await dispatch(removeCartItem(itemId)).unwrap();
            toast.success('Item removed from cart');
        } catch (error) {
            console.error('Error removing item:', error);
            toast.error(typeof error === 'string' ? error : 'Failed to remove item');
        } finally {
            setUpdatingItems(prev => ({ ...prev, [itemId]: false }));
        }
    };

    const handleClearCart = async () => {
        if (!window.confirm('Are you sure you want to clear your entire cart?')) return;

        try {
            await dispatch(clearCart()).unwrap();
            toast.success('Cart cleared');
        } catch (error) {
            console.error('Error clearing cart:', error);
            toast.error(typeof error === 'string' ? error : 'Failed to clear cart');
        }
    };

    const calculateShipping = () => {
        return totalAmount > 100 ? 0 : 10;
    };

    const calculateTax = () => {
        return totalAmount * 0.08; // 8% tax
    };

    const shipping = calculateShipping();
    const tax = calculateTax();
    const total = totalAmount + shipping + tax;

    if (!accessToken) {
        return (
            <Container className="my-5 text-center">
                <FaShoppingCart size={60} className="text-muted mb-4" />
                <h3>Your cart is empty</h3>
                <p className="text-muted">Please login to view your cart</p>
                <Button as={Link} to="/login" variant="primary" size="lg" className="mt-3">
                    Login
                </Button>
            </Container>
        );
    }

    if (loading && items.length === 0) {
        return (
            <Container className="my-5 text-center">
                <Spinner animation="border" variant="primary" />
                <p className="mt-3">Loading your cart...</p>
            </Container>
        );
    }

    if (error) {
        return (
            <Container className="my-5">
                <Alert variant="danger">
                    <Alert.Heading>Error loading cart</Alert.Heading>
                    <p>{typeof error === 'string' ? error : 'Failed to load cart'}</p>
                    <Button onClick={() => dispatch(fetchCart())} variant="outline-danger">
                        Try Again
                    </Button>
                </Alert>
            </Container>
        );
    }

    if (!items || items.length === 0) {
        return (
            <Container className="my-5 text-center">
                <FaShoppingCart size={60} className="text-muted mb-4" />
                <h3>Your cart is empty</h3>
                <p className="text-muted">Looks like you haven't added any items to your cart yet.</p>
                <Button as={Link} to="/products" variant="primary" size="lg" className="mt-3">
                    Start Shopping
                </Button>
            </Container>
        );
    }

    return (
        <Container className="my-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2>Shopping Cart ({totalItems} {totalItems === 1 ? 'item' : 'items'})</h2>
                <Button
                    variant="outline-danger"
                    onClick={handleClearCart}
                    className="d-flex align-items-center"
                    disabled={loading}
                >
                    <FaTrash className="me-2" /> Clear Cart
                </Button>
            </div>

            <Row>
                <Col lg={8}>
                    <Card className="shadow-sm">
                        <Card.Body>
                            <Table responsive className="align-middle">
                                <thead>
                                <tr>
                                    <th>Product</th>
                                    <th>Price</th>
                                    <th width="150">Quantity</th>
                                    <th>Total</th>
                                    <th></th>
                                </tr>
                                </thead>
                                <tbody>
                                {items.map((item) => (
                                    <tr key={item.id}>
                                        <td>
                                            <div className="d-flex align-items-center">
                                                <img
                                                    src={item.productImageUrl || 'https://via.placeholder.com/60x60'}
                                                    alt={item.productName || item.product?.name}
                                                    style={{
                                                        width: '60px',
                                                        height: '60px',
                                                        objectFit: 'cover',
                                                        borderRadius: '4px',
                                                        marginRight: '15px'
                                                    }}
                                                />
                                                <div>
                                                    <Link
                                                        to={`/product/${item.productId || item.product?.id}`}
                                                        className="text-decoration-none text-dark fw-bold"
                                                    >
                                                        {item.productName || item.product?.name}
                                                    </Link>
                                                    <div className="text-muted small">
                                                        SKU: {item.sku || item.product?.sku || 'N/A'}
                                                    </div>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="fw-bold">
                                            ${(item.price || item.product?.price)?.toFixed(2)}
                                        </td>
                                        <td>
                                            <div className="d-flex align-items-center">
                                                <Button
                                                    size="sm"
                                                    variant="outline-secondary"
                                                    onClick={() => handleQuantityChange(
                                                        item.id,
                                                        (quantities[item.id] || item.quantity) - 1,
                                                        item.stock || item.product?.stock || 99
                                                    )}
                                                    disabled={updatingItems[item.id] || (quantities[item.id] || item.quantity) <= 1}
                                                >
                                                    <FaMinus size={10} />
                                                </Button>
                                                <Form.Control
                                                    type="number"
                                                    min="1"
                                                    max={item.stock || item.product?.stock || 99}
                                                    value={quantities[item.id] || item.quantity}
                                                    onChange={(e) => handleQuantityChange(
                                                        item.id,
                                                        parseInt(e.target.value) || 1,
                                                        item.stock || item.product?.stock || 99
                                                    )}
                                                    disabled={updatingItems[item.id]}
                                                    className="mx-2 text-center"
                                                    style={{ width: '70px' }}
                                                />
                                                <Button
                                                    size="sm"
                                                    variant="outline-secondary"
                                                    onClick={() => handleQuantityChange(
                                                        item.id,
                                                        (quantities[item.id] || item.quantity) + 1,
                                                        item.stock || item.product?.stock || 99
                                                    )}
                                                    disabled={
                                                        updatingItems[item.id] ||
                                                        (quantities[item.id] || item.quantity) >=
                                                        (item.stock || item.product?.stock || 99)
                                                    }
                                                >
                                                    <FaPlus size={10} />
                                                </Button>
                                                {updatingItems[item.id] && (
                                                    <Spinner
                                                        animation="border"
                                                        size="sm"
                                                        variant="primary"
                                                        className="ms-2"
                                                    />
                                                )}
                                            </div>
                                            {(item.stock || item.product?.stock) && (
                                                <small className="text-muted">
                                                    {item.stock || item.product?.stock} available
                                                </small>
                                            )}
                                        </td>
                                        <td className="fw-bold text-primary">
                                            ${(
                                            (item.price || item.product?.price) *
                                            (quantities[item.id] || item.quantity)
                                        )?.toFixed(2)}
                                        </td>
                                        <td>
                                            <Button
                                                variant="link"
                                                className="text-danger p-0"
                                                onClick={() => handleRemoveItem(item.id)}
                                                disabled={updatingItems[item.id]}
                                            >
                                                <FaTrash size={18} />
                                            </Button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </Table>
                        </Card.Body>
                    </Card>
                </Col>

                <Col lg={4}>
                    <Card className="shadow-sm sticky-top" style={{ top: '20px' }}>
                        <Card.Body>
                            <Card.Title className="mb-4">Order Summary</Card.Title>

                            <div className="d-flex justify-content-between mb-3">
                                <span>Subtotal ({totalItems} items):</span>
                                <span className="fw-bold">${totalAmount.toFixed(2)}</span>
                            </div>

                            <div className="d-flex justify-content-between mb-3">
                                <span>Shipping:</span>
                                <span className="fw-bold">
                                    {shipping === 0 ? 'FREE' : `$${shipping.toFixed(2)}`}
                                </span>
                            </div>

                            <div className="d-flex justify-content-between mb-3">
                                <span>Tax (8%):</span>
                                <span className="fw-bold">${tax.toFixed(2)}</span>
                            </div>

                            <hr />

                            <div className="d-flex justify-content-between mb-4">
                                <strong>Total:</strong>
                                <h4 className="text-primary mb-0">${total.toFixed(2)}</h4>
                            </div>

                            {shipping === 0 && (
                                <Alert variant="success" className="text-center p-2 small">
                                    🎉 You've got FREE shipping!
                                </Alert>
                            )}

                            <div className="d-grid gap-2">
                                <Button
                                    variant="success"
                                    size="lg"
                                    onClick={() => navigate('/checkout')}
                                    className="d-flex align-items-center justify-content-center"
                                    disabled={loading || items.length === 0}
                                >
                                    Proceed to Checkout
                                </Button>

                                <Button
                                    variant="outline-primary"
                                    onClick={() => navigate('/products')}
                                    className="d-flex align-items-center justify-content-center"
                                >
                                    <FaArrowLeft className="me-2" /> Continue Shopping
                                </Button>
                            </div>

                            <div className="mt-4 text-center">
                                <small className="text-muted">
                                    We accept: Visa, Mastercard, PayPal
                                </small>
                            </div>
                        </Card.Body>
                    </Card>
                </Col>
            </Row>
        </Container>
    );
};

export default CartPage;