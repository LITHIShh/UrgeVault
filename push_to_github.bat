@echo off
cd /d "%~dp0"
echo ========================================================
echo   Pushing BetShield to GitHub (LITHIShh/BetShield)
echo ========================================================
echo.
git push -u origin main
echo.
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] BetShield has been pushed to GitHub!
    echo.
    echo GitHub Actions will now automatically build your APK and AAB.
    echo Check your build progress and download the files here:
    echo https://github.com/LITHIShh/BetShield/actions
) else (
    echo [ACTION NEEDED] Authentication required.
    echo When prompted by Git Credential Manager, click "Sign in with your browser"
    echo or use a GitHub Personal Access Token (PAT).
)
echo.
pause
