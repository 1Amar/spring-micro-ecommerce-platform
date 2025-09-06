package com.amar.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.amar.document.ProductSearchDocument;
import com.amar.repository.ProductSearchRepository;
import com.amar.search.dto.ProductSearchDto;
import com.amar.search.dto.SearchResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private ProductSearchRepository searchRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String elasticsearchUrl = "http://localhost:9200";

    /**
     * Search products with basic text search functionality
     */
    public SearchResponseDto searchProducts(String query, Pageable pageable) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "search-" + UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Search service: Starting product search - query: '{}', correlationId: {}", query, correlationId);

            List<ProductSearchDocument> documents;
            long totalHits;

            if (StringUtils.hasText(query)) {
                // Use direct HTTP client to avoid Spring Data compatibility issues
                String searchQuery = buildElasticsearchQuery(query.trim(), pageable);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                HttpEntity<String> entity = new HttpEntity<>(searchQuery, headers);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    elasticsearchUrl + "/products/_search",
                    HttpMethod.POST,
                    entity,
                    String.class
                );
                
                // Parse Elasticsearch response
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                documents = parseSearchResults(responseJson);
                totalHits = responseJson.get("hits").get("total").get("value").asLong();
            } else {
                // If no query provided, return all products using repository (count() works)
                Page<ProductSearchDocument> documentPage = searchRepository.findAll(pageable);
                documents = documentPage.getContent();
                totalHits = documentPage.getTotalElements();
            }

            // Convert documents to DTOs
            List<ProductSearchDto> searchResults = documents.stream()
                    .map(this::mapToSearchDto)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;

            // Calculate pagination info
            int totalPages = (int) Math.ceil((double) totalHits / pageable.getPageSize());
            
            SearchResponseDto response = SearchResponseDto.success(
                searchResults,
                totalHits,
                totalPages,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                query != null ? query : "",
                duration
            );

            logger.info("Search service: Search completed - query: '{}', results: {}, duration: {}ms, correlationId: {}", 
                       query, totalHits, duration, correlationId);

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Search service: Search failed - query: '{}', duration: {}ms, correlationId: {}, error: {}", 
                        query, duration, correlationId, e.getMessage(), e);
            
            return SearchResponseDto.error("Search operation failed: " + e.getMessage(), query != null ? query : "");
        }
    }

    /**
     * Get search statistics
     */
    public long getTotalProductsIndexed() {
        try {
            return searchRepository.count();
        } catch (Exception e) {
            logger.error("Failed to get total products count", e);
            return 0;
        }
    }

    /**
     * Search products by specific criteria
     */
    public SearchResponseDto searchByBrand(String brand, Pageable pageable) {
        String correlationId = MDC.get("correlationId");
        long startTime = System.currentTimeMillis();

        try {
            logger.info("Search service: Searching by brand - brand: '{}', correlationId: {}", brand, correlationId);

            Page<ProductSearchDocument> documentPage = searchRepository.findByBrand(brand, pageable);
            
            List<ProductSearchDto> searchResults = documentPage.getContent().stream()
                    .map(this::mapToSearchDto)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;

            return SearchResponseDto.success(
                searchResults,
                documentPage.getTotalElements(),
                documentPage.getTotalPages(),
                documentPage.getNumber(),
                documentPage.getSize(),
                "brand:" + brand,
                duration
            );

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Search service: Brand search failed - brand: '{}', duration: {}ms, correlationId: {}, error: {}", 
                        brand, duration, correlationId, e.getMessage(), e);
            
            return SearchResponseDto.error("Brand search failed: " + e.getMessage(), "brand:" + brand);
        }
    }

    /**
     * Search products by category
     */
    public SearchResponseDto searchByCategory(String categoryName, Pageable pageable) {
        String correlationId = MDC.get("correlationId");
        long startTime = System.currentTimeMillis();

        try {
            logger.info("Search service: Searching by category - category: '{}', correlationId: {}", categoryName, correlationId);

            Page<ProductSearchDocument> documentPage = searchRepository.findByCategoryName(categoryName, pageable);
            
            List<ProductSearchDto> searchResults = documentPage.getContent().stream()
                    .map(this::mapToSearchDto)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;

            return SearchResponseDto.success(
                searchResults,
                documentPage.getTotalElements(),
                documentPage.getTotalPages(),
                documentPage.getNumber(),
                documentPage.getSize(),
                "category:" + categoryName,
                duration
            );

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Search service: Category search failed - category: '{}', duration: {}ms, correlationId: {}, error: {}", 
                        categoryName, duration, correlationId, e.getMessage(), e);
            
            return SearchResponseDto.error("Category search failed: " + e.getMessage(), "category:" + categoryName);
        }
    }

    /**
     * Convert ProductSearchDocument to ProductSearchDto
     */
    private ProductSearchDto mapToSearchDto(ProductSearchDocument document) {
        if (document == null) {
            return null;
        }

        return ProductSearchDto.builder()
                .productId(document.getProductId())
                .name(document.getName())
                .description(document.getDescription())
                .brand(document.getBrand())
                .categoryName(document.getCategoryName())
                .price(document.getPrice())
                .imageUrl(document.getImageUrl())
                .sku(document.getSku())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private String buildElasticsearchQuery(String query, Pageable pageable) {
        try {
            return String.format("{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"should\": [\n" +
                "        { \"match\": { \"name\": { \"query\": \"%s\", \"boost\": 2.0 } } },\n" +
                "        { \"match\": { \"description\": { \"query\": \"%s\", \"boost\": 1.0 } } },\n" +
                "        { \"match\": { \"brand\": { \"query\": \"%s\", \"boost\": 1.5 } } }\n" +
                "      ],\n" +
                "      \"minimum_should_match\": 1\n" +
                "    }\n" +
                "  },\n" +
                "  \"from\": %d,\n" +
                "  \"size\": %d,\n" +
                "  \"sort\": [\n" +
                "    { \"_score\": { \"order\": \"desc\" } }\n" +
                "  ]\n" +
                "}", query, query, query, pageable.getOffset(), pageable.getPageSize());
        } catch (Exception e) {
            logger.error("Failed to build Elasticsearch query", e);
            return "{\"query\": {\"match_all\": {}}}";
        }
    }

    private List<ProductSearchDocument> parseSearchResults(JsonNode responseJson) {
        List<ProductSearchDocument> documents = new ArrayList<>();
        try {
            JsonNode hits = responseJson.get("hits").get("hits");
            for (JsonNode hit : hits) {
                JsonNode source = hit.get("_source");
                ProductSearchDocument document = new ProductSearchDocument();
                
                document.setId(hit.get("_id").asText());
                document.setProductId(source.get("productId").asLong());
                document.setName(source.get("name").asText());
                document.setDescription(source.has("description") ? source.get("description").asText() : null);
                document.setBrand(source.has("brand") ? source.get("brand").asText() : null);
                document.setCategoryName(source.has("categoryName") ? source.get("categoryName").asText() : null);
                document.setPrice(source.has("price") ? source.get("price").asDouble() : null);
                document.setImageUrl(source.has("imageUrl") ? source.get("imageUrl").asText() : null);
                document.setSku(source.has("sku") ? source.get("sku").asText() : null);
                
                // Parse dates if available
                if (source.has("createdAt")) {
                    try {
                        document.setCreatedAt(LocalDateTime.parse(source.get("createdAt").asText(), 
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    } catch (Exception e) {
                        logger.debug("Failed to parse createdAt date", e);
                    }
                }
                if (source.has("updatedAt")) {
                    try {
                        document.setUpdatedAt(LocalDateTime.parse(source.get("updatedAt").asText(), 
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    } catch (Exception e) {
                        logger.debug("Failed to parse updatedAt date", e);
                    }
                }
                
                documents.add(document);
            }
        } catch (Exception e) {
            logger.error("Failed to parse Elasticsearch response", e);
        }
        return documents;
    }
}