package com.amar.controller;

import com.amar.dto.CategoryDto;
import com.amar.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/categories")
@Validated
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
    
    @Autowired
    private CategoryService categoryService;

    /**
     * Create a new category
     */
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        logger.info("Creating new category: {}", categoryDto.getName());
        CategoryDto createdCategory = categoryService.createCategory(categoryDto);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }
    
    /**
     * Update an existing category
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody CategoryDto categoryDto) {
        logger.info("Updating category with ID: {}", id);
        CategoryDto updatedCategory = categoryService.updateCategory(id, categoryDto);
        return ResponseEntity.ok(updatedCategory);
    }
    
    /**
     * Get category by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable @Min(1) Long id) {
        logger.info("Fetching category with ID: {}", id);
        CategoryDto category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }
    
    /**
     * Get category by slug
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryDto> getCategoryBySlug(@PathVariable String slug) {
        logger.info("Fetching category with slug: {}", slug);
        CategoryDto category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(category);
    }
    
    /**
     * Get all categories with pagination
     */
    @GetMapping
    public ResponseEntity<Page<CategoryDto>> getAllCategories(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        logger.info("Fetching all categories - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<CategoryDto> categories = categoryService.getAllCategories(pageable);
        
        return ResponseEntity.ok(categories);
    }
    
    /**
     * Get parent categories (top-level categories)
     */
    @GetMapping("/parents")
    public ResponseEntity<List<CategoryDto>> getParentCategories() {
        logger.info("Fetching parent categories");
        List<CategoryDto> categories = categoryService.getParentCategories();
        return ResponseEntity.ok(categories);
    }
    
    /**
     * Get children categories by parent ID
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<CategoryDto>> getChildrenCategories(@PathVariable @Min(1) Long parentId) {
        logger.info("Fetching children categories for parent ID: {}", parentId);
        List<CategoryDto> categories = categoryService.getChildrenCategories(parentId);
        return ResponseEntity.ok(categories);
    }
    
    /**
     * Search categories by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<CategoryDto>> searchCategories(@RequestParam String name) {
        logger.info("Searching categories by name: {}", name);
        List<CategoryDto> categories = categoryService.searchCategoriesByName(name);
        return ResponseEntity.ok(categories);
    }
    
    /**
     * Get category hierarchy
     */
    @GetMapping("/{id}/hierarchy")
    public ResponseEntity<List<CategoryDto>> getCategoryHierarchy(@PathVariable @Min(1) Long id) {
        logger.info("Fetching category hierarchy for ID: {}", id);
        List<CategoryDto> hierarchy = categoryService.getCategoryHierarchy(id);
        return ResponseEntity.ok(hierarchy);
    }
    
    /**
     * Get categories with product count
     */
    @GetMapping("/with-product-count")
    public ResponseEntity<List<CategoryDto>> getCategoriesWithProductCount() {
        logger.info("Fetching categories with product count");
        List<CategoryDto> categories = categoryService.getCategoriesWithProductCount();
        return ResponseEntity.ok(categories);
    }
    
    /**
     * Delete category (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable @Min(1) Long id) {
        logger.info("Deleting category with ID: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Restore category
     */
    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restoreCategory(@PathVariable @Min(1) Long id) {
        logger.info("Restoring category with ID: {}", id);
        categoryService.restoreCategory(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Update display order
     */
    @PatchMapping("/{id}/display-order")
    public ResponseEntity<Void> updateDisplayOrder(
            @PathVariable @Min(1) Long id,
            @RequestParam @Min(0) Integer displayOrder) {
        
        logger.info("Updating display order for category ID: {} to: {}", id, displayOrder);
        categoryService.updateDisplayOrder(id, displayOrder);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get category count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCategoryCount() {
        logger.info("Fetching category count");
        try {
            List<CategoryDto> categories = categoryService.getParentCategories();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "totalCategories", categories.size(),
                "message", "Category count retrieved successfully"
            ));
        } catch (Exception e) {
            logger.error("Error fetching category count", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error", 
                "message", "Unable to fetch category count",
                "error", e.getMessage()
            ));
        }
    }
}