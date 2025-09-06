# Search Service Implementation Plan - Simple MVP First

## Current State Analysis
**Frontend Search Status:**
- Frontend already calls `/api/v1/products/catalog/search?name=query` 
- Product service exposes basic search endpoint at `/catalog/search`
- Also has advanced search at `/catalog/search/advanced` 
- Frontend uses ProductService.searchProducts() method

## **APPROVED INTEGRATION STRATEGY**
**User's Enhanced Plan - Navbar Search Integration:**
- Navbar search container calls new search service
- Search results redirect to `/products` page with enhanced results
- Existing product page layout preserved 
- Enhanced UI with better search features
- Keep existing product service search as fallback

## Implementation Strategy - Phase 1 (Simple Search Service)

### Day 1: Core Search Service Setup (6-8 hours)

**1.1 Project Structure & Dependencies (2 hours)**
```
search-service/
├── pom.xml (Spring Boot + Elasticsearch + Eureka)
├── src/main/java/com/amar/
│   ├── SearchServiceApplication.java
│   ├── config/
│   │   ├── ElasticsearchConfig.java
│   │   └── RestTemplateConfig.java (with @LoadBalanced)
│   ├── controller/
│   │   └── SearchController.java
│   ├── service/
│   │   ├── SearchService.java
│   │   └── ProductIndexService.java
│   ├── repository/
│   │   └── ProductSearchRepository.java
│   ├── document/
│   │   └── ProductSearchDocument.java
│   └── dto/
│       ├── ProductSearchDto.java
│       └── SearchResponseDto.java
└── src/main/resources/
    └── application.yml (port 8086 - no conflicts confirmed)
```

**1.2 Elasticsearch Document Model (1 hour)**
```java
@Document(indexName = "products")
public class ProductSearchDocument {
    @Id private String id;
    @Field(type = FieldType.Long) private Long productId;
    @Field(type = FieldType.Text, analyzer = "standard") private String name;
    @Field(type = FieldType.Text) private String description;
    @Field(type = FieldType.Keyword) private String brand;
    @Field(type = FieldType.Keyword) private String categoryName;
    @Field(type = FieldType.Double) private Double price;
    @Field(type = FieldType.Keyword, index = false) private String imageUrl;
}
```

**1.3 Simple Search API (2 hours)**
```java
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    
    @GetMapping("/products")
    public ResponseEntity<Page<ProductSearchDto>> searchProducts(
        @RequestParam(required = false) String query,
        @PageableDefault(size = 20) Pageable pageable) {
        // Implement basic text search
    }
    
    @GetMapping("/health") 
    // Health endpoint for ELK test integration
}
```

**1.4 Product Indexing from Product Service (3 hours)**
```java
@Service
public class ProductIndexService {
    @PostConstruct
    public void indexAllProductsOnStartup() {
        // Fetch products from Product Service via RestTemplate
        // Index 1.4M products in batches of 1000
        // Show progress logging
    }
}
```

### Day 2: Integration & API Gateway (4-6 hours)

**2.1 API Gateway Routes (1 hour)**
Add to API Gateway application.yml:
```yaml
- id: search-service
  uri: lb://search-service  
  predicates:
    - Path=/api/v1/search/**
```

**2.2 Frontend Integration - Navbar Search Enhancement (4 hours)**
**User's Approved Approach:**
1. Create new SearchService in Angular
2. Update navbar search to call search service
3. Route search results to `/products` page with search params
4. Enhance product page to display search results from search service
5. Keep existing ProductService as fallback

```typescript
// New: navbar-search.component.ts
onSearch() {
  this.router.navigate(['/products'], { 
    queryParams: { 
      search: this.searchQuery,
      source: 'search-service' 
    }
  });
}

// Enhanced: products.component.ts  
ngOnInit() {
  this.route.queryParams.subscribe(params => {
    if (params['search'] && params['source'] === 'search-service') {
      this.useSearchService(params['search']);
    } else {
      this.useProductService(); // Fallback
    }
  });
}
```

### Day 3: Testing & Polish (4 hours)

**3.1 Health Check Integration (1 hour)**
- Add search service health to ELK test component
- Test correlation ID propagation

**3.2 Error Handling & Logging (1 hour)**  
- Add proper exception handling
- Implement correlation ID logging

**3.3 Basic Performance Testing (2 hours)**
- Test search response times < 2 seconds
- Verify 1.4M product indexing works
- Load test with multiple concurrent requests

## Technical Implementation Details

### Backend Classes to Create:

**1. SearchServiceApplication.java**
```java
@SpringBootApplication
@EnableEurekaClient 
@EnableElasticsearchRepositories
public class SearchServiceApplication { }
```

**2. ElasticsearchConfig.java**
```java
@Configuration
public class ElasticsearchConfig {
    @Bean
    public ElasticsearchRestTemplate elasticsearchTemplate() {
        // Configure connection to localhost:9200
    }
}
```

**3. ProductSearchDocument.java** - Elasticsearch entity
**4. ProductSearchRepository.java** - Spring Data Elasticsearch repo  
**5. SearchService.java** - Business logic for search operations
**6. ProductIndexService.java** - Handles indexing from Product Service
**7. SearchController.java** - REST endpoints

### Frontend Integration - Navbar Search Flow:

**Enhanced User Experience:**
1. User types in navbar search
2. Search calls `/api/v1/search/products` 
3. Results displayed on familiar `/products` page
4. Enhanced search features (better relevance, speed)
5. Fallback to existing product service if needed

### Dependencies to Add:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

## Success Criteria:
1. ✅ Search service running on port 8086 and registered with Eureka
2. ✅ Basic text search working with response < 2 seconds  
3. ✅ 1.4M products indexed from Product Service
4. ✅ Navbar search integrated with enhanced product page results
5. ✅ ELK test shows search service health
6. ✅ Correlation IDs working through search requests
7. ✅ Existing product service search maintained as fallback

**Timeline: 3 days for working MVP, then enhance with advanced features from search-service-plan.md**

---

## Post-MVP Enhancement Path:
After MVP completion, implement advanced features from `search-service-plan.md`:
- Advanced faceting and filters
- Auto-complete suggestions
- Search analytics and trending
- Performance optimization with Redis caching
- Real-time updates via Kafka events