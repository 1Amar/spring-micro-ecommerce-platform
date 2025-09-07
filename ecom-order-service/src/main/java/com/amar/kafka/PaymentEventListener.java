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
import java.util.UUID;

@Component
public class PaymentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventListener.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Autowired
    public PaymentEventListener(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    // =====================================================
    // Payment Events (for order payment status updates)
    // =====================================================

    @KafkaListener(topics = "payment-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentEvents(@Payload Map<String, Object> event,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                   Acknowledgment acknowledgment) {
        
        logger.info("Received payment event from topic: {} partition: {} key: {}", topic, partition, key);
        
        try {
            String eventType = (String) event.get("eventType");
            
            switch (eventType) {
                case "payment.initiated":
                    handlePaymentInitiated(event);
                    break;
                case "payment.completed":
                    handlePaymentCompleted(event);
                    break;
                case "payment.failed":
                    handlePaymentFailed(event);
                    break;
                case "payment.refunded":
                    handlePaymentRefunded(event);
                    break;
                case "payment.cancelled":
                    handlePaymentCancelled(event);
                    break;
                default:
                    logger.debug("Unhandled payment event type: {}", eventType);
            }
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed payment event: {}", eventType);
            
        } catch (Exception ex) {
            logger.error("Failed to process payment event from topic: {}", topic, ex);
            // Don't acknowledge on failure - message will be retried
        }
    }

    private void handlePaymentInitiated(Map<String, Object> event) {
        logger.info("Processing payment initiated event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentData = (Map<String, Object>) event.get("paymentData");
            
            if (paymentData != null && paymentData.containsKey("orderId")) {
                String orderIdStr = (String) paymentData.get("orderId");
                UUID orderId = UUID.fromString(orderIdStr);
                
                Number amount = (Number) paymentData.get("amount");
                String paymentMethod = (String) paymentData.get("paymentMethod");
                
                logger.info("Payment {} initiated for order {} - amount: {}, method: {}", 
                           paymentId, orderId, amount, paymentMethod);
                
                // Update order status to indicate payment is being processed
                try {
                    orderService.updateOrderPaymentStatus(orderId, "PAYMENT_INITIATED", paymentId);
                } catch (Exception ex) {
                    logger.error("Failed to update order payment status for order: {}", orderId, ex);
                    // Continue processing - this shouldn't fail the entire event
                }
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process payment initiated event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handlePaymentCompleted(Map<String, Object> event) {
        logger.info("Processing payment completed event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentData = (Map<String, Object>) event.get("paymentData");
            
            if (paymentData != null && paymentData.containsKey("orderId")) {
                String orderIdStr = (String) paymentData.get("orderId");
                UUID orderId = UUID.fromString(orderIdStr);
                
                Number amount = (Number) paymentData.get("amount");
                String transactionId = (String) paymentData.get("transactionId");
                
                logger.info("Payment {} completed for order {} - amount: {}, transaction: {}", 
                           paymentId, orderId, amount, transactionId);
                
                // Update order status to PAID and trigger fulfillment
                try {
                    orderService.updateOrderPaymentStatus(orderId, "PAYMENT_COMPLETED", paymentId);
                    orderService.processPaymentCompletedOrder(orderId, paymentId, transactionId);
                } catch (Exception ex) {
                    logger.error("Failed to process payment completion for order: {}", orderId, ex);
                    throw ex; // Re-throw to prevent acknowledgment - this is critical
                }
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process payment completed event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handlePaymentFailed(Map<String, Object> event) {
        logger.info("Processing payment failed event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentData = (Map<String, Object>) event.get("paymentData");
            
            if (paymentData != null && paymentData.containsKey("orderId")) {
                String orderIdStr = (String) paymentData.get("orderId");
                UUID orderId = UUID.fromString(orderIdStr);
                
                String errorCode = (String) paymentData.get("errorCode");
                String errorMessage = (String) paymentData.get("errorMessage");
                
                logger.error("Payment {} failed for order {} - error: {} ({})", 
                           paymentId, orderId, errorCode, errorMessage);
                
                // Update order status to PAYMENT_FAILED and release inventory
                try {
                    orderService.updateOrderPaymentStatus(orderId, "PAYMENT_FAILED", paymentId);
                    orderService.processPaymentFailedOrder(orderId, paymentId, errorCode, errorMessage);
                } catch (Exception ex) {
                    logger.error("Failed to process payment failure for order: {}", orderId, ex);
                    throw ex; // Re-throw to prevent acknowledgment - this is critical
                }
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process payment failed event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handlePaymentRefunded(Map<String, Object> event) {
        logger.info("Processing payment refunded event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentData = (Map<String, Object>) event.get("paymentData");
            
            if (paymentData != null && paymentData.containsKey("orderId")) {
                String orderIdStr = (String) paymentData.get("orderId");
                UUID orderId = UUID.fromString(orderIdStr);
                
                Number refundAmount = (Number) paymentData.get("refundAmount");
                String refundId = (String) paymentData.get("refundId");
                
                logger.info("Payment {} refunded for order {} - amount: {}, refund ID: {}", 
                           paymentId, orderId, refundAmount, refundId);
                
                // Update order status and handle refund processing
                try {
                    orderService.updateOrderPaymentStatus(orderId, "PAYMENT_REFUNDED", paymentId);
                    orderService.processRefundedOrder(orderId, paymentId, refundId, refundAmount);
                } catch (Exception ex) {
                    logger.error("Failed to process payment refund for order: {}", orderId, ex);
                    throw ex; // Re-throw to prevent acknowledgment - this is critical
                }
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process payment refunded event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }

    private void handlePaymentCancelled(Map<String, Object> event) {
        logger.info("Processing payment cancelled event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentData = (Map<String, Object>) event.get("paymentData");
            
            if (paymentData != null && paymentData.containsKey("orderId")) {
                String orderIdStr = (String) paymentData.get("orderId");
                UUID orderId = UUID.fromString(orderIdStr);
                
                String reason = (String) paymentData.get("reason");
                
                logger.info("Payment {} cancelled for order {} - reason: {}", 
                           paymentId, orderId, reason);
                
                // Update order status and release inventory
                try {
                    orderService.updateOrderPaymentStatus(orderId, "PAYMENT_CANCELLED", paymentId);
                    orderService.processCancelledPaymentOrder(orderId, paymentId, reason);
                } catch (Exception ex) {
                    logger.error("Failed to process payment cancellation for order: {}", orderId, ex);
                    throw ex; // Re-throw to prevent acknowledgment - this is critical
                }
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process payment cancelled event", ex);
            throw ex; // Re-throw to prevent acknowledgment
        }
    }
}