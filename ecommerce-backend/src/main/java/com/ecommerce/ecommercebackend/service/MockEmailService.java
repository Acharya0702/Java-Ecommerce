package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")  // Only use in "dev" profile
@Slf4j
public class MockEmailService implements EmailService {

    @Override
    public void sendVerificationEmail(String to, String token) {
        log.warn("⚠️ [MOCK EMAIL - Development Only] Verification email would be sent to: {} with token: {}", to, token);
        log.info("📧 Verification URL: http://localhost:3000/verify-email?token={}", token);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        log.warn("⚠️ [MOCK EMAIL - Development Only] Password reset email would be sent to: {} with token: {}", to, token);
        log.info("📧 Reset URL: http://localhost:3000/reset-password?token={}", token);
    }

    @Override
    public void sendOrderConfirmation(OrderDTO order) {
        log.warn("⚠️ [MOCK EMAIL - Development Only] Order confirmation for order: {}, to: {}",
                order != null ? order.getOrderNumber() : "N/A",
                order != null ? order.getUserEmail() : "N/A");
        if (order != null) {
            log.info("📧 Order details: {} items, Total: ${}",
                    order.getOrderItems() != null ? order.getOrderItems().size() : 0,
                    order.getTotalAmount());
        }
    }

    @Override
    public void sendOrderConfirmation(String to, String orderNumber, String customerName) {
        log.warn("⚠️ [MOCK EMAIL - Development Only] Simple order confirmation to: {} for order: {}, customer: {}",
                to, orderNumber, customerName);
        log.info("📧 Order #{} confirmed for {}", orderNumber, customerName);
    }

    @Override
    public void sendOrderShippedEmail(OrderDTO order, String trackingNumber) {
        log.warn("⚠️ [MOCK EMAIL - Development Only] Order shipped notification for order: {}, to: {}, tracking: {}",
                order != null ? order.getOrderNumber() : "N/A",
                order != null ? order.getUserEmail() : "N/A",
                trackingNumber != null ? trackingNumber : "N/A");
    }

    @Override
    public void sendOrderDeliveredEmail(OrderDTO order) {
        log.warn("⚠️ [MOCK EMAIL - Development Only] Order delivered notification for order: {}, to: {}",
                order != null ? order.getOrderNumber() : "N/A",
                order != null ? order.getUserEmail() : "N/A");
    }

    @Override
    public void sendWelcomeEmail(String to, String customerName) {
        log.warn("⚠️ [MOCK EMAIL - Development Only] Welcome email to: {} for customer: {}", to, customerName);
        log.info("📧 Welcome, {}! Start shopping at http://localhost:3000", customerName);
    }
}