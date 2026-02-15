import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../api/api';

// Async thunk for fetching wishlist
export const fetchWishlist = createAsyncThunk(
    'wishlist/fetchWishlist',
    async () => {
        try {
            const response = await api.get('/wishlist');
            return response.data;
        } catch (error) {
            throw error.response?.data?.message || 'Failed to fetch wishlist';
        }
    }
);

// Async thunk for adding to wishlist
export const addToWishlist = createAsyncThunk(
    'wishlist/addToWishlist',
    async (productId) => {
        try {
            const response = await api.post('/wishlist', { productId });
            return response.data;
        } catch (error) {
            throw error.response?.data?.message || 'Failed to add to wishlist';
        }
    }
);

// Async thunk for removing from wishlist
export const removeFromWishlist = createAsyncThunk(
    'wishlist/removeFromWishlist',
    async (productId) => {
        try {
            await api.delete(`/wishlist/${productId}`);
            return productId;
        } catch (error) {
            throw error.response?.data?.message || 'Failed to remove from wishlist';
        }
    }
);

const wishlistSlice = createSlice({
    name: 'wishlist',
    initialState: {
        items: [],
        loading: false,
        error: null
    },
    reducers: {
        toggleWishlist: (state, action) => {
            const product = action.payload;
            const exists = state.items.some(item => item.id === product.id);

            if (exists) {
                state.items = state.items.filter(item => item.id !== product.id);
            } else {
                state.items.push(product);
            }

            // Save to localStorage
            localStorage.setItem('wishlist', JSON.stringify(state.items));
        },
        clearError: (state) => {
            state.error = null;
        },
        loadWishlistFromStorage: (state) => {
            const saved = localStorage.getItem('wishlist');
            if (saved) {
                state.items = JSON.parse(saved);
            }
        }
    },
    extraReducers: (builder) => {
        builder
            // Fetch Wishlist
            .addCase(fetchWishlist.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchWishlist.fulfilled, (state, action) => {
                state.loading = false;
                state.items = action.payload;
                localStorage.setItem('wishlist', JSON.stringify(action.payload));
            })
            .addCase(fetchWishlist.rejected, (state, action) => {
                state.loading = false;
                state.error = action.error.message;
            })

            // Add to Wishlist
            .addCase(addToWishlist.fulfilled, (state, action) => {
                state.items.push(action.payload);
                localStorage.setItem('wishlist', JSON.stringify(state.items));
            })

            // Remove from Wishlist
            .addCase(removeFromWishlist.fulfilled, (state, action) => {
                state.items = state.items.filter(item => item.id !== action.payload);
                localStorage.setItem('wishlist', JSON.stringify(state.items));
            });
    }
});

export const { toggleWishlist, clearError, loadWishlistFromStorage } = wishlistSlice.actions;
export default wishlistSlice.reducer;