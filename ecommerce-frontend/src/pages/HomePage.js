import React from 'react';
import { Container, Row, Col, Card, Button } from 'react-bootstrap';
import { Link } from 'react-router-dom';

const HomePage = () => {
    return (
        <Container>
            <div className="text-center my-5">
                <h1 className="display-4">Welcome to E-commerce Store</h1>
                <p className="lead">Your one-stop shop for all your needs</p>
                <Link to="/products">
                    <Button variant="primary" size="lg">Shop Now</Button>
                </Link>
            </div>

            <Row className="my-5">
                <Col md={4}>
                    <Card className="text-center h-100">
                        <Card.Body>
                            <Card.Title>🛒 Easy Shopping</Card.Title>
                            <Card.Text>
                                Browse through our wide selection of products with detailed descriptions and reviews.
                            </Card.Text>
                        </Card.Body>
                    </Card>
                </Col>
                <Col md={4}>
                    <Card className="text-center h-100">
                        <Card.Body>
                            <Card.Title>🚚 Fast Delivery</Card.Title>
                            <Card.Text>
                                Get your products delivered quickly with our reliable shipping partners.
                            </Card.Text>
                        </Card.Body>
                    </Card>
                </Col>
                <Col md={4}>
                    <Card className="text-center h-100">
                        <Card.Body>
                            <Card.Title>🔒 Secure Payment</Card.Title>
                            <Card.Text>
                                Shop with confidence using our secure payment gateway.
                            </Card.Text>
                        </Card.Body>
                    </Card>
                </Col>
            </Row>
        </Container>
    );
};

export default HomePage;