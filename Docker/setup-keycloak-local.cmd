@echo off
echo ========================================
echo Setting up Keycloak for Local Development
echo Connecting to Windows PostgreSQL
echo ========================================

echo.
echo Step 1: Stopping any existing Docker containers...
docker-compose -f docker-compose.yml down
docker stop keycloak_local 2>nul
docker rm keycloak_local 2>nul

echo.
echo Step 2: Setting up Keycloak database in Windows PostgreSQL...
echo Please run this SQL script in your PostgreSQL:
echo File: setup-keycloak-db.sql
echo.
echo "CREATE DATABASE keycloak;"
echo "CREATE USER keycloak_user WITH ENCRYPTED PASSWORD 'keycloak_pass';"
echo "GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak_user;"
echo.
pause

echo.
echo Step 3: Starting Keycloak container...
docker-compose -f docker-compose-keycloak-only.yml up -d

echo.
echo Step 4: Waiting for Keycloak to start...
timeout /t 30

echo.
echo Step 5: Checking Keycloak status...
docker logs keycloak_local --tail=20

echo.
echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo Keycloak Admin: http://localhost:8080
echo Login: admin / admin
echo.
echo To import realm:
echo 1. Go to http://localhost:8080
echo 2. Login as admin/admin
echo 3. Click "Import" and upload: keycloak-realm-export.json
echo.
echo To test connection:
echo curl http://localhost:8080/health/ready
echo.
pause