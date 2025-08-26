package com.amar.repository;

import com.amar.entity.Product;
import com.amar.entity.Category;
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
    
    /**
     * Find product by SKU
     */
    Optional<Product> findBySku(String sku);
    
    /**
     * Find product by slug
     */
    Optional<Product> findBySlug(String slug);
    
    /**
     * Find all active products
     */
    Page<Product> findByIsActiveTrue(Pageable pageable);
    
    /**
     * Find all products with category eagerly loaded
     */
    // @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.isActive = true")
    // List<Product> findAllWithCategory();
    
    /**
     * Find product by ID with category eagerly loaded
     */
    // @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id")
    // Optional<Product> findByIdWithCategory(@Param("id") Long id);
    
    /**
     * Find products by category
     */
    Page<Product> findByCategoryAndIsActiveTrue(Category category, Pageable pageable);
    
    /**
     * Find products by category ID
     */
    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);
    
    /**
     * Find featured products
     */
    Page<Product> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);
    
    /**
     * Find products by name containing (case insensitive search)
     */
    Page<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name, Pageable pageable);
    
    /**
     * Find products by brand
     */
    Page<Product> findByBrandIgnoreCaseAndIsActiveTrue(String brand, Pageable pageable);
    
    /**
     * Find products by price range
     */
    Page<Product> findByPriceBetweenAndIsActiveTrue(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    /**
     * Find products on sale
     */
    @Query("SELECT p FROM Product p WHERE p.compareAtPrice IS NOT NULL AND p.compareAtPrice > p.price AND p.isActive = true")
    Page<Product> findProductsOnSale(Pageable pageable);
    
    /**
     * Find low stock products
     */
    @Query("SELECT p FROM Product p WHERE p.trackInventory = true AND p.stockQuantity <= p.lowStockThreshold AND p.isActive = true")
    List<Product> findLowStockProducts();
    
    /**
     * Find out of stock products
     */
    @Query("SELECT p FROM Product p WHERE p.trackInventory = true AND p.stockQuantity = 0 AND p.isActive = true")
    List<Product> findOutOfStockProducts();
    
    /**
     * Search products by multiple criteria
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:inStock IS NULL OR (:inStock = true AND (p.trackInventory = false OR p.stockQuantity > 0)) OR (:inStock = false AND p.trackInventory = true AND p.stockQuantity = 0)) AND " +
           "p.isActive = true")
    Page<Product> searchProducts(
        @Param("name") String name,
        @Param("categoryId") Long categoryId,
        @Param("brand") String brand,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("inStock") Boolean inStock,
        Pageable pageable
    );
    
    /**
     * Find products with similar names for recommendations
     */
    @Query("SELECT p FROM Product p WHERE " +
           "p.id != :productId AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " p.category.id = :categoryId) AND " +
           "p.isActive = true " +
           "ORDER BY " +
           "CASE WHEN p.category.id = :categoryId THEN 1 ELSE 2 END, " +
           "p.isFeatured DESC, " +
           "p.createdAt DESC")
    List<Product> findRelatedProducts(
        @Param("productId") Long productId,
        @Param("searchTerm") String searchTerm,
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );
    
    /**
     * Check if SKU exists (excluding current product for updates)
     */
    boolean existsBySkuAndIdNot(String sku, Long id);
    
    /**
     * Check if slug exists (excluding current product for updates)
     */
    boolean existsBySlugAndIdNot(String slug, Long id);
    
    /**
     * Count products by category
     */
    long countByCategoryAndIsActiveTrue(Category category);
    
    /**
     * Count products by category ID
     */
    long countByCategoryIdAndIsActiveTrue(Long categoryId);
    
    /**
     * Find distinct brands
     */
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.isActive = true ORDER BY p.brand")
    List<String> findDistinctBrands();
    
    /**
     * Get price range
     */
    @Query("SELECT MIN(p.price), MAX(p.price) FROM Product p WHERE p.isActive = true")
    Object[] findPriceRange();
    
    /**
     * Find products by tags
     */
    @Query("SELECT p FROM Product p WHERE p.tags LIKE CONCAT('%', :tag, '%') AND p.isActive = true")
    Page<Product> findByTagsContaining(@Param("tag") String tag, Pageable pageable);
    
    /**
     * Update stock quantity
     */
    @Query("UPDATE Product p SET p.stockQuantity = :stockQuantity, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :productId")
    void updateStockQuantity(@Param("productId") Long productId, @Param("stockQuantity") Integer stockQuantity);
    
    /**
     * Bulk update stock quantities
     */
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :adjustment, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id IN :productIds")
    void bulkUpdateStockQuantity(@Param("productIds") List<Long> productIds, @Param("adjustment") Integer adjustment);
    
    /**
     * Find recently added products
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findRecentProducts(Pageable pageable);
    
    /**
     * Find best selling products (placeholder - would need order data)
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.isFeatured DESC, p.createdAt DESC")
    List<Product> findBestSellingProducts(Pageable pageable);
}