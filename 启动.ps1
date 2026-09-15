# 小芽绘本 v1.0 一键启动（PowerShell 脚本）
# 双击「一键启动.bat」会调用本脚本

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$BackendPort = 8080
$FrontendPort = 8765
$BackendJar = Join-Path $root 'picture-book-backend\target\picture-book-backend-1.0.0.jar'
$MvnCmd = 'D:\RuoYi\apache-maven-3.9.9\bin\mvn.cmd'
$PythonCmd = 'D:\python\python.exe'
$FrontendDir = Join-Path $root '静态页面'

function Test-PortListening($port) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $conn
}

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "   小芽绘本 v1.0 一键启动" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ===== 步骤 1：启动后端 =====
Write-Host "[1/3] 检查后端服务 (端口 $BackendPort)..." -ForegroundColor Yellow
if (Test-PortListening $BackendPort) {
    Write-Host "  后端已在运行，跳过启动" -ForegroundColor Green
} else {
    if (-not (Test-Path $BackendJar)) {
        Write-Host "  未找到后端 jar，开始构建..." -ForegroundColor Yellow
        if (-not (Test-Path $MvnCmd)) {
            Write-Host "  [错误] 未找到 Maven: $MvnCmd" -ForegroundColor Red
            Read-Host "按回车退出"
            exit 1
        }
        & $MvnCmd clean package -DskipTests -f "picture-book-backend\pom.xml"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  [错误] 后端构建失败" -ForegroundColor Red
            Read-Host "按回车退出"
            exit 1
        }
    }
    Write-Host "  启动后端..." -ForegroundColor Yellow
    $backendLog = Join-Path $root 'backend-out.log'
    $backendErr = Join-Path $root 'backend-err.log'
    Start-Process -FilePath 'java' -ArgumentList "-jar `"$BackendJar`"" -WorkingDirectory $root -RedirectStandardOutput $backendLog -RedirectStandardError $backendErr -WindowStyle Minimized
    Write-Host "  等待后端就绪 (约 15 秒)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 15
    if (Test-PortListening $BackendPort) {
        Write-Host "  后端已启动: http://localhost:$BackendPort" -ForegroundColor Green
    } else {
        Write-Host "  [警告] 后端端口仍未监听，请查看 backend-err.log" -ForegroundColor Red
    }
}
Write-Host ""

# ===== 步骤 2：启动前端静态服务 =====
Write-Host "[2/3] 检查前端静态服务 (端口 $FrontendPort)..." -ForegroundColor Yellow
if (Test-PortListening $FrontendPort) {
    Write-Host "  前端服务已在运行，跳过启动" -ForegroundColor Green
} else {
    if (-not (Test-Path (Join-Path $FrontendDir 'home.html'))) {
        Write-Host "  [错误] 未找到前端目录: $FrontendDir" -ForegroundColor Red
        Read-Host "按回车退出"
        exit 1
    }
    Write-Host "  启动前端静态服务..." -ForegroundColor Yellow
    if (-not (Test-Path $PythonCmd)) {
        Write-Host "  [警告] 未找到 Python: $PythonCmd，尝试 PATH 中的 python" -ForegroundColor Yellow
        $PythonCmd = 'python'
    }
    Start-Process -FilePath $PythonCmd -ArgumentList '-m','http.server',"$FrontendPort" -WorkingDirectory $FrontendDir -WindowStyle Minimized
    Start-Sleep -Seconds 2
    if (Test-PortListening $FrontendPort) {
        Write-Host "  前端已启动: http://localhost:$FrontendPort" -ForegroundColor Green
    } else {
        Write-Host "  [警告] 前端端口未监听，可能是 Python 未正确安装" -ForegroundColor Red
    }
}
Write-Host ""

# ===== 步骤 3：打开浏览器 =====
Write-Host "[3/3] 打开浏览器..." -ForegroundColor Yellow
Start-Process "http://localhost:$FrontendPort/home.html"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "   启动完成" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  前端首页:  http://localhost:$FrontendPort/home.html"
Write-Host "  绘本库:    http://localhost:$FrontendPort/library.html"
Write-Host "  后端 API:  http://localhost:$BackendPort"
Write-Host "  后端日志:  backend-out.log / backend-err.log"
Write-Host ""
Write-Host "  关闭服务：关闭弹出的两个最小化的命令窗口"
Write-Host "  更新记录：见 开发日志.md"
Write-Host ""
Read-Host "按回车退出本窗口（服务仍会继续运行）"
