package com.amar.service;

import com.amar.kafka.PaymentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentEventPublisher eventPublisher;
    
    // In-memory storage for payment tracking (in real app, use database)
    private final Map<String, PaymentInfo> payments = new ConcurrentHashMap<>();
    private final Map<String, String> orderToPaymentMapping = new ConcurrentHashMap<>();

    @Value("${payment.simulation.processing-time-ms:100}")
    private long processingTimeMs;

    @Value("${payment.simulation.success-rate:0.95}")
    private double successRate;

    @Autowired
    public PaymentService(PaymentEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    // =====================================================
    // Main Payment Processing Methods
    // =====================================================

    public PaymentResult processPayment(String orderId, BigDecimal amount, String userId, String paymentMethod) {
        String paymentId = UUID.randomUUID().toString();
        
        logger.info("Processing payment - ID: {}, Order: {}, Amount: {}, User: {}, Method: {}", 
            paymentId, orderId, amount, userId, paymentMethod);

        // Create payment record
        PaymentInfo payment = new PaymentInfo(paymentId, orderId, amount, userId, paymentMethod);
        payments.put(paymentId, payment);
        orderToPaymentMapping.put(orderId, paymentId);

        // Publish payment initiated event
        eventPublisher.publishPaymentInitiated(paymentId, orderId, amount, userId, paymentMethod);

        // Simulate payment processing
        payment.setStatus("PROCESSING");
        eventPublisher.publishPaymentProcessing(paymentId, orderId, amount, userId, "INTERNAL_GATEWAY");

        try {
            // Simulate processing time
            Thread.sleep(processingTimeMs);
            
            // Simulate success/failure based on success rate
            boolean isSuccess = ThreadLocalRandom.current().nextDouble() < successRate;
            
            if (isSuccess) {
                return completePayment(payment);
            } else {
                return failPayment(payment, "Payment declined by gateway", "GATEWAY_DECLINED");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failPayment(payment, "Payment processing interrupted", "PROCESSING_INTERRUPTED");
        } catch (Exception e) {
            logger.error("Payment processing error for payment: {}", paymentId, e);
            return failPayment(payment, "Internal processing error", "INTERNAL_ERROR");
        }
    }

    public PaymentResult processPayPalPayment(String orderId, BigDecimal amount, String userId) {
        String paymentId = UUID.randomUUID().toString();
        String paypalOrderId = "PAYPAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        logger.info("Processing PayPal payment - ID: {}, Order: {}, PayPal Order: {}, Amount: {}, User: {}", 
            paymentId, orderId, paypalOrderId, amount, userId);

        // Create payment record
        PaymentInfo payment = new PaymentInfo(paymentId, orderId, amount, userId, "PAYPAL");
        payment.setGateway("PAYPAL");
        payment.setGatewayTransactionId(paypalOrderId);
        payments.put(paymentId, payment);
        orderToPaymentMapping.put(orderId, paymentId);

        // Publish PayPal payment created event
        eventPublisher.publishPayPalPaymentCreated(paymentId, orderId, paypalOrderId, amount, userId);

        try {
            // Simulate PayPal processing
            Thread.sleep(processingTimeMs + 50);
            
            // Simulate PayPal approval
            String payerId = "PAYER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            payment.setPayerId(payerId);
            
            eventPublisher.publishPayPalPaymentApproved(paymentId, orderId, paypalOrderId, payerId, amount, userId);
            
            // Complete the payment
            return completePayment(payment);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failPayment(payment, "PayPal processing interrupted", "PAYPAL_INTERRUPTED");
        } catch (Exception e) {
            logger.error("PayPal payment processing error for payment: {}", paymentId, e);
            return failPayment(payment, "PayPal processing error", "PAYPAL_ERROR");
        }
    }

    // =====================================================
    // Event-Driven Methods (called by event listeners)
    // =====================================================

    public void initiatePaymentForOrder(String orderId, String userId, BigDecimal totalAmount, String defaultPaymentMethod) {
        logger.info("Initiating payment for order: {} - User: {}, Amount: {}", orderId, userId, totalAmount);
        
        // Check if payment already exists for this order
        if (orderToPaymentMapping.containsKey(orderId)) {
            logger.info("Payment already exists for order: {}", orderId);
            return;
        }

        String paymentId = UUID.randomUUID().toString();
        PaymentInfo payment = new PaymentInfo(paymentId, orderId, totalAmount, userId, defaultPaymentMethod);
        payment.setStatus("INITIATED");
        
        payments.put(paymentId, payment);
        orderToPaymentMapping.put(orderId, paymentId);

        eventPublisher.publishPaymentInitiated(paymentId, orderId, totalAmount, userId, defaultPaymentMethod);
        logger.info("Payment initiated for order: {} - Payment ID: {}", orderId, paymentId);
    }

    public void preparePaymentForCartConversion(String orderId, String userId, String cartId, BigDecimal totalAmount) {
        logger.info("Preparing payment for cart conversion - Order: {}, Cart: {}, User: {}, Amount: {}", 
            orderId, cartId, userId, totalAmount);

        // Similar to initiatePaymentForOrder but with cart context
        if (orderToPaymentMapping.containsKey(orderId)) {
            logger.info("Payment already exists for order: {}", orderId);
            return;
        }

        String paymentId = UUID.randomUUID().toString();
        PaymentInfo payment = new PaymentInfo(paymentId, orderId, totalAmount, userId, "CREDIT_CARD");
        payment.setStatus("PREPARED");
        payment.setCartId(cartId);
        
        payments.put(paymentId, payment);
        orderToPaymentMapping.put(orderId, paymentId);

        eventPublisher.publishPaymentInitiated(paymentId, orderId, totalAmount, userId, "CREDIT_CARD");
        logger.info("Payment prepared for cart conversion - Order: {}, Payment ID: {}", orderId, paymentId);
    }

    public void preAuthorizePaymentForCheckout(String cartId, String userId, BigDecimal totalAmount) {
        logger.info("Pre-authorizing payment for checkout - Cart: {}, User: {}, Amount: {}", cartId, userId, totalAmount);
        // This could involve payment method validation, pre-authorization, etc.
        // For simulation purposes, we'll just log
        logger.info("Payment pre-authorization completed for cart: {}", cartId);
    }

    public void updatePaymentForOrderConfirmation(String orderId, String confirmationNumber) {
        logger.info("Updating payment for order confirmation - Order: {}, Confirmation: {}", orderId, confirmationNumber);
        
        String paymentId = orderToPaymentMapping.get(orderId);
        if (paymentId != null) {
            PaymentInfo payment = payments.get(paymentId);
            if (payment != null) {
                payment.setOrderConfirmationNumber(confirmationNumber);
                logger.info("Payment updated with order confirmation: {}", confirmationNumber);
            }
        }
    }

    public void cancelPaymentForOrder(String orderId, String reason) {
        logger.info("Cancelling payment for order: {} - Reason: {}", orderId, reason);
        
        String paymentId = orderToPaymentMapping.get(orderId);
        if (paymentId != null) {
            PaymentInfo payment = payments.get(paymentId);
            if (payment != null && !payment.getStatus().equals("COMPLETED")) {
                payment.setStatus("CANCELLED");
                eventPublisher.publishPaymentCancelled(paymentId, orderId, payment.getAmount(), payment.getUserId(), reason);
                logger.info("Payment cancelled for order: {} - Payment ID: {}", orderId, paymentId);
            }
        }
    }

    public void cancelPreparedPaymentForCart(String cartId, String reason) {
        logger.info("Cancelling prepared payment for cart: {} - Reason: {}", cartId, reason);
        
        // Find payment by cart ID
        Optional<PaymentInfo> cartPayment = payments.values().stream()
            .filter(p -> cartId.equals(p.getCartId()) && !"COMPLETED".equals(p.getStatus()))
            .findFirst();
            
        if (cartPayment.isPresent()) {
            PaymentInfo payment = cartPayment.get();
            payment.setStatus("CANCELLED");
            eventPublisher.publishPaymentCancelled(payment.getPaymentId(), payment.getOrderId(), 
                payment.getAmount(), payment.getUserId(), reason);
            logger.info("Prepared payment cancelled for cart: {} - Payment ID: {}", cartId, payment.getPaymentId());
        }
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private PaymentResult completePayment(PaymentInfo payment) {
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        payment.setStatus("COMPLETED");
        payment.setTransactionId(transactionId);
        payment.setCompletedAt(new Date());

        eventPublisher.publishPaymentCompleted(payment.getPaymentId(), payment.getOrderId(), 
            payment.getAmount(), payment.getUserId(), transactionId);

        logger.info("Payment completed successfully - ID: {}, Transaction: {}", 
            payment.getPaymentId(), transactionId);

        return new PaymentResult(true, "Payment completed successfully", 
            payment.getPaymentId(), transactionId, "COMPLETED");
    }

    private PaymentResult failPayment(PaymentInfo payment, String reason, String errorCode) {
        payment.setStatus("FAILED");
        payment.setFailureReason(reason);
        payment.setFailedAt(new Date());

        eventPublisher.publishPaymentFailed(payment.getPaymentId(), payment.getOrderId(), 
            payment.getAmount(), payment.getUserId(), reason, errorCode);

        logger.warn("Payment failed - ID: {}, Reason: {}", payment.getPaymentId(), reason);

        return new PaymentResult(false, reason, payment.getPaymentId(), null, "FAILED");
    }

    public PaymentInfo getPaymentByOrderId(String orderId) {
        String paymentId = orderToPaymentMapping.get(orderId);
        return paymentId != null ? payments.get(paymentId) : null;
    }

    public PaymentInfo getPaymentById(String paymentId) {
        return payments.get(paymentId);
    }

    // =====================================================
    // Data Classes
    // =====================================================

    public static class PaymentInfo {
        private String paymentId;
        private String orderId;
        private String cartId;
        private BigDecimal amount;
        private String userId;
        private String paymentMethod;
        private String status;
        private String gateway;
        private String gatewayTransactionId;
        private String transactionId;
        private String payerId;
        private String orderConfirmationNumber;
        private String failureReason;
        private Date createdAt;
        private Date completedAt;
        private Date failedAt;

        public PaymentInfo(String paymentId, String orderId, BigDecimal amount, String userId, String paymentMethod) {
            this.paymentId = paymentId;
            this.orderId = orderId;
            this.amount = amount;
            this.userId = userId;
            this.paymentMethod = paymentMethod;
            this.status = "CREATED";
            this.createdAt = new Date();
        }

        // Getters and setters
        public String getPaymentId() { return paymentId; }
        public String getOrderId() { return orderId; }
        public String getCartId() { return cartId; }
        public void setCartId(String cartId) { this.cartId = cartId; }
        public BigDecimal getAmount() { return amount; }
        public String getUserId() { return userId; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getGateway() { return gateway; }
        public void setGateway(String gateway) { this.gateway = gateway; }
        public String getGatewayTransactionId() { return gatewayTransactionId; }
        public void setGatewayTransactionId(String gatewayTransactionId) { this.gatewayTransactionId = gatewayTransactionId; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getPayerId() { return payerId; }
        public void setPayerId(String payerId) { this.payerId = payerId; }
        public String getOrderConfirmationNumber() { return orderConfirmationNumber; }
        public void setOrderConfirmationNumber(String orderConfirmationNumber) { this.orderConfirmationNumber = orderConfirmationNumber; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
        public Date getCreatedAt() { return createdAt; }
        public Date getCompletedAt() { return completedAt; }
        public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
        public Date getFailedAt() { return failedAt; }
        public void setFailedAt(Date failedAt) { this.failedAt = failedAt; }
    }

    public static class PaymentResult {
        private boolean success;
        private String message;
        private String paymentId;
        private String transactionId;
        private String status;

        public PaymentResult(boolean success, String message, String paymentId, String transactionId, String status) {
            this.success = success;
            this.message = message;
            this.paymentId = paymentId;
            this.transactionId = transactionId;
            this.status = status;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getPaymentId() { return paymentId; }
        public String getTransactionId() { return transactionId; }
        public String getStatus() { return status; }
    }
}