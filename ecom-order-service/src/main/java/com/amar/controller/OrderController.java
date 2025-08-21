package com.amar.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> orderRequest,
                                         @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String productId = (String) orderRequest.get("productId");
        Integer quantity = (Integer) orderRequest.getOrDefault("quantity", 1);
        
        logger.info("Creating order for product: {} quantity: {} - Correlation-ID: {}", 
                   productId, quantity, correlationId);
        
        String orderId = UUID.randomUUID().toString();
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "ecom-order-service");
        response.put("orderId", orderId);
        response.put("productId", productId);
        response.put("quantity", quantity);
        response.put("correlationId", correlationId);
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            // Call product service
            logger.info("Calling product service for product: {} - Correlation-ID: {}", productId, correlationId);
            
            HttpHeaders headers = new HttpHeaders();
            if (correlationId != null) {
                headers.set("X-Correlation-ID", correlationId);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String productUrl = "http://product-service/api/v1/products/" + productId + "/with-inventory";
            Map<String, Object> productData = restTemplate.exchange(productUrl, HttpMethod.GET, entity, Map.class).getBody();
            
            response.put("productData", productData);
            
            // Call inventory reservation
            logger.info("Calling inventory reservation for product: {} quantity: {} - Correlation-ID: {}", 
                       productId, quantity, correlationId);
            
            String inventoryUrl = "http://inventory-service/api/v1/inventory/reserve/" + productId + "?quantity=" + quantity;
            Map<String, Object> reservation = restTemplate.exchange(inventoryUrl, HttpMethod.POST, entity, Map.class).getBody();
            
            response.put("inventoryReservation", reservation);
            
            // Call payment service
            logger.info("Calling payment service for order: {} - Correlation-ID: {}", orderId, correlationId);
            
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("orderId", orderId);
            paymentRequest.put("amount", 99.99 * quantity);
            
            HttpEntity<Map<String, Object>> paymentEntity = new HttpEntity<>(paymentRequest, headers);
            String paymentUrl = "http://payment-service/api/v1/payments/process";
            Map<String, Object> payment = restTemplate.exchange(paymentUrl, HttpMethod.POST, paymentEntity, Map.class).getBody();
            
            response.put("payment", payment);
            
            // Call notification service
            logger.info("Calling notification service for order: {} - Correlation-ID: {}", orderId, correlationId);
            
            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("orderId", orderId);
            notificationRequest.put("message", "Order created successfully");
            
            HttpEntity<Map<String, Object>> notificationEntity = new HttpEntity<>(notificationRequest, headers);
            String notificationUrl = "http://notification-service/api/v1/notifications/send";
            Map<String, Object> notification = restTemplate.exchange(notificationUrl, HttpMethod.POST, notificationEntity, Map.class).getBody();
            
            response.put("notification", notification);
            response.put("status", "COMPLETED");
            
            logger.info("Order created successfully: {} - Correlation-ID: {}", orderId, correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to create order for product: {} - Correlation-ID: {} - Error: {}", 
                        productId, correlationId, e.getMessage());
            response.put("error", e.getMessage());
            response.put("status", "FAILED");
        }
        
        return response;
    }
}