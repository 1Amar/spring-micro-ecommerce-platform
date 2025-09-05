# Complete Order Testing Flow Guide

## Overview
This guide provides step-by-step instructions for testing the complete e-commerce order flow from cart creation to order completion.

## Prerequisites
- All services running: cart-service (8089), product-service (8088), order-service (8083), inventory-service (8084), payment-service (8087)
- API Gateway running (8081)
- Eureka Server running (8761)
- Frontend running (4200)

## Backend Testing Flow (Direct API Calls)

### Step 1: Test Cart Creation and Product Addition

```bash
# Add product to cart (creates anonymous cart)
curl -X POST http://localhost:8089/api/v1/cart/items \
-H "Content-Type: application/json" \
-d '{
  "productId": 2148581,
  "quantity": 2
}'

# Expected Response: Cart created with product validated through product-service
# Note the cartId from response (format: cart:anon:uuid)
```

### Step 2: Test Complete Order Creation

```bash
# Create order with complete shipping/billing info
curl -X POST http://localhost:8083/api/v1/order-management \
-H "Content-Type: application/json" \
-d '{
  "userId": "test-user",
  "cartId": "cart:anon:YOUR_CART_ID_HERE",
  "items": [{"productId": 2148581, "quantity": 2, "unitPrice": 39.99}],
  "paymentMethod": "CREDIT_CARD",
  "customerEmail": "test@example.com",
  "shippingFirstName": "John",
  "shippingLastName": "Doe", 
  "shippingStreet": "123 Main St",
  "shippingCity": "New York",
  "shippingState": "NY",
  "shippingPostalCode": "10001",
  "shippingCountry": "USA",
  "billingFirstName": "John",
  "billingLastName": "Doe",
  "billingStreet": "123 Main St", 
  "billingCity": "New York",
  "billingState": "NY",
  "billingPostalCode": "10001",
  "billingCountry": "USA"
}'

# Expected Response: Order created with status "CONFIRMED" and payment "PAID"
```

### Step 3: Verify Order Details

```bash
# Get order by ID
curl http://localhost:8083/api/v1/orders/{ORDER_ID}

# Expected Response: Complete order details with all items and addresses
```

## Frontend Testing Flow (UI Integration)

### Step 1: Login to Frontend
1. Navigate to http://localhost:4200
2. Click "Login" 
3. Login with Keycloak credentials:
   - Email: customer@ecommerce.com
   - Password: customer123

### Step 2: Browse and Add Products
1. Navigate to Products page
2. Find a product (e.g., "Wireless Charger Samsung")
3. Click "Add to Cart"
4. **Expected**: Success message, cart counter updates
5. **Previous Issue**: 503 Service Unavailable ❌ 
6. **After Fix**: Should work perfectly ✅

### Step 3: Checkout Process
1. Click cart icon to view cart
2. Click "Checkout"
3. Fill in shipping information
4. Fill in billing information  
5. Select payment method
6. Click "Place Order"
7. **Expected**: Order confirmation page with order number

## API Gateway Testing Flow (Through Gateway)

### Step 1: Test Cart Through Gateway (Requires Authentication)

```bash
# This will return 401 without valid JWT token
curl -X POST http://localhost:8081/api/v1/cart/items \
-H "Content-Type: application/json" \
-H "Authorization: Bearer YOUR_JWT_TOKEN" \
-d '{"productId": 2148581, "quantity": 1}'
```

### Step 2: Test Order Through Gateway

```bash
# This will also return 401 without valid JWT token
curl -X POST http://localhost:8081/api/v1/order-management \
-H "Content-Type: application/json" \
-H "Authorization: Bearer YOUR_JWT_TOKEN" \
-d '{...complete order payload...}'
```

## Service Health Checks

Before testing, verify all services are healthy:

```bash
# Service health checks
curl http://localhost:8089/actuator/health  # Cart Service
curl http://localhost:8088/actuator/health  # Product Service  
curl http://localhost:8083/actuator/health  # Order Service
curl http://localhost:8084/actuator/health  # Inventory Service
curl http://localhost:8087/actuator/health  # Payment Service
curl http://localhost:8081/actuator/health  # API Gateway

# Eureka service registry
curl http://localhost:8761/eureka/apps | grep -E "CART-SERVICE|PRODUCT-SERVICE|ORDER-SERVICE"
```

## Test Data

### Valid Product IDs for Testing:
- 2148581 - "Wireless Charger Samsung with Clock"
- Use products from: `SELECT id FROM product_service_schema.products LIMIT 10`

### Required Order Fields:
- All shipping fields: firstName, lastName, street, city, state, postalCode, country
- All billing fields: firstName, lastName, street, city, state, postalCode, country  
- customerEmail, paymentMethod, userId, cartId, items[]

## Troubleshooting

### Common Issues:

1. **503 Service Unavailable on Cart Operations**
   - **Root Cause**: Missing @LoadBalanced annotation in cart-service WebClient
   - **Fix**: Add @LoadBalanced to WebClient.Builder in WebClientConfig
   - **Symptom**: "Product Service Unavailable" error

2. **400 Bad Request on Order Creation**
   - **Root Cause**: Missing required validation fields
   - **Fix**: Include all shipping/billing address fields
   - **Check Logs**: Look for validation error details

3. **401 Unauthorized Through API Gateway**  
   - **Root Cause**: Missing or invalid JWT token
   - **Fix**: Ensure frontend sends valid Keycloak token
   - **Note**: Direct service calls bypass authentication

4. **Service Discovery Failures**
   - **Root Cause**: Services not registered in Eureka or wrong service names
   - **Fix**: Verify Eureka registration and use correct service names
   - **Check**: Use http://service-name instead of localhost:port

## Success Indicators

✅ **Cart Service**: Returns cart with validated product information  
✅ **Product Validation**: Product name and details populated in cart  
✅ **Order Service**: Returns order with CONFIRMED status and PAID payment  
✅ **Frontend Integration**: Add to cart works without 503 errors  
✅ **Service Discovery**: All inter-service calls work via service names  

## Performance Benchmarks

- Cart Operations: < 100ms
- Product Validation: < 500ms (first call), < 50ms (cached)
- Order Creation: < 2000ms (includes payment processing)
- Frontend Add to Cart: < 1000ms end-to-end