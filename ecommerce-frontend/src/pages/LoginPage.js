import React, { useState, useEffect } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Form, Button, Card, Alert, Container, Row, Col } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { toast } from 'react-toastify';
import { login, clearError } from '../store/slices/authSlice';

const schema = yup.object({
    email: yup
        .string()
        .email('Invalid email format')
        .required('Email is required'),
    password: yup
        .string()
        .min(6, 'Password must be at least 6 characters')
        .required('Password is required'),
});

const LoginPage = () => {
    const navigate = useNavigate();
    const dispatch = useDispatch();
    const location = useLocation();
    const [successMessage, setSuccessMessage] = useState('');
    const { isLoading, error } = useSelector((state) => state.auth);

    const { register, handleSubmit, setValue, formState: { errors } } = useForm({
        resolver: yupResolver(schema)
    });

    useEffect(() => {
        // Clear any previous errors when component mounts
        dispatch(clearError());

        // Check for message passed from verification
        if (location.state?.message) {
            setSuccessMessage(location.state.message);

            // Pre-fill email if provided
            if (location.state?.email) {
                setValue('email', location.state.email);
            }

            // Clear the location state after 5 seconds
            setTimeout(() => {
                setSuccessMessage('');
            }, 5000);
        }

        // Clear the location state
        window.history.replaceState({}, document.title);
    }, [location, dispatch, setValue]);

    const onSubmit = async (data) => {
        try {
            const result = await dispatch(login(data)).unwrap();
            toast.success('Login successful!');

            // Clear any stored verification email
            localStorage.removeItem('verifyingEmail');

            navigate('/');
        } catch (error) {
            toast.error(error || 'Login failed');
        }
    };

    return (
        <Container className="mt-5">
            <Row className="justify-content-center">
                <Col md={6} lg={5}>
                    <Card className="shadow">
                        <Card.Body>
                            <Card.Title className="text-center mb-4">Login</Card.Title>

                            {successMessage && (
                                <Alert variant="success" dismissible onClose={() => setSuccessMessage('')}>
                                    {successMessage}
                                </Alert>
                            )}

                            {error && <Alert variant="danger">{error}</Alert>}

                            <Form onSubmit={handleSubmit(onSubmit)}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Email</Form.Label>
                                    <Form.Control
                                        type="email"
                                        {...register('email')}
                                        isInvalid={!!errors.email}
                                        placeholder="Enter your email"
                                    />
                                    <Form.Control.Feedback type="invalid">
                                        {errors.email?.message}
                                    </Form.Control.Feedback>
                                </Form.Group>

                                <Form.Group className="mb-3">
                                    <Form.Label>Password</Form.Label>
                                    <Form.Control
                                        type="password"
                                        {...register('password')}
                                        isInvalid={!!errors.password}
                                        placeholder="Enter your password"
                                    />
                                    <Form.Control.Feedback type="invalid">
                                        {errors.password?.message}
                                    </Form.Control.Feedback>
                                </Form.Group>

                                <div className="d-grid mb-3">
                                    <Button
                                        variant="primary"
                                        type="submit"
                                        disabled={isLoading}
                                    >
                                        {isLoading ? 'Logging in...' : 'Login'}
                                    </Button>
                                </div>

                                <div className="text-center">
                                    <Link to="/forgot-password" className="text-decoration-none">
                                        Forgot Password?
                                    </Link>
                                    <p className="mt-3">
                                        Don't have an account?{' '}
                                        <Link to="/register" className="text-decoration-none">
                                            Register here
                                        </Link>
                                    </p>
                                </div>
                            </Form>
                        </Card.Body>
                    </Card>
                </Col>
            </Row>
        </Container>
    );
};

export default LoginPage;