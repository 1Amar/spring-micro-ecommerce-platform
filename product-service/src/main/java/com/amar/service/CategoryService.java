package com.amar.service;

import com.amar.dto.CategoryDto;
import com.amar.entity.Category;
import com.amar.repository.CategoryRepository;
import com.amar.exception.ResourceNotFoundException;
import com.amar.exception.DuplicateResourceException;
import com.amar.mapper.CategoryMapperMS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private CategoryMapperMS categoryMapperMS;
    
    /**
     * Create a new category
     */
    public CategoryDto createCategory(CategoryDto categoryDto) {
        logger.info("Creating new category: {}", categoryDto.getName());
        
        // Validate name uniqueness
        if (categoryRepository.existsByNameAndIdNot(categoryDto.getName(), 0L)) {
            throw new DuplicateResourceException("Category with name '" + categoryDto.getName() + "' already exists");
        }
        
        // Validate parent category if provided
        Category parent = null;
        if (categoryDto.getParentId() != null) {
            parent = categoryRepository.findById(categoryDto.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + categoryDto.getParentId()));
        }
        
        Category category = categoryMapperMS.toEntity(categoryDto);
        category.setParent(parent);
        
        // Set defaults
        if (category.getIsActive() == null) category.setIsActive(true);
        if (category.getDisplayOrder() == null) category.setDisplayOrder(0);
        
        Category savedCategory = categoryRepository.save(category);
        logger.info("Category created successfully with ID: {}", savedCategory.getId());
        
        return categoryMapperMS.toDto(savedCategory);
    }
    
    /**
     * Update an existing category
     */
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        logger.info("Updating category with ID: {}", id);
        
        Category existingCategory = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        
        // Validate name uniqueness (excluding current category)
        if (!existingCategory.getName().equals(categoryDto.getName()) && 
            categoryRepository.existsByNameAndIdNot(categoryDto.getName(), id)) {
            throw new DuplicateResourceException("Category with name '" + categoryDto.getName() + "' already exists");
        }
        
        // Validate parent category if provided
        Category parent = null;
        if (categoryDto.getParentId() != null) {
            if (categoryDto.getParentId().equals(id)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            parent = categoryRepository.findById(categoryDto.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + categoryDto.getParentId()));
        }
        
        // Update fields
        categoryMapperMS.updateEntityFromDto(categoryDto, existingCategory);
        existingCategory.setParent(parent);
        
        Category updatedCategory = categoryRepository.save(existingCategory);
        logger.info("Category updated successfully with ID: {}", updatedCategory.getId());
        
        return categoryMapperMS.toDto(updatedCategory);
    }
    
    /**
     * Get category by ID
     */
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        logger.debug("Fetching category with ID: {}", id);
        
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        
        return categoryMapperMS.toDto(category);
    }
    
    /**
     * Get category by slug
     */
    @Transactional(readOnly = true)
    public CategoryDto getCategoryBySlug(String slug) {
        logger.debug("Fetching category with slug: {}", slug);
        
        Category category = categoryRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        
        return categoryMapperMS.toDto(category);
    }
    
    /**
     * Get all categories with pagination
     */
    @Transactional(readOnly = true)
    public Page<CategoryDto> getAllCategories(Pageable pageable) {
        logger.debug("Fetching all categories with pagination: {}", pageable);
        
        Page<Category> categories = categoryRepository.findByIsActiveTrue(pageable);
        return categories.map(categoryMapperMS::toDto);
    }
    
    /**
     * Get parent categories (top-level categories)
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getParentCategories() {
        logger.debug("Fetching parent categories");
        
        List<Category> categories = categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();
        return categories.stream()
                        .map(categoryMapperMS::toDto)
                        .collect(Collectors.toList());
    }
    
    /**
     * Get children categories by parent ID
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getChildrenCategories(Long parentId) {
        logger.debug("Fetching children categories for parent ID: {}", parentId);
        
        // Validate parent exists
        Category parent = categoryRepository.findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + parentId));
        
        List<Category> categories = categoryRepository.findByParentAndIsActiveTrueOrderByDisplayOrderAsc(parent);
        return categories.stream()
                        .map(categoryMapperMS::toDto)
                        .collect(Collectors.toList());
    }
    
    /**
     * Search categories by name
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> searchCategoriesByName(String name) {
        logger.debug("Searching categories by name: {}", name);
        
        if (!StringUtils.hasText(name)) {
            return List.of();
        }
        
        List<Category> categories = categoryRepository.findByNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(name);
        return categories.stream()
                        .map(categoryMapperMS::toDto)
                        .collect(Collectors.toList());
    }
    
    /**
     * Get category hierarchy
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoryHierarchy(Long categoryId) {
        logger.debug("Fetching category hierarchy for ID: {}", categoryId);
        
        // Validate category exists
        categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
        
        List<Category> hierarchy = categoryRepository.findCategoryHierarchy(categoryId);
        return hierarchy.stream()
                       .map(categoryMapperMS::toDto)
                       .collect(Collectors.toList());
    }
    
    /**
     * Get categories with product count
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoriesWithProductCount() {
        logger.debug("Fetching categories with product count");
        
        List<Object[]> results = categoryRepository.findCategoriesWithProductCount();
        return results.stream()
                     .map(result -> {
                         Category category = (Category) result[0];
                         Long productCount = (Long) result[1];
                         CategoryDto dto = categoryMapperMS.toDto(category);
                         dto.setProductCount(productCount);
                         return dto;
                     })
                     .collect(Collectors.toList());
    }
    
    /**
     * Delete category (soft delete)
     */
    public void deleteCategory(Long id) {
        logger.info("Soft deleting category with ID: {}", id);
        
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        
        // Check if category has children
        List<Category> children = categoryRepository.findByParentAndIsActiveTrueOrderByDisplayOrderAsc(category);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete category that has active children. Please delete or reassign children first.");
        }
        
        // Check if category has products - using ProductRepository would be better here
        // For now, just proceed with the soft delete
        // TODO: Add proper product count check when ProductRepository is integrated
        
        category.setIsActive(false);
        categoryRepository.save(category);
        
        logger.info("Category soft deleted successfully with ID: {}", id);
    }
    
    /**
     * Restore category
     */
    public void restoreCategory(Long id) {
        logger.info("Restoring category with ID: {}", id);
        
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        
        category.setIsActive(true);
        categoryRepository.save(category);
        
        logger.info("Category restored successfully with ID: {}", id);
    }
    
    /**
     * Update display order
     */
    public void updateDisplayOrder(Long id, Integer displayOrder) {
        logger.info("Updating display order for category ID: {} to: {}", id, displayOrder);
        
        // Validate category exists
        categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        
        categoryRepository.updateDisplayOrder(id, displayOrder);
        
        logger.info("Display order updated successfully for category ID: {}", id);
    }
    
    /**
     * Get category count
     */
    @Transactional(readOnly = true)
    public long getCategoryCount() {
        return categoryRepository.countByIsActiveTrue();
    }
}