package com.amar.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/order")
public class OrderSimulationController {

    private final RestTemplate restTemplate;

    public OrderSimulationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/simulate")
    public String simulateOrder() {
        System.out.println("Product service: Getting product details...");
        String orderResponse = restTemplate.postForObject("http://ecom-order-service/api/v1/order/simulate", null, String.class);
        return "Product details retrieved successfully. -> " + orderResponse;
    }
}
