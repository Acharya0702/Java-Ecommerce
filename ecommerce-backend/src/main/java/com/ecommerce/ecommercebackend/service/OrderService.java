package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.AddressDTO;
import com.ecommerce.ecommercebackend.dto.OrderDTO;
import com.ecommerce.ecommercebackend.dto.OrderRequestDTO;
import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Transactional
    public OrderDTO createOrder(OrderRequestDTO request) {
        User currentUser = authService.getCurrentUser();
        log.info("=== STARTING ORDER CREATION ===");
        log.info("User: {}", currentUser.getEmail());

        try {
            // Get user's cart
            Cart cart = cartRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Cart not found"));

            // Get cart items with products
            List<CartItem> cartItems = cartItemRepository.findAllByCartIdWithProduct(cart.getId());
            if (cartItems.isEmpty()) {
                throw new RuntimeException("Cart is empty");
            }

            // Validate stock
            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();
                if (product.getStockQuantity() < cartItem.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }
            }

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
            order.setBillingAddress(request.getBillingAddress() != null ?
                    createAddress(request.getBillingAddress()) :
                    createAddress(request.getShippingAddress()));

            // Calculate amounts
            BigDecimal subtotal = cart.getTotalAmount();
            BigDecimal shippingAmount = BigDecimal.ZERO;
            BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.10"));
            BigDecimal totalAmount = subtotal.add(shippingAmount).add(taxAmount);

            order.setSubtotal(subtotal);
            order.setShippingAmount(shippingAmount);
            order.setTaxAmount(taxAmount);
            order.setTotalAmount(totalAmount);

            // Save order first
            log.info("Saving order...");
            order = orderRepository.save(order);
            log.info("Order saved with ID: {}", order.getId());

            // Create and save order items
            List<OrderItem> savedOrderItems = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(cartItem.getPrice() != null ? cartItem.getPrice() : product.getDiscountedPrice());
                orderItem.setProductName(product.getName());
                orderItem.setProductImageUrl(product.getImageUrl());
                orderItem.setProductSku(product.getSku());
                orderItem.calculateSubtotal();

                orderItem = orderItemRepository.save(orderItem);
                savedOrderItems.add(orderItem);

                // Update stock
                product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
                productRepository.save(product);
            }

            // Clear cart
            cartItemRepository.deleteByCartId(cart.getId());
            cart.setTotalAmount(BigDecimal.ZERO);
            cart.setTotalItems(0);
            cartRepository.save(cart);

            log.info("Order created successfully: {}", order.getOrderNumber());

            // Build DTO
            OrderDTO orderDTO = buildOrderDTO(order, savedOrderItems);

            // 🔥 SEND ORDER CONFIRMATION EMAIL (ASYNC) 🔥
            try {
                log.info("Attempting to send order confirmation email to: {}", currentUser.getEmail());
                emailService.sendOrderConfirmation(orderDTO);
                log.info("Order confirmation email sent successfully to: {}", currentUser.getEmail());
            } catch (Exception e) {
                // Don't fail the order if email fails, just log it
                log.error("Failed to send order confirmation email: {}", e.getMessage(), e);
            }

            return orderDTO;

        } catch (Exception e) {
            log.error("Order creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create order: " + e.getMessage());
        }
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

    private OrderDTO createOrderDTO(Order order, Set<OrderItem> orderItems) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUser().getId());
        dto.setUserName(order.getUser().getFullName());
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

        // Convert shipping address
        if (order.getShippingAddress() != null) {
            dto.setShippingAddress(convertToAddressDTO(order.getShippingAddress()));
        }

        // Convert billing address
        if (order.getBillingAddress() != null) {
            dto.setBillingAddress(convertToAddressDTO(order.getBillingAddress()));
        }

        // Convert order items - WITHOUT accessing product relationships
        if (orderItems != null && !orderItems.isEmpty()) {
            List<OrderDTO.OrderItemDTO> orderItemDTOs = orderItems.stream()
                    .map(this::convertToOrderItemDTO)
                    .collect(Collectors.toList());
            dto.setOrderItems(orderItemDTOs);
        } else {
            dto.setOrderItems(Collections.emptyList());
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching order: {} for user: {}", orderId, currentUser.getId());

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

            // Check if order belongs to current user
            if (!order.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Unauthorized access to order");
            }

            // Explicitly fetch order items with products
            List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(order.getId());
            log.info("Found {} items for order", items.size());

            // Build DTO manually
            return buildOrderDTO(order, items);

        } catch (RuntimeException e) {
            log.error("Error fetching order: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching order: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching order: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrders() {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching orders for user: {}", currentUser.getId());

        try {
            List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
            log.info("Found {} orders for user", orders.size());

            List<OrderDTO> orderDTOs = new ArrayList<>();
            for (Order order : orders) {
                // For each order, explicitly fetch its items
                List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(order.getId());
                log.info("Order {} has {} items", order.getOrderNumber(), items.size());

                // Build DTO manually
                OrderDTO dto = buildOrderDTO(order, items);
                orderDTOs.add(dto);
            }

            return orderDTOs;

        } catch (Exception e) {
            log.error("Error fetching user orders: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }


    @Transactional(readOnly = true)
    public OrderDTO getOrderByNumber(String orderNumber) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching order by number: {} for user: {}", orderNumber, currentUser.getId());

        try {
            // First get the order
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new RuntimeException("Order not found with number: " + orderNumber));

            log.info("Order found: ID={}, UserID={}, CurrentUserID={}",
                    order.getId(), order.getUser().getId(), currentUser.getId());

            // Check authorization
            if (!order.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Unauthorized access to order");
            }

            // EXPLICITLY fetch order items with products
            List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(order.getId());
            log.info("Found {} items for order", items.size());

            // Log each item
            items.forEach(item ->
                    log.info("Item: ID={}, Product={}, Quantity={}, Price={}",
                            item.getId(), item.getProductName(), item.getQuantity(), item.getPrice())
            );

            // Create DTO manually to avoid lazy loading issues
            OrderDTO dto = new OrderDTO();
            dto.setId(order.getId());
            dto.setOrderNumber(order.getOrderNumber());
            dto.setUserId(order.getUser().getId());
            dto.setUserName(order.getUser().getFullName());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setSubtotal(order.getSubtotal());
            dto.setTaxAmount(order.getTaxAmount());
            dto.setShippingAmount(order.getShippingAmount());
            dto.setDiscountAmount(order.getDiscountAmount());
            dto.setStatus(order.getStatus());
            dto.setPaymentMethod(order.getPaymentMethod());
            dto.setPaymentStatus(order.getPaymentStatus());
            dto.setNotes(order.getNotes());
            dto.setCreatedAt(order.getCreatedAt());
            dto.setUpdatedAt(order.getUpdatedAt());
            dto.setShippedAt(order.getShippedAt());
            dto.setDeliveredAt(order.getDeliveredAt());
            dto.setCancelledAt(order.getCancelledAt());

            // Convert addresses
            if (order.getShippingAddress() != null) {
                AddressDTO shippingAddr = new AddressDTO();
                shippingAddr.setStreet(order.getShippingAddress().getStreet());
                shippingAddr.setCity(order.getShippingAddress().getCity());
                shippingAddr.setState(order.getShippingAddress().getState());
                shippingAddr.setZipCode(order.getShippingAddress().getZipCode());
                shippingAddr.setCountry(order.getShippingAddress().getCountry());
                shippingAddr.setPhone(order.getShippingAddress().getPhone());
                shippingAddr.setRecipientName(order.getShippingAddress().getRecipientName());
                dto.setShippingAddress(shippingAddr);
            }

            if (order.getBillingAddress() != null) {
                AddressDTO billingAddr = new AddressDTO();
                billingAddr.setStreet(order.getBillingAddress().getStreet());
                billingAddr.setCity(order.getBillingAddress().getCity());
                billingAddr.setState(order.getBillingAddress().getState());
                billingAddr.setZipCode(order.getBillingAddress().getZipCode());
                billingAddr.setCountry(order.getBillingAddress().getCountry());
                billingAddr.setPhone(order.getBillingAddress().getPhone());
                billingAddr.setRecipientName(order.getBillingAddress().getRecipientName());
                dto.setBillingAddress(billingAddr);
            }

            // Convert order items from our explicitly fetched list
            List<OrderDTO.OrderItemDTO> itemDTOs = new ArrayList<>();
            for (OrderItem item : items) {
                OrderDTO.OrderItemDTO itemDTO = new OrderDTO.OrderItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductName(item.getProductName());
                itemDTO.setSku(item.getProductSku());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());
                itemDTO.setSubtotal(item.getSubtotal());
                itemDTO.setProductImage(item.getProductImageUrl());

                // Add product ID if needed
                if (item.getProduct() != null) {
                    itemDTO.setProductId(item.getProduct().getId());
                }

                itemDTOs.add(itemDTO);
            }
            dto.setOrderItems(itemDTOs);

            // Calculate derived fields
            dto.calculateDerivedFields();

            log.info("Returning DTO with {} items", itemDTOs.size());
            return dto;

        } catch (Exception e) {
            log.error("Error fetching order: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching order: " + e.getMessage(), e);
        }
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        User currentUser = authService.getCurrentUser();
        log.info("Cancelling order: {} for user: {}", orderId, currentUser.getId());

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
        Set<OrderItem> orderItems = order.getOrderItems();
        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
                productRepository.save(product);
            }
        }

        log.info("Order cancelled: {}", order.getOrderNumber());
        return convertToSimpleDTO(order);
    }

    private String generateOrderNumber() {
        String orderNumber;
        do {
            orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    private OrderDTO convertToSimpleDTO(Order order) {
        if (order == null) return null;

        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());

        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getId());
            dto.setUserName(order.getUser().getFullName());
        }

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

        // Get order items separately to avoid recursion
        Set<OrderItem> orderItems = order.getOrderItems();
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

    private OrderDTO.OrderItemDTO convertToOrderItemDTO(OrderItem orderItem) {
        if (orderItem == null) return null;

        OrderDTO.OrderItemDTO itemDTO = new OrderDTO.OrderItemDTO();
        itemDTO.setId(orderItem.getId());
        itemDTO.setProductName(orderItem.getProductName());
        itemDTO.setProductImage(orderItem.getProductImageUrl());
        itemDTO.setSku(orderItem.getProductSku());
        itemDTO.setPrice(orderItem.getPrice());
        itemDTO.setQuantity(orderItem.getQuantity());
        itemDTO.setSubtotal(orderItem.getSubtotal());
        return itemDTO;
    }

    private OrderDTO buildOrderDTO(Order order, List<OrderItem> items) {
        OrderDTO dto = new OrderDTO();

        // Basic order info
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUser().getId());
        dto.setUserName(order.getUser().getFullName());
        dto.setUserEmail(order.getUser().getEmail());

        // Amounts
        dto.setTotalAmount(order.getTotalAmount());
        dto.setSubtotal(order.getSubtotal());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setShippingAmount(order.getShippingAmount());
        dto.setDiscountAmount(order.getDiscountAmount());

        // Status
        dto.setStatus(order.getStatus());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());

        // Notes and tracking
        dto.setNotes(order.getNotes());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setShippingMethod(order.getShippingMethod());

        // Timestamps
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setShippedAt(order.getShippedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setCancelledAt(order.getCancelledAt());

        // Convert addresses
        if (order.getShippingAddress() != null) {
            AddressDTO shippingAddr = new AddressDTO();
            shippingAddr.setStreet(order.getShippingAddress().getStreet());
            shippingAddr.setCity(order.getShippingAddress().getCity());
            shippingAddr.setState(order.getShippingAddress().getState());
            shippingAddr.setZipCode(order.getShippingAddress().getZipCode());
            shippingAddr.setCountry(order.getShippingAddress().getCountry());
            shippingAddr.setPhone(order.getShippingAddress().getPhone());
            shippingAddr.setRecipientName(order.getShippingAddress().getRecipientName());
            dto.setShippingAddress(shippingAddr);
        }

        if (order.getBillingAddress() != null) {
            AddressDTO billingAddr = new AddressDTO();
            billingAddr.setStreet(order.getBillingAddress().getStreet());
            billingAddr.setCity(order.getBillingAddress().getCity());
            billingAddr.setState(order.getBillingAddress().getState());
            billingAddr.setZipCode(order.getBillingAddress().getZipCode());
            billingAddr.setCountry(order.getBillingAddress().getCountry());
            billingAddr.setPhone(order.getBillingAddress().getPhone());
            billingAddr.setRecipientName(order.getBillingAddress().getRecipientName());
            dto.setBillingAddress(billingAddr);
        }

        // Convert order items
        List<OrderDTO.OrderItemDTO> itemDTOs = new ArrayList<>();
        for (OrderItem item : items) {
            OrderDTO.OrderItemDTO itemDTO = new OrderDTO.OrderItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
            itemDTO.setProductName(item.getProductName());
            itemDTO.setSku(item.getProductSku());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setPrice(item.getPrice());
            itemDTO.setSubtotal(item.getSubtotal());
            itemDTO.setProductImage(item.getProductImageUrl());

            // Format prices
            itemDTO.setFormattedPrice(String.format("$%.2f", item.getPrice()));
            itemDTO.setFormattedSubtotal(String.format("$%.2f", item.getSubtotal()));

            itemDTOs.add(itemDTO);
        }
        dto.setOrderItems(itemDTOs);

        // Calculate derived fields
        dto.calculateDerivedFields();

        return dto;
    }
}