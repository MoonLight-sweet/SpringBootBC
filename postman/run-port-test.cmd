@echo off
chcp 65001 >nul
title CodeClinic Postman端口验收
set "POSTMAN_HOME=%LOCALAPPDATA%\Postman"
set "POSTMAN_APP="
for /f "delims=" %%D in ('dir /b /ad /o-n "%POSTMAN_HOME%\app-*" 2^>nul') do if not defined POSTMAN_APP set "POSTMAN_APP=%POSTMAN_HOME%\%%D"
if not defined POSTMAN_APP (
    echo 未找到Postman安装目录。
    exit /b 1
)
set "POSTMAN_CLI_ELECTRON=%POSTMAN_APP%\Postman.exe"
set "POSTMAN_CLI=%POSTMAN_APP%\resources\data\postman-cli\bin\postman.cmd"
if not exist "%POSTMAN_CLI%" (
    echo 当前Postman版本缺少命令行测试组件：%POSTMAN_APP%
    exit /b 1
)
cd /d "%~dp0.."
call "%POSTMAN_CLI%" collection run "postman\CodeClinic.postman_collection.json" --reporters cli,json --reporter-json-export "postman\results\postman-latest.json"
echo.
if errorlevel 1 (
    echo 测试失败，请查看上方错误信息。
) else (
    echo 测试完成：8080端口与核心接口全部通过。
)
pause
