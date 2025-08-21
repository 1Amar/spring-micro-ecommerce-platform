package com.amar.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/fallback")
    public Mono<Map<String, String>> fallback() {
        logger.warn("Circuit breaker fallback triggered");
        return Mono.just(Map.of(
            "message", "Service is temporarily unavailable. Please try again later.",
            "status", "SERVICE_UNAVAILABLE",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
}