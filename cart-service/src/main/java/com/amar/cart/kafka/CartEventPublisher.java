package com.amar.cart.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class CartEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(CartEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${cart.kafka.topics.cart-events:cart-events}")
    private String cartEventsTopic;

    @Autowired
    public CartEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // =====================================================
    // Cart Lifecycle Events
    // =====================================================

    public void publishCartEvent(String eventType, String cartId, String userId, Map<String, Object> cartData) {
        logger.debug("Publishing cart event: {} for cart: {}", eventType, cartId);
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "cartId", cartId != null ? cartId : "",
                "userId", userId != null ? userId : "",
                "cartData", cartData,
                "timestamp", LocalDateTime.now(),
                "source", "cart-service"
            );
            
            String key = cartId != null ? cartId : userId != null ? userId : "unknown";
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(cartEventsTopic, key, event);
            
            future.thenAccept(result -> 
                logger.debug("Successfully published cart event: {} for cart: {}", eventType, cartId))
                .exceptionally(ex -> {
                    logger.error("Failed to publish cart event: {} for cart: {}", eventType, cartId, ex);
                    return null;
                });
                
        } catch (Exception ex) {
            logger.error("Error publishing cart event: {} for cart: {}", eventType, cartId, ex);
        }
    }

    // =====================================================
    // Cart Item Events
    // =====================================================

    public void publishItemAdded(String cartId, String userId, Long productId, String productName, 
                               Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice, 
                               Integer cartItemCount, BigDecimal cartTotalAmount) {
        Map<String, Object> cartData = Map.of(
            "productId", productId,
            "productName", productName != null ? productName : "",
            "quantity", quantity != null ? quantity : 0,
            "unitPrice", unitPrice != null ? unitPrice : BigDecimal.ZERO,
            "totalPrice", totalPrice != null ? totalPrice : BigDecimal.ZERO,
            "cartItemCount", cartItemCount != null ? cartItemCount : 0,
            "cartTotalAmount", cartTotalAmount != null ? cartTotalAmount : BigDecimal.ZERO,
            "action", "ITEM_ADDED"
        );
        
        publishCartEvent("cart.item.added", cartId, userId, cartData);
        logger.info("Published cart.item.added event for cart: {} - product: {} (qty: {})", 
                   cartId, productId, quantity);
    }

    public void publishItemRemoved(String cartId, String userId, Long productId, String productName,
                                 Integer removedQuantity, Integer remainingQuantity, 
                                 Integer cartItemCount, BigDecimal cartTotalAmount) {
        Map<String, Object> cartData = Map.of(
            "productId", productId,
            "productName", productName != null ? productName : "",
            "removedQuantity", removedQuantity != null ? removedQuantity : 0,
            "remainingQuantity", remainingQuantity != null ? remainingQuantity : 0,
            "cartItemCount", cartItemCount != null ? cartItemCount : 0,
            "cartTotalAmount", cartTotalAmount != null ? cartTotalAmount : BigDecimal.ZERO,
            "action", "ITEM_REMOVED",
            "isCompletelyRemoved", remainingQuantity == null || remainingQuantity <= 0
        );
        
        publishCartEvent("cart.item.removed", cartId, userId, cartData);
        logger.info("Published cart.item.removed event for cart: {} - product: {} (removed: {}, remaining: {})", 
                   cartId, productId, removedQuantity, remainingQuantity);
    }

    public void publishItemUpdated(String cartId, String userId, Long productId, String productName,
                                 Integer oldQuantity, Integer newQuantity, BigDecimal unitPrice,
                                 Integer cartItemCount, BigDecimal cartTotalAmount) {
        Map<String, Object> cartData = Map.of(
            "productId", productId,
            "productName", productName != null ? productName : "",
            "oldQuantity", oldQuantity != null ? oldQuantity : 0,
            "newQuantity", newQuantity != null ? newQuantity : 0,
            "quantityChange", newQuantity != null && oldQuantity != null ? newQuantity - oldQuantity : 0,
            "unitPrice", unitPrice != null ? unitPrice : BigDecimal.ZERO,
            "cartItemCount", cartItemCount != null ? cartItemCount : 0,
            "cartTotalAmount", cartTotalAmount != null ? cartTotalAmount : BigDecimal.ZERO,
            "action", "ITEM_UPDATED"
        );
        
        publishCartEvent("cart.item.updated", cartId, userId, cartData);
        logger.debug("Published cart.item.updated event for cart: {} - product: {} (qty: {} -> {})", 
                    cartId, productId, oldQuantity, newQuantity);
    }

    // =====================================================
    // Cart Status Events
    // =====================================================

    public void publishCartCreated(String cartId, String userId, String sessionId) {
        Map<String, Object> cartData = Map.of(
            "sessionId", sessionId != null ? sessionId : "",
            "isAnonymous", userId == null || userId.isEmpty(),
            "createdBy", userId != null ? "USER" : "SESSION",
            "action", "CART_CREATED"
        );
        
        publishCartEvent("cart.created", cartId, userId, cartData);
        logger.info("Published cart.created event for cart: {} (user: {}, session: {})", 
                   cartId, userId, sessionId);
    }

    public void publishCartUpdated(String cartId, String userId, Integer itemCount, BigDecimal totalAmount,
                                 String updateReason) {
        Map<String, Object> cartData = Map.of(
            "itemCount", itemCount != null ? itemCount : 0,
            "totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO,
            "updateReason", updateReason != null ? updateReason : "Cart updated",
            "action", "CART_UPDATED"
        );
        
        publishCartEvent("cart.updated", cartId, userId, cartData);
        logger.debug("Published cart.updated event for cart: {} - items: {}, total: {}", 
                    cartId, itemCount, totalAmount);
    }

    public void publishCartCleared(String cartId, String userId, Integer clearedItemCount, 
                                 BigDecimal clearedAmount, String reason) {
        Map<String, Object> cartData = Map.of(
            "clearedItemCount", clearedItemCount != null ? clearedItemCount : 0,
            "clearedAmount", clearedAmount != null ? clearedAmount : BigDecimal.ZERO,
            "reason", reason != null ? reason : "Cart cleared",
            "action", "CART_CLEARED"
        );
        
        publishCartEvent("cart.cleared", cartId, userId, cartData);
        logger.info("Published cart.cleared event for cart: {} - cleared {} items worth {}", 
                   cartId, clearedItemCount, clearedAmount);
    }

    public void publishCartAbandoned(String cartId, String userId, Integer itemCount, 
                                   BigDecimal totalAmount, Long abandonedAfterMinutes) {
        Map<String, Object> cartData = Map.of(
            "itemCount", itemCount != null ? itemCount : 0,
            "totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO,
            "abandonedAfterMinutes", abandonedAfterMinutes != null ? abandonedAfterMinutes : 0,
            "reason", "Cart abandoned - no activity",
            "action", "CART_ABANDONED"
        );
        
        publishCartEvent("cart.abandoned", cartId, userId, cartData);
        logger.info("Published cart.abandoned event for cart: {} - {} items worth {} after {} minutes", 
                   cartId, itemCount, totalAmount, abandonedAfterMinutes);
    }

    // =====================================================
    // Cart Conversion Events
    // =====================================================

    public void publishCartConvertedToOrder(String cartId, String userId, String orderId, 
                                          Integer itemCount, BigDecimal totalAmount) {
        Map<String, Object> cartData = Map.of(
            "orderId", orderId != null ? orderId : "",
            "itemCount", itemCount != null ? itemCount : 0,
            "totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO,
            "conversionType", "ORDER_CREATION",
            "action", "CART_CONVERTED_TO_ORDER"
        );
        
        publishCartEvent("cart.converted.to.order", cartId, userId, cartData);
        logger.info("Published cart.converted.to.order event - cart: {} -> order: {} ({} items, {})", 
                   cartId, orderId, itemCount, totalAmount);
    }

    // =====================================================
    // User Association Events
    // =====================================================

    public void publishCartMerged(String targetCartId, String sourceCartId, String userId,
                                Integer mergedItemCount, BigDecimal finalTotalAmount) {
        Map<String, Object> cartData = Map.of(
            "sourceCartId", sourceCartId != null ? sourceCartId : "",
            "targetCartId", targetCartId != null ? targetCartId : "",
            "mergedItemCount", mergedItemCount != null ? mergedItemCount : 0,
            "finalTotalAmount", finalTotalAmount != null ? finalTotalAmount : BigDecimal.ZERO,
            "reason", "Anonymous cart merged with user cart",
            "action", "CART_MERGED"
        );
        
        publishCartEvent("cart.merged", targetCartId, userId, cartData);
        logger.info("Published cart.merged event - source: {} -> target: {} ({} items merged)", 
                   sourceCartId, targetCartId, mergedItemCount);
    }

    public void publishCartAssociatedWithUser(String cartId, String userId, String previousSessionId) {
        Map<String, Object> cartData = Map.of(
            "previousSessionId", previousSessionId != null ? previousSessionId : "",
            "reason", "Anonymous cart associated with logged-in user",
            "action", "CART_USER_ASSOCIATED"
        );
        
        publishCartEvent("cart.user.associated", cartId, userId, cartData);
        logger.info("Published cart.user.associated event for cart: {} - user: {} (session: {})", 
                   cartId, userId, previousSessionId);
    }

    // =====================================================
    // Stock Validation Events
    // =====================================================

    public void publishStockValidationFailed(String cartId, String userId, Long productId, 
                                           String productName, Integer requestedQuantity, 
                                           Integer availableStock) {
        Map<String, Object> cartData = Map.of(
            "productId", productId,
            "productName", productName != null ? productName : "",
            "requestedQuantity", requestedQuantity != null ? requestedQuantity : 0,
            "availableStock", availableStock != null ? availableStock : 0,
            "shortage", requestedQuantity != null && availableStock != null ? 
                       requestedQuantity - availableStock : 0,
            "reason", "Insufficient stock for cart item",
            "action", "STOCK_VALIDATION_FAILED"
        );
        
        publishCartEvent("cart.stock.validation.failed", cartId, userId, cartData);
        logger.warn("Published cart.stock.validation.failed event for cart: {} - product: {} (requested: {}, available: {})", 
                   cartId, productId, requestedQuantity, availableStock);
    }

    // =====================================================
    // Health Check Event
    // =====================================================

    public void publishHealthCheckEvent() {
        logger.debug("Publishing health check event");
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", "cart.service.health",
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "source", "cart-service"
            );
            
            kafkaTemplate.send(cartEventsTopic, "health-check", event);
            
        } catch (Exception ex) {
            logger.error("Error publishing health check event", ex);
        }
    }
}