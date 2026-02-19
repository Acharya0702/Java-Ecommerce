import api from './api';

export const adminApi = {
    // Dashboard
    getDashboardStats: () => api.get('/admin/dashboard/stats'),

    // Fix: Get sales chart data with date range instead of days
    getSalesChartData: (days = 7) => {
        const endDate = new Date();
        const startDate = new Date();
        startDate.setDate(startDate.getDate() - days);

        // Format dates as ISO strings
        const start = startDate.toISOString();
        const end = endDate.toISOString();

        return api.get(`/admin/dashboard/sales-chart?start=${start}&end=${end}`);
    },

    // Orders
    getAllOrders: (page = 0, size = 20, status = '', search = '') =>
        api.get(`/admin/orders?page=${page}&size=${size}&status=${status}&search=${search}`),
    getOrderDetails: (id) => api.get(`/admin/orders/${id}`),
    updateOrderStatus: (id, data) => api.put(`/admin/orders/${id}/status`, data),
    processPayment: (id) => api.post(`/admin/orders/${id}/process-payment`),
    bulkUpdateOrderStatus: (orderIds, status) =>
        api.post(`/admin/orders/bulk-status-update?status=${status}`, orderIds),

    // Products
    getAllProducts: (page = 0, size = 20, category = '', search = '', inStock = null) =>
        api.get(`/admin/products?page=${page}&size=${size}&category=${category}&search=${search}&inStock=${inStock}`),
    createProduct: (data) => api.post('/admin/products', data),
    updateProduct: (id, data) => api.put(`/admin/products/${id}`, data),
    deleteProduct: (id) => api.delete(`/admin/products/${id}`),
    bulkUpdateProducts: (data) => api.post('/admin/products/bulk-update', data),
    uploadProductImage: (id, file) => {
        const formData = new FormData();
        formData.append('image', file);
        return api.post(`/admin/products/${id}/upload-image`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
    },

    // Categories
    getAllCategories: () => api.get('/admin/categories'),
    createCategory: (data) => api.post('/admin/categories', data),
    updateCategory: (id, data) => api.put(`/admin/categories/${id}`, data),
    deleteCategory: (id) => api.delete(`/admin/categories/${id}`),

    // Users
    getAllUsers: (page = 0, size = 20, role = '', search = '') =>
        api.get(`/admin/users?page=${page}&size=${size}&role=${role}&search=${search}`),
    getUserDetails: (id) => api.get(`/admin/users/${id}`),
    updateUserRole: (id, role) => api.put(`/admin/users/${id}/role?role=${role}`),
    toggleUserStatus: (id) => api.put(`/admin/users/${id}/toggle-status`),
    getUserStats: () => api.get('/admin/users/stats'),

    // Reports
    getSalesReport: (start, end, interval = 'daily') =>
        api.get(`/admin/reports/sales?start=${start}&end=${end}&interval=${interval}`),
    getTopProducts: (limit = 10) => api.get(`/admin/reports/top-products?limit=${limit}`),
    getInventoryReport: () => api.get('/admin/reports/inventory'),

    // Inventory
    getLowStockProducts: (threshold = 10) =>
        api.get(`/admin/inventory/low-stock?threshold=${threshold}`),
    getOutOfStockProducts: () => api.get('/admin/inventory/out-of-stock'),
};