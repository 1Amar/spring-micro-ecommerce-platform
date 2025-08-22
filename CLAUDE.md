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

## Important 
 - write small change ->test. if working continue else revert.
