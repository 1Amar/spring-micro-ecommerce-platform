package com.amar.client;

import com.amar.dto.CartDto;
import com.amar.dto.CartItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CartServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceClient.class);

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    @Value("${services.cart.url:http://cart-service}")
    private String cartServiceUrl;

    @Autowired
    public CartServiceClient(WebClient.Builder webClientBuilder,
                           CircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = webClientBuilder.build();
        this.circuitBreaker = circuitBreakerFactory.create("cart-service");
    }

    // =====================================================
    // Cart Retrieval
    // =====================================================

    public Optional<CartDto> getCart(String cartId) {
        logger.debug("Retrieving cart: {}", cartId);
        
        // Extract userId from cartId (format: cart:auth:userId or cart:session:sessionId)
        String userId = extractUserIdFromCartId(cartId);
        String sessionId = extractSessionIdFromCartId(cartId);
        
        return circuitBreaker.run(() -> {
            try {
                String url = cartServiceUrl + "/api/v1/cart";
                
                CartDto cart = webClient.get()
                        .uri(url)
                        .header("X-User-Id", userId)
                        .retrieve()
                        .bodyToMono(CartDto.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                
                if (cart != null) {
                    logger.debug("Retrieved cart {} with {} items", cartId, cart.getItems().size());
                } else {
                    logger.debug("Cart not found: {}", cartId);
                }
                
                return Optional.ofNullable(cart);
                
            } catch (WebClientResponseException.NotFound e) {
                logger.debug("Cart not found: {}", cartId);
                return Optional.empty();
            } catch (WebClientResponseException e) {
                logger.error("Error retrieving cart: {} - Status: {}, Response: {}", 
                           cartId, e.getStatusCode(), e.getResponseBodyAsString());
                return Optional.empty();
            } catch (Exception e) {
                logger.error("Unexpected error retrieving cart: {}", cartId, e);
                return Optional.empty();
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for cart retrieval - cart: {}", cartId);
            return Optional.empty();
        });
    }

    public Optional<CartDto> getUserCart(String userId) {
        logger.debug("Retrieving cart for user: {}", userId);
        
        return circuitBreaker.run(() -> {
            try {
                String url = cartServiceUrl + "/api/v1/cart/user/" + userId;
                
                CartDto cart = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(CartDto.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                
                if (cart != null) {
                    logger.debug("Retrieved cart for user {} with {} items", userId, cart.getItems().size());
                } else {
                    logger.debug("No cart found for user: {}", userId);
                }
                
                return Optional.ofNullable(cart);
                
            } catch (WebClientResponseException.NotFound e) {
                logger.debug("No cart found for user: {}", userId);
                return Optional.empty();
            } catch (WebClientResponseException e) {
                logger.error("Error retrieving cart for user: {} - Status: {}, Response: {}", 
                           userId, e.getStatusCode(), e.getResponseBodyAsString());
                return Optional.empty();
            } catch (Exception e) {
                logger.error("Unexpected error retrieving cart for user: {}", userId, e);
                return Optional.empty();
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for user cart retrieval - user: {}", userId);
            return Optional.empty();
        });
    }

    // =====================================================
    // Cart Validation
    // =====================================================

    public CartValidationResponse validateCartForCheckout(String cartId) {
        logger.debug("Validating cart for checkout: {}", cartId);
        
        // Extract userId from cartId
        String userId = extractUserIdFromCartId(cartId);
        
        return circuitBreaker.run(() -> {
            try {
                String url = cartServiceUrl + "/api/v1/cart/validate-inventory";
                
                // Use the actual cart service endpoint with proper headers
                Map<String, Object> response = webClient.post()
                        .uri(url)
                        .header("X-User-Id", userId)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(10))
                        .block();
                
                if (response != null) {
                    boolean isValid = (Boolean) response.getOrDefault("valid", false);
                    String message = (String) response.getOrDefault("message", "Unknown validation result");
                    
                    logger.debug("Cart validation for {}: Valid={}, Message={}", cartId, isValid, message);
                    return new CartValidationResponse(isValid, message);
                } else {
                    logger.warn("No validation response for cart: {}", cartId);
                    return new CartValidationResponse(false, "No response from cart service");
                }
                
            } catch (WebClientResponseException e) {
                logger.error("Error validating cart: {} - Status: {}, Response: {}", 
                           cartId, e.getStatusCode(), e.getResponseBodyAsString());
                return new CartValidationResponse(false, "Cart validation failed: " + e.getStatusCode());
            } catch (Exception e) {
                logger.error("Unexpected error validating cart: {}", cartId, e);
                return new CartValidationResponse(false, "Unexpected error: " + e.getMessage());
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for cart validation - cart: {}", cartId);
            return new CartValidationResponse(false, "Cart service temporarily unavailable");
        });
    }

    // =====================================================
    // Cart Actions After Order
    // =====================================================

    public boolean clearCart(String cartId) {
        logger.debug("Clearing cart after order: {}", cartId);
        
        // Extract userId from cartId
        String userId = extractUserIdFromCartId(cartId);
        
        return circuitBreaker.run(() -> {
            try {
                String url = cartServiceUrl + "/api/v1/cart";
                
                webClient.delete()
                        .uri(url)
                        .header("X-User-Id", userId)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                
                logger.info("Cart cleared successfully: {}", cartId);
                return true;
                
            } catch (WebClientResponseException e) {
                logger.error("Error clearing cart: {} - Status: {}, Response: {}", 
                           cartId, e.getStatusCode(), e.getResponseBodyAsString());
                return false;
            } catch (Exception e) {
                logger.error("Unexpected error clearing cart: {}", cartId, e);
                return false;
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for cart clearing - cart: {}", cartId);
            return false;
        });
    }

    public boolean markCartAsOrdered(String cartId, String orderId) {
        logger.debug("Marking cart as ordered: {} -> order: {}", cartId, orderId);
        
        // NOTE: Cart Service doesn't have a mark-as-ordered endpoint
        // This functionality might need to be implemented or handled differently
        // For now, we'll just clear the cart after successful order
        
        logger.info("Cart Service doesn't support marking as ordered. Cart will be cleared instead: {} -> order: {}", cartId, orderId);
        return clearCart(cartId);
    }

    // =====================================================
    // Cart Item Operations
    // =====================================================

    public boolean reserveCartItems(String cartId) {
        logger.debug("Reserving cart items: {}", cartId);
        
        // Extract userId from cartId
        String userId = extractUserIdFromCartId(cartId);
        
        return circuitBreaker.run(() -> {
            try {
                String url = cartServiceUrl + "/api/v1/cart/reserve-stock";
                
                Map<String, Object> response = webClient.post()
                        .uri(url)
                        .header("X-User-Id", userId)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                
                boolean reserved = (Boolean) response.getOrDefault("reserved", false);
                logger.info("Cart items reservation for {}: {}", cartId, reserved);
                return reserved;
                
            } catch (WebClientResponseException e) {
                logger.error("Error reserving cart items: {} - Status: {}, Response: {}", 
                           cartId, e.getStatusCode(), e.getResponseBodyAsString());
                return false;
            } catch (Exception e) {
                logger.error("Unexpected error reserving cart items: {}", cartId, e);
                return false;
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for cart item reservation - cart: {}", cartId);
            return false;
        });
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private String extractUserIdFromCartId(String cartId) {
        // Expected format: cart:auth:userId or cart:session:sessionId
        if (cartId != null && cartId.startsWith("cart:")) {
            String[] parts = cartId.split(":");
            if (parts.length >= 3) {
                if ("auth".equals(parts[1])) {
                    return parts[2]; // Return userId for authenticated carts
                }
            }
        }
        return null;
    }

    private String extractSessionIdFromCartId(String cartId) {
        // Expected format: cart:session:sessionId
        if (cartId != null && cartId.startsWith("cart:")) {
            String[] parts = cartId.split(":");
            if (parts.length >= 3) {
                if ("session".equals(parts[1])) {
                    return parts[2]; // Return sessionId for anonymous carts
                }
            }
        }
        return null;
    }

    // =====================================================
    // Cart Conversion Events
    // =====================================================

    public boolean publishCartConversionEvent(String userId, String sessionId, String orderId) {
        logger.debug("Publishing cart conversion event - userId: {}, sessionId: {}, orderId: {}", userId, sessionId, orderId);
        
        return circuitBreaker.run(() -> {
            try {
                // Create request body
                Map<String, Object> requestBody = Map.of(
                    "userId", userId != null ? userId : "",
                    "sessionId", sessionId != null ? sessionId : "",
                    "orderId", orderId
                );
                
                // Call cart service to publish conversion event
                webClient.post()
                    .uri(cartServiceUrl + "/api/v1/cart/conversion-event")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
                
                logger.info("Successfully published cart conversion event for order: {}", orderId);
                return true;
                
            } catch (WebClientResponseException ex) {
                logger.error("Failed to publish cart conversion event - HTTP {}: {}", 
                           ex.getStatusCode(), ex.getResponseBodyAsString());
                return false;
            } catch (Exception ex) {
                logger.error("Error publishing cart conversion event for order: {}", orderId, ex);
                return false;
            }
        }, throwable -> {
            logger.error("Circuit breaker triggered for cart conversion event publishing", throwable);
            return false;
        });
    }

    // =====================================================
    // Data Classes
    // =====================================================

    public static class CartValidationResponse {
        private boolean valid;
        private String message;
        private List<CartValidationIssue> issues;
        
        public CartValidationResponse() {}
        
        public CartValidationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public List<CartValidationIssue> getIssues() { return issues; }
        public void setIssues(List<CartValidationIssue> issues) { this.issues = issues; }
    }

    public static class CartValidationIssue {
        private String type;
        private String message;
        private Long productId;
        private String productName;
        
        public CartValidationIssue() {}
        
        public CartValidationIssue(String type, String message, Long productId, String productName) {
            this.type = type;
            this.message = message;
            this.productId = productId;
            this.productName = productName;
        }

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
    }

    public static class OrderMarkRequest {
        private String orderId;
        
        public OrderMarkRequest() {}
        
        public OrderMarkRequest(String orderId) {
            this.orderId = orderId;
        }

        // Getters and setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
    }
}