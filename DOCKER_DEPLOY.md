# Admin Platform Docker 部署指南

## 📋 项目架构

```
前端 (Nginx) → 后端 (Spring Boot) → MySQL + Redis
   :3000              :8080           :3307   :6379
```

### 服务端口说明

| 服务 | 容器端口 | 外部端口 | 说明 |
|------|----------|----------|------|
| 前端 (Nginx) | 80 | 3000 | http://localhost:3000 |
| 后端 (Spring Boot) | 8080 | 8080 | http://localhost:8080/api |
| MySQL | 3306 | 3307 | 127.0.0.1:3307 |
| Redis | 6379 | 6379 | 127.0.0.1:6379 |

---

## 🛠️ 环境要求

### 必需软件

1. **Docker Desktop** (Windows/Mac)
   - 下载: https://www.docker.com/products/docker-desktop
   - 版本: 最新版本

2. **Clash代理** (中国大陆用户)
   - 用于拉取Docker Hub镜像
   - 端口: 7897 (根据你的实际端口调整)

3. **Maven** (Java打包)
   - 版本: 3.6+
   - **JDK 17**（backend）
   - **Spring Boot 3.5.13**（镜像内运行版本，与本地 `pom.xml` 一致）
   - 接口文档依赖：**springdoc 2.8.9** + Knife4j 4.5（`knife4j.enable: false`），详见 [README.md](./README.md#接口文档toolapi-doc)

---

## 🚀 快速开始

### 1. 配置Docker代理 (中国大陆用户)

打开 Docker Desktop → Settings → Resources → Proxies:

```
Proxy mode: Manual configuration
HTTP Proxy: http://127.0.0.1:7897
HTTPS Proxy: http://127.0.0.1:7897
No Proxy: localhost,127.0.0.1,*.local,192.168.0.0/16,10.0.0.0/8,172.16.0.0/12
Containers proxy: Same as host proxy
```

点击 **Apply & Restart** 使配置生效。

### 2. 打包Java项目

#### 打包后端

```bash
cd backend
mvn clean package -DskipTests
```

生成文件: `backend/target/backend.jar`

> Windows 一键重建：`powershell -ExecutionPolicy Bypass -File scripts/docker-rebuild.ps1`

### 3. 导入数据库 (首次部署)

```bash
# 在项目根目录 admin-vue 下执行
cd /path/to/admin-vue
docker compose up -d mysql

# 等待MySQL启动完成 (约10秒)
docker compose ps

# 导入SQL数据
docker exec admin-mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS \`wu-admin\`; CREATE DATABASE \`wu-admin\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

docker cp "./sql/admin_platform.sql" admin-mysql:/tmp/init.sql

docker exec admin-mysql mysql -uroot -proot --default-character-set=utf8mb4 --database=wu-admin -e "SET NAMES utf8mb4; SOURCE /tmp/init.sql;"
```

### 4. 构建并启动所有服务

```bash
cd /path/to/admin-vue
docker compose up -d --build
```

### 5. 验证部署

```bash
# 查看所有服务状态
docker compose ps

# 查看日志
docker compose logs -f

# 测试前端
curl http://localhost:3000

# 测试后端 API
curl http://localhost:8080/api/auth/config
```

---

## 📁 项目结构

```
admin-vue/
├── docker-compose.yml          # Docker 编排配置
├── scripts/
│   └── docker-rebuild.ps1      # 重新打包并重建容器
├── sql/                        # 数据库初始化脚本
│   └── admin_platform.sql
├── backend/                    # Spring Boot 后端
│   ├── pom.xml
│   ├── src/
│   └── target/
│       └── backend.jar
├── frontend/                   # Vue 3 前端
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
└── Dockerfile                  # 后端镜像（根目录）
```

---

## 🔧 Docker 配置文件详解

### docker-compose.yml

单体架构包含 **MySQL、Redis、backend、frontend** 四个服务，详见项目根目录 `docker-compose.yml`。

要点：

- 前端 Nginx 将 `/api/` 代理到 `http://backend:8080/api/`
- 后端 `context-path=/api`，容器内外端口均为 **8080**
- MySQL 外部端口 **3307**，Redis **6379**

### 前端 Dockerfile (多阶段构建)

```dockerfile
# 构建阶段
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm install
RUN npm install -D @vitejs/plugin-vue
COPY . .
RUN npm run build

# 运行阶段
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Nginx 配置

生产环境见 `frontend/nginx.conf`，核心代理规则：

```nginx
location /api/ {
    proxy_pass http://backend:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

---

## 🔄 常用运维命令

### 启动服务

```bash
# 启动所有服务
docker compose up -d

# 启动指定服务
docker compose up -d mysql redis

# 重新构建并启动
docker compose up -d --build
```

### 停止服务

```bash
# 停止所有服务
docker compose stop

# 停止并删除容器
docker compose down

# 停止并删除容器+数据卷 (谨慎使用!)
docker compose down -v
```

### 查看日志

```bash
# 查看所有服务日志
docker compose logs -f

# 查看指定服务日志
docker compose logs -f backend

# 查看最近100行日志
docker compose logs --tail=100 backend
```

### 重启服务

```bash
# 重启所有服务
docker compose restart

# 重启指定服务
docker compose restart backend frontend
```

### 进入容器

```bash
# 进入MySQL容器
docker exec -it admin-mysql bash

# 进入MySQL命令行
docker exec -it admin-mysql mysql -uroot -proot --database=wu-admin

# 进入Redis容器
docker exec -it admin-redis redis-cli

# 查看容器IP
docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' backend
```

### 查看状态

```bash
# 查看所有容器状态
docker compose ps

# 查看资源使用
docker stats

# 查看磁盘使用
docker system df
```

---

## 🐛 常见问题

### 1. Docker Hub拉取镜像失败

**错误**: `dial tcp ... connectex: A connection attempt failed`

**解决**:
- 确保Clash代理已开启
- 检查Docker Desktop代理配置
- 点击 Apply & Restart
- 或手动拉取: `docker pull nginx:alpine`

### 2. 端口冲突

**错误**: `ports are not available: exposing port TCP 0.0.0.0:3306`

**解决**:
```bash
# 查看端口占用
netstat -ano | findstr :3306

# 终止占用进程
taskkill /F /PID <进程ID>

# 或修改docker-compose.yml端口映射
ports:
  - "3307:3306"  # 改用3307
```

### 3. 数据库乱码

**错误**: 中文显示为 `???`

**解决**:
```bash
# 重新导入时指定字符集
docker exec -i admin-mysql mysql -uroot -proot --default-character-set=utf8mb4 --database=wu-admin < init.sql
```

### 4. 前端无法访问后端 API

**错误**: `502 Bad Gateway` 或 API 请求超时

**原因**: Docker 容器内应使用服务名 `backend`，而非 `localhost`

**解决**: 确认 `frontend/nginx.conf` 中：

```nginx
proxy_pass http://backend:8080/api/;
```

### 5. 前端构建失败

**错误**: `Could not resolve entry module "index.html"`

**原因**: `.dockerignore` 排除了必要文件

**解决**: 检查 `.dockerignore`,确保不移除:
- `index.html`
- `vite.config.ts`
- `package.json`

### 6. Maven打包失败

**错误**: `NoClassDefFoundError` 或 `repackage failed`

**解决**: 检查pom.xml配置
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>repackage</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

---

## 🔐 安全建议

1. **生产环境修改默认密码**
   ```yaml
   MYSQL_ROOT_PASSWORD: your_strong_password
   ```

2. **使用Docker Secret管理敏感信息**
   ```yaml
   secrets:
     db_password:
       file: ./secrets/db_password.txt
   ```

3. **限制容器资源**
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '2'
         memory: 2G
   ```

4. **使用只读文件系统**
   ```yaml
   read_only: true
   tmpfs:
     - /tmp
   ```

5. **定期更新镜像**
   ```bash
   docker compose pull
   docker compose up -d
   ```

---

## 📊 性能优化

### JVM参数优化

```dockerfile
# Dockerfile中添加
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
```

### Nginx优化

```nginx
# nginx.conf中添加
worker_processes auto;
events {
    worker_connections 1024;
}
http {
    sendfile on;
    tcp_nopush on;
    keepalive_timeout 65;
}
```

### MySQL优化

```yaml
# docker-compose.yml中添加
command: >
  --default-authentication-plugin=mysql_native_password
  --character-set-server=utf8mb4
  --collation-server=utf8mb4_unicode_ci
  --max_connections=500
  --innodb_buffer_pool_size=512M
```

---

## 📝 更新日志

- 2026-05-23: 调整为单体前后端架构（`backend/` + `frontend/`），移除网关相关部署步骤
- 2026-04-13: 初始 Docker 部署方案
  - 前端 Docker 化 (Nginx)
  - 后端 Docker 化 (Java 17)
  - MySQL + Redis 容器化
  - 完整的代理配置说明

---

## 🤝 技术支持

如有问题,请检查:
1. Docker Desktop版本是否最新
2. 代理配置是否正确
3. 端口是否被占用
4. 日志输出 (`docker compose logs -f`)

---

**祝你部署顺利! 🎉**
