package com.ecommerce.ecommercebackend.dto.admin;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStatsDTO {
    // Overview stats
    private Long totalOrders;
    private Long totalProducts;
    private Long totalCustomers;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;

    // Order stats
    private Long pendingOrders;
    private Long processingOrders;
    private Long shippedOrders;
    private Long deliveredOrders;
    private Long cancelledOrders;

    // Recent data
    private List<RecentOrderDTO> recentOrders;
    private List<PopularProductDTO> popularProducts;

    // Charts data
    private Map<String, BigDecimal> dailySales;
    private Map<String, Long> ordersByStatus;
    private Map<String, Long> productsByCategory;

    @Data
    public static class RecentOrderDTO {
        private Long id;
        private String orderNumber;
        private String customerName;
        private BigDecimal totalAmount;
        private String status;
        private String paymentStatus;
        private String createdAt;
    }

    @Data
    public static class PopularProductDTO {
        private Long id;
        private String name;
        private String imageUrl;
        private Long totalSold;
        private BigDecimal revenue;
        private Integer stockQuantity;
    }
}