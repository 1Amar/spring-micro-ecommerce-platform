package com.amar.controller;

import com.amar.dto.StockMovementDto;
import com.amar.service.StockMovementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/movements")
@CrossOrigin(origins = "*")
public class StockMovementController {

    private static final Logger logger = LoggerFactory.getLogger(StockMovementController.class);

    private final StockMovementService stockMovementService;

    @Autowired
    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    // Get movements for a specific product
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getMovementsForProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Getting movements for product ID: {}", productId);
        
        try {
            List<StockMovementDto> movements = stockMovementService.getMovementsForProductPaged(
                productId, PageRequest.of(page, size));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", movements);
            response.put("productId", productId);
            response.put("totalMovements", movements.size());
            response.put("page", page);
            response.put("size", size);

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error getting movements for product {}", productId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get movements: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get movements by date range
    @GetMapping("/date-range")
    public ResponseEntity<Map<String, Object>> getMovementsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        logger.info("Getting movements between {} and {}", startDate, endDate);
        
        try {
            List<StockMovementDto> movements = stockMovementService.getMovementsByDateRangePaged(
                startDate, endDate, PageRequest.of(page, size));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", movements);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("totalMovements", movements.size());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error getting movements by date range", ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get movements: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get movements by reference ID (order ID, etc.)
    @GetMapping("/reference/{referenceId}")
    public ResponseEntity<Map<String, Object>> getMovementsByReference(@PathVariable UUID referenceId) {
        logger.info("Getting movements for reference ID: {}", referenceId);
        
        try {
            List<StockMovementDto> movements = stockMovementService.getMovementsByReferenceId(referenceId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", movements);
            response.put("referenceId", referenceId);
            response.put("totalMovements", movements.size());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error getting movements for reference {}", referenceId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get movements: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Record manual adjustment
    @PostMapping("/adjustment")
    public ResponseEntity<Map<String, Object>> recordAdjustment(@RequestBody Map<String, Object> adjustmentData) {
        logger.info("Recording manual stock adjustment");
        
        try {
            Long productId = Long.valueOf(adjustmentData.get("productId").toString());
            Integer quantityChange = Integer.valueOf(adjustmentData.get("quantityChange").toString());
            String reason = (String) adjustmentData.get("reason");
            String performedBy = (String) adjustmentData.get("performedBy");
            String notes = (String) adjustmentData.getOrDefault("notes", "");

            stockMovementService.recordAdjustment(productId, quantityChange, reason, performedBy, notes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock adjustment recorded successfully");
            response.put("productId", productId);
            response.put("quantityChange", quantityChange);
            response.put("reason", reason);

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error recording adjustment", ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to record adjustment: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get movement statistics
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getMovementStatistics(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.info("Getting movement statistics for product: {}, date range: {} to {}", productId, startDate, endDate);
        
        try {
            Map<String, Object> statistics = stockMovementService.getMovementStatistics(productId, startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            response.put("generatedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error getting movement statistics", ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get statistics: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "StockMovementController");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}