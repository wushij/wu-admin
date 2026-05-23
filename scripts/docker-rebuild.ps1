# 单体项目：重新打包后端并重建 Docker 容器
# 用法：在项目根目录执行
#   powershell -ExecutionPolicy Bypass -File scripts/docker-rebuild.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

Push-Location (Join-Path $root "backend")
try {
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Maven 打包失败" }
} finally {
    Pop-Location
}

Push-Location $root
try {
    docker compose up -d --build
} finally {
    Pop-Location
}

Write-Host "完成。前端: http://localhost:3000  后端 API: http://localhost:8080/api"
