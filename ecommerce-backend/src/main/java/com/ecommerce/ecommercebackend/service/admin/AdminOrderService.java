package com.ecommerce.ecommercebackend.service.admin;

import com.ecommerce.ecommercebackend.dto.OrderDTO;
import com.ecommerce.ecommercebackend.dto.OrderItemDTO;
import com.ecommerce.ecommercebackend.dto.AddressDTO;
import com.ecommerce.ecommercebackend.dto.admin.OrderUpdateDTO;
import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.OrderItem;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.repository.OrderItemRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;

    // ============= READ OPERATIONS =============
    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(Pageable pageable, String status, String search) {
        Order.OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", status);
            }
        }

        Page<Order> orders = orderRepository.findOrdersWithFilters(orderStatus, search, pageable);
        return orders.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderDetails(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return convertToDTO(order);
    }

    // ============= UPDATE OPERATIONS =============
    public OrderDTO updateOrderStatus(Long id, OrderUpdateDTO updateDTO) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(updateDTO.getStatus());

        if (updateDTO.getTrackingNumber() != null) {
            order.setTrackingNumber(updateDTO.getTrackingNumber());
        }
        if (updateDTO.getShippingMethod() != null) {
            order.setShippingMethod(updateDTO.getShippingMethod());
        }
        if (updateDTO.getNotes() != null) {
            order.setNotes(updateDTO.getNotes());
        }

        // Handle status-specific logic
        switch (updateDTO.getStatus()) {
            case PROCESSING:
                log.info("Order {} is now processing", order.getOrderNumber());
                break;

            case SHIPPED:
                if (order.getShippedAt() == null) {
                    order.setShippedAt(LocalDateTime.now());
                }
                // Send shipping notification email
                try {
                    emailService.sendOrderShippedEmail(convertToDTO(order), order.getTrackingNumber());
                    log.info("Shipping email sent for order: {}", order.getOrderNumber());
                } catch (Exception e) {
                    log.error("Failed to send shipping email: {}", e.getMessage());
                }
                break;

            case DELIVERED:
                if (order.getDeliveredAt() == null) {
                    order.setDeliveredAt(LocalDateTime.now());
                }
                // Send delivery confirmation email
                try {
                    emailService.sendOrderDeliveredEmail(convertToDTO(order));
                    log.info("Delivery email sent for order: {}", order.getOrderNumber());
                } catch (Exception e) {
                    log.error("Failed to send delivery email: {}", e.getMessage());
                }
                break;

            case CANCELLED:
                if (order.getCancelledAt() == null) {
                    order.setCancelledAt(LocalDateTime.now());
                }
                // Restore stock for cancelled orders
                restoreStock(order);
                log.info("Stock restored for cancelled order: {}", order.getOrderNumber());
                break;

            case REFUNDED:
                order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
                break;
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated from {} to {}",
                order.getOrderNumber(), oldStatus, updateDTO.getStatus());

        return convertToDTO(updatedOrder);
    }

    public OrderDTO processPayment(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setStatus(Order.OrderStatus.PROCESSING);

        Order updatedOrder = orderRepository.save(order);
        log.info("Payment processed for order: {}", order.getOrderNumber());

        return convertToDTO(updatedOrder);
    }

    public List<OrderDTO> bulkUpdateOrderStatus(List<Long> orderIds, String status) {
        Order.OrderStatus newStatus;
        try {
            newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        List<Order> orders = orderRepository.findAllById(orderIds);
        orders.forEach(order -> order.setStatus(newStatus));

        List<Order> updatedOrders = orderRepository.saveAll(orders);
        log.info("Bulk updated {} orders to status: {}", updatedOrders.size(), status);

        return updatedOrders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ============= HELPER METHODS =============
    private void restoreStock(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            Product product = item.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
                log.debug("Restored {} units of product {}", item.getQuantity(), product.getId());
            }
        }
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();

        // Basic order info
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());

        // User info
        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getId());
            dto.setUserName(order.getUser().getFullName());
            dto.setUserEmail(order.getUser().getEmail());
        }

        // Amounts
        dto.setTotalAmount(order.getTotalAmount());
        dto.setSubtotal(order.getSubtotal());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setShippingAmount(order.getShippingAmount());
        dto.setDiscountAmount(order.getDiscountAmount());

        // Addresses
        if (order.getShippingAddress() != null) {
            dto.setShippingAddress(convertToAddressDTO(order.getShippingAddress()));
        }
        if (order.getBillingAddress() != null) {
            dto.setBillingAddress(convertToAddressDTO(order.getBillingAddress()));
        }

        // Status and payment
        dto.setStatus(order.getStatus());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setShippingMethod(order.getShippingMethod());
        dto.setNotes(order.getNotes());

        // Timestamps
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setShippedAt(order.getShippedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setCancelledAt(order.getCancelledAt());

        // Order items
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            List<OrderDTO.OrderItemDTO> itemDTOs = order.getOrderItems().stream()
                    .map(this::convertToItemDTO)
                    .collect(Collectors.toList());
            dto.setOrderItems(itemDTOs);

            // Calculate total items
            int totalItems = order.getOrderItems().stream()
                    .mapToInt(OrderItem::getQuantity)
                    .sum();
            dto.setTotalItems(totalItems);
        }

        // Calculate derived fields
        dto.calculateDerivedFields();

        return dto;
    }

    private OrderDTO.OrderItemDTO convertToItemDTO(OrderItem item) {
        OrderDTO.OrderItemDTO dto = new OrderDTO.OrderItemDTO();

        dto.setId(item.getId());
        dto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
        dto.setProductName(item.getProductName());
        dto.setSku(item.getProductSku());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setSubtotal(item.getSubtotal());
        dto.setProductImage(item.getProductImageUrl());

        // Format prices
        if (item.getPrice() != null) {
            dto.setFormattedPrice(String.format("$%.2f", item.getPrice()));
        }
        if (item.getSubtotal() != null) {
            dto.setFormattedSubtotal(String.format("$%.2f", item.getSubtotal()));
        }

        // Check if product has discount
        if (item.getProduct() != null && item.getProduct().getDiscountPrice() != null) {
            dto.setHasDiscount(true);
            dto.setOriginalPrice(item.getProduct().getPrice());
            dto.setDiscountPercentage(item.getProduct().getDiscountPercentage());
        }

        return dto;
    }

    private AddressDTO convertToAddressDTO(Order.Address address) {
        if (address == null) return null;

        AddressDTO dto = new AddressDTO();
        dto.setStreet(address.getStreet());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setZipCode(address.getZipCode());
        dto.setCountry(address.getCountry());
        dto.setPhone(address.getPhone());
        dto.setRecipientName(address.getRecipientName());
        return dto;
    }

    // ============= ADDITIONAL ADMIN METHODS =============

    @Transactional(readOnly = true)
    public List<OrderDTO> getRecentOrders(int limit) {
        return orderRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::convertToDTO)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalOrders", orderRepository.count());
        stats.put("totalRevenue", orderRepository.getTotalRevenue());

        Map<String, Long> statusCounts = new HashMap<>();
        for (Order.OrderStatus status : Order.OrderStatus.values()) {
            long count = orderRepository.countByStatus(status);
            if (count > 0) {
                statusCounts.put(status.toString(), count);
            }
        }
        stats.put("ordersByStatus", statusCounts);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrdersByDateRange(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        return orders.stream()
                .map(order -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", order.getId());
                    map.put("orderNumber", order.getOrderNumber());
                    map.put("date", order.getCreatedAt().format(DateTimeFormatter.ISO_DATE));
                    map.put("total", order.getTotalAmount());
                    map.put("status", order.getStatus());
                    return map;
                })
                .collect(Collectors.toList());
    }
}