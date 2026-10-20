@echo off
title Social Network Connection Finder
cd /d "%~dp0"
echo ========================================================
echo Starting Social Network Connection Finder...
echo ========================================================
call ".\mvnw.cmd" javafx:run
if %ERRORLEVEL% neq 0 (
    echo.
    echo Application stopped or encountered an error.
    pause
)
