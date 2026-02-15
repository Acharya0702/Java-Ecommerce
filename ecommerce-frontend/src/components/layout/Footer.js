// src/components/layout/Footer.js
import React from 'react';
import { Container, Row, Col, Nav, Button } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import {
    FaFacebook,
    FaTwitter,
    FaInstagram,
    FaLinkedin,
    FaYoutube,
    FaCcVisa,
    FaCcMastercard,
    FaCcPaypal,
    FaCcAmex,
    FaApple,
    FaAndroid,
} from 'react-icons/fa';

const Footer = () => {
    return (
        <footer className="bg-dark text-white mt-5">
            {/* Main Footer */}
            <Container className="py-5">
                <Row>
                    {/* About Section */}
                    <Col lg={3} md={6} className="mb-4">
                        <h5 className="mb-4">About ShopEase</h5>
                        <p className="text-white-50">
                            Your one-stop destination for quality products at
                            affordable prices. We offer fast shipping and
                            excellent customer service.
                        </p>
                        <div className="mt-4">
                            <h6 className="mb-3">Download Our App</h6>
                            <div className="d-flex gap-2">
                                <FaApple size={30} className="text-white-50" />
                                <FaAndroid size={30} className="text-white-50" />
                            </div>
                        </div>
                    </Col>

                    {/* Quick Links */}
                    <Col lg={2} md={6} className="mb-4">
                        <h5 className="mb-4">Quick Links</h5>
                        <Nav className="flex-column">
                            <Nav.Link as={Link} to="/about" className="text-white-50 p-0 mb-2">
                                About Us
                            </Nav.Link>
                            <Nav.Link as={Link} to="/contact" className="text-white-50 p-0 mb-2">
                                Contact Us
                            </Nav.Link>
                            <Nav.Link as={Link} to="/faq" className="text-white-50 p-0 mb-2">
                                FAQ
                            </Nav.Link>
                            <Nav.Link as={Link} to="/shipping" className="text-white-50 p-0 mb-2">
                                Shipping Info
                            </Nav.Link>
                            <Nav.Link as={Link} to="/returns" className="text-white-50 p-0 mb-2">
                                Returns Policy
                            </Nav.Link>
                        </Nav>
                    </Col>

                    {/* Customer Service */}
                    <Col lg={2} md={6} className="mb-4">
                        <h5 className="mb-4">Customer Service</h5>
                        <Nav className="flex-column">
                            <Nav.Link as={Link} to="/track-order" className="text-white-50 p-0 mb-2">
                                Track Order
                            </Nav.Link>
                            <Nav.Link as={Link} to="/size-guide" className="text-white-50 p-0 mb-2">
                                Size Guide
                            </Nav.Link>
                            <Nav.Link as={Link} to="/gift-cards" className="text-white-50 p-0 mb-2">
                                Gift Cards
                            </Nav.Link>
                            <Nav.Link as={Link} to="/bulk-orders" className="text-white-50 p-0 mb-2">
                                Bulk Orders
                            </Nav.Link>
                            <Nav.Link as={Link} to="/affiliate" className="text-white-50 p-0 mb-2">
                                Affiliate Program
                            </Nav.Link>
                        </Nav>
                    </Col>

                    {/* Contact Info */}
                    <Col lg={2} md={6} className="mb-4">
                        <h5 className="mb-4">Contact Us</h5>
                        <div className="text-white-50 mb-3">
                            <p className="mb-1">📍 123 E-commerce St.</p>
                            <p className="mb-1">New York, NY 10001</p>
                            <p className="mb-1">📞 +1 (234) 567-890</p>
                            <p className="mb-1">✉️ support@shopease.com</p>
                        </div>
                    </Col>

                    {/* Newsletter */}
                    <Col lg={3} md={6} className="mb-4">
                        <h5 className="mb-4">Newsletter</h5>
                        <p className="text-white-50 mb-3">
                            Subscribe to get updates on new products and special offers!
                        </p>
                        <div className="input-group mb-3">
                            <input
                                type="email"
                                className="form-control"
                                placeholder="Your email"
                                aria-label="Your email"
                            />
                            <Button variant="primary">Subscribe</Button>
                        </div>

                        {/* Social Media Links */}
                        <div className="mt-4">
                            <h6 className="mb-3">Follow Us</h6>
                            <div className="d-flex gap-3">
                                <FaFacebook size={24} className="text-white-50 cursor-pointer hover:text-primary" />
                                <FaTwitter size={24} className="text-white-50 cursor-pointer hover:text-info" />
                                <FaInstagram size={24} className="text-white-50 cursor-pointer hover:text-danger" />
                                <FaLinkedin size={24} className="text-white-50 cursor-pointer hover:text-primary" />
                                <FaYoutube size={24} className="text-white-50 cursor-pointer hover:text-danger" />
                            </div>
                        </div>
                    </Col>
                </Row>

                {/* Payment Methods */}
                <Row className="mt-4 pt-4 border-top border-secondary">
                    <Col md={6}>
                        <p className="text-white-50 mb-2">We Accept:</p>
                        <div className="d-flex gap-3">
                            <FaCcVisa size={40} className="text-white-50" />
                            <FaCcMastercard size={40} className="text-white-50" />
                            <FaCcPaypal size={40} className="text-white-50" />
                            <FaCcAmex size={40} className="text-white-50" />
                        </div>
                    </Col>
                    <Col md={6} className="text-md-end">
                        <p className="text-white-50 mb-2">© 2026 ShopEase. All rights reserved.</p>
                        <p className="text-white-50 small">
                            <Link to="/privacy" className="text-white-50 text-decoration-none me-3">
                                Privacy Policy
                            </Link>
                            <Link to="/terms" className="text-white-50 text-decoration-none me-3">
                                Terms of Service
                            </Link>
                            <Link to="/sitemap" className="text-white-50 text-decoration-none">
                                Sitemap
                            </Link>
                        </p>
                    </Col>
                </Row>
            </Container>

            {/* Copyright Bar */}
            <div className="bg-black py-3">
                <Container className="text-center text-white-50 small">
                    <p className="mb-0">
                        Designed and developed with ❤️ by Gaurishankar Acharya.
                        {/*All trademarks and copyrights are property of their respective owners.*/}
                    </p>
                </Container>
            </div>
        </footer>
    );
};

export default Footer;