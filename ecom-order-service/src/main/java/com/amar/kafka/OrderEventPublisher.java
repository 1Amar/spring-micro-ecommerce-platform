package com.amar.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${order.kafka.topics.order-events:order-events}")
    private String orderEventsTopic;

    @Autowired
    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // =====================================================
    // Order Lifecycle Events
    // =====================================================

    public void publishOrderEvent(String eventType, UUID orderId, Map<String, Object> orderData) {
        logger.debug("Publishing order event: {} for order: {}", eventType, orderId);
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "orderId", orderId.toString(),
                "orderData", orderData,
                "timestamp", LocalDateTime.now(),
                "source", "ecom-order-service"
            );
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(orderEventsTopic, orderId.toString(), event);
            
            future.thenAccept(result -> 
                logger.debug("Successfully published order event: {} for order: {}", eventType, orderId))
                .exceptionally(ex -> {
                    logger.error("Failed to publish order event: {} for order: {}", eventType, orderId, ex);
                    return null;
                });
                
        } catch (Exception ex) {
            logger.error("Error publishing order event: {} for order: {}", eventType, orderId, ex);
        }
    }

    public void publishOrderCreated(UUID orderId, String userId, List<OrderItem> items, 
                                   String totalAmount, String status) {
        Map<String, Object> orderData = Map.of(
            "userId", userId != null ? userId : "",
            "items", items,
            "totalAmount", totalAmount,
            "status", status,
            "itemCount", items.size()
        );
        
        publishOrderEvent("order.created", orderId, orderData);
    }

    public void publishOrderUpdated(UUID orderId, String status, String previousStatus, String reason) {
        Map<String, Object> orderData = Map.of(
            "status", status,
            "previousStatus", previousStatus,
            "reason", reason != null ? reason : "Order status updated"
        );
        
        publishOrderEvent("order.updated", orderId, orderData);
    }

    public void publishOrderConfirmed(UUID orderId, String confirmationNumber) {
        Map<String, Object> orderData = Map.of(
            "confirmationNumber", confirmationNumber,
            "status", "CONFIRMED"
        );
        
        publishOrderEvent("order.confirmed", orderId, orderData);
    }

    public void publishOrderCancelled(UUID orderId, String reason, String cancelledBy) {
        Map<String, Object> orderData = Map.of(
            "reason", reason != null ? reason : "Order cancelled",
            "cancelledBy", cancelledBy != null ? cancelledBy : "SYSTEM",
            "status", "CANCELLED"
        );
        
        publishOrderEvent("order.cancelled", orderId, orderData);
    }

    // =====================================================
    // Payment Events
    // =====================================================

    public void publishPaymentCompleted(UUID orderId, String paymentId, String amount, String paymentMethod) {
        Map<String, Object> orderData = Map.of(
            "paymentId", paymentId,
            "amount", amount,
            "paymentMethod", paymentMethod,
            "status", "PAID"
        );
        
        publishOrderEvent("order.payment.completed", orderId, orderData);
    }

    public void publishPaymentFailed(UUID orderId, String paymentId, String reason, String errorCode) {
        Map<String, Object> orderData = Map.of(
            "paymentId", paymentId != null ? paymentId : "",
            "reason", reason,
            "errorCode", errorCode,
            "status", "PAYMENT_FAILED"
        );
        
        publishOrderEvent("order.payment.failed", orderId, orderData);
    }

    // =====================================================
    // Fulfillment Events
    // =====================================================

    public void publishOrderShipped(UUID orderId, String trackingNumber, String carrier) {
        Map<String, Object> orderData = Map.of(
            "trackingNumber", trackingNumber,
            "carrier", carrier,
            "status", "SHIPPED"
        );
        
        publishOrderEvent("order.shipped", orderId, orderData);
    }

    public void publishOrderDelivered(UUID orderId, String deliveryDate, String signature) {
        Map<String, Object> orderData = Map.of(
            "deliveryDate", deliveryDate,
            "signature", signature != null ? signature : "",
            "status", "DELIVERED"
        );
        
        publishOrderEvent("order.delivered", orderId, orderData);
    }

    // =====================================================
    // Order Item Data Class
    // =====================================================

    public static class OrderItem {
        private Long productId;
        private Integer quantity;
        private String unitPrice;
        private String totalPrice;
        private String productName;
        
        public OrderItem() {}
        
        public OrderItem(Long productId, Integer quantity, String unitPrice, String totalPrice, String productName) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
            this.productName = productName;
        }
        
        // Getters and setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        
        public String getUnitPrice() { return unitPrice; }
        public void setUnitPrice(String unitPrice) { this.unitPrice = unitPrice; }
        
        public String getTotalPrice() { return totalPrice; }
        public void setTotalPrice(String totalPrice) { this.totalPrice = totalPrice; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
    }
}