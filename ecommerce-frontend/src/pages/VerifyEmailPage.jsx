import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Container, Row, Col, Card, Button, Alert, Spinner } from 'react-bootstrap';
import { authApi } from '../api/authApi';
import { toast } from 'react-toastify';

const VerifyEmailPage = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const dispatch = useDispatch();
    const [status, setStatus] = useState('verifying'); // verifying, success, error
    const [message, setMessage] = useState('');
    const [verifiedEmail, setVerifiedEmail] = useState('');

    useEffect(() => {
        const token = searchParams.get('token');
        if (token) {
            verifyEmail(token);
        } else {
            setStatus('error');
            setMessage('No verification token provided');
        }
    }, [searchParams]);

    const verifyEmail = async (token) => {
        try {
            // Call the verification endpoint
            const response = await authApi.verifyEmail(token);

            // Get the email that was being verified from localStorage
            const verifyingEmail = localStorage.getItem('verifyingEmail');
            if (verifyingEmail) {
                setVerifiedEmail(verifyingEmail);
                localStorage.removeItem('verifyingEmail');
            }

            setStatus('success');
            setMessage('Email verified successfully! You can now login to your account.');
            toast.success('Email verified successfully!');

        } catch (error) {
            console.error('Verification error:', error);
            setStatus('error');
            setMessage(
                error.response?.data?.message ||
                'Verification failed. The link may have expired or is invalid.'
            );
            toast.error('Email verification failed');
        }
    };

    const handleGoToLogin = () => {
        navigate('/login', {
            state: {
                message: 'Email verified successfully! Please login.',
                email: verifiedEmail
            }
        });
    };

    return (
        <Container className="mt-5">
            <Row className="justify-content-center">
                <Col md={6} lg={5}>
                    <Card className="shadow">
                        <Card.Body className="text-center p-5">
                            {status === 'verifying' && (
                                <>
                                    <Spinner
                                        animation="border"
                                        variant="primary"
                                        className="mb-4"
                                        style={{ width: '3rem', height: '3rem' }}
                                    />
                                    <Card.Title className="mb-3">
                                        Verifying Your Email
                                    </Card.Title>
                                    <Card.Text className="text-muted">
                                        Please wait while we verify your email address...
                                    </Card.Text>
                                </>
                            )}

                            {status === 'success' && (
                                <>
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

                                    <Card.Title className="mb-3 text-success">
                                        Email Verified Successfully!
                                    </Card.Title>

                                    {verifiedEmail && (
                                        <Card.Text className="mb-3">
                                            <strong>{verifiedEmail}</strong>
                                        </Card.Text>
                                    )}

                                    <Card.Text className="text-muted mb-4">
                                        {message}
                                    </Card.Text>

                                    <div className="d-grid gap-2">
                                        <Button
                                            variant="primary"
                                            size="lg"
                                            onClick={handleGoToLogin}
                                        >
                                            Go to Login
                                        </Button>
                                    </div>
                                </>
                            )}

                            {status === 'error' && (
                                <>
                                    <div className="mb-4">
                                        <div className="mx-auto bg-danger bg-opacity-10 p-3 rounded-circle d-inline-block">
                                            <svg
                                                className="text-danger"
                                                width="48"
                                                height="48"
                                                fill="currentColor"
                                                viewBox="0 0 16 16"
                                            >
                                                <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM8 4a.905.905 0 0 0-.9.995l.35 3.507a.552.552 0 0 0 1.1 0l.35-3.507A.905.905 0 0 0 8 4zm.002 6a1 1 0 1 0 0 2 1 1 0 0 0 0-2z"/>
                                            </svg>
                                        </div>
                                    </div>

                                    <Card.Title className="mb-3 text-danger">
                                        Verification Failed
                                    </Card.Title>

                                    <Alert variant="danger" className="mb-4">
                                        {message}
                                    </Alert>

                                    <div className="d-grid gap-2">
                                        <Button
                                            variant="primary"
                                            onClick={() => navigate('/login')}
                                        >
                                            Go to Login
                                        </Button>

                                        <Button
                                            variant="link"
                                            onClick={() => navigate('/register')}
                                        >
                                            Register Again
                                        </Button>
                                    </div>
                                </>
                            )}
                        </Card.Body>
                    </Card>
                </Col>
            </Row>
        </Container>
    );
};

export default VerifyEmailPage;