package com.amar.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceClient.class);

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    @Value("${payment.service.url:http://localhost:8087}")
    private String paymentServiceUrl;

    @Autowired
    public PaymentServiceClient(WebClient.Builder webClientBuilder,
                              CircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = webClientBuilder.build();
        this.circuitBreaker = circuitBreakerFactory.create("payment-service");
    }

    // =====================================================
    // Payment Processing
    // =====================================================

    public PaymentResponse processPayment(PaymentRequest request) {
        logger.debug("Processing payment for order: {} amount: {}", request.getOrderId(), request.getAmount());
        
        return circuitBreaker.run(() -> {
            try {
                String url = paymentServiceUrl + "/api/v1/payments/process";
                
                Map<String, Object> rawResponse = webClient.post()
                        .uri(url)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(15)) // Payment processing can take longer
                        .block();
                
                PaymentResponse response = null;
                if (rawResponse != null) {
                    String status = (String) rawResponse.get("status");
                    String paymentId = (String) rawResponse.get("paymentId");
                    String orderId = (String) rawResponse.get("orderId");
                    
                    boolean isSuccess = "COMPLETED".equals(status);
                    response = new PaymentResponse(isSuccess, paymentId, orderId, status);
                    
                    if (isSuccess) {
                        logger.info("Payment processed successfully for order: {} - Transaction ID: {}", 
                                  request.getOrderId(), paymentId);
                    } else {
                        logger.warn("Payment failed for order: {} - Status: {}", request.getOrderId(), status);
                    }
                } else {
                    logger.warn("Payment failed for order: {} - No response", request.getOrderId());
                }
                
                return response != null ? response : createFailedResponse("No response from payment service");
                
            } catch (WebClientResponseException e) {
                logger.error("Error processing payment for order: {} - Status: {}, Response: {}", 
                           request.getOrderId(), e.getStatusCode(), e.getResponseBodyAsString());
                return createFailedResponse("Payment service error: " + e.getStatusCode());
            } catch (Exception e) {
                logger.error("Unexpected error processing payment for order: {}", request.getOrderId(), e);
                return createFailedResponse("Unexpected error: " + e.getMessage());
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for payment processing - order: {}", request.getOrderId());
            return createFailedResponse("Payment service temporarily unavailable");
        });
    }

    // =====================================================
    // Payment Status Check
    // =====================================================

    public PaymentStatusResponse getPaymentStatus(UUID orderId, String transactionId) {
        logger.debug("Checking payment status for order: {} transaction: {}", orderId, transactionId);
        
        return circuitBreaker.run(() -> {
            try {
                String url = paymentServiceUrl + "/api/v1/payments/status/" + transactionId + "?orderId=" + orderId;
                
                PaymentStatusResponse response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(PaymentStatusResponse.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                
                logger.debug("Payment status for order {}: {}", orderId, response != null ? response.getStatus() : "No response");
                return response != null ? response : new PaymentStatusResponse("UNKNOWN", "No response from payment service");
                
            } catch (WebClientResponseException e) {
                logger.error("Error checking payment status for order: {} - Status: {}, Response: {}", 
                           orderId, e.getStatusCode(), e.getResponseBodyAsString());
                return new PaymentStatusResponse("UNKNOWN", "Payment service error: " + e.getStatusCode());
            } catch (Exception e) {
                logger.error("Unexpected error checking payment status for order: {}", orderId, e);
                return new PaymentStatusResponse("UNKNOWN", "Unexpected error: " + e.getMessage());
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for payment status check - order: {}", orderId);
            return new PaymentStatusResponse("UNKNOWN", "Payment service temporarily unavailable");
        });
    }

    // =====================================================
    // Payment Refund
    // =====================================================

    public RefundResponse refundPayment(UUID orderId, String transactionId, BigDecimal amount, String reason) {
        logger.debug("Processing refund for order: {} transaction: {} amount: {}", orderId, transactionId, amount);
        
        return circuitBreaker.run(() -> {
            try {
                String url = paymentServiceUrl + "/api/v1/payments/refund";
                
                RefundRequest request = new RefundRequest(orderId, transactionId, amount, reason);
                
                RefundResponse response = webClient.post()
                        .uri(url)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(RefundResponse.class)
                        .timeout(Duration.ofSeconds(10))
                        .block();
                
                if (response != null && response.isSuccess()) {
                    logger.info("Refund processed successfully for order: {} - Refund ID: {}", 
                              orderId, response.getRefundId());
                } else {
                    logger.warn("Refund failed for order: {} - Response: {}", orderId, response);
                }
                
                return response != null ? response : new RefundResponse(false, "No response from payment service");
                
            } catch (WebClientResponseException e) {
                logger.error("Error processing refund for order: {} - Status: {}, Response: {}", 
                           orderId, e.getStatusCode(), e.getResponseBodyAsString());
                return new RefundResponse(false, "Payment service error: " + e.getStatusCode());
            } catch (Exception e) {
                logger.error("Unexpected error processing refund for order: {}", orderId, e);
                return new RefundResponse(false, "Unexpected error: " + e.getMessage());
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for payment refund - order: {}", orderId);
            return new RefundResponse(false, "Payment service temporarily unavailable");
        });
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private PaymentResponse createFailedResponse(String message) {
        return new PaymentResponse(false, message, null, "FAILED");
    }

    // =====================================================
    // Data Classes
    // =====================================================

    public static class PaymentRequest {
        private UUID orderId;
        private BigDecimal amount;
        private String currency = "USD";
        private String paymentMethod;
        private String customerEmail;
        private String customerName;
        private String description;
        private String callbackUrl;
        
        // Billing information for payment processing
        private String billingFirstName;
        private String billingLastName;
        private String billingStreet;
        private String billingCity;
        private String billingState;
        private String billingPostalCode;
        private String billingCountry;
        
        // Payment method specific data
        private CreditCardInfo creditCardInfo;
        private PayPalInfo paypalInfo;
        
        public PaymentRequest() {}
        
        public PaymentRequest(UUID orderId, BigDecimal amount, String paymentMethod, String customerEmail) {
            this.orderId = orderId;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.customerEmail = customerEmail;
            this.description = "Order payment for order: " + orderId;
        }

        // Getters and setters
        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
        
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCallbackUrl() { return callbackUrl; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
        
        public String getBillingFirstName() { return billingFirstName; }
        public void setBillingFirstName(String billingFirstName) { this.billingFirstName = billingFirstName; }
        
        public String getBillingLastName() { return billingLastName; }
        public void setBillingLastName(String billingLastName) { this.billingLastName = billingLastName; }
        
        public String getBillingStreet() { return billingStreet; }
        public void setBillingStreet(String billingStreet) { this.billingStreet = billingStreet; }
        
        public String getBillingCity() { return billingCity; }
        public void setBillingCity(String billingCity) { this.billingCity = billingCity; }
        
        public String getBillingState() { return billingState; }
        public void setBillingState(String billingState) { this.billingState = billingState; }
        
        public String getBillingPostalCode() { return billingPostalCode; }
        public void setBillingPostalCode(String billingPostalCode) { this.billingPostalCode = billingPostalCode; }
        
        public String getBillingCountry() { return billingCountry; }
        public void setBillingCountry(String billingCountry) { this.billingCountry = billingCountry; }
        
        public CreditCardInfo getCreditCardInfo() { return creditCardInfo; }
        public void setCreditCardInfo(CreditCardInfo creditCardInfo) { this.creditCardInfo = creditCardInfo; }
        
        public PayPalInfo getPaypalInfo() { return paypalInfo; }
        public void setPaypalInfo(PayPalInfo paypalInfo) { this.paypalInfo = paypalInfo; }
    }

    public static class PaymentResponse {
        private boolean success;
        private String message;
        private String transactionId;
        private String status;
        private LocalDateTime processedAt;
        private String paymentMethod;
        private String providerResponse;
        
        public PaymentResponse() {}
        
        public PaymentResponse(boolean success, String message, String transactionId, String status) {
            this.success = success;
            this.message = message;
            this.transactionId = transactionId;
            this.status = status;
            this.processedAt = LocalDateTime.now();
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        
        public String getProviderResponse() { return providerResponse; }
        public void setProviderResponse(String providerResponse) { this.providerResponse = providerResponse; }
    }

    public static class PaymentStatusResponse {
        private String status;
        private String message;
        private LocalDateTime lastUpdated;
        
        public PaymentStatusResponse() {}
        
        public PaymentStatusResponse(String status, String message) {
            this.status = status;
            this.message = message;
            this.lastUpdated = LocalDateTime.now();
        }

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class RefundRequest {
        private UUID orderId;
        private String transactionId;
        private BigDecimal amount;
        private String reason;
        
        public RefundRequest() {}
        
        public RefundRequest(UUID orderId, String transactionId, BigDecimal amount, String reason) {
            this.orderId = orderId;
            this.transactionId = transactionId;
            this.amount = amount;
            this.reason = reason;
        }

        // Getters and setters
        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class RefundResponse {
        private boolean success;
        private String message;
        private String refundId;
        private LocalDateTime processedAt;
        
        public RefundResponse() {}
        
        public RefundResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.processedAt = LocalDateTime.now();
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getRefundId() { return refundId; }
        public void setRefundId(String refundId) { this.refundId = refundId; }
        
        public LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    }

    // Placeholder classes for future payment method implementations
    public static class CreditCardInfo {
        private String cardNumber;
        private String expiryMonth;
        private String expiryYear;
        private String cvv;
        private String cardholderName;
        
        // Getters and setters
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public String getExpiryMonth() { return expiryMonth; }
        public void setExpiryMonth(String expiryMonth) { this.expiryMonth = expiryMonth; }
        
        public String getExpiryYear() { return expiryYear; }
        public void setExpiryYear(String expiryYear) { this.expiryYear = expiryYear; }
        
        public String getCvv() { return cvv; }
        public void setCvv(String cvv) { this.cvv = cvv; }
        
        public String getCardholderName() { return cardholderName; }
        public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }
    }

    public static class PayPalInfo {
        private String paypalEmail;
        private String returnUrl;
        private String cancelUrl;
        
        // Getters and setters
        public String getPaypalEmail() { return paypalEmail; }
        public void setPaypalEmail(String paypalEmail) { this.paypalEmail = paypalEmail; }
        
        public String getReturnUrl() { return returnUrl; }
        public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
        
        public String getCancelUrl() { return cancelUrl; }
        public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    }
}