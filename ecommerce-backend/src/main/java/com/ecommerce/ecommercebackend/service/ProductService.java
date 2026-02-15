package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.ProductDTO;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    public List<ProductDTO> getAllProducts(Long categoryId, Boolean inStock, Boolean onSale, int page, int size) {
        List<Product> products;

        if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else if (inStock != null && inStock) {
            products = productRepository.findInStockProducts();
        } else if (onSale != null && onSale) {
            products = productRepository.findProductsOnSale();
        } else {
            products = productRepository.findAllActiveProducts();
        }

        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return convertToDTO(product);
    }

    public ProductDTO getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return convertToDTO(product);
    }

    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = convertToEntity(productDTO);
        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }

    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Update fields
        existingProduct.setName(productDTO.getName());
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setDiscountPrice(productDTO.getDiscountPrice());
        existingProduct.setStockQuantity(productDTO.getStockQuantity());
        existingProduct.setImageUrl(productDTO.getImageUrl());
        existingProduct.setAdditionalImages(productDTO.getAdditionalImages());
        existingProduct.setSpecifications(productDTO.getSpecifications());
        existingProduct.setIsActive(productDTO.getIsActive());

        Product updatedProduct = productRepository.save(existingProduct);
        return convertToDTO(updatedProduct);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setIsActive(false);
        productRepository.save(product);
    }

    public List<ProductDTO> searchProducts(String query) {
        List<Product> products = productRepository.searchProducts(query);
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> getFeaturedProducts() {
        List<Product> products = productRepository.findFeaturedProducts();
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> getProductsOnSale() {
        List<Product> products = productRepository.findProductsOnSale();
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get product with detailed review statistics
    public Map<String, Object> getProductWithReviews(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductDTO productDTO = convertToDTO(product);

        Map<String, Object> result = new HashMap<>();
        result.put("product", productDTO);

        // Get additional review statistics
        Double averageRating = reviewRepository.findAverageRatingByProductId(productId);
        Long reviewCount = reviewRepository.countByProductId(productId);
        List<Object[]> ratingDistribution = reviewRepository.getRatingDistribution(productId);

        // Convert rating distribution to map
        Map<Integer, Long> distributionMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distributionMap.put(i, 0L);
        }

        if (ratingDistribution != null) {
            for (Object[] row : ratingDistribution) {
                Integer rating = (Integer) row[0];
                Long count = (Long) row[1];
                distributionMap.put(rating, count);
            }
        }

        result.put("averageRating", averageRating != null ? String.format("%.1f", averageRating) : "0.0");
        result.put("reviewCount", reviewCount != null ? reviewCount : 0);
        result.put("ratingDistribution", distributionMap);

        return result;
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setDiscountPrice(product.getDiscountPrice());
        dto.setSku(product.getSku());
        dto.setStockQuantity(product.getStockQuantity());

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }

        dto.setImageUrl(product.getImageUrl());
        dto.setAdditionalImages(product.getAdditionalImages());
        dto.setSpecifications(product.getSpecifications());
        dto.setIsActive(product.getIsActive());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        // Calculate derived fields
        dto.setDiscountedPrice(product.getDiscountedPrice());
        dto.setHasDiscount(product.hasDiscount());
        dto.setInStock(product.isInStock());

        // Add rating data
        Double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
        Long reviewCount = reviewRepository.countByProductId(product.getId());
        List<Object[]> ratingDistribution = reviewRepository.getRatingDistribution(product.getId());

        // Set average rating
        if (avgRating != null) {
            dto.setAverageRating(BigDecimal.valueOf(avgRating));
        } else {
            dto.setAverageRating(BigDecimal.ZERO);
        }

        // Set total reviews
        if (reviewCount != null) {
            dto.setTotalReviews(reviewCount.intValue());
        } else {
            dto.setTotalReviews(0);
        }

        // Set rating distribution
        if (ratingDistribution != null && !ratingDistribution.isEmpty()) {
            Map<Integer, Long> distributionMap = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                distributionMap.put(i, 0L);
            }

            for (Object[] row : ratingDistribution) {
                Integer rating = (Integer) row[0];
                Long count = (Long) row[1];
                distributionMap.put(rating, count);
            }
            dto.setRatingDistribution(distributionMap);
        }

        return dto;
    }

    private Product convertToEntity(ProductDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setDiscountPrice(dto.getDiscountPrice());
        product.setSku(dto.getSku());
        product.setStockQuantity(dto.getStockQuantity());
        product.setImageUrl(dto.getImageUrl());
        product.setAdditionalImages(dto.getAdditionalImages());
        product.setSpecifications(dto.getSpecifications());
        product.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        // Note: Category would need to be set separately
        return product;
    }
}