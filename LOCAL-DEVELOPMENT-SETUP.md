# Local Development Setup Guide

## Overview
This guide sets up the complete development environment using Windows PostgreSQL as the single database for all services.

## Architecture
```
Local Development:
├── Windows PostgreSQL (localhost:5432)
│   ├── microservices_ecom (database) - User/Product/Order data
│   └── keycloak (database) - Keycloak authentication data
├── Keycloak (Docker) - Authentication server
├── User Service (JAR) - User management
├── Product Service (JAR) - Product catalog (with 1.4M+ products)
└── Angular Frontend - Web application
```

## Prerequisites
- ✅ Windows PostgreSQL running on localhost:5432
- ✅ Java 17+ installed
- ✅ Maven installed
- ✅ Node.js and Angular CLI installed
- ✅ Docker Desktop installed

## Setup Steps

### Step 1: Setup PostgreSQL Databases
```sql
-- Run in your Windows PostgreSQL (as superuser):
psql -U postgres -d postgres -f Docker/setup-keycloak-db.sql
```

### Step 2: Start Keycloak
```bash
cd Docker
# Stop any existing Docker containers
docker-compose down
# Start only Keycloak
.\setup-keycloak-local.cmd
```

### Step 3: Import Keycloak Realm
1. Go to http://localhost:8080
2. Login: admin / admin
3. Click "Import" → Upload `keycloak-realm-export.json`
4. Verify realm "ecommerce-realm" is created

### Step 4: Start Microservices
```bash
# Start User Service
cd user-service
mvn spring-boot:run

# Start Product Service (in separate terminal)
cd product-service
mvn spring-boot:run

# Start API Gateway (in separate terminal)
cd ecom-api-gateway
mvn spring-boot:run
```

### Step 5: Start Angular Frontend
```bash
cd ecommerce-frontend
npm install
ng serve
```

### Step 6: Verify Setup
- Keycloak Admin: http://localhost:8080
- User Service: http://localhost:8082/api/v1/users/health
- Product Service: http://localhost:8088/api/v1/products/catalog/health
- API Gateway: http://localhost:8081/actuator/health
- Angular App: http://localhost:4200

## Testing Authentication Flow
1. Open http://localhost:4200
2. Click "Login" - should redirect to Keycloak
3. Login with: customer@ecommerce.com / customer123
4. Verify successful authentication and redirect back to Angular

## Database Connection Details

### User Service → Windows PostgreSQL
```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/microservices_ecom
  username: ecom_user
  password: ecom_pass
```

### Keycloak → Windows PostgreSQL
```yaml
KC_DB_URL: jdbc:postgresql://host.docker.internal:5432/keycloak
KC_DB_USERNAME: keycloak_user
KC_DB_PASSWORD: keycloak_pass
```

### Product Service → Windows PostgreSQL
```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/microservices_ecom
  username: ecom_user
  password: ecom_pass
```

## Troubleshooting

### Keycloak Connection Issues
```bash
# Check Keycloak logs
docker logs keycloak_local -f

# Test database connection
psql -U keycloak_user -d keycloak -h localhost -p 5432
```

### User Service Issues
```bash
# Check if user tables exist
psql -U ecom_user -d microservices_ecom -c "\dt"

# Test service health
curl http://localhost:8082/api/v1/users/health
```

### Angular Authentication Issues
1. Check Keycloak realm configuration
2. Verify CORS settings in Keycloak
3. Check browser console for errors
4. Verify environment.ts has correct Keycloak URLs

## Development Workflow
1. Make code changes
2. Restart affected service
3. Test via Angular frontend or direct API calls
4. Check logs for any issues

## Database Migrations
```bash
# Run user-related migrations
cd sql-migration
mvn liquibase:update
```

## Stopping Services
```bash
# Stop Docker containers
docker-compose -f docker-compose-keycloak-only.yml down

# Stop Spring Boot services (Ctrl+C in terminals)
# Keep PostgreSQL running
```