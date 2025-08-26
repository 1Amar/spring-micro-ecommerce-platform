package com.amar.controller;

import com.amar.dto.ProductDto;
import com.amar.service.ProductService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products/catalog")
@Validated
public class ProductControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(ProductControllerV2.class);
    
    @Autowired
    private ProductService productService;

    @GetMapping("/health")
    public String health() {
        logger.info("Product catalog health check endpoint called");
        return "Product Catalog Service is running!";
    }
    
    /**
     * Create a new product
     */
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
        logger.info("Creating new product with SKU: {}", productDto.getSku());
        ProductDto createdProduct = productService.createProduct(productDto);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }
    
    /**
     * Update an existing product
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody ProductDto productDto) {
        logger.info("Updating product with ID: {}", id);
        ProductDto updatedProduct = productService.updateProduct(id, productDto);
        return ResponseEntity.ok(updatedProduct);
    }
    
    /**
     * Get product by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable @Min(1) Long id) {
        logger.info("Fetching product with ID: {}", id);
        ProductDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
    /**
     * Get product by SKU
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductDto> getProductBySku(@PathVariable String sku) {
        logger.info("Fetching product with SKU: {}", sku);
        ProductDto product = productService.getProductBySku(sku);
        return ResponseEntity.ok(product);
    }
    
    /**
     * Get product by slug
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductDto> getProductBySlug(@PathVariable String slug) {
        logger.info("Fetching product with slug: {}", slug);
        ProductDto product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(product);
    }
    
    /**
     * Get all products with pagination and sorting
     */
    @GetMapping
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        logger.info("Fetching all products - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductDto> products = productService.getAllProducts(pageable);
        
        return ResponseEntity.ok(products);
    }
    
    /**
     * Get products by category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductDto>> getProductsByCategory(
            @PathVariable @Min(1) Long categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        logger.info("Fetching products for category: {}", categoryId);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductDto> products = productService.getProductsByCategory(categoryId, pageable);
        
        return ResponseEntity.ok(products);
    }
    
    /**
     * Get featured products
     */
    @GetMapping("/featured")
    public ResponseEntity<Page<ProductDto>> getFeaturedProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        logger.info("Fetching featured products");
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductDto> products = productService.getFeaturedProducts(pageable);
        
        return ResponseEntity.ok(products);
    }
    
    /**
     * Search products by name
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDto>> searchProductsByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        logger.info("Searching products by name: {}", name);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductDto> products = productService.searchProductsByName(name, pageable);
        
        return ResponseEntity.ok(products);
    }
    
    /**
     * Advanced product search with filters
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<Page<ProductDto>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        logger.info("Advanced product search - name: {}, category: {}, brand: {}, price: {}-{}, inStock: {}", 
                   name, categoryId, brand, minPrice, maxPrice, inStock);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductDto> products = productService.searchProducts(name, categoryId, brand, minPrice, maxPrice, inStock, pageable);
        
        return ResponseEntity.ok(products);
    }
    
    /**
     * Get products on sale
     */
    @GetMapping("/sale")
    public ResponseEntity<Page<ProductDto>> getProductsOnSale(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        logger.info("Fetching products on sale");
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductDto> products = productService.getProductsOnSale(pageable);
        
        return ResponseEntity.ok(products);
    }
    
    /**
     * Get related products
     */
    @GetMapping("/{id}/related")
    public ResponseEntity<List<ProductDto>> getRelatedProducts(
            @PathVariable @Min(1) Long id,
            @RequestParam(defaultValue = "5") @Min(1) int limit) {
        
        logger.info("Fetching related products for product ID: {}", id);
        List<ProductDto> relatedProducts = productService.getRelatedProducts(id, limit);
        return ResponseEntity.ok(relatedProducts);
    }
    
    /**
     * Update stock quantity
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Void> updateStockQuantity(
            @PathVariable @Min(1) Long id,
            @RequestParam @Min(0) Integer quantity) {
        
        logger.info("Updating stock quantity for product ID: {} to: {}", id, quantity);
        productService.updateStockQuantity(id, quantity);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Adjust stock quantity
     */
    @PatchMapping("/{id}/stock/adjust")
    public ResponseEntity<Void> adjustStockQuantity(
            @PathVariable @Min(1) Long id,
            @RequestParam Integer adjustment) {
        
        logger.info("Adjusting stock quantity for product ID: {} by: {}", id, adjustment);
        productService.adjustStockQuantity(id, adjustment);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get low stock products
     */
    @GetMapping("/inventory/low-stock")
    public ResponseEntity<List<ProductDto>> getLowStockProducts() {
        logger.info("Fetching low stock products");
        List<ProductDto> products = productService.getLowStockProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * Get out of stock products
     */
    @GetMapping("/inventory/out-of-stock")
    public ResponseEntity<List<ProductDto>> getOutOfStockProducts() {
        logger.info("Fetching out of stock products");
        List<ProductDto> products = productService.getOutOfStockProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * Get distinct brands
     */
    @GetMapping("/brands")
    public ResponseEntity<List<String>> getDistinctBrands() {
        logger.info("Fetching distinct brands");
        List<String> brands = productService.getDistinctBrands();
        return ResponseEntity.ok(brands);
    }
    
    /**
     * Get price range
     */
    @GetMapping("/price-range")
    public ResponseEntity<BigDecimal[]> getPriceRange() {
        logger.info("Fetching price range");
        BigDecimal[] priceRange = productService.getPriceRange();
        return ResponseEntity.ok(priceRange);
    }
    
    /**
     * Delete product (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable @Min(1) Long id) {
        logger.info("Deleting product with ID: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Restore product
     */
    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restoreProduct(@PathVariable @Min(1) Long id) {
        logger.info("Restoring product with ID: {}", id);
        productService.restoreProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Toggle featured status
     */
    @PatchMapping("/{id}/featured")
    public ResponseEntity<ProductDto> toggleFeaturedStatus(@PathVariable @Min(1) Long id) {
        logger.info("Toggling featured status for product ID: {}", id);
        ProductDto product = productService.toggleFeaturedStatus(id);
        return ResponseEntity.ok(product);
    }
}