import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { selectIsAuthenticated, selectCurrentUser } from '../../store/slices/authSlice';

const PrivateRoute = ({ children, adminOnly = false }) => {
    const isAuthenticated = useSelector(selectIsAuthenticated);
    const user = useSelector(selectCurrentUser);

    // Not authenticated - redirect to login
    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    // Admin route but user is not admin - redirect to home
    if (adminOnly && user?.role !== 'ADMIN') {
        return <Navigate to="/" replace />;
    }

    // If children are provided, render them, otherwise render Outlet for nested routes
    return children ? children : <Outlet />;
};

export default PrivateRoute;