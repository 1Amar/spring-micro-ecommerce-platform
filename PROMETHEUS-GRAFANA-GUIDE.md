# Prometheus & Grafana Implementation Guide

## Overview
This guide covers the complete Prometheus metrics collection and Grafana visualization setup for the Spring Boot microservices platform.

## Architecture
- **Metrics Collection**: Spring Boot Actuator + Micrometer (native approach)
- **Metrics Storage**: Prometheus
- **Visualization**: Grafana dashboards
- **No OpenTelemetry Agent**: Using lighter-weight native Spring Boot integration

## Current Implementation Status

### ✅ Completed
1. **Prometheus Configuration** - All services configured for scraping
2. **Service Metrics Endpoints** - `/actuator/prometheus` exposed on all services
3. **Grafana Dashboard** - Comprehensive microservices overview
4. **Health Checks** - Service health monitoring
5. **Custom Metrics Tags** - Application and environment tagging

### 🔧 Services Configured
- **eureka-service-registry** (8761)
- **ecom-api-gateway** (8081) 
- **ecom-order-service** (8083)
- **inventory-service** (8084)
- **product-service** (8085)
- **payment-service** (8086)
- **notification-service** (8087)
- **catalog-service** (8089)
- **search-service** (8090)

## Quick Start

### 1. Start Infrastructure
```bash
cd Docker
docker-compose up -d
```

### 2. Start Services
```bash
# Start Eureka first
cd eureka-service-registry && mvn spring-boot:run

# Start API Gateway
cd ecom-api-gateway && mvn spring-boot:run

# Start other services
cd inventory-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
# ... etc
```

### 3. Setup Grafana (Run Once)
```bash
cd Docker
setup-grafana.cmd
```

### 4. Access Dashboards
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## Available Metrics

### System Metrics
- **CPU Usage**: `system_cpu_usage`
- **Memory**: `jvm_memory_used_bytes`, `jvm_memory_max_bytes`
- **Garbage Collection**: `jvm_gc_pause_seconds`
- **Thread Count**: `jvm_threads_live_threads`

### HTTP Metrics  
- **Request Rate**: `http_server_requests_seconds_count`
- **Response Time**: `http_server_requests_seconds` (histogram)
- **Status Codes**: Tagged by status (200, 404, 500, etc.)
- **Endpoints**: Tagged by URI and method

### Custom Business Metrics
Ready to add:
- Order processing metrics
- Inventory levels
- Payment success/failure rates
- User activity metrics

## Testing Metrics Collection

### 1. Check Prometheus Targets
```bash
# Check target health
curl http://localhost:9090/api/v1/targets

# Check specific service metrics
curl http://localhost:8081/actuator/prometheus  # API Gateway
curl http://localhost:8084/actuator/prometheus  # Inventory Service
```

### 2. Generate Traffic
```bash
# Simulate order flow
curl -X POST http://localhost:8081/api/v1/order/simulate

# Check inventory
curl http://localhost:8081/api/v1/inventory/products

# Test authentication endpoints
curl http://localhost:8081/api/v1/test/roles
```

### 3. Verify in Grafana
1. Go to http://localhost:3000
2. Navigate to "Spring Boot Microservices Overview" dashboard
3. Verify metrics are displaying correctly

## Dashboard Panels

### Service Health Status
- Shows UP/DOWN status for all services
- Real-time service availability monitoring

### HTTP Request Rate
- Requests per second by service
- Breakdown by HTTP method and endpoint

### Response Times
- 95th percentile response times
- Performance monitoring across services

### JVM Metrics
- Heap memory usage and limits
- Garbage collection frequency
- Thread usage

### System Metrics  
- CPU utilization
- System load

## Advanced Configuration

### Adding Custom Metrics
Add to service classes:
```java
@Component
public class OrderMetrics {
    private final Counter orderCounter = Counter.builder("orders_total")
        .description("Total number of orders")
        .tag("status", "created")
        .register(Metrics.globalRegistry);
        
    public void incrementOrderCount() {
        orderCounter.increment();
    }
}
```

### Alert Rules (Future Enhancement)
Create Prometheus alerting rules:
```yaml
# alerts.yml
groups:
  - name: microservices
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"
```

## Troubleshooting

### Services Not Appearing in Prometheus
1. Check service is running: `curl http://localhost:8084/actuator/health`
2. Check metrics endpoint: `curl http://localhost:8084/actuator/prometheus`
3. Verify Prometheus config in `Docker/prometheus.yml`
4. Check Prometheus logs: `docker logs prometheus`

### No Metrics in Grafana
1. Verify Prometheus data source configured
2. Check Prometheus is collecting metrics: http://localhost:9090/targets
3. Verify dashboard queries are correct
4. Check time range in Grafana dashboard

### Common Issues
- **Port conflicts**: Ensure services are running on correct ports
- **Docker networking**: Services use `host.docker.internal` to reach host services
- **Firewall**: Ensure ports 3000, 9090, and service ports are accessible

## Performance Impact

### Resource Usage (Micrometer vs OpenTelemetry Agent)
- **CPU Overhead**: <2% vs 5-10% with agent
- **Memory**: ~10MB vs 50-100MB with agent  
- **Network**: Minimal HTTP scraping vs constant OTLP export

### Recommended Settings
```yaml
management:
  metrics:
    export:
      prometheus:
        step: 30s  # Scrape interval
        descriptions: false  # Reduce payload size
```

## Next Steps

### Immediate
1. ✅ Test current dashboard with running services
2. 🔄 Add custom business metrics
3. 🔄 Create service-specific dashboards

### Future Enhancements
1. **Alerting**: Prometheus AlertManager integration
2. **Custom Dashboards**: Service-specific monitoring
3. **Distributed Tracing**: Correlate metrics with traces
4. **Log Aggregation**: ELK stack integration with metrics

---

## Links
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Eureka**: http://localhost:8761
- **API Gateway**: http://localhost:8081

## Metrics Endpoints
- Gateway: http://localhost:8081/actuator/prometheus
- Order Service: http://localhost:8083/actuator/prometheus  
- Inventory: http://localhost:8084/actuator/prometheus
- Products: http://localhost:8085/actuator/prometheus
- Payments: http://localhost:8086/actuator/prometheus