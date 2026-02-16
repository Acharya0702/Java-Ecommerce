package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.AddressDTO;
import com.ecommerce.ecommercebackend.dto.OrderDTO;
import com.ecommerce.ecommercebackend.dto.OrderItemDTO;
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
    private static final BigDecimal SHIPPING_BASE_RATE = new BigDecimal("5.00");
    private static final BigDecimal SHIPPING_PER_ITEM_RATE = new BigDecimal("0.50");

    @Transactional
    public OrderDTO createOrder(OrderRequestDTO request) {
        User currentUser = authService.getCurrentUser();
        log.info("=== STARTING ORDER CREATION for user: {} ===", currentUser.getId());

        try {
            // Step 1: Get cart with items
            log.debug("Step 1: Fetching cart for user: {}", currentUser.getId());
            Cart cart = cartRepository.findByUserIdWithItems(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + currentUser.getId()));

            // Force initialization of cart items to avoid LazyInitializationException
            Set<CartItem> cartItemsSet = cart.getCartItems();
            List<CartItem> cartItems = new ArrayList<>(cartItemsSet);

            if (cartItems.isEmpty()) {
                throw new IllegalStateException("Cannot create order with empty cart");
            }
            log.info("Found {} items in cart", cartItems.size());

            // Log cart items for debugging
            for (CartItem item : cartItems) {
                log.debug("Cart Item - Product: {}, Quantity: {}, Price: {}",
                        item.getProduct().getName(), item.getQuantity(), item.getPrice());
            }

            // Step 2: Validate stock
            log.debug("Step 2: Validating stock");
            validateStock(cartItems);

            // Step 3: Create order
            log.debug("Step 3: Creating order object");
            Order order = buildOrder(currentUser, request);

            // Step 4: Calculate amounts
            log.debug("Step 4: Calculating amounts");
            BigDecimal subtotal = calculateSubtotal(cartItems);
            BigDecimal taxAmount = calculateTax(subtotal);
            BigDecimal shippingAmount = calculateShipping(cartItems);

            order.setSubtotal(subtotal);
            order.setTaxAmount(taxAmount);
            order.setShippingAmount(shippingAmount);
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setTotalAmount(subtotal.add(taxAmount).add(shippingAmount));

            // Step 5: Save order
            log.debug("Step 5: Saving order to database");
            order = orderRepository.save(order);
            log.info("Order saved with ID: {}, Number: {}", order.getId(), order.getOrderNumber());

            // Step 6: Create order items and update stock
            log.debug("Step 6: Creating order items");
            List<OrderItem> orderItems = createOrderItems(order, cartItems);
            orderItems = orderItemRepository.saveAll(orderItems);
            log.debug("Created {} order items", orderItems.size());

            // Step 7: Add items to order and recalculate
            log.debug("Step 7: Adding items to order");
            orderItems.forEach(order::addOrderItem);
            order.calculateTotals();
            order = orderRepository.save(order);

            // Step 8: Clear cart
            log.debug("Step 8: Clearing cart");
            clearCart(cart);

            log.info("Order created successfully: {}", order.getOrderNumber());

            // Step 9: Create DTO
            OrderDTO orderDTO = createCompleteOrderDTO(order, orderItems);

            // Step 10: Send confirmation email (don't fail if email fails)
            try {
                emailService.sendOrderConfirmation(orderDTO);
                log.info("Order confirmation email triggered for order: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Failed to send order confirmation email for order {}: {}",
                        order.getOrderNumber(), e.getMessage());
            }

            return orderDTO;

        } catch (Exception e) {
            log.error("Order creation failed at step: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Order buildOrder(User user, OrderRequestDTO request) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setNotes(request.getNotes());

        // Set shipping address
        order.setShippingAddress(createAddress(request.getShippingAddress()));

        // Set billing address
        if (request.getUseShippingForBilling() != null && request.getUseShippingForBilling()) {
            order.setBillingAddress(createAddress(request.getShippingAddress()));
        } else if (request.getBillingAddress() != null) {
            order.setBillingAddress(createAddress(request.getBillingAddress()));
        } else {
            order.setBillingAddress(createAddress(request.getShippingAddress()));
        }

        return order;
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
        int totalItems = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        if (totalItems == 0) return BigDecimal.ZERO;

        BigDecimal additionalCharge = SHIPPING_PER_ITEM_RATE.multiply(BigDecimal.valueOf(Math.max(0, totalItems - 1)));
        return SHIPPING_BASE_RATE.add(additionalCharge).setScale(2, RoundingMode.HALF_UP);
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
            orderItem.calculateSubtotal();

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
        cart.getCartItems().clear();
        cart.setTotalItems(0);
        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);
        log.info("Cart cleared for user: {}", cart.getUser().getId());
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, Order.OrderStatus newStatus, String trackingNumber) {
        User currentUser = authService.getCurrentUser();

        if (!isAdmin(currentUser)) {
            throw new UnauthorizedAccessException("Only administrators can update order status");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Order.OrderStatus oldStatus = order.getStatus();

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
        log.info("Order {} status updated from {} to {}", order.getOrderNumber(), oldStatus, newStatus);

        OrderDTO orderDTO = getOrderById(orderId);

        try {
            if (newStatus == Order.OrderStatus.SHIPPED) {
                emailService.sendOrderShippedEmail(orderDTO, trackingNumber);
                log.info("Order shipped email sent for order: {}", order.getOrderNumber());
            } else if (newStatus == Order.OrderStatus.DELIVERED) {
                emailService.sendOrderDeliveredEmail(orderDTO);
                log.info("Order delivered email sent for order: {}", order.getOrderNumber());
            }
        } catch (Exception e) {
            log.error("Failed to send order status email for order {}: {}", order.getOrderNumber(), e.getMessage());
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
                .map(this::convertToMinimalDTO)
                .collect(Collectors.toList());
    }

    private OrderDTO convertToMinimalDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUser().getId());
        dto.setUserName(order.getUser().getFullName());
        dto.setUserEmail(order.getUser().getEmail());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus() != null ? order.getStatus().toString() : "PENDING");
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : null);
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().toString() : "PENDING");
        dto.setCreatedAt(order.getCreatedAt());
        dto.setOrderItems(Collections.emptyList());
        return dto;
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching order: {} for user: {}", orderId, currentUser.getId());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new UnauthorizedAccessException("You don't have permission to view this order");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithProduct(orderId);
        return createCompleteOrderDTO(order, orderItems);
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderByNumber(String orderNumber) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching order by number: {} for user: {}", orderNumber, currentUser.getId());

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));

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

        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new UnauthorizedAccessException("You don't have permission to cancel this order");
        }

        if (!order.canBeCancelled()) {
            throw new OrderCancellationException(
                    String.format("Order %s cannot be cancelled at status: %s",
                            order.getOrderNumber(), order.getStatus())
            );
        }

        order.cancelOrder();

        // Restore product stock
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithProduct(orderId);
        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
                productRepository.save(product);
                log.debug("Restored stock for product: {}, quantity: {}", product.getName(), orderItem.getQuantity());
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

        // Convert enums to strings to avoid serialization issues
        dto.setStatus(order.getStatus() != null ? order.getStatus().toString() : "PENDING");
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : null);
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().toString() : "PENDING");

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
            List<OrderItemDTO> itemDTOs = orderItems.stream()
                    .map(this::convertToOrderItemDTO)
                    .collect(Collectors.toList());
            dto.setOrderItems(itemDTOs);
        }

        return dto;
    }

    private OrderItemDTO convertToOrderItemDTO(OrderItem item) {
        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setId(item.getId());
        itemDTO.setProductName(item.getProductName());
        itemDTO.setProductImage(item.getProductImageUrl());
        itemDTO.setSku(item.getProductSku());
        itemDTO.setPrice(item.getPrice());
        itemDTO.setQuantity(item.getQuantity());
        itemDTO.setSubtotal(item.getSubtotal());
        return itemDTO;
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
        return user.getRole() != null && User.Role.ADMIN.equals(user.getRole());
    }
}