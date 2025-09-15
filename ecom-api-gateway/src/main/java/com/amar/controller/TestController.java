package com.amar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TestController {

    @GetMapping("/public/health")
    public ResponseEntity<Map<String, Object>> publicHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "API Gateway is running");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/auth/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        
        if (jwt != null) {
            response.put("authenticated", true);
            response.put("username", jwt.getClaimAsString("preferred_username"));
            response.put("email", jwt.getClaimAsString("email"));
            response.put("roles", jwt.getClaimAsMap("realm_access"));
            response.put("subject", jwt.getSubject());
        } else {
            response.put("authenticated", false);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/order/simulate")
    public ResponseEntity<Map<String, Object>> simulateOrder(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Order simulation endpoint");
        response.put("authenticated", jwt != null);
        
        if (jwt != null) {
            response.put("user", jwt.getClaimAsString("preferred_username"));
        }
        
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/status")
    public ResponseEntity<Map<String, Object>> adminStatus(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin endpoint accessed");
        response.put("user", jwt.getClaimAsString("preferred_username"));
        response.put("roles", jwt.getClaimAsMap("realm_access"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/catalog/categories")
    public ResponseEntity<Object> getCategories() {
        // Mock categories data for development
        Object[] categories = {
            Map.of("id", 1, "name", "Electronics", "description", "Electronic devices and gadgets"),
            Map.of("id", 2, "name", "Clothing", "description", "Fashion and apparel"),
            Map.of("id", 3, "name", "Books", "description", "Books and literature"),
            Map.of("id", 4, "name", "Home & Garden", "description", "Home improvement and gardening"),
            Map.of("id", 5, "name", "Sports", "description", "Sports and outdoor equipment")
        };
        
        Map<String, Object> response = new HashMap<>();
        response.put("categories", categories);
        response.put("total", categories.length);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/manager/reports")
    public ResponseEntity<Map<String, Object>> getManagerReports(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Manager reports endpoint accessed");
        response.put("user", jwt.getClaimAsString("preferred_username"));
        response.put("roles", jwt.getClaimAsMap("realm_access"));
        response.put("data", Map.of(
            "totalSales", 150000,
            "totalOrders", 1250,
            "totalCustomers", 890
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/users")
    public ResponseEntity<Map<String, Object>> getAdminUsers(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin users endpoint accessed");
        response.put("user", jwt.getClaimAsString("preferred_username"));
        response.put("roles", jwt.getClaimAsMap("realm_access"));
        response.put("users", Arrays.asList(
            Map.of("id", 1, "username", "admin", "role", "admin"),
            Map.of("id", 2, "username", "manager", "role", "manager"),
            Map.of("id", 3, "username", "user", "role", "user")
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test/roles")
    public ResponseEntity<Map<String, Object>> testRoles(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Role test endpoint - requires authentication only");
        response.put("user", jwt.getClaimAsString("preferred_username"));
        response.put("email", jwt.getClaimAsString("email"));
        response.put("roles", jwt.getClaimAsMap("realm_access"));
        response.put("allClaims", jwt.getClaims());
        return ResponseEntity.ok(response);
    }
}