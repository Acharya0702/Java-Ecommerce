import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../api/api';

export const fetchCart = createAsyncThunk('cart/fetchCart', async (_, { rejectWithValue }) => {
    try {
        const response = await api.get('/cart');
        console.log('fetchCart API response:', response.data);
        return response.data;
    } catch (error) {
        console.error('fetchCart error:', error.response?.data || error.message);
        return rejectWithValue(error.response?.data || error.message);
    }
});

export const addToCart = createAsyncThunk('cart/addToCart', async (productId, { rejectWithValue }) => {
    try {
        console.log('addToCart API call for product:', productId);
        const response = await api.post('/cart/items', { productId, quantity: 1 });
        console.log('addToCart API response:', response.data);
        return response.data;
    } catch (error) {
        console.error('addToCart error:', error.response?.data || error.message);
        return rejectWithValue(error.response?.data || error.message);
    }
});

export const updateCartItem = createAsyncThunk('cart/updateCartItem',
    async ({ itemId, quantity }, { rejectWithValue }) => {
        try {
            const response = await api.put(`/cart/items/${itemId}?quantity=${quantity}`);
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data || error.message);
        }
    }
);

export const removeCartItem = createAsyncThunk('cart/removeCartItem',
    async (itemId, { rejectWithValue }) => {
        try {
            const response = await api.delete(`/cart/items/${itemId}`);
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data || error.message);
        }
    }
);

export const clearCart = createAsyncThunk('cart/clearCart',
    async (_, { rejectWithValue }) => {
        try {
            const response = await api.delete('/cart');
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data || error.message);
        }
    }
);

const cartSlice = createSlice({
    name: 'cart',
    initialState: {
        items: [],
        totalItems: 0,
        totalAmount: 0,
        loading: false,
        error: null,
    },
    reducers: {
        // Remove updateCart reducer to avoid conflicts
        resetCart: (state) => {
            state.items = [];
            state.totalItems = 0;
            state.totalAmount = 0;
            state.error = null;
        },
    },
    extraReducers: (builder) => {
        builder
            // fetchCart
            .addCase(fetchCart.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchCart.fulfilled, (state, action) => {
                state.loading = false;
                state.items = action.payload.cartItems || [];
                state.totalItems = action.payload.totalItems || 0;
                state.totalAmount = action.payload.totalAmount || 0;
                console.log('Redux state updated with cart:', {
                    itemsCount: state.items.length,
                    totalItems: state.totalItems,
                    totalAmount: state.totalAmount
                });
            })
            .addCase(fetchCart.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload || action.error.message;
                console.error('fetchCart rejected:', action.payload);
            })

            // addToCart
            .addCase(addToCart.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(addToCart.fulfilled, (state, action) => {
                state.loading = false;
                state.items = action.payload.cartItems || [];
                state.totalItems = action.payload.totalItems || 0;
                state.totalAmount = action.payload.totalAmount || 0;
                console.log('Cart added successfully, new state:', {
                    itemsCount: state.items.length,
                    totalItems: state.totalItems,
                    totalAmount: state.totalAmount
                });
            })
            .addCase(addToCart.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload || action.error.message;
                console.error('addToCart rejected:', action.payload);
            })

            // updateCartItem
            .addCase(updateCartItem.fulfilled, (state, action) => {
                state.items = action.payload.cartItems || [];
                state.totalItems = action.payload.totalItems || 0;
                state.totalAmount = action.payload.totalAmount || 0;
            })

            // removeCartItem
            .addCase(removeCartItem.fulfilled, (state, action) => {
                state.items = action.payload.cartItems || [];
                state.totalItems = action.payload.totalItems || 0;
                state.totalAmount = action.payload.totalAmount || 0;
            })

            // clearCart
            .addCase(clearCart.fulfilled, (state) => {
                state.items = [];
                state.totalItems = 0;
                state.totalAmount = 0;
            });
    },
});

export const { resetCart } = cartSlice.actions;
export default cartSlice.reducer;