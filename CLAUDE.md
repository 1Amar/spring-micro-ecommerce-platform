# Spring Boot Microservices E-Commerce Platform with Angular Frontend

## Project Overview
E-commerce platform with Spring Boot microservices backend and Angular frontend. Features modern observability, authentication, and scalable architecture.

## Architecture
- **Backend**: Spring Boot microservices with Spring Cloud
- **Frontend**: Angular 16 with Material Design and Bootstrap
- **Authentication**: Keycloak integration
- **Infrastructure**: Docker Compose with ELK stack, Prometheus, Grafana, Jaeger
- **Database**: PostgreSQL with separate schemas per service
- **Messaging**: Apache Kafka for event streaming
- **Caching**: Redis for performance optimization
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
- ✅ Product catalog with real Amazon dataset images
- ✅ Shopping cart with Redis storage
- ✅ Real-time cart updates and session management

## 📊 **Current Service Status (August 31, 2025)**

### **✅ COMPLETED SERVICES**

#### **Product Service (Port 8088) - PRODUCTION READY**
- ✅ **Database**: 1.4M+ Amazon products with real images
- ✅ **Performance**: Optimized queries and caching
- ✅ **API**: Full CRUD operations with pagination
- ✅ **Integration**: Eureka registration, health endpoints

#### **User Service (Port 8082) - PRODUCTION READY**
- ✅ **Authentication**: Keycloak OAuth2 integration
- ✅ **Profile Management**: User profiles and preferences
- ✅ **Security**: JWT token validation
- ✅ **Frontend**: Complete Angular integration

#### **Cart Service (Port 8089) - PRODUCTION READY**
- ✅ **Redis Storage**: Session-based cart management
- ✅ **Real-time Updates**: WebSocket-like reactivity
- ✅ **Product Integration**: Live product validation
- ✅ **Anonymous/Auth**: UUID sessions + user accounts

#### **Inventory Service (Port 8084) - PRODUCTION READY ✅**
- ✅ **Stock Management**: Real-time inventory tracking
- ✅ **Stock Reservations**: Temporary holds during checkout (15-min TTL)
- ✅ **Stock Movements**: Complete audit trail (INBOUND/OUTBOUND/RESERVED/RELEASED)
- ✅ **Low Stock Alerts**: Configurable threshold monitoring
- ✅ **Redis Caching**: Performance-optimized inventory lookups
- ✅ **Database Integration**: PostgreSQL with proper migrations
- ✅ **Event Streaming**: Kafka integration for inventory events
- ✅ **API Endpoints**: Complete REST API with proper error handling

#### **API Gateway (Port 8081) - PRODUCTION READY**
- ✅ **Security**: JWT token validation for all requests
- ✅ **Routing**: Intelligent request routing to services
- ✅ **CORS**: Configured for Angular frontend
- ✅ **Load Balancing**: Eureka-based service discovery

#### **Database Layer - PRODUCTION READY**
- ✅ **Liquibase Migrations**: Version-controlled schema management (14 changesets)
- ✅ **Multi-Schema**: Separate schemas per service
- ✅ **Amazon Dataset**: Real product data with images
- ✅ **Performance Indexes**: Optimized for common queries
- ✅ **Referential Integrity**: Proper foreign key constraints

### **📋 TODO: SERVICE INTEGRATIONS (September 2025)**

#### **Priority 1: Inventory → Cart Integration**
- [ ] **Real-time Stock Validation**: Cart should check inventory before adding items
- [ ] **Stock Reservation**: Reserve items when added to cart
- [ ] **Reservation Release**: Auto-release expired cart items
- [ ] **Stock Level Display**: Show available quantity in cart
- [ ] **Out-of-Stock Handling**: Prevent checkout for unavailable items

#### **Priority 2: Cart → Order Integration** 
- [ ] **Order Creation**: Convert cart to order workflow
- [ ] **Inventory Commitment**: Permanent stock allocation during order
- [ ] **Stock Movement Recording**: Track inventory changes through order lifecycle
- [ ] **Order Fulfillment**: Update inventory when orders ship
- [ ] **Cancellation Handling**: Release inventory for cancelled orders

#### **Priority 3: Product → Inventory Integration**
- [ ] **Product Availability**: Display real-time stock status on product pages
- [ ] **Inventory Validation**: Prevent product catalog showing unavailable items
- [ ] **Low Stock Indicators**: Show "Only X left" warnings
- [ ] **Restock Notifications**: Alert users when out-of-stock items return

#### **Priority 4: User → Inventory Integration**
- [ ] **Wishlist Stock Tracking**: Monitor stock for saved items
- [ ] **Purchase History**: Link with inventory movements
- [ ] **Personalized Alerts**: User-specific low stock notifications
- [ ] **Reserved Items Dashboard**: Show user's active reservations

### **🔧 Next Development Priorities**

#### **Search Service Implementation**
- **Elasticsearch Integration**: Product search with advanced filtering
- **Auto-suggestions**: Real-time search as user types
- **Search Analytics**: Track popular searches
- **Inventory-aware Search**: Filter by stock availability

#### **Order Service Implementation**  
- **Order Management**: Complete order workflow
- **Status Tracking**: Real-time order updates
- **Inventory Integration**: Stock commitment and fulfillment
- **Return Processing**: Inventory restoration for returns

#### **Payment Service Integration**
- **Payment Gateway**: Stripe/PayPal integration
- **Transaction Security**: PCI compliant processing
- **Inventory Hold**: Extend reservations during payment
- **Failed Payment Handling**: Release inventory on failure

### **⚡ Quick Health Checks**
```bash
# All service health
curl -s http://localhost:8084/actuator/health  # Inventory
curl -s http://localhost:8089/actuator/health  # Cart  
curl -s http://localhost:8088/actuator/health  # Product
curl -s http://localhost:8082/actuator/health  # User

# Service registration
curl -s http://localhost:8761/eureka/apps | grep -E 'INVENTORY-SERVICE|CART-SERVICE|PRODUCT-SERVICE'

# Test integration endpoints
curl -s "http://localhost:8084/api/v1/inventory/product/2148581"
curl -s "http://localhost:8089/api/v1/cart/health"
```

### **📈 Performance Metrics**
- **Inventory Operations**: < 50ms (Redis cached)
- **Cart Operations**: < 50ms (Redis optimized)
- **Product Catalog**: < 100ms (Database optimized)
- **Stock Reservations**: < 100ms (PostgreSQL + Redis)
- **Service Discovery**: < 10ms (Eureka)

### **🔧 Development Patterns**
```bash
# Standard service development
cd common-library && mvn clean install
cd ../[service-name] && mvn clean compile
curl http://localhost:[port]/actuator/health

# Database migrations
cd sql-migration && mvn liquibase:update

# Integration testing
curl -s "http://localhost:8084/api/v1/inventory/reserve" -X POST \
-H "Content-Type: application/json" \
-d '{"orderId":"uuid","items":[{"productId":123,"quantity":2}],"userId":"test"}'
```

## **💡 Key Architecture Decisions**
- **Microservices Pattern**: Independent deployable services
- **Database-per-Service**: Isolated data with proper migrations  
- **Event-Driven Architecture**: Kafka for service communication
- **API-First Design**: OpenAPI documentation for all endpoints
- **Caching Strategy**: Redis for frequently accessed data
- **Security**: Keycloak OAuth2 with JWT tokens
- **Observability**: OpenTelemetry + Prometheus + Grafana stack