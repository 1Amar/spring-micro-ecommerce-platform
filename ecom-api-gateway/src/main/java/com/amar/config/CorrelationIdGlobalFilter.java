package com.amar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdGlobalFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        
        // Generate correlation ID if not present
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = "gw-" + UUID.randomUUID().toString();
            logger.debug("Generated new correlation ID: {}", correlationId);
        } else {
            logger.debug("Using existing correlation ID from request: {}", correlationId);
        }
        
        // Add correlation ID to downstream request headers
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();
        
        // Add correlation ID to response headers for client visibility
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        
        logger.debug("Propagating correlation ID {} to downstream service for path: {}", 
                    correlationId, request.getPath());
        
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -2; // Execute before LoggingFilter which has order -1
    }
}