package com.amar.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/process")
    public Map<String, Object> processPayment(@RequestBody Map<String, Object> paymentRequest,
                                            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String orderId = (String) paymentRequest.get("orderId");
        Double amount = (Double) paymentRequest.get("amount");
        
        logger.info("Processing payment for order: {} amount: {} - Correlation-ID: {}", 
                   orderId, amount, correlationId);
        
        // Simulate payment processing
        try {
            Thread.sleep(100); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String paymentId = UUID.randomUUID().toString();
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "payment-service");
        response.put("paymentId", paymentId);
        response.put("orderId", orderId);
        response.put("amount", amount);
        response.put("status", "COMPLETED");
        response.put("correlationId", correlationId);
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("Payment processed successfully: {} for order: {} - Correlation-ID: {}", 
                   paymentId, orderId, correlationId);
        
        return response;
    }

    @GetMapping("/status/{paymentId}")
    public Map<String, Object> getPaymentStatus(@PathVariable String paymentId,
                                              @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        logger.info("Getting payment status for: {} - Correlation-ID: {}", paymentId, correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "payment-service");
        response.put("paymentId", paymentId);
        response.put("status", "COMPLETED");
        response.put("correlationId", correlationId);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    @PostMapping("/process-and-notify")
    public Map<String, Object> processPaymentAndNotify(@RequestBody Map<String, Object> paymentRequest,
                                                      @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String orderId = (String) paymentRequest.get("orderId");
        Double amount = (Double) paymentRequest.get("amount");
        String customerId = (String) paymentRequest.get("customerId");
        
        logger.info("Starting complex payment and notification flow for order: {} amount: {} customer: {} - Correlation-ID: {}", 
                   orderId, amount, customerId, correlationId);

        Map<String, Object> result = new HashMap<>();
        result.put("service", "payment-service");
        result.put("flow", "payment-and-notification");
        result.put("correlationId", correlationId);
        result.put("steps", new HashMap<>());
        Map<String, Object> steps = (Map<String, Object>) result.get("steps");

        // Step 1: Process Payment (local)
        logger.info("Step 1: Processing payment for order: {} - Correlation-ID: {}", orderId, correlationId);
        String paymentId = UUID.randomUUID().toString();
        
        // Simulate payment processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, Object> paymentStep = new HashMap<>();
        paymentStep.put("paymentId", paymentId);
        paymentStep.put("orderId", orderId);
        paymentStep.put("amount", amount);
        paymentStep.put("status", "COMPLETED");
        paymentStep.put("timestamp", System.currentTimeMillis());
        steps.put("1-payment-processing", paymentStep);
        
        logger.info("Step 1 completed: Payment processed - PaymentID: {} - Correlation-ID: {}", paymentId, correlationId);

        // Step 2: Send Notification
        logger.info("Step 2: Sending notification through notification-service - Correlation-ID: {}", correlationId);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Correlation-ID", correlationId);

            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("orderId", orderId);
            notificationRequest.put("message", String.format("Payment of $%.2f processed successfully for order %s (PaymentID: %s)", 
                                                            amount, orderId, paymentId));

            HttpEntity<Map<String, Object>> notificationEntity = new HttpEntity<>(notificationRequest, headers);
            ResponseEntity<Map> notificationResponse = restTemplate.postForEntity(
                "http://notification-service/api/v1/notifications/send", 
                notificationEntity, 
                Map.class);

            steps.put("2-notification-sending", notificationResponse.getBody());
            logger.info("Step 2 completed: Notification sent successfully - Correlation-ID: {}", correlationId);

        } catch (Exception e) {
            logger.error("Step 2 failed: Notification service error - Correlation-ID: {} - Error: {}", correlationId, e.getMessage());
            Map<String, Object> notificationError = new HashMap<>();
            notificationError.put("error", "Notification service unavailable: " + e.getMessage());
            notificationError.put("timestamp", System.currentTimeMillis());
            steps.put("2-notification-sending", notificationError);
        }

        result.put("status", "COMPLETED");
        result.put("totalSteps", 2);
        result.put("paymentId", paymentId);
        result.put("orderId", orderId);
        result.put("amount", amount);
        result.put("timestamp", System.currentTimeMillis());

        logger.info("Complex payment and notification flow completed - PaymentID: {} OrderID: {} - Correlation-ID: {}", 
                   paymentId, orderId, correlationId);

        return result;
    }

    @GetMapping("/health")
    public Map<String, Object> health(@RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        logger.info("Payment service health check - Correlation-ID: {}", correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "payment-service");
        response.put("status", "UP");
        response.put("correlationId", correlationId);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    @PostMapping("/simulate")
    public String simulatePayment() {
        String correlationId = org.slf4j.MDC.get("correlationId");
        logger.info("Payment service: Simulation endpoint called - correlationId: {}", correlationId);
        return "Payment service simulation completed - correlationId: " + correlationId;
    }
}