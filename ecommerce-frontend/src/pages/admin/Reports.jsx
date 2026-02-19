import React, { useState, useEffect } from 'react';
import {
    Download,
    Calendar,
    TrendingUp,
    DollarSign,
    Package,
    Users,
    ShoppingBag,
    BarChart3,
    PieChart,
    LineChart,
    Filter
} from 'lucide-react';
import {
    LineChart as RechartsLineChart,
    Line,
    BarChart,
    Bar,
    PieChart as RechartsPieChart,
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
import { format, subDays, subMonths, subYears } from 'date-fns';

const Reports = () => {
    const [dateRange, setDateRange] = useState({
        start: format(subMonths(new Date(), 1), 'yyyy-MM-dd'),
        end: format(new Date(), 'yyyy-MM-dd')
    });
    const [interval, setInterval] = useState('daily');
    const [salesReport, setSalesReport] = useState(null);
    const [topProducts, setTopProducts] = useState([]);
    const [inventoryReport, setInventoryReport] = useState(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('sales');

    const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899'];

    useEffect(() => {
        fetchReports();
    }, [dateRange, interval]);

    const fetchReports = async () => {
        setLoading(true);
        try {
            const [sales, products, inventory] = await Promise.all([
                adminApi.getSalesReport(dateRange.start, dateRange.end, interval),
                adminApi.getTopProducts(10),
                adminApi.getInventoryReport()
            ]);
            setSalesReport(sales.data);
            setTopProducts(products.data);
            setInventoryReport(inventory.data);
        } catch (error) {
            console.error('Error fetching reports:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleExportCSV = () => {
        // Convert report data to CSV
        const csvContent = generateCSV();
        const blob = new Blob([csvContent], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `report_${activeTab}_${dateRange.start}_${dateRange.end}.csv`;
        a.click();
    };

    const generateCSV = () => {
        if (activeTab === 'sales' && salesReport?.groupedData) {
            const headers = ['Date', 'Orders', 'Revenue'];
            const rows = Object.entries(salesReport.groupedData).map(([date, data]) => [
                date,
                data.count,
                data.revenue
            ]);
            return [headers, ...rows].map(row => row.join(',')).join('\n');
        }
        return '';
    };

    const quickDateRanges = [
        { label: 'Last 7 days', days: 7 },
        { label: 'Last 30 days', days: 30 },
        { label: 'Last 90 days', days: 90 },
        { label: 'Last year', days: 365 }
    ];

    const handleQuickRange = (days) => {
        setDateRange({
            start: format(subDays(new Date(), days), 'yyyy-MM-dd'),
            end: format(new Date(), 'yyyy-MM-dd')
        });
    };

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-semibold text-gray-900">Reports & Analytics</h1>
                <button
                    onClick={handleExportCSV}
                    className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700 flex items-center"
                >
                    <Download size={16} className="mr-2" />
                    Export CSV
                </button>
            </div>

            {/* Date Range Selector */}
            <div className="bg-white rounded-lg shadow p-4">
                <div className="flex flex-wrap items-center gap-4">
                    <div className="flex items-center space-x-2">
                        <Calendar size={20} className="text-gray-400" />
                        <input
                            type="date"
                            value={dateRange.start}
                            onChange={(e) => setDateRange({ ...dateRange, start: e.target.value })}
                            className="px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                        <span>to</span>
                        <input
                            type="date"
                            value={dateRange.end}
                            onChange={(e) => setDateRange({ ...dateRange, end: e.target.value })}
                            className="px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>

                    <select
                        value={interval}
                        onChange={(e) => setInterval(e.target.value)}
                        className="px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="daily">Daily</option>
                        <option value="weekly">Weekly</option>
                        <option value="monthly">Monthly</option>
                    </select>

                    <div className="flex space-x-2">
                        {quickDateRanges.map(range => (
                            <button
                                key={range.label}
                                onClick={() => handleQuickRange(range.days)}
                                className="px-3 py-1 border rounded-lg text-sm hover:bg-gray-50"
                            >
                                {range.label}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            {/* Report Tabs */}
            <div className="border-b border-gray-200">
                <nav className="flex space-x-8">
                    <button
                        onClick={() => setActiveTab('sales')}
                        className={`py-4 px-1 border-b-2 font-medium text-sm ${
                            activeTab === 'sales'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                        }`}
                    >
                        <DollarSign size={16} className="inline mr-2" />
                        Sales Report
                    </button>
                    <button
                        onClick={() => setActiveTab('products')}
                        className={`py-4 px-1 border-b-2 font-medium text-sm ${
                            activeTab === 'products'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                        }`}
                    >
                        <Package size={16} className="inline mr-2" />
                        Top Products
                    </button>
                    <button
                        onClick={() => setActiveTab('inventory')}
                        className={`py-4 px-1 border-b-2 font-medium text-sm ${
                            activeTab === 'inventory'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                        }`}
                    >
                        <ShoppingBag size={16} className="inline mr-2" />
                        Inventory Report
                    </button>
                </nav>
            </div>

            {loading ? (
                <div className="flex justify-center py-12">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
                </div>
            ) : (
                <>
                    {/* Sales Report Tab */}
                    {activeTab === 'sales' && salesReport && (
                        <div className="space-y-6">
                            {/* Summary Cards */}
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                <div className="bg-white rounded-lg shadow p-6">
                                    <p className="text-sm text-gray-600">Total Orders</p>
                                    <p className="text-3xl font-semibold mt-2">{salesReport.totalOrders}</p>
                                </div>
                                <div className="bg-white rounded-lg shadow p-6">
                                    <p className="text-sm text-gray-600">Total Revenue</p>
                                    <p className="text-3xl font-semibold mt-2">
                                        ${salesReport.totalRevenue?.toFixed(2)}
                                    </p>
                                </div>
                                <div className="bg-white rounded-lg shadow p-6">
                                    <p className="text-sm text-gray-600">Average Order Value</p>
                                    <p className="text-3xl font-semibold mt-2">
                                        ${salesReport.averageOrderValue?.toFixed(2)}
                                    </p>
                                </div>
                            </div>

                            {/* Sales Chart */}
                            <div className="bg-white rounded-lg shadow p-6">
                                <h3 className="text-lg font-semibold mb-4">Sales Trend</h3>
                                <ResponsiveContainer width="100%" height={400}>
                                    <RechartsLineChart data={Object.entries(salesReport.groupedData || {}).map(([date, data]) => ({
                                        date,
                                        orders: data.count,
                                        revenue: data.revenue
                                    }))}>
                                        <CartesianGrid strokeDasharray="3 3" />
                                        <XAxis dataKey="date" />
                                        <YAxis yAxisId="left" />
                                        <YAxis yAxisId="right" orientation="right" />
                                        <Tooltip />
                                        <Legend />
                                        <Line
                                            yAxisId="left"
                                            type="monotone"
                                            dataKey="orders"
                                            stroke="#3B82F6"
                                            name="Orders"
                                        />
                                        <Line
                                            yAxisId="right"
                                            type="monotone"
                                            dataKey="revenue"
                                            stroke="#10B981"
                                            name="Revenue"
                                        />
                                    </RechartsLineChart>
                                </ResponsiveContainer>
                            </div>

                            {/* Data Table */}
                            <div className="bg-white rounded-lg shadow overflow-hidden">
                                <div className="px-6 py-4 border-b border-gray-200">
                                    <h3 className="text-lg font-semibold">Detailed Data</h3>
                                </div>
                                <div className="overflow-x-auto">
                                    <table className="w-full">
                                        <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                Date
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                Orders
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                Revenue
                                            </th>
                                        </tr>
                                        </thead>
                                        <tbody className="divide-y divide-gray-200">
                                        {Object.entries(salesReport.groupedData || {}).map(([date, data]) => (
                                            <tr key={date} className="hover:bg-gray-50">
                                                <td className="px-6 py-4 text-sm">{date}</td>
                                                <td className="px-6 py-4 text-sm">{data.count}</td>
                                                <td className="px-6 py-4 text-sm">${data.revenue?.toFixed(2)}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Top Products Tab */}
                    {activeTab === 'products' && (
                        <div className="space-y-6">
                            <div className="bg-white rounded-lg shadow overflow-hidden">
                                <div className="px-6 py-4 border-b border-gray-200">
                                    <h3 className="text-lg font-semibold">Top 10 Best Selling Products</h3>
                                </div>
                                <div className="overflow-x-auto">
                                    <table className="w-full">
                                        <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                Product
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                Units Sold
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                Revenue
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                Stock
                                            </th>
                                        </tr>
                                        </thead>
                                        <tbody className="divide-y divide-gray-200">
                                        {topProducts.map((product, index) => (
                                            <tr key={product.id} className="hover:bg-gray-50">
                                                <td className="px-6 py-4">
                                                    <div className="flex items-center">
                                                        <span className="w-6 text-gray-500 mr-3">#{index + 1}</span>
                                                        <img
                                                            src={product.imageUrl || 'https://via.placeholder.com/40'}
                                                            alt={product.name}
                                                            className="w-10 h-10 rounded object-cover mr-3"
                                                        />
                                                        <div>
                                                            <div className="text-sm font-medium text-gray-900">
                                                                {product.name}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </td>
                                                <td className="px-6 py-4 text-sm font-medium">{product.totalSold}</td>
                                                <td className="px-6 py-4 text-sm">${product.revenue?.toFixed(2)}</td>
                                                <td className="px-6 py-4">
                            <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                                product.stockQuantity > 10 ? 'bg-green-100 text-green-800' :
                                    product.stockQuantity > 0 ? 'bg-yellow-100 text-yellow-800' :
                                        'bg-red-100 text-red-800'
                            }`}>
                              {product.stockQuantity} units
                            </span>
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Inventory Report Tab */}
                    {activeTab === 'inventory' && inventoryReport && (
                        <div className="space-y-6">
                            {/* Inventory Stats */}
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                <div className="bg-white rounded-lg shadow p-6">
                                    <p className="text-sm text-gray-600">Total Products</p>
                                    <p className="text-3xl font-semibold mt-2">{inventoryReport.totalProducts}</p>
                                </div>
                                <div className="bg-white rounded-lg shadow p-6">
                                    <p className="text-sm text-gray-600">Total Stock Value</p>
                                    <p className="text-3xl font-semibold mt-2">
                                        ${inventoryReport.totalStockValue?.toFixed(2)}
                                    </p>
                                </div>
                                <div className="bg-white rounded-lg shadow p-6">
                                    <p className="text-sm text-gray-600">Low Stock Items</p>
                                    <p className="text-3xl font-semibold mt-2 text-yellow-600">
                                        {inventoryReport.lowStockCount}
                                    </p>
                                </div>
                            </div>

                            {/* Stock Distribution Pie Chart */}
                            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                                <div className="bg-white rounded-lg shadow p-6">
                                    <h3 className="text-lg font-semibold mb-4">Stock Distribution</h3>
                                    <ResponsiveContainer width="100%" height={300}>
                                        <RechartsPieChart>
                                            <Pie
                                                data={[
                                                    { name: 'In Stock', value: inventoryReport.totalProducts - inventoryReport.outOfStockCount },
                                                    { name: 'Out of Stock', value: inventoryReport.outOfStockCount },
                                                    { name: 'Low Stock', value: inventoryReport.lowStockCount }
                                                ]}
                                                cx="50%"
                                                cy="50%"
                                                labelLine={false}
                                                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                                                outerRadius={80}
                                                fill="#8884d8"
                                                dataKey="value"
                                            >
                                                {COLORS.map((color, index) => (
                                                    <Cell key={`cell-${index}`} fill={color} />
                                                ))}
                                            </Pie>
                                            <Tooltip />
                                        </RechartsPieChart>
                                    </ResponsiveContainer>
                                </div>

                                {/* Stock Alerts */}
                                <div className="bg-white rounded-lg shadow p-6">
                                    <h3 className="text-lg font-semibold mb-4">Stock Alerts</h3>
                                    <div className="space-y-4">
                                        <div className="flex items-center justify-between p-3 bg-yellow-50 rounded-lg">
                                            <div className="flex items-center">
                                                <div className="w-2 h-2 bg-yellow-400 rounded-full mr-3"></div>
                                                <span>Low Stock Items</span>
                                            </div>
                                            <span className="font-semibold">{inventoryReport.lowStockCount}</span>
                                        </div>
                                        <div className="flex items-center justify-between p-3 bg-red-50 rounded-lg">
                                            <div className="flex items-center">
                                                <div className="w-2 h-2 bg-red-400 rounded-full mr-3"></div>
                                                <span>Out of Stock Items</span>
                                            </div>
                                            <span className="font-semibold">{inventoryReport.outOfStockCount}</span>
                                        </div>
                                        <div className="flex items-center justify-between p-3 bg-green-50 rounded-lg">
                                            <div className="flex items-center">
                                                <div className="w-2 h-2 bg-green-400 rounded-full mr-3"></div>
                                                <span>Healthy Stock</span>
                                            </div>
                                            <span className="font-semibold">
                        {inventoryReport.totalProducts - inventoryReport.lowStockCount - inventoryReport.outOfStockCount}
                      </span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default Reports;