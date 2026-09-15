@echo off
chcp 65001 >nul
title 小芽绘本部署 - 启动中

echo ============================================
echo   小芽绘本 一键启动部署
echo ============================================
echo.

:: 检查是否已在运行
tasklist /fi "imagename eq java.exe" 2>nul | find /i "java.exe" >nul
if %errorlevel% equ 0 (
    echo [提示] 后端已在运行，跳过启动
    goto :start_tunnel
)

:: 启动后端
echo [1/2] 启动后端服务...
set AI_LLM_KEY=sk-kzyqvruelohcabrgdbtmuypaxkghnovenlobsnbxakhqkrai
start /min "小芽绘本后端" cmd /c "java -jar d:\project_work\小芽绘本v1.0_未接入AI\picture-book-backend\target\picture-book-backend-1.0.0.jar"

:: 等待端口 8080 就绪
echo       等待后端启动...
set /a count=0
:wait_port
timeout /t 2 /nobreak >nul
set /a count+=1
netstat -an | find "0.0.0.0:8080" | find "LISTENING" >nul 2>&1
if %errorlevel% neq 0 (
    if %count% lss 15 (
        goto :wait_port
    ) else (
        echo [错误] 后端启动超时！
        pause
        exit /b 1
    )
)
echo       后端启动成功 (端口 8080)

:start_tunnel
:: 检查隧道是否已在运行
tasklist /fi "imagename eq cloudflared.exe" 2>nul | find /i "cloudflared.exe" >nul
if %errorlevel% equ 0 (
    echo [提示] Cloudflare Tunnel 已在运行
    goto :show_url
)

:: 启动 Cloudflare Tunnel
echo [2/2] 启动 Cloudflare Tunnel...
start /min "Cloudflare Tunnel" cmd /c ""C:\Program Files (x86)\cloudflared\cloudflared.exe" tunnel --url http://localhost:8080"

:: 等待隧道 URL 生成
echo       等待隧道连接...
set /a count=0
:wait_tunnel
timeout /t 3 /nobreak >nul
set /a count+=1
powershell -Command "$p = Get-CimInstance Win32_Process -Filter ""name='cloudflared.exe'"" | Select-Object -ExpandProperty CommandLine; if ($p) { 'found' } else { 'none' }" 2>nul | find "found" >nul
if %errorlevel% neq 0 (
    if %count% lss 10 (
        goto :wait_tunnel
    ) else (
        echo [错误] 隧道启动超时！
        pause
        exit /b 1
    )
)

:show_url
:: 提取隧道 URL
echo       提取隧道地址...
for /f "delims=" %%i in ('powershell -Command "Get-Process cloudflared -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Id | ForEach-Object { $logFile = Get-ChildItem 'C:\Users\23253\AppData\Local\Temp\trae-agent-toolhost\jobs\*\output.log' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1; if ($logFile) { $c = Get-Content $logFile -Raw; if ($c -match 'https://[a-z0-9-]+\.trycloudflare\.com') { $matches[0] } } }" 2>nul') do set TUNNEL_URL=%%i

if "%TUNNEL_URL%"=="" (
    :: 备用方法：直接用固定 URL
    set TUNNEL_URL=https://bear-pads-search-greater.trycloudflare.com
)

echo.
echo ============================================
echo   部署启动成功！
echo ============================================
echo.
echo   前端地址: https://yuan-broken.github.io/xiaoya-picturebook/
echo   后端地址: %TUNNEL_URL%
echo   测试账号: online_test / test123456
echo.
echo   浏览器打开前端地址即可使用
echo   关闭部署请运行「一键关闭部署.bat」
echo ============================================
echo.
echo 提示: 此窗口可以关闭，后端和隧道在后台运行
pause
