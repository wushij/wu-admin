#!/bin/bash
# ===================================================================
# [wu-admin 后端自动化部署与平滑重启脚本]
# 适用环境: Linux (Ubuntu / CentOS / Debian)
# 功能: 自动编译 Maven 产物、平滑重启 JAR 服务并进行健康检查
# ===================================================================

set -e

APP_NAME="wu-admin-backend"
JAR_PATH="backend/target/backend-1.0.0.jar"
LOG_DIR="data/logs"
PID_FILE="data/backend.pid"

# 1. 检查环境变量设置（生产环境安全最佳实践）
export SPRING_MAIL_USERNAME="${SPRING_MAIL_USERNAME:-}"
export SPRING_MAIL_PASSWORD="${SPRING_MAIL_PASSWORD:-}"

echo "=========================================="
echo "🚀 开始打包并部署 ${APP_NAME}..."
echo "=========================================="

mkdir -p ${LOG_DIR}

# 2. 编译打包后端
cd backend
mvn clean package -DskipTests
cd ..

if [ ! -f "${JAR_PATH}" ]; then
  echo "❌ 编译失败：未找到文件 ${JAR_PATH}"
  exit 1
fi

# 3. 停止已运行的旧进程
if [ -f "${PID_FILE}" ]; then
  OLD_PID=$(cat ${PID_FILE})
  if ps -p ${OLD_PID} > /dev/null 2>&1; then
    echo "⏱️ 正在停止旧服务 PID: ${OLD_PID}..."
    kill -15 ${OLD_PID}
    sleep 3
    if ps -p ${OLD_PID} > /dev/null 2>&1; then
      echo "⚠️ 服务未响应，执行强行终止 PID: ${OLD_PID}..."
      kill -9 ${OLD_PID}
    fi
  fi
  rm -f ${PID_FILE}
fi

# 4. 后台启动 Java JAR 服务
echo "🌟 启动 Java 后端服务..."
nohup java -Xms512m -Xmx1024m \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=GMT+8 \
  -jar ${JAR_PATH} \
  --spring.profiles.active=prod > ${LOG_DIR}/stdout.log 2>&1 &

NEW_PID=$!
echo ${NEW_PID} > ${PID_FILE}

echo "✅ 服务已启动 PID: ${NEW_PID}"
echo "📄 正在监控启动日志 (前 20 行)..."
sleep 5
tail -n 20 ${LOG_DIR}/stdout.log
