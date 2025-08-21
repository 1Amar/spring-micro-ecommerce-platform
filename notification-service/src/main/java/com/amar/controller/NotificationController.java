package com.amar.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @PostMapping("/send")
    public Map<String, Object> sendNotification(@RequestBody Map<String, Object> notificationRequest,
                                              @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        String orderId = (String) notificationRequest.get("orderId");
        String message = (String) notificationRequest.get("message");
        
        logger.info("Sending notification for order: {} message: '{}' - Correlation-ID: {}", 
                   orderId, message, correlationId);
        
        // Simulate notification sending
        try {
            Thread.sleep(50); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String notificationId = UUID.randomUUID().toString();
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "notification-service");
        response.put("notificationId", notificationId);
        response.put("orderId", orderId);
        response.put("message", message);
        response.put("status", "SENT");
        response.put("channel", "EMAIL");
        response.put("correlationId", correlationId);
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("Notification sent successfully: {} for order: {} - Correlation-ID: {}", 
                   notificationId, orderId, correlationId);
        
        return response;
    }

    @GetMapping("/status/{notificationId}")
    public Map<String, Object> getNotificationStatus(@PathVariable String notificationId,
                                                   @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        logger.info("Getting notification status for: {} - Correlation-ID: {}", notificationId, correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "notification-service");
        response.put("notificationId", notificationId);
        response.put("status", "DELIVERED");
        response.put("correlationId", correlationId);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    @PostMapping("/simulate")
    public String simulateNotification() {
        String correlationId = org.slf4j.MDC.get("correlationId");
        logger.info("Notification service: Simulation endpoint called - correlationId: {}", correlationId);
        return "Notification service simulation completed - correlationId: " + correlationId;
    }
}