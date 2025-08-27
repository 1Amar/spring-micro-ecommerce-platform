package com.amar.service;

import com.amar.repository.BulkProductImportRepository;
import com.amar.repository.CategoryRepository;
import com.amar.entity.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmazonBulkImportService {
    
    private final BulkProductImportRepository bulkRepository;
    private final CategoryRepository categoryRepository;
    
    private final Map<Long, Category> categoryCache = new HashMap<>();
    private Long defaultCategoryId = null;
    
    /**
     * Import categories from amazon_categories.csv
     */
    public BulkImportResult importCategoriesFromCsv(String csvFilePath) {
        log.info("Starting categories import from: {}", csvFilePath);
        
        BulkImportResult result = new BulkImportResult();
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            int lineNumber = 0;
            
            while ((line = br.readLine()) != null) {
                lineNumber++;
                
                if (lineNumber == 1) {
                    // Skip header: id,category_name
                    continue;
                }
                
                try {
                    String[] values = parseCsvLine(line);
                    if (values.length < 2) {
                        log.warn("Line {}: Insufficient columns", lineNumber);
                        result.skippedCount++;
                        continue;
                    }
                    
                    String idStr = values[0].trim();
                    String categoryName = values[1].trim();
                    
                    if (isBlank(idStr) || isBlank(categoryName)) {
                        log.warn("Line {}: Missing ID or name", lineNumber);
                        result.skippedCount++;
                        continue;
                    }
                    
                    try {
                        Long categoryId = Long.parseLong(idStr);
                        String slug = generateSlug(categoryName);
                        
                        // Insert category
                        int rowsAffected = categoryRepository.insertCategory(categoryId, categoryName, slug);
                        if (rowsAffected > 0) {
                            result.successCount++;
                        } else {
                            result.duplicateCount++;
                        }
                        
                        if (result.successCount % 50 == 0) {
                            log.info("Processed {} categories", result.successCount);
                        }
                        
                    } catch (NumberFormatException e) {
                        log.warn("Line {}: Invalid category ID: {}", lineNumber, idStr);
                        result.skippedCount++;
                    } catch (Exception e) {
                        log.error("Line {}: Error inserting category: {}", lineNumber, e.getMessage());
                        result.errorCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing line {}: {}", lineNumber, e.getMessage());
                    result.errorCount++;
                }
            }
            
        } catch (IOException e) {
            log.error("Error reading categories CSV file: {}", e.getMessage());
            throw new RuntimeException("Failed to read categories CSV file", e);
        }
        
        result.endTime = java.time.LocalDateTime.now();
        log.info("Categories import completed: {}", result);
        
        return result;
    }

    /**
     * High-performance bulk import using JdbcTemplate.batchUpdate()
     */
    public BulkImportResult importProductsFromCsv(String csvFilePath, int batchSize) {
        log.info("Starting JDBC bulk import from: {} with batch size: {}", csvFilePath, batchSize);
        
        BulkImportResult result = new BulkImportResult();
        loadCategoryCache();
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            String[] headers = null;
            int lineNumber = 0;
            
            List<Map<String, Object>> productBatch = new ArrayList<>();
            
            while ((line = br.readLine()) != null) {
                lineNumber++;
                
                if (lineNumber == 1) {
                    headers = parseCsvLine(line);
                    log.info("CSV headers: {}", String.join(", ", headers));
                    continue;
                }
                
                try {
                    String[] values = parseCsvLine(line);
                    if (values.length != headers.length) {
                        log.warn("Line {}: Column mismatch. Expected {}, got {}", 
                                lineNumber, headers.length, values.length);
                        result.skippedCount++;
                        continue;
                    }
                    
                    Map<String, String> row = createRowMap(headers, values);
                    
                    // Skip if essential fields missing
                    if (isBlank(row.get("title")) || isBlank(row.get("price"))) {
                        result.skippedCount++;
                        continue;
                    }
                    
                    Map<String, Object> product = createProductMap(row);
                    if (product != null) {
                        productBatch.add(product);
                    } else {
                        result.skippedCount++;
                        continue;
                    }
                    
                    // Process batch when full
                    if (productBatch.size() >= batchSize) {
                        processBatch(productBatch, result, productBatch.size());
                        productBatch.clear();
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing line {}: {}", lineNumber, e.getMessage());
                    result.errorCount++;
                }
            }
            
            // Process remaining items
            if (!productBatch.isEmpty()) {
                processBatch(productBatch, result, productBatch.size());
            }
            
        } catch (IOException e) {
            log.error("Error reading CSV file: {}", e.getMessage());
            throw new RuntimeException("Failed to read CSV file", e);
        }
        
        result.endTime = LocalDateTime.now();
        log.info("JDBC bulk import completed. Success: {}, Errors: {}, Skipped: {}, Duration: {}s", 
                result.successCount, result.errorCount, result.skippedCount, result.getDurationSeconds());
        
        return result;
    }
    
    @Transactional
    private void processBatch(List<Map<String, Object>> productBatch, 
                             BulkImportResult result, 
                             int batchNumber) {
        try {
            // Batch insert products
            int[] productResults = bulkRepository.batchInsertProducts(productBatch);
            
            // Count successful inserts (non-zero means inserted, zero means duplicate/conflict)
            int productSuccess = Arrays.stream(productResults).sum();
            result.successCount += productSuccess;
            result.duplicateCount += (productBatch.size() - productSuccess);
            
            result.batchCount++;
            log.info("Completed batch {} - {} products processed ({} inserted, {} duplicates)", 
                    result.batchCount, productBatch.size(), productSuccess, productBatch.size() - productSuccess);
            
            // Log major milestones
            if (result.successCount > 0 && result.successCount % 10000 == 0) {
                log.info("🎉 Major milestone: {} products successfully imported!", result.successCount);
            }
            
        } catch (Exception e) {
            log.error("Batch processing failed: {}", e.getMessage());
            result.errorCount += productBatch.size();
        }
    }
    
    private void loadCategoryCache() {
        log.info("Loading category cache...");
        categoryRepository.findAll().forEach(category -> {
            categoryCache.put(category.getId(), category);
            // Look for default category (Uncategorized, Other, etc.)
            if (category.getName().equalsIgnoreCase("Uncategorized") || 
                category.getName().equalsIgnoreCase("Other") ||
                category.getName().equalsIgnoreCase("Default")) {
                defaultCategoryId = category.getId();
            }
        });
        
        // If no default category exists, use the first category or create one
        if (defaultCategoryId == null && !categoryCache.isEmpty()) {
            defaultCategoryId = categoryCache.keySet().iterator().next();
            log.info("Using first available category as default: ID {}", defaultCategoryId);
        }
        
        log.info("Loaded {} categories into cache, default category ID: {}", categoryCache.size(), defaultCategoryId);
    }
    
    private Map<String, Object> createProductMap(Map<String, String> row) {
        Map<String, Object> product = new HashMap<>();
        
        String title = row.get("title");
        String priceStr = row.get("price");
        String imgUrl = row.get("imgUrl");
        
        product.put("name", truncate(title, 200));
        product.put("description", title);
        product.put("sku", generateUniqueSku(title)); // Generate SKU from title
        product.put("slug", generateSlug(title)); // Generate slug from title
        
        // Parse price
        BigDecimal price = parseDecimal(priceStr);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // Skip products with invalid prices
        }
        product.put("price", price);
        
        // Parse list price
        BigDecimal listPrice = parseDecimal(row.get("listPrice"));
        product.put("compareAtPrice", listPrice);
        
        // Parse stars
        BigDecimal stars = parseDecimal(row.get("stars"));
        product.put("stars", stars);
        
        // Parse review count
        Integer reviewCount = parseInteger(row.get("reviews"));
        product.put("reviewCount", reviewCount);
        
        // Parse bestseller flag
        String isBestSellerStr = row.get("isBestSeller");
        boolean isBestSeller = "True".equalsIgnoreCase(isBestSellerStr) || "1".equals(isBestSellerStr);
        product.put("isBestSeller", isBestSeller);
        
        // Parse bought in last month
        Integer boughtInLastMonth = parseInteger(row.get("boughtInLastMonth"));
        product.put("boughtInLastMonth", boughtInLastMonth);
        
        // Category association - always provide a valid category ID
        String categoryIdStr = row.get("category_id");
        Long categoryId = defaultCategoryId; // Default to "Uncategorized" category
        
        if (!isBlank(categoryIdStr)) {
            try {
                Long parsedCategoryId = Long.parseLong(categoryIdStr);
                if (categoryCache.containsKey(parsedCategoryId)) {
                    categoryId = parsedCategoryId; // Use valid category
                } else {
                    log.debug("Category not found for id: {} (product: {}), using default category", parsedCategoryId, title);
                }
            } catch (NumberFormatException e) {
                log.debug("Invalid category_id: {} (product: {}), using default category", categoryIdStr, title);
            }
        }
        
        product.put("categoryId", categoryId);
        
        product.put("productUrl", row.get("productURL"));
        product.put("brand", "Amazon"); // Default brand for Amazon products
        
        // Add image fields directly to product
        if (!isBlank(imgUrl)) {
            product.put("imageUrl", imgUrl);
            product.put("imageAltText", truncate(title, 200)); // Use title as alt text
        }
        
        return product;
    }
    
    
    private Map<String, String> createRowMap(String[] headers, String[] values) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.length && i < values.length; i++) {
            row.put(headers[i].trim(), values[i].trim());
        }
        return row;
    }
    
    private String[] parseCsvLine(String line) {
        return line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
    }
    
    private BigDecimal parseDecimal(String str) {
        if (isBlank(str) || "0.0".equals(str)) return null;
        try {
            return new BigDecimal(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Integer parseInteger(String str) {
        if (isBlank(str)) return null;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
    
    private String generateUniqueSku(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "PROD-" + System.currentTimeMillis();
        }
        
        // Create SKU from title: take first 2 words, limit length, add timestamp
        String[] words = title.trim().toLowerCase().split("\\s+");
        StringBuilder skuBuilder = new StringBuilder("PROD-");
        
        int wordCount = Math.min(words.length, 2);
        for (int i = 0; i < wordCount; i++) {
            String word = words[i].replaceAll("[^a-z0-9]", "");
            if (word.length() > 0) {
                skuBuilder.append(word.substring(0, Math.min(word.length(), 6))); // Max 6 chars per word
                if (i < wordCount - 1) skuBuilder.append("-");
            }
        }
        
        // Add timestamp suffix for uniqueness (max 5 digits)
        skuBuilder.append("-").append(System.currentTimeMillis() % 99999);
        
        // Ensure total length <= 50 characters (well under 100 limit)
        String sku = skuBuilder.toString().toUpperCase();
        if (sku.length() > 50) {
            sku = sku.substring(0, 50);
        }
        
        return sku;
    }
    
    private String generateSlug(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "product-" + System.currentTimeMillis();
        }
        
        // Create base slug from title: lowercase, replace special chars with dashes, remove consecutive dashes
        String baseSlug = title.toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters except spaces and dashes
                    .replaceAll("\\s+", "-")          // Replace spaces with dashes
                    .replaceAll("-+", "-")            // Remove consecutive dashes
                    .replaceAll("^-+|-+$", "");       // Remove leading/trailing dashes
        
        // Add timestamp suffix to ensure uniqueness
        String uniqueSuffix = "-" + (System.currentTimeMillis() % 100000);
        
        // Limit total length to 200 chars (database constraint)
        int maxBaseLength = 200 - uniqueSuffix.length();
        if (baseSlug.length() > maxBaseLength) {
            baseSlug = baseSlug.substring(0, maxBaseLength);
        }
        
        return baseSlug + uniqueSuffix;
    }
    
    /**
     * Import result with JDBC-specific metrics
     */
    public static class BulkImportResult {
        public int successCount = 0;
        public int errorCount = 0;
        public int skippedCount = 0;
        public int duplicateCount = 0;
        public int batchCount = 0;
        public LocalDateTime startTime = LocalDateTime.now();
        public LocalDateTime endTime;
        
        public long getDurationSeconds() {
            if (endTime == null) return 0;
            return java.time.Duration.between(startTime, endTime).getSeconds();
        }
        
        public int getTotalProcessed() {
            return successCount + errorCount + skippedCount + duplicateCount;
        }
        
        public double getSuccessRate() {
            int total = getTotalProcessed();
            return total > 0 ? (double) successCount / total * 100.0 : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("BulkImportResult{success=%d, errors=%d, skipped=%d, duplicates=%d, batches=%d, duration=%ds, successRate=%.1f%%}", 
                    successCount, errorCount, skippedCount, duplicateCount, batchCount, getDurationSeconds(), getSuccessRate());
        }
    }
}