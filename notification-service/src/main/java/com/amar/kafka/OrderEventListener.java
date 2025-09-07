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
import java.util.List;
import java.util.Map;

@Component
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
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
                case "order.shipped":
                    handleOrderShipped(event);
                    break;
                case "order.delivered":
                    handleOrderDelivered(event);
                    break;
                case "order.cancelled":
                    handleOrderCancelled(event);
                    break;
                case "order.returned":
                    handleOrderReturned(event);
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
            Object itemCountObj = orderData.get("itemCount");
            Integer itemCount = itemCountObj instanceof Number ? ((Number) itemCountObj).intValue() : 0;
            
            List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");
            
            logger.info("Order created - ID: {}, User: {}, Amount: {}, Items: {}", 
                orderId, userId, totalAmount, itemCount);

            // Send order creation confirmation
            String message = String.format("Your order #%s has been created successfully! " +
                "Total amount: $%.2f for %d item%s. We're processing your order now.", 
                orderId, totalAmount, itemCount, itemCount == 1 ? "" : "s");
                
            notificationService.sendNotification(userId, orderId, "ORDER_CREATED", message, "EMAIL");
            
            // Send SMS for immediate confirmation
            String smsMessage = String.format("Order #%s created! Amount: $%.2f. You'll receive updates via email.", 
                orderId, totalAmount);
            notificationService.sendNotification(userId, orderId, "ORDER_CREATED", smsMessage, "SMS");
            
            // Send detailed order summary via email
            if (items != null && !items.isEmpty()) {
                StringBuilder itemDetails = new StringBuilder();
                itemDetails.append("Order Details:\n");
                for (Map<String, Object> item : items) {
                    String productName = (String) item.get("productName");
                    Object quantityObj = item.get("quantity");
                    Object unitPriceObj = item.get("unitPrice");
                    
                    Integer quantity = quantityObj instanceof Number ? ((Number) quantityObj).intValue() : 1;
                    BigDecimal unitPrice = parseAmount(unitPriceObj);
                    
                    if (productName != null) {
                        itemDetails.append(String.format("- %s (Qty: %d) - $%.2f each\n", 
                            productName.replace("\"", ""), quantity, unitPrice));
                    }
                }
                
                String detailedMessage = String.format("Order #%s Details:\n\n%s\nTotal: $%.2f\n\nThank you for your order!", 
                    orderId, itemDetails.toString(), totalAmount);
                    
                notificationService.sendNotification(userId, orderId, "ORDER_DETAILS", detailedMessage, "EMAIL");
            }
            
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
            
            String confirmationNumber = null;
            String status = null;
            
            if (orderData != null) {
                confirmationNumber = (String) orderData.get("confirmationNumber");
                status = (String) orderData.get("status");
            }
            
            logger.info("Order confirmed - ID: {}, Confirmation: {}, Status: {}", orderId, confirmationNumber, status);

            // Send order confirmation notification
            String message;
            if (confirmationNumber != null) {
                message = String.format("Great news! Your order #%s has been confirmed. " +
                    "Confirmation number: %s. Your items are being prepared for shipment.", 
                    orderId, confirmationNumber);
            } else {
                message = String.format("Your order #%s has been confirmed and is being processed.", orderId);
            }
            
            // Extract user ID from order data or use a placeholder
            String userId = orderData != null ? (String) orderData.get("userId") : "unknown";
            notificationService.sendNotification(userId, orderId, "ORDER_CONFIRMED", message, "EMAIL");
            
        } catch (Exception ex) {
            logger.error("Failed to process order confirmed event", ex);
            throw ex;
        }
    }

    private void handleOrderShipped(Map<String, Object> event) {
        logger.info("Processing order shipped event");
        
        try {
            String orderId = (String) event.get("orderId");
            Map<String, Object> orderData = (Map<String, Object>) event.get("orderData");
            
            String trackingNumber = null;
            String carrier = null;
            String estimatedDelivery = null;
            String userId = null;
            
            if (orderData != null) {
                trackingNumber = (String) orderData.get("trackingNumber");
                carrier = (String) orderData.get("carrier");
                estimatedDelivery = (String) orderData.get("estimatedDelivery");
                userId = (String) orderData.get("userId");
            }
            
            logger.info("Order shipped - ID: {}, User: {}, Tracking: {}, Carrier: {}", 
                orderId, userId, trackingNumber, carrier);

            // Send shipment notification
            StringBuilder message = new StringBuilder();
            message.append(String.format("Exciting news! Your order #%s has been shipped!", orderId));
            
            if (trackingNumber != null) {
                message.append(String.format(" Tracking number: %s", trackingNumber));
            }
            
            if (carrier != null) {
                message.append(String.format(" via %s", carrier));
            }
            
            if (estimatedDelivery != null) {
                message.append(String.format(". Estimated delivery: %s", estimatedDelivery));
            } else {
                message.append(". You should receive it within 3-5 business days.");
            }
            
            if (userId == null) {
                userId = "unknown";
            }
            
            notificationService.sendNotification(userId, orderId, "ORDER_SHIPPED", message.toString(), "EMAIL");
            
            // Send SMS notification for shipment
            String smsMessage = String.format("Order #%s shipped! Track: %s", 
                orderId, trackingNumber != null ? trackingNumber : "Check email for details");
            notificationService.sendNotification(userId, orderId, "ORDER_SHIPPED", smsMessage, "SMS");
            
        } catch (Exception ex) {
            logger.error("Failed to process order shipped event", ex);
            throw ex;
        }
    }

    private void handleOrderDelivered(Map<String, Object> event) {
        logger.info("Processing order delivered event");
        
        try {
            String orderId = (String) event.get("orderId");
            Map<String, Object> orderData = (Map<String, Object>) event.get("orderData");
            
            String userId = null;
            String deliveryDate = null;
            String deliveryLocation = null;
            
            if (orderData != null) {
                userId = (String) orderData.get("userId");
                deliveryDate = (String) orderData.get("deliveryDate");
                deliveryLocation = (String) orderData.get("deliveryLocation");
            }
            
            logger.info("Order delivered - ID: {}, User: {}, Date: {}, Location: {}", 
                orderId, userId, deliveryDate, deliveryLocation);

            // Send delivery confirmation
            String message = String.format("Your order #%s has been delivered successfully!", orderId);
            
            if (deliveryDate != null) {
                message += String.format(" Delivered on: %s", deliveryDate);
            }
            
            if (deliveryLocation != null) {
                message += String.format(" at %s", deliveryLocation);
            }
            
            message += " Thank you for choosing us! We'd love your feedback.";
            
            if (userId == null) {
                userId = "unknown";
            }
            
            notificationService.sendNotification(userId, orderId, "ORDER_DELIVERED", message, "EMAIL");
            
            // Send SMS confirmation
            String smsMessage = String.format("Order #%s delivered! Thank you for your purchase.", orderId);
            notificationService.sendNotification(userId, orderId, "ORDER_DELIVERED", smsMessage, "SMS");
            
        } catch (Exception ex) {
            logger.error("Failed to process order delivered event", ex);
            throw ex;
        }
    }

    private void handleOrderCancelled(Map<String, Object> event) {
        logger.info("Processing order cancelled event");
        
        try {
            String orderId = (String) event.get("orderId");
            Map<String, Object> orderData = (Map<String, Object>) event.get("orderData");
            
            String userId = null;
            String cancellationReason = "Order cancelled per your request";
            BigDecimal refundAmount = BigDecimal.ZERO;
            
            if (orderData != null) {
                userId = (String) orderData.get("userId");
                String reason = (String) orderData.get("cancellationReason");
                if (reason != null) {
                    cancellationReason = reason;
                }
                refundAmount = parseAmount(orderData.get("totalAmount"));
            }
            
            logger.info("Order cancelled - ID: {}, User: {}, Reason: {}", orderId, userId, cancellationReason);

            // Send cancellation notification
            String message = String.format("Your order #%s has been cancelled. %s", orderId, cancellationReason);
            
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                message += String.format(" A refund of $%.2f will be processed within 3-5 business days.", refundAmount);
            }
            
            if (userId == null) {
                userId = "unknown";
            }
            
            notificationService.sendNotification(userId, orderId, "ORDER_CANCELLED", message, "EMAIL");
            
        } catch (Exception ex) {
            logger.error("Failed to process order cancelled event", ex);
            throw ex;
        }
    }

    private void handleOrderReturned(Map<String, Object> event) {
        logger.info("Processing order returned event");
        
        try {
            String orderId = (String) event.get("orderId");
            Map<String, Object> orderData = (Map<String, Object>) event.get("orderData");
            
            String userId = null;
            String returnReason = "Return processed";
            BigDecimal refundAmount = BigDecimal.ZERO;
            
            if (orderData != null) {
                userId = (String) orderData.get("userId");
                String reason = (String) orderData.get("returnReason");
                if (reason != null) {
                    returnReason = reason;
                }
                refundAmount = parseAmount(orderData.get("refundAmount"));
            }
            
            logger.info("Order returned - ID: {}, User: {}, Reason: {}, Refund: {}", 
                orderId, userId, returnReason, refundAmount);

            // Send return confirmation notification
            String message = String.format("Your return for order #%s has been processed. Reason: %s", 
                orderId, returnReason);
            
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                message += String.format(" A refund of $%.2f will be processed within 3-5 business days.", refundAmount);
            }
            
            if (userId == null) {
                userId = "unknown";
            }
            
            notificationService.sendNotification(userId, orderId, "ORDER_RETURNED", message, "EMAIL");
            
        } catch (Exception ex) {
            logger.error("Failed to process order returned event", ex);
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