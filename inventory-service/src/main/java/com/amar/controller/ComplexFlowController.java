package com.amar.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/complex")
public class ComplexFlowController {

    private static final Logger logger = LoggerFactory.getLogger(ComplexFlowController.class);

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/order-simulation")
    public Map<String, Object> simulateComplexOrder(@RequestBody Map<String, Object> orderRequest,
                                                   @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String productId = (String) orderRequest.get("productId");
        Integer quantity = (Integer) orderRequest.get("quantity");
        String customerId = (String) orderRequest.get("customerId");
        Double amount = (Double) orderRequest.get("amount");
        
        logger.info("Starting complex order simulation for product: {} quantity: {} customer: {} - Correlation-ID: {}", 
                   productId, quantity, customerId, correlationId);

        Map<String, Object> result = new HashMap<>();
        result.put("service", "inventory-service");
        result.put("flow", "complex-order-simulation");
        result.put("correlationId", correlationId);
        result.put("steps", new HashMap<>());
        Map<String, Object> steps = (Map<String, Object>) result.get("steps");

        // Step 1: Check inventory (local)
        logger.info("Step 1: Checking inventory for product: {} - Correlation-ID: {}", productId, correlationId);
        Map<String, Object> inventoryCheck = new HashMap<>();
        inventoryCheck.put("productId", productId);
        inventoryCheck.put("available", true);
        inventoryCheck.put("quantity", 150);
        inventoryCheck.put("timestamp", System.currentTimeMillis());
        steps.put("1-inventory-check", inventoryCheck);

        // Step 2: Call Payment Service
        logger.info("Step 2: Processing payment through payment-service - Correlation-ID: {}", correlationId);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Correlation-ID", correlationId);

            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("orderId", "ORDER-" + System.currentTimeMillis());
            paymentRequest.put("amount", amount);

            HttpEntity<Map<String, Object>> paymentEntity = new HttpEntity<>(paymentRequest, headers);
            ResponseEntity<Map> paymentResponse = restTemplate.postForEntity(
                "http://payment-service/api/v1/payments/process", 
                paymentEntity, 
                Map.class);

            steps.put("2-payment-processing", paymentResponse.getBody());
            logger.info("Step 2 completed: Payment processed successfully - Correlation-ID: {}", correlationId);

        } catch (Exception e) {
            logger.error("Step 2 failed: Payment processing error - Correlation-ID: {} - Error: {}", correlationId, e.getMessage());
            Map<String, Object> paymentError = new HashMap<>();
            paymentError.put("error", "Payment service unavailable: " + e.getMessage());
            paymentError.put("timestamp", System.currentTimeMillis());
            steps.put("2-payment-processing", paymentError);
        }

        // Step 3: Call Notification Service
        logger.info("Step 3: Sending notification through notification-service - Correlation-ID: {}", correlationId);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Correlation-ID", correlationId);

            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("orderId", "ORDER-" + System.currentTimeMillis());
            notificationRequest.put("message", "Order processed successfully for customer " + customerId);

            HttpEntity<Map<String, Object>> notificationEntity = new HttpEntity<>(notificationRequest, headers);
            ResponseEntity<Map> notificationResponse = restTemplate.postForEntity(
                "http://notification-service/api/v1/notifications/send", 
                notificationEntity, 
                Map.class);

            steps.put("3-notification-sending", notificationResponse.getBody());
            logger.info("Step 3 completed: Notification sent successfully - Correlation-ID: {}", correlationId);

        } catch (Exception e) {
            logger.error("Step 3 failed: Notification service error - Correlation-ID: {} - Error: {}", correlationId, e.getMessage());
            Map<String, Object> notificationError = new HashMap<>();
            notificationError.put("error", "Notification service unavailable: " + e.getMessage());
            notificationError.put("timestamp", System.currentTimeMillis());
            steps.put("3-notification-sending", notificationError);
        }

        result.put("status", "COMPLETED");
        result.put("totalSteps", 3);
        result.put("timestamp", System.currentTimeMillis());

        logger.info("Complex order simulation completed for product: {} - Correlation-ID: {} - Status: COMPLETED", 
                   productId, correlationId);

        return result;
    }
}