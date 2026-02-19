package com.ecommerce.ecommercebackend.dto;

import com.ecommerce.ecommercebackend.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderDTO {

    // Basic order information
    private Long id;
    private String orderNumber;

    // User information
    private Long userId;
    private String userName;
    private String userEmail;  // Important for email notifications

    // Order items
    private List<OrderItemDTO> orderItems = new ArrayList<>();

    // Amount calculations
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;

    // Addresses
    private AddressDTO shippingAddress;
    private AddressDTO billingAddress;

    // Order status and payment
    private Order.OrderStatus status;
    private Order.PaymentMethod paymentMethod;
    private Order.PaymentStatus paymentStatus;

    // Shipping tracking
    private String trackingNumber;
    private String shippingMethod;

    // Additional information
    private String notes;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    // Calculated fields for UI convenience
    private Integer totalItems;  // Total number of items in order
    private String statusDisplay; // Formatted status for display
    private String paymentMethodDisplay; // Formatted payment method for display
    private String formattedTotal; // Formatted total amount with currency symbol
    private String formattedDate; // Formatted order date
    private boolean canBeCancelled; // Whether order can be cancelled by user
    private boolean canBeReviewed; // Whether user can leave reviews for items

    @Data
    public static class OrderItemDTO {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private String sku;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;

        // Additional fields for display
        private String formattedPrice;
        private String formattedSubtotal;
        private boolean hasDiscount;
        private BigDecimal originalPrice;
        private BigDecimal discountPercentage;
    }

    // Helper methods to set derived fields
    public void calculateDerivedFields() {
        // Calculate total items
        if (orderItems != null) {
            this.totalItems = orderItems.stream()
                    .mapToInt(OrderItemDTO::getQuantity)
                    .sum();
        }

        // Format status for display
        if (status != null) {
            this.statusDisplay = status.toString().replace("_", " ").toLowerCase();
            this.statusDisplay = this.statusDisplay.substring(0, 1).toUpperCase() +
                    this.statusDisplay.substring(1);
        }

        // Format payment method for display
        if (paymentMethod != null) {
            this.paymentMethodDisplay = paymentMethod.toString().replace("_", " ").toLowerCase();
            this.paymentMethodDisplay = this.paymentMethodDisplay.substring(0, 1).toUpperCase() +
                    this.paymentMethodDisplay.substring(1);
        }

        // Format total with currency symbol
        if (totalAmount != null) {
            this.formattedTotal = String.format("$%.2f", totalAmount);
        }

        // Format date
        if (createdAt != null) {
            this.formattedDate = createdAt.format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"));
        }

        // Check if order can be cancelled
        this.canBeCancelled = status == Order.OrderStatus.PENDING ||
                status == Order.OrderStatus.PROCESSING ||
                status == Order.OrderStatus.CONFIRMED;

        // Check if order can be reviewed (delivered orders)
        this.canBeReviewed = status == Order.OrderStatus.DELIVERED;

        // Format item prices
        if (orderItems != null) {
            orderItems.forEach(item -> {
                if (item.getPrice() != null) {
                    item.setFormattedPrice(String.format("$%.2f", item.getPrice()));
                }
                if (item.getSubtotal() != null) {
                    item.setFormattedSubtotal(String.format("$%.2f", item.getSubtotal()));
                }
            });
        }
    }

    // Helper method to get order summary for emails
    public String getEmailSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Order #%s - Total: $%.2f", orderNumber, totalAmount));

        if (orderItems != null && !orderItems.isEmpty()) {
            summary.append(" - Items: ");
            for (int i = 0; i < Math.min(orderItems.size(), 3); i++) {
                if (i > 0) summary.append(", ");
                summary.append(orderItems.get(i).getQuantity())
                        .append("x ")
                        .append(orderItems.get(i).getProductName());
            }
            if (orderItems.size() > 3) {
                summary.append(" and ").append(orderItems.size() - 3).append(" more");
            }
        }

        return summary.toString();
    }

    // Helper method to get shipping address as formatted string
    public String getFormattedShippingAddress() {
        if (shippingAddress == null) return "No shipping address provided";

        return String.format("%s, %s, %s %s, %s - Phone: %s",
                shippingAddress.getStreet(),
                shippingAddress.getCity(),
                shippingAddress.getState(),
                shippingAddress.getZipCode(),
                shippingAddress.getCountry(),
                shippingAddress.getPhone()
        );
    }

    // Helper method to get billing address as formatted string
    public String getFormattedBillingAddress() {
        if (billingAddress == null) return "No billing address provided";

        return String.format("%s, %s, %s %s, %s - Phone: %s",
                billingAddress.getStreet(),
                billingAddress.getCity(),
                billingAddress.getState(),
                billingAddress.getZipCode(),
                billingAddress.getCountry(),
                billingAddress.getPhone()
        );
    }

    // Helper method to check if shipping and billing addresses are the same
    public boolean isShippingAndBillingSame() {
        if (shippingAddress == null || billingAddress == null) return false;

        return shippingAddress.getStreet().equals(billingAddress.getStreet()) &&
                shippingAddress.getCity().equals(billingAddress.getCity()) &&
                shippingAddress.getState().equals(billingAddress.getState()) &&
                shippingAddress.getZipCode().equals(billingAddress.getZipCode()) &&
                shippingAddress.getCountry().equals(billingAddress.getCountry());
    }

    // Helper method to get status badge color for UI
    public String getStatusBadgeColor() {
        if (status == null) return "gray";

        return switch (status) {
            case PENDING, PROCESSING, CONFIRMED -> "yellow";
            case SHIPPED -> "blue";
            case DELIVERED -> "green";
            case CANCELLED -> "red";
            case REFUNDED -> "purple";
            case ON_HOLD -> "orange";
        };
    }

    // Helper method to get payment status badge color for UI
    public String getPaymentStatusBadgeColor() {
        if (paymentStatus == null) return "gray";

        return switch (paymentStatus) {
            case PENDING -> "yellow";
            case AUTHORIZED -> "blue";
            case PAID -> "green";
            case FAILED -> "red";
            case REFUNDED, PARTIALLY_REFUNDED -> "purple";
        };
    }

    // Helper method to get order progress percentage for tracking
    public int getOrderProgressPercentage() {
        if (status == null) return 0;

        return switch (status) {
            case PENDING -> 10;
            case PROCESSING -> 30;
            case CONFIRMED -> 50;
            case SHIPPED -> 75;
            case DELIVERED -> 100;
            default -> 0;
        };
    }

    // Helper method to get estimated delivery date (example: 5-7 business days from order date)
    public String getEstimatedDeliveryDate() {
        if (createdAt == null) return "Not available";

        if (status == Order.OrderStatus.DELIVERED && deliveredAt != null) {
            return "Delivered on " + deliveredAt.format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        }

        if (status == Order.OrderStatus.SHIPPED && shippedAt != null) {
            // Estimate 3-5 business days from ship date
            return "Estimated within 3-5 business days";
        }

        // Estimate 5-7 business days from order date
        return "Estimated within 5-7 business days";
    }
}