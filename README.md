# Admin Platform

基于 **Vue 3 + Spring Boot** 的企业级后台管理系统，提供用户权限、组织岗位、业务工单、系统监控、日志审计、文件与字典、**分组系统配置**、**注册审核**等能力，支持 Docker 一键部署与本地开发调试。

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
| **消息中心** | 系统通知（公告发布）、即时聊天（私聊/群聊）、WebSocket 实时推送 |
| **认证安全** | 图片/滑块验证码、登录/注册限流、Sa-Token 会话（Redis db=1） |

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

## 消息中心

消息中心与原有 **业务收件箱**（`sys_notice`，工单/审批/注册审核触达）并存，通过顶栏铃铛统一入口展示。

### 能力一览

| 能力 | 说明 |
|------|------|
| **业务消息** | 顶栏铃铛「业务消息」Tab，数据表 `sys_notice`，点击跳转工单/审批 |
| **系统通知** | 管理员在「系统通知」页发布广播/定向公告（`sys_announce`），用户顶栏「系统通知」Tab 查看 |
| **即时聊天** | 私聊 + 群聊；文本/表情/图片；在线状态；拉黑；群管（邀请/移除/禁言/转让/解散） |
| **群聊日志** | 群组详情 →「群聊日志」Tab，记录建群、邀请、退群等操作（`sys_chat_group_log`） |
| **实时推送** | WebSocket 推送新通知、私聊、群聊；顶栏角标与聊天页联动刷新 |

### 菜单与页面

| 菜单 | 路由 | 组件 | 权限 |
|------|------|------|------|
| 消息中心 | `/message` | — | 目录 |
| 系统通知 | `/message/notice` | `message/notice/index` | `system:announce:list`（管理端） |
| 即时聊天 | `/message/chat` | `message/chat/index` | `system:chat:list` |

普通用户默认拥有 **即时聊天** + 顶栏查看 **系统通知**，不含「系统通知」管理页（需 `system:announce:*`）。

### 主要 API（前缀 `/api`）

| 分类 | 路径 | 说明 |
|------|------|------|
| 汇总 | `GET /system/message/summary` | 业务 + 公告 + 聊天未读数 |
| 通知 | `/system/announce/*` | 分页、CRUD、发布、我的通知、已读、发送日志 |
| 私聊 | `POST /system/chat/send` | 发消息 |
| 私聊 | `GET /system/chat/history/{targetId}` | 历史记录 |
| 私聊 | `POST /system/chat/read/{senderId}` | 标记已读 |
| 群聊 | `/system/chat/group/*` | 建群、成员、消息、禁言、转让等 |
| 群聊 | `GET /system/chat/group/{groupId}/logs` | 群操作日志 |
| 聊天图片 | `POST /system/chat/upload/image` | 上传至 `images/chat/` 目录，**不出现在文件管理列表** |
| WebSocket | `ws(s)://{host}/api/ws/message?token=...` | 推送类型：`notice` / `chat` / `groupChat` |

### 前端关键文件

```
frontend/src/
├── api/message/index.ts          # 通知、聊天、群聊 API
├── store/message.ts              # 未读汇总、WebSocket、群未读角标
├── types/message.ts              # 消息相关类型
├── utils/messageWebSocket.ts     # WS 连接封装
├── components/MessageNotification.vue  # 新消息浮层提示
└── views/message/
    ├── notice/index.vue          # 系统通知管理（发布/发送日志）
    └── chat/index.vue            # 即时聊天（私聊/群聊/群组详情）
```

后端：`modules/system/api/message/`（`AnnounceController`、`ChatController`）、`framework/websocket/`（`WebSocketConfig`、`MessageWebSocketHandler`）。

### 权限标识

| 权限 | 说明 |
|------|------|
| `system:announce:list` | 进入通知管理页 |
| `system:announce:create/update/delete/publish` | 通知 CRUD 与发布 |
| `system:chat:list` | 即时聊天与聊天图片上传 |

### 数据库

全量安装：`sql/admin_platform.sql` 已含消息中心表（§11b）及 `sys_chat_group_log`。

**已有库增量**（按顺序执行，均可重复执行、无 DROP）：

| 脚本 | 用途 |
|------|------|
| `sql/add1.sql` | 注册验证码类型、系统配置补全等 |
| `sql/add2.sql` | **消息中心**（通知/聊天/群聊表 + 菜单 170–178） |
| `sql/add3.sql` | **群聊操作日志**表 `sys_chat_group_log` |

```bash
mysql -u root -p wu-admin < sql/add2.sql
mysql -u root -p wu-admin < sql/add3.sql
```

执行涉及菜单的脚本后请 **重新登录** 以刷新侧栏。升级后需 **重启后端** 使 WebSocket 与新接口生效。

> 说明：历史聊天图片若曾走通用文件上传，可能仍出现在文件列表；升级后新发的聊天图片走专用目录，列表会自动排除。

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
- 开发：文档静态资源经 Vite 代理到后端 `8080`；**调试请求**默认 `http://localhost:3000/api`
- 生产：Nginx → 后端 `/api/doc.html`、`/api/v3/api-docs` 等
- 调试需带请求头 `Authorization: <登录 token>`，修改类接口用 **PUT/POST**，勿用 GET
- Spring Boot **3.5** 需 **springdoc ≥ 2.8.9**；`knife4j.enable` 建议为 `false`（4.5.0 增强模块与 springdoc 2.8 API 不兼容，关闭后 `doc.html` 仍正常）

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios、ECharts、Three.js |
| 后端 | Spring Boot 3.5、Spring Security 6、Sa-Token、MyBatis-Plus 3.5、Druid、Knife4j 4.5、Springdoc 2.8 |
| 数据 | MySQL 8、Redis 7 |
| 部署 | Docker Compose、Nginx |

**JDK 17**。核心版本见下表：

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.5.13（`backend`） |
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
│    frontend     │ ──────────────► │     backend      │
│  Vite / Nginx   │   /doc.html*    │  Spring Boot     │
│  :3000          │ ──────────────► │  :8080           │
└─────────────────┘                 └────────┬─────────┘
                                             │
                         ┌───────────────────┴───────────────────┐
                         ▼                                       ▼
                  ┌─────────────┐                        ┌─────────────┐
                  │   MySQL     │                        │   Redis     │
                  │  wu-admin   │                        │  database 1 │
                  └─────────────┘                        └─────────────┘
```

- 业务 API 前缀：`/api`（后端 `server.servlet.context-path=/api`）
- 示例：`/api/system/config-group/list`

---

## 目录结构

```
admin-vue/
├── backend/                    # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/java/cn/rbac/server/
│   │   ├── common/             # 通用 POJO、工具类
│   │   ├── framework/          # 安全、MyBatis、Redis、Web 过滤器等
│   │   └── modules/system/     # 系统业务（api / service / dal）
│   └── src/main/resources/
│       └── application.yml
├── frontend/                   # Vue 3 + TypeScript 前端
│   ├── src/
│   │   ├── api/                # 接口封装（system、message、monitor 等，均为 .ts）
│   │   ├── views/              # 页面（system、message、monitor、login 等）
│   │   ├── components/         # 公共组件（DictSelect、SliderCaptcha、MessageNotification、earth/Earth3D 等）
│   │   ├── router/             # 路由与守卫
│   │   ├── store/              # Pinia（user、message 等）
│   │   ├── types/              # TS 类型（api、message、config）
│   │   ├── utils/              # request、主题、菜单、WebSocket 工具
│   │   └── directives/         # v-permission 等指令
│   ├── tsconfig.json
│   ├── vite.config.ts          # 开发代理 /api → backend:8080
│   ├── nginx.conf              # 生产静态资源与 API 反代
│   └── Dockerfile
├── sql/
│   ├── admin_platform.sql      # 全量安装 + 文末「附录」升级段
│   ├── add1.sql                # 已有库增量（配置/注册等）
│   ├── add2.sql                # 已有库增量（消息中心）
│   └── add3.sql                # 已有库增量（群聊操作日志）
├── data/                       # 本地上传目录（git 忽略，对应 file.storage.local-path）
├── Dockerfile                  # 后端镜像（根目录）
├── docker-compose.yml          # MySQL + Redis + backend + frontend
├── DOCKER_DEPLOY.md
└── README.md
```

### 后端包结构（`backend/src/main/java`，分层约定）

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
        ├── api/                      # REST Controller
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
| 复用基础层 | 将 `common` + `framework` 打成 jar，供其它 Spring Boot 项目依赖 |

---

## 环境要求

| 软件 | 版本建议 |
|------|----------|
| Node.js | 18+ |
| Maven | 3.6+ |
| JDK | 17 |
| Spring Boot | **3.5.13** |
| Springdoc / Knife4j | 2.8.9 / 4.5.0（见上文接口文档说明） |
| MySQL | 8.0 |
| Redis | 7.x |
| Docker Desktop | 可选 |

---

## 快速开始

### 方式一：Docker Compose（推荐）

```powershell
cd admin-vue
docker compose up -d --build
```

首次启动会执行 `sql/admin_platform.sql`（`docker-entrypoint-initdb.d` 仅对**空数据卷**生效）。

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| 后端 API | http://localhost:8080/api |
| MySQL | `127.0.0.1:3307`，库 `wu-admin`，`root`/`root` |
| Redis | `127.0.0.1:6379`，database `1` |

详见 [DOCKER_DEPLOY.md](./DOCKER_DEPLOY.md)。

改 Java/前端代码后重建 Docker：

```powershell
docker compose up -d --build
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

#### 3. 启动后端（8080）

```powershell
cd backend
mvn spring-boot:run -DskipTests
```

#### 4. 启动前端（3000）

```powershell
cd frontend
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

### 后端 `application.yml`

| 配置项 | 说明 |
|--------|------|
| `server.port` | `8080` |
| `server.servlet.context-path` | `/api`（统一 API 前缀） |
| `spring.datasource.*` | MySQL `wu-admin` |
| `spring.redis.database` | `1` |
| `spring.servlet.multipart.max-file-size` | 平台物理上限 **500MB** |
| `sa-token.*` | Sa-Token 缺省（`session.tokenExpireHours` 在登录时覆盖 timeout） |
| `file.storage.*` | 上传目录与缺省限制 |
| `auth.security.*` | 验证码与限流缺省 |
| `knife4j.enable` | 接口文档增强开关；**3.5 + springdoc 2.8 建议 `false`**（见 `application.yml` 注释） |
| `springdoc.api-docs.path` | OpenAPI JSON 路径，默认 `/v3/api-docs` |

运行时优先读取 `sys_config_group`（`SystemConfigHelper` 实现 `DynamicConfigProvider`，供 Token、文件上传等使用）。

**Redis 业务缓存**：启动时预热系统配置（`cache:sys:config:groups`）与字典（`cache:sys:dict:*`）；修改配置或字典后自动刷新。字典管理页「刷新缓存」会同步刷新服务端 Redis。

**日志策略**：操作日志 `@Async` 写入；登录日志异步写入；API 访问日志先入 Redis 队列再批量落库。每天凌晨按 `app.log.retention.*` 清理过期日志（默认操作/登录 90 天、API 访问 30 天）。

### 前端

- 开发：`frontend/vite.config.ts`（`/api` 代理到 `localhost:8080`）
- 生产：`npm run build` + `frontend/nginx.conf`

---

## 主要 API 前缀（`/api`）

| 前缀 | 说明 |
|------|------|
| `/api/auth/**` | 登录、注册、验证码、`config`（公开配置） |
| `/api/system/**` | 用户、角色、菜单、组织、字典、**config-group**、审批、工单、**announce/chat** 等 |
| `/api/files/**` | 文件上传与访问 |
| `/api/monitor/**` | API 访问、在线用户 |
| `/api/dashboard/**` | 工作台统计（含配置摘要、待审核用户数） |

---

## 数据库脚本

维护 **`sql/admin_platform.sql`**（全量）及增量脚本 **`add1.sql` / `add2.sql` / `add3.sql`**。

| 场景 | 做法 |
|------|------|
| **全新安装** | 执行全文：`mysql -u root -p wu-admin < sql/admin_platform.sql`（空库） |
| **已有库升级（配置/注册等）** | `mysql -u root -p wu-admin < sql/add1.sql` |
| **已有库升级消息中心** | `mysql -u root -p wu-admin < sql/add2.sql` |
| **已有库升级群聊日志** | `mysql -u root -p wu-admin < sql/add3.sql` |
| **仅补索引** | 执行 `add1.sql` 末尾 `ALTER TABLE`，或全文 `admin_platform.sql` 附录索引段 |

增量脚本均 **无 DROP**，可重复执行。执行涉及菜单的升级后请 **重新登录**。

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

常量见 `frontend/src/constants/dict.ts`。字典管理页修改数据后点「刷新缓存」，或调用 `clearDictCache('sys_normal_disable')`。

脚本方式：`useDict('sys_user_sex')` + `onMounted(() => load())`（见 `composables/useDict.ts`）。

### 操作日志

在 `modules/system/api` 的 Controller 方法上添加 `@Log`（`framework.log.annotation`），由 `modules/system/framework/operlog/LogAspect` 经 `OperLogRecorder` 写入 `sys_oper_log`。

### 按钮权限

```html
<el-button v-permission="'system:config:update'">保存</el-button>
```

标识与 `sys_menu.permission` 一致，如 `system:approval:approve`。

### 登录 / 注册页

- 路由：`/login`、`/register`；左侧为 **Three.js 3D 地球**（`frontend/src/components/earth/Earth3D.vue`），透明画布透出粒子星空，支持鼠标拖拽旋转与滚轮缩放。
- 文案与验证码等行为由公开配置驱动，见下节。

### 登录页读取配置

```typescript
// frontend/src/api/system/auth/index.ts
GET /auth/config  // 实际请求 /api/auth/config
```

滑块验证码组件见 `frontend/src/components/SliderCaptcha.vue`。

---

### 前端 TypeScript

- 业务代码均为 **`.ts`**，Vue 页面使用 `<script setup lang="ts">`。
- 类型检查：`cd frontend && npm run typecheck`（`vue-tsc --noEmit`）。
- 生产构建会先跑类型检查：`npm run build`。

### 构建与打包

```powershell
cd frontend && npm run build
cd backend && mvn clean package -DskipTests
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
A：① 确认后端（8080）已启动，浏览器访问 `http://127.0.0.1:8080/api/v3/api-docs` 应返回 JSON；② 开发环境重启 Vite 以加载 Knife4j 代理；③ 勿将 springdoc 降为 2.6（与 Spring Boot 3.5 不兼容）；④ `knife4j.enable` 保持 `false` 直至升级兼容的 Knife4j 版本。

**Q：Knife4j 调试 404 或返回 HTML？**  
A：已配置 OpenAPI 默认服务 `http://localhost:3000/api`；重启后端与 Vite 后，在文档页选择该服务器、方法用 PUT/POST，并填 `Authorization`。若仍 401，先登录管理端复制 token。

**Q：上传失败提示大小或类型？**  
A：在 **系统配置 → 文件存储** 调整；单文件上限不得超过 500MB。

**Q：消息中心菜单不显示或聊天 403？**  
A：对已有库执行 `add2.sql`（及 `add3.sql` 若需群聊日志），重启后端后 **重新登录**。普通用户需角色分配菜单 170/172；只读权限用户访问 `:list` 接口时会映射为 `:query`。

**Q：顶栏有通知角标但列表为空？**  
A：确认 WebSocket 已连接（登录后自动初始化）；在顶栏铃铛打开「系统通知」Tab 会拉取列表。管理员发布通知需 `system:announce:publish`。

**Q：聊天图片出现在文件管理里？**  
A：升级后新图片走 `/system/chat/upload/image`，存储于 `images/chat/` 且文件列表已排除；历史旧数据可手动删除。

**Q：Git 仓库？**  
A：https://github.com/wushij/wu-admin

---

## 相关文档

- [Docker 部署指南](./DOCKER_DEPLOY.md)
- [GitHub 上传与推送](./GitHub上传与推送全流程.md)

---

## 许可证

本项目仅供学习与内部使用。生产部署前请修改默认密码、数据库与 Redis 等敏感配置。
