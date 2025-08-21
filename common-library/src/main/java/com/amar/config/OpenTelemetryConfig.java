package com.amar.config;

import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry Configuration for Spring Boot Applications
 * 
 * This configuration relies on Spring Boot's auto-configuration for OpenTelemetry.
 * The actual configuration is done via application.yml properties:
 * 
 * otel:
 *   exporter:
 *     otlp:
 *       endpoint: http://localhost:4318
 *   resource:
 *     attributes:
 *       service.name: ${spring.application.name}
 *       service.version: 1.0.0
 *       deployment.environment: dev
 */
@Configuration
public class OpenTelemetryConfig {
    // OpenTelemetry auto-configuration is handled by Spring Boot
    // Configuration is provided via application.yml
}