package com.amar.config;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ReactiveLoggingConfig {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_CONTEXT_KEY = "correlationId";

    @Bean
    public WebFilter correlationIdWebFilter() {
        return new CorrelationIdWebFilter();
    }

    public static class CorrelationIdWebFilter implements WebFilter {

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            String correlationId = exchange.getRequest()
                    .getHeaders()
                    .getFirst(CORRELATION_ID_HEADER);

            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }

            // Add correlation ID to response headers
            exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

            final String finalCorrelationId = correlationId;
            
            return chain.filter(exchange)
                    .contextWrite(Context.of(CORRELATION_ID_CONTEXT_KEY, finalCorrelationId))
                    .doOnEach(signal -> {
                        if (signal.hasValue() || signal.hasError()) {
                            String contextCorrelationId = signal.getContextView()
                                    .getOrDefault(CORRELATION_ID_CONTEXT_KEY, "unknown");
                            MDC.put(CORRELATION_ID_CONTEXT_KEY, contextCorrelationId);
                        }
                    })
                    .doFinally(signalType -> MDC.clear());
        }
    }
}