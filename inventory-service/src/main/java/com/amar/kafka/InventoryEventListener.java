package com.amar.kafka;

import com.amar.service.InventoryService;
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
import java.util.UUID;

@Component
public class InventoryEventListener {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventListener.class);

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @Autowired
    public InventoryEventListener(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    // =====================================================
    // Order Events (for stock management)
    // =====================================================

    @KafkaListener(topics = "order-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderEvents(@Payload Map<String, Object> event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                 Acknowledgment acknowledgment) {
        
        logger.info("Received order event from topic: {} partition: {} key: {}", topic, partition, key);
        
        try {
            String eventType = (String) event.get("eventType");
            
            switch (eventType) {
                case "order.created":
                    handleOrderCreated(event);
                    break;
                case "order.cancelled":
                    handleOrderCancelled(event);
                    break;
                case "order.confirmed":
                    handleOrderConfirmed(event);
                    break;
                case "order.payment.completed":
                    handlePaymentCompleted(event);
                    break;
                case "order.payment.failed":
                    handlePaymentFailed(event);
                    break;
                default:
                    logger.warn("Unknown order event type: {}", eventType);
            }
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed order event: {}", eventType);
            
        } catch (Exception ex) {
            logger.error("Failed to process order event from topic: {}", topic, ex);
            // Don't acknowledge on failure - message will be retried
        }
    }

    private void handleOrderCreated(Map<String, Object> event) {
        logger.info("Processing order created event");
        
        try {
            String orderIdStr = (String) event.get("orderId");
            UUID orderId = UUID.fromString(orderIdStr);
            
            // Extract order items for reservation
            @SuppressWarnings("unchecked")
            Map<String, Object> orderData = (Map<String, Object>) event.get("orderData");
            
            if (orderData != null && orderData.containsKey("items")) {
                logger.info("Order {} created - inventory will handle reservation via direct API calls", orderId);
                // Note: Stock reservation is handled via direct API calls during checkout
                // This event is for logging/audit purposes
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process order created event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleOrderCancelled(Map<String, Object> event) {
        logger.info("Processing order cancelled event");
        
        try {
            String orderIdStr = (String) event.get("orderId");
            UUID orderId = UUID.fromString(orderIdStr);
            
            // Release any reserved stock for this order
            inventoryService.releaseReservation(orderId);
            logger.info("Released stock reservation for cancelled order: {}", orderId);
            
        } catch (Exception ex) {
            logger.error("Failed to process order cancelled event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleOrderConfirmed(Map<String, Object> event) {
        logger.info("Processing order confirmed event");
        
        try {
            String orderIdStr = (String) event.get("orderId");
            UUID orderId = UUID.fromString(orderIdStr);
            
            logger.info("Order {} confirmed - stock reservation maintained until payment", orderId);
            // Keep reservation active until payment is completed
            
        } catch (Exception ex) {
            logger.error("Failed to process order confirmed event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handlePaymentCompleted(Map<String, Object> event) {
        logger.info("Processing payment completed event");
        
        try {
            String orderIdStr = (String) event.get("orderId");
            UUID orderId = UUID.fromString(orderIdStr);
            
            // Commit the stock reservation (finalize the sale)
            inventoryService.commitReservation(orderId);
            logger.info("Committed stock reservation for paid order: {}", orderId);
            
        } catch (Exception ex) {
            logger.error("Failed to process payment completed event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handlePaymentFailed(Map<String, Object> event) {
        logger.info("Processing payment failed event");
        
        try {
            String orderIdStr = (String) event.get("orderId");
            UUID orderId = UUID.fromString(orderIdStr);
            
            // Release stock reservation due to payment failure
            inventoryService.releaseReservation(orderId);
            logger.info("Released stock reservation for failed payment order: {}", orderId);
            
        } catch (Exception ex) {
            logger.error("Failed to process payment failed event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    // =====================================================
    // Product Events (for inventory management)
    // =====================================================

    @KafkaListener(topics = "product-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleProductEvents(@Payload Map<String, Object> event,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                   Acknowledgment acknowledgment) {
        
        logger.info("Received product event from topic: {} partition: {} key: {}", topic, partition, key);
        
        try {
            String eventType = (String) event.get("eventType");
            
            switch (eventType) {
                case "product.created":
                    handleProductCreated(event);
                    break;
                case "product.updated":
                    handleProductUpdated(event);
                    break;
                case "product.deleted":
                    handleProductDeleted(event);
                    break;
                case "product.activated":
                    handleProductActivated(event);
                    break;
                case "product.deactivated":
                    handleProductDeactivated(event);
                    break;
                default:
                    logger.warn("Unknown product event type: {}", eventType);
            }
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed product event: {}", eventType);
            
        } catch (Exception ex) {
            logger.error("Failed to process product event from topic: {}", topic, ex);
            // Don't acknowledge on failure - message will be retried
        }
    }

    private void handleProductCreated(Map<String, Object> event) {
        logger.info("Processing product created event");
        
        try {
            Long productId = extractProductId(event);
            
            // Initialize inventory for new product with default values
            inventoryService.createOrUpdateInventory(productId, 0, 10, 1000);
            logger.info("Initialized inventory for new product: {}", productId);
            
        } catch (Exception ex) {
            logger.error("Failed to process product created event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleProductUpdated(Map<String, Object> event) {
        logger.info("Processing product updated event");
        
        try {
            Long productId = extractProductId(event);
            
            // Product update doesn't require inventory changes
            logger.debug("Product {} updated - no inventory action required", productId);
            
        } catch (Exception ex) {
            logger.error("Failed to process product updated event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleProductDeleted(Map<String, Object> event) {
        logger.info("Processing product deleted event");
        
        try {
            Long productId = extractProductId(event);
            
            // Note: We might want to keep inventory records for audit purposes
            // Instead of deleting, we could mark them as inactive
            logger.info("Product {} deleted - inventory records maintained for audit", productId);
            
        } catch (Exception ex) {
            logger.error("Failed to process product deleted event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleProductActivated(Map<String, Object> event) {
        logger.info("Processing product activated event");
        
        try {
            Long productId = extractProductId(event);
            
            // Product activation doesn't require specific inventory action
            logger.debug("Product {} activated - inventory remains unchanged", productId);
            
        } catch (Exception ex) {
            logger.error("Failed to process product activated event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleProductDeactivated(Map<String, Object> event) {
        logger.info("Processing product deactivated event");
        
        try {
            Long productId = extractProductId(event);
            
            // When product is deactivated, we might want to release any active reservations
            // This would require additional service methods
            logger.info("Product {} deactivated - consider releasing active reservations", productId);
            
        } catch (Exception ex) {
            logger.error("Failed to process product deactivated event", ex);
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

    private UUID extractOrderId(Map<String, Object> event) {
        Object orderIdObj = event.get("orderId");
        if (orderIdObj instanceof String) {
            return UUID.fromString((String) orderIdObj);
        } else {
            throw new IllegalArgumentException("Invalid orderId format: " + orderIdObj);
        }
    }

    // =====================================================
    // Cart Events (for real-time stock validation)
    // =====================================================

    @KafkaListener(topics = "cart-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleCartEvents(@Payload Map<String, Object> event,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                Acknowledgment acknowledgment) {
        
        logger.info("Received cart event from topic: {} partition: {} key: {}", topic, partition, key);
        
        try {
            String eventType = (String) event.get("eventType");
            
            switch (eventType) {
                case "cart.item.added":
                    handleCartItemAdded(event);
                    break;
                case "cart.item.updated":
                    handleCartItemUpdated(event);
                    break;
                case "cart.stock.validation.failed":
                    handleCartStockValidationFailed(event);
                    break;
                case "cart.converted.to.order":
                    handleCartConvertedToOrder(event);
                    break;
                default:
                    logger.debug("Unhandled cart event type: {}", eventType);
            }
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed cart event: {}", eventType);
            
        } catch (Exception ex) {
            logger.error("Failed to process cart event from topic: {}", topic, ex);
            // Don't acknowledge on failure - message will be retried
        }
    }

    private void handleCartItemAdded(Map<String, Object> event) {
        logger.debug("Processing cart item added event");
        
        try {
            String cartId = (String) event.get("cartId");
            String userId = (String) event.get("userId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> cartData = (Map<String, Object>) event.get("cartData");
            
            if (cartData != null) {
                Long productId = extractProductId(cartData.get("productId"));
                Integer quantity = (Integer) cartData.get("quantity");
                
                // Validate current stock availability (proactive validation)
                try {
                    var availabilityResponse = inventoryService.checkAvailability(productId, quantity);
                    if (availabilityResponse != null && !Boolean.TRUE.equals(availabilityResponse.getAvailable())) {
                        logger.warn("Stock validation failed for cart {} - product {} quantity {} may have insufficient stock", 
                                   cartId, productId, quantity);
                        
                        // Could publish a stock validation alert event here if needed
                        // This is for monitoring and analytics purposes
                    } else {
                        logger.debug("Stock validation passed for cart {} - product {} quantity {}", 
                                    cartId, productId, quantity);
                    }
                } catch (Exception ex) {
                    logger.error("Error validating stock for cart item added event - cart: {} product: {}", 
                               cartId, productId, ex);
                }
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process cart item added event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleCartItemUpdated(Map<String, Object> event) {
        logger.debug("Processing cart item updated event");
        
        try {
            String cartId = (String) event.get("cartId");
            String userId = (String) event.get("userId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> cartData = (Map<String, Object>) event.get("cartData");
            
            if (cartData != null) {
                Long productId = extractProductId(cartData.get("productId"));
                Integer newQuantity = (Integer) cartData.get("newQuantity");
                Integer oldQuantity = (Integer) cartData.get("oldQuantity");
                
                // Validate new stock availability
                try {
                    if (newQuantity > oldQuantity) {
                        // Quantity increased - validate additional stock
                        Integer additionalQuantity = newQuantity - oldQuantity;
                        var availabilityResponse = inventoryService.checkAvailability(productId, newQuantity);
                        
                        if (availabilityResponse != null && !Boolean.TRUE.equals(availabilityResponse.getAvailable())) {
                            logger.warn("Stock validation failed for cart {} - product {} updated quantity {} exceeds available stock", 
                                       cartId, productId, newQuantity);
                            
                            // Could trigger a stock shortage alert
                        } else {
                            logger.debug("Stock validation passed for cart {} - product {} updated to quantity {}", 
                                        cartId, productId, newQuantity);
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Error validating stock for cart item updated event - cart: {} product: {}", 
                               cartId, productId, ex);
                }
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process cart item updated event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleCartStockValidationFailed(Map<String, Object> event) {
        logger.warn("Processing cart stock validation failed event");
        
        try {
            String cartId = (String) event.get("cartId");
            String userId = (String) event.get("userId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> cartData = (Map<String, Object>) event.get("cartData");
            
            if (cartData != null) {
                Long productId = extractProductId(cartData.get("productId"));
                Integer requestedQuantity = (Integer) cartData.get("requestedQuantity");
                Integer availableStock = (Integer) cartData.get("availableStock");
                String productName = (String) cartData.get("productName");
                
                logger.error("Stock validation failed - cart: {}, product: {} ({}), requested: {}, available: {}", 
                           cartId, productId, productName, requestedQuantity, availableStock);
                
                // This event indicates a stock shortage situation
                // Inventory service can use this for:
                // 1. Triggering low stock alerts
                // 2. Adjusting stock availability calculations
                // 3. Analytics on high-demand products
                
                // Check if we need to trigger a low stock alert
                try {
                    if (availableStock != null && availableStock < 10) { // configurable threshold
                        logger.info("Low stock detected for product {} due to cart validation failure - available: {}", 
                                   productId, availableStock);
                        
                        // Log for analytics - actual alert publishing would be handled by scheduled jobs
                        // or other inventory management processes
                    }
                } catch (Exception ex) {
                    logger.error("Error processing low stock detection for product: {}", productId, ex);
                }
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process cart stock validation failed event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleCartConvertedToOrder(Map<String, Object> event) {
        logger.info("Processing cart converted to order event");
        
        try {
            String cartId = (String) event.get("cartId");
            String userId = (String) event.get("userId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> cartData = (Map<String, Object>) event.get("cartData");
            
            if (cartData != null && cartData.containsKey("orderId")) {
                String orderIdStr = (String) cartData.get("orderId");
                UUID orderId = UUID.fromString(orderIdStr);
                Integer itemCount = (Integer) cartData.get("itemCount");
                
                logger.info("Cart {} with {} items converted to order {} - inventory reservation should be handled by order service", 
                           cartId, itemCount, orderId);
                
                // The actual inventory reservation is handled by the order service
                // This event is mainly for analytics and tracking purposes
                // Inventory service can track conversion patterns, popular products, etc.
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process cart converted to order event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private Long extractProductId(Object productIdObj) {
        if (productIdObj instanceof Number) {
            return ((Number) productIdObj).longValue();
        } else if (productIdObj instanceof String) {
            return Long.parseLong((String) productIdObj);
        } else {
            throw new IllegalArgumentException("Invalid productId format: " + productIdObj);
        }
    }
}