# Cart Service

## Overview
The Cart Service is a critical microservice in the Spring Boot e-commerce platform that manages shopping cart operations for both authenticated users and anonymous sessions. It provides real-time cart management with Redis-based storage and integrates with inventory validation.

## Key Features
- ✅ **Session-based Cart Management** - Supports both authenticated users and anonymous sessions
- ✅ **Redis Storage** - High-performance cart data storage with TTL support
- ✅ **Real-time Inventory Integration** - Stock validation and reservation capabilities
- ✅ **Kafka Event Publishing** - Cart events for analytics and order processing
- ✅ **Circuit Breaker Pattern** - Resilient service-to-service communication
- ✅ **Health Monitoring** - Comprehensive health checks and metrics

## Architecture

### Technology Stack
- **Framework**: Spring Boot 3.x with Java 17
- **Storage**: Redis (Database 1) for cart persistence
- **Messaging**: Apache Kafka for event streaming
- **Service Discovery**: Netflix Eureka Client
- **Resilience**: Resilience4j Circuit Breaker
- **Monitoring**: Spring Actuator with OpenTelemetry tracing

### Port Configuration
- **Service Port**: 8089
- **Health Endpoint**: `/actuator/health`
- **Metrics**: `/actuator/metrics`

## Core Functionality

### Cart Operations
1. **Get Cart** - Retrieve cart items for user/session
2. **Add Item** - Add products to cart with quantity validation
3. **Update Item** - Modify item quantities in cart
4. **Remove Item** - Remove specific items from cart
5. **Clear Cart** - Empty entire cart contents
6. **Item Count** - Get total item count in cart

### Inventory Integration
1. **Stock Validation** - Verify product availability
2. **Stock Reservation** - Reserve items during checkout (15-min TTL)
3. **Reservation Release** - Release unused reservations
4. **Real-time Stock Info** - Include inventory data with cart items

### Event Publishing
- **Cart Events** - Item additions, removals, updates
- **Conversion Events** - Cart-to-order conversion tracking
- **Analytics Events** - User behavior and cart metrics

## API Endpoints

### Core Cart Operations
```http
GET    /api/v1/cart                    # Get cart contents
POST   /api/v1/cart/items             # Add item to cart
PUT    /api/v1/cart/items             # Update item quantity
DELETE /api/v1/cart/items/{productId} # Remove specific item
DELETE /api/v1/cart                   # Clear entire cart
GET    /api/v1/cart/count             # Get item count
```

### Inventory Integration
```http
GET    /api/v1/cart/with-inventory      # Get cart with stock info
POST   /api/v1/cart/validate-inventory  # Validate cart stock
POST   /api/v1/cart/reserve-stock       # Reserve cart items
POST   /api/v1/cart/release-reservation # Release stock reservation
```

### Event Publishing
```http
POST   /api/v1/cart/conversion-event    # Publish cart conversion event
```

### Health & Monitoring
```http
GET    /api/v1/cart/health             # Service health check
GET    /actuator/health                # Detailed health info
GET    /actuator/metrics               # Service metrics
```

## Service Integration

### Dependencies
- **Inventory Service** - Stock validation and reservation
- **Product Service** - Product information validation
- **Redis** - Cart data persistence
- **Kafka** - Event streaming
- **Eureka** - Service discovery

### Client Services
- **Order Service** - Cart-to-order conversion
- **Frontend Application** - User cart interactions
- **Analytics Service** - Cart behavior tracking

## Data Models

### Cart Session
```java
// Session-based cart identification
String userId;      // For authenticated users
String sessionId;   // For anonymous sessions (UUID)
```

### Cart Item
```java
Long productId;     // Product identifier
Integer quantity;   // Item quantity
BigDecimal price;   // Item price at time of addition
Instant addedAt;    // Timestamp when added
```

### Cart DTO
```java
List<CartItemDto> items;    // Cart items
Integer itemCount;          // Total item count
BigDecimal totalAmount;     // Cart total value
Instant lastUpdated;        // Last modification time
```

## Redis Configuration

### Database Assignment
- **Database**: 1 (dedicated cart database)
- **TTL**: 15 days for anonymous sessions
- **Pool Configuration**: Max 20 connections, optimized for cart operations

### Key Patterns
```
cart:user:{userId}           # User-based cart
cart:session:{sessionId}     # Session-based cart
cart:reservation:{orderId}   # Stock reservations
```

## Kafka Integration

### Topics
- **cart-events** - Cart operation events
- **inventory-events** - Stock-related events

### Event Types
```java
CART_ITEM_ADDED     // Item added to cart
CART_ITEM_UPDATED   // Item quantity changed
CART_ITEM_REMOVED   // Item removed from cart
CART_CLEARED        // Cart emptied
CART_CONVERTED      // Cart converted to order
```

### Event Structure
```json
{
  "eventType": "CART_ITEM_ADDED",
  "userId": "user123",
  "sessionId": "session456",
  "productId": 789,
  "quantity": 2,
  "timestamp": "2025-09-16T10:30:00Z",
  "metadata": {
    "source": "cart-service",
    "version": "1.0.0"
  }
}
```

## Configuration

### Environment Variables
```bash
# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Service Discovery
EUREKA_CLIENT_SERVICE_URL=http://localhost:8761/eureka/

# External Services
INVENTORY_SERVICE_URL=http://inventory-service
PRODUCT_SERVICE_URL=http://product-service

# OpenTelemetry Tracing
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
```

### Application Properties
```yaml
server:
  port: 8089

spring:
  application:
    name: cart-service
  data:
    redis:
      database: 1
      timeout: 2000ms
```

## Logging & Monitoring

### Log Levels
- **DEBUG**: Cart operations, Redis interactions
- **INFO**: Service events, API calls
- **ERROR**: Service failures, integration issues

### Log Format
```
2025-09-16 10:30:00 - Adding item to cart - UserId: user123, ProductId: 789, Quantity: 2
2025-09-16 10:30:01 - Added item to cart successfully - Items: 3, Total: $45.99
```

### Metrics
- **Cart Operations**: Add, update, remove operations per minute
- **Redis Performance**: Connection pool usage, response times
- **Inventory Integration**: Stock validation success/failure rates
- **Kafka Events**: Published events count and success rates

### Health Checks
- **Redis Connectivity**: Connection pool health
- **Kafka Producer**: Message publishing capability
- **External Services**: Inventory and product service availability
- **Memory Usage**: JVM heap and non-heap memory

## Circuit Breaker Configuration

### Inventory Service Circuit Breaker
```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventory-service:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
```

## Error Handling

### Common Error Scenarios
1. **Product Not Found** - Invalid product ID
2. **Insufficient Stock** - Requested quantity unavailable
3. **Redis Connection** - Cache unavailable
4. **Service Timeout** - External service unavailable

### Error Response Format
```json
{
  "error": "INSUFFICIENT_STOCK",
  "message": "Product 789 has only 2 items in stock",
  "timestamp": "2025-09-16T10:30:00Z",
  "productId": 789,
  "requestedQuantity": 5,
  "availableStock": 2
}
```

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
# Test with Redis running
mvn integration-test
```

### Health Check
```bash
curl http://localhost:8089/actuator/health
```

### Cart Operations Test
```bash
# Add item to cart
curl -X POST http://localhost:8089/api/v1/cart/items \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user" \
  -d '{"productId": 789, "quantity": 2}'

# Get cart contents
curl http://localhost:8089/api/v1/cart \
  -H "X-User-Id: test-user"
```

## Development Guidelines

### Prerequisites
1. **Redis Server** running on localhost:6379
2. **Kafka Cluster** running on localhost:9092
3. **Eureka Service Registry** on localhost:8761
4. **Inventory Service** available for stock validation
5. **Product Service** for product validation

### Build & Run
```bash
# Build the service
mvn clean compile

# Run locally
mvn spring-boot:run

# Build Docker image
docker build -t cart-service .
```

### Development Mode
```bash
# Start dependencies with Docker Compose
docker-compose up redis kafka eureka

# Run service in development mode
mvn spring-boot:run -Dspring.profiles.active=dev
```

## Production Considerations

### Performance
- **Redis Connection Pool**: Configured for high throughput
- **Kafka Batch Size**: Optimized for event publishing
- **Circuit Breaker**: Prevents cascade failures

### Security
- **Session Cookies**: HttpOnly, Secure flags
- **Input Validation**: All request parameters validated
- **CORS Configuration**: Restricted origins in production

### Scalability
- **Stateless Design**: No local state, fully Redis-based
- **Horizontal Scaling**: Multiple instances supported
- **Load Balancing**: Eureka-based service discovery

## Troubleshooting

### Common Issues
1. **Redis Connection Failed** - Check Redis server status and connection pool
2. **Kafka Producer Error** - Verify Kafka cluster availability
3. **Inventory Service Timeout** - Check network connectivity and service health
4. **Session Cookie Issues** - Verify cookie domain and secure settings

### Debug Commands
```bash
# Check service logs
docker logs cart-service

# Monitor Redis connections
redis-cli monitor

# View Kafka topics
kafka-topics --bootstrap-server localhost:9092 --list

# Test circuit breaker
curl http://localhost:8089/actuator/circuitbreakers
```

## Future Enhancements
- [ ] **Cart Sharing** - Share cart between devices
- [ ] **Wish List Integration** - Move items to wish list
- [ ] **Cart Analytics** - Advanced cart abandonment tracking
- [ ] **Personalized Recommendations** - AI-based product suggestions
- [ ] **Multi-currency Support** - International cart handling

---

**Service Status**: ✅ Production Ready
**Last Updated**: September 16, 2025
**Version**: 1.0.0