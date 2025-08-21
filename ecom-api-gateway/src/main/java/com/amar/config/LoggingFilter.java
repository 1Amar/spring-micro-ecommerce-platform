package com.amar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-1)
public class LoggingFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        final String path = exchange.getRequest().getPath().pathWithinApplication().value();
        final String method = exchange.getRequest().getMethod().toString();
        String correlationIdHeader = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        
        final String correlationId = (correlationIdHeader != null) ? 
            correlationIdHeader : "gw-" + System.nanoTime();
        
        // Set correlation ID in MDC for logging
        MDC.put("correlationId", correlationId);
        
        logger.info("Incoming HTTP Request: {} {} - Correlation-ID: {}", method, path, correlationId);
        
        return chain.filter(exchange)
                .doOnSuccess(result -> {
                    int status = exchange.getResponse().getStatusCode().value();
                    logger.info("Outgoing HTTP Response: {} {} - Status: {} - Correlation-ID: {}", 
                               method, path, status, correlationId);
                })
                .doFinally(signalType -> {
                    // Clean up MDC
                    MDC.remove("correlationId");
                });
    }
}