@echo off
echo ================================
echo Building All Microservices
echo ================================

echo.
echo [1/10] Building common-library...
cd /d "C:\Java-workspace\spring-micro-ecommerce-platform\common-library"
call mvn clean install -q
if errorlevel 1 (
    echo ERROR: Failed to build common-library
    exit /b 1
)
echo ✓ common-library built successfully

echo.
echo [2/10] Building eureka-service-registry...
cd /d "C:\Java-workspace\spring-micro-ecommerce-platform\eureka-service-registry"
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Failed to build eureka-service-registry
    exit /b 1
)
echo ✓ eureka-service-registry built successfully

echo.
echo [3/10] Building ecom-api-gateway...
cd /d "C:\Java-workspace\spring-micro-ecommerce-platform\ecom-api-gateway"
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Failed to build ecom-api-gateway
    exit /b 1
)
echo ✓ ecom-api-gateway built successfully

echo.
echo [4/10] Building ecom-order-service...
cd /d "C:\Java-workspace\spring-micro-ecommerce-platform\ecom-order-service"
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Failed to build ecom-order-service
    exit /b 1
)
echo ✓ ecom-order-service built successfully

echo.
echo [5/10] Building inventory-service...
cd /d "C:\Java-workspace\spring-micro-ecommerce-platform\inventory-service"
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Failed to build inventory-service
    exit /b 1
)
echo ✓ inventory-service built successfully

echo.
echo [6/10] Building product-service...
cd /d "C:\Java-workspace\spring-micro-ecommerce-platform\product-service"
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Failed to build product-service
    exit /b 1
)
echo ✓ product-service built successfully

echo.
echo [7/10] Building payment-service...
cd /d "C:\Java-workspace\spring-micro-ecommerce-platform\payment-service"
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Failed to build payment-service
    exit /b 1
)
echo ✓ payment-service built successfully

echo.
echo [8/10] Building notification-service...
cd /d "C:\Java-workspace\spring-micro-ecommerce-platform\notification-service"
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Failed to build notification-service
    exit /b 1
)
echo ✓ notification-service built successfully

echo.
echo [9/10] Building notification-worker...
cd /d "C:\Java-workspace\spring-micro-ecommerce-platform\notification-worker"
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Failed to build notification-worker
    exit /b 1
)
echo ✓ notification-worker built successfully

echo.
echo [10/10] Building search-service...
cd /d "C:\Java-workspace\spring-micro-ecommerce-platform\search-service"
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Failed to build search-service
    exit /b 1
)
echo ✓ search-service built successfully

echo.
echo ================================
echo ✅ ALL SERVICES BUILT SUCCESSFULLY!
echo ================================
echo.
echo Next steps:
echo 1. Start infrastructure: docker-compose -f Docker/docker-compose.yml up -d
echo 2. Start services in order: Eureka → Gateway → Others
echo 3. Test observability with: test-observability.md
echo.
pause