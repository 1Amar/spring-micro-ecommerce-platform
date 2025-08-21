# ELK Stack Testing Guide

## Overview
Complete guide for testing the ELK (Elasticsearch, Logstash, Kibana) logging stack with correlation ID tracking across microservices.

## Prerequisites

### Infrastructure Requirements
- Docker and Docker Compose running
- All infrastructure services started via `docker-compose up -d`
- Backend microservices running (Eureka, Gateway, and at least one service)
- Angular frontend running on port 4200 or 4201

### Required Services
```bash
# Infrastructure (Docker)
- Elasticsearch (9200)
- Logstash (5000)
- Kibana (5601)
- PostgreSQL, Redis, Kafka, etc.

# Backend Services  
- Eureka Service Registry (8761)
- API Gateway (8081)
- Any microservice (8083-8090)
```

## Backend ELK Testing

### 1. Manual Correlation ID Testing
```bash
# Test public health endpoint with custom correlation ID
curl -s "http://localhost:8081/api/v1/public/health" \
  -H "X-Correlation-ID: MANUAL-TEST-$(date +%s)"

# Test order simulation with correlation tracking
curl -X POST "http://localhost:8081/api/v1/order/simulate" \
  -H "X-Correlation-ID: ORDER-FLOW-$(date +%s)" \
  -H "Content-Type: application/json"

# Test multi-service endpoint chain
curl -s "http://localhost:8081/api/v1/products/health" \
  -H "X-Correlation-ID: MULTI-SERVICE-$(date +%s)"
```

### 2. Verify Log Flow
```bash
# Check if logs are reaching Elasticsearch
curl -s "http://localhost:9200/spring-boot-logs-*/_search?q=correlationId:MANUAL-TEST*&size=10&sort=@timestamp:desc" | jq .

# Check specific service logs
curl -s "http://localhost:9200/spring-boot-logs-*/_search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "bool": {
        "must": [
          {"term": {"springAppName": "ecom-api-gateway"}},
          {"range": {"@timestamp": {"gte": "now-10m"}}}
        ]
      }
    },
    "sort": [{"@timestamp": {"order": "desc"}}],
    "size": 5
  }' | jq .
```

## Frontend ELK Testing

### 1. Angular ELK Test Component

#### Accessing the Test Interface
1. Start Angular frontend: `ng serve --port 4201`
2. Login to the application (http://localhost:4201)
3. Navigate to user menu → "ELK Testing" 
4. Or directly visit: http://localhost:4201/elk-test

#### Available Test Scenarios

**✅ Working ELK Test (Recommended)**
- Uses verified public health endpoint
- Generates two sequential requests with unique correlation IDs
- CORS-free and authentication-free
- Perfect for verifying ELK pipeline

**🚀 Simple Gateway Test**
- Tests API Gateway health and categories endpoint
- May require authentication
- Good for authenticated user testing

**🛒 Complex Order Flow Test**
- Simulates multi-service order processing
- Tests product, inventory, payment, order, notification services
- Generates multiple correlation-tracked requests
- Best for end-to-end service testing

**⚡ Stress Test**
- Generates 10 concurrent requests with unique correlation IDs
- Tests ELK stack under load
- Good for performance testing

**💥 Error Simulation Test**
- Intentionally triggers 404 and error responses
- Tests error logging and correlation tracking
- Good for error scenario testing

### 2. Using the Test Interface

1. **Select Test Type**: Choose appropriate test button
2. **Monitor Results**: Watch real-time test execution with status indicators
3. **Copy Correlation ID**: Use the "📋 Copy for Kibana" button
4. **Check Logs**: Search in Kibana using copied correlation ID

## Kibana Log Analysis

### 1. Access Kibana
- URL: http://localhost:5601
- No authentication required for local development

### 2. Configure Index Pattern
```
Index Pattern: spring-boot-logs-*
Time Field: @timestamp
```

### 3. Search Strategies

#### By Correlation ID
```
Search: correlationId:"web-elk-test-abc123def-1692456789"
```

#### By Service Name
```
Search: springAppName:"ecom-api-gateway"
```

#### By Time Range + Service
```
Search: springAppName:"product-service" AND @timestamp:[now-15m TO now]
```

#### Complex Query Example
```json
{
  "query": {
    "bool": {
      "must": [
        {"match": {"message": "ELK-DEMO"}},
        {"term": {"level": "INFO"}},
        {"range": {"@timestamp": {"gte": "now-30m"}}}
      ]
    }
  }
}
```

### 4. Useful Kibana Filters
- **Time Range**: Set to "Last 15 minutes" for recent tests
- **Log Level**: Filter by INFO, WARN, ERROR
- **Service Filter**: `springAppName: "specific-service"`
- **Correlation ID**: `correlationId: "your-correlation-id"`

## Correlation ID Patterns

### Frontend Generated IDs
```
web-elk-test-abc123def-1692456789    # Angular ELK test
web-abc123def-1692456789             # Regular Angular requests
```

### Backend Generated IDs  
```
ELK-DEMO-1692456789                  # Manual backend testing
ORDER-FLOW-1692456789                # Order simulation
MULTI-SERVICE-1692456789             # Multi-service requests
```

### Manual Testing IDs
```
MANUAL-TEST-1692456789               # Manual curl testing
STRESS-TEST-001-1692456789           # Load testing
```

## Troubleshooting

### Common Issues

#### 1. Logs Not Appearing in Kibana
```bash
# Check Elasticsearch indices
curl http://localhost:9200/_cat/indices?v

# Check Logstash pipeline
curl http://localhost:9600/_node/stats/pipelines

# Verify service logging configuration
curl http://localhost:8081/actuator/loggers
```

#### 2. Correlation ID Not Propagating
```bash
# Check HTTP interceptor in Angular
# Verify X-Correlation-ID header in network tab

# Check backend correlation ID extraction
curl -v "http://localhost:8081/api/v1/public/health" \
  -H "X-Correlation-ID: DEBUG-TEST-123"
```

#### 3. CORS Issues in Frontend
- Use "Working ELK Test" button (CORS-free)
- Check browser developer console for CORS errors
- Verify API Gateway CORS configuration

### Debug Commands

```bash
# Check all running services
docker ps

# Check ELK stack health
curl http://localhost:9200/_cluster/health
curl http://localhost:9600/_node/stats
curl http://localhost:5601/api/status

# Check Spring Boot actuator endpoints
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/info
curl http://localhost:8761/actuator/health
```

## Expected Log Flow

### 1. Request Journey
```
Angular Frontend → HTTP Interceptor (adds correlation ID) 
→ API Gateway (logs request) 
→ Target Service (logs processing) 
→ Response back through chain
```

### 2. Log Processing Pipeline
```
Spring Boot Service → Logback → TCP Appender 
→ Logstash (5000) → Elasticsearch (9200) 
→ Kibana (5601) for visualization
```

### 3. Correlation ID Propagation
```
X-Correlation-ID header → Spring Boot filter → MDC context 
→ All log statements → Logstash → Elasticsearch → Kibana search
```

## Performance Testing

### Load Testing Script
```bash
#!/bin/bash
# Generate 50 requests with unique correlation IDs
for i in {1..50}; do
  correlation_id="LOAD-TEST-$i-$(date +%s)"
  curl -s "http://localhost:8081/api/v1/public/health" \
    -H "X-Correlation-ID: $correlation_id" &
done
wait
echo "Load test completed - check Kibana for correlation IDs: LOAD-TEST-*"
```

### Monitoring Query
```json
{
  "query": {
    "bool": {
      "must": [
        {"wildcard": {"correlationId": "LOAD-TEST-*"}},
        {"range": {"@timestamp": {"gte": "now-5m"}}}
      ]
    }
  },
  "aggs": {
    "services": {
      "terms": {"field": "springAppName"}
    }
  }
}
```

## Best Practices

### 1. Correlation ID Format
- Use descriptive prefixes: `WEB-`, `API-`, `TEST-`
- Include timestamp for uniqueness
- Keep under 100 characters
- Use alphanumeric and hyphens only

### 2. Testing Strategy
- Start with "Working ELK Test" to verify pipeline
- Test individual services before complex flows
- Use unique correlation IDs for each test session
- Monitor logs in real-time during testing

### 3. Production Considerations
- Implement log retention policies
- Monitor Elasticsearch disk usage
- Set up log level filtering
- Configure Kibana user access controls

## Integration with CI/CD

### Automated Testing Example
```bash
#!/bin/bash
# ELK stack health check script
CORRELATION_ID="CI-HEALTH-CHECK-$(date +%s)"

# Test endpoint
response=$(curl -s -w "%{http_code}" "http://localhost:8081/api/v1/public/health" \
  -H "X-Correlation-ID: $CORRELATION_ID")

http_code="${response: -3}"
if [ "$http_code" -eq 200 ]; then
  echo "✅ API Gateway healthy"
else
  echo "❌ API Gateway health check failed: $http_code"
  exit 1
fi

# Wait for log processing
sleep 5

# Verify log in Elasticsearch
log_count=$(curl -s "http://localhost:9200/spring-boot-logs-*/_count?q=correlationId:$CORRELATION_ID" | jq '.count')

if [ "$log_count" -gt 0 ]; then
  echo "✅ ELK logging pipeline healthy ($log_count logs found)"
else
  echo "❌ ELK pipeline issue - no logs found for $CORRELATION_ID"
  exit 1
fi
```

---

## Summary

This guide provides comprehensive testing strategies for the ELK logging stack with correlation ID tracking. Use the Angular test interface for interactive testing and curl commands for automated testing. The correlation ID system enables full request tracing across the microservices architecture.

**Key Testing Flow:**
1. Generate request with correlation ID
2. Monitor real-time logs in services  
3. Verify log processing in Elasticsearch
4. Search and analyze in Kibana
5. Validate end-to-end correlation tracking

**Last Updated:** 2025-08-19