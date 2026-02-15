import api from './api';

export const productApi = {
    getAllProducts: async (params = {}) => {
        const response = await api.get('/products', { params });
        return response.data;
    },

    getProductById: async (id) => {
        const response = await api.get(`/products/${id}`);
        return response.data;
    },

    getProductsByCategory: async (category, params = {}) => {
        const response = await api.get(`/products/category/${category}`, { params });
        return response.data;
    },

    searchProducts: async (query) => {
        const response = await api.get('/products/search', { params: { q: query } });
        return response.data;
    }
};