@echo off
cd /d "%~dp0"
title Pushing UrgeVault to GitHub
echo ========================================================
echo   Pushing UrgeVault to GitHub (LITHIShh/BetShield)
echo ========================================================
echo.
echo Attempting push to origin main...
git push -u origin main
if %ERRORLEVEL% EQU 0 goto success

echo.
echo ========================================================
echo [AUTHENTICATION HELPER]
echo If your browser did not automatically open for GitHub login,
echo you can push instantly using a GitHub Personal Access Token.
echo (Create one in GitHub -> Settings -> Developer settings -> Personal access tokens)
echo ========================================================
echo.
set /p USER_TOKEN="Enter / Paste your GitHub Personal Access Token (or press ENTER to exit): "
if "%USER_TOKEN%"=="" goto end

echo.
echo Pushing with your token...
git push https://%USER_TOKEN%@github.com/LITHIShh/BetShield.git main
if %ERRORLEVEL% EQU 0 goto success
goto error

:success
echo.
echo ========================================================
echo [SUCCESS] UrgeVault pushed to GitHub successfully!
echo ========================================================
echo.
echo GitHub Actions is now compiling your APK and Play Store AAB.
echo Download your files here once the build finishes:
echo https://github.com/LITHIShh/BetShield/actions
echo.
goto end

:error
echo.
echo [ERROR] Push failed. Please check your GitHub permissions or token.
echo.

:end
pause
