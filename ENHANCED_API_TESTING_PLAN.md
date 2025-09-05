# Enhanced API Testing Plan for Spring Boot Microservices E-Commerce Platform

## 📋 Executive Summary

This enhanced testing plan builds upon Gemini AI's excellent foundation and adds practical, executable test scenarios specifically tailored to our Spring Boot microservices architecture. The plan includes detailed API endpoints, test data, and automation scripts for systematic testing.

## 🏗️ Testing Architecture Overview

### Service Ports & Health Endpoints
- **API Gateway**: 8081 → `GET /actuator/health`
- **User Service**: 8082 → `GET /actuator/health`  
- **Order Service**: 8083 → `GET /actuator/health`
- **Inventory Service**: 8084 → `GET /actuator/health`
- **Product Service**: 8088 → `GET /actuator/health`
- **Cart Service**: 8089 → `GET /actuator/health`
- **Keycloak**: 8080 → Authentication Server

### Infrastructure Dependencies
- **PostgreSQL**: 5432 (Multi-schema: user, product, inventory, cart, order)
- **Redis**: 6379 (Cart sessions + inventory caching)
- **Kafka**: 9092 (Event streaming)
- **Eureka**: 8761 (Service discovery)

---

## 🎯 Phase 1: Individual Service Testing

### 1.1 User Service Testing (Port 8082)

#### Authentication Flow
```bash
# TC-US-01: Keycloak Token Request
POST http://localhost:8080/realms/ecommerce/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=ecommerce-app&username=testuser&password=testpass

Expected: 200 OK with JWT access_token
```

#### Profile Management
```bash
# TC-US-03: Get User Profile
GET http://localhost:8082/api/v1/users/profile
Authorization: Bearer <JWT_TOKEN>

Expected: 200 OK with user profile data

# TC-US-05: Update User Profile
PUT http://localhost:8082/api/v1/users/profile
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "firstName": "Updated Name",
  "email": "updated@test.com",
  "phone": "+1234567890"
}

Expected: 200 OK with updated profile
```

#### Address Management
```bash
# TC-US-06: Get User Addresses
GET http://localhost:8082/api/v1/users/addresses
Authorization: Bearer <JWT_TOKEN>

# TC-US-07: Add New Address
POST http://localhost:8082/api/v1/users/addresses
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "street": "123 Test Street",
  "city": "Test City",
  "state": "TS",
  "zipCode": "12345",
  "country": "USA",
  "isDefault": true
}

Expected: 201 Created with address ID
```

### 1.2 Product Service Testing (Port 8088)

#### Product Catalog
```bash
# TC-PS-01: Get Product List with Pagination
GET http://localhost:8088/api/v1/products?page=0&size=20&sort=name,asc

Expected: 200 OK with paginated product list

# TC-PS-02: Get Product by ID (Valid)
GET http://localhost:8088/api/v1/products/2148581

Expected: 200 OK with product details including Amazon images

# TC-PS-03: Get Product by ID (Invalid)
GET http://localhost:8088/api/v1/products/999999999

Expected: 404 Not Found

# TC-PS-04: Product Search
GET http://localhost:8088/api/v1/products/search?query=laptop&page=0&size=10

Expected: 200 OK with search results
```

#### Category Management
```bash
# TC-PS-05: Get Categories
GET http://localhost:8088/api/v1/categories

Expected: 200 OK with category tree

# TC-PS-06: Get Products by Category
GET http://localhost:8088/api/v1/products/category/1?page=0&size=20

Expected: 200 OK with category-filtered products
```

### 1.3 Cart Service Testing (Port 8089)

#### Cart Session Management
```bash
# TC-CS-01: Get Cart (Empty)
GET http://localhost:8089/api/v1/cart
Authorization: Bearer <JWT_TOKEN>

Expected: 200 OK with empty cart structure

# TC-CS-02: Add Item to Cart
POST http://localhost:8089/api/v1/cart/items
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "productId": 2148581,
  "quantity": 2,
  "unitPrice": 29.99
}

Expected: 201 Created with cart item details

# TC-CS-03: Update Cart Item Quantity
PUT http://localhost:8089/api/v1/cart/items/1
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "quantity": 5
}

Expected: 200 OK with updated quantity

# TC-CS-04: Remove Item from Cart
DELETE http://localhost:8089/api/v1/cart/items/1
Authorization: Bearer <JWT_TOKEN>

Expected: 204 No Content

# TC-CS-05: Clear Entire Cart
POST http://localhost:8089/api/v1/cart/clear
Authorization: Bearer <JWT_TOKEN>

Expected: 204 No Content
```

#### Cart Persistence Testing
```bash
# TC-CS-06: Redis Persistence Test
# 1. Add items to cart
# 2. Get new JWT token (simulate session refresh)
# 3. Verify cart items persist
GET http://localhost:8089/api/v1/cart
Authorization: Bearer <NEW_JWT_TOKEN>

Expected: 200 OK with previously added items
```

### 1.4 Inventory Service Testing (Port 8084)

#### Stock Management
```bash
# TC-IS-01: Check Product Stock
GET http://localhost:8084/api/v1/inventory/product/2148581

Expected: 200 OK with current stock quantity

# TC-IS-02: Get Low Stock Alerts
GET http://localhost:8084/api/v1/inventory/low-stock-alerts

Expected: 200 OK with list of products below threshold
```

#### Stock Reservation (Internal API)
```bash
# TC-IS-03: Reserve Stock (Internal)
POST http://localhost:8084/api/v1/inventory/reserve
Content-Type: application/json

{
  "orderId": "order-uuid-123",
  "userId": "user-123",
  "items": [
    {
      "productId": 2148581,
      "quantity": 2
    }
  ]
}

Expected: 200 OK with reservation confirmation

# TC-IS-04: Release Stock Reservation (Internal)
POST http://localhost:8084/api/v1/inventory/release
Content-Type: application/json

{
  "orderId": "order-uuid-123",
  "reservationId": "res-123"
}

Expected: 200 OK with release confirmation
```

#### Stock Movement History
```bash
# TC-IS-05: Get Stock Movements
GET http://localhost:8084/api/v1/inventory/movements/product/2148581

Expected: 200 OK with movement history (INBOUND/OUTBOUND/RESERVED/RELEASED)
```

### 1.5 Order Service Testing (Port 8083)

#### Order Creation Flow
```bash
# TC-OS-01: Create Order from Cart (Happy Path)
POST http://localhost:8083/api/v1/order-management
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "userId": "test-user-123",
  "cartId": "cart-uuid-456",
  "items": [
    {
      "productId": 2148581,
      "quantity": 2,
      "unitPrice": 29.99
    }
  ],
  "shippingAddress": {
    "street": "123 Test Street",
    "city": "Test City",
    "state": "TS",
    "zipCode": "12345",
    "country": "USA"
  },
  "paymentMethod": "CREDIT_CARD",
  "customerEmail": "test@example.com"
}

Expected: 201 Created with order details and status PENDING

# TC-OS-02: Get Order Details
GET http://localhost:8083/api/v1/orders/{orderId}
Authorization: Bearer <JWT_TOKEN>

Expected: 200 OK with complete order information

# TC-OS-03: Get User Orders
GET http://localhost:8083/api/v1/orders/user
Authorization: Bearer <JWT_TOKEN>

Expected: 200 OK with user's order history
```

#### Order Status Management
```bash
# TC-OS-04: Update Order Status (Admin)
PUT http://localhost:8083/api/v1/orders/{orderId}/status
Authorization: Bearer <ADMIN_JWT_TOKEN>
Content-Type: application/json

{
  "status": "SHIPPED",
  "trackingNumber": "TRACK123456"
}

Expected: 200 OK with updated order status
```

---

## 🔗 Phase 2: Service Integration Testing

### 2.1 Cart → Order Integration
```bash
# Test Flow: Cart items are correctly converted to order items
# 1. Add items to cart via Cart Service
# 2. Create order via Order Service
# 3. Verify cart is cleared after successful order
# 4. Verify order contains exact cart items

POST http://localhost:8089/api/v1/cart/items (Add items)
POST http://localhost:8083/api/v1/order-management (Create order)
GET http://localhost:8089/api/v1/cart (Verify cart cleared)
GET http://localhost:8083/api/v1/orders/{orderId} (Verify order items)
```

### 2.2 Order → Inventory Integration
```bash
# Test Flow: Inventory is properly reserved during order creation
# 1. Check initial inventory
# 2. Create order
# 3. Verify inventory reservation
# 4. Verify stock movement recorded

GET http://localhost:8084/api/v1/inventory/product/2148581 (Initial stock)
POST http://localhost:8083/api/v1/order-management (Create order)
GET http://localhost:8084/api/v1/inventory/product/2148581 (Check reserved stock)
GET http://localhost:8084/api/v1/inventory/movements/product/2148581 (Check movements)
```

### 2.3 Payment → Order → Inventory Integration
```bash
# Test Flow: Payment confirmation commits inventory
# 1. Create order (reserves inventory)
# 2. Payment processing (mocked)
# 3. Verify inventory committed (reserved stock becomes unavailable)
# 4. Verify order status updated to CONFIRMED

# This involves Kafka event testing - monitor events:
# - inventory.reservation.created
# - payment.processed
# - inventory.reservation.committed
# - order.status.updated
```

---

## 🎭 Phase 3: End-to-End Testing Scenarios

### 3.1 Complete Order Flow (Happy Path)
```bash
# E2E-01: Successful Order Placement
# Step 1: Authentication
POST http://localhost:8080/realms/ecommerce/protocol/openid-connect/token
# Extract: access_token

# Step 2: Browse Products
GET http://localhost:8088/api/v1/products?page=0&size=20

# Step 3: Add to Cart
POST http://localhost:8089/api/v1/cart/items
# Body: {"productId": 2148581, "quantity": 2, "unitPrice": 29.99}

# Step 4: Review Cart
GET http://localhost:8089/api/v1/cart

# Step 5: Create Order
POST http://localhost:8083/api/v1/order-management
# Body: Complete order request with cart items

# Step 6: Verify Order Created
GET http://localhost:8083/api/v1/orders/{orderId}

# Step 7: Check Inventory Reserved
GET http://localhost:8084/api/v1/inventory/product/2148581

# Step 8: Verify Cart Cleared
GET http://localhost:8089/api/v1/cart

Expected Results:
- Order status: PENDING → CONFIRMED
- Inventory: Reduced by ordered quantity
- Cart: Empty
- Stock movements: RESERVED → COMMITTED
```

### 3.2 Multiple Users Concurrent Shopping
```bash
# E2E-02: Concurrent User Testing
# Simulate 10 users simultaneously:
# 1. Authenticate
# 2. Add same product to cart
# 3. Attempt to order simultaneously
# 4. Verify inventory consistency
# 5. Some orders succeed, others fail with out-of-stock

Expected: Proper inventory control, no overselling
```

---

## 🚨 Phase 4: Error Handling & Rollback Testing

### 4.1 Payment Failure Rollback
```bash
# ERR-01: Payment Failure Scenario
# 1. Create order (inventory reserved)
# 2. Simulate payment failure
# 3. Verify inventory reservation released
# 4. Verify order status = FAILED
# 5. Verify cart NOT cleared (user can retry)

POST http://localhost:8083/api/v1/order-management
# Payment processing fails internally
GET http://localhost:8084/api/v1/inventory/product/2148581 (Stock restored)
GET http://localhost:8083/api/v1/orders/{orderId} (Status: FAILED)
GET http://localhost:8089/api/v1/cart (Items still present)
```

### 4.2 Insufficient Stock Handling
```bash
# ERR-02: Out of Stock Error
# 1. Reduce inventory to 1 item
# 2. Attempt to order 5 items
# 3. Verify order fails with appropriate error
# 4. Verify no inventory changes

POST http://localhost:8083/api/v1/order-management
# Body: quantity > available stock

Expected: 409 Conflict - Insufficient stock
```

### 4.3 Service Circuit Breaker Testing
```bash
# ERR-03: Service Unavailability
# 1. Stop Payment Service
# 2. Attempt order creation
# 3. Verify circuit breaker opens
# 4. Verify graceful error handling

Expected: 503 Service Unavailable after circuit breaker opens
```

---

## ⚡ Phase 5: Performance Testing

### 5.1 Load Testing Scenarios
```bash
# PERF-01: Browse Products Load Test
# Concurrent Users: 1000
# Duration: 10 minutes
# Endpoints: GET /api/v1/products, GET /api/v1/products/{id}
# Target: p95 < 500ms, Error rate < 0.1%

# PERF-02: Cart Operations Load Test
# Concurrent Users: 500
# Duration: 5 minutes
# Endpoints: POST/GET/PUT/DELETE /api/v1/cart/*
# Target: p95 < 300ms (Redis-cached)

# PERF-03: Order Creation Stress Test
# Concurrent Users: 200
# Duration: 2 minutes
# Endpoint: POST /api/v1/order-management
# Target: p95 < 1000ms, No inventory inconsistencies
```

### 5.2 Database Performance
```bash
# Monitor during load tests:
# - PostgreSQL connection pool usage
# - Query execution times
# - Redis cache hit rates
# - Kafka consumer lag
```

---

## 🔒 Phase 6: Security Testing

### 6.1 Authentication Testing
```bash
# SEC-01: No Token Access
GET http://localhost:8082/api/v1/users/profile
# (No Authorization header)
Expected: 401 Unauthorized

# SEC-02: Invalid Token
GET http://localhost:8082/api/v1/users/profile
Authorization: Bearer invalid-token
Expected: 401 Unauthorized

# SEC-03: Expired Token
GET http://localhost:8082/api/v1/users/profile
Authorization: Bearer <EXPIRED_TOKEN>
Expected: 401 Unauthorized
```

### 6.2 Authorization Testing
```bash
# SEC-04: Cross-User Data Access
# User A tries to access User B's order
GET http://localhost:8083/api/v1/orders/{userB-orderId}
Authorization: Bearer <USER_A_TOKEN>
Expected: 403 Forbidden

# SEC-05: Admin vs User Access
GET http://localhost:8083/api/v1/orders/admin/all
Authorization: Bearer <REGULAR_USER_TOKEN>
Expected: 403 Forbidden
```

---

## 📊 Test Data Management

### 6.1 Test Users
```json
{
  "users": [
    {
      "username": "testuser1",
      "password": "testpass123",
      "role": "USER",
      "email": "testuser1@example.com"
    },
    {
      "username": "adminuser",
      "password": "adminpass123",
      "role": "ADMIN",
      "email": "admin@example.com"
    }
  ]
}
```

### 6.2 Test Products
```json
{
  "products": [
    {
      "id": 2148581,
      "name": "Test Laptop",
      "price": 29.99,
      "stockQuantity": 100
    },
    {
      "id": 2148582,
      "name": "Test Phone",
      "price": 799.99,
      "stockQuantity": 5
    },
    {
      "id": 2148583,
      "name": "Out of Stock Item",
      "price": 199.99,
      "stockQuantity": 0
    }
  ]
}
```

---

## 🛠️ Test Automation Framework

### Postman/Newman Collection Structure
```
/collections
  /01-health-checks.json
  /02-authentication.json
  /03-user-service.json
  /04-product-service.json
  /05-cart-service.json
  /06-inventory-service.json
  /07-order-service.json
  /08-integration-tests.json
  /09-end-to-end-flows.json
  /10-error-scenarios.json
  /11-performance-tests.json
  /12-security-tests.json
```

### Test Execution Commands
```bash
# Individual service tests
newman run collections/03-user-service.json -e environment.json

# Complete test suite
newman run collections/*.json -e environment.json --reporters cli,html

# Performance testing with iterations
newman run collections/11-performance-tests.json -n 100 --timeout-request 10000
```

---

## 📈 Success Criteria

### Functional Testing
- ✅ All individual service endpoints respond correctly
- ✅ Service integrations work end-to-end  
- ✅ Error handling and rollbacks function properly
- ✅ Security controls prevent unauthorized access

### Performance Testing
- ✅ P95 response times under load targets
- ✅ No inventory inconsistencies under concurrent load
- ✅ Circuit breakers activate properly during outages
- ✅ System gracefully handles peak traffic

### Coverage Goals
- ✅ API Endpoint Coverage: 100%
- ✅ Integration Path Coverage: 95%
- ✅ Error Scenario Coverage: 90%
- ✅ Security Test Coverage: 100%

---

## 🚀 Ready to Execute

This enhanced testing plan provides:
1. **Specific API endpoints** with exact request/response examples
2. **Test data** tailored to your current system
3. **Executable scenarios** for automation
4. **Clear success criteria** for each test phase
5. **Practical rollback testing** for real-world scenarios

Let's start with Phase 1 - Individual Service Testing. Which service would you like to begin testing first?