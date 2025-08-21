# Role-Based Access Control Testing Guide

## Overview
This guide demonstrates how to test role-based access control in your API Gateway with Keycloak JWT tokens.

## Step 1: Enable Direct Access Grants in Keycloak

1. **Go to Keycloak Admin Console**: http://localhost:8080/admin/
2. **Navigate to**: Clients → ecommerce-frontend → Settings
3. **Enable**: Direct Access Grants Enabled = ON
4. **Save** the configuration

## Step 2: Get JWT Tokens for Different Users

### Admin User Token
```bash
curl -X POST http://localhost:8080/realms/ecommerce-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin@ecommerce.com" \
  -d "password=admin123" \
  -d "grant_type=password" \
  -d "client_id=ecommerce-frontend"
```

### Customer User Token
```bash
curl -X POST http://localhost:8080/realms/ecommerce-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=customer@ecommerce.com" \
  -d "password=customer123" \
  -d "grant_type=password" \
  -d "client_id=ecommerce-frontend"
```

### Manager User Token
```bash
curl -X POST http://localhost:8080/realms/ecommerce-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=manager@ecommerce.com" \
  -d "password=manager123" \
  -d "grant_type=password" \
  -d "client_id=ecommerce-frontend"
```

## Step 3: Test Role-Based Access

### Test Admin Endpoints

#### With Admin Token (Should Work)
```bash
# Extract access_token from the response above
ADMIN_TOKEN="<admin_access_token>"

curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8081/api/v1/admin/status
```

#### With Customer Token (Should Fail - 403 Forbidden)
```bash
CUSTOMER_TOKEN="<customer_access_token>"

curl -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8081/api/v1/admin/status
```

### Test Manager Endpoints

#### With Manager Token (Should Work)
```bash
MANAGER_TOKEN="<manager_access_token>"

curl -H "Authorization: Bearer $MANAGER_TOKEN" \
  http://localhost:8081/api/v1/manager/reports
```

#### With Customer Token (Should Fail - 403 Forbidden)
```bash
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8081/api/v1/manager/reports
```

### Test General Authenticated Endpoints

#### With Any Valid Token (Should Work)
```bash
# Test user info endpoint
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8081/api/v1/auth/me

curl -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8081/api/v1/auth/me

curl -H "Authorization: Bearer $MANAGER_TOKEN" \
  http://localhost:8081/api/v1/auth/me
```

## Step 4: Verify JWT Token Claims

You can decode the JWT token to verify the roles:

```bash
# Install jq if not available
# Extract the payload (middle part of JWT)
echo "$ADMIN_TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .

# Look for:
# - "preferred_username": "admin@ecommerce.com"
# - "realm_access": {"roles": ["admin", "customer"]}
# - "email": "admin@ecommerce.com"
```

## Expected Results

### ✅ Admin User (admin@ecommerce.com)
- `/api/v1/admin/*` → 200 OK
- `/api/v1/manager/*` → 200 OK (admin has manager access too)
- `/api/v1/auth/*` → 200 OK
- Public endpoints → 200 OK

### ✅ Manager User (manager@ecommerce.com)  
- `/api/v1/admin/*` → 403 Forbidden
- `/api/v1/manager/*` → 200 OK
- `/api/v1/auth/*` → 200 OK
- Public endpoints → 200 OK

### ✅ Customer User (customer@ecommerce.com)
- `/api/v1/admin/*` → 403 Forbidden
- `/api/v1/manager/*` → 403 Forbidden  
- `/api/v1/auth/*` → 200 OK
- Public endpoints → 200 OK

### ❌ No Token
- `/api/v1/admin/*` → 401 Unauthorized
- `/api/v1/manager/*` → 401 Unauthorized
- `/api/v1/auth/*` → 401 Unauthorized  
- Public endpoints → 200 OK

## Troubleshooting

### Token Decoding Issues
If you can't decode the token, use online JWT decoders like jwt.io

### Role Mapping Issues
Check that the JWT contains `realm_access.roles` claim with the correct roles

### 403 vs 401 Errors
- **401 Unauthorized**: No token or invalid token
- **403 Forbidden**: Valid token but insufficient privileges

## Rate Limiting Test

Test rate limiting by making multiple requests quickly:

```bash
for i in {1..25}; do
  curl -H "Authorization: Bearer $ADMIN_TOKEN" \
    http://localhost:8081/api/v1/auth/me
  echo "Request $i"
done
```

After 20 requests in quick succession, you should see rate limiting (429 Too Many Requests).

## Circuit Breaker Test

If backend services are down, you should see circuit breaker responses with fallback messages.