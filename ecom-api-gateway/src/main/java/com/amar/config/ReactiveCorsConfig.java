package com.amar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ReactiveCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Proper CORS configuration for production
        configuration.setAllowCredentials(true);
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:4200",      // Angular frontend
            "http://localhost:8080",      // Keycloak
            "http://127.0.0.1:4200",
            "http://127.0.0.1:8080"
        ));
        
        // Allow necessary headers
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Cache-Control",
            "X-Correlation-ID",
            "X-User-Id",
            "X-User-Email",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Headers",
            "Origin"
        ));
        
        // Allow necessary HTTP methods
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        // Expose headers that frontend can read
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "X-Correlation-ID",
            "Content-Disposition"
        ));
        
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}