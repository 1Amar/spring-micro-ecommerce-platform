package com.amar.cart.controller;

import com.amar.cart.service.CartService;
import com.amar.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@Validated
public class CartController {
    
    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    
    @Autowired
    private CartService cartService;
    
    @GetMapping
    public ResponseEntity<CartDto> getCart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @CookieValue(value = "cart-session", required = false) String sessionId,
            HttpServletResponse response) {
        
        log.debug("Getting cart - UserId: {}, SessionId: {}", userId, sessionId);
        
        // Generate session ID for anonymous users if not present
        if (userId == null && sessionId == null) {
            sessionId = generateSessionId();
            setSessionCookie(response, sessionId);
            log.debug("Generated new session ID for anonymous user: {}", sessionId);
        }
        
        CartDto cart = cartService.getCart(userId, sessionId);
        log.info("Retrieved cart - Items: {}, Total: {}", cart.getItemCount(), cart.getTotalAmount());
        
        return ResponseEntity.ok(cart);
    }
    
    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
            @RequestBody @Valid AddToCartRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @CookieValue(value = "cart-session", required = false) String sessionId,
            HttpServletResponse response) {
        
        log.info("Adding item to cart - UserId: {}, ProductId: {}, Quantity: {}", 
                userId, request.getProductId(), request.getQuantity());
        
        // Handle session for anonymous users
        if (userId == null && sessionId == null) {
            sessionId = generateSessionId();
            setSessionCookie(response, sessionId);
            log.debug("Generated new session ID for anonymous user: {}", sessionId);
        }
        
        CartDto cart = cartService.addItemToCart(userId, sessionId, request);
        log.info("Added item to cart successfully - Items: {}, Total: {}", 
                cart.getItemCount(), cart.getTotalAmount());
        
        return ResponseEntity.ok(cart);
    }
    
    @PutMapping("/items")
    public ResponseEntity<CartDto> updateItem(
            @RequestBody @Valid UpdateCartItemRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @CookieValue(value = "cart-session", required = false) String sessionId) {
        
        log.info("Updating item in cart - UserId: {}, ProductId: {}, Quantity: {}", 
                userId, request.getProductId(), request.getQuantity());
        
        if (userId == null && sessionId == null) {
            log.warn("No user ID or session ID provided for cart update");
            return ResponseEntity.badRequest().build();
        }
        
        CartDto cart = cartService.updateItemQuantity(userId, sessionId, request);
        log.info("Updated item in cart successfully - Items: {}, Total: {}", 
                cart.getItemCount(), cart.getTotalAmount());
        
        return ResponseEntity.ok(cart);
    }
    
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartDto> removeItem(
            @PathVariable Long productId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @CookieValue(value = "cart-session", required = false) String sessionId) {
        
        log.info("Removing item from cart - UserId: {}, ProductId: {}", userId, productId);
        
        if (userId == null && sessionId == null) {
            log.warn("No user ID or session ID provided for cart item removal");
            return ResponseEntity.badRequest().build();
        }
        
        CartDto cart = cartService.removeItemFromCart(userId, sessionId, productId);
        log.info("Removed item from cart successfully - Items: {}, Total: {}", 
                cart.getItemCount(), cart.getTotalAmount());
        
        return ResponseEntity.ok(cart);
    }
    
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @CookieValue(value = "cart-session", required = false) String sessionId) {
        
        log.info("Clearing cart - UserId: {}, SessionId: {}", userId, sessionId);
        
        if (userId == null && sessionId == null) {
            log.warn("No user ID or session ID provided for cart clearing");
            return ResponseEntity.badRequest().build();
        }
        
        cartService.clearCart(userId, sessionId);
        log.info("Cart cleared successfully");
        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getItemCount(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @CookieValue(value = "cart-session", required = false) String sessionId) {
        
        if (userId == null && sessionId == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
        
        int count = cartService.getCartItemCount(userId, sessionId);
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "cart-service",
            "timestamp", Instant.now().toString()
        ));
    }
    
    // Helper Methods
    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }
    
    private void setSessionCookie(HttpServletResponse response, String sessionId) {
        Cookie cookie = new Cookie("cart-session", sessionId);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(15 * 24 * 60 * 60); // 15 days
        response.addCookie(cookie);
        log.debug("Set cart session cookie with ID: {}", sessionId);
    }
}