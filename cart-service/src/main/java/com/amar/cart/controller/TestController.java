package com.amar.cart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private WebClient productServiceClient;
    
    @Autowired
    private WebClient inventoryServiceClient;
    
    @GetMapping("/product/{id}")
    public Mono<String> testProduct(@PathVariable Long id) {
        return productServiceClient
            .get()
            .uri("/api/v1/products/catalog/{id}", id)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(30))
            .onErrorReturn("ERROR: " + id);
    }
    
    @GetMapping("/inventory/{id}")
    public Mono<String> testInventory(@PathVariable Long id) {
        return inventoryServiceClient
            .get()
            .uri("/api/v1/inventory/availability/{id}?quantity=1", id)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(30))
            .onErrorReturn("ERROR: " + id);
    }
}