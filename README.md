# Spring Boot Microservices E-Commerce Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-16-red.svg)](https://angular.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://docker.com/)
[![Keycloak](https://img.shields.io/badge/Keycloak-OAuth2-orange.svg)](https://keycloak.org/)

## 🚀 Project Overview

A comprehensive microservices-based e-commerce platform featuring:
- **Backend**: 10 Spring Boot microservices with Spring Cloud
- **Frontend**: Modern Angular 16 application with Material Design
- **Authentication**: Keycloak OAuth2 with PKCE flow
- **Observability**: Complete ELK stack + Prometheus + Grafana + OpenTelemetry
- **Infrastructure**: Docker Compose with full development environment

## ✅ Current Status: **Production-Ready Core Features**

### 🏗️ Architecture Complete
- ✅ **Service Discovery**: Eureka server with all services registered
- ✅ **API Gateway**: Spring Cloud Gateway with routing, circuit breakers, rate limiting
- ✅ **Authentication**: Keycloak OAuth2 + JWT + Role-based access control
- ✅ **Database**: PostgreSQL with proper schemas and multi-tenancy
- ✅ **Observability**: Full ELK + Prometheus + Grafana + OpenTelemetry tracing
- ✅ **Security**: CORS, JWT validation, role-based authorization

### 📊 Microservices (All Implemented)
1. **eureka-service-registry** (8761) - Service discovery
2. **ecom-api-gateway** (8081) - API Gateway with security
3. **ecom-order-service** (8083) - Order management with correlation tracking
4. **inventory-service** (8084) - Inventory tracking and management
5. **product-service** (8085) - Product catalog and categories
6. **payment-service** (8086) - Payment processing simulation
7. **notification-service** (8087) - User notifications and alerts
8. **catalog-service** (8089) - Category and search management
9. **search-service** (8090) - Advanced search functionality
10. **common-library** - Shared utilities and observability components

### 🌐 Frontend Application (Angular 16)
- ✅ **Authentication**: Complete Keycloak integration with OAuth2 + PKCE
- ✅ **Responsive UI**: Material Design + Bootstrap with mobile support
- ✅ **API Integration**: Type-safe services for all backend microservices
- ✅ **Error Handling**: HTTP interceptors with retry logic and error states
- ✅ **ELK Testing**: Built-in correlation ID testing interface
- 🚧 **Product Catalog**: Structure ready for implementation
- 🚧 **Shopping Cart**: API services ready for cart management
- 🚧 **Checkout Flow**: Authentication-protected checkout process

### 🔍 Observability Stack
- **Logging**: ELK stack with correlation ID tracking across all services
- **Metrics**: Prometheus collection with Grafana dashboards
- **Tracing**: OpenTelemetry OTLP export to Jaeger for distributed tracing
- **Health Monitoring**: Actuator endpoints with custom health indicators
- **Testing Interface**: Angular component for generating correlation-tracked requests

## 🛠️ Quick Start

### Prerequisites
- Java 17+ and Maven 3.8+
- Node.js 18+ and Angular CLI
- Docker and Docker Compose
- Git

### 1. Infrastructure Setup
```bash
# Start all infrastructure services
cd Docker
docker-compose up -d

# Verify services are running
curl http://localhost:9200    # Elasticsearch
curl http://localhost:5601    # Kibana  
curl http://localhost:9090    # Prometheus
curl http://localhost:3000    # Grafana (admin/admin)
```

### 2. Backend Services
```bash
# Build all services
mvn clean install

# Start services in order (use separate terminals or background)
cd eureka-service-registry && mvn spring-boot:run
cd ecom-api-gateway && mvn spring-boot:run
cd inventory-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd ecom-order-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run

# Verify services
curl http://localhost:8761    # Eureka Dashboard
curl http://localhost:8081/actuator/health    # Gateway Health
```

### 3. Frontend Application
```bash
cd ecommerce-frontend

# Install dependencies (first time)
npm install

# Start development server
ng serve --port 4201

# Access application
open http://localhost:4201
```

### 4. Test End-to-End Flow
```bash
# Test order simulation with correlation tracking
curl -X POST http://localhost:8081/api/v1/order/simulate

# Test ELK logging pipeline
curl -s "http://localhost:8081/api/v1/public/health" \
  -H "X-Correlation-ID: TEST-$(date +%s)"

# View logs in Kibana: http://localhost:5601
# Search for correlation ID: TEST-*
```

## 🧪 Testing & Monitoring

### ELK Logging Testing
The platform includes a comprehensive ELK testing interface:

1. **Angular ELK Test Page**: http://localhost:4201/elk-test
2. **Manual Testing**: See [ELK-TESTING-GUIDE.md](./ELK-TESTING-GUIDE.md)
3. **Correlation Tracking**: Every request gets a unique correlation ID
4. **Multi-Service Tracing**: Track requests across all microservices

### Health Monitoring
```bash
# Service Health
curl http://localhost:8081/actuator/health

# Metrics
curl http://localhost:8081/actuator/prometheus

# Distributed Tracing
open http://localhost:16686  # Jaeger UI

# Log Analysis  
open http://localhost:5601   # Kibana
```

### Security Testing
```bash
# Public endpoints (no auth required)
curl http://localhost:8081/api/v1/public/health

# Protected endpoints (JWT required)
curl http://localhost:8081/api/v1/test/roles  # Should return 401

# Role-based endpoints
curl http://localhost:8081/api/v1/admin/users  # Admin only
curl http://localhost:8081/api/v1/manager/reports  # Manager or Admin
```

## 📁 Project Structure

```
spring-micro-ecommerce-platform/
├── Docker/                     # Infrastructure setup
├── ecommerce-frontend/         # Angular 16 application  
├── eureka-service-registry/    # Service discovery
├── ecom-api-gateway/          # API Gateway with security
├── ecom-order-service/        # Order management
├── inventory-service/         # Inventory tracking
├── product-service/           # Product catalog
├── payment-service/           # Payment processing
├── notification-service/      # Notifications
├── catalog-service/           # Category management
├── search-service/            # Search functionality
├── common-library/            # Shared components
├── CLAUDE.md                  # Detailed project documentation
├── ELK-TESTING-GUIDE.md      # ELK testing procedures
└── README.md                  # This file
```

## 🔧 Configuration

### Environment Variables
```bash
# Database
export POSTGRES_URL=jdbc:postgresql://localhost:5432/ecommerce
export POSTGRES_USER=ecom_user
export POSTGRES_PASSWORD=ecom_password

# Keycloak
export KEYCLOAK_URL=http://localhost:8080
export KEYCLOAK_REALM=ecommerce
export KEYCLOAK_CLIENT_ID=ecommerce-app

# Observability
export ELASTICSEARCH_URL=http://localhost:9200
export JAEGER_ENDPOINT=http://localhost:14268/api/traces
```

### Key Configuration Files
- `Docker/docker-compose.yml` - Infrastructure services
- `ecom-api-gateway/src/main/resources/application.yml` - Gateway routing
- `ecommerce-frontend/src/environments/` - Angular environment config
- `*/src/main/resources/logback-spring.xml` - ELK logging configuration

## 🚧 Development Roadmap

### Immediate Priorities
- [ ] Complete product catalog frontend implementation
- [ ] Implement shopping cart functionality with persistence
- [ ] Build checkout flow with payment integration
- [ ] Add user profile management pages
- [ ] Implement order history and tracking

### Future Enhancements
- [ ] Real payment gateway integration (Stripe/PayPal)
- [ ] Advanced search with Elasticsearch
- [ ] Product recommendations engine
- [ ] Admin dashboard for business metrics
- [ ] Mobile app with React Native
- [ ] Kubernetes deployment manifests

## 🤝 Contributing

### Development Workflow
1. Start infrastructure: `cd Docker && docker-compose up -d`
2. Start backend services in dependency order
3. Start frontend: `cd ecommerce-frontend && ng serve --port 4201`
4. Make changes and test with ELK logging interface
5. Use correlation IDs for debugging across services

### Code Standards
- **Java**: Spring Boot best practices with proper logging
- **TypeScript**: Angular style guide compliance with strict typing
- **API**: RESTful design with proper HTTP status codes
- **Security**: OAuth2 + JWT with role-based access control
- **Observability**: Correlation ID tracking for all requests

## 📄 Documentation

- **[CLAUDE.md](./CLAUDE.md)** - Complete technical documentation and architecture
- **[ELK-TESTING-GUIDE.md](./ELK-TESTING-GUIDE.md)** - Comprehensive ELK testing procedures
- **Swagger UI**: http://localhost:8081/swagger-ui.html (can be added)
- **API Documentation**: Available through Spring Boot Actuator endpoints

## 🐳 Docker Deployment

```bash
# Build and run entire stack
docker-compose up --build

# Scale specific services
docker-compose up --scale product-service=2

# Production deployment (coming soon)
docker-compose -f docker-compose.prod.yml up
```

## 📊 Monitoring URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Angular App | http://localhost:4201 | Login via Keycloak |
| Keycloak Admin | http://localhost:8080 | admin/admin |
| Eureka Dashboard | http://localhost:8761 | No auth |
| API Gateway | http://localhost:8081 | Protected endpoints |
| Kibana | http://localhost:5601 | No auth |
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | No auth |
| Jaeger | http://localhost:16686 | No auth |

---

## 📝 Recent Updates (2025-08-19)

✅ **Major Accomplishments**
- Complete ELK logging stack with correlation ID tracking
- Angular ELK testing interface for interactive log generation
- OpenTelemetry distributed tracing implementation
- Full OAuth2 + JWT security with role-based access control
- All frontend console errors resolved and APIs working
- Production-ready observability stack

🎯 **Ready for Feature Development**
- All core infrastructure and security features complete
- Backend microservices fully operational with end-to-end testing
- Frontend architecture ready with API integration
- Comprehensive documentation and testing guides available

**Contributors**: Development by Claude AI Assistant  
**License**: MIT (add license file as needed)  
**Last Updated**: 2025-08-19
