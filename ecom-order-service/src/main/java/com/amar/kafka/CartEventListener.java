package com.amar.kafka;

import com.amar.service.OrderService;
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
public class CartEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CartEventListener.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Autowired
    public CartEventListener(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    // =====================================================
    // Cart Events (for order lifecycle management)
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
                case "cart.converted.to.order":
                    handleCartConvertedToOrder(event);
                    break;
                case "cart.abandoned":
                    handleCartAbandoned(event);
                    break;
                case "cart.stock.validation.failed":
                    handleCartStockValidationFailed(event);
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

    private void handleCartConvertedToOrder(Map<String, Object> event) {
        logger.info("Processing cart converted to order event");
        
        try {
            String cartId = (String) event.get("cartId");
            String userId = (String) event.get("userId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> cartData = (Map<String, Object>) event.get("cartData");
            
            if (cartData != null && cartData.containsKey("orderId")) {
                String orderId = (String) cartData.get("orderId");
                Integer itemCount = (Integer) cartData.get("itemCount");
                
                logger.info("Cart {} converted to order {} with {} items for user {}", 
                           cartId, orderId, itemCount, userId);
                
                // Order service can log this for analytics/tracking
                // The actual order creation happens through direct API calls
                
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process cart converted to order event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleCartAbandoned(Map<String, Object> event) {
        logger.info("Processing cart abandoned event");
        
        try {
            String cartId = (String) event.get("cartId");
            String userId = (String) event.get("userId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> cartData = (Map<String, Object>) event.get("cartData");
            
            if (cartData != null) {
                Integer itemCount = (Integer) cartData.get("itemCount");
                Number totalAmount = (Number) cartData.get("totalAmount");
                Number abandonedAfterMinutes = (Number) cartData.get("abandonedAfterMinutes");
                
                logger.info("Cart {} abandoned by user {} - {} items worth {} after {} minutes", 
                           cartId, userId, itemCount, totalAmount, abandonedAfterMinutes);
                
                // Order service can use this for:
                // 1. Analytics on cart abandonment
                // 2. Triggering abandoned cart recovery campaigns
                // 3. Stock reservation cleanup if needed
                
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process cart abandoned event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handleCartStockValidationFailed(Map<String, Object> event) {
        logger.info("Processing cart stock validation failed event");
        
        try {
            String cartId = (String) event.get("cartId");
            String userId = (String) event.get("userId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> cartData = (Map<String, Object>) event.get("cartData");
            
            if (cartData != null) {
                Number productId = (Number) cartData.get("productId");
                String productName = (String) cartData.get("productName");
                Integer requestedQuantity = (Integer) cartData.get("requestedQuantity");
                Integer availableStock = (Integer) cartData.get("availableStock");
                
                logger.warn("Stock validation failed for cart {} - product {} ({}): requested {}, available {}", 
                           cartId, productId, productName, requestedQuantity, availableStock);
                
                // Order service can use this for:
                // 1. Preventing order creation with insufficient stock
                // 2. Alerting about high-demand products
                // 3. Analytics on stock shortage incidents
                
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process cart stock validation failed event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }
}