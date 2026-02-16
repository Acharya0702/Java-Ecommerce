import React from 'react';
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

const PrivateRoute = ({ children }) => {
    const { accessToken } = useSelector((state) => state.auth);

    // If not authenticated, redirect to login
    if (!accessToken) {
        return <Navigate to="/login" replace />;
    }

    // If authenticated, render the child component
    console.log(accessToken);
    return children;
};

export default PrivateRoute;