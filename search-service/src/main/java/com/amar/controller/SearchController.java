package com.amar.controller;

import com.amar.search.dto.SearchResponseDto;
import com.amar.service.SearchService;
import com.amar.service.ProductIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private SearchService searchService;

    @Autowired
    private ProductIndexService productIndexService;

    /**
     * Health check endpoint for ELK test integration
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "search-health-" + UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        
        logger.info("Search service: Health check requested - correlationId: {}", correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "search-service");
        response.put("status", "UP");
        response.put("correlationId", correlationId);
        response.put("port", 8086);
        response.put("totalProductsIndexed", searchService.getTotalProductsIndexed());
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Search service is running and ready");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Main product search endpoint
     * Compatible with frontend ProductService.searchProducts() method
     */
    @GetMapping("/products")
    public ResponseEntity<SearchResponseDto> searchProducts(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "name") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {
        
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "search-products-" + UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        
        logger.info("Search service: Product search requested - query: '{}', page: {}, size: {}, correlationId: {}", 
                   query, page, size, correlationId);
        
        try {
            // Create pageable with sorting
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                       Sort.by(sortBy).descending() : 
                       Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            SearchResponseDto response = searchService.searchProducts(query, pageable);
            
            logger.info("Search service: Product search completed - query: '{}', results: {}, correlationId: {}", 
                       query, response.getTotalElements(), correlationId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Search service: Product search failed - query: '{}', correlationId: {}, error: {}", 
                        query, correlationId, e.getMessage(), e);
            
            SearchResponseDto errorResponse = SearchResponseDto.error(
                "Search service temporarily unavailable", query != null ? query : "");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Search products by brand
     */
    @GetMapping("/products/brand")
    public ResponseEntity<SearchResponseDto> searchByBrand(
            @RequestParam(value = "brand") String brand,
            @PageableDefault(size = 20) Pageable pageable) {
        
        String correlationId = MDC.get("correlationId");
        logger.info("Search service: Brand search requested - brand: '{}', correlationId: {}", brand, correlationId);
        
        try {
            SearchResponseDto response = searchService.searchByBrand(brand, pageable);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Search service: Brand search failed - brand: '{}', correlationId: {}, error: {}", 
                        brand, correlationId, e.getMessage(), e);
            
            SearchResponseDto errorResponse = SearchResponseDto.error(
                "Brand search failed", "brand:" + brand);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Search products by category
     */
    @GetMapping("/products/category")
    public ResponseEntity<SearchResponseDto> searchByCategory(
            @RequestParam(value = "category") String category,
            @PageableDefault(size = 20) Pageable pageable) {
        
        String correlationId = MDC.get("correlationId");
        logger.info("Search service: Category search requested - category: '{}', correlationId: {}", category, correlationId);
        
        try {
            SearchResponseDto response = searchService.searchByCategory(category, pageable);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Search service: Category search failed - category: '{}', correlationId: {}, error: {}", 
                        category, correlationId, e.getMessage(), e);
            
            SearchResponseDto errorResponse = SearchResponseDto.error(
                "Category search failed", "category:" + category);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get search service statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSearchStats() {
        String correlationId = MDC.get("correlationId");
        logger.info("Search service: Stats requested - correlationId: {}", correlationId);
        
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("service", "search-service");
            stats.put("totalProductsIndexed", searchService.getTotalProductsIndexed());
            stats.put("correlationId", correlationId);
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("elasticsearchStatus", "connected");
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Search service: Stats request failed - correlationId: {}, error: {}", 
                        correlationId, e.getMessage(), e);
            
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("service", "search-service");
            errorStats.put("error", "Failed to retrieve stats");
            errorStats.put("correlationId", correlationId);
            
            return ResponseEntity.status(500).body(errorStats);
        }
    }

    /**
     * Manual indexing endpoint for testing
     */
    @PostMapping("/index")
    public ResponseEntity<Map<String, Object>> indexProducts() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "manual-index-" + UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        
        logger.info("Search service: Manual indexing requested - correlationId: {}", correlationId);
        
        try {
            // Trigger the indexing process
            ProductIndexService.IndexingResult result = productIndexService.indexAllProducts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("service", "search-service");
            response.put("operation", "manual-indexing");
            response.put("correlationId", correlationId);
            response.put("status", result.isSuccess() ? "SUCCESS" : "ERROR");
            response.put("totalProcessed", result.getTotalProcessed());
            response.put("totalErrors", result.getTotalErrors());
            response.put("durationMs", result.getDurationMs());
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", result.isSuccess() ? 
                "Product indexing completed successfully" : 
                "Product indexing failed: " + result.getErrorMessage());
            
            logger.info("Search service: Manual indexing completed - correlationId: {}, result: {}", 
                       correlationId, result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Search service: Manual indexing failed - correlationId: {}, error: {}", 
                        correlationId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "search-service");
            errorResponse.put("operation", "manual-indexing");
            errorResponse.put("correlationId", correlationId);
            errorResponse.put("status", "ERROR");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Simulation endpoint for ELK testing
     */
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulateSearch() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "search-sim-" + UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        
        logger.info("Search service: Simulation requested - correlationId: {}", correlationId);
        
        try {
            // Perform a sample search to test functionality
            SearchResponseDto testSearch = searchService.searchProducts("laptop", 
                PageRequest.of(0, 5, Sort.by("name").ascending()));
            
            Map<String, Object> response = new HashMap<>();
            response.put("service", "search-service");
            response.put("operation", "search-simulation");
            response.put("correlationId", correlationId);
            response.put("status", "SUCCESS");
            response.put("simulatedQuery", "laptop");
            response.put("simulatedResults", testSearch.getTotalElements());
            response.put("searchDuration", testSearch.getSearchDuration());
            response.put("totalProductsIndexed", searchService.getTotalProductsIndexed());
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "Search simulation completed successfully");
            
            logger.info("Search service: Simulation completed - correlationId: {}, results: {}", 
                       correlationId, testSearch.getTotalElements());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Search service: Simulation failed - correlationId: {}, error: {}", 
                        correlationId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "search-service");
            errorResponse.put("operation", "search-simulation");
            errorResponse.put("correlationId", correlationId);
            errorResponse.put("status", "ERROR");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}