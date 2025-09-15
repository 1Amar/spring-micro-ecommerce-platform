package com.amar.client;

import com.amar.dto.ProductDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    @Value("${services.product.url:http://product-service}")
    private String productServiceUrl;

    @Autowired
    public ProductServiceClient(WebClient.Builder webClientBuilder,
                              CircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = webClientBuilder.build();
        this.circuitBreaker = circuitBreakerFactory.create("product-service");
    }

    // =====================================================
    // Product Retrieval
    // =====================================================

    public Optional<ProductDto> getProduct(Long productId) {
        logger.info("🔍 [PRODUCT-RETRIEVAL] Starting product lookup - Product ID: {}", productId);
        
        return circuitBreaker.run(() -> {
            try {
                String url = productServiceUrl + "/api/v1/products/catalog/" + productId;
                logger.debug("🌐 [PRODUCT-RETRIEVAL] Making HTTP request to: {}", url);
                
                Map<String, Object> response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                
                logger.debug("✅ [PRODUCT-RETRIEVAL] Received response from product service - Size: {}", 
                           response != null ? response.size() : "null");
                
                if (response != null && response.get("id") != null) {
                    // Product service returns product directly, not wrapped in "data"
                    Map<String, Object> productData = response;
                    
                    ProductDto product = new ProductDto();
                    product.setId(((Number) productData.get("id")).longValue());
                    product.setName((String) productData.get("name"));
                    product.setDescription((String) productData.get("description"));
                    product.setBrand((String) productData.get("brand"));
                    product.setImageUrl((String) productData.get("imageUrl"));
                    product.setSku((String) productData.get("sku"));
                    
                    Object priceObj = productData.get("price");
                    if (priceObj instanceof Number) {
                        product.setPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
                    }
                    
                    logger.info("✅ [PRODUCT-RETRIEVAL] Successfully retrieved product - ID: {}, Name: '{}', SKU: '{}', Price: {}", 
                              productId, product.getName(), product.getSku(), product.getPrice());
                    return Optional.of(product);
                } else {
                    logger.warn("❌ [PRODUCT-RETRIEVAL] Product not found - ID: {}, Response keys: {}", 
                              productId, response != null ? response.keySet() : "null");
                    return Optional.empty();
                }
                
            } catch (WebClientResponseException.NotFound e) {
                logger.warn("❌ [PRODUCT-RETRIEVAL] Product not found (404) - ID: {}", productId);
                return Optional.empty();
            } catch (WebClientResponseException e) {
                logger.error("❌ [PRODUCT-RETRIEVAL] HTTP error retrieving product - ID: {}, Status: {}, Response: {}", 
                           productId, e.getStatusCode(), e.getResponseBodyAsString());
                return Optional.empty();
            } catch (Exception e) {
                logger.error("❌ [PRODUCT-RETRIEVAL] Unexpected error retrieving product - ID: {}", productId, e);
                return Optional.empty();
            }
        }, throwable -> {
            logger.error("⚡ [PRODUCT-RETRIEVAL] Circuit breaker activated for product - ID: {}, Error: {}", 
                       productId, throwable.getMessage());
            return Optional.empty();
        });
    }

    public boolean isProductAvailable(Long productId) {
        logger.debug("Checking product availability: {}", productId);
        
        return circuitBreaker.run(() -> {
            try {
                String url = productServiceUrl + "/api/v1/products/catalog/" + productId;
                
                Map<String, Object> response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(3))
                        .block();
                
                boolean available = response != null && response.get("id") != null;
                logger.debug("Product availability for {}: {}", productId, available);
                return available;
                
            } catch (WebClientResponseException.NotFound e) {
                logger.debug("Product not available: {}", productId);
                return false;
            } catch (Exception e) {
                logger.error("Error checking product availability: {}", productId, e);
                return false;
            }
        }, throwable -> {
            logger.warn("Circuit breaker fallback for product availability check - product: {}", productId);
            return false;
        });
    }
}