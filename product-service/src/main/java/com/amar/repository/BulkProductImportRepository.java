package com.amar.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class BulkProductImportRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Batch insert products using JDBC for optimal performance
     */
    public int[] batchInsertProducts(List<Map<String, Object>> productBatches) {
        String sql = """
            INSERT INTO products (
                name, description, sku, slug, price, compare_at_price, stock_quantity, low_stock_threshold,
                track_inventory, is_active, is_featured, brand, category_id, product_url,
                stars, review_count, is_best_seller, bought_in_last_month, 
                image_url, image_alt_text, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (sku) DO NOTHING
            """;
        
        List<Object[]> batchArgs = productBatches.stream()
            .map(this::mapProductToArray)
            .toList();
            
        log.debug("Executing batch insert for {} products", batchArgs.size());
        return jdbcTemplate.batchUpdate(sql, batchArgs);
    }
    
    
    
    private Object[] mapProductToArray(Map<String, Object> product) {
        LocalDateTime now = LocalDateTime.now();
        
        return new Object[]{
            product.get("name"),                    // name
            product.get("description"),             // description  
            product.get("sku"),                     // sku
            product.get("slug"),                    // slug
            product.get("price"),                   // price
            product.get("compareAtPrice"),          // compare_at_price
            0,                                      // stock_quantity
            1,                                      // low_stock_threshold (must be > 0)
            false,                                  // track_inventory
            true,                                   // is_active
            product.get("isBestSeller"),           // is_featured
            product.get("brand"),                   // brand
            product.get("categoryId"),              // category_id
            product.get("productUrl"),              // product_url
            product.get("stars"),                   // stars
            product.get("reviewCount"),             // review_count
            product.get("isBestSeller"),           // is_best_seller
            product.get("boughtInLastMonth"),      // bought_in_last_month
            product.get("imageUrl"),                // image_url
            product.get("imageAltText"),            // image_alt_text
            now,                                    // created_at
            now                                     // updated_at
        };
    }
    
}