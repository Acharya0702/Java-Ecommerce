package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.User;
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
public interface UserRepository extends JpaRepository<User, Long> {

    // ============= AUTHENTICATION QUERIES =============
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    boolean existsByEmail(String email);

    Optional<User> findByPhone(String phone);

    // ============= ADMIN DASHBOARD QUERIES =============

    // Count users by role
    long countByRole(User.Role role);

    // Get users with filters for admin panel
    @Query("SELECT u FROM User u WHERE " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:search IS NULL OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findUsersWithFilters(
            @Param("role") User.Role role,
            @Param("search") String search,
            Pageable pageable);

    // Get recent users
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findTop10ByOrderByCreatedAtDesc(Pageable pageable);

    default List<User> findTop10RecentUsers() {
        return findTop10ByOrderByCreatedAtDesc(Pageable.ofSize(10));
    }

    // Get user registration stats by date
    @Query("SELECT DATE(u.createdAt), COUNT(u) FROM User u " +
            "WHERE u.createdAt BETWEEN :start AND :end " +
            "GROUP BY DATE(u.createdAt) ORDER BY DATE(u.createdAt)")
    List<Object[]> getRegistrationCountByDate(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Get users by role for pie chart
    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> countByRole();

    // Get active vs inactive users
    long countByIsActiveTrue();
    long countByIsActiveFalse();

    // Get email verified vs unverified
    long countByIsEmailVerifiedTrue();
    long countByIsEmailVerifiedFalse();

    // Search users by name or email (for admin)
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchUsers(@Param("query") String query, Pageable pageable);

    // Get users with most orders (for admin reports)
    @Query("SELECT u.id, u.firstName, u.lastName, u.email, COUNT(o) as orderCount, SUM(o.totalAmount) as totalSpent " +
            "FROM User u LEFT JOIN u.orders o " +
            "GROUP BY u.id, u.firstName, u.lastName, u.email " +
            "ORDER BY orderCount DESC")
    List<Object[]> findTopCustomers(Pageable pageable);

    default List<Object[]> findTop10Customers() {
        return findTopCustomers(Pageable.ofSize(10));
    }

    // Get users who haven't logged in recently (inactive)
    @Query("SELECT u FROM User u WHERE u.lastLogin < :date OR u.lastLogin IS NULL")
    List<User> findInactiveUsers(@Param("date") LocalDateTime date, Pageable pageable);
}