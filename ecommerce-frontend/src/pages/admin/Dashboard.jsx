import React, { useEffect, useState } from 'react';
import {
    ShoppingBag,
    Package,
    Users,
    DollarSign,
    TrendingUp,
    Clock,
    CheckCircle,
    XCircle,
    Truck,
    AlertCircle
} from 'lucide-react';
import {
    LineChart,
    Line,
    PieChart,
    Pie,
    Cell,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer
} from 'recharts';
import { adminApi } from '../../api/adminApi';

const Dashboard = () => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [salesData, setSalesData] = useState([]);

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        setLoading(true);
        try {
            const [statsResponse, salesResponse] = await Promise.all([
                adminApi.getDashboardStats(),
                adminApi.getSalesChartData(7) // Last 7 days
            ]);

            console.log('Stats response:', statsResponse.data);
            console.log('Sales response:', salesResponse.data);

            setStats(statsResponse.data);

            // Transform sales data for the chart
            const transformedSalesData = Object.entries(salesResponse.data || {}).map(([date, value]) => ({
                date,
                sales: value
            }));
            setSalesData(transformedSalesData);

        } catch (error) {
            console.error('Error fetching dashboard data:', error);
            if (error.response) {
                console.error('Error response:', error.response.data);
                console.error('Error status:', error.response.status);
            }
        } finally {
            setLoading(false);
        }
    };

    const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6'];

    const StatCard = ({ title, value, icon: Icon, color, trend }) => (
        <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-sm text-gray-600 mb-1">{title}</p>
                    <p className="text-2xl font-semibold">{value}</p>
                    {trend && (
                        <p className="text-sm text-green-600 mt-2">
                            <TrendingUp size={14} className="inline mr-1" />
                            {trend}% from last month
                        </p>
                    )}
                </div>
                <div className={`p-3 rounded-full bg-${color}-100`}>
                    <Icon className={`text-${color}-600`} size={24} />
                </div>
            </div>
        </div>
    );

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-semibold text-gray-900">Dashboard</h1>
                <div className="flex space-x-2">
                    <select
                        className="border rounded-md px-3 py-2 text-sm"
                        onChange={(e) => {
                            const days = parseInt(e.target.value);
                            fetchDashboardData(days);
                        }}
                    >
                        <option value="7">Last 7 days</option>
                        <option value="30">Last 30 days</option>
                        <option value="90">Last 3 months</option>
                        <option value="365">This year</option>
                    </select>
                    <button className="bg-blue-600 text-white px-4 py-2 rounded-md text-sm hover:bg-blue-700">
                        Download Report
                    </button>
                </div>
            </div>

            {/* Stats Grid */}
            {stats && (
                <>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                        <StatCard
                            title="Total Revenue"
                            value={`$${stats.totalRevenue?.toLocaleString() || '0'}`}
                            icon={DollarSign}
                            color="green"
                            trend="+12.5"
                        />
                        <StatCard
                            title="Total Orders"
                            value={stats.totalOrders?.toLocaleString() || '0'}
                            icon={ShoppingBag}
                            color="blue"
                            trend="+8.2"
                        />
                        <StatCard
                            title="Total Customers"
                            value={stats.totalCustomers?.toLocaleString() || '0'}
                            icon={Users}
                            color="purple"
                            trend="+15.3"
                        />
                        <StatCard
                            title="Total Products"
                            value={stats.totalProducts?.toLocaleString() || '0'}
                            icon={Package}
                            color="orange"
                            trend="+5.7"
                        />
                    </div>

                    {/* Order Status Cards */}
                    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
                        <div className="bg-yellow-50 rounded-lg p-4 border border-yellow-200">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm text-yellow-800">Pending</p>
                                    <p className="text-xl font-semibold text-yellow-900">{stats.pendingOrders || 0}</p>
                                </div>
                                <Clock className="text-yellow-600" size={24} />
                            </div>
                        </div>
                        <div className="bg-blue-50 rounded-lg p-4 border border-blue-200">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm text-blue-800">Processing</p>
                                    <p className="text-xl font-semibold text-blue-900">{stats.processingOrders || 0}</p>
                                </div>
                                <Package className="text-blue-600" size={24} />
                            </div>
                        </div>
                        <div className="bg-purple-50 rounded-lg p-4 border border-purple-200">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm text-purple-800">Shipped</p>
                                    <p className="text-xl font-semibold text-purple-900">{stats.shippedOrders || 0}</p>
                                </div>
                                <Truck className="text-purple-600" size={24} />
                            </div>
                        </div>
                        <div className="bg-green-50 rounded-lg p-4 border border-green-200">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm text-green-800">Delivered</p>
                                    <p className="text-xl font-semibold text-green-900">{stats.deliveredOrders || 0}</p>
                                </div>
                                <CheckCircle className="text-green-600" size={24} />
                            </div>
                        </div>
                        <div className="bg-red-50 rounded-lg p-4 border border-red-200">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm text-red-800">Cancelled</p>
                                    <p className="text-xl font-semibold text-red-900">{stats.cancelledOrders || 0}</p>
                                </div>
                                <XCircle className="text-red-600" size={24} />
                            </div>
                        </div>
                    </div>

                    {/* Charts Section */}
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        {/* Sales Chart */}
                        <div className="lg:col-span-2 bg-white rounded-lg shadow p-6">
                            <h2 className="text-lg font-semibold mb-4">Sales Overview</h2>
                            <ResponsiveContainer width="100%" height={300}>
                                <LineChart data={salesData}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="date" />
                                    <YAxis />
                                    <Tooltip />
                                    <Legend />
                                    <Line type="monotone" dataKey="sales" stroke="#3B82F6" strokeWidth={2} />
                                </LineChart>
                            </ResponsiveContainer>
                        </div>

                        {/* Orders by Status Pie Chart */}
                        <div className="bg-white rounded-lg shadow p-6">
                            <h2 className="text-lg font-semibold mb-4">Orders by Status</h2>
                            <ResponsiveContainer width="100%" height={300}>
                                <PieChart>
                                    <Pie
                                        data={[
                                            { name: 'Pending', value: stats.pendingOrders || 0 },
                                            { name: 'Processing', value: stats.processingOrders || 0 },
                                            { name: 'Shipped', value: stats.shippedOrders || 0 },
                                            { name: 'Delivered', value: stats.deliveredOrders || 0 },
                                            { name: 'Cancelled', value: stats.cancelledOrders || 0 },
                                        ]}
                                        cx="50%"
                                        cy="50%"
                                        labelLine={false}
                                        label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                                        outerRadius={80}
                                        fill="#8884d8"
                                        dataKey="value"
                                    >
                                        {stats.ordersByStatus?.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                        ))}
                                    </Pie>
                                    <Tooltip />
                                </PieChart>
                            </ResponsiveContainer>
                        </div>
                    </div>

                    {/* Recent Orders */}
                    <div className="bg-white rounded-lg shadow">
                        <div className="px-6 py-4 border-b border-gray-200">
                            <h2 className="text-lg font-semibold">Recent Orders</h2>
                        </div>
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Order #
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Customer
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Amount
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Status
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Date
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Actions
                                    </th>
                                </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-200">
                                {stats.recentOrders?.map((order) => (
                                    <tr key={order.id} className="hover:bg-gray-50">
                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-blue-600">
                                            #{order.orderNumber}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                            {order.customerName}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                            ${order.totalAmount?.toFixed(2)}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                                <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                                                    order.status === 'DELIVERED' ? 'bg-green-100 text-green-800' :
                                                        order.status === 'SHIPPED' ? 'bg-purple-100 text-purple-800' :
                                                            order.status === 'PROCESSING' ? 'bg-blue-100 text-blue-800' :
                                                                order.status === 'CANCELLED' ? 'bg-red-100 text-red-800' :
                                                                    'bg-yellow-100 text-yellow-800'
                                                }`}>
                                                    {order.status}
                                                </span>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {new Date(order.createdAt).toLocaleDateString()}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                                            <button className="text-blue-600 hover:text-blue-900">View</button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default Dashboard;