@echo off
echo Rebuilding API Gateway with updated CORS configuration...
cd "C:\Java-workspace\spring-micro-ecommerce-platform\ecom-api-gateway"
call mvn clean package -DskipTests
echo Build complete! Please restart the API Gateway service.
pause