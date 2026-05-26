# Admin Platform

基于 **Vue 3 + Spring Cloud Gateway + Spring Boot RBAC** 的企业级后台管理系统，提供用户权限、组织岗位、业务工单、系统监控、日志审计、文件与字典等能力，支持 Docker 一键部署与本地开发调试。

---

## 功能概览

| 模块 | 说明 |
|------|------|
| **系统管理** | 用户、角色、菜单、**组织管理**、字典管理 |
| **组织管理** | 部门体系 + 岗位体系；左树右表、拖拽调整部门、岗位成员、部门回收站 |
| **用户管理** | 支持部门、**岗位多选**、角色单选、回收站 |
| **菜单管理** | 树形表格；目录/菜单/按钮联动表单；**图标网格选择器**；外链（`https://`）新窗口打开 |
| **开发工具** | **接口文档**：Layout 内嵌 Knife4j（`doc.html`），支持刷新 / 新窗口打开 |
| **系统日志** | 操作日志（AOP 自动记录）、登录日志 |
| **系统监控** | API 访问统计、在线用户与强退 |
| **文件管理** | 分组、上传、预览、列表/平铺视图 |
| **业务中心** | 工单管理、审批单中心 |
| **认证安全** | 图形验证码、登录/注册限流、JWT + 网关 Sa-Token 会话 |

菜单与权限由数据库 `sys_menu` 动态加载，超级管理员默认拥有全部功能。修改菜单或角色后需**重新登录**侧栏才会更新。

---

## 近期能力说明

### 组织管理（`/system/org`）

- 原「部门管理」升级为 **组织管理**，Tab 切换：**部门体系 | 岗位体系**
- 部门：树形结构、`ancestors` 祖级路径、拖拽移动、子部门/成员查看、回收站
- 岗位：岗位树、用户关联（`sys_user_post`）、组织内成员列表

### 菜单管理（`/system/menu`）

- 树表展示：类型、图标、路由、组件/外链、排序、状态开关
- 表单按类型显隐：目录 / 菜单 / 按钮；支持外链地址（`component` 存完整 URL）
- 图标选择：Popover 网格 + 中文标签 + 搜索（`components/IconSelect.vue`）
- 按钮行不显示「新增」；支持全部展开/折叠

### 接口文档（`/tool/api-doc`）

- 侧栏：**开发工具 → 接口文档**
- 内嵌本项目 **Knife4j**（RBAC `doc.html`），非独立业务 CRUD
- 开发环境：Vite 代理 `/doc.html`、`/webjars`、`/swagger-ui`、`/v3/api-docs` → RBAC `8081`
- 生产环境：Nginx 转发上述路径至网关 → RBAC
- 菜单 `component` 填 `/doc.html`；若改为 `https://...` 外链，侧栏点击将在**新窗口**打开（Apifox 等）
- Knife4j 分组下拉 **default**：表示当前仅一个 OpenAPI 分组（全部接口），属正常现象

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、Vite、Element Plus、Pinia、Axios、ECharts |
| 网关 | Spring Cloud Gateway、Sa-Token、Redis |
| 后端 | Spring Boot 2.7、Spring Security、MyBatis-Plus、Druid、Knife4j（SpringDoc） |
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
│  Vite / Nginx   │   /doc.html*  │  :8080           │
│  :3000          │ ──────────────► │  Sa-Token + 路由 │
└─────────────────┘                 └────────┬─────────┘
                                           │ 转发
                                           ▼
                                  ┌──────────────────┐
                                  │  admin-backend   │
                                  │  :8081           │
                                  │  Knife4j         │
                                  └────────┬─────────┘
                                           │
                           ┌───────────────┴───────────────┐
                           ▼                               ▼
                    ┌─────────────┐                ┌─────────────┐
                    │   MySQL     │                │   Redis     │
                    │   RBAC1     │                │  database 3 │
                    └─────────────┘                └─────────────┘

* 接口文档静态资源同源代理，见 vite.config.ts / nginx.conf
```

**请求路径约定**

- 前端业务 API 统一前缀：`/api`（网关去掉 `/api` 后转发 RBAC）
- 示例：`/api/system/user/page` → RBAC `/system/user/page`

---

## 目录结构

```
admin/                          # 管理系统总根目录
├── admin-gateway/              # 微服务网关 (Spring Cloud Gateway)
├── admin-backend/              # RBAC 后端
├── admin-frontend/             # Vue3 后台管理前端
├── sql/
│   └── admin_platform.sql      # 数据库初始化脚本（全量）
├── scripts/
│   └── docker-rebuild.ps1      # Docker 重建脚本
├── Dockerfile                  # 后端镜像构建
├── docker-compose.yml          # 容器一键部署
├── DOCKER_DEPLOY.md            # Docker 部署教程
├── MICROSERVICE_GUIDE.md       # 微服务使用文档
└── README.md                   # 项目说明
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
| Docker Desktop | 可选 |

---

## 快速开始

### 方式一：Docker Compose（推荐）

```powershell
cd admin
docker compose up -d --build
```

首次启动会执行 `sql/admin_platform.sql` 初始化数据库（含组织管理、开发工具菜单）。

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| 网关 | http://localhost:8080 |
| admin-backend（容器映射） | http://localhost:8082 |
| MySQL | `127.0.0.1:3307`，库 `RBAC1`，`root`/`root` |
| Redis | `127.0.0.1:6379`，database `3` |

详见 [DOCKER_DEPLOY.md](./DOCKER_DEPLOY.md)。

---

### 方式二：本地开发

#### 1. 初始化数据库

```bash
mysql -u root -p < sql/admin_platform.sql
```

> `admin_platform.sql` 为全量脚本（含组织管理、开发工具菜单等），内含 `DROP TABLE`，仅用于新库或开发环境。

#### 2. 启动 Redis

本机 `127.0.0.1:6379`，RBAC 与网关使用 **database 3**。

#### 3. 启动 RBAC（8081）

```powershell
cd admin-backend
mvn spring-boot:run -DskipTests
```

#### 4. 启动网关（8080）

```powershell
cd admin-gateway
mvn spring-boot:run -DskipTests
```

#### 5. 启动前端（3000）

```powershell
cd admin-frontend
npm install
npm run dev
```

访问：**http://localhost:3000**

Vite 代理：`/api` → 网关 `8080`；Knife4j 相关路径 → RBAC `8081`（见 `vite.config.ts`）。

---

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | 超级管理员 |
| `zhangsan` | `admin123` | 普通用户 |

---

## 配置说明

### RBAC `application.yml`

| 配置项 | 说明 | 默认 |
|--------|------|------|
| `server.port` | 服务端口 | `8081` |
| `spring.datasource.*` | MySQL | `RBAC1` @ `3306` |
| `spring.redis.database` | Redis 库 | `3` |
| `jwt.secret` / `jwt.expiration` | JWT | 24h |
| `file.storage.local-path` | 上传目录 | `./data/uploads` |
| `knife4j.enable` | 接口文档 | `true` |

### 网关 `application.yml`

| 配置项 | 说明 |
|--------|------|
| `app.backend.base-url` | 后端地址；Docker 为 `http://admin-backend:8081` |
| `spring.cloud.gateway.routes` | `/api/auth/**`、`/api/system/**`、`/api/files/**`、`/api/doc.html` 等 |

网关 Sa-Token 已放行 Knife4j 路径（`/api/doc.html`、`/api/swagger-ui/**` 等），供管理端 iframe 加载文档。

### 前端

- 开发：`vite.config.ts`（`/api` + Knife4j 同源代理）
- 生产：`npm run build`，`nginx.conf` 反向代理网关与 `doc.html`

---

## 接口文档访问方式

| 场景 | 地址 |
|------|------|
| 管理端内嵌 | 登录后 **开发工具 → 接口文档** |
| RBAC 直连（开发） | http://localhost:8081/doc.html |
| 经网关 | http://localhost:8080/api/doc.html |

---

## 主要 API 前缀（经网关 `/api`）

| 前缀 | 说明 |
|------|------|
| `/api/auth/**` | 登录、注册、验证码、用户信息 |
| `/api/system/**` | 用户、角色、菜单、部门、**岗位**、字典、日志、工单、审批等 |
| `/api/files/**` | 文件上传与访问 |
| `/api/monitor/**` | API 访问统计、在线用户 |
| `/api/dashboard/**` | 仪表盘统计 |

---

## 数据库脚本

| 脚本 | 用途 |
|------|------|
| `sql/admin_platform.sql` | 建库、全表、菜单权限、演示数据（含组织管理、开发工具菜单） |

导入后请**重新登录**。

---

## 功能开发提示

### 字典下拉

```javascript
import { useDict } from '@/composables/useDict'
const { options, load } = useDict('sys_user_sex')
onMounted(() => load())
```

### 操作日志

Controller 方法添加 `@Log`，由 `LogAspect` 写入 `sys_oper_log`。

### 按钮权限

```html
<el-button v-permission="'system:user:create'">新增</el-button>
```

标识需与 `sys_menu.permission` 一致，例如 `system:post:create`、`tool:apiDoc:view`。

### 菜单外链

- `component` 以 `http://` 或 `https://` 开头 → 侧栏**新窗口**打开
- 填 `/doc.html` 或视图路径 → 走路由或 iframe 内嵌

## 构建与打包

```powershell
cd admin-frontend && npm run build
cd admin-backend && mvn clean package -DskipTests
cd admin-gateway && mvn clean package -DskipTests
```

改 Java 或前端运行代码后，Docker 环境可执行：`powershell -File scripts/docker-rebuild.ps1`（或 `docker compose up -d --build`）。

---

## 常见问题

**Q：登录后菜单为空或 403？**  
A：确认已执行 `sql/admin_platform.sql`，角色已分配菜单，然后重新登录。

**Q：看不到「组织管理」或「接口文档」？**  
A：确认全量脚本已导入，或为角色勾选对应菜单（如 5、150、151），再重新登录。

**Q：接口文档 iframe 空白或 500？**  
A：确认 RBAC、网关已启动；网关已放行 `/api/doc.html`；前端 dev 需重启以加载 Vite 代理；库中菜单 151 的 `component` 建议为 `/doc.html`。

**Q：Knife4j 下拉只有 default？**  
A：表示当前只有一个 OpenAPI 分组（全部接口），正常。可按模块配置 `GroupedOpenApi` 拆分并自定义中文名。

**Q：上传无法预览？**  
A：需登录且请求走网关 `/api/files/**`。

**Q：Git 仓库？**  
A：https://github.com/wushij/admin.git

---

## 相关文档

- [Docker 部署指南](./DOCKER_DEPLOY.md)
- [微服务说明](./MICROSERVICE_GUIDE.md)

---

## 许可证

本项目仅供学习与内部使用。生产部署前请修改默认密码、JWT 密钥等敏感配置。
