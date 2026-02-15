package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.OrderDTO;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendPasswordResetEmail(String to, String token);
    void sendOrderConfirmation(String to, String orderNumber, String customerName);
    void sendOrderConfirmation(OrderDTO order);
    void sendOrderShippedEmail(OrderDTO order, String trackingNumber);
    void sendOrderDeliveredEmail(OrderDTO order);
    void sendWelcomeEmail(String to, String customerName);
}