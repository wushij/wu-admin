# 微服务架构开发指南

## 项目架构

### 当前目录结构

```
E:\admin\
├── admin-platform/           # 主项目（网关 + 前端）
│   ├── admin-gateway/        # Gateway 网关 :8080
│   ├── admin-frontend/       # Vue3 前端 :3000
│   ├── sql/                  # 数据库脚本
│   └── MICROSERVICE_GUIDE.md # 本文档
│
└── RBAC/                    # 后端服务
    ├── rbac-backend/        # 系统服务 :8081 ✅ 当前运行
    └── sql/                 # 数据库脚本
```

### 添加新服务后的目录结构

```
E:\admin\
├── admin-platform/           # 主项目（网关 + 前端）
│   ├── admin-gateway/        # Gateway 网关 :8080
│   ├── admin-frontend/       # Vue3 前端 :3000（合并所有页面）
│   └── ...
│
├── RBAC/                    # 后端服务1
│   └── rbac-backend/        # 系统服务 :8081
│
├── new-service/            # 后端服务2（新添加）
│   └── backend/             # 业务服务 :8082
│
└── another-service/        # 后端服务3（新添加）
    └── backend/             # 业务服务 :8083
```

> **说明**：每个新服务保留独立后端，前端统一合并到 `admin-frontend`

## 整体架构图

```
                    ┌─────────────────┐
                    │   前端 :3000    │
                    │   Vue3 + TS     │
                    └────────┬────────┘
                             │ /api/**
                             ▼
                    ┌─────────────────┐
                    │  Gateway :8080  │
                    │  路由 + 认证     │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  RBAC :8081   │   │ 新服务 :8082  │   │ 新服务 :8083  │
│  系统管理      │   │  ...          │   │  ...          │
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
| admin-frontend | 3000 | 统一前端入口 | ✅ 运行中 |
| admin-gateway | 8080 | API网关、路由、认证 | ✅ 运行中 |
| rbac-backend | 8081 | 用户、角色、菜单、部门管理 | ✅ 运行中 |

> **当前只有1个后端服务**，后续添加新服务时按下方步骤操作。

## 启动顺序

```bash
# 1. 启动基础设施
MySQL (:3306)
Redis (:6379)

# 2. 启动后端服务
cd RBAC/rbac-backend
mvn spring-boot:run

# 3. 启动网关
cd admin-platform/admin-gateway
mvn spring-boot:run

# 4. 启动前端
cd admin-platform/admin-frontend
npm run dev
```

## 添加新服务

### 1. 创建后端项目

```bash
# 在 E:\admin 下创建新目录
mkdir new-service
```

### 2. 配置端口

```yaml
# application.yml
server:
  port: 8082  # 依次递增：8082、8083...

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/RBAC1  # 可共用或新建数据库
  data:
    redis:
      database: 3  # 统一使用 DB 3
```

### 3. Gateway 添加路由

```yaml
# admin-gateway/application.yml
spring:
  cloud:
    gateway:
      routes:
        # 添加新服务路由
        - id: new-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/new-service/**
          filters:
            - StripPrefix=1
```

### 4. 前端添加页面

```bash
# 创建页面目录
admin-frontend/src/views/new-service/
├── index.vue

# 添加路由
admin-frontend/src/router/index.ts

# 添加 API
admin-frontend/src/api/new-service.ts
```

### 5. 前端路由示例

```typescript
// router/index.ts
{
  path: 'new-service',
  name: 'NewService',
  component: () => import('@/views/new-service/index.vue'),
  meta: { title: '新服务', permission: 'new-service:list' }
}
```

### 6. 前端 API 示例

```typescript
// api/new-service.ts
import { get, post } from '@/utils/request'

export const getList = (params: any) => {
  return get('/new-service/list', params)
}

export const create = (data: any) => {
  return post('/new-service', data)
}
```

## 数据库规范

### 共享数据库（推荐）

```yaml
# 使用 RBAC1 数据库
url: jdbc:mysql://127.0.0.1:3306/RBAC1
```

优点：
- 跨服务查询方便
- 维护简单

### 独立数据库

```yaml
# 创建新数据库
url: jdbc:mysql://127.0.0.1:3306/new_db
```

适用场景：
- 数据隔离要求高
- 独立部署需求

## Redis 规范

```yaml
# 统一使用 DB 3
spring:
  data:
    redis:
      database: 3
```

Key 命名规范：
```
服务名:功能:ID
例如：
  rbac:token:1
  order:cache:123
```

## 认证方案

当前使用 JWT + Redis：

```
登录流程：
1. 用户登录 → RBAC后端验证
2. 生成 JWT Token
3. Token 存入 Redis (key: rbac:token:userId)
4. 返回 Token 给前端

请求流程：
1. 前端携带 Token
2. Gateway 验证 Token
3. 转发到后端服务
```

## 端口分配

| 端口 | 用途 |
|------|------|
| 3000 | 前端 |
| 8080 | Gateway |
| 8081 | RBAC 后端 |
| 8082 | 新服务 1 |
| 8083 | 新服务 2 |
| ... | 依次递增 |
| 3306 | MySQL |
| 6379 | Redis |

## 注意事项

1. **端口不冲突**：新服务使用 8082+
2. **路由前缀统一**：所有 API 加 `/api` 前缀
3. **Redis DB 统一**：使用 DB 3
4. **前端合并**：新页面放到 admin-frontend

## 默认账号

- 管理员：admin / admin123
- 普通用户：zhangsan / admin123
