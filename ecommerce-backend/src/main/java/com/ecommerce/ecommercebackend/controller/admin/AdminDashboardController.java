package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.dto.admin.DashboardStatsDTO;
import com.ecommerce.ecommercebackend.dto.admin.OrderUpdateDTO;
import com.ecommerce.ecommercebackend.dto.CategoryDTO;
import com.ecommerce.ecommercebackend.dto.OrderDTO;
import com.ecommerce.ecommercebackend.dto.UserDTO;
import com.ecommerce.ecommercebackend.service.admin.AdminDashboardService;
import com.ecommerce.ecommercebackend.service.admin.AdminOrderService;
import com.ecommerce.ecommercebackend.service.admin.AdminProductService;
import com.ecommerce.ecommercebackend.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final AdminOrderService orderService;
    private final AdminProductService productService;
    private final AdminUserService userService;

    // ============= DASHBOARD =============
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    @GetMapping("/dashboard/sales-chart")
    public ResponseEntity<Map<String, BigDecimal>> getSalesChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(dashboardService.getSalesChartData(start, end));
    }

    // ============= ORDER MANAGEMENT =============
    @GetMapping("/orders")
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable, status, search));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDTO> getOrderDetails(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetails(id));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateDTO updateDTO) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, updateDTO));
    }

    @PostMapping("/orders/{id}/process-payment")
    public ResponseEntity<OrderDTO> processPayment(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.processPayment(id));
    }

    @PostMapping("/orders/bulk-status-update")
    public ResponseEntity<List<OrderDTO>> bulkUpdateOrderStatus(
            @RequestBody List<Long> orderIds,
            @RequestParam String status) {
        return ResponseEntity.ok(orderService.bulkUpdateOrderStatus(orderIds, status));
    }

    // ============= CATEGORY MANAGEMENT =============
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createCategory(categoryDTO));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDTO categoryDTO) {
        return ResponseEntity.ok(productService.updateCategory(id, categoryDTO));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        productService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ============= USER MANAGEMENT =============
    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(userService.getAllUsers(pageable, role, search));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserDetails(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserDetails(id));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role) {
        return ResponseEntity.ok(userService.updateUserRole(id, role));
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<UserDTO> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }

    @GetMapping("/users/stats")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        return ResponseEntity.ok(userService.getUserStats());
    }

    // ============= REPORTS =============
    @GetMapping("/reports/sales")
    public ResponseEntity<Map<String, Object>> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "daily") String interval) {
        return ResponseEntity.ok(dashboardService.getSalesReport(start, end, interval));
    }

    @GetMapping("/reports/top-products")
    public ResponseEntity<List<DashboardStatsDTO.PopularProductDTO>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getTopProducts(limit));
    }

    @GetMapping("/reports/inventory")
    public ResponseEntity<Map<String, Object>> getInventoryReport() {
        return ResponseEntity.ok(dashboardService.getInventoryReport());
    }

    // ============= INVENTORY ALERTS =============
    @GetMapping("/inventory/low-stock")
    public ResponseEntity<List<com.ecommerce.ecommercebackend.dto.ProductDTO>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }

    @GetMapping("/inventory/out-of-stock")
    public ResponseEntity<List<com.ecommerce.ecommercebackend.dto.ProductDTO>> getOutOfStockProducts() {
        return ResponseEntity.ok(productService.getOutOfStockProducts());
    }
}