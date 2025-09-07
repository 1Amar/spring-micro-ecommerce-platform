package com.amar.kafka;

import com.amar.service.NotificationService;
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
import java.util.Map;

@Component
public class PaymentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Autowired
    public PaymentEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentEvent(@Payload Map<String, Object> event,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                  Acknowledgment acknowledgment) {

        logger.info("Received payment event from topic: {} partition: {} key: {}", topic, partition, key);

        try {
            String eventType = (String) event.get("eventType");
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");

            logger.debug("Processing payment event type: {} for payment: {} order: {}", eventType, paymentId, orderId);

            switch (eventType) {
                case "payment.initiated":
                    handlePaymentInitiated(event);
                    break;
                case "payment.processing":
                    handlePaymentProcessing(event);
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
                case "payment.paypal.created":
                    handlePayPalPaymentCreated(event);
                    break;
                case "payment.paypal.approved":
                    handlePayPalPaymentApproved(event);
                    break;
                default:
                    logger.debug("Unhandled payment event type: {}", eventType);
            }

            acknowledgment.acknowledge();
            logger.debug("Successfully processed payment event: {}", eventType);

        } catch (Exception e) {
            logger.error("Error processing payment event: {}", e.getMessage(), e);
            // Don't acknowledge - message will be retried
            throw e;
        }
    }

    private void handlePaymentInitiated(Map<String, Object> event) {
        logger.info("Processing payment initiated event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String userId = (String) event.get("userId");
            Map<String, Object> paymentData = (Map<String, Object>) event.get("paymentData");
            
            if (paymentData != null) {
                BigDecimal amount = parseAmount(paymentData.get("amount"));
                String paymentMethod = (String) paymentData.get("paymentMethod");
                
                logger.info("Payment initiated - Payment: {}, Order: {}, User: {}, Amount: {}, Method: {}", 
                    paymentId, orderId, userId, amount, paymentMethod);

                // Send payment initiated notification
                String message = String.format("Payment initiated for order %s. Amount: $%.2f using %s", 
                    orderId, amount, paymentMethod);
                    
                notificationService.sendNotification(userId, orderId, "PAYMENT_INITIATED", message, "EMAIL");
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process payment initiated event", ex);
            throw ex;
        }
    }

    private void handlePaymentProcessing(Map<String, Object> event) {
        logger.info("Processing payment processing event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String userId = (String) event.get("userId");
            String gateway = (String) event.get("gateway");
            
            logger.info("Payment processing - Payment: {}, Order: {}, User: {}, Gateway: {}", 
                paymentId, orderId, userId, gateway);

            // Optional: Send processing notification for high-value transactions
            Map<String, Object> paymentData = (Map<String, Object>) event.get("paymentData");
            if (paymentData != null) {
                BigDecimal amount = parseAmount(paymentData.get("amount"));
                if (amount.compareTo(BigDecimal.valueOf(100.0)) > 0) { // High value threshold
                    String message = String.format("Processing your payment of $%.2f for order %s", amount, orderId);
                    notificationService.sendNotification(userId, orderId, "PAYMENT_PROCESSING", message, "SMS");
                }
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process payment processing event", ex);
            throw ex;
        }
    }

    private void handlePaymentCompleted(Map<String, Object> event) {
        logger.info("Processing payment completed event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String userId = (String) event.get("userId");
            String transactionId = (String) event.get("transactionId");
            Map<String, Object> paymentData = (Map<String, Object>) event.get("paymentData");
            
            if (paymentData != null) {
                BigDecimal amount = parseAmount(paymentData.get("amount"));
                
                logger.info("Payment completed - Payment: {}, Order: {}, User: {}, Amount: {}, Transaction: {}", 
                    paymentId, orderId, userId, amount, transactionId);

                // Send payment success notification
                String message = String.format("Payment of $%.2f completed successfully for order %s. Transaction ID: %s", 
                    amount, orderId, transactionId);
                    
                notificationService.sendNotification(userId, orderId, "PAYMENT_SUCCESS", message, "EMAIL");
                
                // Send SMS for immediate confirmation
                String smsMessage = String.format("Payment confirmed! $%.2f for order %s. Thank you!", amount, orderId);
                notificationService.sendNotification(userId, orderId, "PAYMENT_SUCCESS", smsMessage, "SMS");
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process payment completed event", ex);
            throw ex;
        }
    }

    private void handlePaymentFailed(Map<String, Object> event) {
        logger.info("Processing payment failed event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String userId = (String) event.get("userId");
            String reason = (String) event.get("reason");
            String errorCode = (String) event.get("errorCode");
            Map<String, Object> paymentData = (Map<String, Object>) event.get("paymentData");
            
            if (paymentData != null) {
                BigDecimal amount = parseAmount(paymentData.get("amount"));
                
                logger.warn("Payment failed - Payment: {}, Order: {}, User: {}, Amount: {}, Reason: {}", 
                    paymentId, orderId, userId, amount, reason);

                // Send payment failure notification
                String message = String.format("Payment of $%.2f for order %s failed. Reason: %s. Please try again or contact support.", 
                    amount, orderId, reason);
                    
                notificationService.sendNotification(userId, orderId, "PAYMENT_FAILED", message, "EMAIL");
                
                // Send immediate SMS for critical failures
                if ("GATEWAY_DECLINED".equals(errorCode) || "INSUFFICIENT_FUNDS".equals(errorCode)) {
                    String smsMessage = String.format("Payment declined for order %s. Please check payment method.", orderId);
                    notificationService.sendNotification(userId, orderId, "PAYMENT_DECLINED", smsMessage, "SMS");
                }
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process payment failed event", ex);
            throw ex;
        }
    }

    private void handlePaymentRefunded(Map<String, Object> event) {
        logger.info("Processing payment refunded event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String userId = (String) event.get("userId");
            String refundId = (String) event.get("refundId");
            String reason = (String) event.get("reason");
            Map<String, Object> paymentData = (Map<String, Object>) event.get("paymentData");
            
            if (paymentData != null) {
                BigDecimal refundAmount = parseAmount(paymentData.get("refundAmount"));
                
                logger.info("Payment refunded - Payment: {}, Order: {}, User: {}, Refund Amount: {}, Refund ID: {}", 
                    paymentId, orderId, userId, refundAmount, refundId);

                // Send refund confirmation notification
                String message = String.format("Refund of $%.2f for order %s has been processed. Refund ID: %s. Reason: %s", 
                    refundAmount, orderId, refundId, reason);
                    
                notificationService.sendNotification(userId, orderId, "PAYMENT_REFUNDED", message, "EMAIL");
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process payment refunded event", ex);
            throw ex;
        }
    }

    private void handlePaymentCancelled(Map<String, Object> event) {
        logger.info("Processing payment cancelled event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String userId = (String) event.get("userId");
            String reason = (String) event.get("reason");
            
            logger.info("Payment cancelled - Payment: {}, Order: {}, User: {}, Reason: {}", 
                paymentId, orderId, userId, reason);

            // Send payment cancellation notification
            String message = String.format("Payment for order %s has been cancelled. Reason: %s", orderId, reason);
            notificationService.sendNotification(userId, orderId, "PAYMENT_CANCELLED", message, "EMAIL");
            
        } catch (Exception ex) {
            logger.error("Failed to process payment cancelled event", ex);
            throw ex;
        }
    }

    private void handlePayPalPaymentCreated(Map<String, Object> event) {
        logger.info("Processing PayPal payment created event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String paypalOrderId = (String) event.get("paypalOrderId");
            String userId = (String) event.get("userId");
            
            logger.info("PayPal payment created - Payment: {}, Order: {}, PayPal Order: {}, User: {}", 
                paymentId, orderId, paypalOrderId, userId);

            // Send PayPal payment creation notification
            String message = String.format("PayPal payment created for order %s. Please complete your payment in PayPal.", orderId);
            notificationService.sendNotification(userId, orderId, "PAYPAL_PAYMENT_CREATED", message, "EMAIL");
            
        } catch (Exception ex) {
            logger.error("Failed to process PayPal payment created event", ex);
            throw ex;
        }
    }

    private void handlePayPalPaymentApproved(Map<String, Object> event) {
        logger.info("Processing PayPal payment approved event");
        
        try {
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String paypalOrderId = (String) event.get("paypalOrderId");
            String payerId = (String) event.get("payerId");
            String userId = (String) event.get("userId");
            
            logger.info("PayPal payment approved - Payment: {}, Order: {}, PayPal Order: {}, Payer: {}, User: {}", 
                paymentId, orderId, paypalOrderId, payerId, userId);

            // Send PayPal approval confirmation
            String message = String.format("Your PayPal payment for order %s has been approved and will be processed shortly.", orderId);
            notificationService.sendNotification(userId, orderId, "PAYPAL_PAYMENT_APPROVED", message, "EMAIL");
            
        } catch (Exception ex) {
            logger.error("Failed to process PayPal payment approved event", ex);
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