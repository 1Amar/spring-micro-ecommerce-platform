@echo off
setlocal enabledelayedexpansion
echo Stopping Spring Boot Microservices E-Commerce Platform...
echo.

REM Kill all Java processes running Spring Boot applications
echo Stopping all Spring Boot services...

REM Kill processes by window title (more reliable)
taskkill /FI "WINDOWTITLE:Eureka Service Registry*" /F /T 2>nul
taskkill /FI "WINDOWTITLE:API Gateway*" /F /T 2>nul
taskkill /FI "WINDOWTITLE:Order Service*" /F /T 2>nul
taskkill /FI "WINDOWTITLE:Inventory Service*" /F /T 2>nul
taskkill /FI "WINDOWTITLE:Product Service*" /F /T 2>nul
taskkill /FI "WINDOWTITLE:Payment Service*" /F /T 2>nul
taskkill /FI "WINDOWTITLE:Notification Service*" /F /T 2>nul
taskkill /FI "WINDOWTITLE:Catalog Service*" /F /T 2>nul
taskkill /FI "WINDOWTITLE:Search Service*" /F /T 2>nul

REM Kill all Java processes (more aggressive approach)
echo Killing all Java processes running Spring Boot...
tasklist /FI "IMAGENAME eq java.exe" /FO CSV | findstr /V "PID" > temp_java_processes.txt 2>nul
for /f "tokens=2 delims=," %%i in (temp_java_processes.txt) do (
    set pid=%%i
    set pid=!pid:"=!
    taskkill /PID !pid! /F /T 2>nul
)
del temp_java_processes.txt 2>nul

REM Kill Maven processes that might be running spring-boot:run
taskkill /IM "mvn.cmd" /F /T 2>nul
taskkill /IM "cmd.exe" /FI "WINDOWTITLE:*spring-boot:run*" /F /T 2>nul


echo Spring Boot services stopped.
echo Docker infrastructure left running (manage manually).
echo.
echo All Spring Boot services have been stopped.
echo.