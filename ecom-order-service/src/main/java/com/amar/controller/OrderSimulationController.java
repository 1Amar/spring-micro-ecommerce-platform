package com.amar.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulation")
public class OrderSimulationController {

    private static final Logger logger = LoggerFactory.getLogger(OrderSimulationController.class);
    private final RestTemplate restTemplate;

    public OrderSimulationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulateOrder() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "order-" + UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        
        logger.info("Order service: Starting order simulation - correlationId: {}", correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "order-service");
        response.put("correlationId", correlationId);
        response.put("status", "SUCCESS");
        
        try {
            // Call inventory service to check stock
            logger.info("Order service: Checking inventory - correlationId: {}", correlationId);
            String inventoryResponse = restTemplate.postForObject("http://inventory-service/api/v1/inventory/simulate", null, String.class);
            response.put("inventoryCheck", inventoryResponse);
            
            // Call payment service
            logger.info("Order service: Processing payment - correlationId: {}", correlationId);
            String paymentResponse = restTemplate.postForObject("http://payment-service/api/v1/payments/simulate", null, String.class);
            response.put("paymentProcessing", paymentResponse);
            
            response.put("message", "Order simulation completed successfully");
            logger.info("Order service: Order simulation completed - correlationId: {}", correlationId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Order service: Error during simulation - correlationId: {}, error: {}", correlationId, e.getMessage());
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/complete-order-flow")
    public ResponseEntity<Map<String, Object>> completeOrderFlow() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "complete-flow-" + UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        
        logger.info("Order service: Starting complete order flow simulation - correlationId: {}", correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "order-service");
        response.put("operation", "complete-order-flow");
        response.put("correlationId", correlationId);
        response.put("status", "SUCCESS");
        
        try {
            // Step 1: Check inventory
            logger.info("Order service: Step 1 - Checking inventory - correlationId: {}", correlationId);
            String inventoryResponse = restTemplate.postForObject("http://inventory-service/api/v1/inventory/simulate", null, String.class);
            response.put("step1_inventory", inventoryResponse);
            
            // Step 2: Process payment
            logger.info("Order service: Step 2 - Processing payment - correlationId: {}", correlationId);
            String paymentResponse = restTemplate.postForObject("http://payment-service/api/v1/payments/simulate", null, String.class);
            response.put("step2_payment", paymentResponse);
            
            // Step 3: Send notification  
            logger.info("Order service: Step 3 - Sending notification - correlationId: {}", correlationId);
            String notificationResponse = restTemplate.postForObject("http://notification-service/api/v1/notifications/simulate", null, String.class);
            response.put("step3_notification", notificationResponse);
            
            response.put("message", "Complete order flow simulation completed successfully");
            response.put("totalSteps", 3);
            logger.info("Order service: Complete order flow simulation finished - correlationId: {}", correlationId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Order service: Error during complete order flow simulation - correlationId: {}, error: {}", correlationId, e.getMessage());
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
