import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { authApi } from '../../api/authApi';

// Helper function to safely get user from localStorage
const getUserFromStorage = () => {
    try {
        const user = localStorage.getItem('user');
        return user && user !== 'undefined' ? JSON.parse(user) : null;
    } catch (error) {
        console.error('Error parsing user from localStorage:', error);
        return null;
    }
};

// Async thunks
export const login = createAsyncThunk(
    'auth/login',
    async (credentials, { rejectWithValue }) => {
        try {
            const response = await authApi.login(credentials);
            return response;
        } catch (error) {
            return rejectWithValue(error.response?.data?.message || 'Login failed');
        }
    }
);

export const register = createAsyncThunk(
    'auth/register',
    async (userData, { rejectWithValue }) => {
        try {
            const response = await authApi.register(userData);
            return response;
        } catch (error) {
            return rejectWithValue(error.response?.data?.message || 'Registration failed');
        }
    }
);

export const verifyEmail = createAsyncThunk(
    'auth/verifyEmail',
    async (token, { rejectWithValue }) => {
        try {
            const response = await authApi.verifyEmail(token);
            return response;
        } catch (error) {
            return rejectWithValue(error.response?.data?.message || 'Email verification failed');
        }
    }
);

export const logout = createAsyncThunk(
    'auth/logout',
    async (_, { dispatch }) => {
        try {
            await authApi.logout();
        } finally {
            dispatch(clearCredentials());
        }
    }
);

const authSlice = createSlice({
    name: 'auth',
    initialState: {
        user: getUserFromStorage(),
        token: localStorage.getItem('accessToken'),
        refreshToken: localStorage.getItem('refreshToken'),
        isLoading: false,
        error: null,
        verificationMessage: null,
    },
    reducers: {
        setCredentials: (state, action) => {
            const { accessToken, refreshToken, user } = action.payload;
            state.token = accessToken;
            state.refreshToken = refreshToken;
            state.user = user;

            // Save to localStorage with checks
            if (accessToken) localStorage.setItem('accessToken', accessToken);
            if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
            if (user) localStorage.setItem('user', JSON.stringify(user));
        },
        clearCredentials: (state) => {
            state.user = null;
            state.token = null;
            state.refreshToken = null;
            state.error = null;
            state.verificationMessage = null;

            // Remove from localStorage
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
        },
        clearError: (state) => {
            state.error = null;
        },
        clearVerificationMessage: (state) => {
            state.verificationMessage = null;
        },
    },
    extraReducers: (builder) => {
        builder
            // Login cases
            .addCase(login.pending, (state) => {
                state.isLoading = true;
                state.error = null;
            })
            .addCase(login.fulfilled, (state, action) => {
                state.isLoading = false;
                state.user = action.payload.user;
                state.token = action.payload.access_token;
                state.refreshToken = action.payload.refresh_token;
                state.error = null;

                // Save to localStorage
                localStorage.setItem('accessToken', action.payload.access_token);
                localStorage.setItem('refreshToken', action.payload.refresh_token);
                localStorage.setItem('user', JSON.stringify(action.payload.user));
            })
            .addCase(login.rejected, (state, action) => {
                state.isLoading = false;
                state.error = action.payload;
            })

            // Register cases
            .addCase(register.pending, (state) => {
                state.isLoading = true;
                state.error = null;
                state.verificationMessage = null;
            })
            .addCase(register.fulfilled, (state, action) => {
                state.isLoading = false;
                state.user = action.payload.user;
                state.token = action.payload.accessToken;
                state.refreshToken = action.payload.refreshToken;
                state.verificationMessage = 'Registration successful! Please check your email for verification.';
                state.error = null;

                // Save to localStorage
                localStorage.setItem('accessToken', action.payload.accessToken);
                localStorage.setItem('refreshToken', action.payload.refreshToken);
                localStorage.setItem('user', JSON.stringify(action.payload.user));
            })
            .addCase(register.rejected, (state, action) => {
                state.isLoading = false;
                state.error = action.payload;
                state.verificationMessage = null;
            })

            // Verify email cases
            .addCase(verifyEmail.pending, (state) => {
                state.isLoading = true;
                state.error = null;
            })
            .addCase(verifyEmail.fulfilled, (state) => {
                state.isLoading = false;
                if (state.user) {
                    state.user.isEmailVerified = true;
                    localStorage.setItem('user', JSON.stringify(state.user));
                }
                state.verificationMessage = 'Email verified successfully! You can now log in.';
            })
            .addCase(verifyEmail.rejected, (state, action) => {
                state.isLoading = false;
                state.error = action.payload;
            });
    },
});

export const { setCredentials, clearCredentials, clearError, clearVerificationMessage } = authSlice.actions;

// Selectors
export const selectCurrentUser = (state) => state.auth.user;
export const selectCurrentToken = (state) => state.auth.token;
export const selectIsAuthenticated = (state) => !!state.auth.token;
export const selectAuthLoading = (state) => state.auth.isLoading;
export const selectAuthError = (state) => state.auth.error;
export const selectVerificationMessage = (state) => state.auth.verificationMessage;
export const selectIsEmailVerified = (state) => state.auth.user?.isEmailVerified || false;

export default authSlice.reducer;