package com.ecommerce.ecommercebackend.service.admin;

import com.ecommerce.ecommercebackend.dto.admin.DashboardStatsDTO;
import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public DashboardStatsDTO getDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        // Basic counts
        stats.setTotalOrders(orderRepository.count());
        stats.setTotalProducts(productRepository.count());
        stats.setTotalCustomers(userRepository.countByRole(User.Role.CUSTOMER));

        // Revenue calculations
        List<Order> deliveredOrders = orderRepository.findByStatus(Order.OrderStatus.DELIVERED);
        BigDecimal totalRevenue = deliveredOrders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);

        BigDecimal averageOrderValue = deliveredOrders.isEmpty() ? BigDecimal.ZERO :
                totalRevenue.divide(BigDecimal.valueOf(deliveredOrders.size()), 2, RoundingMode.HALF_UP);
        stats.setAverageOrderValue(averageOrderValue);

        // Order status counts
        stats.setPendingOrders(orderRepository.countByStatus(Order.OrderStatus.PENDING));
        stats.setProcessingOrders(orderRepository.countByStatus(Order.OrderStatus.PROCESSING));
        stats.setShippedOrders(orderRepository.countByStatus(Order.OrderStatus.SHIPPED));
        stats.setDeliveredOrders(orderRepository.countByStatus(Order.OrderStatus.DELIVERED));
        stats.setCancelledOrders(orderRepository.countByStatus(Order.OrderStatus.CANCELLED));

        // Recent orders
        stats.setRecentOrders(getRecentOrders());

        // Popular products
        stats.setPopularProducts(getPopularProducts());

        // Orders by status for pie chart
        stats.setOrdersByStatus(getOrdersByStatus());

        return stats;
    }

    public Map<String, BigDecimal> getSalesChartData(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatus(
                start, end, Order.OrderStatus.DELIVERED);

        Map<String, BigDecimal> dailySales = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        orders.forEach(order -> {
            if (order.getCreatedAt() != null && order.getTotalAmount() != null) {
                String day = order.getCreatedAt().format(formatter);
                dailySales.merge(day, order.getTotalAmount(), BigDecimal::add);
            }
        });

        return dailySales;
    }

    public Map<String, Object> getSalesReport(LocalDateTime start, LocalDateTime end, String interval) {
        Map<String, Object> report = new HashMap<>();

        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);

        report.put("totalOrders", orders.size());

        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.put("totalRevenue", totalRevenue);

        double averageOrderValue = orders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0)
                .average()
                .orElse(0.0);
        report.put("averageOrderValue", averageOrderValue);

        // Group by interval (daily/weekly/monthly)
        Map<String, Object> groupedData = groupOrdersByInterval(orders, interval);
        report.put("groupedData", groupedData);

        return report;
    }

    public List<DashboardStatsDTO.PopularProductDTO> getTopProducts(int limit) {
        return getPopularProducts().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getInventoryReport() {
        Map<String, Object> report = new HashMap<>();

        List<Product> allProducts = productRepository.findAll();

        report.put("totalProducts", allProducts.size());

        BigDecimal totalStockValue = allProducts.stream()
                .map(p -> p.getPrice() != null && p.getStockQuantity() != null ?
                        p.getPrice().multiply(BigDecimal.valueOf(p.getStockQuantity())) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.put("totalStockValue", totalStockValue);

        long lowStockCount = allProducts.stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0 && p.getStockQuantity() <= 10)
                .count();
        report.put("lowStockCount", lowStockCount);

        long outOfStockCount = allProducts.stream()
                .filter(p -> p.getStockQuantity() == null || p.getStockQuantity() == 0)
                .count();
        report.put("outOfStockCount", outOfStockCount);

        return report;
    }

    // Helper methods
    private List<DashboardStatsDTO.RecentOrderDTO> getRecentOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Order> recentOrders = orderRepository.findTop10ByOrderByCreatedAtDesc(pageable);

        return recentOrders.stream()
                .map(order -> {
                    DashboardStatsDTO.RecentOrderDTO dto = new DashboardStatsDTO.RecentOrderDTO();
                    dto.setId(order.getId());
                    dto.setOrderNumber(order.getOrderNumber());

                    if (order.getUser() != null) {
                        dto.setCustomerName(order.getUser().getFullName());
                    }

                    dto.setTotalAmount(order.getTotalAmount());

                    if (order.getStatus() != null) {
                        dto.setStatus(order.getStatus().toString());
                    }

                    if (order.getPaymentStatus() != null) {
                        dto.setPaymentStatus(order.getPaymentStatus().toString());
                    }

                    if (order.getCreatedAt() != null) {
                        dto.setCreatedAt(order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<DashboardStatsDTO.PopularProductDTO> getPopularProducts() {
        try {
            // Try to get from repository query first
            List<Object[]> popularProductsData = orderRepository.findTop10PopularProducts();

            if (popularProductsData != null && !popularProductsData.isEmpty()) {
                return popularProductsData.stream()
                        .map(data -> {
                            DashboardStatsDTO.PopularProductDTO dto = new DashboardStatsDTO.PopularProductDTO();
                            dto.setId((Long) data[0]);
                            dto.setName((String) data[1]);
                            dto.setImageUrl((String) data[2]);
                            dto.setTotalSold((Long) data[3]);
                            dto.setRevenue((BigDecimal) data[4]);

                            // Get current stock from product repository
                            productRepository.findById(dto.getId()).ifPresent(product ->
                                    dto.setStockQuantity(product.getStockQuantity())
                            );

                            return dto;
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Could not fetch popular products from repository, falling back to manual calculation: {}", e.getMessage());
        }

        // Fallback: manual calculation
        return productRepository.findAll().stream()
                .map(product -> {
                    DashboardStatsDTO.PopularProductDTO dto = new DashboardStatsDTO.PopularProductDTO();
                    dto.setId(product.getId());
                    dto.setName(product.getName());
                    dto.setImageUrl(product.getImageUrl());
                    dto.setStockQuantity(product.getStockQuantity());

                    // Calculate total sold from order items
                    long totalSold = 0L;
                    BigDecimal revenue = BigDecimal.ZERO;

                    if (product.getOrderItems() != null && !product.getOrderItems().isEmpty()) {
                        totalSold = product.getOrderItems().stream()
                                .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0L)
                                .sum();

                        revenue = product.getOrderItems().stream()
                                .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                    }

                    dto.setTotalSold(totalSold);
                    dto.setRevenue(revenue);

                    return dto;
                })
                .sorted((a, b) -> Long.compare(b.getTotalSold(), a.getTotalSold()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private Map<String, Long> getOrdersByStatus() {
        Map<String, Long> statusCounts = new HashMap<>();
        List<Object[]> results = orderRepository.countByStatus();

        if (results != null) {
            for (Object[] result : results) {
                if (result.length >= 2) {
                    Order.OrderStatus status = (Order.OrderStatus) result[0];
                    Long count = (Long) result[1];
                    statusCounts.put(status.toString(), count);
                }
            }
        } else {
            // Fallback: manual counting
            for (Order.OrderStatus status : Order.OrderStatus.values()) {
                long count = orderRepository.countByStatus(status);
                if (count > 0) {
                    statusCounts.put(status.toString(), count);
                }
            }
        }

        return statusCounts;
    }

    private Map<String, Object> groupOrdersByInterval(List<Order> orders, String interval) {
        Map<String, Object> grouped = new TreeMap<>();
        Map<String, List<Order>> groupedOrders = new TreeMap<>();

        DateTimeFormatter formatter;
        switch (interval.toLowerCase()) {
            case "daily":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                break;
            case "weekly":
                formatter = DateTimeFormatter.ofPattern("yyyy-'W'ww");
                break;
            case "monthly":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                break;
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        }

        orders.forEach(order -> {
            if (order.getCreatedAt() != null) {
                String key = order.getCreatedAt().format(formatter);
                groupedOrders.computeIfAbsent(key, k -> new ArrayList<>()).add(order);
            }
        });

        // Transform to summary data
        Map<String, Object> summary = new HashMap<>();
        groupedOrders.forEach((key, orderList) -> {
            Map<String, Object> periodData = new HashMap<>();
            periodData.put("count", orderList.size());

            BigDecimal revenue = orderList.stream()
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            periodData.put("revenue", revenue);

            summary.put(key, periodData);
        });

        return summary;
    }
}