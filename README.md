# Admin Platform

基于 **Vue 3 + Spring Cloud Gateway + Spring Boot RBAC** 的企业级后台管理系统，提供用户权限、业务工单、系统监控、日志审计、文件与字典等能力，支持 Docker 一键部署与本地开发调试。

---

## 功能概览

| 模块 | 说明 |
|------|------|
| **系统管理** | 用户、角色、菜单、部门、字典管理 |
| **系统日志** | 操作日志（AOP 自动记录）、登录日志 |
| **系统监控** | API 访问统计、在线用户与强退 |
| **文件管理** | 分组、上传、预览、列表/平铺视图 |
| **业务中心** | 工单管理、审批单中心 |
| **认证安全** | 图形验证码、登录/注册限流、JWT + 网关 Sa-Token 会话 |

菜单与权限由数据库 `sys_menu` 动态加载，超级管理员默认拥有全部功能。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios、ECharts |
| 网关 | Spring Cloud Gateway、Sa-Token、Redis |
| 后端 | Spring Boot 2.7、Spring Security、MyBatis-Plus、Druid、Knife4j |
| 数据 | MySQL 8、Redis 7 |
| 部署 | Docker Compose、Nginx |

**JDK 要求**

- RBAC 后端：**JDK 8**
- 网关：**JDK 17**

---

## 系统架构

```
浏览器
   │
   ▼
┌─────────────────┐     /api/*      ┌──────────────────┐
│  admin-frontend │ ──────────────► │  admin-gateway   │
│  Vite / Nginx   │                 │  :8080           │
│  :3000          │                 │  Sa-Token + 路由 │
└─────────────────┘                 └────────┬─────────┘
                                           │ 转发
                                           ▼
                                  ┌──────────────────┐
                                  │  rbac-server     │
                                  │  :8081           │
                                  └────────┬─────────┘
                                           │
                           ┌───────────────┴───────────────┐
                           ▼                               ▼
                    ┌─────────────┐                ┌─────────────┐
                    │   MySQL     │                │   Redis     │
                    │   RBAC1     │                │  database 3 │
                    └─────────────┘                └─────────────┘
```

**请求路径约定**

- 前端统一请求前缀：`/api`
- 网关去掉 `/api` 后转发至 RBAC，例如：`/api/system/user/list` → RBAC `/system/user/list`

---

## 目录结构

```
admin/
├── RBAC/                          # RBAC 后端工程
│   ├── rbac-backend/              # 业务 API、权限、文件、监控等
│   └── Dockerfile
├── admin-platform/
│   ├── admin-frontend/            # Vue3 管理端
│   ├── admin-gateway/             # Spring Cloud 网关
│   ├── sql/
│   │   └── admin_platform.sql     # 全量建库脚本（表结构 + 初始数据）
│   ├── docker-compose.yml
│   ├── DOCKER_DEPLOY.md           # Docker 部署详细说明
│   └── MICROSERVICE_GUIDE.md      # 微服务拆分说明
├── 整合代码/                       # 各功能参考实现与提示词（可选阅读）
└── README.md
```

---

## 环境要求

| 软件 | 版本建议 |
|------|----------|
| Node.js | 18+ |
| Maven | 3.6+ |
| JDK | 8（RBAC）+ 17（网关） |
| MySQL | 8.0 |
| Redis | 7.x |
| Docker Desktop | 可选，用于容器部署 |

---

## 快速开始

### 方式一：Docker Compose（推荐）

```powershell
cd admin-platform
docker compose up -d --build
```

首次启动会自动执行 `sql/admin_platform.sql` 初始化数据库。

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| 网关 | http://localhost:8080 |
| RBAC（容器映射） | http://localhost:8082 |
| MySQL | `127.0.0.1:3307`，库名 `RBAC1`，用户/密码 `root`/`root` |
| Redis | `127.0.0.1:6379`，database `3` |

更多代理、排错与重建说明见 [admin-platform/DOCKER_DEPLOY.md](./admin-platform/DOCKER_DEPLOY.md)。

---

### 方式二：本地开发

#### 1. 初始化数据库

```bash
mysql -u root -p < admin-platform/sql/admin_platform.sql
```

或在客户端中执行该脚本。脚本会创建库 `RBAC1`、全部表结构、菜单权限及演示数据。

> **注意**：脚本内含 `DROP TABLE`，仅适用于新库或开发环境，勿对已有生产数据直接整文件执行。

#### 2. 启动 Redis

确保本机 `127.0.0.1:6379` 可用，RBAC 与网关均使用 **database 3**。

#### 3. 启动 RBAC 后端（8081）

```powershell
cd RBAC/rbac-backend
mvn spring-boot:run -DskipTests
```

按需修改 `src/main/resources/application.yml` 中的数据库账号密码。

#### 4. 启动网关（8080）

```powershell
cd admin-platform/admin-gateway
mvn spring-boot:run -DskipTests
```

#### 5. 启动前端（3000）

```powershell
cd admin-platform/admin-frontend
npm install
npm run dev
```

浏览器访问：**http://localhost:3000**

Vite 已将 `/api` 代理到 `http://localhost:8080`（见 `vite.config.ts`）。

---

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | 超级管理员 |
| `zhangsan` | `admin123` | 普通用户 |

登录后菜单来自服务端权限树，修改菜单/角色后需**重新登录**生效。

---

## 配置说明

### RBAC `application.yml` 要点

| 配置项 | 说明 | 默认 |
|--------|------|------|
| `server.port` | 服务端口 | `8081` |
| `spring.datasource.*` | MySQL 连接 | `RBAC1` @ `127.0.0.1:3306` |
| `spring.redis.database` | Redis 库索引 | `3` |
| `jwt.secret` / `jwt.expiration` | JWT 密钥与过期时间 | 24h |
| `file.storage.local-path` | 本地上传目录 | `./data/uploads` |
| `auth.security.captcha-enabled` | 登录图形验证码 | `true` |

### 网关 `application.yml` 要点

| 配置项 | 说明 |
|--------|------|
| `app.rbac.base-url` | RBAC 地址；Docker 中为 `http://rbac-server:8081` |
| `spring.cloud.gateway.routes` | `/api/auth/**`、`/api/system/**`、`/api/files/**` 等路由 |

### 前端

- 开发代理：`admin-frontend/vite.config.ts` → `/api` → `8080`
- 生产构建：`npm run build`，由 Nginx 反向代理网关（见 `nginx.conf`）

---

## 主要 API 前缀（经网关）

| 前缀 | 说明 |
|------|------|
| `/api/auth/**` | 登录、注册、验证码、登出 |
| `/api/system/**` | 用户、角色、菜单、部门、字典、日志、工单、审批等 |
| `/api/files/**` | 文件访问与上传 |

RBAC 直连 Swagger（开发）：http://localhost:8081/swagger-ui/index.html

---

## 数据库

- **唯一全量脚本**：`admin-platform/sql/admin_platform.sql`
- 包含：核心业务表、字典、操作日志、API 访问统计、文件表、菜单与 `sys_role_menu` 初始数据
- 工单已挂在 **业务中心** 目录下；登录日志、操作日志挂在 **系统日志** 目录下

本地开发 MySQL 端口一般为 `3306`；Docker 映射为 `3307`。

---

## 功能开发提示

### 字典下拉（前端）

```javascript
import { useDict } from '@/composables/useDict'

const { options, load, labelOf } = useDict('sys_user_sex')
onMounted(() => load())
```

### 操作日志（后端）

在 Controller 方法上添加 `@Log` 注解即可由 `LogAspect` 异步写入 `sys_oper_log`。

### 按钮权限（前端）

```html
<el-button v-permission="'system:user:create'">新增</el-button>
```

权限标识需与 `sys_menu.permission` 字段一致。

---

## 构建与打包

```powershell
# 前端
cd admin-platform/admin-frontend
npm run build

# RBAC
cd RBAC/rbac-backend
mvn clean package -DskipTests

# 网关
cd admin-platform/admin-gateway
mvn clean package -DskipTests
```

---

## 常见问题

**Q：登录后菜单为空或 403？**  
A：检查是否执行了 `admin_platform.sql`，并为角色分配菜单；然后重新登录。

**Q：上传图片无法预览？**  
A：文件 URL 需经网关鉴权；确认已登录且网关 JWT 中继配置包含 `/api/files/**`。

**Q：改 Java 代码后 Docker 未生效？**  
A：需要重新构建镜像：`docker compose up -d --build`，参见 `admin-platform/DOCKER_DEPLOY.md`。

**Q：Git 仓库地址？**  
A：https://github.com/wushij/admin.git

---

## 相关文档

- [Docker 部署指南](./admin-platform/DOCKER_DEPLOY.md)
- [微服务说明](./admin-platform/MICROSERVICE_GUIDE.md)
- [整合代码 / 功能参考](./整合代码/)（各模块关键代码与 AI 提示词）

---

## 许可证

本项目仅供学习与内部使用，部署到生产环境前请修改默认密码、JWT 密钥等敏感配置。
