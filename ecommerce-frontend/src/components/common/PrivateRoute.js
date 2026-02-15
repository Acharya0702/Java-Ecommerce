import React from 'react';
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

const PrivateRoute = ({ children }) => {
    const { token } = useSelector((state) => state.auth);

    // If not authenticated, redirect to login
    if (!token) {
        return <Navigate to="/login" replace />;
    }

    // If authenticated, render the child component
    return children;
};

export default PrivateRoute;