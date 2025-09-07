package com.amar.controller;

import com.amar.dto.InventoryDto;
import com.amar.dto.request.StockReservationRequest;
import com.amar.dto.response.InventoryAvailabilityResponse;
import com.amar.dto.response.StockReservationResponse;
import com.amar.service.InventoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // =====================================================
    // Legacy endpoints (for compatibility)
    // =====================================================

    @GetMapping("/check/{productId}")
    public Map<String, Object> checkInventory(@PathVariable("productId") String productId,
                                            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        logger.info("Processing legacy inventory check for product: {} - Correlation-ID: {}", productId, correlationId);
        
        try {
            Long productIdLong = Long.parseLong(productId);
            Optional<InventoryDto> inventory = inventoryService.getInventoryByProductId(productIdLong);
            
            Map<String, Object> response = new HashMap<>();
            response.put("service", "inventory-service");
            response.put("productId", productId);
            response.put("correlationId", correlationId);
            response.put("timestamp", System.currentTimeMillis());
            
            if (inventory.isPresent()) {
                InventoryDto inv = inventory.get();
                response.put("available", inv.getAvailableQuantity() > 0);
                response.put("quantity", inv.getAvailableQuantity());
                response.put("totalQuantity", inv.getQuantity());
                response.put("reservedQuantity", inv.getReservedQuantity());
                response.put("stockStatus", inv.getStockStatus());
            } else {
                response.put("available", false);
                response.put("quantity", 0);
                response.put("error", "Product not found in inventory");
            }
            
            logger.info("Legacy inventory check completed for product: {} - Available: {} - Correlation-ID: {}", 
                       productId, response.get("available"), correlationId);
            
            return response;
        } catch (NumberFormatException ex) {
            logger.warn("Invalid product ID format: {}", productId);
            Map<String, Object> response = new HashMap<>();
            response.put("service", "inventory-service");
            response.put("productId", productId);
            response.put("available", false);
            response.put("quantity", 0);
            response.put("error", "Invalid product ID format");
            response.put("correlationId", correlationId);
            response.put("timestamp", System.currentTimeMillis());
            return response;
        }
    }

    @PostMapping("/reserve/{productId}")
    public Map<String, Object> reserveInventory(@PathVariable("productId") String productId,
                                              @RequestParam("quantity") int quantity,
                                              @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        logger.info("Processing legacy inventory reservation for product: {} quantity: {} - Correlation-ID: {}", 
                   productId, quantity, correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "inventory-service");
        response.put("productId", productId);
        response.put("correlationId", correlationId);
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            Long productIdLong = Long.parseLong(productId);
            InventoryAvailabilityResponse availability = inventoryService.checkAvailability(productIdLong, quantity);
            
            response.put("reserved", availability.getAvailable());
            response.put("quantity", quantity);
            response.put("availableQuantity", availability.getAvailableQuantity());
            response.put("message", availability.getMessage());
            
            if (!availability.getAvailable()) {
                response.put("suggestedQuantity", availability.getSuggestedQuantity());
            }
            
        } catch (Exception ex) {
            logger.error("Legacy reservation failed for product: {}", productId, ex);
            response.put("reserved", false);
            response.put("quantity", 0);
            response.put("error", "Reservation failed: " + ex.getMessage());
        }
        
        logger.info("Legacy inventory reservation completed for product: {} quantity: {} - Reserved: {} - Correlation-ID: {}", 
                   productId, quantity, response.get("reserved"), correlationId);
        
        return response;
    }

    @PostMapping("/simulate")
    public String simulateInventory() {
        String correlationId = MDC.get("correlationId");
        logger.info("Inventory service: Simulation endpoint called - correlationId: {}", correlationId);
        return "Inventory service simulation completed - correlationId: " + correlationId;
    }

    // =====================================================
    // Core API endpoints
    // =====================================================

    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getInventoryByProductId(@PathVariable Long productId) {
        logger.info("Getting inventory for product ID: {}", productId);

        try {
            Optional<InventoryDto> inventory = inventoryService.getInventoryByProductId(productId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("service", "inventory-service");
            response.put("timestamp", System.currentTimeMillis());
            
            if (inventory.isPresent()) {
                response.put("success", true);
                response.put("data", inventory.get());
                response.put("message", "Inventory retrieved successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Inventory not found for product ID: " + productId);
                response.put("errorCode", "INVENTORY_NOT_FOUND");
                return ResponseEntity.status(404).body(response);
            }
        } catch (Exception ex) {
            logger.error("Failed to get inventory for product ID: {}", productId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "inventory-service");
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error: " + ex.getMessage());
            errorResponse.put("errorCode", "INTERNAL_ERROR");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> getInventoryForProducts(@RequestBody List<Long> productIds) {
        logger.info("Getting inventory for {} products", productIds.size());

        try {
            List<InventoryDto> inventories = inventoryService.getInventoryForProducts(productIds);
            Map<String, Object> response = new HashMap<>();
            response.put("service", "inventory-service");
            response.put("success", true);
            response.put("data", inventories);
            response.put("message", "Inventory retrieved for " + inventories.size() + " products");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            logger.error("Failed to get inventory for products", ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "inventory-service");
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve inventory: " + ex.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/availability/{productId}")
    public ResponseEntity<Map<String, Object>> checkAvailability(@PathVariable Long productId, @RequestParam Integer quantity) {
        logger.info("Checking availability for product ID: {} quantity: {}", productId, quantity);

        try {
            InventoryAvailabilityResponse availability = inventoryService.checkAvailability(productId, quantity);
            Map<String, Object> response = new HashMap<>();
            response.put("service", "inventory-service");
            response.put("success", true);
            response.put("data", availability);
            response.put("message", "Availability checked successfully");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            logger.error("Failed to check availability for product ID: {}", productId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "inventory-service");
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to check availability: " + ex.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserveStock(@Valid @RequestBody StockReservationRequest request) {
        logger.info("Reserving stock for order ID: {}", request.getOrderId());

        try {
            StockReservationResponse reservationResponse = inventoryService.reserveStock(request);
            Map<String, Object> response = new HashMap<>();
            response.put("service", "inventory-service");
            response.put("success", true);
            response.put("data", reservationResponse);
            response.put("message", "Stock reserved successfully");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            logger.error("Failed to reserve stock for order: {}", request.getOrderId(), ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "inventory-service");
            errorResponse.put("success", false);
            errorResponse.put("error", ex.getMessage());
            errorResponse.put("errorCode", "RESERVATION_FAILED");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception ex) {
            logger.error("Unexpected error during stock reservation for order: {}", request.getOrderId(), ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "inventory-service");
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error: " + ex.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/reserve/{orderId}/commit")
    public ResponseEntity<Map<String, Object>> commitReservation(@PathVariable UUID orderId) {
        logger.info("Committing reservation for order ID: {}", orderId);

        try {
            inventoryService.commitReservation(orderId);
            Map<String, Object> response = new HashMap<>();
            response.put("service", "inventory-service");
            response.put("success", true);
            response.put("message", "Stock reservation committed successfully");
            response.put("orderId", orderId);
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "inventory-service");
            errorResponse.put("success", false);
            errorResponse.put("error", ex.getMessage());
            errorResponse.put("errorCode", "COMMIT_FAILED");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(400).body(errorResponse);
        }
    }

    @PostMapping("/reserve/{orderId}/release")
    public ResponseEntity<Map<String, Object>> releaseReservation(@PathVariable UUID orderId) {
        logger.info("Releasing reservation for order ID: {}", orderId);

        try {
            inventoryService.releaseReservation(orderId);
            Map<String, Object> response = new HashMap<>();
            response.put("service", "inventory-service");
            response.put("success", true);
            response.put("message", "Stock reservation released successfully");
            response.put("orderId", orderId);
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "inventory-service");
            errorResponse.put("success", false);
            errorResponse.put("error", ex.getMessage());
            errorResponse.put("errorCode", "RELEASE_FAILED");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(400).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "inventory-service");
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}