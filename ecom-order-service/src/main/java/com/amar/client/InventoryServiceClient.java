package com.amar.client;

import com.amar.dto.InventoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Service
public class InventoryServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceClient.class);

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    @Value("${services.inventory.url:http://inventory-service}")
    private String inventoryServiceUrl;

    @Autowired
    public InventoryServiceClient(WebClient.Builder webClientBuilder,
                                CircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = webClientBuilder.build();
        this.circuitBreaker = circuitBreakerFactory.create("inventory-service");
    }

    // =====================================================
    // Stock Availability Check
    // =====================================================

    public boolean checkStockAvailability(Long productId, Integer quantity) {
        logger.debug("Checking stock availability for product: {} quantity: {}", productId, quantity);
        
        return circuitBreaker.run(() -> {
            try {
                String url = inventoryServiceUrl + "/api/v1/inventory/check-availability/" + productId + "?quantity=" + quantity;
                
                Boolean isAvailable = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(Boolean.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                
                logger.debug("Stock availability for product {}: {}", productId, isAvailable);
                return isAvailable != null ? isAvailable : false;
                
            } catch (WebClientResponseException e) {
                logger.error("Error checking stock availability for product: {} - Status: {}, Response: {}", 
                           productId, e.getStatusCode(), e.getResponseBodyAsString());
                return false;
            } catch (Exception e) {
                logger.error("Unexpected error checking stock availability for product: {}", productId, e);
                return false;
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for stock availability check - product: {}", productId);
            return false;
        });
    }

    public Map<Long, Boolean> checkBulkStockAvailability(Map<Long, Integer> productQuantities) {
        logger.debug("Checking bulk stock availability for {} products", productQuantities.size());
        
        return circuitBreaker.run(() -> {
            try {
                Map<Long, Boolean> result = new HashMap<>();
                
                // Since there's no bulk endpoint, check each product individually
                for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
                    Long productId = entry.getKey();
                    Integer quantity = entry.getValue();
                    
                    String url = inventoryServiceUrl + "/api/v1/inventory/availability/" + productId + "?quantity=" + quantity;
                    
                    try {
                        Map<String, Object> response = webClient.get()
                                .uri(url)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .timeout(Duration.ofSeconds(5))
                                .block();
                        
                        if (response != null && response.get("data") != null) {
                            Map<String, Object> data = (Map<String, Object>) response.get("data");
                            Boolean available = (Boolean) data.getOrDefault("available", false);
                            result.put(productId, available);
                        } else {
                            result.put(productId, false);
                        }
                    } catch (Exception e) {
                        logger.error("Error checking availability for product {}: {}", productId, e.getMessage());
                        result.put(productId, false);
                    }
                }
                    
                logger.debug("Bulk stock availability check completed for {} products", result.size());
                return result;
                
            } catch (Exception e) {
                logger.error("Unexpected error checking bulk stock availability", e);
                return new HashMap<>();
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for bulk stock availability check");
            return new HashMap<>();
        });
    }

    // =====================================================
    // Stock Reservation
    // =====================================================

    public StockReservationResponse reserveStock(UUID orderId, List<StockReservationItem> items, String userId) {
        logger.debug("Reserving stock for order: {} with {} items", orderId, items.size());
        
        return circuitBreaker.run(() -> {
            try {
                String url = inventoryServiceUrl + "/api/v1/inventory/reserve";
                
                StockReservationRequest request = new StockReservationRequest();
                request.setOrderId(orderId);
                request.setUserId(userId);
                request.setItems(items);
                
                StockReservationResponse response = webClient.post()
                        .uri(url)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(StockReservationResponse.class)
                        .timeout(Duration.ofSeconds(10))
                        .block();
                
                if (response != null && response.isSuccess()) {
                    logger.info("Stock reserved successfully for order: {}", orderId);
                } else {
                    logger.warn("Stock reservation failed for order: {} - Response: {}", orderId, response);
                }
                
                return response != null ? response : new StockReservationResponse(false, "No response from inventory service");
                
            } catch (WebClientResponseException e) {
                logger.error("Error reserving stock for order: {} - Status: {}, Response: {}", 
                           orderId, e.getStatusCode(), e.getResponseBodyAsString());
                return new StockReservationResponse(false, "Inventory service error: " + e.getStatusCode());
            } catch (Exception e) {
                logger.error("Unexpected error reserving stock for order: {}", orderId, e);
                return new StockReservationResponse(false, "Unexpected error: " + e.getMessage());
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for stock reservation - order: {}", orderId);
            return new StockReservationResponse(false, "Service temporarily unavailable");
        });
    }

    // =====================================================
    // Stock Commitment (Convert reservation to permanent allocation)
    // =====================================================

    public boolean commitStock(UUID orderId) {
        logger.debug("Committing stock for order: {}", orderId);
        
        return circuitBreaker.run(() -> {
            try {
                String url = inventoryServiceUrl + "/api/v1/inventory/reserve/" + orderId + "/commit";
                
                Map<String, Object> response = webClient.post()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                
                Boolean committed = response != null && (Boolean) response.getOrDefault("success", false);
                
                boolean success = committed != null ? committed : false;
                logger.info("Stock commitment for order {}: {}", orderId, success ? "SUCCESS" : "FAILED");
                return success;
                
            } catch (WebClientResponseException e) {
                logger.error("Error committing stock for order: {} - Status: {}, Response: {}", 
                           orderId, e.getStatusCode(), e.getResponseBodyAsString());
                return false;
            } catch (Exception e) {
                logger.error("Unexpected error committing stock for order: {}", orderId, e);
                return false;
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for stock commitment - order: {}", orderId);
            return false;
        });
    }

    // =====================================================
    // Stock Release (Release reservation)
    // =====================================================

    public boolean releaseReservation(UUID orderId) {
        logger.debug("Releasing stock reservation for order: {}", orderId);
        
        return circuitBreaker.run(() -> {
            try {
                String url = inventoryServiceUrl + "/api/v1/inventory/reserve/" + orderId + "/release";
                
                Map<String, Object> response = webClient.post()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                
                Boolean released = response != null && (Boolean) response.getOrDefault("success", false);
                
                boolean success = released != null ? released : false;
                logger.info("Stock reservation release for order {}: {}", orderId, success ? "SUCCESS" : "FAILED");
                return success;
                
            } catch (WebClientResponseException e) {
                logger.error("Error releasing stock reservation for order: {} - Status: {}, Response: {}", 
                           orderId, e.getStatusCode(), e.getResponseBodyAsString());
                return false;
            } catch (Exception e) {
                logger.error("Unexpected error releasing stock reservation for order: {}", orderId, e);
                return false;
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for stock reservation release - order: {}", orderId);
            return false;
        });
    }

    // =====================================================
    // Get Inventory Information
    // =====================================================

    public Optional<InventoryDto> getInventoryByProductId(Long productId) {
        logger.debug("Getting inventory information for product: {}", productId);
        
        return circuitBreaker.run(() -> {
            try {
                String url = inventoryServiceUrl + "/api/v1/inventory/product/" + productId;
                
                InventoryDto inventory = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(InventoryDto.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                
                return Optional.ofNullable(inventory);
                
            } catch (WebClientResponseException.NotFound e) {
                logger.debug("Inventory not found for product: {}", productId);
                return Optional.empty();
            } catch (WebClientResponseException e) {
                logger.error("Error getting inventory for product: {} - Status: {}, Response: {}", 
                           productId, e.getStatusCode(), e.getResponseBodyAsString());
                return Optional.empty();
            } catch (Exception e) {
                logger.error("Unexpected error getting inventory for product: {}", productId, e);
                return Optional.empty();
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for inventory retrieval - product: {}", productId);
            return Optional.empty();
        });
    }

    // =====================================================
    // Data Classes
    // =====================================================

    public static class StockReservationRequest {
        private UUID orderId;
        private String userId;
        private List<StockReservationItem> items;

        // Getters and setters
        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public List<StockReservationItem> getItems() { return items; }
        public void setItems(List<StockReservationItem> items) { this.items = items; }
    }

    public static class StockReservationItem {
        private Long productId;
        private Integer quantity;

        public StockReservationItem() {}
        
        public StockReservationItem(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        // Getters and setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public static class StockReservationResponse {
        private boolean success;
        private String message;
        private UUID reservationId;
        private List<String> errors;

        public StockReservationResponse() {}
        
        public StockReservationResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public UUID getReservationId() { return reservationId; }
        public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }
}