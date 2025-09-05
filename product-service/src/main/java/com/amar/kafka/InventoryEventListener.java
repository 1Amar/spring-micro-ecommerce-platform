package com.amar.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InventoryEventListener {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventListener.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public InventoryEventListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // =====================================================
    // Inventory Stock Change Events
    // =====================================================

    @KafkaListener(topics = "inventory-events", groupId = "${spring.kafka.consumer.group-id:product-service}")
    public void handleInventoryEvents(@Payload Map<String, Object> event,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                     Acknowledgment acknowledgment) {
        
        logger.info("Received inventory event from topic: {} partition: {} key: {}", topic, partition, key);
        
        try {
            String eventType = (String) event.get("eventType");
            
            switch (eventType) {
                case "inventory.stock.updated":
                    handleStockUpdated(event);
                    break;
                case "inventory.stock.added":
                    handleStockAdded(event);
                    break;
                case "inventory.stock.removed":
                    handleStockRemoved(event);
                    break;
                case "inventory.adjusted":
                    handleStockAdjusted(event);
                    break;
                case "inventory.updated":
                    handleInventoryUpdated(event);
                    break;
                default:
                    logger.debug("Unhandled inventory event type: {}", eventType);
            }
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed inventory event: {}", eventType);
            
        } catch (Exception ex) {
            logger.error("Failed to process inventory event from topic: {}", topic, ex);
            // Don't acknowledge on failure - message will be retried
        }
    }

    private void handleStockUpdated(Map<String, Object> event) {
        logger.info("Processing inventory stock updated event");
        
        try {
            Long productId = extractProductId(event);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) event.get("eventData");
            
            if (eventData != null) {
                Integer oldQuantity = (Integer) eventData.get("oldQuantity");
                Integer newQuantity = (Integer) eventData.get("newQuantity");
                Integer availableQuantity = (Integer) eventData.get("availableQuantity");
                String stockStatus = (String) eventData.get("stockStatus");
                
                logger.info("Product {} stock updated: {} -> {}, available: {}, status: {}", 
                           productId, oldQuantity, newQuantity, availableQuantity, stockStatus);
                
                // TODO: Update product search index if needed
                // TODO: Invalidate product cache if using caching
                // TODO: Update product display status based on stock levels
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process stock updated event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleStockAdded(Map<String, Object> event) {
        logger.info("Processing inventory stock added event");
        
        try {
            Long productId = extractProductId(event);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) event.get("eventData");
            
            if (eventData != null) {
                Integer quantityAdded = (Integer) eventData.get("quantityAdded");
                Integer newTotal = (Integer) eventData.get("newTotal");
                String reason = (String) eventData.get("reason");
                
                logger.info("Product {} stock added: {} units, new total: {}, reason: {}", 
                           productId, quantityAdded, newTotal, reason);
                
                // TODO: Update product availability status
                // TODO: Trigger back-in-stock notifications if product was out of stock
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process stock added event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleStockRemoved(Map<String, Object> event) {
        logger.info("Processing inventory stock removed event");
        
        try {
            Long productId = extractProductId(event);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) event.get("eventData");
            
            if (eventData != null) {
                Integer quantityRemoved = (Integer) eventData.get("quantityRemoved");
                Integer newTotal = (Integer) eventData.get("newTotal");
                String reason = (String) eventData.get("reason");
                
                logger.info("Product {} stock removed: {} units, new total: {}, reason: {}", 
                           productId, quantityRemoved, newTotal, reason);
                
                // TODO: Update product availability status
                // TODO: If stock reaches 0, mark product as out of stock
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process stock removed event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleStockAdjusted(Map<String, Object> event) {
        logger.info("Processing inventory stock adjusted event");
        
        try {
            Long productId = extractProductId(event);
            
            logger.info("Product {} inventory adjusted", productId);
            
            // TODO: Refresh product data from inventory service
            // TODO: Update search indexes
            
        } catch (Exception ex) {
            logger.error("Failed to process stock adjusted event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleInventoryUpdated(Map<String, Object> event) {
        logger.info("Processing inventory updated event");
        
        try {
            Long productId = extractProductId(event);
            
            logger.info("Product {} inventory updated", productId);
            
            // TODO: Refresh product data from inventory service
            // TODO: Update caches and search indexes
            
        } catch (Exception ex) {
            logger.error("Failed to process inventory updated event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private Long extractProductId(Map<String, Object> event) {
        Object productIdObj = event.get("productId");
        if (productIdObj instanceof Number) {
            return ((Number) productIdObj).longValue();
        } else if (productIdObj instanceof String) {
            return Long.parseLong((String) productIdObj);
        } else {
            throw new IllegalArgumentException("Invalid productId format: " + productIdObj);
        }
    }
}