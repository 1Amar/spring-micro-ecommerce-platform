package com.amar.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.amar.service.OrderWebSocketService;

/**
 * WebSocket controller for handling real-time order status updates
 * Supports both individual order subscriptions and user-wide order updates
 */
@Controller
public class OrderWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(OrderWebSocketController.class);

    private final OrderWebSocketService orderWebSocketService;

    @Autowired
    public OrderWebSocketController(OrderWebSocketService orderWebSocketService) {
        this.orderWebSocketService = orderWebSocketService;
    }

    /**
     * Handle subscription to individual order updates
     * Client subscribes to: /topic/orders/{orderId}
     */
    @SubscribeMapping("/orders/{orderId}")
    public void subscribeToOrder(@DestinationVariable String orderId) {
        logger.info("Client subscribed to order updates for orderId: {}", orderId);
        
        try {
            UUID orderUUID = UUID.fromString(orderId);
            orderWebSocketService.handleOrderSubscription(orderUUID);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid order ID format: {}", orderId, e);
        }
    }

    /**
     * Handle subscription to all orders for a specific user
     * Client subscribes to: /topic/orders/user/{userId}
     */
    @SubscribeMapping("/orders/user/{userId}")
    public void subscribeToUserOrders(@DestinationVariable String userId) {
        logger.info("Client subscribed to all order updates for userId: {}", userId);
        orderWebSocketService.handleUserOrdersSubscription(userId);
    }

    /**
     * Handle subscription to order progress updates
     * Client subscribes to: /topic/orders/{orderId}/progress
     */
    @SubscribeMapping("/orders/{orderId}/progress")
    public void subscribeToOrderProgress(@DestinationVariable String orderId) {
        logger.info("Client subscribed to order progress for orderId: {}", orderId);
        
        try {
            UUID orderUUID = UUID.fromString(orderId);
            orderWebSocketService.handleOrderProgressSubscription(orderUUID);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid order ID format for progress subscription: {}", orderId, e);
        }
    }

    /**
     * Handle ping messages from clients to keep connections alive
     * Client sends to: /app/orders/ping
     */
    @MessageMapping("/orders/ping")
    @SendTo("/topic/orders/pong")
    public String handlePing(@Payload String message) {
        logger.debug("Received ping: {}", message);
        return "pong-" + System.currentTimeMillis();
    }

    /**
     * Handle client requests for order status refresh
     * Client sends to: /app/orders/{orderId}/refresh
     */
    @MessageMapping("/orders/{orderId}/refresh")
    public void refreshOrderStatus(@DestinationVariable String orderId) {
        logger.info("Client requested status refresh for orderId: {}", orderId);
        
        try {
            UUID orderUUID = UUID.fromString(orderId);
            orderWebSocketService.sendOrderStatusRefresh(orderUUID);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid order ID format for refresh: {}", orderId, e);
        }
    }

    /**
     * Handle client requests for user orders refresh
     * Client sends to: /app/orders/user/{userId}/refresh
     */
    @MessageMapping("/orders/user/{userId}/refresh")
    public void refreshUserOrders(@DestinationVariable String userId) {
        logger.info("Client requested orders refresh for userId: {}", userId);
        orderWebSocketService.sendUserOrdersRefresh(userId);
    }
}