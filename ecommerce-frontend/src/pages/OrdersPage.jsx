// src/pages/OrdersPage.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { orderApi } from '../api/orderApi';
import {
    Container,
    Card,
    Table,
    Badge,
    Button,
    Spinner,
    Alert
} from 'react-bootstrap';
import { FaEye } from 'react-icons/fa';

const OrdersPage = () => {
    const navigate = useNavigate();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        fetchOrders();
    }, []);

    const fetchOrders = async () => {
        try {
            setLoading(true);
            const response = await orderApi.getUserOrders();
            setOrders(response.data);
        } catch (err) {
            console.error('Error fetching orders:', err);
            setError('Failed to load orders');
        } finally {
            setLoading(false);
        }
    };

    const getStatusBadge = (status) => {
        const variants = {
            'PENDING': 'warning',
            'PROCESSING': 'info',
            'CONFIRMED': 'primary',
            'SHIPPED': 'success',
            'DELIVERED': 'success',
            'CANCELLED': 'danger'
        };
        return <Badge bg={variants[status] || 'secondary'}>{status}</Badge>;
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    };

    if (loading) {
        return (
            <Container className="py-5 text-center">
                <Spinner animation="border" variant="primary" />
                <p className="mt-3">Loading orders...</p>
            </Container>
        );
    }

    return (
        <Container className="py-5">
            <h2 className="mb-4">My Orders</h2>

            {error && (
                <Alert variant="danger" className="mb-4">
                    {error}
                </Alert>
            )}

            {orders.length === 0 ? (
                <Card className="text-center p-5">
                    <Card.Body>
                        <h5>No orders yet</h5>
                        <p className="text-muted mb-4">
                            You haven't placed any orders yet.
                        </p>
                        <Button variant="primary" onClick={() => navigate('/products')}>
                            Start Shopping
                        </Button>
                    </Card.Body>
                </Card>
            ) : (
                <Card>
                    <Card.Body>
                        <Table responsive hover>
                            <thead>
                            <tr>
                                <th>Order #</th>
                                <th>Date</th>
                                <th>Status</th>
                                <th>Payment</th>
                                <th className="text-end">Total</th>
                                <th className="text-center">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            {orders.map((order) => (
                                <tr key={order.id}>
                                    <td>
                                        <strong>{order.orderNumber}</strong>
                                    </td>
                                    <td>{formatDate(order.createdAt)}</td>
                                    <td>{getStatusBadge(order.status)}</td>
                                    <td>
                                        <Badge bg="secondary">
                                            {order.paymentMethod?.replace('_', ' ')}
                                        </Badge>
                                    </td>
                                    <td className="text-end fw-bold">
                                        ${order.totalAmount?.toFixed(2)}
                                    </td>
                                    <td className="text-center">
                                        <Button
                                            variant="outline-primary"
                                            size="sm"
                                            onClick={() => navigate(`/orders/${order.id}`)}
                                        >
                                            <FaEye className="me-1" />
                                            View
                                        </Button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </Table>
                    </Card.Body>
                </Card>
            )}
        </Container>
    );
};

export default OrdersPage;