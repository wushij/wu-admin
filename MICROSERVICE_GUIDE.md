# 微服务架构开发指南

## 项目架构

### 目录结构

```
admin/                          # 管理系统总根目录
├── admin-gateway/              # 微服务网关 (Spring Boot / Spring Cloud Gateway)
├── admin-backend/              # RBAC 后端
├── admin-frontend/             # Vue3 后台管理前端
├── sql/
│   └── admin_platform.sql      # 唯一库脚本（全量 + 文末可重复升级段）
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
| admin-backend | 8081 | 用户、角色、菜单、组织、系统配置、审批/工单等 | ✅ |

> **当前只有 1 个后端服务**，后续按下方步骤添加新服务。

## 后端技术版本（Spring Boot 3.5 升级后）

| 项目 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | RBAC、网关统一 |
| Spring Boot | 3.5.13 | `admin-backend`、`admin-gateway` 父 POM |
| Spring Cloud | 2025.0.0 | 仅网关 |
| Springdoc OpenAPI | 2.8.9 | 须 ≥ 2.8.9（兼容 Spring Framework 6.2） |
| Knife4j | 4.5.0 | UI 使用 `doc.html`；`knife4j.enable: false` 关闭与 springdoc 2.8 不兼容的增强 Customizer |

接口文档、依赖对齐细节见根目录 [README.md](./README.md#技术栈)。

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

## 后端分层（与 admin-backend 一致）

`admin-backend` 已按 **B 方案** 分层：`common` → `framework`（SPI，不依赖业务）→ `modules/system`（含 `api` 与 `framework` 实现类）。新增独立微服务时建议：

1. 复制或依赖 **`common` + `framework` jar**（含 `DynamicConfigProvider`、`PermissionApi` 等接口）；
2. 新业务代码放在 **`modules/<域>/api|service|dal`**，域内横切放在 **`modules/<域>/framework`**；
3. 勿在全局 `framework` 中直接 `import` 业务 `service` / `dal`。

详见根目录 [README.md — 后端包结构](./README.md)（「目录结构 → 后端包结构」）。

## 添加新服务

### 1. 创建后端项目

在 `admin/` 下新建目录，例如 `order-service`，端口依次递增 8082、8083…；可依赖 `admin-backend` 抽出的 `common`/`framework` 模块（后续 Maven 多模块化时）。

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

登录前可调用 `GET /api/auth/config`（网关已放行）读取 `sys_config_group` 中的公开项（站点文案、验证码类型、注册开关等）。会话时长、限流、上传限制等由 `SystemConfigHelper` 从库读取，详见根目录 [README.md](./README.md#系统配置systemconfig)。

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
