package com.amar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(1)
public class LoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        String correlationId = httpRequest.getHeader("X-Correlation-ID");
        
        if (correlationId == null) {
            correlationId = "svc-" + System.nanoTime();
        }
        
        // Set correlation ID in MDC for logging
        MDC.put("correlationId", correlationId);
        
        logger.info("Incoming HTTP Request: {} {} - Correlation-ID: {}", method, path, correlationId);
        
        try {
            chain.doFilter(request, response);
            
            int status = httpResponse.getStatus();
            logger.info("Outgoing HTTP Response: {} {} - Status: {} - Correlation-ID: {}", 
                       method, path, status, correlationId);
        } finally {
            // Clean up MDC
            MDC.remove("correlationId");
        }
    }
}