// OrderDetailPage.jsx
import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../api/api';

function OrderDetailPage() {
    const { id } = useParams();
    const [order, setOrder] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchOrder();
    }, [id]);

    const fetchOrder = async () => {
        try {
            const response = await api.get(`/orders/${id}`);
            setOrder(response.data);
        } catch (error) {
            console.error('Error fetching order:', error);
        } finally {
            setLoading(false);
        }
    };

    const formatCurrency = (amount) => {
        return `$${parseFloat(amount).toFixed(2)}`;
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleDateString();
    };

    if (loading) return <div>Loading order details...</div>;
    if (!order) return <div>Order not found</div>;

    return (
        <div className="container mx-auto px-4 py-8 max-w-4xl">
            <div className="mb-6">
                <Link to="/orders" className="text-blue-600 hover:underline">
                    ← Back to Orders
                </Link>
                <h1 className="text-2xl font-bold mt-2">Order #{order.orderNumber}</h1>
                <p className="text-gray-600">
                    Placed on {formatDate(order.createdAt)}
                </p>
                <div className="mt-2">
          <span className={`inline-block px-3 py-1 text-sm rounded-full ${
              order.status === 'DELIVERED' ? 'bg-green-100 text-green-800' :
                  order.status === 'SHIPPED' ? 'bg-blue-100 text-blue-800' :
                      order.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                          'bg-gray-100 text-gray-800'
          }`}>
            {order.status}
          </span>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                {/* Order Items */}
                <div className="bg-white rounded-lg shadow p-6">
                    <h2 className="text-xl font-semibold mb-4">Order Items</h2>
                    <div className="space-y-4">
                        {order.orderItems.map((item) => (
                            <div key={item.id} className="flex items-center border-b pb-4">
                                <img
                                    src={item.productImage}
                                    alt={item.productName}
                                    className="w-16 h-16 object-cover rounded"
                                />
                                <div className="ml-4 flex-1">
                                    <h3 className="font-medium">{item.productName}</h3>
                                    <p className="text-sm text-gray-600">SKU: {item.sku}</p>
                                    <p className="text-sm">Qty: {item.quantity}</p>
                                </div>
                                <div className="text-right">
                                    <p className="font-medium">{formatCurrency(item.price)}</p>
                                    <p className="text-sm text-gray-600">Total: {formatCurrency(item.subtotal)}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Order Summary */}
                <div className="space-y-6">
                    {/* Shipping Address */}
                    <div className="bg-white rounded-lg shadow p-6">
                        <h2 className="text-xl font-semibold mb-4">Shipping Address</h2>
                        {order.shippingAddress ? (
                            <div className="text-gray-700">
                                <p>{order.shippingAddress.recipientName}</p>
                                <p>{order.shippingAddress.street}</p>
                                <p>{order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.zipCode}</p>
                                <p>{order.shippingAddress.country}</p>
                                <p className="mt-2">Phone: {order.shippingAddress.phone}</p>
                            </div>
                        ) : (
                            <p className="text-gray-500">No shipping address provided</p>
                        )}
                    </div>

                    {/* Payment Summary */}
                    <div className="bg-white rounded-lg shadow p-6">
                        <h2 className="text-xl font-semibold mb-4">Payment Summary</h2>
                        <div className="space-y-2">
                            <div className="flex justify-between">
                                <span>Subtotal</span>
                                <span>{formatCurrency(order.subtotal)}</span>
                            </div>
                            <div className="flex justify-between">
                                <span>Shipping</span>
                                <span>{formatCurrency(order.shippingAmount)}</span>
                            </div>
                            <div className="flex justify-between">
                                <span>Tax</span>
                                <span>{formatCurrency(order.taxAmount)}</span>
                            </div>
                            <div className="flex justify-between font-bold text-lg border-t pt-2 mt-2">
                                <span>Total</span>
                                <span>{formatCurrency(order.totalAmount)}</span>
                            </div>
                        </div>
                        <div className="mt-4 pt-4 border-t">
                            <p><span className="font-medium">Payment Method:</span> {order.paymentMethod}</p>
                            <p><span className="font-medium">Payment Status:</span> {order.paymentStatus}</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default OrderDetailPage;