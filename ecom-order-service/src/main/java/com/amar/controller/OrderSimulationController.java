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
        System.out.println("Order service: Placing order...");
        String paymentResponse = restTemplate.postForObject("http://payment-service/api/v1/order/simulate", null, String.class);
        return "Order placed successfully. -> " + paymentResponse;
    }
}
