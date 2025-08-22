@echo off
echo Stopping all Spring Boot services...
echo.

REM Kill all Java processes (Spring Boot services)
echo Killing all Java processes (this will stop all Spring Boot services)...
taskkill /IM java.exe /F /T 2>nul

echo.
echo Killing Maven processes...
taskkill /IM mvn.cmd /F /T 2>nul

echo.
echo All Spring Boot services stopped.
echo Docker infrastructure left running (manage manually).
echo.
echo Done!