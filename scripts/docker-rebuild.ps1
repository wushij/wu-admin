# 在仓库根目录 admin/ 下执行：改代码后重建 Docker
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host ">>> Maven: admin-backend"
Set-Location "$Root\admin-backend"
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ">>> Maven: admin-gateway"
Set-Location "$Root\admin-gateway"
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ">>> Docker Compose rebuild"
Set-Location $Root
docker compose down
docker compose up -d --build

Write-Host ">>> Done"
