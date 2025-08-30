# Grafana Dashboard Setup for Docker

## Method 1: Manual Import via Web UI (Easiest)

1. **Access Grafana**: http://localhost:3000
2. **Login**: admin/admin
3. **Import Dashboard**:
   - Click "+" → "Import"
   - Click "Upload JSON file"
   - Select: `Docker/grafana-dashboard-microservices.json`
   - Click "Import"

## Method 2: Docker Copy Command

```bash
# Copy dashboard file into running Grafana container
docker cp Docker/grafana-dashboard-microservices.json grafana:/tmp/dashboard.json

# Import via API inside container
docker exec grafana curl -X POST \
  -H "Content-Type: application/json" \
  -u "admin:admin" \
  -d @/tmp/dashboard.json \
  http://localhost:3000/api/dashboards/db
```

## Method 3: Update Docker Compose (For Next Restart)

Add this to your `docker-compose.yml` under grafana service:

```yaml
grafana:
  # ... existing config
  volumes:
    - ./Docker/grafana-dashboard-microservices.json:/var/lib/grafana/dashboards/microservices.json:ro
  environment:
    - GF_DASHBOARDS_DEFAULT_HOME_DASHBOARD_PATH=/var/lib/grafana/dashboards/microservices.json
```

## Dashboard Features

✅ **Service Health Monitoring**: All services (cart, user, product, api-gateway)
✅ **Cart Service Operations**: Add/remove/update cart operations tracking  
✅ **User Service Operations**: Profile management and authentication metrics
✅ **Product Service Performance**: Response times for 1.4M+ product catalog
✅ **Memory & CPU Usage**: JVM and system resource monitoring
✅ **Error Rate Tracking**: 4xx/5xx HTTP errors across all services

## Access Dashboard
- URL: http://localhost:3000/d/microservices-overview
- Login: admin/admin