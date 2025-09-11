package com.amar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time order status updates
 * Enables STOMP messaging with SockJS fallback support
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker for handling subscription destinations
     * and routing messages to connected clients
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple memory-based message broker for destinations prefixed with "/topic"
        // For production, consider using external brokers like RabbitMQ or ActiveMQ
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for messages that are bound to methods annotated with @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints for WebSocket connections
     * SockJS fallback provides compatibility for browsers that don't support WebSocket
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Main WebSocket endpoint
        registry.addEndpoint("/ws/orders")
                .setAllowedOriginPatterns("http://localhost:4200", "http://localhost:3000") // Angular frontend
                .withSockJS(); // Enable SockJS fallback
        
        // Alternative endpoint without SockJS for native WebSocket clients
        registry.addEndpoint("/ws/orders-native")
                .setAllowedOriginPatterns("http://localhost:4200", "http://localhost:3000");
    }
}