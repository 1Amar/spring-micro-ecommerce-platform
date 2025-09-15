package com.amar.service;

import com.amar.document.ProductSearchDocument;
import com.amar.dto.ProductDto;
import com.amar.repository.ProductSearchRepository;
import com.amar.search.mapper.ProductSearchMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductIndexService {

    private static final Logger logger = LoggerFactory.getLogger(ProductIndexService.class);
    private static final int BATCH_SIZE = 1000;

    @Autowired
    private ProductSearchRepository searchRepository;
    
    @Value("${services.product.url:http://product-service}")
    private String productServiceUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ProductSearchMapper productSearchMapper;

    /**
     * Custom Page implementation for REST template response
     */
    public static class RestPageImpl<T> implements Page<T> {
        private List<T> content;
        private long totalElements;
        private int totalPages;
        private int size;
        private int number;
        private boolean first;
        private boolean last;

        public RestPageImpl() {}

        @Override public List<T> getContent() { return content; }
        @Override public long getTotalElements() { return totalElements; }
        @Override public int getTotalPages() { return totalPages; }
        @Override public int getSize() { return size; }
        @Override public int getNumber() { return number; }
        @Override public boolean isFirst() { return first; }
        @Override public boolean isLast() { return last; }
        @Override public boolean hasContent() { return content != null && !content.isEmpty(); }
        @Override public boolean hasNext() { return !isLast(); }
        @Override public boolean hasPrevious() { return !isFirst(); }

        // Setters for JSON deserialization
        public void setContent(List<T> content) { this.content = content; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public void setSize(int size) { this.size = size; }
        public void setNumber(int number) { this.number = number; }
        public void setFirst(boolean first) { this.first = first; }
        public void setLast(boolean last) { this.last = last; }

        // Unused methods for simplicity
        @Override public org.springframework.data.domain.Pageable nextPageable() { return null; }
        @Override public org.springframework.data.domain.Pageable previousPageable() { return null; }
        @Override public org.springframework.data.domain.Pageable getPageable() { return PageRequest.of(number, size); }
        @Override public org.springframework.data.domain.Sort getSort() { return org.springframework.data.domain.Sort.unsorted(); }
        @Override public int getNumberOfElements() { return content != null ? content.size() : 0; }
        @Override public boolean isEmpty() { return !hasContent(); }
        @Override public <U> Page<U> map(java.util.function.Function<? super T, ? extends U> converter) { return null; }
        @Override public java.util.Iterator<T> iterator() { return content != null ? content.iterator() : java.util.Collections.emptyIterator(); }
    }

    /**
     * Index all products on startup if the search index is empty
     */
    @PostConstruct
    public void indexAllProductsOnStartup() {
        String correlationId = "startup-index-" + UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        try {
            long existingCount = searchRepository.count();
            logger.info("Search service: Startup indexing check - existing products: {}, correlationId: {}", 
                       existingCount, correlationId);

            if (existingCount == 0) {
                logger.info("Search service: No products found in search index. Starting initial indexing - correlationId: {}", 
                           correlationId);
                indexAllProducts();
            } else {
                logger.info("Search service: Products already indexed ({}). Skipping initial indexing - correlationId: {}", 
                           existingCount, correlationId);
            }
        } catch (Exception e) {
            logger.error("Search service: Startup indexing failed - correlationId: {}, error: {}", 
                        correlationId, e.getMessage(), e);
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Index all products from Product Service
     * Fetches products in batches and indexes them to Elasticsearch
     */
    public IndexingResult indexAllProducts() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "bulk-index-" + UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        long startTime = System.currentTimeMillis();
        long totalProcessed = 0;
        long totalErrors = 0;

        try {
            logger.info("Search service: Starting product indexing from Product Service - correlationId: {}", correlationId);

            int page = 0;
            boolean hasMorePages = true;

            while (hasMorePages) {
                try {
                    logger.info("Search service: Fetching products batch - page: {}, size: {}, correlationId: {}", 
                               page, BATCH_SIZE, correlationId);

                    // Fetch products from Product Service
                    String url = productServiceUrl + "/api/v1/products/catalog?page={page}&size={size}&sortBy=id&sortDir=asc";
                    
                    ResponseEntity<RestPageImpl<ProductDto>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<RestPageImpl<ProductDto>>() {},
                        page,
                        BATCH_SIZE
                    );

                    RestPageImpl<ProductDto> productPage = response.getBody();

                    if (productPage != null && productPage.hasContent()) {
                        List<ProductDto> products = productPage.getContent();
                        
                        logger.info("Search service: Indexing {} products from page {}, correlationId: {}", 
                                   products.size(), page, correlationId);

                        // Convert to search documents and save
                        List<ProductSearchDocument> documents = products.stream()
                                .map(this::mapToSearchDocument)
                                .collect(Collectors.toList());

                        searchRepository.saveAll(documents);
                        
                        totalProcessed += documents.size();
                        
                        logger.info("Search service: Indexed {} products, total: {}, correlationId: {}", 
                                   documents.size(), totalProcessed, correlationId);

                        // Check if there are more pages
                        hasMorePages = productPage.hasNext();
                        page++;

                        // Small delay to avoid overwhelming the system
                        if (hasMorePages) {
                            Thread.sleep(100);
                        }

                    } else {
                        logger.info("Search service: No products found on page {}, ending indexing - correlationId: {}", 
                                   page, correlationId);
                        hasMorePages = false;
                    }

                } catch (Exception e) {
                    logger.error("Search service: Error indexing page {} - correlationId: {}, error: {}", 
                                page, correlationId, e.getMessage(), e);
                    totalErrors += BATCH_SIZE;
                    page++;
                    
                    // Continue with next page instead of failing completely
                    if (page >= 10) { // Safety limit to prevent infinite loops
                        logger.error("Search service: Too many consecutive errors, stopping indexing - correlationId: {}", 
                                    correlationId);
                        hasMorePages = false;
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("Search service: Product indexing completed - processed: {}, errors: {}, duration: {}ms, correlationId: {}", 
                       totalProcessed, totalErrors, duration, correlationId);

            return new IndexingResult(totalProcessed, totalErrors, duration, correlationId);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Search service: Product indexing failed - processed: {}, duration: {}ms, correlationId: {}, error: {}", 
                        totalProcessed, duration, correlationId, e.getMessage(), e);
            
            return new IndexingResult(totalProcessed, totalErrors + 1, duration, correlationId, 
                                    "Indexing failed: " + e.getMessage());
        }
    }

    /**
     * Index a single product
     */
    public void indexSingleProduct(Long productId) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "single-index-" + UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        try {
            logger.info("Search service: Indexing single product - productId: {}, correlationId: {}", 
                       productId, correlationId);

            // Fetch product from Product Service
            String url = productServiceUrl + "/api/v1/products/catalog/{id}";
            ResponseEntity<ProductDto> response = restTemplate.getForEntity(url, ProductDto.class, productId);
            
            if (response.getBody() != null) {
                ProductSearchDocument document = mapToSearchDocument(response.getBody());
                searchRepository.save(document);
                
                logger.info("Search service: Successfully indexed product - productId: {}, correlationId: {}", 
                           productId, correlationId);
            } else {
                logger.warn("Search service: Product not found for indexing - productId: {}, correlationId: {}", 
                           productId, correlationId);
            }

        } catch (Exception e) {
            logger.error("Search service: Failed to index single product - productId: {}, correlationId: {}, error: {}", 
                        productId, correlationId, e.getMessage(), e);
        }
    }

    /**
     * Remove product from search index
     */
    public void removeProductFromIndex(Long productId) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Search service: Removing product from index - productId: {}, correlationId: {}", 
                       productId, correlationId);

            ProductSearchDocument existing = searchRepository.findByProductId(productId);
            if (existing != null) {
                searchRepository.delete(existing);
                logger.info("Search service: Successfully removed product from index - productId: {}, correlationId: {}", 
                           productId, correlationId);
            } else {
                logger.warn("Search service: Product not found in index for removal - productId: {}, correlationId: {}", 
                           productId, correlationId);
            }

        } catch (Exception e) {
            logger.error("Search service: Failed to remove product from index - productId: {}, correlationId: {}, error: {}", 
                        productId, correlationId, e.getMessage(), e);
        }
    }

    /**
     * Convert ProductDto to ProductSearchDocument
     */
    private ProductSearchDocument mapToSearchDocument(ProductDto productDto) {
        if (productDto == null) {
            return null;
        }

        ProductSearchDocument document = new ProductSearchDocument();
        document.setId(productDto.getId().toString());
        document.setProductId(productDto.getId());
        document.setName(productDto.getName());
        document.setDescription(productDto.getDescription());
        document.setBrand(productDto.getBrand());
        document.setCategoryName(productDto.getCategoryName() != null ? productDto.getCategoryName() : "");
        document.setPrice(productDto.getPrice() != null ? productDto.getPrice().doubleValue() : 0.0);
        document.setImageUrl(productDto.getImageUrl());
        document.setSku(productDto.getSku());
        document.setCreatedAt(productDto.getCreatedAt());
        document.setUpdatedAt(productDto.getUpdatedAt());

        return document;
    }

    /**
     * Result class for indexing operations
     */
    public static class IndexingResult {
        private final long totalProcessed;
        private final long totalErrors;
        private final long durationMs;
        private final String correlationId;
        private final String errorMessage;
        private final boolean success;

        public IndexingResult(long totalProcessed, long totalErrors, long durationMs, String correlationId) {
            this.totalProcessed = totalProcessed;
            this.totalErrors = totalErrors;
            this.durationMs = durationMs;
            this.correlationId = correlationId;
            this.errorMessage = null;
            this.success = totalErrors == 0;
        }

        public IndexingResult(long totalProcessed, long totalErrors, long durationMs, String correlationId, String errorMessage) {
            this.totalProcessed = totalProcessed;
            this.totalErrors = totalErrors;
            this.durationMs = durationMs;
            this.correlationId = correlationId;
            this.errorMessage = errorMessage;
            this.success = false;
        }

        // Getters
        public long getTotalProcessed() { return totalProcessed; }
        public long getTotalErrors() { return totalErrors; }
        public long getDurationMs() { return durationMs; }
        public String getCorrelationId() { return correlationId; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isSuccess() { return success; }

        @Override
        public String toString() {
            return "IndexingResult{" +
                    "totalProcessed=" + totalProcessed +
                    ", totalErrors=" + totalErrors +
                    ", durationMs=" + durationMs +
                    ", correlationId='" + correlationId + '\'' +
                    ", success=" + success +
                    '}';
        }
    }
}