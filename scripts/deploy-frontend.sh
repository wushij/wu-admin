#!/bin/bash
# ===================================================================
# [wu-admin 前端与移动端 H5 自动化构建发布脚本]
# 适用环境: Linux / macOS / WSL
# 功能: 全量构建 Vue 3 PC 端与 UniApp H5 移动端，并自动更新 Nginx 目录
# ===================================================================

set -e

DIST_PC_DIR="/www/wwwroot/wu-admin/pc"
DIST_H5_DIR="/www/wwwroot/wu-admin/h5"

echo "=========================================="
echo "📦 1/2 开始构建 PC 端 (frontend)..."
echo "=========================================="
cd frontend
npm install --registry=https://registry.npmmirror.com
npm run build
cd ..

mkdir -p ${DIST_PC_DIR}
rsync -avz --delete frontend/dist/ ${DIST_PC_DIR}/
echo "✅ PC 端构建并同步完成 -> ${DIST_PC_DIR}"

echo "=========================================="
echo "📱 2/2 开始构建 移动端 H5 (uniapp)..."
echo "=========================================="
cd uniapp
npm install --registry=https://registry.npmmirror.com
npm run build:h5
cd ..

mkdir -p ${DIST_H5_DIR}
rsync -avz --delete uniapp/dist/build/h5/ ${DIST_H5_DIR}/
echo "✅ 移动端 H5 构建并同步完成 -> ${DIST_H5_DIR}"

# 重载 Nginx 服务
if command -v nginx > /dev/null 2>&1; then
  echo "🔄 正在平滑重载 Nginx 配置..."
  nginx -s reload || systemctl reload nginx
  echo "🎉 Nginx 重载成功！发布完成。"
fi
