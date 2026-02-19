package com.ecommerce.ecommercebackend.service.admin;

import com.ecommerce.ecommercebackend.dto.CategoryDTO;
import com.ecommerce.ecommercebackend.dto.ProductDTO;
import com.ecommerce.ecommercebackend.dto.admin.ProductBulkUpdateDTO;
import com.ecommerce.ecommercebackend.entity.Category;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.CategoryRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    // ============= READ OPERATIONS =============
    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Pageable pageable, String search, Long categoryId,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           Boolean inStock, Boolean active) {
        log.info("Fetching products with filters - search: {}, categoryId: {}, minPrice: {}, maxPrice: {}, inStock: {}, active: {}",
                search, categoryId, minPrice, maxPrice, inStock, active);

        // Use the repository's findProductsWithFilters method directly with all parameters
        return productRepository.findProductsWithFilters(
                        search, categoryId, minPrice, maxPrice, inStock, active, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return convertToDTO(product);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProductStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByIsActive(true);
        long inactiveProducts = totalProducts - activeProducts;
        long outOfStock = productRepository.countByStockQuantity(0);
        long lowStock = productRepository.countByStockQuantityBetween(1, 10);

        BigDecimal totalInventoryValue = productRepository.getTotalInventoryValue();
        if (totalInventoryValue == null) {
            totalInventoryValue = BigDecimal.ZERO;
        }

        stats.put("totalProducts", totalProducts);
        stats.put("activeProducts", activeProducts);
        stats.put("inactiveProducts", inactiveProducts);
        stats.put("outOfStock", outOfStock);
        stats.put("lowStock", lowStock);
        stats.put("totalInventoryValue", totalInventoryValue);

        // Top categories by product count
        stats.put("topCategories", categoryRepository.findAll().stream()
                .map(c -> {
                    Map<String, Object> categoryMap = new HashMap<>();
                    categoryMap.put("category", c.getName());
                    categoryMap.put("count", c.getProducts() != null ? c.getProducts().size() : 0);
                    return categoryMap;
                })
                .sorted((a, b) -> {
                    Integer aCount = (Integer) a.get("count");
                    Integer bCount = (Integer) b.get("count");
                    return bCount.compareTo(aCount);
                })
                .limit(5)
                .collect(Collectors.toList()));

        return stats;
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getLowStockProducts(int threshold) {
        return productRepository.findByStockQuantityLessThanEqual(threshold).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getOutOfStockProducts() {
        log.info("Fetching out of stock products");
        return productRepository.findByStockQuantity(0).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ============= CREATE OPERATIONS =============
    public ProductDTO createProduct(ProductDTO productDTO) {
        log.info("Creating new product: {}", productDTO.getName());

        Product product = new Product();
        updateProductFromDTO(product, productDTO);

        // Generate SKU if not provided
        if (product.getSku() == null || product.getSku().isEmpty()) {
            product.setSku(generateSKU(product.getName()));
        }

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());

        return convertToDTO(savedProduct);
    }

    // Overloaded method for backward compatibility
    public ProductDTO createProduct(ProductDTO productDTO, MultipartFile image) {
        log.info("Creating new product with image: {}", productDTO.getName());

        ProductDTO created = createProduct(productDTO);

        if (image != null && !image.isEmpty()) {
            return uploadProductImage(created.getId(), image);
        }

        return created;
    }

    public ProductDTO duplicateProduct(Long id) {
        Product original = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Product duplicate = new Product();
        duplicate.setName(original.getName() + " (Copy)");
        duplicate.setDescription(original.getDescription());
        duplicate.setPrice(original.getPrice());
        duplicate.setDiscountPrice(original.getDiscountPrice());
        duplicate.setSku(generateSKU(original.getName() + "-copy"));
        duplicate.setStockQuantity(0);
        duplicate.setCategory(original.getCategory());
        duplicate.setImageUrl(original.getImageUrl());
        duplicate.setAdditionalImages(original.getAdditionalImages());
        duplicate.setSpecifications(original.getSpecifications());
        duplicate.setIsActive(false);

        Product savedDuplicate = productRepository.save(duplicate);
        log.info("Product duplicated: {} -> {}", original.getId(), savedDuplicate.getId());

        return convertToDTO(savedDuplicate);
    }

    // ============= UPDATE OPERATIONS =============
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        updateProductFromDTO(product, productDTO);
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully: {}", id);

        return convertToDTO(updatedProduct);
    }

    // Overloaded method for backward compatibility
    public ProductDTO updateProduct(Long id, ProductDTO productDTO, MultipartFile image) {
        log.info("Updating product with image: {}", id);

        ProductDTO updated = updateProduct(id, productDTO);

        if (image != null && !image.isEmpty()) {
            return uploadProductImage(id, image);
        }

        return updated;
    }

    public ProductDTO toggleProductStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setIsActive(!product.getIsActive());
        Product updatedProduct = productRepository.save(product);

        log.info("Product {} status toggled to: {}", id, updatedProduct.getIsActive());
        return convertToDTO(updatedProduct);
    }

    public List<ProductDTO> bulkUpdateProducts(ProductBulkUpdateDTO bulkUpdateDTO) {
        List<Product> products = productRepository.findAllById(bulkUpdateDTO.getProductIds());

        products.forEach(product -> {
            if (bulkUpdateDTO.getDiscountPrice() != null) {
                product.setDiscountPrice(bulkUpdateDTO.getDiscountPrice());
            }
            if (bulkUpdateDTO.getStockAdjustment() != null) {
                int newStock = product.getStockQuantity() + bulkUpdateDTO.getStockAdjustment();
                product.setStockQuantity(Math.max(0, newStock));
            }
            if (bulkUpdateDTO.getIsActive() != null) {
                product.setIsActive(bulkUpdateDTO.getIsActive());
            }
        });

        List<Product> updatedProducts = productRepository.saveAll(products);
        log.info("Bulk updated {} products", updatedProducts.size());

        return updatedProducts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ============= DELETE OPERATIONS =============
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Delete product image
        if (product.getImageUrl() != null) {
            deleteImage(product.getImageUrl());
        }

        productRepository.delete(product);
        log.info("Product deleted successfully: {}", id);
    }

    public void bulkDeleteProducts(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);

        // Delete associated images
        products.forEach(product -> {
            if (product.getImageUrl() != null) {
                deleteImage(product.getImageUrl());
            }
        });

        productRepository.deleteAll(products);
        log.info("Bulk deleted {} products", products.size());
    }

    // ============= EXPORT OPERATIONS =============
    public List<ProductDTO> exportProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return productRepository.findAll().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            return productRepository.findAllById(productIds).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    // ============= IMAGE UPLOAD METHOD =============
    public ProductDTO uploadProductImage(Long id, MultipartFile file) {
        log.info("Uploading image for product: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (file != null && !file.isEmpty()) {
            // Delete old image if exists
            if (product.getImageUrl() != null) {
                deleteImage(product.getImageUrl());
            }

            String imageUrl = saveImage(file);
            product.setImageUrl(imageUrl);

            Product updatedProduct = productRepository.save(product);
            log.info("Image uploaded successfully for product: {}", id);

            return convertToDTO(updatedProduct);
        } else {
            throw new IllegalArgumentException("Image file is empty");
        }
    }

    // ============= HELPER METHODS =============
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setDiscountPrice(product.getDiscountPrice());
        dto.setSku(product.getSku());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setImageUrl(product.getImageUrl());
        dto.setAdditionalImages(product.getAdditionalImages());
        dto.setSpecifications(product.getSpecifications());
        dto.setIsActive(product.getIsActive());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }

        // Set calculated fields
        dto.setInStock(product.isInStock());
        dto.setHasDiscount(product.hasDiscount());
        dto.setDiscountedPrice(product.getDiscountedPrice());

        return dto;
    }

    private void updateProductFromDTO(Product product, ProductDTO dto) {
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());
        if (dto.getDiscountPrice() != null) product.setDiscountPrice(dto.getDiscountPrice());
        if (dto.getSku() != null) product.setSku(dto.getSku());
        if (dto.getStockQuantity() != null) product.setStockQuantity(dto.getStockQuantity());
        if (dto.getIsActive() != null) product.setIsActive(dto.getIsActive());
        if (dto.getSpecifications() != null) product.setSpecifications(dto.getSpecifications());
        if (dto.getAdditionalImages() != null) product.setAdditionalImages(dto.getAdditionalImages());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));
            product.setCategory(category);
        }
    }

    private String saveImage(MultipartFile image) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = image.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;

            Path filePath = uploadPath.resolve(filename);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/products/" + filename;

        } catch (IOException e) {
            log.error("Failed to save image: {}", e.getMessage());
            throw new RuntimeException("Failed to save image: " + e.getMessage());
        }
    }

    private void deleteImage(String imageUrl) {
        try {
            if (imageUrl != null && imageUrl.startsWith("/uploads/")) {
                String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(uploadDir).resolve(filename);
                Files.deleteIfExists(filePath);
                log.info("Deleted image: {}", filename);
            }
        } catch (IOException e) {
            log.error("Failed to delete image: {}", e.getMessage());
        }
    }

    private String generateSKU(String productName) {
        String base = productName.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .substring(0, Math.min(5, productName.length()));
        String uniqueId = String.valueOf(System.currentTimeMillis()).substring(7);
        return base + "-" + uniqueId;
    }

    // ============= CATEGORY MANAGEMENT METHODS =============

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        log.info("Fetching all categories");
        return categoryRepository.findAll().stream()
                .map(this::convertToCategoryDTO)
                .collect(Collectors.toList());
    }

    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        log.info("Creating new category: {}", categoryDTO.getName());

        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());

        // Generate slug if not provided
        if (categoryDTO.getSlug() != null && !categoryDTO.getSlug().isEmpty()) {
            category.setSlug(categoryDTO.getSlug());
        } else {
            category.setSlug(categoryDTO.getName().toLowerCase()
                    .replaceAll("[^a-zA-Z0-9\\s-]", "")
                    .replaceAll("\\s+", "-"));
        }

        category.setImageUrl(categoryDTO.getImageUrl());
        category.setIsActive(categoryDTO.getIsActive() != null ? categoryDTO.getIsActive() : true);
        category.setDisplayOrder(categoryDTO.getDisplayOrder() != null ? categoryDTO.getDisplayOrder() : 0);

        // Handle parent category if specified
        if (categoryDTO.getParentId() != null) {
            Category parent = categoryRepository.findById(categoryDTO.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + categoryDTO.getParentId()));
            category.setParent(parent);
        }

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", savedCategory.getId());

        return convertToCategoryDTO(savedCategory);
    }

    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        log.info("Updating category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (categoryDTO.getName() != null) category.setName(categoryDTO.getName());
        if (categoryDTO.getDescription() != null) category.setDescription(categoryDTO.getDescription());
        if (categoryDTO.getSlug() != null && !categoryDTO.getSlug().isEmpty()) category.setSlug(categoryDTO.getSlug());
        if (categoryDTO.getImageUrl() != null) category.setImageUrl(categoryDTO.getImageUrl());
        if (categoryDTO.getIsActive() != null) category.setIsActive(categoryDTO.getIsActive());
        if (categoryDTO.getDisplayOrder() != null) category.setDisplayOrder(categoryDTO.getDisplayOrder());

        // Handle parent category update
        if (categoryDTO.getParentId() != null) {
            if (!categoryDTO.getParentId().equals(id)) { // Prevent self-reference
                Category parent = categoryRepository.findById(categoryDTO.getParentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + categoryDTO.getParentId()));
                category.setParent(parent);
            }
        } else if (categoryDTO.getParentId() == null && category.getParent() != null) {
            category.setParent(null);
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully: {}", id);

        return convertToCategoryDTO(updatedCategory);
    }

    public void deleteCategory(Long id) {
        log.info("Deleting category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Check if category has products
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with associated products");
        }

        // Check if category has subcategories
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with subcategories");
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully: {}", id);
    }

    // ============= HELPER METHOD FOR CATEGORY DTO CONVERSION =============

    private CategoryDTO convertToCategoryDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setSlug(category.getSlug());
        dto.setImageUrl(category.getImageUrl());
        dto.setIsActive(category.getIsActive());
        dto.setDisplayOrder(category.getDisplayOrder());

        if (category.getParent() != null) {
            dto.setParentId(category.getParent().getId());
            dto.setParentName(category.getParent().getName());
        }

        if (category.getSubCategories() != null) {
            dto.setSubCategoryCount(category.getSubCategories().size());
        }

        if (category.getProducts() != null) {
            dto.setProductCount(category.getProducts().size());
        }

        return dto;
    }
}