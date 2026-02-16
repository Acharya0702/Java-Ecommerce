// src/api/orderApi.js
import api from './api';

export const orderApi = {
    createOrder: (orderData) => {
        return api.post('/orders', orderData);
    },

    getUserOrders: () => {
        return api.get('/orders');
    },

    getOrderById: (id) => {
        return api.get(`/orders/${id}`);
    },

    getOrderByNumber: (orderNumber) => {
        return api.get(`/orders/number/${orderNumber}`);
    },

    cancelOrder: (id) => {
        return api.put(`/orders/${id}/cancel`);
    }
};