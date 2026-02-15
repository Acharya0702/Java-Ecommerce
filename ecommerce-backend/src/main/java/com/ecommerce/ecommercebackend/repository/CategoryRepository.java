package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentId(Long parentId);

    List<Category> findByParentIsNull();

    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.displayOrder")
    List<Category> findAllActiveCategories();

    @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.parent.id = :parentId ORDER BY c.displayOrder")
    List<Category> findActiveSubCategories(@Param("parentId") Long parentId);
}