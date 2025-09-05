package com.amar.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amar.entity.inventory.LowStockAlert;
import com.amar.service.LowStockAlertService;

@RestController
@RequestMapping("/api/v1/inventory/alerts")
@CrossOrigin(origins = "*")
public class LowStockAlertController {

    private static final Logger logger = LoggerFactory.getLogger(LowStockAlertController.class);

    private final LowStockAlertService lowStockAlertService;

    @Autowired
    public LowStockAlertController(LowStockAlertService lowStockAlertService) {
        this.lowStockAlertService = lowStockAlertService;
    }

    // Get all alerts (default to active)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAlerts(
            @RequestParam(defaultValue = "PENDING") String status) {
        logger.info("Getting all alerts with status: {}", status);
        
        try {
            List<LowStockAlert> alerts;
            if ("ALL".equalsIgnoreCase(status)) {
                // Get all alerts regardless of status
                alerts = lowStockAlertService.getAlertsByStatus(LowStockAlert.AlertStatus.PENDING);
                alerts.addAll(lowStockAlertService.getAlertsByStatus(LowStockAlert.AlertStatus.ACKNOWLEDGED));
                alerts.addAll(lowStockAlertService.getAlertsByStatus(LowStockAlert.AlertStatus.RESOLVED));
            } else {
                alerts = lowStockAlertService.getAlertsByStatus(LowStockAlert.AlertStatus.valueOf(status.toUpperCase()));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            response.put("totalAlerts", alerts.size());
            response.put("status", status);
            response.put("retrievedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Invalid status: " + status + ". Valid values: PENDING, ACKNOWLEDGED, RESOLVED, ALL");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception ex) {
            logger.error("Failed to get alerts", ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve alerts: " + ex.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get all active alerts
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveAlerts() {
        logger.info("Getting all active low stock alerts");
        
        try {
            List<LowStockAlert> alerts = lowStockAlertService.getActiveAlerts();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            response.put("totalAlerts", alerts.size());
            response.put("retrievedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error getting active alerts", ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get active alerts: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get alerts by status
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getAlertsByStatus(@PathVariable String status) {
        logger.info("Getting alerts with status: {}", status);
        
        try {
            LowStockAlert.AlertStatus alertStatus = LowStockAlert.AlertStatus.valueOf(status.toUpperCase());
            List<LowStockAlert> alerts = lowStockAlertService.getAlertsByStatus(alertStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            response.put("status", status);
            response.put("totalAlerts", alerts.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            logger.error("Invalid alert status: {}", status);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Invalid status. Valid values: PENDING, ACKNOWLEDGED, RESOLVED");
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception ex) {
            logger.error("Error getting alerts by status", ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get alerts: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get alerts for specific product
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getAlertsForProduct(@PathVariable Long productId) {
        logger.info("Getting alerts for product ID: {}", productId);
        
        try {
            List<LowStockAlert> alerts = lowStockAlertService.getAlertsForProduct(productId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            response.put("productId", productId);
            response.put("totalAlerts", alerts.size());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error getting alerts for product {}", productId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get alerts: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Acknowledge an alert
    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeAlert(@PathVariable Long alertId) {
        logger.info("Acknowledging alert ID: {}", alertId);
        
        try {
            lowStockAlertService.acknowledgeAlert(alertId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alert acknowledged successfully");
            response.put("alertId", alertId);
            response.put("acknowledgedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error acknowledging alert {}", alertId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to acknowledge alert: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Resolve an alert
    @PostMapping("/{alertId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveAlert(@PathVariable Long alertId) {
        logger.info("Resolving alert ID: {}", alertId);
        
        try {
            boolean resolved = lowStockAlertService.resolveAlert(alertId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", resolved);
            response.put("message", resolved ? "Alert resolved successfully" : "Alert not found or already resolved");
            response.put("alertId", alertId);
            response.put("resolvedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error resolving alert {}", alertId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to resolve alert: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Manually trigger alert check for specific product
    @PostMapping("/check/{productId}")
    public ResponseEntity<Map<String, Object>> triggerAlertCheck(@PathVariable Long productId) {
        logger.info("Manually triggering alert check for product ID: {}", productId);
        
        try {
            lowStockAlertService.checkProductForLowStock(productId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alert check triggered successfully");
            response.put("productId", productId);
            response.put("checkedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error triggering alert check for product {}", productId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to trigger alert check: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Run global low stock check (admin function)
    @PostMapping("/check-all")
    public ResponseEntity<Map<String, Object>> triggerGlobalAlertCheck() {
        logger.info("Manually triggering global low stock check");
        
        try {
            lowStockAlertService.checkLowStockItems();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Global low stock check completed");
            response.put("checkedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error triggering global alert check", ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to trigger global check: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get alert statistics/dashboard data
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAlertStatistics() {
        logger.info("Getting alert statistics");
        
        try {
            Map<String, Object> statistics = lowStockAlertService.getAlertStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            response.put("generatedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error getting alert statistics", ex);
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
        response.put("service", "LowStockAlertController");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}