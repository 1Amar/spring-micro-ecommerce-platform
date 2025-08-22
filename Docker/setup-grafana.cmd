@echo off
echo Setting up Grafana with Prometheus data source and E-Commerce microservices dashboard...
echo.

REM Wait for Grafana to be ready
echo Waiting for Grafana to be ready...
timeout /t 15 /nobreak >nul

REM Check if Grafana is responding
echo Checking Grafana availability...
curl -s http://localhost:3000/api/health > nul
if errorlevel 1 (
    echo ERROR: Grafana is not responding. Please ensure Docker containers are running.
    echo Run: docker-compose up -d
    pause
    exit /b 1
)

echo Grafana is ready!
echo.

REM Add Prometheus data source
echo Adding Prometheus data source...
curl -X POST ^
  -H "Content-Type: application/json" ^
  -u admin:admin ^
  -d "{\"name\":\"Prometheus\",\"type\":\"prometheus\",\"url\":\"http://prometheus:9090\",\"access\":\"proxy\",\"isDefault\":true,\"basicAuth\":false}" ^
  http://localhost:3000/api/datasources 2>nul

echo.
echo Data source added (or already exists).

REM Import microservices dashboard
echo.
echo Importing Spring Boot E-Commerce microservices dashboard...
curl -X POST ^
  -H "Content-Type: application/json" ^
  -u admin:admin ^
  -d @grafana-dashboard-microservices.json ^
  http://localhost:3000/api/dashboards/db 2>nul

echo.
echo ========================================
echo Grafana setup complete! 🎉
echo ========================================
echo.
echo 🌐 Access Grafana at: http://localhost:3000
echo 👤 Username: admin
echo 🔐 Password: admin
echo.
echo 📊 Available dashboards:
echo   - Spring Boot Microservices E-Commerce Platform
echo.
echo 🔧 Services monitored:
echo   - Eureka Service Registry (8761)
echo   - API Gateway (8081)
echo   - Order Service (8083)
echo   - Inventory Service (8084)
echo   - Notification Service (8085)
echo   - Payment Service (8087)
echo   - Product Service (8088)
echo   - Catalog Service (8082)
echo   - Search Service (8089)
echo.
echo 📈 Metrics available:
echo   - Service health status
echo   - HTTP request rates and response times
echo   - JVM memory and CPU usage
echo   - Error rates and order flow simulation
echo   - Correlation ID tracing metrics
echo.
echo Press any key to continue...
pause >nul