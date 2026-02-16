// src/components/layout/Header.js - Cart icon section
import React, { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { Container, Navbar, Nav, Badge, Button, NavDropdown, Image } from 'react-bootstrap';
import { FaShoppingCart, FaUser, FaHeart, FaBox, FaSignOutAlt, FaUserCircle, FaStore } from 'react-icons/fa';
import { clearCredentials } from '../../store/slices/authSlice';
import { fetchCart } from '../../store/slices/cartSlice'; // Import fetchCart

const Header = () => {
    const navigate = useNavigate();
    const dispatch = useDispatch();
    const { user, accessToken } = useSelector((state) => state.auth);
    const { totalItems } = useSelector((state) => state.cart);

    // Fetch cart when user logs in
    useEffect(() => {
        if (accessToken) {
            dispatch(fetchCart());
        }
    }, [accessToken, dispatch]);

    const handleLogout = () => {
        dispatch(clearCredentials());
        navigate('/login');
    };

    return (
        <>
            {/* Top Bar */}
            <div className="bg-primary text-white py-2">
                <Container className="d-flex justify-content-between align-items-center">
                    <div><small>📞 Customer Support: +1 234 567 890</small></div>
                    <div><small>🚚 Free Shipping on Orders Over $50</small></div>
                    <div><small>💰 30-Day Money Back Guarantee</small></div>
                </Container>
            </div>

            {/* Main Header */}
            <Navbar bg="dark" variant="dark" expand="lg" sticky="top" className="py-3">
                <Container>
                    <Navbar.Brand as={Link} to="/" className="d-flex align-items-center">
                        <FaStore className="me-2" size={28} />
                        <span className="fw-bold fs-4">ManguliShop</span>
                    </Navbar.Brand>

                    <Navbar.Toggle aria-controls="basic-navbar-nav" />

                    <Navbar.Collapse id="basic-navbar-nav">
                        <Nav className="ms-auto align-items-center">
                            <Nav.Link as={Link} to="/">Home</Nav.Link>
                            <Nav.Link as={Link} to="/products">Products</Nav.Link>

                            {/* Cart Icon with Badge - This will now update */}
                            <Nav.Link as={Link} to="/cart" className="position-relative me-3">
                                <FaShoppingCart size={22} />
                                {totalItems > 0 && (
                                    <Badge
                                        pill
                                        bg="danger"
                                        className="position-absolute top-0 start-100 translate-middle"
                                    >
                                        {totalItems}
                                    </Badge>
                                )}
                            </Nav.Link>

                            {/* User Menu */}
                            {accessToken ? (
                                <NavDropdown
                                    title={
                                        <div className="d-inline-flex align-items-center">
                                            <FaUserCircle size={28} className="me-2" />
                                            <span>{user?.firstName || 'User'}</span>
                                        </div>
                                    }
                                    id="user-dropdown"
                                    align="end"
                                >
                                    <NavDropdown.Item as={Link} to="/profile">
                                        <FaUser className="me-2" /> Profile
                                    </NavDropdown.Item>
                                    <NavDropdown.Item as={Link} to="/orders">
                                        <FaBox className="me-2" /> Orders
                                    </NavDropdown.Item>
                                    <NavDropdown.Divider />
                                    <NavDropdown.Item onClick={handleLogout}>
                                        <FaSignOutAlt className="me-2" /> Logout
                                    </NavDropdown.Item>
                                </NavDropdown>
                            ) : (
                                <div className="d-flex">
                                    <Button
                                        variant="outline-light"
                                        as={Link}
                                        to="/login"
                                        className="me-2 rounded-pill px-4"
                                    >
                                        Sign In
                                    </Button>
                                    <Button
                                        variant="primary"
                                        as={Link}
                                        to="/register"
                                        className="rounded-pill px-4"
                                    >
                                        Register
                                    </Button>
                                </div>
                            )}
                        </Nav>
                    </Navbar.Collapse>
                </Container>
            </Navbar>
        </>
    );
};

export default Header;