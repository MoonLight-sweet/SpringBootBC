@echo off
chcp 65001 >nul
title CodeClinic Postman端口验收
set "POSTMAN_CLI_ELECTRON=C:\Users\Moon\AppData\Local\Postman\app-12.28.2\Postman.exe"
set "POSTMAN_CLI=C:\Users\Moon\AppData\Local\Postman\app-12.28.2\resources\data\postman-cli\bin\postman.cmd"
cd /d "%~dp0.."
call "%POSTMAN_CLI%" collection run "postman\CodeClinic.postman_collection.json" --reporters cli,json --reporter-json-export "postman\results\postman-latest.json"
echo.
if errorlevel 1 (
    echo 测试失败，请查看上方错误信息。
) else (
    echo 测试完成：8080端口与核心接口全部通过。
)
pause
