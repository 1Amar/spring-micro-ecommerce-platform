package com.amar.kafka;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.amar.dto.OrderStatusUpdate;
import com.amar.service.OrderWebSocketService;
import com.amar.service.EventOutboxService;

@Service
public class OrderEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderWebSocketService webSocketService;
    private final EventOutboxService eventOutboxService;

    @Value("${order.kafka.topics.order-events:order-events}")
    private String orderEventsTopic;

    @Autowired
    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                              OrderWebSocketService webSocketService,
                              EventOutboxService eventOutboxService) {
        this.kafkaTemplate = kafkaTemplate;
        this.webSocketService = webSocketService;
        this.eventOutboxService = eventOutboxService;
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
            
            // Use transactional outbox pattern for reliable event publishing
            eventOutboxService.saveEvent(
                orderId.toString(),
                "Order",
                eventType,
                event,
                orderEventsTopic,
                orderId.toString()
            );
            
            logger.debug("Order event saved to outbox: {} for order: {}", eventType, orderId);
                
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
        
        // Broadcast real-time WebSocket update
        broadcastWebSocketUpdate(orderId, null, previousStatus, status, reason, "SYSTEM");
    }

    public void publishOrderConfirmed(UUID orderId, String confirmationNumber) {
        Map<String, Object> orderData = Map.of(
            "confirmationNumber", confirmationNumber,
            "status", "CONFIRMED"
        );
        
        publishOrderEvent("order.confirmed", orderId, orderData);
        
        // Broadcast real-time WebSocket update
        broadcastWebSocketUpdate(orderId, confirmationNumber, "PENDING", "CONFIRMED", 
                                "Order confirmed with confirmation number: " + confirmationNumber, "SYSTEM");
    }

    public void publishOrderCancelled(UUID orderId, String reason, String cancelledBy) {
        Map<String, Object> orderData = Map.of(
            "reason", reason != null ? reason : "Order cancelled",
            "cancelledBy", cancelledBy != null ? cancelledBy : "SYSTEM",
            "status", "CANCELLED"
        );
        
        publishOrderEvent("order.cancelled", orderId, orderData);
        
        // Broadcast real-time WebSocket update
        broadcastWebSocketUpdate(orderId, null, null, "CANCELLED", reason, cancelledBy);
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

    // =====================================================
    // WebSocket Broadcasting Helper
    // =====================================================

    /**
     * Broadcast order status updates via WebSocket in addition to Kafka
     */
    private void broadcastWebSocketUpdate(UUID orderId, String orderNumber, String previousStatus, 
                                        String newStatus, String reason, String changedBy) {
        try {
            OrderStatusUpdate update = new OrderStatusUpdate();
            update.setOrderId(orderId);
            update.setOrderNumber(orderNumber);
            update.setPreviousStatus(previousStatus);
            update.setNewStatus(newStatus);
            update.setReason(reason);
            update.setChangedBy(changedBy);
            
            // Let WebSocketService handle the broadcasting
            webSocketService.broadcastOrderStatusUpdate(update);
            
            logger.debug("Broadcasted WebSocket update for order: {} - Status: {} -> {}", 
                        orderId, previousStatus, newStatus);
                        
        } catch (Exception e) {
            logger.error("Failed to broadcast WebSocket update for order: {}", orderId, e);
            // Don't let WebSocket failures affect Kafka publishing
        }
    }
}