@echo off
setlocal enabledelayedexpansion
for /f "tokens=2 delims==" %%a in ('findstr "mod_version" gradle.properties') do (
    set VERSION=%%a
)
set VERSION=!VERSION: =!
if "%~1"=="" (
    echo [Error] Please provide a release message! Format: .\release.bat "message"
    exit /b 1
)
set MSG=%~1
set COMMIT_MSG=!VERSION!:Release:!MSG!
set TAG_NAME=!VERSION!
echo [Release] Creating Tag: !TAG_NAME!
echo [Release] Commit Message: !COMMIT_MSG!
git add .
git commit -m "!COMMIT_MSG!"
git push
git tag -a !TAG_NAME! -m "!MSG!"
git push origin !TAG_NAME!
echo [Done] Released version !VERSION! as tag !TAG_NAME!