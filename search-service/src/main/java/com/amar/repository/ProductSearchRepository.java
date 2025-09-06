package com.amar.repository;

import com.amar.document.ProductSearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, String> {

    /**
     * Search products by name containing the given text (case insensitive)
     */
    Page<ProductSearchDocument> findByNameContaining(String name, Pageable pageable);

    /**
     * Search products by description containing the given text
     */
    Page<ProductSearchDocument> findByDescriptionContaining(String description, Pageable pageable);

    /**
     * Search by brand
     */
    Page<ProductSearchDocument> findByBrand(String brand, Pageable pageable);

    /**
     * Search by category
     */
    Page<ProductSearchDocument> findByCategoryName(String categoryName, Pageable pageable);

    /**
     * Search by price range
     */
    @Query("{\"range\": {\"price\": {\"gte\": ?0, \"lte\": ?1}}}")
    Page<ProductSearchDocument> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);

    /**
     * Find by product ID
     */
    ProductSearchDocument findByProductId(Long productId);

    /**
     * Find by SKU
     */
    ProductSearchDocument findBySku(String sku);

    /**
     * Count all products in index
     */
    long count();
}