package com.amar.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    @GetMapping("/check/{productId}")
    public Map<String, Object> checkInventory(@PathVariable("productId") String productId,
                                            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        logger.info("Processing inventory check for product: {} - Correlation-ID: {}", productId, correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "inventory-service");
        response.put("productId", productId);
        response.put("available", true);
        response.put("quantity", 150);
        response.put("correlationId", correlationId);
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("Inventory check completed for product: {} - Available: {} - Correlation-ID: {}", 
                   productId, true, correlationId);
        
        return response;
    }

    @PostMapping("/reserve/{productId}")
    public Map<String, Object> reserveInventory(@PathVariable("productId") String productId,
                                              @RequestParam("quantity") int quantity,
                                              @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        logger.info("Processing inventory reservation for product: {} quantity: {} - Correlation-ID: {}", 
                   productId, quantity, correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "inventory-service");
        response.put("productId", productId);
        response.put("reserved", true);
        response.put("quantity", quantity);
        response.put("correlationId", correlationId);
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("Inventory reserved for product: {} quantity: {} - Correlation-ID: {}", 
                   productId, quantity, correlationId);
        
        return response;
    }

    @PostMapping("/simulate")
    public String simulateInventory() {
        String correlationId = MDC.get("correlationId");
        logger.info("Inventory service: Simulation endpoint called - correlationId: {}", correlationId);
        return "Inventory service simulation completed - correlationId: " + correlationId;
    }
}