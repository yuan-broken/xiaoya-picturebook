@echo off
chcp 65001 >nul
title 小芽绘本部署 - 关闭中

echo ============================================
echo   小芽绘本 一键关闭部署
echo ============================================
echo.

:: 关闭 Cloudflare Tunnel
echo [1/2] 关闭 Cloudflare Tunnel...
tasklist /fi "imagename eq cloudflared.exe" 2>nul | find /i "cloudflared.exe" >nul
if %errorlevel% equ 0 (
    taskkill /f /im cloudflared.exe >nul 2>&1
    echo       Cloudflare Tunnel 已关闭
) else (
    echo       Cloudflare Tunnel 未在运行
)

:: 关闭后端
echo [2/2] 关闭后端服务...
tasklist /fi "imagename eq java.exe" 2>nul | find /i "java.exe" >nul
if %errorlevel% equ 0 (
    taskkill /f /im java.exe >nul 2>&1
    echo       后端服务已关闭
) else (
    echo       后端服务未在运行
)

echo.
echo ============================================
echo   部署已关闭
echo ============================================
echo.
echo   前端页面仍可通过以下地址访问:
echo   https://yuan-broken.github.io/xiaoya-picturebook/
echo   但后端 API 不可用，页面会显示离线状态
echo.
echo   重新启动请运行「一键启动部署.bat」
echo ============================================
pause
