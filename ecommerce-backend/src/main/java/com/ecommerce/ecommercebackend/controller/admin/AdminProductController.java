package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.dto.CategoryDTO;
import com.ecommerce.ecommercebackend.dto.ProductDTO;
import com.ecommerce.ecommercebackend.dto.admin.ProductBulkUpdateDTO;
import com.ecommerce.ecommercebackend.service.admin.AdminProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class AdminProductController {

    private final AdminProductService productService;
    private final ObjectMapper objectMapper;  // Spring Boot auto-configures this

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getAllProducts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean active) {

        log.info("Fetching products with filters - search: {}, categoryId: {}, minPrice: {}, maxPrice: {}, inStock: {}, active: {}",
                search, categoryId, minPrice, maxPrice, inStock, active);

        return ResponseEntity.ok(productService.getAllProducts(pageable, search, categoryId, minPrice, maxPrice, inStock, active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        log.info("Fetching product with id: {}", id);
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ProductDTO> createProduct(
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile image) throws JsonProcessingException {

        log.info("Creating new product");
        ProductDTO productDTO = objectMapper.readValue(productJson, ProductDTO.class);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(productDTO, image));
    }

    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile image) throws JsonProcessingException {

        log.info("Updating product with id: {}", id);
        ProductDTO productDTO = objectMapper.readValue(productJson, ProductDTO.class);
        return ResponseEntity.ok(productService.updateProduct(id, productDTO, image));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Deleting product with id: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk/delete")
    public ResponseEntity<Void> bulkDeleteProducts(@RequestBody List<Long> productIds) {
        log.info("Bulk deleting {} products", productIds.size());
        productService.bulkDeleteProducts(productIds);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk/update")
    public ResponseEntity<List<ProductDTO>> bulkUpdateProducts(@Valid @RequestBody ProductBulkUpdateDTO bulkUpdateDTO) {
        log.info("Bulk updating products: {}", bulkUpdateDTO);
        return ResponseEntity.ok(productService.bulkUpdateProducts(bulkUpdateDTO));
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<ProductDTO> toggleProductStatus(@PathVariable Long id) {
        log.info("Toggling status for product: {}", id);
        return ResponseEntity.ok(productService.toggleProductStatus(id));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ProductDTO> duplicateProduct(@PathVariable Long id) {
        log.info("Duplicating product with id: {}", id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.duplicateProduct(id));
    }

    @GetMapping("/export")
    public ResponseEntity<List<ProductDTO>> exportProducts(
            @RequestParam(required = false) String format,
            @RequestParam(required = false) List<Long> productIds) {

        log.info("Exporting products in format: {}", format);
        return ResponseEntity.ok(productService.exportProducts(productIds));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        log.info("Fetching all categories for product management");
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        log.info("Creating new category: {}", categoryDTO.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createCategory(categoryDTO));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDTO categoryDTO) {
        log.info("Updating category with id: {}", id);
        return ResponseEntity.ok(productService.updateCategory(id, categoryDTO));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        log.info("Deleting category with id: {}", id);
        productService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getProductStats() {
        log.info("Fetching product statistics");
        return ResponseEntity.ok(productService.getProductStats());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductDTO>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold) {
        log.info("Fetching low stock products with threshold: {}", threshold);
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<List<ProductDTO>> getOutOfStockProducts() {
        log.info("Fetching out of stock products");
        return ResponseEntity.ok(productService.getOutOfStockProducts());
    }

    @PostMapping("/{id}/upload-image")
    public ResponseEntity<ProductDTO> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile file) {
        log.info("Uploading image for product: {}", id);
        return ResponseEntity.ok(productService.uploadProductImage(id, file));
    }
}