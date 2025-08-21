package com.amar.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
@ConditionalOnClass(Filter.class)
public class TracingConfiguration implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TracingConfiguration.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Autowired(required = false)
    private Tracer tracer;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest) || 
            !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        String correlationId = getOrCreateCorrelationId(httpRequest);
        
        // Set correlation ID in response header
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        // Set correlation ID in MDC for logging
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

        try {
            if (tracer != null) {
                // Create a span for the HTTP request
                Span span = tracer.spanBuilder(httpRequest.getMethod() + " " + httpRequest.getRequestURI())
                        .setSpanKind(io.opentelemetry.api.trace.SpanKind.SERVER)
                        .setAttribute("http.method", httpRequest.getMethod())
                        .setAttribute("http.url", httpRequest.getRequestURL().toString())
                        .setAttribute("http.route", httpRequest.getRequestURI())
                        .setAttribute("correlation.id", correlationId)
                        .startSpan();

                try (Scope scope = span.makeCurrent()) {
                    // Add correlation ID as span attribute
                    span.setAttribute("correlation.id", correlationId);
                    
                    chain.doFilter(request, response);
                    
                    // Set response status
                    span.setAttribute("http.status_code", httpResponse.getStatus());
                    
                    if (httpResponse.getStatus() >= 400) {
                        span.recordException(new RuntimeException("HTTP Error: " + httpResponse.getStatus()));
                    }
                } finally {
                    span.end();
                }
            } else {
                chain.doFilter(request, response);
            }
        } finally {
            // Clean up MDC
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private String getOrCreateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
}