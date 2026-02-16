// src/pages/OrderPage.jsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { orderApi } from '../api/orderApi';
import {
    Container,
    Row,
    Col,
    Card,
    Button,
    Badge,
    ListGroup,
    Spinner,
    Alert,
    Table
} from 'react-bootstrap';
import {
    FaCheckCircle,
    FaTruck,
    FaBoxOpen,
    FaExclamationTriangle,
    FaPrint,
    FaArrowLeft,
    FaDownload
} from 'react-icons/fa';

const OrderPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const { user } = useSelector((state) => state.auth);

    const [order, setOrder] = useState(location.state?.order || null);
    const [loading, setLoading] = useState(!order);
    const [error, setError] = useState('');
    const [cancelling, setCancelling] = useState(false);

    useEffect(() => {
        if (!order) {
            fetchOrder();
        }
    }, [id]);

    const fetchOrder = async () => {
        try {
            setLoading(true);
            const response = await orderApi.getOrderById(id);
            setOrder(response.data);
            setError('');
        } catch (err) {
            console.error('Error fetching order:', err);
            setError(err.response?.data?.error || 'Failed to load order details');
        } finally {
            setLoading(false);
        }
    };

    const handleCancelOrder = async () => {
        if (!window.confirm('Are you sure you want to cancel this order?')) {
            return;
        }

        try {
            setCancelling(true);
            const response = await orderApi.cancelOrder(id);
            setOrder(response.data);
        } catch (err) {
            console.error('Error cancelling order:', err);
            alert(err.response?.data?.error || 'Failed to cancel order');
        } finally {
            setCancelling(false);
        }
    };

    const getStatusBadge = (status) => {
        const statusConfig = {
            'PENDING': { variant: 'warning', text: 'Pending' },
            'PROCESSING': { variant: 'info', text: 'Processing' },
            'CONFIRMED': { variant: 'primary', text: 'Confirmed' },
            'SHIPPED': { variant: 'success', text: 'Shipped' },
            'DELIVERED': { variant: 'success', text: 'Delivered' },
            'CANCELLED': { variant: 'danger', text: 'Cancelled' },
            'REFUNDED': { variant: 'secondary', text: 'Refunded' },
            'ON_HOLD': { variant: 'dark', text: 'On Hold' }
        };

        const config = statusConfig[status] || { variant: 'secondary', text: status };
        return <Badge bg={config.variant}>{config.text}</Badge>;
    };

    const getPaymentStatusBadge = (status) => {
        const statusConfig = {
            'PENDING': { variant: 'warning', text: 'Pending' },
            'AUTHORIZED': { variant: 'info', text: 'Authorized' },
            'PAID': { variant: 'success', text: 'Paid' },
            'FAILED': { variant: 'danger', text: 'Failed' },
            'REFUNDED': { variant: 'secondary', text: 'Refunded' },
            'PARTIALLY_REFUNDED': { variant: 'secondary', text: 'Partially Refunded' }
        };

        const config = statusConfig[status] || { variant: 'secondary', text: status };
        return <Badge bg={config.variant}>{config.text}</Badge>;
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';
        return new Date(dateString).toLocaleString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const handlePrint = () => {
        window.print();
    };

    if (loading) {
        return (
            <Container className="py-5 text-center">
                <Spinner animation="border" variant="primary" />
                <p className="mt-3">Loading order details...</p>
            </Container>
        );
    }

    if (error) {
        return (
            <Container className="py-5">
                <Alert variant="danger">
                    <Alert.Heading>Error Loading Order</Alert.Heading>
                    <p>{error}</p>
                    <hr />
                    <div className="d-flex justify-content-center">
                        <Button variant="outline-primary" onClick={() => navigate('/orders')}>
                            View All Orders
                        </Button>
                    </div>
                </Alert>
            </Container>
        );
    }

    if (!order) {
        return (
            <Container className="py-5">
                <Alert variant="warning">
                    <Alert.Heading>Order Not Found</Alert.Heading>
                    <p>The order you're looking for doesn't exist.</p>
                    <hr />
                    <div className="d-flex justify-content-center">
                        <Button variant="outline-primary" onClick={() => navigate('/orders')}>
                            View All Orders
                        </Button>
                    </div>
                </Alert>
            </Container>
        );
    }

    const canCancel = ['PENDING', 'PROCESSING', 'CONFIRMED'].includes(order.status);

    return (
        <Container className="py-5">
            {/* Header with Order Number */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h2 className="mb-1">Order #{order.orderNumber}</h2>
                    <p className="text-muted mb-0">
                        Placed on {formatDate(order.createdAt)}
                    </p>
                </div>
                <div className="d-flex gap-2">
                    <Button
                        variant="outline-secondary"
                        onClick={() => navigate('/orders')}
                    >
                        <FaArrowLeft className="me-2" />
                        Back to Orders
                    </Button>
                    <Button variant="outline-primary" onClick={handlePrint}>
                        <FaPrint className="me-2" />
                        Print
                    </Button>
                </div>
            </div>

            {/* Status Cards */}
            <Row className="mb-4">
                <Col md={4}>
                    <Card className="text-center h-100">
                        <Card.Body>
                            <h5>Order Status</h5>
                            <div className="my-3">
                                {getStatusBadge(order.status)}
                            </div>
                            {order.shippedAt && (
                                <small className="text-muted">
                                    Shipped: {formatDate(order.shippedAt)}
                                </small>
                            )}
                        </Card.Body>
                    </Card>
                </Col>
                <Col md={4}>
                    <Card className="text-center h-100">
                        <Card.Body>
                            <h5>Payment Status</h5>
                            <div className="my-3">
                                {getPaymentStatusBadge(order.paymentStatus)}
                            </div>
                            <small className="text-muted">
                                Method: {order.paymentMethod?.replace('_', ' ')}
                            </small>
                        </Card.Body>
                    </Card>
                </Col>
                <Col md={4}>
                    <Card className="text-center h-100">
                        <Card.Body>
                            <h5>Tracking</h5>
                            {order.trackingNumber ? (
                                <>
                                    <div className="my-2">
                                        <FaTruck className="text-success" size={24} />
                                    </div>
                                    <p className="mb-0">
                                        <strong>#{order.trackingNumber}</strong>
                                    </p>
                                    <small className="text-muted">
                                        {order.shippingMethod || 'Standard Shipping'}
                                    </small>
                                </>
                            ) : (
                                <>
                                    <div className="my-3">
                                        <FaBoxOpen className="text-muted" size={24} />
                                    </div>
                                    <p className="text-muted mb-0">Not shipped yet</p>
                                </>
                            )}
                        </Card.Body>
                    </Card>
                </Col>
            </Row>

            {/* Main Content */}
            <Row>
                <Col lg={8}>
                    {/* Order Items */}
                    <Card className="mb-4">
                        <Card.Header>
                            <h5 className="mb-0">Order Items</h5>
                        </Card.Header>
                        <Card.Body>
                            <Table responsive className="mb-0">
                                <thead>
                                <tr>
                                    <th>Product</th>
                                    <th className="text-center">Quantity</th>
                                    <th className="text-end">Price</th>
                                    <th className="text-end">Subtotal</th>
                                </tr>
                                </thead>
                                <tbody>
                                {order.orderItems?.map((item) => (
                                    <tr key={item.id}>
                                        <td>
                                            <div className="d-flex align-items-center">
                                                {item.productImage && (
                                                    <img
                                                        src={item.productImage}
                                                        alt={item.productName}
                                                        style={{
                                                            width: '50px',
                                                            height: '50px',
                                                            objectFit: 'cover',
                                                            borderRadius: '4px',
                                                            marginRight: '10px'
                                                        }}
                                                    />
                                                )}
                                                <div>
                                                    <div className="fw-bold">
                                                        {item.productName}
                                                    </div>
                                                    <small className="text-muted">
                                                        SKU: {item.sku}
                                                    </small>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="text-center align-middle">
                                            {item.quantity}
                                        </td>
                                        <td className="text-end align-middle">
                                            ${item.price?.toFixed(2)}
                                        </td>
                                        <td className="text-end align-middle fw-bold">
                                            ${item.subtotal?.toFixed(2)}
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </Table>
                        </Card.Body>
                    </Card>

                    {/* Addresses */}
                    <Row>
                        <Col md={6}>
                            <Card className="mb-4">
                                <Card.Header>
                                    <h6 className="mb-0">Shipping Address</h6>
                                </Card.Header>
                                <Card.Body>
                                    {order.shippingAddress ? (
                                        <>
                                            <p className="mb-1 fw-bold">
                                                {order.shippingAddress.recipientName}
                                            </p>
                                            <p className="mb-1">{order.shippingAddress.street}</p>
                                            <p className="mb-1">
                                                {order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.zipCode}
                                            </p>
                                            <p className="mb-1">{order.shippingAddress.country}</p>
                                            <p className="mb-0">Phone: {order.shippingAddress.phone}</p>
                                        </>
                                    ) : (
                                        <p className="text-muted mb-0">No shipping address</p>
                                    )}
                                </Card.Body>
                            </Card>
                        </Col>
                        <Col md={6}>
                            <Card className="mb-4">
                                <Card.Header>
                                    <h6 className="mb-0">Billing Address</h6>
                                </Card.Header>
                                <Card.Body>
                                    {order.billingAddress ? (
                                        <>
                                            <p className="mb-1 fw-bold">
                                                {order.billingAddress.recipientName}
                                            </p>
                                            <p className="mb-1">{order.billingAddress.street}</p>
                                            <p className="mb-1">
                                                {order.billingAddress.city}, {order.billingAddress.state} {order.billingAddress.zipCode}
                                            </p>
                                            <p className="mb-1">{order.billingAddress.country}</p>
                                            <p className="mb-0">Phone: {order.billingAddress.phone}</p>
                                        </>
                                    ) : (
                                        <p className="text-muted mb-0">Same as shipping</p>
                                    )}
                                </Card.Body>
                            </Card>
                        </Col>
                    </Row>

                    {/* Order Notes */}
                    {order.notes && (
                        <Card className="mb-4">
                            <Card.Header>
                                <h6 className="mb-0">Order Notes</h6>
                            </Card.Header>
                            <Card.Body>
                                <p className="mb-0">{order.notes}</p>
                            </Card.Body>
                        </Card>
                    )}
                </Col>

                <Col lg={4}>
                    {/* Order Summary */}
                    <Card className="mb-4">
                        <Card.Header>
                            <h5 className="mb-0">Order Summary</h5>
                        </Card.Header>
                        <Card.Body>
                            <ListGroup variant="flush">
                                <ListGroup.Item className="d-flex justify-content-between">
                                    <span>Subtotal:</span>
                                    <span className="fw-bold">${order.subtotal?.toFixed(2)}</span>
                                </ListGroup.Item>
                                <ListGroup.Item className="d-flex justify-content-between">
                                    <span>Shipping:</span>
                                    <span>${order.shippingAmount?.toFixed(2)}</span>
                                </ListGroup.Item>
                                <ListGroup.Item className="d-flex justify-content-between">
                                    <span>Tax:</span>
                                    <span>${order.taxAmount?.toFixed(2)}</span>
                                </ListGroup.Item>
                                {order.discountAmount > 0 && (
                                    <ListGroup.Item className="d-flex justify-content-between text-success">
                                        <span>Discount:</span>
                                        <span>-${order.discountAmount?.toFixed(2)}</span>
                                    </ListGroup.Item>
                                )}
                                <ListGroup.Item className="d-flex justify-content-between fw-bold fs-5">
                                    <span>Total:</span>
                                    <span>${order.totalAmount?.toFixed(2)}</span>
                                </ListGroup.Item>
                            </ListGroup>
                        </Card.Body>
                    </Card>

                    {/* Actions */}
                    {canCancel && (
                        <Card className="mb-4 border-danger">
                            <Card.Body>
                                <h6 className="text-danger mb-3">Need to cancel?</h6>
                                <p className="small text-muted mb-3">
                                    You can cancel this order if it hasn't been shipped yet.
                                </p>
                                <Button
                                    variant="outline-danger"
                                    size="sm"
                                    onClick={handleCancelOrder}
                                    disabled={cancelling}
                                    className="w-100"
                                >
                                    {cancelling ? (
                                        <>
                                            <Spinner
                                                as="span"
                                                animation="border"
                                                size="sm"
                                                className="me-2"
                                            />
                                            Cancelling...
                                        </>
                                    ) : (
                                        'Cancel Order'
                                    )}
                                </Button>
                            </Card.Body>
                        </Card>
                    )}

                    {/* Need Help */}
                    <Card className="bg-light">
                        <Card.Body>
                            <h6>Need Help?</h6>
                            <p className="small text-muted mb-2">
                                If you have any questions about your order, please contact our support team.
                            </p>
                            <Button
                                variant="outline-primary"
                                size="sm"
                                href="mailto:support@ecommerce.com"
                                className="w-100"
                            >
                                Contact Support
                            </Button>
                        </Card.Body>
                    </Card>
                </Col>
            </Row>

            {/* Print Styles */}
            <style type="text/css" media="print">
                {`
                    @media print {
                        .btn, .navbar, footer, .no-print {
                            display: none !important;
                        }
                        .card {
                            border: 1px solid #ddd !important;
                            break-inside: avoid;
                        }
                        body {
                            padding: 20px;
                            font-size: 12pt;
                        }
                    }
                `}
            </style>
        </Container>
    );
};

export default OrderPage;