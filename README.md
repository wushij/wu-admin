# Admin Platform

基于 **Vue 3 + Spring Cloud Gateway + Spring Boot RBAC** 的企业级后台管理系统，提供用户权限、组织岗位、业务工单、系统监控、日志审计、文件与字典、**分组系统配置**、**注册审核**等能力，支持 Docker 一键部署与本地开发调试。

---

## 功能概览

| 模块 | 说明 |
|------|------|
| **系统管理** | 用户、角色、菜单、组织管理、字典管理、**系统配置** |
| **组织管理** | 部门体系 + 岗位体系；左树右表、拖拽调整部门、岗位成员、部门回收站 |
| **用户管理** | 部门、岗位多选、角色单选、回收站；支持**待审核 / 已驳回**状态 |
| **菜单管理** | 树形表格；目录/菜单/按钮联动；图标网格选择器；外链新窗口打开 |
| **系统配置** | 六分组 Tab：基础信息、会话令牌、文件存储、接口限流、登录认证、注册认证 |
| **开发工具** | 内嵌 Knife4j 接口文档（`doc.html`） |
| **系统日志** | 操作日志（AOP）、登录日志 |
| **系统监控** | API 访问统计、在线用户与强退 |
| **文件管理** | 分组、上传、预览；大小与扩展名受**系统配置**约束 |
| **业务中心** | 工单管理、审批单中心（含**注册审核单** `REGISTER`） |
| **认证安全** | 图片/滑块验证码、登录/注册限流、网关 + 后端共用 Sa-Token（Redis db=1） |

菜单与权限由数据库 `sys_menu` 动态加载；超级管理员默认拥有全部功能。修改菜单或角色后需**重新登录**侧栏才会更新。

---

## 系统配置（`/system/config`）

配置存储在表 `sys_config_group`，按 `group_code` 分组，值为 JSON。管理端 **系统管理 → 系统配置** 可编辑；部分项保存后**立即生效**（无需重启）。

| 分组编码 | 名称 | 主要字段 | 生效范围 |
|----------|------|----------|----------|
| `site` | 基础信息 | 平台名称、副标题、登录/注册页标题、版权 | 登录页、注册页、工作台展示 |
| `session` | 会话配置 | `tokenExpireHours`（1～720） | Sa-Token 会话 TTL |
| `file` | 文件配置 | `maxSizeMb`、`allowedExtensions` | 上传校验（上限不超过平台 500MB） |
| `rateLimit` | 接口限流 | 验证码/登录/注册 每分钟每 IP 次数（0=不限） | 认证接口防刷 |
| `login` | 登录认证 | 验证码开关、类型（`image`/`slider`）、记住我、重试锁定 | 登录流程 |
| `register` | 注册认证 | 开放注册、验证码、默认角色、**需审核**、密码最小长度 | 注册流程 |

**公开接口**（无需登录）：`GET /api/auth/config`，返回 `site`、`login`、`register` 等前端登录/注册页所需配置。

**管理接口**（需权限 `system:config:list` / `system:config:update`）：

- `GET /api/system/config-group/list`
- `GET /api/system/config-group/{groupCode}`
- `PUT /api/system/config-group/{groupCode}`，body：`{ "configValue": "{...json...}" }`

`application.yml` 中 `sa-token.timeout`、`auth.security.*`、`file.storage.*` 为**缺省兜底**；库中有对应分组时以库为准（`session.tokenExpireHours` 在登录时写入 Sa-Token 超时）。

---

## 注册审核

在 **系统配置 → 注册认证** 开启「注册需审核」后：

1. 用户注册成功，账号 `status = 2`（待审核），分配配置中的默认角色；
2. 自动创建类型为 `REGISTER` 的审批单，并通知超级管理员（站内通知）；
3. 管理员在 **业务中心 → 审批单中心** 通过或驳回；
4. 通过 → `status = 1` 可登录；驳回 → `status = 3`；
5. 申请人收到审核结果通知。

用户状态约定：

| status | 含义 |
|--------|------|
| `0` | 停用 |
| `1` | 正常 |
| `2` | 待审核 |
| `3` | 审核驳回 |

工作台统计含「待审核用户」数量（`userPendingCount`）。

---

## 组织管理 / 菜单 / 接口文档

### 组织管理（`/system/org`）

- Tab：**部门体系 | 岗位体系**
- 部门：树形、`ancestors`、拖拽、回收站
- 岗位：`sys_user_post` 关联、组织内成员

### 菜单管理（`/system/menu`）

- 类型目录/菜单/按钮联动表单；`IconSelect` 图标选择
- `component` 以 `http(s)://` 开头 → 侧栏新窗口；`/doc.html` → iframe 内嵌

### 接口文档（`/tool/api-doc`）

- **开发工具 → 接口文档**，内嵌 Knife4j（基于 **Springdoc OpenAPI 3**）
- 开发：文档静态资源 → RBAC `8081`；**调试请求**默认 `http://localhost:8080/api`（Vite 已将 `/system`、`/auth` 等代理到网关）
- 生产：Nginx → 网关 `/api/doc.html`、`/api/v3/api-docs` 等 → RBAC
- 调试需带请求头 `Authorization: <登录 token>`，修改类接口用 **PUT/POST**，勿用 GET
- Spring Boot **3.5** 需 **springdoc ≥ 2.8.9**；`knife4j.enable` 建议为 `false`（4.5.0 增强模块与 springdoc 2.8 API 不兼容，关闭后 `doc.html` 仍正常）

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、Vite、Element Plus、Pinia、Axios、ECharts |
| 网关 | Spring Boot 3.5、Spring Cloud Gateway 2025.0、Sa-Token、Redis |
| 后端 | Spring Boot 3.5、Spring Security 6、MyBatis-Plus 3.5、Druid、Knife4j 4.5、Springdoc 2.8 |
| 数据 | MySQL 8、Redis 7 |
| 部署 | Docker Compose、Nginx |

**JDK 17**（RBAC + 网关）。核心版本见下表：

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.5.13（`admin-backend`、`admin-gateway`） |
| Spring Cloud | 2025.0.0（网关） |
| Springdoc OpenAPI | 2.8.9（须 ≥ 2.8.9，兼容 Spring 6.2） |
| Knife4j | 4.5.0（`knife4j.enable: false` 关闭增强以避免与 springdoc 冲突） |
| MyBatis-Plus | 3.5.9 |

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
                                           │
                                           ▼
                                  ┌──────────────────┐
                                  │  admin-backend   │
                                  │  :8081           │
                                  └────────┬─────────┘
                                           │
                           ┌───────────────┴───────────────┐
                           ▼                               ▼
                    ┌─────────────┐                ┌─────────────┐
                    │   MySQL     │                │   Redis     │
                    │   RBAC1     │                │  database 1 │
                    └─────────────┘                └─────────────┘
```

- 业务 API 前缀：`/api`（网关去掉 `/api` 转发 RBAC）
- 示例：`/api/system/config-group/list` → `/system/config-group/list`

---

## 目录结构

```
admin/
├── admin-gateway/              # 微服务网关
├── admin-backend/              # RBAC 后端（见下方「后端包结构」）
├── admin-frontend/             # Vue3 前端
├── sql/
│   └── admin_platform.sql      # 唯一脚本：全量安装 + 文末「附录」升级段
├── scripts/
│   └── docker-rebuild.ps1
├── Dockerfile
├── docker-compose.yml
├── DOCKER_DEPLOY.md
├── MICROSERVICE_GUIDE.md
└── README.md
```

### 后端包结构（`admin-backend`，B 方案分层）

依赖方向：**`modules/*` → `framework` → `common`**（`framework` 禁止引用 `modules`）。

```
cn.rbac.server/
├── common/
│   ├── pojo/                         # CommonResult、PageParam、PageResult
│   └── util/                         # ClientIpUtils、UserAgentUtils
├── framework/                        # 技术基础设施（可抽公共 starter）
│   ├── config/                       # DynamicConfigProvider（SPI）
│   ├── security/
│   │   ├── api/                      # PermissionApi（SPI）
│   │   ├── config/                   # SecurityConfig
│   │   └── core/                     # TokenService、SecurityUtils
│   ├── web/
│   │   ├── core/                     # GlobalExceptionHandler
│   │   └── filter/                   # SaTokenAuthenticationFilter（Sa-Token → Spring Security 桥接）
│   ├── log/annotation/               # @Log
│   ├── mybatis/、redis/、storage/
└── modules/
    └── system/                       # 系统域业务
        ├── api/                      # REST Controller（原 controller.admin）
        ├── service/、dal/
        └── framework/                # 对本项目 framework SPI 的实现
            ├── config/               # SystemConfigProvider
            ├── security/             # SystemPermissionService（bean 名 ss）
            ├── operlog/              # LogAspect、OperLogRecorder
            └── monitor/              # API 访问采集拦截器
```

| 扩展场景 | 做法 |
|----------|------|
| 改会话/上传限制等运行时配置 | 改库表 `sys_config_group`，经 `SystemConfigHelper` → `SystemConfigProvider` |
| 新增业务模块 | 增加 `modules/xxx`，在 `xxx/framework` 实现 SPI，勿让 `framework` 依赖业务 |
| 拆独立微服务 | 将 `framework` + `common` 打成 jar，新业务服务只依赖该 jar（见 [MICROSERVICE_GUIDE.md](./MICROSERVICE_GUIDE.md)） |

---

## 环境要求

| 软件 | 版本建议 |
|------|----------|
| Node.js | 18+ |
| Maven | 3.6+ |
| JDK | 17（RBAC + 网关） |
| Spring Boot | **3.5.13**（RBAC + 网关，自 2.7 升级） |
| Spring Cloud | 2025.0.0（网关） |
| Springdoc / Knife4j | 2.8.9 / 4.5.0（见上文接口文档说明） |
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

首次启动会执行 `sql/admin_platform.sql`（`docker-entrypoint-initdb.d` 仅对**空数据卷**生效）。

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| 网关 | http://localhost:8080 |
| admin-backend（宿主机映射） | http://localhost:8082 |
| MySQL | `127.0.0.1:3307`，库 `RBAC1`，`root`/`root` |
| Redis | `127.0.0.1:6379`，database `1` |

详见 [DOCKER_DEPLOY.md](./DOCKER_DEPLOY.md)。

改 Java/前端代码后重建：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/docker-rebuild.ps1
```

---

### 方式二：本地开发

#### 1. 初始化数据库

```bash
mysql -u root -p < sql/admin_platform.sql
```

> 全量脚本含 `DROP TABLE`，仅用于新库。已有旧库按下方「数据库脚本」顺序执行增量。

#### 2. 启动 Redis

`127.0.0.1:6379`，database **1**。

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

---

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | 超级管理员 |
| `zhangsan` | `admin123` | 普通用户 |

---

## 配置说明

### RBAC `application.yml`

| 配置项 | 说明 |
|--------|------|
| `server.port` | `8081` |
| `spring.datasource.*` | MySQL `RBAC1` |
| `spring.redis.database` | `1` |
| `spring.servlet.multipart.max-file-size` | 平台物理上限 **500MB** |
| `sa-token.*` | Sa-Token 缺省（`session.tokenExpireHours` 在登录时覆盖 timeout） |
| `file.storage.*` | 上传目录与缺省限制 |
| `auth.security.*` | 验证码与限流缺省 |
| `knife4j.enable` | 接口文档增强开关；**3.5 + springdoc 2.8 建议 `false`**（见 `application.yml` 注释） |
| `springdoc.api-docs.path` | OpenAPI JSON 路径，默认 `/v3/api-docs` |

运行时优先读取 `sys_config_group`（`SystemConfigHelper` 实现 `DynamicConfigProvider`，供 Token、文件上传等使用）。

### 网关

- `app.backend.base-url`：Docker 为 `http://admin-backend:8081`
- 已放行：`/api/auth/login`、`/api/auth/register`、`/api/auth/captcha`、`/api/auth/config`、Knife4j 静态路径等

### 前端

- 开发：`vite.config.ts`
- 生产：`npm run build` + `nginx.conf`

---

## 主要 API 前缀（经网关 `/api`）

| 前缀 | 说明 |
|------|------|
| `/api/auth/**` | 登录、注册、验证码、`config`（公开配置） |
| `/api/system/**` | 用户、角色、菜单、组织、字典、**config-group**、审批、工单等 |
| `/api/files/**` | 文件上传与访问 |
| `/api/monitor/**` | API 访问、在线用户 |
| `/api/dashboard/**` | 工作台统计（含配置摘要、待审核用户数） |

---

## 数据库脚本

仅维护 **`sql/admin_platform.sql`** 一个文件。

| 场景 | 做法 |
|------|------|
| **全新安装** | 执行全文：`mysql -u root -p < sql/admin_platform.sql` |
| **已有库升级** | 只执行文末「附录：已有库升级」段（或执行全文亦可，建表语句为 IF NOT EXISTS） |

执行涉及菜单的升级后请**重新登录**。

---

## 开发提示

### 字典（业务闭环）

登录后布局会自动预加载 `sys_normal_disable`、`sys_user_sex`、`sys_yes_no`。业务页优先用全局组件：

```html
<!-- 筛选/表单下拉 -->
<DictSelect v-model="form.status" dict-type="sys_normal_disable" value-type="number" />

<!-- 列表标签回显 -->
<DictTag :value="row.status" dict-type="sys_normal_disable" />
```

常量见 `admin-frontend/src/constants/dict.js`。字典管理页修改数据后点「刷新缓存」，或调用 `clearDictCache('sys_normal_disable')`。

脚本方式：`useDict('sys_user_sex')` + `onMounted(() => load())`（见 `composables/useDict.js`）。

### 操作日志

在 `modules/system/api` 的 Controller 方法上添加 `@Log`（`framework.log.annotation`），由 `modules/system/framework/operlog/LogAspect` 经 `OperLogRecorder` 写入 `sys_oper_log`。

### 按钮权限

```html
<el-button v-permission="'system:config:update'">保存</el-button>
```

标识与 `sys_menu.permission` 一致，如 `system:approval:approve`。

### 登录页读取配置

```javascript
// admin-frontend/src/api/system/auth/index.js
GET /auth/config  // 经网关 /api/auth/config
```

滑块验证码实现可参考目录 `滑块验证码-关键代码与提示词/`。

---

## 构建与打包

```powershell
cd admin-frontend && npm run build
cd admin-backend && mvn clean package -DskipTests
cd admin-gateway && mvn clean package -DskipTests
```

---

## 常见问题

**Q：登录后菜单为空或 403？**  
A：确认已导入 `admin_platform.sql` 或为角色分配菜单，然后重新登录。

**Q：系统配置页报错或只有 login/register？**  
A：对已有库执行 `admin_platform.sql` 文末「附录：已有库升级」段，补全 site/session/file/rateLimit 等分组。

**Q：注册后无法登录？**  
A：若开启「注册需审核」，需管理员在审批单中心通过；登录提示「账号待审核」属正常。

**Q：接口文档 iframe 空白或 `/v3/api-docs` 403？**  
A：① 确认 RBAC（8081）已启动，浏览器访问 `http://127.0.0.1:8081/v3/api-docs` 应返回 JSON；② 开发环境重启 Vite 以加载 Knife4j 代理；③ 生产环境确认网关已放行 `/api/v3/api-docs`；④ 勿将 springdoc 降为 2.6（与 Spring Boot 3.5 不兼容）；⑤ `knife4j.enable` 保持 `false` 直至升级兼容的 Knife4j 版本。

**Q：Knife4j 调试 404 或返回 HTML？**  
A：已配置 OpenAPI 默认服务 `http://localhost:8080/api`；重启后端与 Vite 后，在文档页选择该服务器、方法用 PUT/POST，并填 `Authorization`。若仍 401，先登录管理端复制 token。

**Q：上传失败提示大小或类型？**  
A：在 **系统配置 → 文件存储** 调整；单文件上限不得超过 500MB。

**Q：Git 仓库？**  
A：https://github.com/wushij/admin.git

---

## 相关文档

- [Docker 部署指南](./DOCKER_DEPLOY.md)
- [微服务说明](./MICROSERVICE_GUIDE.md)
- [滑块验证码参考](./滑块验证码-关键代码与提示词/README.md)

---

## 许可证

本项目仅供学习与内部使用。生产部署前请修改默认密码、数据库与 Redis 等敏感配置。
