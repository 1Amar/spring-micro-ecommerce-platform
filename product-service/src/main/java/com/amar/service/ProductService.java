package com.amar.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.amar.client.InventoryServiceClient;
import com.amar.dto.InventoryDto;
import com.amar.dto.ProductDto;
import com.amar.entity.product.Category;
import com.amar.entity.product.Product;
import com.amar.exception.DuplicateResourceException;
import com.amar.exception.ResourceNotFoundException;
import com.amar.mapper.ProductMapperMS;
import com.amar.repository.CategoryRepository;
import com.amar.repository.ProductRepository;

@Service
@Transactional
public class ProductService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ProductMapperMS productMapperMS;
    
    @Autowired
    private InventoryServiceClient inventoryServiceClient;
    
    /**
     * Create a new product
     */
    public ProductDto createProduct(ProductDto productDto) {
        logger.info("Creating new product with SKU: {}", productDto.getSku());
        
        // Validate SKU uniqueness
        if (productRepository.existsBySkuAndIdNot(productDto.getSku(), 0L)) {
            throw new DuplicateResourceException("Product with SKU '" + productDto.getSku() + "' already exists");
        }
        
        // Validate category if provided
        Category category = null;
        if (productDto.getCategoryId() != null) {
            category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + productDto.getCategoryId()));
        }
        
        Product product = productMapperMS.toEntity(productDto);
        product.setCategory(category);
        
        // Set defaults
        if (product.getIsActive() == null) product.setIsActive(true);
        if (product.getIsFeatured() == null) product.setIsFeatured(false);
        if (product.getTrackInventory() == null) product.setTrackInventory(true);
        if (product.getStockQuantity() == null) product.setStockQuantity(0);
        if (product.getLowStockThreshold() == null) product.setLowStockThreshold(0);
        if (product.getSortOrder() == null) product.setSortOrder(0);
        
        Product savedProduct = productRepository.save(product);
        logger.info("Product created successfully with ID: {}", savedProduct.getId());
        
        return productMapperMS.toDto(savedProduct);
    }
    
    /**
     * Update an existing product
     */
    public ProductDto updateProduct(Long id, ProductDto productDto) {
        logger.info("Updating product with ID: {}", id);
        
        Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        
        // Validate SKU uniqueness (excluding current product)
        if (!existingProduct.getSku().equals(productDto.getSku()) && 
            productRepository.existsBySkuAndIdNot(productDto.getSku(), id)) {
            throw new DuplicateResourceException("Product with SKU '" + productDto.getSku() + "' already exists");
        }
        
        // Validate category if provided
        Category category = null;
        if (productDto.getCategoryId() != null) {
            category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + productDto.getCategoryId()));
        }
        
        // Update fields
        productMapperMS.updateEntityFromDto(productDto, existingProduct);
        existingProduct.setCategory(category);
        
        Product updatedProduct = productRepository.save(existingProduct);
        logger.info("Product updated successfully with ID: {}", updatedProduct.getId());
        
        return productMapperMS.toDto(updatedProduct);
    }
    
    /**
     * Get product by ID with real-time inventory
     */
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        logger.debug("Fetching product with ID: {}", id);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        
        ProductDto productDto = productMapperMS.toDto(product);
        
        // Enrich with real-time inventory data
        enrichWithInventoryData(productDto);
        
        return productDto;
    }
    
    /**
     * Get product by SKU
     */
    @Transactional(readOnly = true)
    public ProductDto getProductBySku(String sku) {
        logger.debug("Fetching product with SKU: {}", sku);
        
        Product product = productRepository.findBySku(sku)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        
        return productMapperMS.toDto(product);
    }
    
    /**
     * Get product by slug
     */
    @Transactional(readOnly = true)
    public ProductDto getProductBySlug(String slug) {
        logger.debug("Fetching product with slug: {}", slug);
        
        Product product = productRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        
        return productMapperMS.toDto(product);
    }
    
    /**
     * Get all products with pagination and real-time inventory
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getAllProducts(Pageable pageable) {
        logger.debug("Fetching all products with pagination: {}", pageable);
        
        // PERFORMANCE FIX: Use pageable directly instead of loading all 1.4M+ products
        Page<Product> productPage = productRepository.findAll(pageable);
        
        // Convert to DTOs
        List<ProductDto> productDtos = productPage.getContent().stream()
                .map(productMapperMS::toDto)
                .toList();
        
        // Enrich with bulk inventory data
        enrichWithBulkInventoryData(productDtos);
        
        // Return paginated result using database pagination
        return new PageImpl<>(productDtos, pageable, productPage.getTotalElements());
    }
    
    /**
     * Get products by category with real-time inventory
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsByCategory(Long categoryId, Pageable pageable) {
        logger.debug("Fetching products for category ID: {} with pagination: {}", categoryId, pageable);
        
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
        
        Page<Product> products = productRepository.findByCategoryAndIsActiveTrue(category, pageable);
        Page<ProductDto> productDtos = products.map(productMapperMS::toDto);
        
        // Enrich with bulk inventory data
        enrichWithBulkInventoryData(productDtos.getContent());
        
        return productDtos;
    }
    
    /**
     * Get featured products
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getFeaturedProducts(Pageable pageable) {
        logger.debug("Fetching featured products with pagination: {}", pageable);
        
        Page<Product> products = productRepository.findByIsFeaturedTrueAndIsActiveTrue(pageable);
        return products.map(productMapperMS::toDto);
    }
    
    /**
     * Search products by name
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> searchProductsByName(String name, Pageable pageable) {
        logger.debug("Searching products by name: {} with pagination: {}", name, pageable);
        
        Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(name, pageable);
        return products.map(productMapperMS::toDto);
    }
    
    /**
     * Advanced product search
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> searchProducts(String name, Long categoryId, String brand, 
                                          BigDecimal minPrice, BigDecimal maxPrice, 
                                          Boolean inStock, Pageable pageable) {
        logger.debug("Advanced product search - name: {}, category: {}, brand: {}, price range: {}-{}, inStock: {}", 
                    name, categoryId, brand, minPrice, maxPrice, inStock);
        
        Page<Product> products = productRepository.searchProducts(
            StringUtils.hasText(name) ? name : null,
            categoryId,
            StringUtils.hasText(brand) ? brand : null,
            minPrice,
            maxPrice,
            inStock,
            pageable
        );
        
        return products.map(productMapperMS::toDto);
    }
    
    /**
     * Get products on sale
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsOnSale(Pageable pageable) {
        logger.debug("Fetching products on sale with pagination: {}", pageable);
        
        Page<Product> products = productRepository.findProductsOnSale(pageable);
        return products.map(productMapperMS::toDto);
    }
    
    /**
     * Get related products
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getRelatedProducts(Long productId, int limit) {
        logger.debug("Fetching related products for product ID: {}, limit: {}", productId, limit);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        String searchTerm = extractSearchTermFromName(product.getName());
        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        
        List<Product> relatedProducts = productRepository.findRelatedProducts(
            productId, searchTerm, categoryId, Pageable.ofSize(limit)
        );
        
        return relatedProducts.stream()
                             .map(productMapperMS::toDto)
                             .collect(Collectors.toList());
    }
    
    /**
     * Update stock quantity
     */
    public void updateStockQuantity(Long productId, Integer newQuantity) {
        logger.info("Updating stock quantity for product ID: {} to: {}", productId, newQuantity);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        if (!product.getTrackInventory()) {
            logger.warn("Attempting to update stock for product ID: {} that doesn't track inventory", productId);
            return;
        }
        
        product.setStockQuantity(newQuantity);
        productRepository.save(product);
        
        logger.info("Stock quantity updated successfully for product ID: {}", productId);
    }
    
    /**
     * Adjust stock quantity (add or subtract)
     */
    public void adjustStockQuantity(Long productId, Integer adjustment) {
        logger.info("Adjusting stock quantity for product ID: {} by: {}", productId, adjustment);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        if (!product.getTrackInventory()) {
            logger.warn("Attempting to adjust stock for product ID: {} that doesn't track inventory", productId);
            return;
        }
        
        int newQuantity = Math.max(0, product.getStockQuantity() + adjustment);
        product.setStockQuantity(newQuantity);
        productRepository.save(product);
        
        logger.info("Stock quantity adjusted successfully for product ID: {} to: {}", productId, newQuantity);
    }
    
    /**
     * Get low stock products
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getLowStockProducts() {
        logger.debug("Fetching low stock products");
        
        List<Product> products = productRepository.findLowStockProducts();
        return products.stream()
                      .map(productMapperMS::toDto)
                      .collect(Collectors.toList());
    }
    
    /**
     * Get out of stock products
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getOutOfStockProducts() {
        logger.debug("Fetching out of stock products");
        
        List<Product> products = productRepository.findOutOfStockProducts();
        return products.stream()
                      .map(productMapperMS::toDto)
                      .collect(Collectors.toList());
    }
    
    /**
     * Get distinct brands
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctBrands() {
        logger.debug("Fetching distinct brands");
        return productRepository.findDistinctBrands();
    }
    
    /**
     * Get price range
     */
    @Transactional(readOnly = true)
    public BigDecimal[] getPriceRange() {
        logger.debug("Fetching price range");
        
        Object[] range = productRepository.findPriceRange();
        if (range != null && range.length == 2) {
            return new BigDecimal[]{(BigDecimal) range[0], (BigDecimal) range[1]};
        }
        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
    }
    
    /**
     * Delete product (soft delete - set inactive)
     */
    public void deleteProduct(Long id) {
        logger.info("Soft deleting product with ID: {}", id);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        
        product.setIsActive(false);
        productRepository.save(product);
        
        logger.info("Product soft deleted successfully with ID: {}", id);
    }
    
    /**
     * Restore product (set active)
     */
    public void restoreProduct(Long id) {
        logger.info("Restoring product with ID: {}", id);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        
        product.setIsActive(true);
        productRepository.save(product);
        
        logger.info("Product restored successfully with ID: {}", id);
    }
    
    /**
     * Toggle featured status
     */
    public ProductDto toggleFeaturedStatus(Long id) {
        logger.info("Toggling featured status for product ID: {}", id);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        
        product.setIsFeatured(!product.getIsFeatured());
        Product updatedProduct = productRepository.save(product);
        
        logger.info("Featured status toggled for product ID: {} to: {}", id, updatedProduct.getIsFeatured());
        
        return productMapperMS.toDto(updatedProduct);
    }
    
    // Helper methods
    private String extractSearchTermFromName(String name) {
        if (!StringUtils.hasText(name)) return "";
        
        // Extract first meaningful word from product name
        String[] words = name.split("\\s+");
        return words.length > 0 ? words[0] : name;
    }
    
    /**
     * Enrich single product with real-time inventory data
     */
    private void enrichWithInventoryData(ProductDto productDto) {
        if (productDto == null || productDto.getId() == null) {
            return;
        }
        
        try {
            Optional<InventoryDto> inventory = inventoryServiceClient.getInventoryByProductId(productDto.getId());
            if (inventory.isPresent()) {
                productDto.setInventory(inventory.get());
                logger.debug("Product ID {} enriched with inventory data", productDto.getId());
            } else {
                logger.debug("No inventory data found for product ID {}", productDto.getId());
            }
        } catch (Exception ex) {
            logger.warn("Failed to fetch inventory for product ID {}: {}", productDto.getId(), ex.getMessage());
            // Continue without inventory data - graceful degradation
        }
    }
    
    /**
     * Enrich multiple products with bulk inventory data
     */
    private void enrichWithBulkInventoryData(List<ProductDto> productDtos) {
        if (productDtos == null || productDtos.isEmpty()) {
            return;
        }
        
        try {
            // Extract product IDs
            List<Long> productIds = productDtos.stream()
                    .map(ProductDto::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            if (productIds.isEmpty()) {
                return;
            }
            
            // Fetch inventory data for all products in one call
            Map<Long, InventoryDto> inventoryMap = inventoryServiceClient.getInventoryForProducts(productIds);
            
            // Apply inventory data to products
            for (ProductDto productDto : productDtos) {
                if (productDto.getId() != null && inventoryMap.containsKey(productDto.getId())) {
                    productDto.setInventory(inventoryMap.get(productDto.getId()));
                }
            }
            
            logger.debug("Bulk enriched {} products with inventory data from {} inventory records", 
                        productDtos.size(), inventoryMap.size());
                        
        } catch (Exception ex) {
            logger.warn("Failed to fetch bulk inventory data: {}", ex.getMessage());
            // Continue without inventory data - graceful degradation
        }
    }
}