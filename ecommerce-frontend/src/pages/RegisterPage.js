import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Form, Button, Card, Alert, Container, Row, Col } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { toast } from 'react-toastify';
import { register as registerUser, clearError } from '../store/slices/authSlice';

const schema = yup.object({
    email: yup
        .string()
        .email('Invalid email format')
        .required('Email is required'),
    password: yup
        .string()
        .min(6, 'Password must be at least 6 characters')
        .required('Password is required'),
    firstName: yup
        .string()
        .required('First name is required'),
    lastName: yup
        .string()
        .required('Last name is required'),
    phone: yup
        .string()
        .matches(/^[0-9]{10}$/, 'Phone must be 10 digits')
        .required('Phone is required'),
});

const RegisterPage = () => {
    const navigate = useNavigate();
    const dispatch = useDispatch();
    const [registrationSuccess, setRegistrationSuccess] = useState(false);
    const [registeredEmail, setRegisteredEmail] = useState('');
    const { isLoading, error } = useSelector((state) => state.auth);

    const { register: formRegister, handleSubmit, formState: { errors } } = useForm({
        resolver: yupResolver(schema)
    });

    const onSubmit = async (data) => {
        try {
            // Store email for verification flow
            localStorage.setItem('verifyingEmail', data.email);
            setRegisteredEmail(data.email);

            const result = await dispatch(registerUser(data)).unwrap();

            setRegistrationSuccess(true);
            toast.success('Registration successful! Please check your email to verify your account.');

            // Don't navigate immediately - show success message
            // User will click the verification link in email
        } catch (error) {
            toast.error(error || 'Registration failed');
        }
    };

    const handleResendVerification = async () => {
        try {
            // You'll need to add this to your authApi
            // await authApi.resendVerification(registeredEmail);
            toast.info('Verification email resent. Please check your inbox.');
        } catch (error) {
            toast.error('Failed to resend verification email');
        }
    };

    if (registrationSuccess) {
        return (
            <Container className="mt-5">
                <Row className="justify-content-center">
                    <Col md={8} lg={6}>
                        <Card className="shadow">
                            <Card.Body className="text-center">
                                <div className="mb-4">
                                    <div className="mx-auto bg-success bg-opacity-10 p-3 rounded-circle d-inline-block">
                                        <svg
                                            className="text-success"
                                            width="48"
                                            height="48"
                                            fill="currentColor"
                                            viewBox="0 0 16 16"
                                        >
                                            <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"/>
                                        </svg>
                                    </div>
                                </div>

                                <Card.Title className="mb-3">Verify Your Email</Card.Title>

                                <Card.Text className="text-muted mb-4">
                                    We've sent a verification email to:<br />
                                    <strong>{registeredEmail}</strong>
                                </Card.Text>

                                <Card.Text className="mb-4">
                                    Please check your email and click the verification link to activate your account.
                                    If you don't see the email, check your spam folder.
                                </Card.Text>

                                <div className="d-grid gap-2">
                                    <Button
                                        variant="outline-primary"
                                        onClick={handleResendVerification}
                                    >
                                        Resend Verification Email
                                    </Button>

                                    <Button
                                        variant="link"
                                        onClick={() => navigate('/login')}
                                    >
                                        Go to Login
                                    </Button>
                                </div>
                            </Card.Body>
                        </Card>
                    </Col>
                </Row>
            </Container>
        );
    }

    return (
        <Container className="mt-5">
            <Row className="justify-content-center">
                <Col md={8} lg={6}>
                    <Card className="shadow">
                        <Card.Body>
                            <Card.Title className="text-center mb-4">Create Account</Card.Title>

                            {error && <Alert variant="danger">{error}</Alert>}

                            <Form onSubmit={handleSubmit(onSubmit)}>
                                <Row>
                                    <Col md={6}>
                                        <Form.Group className="mb-3">
                                            <Form.Label>First Name</Form.Label>
                                            <Form.Control
                                                type="text"
                                                {...formRegister('firstName')}
                                                isInvalid={!!errors.firstName}
                                                placeholder="Enter your first name"
                                            />
                                            <Form.Control.Feedback type="invalid">
                                                {errors.firstName?.message}
                                            </Form.Control.Feedback>
                                        </Form.Group>
                                    </Col>
                                    <Col md={6}>
                                        <Form.Group className="mb-3">
                                            <Form.Label>Last Name</Form.Label>
                                            <Form.Control
                                                type="text"
                                                {...formRegister('lastName')}
                                                isInvalid={!!errors.lastName}
                                                placeholder="Enter your last name"
                                            />
                                            <Form.Control.Feedback type="invalid">
                                                {errors.lastName?.message}
                                            </Form.Control.Feedback>
                                        </Form.Group>
                                    </Col>
                                </Row>

                                <Form.Group className="mb-3">
                                    <Form.Label>Email</Form.Label>
                                    <Form.Control
                                        type="email"
                                        {...formRegister('email')}
                                        isInvalid={!!errors.email}
                                        placeholder="Enter your email"
                                    />
                                    <Form.Control.Feedback type="invalid">
                                        {errors.email?.message}
                                    </Form.Control.Feedback>
                                </Form.Group>

                                <Form.Group className="mb-3">
                                    <Form.Label>Phone</Form.Label>
                                    <Form.Control
                                        type="tel"
                                        {...formRegister('phone')}
                                        isInvalid={!!errors.phone}
                                        placeholder="Enter your phone number"
                                    />
                                    <Form.Control.Feedback type="invalid">
                                        {errors.phone?.message}
                                    </Form.Control.Feedback>
                                </Form.Group>

                                <Form.Group className="mb-3">
                                    <Form.Label>Password</Form.Label>
                                    <Form.Control
                                        type="password"
                                        {...formRegister('password')}
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
                                        {isLoading ? 'Creating Account...' : 'Register'}
                                    </Button>
                                </div>

                                <div className="text-center">
                                    <p>
                                        Already have an account?{' '}
                                        <Link to="/login" className="text-decoration-none">
                                            Login here
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

export default RegisterPage;