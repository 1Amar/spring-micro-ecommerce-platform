package com.amar.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amar.entity.inventory.Inventory;
import com.amar.entity.inventory.LowStockAlert;
import com.amar.repository.InventoryRepository;
import com.amar.repository.LowStockAlertRepository;

@Service
public class LowStockAlertService {

    private static final Logger logger = LoggerFactory.getLogger(LowStockAlertService.class);

    private final LowStockAlertRepository lowStockAlertRepository;
    private final InventoryRepository inventoryRepository;

    @Value("${inventory.alerts.enabled:true}")
    private Boolean alertsEnabled;

    @Value("${inventory.alerts.default-threshold:10}")
    private Integer defaultThreshold;

    @Autowired
    public LowStockAlertService(LowStockAlertRepository lowStockAlertRepository,
                               InventoryRepository inventoryRepository) {
        this.lowStockAlertRepository = lowStockAlertRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Scheduled(fixedRateString = "${inventory.jobs.low-stock-check-interval-ms:1800000}") // 30 minutes
    @Transactional
    public void checkLowStockItems() {
        if (!alertsEnabled) {
            logger.debug("Low stock alerts are disabled, skipping check");
            return;
        }

        logger.info("Starting scheduled low stock check");

        try {
            List<Inventory> lowStockItems = inventoryRepository.findLowStockItems(defaultThreshold);
            
            logger.info("Found {} items with low stock", lowStockItems.size());

            for (Inventory inventory : lowStockItems) {
                processLowStockItem(inventory);
            }

            logger.info("Completed scheduled low stock check");

        } catch (Exception ex) {
            logger.error("Error during scheduled low stock check", ex);
        }
    }

    @Transactional
    public void processLowStockItem(Inventory inventory) {
        try {
            // Check if alert already exists and is recent
            Optional<LowStockAlert> existingAlert = lowStockAlertRepository
                .findFirstByProductIdAndStatusOrderByCreatedAtDesc(
                    inventory.getProductId(), 
                    LowStockAlert.AlertStatus.PENDING
                );

            if (existingAlert.isPresent()) {
                LowStockAlert alert = existingAlert.get();
                // If alert was created within last 24 hours, skip
                if (alert.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24))) {
                    logger.debug("Skipping low stock alert for product {} - recent alert exists", 
                               inventory.getProductId());
                    return;
                }
            }

            // Create new alert
            LowStockAlert alert = new LowStockAlert();
            alert.setProductId(inventory.getProductId());
            alert.setCurrentStock(inventory.getAvailableQuantity());
            alert.setThreshold(defaultThreshold);
            alert.setMessage("Low stock alert: Product " + inventory.getProductId() + 
                           " has only " + inventory.getAvailableQuantity() + " items remaining");
            alert.setStatus(LowStockAlert.AlertStatus.PENDING);
            alert.setCreatedAt(LocalDateTime.now());

            lowStockAlertRepository.save(alert);
            
            logger.info("Created low stock alert for product {} - current stock: {}", 
                       inventory.getProductId(), inventory.getAvailableQuantity());

        } catch (Exception ex) {
            logger.error("Error processing low stock item for product {}", inventory.getProductId(), ex);
        }
    }

    @Transactional
    public void acknowledgeAlert(Long alertId) {
        Optional<LowStockAlert> alertOpt = lowStockAlertRepository.findById(alertId);
        if (alertOpt.isPresent()) {
            LowStockAlert alert = alertOpt.get();
            alert.setStatus(LowStockAlert.AlertStatus.ACKNOWLEDGED);
            alert.setAcknowledgedAt(LocalDateTime.now());
            lowStockAlertRepository.save(alert);
            
            logger.info("Low stock alert {} acknowledged", alertId);
        }
    }

    public List<LowStockAlert> getActiveAlerts() {
        return lowStockAlertRepository.findByStatusOrderByCreatedAtDesc(LowStockAlert.AlertStatus.PENDING);
    }
    
    // Additional methods needed by controllers
    public List<LowStockAlert> getAlertsByStatus(LowStockAlert.AlertStatus status) {
        return lowStockAlertRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    public List<LowStockAlert> getAlertsForProduct(Long productId) {
        return lowStockAlertRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
    
    public boolean resolveAlert(Long alertId) {
        Optional<LowStockAlert> alertOpt = lowStockAlertRepository.findById(alertId);
        if (alertOpt.isPresent()) {
            LowStockAlert alert = alertOpt.get();
            alert.setStatus(LowStockAlert.AlertStatus.RESOLVED);
            alert.setAcknowledgedAt(LocalDateTime.now());
            lowStockAlertRepository.save(alert);
            
            logger.info("Low stock alert {} resolved", alertId);
            return true;
        }
        return false;
    }
    
    public void checkProductForLowStock(Long productId) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            if (inventory.getAvailableQuantity() <= defaultThreshold) {
                processLowStockItem(inventory);
            }
        }
    }
    
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        long totalAlerts = lowStockAlertRepository.count();
        long pendingAlerts = lowStockAlertRepository.countByStatus(LowStockAlert.AlertStatus.PENDING);
        long acknowledgedAlerts = lowStockAlertRepository.countByStatus(LowStockAlert.AlertStatus.ACKNOWLEDGED);
        long resolvedAlerts = lowStockAlertRepository.countByStatus(LowStockAlert.AlertStatus.RESOLVED);
        
        statistics.put("totalAlerts", totalAlerts);
        statistics.put("pendingAlerts", pendingAlerts);
        statistics.put("acknowledgedAlerts", acknowledgedAlerts);
        statistics.put("resolvedAlerts", resolvedAlerts);
        statistics.put("generatedAt", LocalDateTime.now());
        
        return statistics;
    }
}