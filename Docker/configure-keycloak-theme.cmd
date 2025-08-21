@echo off
echo Configuring Keycloak theme for ecommerce-realm...

REM Get admin access token
for /f "tokens=*" %%i in ('curl -s -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" ^| jq -r .access_token') do set ACCESS_TOKEN=%%i

echo Access token obtained: %ACCESS_TOKEN:~0,20%...

REM Update realm to use custom theme
curl -X PUT "http://localhost:8080/admin/realms/ecommerce-realm" ^
  -H "Authorization: Bearer %ACCESS_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"loginTheme\": \"ecommerce\"}"

echo Theme configured successfully!
pause