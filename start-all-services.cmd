@echo off
echo Starting Spring Boot Microservices E-Commerce Platform...
echo.

REM Set the root directory
set ROOT_DIR=%~dp0

REM Start infrastructure first
echo Starting Docker infrastructure...
cd "%ROOT_DIR%Docker"
docker-compose up -d
if errorlevel 1 (
    echo Failed to start Docker infrastructure
    exit /b 1
)
echo Docker infrastructure started successfully
echo.

REM Wait for infrastructure to be ready
echo Waiting for infrastructure to initialize...
timeout /t 15 /nobreak >nul
echo.

REM Start Eureka Service Registry first (port 8761)
echo Starting Eureka Service Registry...
cd "%ROOT_DIR%eureka-service-registry"
start "Eureka Service Registry" cmd /c "mvn spring-boot:run"
echo Eureka Service Registry starting...

REM Wait for Eureka to be ready
echo Waiting for Eureka to start (10 seconds)...
timeout /t 20 /nobreak >nul
echo.

REM Start API Gateway (port 8081)
echo Starting API Gateway...
cd "%ROOT_DIR%ecom-api-gateway"
start "API Gateway" cmd /c "mvn spring-boot:run"
echo API Gateway starting...

REM Wait for API Gateway
echo Waiting for API Gateway to start (5 seconds)...
timeout /t 10 /nobreak >nul
echo.

REM Start User Service (port 8082)
echo Starting User Service...
cd "%ROOT_DIR%user-service"
start "User Service" cmd /c "mvn spring-boot:run"
echo API User Service...

REM Wait for API Gateway
echo Waiting for User Service to start (10 seconds)...
timeout /t 10 /nobreak >nul
echo.

REM Start Cart Service (port 8089)
echo Starting Cart Service...
cd "%ROOT_DIR%cart-service"
start "Cart Service" cmd /c "mvn spring-boot:run"
echo Cart Service...

REM Wait for API Gateway
echo Waiting for Cart Service to start (10 seconds)...
timeout /t 10 /nobreak >nul
echo.

REM Start Order Service (port 8083)
echo Starting Order Service...
cd "%ROOT_DIR%ecom-order-service"
start "Order Service" cmd /c "mvn spring-boot:run"
echo Order Service starting...

REM Wait a bit before starting next service
timeout /t 10 /nobreak >nul

REM Start Inventory Service (port 8084)
echo Starting Inventory Service...
cd "%ROOT_DIR%inventory-service"
start "Inventory Service" cmd /c "mvn spring-boot:run"
echo Inventory Service starting...

REM Wait a bit before starting next service
timeout /t 10 /nobreak >nul

REM Start Product Service (port 8085)
echo Starting Product Service...
cd "%ROOT_DIR%product-service"
start "Product Service" cmd /c "mvn spring-boot:run"
echo Product Service starting...

REM Wait a bit before starting next service
timeout /t 10 /nobreak >nul

REM Start Payment Service (port 8086)
echo Starting Payment Service...
cd "%ROOT_DIR%payment-service"
start "Payment Service" cmd /c "mvn spring-boot:run"
echo Payment Service starting...

REM Wait a bit before starting next service
timeout /t 10 /nobreak >nul

REM Start Notification Service (port 8087)
echo Starting Notification Service...
cd "%ROOT_DIR%notification-service"
start "Notification Service" cmd /c "mvn spring-boot:run"
echo Notification Service starting...

REM Wait a bit before starting next service
timeout /t 10 /nobreak >nul


REM Start Search Service (port 8090)
echo Starting Search Service...
cd "%ROOT_DIR%search-service"
start "Search Service" cmd /c "mvn spring-boot:run"
echo Search Service starting...

echo.
echo All services are starting...
echo.
echo Service URLs:
echo - Eureka Service Registry: http://localhost:8761
echo - API Gateway: http://localhost:8081
echo - Order Service: http://localhost:8083
echo - Inventory Service: http://localhost:8084
echo - Product Service: http://localhost:8085
echo - Payment Service: http://localhost:8086
echo - Notification Service: http://localhost:8087
echo - Search Service: http://localhost:8090
echo.
echo Infrastructure URLs:
echo - Keycloak: http://localhost:8080
echo - Prometheus: http://localhost:9090
echo - Grafana: http://localhost:3000
echo - Jaeger: http://localhost:16686
echo - Kibana: http://localhost:5601
echo - PostgreSQL: localhost:5432
echo - Redis: localhost:6379
echo - Elasticsearch: http://localhost:9200
echo.
echo Wait 30-60 seconds for all services to fully start up...
echo Check Eureka dashboard at http://localhost:8761 to see all registered services
echo.

REM Return to root directory
cd "%ROOT_DIR%"