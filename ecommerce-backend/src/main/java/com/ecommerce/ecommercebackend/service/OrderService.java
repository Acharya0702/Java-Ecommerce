package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.AddressDTO;
import com.ecommerce.ecommercebackend.dto.OrderDTO;
import com.ecommerce.ecommercebackend.dto.OrderRequestDTO;
import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.exception.InsufficientStockException;
import com.ecommerce.ecommercebackend.exception.OrderCancellationException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.exception.UnauthorizedAccessException;
import com.ecommerce.ecommercebackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final EmailService emailService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10% tax

    @Transactional
    public OrderDTO createOrder(OrderRequestDTO request) {
        User currentUser = authService.getCurrentUser();
        log.info("=== STARTING ORDER CREATION for user: {} ===", currentUser.getId());

        // Get cart - FIXED: Now properly returns Cart entity
        Cart cart = cartRepository.findByUserIdWithItems(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + currentUser.getId()));

        // Get cart items
        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot create order with empty cart");
        }

        log.info("Found {} items in cart", cartItems.size());

        // Validate stock for all items before processing
        validateStock(cartItems);

        // Create order
        Order order = new Order();
        order.setUser(currentUser);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setNotes(request.getNotes());

        // Set addresses
        order.setShippingAddress(createAddress(request.getShippingAddress()));

        // Handle billing address
        if (request.getUseShippingForBilling() != null && request.getUseShippingForBilling()) {
            order.setBillingAddress(createAddress(request.getShippingAddress()));
        } else if (request.getBillingAddress() != null) {
            order.setBillingAddress(createAddress(request.getBillingAddress()));
        } else {
            order.setBillingAddress(createAddress(request.getShippingAddress()));
        }

        // Calculate amounts
        BigDecimal subtotal = calculateSubtotal(cartItems);
        BigDecimal taxAmount = calculateTax(subtotal);
        BigDecimal shippingAmount = calculateShipping(cartItems);

        order.setSubtotal(subtotal);
        order.setTaxAmount(taxAmount);
        order.setShippingAmount(shippingAmount);
        order.setDiscountAmount(BigDecimal.ZERO); // Initialize discount
        order.setTotalAmount(subtotal.add(taxAmount).add(shippingAmount));

        // Save order first to get ID
        order = orderRepository.save(order);
        log.info("Order saved with ID: {}, Number: {}", order.getId(), order.getOrderNumber());

        // Create order items and update stock
        List<OrderItem> orderItems = createOrderItems(order, cartItems);
        orderItems = orderItemRepository.saveAll(orderItems);

        // Add order items to order
        orderItems.forEach(order::addOrderItem);

        // Recalculate totals with all items
        order.calculateTotals();
        order = orderRepository.save(order);

        // Clear the cart
        clearCart(cart);

        log.info("Order created successfully: {}", order.getOrderNumber());

        // Create complete DTO
        OrderDTO orderDTO = createCompleteOrderDTO(order, orderItems);

        // Send order confirmation email asynchronously
        try {
            emailService.sendOrderConfirmation(orderDTO);
            log.info("Order confirmation email triggered for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order {}: {}",
                    order.getOrderNumber(), e.getMessage(), e);
            // Don't fail the order if email fails
        }

        return orderDTO;
    }

    private void validateStock(List<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        String.format("Insufficient stock for product: '%s'. Available: %d, Requested: %d",
                                product.getName(), product.getStockQuantity(), cartItem.getQuantity())
                );
            }
        }
        log.info("Stock validation passed for all items");
    }

    private BigDecimal calculateSubtotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        return subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateShipping(List<CartItem> cartItems) {
        // Simple shipping calculation - can be made more sophisticated
        int totalItems = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        if (totalItems == 0) return BigDecimal.ZERO;

        // Base shipping rate
        BigDecimal baseRate = new BigDecimal("5.00");

        // Additional per item rate
        BigDecimal perItemRate = new BigDecimal("0.50");
        BigDecimal additionalCharge = perItemRate.multiply(BigDecimal.valueOf(totalItems - 1));

        return baseRate.add(additionalCharge).setScale(2, RoundingMode.HALF_UP);
    }

    private List<OrderItem> createOrderItems(Order order, List<CartItem> cartItems) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setProductName(product.getName());
            orderItem.setProductImageUrl(product.getImageUrl());
            orderItem.setProductSku(product.getSku());
            orderItem.calculateSubtotal(); // This will set the subtotal

            orderItems.add(orderItem);

            // Update product stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            log.debug("Created order item for product: {}, quantity: {}",
                    product.getName(), cartItem.getQuantity());
        }

        return orderItems;
    }

    private void clearCart(Cart cart) {
        cart.clearCart(); // Using entity helper method
        cartRepository.save(cart);
        log.info("Cart cleared for user: {}", cart.getUser().getId());
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, Order.OrderStatus newStatus, String trackingNumber) {
        User currentUser = authService.getCurrentUser();

        // Check if user is admin
        if (!isAdmin(currentUser)) {
            throw new UnauthorizedAccessException("Only administrators can update order status");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Order.OrderStatus oldStatus = order.getStatus();

        // Update status based on the new status
        switch (newStatus) {
            case SHIPPED:
                order.markAsShipped(trackingNumber, "Standard Shipping");
                break;
            case DELIVERED:
                order.markAsDelivered();
                break;
            case CANCELLED:
                order.cancelOrder();
                break;
            default:
                order.setStatus(newStatus);
        }

        order = orderRepository.save(order);
        log.info("Order {} status updated from {} to {}",
                order.getOrderNumber(), oldStatus, newStatus);

        OrderDTO orderDTO = getOrderById(orderId);

        // Send email notifications
        try {
            if (newStatus == Order.OrderStatus.SHIPPED) {
                emailService.sendOrderShippedEmail(orderDTO, trackingNumber);
                log.info("Order shipped email sent for order: {}", order.getOrderNumber());
            } else if (newStatus == Order.OrderStatus.DELIVERED) {
                emailService.sendOrderDeliveredEmail(orderDTO);
                log.info("Order delivered email sent for order: {}", order.getOrderNumber());
            }
        } catch (Exception e) {
            log.error("Failed to send order status email for order {}: {}",
                    order.getOrderNumber(), e.getMessage(), e);
        }

        return orderDTO;
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrders() {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching orders for user: {}", currentUser.getId());

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
                    dto.setOrderItems(Collections.emptyList()); // Don't load items for list view
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching order: {} for user: {}", orderId, currentUser.getId());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Check authorization
        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new UnauthorizedAccessException("You don't have permission to view this order");
        }

        // Get order items with product details
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithProduct(orderId);

        return createCompleteOrderDTO(order, orderItems);
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderByNumber(String orderNumber) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching order by number: {} for user: {}", orderNumber, currentUser.getId());

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));

        // Check authorization
        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new UnauthorizedAccessException("You don't have permission to view this order");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithProduct(order.getId());

        return createCompleteOrderDTO(order, orderItems);
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        User currentUser = authService.getCurrentUser();
        log.info("Cancelling order: {} for user: {}", orderId, currentUser.getId());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Check authorization
        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new UnauthorizedAccessException("You don't have permission to cancel this order");
        }

        // Check if order can be cancelled
        if (!order.canBeCancelled()) {
            throw new OrderCancellationException(
                    String.format("Order %s cannot be cancelled at status: %s",
                            order.getOrderNumber(), order.getStatus())
            );
        }

        // Cancel order
        order.cancelOrder();

        // Restore product stock
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithProduct(orderId);
        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
                productRepository.save(product);
                log.debug("Restored stock for product: {}, quantity: {}",
                        product.getName(), orderItem.getQuantity());
            }
        }

        order = orderRepository.save(order);
        log.info("Order cancelled successfully: {}", order.getOrderNumber());

        return createCompleteOrderDTO(order, orderItems);
    }

    private OrderDTO createCompleteOrderDTO(Order order, List<OrderItem> orderItems) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUser().getId());
        dto.setUserName(order.getUser().getFullName());
        dto.setUserEmail(order.getUser().getEmail());
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
        }

        return dto;
    }

    private String generateOrderNumber() {
        String orderNumber;
        do {
            String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            orderNumber = "ORD-" + uuid + "-" + System.currentTimeMillis() % 10000;
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

    private boolean isAdmin(User user) {
        // Implement your admin check logic here
        // This depends on how you store roles in your User entity
        return user.getRole() != null && "ADMIN".equals(user.getRole().name());
    }
}