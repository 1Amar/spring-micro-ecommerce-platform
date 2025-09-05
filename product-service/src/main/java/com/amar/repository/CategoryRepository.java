package com.amar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.amar.entity.product.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Find category by name
     */
    Optional<Category> findByName(String name);
    
    /**
     * Find category by slug
     */
    Optional<Category> findBySlug(String slug);
    
    /**
     * Find all active categories
     */
    List<Category> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    /**
     * Find all active categories with pagination
     */
    Page<Category> findByIsActiveTrue(Pageable pageable);
    
    /**
     * Find parent categories (categories with no parent)
     */
    List<Category> findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();
    
    /**
     * Find children categories by parent ID
     */
    List<Category> findByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(Long parentId);
    
    /**
     * Find categories by name containing (case insensitive search)
     */
    List<Category> findByNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(String name);
    
    /**
     * Check if category name exists (excluding current category for updates)
     */
    boolean existsByNameAndIdNot(String name, Long id);
    
    /**
     * Check if category slug exists (excluding current category for updates)
     */
    boolean existsBySlugAndIdNot(String slug, Long id);
    
    /**
     * Find categories with products count
     */
    @Query("SELECT c, COUNT(p) as productCount " +
           "FROM Category c LEFT JOIN c.products p " +
           "WHERE c.isActive = true " +
           "GROUP BY c " +
           "ORDER BY c.displayOrder ASC")
    List<Object[]> findCategoriesWithProductCount();
    
    /**
     * Find category hierarchy (parent and all children)
     */
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId OR c.id = :parentId ORDER BY c.displayOrder ASC")
    List<Category> findCategoryHierarchy(@Param("parentId") Long parentId);
    
    /**
     * Find all categories in hierarchy by root category
     */
    @Query(value = "WITH RECURSIVE category_hierarchy AS (" +
           "SELECT id, name, parent_id, display_order, 0 as level " +
           "FROM categories WHERE id = :rootId " +
           "UNION ALL " +
           "SELECT c.id, c.name, c.parent_id, c.display_order, ch.level + 1 " +
           "FROM categories c " +
           "INNER JOIN category_hierarchy ch ON c.parent_id = ch.id" +
           ") " +
           "SELECT * FROM category_hierarchy ORDER BY level, display_order", 
           nativeQuery = true)
    List<Object[]> findCategoryHierarchyNative(@Param("rootId") Long rootId);
    
    /**
     * Update display orders for categories
     */
    @Query("UPDATE Category c SET c.displayOrder = :displayOrder WHERE c.id = :categoryId")
    void updateDisplayOrder(@Param("categoryId") Long categoryId, @Param("displayOrder") Integer displayOrder);
    
    /**
     * Count active categories
     */
    long countByIsActiveTrue();
    
    /**
     * Find categories by parent category
     */
    List<Category> findByParentAndIsActiveTrueOrderByDisplayOrderAsc(Category parent);
    
    /**
     * Insert category with specific ID (for CSV import)
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO categories (id, name, slug, is_active, display_order, created_at, updated_at) " +
                   "VALUES (:id, :name, :slug, true, :displayOrder, NOW(), NOW()) " +
                   "ON CONFLICT (id) DO NOTHING", 
           nativeQuery = true)
    int insertCategory(@Param("id") Long id, 
                      @Param("name") String name, 
                      @Param("slug") String slug, 
                      @Param("displayOrder") Integer displayOrder);
    
    /**
     * Insert category with specific ID (for CSV import) - simplified version
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO categories (id, name, slug, is_active, display_order, created_at, updated_at) " +
                   "VALUES (:id, :name, :slug, true, :id, NOW(), NOW()) " +
                   "ON CONFLICT (id) DO NOTHING", 
           nativeQuery = true)
    int insertCategory(@Param("id") Long id, 
                      @Param("name") String name, 
                      @Param("slug") String slug);
}