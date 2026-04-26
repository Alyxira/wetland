@echo off
setlocal

set "ROOT=%~dp0"
set "MAP_DIR=%ROOT%frontend\travel-map"

start "travel-map" cmd /c "cd /d ""%MAP_DIR%"" && npm run dev"

call "%ROOT%mvnw.cmd" spring-boot:run
