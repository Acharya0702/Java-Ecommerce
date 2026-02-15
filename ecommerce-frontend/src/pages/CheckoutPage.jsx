import React, { useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { orderApi } from '../api/orderApi';
import { clearCart } from '../store/slices/cartSlice';
import {
    Container,
    Card,
    Form,
    Button,
    Alert,
    Row,
    Col,
    ListGroup,
    Spinner
} from 'react-bootstrap';

const CheckoutPage = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { items, total } = useSelector((state) => state.cart);
    const user = JSON.parse(localStorage.getItem('user') || '{}');

    const [formData, setFormData] = useState({
        shippingAddress: {
            street: '',
            city: '',
            state: '',
            zipCode: '',
            country: '',
            phone: '',
            recipientName: user.fullName || '',
        },
        useShippingForBilling: true,
        billingAddress: {
            street: '',
            city: '',
            state: '',
            zipCode: '',
            country: '',
            phone: '',
            recipientName: user.fullName || '',
        },
        notes: '',
        paymentMethod: 'CREDIT_CARD',
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [showBilling, setShowBilling] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;

        if (name.startsWith('shipping.')) {
            const field = name.split('.')[1];
            setFormData(prev => ({
                ...prev,
                shippingAddress: {
                    ...prev.shippingAddress,
                    [field]: value
                }
            }));
        } else if (name.startsWith('billing.')) {
            const field = name.split('.')[1];
            setFormData(prev => ({
                ...prev,
                billingAddress: {
                    ...prev.billingAddress,
                    [field]: value
                }
            }));
        } else {
            setFormData(prev => ({
                ...prev,
                [name]: value
            }));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            // Prepare order data
            const orderData = {
                shippingAddress: formData.shippingAddress,
                billingAddress: formData.useShippingForBilling ?
                    formData.shippingAddress : formData.billingAddress,
                notes: formData.notes,
                paymentMethod: formData.paymentMethod
            };

            console.log('Submitting order:', orderData);

            const response = await orderApi.createOrder(orderData);
            console.log('Order response:', response.data);

            // Clear cart
            dispatch(clearCart());

            // Navigate to order confirmation page
            navigate(`/orders/${response.data.id}`, {
                state: { order: response.data }
            });
        } catch (err) {
            console.error('Order error:', err);
            setError(err.response?.data?.error ||
                err.response?.data?.message ||
                'Failed to place order. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const calculateTotals = () => {
        const subtotal = items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
        const shipping = 5.99; // Fixed shipping for now
        const tax = subtotal * 0.1; // 10% tax
        const total = subtotal + shipping + tax;

        return { subtotal, shipping, tax, total };
    };

    const totals = calculateTotals();

    if (items.length === 0) {
        return (
            <Container className="py-5">
                <Alert variant="info">
                    Your cart is empty. Add some products before checkout.
                </Alert>
                <Button variant="primary" onClick={() => navigate('/products')}>
                    Continue Shopping
                </Button>
            </Container>
        );
    }

    return (
        <Container className="py-5">
            <h1 className="mb-4">Checkout</h1>

            {error && (
                <Alert variant="danger" className="mb-4">
                    {error}
                </Alert>
            )}

            <Row>
                <Col lg={8}>
                    <Card className="mb-4">
                        <Card.Header>
                            <h5 className="mb-0">Shipping Address</h5>
                        </Card.Header>
                        <Card.Body>
                            <Form onSubmit={handleSubmit}>
                                <Row>
                                    <Col md={6}>
                                        <Form.Group className="mb-3">
                                            <Form.Label>Recipient Name *</Form.Label>
                                            <Form.Control
                                                type="text"
                                                name="shipping.recipientName"
                                                value={formData.shippingAddress.recipientName}
                                                onChange={handleChange}
                                                required
                                            />
                                        </Form.Group>
                                    </Col>
                                    <Col md={6}>
                                        <Form.Group className="mb-3">
                                            <Form.Label>Phone *</Form.Label>
                                            <Form.Control
                                                type="tel"
                                                name="shipping.phone"
                                                value={formData.shippingAddress.phone}
                                                onChange={handleChange}
                                                required
                                            />
                                        </Form.Group>
                                    </Col>
                                </Row>

                                <Form.Group className="mb-3">
                                    <Form.Label>Street Address *</Form.Label>
                                    <Form.Control
                                        type="text"
                                        name="shipping.street"
                                        value={formData.shippingAddress.street}
                                        onChange={handleChange}
                                        required
                                    />
                                </Form.Group>

                                <Row>
                                    <Col md={6}>
                                        <Form.Group className="mb-3">
                                            <Form.Label>City *</Form.Label>
                                            <Form.Control
                                                type="text"
                                                name="shipping.city"
                                                value={formData.shippingAddress.city}
                                                onChange={handleChange}
                                                required
                                            />
                                        </Form.Group>
                                    </Col>
                                    <Col md={6}>
                                        <Form.Group className="mb-3">
                                            <Form.Label>State *</Form.Label>
                                            <Form.Control
                                                type="text"
                                                name="shipping.state"
                                                value={formData.shippingAddress.state}
                                                onChange={handleChange}
                                                required
                                            />
                                        </Form.Group>
                                    </Col>
                                </Row>

                                <Row>
                                    <Col md={6}>
                                        <Form.Group className="mb-3">
                                            <Form.Label>ZIP Code *</Form.Label>
                                            <Form.Control
                                                type="text"
                                                name="shipping.zipCode"
                                                value={formData.shippingAddress.zipCode}
                                                onChange={handleChange}
                                                required
                                            />
                                        </Form.Group>
                                    </Col>
                                    <Col md={6}>
                                        <Form.Group className="mb-3">
                                            <Form.Label>Country *</Form.Label>
                                            <Form.Control
                                                type="text"
                                                name="shipping.country"
                                                value={formData.shippingAddress.country}
                                                onChange={handleChange}
                                                required
                                            />
                                        </Form.Group>
                                    </Col>
                                </Row>

                                <Form.Group className="mb-3">
                                    <Form.Check
                                        type="checkbox"
                                        label="Use shipping address for billing"
                                        checked={formData.useShippingForBilling}
                                        onChange={(e) => setFormData(prev => ({
                                            ...prev,
                                            useShippingForBilling: e.target.checked
                                        }))}
                                    />
                                </Form.Group>

                                {!formData.useShippingForBilling && (
                                    <div className="mt-4">
                                        <h6>Billing Address</h6>
                                        <Row>
                                            <Col md={6}>
                                                <Form.Group className="mb-3">
                                                    <Form.Label>Recipient Name</Form.Label>
                                                    <Form.Control
                                                        type="text"
                                                        name="billing.recipientName"
                                                        value={formData.billingAddress.recipientName}
                                                        onChange={handleChange}
                                                    />
                                                </Form.Group>
                                            </Col>
                                            <Col md={6}>
                                                <Form.Group className="mb-3">
                                                    <Form.Label>Street Address</Form.Label>
                                                    <Form.Control
                                                        type="text"
                                                        name="billing.street"
                                                        value={formData.billingAddress.street}
                                                        onChange={handleChange}
                                                    />
                                                </Form.Group>
                                            </Col>
                                        </Row>
                                    </div>
                                )}

                                <Form.Group className="mb-3">
                                    <Form.Label>Payment Method</Form.Label>
                                    <Form.Select
                                        name="paymentMethod"
                                        value={formData.paymentMethod}
                                        onChange={handleChange}
                                    >
                                        <option value="CREDIT_CARD">Credit Card</option>
                                        <option value="DEBIT_CARD">Debit Card</option>
                                        <option value="PAYPAL">PayPal</option>
                                        <option value="CASH_ON_DELIVERY">Cash on Delivery</option>
                                    </Form.Select>
                                </Form.Group>

                                <Form.Group className="mb-4">
                                    <Form.Label>Order Notes (Optional)</Form.Label>
                                    <Form.Control
                                        as="textarea"
                                        rows={3}
                                        name="notes"
                                        value={formData.notes}
                                        onChange={handleChange}
                                        placeholder="Special instructions for your order..."
                                    />
                                </Form.Group>

                                <Button
                                    type="submit"
                                    variant="primary"
                                    size="lg"
                                    disabled={loading}
                                    className="w-100"
                                >
                                    {loading ? (
                                        <>
                                            <Spinner
                                                as="span"
                                                animation="border"
                                                size="sm"
                                                role="status"
                                                aria-hidden="true"
                                                className="me-2"
                                            />
                                            Processing Order...
                                        </>
                                    ) : (
                                        `Place Order - $${totals.total.toFixed(2)}`
                                    )}
                                </Button>
                            </Form>
                        </Card.Body>
                    </Card>
                </Col>

                <Col lg={4}>
                    <Card>
                        <Card.Header>
                            <h5 className="mb-0">Order Summary</h5>
                        </Card.Header>
                        <Card.Body>
                            <ListGroup variant="flush">
                                {items.map((item) => (
                                    <ListGroup.Item key={item.id} className="d-flex justify-content-between">
                                        <div>
                                            <span className="fw-bold">{item.name}</span>
                                            <br />
                                            <small className="text-muted">Qty: {item.quantity}</small>
                                        </div>
                                        <span>${(item.price * item.quantity).toFixed(2)}</span>
                                    </ListGroup.Item>
                                ))}

                                <ListGroup.Item className="d-flex justify-content-between">
                                    <span>Subtotal</span>
                                    <span>${totals.subtotal.toFixed(2)}</span>
                                </ListGroup.Item>

                                <ListGroup.Item className="d-flex justify-content-between">
                                    <span>Shipping</span>
                                    <span>${totals.shipping.toFixed(2)}</span>
                                </ListGroup.Item>

                                <ListGroup.Item className="d-flex justify-content-between">
                                    <span>Tax (10%)</span>
                                    <span>${totals.tax.toFixed(2)}</span>
                                </ListGroup.Item>

                                <ListGroup.Item className="d-flex justify-content-between fw-bold fs-5">
                                    <span>Total</span>
                                    <span>${totals.total.toFixed(2)}</span>
                                </ListGroup.Item>
                            </ListGroup>
                        </Card.Body>
                    </Card>

                    <div className="mt-3">
                        <Button
                            variant="outline-secondary"
                            onClick={() => navigate('/cart')}
                            className="w-100"
                        >
                            Return to Cart
                        </Button>
                    </div>
                </Col>
            </Row>
        </Container>
    );
};

export default CheckoutPage;