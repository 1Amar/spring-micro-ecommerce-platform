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
import java.util.Map;

@Component
public class CartEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CartEventListener.class);

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @Autowired
    public CartEventListener(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "cart-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleCartEvent(@Payload Map<String, Object> event,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.RECEIVED_KEY) String key,
                               Acknowledgment acknowledgment) {

        logger.info("Received cart event from topic: {} partition: {} key: {}", topic, partition, key);

        try {
            String eventType = (String) event.get("eventType");
            String cartId = (String) event.get("cartId");

            logger.debug("Processing cart event type: {} for cart: {}", eventType, cartId);

            switch (eventType) {
                case "cart.converted.to.order":
                    handleCartConvertedToOrder(event);
                    break;
                case "cart.abandoned":
                    handleCartAbandoned(event);
                    break;
                case "cart.checkout.started":
                    handleCheckoutStarted(event);
                    break;
                default:
                    logger.debug("Unhandled cart event type: {}", eventType);
            }

            acknowledgment.acknowledge();
            logger.debug("Successfully processed cart event: {}", eventType);

        } catch (Exception e) {
            logger.error("Error processing cart event: {}", e.getMessage(), e);
            // Don't acknowledge - message will be retried
            throw e;
        }
    }

    private void handleCartConvertedToOrder(Map<String, Object> event) {
        logger.info("Processing cart converted to order event");
        
        try {
            String cartId = (String) event.get("cartId");
            String userId = (String) event.get("userId");
            Map<String, Object> cartData = (Map<String, Object>) event.get("cartData");
            
            if (cartData == null) {
                logger.warn("Cart data is null for cart: {}", cartId);
                return;
            }

            String orderId = (String) cartData.get("orderId");
            Object totalAmountObj = cartData.get("totalAmount");
            BigDecimal totalAmount = parseAmount(totalAmountObj);
            Object itemCountObj = cartData.get("itemCount");
            Integer itemCount = itemCountObj instanceof Number ? ((Number) itemCountObj).intValue() : 0;
            
            logger.info("Cart converted to order - Cart: {}, Order: {}, User: {}, Amount: {}, Items: {}", 
                cartId, orderId, userId, totalAmount, itemCount);

            // Prepare payment for the new order
            if (orderId != null && totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentService.preparePaymentForCartConversion(orderId, userId, cartId, totalAmount);
            } else {
                logger.warn("Invalid order data for cart conversion - Order: {}, Amount: {}", orderId, totalAmount);
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process cart converted to order event", ex);
            throw ex;
        }
    }

    private void handleCartAbandoned(Map<String, Object> event) {
        logger.info("Processing cart abandoned event");
        
        try {
            String cartId = (String) event.get("cartId");
            String userId = (String) event.get("userId");
            Map<String, Object> cartData = (Map<String, Object>) event.get("cartData");
            
            Object totalAmountObj = null;
            if (cartData != null) {
                totalAmountObj = cartData.get("totalAmount");
            }
            BigDecimal totalAmount = parseAmount(totalAmountObj);
            
            logger.info("Cart abandoned - Cart: {}, User: {}, Amount: {}", cartId, userId, totalAmount);
            
            // Cancel any prepared payments
            paymentService.cancelPreparedPaymentForCart(cartId, "Cart abandoned");
            
        } catch (Exception ex) {
            logger.error("Failed to process cart abandoned event", ex);
            throw ex;
        }
    }

    private void handleCheckoutStarted(Map<String, Object> event) {
        logger.info("Processing checkout started event");
        
        try {
            String cartId = (String) event.get("cartId");
            String userId = (String) event.get("userId");
            Map<String, Object> cartData = (Map<String, Object>) event.get("cartData");
            
            if (cartData == null) {
                logger.warn("Cart data is null for checkout started: {}", cartId);
                return;
            }

            Object totalAmountObj = cartData.get("totalAmount");
            BigDecimal totalAmount = parseAmount(totalAmountObj);
            Object itemCountObj = cartData.get("itemCount");
            Integer itemCount = itemCountObj instanceof Number ? ((Number) itemCountObj).intValue() : 0;
            
            logger.info("Checkout started - Cart: {}, User: {}, Amount: {}, Items: {}", 
                cartId, userId, totalAmount, itemCount);

            // Pre-authorize payment methods if needed
            if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentService.preAuthorizePaymentForCheckout(cartId, userId, totalAmount);
            }
            
        } catch (Exception ex) {
            logger.error("Failed to process checkout started event", ex);
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