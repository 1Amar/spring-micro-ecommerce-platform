@echo off
echo 🎯 Importing Grafana Dashboard using Docker...
echo.

REM Get the Grafana container name/ID
for /f %%i in ('docker ps --filter "name=grafana" --format "{{.Names}}"') do set GRAFANA_CONTAINER=%%i

if "%GRAFANA_CONTAINER%"=="" (
    echo ❌ Error: Grafana container not found
    echo    Make sure Grafana is running with Docker
    echo    Expected container name containing 'grafana'
    pause
    exit /b 1
)

echo 📦 Found Grafana container: %GRAFANA_CONTAINER%

REM Copy dashboard file to container
echo 📂 Copying dashboard file to container...
docker cp "%CD%\grafana-dashboard-microservices.json" %GRAFANA_CONTAINER%:/tmp/dashboard.json

if %errorlevel% neq 0 (
    echo ❌ Error: Failed to copy dashboard file
    pause
    exit /b 1
)

REM Wait a moment for file to be ready
timeout /t 2 /nobreak >nul

REM Import dashboard via API inside container
echo 📤 Importing dashboard via Grafana API...
docker exec %GRAFANA_CONTAINER% curl -X POST -H "Content-Type: application/json" -u "admin:admin" -d @/tmp/dashboard.json http://localhost:3000/api/dashboards/db

echo.
echo.
echo ✅ Dashboard import completed!
echo 🌐 Access it at: http://localhost:3000/d/microservices-overview
echo 🔑 Login: admin/admin
echo.
echo 📋 Updated Dashboard Features:
echo    • ✅ Cart Service Operations (add/remove/update cart items)
echo    • ✅ User Service Operations (profile management)  
echo    • ✅ Product Service Performance (1.4M+ products response times)
echo    • ✅ All Service Health Monitoring (cart, user, product, api-gateway)
echo    • ✅ Memory, CPU, and Error Rate tracking
echo.
echo 🎉 Grafana Dashboard is ready!
echo 💡 Tip: Use your cart service to generate some metrics data
pause