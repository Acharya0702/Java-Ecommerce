package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ============= USER SPECIFIC QUERIES =============
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Order.OrderStatus status);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    // ============= ADMIN DASHBOARD QUERIES =============

    // Find orders by status (ADD THIS METHOD)
    List<Order> findByStatus(Order.OrderStatus status);

    // Count by status
    long countByStatus(Order.OrderStatus status);

    // Get recent orders for dashboard
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findTop10ByOrderByCreatedAtDesc(Pageable pageable);

    default List<Order> findTop10ByOrderByCreatedAtDesc() {
        return findTop10ByOrderByCreatedAtDesc(Pageable.ofSize(10));
    }

    // Get orders by date range
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :start AND :end AND o.status = :status")
    List<Order> findByCreatedAtBetweenAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") Order.OrderStatus status);

    // Get orders with filters for admin panel
    @Query("SELECT o FROM Order o WHERE " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:search IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(o.user.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(o.user.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findOrdersWithFilters(
            @Param("status") Order.OrderStatus status,
            @Param("search") String search,
            Pageable pageable);

    // Get revenue stats
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED'")
    Double getTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED' AND o.createdAt BETWEEN :start AND :end")
    Double getRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Get order count by date for charts
    @Query("SELECT DATE(o.createdAt), COUNT(o) FROM Order o " +
            "WHERE o.createdAt BETWEEN :start AND :end " +
            "GROUP BY DATE(o.createdAt) ORDER BY DATE(o.createdAt)")
    List<Object[]> getOrderCountByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Get revenue by date for charts
    @Query("SELECT DATE(o.createdAt), SUM(o.totalAmount) FROM Order o " +
            "WHERE o.status = 'DELIVERED' AND o.createdAt BETWEEN :start AND :end " +
            "GROUP BY DATE(o.createdAt) ORDER BY DATE(o.createdAt)")
    List<Object[]> getRevenueByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Get orders by status for pie chart
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatus();

    // Get popular products (most ordered)
    @Query("SELECT oi.product.id, oi.product.name, oi.product.imageUrl, SUM(oi.quantity) as totalSold, SUM(oi.subtotal) as revenue " +
            "FROM OrderItem oi GROUP BY oi.product.id, oi.product.name, oi.product.imageUrl " +
            "ORDER BY totalSold DESC")
    List<Object[]> findPopularProducts(Pageable pageable);

    default List<Object[]> findTop10PopularProducts() {
        return findPopularProducts(Pageable.ofSize(10));
    }
}