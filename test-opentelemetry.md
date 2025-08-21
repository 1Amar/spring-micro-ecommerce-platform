# OpenTelemetry Testing Guide

## Overview
This guide explains how to test the newly implemented OpenTelemetry tracing in your Spring Boot microservices platform.

## What's Been Implemented

### 1. OpenTelemetry Dependencies
- Replaced Zipkin dependencies with OpenTelemetry in `common-library/pom.xml`
- Added `micrometer-tracing-bridge-otel`
- Added `opentelemetry-exporter-otlp`
- Added `opentelemetry-spring-boot-starter`

### 2. Configuration Updates
- Updated `application-observability.yml` with OTLP configuration
- Configured endpoint: `http://localhost:4318` (OTLP HTTP)
- Added resource attributes for service identification

### 3. Custom Configuration Classes
- `OpenTelemetryConfig.java`: Manual OpenTelemetry SDK configuration
- `TracingConfiguration.java`: HTTP request tracing with correlation IDs

### 4. Infrastructure Updates
- Enhanced Jaeger configuration in `docker-compose.yml`
- Added health checks for Jaeger
- Enabled OTLP receivers on ports 4317 (gRPC) and 4318 (HTTP)

## Testing Steps

### 1. Start Infrastructure
```bash
cd Docker
docker-compose up -d
```

### 2. Verify Jaeger is Running
```bash
# Check Jaeger UI
curl http://localhost:16686/

# Check OTLP endpoints
curl http://localhost:4318/v1/traces -X POST -H "Content-Type: application/json" -d '{}'
```

### 3. Build and Start Services
```bash
# Build all services with new OpenTelemetry dependencies
mvn clean install

# Start services in order
cd eureka-service-registry && mvn spring-boot:run &
cd ecom-api-gateway && mvn spring-boot:run &
cd product-service && mvn spring-boot:run &
cd inventory-service && mvn spring-boot:run &
cd ecom-order-service && mvn spring-boot:run &
cd payment-service && mvn spring-boot:run &
cd notification-service && mvn spring-boot:run &
```

### 4. Test Order Simulation
```bash
# Trigger order simulation to generate traces
curl -X POST http://localhost:8081/api/v1/order/simulate

# Check logs for correlation IDs
curl http://localhost:8081/actuator/health
```

### 5. Verify Traces in Jaeger
1. Open Jaeger UI: http://localhost:16686
2. Select a service from dropdown (e.g., ecom-api-gateway)
3. Click "Find Traces"
4. Look for traces with correlation IDs
5. Verify span hierarchy: Gateway → Inventory → Product → Order → Payment → Notification

## Expected Results

### Trace Structure
```
ecom-api-gateway (entry point)
├── inventory-service (check stock)
├── product-service (get product details)
├── ecom-order-service (create order)
├── payment-service (process payment)
└── notification-service (send notifications)
```

### Span Attributes
- `service.name`: Service identifier
- `service.version`: 1.0.0
- `http.method`: HTTP method (GET, POST, etc.)
- `http.url`: Full request URL
- `correlation.id`: UUID for request correlation
- `deployment.environment`: dev

### Correlation ID Flow
- Generated at gateway level
- Propagated through all services
- Visible in logs and traces
- Returned in HTTP response headers

## Troubleshooting

### 1. No Traces Appearing
- Check service logs for OpenTelemetry initialization
- Verify OTLP endpoint connectivity: `telnet localhost 4318`
- Check Jaeger logs: `docker logs jaeger`

### 2. Missing Dependencies
- Run `mvn dependency:tree` to verify OpenTelemetry libraries
- Check for conflicting Zipkin dependencies

### 3. Configuration Issues
- Verify `application-observability.yml` is included in service configs
- Check Spring profiles are active
- Verify environment variables

## Benefits of OpenTelemetry

### Over Zipkin
- Vendor-neutral standard
- Better performance and reliability
- Rich semantic conventions
- Broader ecosystem support
- Future-proof observability

### Features
- Automatic instrumentation for Spring Boot
- Correlation ID propagation
- Custom span attributes
- Multiple export formats (OTLP, Jaeger, Zipkin)
- Metrics and logs correlation (future)

## Next Steps

1. Add custom business metrics
2. Implement OpenTelemetry metrics collection
3. Add log correlation with traces
4. Create Grafana dashboards for trace metrics
5. Set up sampling strategies for production