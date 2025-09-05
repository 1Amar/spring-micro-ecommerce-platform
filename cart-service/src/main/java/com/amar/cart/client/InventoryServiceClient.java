package com.amar.cart.client;

import com.amar.dto.InventoryDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceClient.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${app.services.inventory.timeout:5000}")
    private int timeoutMs;

    @Autowired
    public InventoryServiceClient(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Check if a specific quantity is available for a product
     */
    public boolean checkAvailability(Long productId, Integer quantity) {
        logger.debug("Checking availability for product ID: {} quantity: {}", productId, quantity);
        
        try {
            String response = webClient.get()
                .uri("http://localhost:8084/api/v1/inventory/availability/{productId}?quantity={quantity}", 
                     productId, quantity)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .block();

            return parseAvailabilityResponse(response);
            
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.warn("Inventory not found for product ID: {}", productId);
                return false;
            }
            logger.error("Error checking availability for product ID: {} quantity: {}", productId, quantity, ex);
            return false; // Default to unavailable on error
        } catch (Exception ex) {
            logger.error("Unexpected error checking availability for product ID: {} quantity: {}", productId, quantity, ex);
            return false; // Default to unavailable on error
        }
    }

    /**
     * Reserve stock for cart items
     */
    public boolean reserveStock(UUID orderId, List<StockReservationItem> items, String userId) {
        logger.debug("Reserving stock for order ID: {} items: {} user: {}", orderId, items.size(), userId);
        
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("orderId", orderId);
            request.put("items", items);
            request.put("userId", userId);
            
            String response = webClient.post()
                .uri("http://localhost:8084/api/v1/inventory/reserve")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs * 2)) // Longer timeout for reservations
                .block();

            return parseReservationResponse(response);
            
        } catch (Exception ex) {
            logger.error("Error reserving stock for order ID: {}", orderId, ex);
            return false;
        }
    }

    /**
     * Release stock reservation
     */
    public boolean releaseReservation(UUID orderId) {
        logger.debug("Releasing reservation for order ID: {}", orderId);
        
        try {
            String response = webClient.post()
                .uri("http://localhost:8084/api/v1/inventory/reserve/{orderId}/release", orderId)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .block();

            return parseReleaseResponse(response);
            
        } catch (Exception ex) {
            logger.error("Error releasing reservation for order ID: {}", orderId, ex);
            return false;
        }
    }

    /**
     * Get current stock level for a product
     */
    public Optional<InventoryDto> getInventoryByProductId(Long productId) {
        logger.debug("Fetching inventory for product ID: {}", productId);
        
        try {
            String response = webClient.get()
                .uri("http://localhost:8084/api/v1/inventory/product/{productId}", productId)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .block();

            return parseInventoryResponse(response);
            
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.warn("Inventory not found for product ID: {}", productId);
                return Optional.empty();
            }
            logger.error("Error fetching inventory for product ID: {}", productId, ex);
            return Optional.empty();
        } catch (Exception ex) {
            logger.error("Unexpected error fetching inventory for product ID: {}", productId, ex);
            return Optional.empty();
        }
    }

    // Helper parsing methods
    private boolean parseAvailabilityResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            if (!root.has("success") || !root.get("success").asBoolean()) {
                return false;
            }
            
            JsonNode dataNode = root.get("data");
            return dataNode != null && dataNode.has("available") && dataNode.get("available").asBoolean();
            
        } catch (Exception ex) {
            logger.error("Error parsing availability response: {}", response, ex);
            return false;
        }
    }

    private boolean parseReservationResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.has("success") && root.get("success").asBoolean();
        } catch (Exception ex) {
            logger.error("Error parsing reservation response: {}", response, ex);
            return false;
        }
    }

    private boolean parseReleaseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.has("success") && root.get("success").asBoolean();
        } catch (Exception ex) {
            logger.error("Error parsing release response: {}", response, ex);
            return false;
        }
    }

    private Optional<InventoryDto> parseInventoryResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            if (!root.has("success") || !root.get("success").asBoolean()) {
                return Optional.empty();
            }
            
            JsonNode dataNode = root.get("data");
            if (dataNode == null || dataNode.isNull()) {
                return Optional.empty();
            }
            
            InventoryDto inventory = objectMapper.treeToValue(dataNode, InventoryDto.class);
            return Optional.of(inventory);
            
        } catch (Exception ex) {
            logger.error("Error parsing inventory response: {}", response, ex);
            return Optional.empty();
        }
    }

    // Inner class for stock reservation items
    public static class StockReservationItem {
        private Long productId;
        private Integer quantity;
        
        public StockReservationItem() {}
        
        public StockReservationItem(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
        
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}