package com.amar.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProductValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ProductValidationService.class);

    private final WebClient webClient;
    private final Map<Long, Boolean> productExistenceCache = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> productDetailsCache = new ConcurrentHashMap<>();

    @Value("${inventory.services.product-service-url:http://localhost:8088}")
    private String productServiceBaseUrl;

    @Value("${inventory.product-validation.timeout-seconds:5}")
    private Integer timeoutSeconds;

    @Autowired
    public ProductValidationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
    }

    @CircuitBreaker(name = "product-service", fallbackMethod = "fallbackValidateProductExists")
    @Retry(name = "product-service")
    public boolean validateProductExists(Long productId) {
        logger.debug("Validating product existence for ID: {}", productId);

        Boolean cachedResult = productExistenceCache.get(productId);
        if (cachedResult != null) {
            logger.debug("Product existence check cache hit for ID: {}", productId);
            return cachedResult;
        }

        try {
            Boolean exists = webClient.get()
                .uri(productServiceBaseUrl + "/api/v1/products/catalog/{productId}/exists", productId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

            boolean result = exists != null ? exists : false;
            productExistenceCache.put(productId, result);
            
            logger.debug("Product existence validated for ID: {} - exists: {}", productId, result);
            return result;

        } catch (WebClientException ex) {
            logger.warn("Failed to validate product existence for ID: {}", productId, ex);
            throw ex;
        }
    }

    public boolean isProductActive(Long productId) {
        logger.debug("Checking if product is active for ID: {}", productId);
        return true; // Simple implementation
    }

    public void invalidateProductCache(Long productId) {
        logger.debug("Invalidating cache for product ID: {}", productId);
        productExistenceCache.remove(productId);
        productDetailsCache.remove(productId);
    }

    public boolean isProductServiceHealthy() {
        try {
            String healthResponse = webClient.get()
                .uri(productServiceBaseUrl + "/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                .block();

            boolean healthy = healthResponse != null && healthResponse.contains("\"status\":\"UP\"");
            logger.debug("Product service health check - healthy: {}", healthy);
            return healthy;

        } catch (Exception ex) {
            logger.warn("Product service health check failed", ex);
            return false;
        }
    }

    private boolean fallbackValidateProductExists(Long productId, Exception ex) {
        logger.warn("Product validation failed for product ID: {}, using fallback", productId, ex);
        Boolean cachedResult = productExistenceCache.get(productId);
        return cachedResult != null ? cachedResult : true;
    }
}