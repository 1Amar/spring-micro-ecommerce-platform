package com.amar.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ProductEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ProductEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${product.kafka.topics.product-events:product-events}")
    private String productEventsTopic;

    @Autowired
    public ProductEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // =====================================================
    // Product Lifecycle Events
    // =====================================================

    public void publishProductEvent(String eventType, Long productId, Map<String, Object> productData) {
        logger.debug("Publishing product event: {} for product: {}", eventType, productId);
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "productId", productId,
                "productData", productData,
                "timestamp", LocalDateTime.now(),
                "source", "product-service"
            );
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(productEventsTopic, productId.toString(), event);
            
            future.thenAccept(result -> 
                logger.debug("Successfully published product event: {} for product: {}", eventType, productId))
                .exceptionally(ex -> {
                    logger.error("Failed to publish product event: {} for product: {}", eventType, productId, ex);
                    return null;
                });
                
        } catch (Exception ex) {
            logger.error("Error publishing product event: {} for product: {}", eventType, productId, ex);
        }
    }

    public void publishProductCreated(Long productId, String name, String sku, BigDecimal price, 
                                    String categoryName, String description, Boolean isActive) {
        Map<String, Object> productData = Map.of(
            "name", name != null ? name : "",
            "sku", sku != null ? sku : "",
            "price", price != null ? price : BigDecimal.ZERO,
            "categoryName", categoryName != null ? categoryName : "",
            "description", description != null ? description : "",
            "isActive", isActive != null ? isActive : true
        );
        
        publishProductEvent("product.created", productId, productData);
        logger.info("Published product.created event for product: {} ({})", productId, name);
    }

    public void publishProductUpdated(Long productId, String name, String sku, BigDecimal oldPrice, 
                                    BigDecimal newPrice, Boolean wasActive, Boolean isActive) {
        Map<String, Object> productData = Map.of(
            "name", name != null ? name : "",
            "sku", sku != null ? sku : "",
            "oldPrice", oldPrice != null ? oldPrice : BigDecimal.ZERO,
            "newPrice", newPrice != null ? newPrice : BigDecimal.ZERO,
            "priceChanged", oldPrice != null && newPrice != null && !oldPrice.equals(newPrice),
            "wasActive", wasActive != null ? wasActive : true,
            "isActive", isActive != null ? isActive : true,
            "statusChanged", wasActive != null && isActive != null && !wasActive.equals(isActive)
        );
        
        publishProductEvent("product.updated", productId, productData);
        logger.info("Published product.updated event for product: {} ({})", productId, name);
    }

    public void publishProductDeleted(Long productId, String name, String sku) {
        Map<String, Object> productData = Map.of(
            "name", name != null ? name : "",
            "sku", sku != null ? sku : "",
            "deleted", true,
            "reason", "Product soft deleted - set to inactive"
        );
        
        publishProductEvent("product.deleted", productId, productData);
        logger.info("Published product.deleted event for product: {} ({})", productId, name);
    }

    public void publishProductRestored(Long productId, String name, String sku) {
        Map<String, Object> productData = Map.of(
            "name", name != null ? name : "",
            "sku", sku != null ? sku : "",
            "restored", true,
            "reason", "Product restored - set to active"
        );
        
        publishProductEvent("product.restored", productId, productData);
        logger.info("Published product.restored event for product: {} ({})", productId, name);
    }

    // =====================================================
    // Price Change Events
    // =====================================================

    public void publishPriceChanged(Long productId, String name, BigDecimal oldPrice, BigDecimal newPrice, 
                                   String reason, Double discountPercentage) {
        Map<String, Object> productData = Map.of(
            "name", name != null ? name : "",
            "oldPrice", oldPrice != null ? oldPrice : BigDecimal.ZERO,
            "newPrice", newPrice != null ? newPrice : BigDecimal.ZERO,
            "priceChange", oldPrice != null && newPrice != null ? newPrice.subtract(oldPrice) : BigDecimal.ZERO,
            "reason", reason != null ? reason : "Price updated",
            "discountPercentage", discountPercentage != null ? discountPercentage : 0.0,
            "isPriceIncrease", oldPrice != null && newPrice != null && newPrice.compareTo(oldPrice) > 0,
            "isPriceDecrease", oldPrice != null && newPrice != null && newPrice.compareTo(oldPrice) < 0
        );
        
        publishProductEvent("product.price.changed", productId, productData);
        logger.info("Published product.price.changed event for product: {} - {} -> {}", productId, oldPrice, newPrice);
    }

    // =====================================================
    // Stock Events
    // =====================================================

    public void publishStockUpdated(Long productId, String name, Integer oldStock, Integer newStock, 
                                   Boolean trackInventory) {
        Map<String, Object> productData = Map.of(
            "name", name != null ? name : "",
            "oldStock", oldStock != null ? oldStock : 0,
            "newStock", newStock != null ? newStock : 0,
            "stockChange", oldStock != null && newStock != null ? newStock - oldStock : 0,
            "trackInventory", trackInventory != null ? trackInventory : true,
            "isOutOfStock", newStock != null && newStock <= 0,
            "wasOutOfStock", oldStock != null && oldStock <= 0
        );
        
        publishProductEvent("product.stock.updated", productId, productData);
        logger.debug("Published product.stock.updated event for product: {} - stock: {} -> {}", productId, oldStock, newStock);
    }

    // =====================================================
    // Feature Status Events
    // =====================================================

    public void publishFeaturedStatusChanged(Long productId, String name, Boolean wasFeatured, Boolean isFeatured) {
        Map<String, Object> productData = Map.of(
            "name", name != null ? name : "",
            "wasFeatured", wasFeatured != null ? wasFeatured : false,
            "isFeatured", isFeatured != null ? isFeatured : false,
            "statusChanged", wasFeatured != null && isFeatured != null && !wasFeatured.equals(isFeatured),
            "reason", isFeatured != null && isFeatured ? "Product marked as featured" : "Product unmarked as featured"
        );
        
        publishProductEvent("product.featured.changed", productId, productData);
        logger.info("Published product.featured.changed event for product: {} - featured: {} -> {}", 
                   productId, wasFeatured, isFeatured);
    }

    // =====================================================
    // Bulk Events
    // =====================================================

    public void publishBulkProductUpdate(String operation, int productCount, String reason) {
        logger.info("Publishing bulk product update event: {} for {} products", operation, productCount);
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", "product.bulk.updated",
                "operation", operation,
                "productCount", productCount,
                "reason", reason != null ? reason : "Bulk product operation",
                "timestamp", LocalDateTime.now(),
                "source", "product-service"
            );
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(productEventsTopic, "bulk-update", event);
            
            future.thenAccept(result -> 
                logger.info("Successfully published bulk product update event: {} for {} products", operation, productCount))
                .exceptionally(ex -> {
                    logger.error("Failed to publish bulk product update event", ex);
                    return null;
                });
                
        } catch (Exception ex) {
            logger.error("Error publishing bulk product update event", ex);
        }
    }

    // =====================================================
    // Health Check Event
    // =====================================================

    public void publishHealthCheckEvent() {
        logger.debug("Publishing health check event");
        
        try {
            Map<String, Object> event = Map.of(
                "eventType", "product.service.health",
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "source", "product-service"
            );
            
            kafkaTemplate.send(productEventsTopic, "health-check", event);
            
        } catch (Exception ex) {
            logger.error("Error publishing health check event", ex);
        }
    }
}