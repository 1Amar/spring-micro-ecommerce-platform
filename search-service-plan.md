# 🔍 Search Service Implementation Plan
**Lead Developer Implementation Strategy - Based on Gemini Analysis + Production Requirements**

*September 5, 2025 - For 1.4M+ Product E-commerce Platform*

---

## 📊 **Current Context & Requirements**

### **Dataset Scale & Challenges**
```yaml
Current Data Volume:
  - Products: 1.4M+ with real Amazon dataset
  - Categories: 248+ with hierarchy
  - Real product images and metadata
  - Growing dataset (future expansion expected)

Performance Requirements:
  - Search response time: < 200ms
  - Auto-complete: < 50ms
  - Index update latency: < 5 seconds
  - Concurrent users: 1000+
  - Search accuracy: > 85% relevance
```

### **Existing Infrastructure**
```yaml
Available Services:
  ✅ Product Service (8088): Source of truth for product data
  ✅ Kafka (9092): Event streaming for real-time updates
  ✅ Redis (6379): Caching layer available
  ✅ API Gateway (8081): Routing and security
  ✅ Eureka (8761): Service discovery
  ✅ PostgreSQL: 1.4M+ products ready for indexing

Missing Components:
  ❌ Elasticsearch cluster
  ❌ Search service (Port 8090)
  ❌ Search indexing pipeline
  ❌ Frontend search integration
```

---

## 🏗️ **COMPREHENSIVE IMPLEMENTATION PLAN**

### **🚀 PHASE 1: Infrastructure Setup & Data Modeling (Day 1-2)**

#### **Task 1.1: Elasticsearch Infrastructure Setup - 4 Hours**
```yaml
Environment Setup:
  1. Add Elasticsearch to Docker Compose:
     - Single-node cluster for development
     - 2GB heap size minimum for 1.4M products
     - Data persistence volume mapping
     
  2. Configure Elasticsearch for production:
     - Index templates for consistent mappings
     - Cluster health monitoring
     - Backup and recovery procedures

Docker Configuration:
  services:
    elasticsearch:
      image: docker.elastic.co/elasticsearch/elasticsearch:8.10.0
      environment:
        - discovery.type=single-node
        - ES_JAVA_OPTS=-Xms2g -Xmx2g
        - xpack.security.enabled=false
      ports:
        - "9200:9200"
      volumes:
        - elasticsearch-data:/usr/share/elasticsearch/data

Success Criteria:
  - Elasticsearch accessible at localhost:9200
  - Cluster health: GREEN status
  - Index operations working
```

#### **Task 1.2: Search Service Project Structure - 2 Hours**
```yaml
Project Setup:
  1. Create search-service Spring Boot module
  2. Add dependencies:
     - Spring Boot Web
     - Spring Data Elasticsearch
     - Spring Cloud Eureka Client
     - Spring Kafka
     - Common Library integration

Directory Structure:
  search-service/
  ├── src/main/java/com/amar/search/
  │   ├── SearchServiceApplication.java
  │   ├── config/
  │   │   ├── ElasticsearchConfig.java
  │   │   └── KafkaConfig.java
  │   ├── controller/
  │   │   └── SearchController.java
  │   ├── service/
  │   │   ├── SearchService.java
  │   │   ├── ProductIndexService.java
  │   │   └── SearchAnalyticsService.java
  │   ├── repository/
  │   │   └── ProductSearchRepository.java
  │   └── dto/
  │       ├── SearchRequestDto.java
  │       ├── SearchResponseDto.java
  │       └── ProductSearchDto.java
  └── src/main/resources/
      └── application.yml

Port Assignment: 8090 (Search Service)
```

#### **Task 1.3: Advanced Elasticsearch Mapping Design - 3 Hours**
```json
{
  "mappings": {
    "properties": {
      "productId": { 
        "type": "long",
        "index": true
      },
      "name": { 
        "type": "text",
        "analyzer": "standard",
        "fields": {
          "keyword": { "type": "keyword" },
          "suggest": { 
            "type": "completion",
            "contexts": [
              {
                "name": "category_context",
                "type": "category"
              }
            ]
          }
        }
      },
      "description": { 
        "type": "text",
        "analyzer": "english"
      },
      "brand": { 
        "type": "keyword",
        "fields": {
          "text": { "type": "text" }
        }
      },
      "category": {
        "type": "nested",
        "properties": {
          "id": { "type": "long" },
          "name": { "type": "keyword" },
          "level": { "type": "integer" },
          "path": { "type": "keyword" }
        }
      },
      "price": { 
        "type": "double",
        "index": true
      },
      "imageUrl": { 
        "type": "keyword",
        "index": false
      },
      "availability": {
        "type": "keyword",
        "index": true
      },
      "rating": {
        "type": "float",
        "index": true
      },
      "reviewCount": {
        "type": "integer",
        "index": true
      },
      "tags": { 
        "type": "keyword"
      },
      "createdAt": { 
        "type": "date",
        "format": "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
      },
      "updatedAt": { 
        "type": "date",
        "format": "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
      },
      "searchScore": {
        "type": "float",
        "index": false
      }
    }
  },
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "product_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": [
            "lowercase",
            "stop",
            "snowball"
          ]
        }
      }
    }
  }
}
```

---

### **🔄 PHASE 2: Batch Indexing Pipeline (Day 3-4)**

#### **Task 2.1: High-Performance Bulk Indexing - 8 Hours**
```java
@Service
@Slf4j
public class ProductIndexService {
    
    private static final int BATCH_SIZE = 5000; // Optimal for 1.4M products
    private static final int THREAD_POOL_SIZE = 4;
    
    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @Value("${search.indexing.enabled:true}")
    private boolean indexingEnabled;
    
    /**
     * Initial bulk indexing of all 1.4M+ products
     * Estimated time: 15-20 minutes for complete indexing
     */
    public IndexingResult indexAllProducts() {
        if (!indexingEnabled) {
            log.warn("Product indexing is disabled via configuration");
            return IndexingResult.skipped();
        }
        
        long startTime = System.currentTimeMillis();
        AtomicLong totalProcessed = new AtomicLong(0);
        AtomicLong totalErrors = new AtomicLong(0);
        
        try {
            // Get total product count for progress tracking
            long totalProducts = productServiceClient.getTotalProductCount();
            log.info("Starting bulk indexing of {} products in batches of {}", 
                    totalProducts, BATCH_SIZE);
            
            // Create thread pool for parallel processing
            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            List<Future<BatchResult>> futures = new ArrayList<>();
            
            // Process in batches
            for (int page = 0; page * BATCH_SIZE < totalProducts; page++) {
                final int currentPage = page;
                
                Future<BatchResult> future = executorService.submit(() -> 
                    processBatch(currentPage, BATCH_SIZE));
                futures.add(future);
            }
            
            // Collect results
            for (Future<BatchResult> future : futures) {
                BatchResult result = future.get();
                totalProcessed.addAndGet(result.getProcessed());
                totalErrors.addAndGet(result.getErrors());
                
                // Progress logging every 50k products
                if (totalProcessed.get() % 50000 == 0) {
                    log.info("Indexed {} products so far", totalProcessed.get());
                }
            }
            
            executorService.shutdown();
            
            long duration = System.currentTimeMillis() - startTime;
            IndexingResult result = IndexingResult.builder()
                .totalProcessed(totalProcessed.get())
                .totalErrors(totalErrors.get())
                .durationMs(duration)
                .build();
                
            log.info("Bulk indexing completed: {} products indexed, {} errors, {} seconds", 
                    result.getTotalProcessed(), 
                    result.getTotalErrors(),
                    duration / 1000);
                    
            return result;
            
        } catch (Exception e) {
            log.error("Bulk indexing failed", e);
            throw new SearchIndexingException("Failed to index products", e);
        }
    }
    
    private BatchResult processBatch(int page, int size) {
        try {
            // Fetch products from Product Service
            Page<ProductDto> productPage = productServiceClient.getProducts(
                PageRequest.of(page, size));
            
            List<ProductSearchDocument> documents = productPage.getContent()
                .stream()
                .map(this::mapToSearchDocument)
                .collect(Collectors.toList());
            
            // Bulk index to Elasticsearch
            List<IndexQuery> indexQueries = documents.stream()
                .map(doc -> {
                    IndexQuery query = new IndexQuery();
                    query.setId(doc.getProductId().toString());
                    query.setObject(doc);
                    return query;
                })
                .collect(Collectors.toList());
            
            elasticsearchTemplate.bulkIndex(indexQueries, ProductSearchDocument.class);
            
            return BatchResult.builder()
                .processed(documents.size())
                .errors(0)
                .build();
                
        } catch (Exception e) {
            log.error("Batch processing failed for page {}", page, e);
            return BatchResult.builder()
                .processed(0)
                .errors(size)
                .build();
        }
    }
    
    /**
     * Scheduled re-indexing for data freshness
     * Runs every night at 2 AM when traffic is low
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledReindex() {
        if (isMaintenanceWindow()) {
            log.info("Starting scheduled product re-indexing");
            indexAllProducts();
        }
    }
}
```

#### **Task 2.2: Real-time Indexing via Kafka - 6 Hours**
```java
@Component
@Slf4j
public class ProductEventListener {
    
    @Autowired
    private ProductIndexService productIndexService;
    
    /**
     * Handle product creation events
     */
    @KafkaListener(topics = "product.created", groupId = "search-service")
    public void handleProductCreated(ProductCreatedEvent event) {
        try {
            log.info("Indexing new product: {}", event.getProductId());
            productIndexService.indexSingleProduct(event.getProductId());
            
        } catch (Exception e) {
            log.error("Failed to index new product: {}", event.getProductId(), e);
            // Send to DLQ for retry
            sendToDeadLetterQueue(event);
        }
    }
    
    /**
     * Handle product updates
     */
    @KafkaListener(topics = "product.updated", groupId = "search-service")
    public void handleProductUpdated(ProductUpdatedEvent event) {
        try {
            log.info("Re-indexing updated product: {}", event.getProductId());
            productIndexService.reindexProduct(event.getProductId());
            
        } catch (Exception e) {
            log.error("Failed to re-index product: {}", event.getProductId(), e);
            sendToDeadLetterQueue(event);
        }
    }
    
    /**
     * Handle product deletion
     */
    @KafkaListener(topics = "product.deleted", groupId = "search-service")
    public void handleProductDeleted(ProductDeletedEvent event) {
        try {
            log.info("Removing product from index: {}", event.getProductId());
            productIndexService.removeFromIndex(event.getProductId());
            
        } catch (Exception e) {
            log.error("Failed to remove product from index: {}", event.getProductId(), e);
        }
    }
    
    /**
     * Handle inventory updates for availability
     */
    @KafkaListener(topics = "inventory.stock.updated", groupId = "search-service")
    public void handleStockUpdated(StockUpdatedEvent event) {
        try {
            // Update only availability field without full re-indexing
            productIndexService.updateAvailability(
                event.getProductId(), 
                event.getQuantity() > 0);
                
        } catch (Exception e) {
            log.error("Failed to update product availability: {}", event.getProductId(), e);
        }
    }
}
```

---

### **🔍 PHASE 3: Advanced Search API Implementation (Day 5-6)**

#### **Task 3.1: Multi-faceted Search Controller - 6 Hours**
```java
@RestController
@RequestMapping("/api/v1/search")
@Slf4j
public class SearchController {
    
    @Autowired
    private SearchService searchService;
    
    @Autowired
    private SearchAnalyticsService analyticsService;
    
    /**
     * Main product search endpoint with advanced filtering
     */
    @GetMapping("/products")
    public ResponseEntity<SearchResponseDto> searchProducts(
            @RequestParam String query,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> brands,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            SearchRequestDto searchRequest = SearchRequestDto.builder()
                .query(query)
                .categories(categories)
                .brands(brands)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .pageable(pageable)
                .build();
            
            SearchResponseDto response = searchService.searchProducts(searchRequest);
            
            // Track search analytics
            analyticsService.trackSearch(query, response.getTotalElements(), 
                getUserId(request), getSessionId(request));
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Search completed: query='{}', results={}, duration={}ms", 
                    query, response.getTotalElements(), duration);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Search failed for query: {}", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SearchResponseDto.error("Search temporarily unavailable"));
        }
    }
    
    /**
     * Auto-complete suggestions endpoint
     */
    @GetMapping("/suggest")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        try {
            List<String> suggestions = searchService.getSuggestions(query, limit);
            return ResponseEntity.ok(suggestions);
            
        } catch (Exception e) {
            log.error("Suggestion failed for query: {}", query, e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
    
    /**
     * Search facets for filtering UI
     */
    @GetMapping("/facets")
    public ResponseEntity<SearchFacetsDto> getSearchFacets(
            @RequestParam String query) {
        
        try {
            SearchFacetsDto facets = searchService.getSearchFacets(query);
            return ResponseEntity.ok(facets);
            
        } catch (Exception e) {
            log.error("Facets failed for query: {}", query, e);
            return ResponseEntity.ok(SearchFacetsDto.empty());
        }
    }
    
    /**
     * Popular/trending searches
     */
    @GetMapping("/trending")
    public ResponseEntity<List<String>> getTrendingSearches(
            @RequestParam(defaultValue = "10") Integer limit) {
        
        try {
            List<String> trending = analyticsService.getTrendingSearches(limit);
            return ResponseEntity.ok(trending);
            
        } catch (Exception e) {
            log.error("Failed to get trending searches", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}
```

#### **Task 3.2: Advanced Search Service Implementation - 8 Hours**
```java
@Service
@Slf4j
public class SearchService {
    
    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;
    
    @Autowired
    private ProductSearchRepository productSearchRepository;
    
    private static final String PRODUCTS_INDEX = "products";
    
    /**
     * Advanced product search with multiple criteria
     */
    public SearchResponseDto searchProducts(SearchRequestDto request) {
        
        // Build complex Elasticsearch query
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        
        // Main search query (multi-match across name and description)
        if (StringUtils.hasText(request.getQuery())) {
            queryBuilder.must(
                QueryBuilders.multiMatchQuery(request.getQuery())
                    .field("name", 2.0f)  // Boost name matches
                    .field("description", 1.0f)
                    .field("brand", 1.5f)  // Boost brand matches
                    .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                    .fuzziness(Fuzziness.AUTO)
                    .prefixLength(2)
            );
        } else {
            queryBuilder.must(QueryBuilders.matchAllQuery());
        }
        
        // Category filter
        if (CollectionUtils.isNotEmpty(request.getCategories())) {
            queryBuilder.filter(
                QueryBuilders.termsQuery("category.name", request.getCategories())
            );
        }
        
        // Brand filter
        if (CollectionUtils.isNotEmpty(request.getBrands())) {
            queryBuilder.filter(
                QueryBuilders.termsQuery("brand", request.getBrands())
            );
        }
        
        // Price range filter
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            RangeQueryBuilder priceRange = QueryBuilders.rangeQuery("price");
            if (request.getMinPrice() != null) {
                priceRange.gte(request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                priceRange.lte(request.getMaxPrice());
            }
            queryBuilder.filter(priceRange);
        }
        
        // Rating filter
        if (request.getMinRating() != null) {
            queryBuilder.filter(
                QueryBuilders.rangeQuery("rating").gte(request.getMinRating())
            );
        }
        
        // Availability filter (only show in-stock items by default)
        queryBuilder.filter(
            QueryBuilders.termQuery("availability", "IN_STOCK")
        );
        
        // Build search request
        SearchRequest searchRequest = new SearchRequest(PRODUCTS_INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(queryBuilder);
        
        // Sorting
        addSorting(sourceBuilder, request);
        
        // Pagination
        sourceBuilder.from(request.getPageable().getPageNumber() * 
                          request.getPageable().getPageSize());
        sourceBuilder.size(request.getPageable().getPageSize());
        
        // Add aggregations for facets
        addFacetAggregations(sourceBuilder);
        
        searchRequest.source(sourceBuilder);
        
        try {
            SearchResponse response = elasticsearchTemplate.execute(
                ElasticsearchRestTemplate.class, operations -> 
                    operations.search(searchRequest, RequestOptions.DEFAULT)
            );
            
            return buildSearchResponse(response, request);
            
        } catch (Exception e) {
            log.error("Elasticsearch search failed", e);
            throw new SearchException("Search operation failed", e);
        }
    }
    
    private void addSorting(SearchSourceBuilder sourceBuilder, SearchRequestDto request) {
        String sortBy = request.getSortBy();
        SortOrder sortOrder = "asc".equalsIgnoreCase(request.getSortDirection()) 
                              ? SortOrder.ASC : SortOrder.DESC;
        
        switch (sortBy) {
            case "price":
                sourceBuilder.sort("price", sortOrder);
                break;
            case "rating":
                sourceBuilder.sort("rating", sortOrder);
                break;
            case "name":
                sourceBuilder.sort("name.keyword", sortOrder);
                break;
            case "newest":
                sourceBuilder.sort("createdAt", SortOrder.DESC);
                break;
            case "relevance":
            default:
                sourceBuilder.sort("_score", SortOrder.DESC);
                break;
        }
    }
    
    /**
     * Auto-complete suggestions using completion suggester
     */
    public List<String> getSuggestions(String query, Integer limit) {
        CompletionSuggestionBuilder suggestionBuilder = 
            SuggestBuilders.completionSuggestion("name.suggest")
                .prefix(query)
                .size(limit);
        
        SuggestBuilder suggestBuilder = new SuggestBuilder()
            .addSuggestion("product_suggestions", suggestionBuilder);
        
        SearchRequest searchRequest = new SearchRequest(PRODUCTS_INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.suggest(suggestBuilder);
        sourceBuilder.size(0); // We only want suggestions, not documents
        
        searchRequest.source(sourceBuilder);
        
        try {
            SearchResponse response = elasticsearchTemplate.execute(
                ElasticsearchRestTemplate.class, operations -> 
                    operations.search(searchRequest, RequestOptions.DEFAULT)
            );
            
            return extractSuggestions(response);
            
        } catch (Exception e) {
            log.error("Suggestion search failed", e);
            return Collections.emptyList();
        }
    }
}
```

---

### **🔗 PHASE 4: Integration & Frontend (Day 7-8)**

#### **Task 4.1: API Gateway Integration - 2 Hours**
```yaml
API Gateway Configuration:
  # Add to ecom-api-gateway/src/main/resources/application.yml
  spring:
    cloud:
      gateway:
        routes:
          - id: search-service
            uri: lb://search-service
            predicates:
              - Path=/api/v1/search/**
            filters:
              - StripPrefix=0

Service Registration:
  # search-service application.yml
  eureka:
    client:
      service-url:
        defaultZone: http://localhost:8761/eureka
    instance:
      prefer-ip-address: true
  
  server:
    port: 8090
  
  spring:
    application:
      name: search-service
```

#### **Task 4.2: Angular Search Integration - 6 Hours**
```typescript
// search.service.ts
@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private apiUrl = `${environment.apiUrl}/search`;
  
  constructor(private http: HttpClient) {}
  
  searchProducts(searchRequest: SearchRequest): Observable<SearchResponse> {
    const params = this.buildSearchParams(searchRequest);
    
    return this.http.get<SearchResponse>(`${this.apiUrl}/products`, { params })
      .pipe(
        retry(2),
        catchError(this.handleError)
      );
  }
  
  getSuggestions(query: string): Observable<string[]> {
    const params = new HttpParams().set('query', query).set('limit', '10');
    
    return this.http.get<string[]>(`${this.apiUrl}/suggest`, { params })
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        catchError(() => of([]))
      );
  }
  
  getTrendingSearches(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/trending`);
  }
}

// search.component.ts
@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {
  searchForm: FormGroup;
  searchResults: SearchResponse | null = null;
  suggestions: string[] = [];
  isLoading = false;
  facets: SearchFacets | null = null;
  
  constructor(
    private fb: FormBuilder,
    private searchService: SearchService,
    private router: Router,
    private route: ActivatedRoute
  ) {}
  
  ngOnInit() {
    this.initializeForm();
    this.setupAutoComplete();
    this.handleRouteParams();
  }
  
  private setupAutoComplete() {
    this.searchForm.get('query')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(query => {
          if (query && query.length >= 2) {
            return this.searchService.getSuggestions(query);
          }
          return of([]);
        })
      )
      .subscribe(suggestions => {
        this.suggestions = suggestions;
      });
  }
  
  onSearch() {
    const searchRequest = this.buildSearchRequest();
    this.isLoading = true;
    
    this.searchService.searchProducts(searchRequest)
      .subscribe({
        next: (response) => {
          this.searchResults = response;
          this.facets = response.facets;
          this.isLoading = false;
          this.updateUrl(searchRequest);
        },
        error: (error) => {
          console.error('Search failed:', error);
          this.isLoading = false;
        }
      });
  }
}
```

---

### **📊 PHASE 5: Performance Optimization & Analytics (Day 9-10)**

#### **Task 5.1: Search Analytics & Performance Monitoring - 4 Hours**
```java
@Service
@Slf4j
public class SearchAnalyticsService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String SEARCH_ANALYTICS_KEY = "search:analytics:";
    private static final String TRENDING_SEARCHES_KEY = "search:trending";
    
    /**
     * Track search queries and results
     */
    public void trackSearch(String query, long resultCount, String userId, String sessionId) {
        try {
            SearchAnalyticsEvent event = SearchAnalyticsEvent.builder()
                .query(query.toLowerCase().trim())
                .resultCount(resultCount)
                .userId(userId)
                .sessionId(sessionId)
                .timestamp(Instant.now())
                .build();
            
            // Store in Redis for real-time analytics
            String key = SEARCH_ANALYTICS_KEY + LocalDate.now().toString();
            redisTemplate.opsForList().leftPush(key, event);
            redisTemplate.expire(key, Duration.ofDays(30));
            
            // Update trending searches (sorted set with scores)
            redisTemplate.opsForZSet().incrementScore(TRENDING_SEARCHES_KEY, query, 1.0);
            
            // Send to Kafka for long-term analytics
            kafkaTemplate.send("search.analytics", event);
            
        } catch (Exception e) {
            log.error("Failed to track search analytics", e);
            // Don't fail the search if analytics fail
        }
    }
    
    /**
     * Get trending searches based on frequency
     */
    public List<String> getTrendingSearches(int limit) {
        try {
            Set<String> trending = redisTemplate.opsForZSet()
                .reverseRange(TRENDING_SEARCHES_KEY, 0, limit - 1);
            
            return new ArrayList<>(trending != null ? trending : Collections.emptySet());
            
        } catch (Exception e) {
            log.error("Failed to get trending searches", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Search performance metrics
     */
    @EventListener
    public void handleSearchPerformed(SearchPerformedEvent event) {
        // Track search performance metrics
        Metrics.timer("search.duration")
            .record(event.getDuration(), TimeUnit.MILLISECONDS);
        
        Metrics.counter("search.requests")
            .increment(Tags.of("result_count", String.valueOf(event.getResultCount())));
        
        if (event.getDuration() > 200) {
            Metrics.counter("search.slow_requests").increment();
        }
    }
}
```

#### **Task 5.2: Performance Optimization - 4 Hours**
```java
@Component
public class SearchPerformanceOptimizer {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String SEARCH_CACHE_KEY = "search:cache:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    
    /**
     * Cache popular searches to reduce Elasticsearch load
     */
    public SearchResponseDto getCachedSearchResult(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(SEARCH_CACHE_KEY + cacheKey);
            if (cached != null) {
                log.debug("Returning cached search result for key: {}", cacheKey);
                return (SearchResponseDto) cached;
            }
        } catch (Exception e) {
            log.warn("Failed to get cached search result", e);
        }
        return null;
    }
    
    /**
     * Cache search results for popular queries
     */
    public void cacheSearchResult(String cacheKey, SearchResponseDto result) {
        try {
            if (result.getTotalElements() > 0) {
                redisTemplate.opsForValue().set(
                    SEARCH_CACHE_KEY + cacheKey, 
                    result, 
                    CACHE_TTL
                );
                log.debug("Cached search result for key: {}", cacheKey);
            }
        } catch (Exception e) {
            log.warn("Failed to cache search result", e);
        }
    }
    
    /**
     * Warm up cache with popular searches
     */
    @Scheduled(fixedDelay = 3600000) // Every hour
    public void warmUpSearchCache() {
        List<String> popularQueries = getPopularQueries();
        
        for (String query : popularQueries) {
            try {
                // Execute search to warm up cache
                searchService.searchProducts(
                    SearchRequestDto.builder()
                        .query(query)
                        .pageable(PageRequest.of(0, 20))
                        .build()
                );
                
                Thread.sleep(100); // Avoid overwhelming Elasticsearch
                
            } catch (Exception e) {
                log.warn("Failed to warm up cache for query: {}", query, e);
            }
        }
    }
}
```

---

## 🎯 **SUCCESS METRICS & VALIDATION**

### **Performance Targets**
```yaml
Response Times:
  - Product search: < 200ms (95th percentile)
  - Auto-complete: < 50ms (95th percentile)
  - Faceted search: < 300ms (95th percentile)
  - Index updates: < 5 seconds (real-time)

Accuracy Metrics:
  - Search relevance: > 85% user satisfaction
  - Auto-complete accuracy: > 90% useful suggestions
  - Zero-result searches: < 5% of total searches

Scalability:
  - Concurrent search requests: 1000+ users
  - Index size: 1.4M+ products with room for 10M+
  - Query throughput: 500+ searches/second
```

### **Quality Gates**
```yaml
Phase 1 Complete ✅:
  - Elasticsearch cluster running and healthy
  - 1.4M+ products successfully indexed
  - Index mappings optimized for search performance

Phase 2 Complete ✅:
  - Real-time product updates via Kafka
  - Batch re-indexing operational
  - Index consistency maintained

Phase 3 Complete ✅:
  - Search API returns relevant results < 200ms
  - Auto-complete working with < 50ms response
  - Faceted search with category/brand/price filters

Phase 4 Complete ✅:
  - Frontend search integration functional
  - API Gateway routing search requests
  - Search analytics tracking user behavior

Phase 5 Complete ✅:
  - Search caching reducing Elasticsearch load
  - Performance metrics monitoring
  - Trending searches feature operational
```

---

## 🚀 **IMPLEMENTATION TIMELINE**

### **10-Day Sprint Schedule**
```yaml
Day 1-2: Infrastructure & Data Modeling
  - Elasticsearch setup and configuration
  - Service project structure
  - Advanced index mapping design

Day 3-4: Data Indexing Pipeline
  - Bulk indexing of 1.4M products
  - Real-time Kafka event processing
  - Index maintenance operations

Day 5-6: Search API Implementation
  - Advanced search service with faceting
  - Auto-complete and suggestions
  - Search controllers and DTOs

Day 7-8: Integration & Frontend
  - API Gateway integration
  - Angular search components
  - Search results UI

Day 9-10: Optimization & Analytics
  - Performance optimization with caching
  - Search analytics and trending
  - Load testing and fine-tuning
```

### **Risk Mitigation**
```yaml
Technical Risks:
  - Elasticsearch memory requirements for 1.4M products
  - Index corruption during bulk operations
  - Search performance under high load

Mitigation Strategies:
  - Elasticsearch cluster monitoring and alerting
  - Incremental indexing with rollback capability
  - Redis caching for popular searches
  - Circuit breaker pattern for service resilience
```

---

## ⭐ **IMMEDIATE NEXT ACTIONS**

1. **Set up Elasticsearch in Docker Compose** (Day 1 Morning)
2. **Create search-service Spring Boot project** (Day 1 Afternoon)
3. **Design and test index mapping with sample data** (Day 2)

**Estimated Total Time: 10 days for complete production-ready search service**

This plan builds upon our proven microservices patterns while delivering enterprise-grade search capabilities for our 1.4M+ product catalog.