package com.amar.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            // Escape JSON string properly to handle quotes and special characters
            String escapedQuery = escapeJsonString(query);
            
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
                "}", escapedQuery, escapedQuery, escapedQuery, pageable.getOffset(), pageable.getPageSize());
        } catch (Exception e) {
            logger.error("Failed to build Elasticsearch query", e);
            return "{\"query\": {\"match_all\": {}}}";
        }
    }
    
    /**
     * Escape JSON string to handle quotes and special characters
     */
    private String escapeJsonString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
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

    /**
     * Get search suggestions for autocomplete functionality
     */
    public List<com.amar.dto.SuggestionDto> getSuggestions(String query, int limit) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "suggestions-" + UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        long startTime = System.currentTimeMillis();
        List<com.amar.dto.SuggestionDto> suggestions = new ArrayList<>();

        try {
            logger.info("Search service: Getting suggestions - query: '{}', limit: {}, correlationId: {}", 
                       query, limit, correlationId);

            if (!StringUtils.hasText(query) || query.trim().length() < 2) {
                return suggestions; // Return empty list for short queries
            }

            String trimmedQuery = query.trim().toLowerCase();

            // Build Elasticsearch aggregation query for suggestions
            String suggestionQuery = buildSuggestionQuery(trimmedQuery, limit);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(suggestionQuery, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                elasticsearchUrl + "/products/_search",
                HttpMethod.POST,
                entity,
                String.class
            );

            // Parse suggestions from Elasticsearch response
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            suggestions = parseSuggestionResults(responseJson, trimmedQuery);

            long duration = System.currentTimeMillis() - startTime;

            logger.info("Search service: Suggestions completed - query: '{}', suggestions: {}, duration: {}ms, correlationId: {}", 
                       query, suggestions.size(), duration, correlationId);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Search service: Suggestions failed - query: '{}', duration: {}ms, correlationId: {}, error: {}", 
                        query, duration, correlationId, e.getMessage(), e);
            
            // Fallback to basic repository search
            suggestions = getFallbackSuggestions(query, limit);
        }

        return suggestions;
    }

    /**
     * Build Elasticsearch query for suggestions using appropriate queries for different field types
     */
    private String buildSuggestionQuery(String query, int limit) {
        String escapedQuery = escapeJsonString(query);
        int queryLength = query.trim().length();
        
        if (queryLength <= 3) {
            // For short queries, prioritize categories only with proper case handling
            String capitalizedQuery = query.trim().substring(0, 1).toUpperCase() + 
                                    (query.trim().length() > 1 ? query.trim().substring(1).toLowerCase() : "");
            String escapedCapitalizedQuery = escapeJsonString(capitalizedQuery);
            
            return String.format("""
                {
                  "size": 0,
                  "query": {
                    "prefix": {
                      "categoryName": {
                        "value": "%s"
                      }
                    }
                  },
                  "aggs": {
                    "categories": {
                      "terms": {
                        "field": "categoryName",
                        "size": %d,
                        "order": {"_count": "desc"}
                      }
                    }
                  }
                }
                """, escapedCapitalizedQuery, limit);
        } else {
            // For longer queries, mix categories and products with category priority
            return String.format("""
                {
                  "size": %d,
                  "query": {
                    "bool": {
                      "should": [
                        {"match": {"categoryName": {"query": "%s", "boost": 10}}},
                        {"match_phrase_prefix": {"name": {"query": "%s", "boost": 1}}}
                      ],
                      "minimum_should_match": 1
                    }
                  },
                  "_source": ["name", "categoryName"],
                  "collapse": {
                    "field": "categoryName"
                  }
                }
                """, Math.min(limit, 6), escapedQuery, escapedQuery);
        }
    }

    /**
     * Parse suggestion results from both aggregation and hits response
     */
    private List<com.amar.dto.SuggestionDto> parseSuggestionResults(JsonNode responseJson, String originalQuery) {
        List<com.amar.dto.SuggestionDto> suggestions = new ArrayList<>();
        
        try {
            // Check if we have aggregations (short query response)
            if (responseJson.has("aggregations") && responseJson.get("aggregations").has("categories")) {
                JsonNode categories = responseJson.get("aggregations").get("categories").get("buckets");
                for (JsonNode bucket : categories) {
                    String categoryName = bucket.get("key").asText();
                    int count = bucket.get("doc_count").asInt();
                    suggestions.add(new com.amar.dto.SuggestionDto(
                        categoryName, com.amar.dto.SuggestionDto.SuggestionType.CATEGORY, count));
                }
            } else {
                // Parse hits response (longer query)
                JsonNode hits = responseJson.get("hits").get("hits");
                Set<String> seenCategories = new HashSet<>();
                
                for (JsonNode hit : hits) {
                    JsonNode source = hit.get("_source");
                    
                    // Prioritize category suggestions
                    if (source.has("categoryName")) {
                        String categoryName = source.get("categoryName").asText();
                        if (!seenCategories.contains(categoryName)) {
                            suggestions.add(new com.amar.dto.SuggestionDto(
                                categoryName, com.amar.dto.SuggestionDto.SuggestionType.CATEGORY, 1));
                            seenCategories.add(categoryName);
                        }
                    }
                    
                    // Add product suggestions only if we have space and categories are covered
                    if (source.has("name") && suggestions.size() < 6) {
                        String productName = source.get("name").asText();
                        // Limit product name length for better UX
                        if (productName.length() > 80) {
                            productName = productName.substring(0, 77) + "...";
                        }
                        suggestions.add(new com.amar.dto.SuggestionDto(
                            productName, com.amar.dto.SuggestionDto.SuggestionType.PRODUCT, 1));
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to parse suggestion results", e);
        }
        
        // Sort suggestions - categories first, then products, limit to 8 max
        return suggestions.stream()
            .sorted((a, b) -> {
                if (a.getType() == b.getType()) {
                    // Within same type, sort categories by count (popularity)
                    if (a.getType() == com.amar.dto.SuggestionDto.SuggestionType.CATEGORY) {
                        return Integer.compare(b.getCount(), a.getCount());
                    }
                    return 0;
                }
                return a.getType() == com.amar.dto.SuggestionDto.SuggestionType.CATEGORY ? -1 : 1;
            })
            .limit(8)
            .collect(Collectors.toList());
    }

    /**
     * Fallback suggestions using direct Elasticsearch calls when primary method fails
     */
    private List<com.amar.dto.SuggestionDto> getFallbackSuggestions(String query, int limit) {
        List<com.amar.dto.SuggestionDto> fallbackSuggestions = new ArrayList<>();
        
        try {
            if (StringUtils.hasText(query)) {
                // Use a simpler Elasticsearch query for fallback
                String escapedQuery = escapeJsonString(query.trim());
                String fallbackQuery = String.format("""
                    {
                      "size": %d,
                      "query": {
                        "bool": {
                          "should": [
                            {"match_phrase_prefix": {"name": {"query": "%s", "boost": 3}}},
                            {"match": {"brand": {"query": "%s", "boost": 2}}},
                            {"match": {"categoryName": {"query": "%s", "boost": 1}}}
                          ]
                        }
                      }
                    }
                    """, limit, escapedQuery, escapedQuery, escapedQuery);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                HttpEntity<String> entity = new HttpEntity<>(fallbackQuery, headers);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    elasticsearchUrl + "/products/_search",
                    HttpMethod.POST,
                    entity,
                    String.class
                );
                
                // Parse simple fallback results
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                JsonNode hits = responseJson.get("hits").get("hits");
                
                for (JsonNode hit : hits) {
                    JsonNode source = hit.get("_source");
                    String productName = source.get("name").asText();
                    fallbackSuggestions.add(new com.amar.dto.SuggestionDto(
                        productName, 
                        com.amar.dto.SuggestionDto.SuggestionType.PRODUCT, 
                        1));
                }
            }
        } catch (Exception e) {
            logger.error("Fallback suggestions also failed", e);
            // If even fallback fails, return some basic suggestions
            fallbackSuggestions.add(new com.amar.dto.SuggestionDto(
                query + " (search)", 
                com.amar.dto.SuggestionDto.SuggestionType.PRODUCT, 
                0));
        }
        
        return fallbackSuggestions;
    }
}