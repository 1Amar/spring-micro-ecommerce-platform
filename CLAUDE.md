# Spring Boot Microservices E-Commerce Platform with Angular Frontend

## Project Overview
e-commerce platform (in progress) with Spring Boot microservices backend and Angular frontend . Features modern observability, authentication, and scalable architecture.

## Architecture
- **Backend**: Spring Boot microservices with Spring Cloud
- **Frontend**: Angular 16 with Material Design and Bootstrap
- **Authentication**: Keycloak integration
- **Infrastructure**: Docker Compose with ELK stack, Prometheus, Grafana, Jaeger
- **Database**: PostgreSQL with separate databases per service
- **Messaging**: Apache Kafka for event streaming
- **Caching**: Redis for performance
- **Search**: Elasticsearch integration

## Backend Services 

### Core Services
1. **eureka-service-registry** (8761) - Service discovery
2. **ecom-api-gateway** (8081) - API Gateway with circuit breakers and rate limiting
3. **ecom-order-service** (8083) - Order management
4. **inventory-service** (8084) - Inventory tracking
5. **product-service** (8085) - Product catalog
6. **payment-service** (8086) - Payment processing
7. **notification-service** (8087) - User notifications
8. **notification-worker** (8088) - Background notification processing
9. **catalog-service** (8089) - Category management
10. **search-service** (8090) - Advanced search functionality
11. **common-library** - Shared observability and utilities

### Infrastructure Services
- **PostgreSQL** (5432) - Primary database with multiple schemas
- **Keycloak** (8080) - Authentication server
- **Redis** (6379) - Caching layer
- **Kafka + Zookeeper** - Event streaming
- **Elasticsearch** (9200) - Search and logging
- **Logstash** (5000) - Log processing
- **Kibana** (5601) - Log visualization
- **Prometheus** (9090) - Metrics collection
- **Grafana** (3000) - Metrics dashboard
- **Jaeger** (16686) - Distributed tracing

## Frontend Application
- **Angular 16** with TypeScript
- **Angular Material** + Bootstrap 5 UI components
- **Keycloak Angular** for authentication
- **RxJS** for reactive programming
- **Responsive design** with mobile support
- **Modular architecture** with lazy loading

### Frontend Features Implemented
- ✅ Complete authentication with Keycloak (OAuth2 + PKCE flow)
- ✅ HTTP interceptors (auth, loading, error handling)

## Observability Stack need to implement and test

### Logging
- **Centralized logging** with ELK stack
- **Structured logging** with JSON format
- **Correlation ID** tracking across services
- **Log forwarding** from all services to Logstash

### Metrics
- **Prometheus** metrics collection from all services
- **Grafana** dashboards for visualization
- **Custom metrics** for business KPIs
- **Service health monitoring**

### Tracing NEW: OpenTelemetry Implementation
- **OpenTelemetry** standard-compliant distributed tracing
- **OTLP export** to Jaeger via HTTP/gRPC
- **Automatic instrumentation** for Spring Boot
- **Correlation ID propagation** across all services
- **Custom span attributes** and semantic conventions
- **Performance monitoring** with detailed trace analysis
- **Future-ready** for metrics and logs correlation

## Current Status

### ✅ Completed Components
1. **Service Discovery**: Eureka server with all services registered
2. **API Gateway**: Spring Cloud Gateway with routing and circuit breakers  
3. **Frontend Authentication**: Complete OAuth2 + PKCE flow with Keycloak
4. **Correlation ID Tracking**: End-to-end correlation ID propagation working
5. **Complete Order Simulation Flow**: Full 3-service order flow (Order → Inventory → Payment → Notification)


### Backend Services
```bash
# Start infrastructure
cd Docker
docker-compose up -d

# Build all services
mvn clean install

# Start individual services (in order)
# 1. Start Eureka (8761)
cd eureka-service-registry && mvn spring-boot:run

# 2. Start API Gateway (8081)
cd ecom-api-gateway && mvn spring-boot:run

# 3. Start other services
cd inventory-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
# ... etc for all services

# Test order simulation
curl -X POST http://localhost:8081/api/v1/order/simulate
```

### Infrastructure Management
```bash
# Start all infrastructure
cd Docker
docker-compose up -d

# Check service health
curl http://localhost:8761  # Eureka
curl http://localhost:8081/actuator/health  # Gateway
curl http://localhost:9090  # Prometheus
curl http://localhost:3000  # Grafana
curl http://localhost:16686  # Jaeger
curl http://localhost:5601  # Kibana

# Restart infrastructure cleanly
.\restart-infrastructure.cmd
```

## Configuration Files

### Backend Configuration
- All services have `application.yml`
- Logback configuration for ELK logging
- Database initialization scripts in `Docker/init-db.sql`
- Keycloak realm configuration required

### Frontend Configuration
- Environment files in `src/environments/`
- API Gateway URL: `http://localhost:8081/api/v1`
- Keycloak URL: `http://localhost:8080`

## Security Status

### ✅ Current Working Security
- **Frontend Authentication**: OAuth2 + PKCE flow with Keycloak working
- **API Gateway Security**: JWT token validation working correctly
- **Public endpoints** (`/api/v1/public/**`) - No authentication required 
- **Protected endpoints** - Require valid JWT tokens
- **Role-based access** - Manager/Admin roles implemented

### 🚨 **CRITICAL SECURITY VULNERABILITY** - TO BE FIXED IN FUTURE
**Direct Service Access Bypasses ALL Security**

**Current Issue:**
- Frontend calls work correctly: `Frontend → API Gateway (JWT auth) → Services` ✅
- But direct service calls bypass security: `Direct → Services (no auth)` ❌
- Anyone can call `http://localhost:8083`, `8084`, `8085` etc. directly

**Examples of vulnerable direct calls:**
```bash
# These bypass ALL authentication/authorization:
curl -X POST http://localhost:8083/api/v1/simulation/complete-order-flow  # Works!
curl http://localhost:8084/api/v1/inventory/simulate                     # Works!  
curl http://localhost:8085/api/v1/notifications/send                     # Works!
```

**Attempted Fix (Temporarily Removed):**
- Added Gateway Authentication Filter with shared secret header
- API Gateway adds `X-Gateway-Secret` header to all requests
- Services validate the secret header before processing
- **Issue**: Broke service-to-service communication, caused 500 errors
- **Status**: Removed to restore functionality, needs proper implementation

**Future Implementation Plan:**
1. **Network-Level Security**: Use Docker networks/firewall to restrict direct access
2. **Service Mesh**: Implement Istio or similar for service-to-service security
3. **Gateway Authentication**: Re-implement shared secret with proper service discovery
4. **Internal TLS**: Add mTLS for service-to-service communication 

## Next Session Tasks

### Immediate Priorities
1. **Fix Critical Security Vulnerability**: Implement proper service-to-service security
2. **Test End-to-End Security**: Verify no direct service access possible
3. **Performance Testing**: Test complete order flow under load

### Current Working Features (Ready for Testing)
1. **✅ Frontend Login**: OAuth2 + PKCE authentication flow
2. **✅ API Gateway Security**: JWT token validation 
3. **✅ Complete Order Flow**: 3-service simulation working
4. **✅ Correlation ID Tracing**: End-to-end tracking in ELK/Kibana
5. **✅ Service Discovery**: All services registered in Eureka
6. **✅ Circuit Breaker Fixed**: Removed problematic circuit breaker config

## Testing & Verification

### Backend Testing
```bash
# Health checks
curl http://localhost:8761/actuator/health
curl http://localhost:8081/actuator/health

# Security endpoint testing (without authentication - should return 401)
curl http://localhost:8081/api/v1/test/roles
curl http://localhost:8081/api/v1/manager/reports  
curl http://localhost:8081/api/v1/admin/users

# Public endpoint (should work without authentication)
curl http://localhost:8081/api/v1/public/health

# Catalog endpoint (requires authentication)
curl http://localhost:8081/api/v1/catalog/categories

# Complete Order Flow simulation (end-to-end test) - WORKS!
curl -X POST http://localhost:8081/api/v1/simulation/complete-order-flow

# Frontend correlation ID test - WORKS!  
curl -H "X-Correlation-ID: test-12345" -X POST http://localhost:8081/api/v1/simulation/complete-order-flow

# Metrics and observability
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8081/actuator/health
```

### Code Standards
- Java: Spring Boot best practices with proper logging
- TypeScript: Angular style guide compliance
- Database: Proper schema design with migrations
- API: RESTful design with proper HTTP status codes

### Documentation
- API documentation with Swagger (can be added)
- Component documentation with JSDoc
- README files for each service
- Architecture decision records (ADRs) can be added

---

##always follow instructions.
##dont declare the issue is resoled until i tell you.
## Dont introduce new bug
## Most important - Don't jump to code Immediatly, first plan then code.

high priority issues to fix:
 Check if catalog service is receiving the correlation ID header
 Debug API Gateway routing and header forwarding
 Test with direct service call to verify LoggingFilter works
 Debug and verify CorrelationIdFilter is actually working
 CorrelationIdFilter not being loaded - fix with YAML approach
 Fix the correlation ID propagation configuration permanently
 Test end-to-end correlation ID propagation with authentication
 Fix API Gateway correlation ID propagation - preserve frontend header
 Test correlation ID propagation with actual service call
 Fix API Gateway routing - calls not reaching downstream services
 CRITICAL: Fix controller endpoints hanging - requests reach services but never complete
 CRITICAL: Debug API Gateway to downstream service routing issues