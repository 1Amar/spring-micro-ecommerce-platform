package com.amar.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class InventoryEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${inventory.kafka.topics.inventory-events:inventory-events}")
    private String inventoryEventsTopic;

    @Value("${inventory.kafka.topics.inventory-movement-events:inventory-movement-events}")
    private String inventoryMovementEventsTopic;

    @Value("${inventory.kafka.topics.inventory-reservation-events:inventory-reservation-events}")
    private String inventoryReservationEventsTopic;

    @Value("${inventory.kafka.topics.inventory-low-stock-alerts:inventory-low-stock-alerts}")
    private String inventoryLowStockAlertsTopic;

    @Value("${inventory.kafka.topics.inventory-alert-events:inventory-alert-events}")
    private String inventoryAlertEventsTopic;

    @Autowired
    public InventoryEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // =====================================================
    // Inventory Events
    // =====================================================

    public void publishInventoryEvent(String eventType, Long productId, Map<String, Object> eventData) {
        logger.debug("Publishing inventory event: {} for product: {}", eventType, productId);
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "productId", productId,
                "eventData", eventData,
                "timestamp", LocalDateTime.now(),
                "source", "inventory-service"
            );
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(inventoryEventsTopic, productId.toString(), event);
            
            future.thenAccept(result -> 
                logger.debug("Successfully published inventory event: {} for product: {}", eventType, productId))
                .exceptionally(ex -> {
                    logger.error("Failed to publish inventory event: {} for product: {}", eventType, productId, ex);
                    return null;
                });
                
        } catch (Exception ex) {
            logger.error("Error publishing inventory event: {} for product: {}", eventType, productId, ex);
        }
    }

    public void publishStockUpdated(Long productId, Integer oldQuantity, Integer newQuantity, 
                                   Integer availableQuantity, String stockStatus) {
        Map<String, Object> eventData = Map.of(
            "oldQuantity", oldQuantity,
            "newQuantity", newQuantity,
            "availableQuantity", availableQuantity,
            "quantityChange", newQuantity - oldQuantity,
            "stockStatus", stockStatus
        );
        
        publishInventoryEvent("inventory.stock.updated", productId, eventData);
    }

    public void publishStockAdded(Long productId, Integer quantityAdded, Integer newTotal, String reason) {
        Map<String, Object> eventData = Map.of(
            "quantityAdded", quantityAdded,
            "newTotal", newTotal,
            "reason", reason != null ? reason : "Stock addition"
        );
        
        publishInventoryEvent("inventory.stock.added", productId, eventData);
    }

    public void publishStockRemoved(Long productId, Integer quantityRemoved, Integer newTotal, String reason) {
        Map<String, Object> eventData = Map.of(
            "quantityRemoved", quantityRemoved,
            "newTotal", newTotal,
            "reason", reason != null ? reason : "Stock removal"
        );
        
        publishInventoryEvent("inventory.stock.removed", productId, eventData);
    }

    // =====================================================
    // Stock Movement Events
    // =====================================================

    public void publishMovementEvent(String eventType, Long productId, Map<String, Object> movementData) {
        logger.debug("Publishing movement event: {} for product: {}", eventType, productId);
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "productId", productId,
                "movementData", movementData,
                "timestamp", LocalDateTime.now(),
                "source", "inventory-service"
            );
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(inventoryMovementEventsTopic, productId.toString(), event);
            
            future.thenAccept(result -> 
                logger.debug("Successfully published movement event: {} for product: {}", eventType, productId))
                .exceptionally(ex -> {
                    logger.error("Failed to publish movement event: {} for product: {}", eventType, productId, ex);
                    return null;
                });
                
        } catch (Exception ex) {
            logger.error("Error publishing movement event: {} for product: {}", eventType, productId, ex);
        }
    }

    // =====================================================
    // Stock Reservation Events
    // =====================================================

    public void publishReservationEvent(String eventType, UUID orderId, Map<String, Object> reservationData) {
        logger.debug("Publishing reservation event: {} for order: {}", eventType, orderId);
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "orderId", orderId,
                "reservationData", reservationData,
                "timestamp", LocalDateTime.now(),
                "source", "inventory-service"
            );
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(inventoryReservationEventsTopic, orderId.toString(), event);
            
            future.thenAccept(result -> 
                logger.debug("Successfully published reservation event: {} for order: {}", eventType, orderId))
                .exceptionally(ex -> {
                    logger.error("Failed to publish reservation event: {} for order: {}", eventType, orderId, ex);
                    return null;
                });
                
        } catch (Exception ex) {
            logger.error("Error publishing reservation event: {} for order: {}", eventType, orderId, ex);
        }
    }

    public void publishStockReserved(UUID orderId, Long productId, Integer quantity, LocalDateTime expiresAt) {
        Map<String, Object> reservationData = Map.of(
            "productId", productId,
            "quantity", quantity,
            "expiresAt", expiresAt
        );
        
        publishReservationEvent("stock.reserved", orderId, reservationData);
    }

    public void publishReservationCommitted(UUID orderId, Long productId, Integer quantity) {
        Map<String, Object> reservationData = Map.of(
            "productId", productId,
            "quantity", quantity
        );
        
        publishReservationEvent("stock.reservation.committed", orderId, reservationData);
    }

    public void publishReservationReleased(UUID orderId, Long productId, Integer quantity) {
        Map<String, Object> reservationData = Map.of(
            "productId", productId,
            "quantity", quantity
        );
        
        publishReservationEvent("stock.reservation.released", orderId, reservationData);
    }

    public void publishReservationExpired(UUID orderId, Long productId, Integer quantity) {
        Map<String, Object> reservationData = Map.of(
            "productId", productId,
            "quantity", quantity,
            "expiredBy", "system"
        );
        
        publishReservationEvent("stock.reservation.expired", orderId, reservationData);
    }

    // =====================================================
    // Low Stock Alert Events
    // =====================================================

    public void publishLowStockAlert(Long productId, Integer currentStock, Integer threshold, 
                                   String notificationMethod, String recipients) {
        logger.info("Publishing low stock alert for product: {} (stock: {}, threshold: {})", 
                   productId, currentStock, threshold);
        
        try {
            Map<String, Object> alertData = Map.of(
                "productId", productId,
                "currentStock", currentStock,
                "threshold", threshold,
                "notificationMethod", notificationMethod != null ? notificationMethod : "EMAIL",
                "recipients", recipients != null ? recipients : "",
                "severity", currentStock == 0 ? "CRITICAL" : "WARNING",
                "timestamp", LocalDateTime.now(),
                "source", "inventory-service"
            );
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(inventoryLowStockAlertsTopic, productId.toString(), alertData);
            
            future.thenAccept(result -> 
                logger.info("Successfully published low stock alert for product: {}", productId))
                .exceptionally(ex -> {
                    logger.error("Failed to publish low stock alert for product: {}", productId, ex);
                    return null;
                });
                
        } catch (Exception ex) {
            logger.error("Error publishing low stock alert for product: {}", productId, ex);
        }
    }

    // =====================================================
    // Alert Configuration Events
    // =====================================================

    public void publishAlertEvent(String eventType, Long productId, Map<String, Object> alertData) {
        logger.debug("Publishing alert event: {} for product: {}", eventType, productId);
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "productId", productId,
                "alertData", alertData,
                "timestamp", LocalDateTime.now(),
                "source", "inventory-service"
            );
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(inventoryAlertEventsTopic, productId.toString(), event);
            
            future.thenAccept(result -> 
                logger.debug("Successfully published alert event: {} for product: {}", eventType, productId))
                .exceptionally(ex -> {
                    logger.error("Failed to publish alert event: {} for product: {}", eventType, productId, ex);
                    return null;
                });
                
        } catch (Exception ex) {
            logger.error("Error publishing alert event: {} for product: {}", eventType, productId, ex);
        }
    }

    public void publishAlertCreated(Long productId, Integer threshold, Integer frequencyHours, Boolean enabled) {
        Map<String, Object> alertData = Map.of(
            "threshold", threshold,
            "frequencyHours", frequencyHours,
            "enabled", enabled
        );
        
        publishAlertEvent("alert.created", productId, alertData);
    }

    public void publishAlertUpdated(Long productId, Integer threshold, Integer frequencyHours, Boolean enabled) {
        Map<String, Object> alertData = Map.of(
            "threshold", threshold,
            "frequencyHours", frequencyHours,
            "enabled", enabled
        );
        
        publishAlertEvent("alert.updated", productId, alertData);
    }

    public void publishAlertDeleted(Long productId) {
        Map<String, Object> alertData = Map.of(
            "deleted", true
        );
        
        publishAlertEvent("alert.deleted", productId, alertData);
    }

    // =====================================================
    // Batch Events
    // =====================================================

    public void publishBatchStockUpdate(Map<Long, Integer> productQuantities, String reason) {
        logger.info("Publishing batch stock update for {} products", productQuantities.size());
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", "inventory.batch.stock.updated",
                "productQuantities", productQuantities,
                "reason", reason != null ? reason : "Batch stock update",
                "count", productQuantities.size(),
                "timestamp", LocalDateTime.now(),
                "source", "inventory-service"
            );
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(inventoryEventsTopic, "batch-update", event);
            
            future.thenAccept(result -> 
                logger.info("Successfully published batch stock update for {} products", productQuantities.size()))
                .exceptionally(ex -> {
                    logger.error("Failed to publish batch stock update", ex);
                    return null;
                });
                
        } catch (Exception ex) {
            logger.error("Error publishing batch stock update", ex);
        }
    }

    // =====================================================
    // Health Check Event
    // =====================================================

    public void publishHealthCheckEvent() {
        logger.debug("Publishing health check event");
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", "inventory.service.health",
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "source", "inventory-service"
            );
            
            kafkaTemplate.send(inventoryEventsTopic, "health-check", event);
            
        } catch (Exception ex) {
            logger.error("Error publishing health check event", ex);
        }
    }
}