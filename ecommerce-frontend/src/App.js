import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import 'bootstrap/dist/css/bootstrap.min.css';

import store from './store/store';
import Header from './components/layout/Header';
import Footer from './components/layout/Footer';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import VerifyEmailPage from './pages/VerifyEmailPage';
import HomePage from './pages/HomePage';
import ProductsPage from './pages/ProductsPage';
import CartPage from './pages/CartPage';
import CheckoutPage from './pages/CheckoutPage';
import OrderPage from './pages/OrderPage';
import OrdersPage from './pages/OrdersPage';
import OrderHistoryPage from './pages/OrderHistoryPage';
import OrderDetailPage from './pages/OrderDetailPage';
import ProductDetailPage from './pages/ProductDetailPage';
import PrivateRoute from './components/common/PrivateRoute';

import AdminLayout from './components/admin/AdminLayout';
import AdminDashboard from './pages/admin/Dashboard';
import Orders from './pages/admin/Orders';
import Products from './pages/admin/Products';
import Categories from './pages/admin/Categories';
import Users from './pages/admin/Users';
import Reports from './pages/admin/Reports';
import Settings from './pages/admin/Settings';


function App() {
  return (
      <Provider store={store}>
        <Router>
          <div className="App">
            <Header />
            <main className="py-4">
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/verify-email" element={<VerifyEmailPage />} />
                <Route path="/products" element={<ProductsPage />} />
                <Route path="/products/:id" element={<ProductDetailPage />} />
                <Route path="/order" element={<OrdersPage />} />
                <Route path="/order/:id" element={<OrderPage />} />

                {/* Protected Routes */}
                <Route path="/cart" element={
                  <PrivateRoute>
                    <CartPage />
                  </PrivateRoute>
                } />
                <Route path="/checkout" element={
                  <PrivateRoute>
                    <CheckoutPage />
                  </PrivateRoute>
                } />
                <Route path="/orders" element={
                  <PrivateRoute>
                    <OrderHistoryPage />
                  </PrivateRoute>
                } />
                <Route path="/order/:id" element={
                  <PrivateRoute>
                    <OrderDetailPage />
                  </PrivateRoute>
                } />

                {/* Admin Routes */}
                <Route path="/admin" element={
                  <PrivateRoute adminOnly>
                    <AdminLayout />
                  </PrivateRoute>
                }>
                  <Route index element={<AdminDashboard />} />
                  <Route path="orders" element={<Orders />} />
                  <Route path="products" element={<Products />} />
                  <Route path="categories" element={<Categories />} />
                  <Route path="users" element={<Users />} />
                  <Route path="reports" element={<Reports />} />
                  <Route path="settings" element={<Settings />} />
                </Route>
                {/* Add more routes as needed */}
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </main>
            <Footer />
            <ToastContainer position="top-right" autoClose={3000} />
          </div>
        </Router>
      </Provider>
  );
}

export default App;