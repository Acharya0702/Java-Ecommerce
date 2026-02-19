package com.ecommerce.ecommercebackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long id;

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;
    private String slug;
    private String imageUrl;
    private Boolean isActive;
    private Integer displayOrder;

    // Parent category relationship
    private Long parentId;
    private String parentName;

    // Subcategories
    private List<CategoryDTO> subCategories;

    // Count fields
    private Integer subCategoryCount;
    private Integer productCount;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Helper methods for backward compatibility
    public Integer getSubCategoryCount() {
        if (subCategoryCount != null) {
            return subCategoryCount;
        }
        return subCategories != null ? subCategories.size() : 0;
    }

    public Integer getProductCount() {
        return productCount != null ? productCount : 0;
    }
}