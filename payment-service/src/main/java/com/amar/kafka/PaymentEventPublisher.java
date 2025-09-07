package com.amar.kafka;

import com.amar.config.KafkaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // =====================================================
    // Payment Lifecycle Events
    // =====================================================

    public void publishPaymentInitiated(String paymentId, String orderId, BigDecimal amount, String userId, String paymentMethod) {
        Map<String, Object> eventData = createBasePaymentEvent("payment.initiated", paymentId, orderId, amount, userId);
        eventData.put("paymentMethod", paymentMethod);
        eventData.put("status", "INITIATED");
        
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("orderId", orderId);
        paymentData.put("amount", amount);
        paymentData.put("currency", "USD");
        paymentData.put("paymentMethod", paymentMethod);
        paymentData.put("status", "INITIATED");
        paymentData.put("userId", userId);
        eventData.put("paymentData", paymentData);

        publishEvent(KafkaConfig.PAYMENT_EVENTS_TOPIC, paymentId, eventData);
    }

    public void publishPaymentProcessing(String paymentId, String orderId, BigDecimal amount, String userId, String gateway) {
        Map<String, Object> eventData = createBasePaymentEvent("payment.processing", paymentId, orderId, amount, userId);
        eventData.put("gateway", gateway);
        eventData.put("status", "PROCESSING");
        
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("orderId", orderId);
        paymentData.put("amount", amount);
        paymentData.put("gateway", gateway);
        paymentData.put("status", "PROCESSING");
        paymentData.put("processingStartedAt", LocalDateTime.now());
        eventData.put("paymentData", paymentData);

        publishEvent(KafkaConfig.PAYMENT_EVENTS_TOPIC, paymentId, eventData);
    }

    public void publishPaymentCompleted(String paymentId, String orderId, BigDecimal amount, String userId, String transactionId) {
        Map<String, Object> eventData = createBasePaymentEvent("payment.completed", paymentId, orderId, amount, userId);
        eventData.put("transactionId", transactionId);
        eventData.put("status", "COMPLETED");
        
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("orderId", orderId);
        paymentData.put("amount", amount);
        paymentData.put("currency", "USD");
        paymentData.put("transactionId", transactionId);
        paymentData.put("status", "COMPLETED");
        paymentData.put("completedAt", LocalDateTime.now());
        paymentData.put("userId", userId);
        eventData.put("paymentData", paymentData);

        publishEvent(KafkaConfig.PAYMENT_EVENTS_TOPIC, paymentId, eventData);
        logger.info("Payment completed event published for payment: {} order: {}", paymentId, orderId);
    }

    public void publishPaymentFailed(String paymentId, String orderId, BigDecimal amount, String userId, String reason, String errorCode) {
        Map<String, Object> eventData = createBasePaymentEvent("payment.failed", paymentId, orderId, amount, userId);
        eventData.put("reason", reason);
        eventData.put("errorCode", errorCode);
        eventData.put("status", "FAILED");
        
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("orderId", orderId);
        paymentData.put("amount", amount);
        paymentData.put("status", "FAILED");
        paymentData.put("failureReason", reason);
        paymentData.put("errorCode", errorCode);
        paymentData.put("failedAt", LocalDateTime.now());
        paymentData.put("userId", userId);
        eventData.put("paymentData", paymentData);

        publishEvent(KafkaConfig.PAYMENT_EVENTS_TOPIC, paymentId, eventData);
        logger.warn("Payment failed event published for payment: {} order: {} - Reason: {}", paymentId, orderId, reason);
    }

    public void publishPaymentRefunded(String paymentId, String orderId, BigDecimal refundAmount, String userId, String reason) {
        Map<String, Object> eventData = createBasePaymentEvent("payment.refunded", paymentId, orderId, refundAmount, userId);
        String refundId = UUID.randomUUID().toString();
        eventData.put("refundId", refundId);
        eventData.put("reason", reason);
        eventData.put("status", "REFUNDED");
        
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("orderId", orderId);
        paymentData.put("refundAmount", refundAmount);
        paymentData.put("refundId", refundId);
        paymentData.put("status", "REFUNDED");
        paymentData.put("refundReason", reason);
        paymentData.put("refundedAt", LocalDateTime.now());
        paymentData.put("userId", userId);
        eventData.put("paymentData", paymentData);

        publishEvent(KafkaConfig.PAYMENT_EVENTS_TOPIC, paymentId, eventData);
        logger.info("Payment refunded event published for payment: {} order: {} - Amount: {}", paymentId, orderId, refundAmount);
    }

    public void publishPaymentCancelled(String paymentId, String orderId, BigDecimal amount, String userId, String reason) {
        Map<String, Object> eventData = createBasePaymentEvent("payment.cancelled", paymentId, orderId, amount, userId);
        eventData.put("reason", reason);
        eventData.put("status", "CANCELLED");
        
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("orderId", orderId);
        paymentData.put("amount", amount);
        paymentData.put("status", "CANCELLED");
        paymentData.put("cancellationReason", reason);
        paymentData.put("cancelledAt", LocalDateTime.now());
        paymentData.put("userId", userId);
        eventData.put("paymentData", paymentData);

        publishEvent(KafkaConfig.PAYMENT_EVENTS_TOPIC, paymentId, eventData);
        logger.info("Payment cancelled event published for payment: {} order: {} - Reason: {}", paymentId, orderId, reason);
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private Map<String, Object> createBasePaymentEvent(String eventType, String paymentId, String orderId, BigDecimal amount, String userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("paymentId", paymentId);
        event.put("orderId", orderId);
        event.put("amount", amount);
        event.put("userId", userId);
        event.put("timestamp", LocalDateTime.now());
        event.put("source", "payment-service");
        event.put("eventId", UUID.randomUUID().toString());
        return event;
    }

    private void publishEvent(String topic, String key, Map<String, Object> eventData) {
        try {
            logger.debug("Publishing event to topic: {} with key: {} - Event: {}", topic, key, eventData.get("eventType"));
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, eventData);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.debug("Event published successfully: {} to topic: {} partition: {} offset: {}", 
                        eventData.get("eventType"), topic, 
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish event: {} to topic: {} - Error: {}", 
                        eventData.get("eventType"), topic, exception.getMessage());
                }
            });
            
        } catch (Exception e) {
            logger.error("Error publishing event: {} to topic: {} - Exception: {}", 
                eventData.get("eventType"), topic, e.getMessage(), e);
        }
    }

    // =====================================================
    // PayPal Gateway Specific Events
    // =====================================================

    public void publishPayPalPaymentCreated(String paymentId, String orderId, String paypalOrderId, BigDecimal amount, String userId) {
        Map<String, Object> eventData = createBasePaymentEvent("payment.paypal.created", paymentId, orderId, amount, userId);
        eventData.put("paypalOrderId", paypalOrderId);
        eventData.put("gateway", "PAYPAL");
        eventData.put("status", "PAYPAL_CREATED");
        
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("orderId", orderId);
        paymentData.put("paypalOrderId", paypalOrderId);
        paymentData.put("amount", amount);
        paymentData.put("gateway", "PAYPAL");
        paymentData.put("status", "PAYPAL_CREATED");
        paymentData.put("userId", userId);
        eventData.put("paymentData", paymentData);

        publishEvent(KafkaConfig.PAYMENT_EVENTS_TOPIC, paymentId, eventData);
        logger.info("PayPal payment created event published for payment: {} PayPal Order: {}", paymentId, paypalOrderId);
    }

    public void publishPayPalPaymentApproved(String paymentId, String orderId, String paypalOrderId, String payerId, BigDecimal amount, String userId) {
        Map<String, Object> eventData = createBasePaymentEvent("payment.paypal.approved", paymentId, orderId, amount, userId);
        eventData.put("paypalOrderId", paypalOrderId);
        eventData.put("payerId", payerId);
        eventData.put("gateway", "PAYPAL");
        eventData.put("status", "PAYPAL_APPROVED");
        
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("orderId", orderId);
        paymentData.put("paypalOrderId", paypalOrderId);
        paymentData.put("payerId", payerId);
        paymentData.put("amount", amount);
        paymentData.put("gateway", "PAYPAL");
        paymentData.put("status", "PAYPAL_APPROVED");
        paymentData.put("approvedAt", LocalDateTime.now());
        paymentData.put("userId", userId);
        eventData.put("paymentData", paymentData);

        publishEvent(KafkaConfig.PAYMENT_EVENTS_TOPIC, paymentId, eventData);
        logger.info("PayPal payment approved event published for payment: {} PayPal Order: {} Payer: {}", paymentId, paypalOrderId, payerId);
    }
}