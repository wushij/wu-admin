# 微服务架构开发指南

## 项目架构

### 目录结构

```
admin/                          # 管理系统总根目录
├── admin-gateway/              # 微服务网关 (Spring Boot / Spring Cloud Gateway)
├── admin-backend/              # RBAC 后端
├── admin-frontend/             # Vue3 后台管理前端
├── sql/
│   └── admin_platform.sql      # 全量建库脚本
├── Dockerfile                  # 后端镜像构建
├── docker-compose.yml          # 容器一键部署
├── DOCKER_DEPLOY.md            # Docker 部署教程
├── MICROSERVICE_GUIDE.md       # 本文档
└── README.md                   # 项目说明
```

### 整体架构图

```
                    ┌─────────────────┐
                    │ admin-frontend  │
                    │   Vue3  :3000   │
                    └────────┬────────┘
                             │ /api/**
                             ▼
                    ┌─────────────────┐
                    │ admin-gateway   │
                    │  路由 + 认证     │
                    │     :8080       │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ admin-backend │   │ 新服务 :8082  │   │ 新服务 :8083  │
│    :8081      │   │  ...          │   │  ...          │
└───────────────┘   └───────────────┘   └───────────────┘
        │                    │                    │
        └────────────────────┴────────────────────┘
                             │
                    ┌────────▼────────┐
                    │  MySQL + Redis  │
                    └─────────────────┘
```

## 当前服务

| 服务 | 端口 | 职责 | 状态 |
|------|------|------|------|
| admin-frontend | 3000 | 统一前端入口 | ✅ |
| admin-gateway | 8080 | API 网关、路由、认证 | ✅ |
| admin-backend | 8081 | 用户、角色、菜单、部门等 | ✅ |

> **当前只有 1 个后端服务**，后续按下方步骤添加新服务。

## 启动顺序

```bash
# 1. 基础设施
MySQL (:3307 映射)
Redis (:6379)

# 2. 后端
cd admin-backend
mvn spring-boot:run

# 3. 网关
cd admin-gateway
mvn spring-boot:run

# 4. 前端
cd admin-frontend
npm run dev
```

## 添加新服务

### 1. 创建后端项目

在 `admin/` 下新建目录，例如 `order-service/backend`，端口依次递增 8082、8083…

### 2. Gateway 添加路由

```yaml
# admin-gateway/src/main/resources/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/order/**
          filters:
            - StripPrefix=1
```

### 3. 前端添加页面

- 页面：`admin-frontend/src/views/order/`
- 路由：`admin-frontend/src/router/index.ts`
- API：`admin-frontend/src/api/order/`

## 数据库与 Redis

- 推荐共用 `RBAC1` 库（跨服务查询方便）
- Redis 统一使用 **database 3**
- Key 命名：`服务名:功能:ID`，如 `admin:token:1`

## 认证流程

```
登录 → admin-backend 校验 → JWT → Redis + 网关 Sa-Token
请求 → 前端带 Token → 网关校验 → 转发 admin-backend
```

## 端口分配

| 端口 | 用途 |
|------|------|
| 3000 | 前端 |
| 8080 | Gateway |
| 8081 | admin-backend |
| 8082+ | 新业务服务 |
| 3307 | MySQL（宿主机映射） |
| 6379 | Redis |

## 默认账号

- 管理员：`admin` / `admin123`
- 普通用户：`zhangsan` / `admin123`
