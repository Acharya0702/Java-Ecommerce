import api from './api';

export const cartApi = {
    getCart: async () => {
        const response = await api.get('/cart');
        return response.data;
    },

    addToCart: async (productId, quantity = 1) => {
        const response = await api.post('/cart/items', { productId, quantity });
        return response.data;
    },

    updateCartItem: async (itemId, data) => {
        const response = await api.put(`/cart/items/${itemId}`, data);
        return response.data;
    },

    removeFromCart: async (itemId) => {
        const response = await api.delete(`/cart/items/${itemId}`);
        return response.data;
    },

    clearCart: async () => {
        const response = await api.delete('/cart/clear');
        return response.data;
    }
};