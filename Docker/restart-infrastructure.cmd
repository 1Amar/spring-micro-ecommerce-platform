@echo off
echo ================================
echo Restarting Infrastructure Services
echo ================================

echo.
echo [1/4] Stopping all services...
docker-compose -f docker-compose.yml down
if errorlevel 1 (
    echo WARNING: Some services may have already been stopped
)

echo.
echo [2/4] Removing volumes to ensure clean database initialization...
docker volume rm docker_pgdata docker_esdata 2>nul
if errorlevel 1 (
    echo WARNING: Volumes may not exist or may be in use
)

echo.
echo [3/4] Starting infrastructure services...
docker-compose -f docker-compose.yml up -d
if errorlevel 1 (
    echo ERROR: Failed to start infrastructure services
    exit /b 1
)

echo.
echo [4/4] Waiting for services to be ready...
echo Waiting for PostgreSQL to be ready...
timeout /t 10 /nobreak > nul

echo Waiting for Keycloak to be ready (this may take a few minutes)...
timeout /t 30 /nobreak > nul

echo.
echo ================================
echo Infrastructure Services Status:
echo ================================
docker-compose -f docker-compose.yml ps

echo.
echo ================================
echo Database Verification:
echo ================================
echo Checking if databases were created...
docker exec postgres psql -U devuser -d ecommerce_dev -c "\l" | findstr keycloak
if errorlevel 1 (
    echo WARNING: Keycloak database may not be created yet
    echo Run: docker exec postgres psql -U devuser -d ecommerce_dev -f /docker-entrypoint-initdb.d/init-db.sql
) else (
    echo ✓ Keycloak database found
)

echo.
echo Service URLs:
echo - Keycloak: http://localhost:8080 (admin/admin)
echo - Prometheus: http://localhost:9090
echo - Grafana: http://localhost:3000
echo - Jaeger: http://localhost:16686
echo - Kibana: http://localhost:5601
echo - Elasticsearch: http://localhost:9200
echo.
pause