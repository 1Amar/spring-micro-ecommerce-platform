package com.amar.cart.service;

import com.amar.dto.ProductDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class ProductValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(ProductValidationService.class);
    
    @Autowired
    private WebClient webClient;
    
    // Cache for product validation (1 minute TTL)
    private final Cache<Long, ProductDto> productCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build();
    
    public ProductDto validateProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        
        // Check cache first
        ProductDto cachedProduct = productCache.getIfPresent(productId);
        if (cachedProduct != null) {
            log.debug("Found product in cache: {}", productId);
            return cachedProduct;
        }
        
        // Call Product Service
        try {
            log.info("Calling product service for product: {} at URL: {}", productId, "/api/v1/products/catalog/" + productId);
            
            ProductDto product = webClient
                .get()
                .uri("http://localhost:8088/api/v1/products/catalog/{id}", productId)
                .retrieve()
                .bodyToMono(ProductDto.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            if (product == null) {
                throw new ProductNotFoundException("Product not found: " + productId);
            }
            
            // Cache the result
            productCache.put(productId, product);
            log.debug("Product validation successful: {} - {}", productId, product.getName());
            return product;
            
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Product not found: {}", productId);
            throw new ProductNotFoundException("Product not found: " + productId);
        } catch (Exception e) {
            log.error("Failed to validate product: {} - Error type: {} - Message: {} - Full stack trace:", productId, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new ProductValidationException("Unable to validate product: " + productId + " - " + e.getMessage());
        }
    }
    
    public void clearCache() {
        productCache.invalidateAll();
        log.info("Product cache cleared");
    }
    
    public long getCacheSize() {
        return productCache.estimatedSize();
    }
}