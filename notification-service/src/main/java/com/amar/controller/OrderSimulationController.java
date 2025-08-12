package com.amar.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/order")
public class OrderSimulationController {

    @PostMapping("/simulate")
    public String simulateOrder() {
        System.out.println("Notification service: Sending notification...");
        return "Notification sent successfully.";
    }
}
