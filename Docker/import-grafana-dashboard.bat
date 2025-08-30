@echo off
echo 🎯 Importing Grafana Dashboard for Microservices...
echo 📊 Dashboard: grafana-dashboard-microservices.json
echo 🔗 Grafana URL: http://localhost:3000

REM Check if Grafana is accessible
curl -s -f "http://localhost:3000/api/health" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Error: Cannot connect to Grafana at http://localhost:3000
    echo    Make sure Grafana is running in Docker
    pause
    exit /b 1
)

REM Import the dashboard
echo 📤 Importing dashboard...
curl -X POST -H "Content-Type: application/json" -u "admin:admin" -d @"grafana-dashboard-microservices.json" "http://localhost:3000/api/dashboards/db"

echo.
echo ✅ Dashboard import completed!
echo 🌐 Access it at: http://localhost:3000/d/microservices-overview
echo.
echo 📋 New Features in Updated Dashboard:
echo    • Cart Service Operations monitoring
echo    • User Service Operations tracking  
echo    • Product Service Performance metrics
echo    • Updated service health checks for all active services
echo.
echo 🎉 Dashboard setup complete!
echo 💡 Tip: The dashboard will show data once services start receiving requests
pause