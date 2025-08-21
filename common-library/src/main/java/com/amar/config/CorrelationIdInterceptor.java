package com.amar.config;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, 
            byte[] body, 
            ClientHttpRequestExecution execution) throws IOException {
        
        // Get correlation ID from MDC (set by logging filter)
        String correlationId = MDC.get("correlationId");
        
        if (correlationId != null && !correlationId.isEmpty()) {
            request.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return execution.execute(request, body);
    }
}