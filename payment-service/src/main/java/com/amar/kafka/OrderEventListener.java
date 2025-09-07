package com.amar.kafka;

import com.amar.service.PaymentService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderEventListener(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderEvent(@Payload Map<String, Object> event,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                Acknowledgment acknowledgment) {

        logger.info("Received order event from topic: {} partition: {} key: {}", topic, partition, key);

        try {
            String eventType = (String) event.get("eventType");
            String orderId = (String) event.get("orderId");

            logger.debug("Processing order event type: {} for order: {}", eventType, orderId);

            switch (eventType) {
                case "order.created":
                    handleOrderCreated(event);
                    break;
                case "order.confirmed":
                    handleOrderConfirmed(event);
                    break;
                case "order.cancelled":
                    handleOrderCancelled(event);
                    break;
                default:
                    logger.debug("Unhandled order event type: {}", eventType);
            }

            acknowledgment.acknowledge();
            logger.debug("Successfully processed order event: {}", eventType);

        } catch (Exception e) {
            logger.error("Error processing order event: {}", e.getMessage(), e);
            // Don't acknowledge - message will be retried
            throw e;
        }
    }

    private void handleOrderCreated(Map<String, Object> event) {
        logger.info("Processing order created event");
        
        try {
            String orderId = (String) event.get("orderId");
            Map<String, Object> orderData = (Map<String, Object>) event.get("orderData");
            
            if (orderData == null) {
                logger.warn("Order data is null for order: {}", orderId);
                return;
            }

            String userId = (String) orderData.get("userId");
            Object totalAmountObj = orderData.get("totalAmount");
            BigDecimal totalAmount = parseAmount(totalAmountObj);
            
            List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");
            
            logger.info("Order created - ID: {}, User: {}, Amount: {}, Items: {}", 
                orderId, userId, totalAmount, items != null ? items.size() : 0);

            // Initiate payment process
            paymentService.initiatePaymentForOrder(orderId, userId, totalAmount, "CREDIT_CARD");
            
        } catch (Exception ex) {
            logger.error("Failed to process order created event", ex);
            throw ex;
        }
    }

    private void handleOrderConfirmed(Map<String, Object> event) {
        logger.info("Processing order confirmed event");
        
        try {
            String orderId = (String) event.get("orderId");
            Map<String, Object> orderData = (Map<String, Object>) event.get("orderData");
            
            if (orderData != null) {
                String confirmationNumber = (String) orderData.get("confirmationNumber");
                logger.info("Order confirmed - ID: {}, Confirmation: {}", orderId, confirmationNumber);
                
                // Update payment status if needed
                paymentService.updatePaymentForOrderConfirmation(orderId, confirmationNumber);
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process order confirmed event", ex);
            throw ex;
        }
    }

    private void handleOrderCancelled(Map<String, Object> event) {
        logger.info("Processing order cancelled event");
        
        try {
            String orderId = (String) event.get("orderId");
            Map<String, Object> orderData = (Map<String, Object>) event.get("orderData");
            
            String cancellationReason = "Order cancelled";
            if (orderData != null) {
                cancellationReason = (String) orderData.getOrDefault("cancellationReason", cancellationReason);
            }
            
            logger.info("Order cancelled - ID: {}, Reason: {}", orderId, cancellationReason);
            
            // Cancel any pending payments or initiate refunds
            paymentService.cancelPaymentForOrder(orderId, cancellationReason);
            
        } catch (Exception ex) {
            logger.error("Failed to process order cancelled event", ex);
            throw ex;
        }
    }

    private BigDecimal parseAmount(Object amountObj) {
        if (amountObj == null) {
            return BigDecimal.ZERO;
        }
        
        if (amountObj instanceof String) {
            try {
                return new BigDecimal((String) amountObj);
            } catch (NumberFormatException e) {
                logger.warn("Invalid amount string: {}", amountObj);
                return BigDecimal.ZERO;
            }
        } else if (amountObj instanceof Number) {
            return BigDecimal.valueOf(((Number) amountObj).doubleValue());
        }
        
        logger.warn("Unknown amount type: {}", amountObj.getClass());
        return BigDecimal.ZERO;
    }
}