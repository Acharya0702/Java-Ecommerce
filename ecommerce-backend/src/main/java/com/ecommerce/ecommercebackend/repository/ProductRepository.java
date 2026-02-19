package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ============= BASIC QUERIES =============
    Optional<Product> findBySku(String sku);

    List<Product> findByIsActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.category.id = :categoryId")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stockQuantity > 0")
    List<Product> findInStockProducts();

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.discountPrice IS NOT NULL AND p.discountPrice < p.price")
    List<Product> findProductsOnSale();

    @Query(value = "SELECT * FROM products p WHERE p.is_active = true ORDER BY p.created_at DESC LIMIT 10", nativeQuery = true)
    List<Product> findFeaturedProducts();

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchProducts(@Param("query") String query);

    default List<Product> findAllActiveProducts() {
        return findByIsActiveTrue();
    }

    // ============= ADMIN DASHBOARD QUERIES =============

    // Find products by stock quantity less than or equal to threshold
    List<Product> findByStockQuantityLessThanEqual(int threshold);

    // Find products by exact stock quantity
    List<Product> findByStockQuantity(int quantity);

    // Find products by stock quantity between min and max
    List<Product> findByStockQuantityBetween(int min, int max);

    // Find products by active status with pagination
    Page<Product> findByIsActive(boolean isActive, Pageable pageable);

    // Find products by category with pagination (SINGLE DEFINITION)
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    // Find products by price range with pagination
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceBetween(@Param("minPrice") BigDecimal minPrice,
                                     @Param("maxPrice") BigDecimal maxPrice,
                                     Pageable pageable);

    // Find products with stock greater than quantity with pagination (SINGLE DEFINITION)
    Page<Product> findByStockQuantityGreaterThan(int quantity, Pageable pageable);

    // Combined search with filters for admin
    @Query("SELECT p FROM Product p WHERE " +
            "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:inStock IS NULL OR (:inStock = true AND p.stockQuantity > 0) OR (:inStock = false AND p.stockQuantity = 0)) AND " +
            "(:active IS NULL OR p.isActive = :active)")
    Page<Product> findProductsWithFilters(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("inStock") Boolean inStock,
            @Param("active") Boolean active,
            Pageable pageable);

    // Count products by active status
    long countByIsActive(boolean isActive);

    // Count products by stock quantity
    long countByStockQuantity(int quantity);

    // Count products by stock quantity between
    long countByStockQuantityBetween(int min, int max);

    // Get low stock products count
    default long countLowStockProducts(int threshold) {
        return countByStockQuantityBetween(1, threshold);
    }

    // Get out of stock products count
    default long countOutOfStockProducts() {
        return countByStockQuantity(0);
    }

    // ============= PAGINATION AND SEARCH METHODS =============

    // Find products by name containing (case insensitive) with pagination
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Find products by name or description containing with pagination
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> findByNameOrDescriptionContaining(@Param("search") String search, Pageable pageable);

    // Combined name/description search (Spring Data JPA will parse this method name)
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description, Pageable pageable);

    // Get all products ordered by creation date
    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Get product statistics
    @Query("SELECT COUNT(p), SUM(p.stockQuantity), AVG(p.price) FROM Product p")
    Object[] getProductStats();

    // Get total inventory value
    @Query("SELECT SUM(p.price * p.stockQuantity) FROM Product p")
    BigDecimal getTotalInventoryValue();

    // Find products by IDs (for bulk operations)
    List<Product> findByIdIn(List<Long> ids);

    // Find products with discounts
    Page<Product> findByDiscountPriceIsNotNull(Pageable pageable);

    // Find products by category and active status
    Page<Product> findByCategoryIdAndIsActive(Long categoryId, boolean isActive, Pageable pageable);

    Page<Product> findByStockQuantity(int quantity, Pageable pageable);
}