import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../api/api';

// Async thunk for fetching products
export const fetchProducts = createAsyncThunk(
    'products/fetchProducts',
    async ({ page = 1, category = '', priceRange = [0, 1000], sortBy = 'newest', search = '' }) => {
        try {
            const params = new URLSearchParams({
                page,
                category,
                minPrice: priceRange[0],
                maxPrice: priceRange[1],
                sort: sortBy,
                search
            });

            const response = await api.get(`/products?${params}`);
            return response.data;
        } catch (error) {
            throw error.response?.data?.message || 'Failed to fetch products';
        }
    }
);

// Async thunk for fetching single product
export const fetchProductById = createAsyncThunk(
    'products/fetchProductById',
    async (id) => {
        try {
            const response = await api.get(`/products/${id}`);
            return response.data;
        } catch (error) {
            throw error.response?.data?.message || 'Failed to fetch product';
        }
    }
);

const productSlice = createSlice({
    name: 'products',
    initialState: {
        products: [],
        currentProduct: null,
        loading: false,
        error: null,
        totalPages: 1,
        currentPage: 1
    },
    reducers: {
        clearError: (state) => {
            state.error = null;
        },
        clearCurrentProduct: (state) => {
            state.currentProduct = null;
        }
    },
    extraReducers: (builder) => {
        builder
            // Fetch Products
            .addCase(fetchProducts.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchProducts.fulfilled, (state, action) => {
                state.loading = false;
                state.products = action.payload.products || action.payload;
                state.totalPages = action.payload.totalPages || 1;
                state.currentPage = action.payload.currentPage || 1;
            })
            .addCase(fetchProducts.rejected, (state, action) => {
                state.loading = false;
                state.error = action.error.message;
            })

            // Fetch Single Product
            .addCase(fetchProductById.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchProductById.fulfilled, (state, action) => {
                state.loading = false;
                state.currentProduct = action.payload;
            })
            .addCase(fetchProductById.rejected, (state, action) => {
                state.loading = false;
                state.error = action.error.message;
            });
    }
});

export const { clearError, clearCurrentProduct } = productSlice.actions;
export default productSlice.reducer;