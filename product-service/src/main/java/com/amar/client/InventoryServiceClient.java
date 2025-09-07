package com.amar.client;

import com.amar.dto.InventoryDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventoryServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceClient.class);
    
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final ObjectMapper objectMapper;
    
    @Value("${app.services.inventory.timeout:5000}")
    private int timeoutMs;

    @Autowired
    public InventoryServiceClient(WebClient.Builder webClientBuilder, 
                                 CircuitBreakerFactory circuitBreakerFactory,
                                 ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
            .baseUrl("http://inventory-service")
            .build();
        this.circuitBreaker = circuitBreakerFactory.create("inventory-service");
        this.objectMapper = objectMapper;
    }

    /**
     * Get inventory information for a single product
     */
    public Optional<InventoryDto> getInventoryByProductId(Long productId) {
        logger.debug("Fetching inventory for product ID: {}", productId);
        
        return circuitBreaker.run(() -> {
            try {
                String response = webClient.get()
                    .uri("/api/v1/inventory/product/{productId}", productId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

                return parseInventoryResponse(response);
                
            } catch (WebClientResponseException ex) {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    logger.warn("Inventory not found for product ID: {}", productId);
                    return Optional.<InventoryDto>empty();
                }
                logger.error("Error fetching inventory for product ID: {}", productId, ex);
                throw ex;
            } catch (Exception ex) {
                logger.error("Unexpected error fetching inventory for product ID: {}", productId, ex);
                throw new RuntimeException("Failed to fetch inventory", ex);
            }
        }, throwable -> {
            logger.error("Circuit breaker fallback triggered for product ID: {}", productId, throwable);
            return Optional.<InventoryDto>empty();
        });
    }

    /**
     * Get inventory information for multiple products (bulk operation)
     */
    public Map<Long, InventoryDto> getInventoryForProducts(List<Long> productIds) {
        logger.debug("Fetching inventory for {} products", productIds.size());
        
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        return circuitBreaker.run(() -> {
            try {
                String response = webClient.post()
                    .uri("/api/v1/inventory/products")
                    .bodyValue(productIds)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs * 2)) // Longer timeout for bulk operations
                    .block();

                return parseInventoryBulkResponse(response);
                
            } catch (Exception ex) {
                logger.error("Error fetching inventory for multiple products", ex);
                throw new RuntimeException("Failed to fetch bulk inventory", ex);
            }
        }, throwable -> {
            logger.error("Circuit breaker fallback triggered for bulk inventory fetch", throwable);
            return Map.<Long, InventoryDto>of();
        });
    }

    /**
     * Create inventory record for a new product
     */
    public boolean createInventory(Long productId, Integer quantity, Integer reorderLevel) {
        logger.debug("Creating inventory for product ID: {} with quantity: {}", productId, quantity);
        
        return circuitBreaker.run(() -> {
            try {
                Map<String, Object> inventoryData = Map.of(
                    "productId", productId,
                    "quantity", quantity,
                    "reservedQuantity", 0,
                    "availableQuantity", quantity,
                    "reorderLevel", reorderLevel != null ? reorderLevel : 10,
                    "maxStockLevel", Math.max(quantity * 2, 100),
                    "isLowStock", false
                );
                
                String response = webClient.post()
                    .uri("/api/v1/inventory")
                    .bodyValue(inventoryData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

                return parseSuccessResponse(response);
                
            } catch (Exception ex) {
                logger.error("Error creating inventory for product ID: {} quantity: {}", productId, quantity, ex);
                return false;
            }
        }, throwable -> {
            logger.error("Circuit breaker fallback triggered for inventory creation", throwable);
            return false;
        });
    }

    /**
     * Update inventory quantity for an existing product
     */
    public boolean updateInventory(Long productId, Integer quantity) {
        logger.debug("Updating inventory for product ID: {} with quantity: {}", productId, quantity);
        
        return circuitBreaker.run(() -> {
            try {
                Map<String, Object> updateData = Map.of(
                    "quantity", quantity,
                    "availableQuantity", quantity
                );
                
                String response = webClient.put()
                    .uri("/api/v1/inventory/product/{productId}", productId)
                    .bodyValue(updateData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

                return parseSuccessResponse(response);
                
            } catch (Exception ex) {
                logger.error("Error updating inventory for product ID: {} quantity: {}", productId, quantity, ex);
                return false;
            }
        }, throwable -> {
            logger.error("Circuit breaker fallback triggered for inventory update", throwable);
            return false;
        });
    }

    /**
     * Check if a specific quantity is available for a product
     */
    public boolean checkAvailability(Long productId, Integer quantity) {
        logger.debug("Checking availability for product ID: {} quantity: {}", productId, quantity);
        
        return circuitBreaker.run(() -> {
            try {
                String response = webClient.get()
                    .uri("/api/v1/inventory/availability/{productId}?quantity={quantity}", 
                         productId, quantity)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

                return parseAvailabilityResponse(response);
                
            } catch (Exception ex) {
                logger.error("Error checking availability for product ID: {} quantity: {}", productId, quantity, ex);
                return false; // Default to unavailable on error
            }
        }, throwable -> {
            logger.error("Circuit breaker fallback triggered for availability check", throwable);
            return false; // Default to unavailable on circuit breaker
        });
    }

    /**
     * Parse inventory response and extract InventoryDto
     */
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

    /**
     * Parse bulk inventory response and create product ID to inventory mapping
     */
    private Map<Long, InventoryDto> parseInventoryBulkResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            if (!root.has("success") || !root.get("success").asBoolean()) {
                return Map.of();
            }
            
            JsonNode dataNode = root.get("data");
            if (dataNode == null || !dataNode.isArray()) {
                return Map.of();
            }
            
            List<InventoryDto> inventories = objectMapper.readerFor(objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, InventoryDto.class))
                    .readValue(dataNode);
                    
            return inventories.stream()
                    .collect(Collectors.toMap(InventoryDto::getProductId, inventory -> inventory));
                    
        } catch (Exception ex) {
            logger.error("Error parsing bulk inventory response: {}", response, ex);
            return Map.of();
        }
    }

    /**
     * Parse availability response
     */
    private boolean parseAvailabilityResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            if (!root.has("success") || !root.get("success").asBoolean()) {
                return false;
            }
            
            JsonNode dataNode = root.get("data");
            if (dataNode == null || !dataNode.has("available")) {
                return false;
            }
            
            return dataNode.get("available").asBoolean();
            
        } catch (Exception ex) {
            logger.error("Error parsing availability response: {}", response, ex);
            return false;
        }
    }

    /**
     * Parse generic success response
     */
    private boolean parseSuccessResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.has("success") && root.get("success").asBoolean();
        } catch (Exception ex) {
            logger.error("Error parsing success response: {}", response, ex);
            return false;
        }
    }
}