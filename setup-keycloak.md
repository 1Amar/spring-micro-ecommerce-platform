# Keycloak Setup Guide for E-Commerce Platform

## Overview
This guide will configure Keycloak authentication for the Spring Boot microservices and Angular frontend.

## Prerequisites
- Keycloak running at http://localhost:8080
- Admin credentials: admin/admin (from docker-compose.yml)

## Step 1: Access Keycloak Admin Console

1. Open browser: http://localhost:8080/admin/
2. Login with: `admin` / `admin`
3. You should see the Keycloak Administration Console

## Step 2: Create E-Commerce Realm

1. **Create New Realm**:
   - Click "Add Realm" button
   - Name: `ecommerce-realm`
   - Display Name: `E-Commerce Platform`
   - Click "Create"

2. **Configure Realm Settings**:
   - Go to Realm Settings → General
   - User-managed access: Enabled
   - Registration allowed: Enabled (for testing)
   - Email as username: Enabled
   - Login with email: Enabled

## Step 3: Create Client for Angular Frontend

1. **Create Client**:
   - Go to Clients → Create
   - Client ID: `ecommerce-frontend`
   - Client Protocol: `openid-connect`
   - Root URL: `http://localhost:4200`
   - Click "Save"

2. **Configure Client Settings**:
   - Access Type: `public`
   - Standard Flow Enabled: `true`
   - Direct Access Grants Enabled: `true`
   - Valid Redirect URIs: `http://localhost:4200/*`
   - Web Origins: `http://localhost:4200`
   - Admin URL: `http://localhost:4200`

## Step 4: Create Client for Backend Services

1. **Create Service Client**:
   - Go to Clients → Create
   - Client ID: `ecommerce-backend`
   - Client Protocol: `openid-connect`
   - Access Type: `confidential`
   - Service Accounts Enabled: `true`
   - Authorization Enabled: `true`

## Step 5: Create Roles

1. **Create Realm Roles**:
   - Go to Roles → Add Role
   - Create these roles:
     - `customer` (Default role for regular users)
     - `admin` (Full administrative access)
     - `manager` (Business management access)
     - `support` (Customer support access)

2. **Set Default Role**:
   - Go to Roles → Default Roles
   - Add `customer` to Default Roles

## Step 6: Create Test Users

### Admin User
1. Go to Users → Add User
2. Username: `admin@ecommerce.com`
3. Email: `admin@ecommerce.com`
4. First Name: `Admin`
5. Last Name: `User`
6. Email Verified: `true`
7. Save → Credentials → Set Password: `admin123`
8. Role Mappings → Assign `admin` role

### Customer User
1. Go to Users → Add User
2. Username: `customer@ecommerce.com`
3. Email: `customer@ecommerce.com`
4. First Name: `John`
5. Last Name: `Customer`
6. Email Verified: `true`
7. Save → Credentials → Set Password: `customer123`
8. Role Mappings → `customer` role (should be default)

### Manager User
1. Go to Users → Add User
2. Username: `manager@ecommerce.com`
3. Email: `manager@ecommerce.com`
4. First Name: `Store`
5. Last Name: `Manager`
6. Email Verified: `true`
7. Save → Credentials → Set Password: `manager123`
8. Role Mappings → Assign `manager` role

## Step 7: Configure Client Scopes

1. **Create Custom Scopes**:
   - Go to Client Scopes → Create
   - Name: `ecommerce-scope`
   - Protocol: `openid-connect`
   - Include in Token Scope: `true`

2. **Add to Frontend Client**:
   - Go to Clients → ecommerce-frontend → Client Scopes
   - Add `ecommerce-scope` to Default Client Scopes

## Step 8: Configure Frontend Client Advanced Settings

1. **Authentication Flow Overrides**:
   - Browser Flow: `browser`
   - Direct Grant Flow: `direct grant`

2. **Advanced Settings**:
   - Proof Key for Code Exchange Code Challenge Method: `S256`
   - Access Token Lifespan: `15 minutes`
   - Client Session Idle: `30 minutes`
   - Client Session Max: `12 hours`

## Step 9: Test Configuration

### Test with curl
```bash
# Get access token for admin user
curl -X POST http://localhost:8080/realms/ecommerce-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin@ecommerce.com" \
  -d "password=admin123" \
  -d "grant_type=password" \
  -d "client_id=ecommerce-frontend"
```

### Test with Angular App
1. Start Angular app: `npm start`
2. Visit: http://localhost:4200
3. Should redirect to Keycloak login
4. Login with test credentials
5. Should redirect back to app authenticated

## Backend Service Integration

### Spring Boot Configuration
Add to each service's `application.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/ecommerce-realm
          jwk-set-uri: http://localhost:8080/realms/ecommerce-realm/protocol/openid-connect/certs

keycloak:
  auth-server-url: http://localhost:8080
  realm: ecommerce-realm
  resource: ecommerce-backend
  credentials:
    secret: [GET_FROM_CLIENT_CREDENTIALS_TAB]
```

## Troubleshooting

### Common Issues
1. **CORS Errors**: Ensure Web Origins is set to `http://localhost:4200`
2. **Redirect Issues**: Check Valid Redirect URIs includes `http://localhost:4200/*`
3. **Token Issues**: Verify client configuration and user roles

### Useful Endpoints
- Realm Info: http://localhost:8080/realms/ecommerce-realm
- OpenID Config: http://localhost:8080/realms/ecommerce-realm/.well-known/openid_configuration
- JWK Keys: http://localhost:8080/realms/ecommerce-realm/protocol/openid-connect/certs

## Security Considerations

### Production Settings
- Change default admin password
- Use HTTPS for all communications
- Configure proper CORS policies
- Set shorter token lifespans
- Enable session management
- Configure rate limiting

### Role-Based Access Control
- `/admin/**` - admin role required
- `/manager/**` - manager or admin role required
- `/api/profile/**` - customer, manager, or admin role required
- `/api/orders/**` - authenticated users only
- `/api/public/**` - public access

This completes the Keycloak setup for your e-commerce platform!