package com.amar.config;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-1)
public class RateLimitErrorHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            
            // Handle rate limit exceeded (429 Too Many Requests)
            if (rse.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return handleRateLimitExceeded(exchange, response);
            }
        }
        
        // Let other error handlers handle non-rate-limit errors
        return Mono.error(ex);
    }
    
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().add("Retry-After", "60"); // Suggest retry after 60 seconds
        
        String body = """
            {
                "error": "Rate limit exceeded",
                "message": "Too many requests. Please try again later.",
                "status": 429,
                "timestamp": "%s",
                "path": "%s"
            }
            """.formatted(
                java.time.Instant.now().toString(),
                exchange.getRequest().getPath().value()
            );
        
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}