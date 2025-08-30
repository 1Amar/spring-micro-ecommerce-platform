@echo off
echo ========================================
echo Testing Keycloak Connection
echo ========================================

echo.
echo 1. Testing Keycloak health endpoint...
curl -s http://localhost:8080/health/ready
if %ERRORLEVEL% EQU 0 (
    echo ✅ Keycloak health check: OK
) else (
    echo ❌ Keycloak health check: FAILED
)

echo.
echo 2. Testing Keycloak admin console...
curl -s http://localhost:8080 | findstr "Keycloak" >nul
if %ERRORLEVEL% EQU 0 (
    echo ✅ Keycloak admin console: OK
) else (
    echo ❌ Keycloak admin console: FAILED
)

echo.
echo 3. Testing database connection (check logs)...
docker logs keycloak_local --tail=10 | findstr -i "database\|connection\|error"

echo.
echo 4. Container status:
docker ps | findstr keycloak_local

echo.
echo ========================================
echo Access Points:
echo ========================================
echo Admin Console: http://localhost:8080
echo Health Check: http://localhost:8080/health/ready
echo Realm URL: http://localhost:8080/realms/master
echo.
echo To check detailed logs:
echo docker logs keycloak_local -f
echo.
pause