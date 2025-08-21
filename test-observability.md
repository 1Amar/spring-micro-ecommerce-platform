# Observability Testing Guide

## Prerequisites
1. Start Docker infrastructure: `docker-compose -f Docker/docker-compose.yml up -d`
2. Build all services: `mvn clean install`
3. Start services in order: Eureka → Gateway → All other services

## Port Assignments
- Eureka Service Registry: 8761
- API Gateway: 8081  
- Order Service: 8083
- Inventory Service: 8084
- Product Service: 8085
- Payment Service: 8086
- Notification Service: 8087
- Notification Worker: 8088
- Catalog Service: 8089
- Search Service: 8090

## Infrastructure Services
- Prometheus: 9090
- Grafana: 3000
- Jaeger UI: 16686
- Kibana: 5601
- Elasticsearch: 9200

## Testing Steps

### 1. Health Checks
```bash
# Check all service health endpoints
curl http://localhost:8761/actuator/health  # Eureka
curl http://localhost:8081/actuator/health  # Gateway
curl http://localhost:8083/actuator/health  # Order Service
curl http://localhost:8084/actuator/health  # Inventory Service
curl http://localhost:8085/actuator/health  # Product Service
curl http://localhost:8086/actuator/health  # Payment Service
curl http://localhost:8087/actuator/health  # Notification Service
curl http://localhost:8088/actuator/health  # Notification Worker
curl http://localhost:8089/actuator/health  # Catalog Service
curl http://localhost:8090/actuator/health  # Search Service
```

### 2. Metrics Endpoints
```bash
# Check Prometheus metrics endpoints
curl http://localhost:8761/actuator/prometheus  # Eureka
curl http://localhost:8081/actuator/prometheus  # Gateway
curl http://localhost:8083/actuator/prometheus  # Order Service
# ... repeat for all services
```

### 3. Distributed Tracing Test
```bash
# Generate traced requests through the order simulation
curl -X POST http://localhost:8081/api/v1/order/simulate

# Check trace in Jaeger UI at http://localhost:16686
# Look for traces with service names and correlation IDs
```

### 4. Logging Test
```bash
# Generate log entries through service calls
curl -X POST http://localhost:8081/api/v1/order/simulate

# Check logs in Kibana at http://localhost:5601
# Look for structured logs with correlation IDs and service names
```

### 5. Prometheus Metrics Verification
Visit http://localhost:9090 and query:
- `http_server_requests_seconds_count` - HTTP request counts
- `jvm_memory_used_bytes` - JVM memory usage
- `eureka_server_registry_size` - Eureka registry size
- `gateway_requests_seconds_count` - Gateway request metrics

### 6. Service Discovery Test
```bash
# Check Eureka registry
curl http://localhost:8761/eureka/apps

# Verify all services are registered
```

### Expected Results
- ✅ All health endpoints return UP status
- ✅ Metrics endpoints return Prometheus-formatted data
- ✅ Traces appear in Jaeger with proper service correlation
- ✅ Structured logs appear in Kibana with correlation IDs
- ✅ Prometheus scrapes all service metrics
- ✅ All services registered in Eureka
- ✅ Order simulation creates end-to-end trace

## Troubleshooting

### Common Issues

**Database Connection Issues:**
```bash
# Check if PostgreSQL is running
docker-compose -f Docker/docker-compose.yml ps postgres

# Check database logs
docker-compose -f Docker/docker-compose.yml logs postgres

# Verify databases were created
docker exec postgres psql -U devuser -d ecommerce_dev -c "\l"

# Manually run database initialization if needed
docker exec postgres psql -U devuser -d ecommerce_dev -f /docker-entrypoint-initdb.d/init-db.sql
```

**Keycloak Issues:**
```bash
# Check Keycloak logs
docker-compose -f Docker/docker-compose.yml logs keycloak

# Check if keycloak database exists
docker exec postgres psql -U devuser -d ecommerce_dev -c "SELECT datname FROM pg_database WHERE datname='keycloak';"

# Restart just Keycloak after PostgreSQL is ready
docker-compose -f Docker/docker-compose.yml restart keycloak
```

**Service Connection Issues:**
- If services fail to start, check port conflicts
- If metrics aren't scraped, verify Prometheus targets
- If traces don't appear, check Zipkin endpoint configuration
- If logs aren't forwarded, verify Logstash connectivity

**Clean Restart:**
```bash
# Use the restart script for a clean restart
cd Docker
restart-infrastructure.cmd
```

### Service Dependencies
Start services in this order:
1. Infrastructure (PostgreSQL, Kafka, Elasticsearch, etc.)
2. Eureka Service Registry (8761)
3. API Gateway (8081)
4. All other microservices (8083-8090)