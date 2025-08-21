package com.amar.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    
    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/{productId}")
    public Map<String, Object> getProduct(@PathVariable String productId,
                                        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        logger.info("Processing product request for: {} - Correlation-ID: {}", productId, correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "product-service");
        response.put("productId", productId);
        response.put("name", "Sample Product " + productId);
        response.put("price", 99.99);
        response.put("correlationId", correlationId);
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("Product details retrieved for: {} - Correlation-ID: {}", productId, correlationId);
        
        return response;
    }

    @GetMapping("/{productId}/with-inventory")
    public Map<String, Object> getProductWithInventory(@PathVariable String productId,
                                                     @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        logger.info("Processing product with inventory request for: {} - Correlation-ID: {}", productId, correlationId);
        
        // Get product details
        Map<String, Object> product = getProduct(productId, correlationId);
        
        try {
            // Call inventory service
            logger.info("Calling inventory service for product: {} - Correlation-ID: {}", productId, correlationId);
            
            String inventoryUrl = "http://inventory-service/api/v1/inventory/check/" + productId;
            Map<String, Object> inventory = restTemplate.getForObject(inventoryUrl, Map.class);
            
            product.put("inventory", inventory);
            logger.info("Inventory data retrieved for product: {} - Correlation-ID: {}", productId, correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to get inventory for product: {} - Correlation-ID: {} - Error: {}", 
                        productId, correlationId, e.getMessage());
            product.put("inventory", Map.of("error", "Inventory service unavailable"));
        }
        
        return product;
    }
}