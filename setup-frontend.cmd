@echo off
echo ================================
echo Setting up E-Commerce Frontend
echo ================================

echo.
echo [1/4] Checking Node.js installation...
node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js is not installed or not in PATH
    echo Please install Node.js 18 or higher from https://nodejs.org/
    pause
    exit /b 1
)

echo ✓ Node.js found: 
node --version

echo.
echo [2/4] Checking Angular CLI...
ng version >nul 2>&1
if errorlevel 1 (
    echo Installing Angular CLI globally...
    npm install -g @angular/cli
    if errorlevel 1 (
        echo ERROR: Failed to install Angular CLI
        pause
        exit /b 1
    )
)

echo ✓ Angular CLI ready

echo.
echo [3/4] Installing frontend dependencies...
cd ecommerce-frontend
npm install
if errorlevel 1 (
    echo ERROR: Failed to install dependencies
    pause
    exit /b 1
)

echo ✓ Dependencies installed

echo.
echo [4/4] Verifying setup...
ng build --configuration development >nul 2>&1
if errorlevel 1 (
    echo WARNING: Build verification failed, but continuing...
) else (
    echo ✓ Build verification passed
)

echo.
echo ================================
echo ✅ Frontend Setup Complete!
echo ================================
echo.
echo Next Steps:
echo 1. Start backend services (microservices + infrastructure)
echo 2. Configure Keycloak realm and client
echo 3. Start frontend: npm start or ng serve
echo 4. Open browser: http://localhost:4200
echo.
echo Development Commands:
echo - npm start          : Start dev server
echo - npm run build     : Build for production  
echo - npm test          : Run unit tests
echo - npm run lint      : Run code linting
echo.
echo Backend Integration:
echo - Update environment.ts with your backend URLs
echo - Ensure API Gateway is running on port 8081
echo - Ensure Keycloak is running on port 8080
echo.
pause