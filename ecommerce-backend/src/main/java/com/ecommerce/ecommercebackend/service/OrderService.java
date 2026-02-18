package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.AddressDTO;
import com.ecommerce.ecommercebackend.dto.OrderDTO;
import com.ecommerce.ecommercebackend.dto.OrderRequestDTO;
import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final OrderItemRepository orderItemRepository;
    private final EmailService emailService;  // Added email service

    @Transactional
    public OrderDTO createOrder(OrderRequestDTO request) {
        User currentUser = authService.getCurrentUser();
        log.info("=== STARTING ORDER CREATION ===");
        log.info("User: {}", currentUser.getEmail());
        log.info("User ID: {}", currentUser.getId());

        try {
            // Get user's cart
            Cart cart = cartRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Cart not found"));
            log.info("Cart found: ID={}, Total Items={}, Total Amount={}",
                    cart.getId(), cart.getTotalItems(), cart.getTotalAmount());

            // Get cart items with products
            List<CartItem> cartItems = cartItemRepository.findAllByCartIdWithProduct(cart.getId());
            log.info("Found {} cart items", cartItems.size());

            if (cartItems.isEmpty()) {
                throw new RuntimeException("Cart is empty");
            }

            // Log each cart item
            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();
                log.info("Cart Item: ID={}, Product ID={}, Product Name={}, Quantity={}, Price={}",
                        cartItem.getId(),
                        product != null ? product.getId() : "NULL",
                        product != null ? product.getName() : "NULL",
                        cartItem.getQuantity(),
                        cartItem.getPrice());

                if (product == null) {
                    throw new RuntimeException("Product not found in cart item ID: " + cartItem.getId());
                }
                if (product.getStockQuantity() < cartItem.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }
            }

            // Create order WITHOUT orderItems initially
            Order order = new Order();
            order.setUser(currentUser);
            order.setOrderNumber(generateOrderNumber());
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentMethod(request.getPaymentMethod());
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            order.setNotes(request.getNotes());

            // Set addresses
            order.setShippingAddress(createAddress(request.getShippingAddress()));

            if (request.getBillingAddress() != null) {
                order.setBillingAddress(createAddress(request.getBillingAddress()));
            } else {
                order.setBillingAddress(createAddress(request.getShippingAddress()));
            }

            // Calculate amounts
            BigDecimal subtotal = cart.getTotalAmount();
            BigDecimal shippingAmount = BigDecimal.ZERO; // Free shipping for now
            BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.10"));
            BigDecimal totalAmount = subtotal.add(shippingAmount).add(taxAmount);

            order.setSubtotal(subtotal);
            order.setShippingAmount(shippingAmount);
            order.setTaxAmount(taxAmount);
            order.setTotalAmount(totalAmount);

            // Save order first
            log.info("Saving order with number: {}", order.getOrderNumber());
            order = orderRepository.save(order);
            log.info("Order saved with ID: {}", order.getId());

            // Create and save order items
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();

                log.info("Creating order item for product ID: {}, Name: {}",
                        product.getId(), product.getName());

                // Create order item WITH product
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(cartItem.getPrice() != null ? cartItem.getPrice() : product.getDiscountedPrice());
                orderItem.setProductName(product.getName());
                orderItem.setProductImageUrl(product.getImageUrl());
                orderItem.setProductSku(product.getSku());
                orderItem.calculateSubtotal();

                log.info("Order item details: Quantity={}, Price={}, Subtotal={}, ProductSku={}",
                        orderItem.getQuantity(), orderItem.getPrice(), orderItem.getSubtotal(), orderItem.getProductSku());

                // Save order item
                try {
                    orderItem = orderItemRepository.save(orderItem);
                    log.info("Order item saved with ID: {}", orderItem.getId());
                    orderItems.add(orderItem);

                } catch (Exception e) {
                    log.error("Failed to save order item for product {}: {}", product.getId(), e.getMessage(), e);
                    throw new RuntimeException("Failed to save order item: " + e.getMessage());
                }

                // Update stock
                int newStock = product.getStockQuantity() - cartItem.getQuantity();
                log.info("Product {} stock update: {} - {} = {}",
                        product.getId(), product.getStockQuantity(), cartItem.getQuantity(), newStock);

                if (newStock < 0) {
                    throw new RuntimeException("Stock would go negative for product: " + product.getName());
                }
                product.setStockQuantity(newStock);
                productRepository.save(product);
                log.info("Stock updated for product {}", product.getId());
            }

            // Clear cart
            log.info("Clearing cart ID: {}", cart.getId());
            cartItemRepository.deleteByCartId(cart.getId());
            cart.setTotalAmount(BigDecimal.ZERO);
            cart.setTotalItems(0);
            cartRepository.save(cart);
            log.info("Cart cleared");

            log.info("Order created successfully: {}", order.getOrderNumber());

            // Create complete DTO for response
            OrderDTO createdOrder = createCompleteOrderDTO(order, orderItems);

            // Send order confirmation email (asynchronous)
            try {
                emailService.sendOrderConfirmation(createdOrder);
                log.info("Order confirmation email triggered for order: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Failed to send order confirmation email for order {}: {}",
                        order.getOrderNumber(), e.getMessage());
                // Don't fail the order if email fails
            }

            return createdOrder;

        } catch (Exception e) {
            log.error("=== ORDER CREATION FAILED ===");
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("Stack trace:", e);
            throw new RuntimeException("Failed to create order: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrders() {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching orders for user: {}", currentUser.getId());

        try {
            List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
            log.info("Found {} orders for user {}", orders.size(), currentUser.getId());

            return orders.stream()
                    .map(order -> {
                        OrderDTO dto = new OrderDTO();
                        dto.setId(order.getId());
                        dto.setOrderNumber(order.getOrderNumber());
                        dto.setUserId(order.getUser().getId());
                        dto.setUserName(order.getUser().getFullName());
                        dto.setUserEmail(order.getUser().getEmail());
                        dto.setTotalAmount(order.getTotalAmount());
                        dto.setStatus(order.getStatus());
                        dto.setPaymentMethod(order.getPaymentMethod());
                        dto.setPaymentStatus(order.getPaymentStatus());
                        dto.setCreatedAt(order.getCreatedAt());

                        // For list view, we don't need order items or addresses
                        dto.setOrderItems(Collections.emptyList());

                        return dto;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching user orders: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch orders: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching order: {} for user: {}", orderId, currentUser.getId());

        try {
            // Get order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            log.info("Order found: ID={}, Number={}", order.getId(), order.getOrderNumber());

            // Check if order belongs to current user
            if (!order.getUser().getId().equals(currentUser.getId())) {
                log.warn("Unauthorized access: Order {} belongs to user {}, requested by user {}",
                        orderId, order.getUser().getId(), currentUser.getId());
                throw new RuntimeException("Unauthorized access to order");
            }

            // Get order items
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
            log.info("Found {} order items for order {}", orderItems.size(), orderId);

            // Create complete DTO
            return createCompleteOrderDTO(order, orderItems);

        } catch (RuntimeException e) {
            log.error("Business error fetching order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch order: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderByNumber(String orderNumber) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching order by number: {} for user: {}", orderNumber, currentUser.getId());

        try {
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Check if order belongs to current user
            if (!order.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Unauthorized access to order");
            }

            // Get order items
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

            // Create complete DTO
            return createCompleteOrderDTO(order, orderItems);

        } catch (Exception e) {
            log.error("Error fetching order by number {}: {}", orderNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch order: " + e.getMessage());
        }
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, Order.OrderStatus newStatus, String trackingNumber) {
        User currentUser = authService.getCurrentUser();
        log.info("Updating order status: {} to {} for user: {}", orderId, newStatus, currentUser.getId());

        try {
            // Check if user is admin (you can implement proper role check)
            // For now, just log
            log.info("User role: {}", currentUser.getRole());

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            Order.OrderStatus oldStatus = order.getStatus();
            order.setStatus(newStatus);

            // Handle special status changes
            if (newStatus == Order.OrderStatus.SHIPPED) {
                if (trackingNumber != null) {
                    order.setTrackingNumber(trackingNumber);
                }
                order.setShippedAt(LocalDateTime.now());

                // Get order items
                List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
                OrderDTO orderDTO = createCompleteOrderDTO(order, orderItems);

                // Send shipped email
                try {
                    emailService.sendOrderShippedEmail(orderDTO, trackingNumber != null ? trackingNumber : "Not provided");
                    log.info("Order shipped email sent for order: {}", order.getOrderNumber());
                } catch (Exception e) {
                    log.error("Failed to send order shipped email for order {}: {}",
                            order.getOrderNumber(), e.getMessage());
                }
            }
            else if (newStatus == Order.OrderStatus.DELIVERED) {
                order.setDeliveredAt(LocalDateTime.now());

                // Get order items
                List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
                OrderDTO orderDTO = createCompleteOrderDTO(order, orderItems);

                // Send delivered email
                try {
                    emailService.sendOrderDeliveredEmail(orderDTO);
                    log.info("Order delivered email sent for order: {}", order.getOrderNumber());
                } catch (Exception e) {
                    log.error("Failed to send order delivered email for order {}: {}",
                            order.getOrderNumber(), e.getMessage());
                }
            }

            order = orderRepository.save(order);
            log.info("Order {} status updated from {} to {}",
                    order.getOrderNumber(), oldStatus, newStatus);

            // Get updated order items
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
            return createCompleteOrderDTO(order, orderItems);

        } catch (Exception e) {
            log.error("Error updating order status {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to update order status: " + e.getMessage());
        }
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        User currentUser = authService.getCurrentUser();
        log.info("Cancelling order: {} for user: {}", orderId, currentUser.getId());

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Check if order belongs to current user
            if (!order.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Unauthorized access to order");
            }

            // Check if order can be cancelled
            if (!order.canBeCancelled()) {
                throw new RuntimeException("Order cannot be cancelled at this stage");
            }

            // Cancel order
            order.cancelOrder();
            order = orderRepository.save(order);

            // Restore product stock
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
            for (OrderItem orderItem : orderItems) {
                Product product = orderItem.getProduct();
                if (product != null) {
                    product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
                    productRepository.save(product);
                }
            }

            log.info("Order cancelled: {}", order.getOrderNumber());

            return createCompleteOrderDTO(order, orderItems);

        } catch (Exception e) {
            log.error("Error cancelling order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to cancel order: " + e.getMessage());
        }
    }

    private OrderDTO createCompleteOrderDTO(Order order, List<OrderItem> orderItems) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUser().getId());
        dto.setUserName(order.getUser().getFullName());
        dto.setUserEmail(order.getUser().getEmail()); // Important for email
        dto.setTotalAmount(order.getTotalAmount());
        dto.setSubtotal(order.getSubtotal());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setShippingAmount(order.getShippingAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setStatus(order.getStatus());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setShippingMethod(order.getShippingMethod());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setShippedAt(order.getShippedAt());
        dto.setDeliveredAt(order.getDeliveredAt());

        // Convert addresses
        if (order.getShippingAddress() != null) {
            dto.setShippingAddress(convertToAddressDTO(order.getShippingAddress()));
        }

        if (order.getBillingAddress() != null) {
            dto.setBillingAddress(convertToAddressDTO(order.getBillingAddress()));
        }

        // Convert order items
        if (orderItems != null && !orderItems.isEmpty()) {
            List<OrderDTO.OrderItemDTO> itemDTOs = orderItems.stream()
                    .map(item -> {
                        OrderDTO.OrderItemDTO itemDTO = new OrderDTO.OrderItemDTO();
                        itemDTO.setId(item.getId());
                        itemDTO.setProductName(item.getProductName());
                        itemDTO.setProductImage(item.getProductImageUrl());
                        itemDTO.setSku(item.getProductSku());
                        itemDTO.setPrice(item.getPrice());
                        itemDTO.setQuantity(item.getQuantity());
                        itemDTO.setSubtotal(item.getSubtotal());
                        return itemDTO;
                    })
                    .collect(Collectors.toList());
            dto.setOrderItems(itemDTOs);
        } else {
            dto.setOrderItems(Collections.emptyList());
        }

        return dto;
    }

    private String generateOrderNumber() {
        String orderNumber;
        do {
            orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    private Order.Address createAddress(AddressDTO addressDTO) {
        if (addressDTO == null) return null;

        Order.Address address = new Order.Address();
        address.setStreet(addressDTO.getStreet());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setZipCode(addressDTO.getZipCode());
        address.setCountry(addressDTO.getCountry());
        address.setPhone(addressDTO.getPhone());
        address.setRecipientName(addressDTO.getRecipientName());
        return address;
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
}