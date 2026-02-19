package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.OrderDTO;
import com.ecommerce.ecommercebackend.dto.AddressDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Primary
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username:no-reply@ecommerce.com}")
    private String fromEmail;

    @Value("${spring.mail.from:noreply@ecommerce.com}")
    private String senderEmail;

    @Override
    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        String subject = "Verify Your Email - E-commerce Store";

        try {
            String htmlContent = buildVerificationEmailHtml(verificationUrl);
            sendHtmlEmail(to, subject, htmlContent);
            log.info("✅ Verification email sent successfully to: {}", to);
        } catch (MailAuthenticationException e) {
            log.error("❌ Email authentication failed. Check your credentials.");
            log.info("Verification URL for manual testing: {}", verificationUrl);
            throw new RuntimeException("Email authentication failed: " + e.getMessage());
        } catch (MailSendException e) {
            log.error("❌ Failed to send email to: {}. Error: {}", to, e.getMessage());
            log.info("Verification URL for manual testing: {}", verificationUrl);
        } catch (Exception e) {
            log.error("❌ Unexpected error sending verification email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset Your Password - E-commerce Store";

        try {
            String htmlContent = buildPasswordResetEmailHtml(resetUrl);
            sendHtmlEmail(to, subject, htmlContent);
            log.info("✅ Password reset email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send password reset email to {}: {}", to, e.getMessage());
            log.info("Reset URL for manual testing: {}", resetUrl);
        }
    }

    @Async
    @Override
    public void sendOrderConfirmation(OrderDTO order) {
        log.info("🎯 EMAIL SERVICE - sendOrderConfirmation called");
        log.info("🎯 Order object: {}", order);
        log.info("🎯 Order userEmail: {}", order != null ? order.getUserEmail() : "order is null");
        if (order == null) {
            log.error("❌ Cannot send order confirmation: Order is null");
            return;
        }

        if (order.getUserEmail() == null) {
            log.error("❌ Cannot send order confirmation: userEmail is null");
            log.error("❌ Order details - ID: {}, Number: {}, UserName: {}",
                    order.getId(), order.getOrderNumber(), order.getUserName());
            return;
        }

        String subject = "Order Confirmation #" +
                Optional.ofNullable(order.getOrderNumber()).orElse("N/A") +
                " - E-commerce Store";
        String to = order.getUserEmail();

        try {
            String htmlContent = buildOrderConfirmationHtml(order);
            sendHtmlEmail(to, subject, htmlContent);
            log.info("✅ Order confirmation email sent successfully to: {} for order: {}",
                    to, order.getOrderNumber());
        } catch (Exception e) {
            log.error("❌ Failed to send order confirmation email to: {} for order: {}. Error: {}",
                    to, order.getOrderNumber(), e.getMessage());
        }
    }

    @Async
    @Override
    public void sendOrderShippedEmail(OrderDTO order, String trackingNumber) {
        if (order == null || order.getUserEmail() == null) {
            log.error("❌ Cannot send order shipped email: Order or user email is null");
            return;
        }

        String subject = "Your Order #" +
                Optional.ofNullable(order.getOrderNumber()).orElse("N/A") +
                " Has Shipped!";
        String to = order.getUserEmail();

        try {
            String htmlContent = buildOrderShippedHtml(order, trackingNumber);
            sendHtmlEmail(to, subject, htmlContent);
            log.info("✅ Order shipped email sent successfully to: {} for order: {}",
                    to, order.getOrderNumber());
        } catch (Exception e) {
            log.error("❌ Failed to send order shipped email to: {} for order: {}. Error: {}",
                    to, order.getOrderNumber(), e.getMessage());
        }
    }

    @Async
    @Override
    public void sendOrderDeliveredEmail(OrderDTO order) {
        if (order == null || order.getUserEmail() == null) {
            log.error("❌ Cannot send order delivered email: Order or user email is null");
            return;
        }

        String subject = "Your Order #" +
                Optional.ofNullable(order.getOrderNumber()).orElse("N/A") +
                " Has Been Delivered!";
        String to = order.getUserEmail();

        try {
            String htmlContent = buildOrderDeliveredHtml(order);
            sendHtmlEmail(to, subject, htmlContent);
            log.info("✅ Order delivered email sent successfully to: {} for order: {}",
                    to, order.getOrderNumber());
        } catch (Exception e) {
            log.error("❌ Failed to send order delivered email to: {} for order: {}. Error: {}",
                    to, order.getOrderNumber(), e.getMessage());
        }
    }

    @Override
    public void sendOrderConfirmation(String to, String orderNumber, String customerName) {
        String subject = "Order Confirmation #" + orderNumber + " - E-commerce Store";

        try {
            String htmlContent = buildSimpleOrderConfirmation(orderNumber, customerName);
            sendHtmlEmail(to, subject, htmlContent);
            log.info("✅ Simple order confirmation sent to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send simple order confirmation to: {}. Error: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendWelcomeEmail(String to, String customerName) {
        String subject = "Welcome to E-commerce Store!";

        try {
            String htmlContent = buildWelcomeEmailHtml(customerName);
            sendHtmlEmail(to, subject, htmlContent);
            log.info("✅ Welcome email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send welcome email to: {}. Error: {}", to, e.getMessage());
        }
    }

    // Private helper methods
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        log.debug("Attempting to send email to: {}, Subject: {}", to, subject);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(senderEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.debug("Email sent successfully to: {}", to);
    }

    private String buildVerificationEmailHtml(String verificationUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4F46E5; color: white; 
                             text-decoration: none; border-radius: 5px; font-weight: bold; }
                    .footer { margin-top: 30px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="color: #4F46E5;">Verify Your Email</h1>
                    </div>
                    
                    <p>Hello,</p>
                    <p>Thank you for registering with our e-commerce store! Please verify your email address by clicking the button below:</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" class="button">Verify Email Address</a>
                    </div>
                    
                    <p>If the button doesn't work, you can copy and paste this link into your browser:</p>
                    <p style="background-color: #eee; padding: 10px; border-radius: 5px; word-break: break-all;">
                        %s
                    </p>
                    
                    <p>This verification link will expire in 24 hours.</p>
                    
                    <p>If you didn't create an account with us, please ignore this email.</p>
                    
                    <div class="footer">
                        <p>Best regards,<br>The E-commerce Team</p>
                        <p>© %d E-commerce Store. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                verificationUrl,
                verificationUrl,
                java.time.Year.now().getValue()
        );
    }

    private String buildOrderConfirmationHtml(OrderDTO order) {
        String orderDate = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
                .format(order.getCreatedAt());
        String orderTime = DateTimeFormatter.ofPattern("hh:mm a")
                .format(order.getCreatedAt());

        // Build order items table
        StringBuilder orderItemsHtml = new StringBuilder();
        if (order.getOrderItems() != null) {
            for (OrderDTO.OrderItemDTO item : order.getOrderItems()) {
                String productImage = item.getProductImage() != null ?
                        item.getProductImage() : "https://picsum.photos/60/60?random=" + item.getId();
                String productName = item.getProductName() != null ? item.getProductName() : "Product";
                String sku = item.getSku() != null ? item.getSku() : "N/A";
                Integer quantity = item.getQuantity() != null ? item.getQuantity() : 1;
                Double price = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
                Double subtotal = item.getSubtotal() != null ? item.getSubtotal().doubleValue() : 0.0;

                orderItemsHtml.append(String.format("""
                <tr style="border-bottom: 1px solid #eee;">
                    <td style="padding: 15px; text-align: left;">
                        <div style="display: flex; align-items: center;">
                            <img src="%s" alt="%s" width="60" height="60" style="border-radius: 5px; margin-right: 10px; object-fit: cover;">
                            <div>
                                <strong>%s</strong><br>
                                <small style="color: #666;">SKU: %s</small>
                            </div>
                        </div>
                    </td>
                    <td style="padding: 15px; text-align: center;">%d</td>
                    <td style="padding: 15px; text-align: right;">$%.2f</td>
                    <td style="padding: 15px; text-align: right;">$%.2f</td>
                </tr>
                """,
                        productImage,
                        productName,
                        productName,
                        sku,
                        quantity,
                        price,
                        subtotal
                ));
            }
        }

        // Convert enums to strings
        String status = order.getStatus() != null ? order.getStatus().toString() : "PENDING";
        String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : "N/A";
        String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus().toString() : "PENDING";

        // Get address values (handle null addresses)
        AddressDTO shippingAddr = order.getShippingAddress();
        AddressDTO billingAddr = order.getBillingAddress();

        String shipRecipient = shippingAddr != null && shippingAddr.getRecipientName() != null ?
                shippingAddr.getRecipientName() : order.getUserName();
        String shipStreet = shippingAddr != null ? shippingAddr.getStreet() : "";
        String shipCity = shippingAddr != null ? shippingAddr.getCity() : "";
        String shipState = shippingAddr != null ? shippingAddr.getState() : "";
        String shipZip = shippingAddr != null ? shippingAddr.getZipCode() : "";
        String shipCountry = shippingAddr != null ? shippingAddr.getCountry() : "";
        String shipPhone = shippingAddr != null ? shippingAddr.getPhone() : "";

        String billRecipient = billingAddr != null && billingAddr.getRecipientName() != null ?
                billingAddr.getRecipientName() : order.getUserName();
        String billStreet = billingAddr != null ? billingAddr.getStreet() : "";
        String billCity = billingAddr != null ? billingAddr.getCity() : "";
        String billState = billingAddr != null ? billingAddr.getState() : "";
        String billZip = billingAddr != null ? billingAddr.getZipCode() : "";
        String billCountry = billingAddr != null ? billingAddr.getCountry() : "";
        String billPhone = billingAddr != null ? billingAddr.getPhone() : "";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Order Confirmation</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .order-info { background-color: #f8f9fa; border-radius: 5px; padding: 20px; margin-bottom: 20px; }
                    .address-section { display: flex; justify-content: space-between; margin-bottom: 30px; }
                    .address-box { flex: 1; margin-right: 20px; background-color: #f8f9fa; padding: 15px; border-radius: 5px; }
                    .address-box:last-child { margin-right: 0; }
                    .order-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .order-table th { background-color: #f8f9fa; padding: 15px; text-align: left; font-weight: bold; color: #333; }
                    .order-table td { padding: 15px; text-align: left; }
                    .summary { background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin-top: 20px; }
                    .summary-row { display: flex; justify-content: space-between; margin-bottom: 10px; }
                    .total-row { border-top: 2px solid #ddd; padding-top: 10px; margin-top: 10px; font-weight: bold; font-size: 1.1em; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4F46E5; color: white; 
                             text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 20px; }
                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 14px; margin-top: 30px; }
                    .status-badge { display: inline-block; padding: 5px 10px; border-radius: 20px; font-weight: bold; }
                    .status-pending { background-color: #fef3c7; color: #92400e; }
                    .status-confirmed { background-color: #d1fae5; color: #065f46; }
                    .status-processing { background-color: #dbeafe; color: #1e40af; }
                    .status-shipped { background-color: #ede9fe; color: #5b21b6; }
                    .status-delivered { background-color: #dcfce7; color: #166534; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin: 0;">🎉 Order Confirmed!</h1>
                        <p>Thank you for your purchase, %s!</p>
                    </div>
                    
                    <div class="content">
                        <div class="order-info">
                            <h2 style="margin-top: 0;">Order #%s</h2>
                            <p><strong>Order Date:</strong> %s at %s</p>
                            <p><strong>Status:</strong> <span class="status-badge status-%s">%s</span></p>
                            <p><strong>Payment Method:</strong> %s</p>
                            <p><strong>Payment Status:</strong> %s</p>
                        </div>
                        
                        <div class="address-section">
                            <div class="address-box">
                                <h3 style="margin-top: 0;">Shipping Address</h3>
                                <p>
                                    <strong>%s</strong><br>
                                    %s<br>
                                    %s, %s %s<br>
                                    %s<br>
                                    📞 %s
                                </p>
                            </div>
                            
                            <div class="address-box">
                                <h3 style="margin-top: 0;">Billing Address</h3>
                                <p>
                                    <strong>%s</strong><br>
                                    %s<br>
                                    %s, %s %s<br>
                                    %s<br>
                                    📞 %s
                                </p>
                            </div>
                        </div>
                        
                        <h3>Order Items</h3>
                        <table class="order-table">
                            <thead>
                                <tr>
                                    <th>Product</th>
                                    <th>Quantity</th>
                                    <th>Price</th>
                                    <th>Subtotal</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                        
                        <div class="summary">
                            <div class="summary-row">
                                <span>Subtotal:</span>
                                <span>$%.2f</span>
                            </div>
                            <div class="summary-row">
                                <span>Shipping:</span>
                                <span>$%.2f</span>
                            </div>
                            <div class="summary-row">
                                <span>Tax:</span>
                                <span>$%.2f</span>
                            </div>
                            <div class="summary-row">
                                <span>Discount:</span>
                                <span>-$%.2f</span>
                            </div>
                            <div class="summary-row total-row">
                                <span><strong>Total:</strong></span>
                                <span><strong>$%.2f</strong></span>
                            </div>
                        </div>
                        
                        <div style="text-align: center; margin-top: 30px;">
                            <a href="%s/orders/%d" class="button">View Order Details</a>
                            <p style="margin-top: 10px; color: #666; font-size: 14px;">
                                You can also track your order from your account dashboard.
                            </p>
                        </div>
                        
                        <div style="background-color: #f0f9ff; padding: 20px; border-radius: 5px; margin-top: 30px;">
                            <h3 style="margin-top: 0; color: #0369a1;">What's Next?</h3>
                            <p>1. We're processing your order and will notify you when it ships.</p>
                            <p>2. You'll receive tracking information once your order is on its way.</p>
                            <p>3. Estimated delivery: 3-5 business days.</p>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>Need help? Contact our customer support at support@ecommerce.com</p>
                        <p>© %d E-commerce Store. All rights reserved.</p>
                        <p style="font-size: 12px;">This is an automated email, please do not reply directly.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                // Header and order info
                order.getUserName() != null ? order.getUserName() : "Customer",
                order.getOrderNumber() != null ? order.getOrderNumber() : "N/A",
                orderDate,
                orderTime,
                status.toLowerCase(),
                status,
                paymentMethod,
                paymentStatus,

                // Shipping address
                shipRecipient,
                shipStreet,
                shipCity,
                shipState,
                shipZip,
                shipCountry,
                shipPhone,

                // Billing address
                billRecipient,
                billStreet,
                billCity,
                billState,
                billZip,
                billCountry,
                billPhone,

                // Order items
                orderItemsHtml.toString(),

                // Summary (handle null BigDecimals)
                order.getSubtotal() != null ? order.getSubtotal().doubleValue() : 0.0,
                order.getShippingAmount() != null ? order.getShippingAmount().doubleValue() : 0.0,
                order.getTaxAmount() != null ? order.getTaxAmount().doubleValue() : 0.0,
                order.getDiscountAmount() != null ? order.getDiscountAmount().doubleValue() : 0.0,
                order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0,

                // Frontend URL and year
                frontendUrl,
                order.getId(),
                java.time.Year.now().getValue()
        );
    }

    private String buildOrderShippedHtml(OrderDTO order, String trackingNumber) {
        if (order == null) return "";

        String orderDate = order.getCreatedAt() != null ?
                DateTimeFormatter.ofPattern("MMMM dd, yyyy").format(order.getCreatedAt()) : "N/A";

        AddressDTO shippingAddr = order.getShippingAddress();
        String shipStreet = shippingAddr != null ? shippingAddr.getStreet() : "";
        String shipCity = shippingAddr != null ? shippingAddr.getCity() : "";
        String shipState = shippingAddr != null ? shippingAddr.getState() : "";
        String shipZip = shippingAddr != null ? shippingAddr.getZipCode() : "";
        String shipCountry = shippingAddr != null ? shippingAddr.getCountry() : "";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; margin-bottom: 30px; background: linear-gradient(135deg, #10B981 0%%, #059669 100%%); 
                             color: white; padding: 30px; border-radius: 10px; }
                    .tracking-info { background-color: white; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #10B981; }
                    .tracking-number { font-size: 1.2em; font-weight: bold; color: #10B981; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #10B981; color: white; 
                             text-decoration: none; border-radius: 5px; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin: 0;">🚚 Your Order Has Shipped!</h1>
                    </div>
                    
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Great news! Your order <strong>#%s</strong> (placed on %s) has been shipped and is on its way to you!</p>
                    
                    <div class="tracking-info">
                        <h3 style="margin-top: 0;">Tracking Information</h3>
                        <p><strong>Tracking Number:</strong> <span class="tracking-number">%s</span></p>
                        <p>You can track your package using the link below:</p>
                    </div>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s/track-order?number=%s" class="button">Track Your Package</a>
                    </div>
                    
                    <p><strong>Delivery Address:</strong></p>
                    <p>
                        %s<br>
                        %s, %s %s<br>
                        %s
                    </p>
                    
                    <div style="background-color: #f0f9ff; padding: 20px; border-radius: 5px; margin-top: 20px;">
                        <h3 style="margin-top: 0; color: #0369a1;">Delivery Information</h3>
                        <p><strong>Estimated Delivery:</strong> 3-5 business days</p>
                        <p><strong>Carrier:</strong> Standard Shipping</p>
                        <p><strong>Note:</strong> Delivery times may vary based on your location.</p>
                    </div>
                    
                    <p>If you have any questions about your delivery, please contact our customer support team.</p>
                    
                    <p>Best regards,<br>The E-commerce Team</p>
                    
                    <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; text-align: center; color: #666; font-size: 12px;">
                        <p>© %d E-commerce Store. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                order.getUserName() != null ? order.getUserName() : "Customer",
                order.getOrderNumber() != null ? order.getOrderNumber() : "N/A",
                orderDate,
                trackingNumber != null ? trackingNumber : "N/A",
                frontendUrl,
                trackingNumber != null ? trackingNumber : "",
                shipStreet,
                shipCity,
                shipState,
                shipZip,
                shipCountry,
                java.time.Year.now().getValue()
        );
    }

    private String buildOrderDeliveredHtml(OrderDTO order) {
        if (order == null) return "";

        String orderDate = order.getCreatedAt() != null ?
                DateTimeFormatter.ofPattern("MMMM dd, yyyy").format(order.getCreatedAt()) : "N/A";

        AddressDTO shippingAddr = order.getShippingAddress();
        String shipCity = shippingAddr != null ? shippingAddr.getCity() : "";
        String shipState = shippingAddr != null ? shippingAddr.getState() : "";
        String shipCountry = shippingAddr != null ? shippingAddr.getCountry() : "";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; margin-bottom: 30px; background: linear-gradient(135deg, #8B5CF6 0%%, #7C3AED 100%%); 
                             color: white; padding: 30px; border-radius: 10px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #8B5CF6; color: white; 
                             text-decoration: none; border-radius: 5px; font-weight: bold; }
                    .rating { color: #F59E0B; font-size: 24px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin: 0;">🎊 Delivery Complete!</h1>
                        <p>Your order has been successfully delivered</p>
                    </div>
                    
                    <p>Hello <strong>%s</strong>,</p>
                    <p>We're excited to let you know that your order <strong>#%s</strong> has been delivered!</p>
                    
                    <div style="background-color: white; padding: 20px; border-radius: 5px; margin: 20px 0;">
                        <h3 style="margin-top: 0;">Order Details</h3>
                        <p><strong>Order Date:</strong> %s</p>
                        <p><strong>Order Total:</strong> $%.2f</p>
                        <p><strong>Delivery Address:</strong> %s, %s, %s</p>
                    </div>
                    
                    <p>We hope you're satisfied with your purchase! If you have a moment, we'd love to hear about your experience.</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s/orders/%d/rate" class="button">Leave a Review</a>
                    </div>
                    
                    <p>Or rate your purchase:</p>
                    <div class="rating" style="text-align: center;">
                        ★★★★★
                    </div>
                    
                    <div style="background-color: #f0f9ff; padding: 20px; border-radius: 5px; margin-top: 20px;">
                        <h3 style="margin-top: 0; color: #0369a1;">Need Help?</h3>
                        <p>If you have any issues with your order or need to initiate a return, please visit our 
                           <a href="%s/help">Help Center</a> or contact customer support.</p>
                    </div>
                    
                    <p>Thank you for shopping with us!</p>
                    <p>Best regards,<br>The E-commerce Team</p>
                    
                    <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; text-align: center; color: #666; font-size: 12px;">
                        <p>© %d E-commerce Store. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                order.getUserName() != null ? order.getUserName() : "Customer",
                order.getOrderNumber() != null ? order.getOrderNumber() : "N/A",
                orderDate,
                order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0,
                shipCity,
                shipState,
                shipCountry,
                frontendUrl,
                order.getId(),
                frontendUrl,
                java.time.Year.now().getValue()
        );
    }

    private String buildSimpleOrderConfirmation(String orderNumber, String customerName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4F46E5; color: white; 
                             text-decoration: none; border-radius: 5px; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="color: #4F46E5;">Order Confirmation</h1>
                    </div>
                    
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Thank you for your order! We have received your order <strong>#%s</strong> and are processing it.</p>
                    <p>You will receive another email with tracking information once your order ships.</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s/orders" class="button">View Your Orders</a>
                    </div>
                    
                    <p>If you have any questions about your order, please contact our customer support.</p>
                    
                    <p>Best regards,<br>The E-commerce Team</p>
                </div>
            </body>
            </html>
            """,
                customerName,
                orderNumber,
                frontendUrl
        );
    }

    private String buildPasswordResetEmailHtml(String resetUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #DC2626; color: white; 
                             text-decoration: none; border-radius: 5px; font-weight: bold; }
                    .footer { margin-top: 30px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="color: #DC2626;">Reset Your Password</h1>
                    </div>
                    
                    <p>Hello,</p>
                    <p>We received a request to reset your password for your E-commerce Store account.</p>
                    <p>Click the button below to create a new password:</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" class="button">Reset Password</a>
                    </div>
                    
                    <p>If the button doesn't work, copy and paste this link into your browser:</p>
                    <p style="background-color: #eee; padding: 10px; border-radius: 5px; word-break: break-all;">
                        %s
                    </p>
                    
                    <p><strong>Important:</strong> This password reset link will expire in 1 hour.</p>
                    <p>If you didn't request a password reset, you can safely ignore this email.</p>
                    <p>Your password will not be changed until you create a new one.</p>
                    
                    <div class="footer">
                        <p>Best regards,<br>The E-commerce Team</p>
                        <p>© %d E-commerce Store. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                resetUrl,
                resetUrl,
                java.time.Year.now().getValue()
        );
    }

    private String buildWelcomeEmailHtml(String customerName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #10B981; color: white; 
                             text-decoration: none; border-radius: 5px; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="color: #10B981;">Welcome to Our Store!</h1>
                    </div>
                    
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Welcome to our e-commerce family! We're excited to have you on board.</p>
                    
                    <p>With your new account, you can:</p>
                    <ul>
                        <li>Browse our wide selection of products</li>
                        <li>Save items to your wishlist</li>
                        <li>Track your orders</li>
                        <li>Write reviews</li>
                        <li>Enjoy exclusive member discounts</li>
                    </ul>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" class="button">Start Shopping Now</a>
                    </div>
                    
                    <p>If you have any questions, feel free to reply to this email or contact our support team.</p>
                    
                    <p>Happy shopping!<br>The E-commerce Team</p>
                </div>
            </body>
            </html>
            """,
                Optional.ofNullable(customerName).orElse("Customer"),
                frontendUrl
        );
    }
}