import React, { useState, useEffect } from 'react';
import {
    Search,
    Filter,
    Eye,
    Truck,
    CheckCircle,
    XCircle,
    Clock,
    Package,
    DollarSign,
    ChevronLeft,
    ChevronRight,
    RefreshCw,
    AlertCircle,
    Download,
    Printer,
    X
} from 'lucide-react';
import { adminApi } from '../../api/adminApi';
import { format } from 'date-fns';

const Orders = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedOrder, setSelectedOrder] = useState(null);
    const [showOrderModal, setShowOrderModal] = useState(false);
    const [showStatusModal, setShowStatusModal] = useState(false);
    const [selectedOrders, setSelectedOrders] = useState([]);
    const [bulkStatus, setBulkStatus] = useState('');
    const [searchTerm, setSearchTerm] = useState('');
    const [filters, setFilters] = useState({
        status: '',
        dateRange: 'all'
    });
    const [pagination, setPagination] = useState({
        page: 0,
        size: 20,
        totalPages: 0,
        totalElements: 0
    });
    const [stats, setStats] = useState(null);

    const statusColors = {
        PENDING: 'bg-yellow-100 text-yellow-800',
        PROCESSING: 'bg-blue-100 text-blue-800',
        SHIPPED: 'bg-purple-100 text-purple-800',
        DELIVERED: 'bg-green-100 text-green-800',
        CANCELLED: 'bg-red-100 text-red-800',
        REFUNDED: 'bg-gray-100 text-gray-800',
        ON_HOLD: 'bg-orange-100 text-orange-800'
    };

    const statusOptions = [
        'PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED', 'ON_HOLD'
    ];

    useEffect(() => {
        fetchOrders();
        fetchStats();
    }, [pagination.page, searchTerm, filters]);

    const fetchOrders = async () => {
        setLoading(true);
        try {
            const response = await adminApi.getAllOrders(
                pagination.page,
                pagination.size,
                filters.status,
                searchTerm
            );
            setOrders(response.data.content);
            setPagination({
                ...pagination,
                totalPages: response.data.totalPages,
                totalElements: response.data.totalElements
            });
        } catch (error) {
            console.error('Error fetching orders:', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchStats = async () => {
        try {
            const response = await adminApi.getOrderStats();
            setStats(response.data);
        } catch (error) {
            console.error('Error fetching order stats:', error);
        }
    };

    const handleViewOrder = async (id) => {
        try {
            const response = await adminApi.getOrderDetails(id);
            setSelectedOrder(response.data);
            setShowOrderModal(true);
        } catch (error) {
            console.error('Error fetching order details:', error);
        }
    };

    const handleUpdateStatus = async (id, newStatus) => {
        try {
            await adminApi.updateOrderStatus(id, { status: newStatus });
            fetchOrders();
            fetchStats();
            setShowStatusModal(false);
        } catch (error) {
            console.error('Error updating order status:', error);
        }
    };

    const handleBulkUpdate = async () => {
        if (!bulkStatus || selectedOrders.length === 0) return;

        try {
            await adminApi.bulkUpdateOrderStatus(selectedOrders, bulkStatus);
            fetchOrders();
            fetchStats();
            setSelectedOrders([]);
            setBulkStatus('');
        } catch (error) {
            console.error('Error bulk updating orders:', error);
        }
    };

    const handleProcessPayment = async (id) => {
        try {
            await adminApi.processPayment(id);
            fetchOrders();
        } catch (error) {
            console.error('Error processing payment:', error);
        }
    };

    const handleExport = async () => {
        try {
            const response = await adminApi.exportOrders(selectedOrders);
            const dataStr = JSON.stringify(response.data, null, 2);
            const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);
            const exportFileDefaultName = `orders_export_${new Date().toISOString()}.json`;

            const linkElement = document.createElement('a');
            linkElement.setAttribute('href', dataUri);
            linkElement.setAttribute('download', exportFileDefaultName);
            linkElement.click();
        } catch (error) {
            console.error('Error exporting orders:', error);
        }
    };

    const toggleOrderSelection = (orderId) => {
        setSelectedOrders(prev =>
            prev.includes(orderId)
                ? prev.filter(id => id !== orderId)
                : [...prev, orderId]
        );
    };

    const selectAllOrders = () => {
        if (selectedOrders.length === orders.length) {
            setSelectedOrders([]);
        } else {
            setSelectedOrders(orders.map(o => o.id));
        }
    };

    const StatCard = ({ title, value, icon: Icon, color }) => (
        <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-sm text-gray-600">{title}</p>
                    <p className="text-2xl font-semibold mt-1">{value}</p>
                </div>
                <div className={`p-3 rounded-full bg-${color}-100`}>
                    <Icon className={`text-${color}-600`} size={24} />
                </div>
            </div>
        </div>
    );

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-semibold text-gray-900">Order Management</h1>
                <div className="flex space-x-2">
                    {selectedOrders.length > 0 && (
                        <>
                            <select
                                value={bulkStatus}
                                onChange={(e) => setBulkStatus(e.target.value)}
                                className="px-3 py-2 border rounded-lg text-sm"
                            >
                                <option value="">Bulk Update Status</option>
                                {statusOptions.map(status => (
                                    <option key={status} value={status}>{status}</option>
                                ))}
                            </select>
                            <button
                                onClick={handleBulkUpdate}
                                disabled={!bulkStatus}
                                className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-purple-700 disabled:opacity-50"
                            >
                                Apply to {selectedOrders.length} orders
                            </button>
                            <button
                                onClick={handleExport}
                                className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700 flex items-center"
                            >
                                <Download size={16} className="mr-2" />
                                Export
                            </button>
                        </>
                    )}
                    <button
                        onClick={fetchOrders}
                        className="p-2 border rounded-lg hover:bg-gray-50"
                    >
                        <RefreshCw size={20} />
                    </button>
                </div>
            </div>

            {/* Stats Cards */}
            {stats && (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    <StatCard
                        title="Total Orders"
                        value={stats.totalOrders}
                        icon={Package}
                        color="blue"
                    />
                    <StatCard
                        title="Total Revenue"
                        value={`$${stats.totalRevenue?.toLocaleString()}`}
                        icon={DollarSign}
                        color="green"
                    />
                    <StatCard
                        title="Pending Orders"
                        value={stats.pendingOrders}
                        icon={Clock}
                        color="yellow"
                    />
                    <StatCard
                        title="Delivered"
                        value={stats.deliveredOrders}
                        icon={CheckCircle}
                        color="green"
                    />
                </div>
            )}

            {/* Search and Filters */}
            <div className="bg-white rounded-lg shadow p-4">
                <div className="flex flex-wrap gap-4">
                    <div className="flex-1 min-w-[200px]">
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                            <input
                                type="text"
                                placeholder="Search by order #, customer name, email..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                    </div>

                    <select
                        value={filters.status}
                        onChange={(e) => setFilters({ ...filters, status: e.target.value })}
                        className="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="">All Status</option>
                        {statusOptions.map(status => (
                            <option key={status} value={status}>{status}</option>
                        ))}
                    </select>

                    <select
                        value={filters.dateRange}
                        onChange={(e) => setFilters({ ...filters, dateRange: e.target.value })}
                        className="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="all">All Time</option>
                        <option value="today">Today</option>
                        <option value="week">This Week</option>
                        <option value="month">This Month</option>
                        <option value="year">This Year</option>
                    </select>

                    <button
                        onClick={() => setFilters({ status: '', dateRange: 'all' })}
                        className="px-4 py-2 text-gray-600 hover:text-gray-900"
                    >
                        Clear Filters
                    </button>
                </div>
            </div>

            {/* Orders Table */}
            <div className="bg-white rounded-lg shadow overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left">
                                <input
                                    type="checkbox"
                                    checked={selectedOrders.length === orders.length && orders.length > 0}
                                    onChange={selectAllOrders}
                                    className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                />
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Order #
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Customer
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Date
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Total
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Status
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Payment
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Actions
                            </th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                        {loading ? (
                            <tr>
                                <td colSpan="8" className="px-6 py-12 text-center">
                                    <div className="flex justify-center">
                                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                                    </div>
                                </td>
                            </tr>
                        ) : orders.length === 0 ? (
                            <tr>
                                <td colSpan="8" className="px-6 py-12 text-center text-gray-500">
                                    No orders found
                                </td>
                            </tr>
                        ) : (
                            orders.map((order) => (
                                <tr key={order.id} className="hover:bg-gray-50">
                                    <td className="px-6 py-4">
                                        <input
                                            type="checkbox"
                                            checked={selectedOrders.includes(order.id)}
                                            onChange={() => toggleOrderSelection(order.id)}
                                            className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                        />
                                    </td>
                                    <td className="px-6 py-4">
                      <span className="text-sm font-medium text-blue-600">
                        #{order.orderNumber}
                      </span>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div>
                                            <div className="text-sm font-medium text-gray-900">
                                                {order.userName}
                                            </div>
                                            <div className="text-sm text-gray-500">
                                                {order.userEmail}
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 text-sm text-gray-500">
                                        {order.createdAt ? format(new Date(order.createdAt), 'MMM dd, yyyy') : 'N/A'}
                                    </td>
                                    <td className="px-6 py-4">
                      <span className="text-sm font-medium text-gray-900">
                        ${order.totalAmount?.toFixed(2)}
                      </span>
                                    </td>
                                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 text-xs font-medium rounded-full ${statusColors[order.status] || 'bg-gray-100 text-gray-800'}`}>
                        {order.status}
                      </span>
                                    </td>
                                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                          order.paymentStatus === 'PAID' ? 'bg-green-100 text-green-800' :
                              order.paymentStatus === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                                  order.paymentStatus === 'FAILED' ? 'bg-red-100 text-red-800' :
                                      'bg-gray-100 text-gray-800'
                      }`}>
                        {order.paymentStatus}
                      </span>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center space-x-2">
                                            <button
                                                onClick={() => handleViewOrder(order.id)}
                                                className="p-1 text-blue-600 hover:text-blue-900"
                                                title="View Details"
                                            >
                                                <Eye size={18} />
                                            </button>
                                            <select
                                                value={order.status}
                                                onChange={(e) => handleUpdateStatus(order.id, e.target.value)}
                                                className="text-xs border rounded px-2 py-1"
                                            >
                                                {statusOptions.map(status => (
                                                    <option key={status} value={status}>{status}</option>
                                                ))}
                                            </select>
                                            {order.paymentStatus === 'PENDING' && (
                                                <button
                                                    onClick={() => handleProcessPayment(order.id)}
                                                    className="p-1 text-green-600 hover:text-green-900"
                                                    title="Process Payment"
                                                >
                                                    <DollarSign size={18} />
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>

                {/* Pagination */}
                <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
                    <div className="text-sm text-gray-700">
                        Showing {pagination.page * pagination.size + 1} to{' '}
                        {Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} of{' '}
                        {pagination.totalElements} orders
                    </div>
                    <div className="flex items-center space-x-2">
                        <button
                            onClick={() => setPagination({ ...pagination, page: pagination.page - 1 })}
                            disabled={pagination.page === 0}
                            className="p-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                        >
                            <ChevronLeft size={20} />
                        </button>
                        <span className="px-4 py-2 bg-gray-100 rounded-lg">
              Page {pagination.page + 1} of {pagination.totalPages}
            </span>
                        <button
                            onClick={() => setPagination({ ...pagination, page: pagination.page + 1 })}
                            disabled={pagination.page === pagination.totalPages - 1}
                            className="p-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                        >
                            <ChevronRight size={20} />
                        </button>
                    </div>
                </div>
            </div>

            {/* Order Details Modal */}
            {showOrderModal && selectedOrder && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
                        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                            <h2 className="text-xl font-semibold">Order #{selectedOrder.orderNumber}</h2>
                            <button
                                onClick={() => setShowOrderModal(false)}
                                className="p-1 hover:bg-gray-100 rounded"
                            >
                                <X size={20} />
                            </button>
                        </div>

                        <div className="p-6 space-y-6">
                            {/* Order Status */}
                            <div className="grid grid-cols-4 gap-4">
                                <div className="col-span-1">
                                    <p className="text-sm text-gray-500">Status</p>
                                    <span className={`mt-1 px-2 py-1 text-xs font-medium rounded-full ${statusColors[selectedOrder.status]}`}>
                    {selectedOrder.status}
                  </span>
                                </div>
                                <div className="col-span-1">
                                    <p className="text-sm text-gray-500">Payment Status</p>
                                    <span className={`mt-1 px-2 py-1 text-xs font-medium rounded-full ${
                                        selectedOrder.paymentStatus === 'PAID' ? 'bg-green-100 text-green-800' :
                                            selectedOrder.paymentStatus === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                                                'bg-gray-100 text-gray-800'
                                    }`}>
                    {selectedOrder.paymentStatus}
                  </span>
                                </div>
                                <div className="col-span-1">
                                    <p className="text-sm text-gray-500">Payment Method</p>
                                    <p className="text-sm font-medium mt-1">{selectedOrder.paymentMethod}</p>
                                </div>
                                <div className="col-span-1">
                                    <p className="text-sm text-gray-500">Order Date</p>
                                    <p className="text-sm font-medium mt-1">
                                        {format(new Date(selectedOrder.createdAt), 'MMM dd, yyyy hh:mm a')}
                                    </p>
                                </div>
                            </div>

                            {/* Customer Information */}
                            <div className="border-t pt-4">
                                <h3 className="text-lg font-semibold mb-3">Customer Information</h3>
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <p className="text-sm text-gray-500">Name</p>
                                        <p className="text-sm font-medium">{selectedOrder.userName}</p>
                                    </div>
                                    <div>
                                        <p className="text-sm text-gray-500">Email</p>
                                        <p className="text-sm font-medium">{selectedOrder.userEmail}</p>
                                    </div>
                                </div>
                            </div>

                            {/* Shipping & Billing Addresses */}
                            <div className="border-t pt-4">
                                <div className="grid grid-cols-2 gap-6">
                                    <div>
                                        <h4 className="font-semibold mb-2">Shipping Address</h4>
                                        <p className="text-sm text-gray-600">
                                            {selectedOrder.shippingAddress?.recipientName}<br />
                                            {selectedOrder.shippingAddress?.street}<br />
                                            {selectedOrder.shippingAddress?.city}, {selectedOrder.shippingAddress?.state} {selectedOrder.shippingAddress?.zipCode}<br />
                                            {selectedOrder.shippingAddress?.country}<br />
                                            Phone: {selectedOrder.shippingAddress?.phone}
                                        </p>
                                    </div>
                                    <div>
                                        <h4 className="font-semibold mb-2">Billing Address</h4>
                                        <p className="text-sm text-gray-600">
                                            {selectedOrder.billingAddress?.recipientName}<br />
                                            {selectedOrder.billingAddress?.street}<br />
                                            {selectedOrder.billingAddress?.city}, {selectedOrder.billingAddress?.state} {selectedOrder.billingAddress?.zipCode}<br />
                                            {selectedOrder.billingAddress?.country}<br />
                                            Phone: {selectedOrder.billingAddress?.phone}
                                        </p>
                                    </div>
                                </div>
                            </div>

                            {/* Order Items */}
                            <div className="border-t pt-4">
                                <h3 className="text-lg font-semibold mb-3">Order Items</h3>
                                <table className="w-full">
                                    <thead className="bg-gray-50">
                                    <tr>
                                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Product</th>
                                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">SKU</th>
                                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Price</th>
                                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Quantity</th>
                                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Subtotal</th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-200">
                                    {selectedOrder.orderItems?.map((item) => (
                                        <tr key={item.id}>
                                            <td className="px-4 py-3">
                                                <div className="flex items-center">
                                                    <img
                                                        src={item.productImage || 'https://via.placeholder.com/40'}
                                                        alt={item.productName}
                                                        className="w-10 h-10 rounded object-cover mr-3"
                                                    />
                                                    <span className="text-sm font-medium">{item.productName}</span>
                                                </div>
                                            </td>
                                            <td className="px-4 py-3 text-sm">{item.sku}</td>
                                            <td className="px-4 py-3 text-sm">${item.price?.toFixed(2)}</td>
                                            <td className="px-4 py-3 text-sm">{item.quantity}</td>
                                            <td className="px-4 py-3 text-sm font-medium">${item.subtotal?.toFixed(2)}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>

                            {/* Order Summary */}
                            <div className="border-t pt-4">
                                <div className="flex justify-end">
                                    <div className="w-64 space-y-2">
                                        <div className="flex justify-between">
                                            <span className="text-sm text-gray-600">Subtotal:</span>
                                            <span className="text-sm font-medium">${selectedOrder.subtotal?.toFixed(2)}</span>
                                        </div>
                                        <div className="flex justify-between">
                                            <span className="text-sm text-gray-600">Shipping:</span>
                                            <span className="text-sm font-medium">${selectedOrder.shippingAmount?.toFixed(2)}</span>
                                        </div>
                                        <div className="flex justify-between">
                                            <span className="text-sm text-gray-600">Tax:</span>
                                            <span className="text-sm font-medium">${selectedOrder.taxAmount?.toFixed(2)}</span>
                                        </div>
                                        {selectedOrder.discountAmount > 0 && (
                                            <div className="flex justify-between">
                                                <span className="text-sm text-gray-600">Discount:</span>
                                                <span className="text-sm font-medium text-green-600">
                          -${selectedOrder.discountAmount?.toFixed(2)}
                        </span>
                                            </div>
                                        )}
                                        <div className="flex justify-between pt-2 border-t">
                                            <span className="text-base font-semibold">Total:</span>
                                            <span className="text-base font-semibold">${selectedOrder.totalAmount?.toFixed(2)}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Notes */}
                            {selectedOrder.notes && (
                                <div className="border-t pt-4">
                                    <h4 className="font-semibold mb-2">Order Notes</h4>
                                    <p className="text-sm text-gray-600 bg-gray-50 p-3 rounded">
                                        {selectedOrder.notes}
                                    </p>
                                </div>
                            )}

                            {/* Tracking Info */}
                            {selectedOrder.trackingNumber && (
                                <div className="border-t pt-4">
                                    <h4 className="font-semibold mb-2">Tracking Information</h4>
                                    <div className="bg-blue-50 p-3 rounded">
                                        <p className="text-sm">
                                            <span className="font-medium">Carrier:</span> {selectedOrder.shippingMethod || 'Standard'}
                                        </p>
                                        <p className="text-sm">
                                            <span className="font-medium">Tracking #:</span> {selectedOrder.trackingNumber}
                                        </p>
                                        {selectedOrder.shippedAt && (
                                            <p className="text-sm">
                                                <span className="font-medium">Shipped on:</span>{' '}
                                                {format(new Date(selectedOrder.shippedAt), 'MMM dd, yyyy')}
                                            </p>
                                        )}
                                    </div>
                                </div>
                            )}
                        </div>

                        <div className="px-6 py-4 border-t border-gray-200 flex justify-end space-x-3">
                            <button
                                onClick={() => window.print()}
                                className="px-4 py-2 border rounded-lg hover:bg-gray-50 flex items-center"
                            >
                                <Printer size={16} className="mr-2" />
                                Print
                            </button>
                            <button
                                onClick={() => setShowOrderModal(false)}
                                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Orders;