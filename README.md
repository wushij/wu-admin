# Admin Platform

基于 **Vue 3 + Spring Boot** 的企业级后台管理系统，配套 **uni-app 移动端（H5 / 微信小程序）**，提供工作台、RBAC 权限、组织岗位、工单审批、系统监控、日志审计、文件字典、分组系统配置（含第三方/支付/短信）、个人中心、注册审核、企业 IM 等能力，支持本地开发与自建部署。

## 目录

- [功能概览](#功能概览)
- [最新更新](#最新更新202606)
- [近期迭代归档](#近期迭代归档)
- [系统配置](#系统配置systemconfig)
- [注册审核](#注册审核)
- [消息中心](#消息中心)
- [技术栈与架构](#技术栈)
- [目录结构](#目录结构)
- [环境要求与快速开始](#环境要求)
- [配置说明](#配置说明)
- [数据库脚本](#数据库脚本)
- [开发提示](#开发提示)
- [构建与 CI](#构建与打包)
- [常见问题](#常见问题)

---

## 功能概览

| 模块 | 说明 |
|------|------|
| **工作台** | PC / 移动端统计（用户/角色/部门/文件、**企业 IM 未读**、**系统配置分组数**等）、待办提醒、快捷入口、最近登录；`/dashboard/*` |
| **系统管理** | 用户、角色、菜单、组织、字典（含**类型复制**）、**系统配置**；用户列表默认仅展示**已入库**用户（启用/停用）；待审核/驳回在审批单中心处理 |
| **组织管理** | 部门 + 岗位；左树右表、拖拽调整、岗位成员；部门负责人关联 **用户 ID**（`leader_user_id`），昵称变更自动同步；软删数据在 **回收中心** 统一恢复 |
| **移动端（uni-app）** | H5 / 微信小程序；工作台、用户/组织/字典、工单审批、企业 IM、监控运维、**个人中心**（资料/账号/短信换绑）；**H5 刷新后浅栈返回**（`localStorage` 父级映射 + 列表页内返回条）；与 PC 共用 `/api` |
| **菜单管理** | 树形表格；目录/菜单/按钮联动；图标选择器；外链新窗口 / iframe 内嵌 |
| **系统配置** | 十分组 Tab：基础信息、会话、文件、限流、登录/注册认证、**第三方配置**、**支付配置**、**短信配置**、安全配置；支付支持**测试订单**与异步回调；短信支持**测试发送**与发送记录 |
| **个人中心** | 顶栏入口 `/profile`：资料编辑、头像上传、**短信验证绑定/更换手机号**（发码前强制滑块）、自助改密、**短信验证重置密码**（忘记当前密码时）、我的登录记录 |
| **回收中心** | **系统管理 → 回收中心**（`/system/recycle`）：12 类软删数据统一汇总、分页、恢复与彻底删除（见下文） |
| **列表导出** | 用户、登录/操作日志、工单、审批、API 访问、在线用户等列表支持 **Excel / CSV** 导出（当前筛选 / 全部） |
| **开发工具** | 内嵌 Knife4j 接口文档（`doc.html`）；**代码生成**（导入表结构、字段配置、预览/下载 ZIP、生成至项目并建菜单） |
| **系统日志** | 操作日志（AOP，含详情）；登录日志（**ip2region IP 归属地**、浏览器解析） |
| **系统监控** | API 访问统计（ECharts 图表 + 日志列表）、在线用户与强退、**定时任务**（Quartz 调度、内置清理任务，默认暂停）、**缓存监控**（Redis 内存/QPS/命中率/连接数趋势 + SCAN 键管理与详情）、**服务监控**（本机 JMX：CPU/内存/JVM/磁盘） |
| **文件管理** | 分组 CRUD、按类型筛选；图片/PDF/Office 预览；大小与扩展名受**系统配置**约束 |
| **流程中心** | **工单**：优先级、截止/超时、评论附件、指派与全员通知（非超管仅看本人相关）；**审批**：请假/采购/报销/用印/合同/通用 + `REGISTER` 注册审核，**详情抽屉内可直接通过/驳回**，支持归档 |
| **消息中心** | **业务消息**（`sys_notice`，工单/审批触达）、系统通知（全员/用户/部门定向、发送日志）、**企业IM**（私聊/群聊、文件、@、撤回、正在输入等）、WebSocket |
| **认证安全** | 图片/滑块验证码、**短信验证码登录**（独立开关，与账号验证码分离）、**短信发码前滑块**（可选）、登录失败锁定（用户+IP）、记住我、登录/注册/短信**限流防刷**；账号密码错误统一提示「账号或密码错误」；Sa-Token 会话（Redis db=1） |
| **界面体验** | 主题色切换；登录/注册页 Three.js 地球 + 粒子背景；顶栏消息铃铛三 Tab；管理页统一 **module-page** 头图/搜索/表格样式，Hero 图标与侧栏菜单一致 |

侧栏菜单由 `sys_menu` 按角色动态渲染（超级管理员默认全部）；页面路由在 `frontend/src/router` **静态注册**，新增菜单时需保证 `path` 与路由一致。修改菜单或角色后需**重新登录**刷新侧栏。

---

## 最新更新（2026.06）

近期发版要点（联调 / 升级时优先对照）。

| 模块 | 变更 |
|------|------|
| **移动端 H5 返回** | F5 刷新后页面栈为 1 时：`localStorage` 持久化父级路由（`pinNavParent`）、列表页 **`SubPageBackBar`** 内返回条、Tab 页 `onShow` 不再误关返回按钮；详情/编辑在 `onLoad` 锁定返回目标；子包列表 `redirectTo` 回退避免 `switchTab` 闪屏 |
| **移动端个人中心** | 手机号绑定/更换独立页 `mobile-bind`；账号状态与 PC 对齐（`1=正常`、`0=已停用`）；「我的」卡片昵称布局微调 |
| **工作台统计** | PC / 移动端新增 **企业 IM 未读**（`chatUnreadCount`）、**系统配置分组数**（`configGroupCount`） |
| **系统通知** | 公告详情发布人昵称/头像按 `createBy` **实时解析**，改名后不再显示旧昵称 |
| **部门负责人** | `sys_dept.leader_user_id` 关联用户；PC / 移动端选择用户，改名同步 `leader_name`；增量 `add15.sql` / `add15_wuadmin.sql` |
| **定时任务日志** | `sys_job_log` 增加 `duration_ms`；调度日志支持软删并在回收中心恢复；本地 `add16.sql` + `add17.sql`，生产合并至 **`add15_wuadmin.sql`** |

```bash
# 本地增量（按已执行版本补跑）
mysql -u root -p wu-admin < sql/add15.sql
mysql -u root -p wu-admin < sql/add16.sql
mysql -u root -p wu-admin < sql/add17.sql

# 移动端 H5
cd uniapp && npm install && npm run dev:h5
```

生产 H5：`npm run build:h5` 后部署静态资源，**`/api` 反代到后端**（与 PC 相同）。

---

## 近期迭代归档

以下为历史迭代摘要；细节见各专题章节与 [常见问题](#常见问题)。

### 移动端 uni-app 基础（2026.06）

| 项 | 说明 |
|------|------|
| **工程** | `uniapp/`（Vue 3 + TS + Pinia）；`npm run dev:h5` / `build:h5` |
| **H5 导航** | `navigateTo` 拦截记录来源；选择页 URL 带 `from`；`navigateToParent` 浅栈回退 |
| **交互** | `FormCell` 去除 H5 下 `@click` + `@tap` 重复触发导致 `navigateTo` 被取消 |

### 架构与代码质量优化（2026.06）

本轮对前后端做了系统性重构，在**不改变对外 API 与页面行为**的前提下提升可维护性、安全性与性能。

#### 后端：瘦 Controller + Service 分层

| 维度 | 改进要点 |
|------|----------|
| **分层** | 认证、用户、角色、工单、**工作台**、**消息汇总**等模块将业务逻辑从 Controller 下沉至 Service（`AuthService`、`UserService`、`RoleService`、`TicketService`、`DashboardService`、`NoticeService` 等）；Controller 负责参数校验、权限注解与结果封装 |
| **异常** | `BusinessException` + `GlobalExceptionHandler`（17 类异常）+ `BusinessHttpStatusMapper` 对齐 HTTP 状态；Security 401/403 JSON 化；详见 [全局异常处理](#全局异常处理与错误契约) |
| **安全** | `UserDO.password` 添加 `@JsonIgnore`；管理端敏感接口补全 `@PreAuthorize`；CORS 改为 `app.cors.allowed-origins` 配置；URL 参数传 Token 仅限 `/files/**` |
| **校验** | 核心 VO（登录/注册/用户/角色/工单/审批/聊天/个人中心）添加 JSR-303（`@NotBlank` / `@Pattern` / `@Size`）+ `@Validated` |
| **事务** | 用户/工单/审批等多表写操作添加 `@Transactional(rollbackFor = Exception.class)` |
| **性能** | 权限匹配、菜单闭包、用户列表角色填充、通知全部已读等 5 处 N+1 查询优化为批量查询；`PageParam` 限制 `pageSize` 上限 200；工作台 `/dashboard/stats` 聚合为单条 SQL + Redis 缓存；`/system/user/list` 限制下拉返回条数 |

依赖方向不变：**`modules/*` → `framework` → `common`**。新增业务优先在 `service/` 实现，Controller 不直接操作 Mapper。

#### 工作台与用户列表（2026.06 续）

针对代码质量审计中的架构与性能项，在**不改变对外 API 路径与响应字段**的前提下完成：

| 项 | 改进 | 主要文件 |
|------|------|----------|
| **工作台分层** | `DashboardController` 不再直接注入 9 个 Mapper；统计、最近登录、访问计数下沉至 `DashboardService` / `DashboardServiceImpl` | `api/dashboard/DashboardController.java`、`service/dashboard/` |
| **消息汇总分层** | `MessageCenterController` 站内消息未读数改调 `NoticeService.unreadCount`，与 `AnnounceService`、`ChatService` 一致 | `service/message/NoticeService.java` |
| **工作台统计性能** | `/dashboard/stats` 由约 20 次 `selectCount` 改为 `DashboardMapper.selectAggregateStats()` **单条 SQL**（标量子查询聚合）；`fileCount` 排除聊天路径（`images/chat/`、`files/chat/`），与文件管理列表一致；计数类结果 Redis 缓存 **2 分钟**（key：`dashboard:stats:aggregate:yyyy-MM-dd`）；`onlineCount`、今日/昨日访问量仍每次实时读取 | `dal/mysql/dashboard/DashboardMapper.java`、`service/dashboard/vo/DashboardStatsRow.java` |
| **用户下拉列表** | `GET /system/user/list` 不再全量 `selectList(null)`；`UserMapper.selectListForOptions` 仅返回**启用**用户（`status=1`）、字段精简，**上限 2000 条**（供通知定向、审批指派人等下拉；管理列表仍用 `/page` 分页） | `UserServiceImpl.listAll()`、`UserMapper.selectListForOptions` |

> 统计缓存默认 2 分钟延迟，适合首页/大屏场景；若需更实时可调整 `DashboardServiceImpl` 中的 `STATS_CACHE_MINUTES`。

#### 编译与静态检查修复（2026.06）

在不改变业务行为的前提下，修复 IDE / 编译器报错与警告：

| 文件 | 问题 | 处理 |
|------|------|------|
| `SysFileServiceImpl` | `Files.isRegularFile` 不抛 `IOException`，`catch` 不可达 | 去掉多余 try-catch；`SysFileDO.updateTime` 增加 `@TableField` 自动填充，软删时正确更新删除时间 |
| `DataExportController` | 未使用的 `DateTimeFormat`、`LocalDateTime` import | 删除无用 import |
| `ListExportService` | `selectBatchIds` 已废弃 | 改为 `selectByIds` |
| `CacheMonitorServiceImpl` | Redis API 空值类型安全警告 | 校验后用 `Objects.requireNonNull` 再传参 |
| `ServerMonitorServiceImpl` | 多余的 `@SuppressWarnings("removal")` | 仅在调用 `getSystemCpuLoad()` 的回退方法保留 `@SuppressWarnings("deprecation")` |
| `SysJobServiceImpl` | 缺少 `PageParam` import 导致启动失败 | 补全 import |

### 代码生成（2026.06）

**开发工具 → 代码生成**（`/tool/gen`，菜单 id=164–169、179），从当前库导入物理表，配置生成信息后一键产出前后端 CRUD 代码。

| 项 | 说明 |
|------|------|
| **路由 / 权限** | `/tool/gen`；`tool:gen:list`（列表）、`import` / `edit` / `remove` / `preview` / `code`（导入、改配置、删、预览、执行生成） |
| **后端** | `GenController`（`/api/tool/gen/*`）、`GenTableServiceImpl`；**Velocity** 模板 `backend/src/main/resources/templates/gen/`（Java DO/Mapper/Service/Controller + Vue API/页面） |
| **前端** | `frontend/src/views/tool/gen/`（`GenPage.vue` + `useGenPage.ts`）、`frontend/src/api/tool/gen.ts` |
| **数据表** | `gen_table`、`gen_table_column`；`table_name` **唯一索引**防重复导入（服务层校验 + 库约束） |
| **生成能力** | 预览（标记新建/覆盖）、ZIP 下载、写入项目目录、自动创建菜单与超管权限；`moduleName` 驱动 API 路径与权限前缀 |
| **类名规则** | 仅剥离配置前缀（`sys` / `system` / `t` / `tb` / `biz` / `app`），不再误剥 `user`、`data` 等业务段 |

**数据库**

| 场景 | 脚本 |
|------|------|
| 本地已有库 | `sql/add11.sql`（#11 建表/菜单 + #12 唯一索引迁移，可重复执行） |
| 生产已有库（MySQL 5.6+） | `sql/add11_wuadmin.sql`（同上；`table_name VARCHAR(191)` 适配 5.6 utf8mb4 索引 767 字节上限） |
| 本地空库全量 | `sql/admin_platform.sql` Part A §17 + Part B 菜单已含 |
| 生产空库全量 | `sql/admin_platform_mysql56.sql`（库名 `wuadmin`，§17 同上） |

执行增量后 **重新登录** 刷新侧栏。`sql/add12.sql` 已合并进 `add11.sql`，单独执行仅提示 SKIP。

### 管理页 UI 统一（2026.06）

各业务列表页采用统一 **module-page** 布局（`frontend/src/styles/admin-page.scss`）：

| 区块 | 类名 | 说明 |
|------|------|------|
| 头图 | `module-hero-card` | 深色 Hero：标题、简述、右侧统计；图标与 `sys_menu.icon` 一致 |
| 搜索 | `module-search-card` | 左对齐紧凑筛选区 |
| 表格 | `table-card` + `table-pagination` | 圆角卡片内表格与分页 |

- 公共组件 **`ModulePageIcon`**（`components/ModulePageIcon.vue`）+ 常量 **`MODULE_PAGE_ICON`**（`constants/module-page-icons.ts`），经 `resolveMenuIcon` 与侧栏同源解析。
- 已统一：用户/角色/菜单/组织/字典/配置/回收、审批/工单、操作/登录日志、文件、通知、API 访问/在线/缓存/服务监控、代码生成等；回收中心 Hero 改为与其他页同高的 `module-hero-card`；定时任务保留 `JobHeroOverview` 统计区。

### 回收中心与列表导出（2026.06）

各业务模块删除改为**逻辑删除**后，原分散在各列表页的回收 Dialog / Drawer 已移除，统一由 **系统管理 → 回收中心** 管理。

#### 回收中心

| 项 | 说明 |
|------|------|
| **路由** | `/system/recycle` |
| **菜单** | id=**163**，权限 `system:recycle:list`（进入页）；恢复/彻底删除复用各模块 `*:delete` 权限 |
| **汇总 API** | `GET /api/system/recycle/summary` — 12 类软删数量角标 |
| **前端** | `frontend/src/views/system/recycle/`（`recycle-config.ts` 驱动 Tab + 表格）；各业务列表页保留 **回收中心** 快捷入口 |

**支持的 12 类**

| Tab | 权限（列表） | 特殊说明 |
|-----|--------------|----------|
| 用户 | `system:user:list` | — |
| 角色 | `system:role:list` | — |
| 菜单 | `system:menu:list` | — |
| 部门 | `system:dept:list` | — |
| 岗位 | `system:post:list` | — |
| 工单 | `system:ticket:list` | — |
| 审批 | `system:approval:list` | — |
| 字典类型 | `system:dict:list` | 恢复类型**不会**自动还原已级联删除的字典数据 |
| 字典数据 | `system:dict:list` | 恢复/清除后自动 `dictCacheService.refreshAll()` |
| 系统通知 | `system:announce:list` | 软删；彻底删除级联 `user_announce`、`send_log` |
| 定时任务 | `monitor:job:list` | 恢复时重新注册 Quartz 调度 |
| 文件 | `sys:file:list` | 软删不删磁盘；恢复前校验磁盘文件存在；超 **30 天** 可由内置任务 `purgeFileRecycleBin` 自动清盘 |

各类型分页/恢复/彻底删除 API 仍挂在原模块路径下，例如：`GET /api/system/user/recycle/page`、`PUT .../restore/{id}`、`DELETE .../permanent/{id}`（以各 Controller 为准）。

**数据库增量（必跑）**

| 脚本 | 环境 | 内容 |
|------|------|------|
| `sql/add10.sql` | 本地 `wu-admin` | 回收中心菜单 id=163 + `sys_file` 的 `update_time`、`deleted`、索引 `idx_deleted_update` |
| `sql/add10_wuadmin.sql` | 生产 `wuadmin` | 同上 |
| `sql/add11.sql` | 本地 `wu-admin` | 代码生成表/菜单 + `table_name` 唯一索引（#11+#12） |
| `sql/add11_wuadmin.sql` | 生产 `wuadmin` | 同上（MySQL 5.6 兼容） |

```bash
# 本地回收中心
mysql -u root -p wu-admin < sql/add10.sql
# 本地代码生成（在 add10 之后）
mysql -u root -p wu-admin < sql/add11.sql
# 生产回收中心
mysql -u wuadmin -p wuadmin < sql/add10_wuadmin.sql
# 生产代码生成
mysql -u wuadmin -p wuadmin < sql/add11_wuadmin.sql
```

执行后 **重新登录** 刷新侧栏。`add10` 先加 `update_time` 再加 `deleted`（`AFTER update_time`），可重复执行。

配置项：`application.yml` → `app.job.file-recycle-retention-days: 30`（文件回收站保留天数）。

#### 列表导出

后端 **EasyExcel** + `ListExportService`；前端统一组件 `ListExportButton.vue`（`format=xlsx|csv`，`scope=filtered|all`）。

| 页面 | 接口 | 权限 |
|------|------|------|
| 用户管理 | `GET /api/system/export/user` | `system:user:list` |
| 登录日志 | `GET /api/system/export/login-log` | `system:loginLog:query` |
| 操作日志 | `GET /api/system/export/oper-log` | `system:operLog:query` |
| 工单 | `GET /api/system/export/ticket` | `system:ticket:list` |
| 审批单 | `GET /api/system/export/approval` | `system:approval:list` |
| API 访问 | `GET /api/monitor/api-access/export` | `monitor:apiAccess:query` |
| 在线用户 | `GET /api/monitor/online/export` | `monitor:online:list` |

导出写操作日志（`@Log` businessType=EXPORT）；`scope=filtered` 与列表当前筛选一致，`all` 在权限范围内导出全量（受 `PageParam` 上限约束）。

#### 前端：页面组件化拆分

采用统一约定：**路由入口 `index.vue`（薄包装）→ `*Page.vue`（页面骨架）+ `composables/use*Page.ts`（状态与业务）+ `components/`（展示子组件）**。

| 模块 | 路由 | 主要文件 |
|------|------|----------|
| 登录 / 注册 | `/login`、`/register` | `LoginPage.vue`、`RegisterPage.vue`；共享 `views/auth/components/`（`AuthSplitLayout`、`AuthCaptchaField` 等） |
| 布局壳层 | `/`（layout） | `LayoutPage.vue` + `useLayoutMenu` / `useLayoutMessages` / `useLayoutTheme` 等 |
| 工作台 | `/dashboard` | `WelcomeBanner`、`CoreStatsRow` 等 + `useDashboardData` |
| 个人中心 | `/profile` | `ProfileHero`、`BasicInfoForm`、`SecuritySettings` + `useProfileInfo` / `useProfileSecurity` |
| 系统管理 | `/system/user` · `dict` · `file` · `menu` · `org` · `config` · **`recycle`** · `approval` · `ticket` · `oper-log` · `login-log` | 各 `*Page.vue` + `use*Page.ts`；统一 `module-page` 样式；回收中心 `recycle-config.ts` 驱动 Tab |
| 开发工具 | `/tool/gen` | `GenPage.vue` + `useGenPage.ts` |
| 系统监控 | `/monitor/job` · `cache` · `server` · `api-access` · `online` | `JobPage.vue` + `useJobPage.ts`；`CacheMonitorPage` / `ServerMonitorPage` / `api-access` / `online` 均 `module-page` |
| 消息 | `/message/notice` | `notice/index.vue`，`module-page` |
| 企业IM | `/message/chat` | `ChatPage.vue` + `useChatPage.ts` / `useChatRender` / `useMention` |

**质量保障**：`npm run typecheck`（`vue-tsc --noEmit`）、`npm run test`（Vitest，约 85 用例）、`npm run build`（构建前自动类型检查）；后端 `mvn test`（JUnit 5 + Mockito，约 126 用例）。推送到 `main`/`master`/`dev` 或开 PR 时由 [GitHub Actions CI](#持续集成ci) 自动执行。详见 [单元测试](#单元测试)。

> 企业 IM 聊天区可进一步拆分子面板组件；`role` 等页已接入 `module-page`，逻辑 composable 拆分可继续做。

**组件化拆分注意**：样式从 Vue `<style scoped>` 抽到独立 `.css` / `.scss` 时，**勿使用 `:deep()`**（仅 SFC scoped 有效）；应改为 `.parent .el-textarea__inner` 等普通选择器。企业 IM 的 `chat-page.css` 已按此修正（输入框黑框问题）。个人中心样式见独立 `profile-page.scss`（由 `index.vue` 非 scoped 引入），子组件无需再套 scoped。

### 系统监控 · 缓存/服务监控（2026.06）

新增 **缓存监控**（Redis）与 **服务监控**（本机 JMX），前后端与菜单增量见 `sql/add7.sql`、`add8.sql`；生产合并脚本 **`sql/add6_7_wuadmin.sql`**（含流程中心 rename + 缓存 + 服务监控）。

#### 能力一览

| 页面 | 路由 | 权限 | 说明 |
|------|------|------|------|
| 缓存监控 | `/monitor/cache` | `monitor:cache:list` / `monitor:cache:delete` | Redis 概览、内存/QPS/命中率/连接数四宫格图表、SCAN 键列表与详情/删除 |
| 服务监控 | `/monitor/server` | `monitor:server:list` | CPU/物理内存/JVM/磁盘、CPU 与 JVM 堆折线趋势 |

#### 后端

| 模块 | 路径 | 要点 |
|------|------|------|
| 缓存监控 | `CacheMonitorController` / `CacheMonitorServiceImpl` | `INFO` 统计、SCAN 键、`GET/DELETE` 键详情；删除键**前缀黑名单**（`Authorization:`、`satoken:`、`captcha:` 等） |
| 服务监控 | `ServerMonitorController` / `ServerMonitorServiceImpl` | JMX 采集 CPU、堆/物理内存、磁盘；JDK 21+ 反射 `getCpuLoad()`，JDK 17 回退 `getSystemCpuLoad()` |

主要 API：`GET /api/monitor/cache/info|stats|keys|value`、`DELETE /api/monitor/cache/key`；`GET /api/monitor/server/info`。

#### 前端 · 折线图与后台采样

登录进入布局后，若角色具备对应 `monitor:*:list` 权限，**无需打开监控页**即开始后台轮询（缓存 **3s**、服务 **5s**），折线采样写入 **`sessionStorage`**（同一标签页 **F5 刷新仍保留**，最多 **20** 个点）。切到其他菜单再返回，历史趋势不丢失。

| 行为 | 说明 |
|------|------|
| 全局采样 | `useLayoutBootstrap` → `startMonitorBackground`（`composables/useMonitorBackground.ts`） |
| 图表状态 | `cacheMonitorChart.ts`、`serverMonitorChart.ts`（模块级 + sessionStorage） |
| 「自动」开关 | 关闭后停止轮询；状态持久化，刷新后仍生效 |
| 退出登录 | 停止采样并清空 session 中的监控折线数据 |
| 浏览器标签隐藏 | 暂停轮询；切回标签页后继续 |

> **平均负载**：来自 Linux `load average`；**Windows 上 JMX 返回不可用**，界面显示 `-` 属正常，请参考 **系统 CPU / 进程 CPU** 与折线图。

> **Redis 内存环图**：未配置 `maxmemory` 时仅展示当前占用（如 1.22M），外圈绿环非占比；配额环需在 Redis 服务端配置 `maxmemory` + `maxmemory-policy`。

### 个人中心 · 布局与样式（2026.06）

- 路由 `/profile` 拆为 `ProfileHero`、`BasicInfoForm`、`SecuritySettings`、`AccountSidebar` 等子组件 + `useProfileInfo` / `useProfileSecurity`。
- 样式集中在 **`profile-page.scss`**（`index.vue` 非 scoped `@import`），避免子组件 scoped 穿透失效；大屏下左右列底部对齐、基本资料「保存」按钮与表单 label 左对齐。

### 生产部署排障实录（2026.06）

以下为实际上线 `wushij.online` 时遇到的问题与处理，供同类环境对照。

| 现象 | 原因 | 处理 |
|------|------|------|
| 登录报「网络连接失败」或 403；Network 里 `login` 的 Response 是 **`index.html`** | Nginx 未把 `/api` 反代到后端，请求落入 `try_files` → SPA 首页 | 配置 `location ^~ /api/ { proxy_pass http://127.0.0.1:8080/api/; ... }`；检查宝塔 `extension/*.conf` 无冲突；重载 Nginx。自测：`https://域名/api/auth/config` 须返回 **JSON** |
| master 能登、dev 包不能登（已确认 Nginx 正常） | dev 将 CORS 从 `*` 改为 `application-prod.yml` 域名，**勿留占位符** `your-domain.com` | 改为实际域名，如 `https://wushij.online,https://www.wushij.online`，重新打包并重启 jar |
| 开启「禁止前端调试」后 F12 打不开，无法排障 | `disableDevtool` 存于 `sys_config_group.security` | 执行 `sql/disable_devtool_off.sql`（**MySQL 5.6** 用 `REPLACE`，勿用 `JSON_SET`），重启后端并强刷浏览器；调试完在系统配置改回或改 SQL 还原 |
| 企业 IM 输入框出现**黑色边框** | 拆分后 `chat-page.css` 中 `:deep()` 不生效 | 已改为 `.chat-textarea .el-textarea__inner { border: none !important; }` |
| 联系人「在线/离线」不实时变，须刷新 | 旧版仅在 `loadUsers()` 时拉取 `online` 字段 | 已增加 WebSocket **`presence`** 推送；前后端需一并升级 |
| 字典管理多出多个「审批类型（副本）」 | 误点「复制类型」；每点一次生成一条 `_copy_时间戳` | 在字典管理删除多余副本即可，不影响业务字典 `sys_approval_form_type` |

**Nginx 反代示例**（`/api` 须写在 `location /` 之前，建议加 `^~`）：

```nginx
location ^~ /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

### 个人中心 · 短信绑定/更换手机号

- **基本资料** 中手机号不再随 `PUT /auth/profile` 直接修改，须走 **短信验证绑定** 流程。
- 已绑定：展示脱敏号码 + **「更换手机号」**；未绑定：在表单内填写新号与验证码。
- **发码前强制滑块**（与登录页「发码前滑块」开关无关，与重置密码发码一致）。
- 短信模板：首次绑定用 `templateBindPhone`（100004），更换用 `templateModifyPhone`（100002）；未配置时回退 `templateVerifyCode`。
- 受 **短信配置** 启用状态与 **接口限流** 分组中的短信防刷规则约束；校验逻辑与登录短信一致（Redis 优先，阿里云短信认证可 fallback）。

| 接口 | 说明 |
|------|------|
| `POST /api/auth/profile/mobile/sms-code` | 发绑定验证码，body：`{ "mobile": "13800138000", "code": "slider_verified" }` |
| `PUT /api/auth/profile/mobile` | 绑定/更换，body：`{ "mobile": "13800138000", "smsCode": "123456" }` |

后端：`ProfileSmsMobileBindService`；前端：`frontend/src/views/profile/`（`useProfileInfo.ts`、`BasicInfoForm.vue` 等）。

### 审批单中心 · 详情页审批

- **审批单详情** 抽屉底部（居中）增加 **通过 / 驳回** 操作，与列表「处理」下拉等效；审批完成后详情自动刷新。
- **注册审核**（`REGISTER`）：须具备 `system:approval:approve`；其他类型须为指定审批人且状态为 `SUBMITTED`。
- 按钮为 plain 描边样式（通过黑色边框、驳回红色边框），与全站次要操作风格一致。

### 开发与生产环境分离

| 环境 | 激活方式 | 说明 |
|------|----------|------|
| **prod**（默认） | 无需额外参数，或 `SPRING_PROFILES_ACTIVE=prod` | `application-prod.yml`：库名 `wuadmin`、Redis 密码等生产配置 |
| **dev**（本地） | `-Dspring.profiles.active=dev` 或 `$env:SPRING_PROFILES_ACTIVE='dev'` | `application-dev.yml`：库名 `wu-admin`、本地 root 账号；Redis **无密码** |

- `application.yml` 中 **Redis 密码仅写在 prod  profile**，避免本地 dev 误连带密 Redis 或空密码覆盖生产配置。
- `DevRedissonConfig`（`@Profile("dev")`）：本地 Redis 无密码时，将 Redisson 空串密码置 `null`，避免无效 `AUTH` 报错。

### IP 归属地（ip2region）

- 公用工具 `IpLocationUtils`，加载 classpath `ip2region/ip2region.xdb`。
- 用于 **登录日志**、**在线用户** 等场景的 IP 解析；内网地址显示「内网IP」。

### 接口文档 iframe 嵌入

- 生产环境 Nginx 反代后，Knife4j 默认 `X-Frame-Options` 会导致 **系统管理 → 接口文档** iframe 空白。
- `Knife4jIframeHeaderFilter` 对 `/doc.html`、`/v3/api-docs`、`/swagger-ui`、`/webjars/` 等路径响应头设为 `SAMEORIGIN`，允许同源 iframe 内嵌。

### 组织树展示

- `frontend/src/utils/org-tree.ts`：`displayOrgTree` 隐藏唯一根节点（如「本部」），直接展示下级中心/部门。
- **岗位体系** 左侧树默认展开至 **第 2 级**（`collectExpandKeysByDepth(..., 2)`）；**部门体系** 与用户管理侧栏部门树默认折叠，减少首屏展开过多节点。

### 企业IM 能力增强

企业 IM（菜单名由「即时聊天」更名为 **企业IM**，见 `sql/add3.sql`）近期新增以下能力，前后端与 WebSocket 需一并升级并 **重启后端**。

| 能力 | 说明 |
|------|------|
| **历史消息分页** | 私聊/群聊默认拉最近 50 条；滚至顶部自动加载更早记录，并保持滚动位置不跳动 |
| **通用文件发送** | 除图片外支持 PDF、Word、Excel、压缩包等；复用 **系统配置 → 文件存储** 的大小与扩展名限制；存储于 `files/chat/`，不出现在文件管理列表 |
| **群聊 @ 成员** | 输入 `@` 弹出成员列表；被 @ 用户 WebSocket 强提醒（通知标题 `[有人@你]`）并计入群未读角标 |
| **消息撤回** | 自己发送的消息 **2 分钟内** 可右键撤回；私聊显示「你/对方撤回了一条消息」，群聊显示「昵称撤回了一条消息」；对方 **实时** 同步，无需刷新 |
| **建群权限** | 仅 **超级管理员**（`super_admin`）可创建群聊；侧栏「+」按权限显示 |
| **正在输入…** | 私聊输入时向对方推送 typing 事件，顶栏显示「对方正在输入…」 |
| **图片预览** | 聊天图片点击后使用 Element Plus 全屏查看器居中预览 |

### 企业IM · 群公告与免打扰（2026.06）

群聊体验与通知策略增强，前后端 + WebSocket + 数据库增量（`sql/add9.sql`）需一并升级并 **重启后端**。

| 能力 | 说明 |
|------|------|
| **群名称 / 群公告编辑** | 仅 **群主 / 群管理员** 可改；普通成员在群组详情中只读 |
| **群公告持久化** | 保存后写入 `sys_chat_group.announcement`；公告变更时重置全员 `announcement_read_time`，全员视为未读 |
| **群公告置顶条** | 有未读公告时，聊天区顶部展示摘要条；点击进入详情（发布者昵称/头像、发布时间、全文） |
| **标记已读** | 详情内点「完成」调用 `POST .../announcement/read`，收起置顶条并记录 `announcement_read_time` |
| **公告实时推送** | 保存后向**全部成员** WebSocket 推送 `type: groupAnnouncement`（含公告全文 `announcement`）；正在该群聊天页时同步置顶条，不重复弹窗 |
| **群聊免打扰** | 成员可对本群开启 `notifyMuted`；**普通群消息**不弹窗、**不计入**群未读角标 |
| **免打扰例外** | **@ 我**、**群公告** 始终提醒并计入未读；顶栏浮层标题为 `[有人@你]` 或「xxx 发布了新公告」 |
| **消息展示** | 群聊每条消息独立头像 + 气泡，连发不合并 |
| **群组详情 UI** | 「保存修改」底部居中（「解散群组」左侧）；群公告编辑区 `autosize` 无内嵌滚动条 |

**WebSocket 新增类型**：`groupAnnouncement`（字段含 `groupId`、`groupName`、`announcement`、`senderId`、`senderName`、`title`、`content` 摘要）。

**主要 API（前缀 `/api/system/chat/group`）**

| 路径 | 说明 |
|------|------|
| `PUT /update` | 更新群名称/公告（body：`id`、`name`、`announcement`）；仅群主/管理员 |
| `POST /{groupId}/announcement/read` | 当前用户标记群公告已读 |
| `POST /{groupId}/notify-muted?muted=true\|false` | 设置本群免打扰 |
| `GET /{groupId}` | 群详情上下文，含 `announcement`、`announcementUnread`、`notifyMuted`、发布者信息 |

**数据库字段**（`sys_chat_group_member`）

| 字段 | 说明 |
|------|------|
| `notify_muted` | `0` 正常；`1` 免打扰（仅 @ / 群公告提醒） |
| `announcement_read_time` | 群公告已读时间；`NULL` 或早于群 `update_time` 视为未读 |

**前端关键逻辑**：`utils/message-push.ts`（`shouldNotifyGroupChat`、`shouldCountGroupUnread`）、`store/message.ts`（群免打扰映射、`groupAnnouncementTick`）、`views/message/chat/composables/useChatPage.ts`（置顶条、详情、保存）。

**消息类型（`msgType`）**

| 值 | 含义 |
|----|------|
| `1` | 文本 |
| `2` | 图片 |
| `3` | 文件（`content` 为 JSON：`url`、`name`、`size`、`fileId`） |
| `4` | 系统消息（群事件等） |
| `5` | 已撤回 |

**WebSocket 推送类型**：`notice` / `chat` / `groupChat` / **`groupAnnouncement`** / `typing` / **`presence`**（联系人上线/下线）；群消息可带 `atMe: true`；撤回带 `recall: true` 与 `messageId`。

**在线状态**：用户 WebSocket 连接/断开时，后端向其他在线用户广播 `{ type: "presence", userId, online }`，企业 IM 联系人列表与聊天顶栏「在线/离线」**实时更新**，无需手动刷新。

**数据库增量**

| 脚本 | 环境 | 内容 |
|------|------|------|
| `sql/add3.sql` | 本地 dev（`wu-admin`） | 菜单更名为「企业IM」 |
| `sql/add4.sql` | 本地 dev | 群消息表 `mention_ids` 字段 |
| `sql/add9.sql` | 本地 dev | 群成员 `notify_muted`、`announcement_read_time`（群公告已读 / 免打扰） |
| `sql/add6_7_wuadmin.sql` | **生产**（`wuadmin`） | 合并 add6～add8 + **add9**（流程中心、缓存/服务监控、群公告免打扰字段） |
| `sql/add9_wuadmin.sql` | **生产**（`wuadmin`） | 仅 add9 字段（已跑过 `add6_7_wuadmin` 可跳过） |

```bash
# 本地（按版本依次）
mysql -u root -p wu-admin < sql/add3.sql
mysql -u root -p wu-admin < sql/add4.sql
mysql -u root -p wu-admin < sql/add9.sql

# 生产（库名 wuadmin；若尚未执行 add3/add4，可先改脚本 USE 或逐条执行 add3、add4 后再跑）
mysql -u wuadmin -p wuadmin < sql/add6_7_wuadmin.sql
# 或仅补 add9：
mysql -u wuadmin -p wuadmin < sql/add9_wuadmin.sql
```

---

## 系统配置（`/system/config`）

配置存储在表 `sys_config_group`，按 `group_code` 分组，值为 JSON。管理端 **系统管理 → 系统配置** 可编辑；部分项保存后**立即生效**（无需重启）。

| 分组编码 | 名称 | 主要字段 | 生效范围 |
|----------|------|----------|----------|
| `site` | 基础信息 | 平台名称、副标题、登录/注册页标题、版权 | 登录页、注册页、工作台展示 |
| `session` | 会话配置 | `tokenExpireHours`（1～720） | Sa-Token 会话 TTL |
| `file` | 文件配置 | `maxSizeMb`、`allowedExtensions` | 上传校验（上限不超过平台 500MB） |
| `rateLimit` | 接口限流 | 验证码/登录/注册 每分钟每 IP；**短信**：每 IP 每分钟、同号发送间隔、同号/同 IP 日上限（0=不限） | 认证与短信发码防刷 |
| `login` | 登录认证 | 验证码开关、类型（`image`/`slider`）、**短信登录**（`smsLoginEnabled`）、**发送前滑块**（`smsLoginSliderCaptchaEnabled`）、记住我、重试锁定 | 登录流程（账号 Tab / 短信 Tab） |
| `register` | 注册认证 | 开放注册、验证码、默认角色、**需审核**、密码最小长度 | 注册流程 |
| `thirdParty` | 第三方配置 | 微信 / 支付宝 / GitHub / **Google** 登录密钥（AppID、Client ID/Secret、重定向 URI 等） | 第三方 OAuth 接入（配置存储，按业务启用） |
| `payment` | 支付配置 | 微信 Native、支付宝当面付；商户密钥、`notifyUrl`；**生成测试订单**（0.01 元） | 测试下单与支付回调 |
| `sms` | 短信配置 | 启用、`provider`（`aliyunAuth`/`tencent`）、密钥、签名、模板 100001～100005；**测试发送**、最近发送记录 | 短信认证 SendSmsVerifyCode；登录页短信 Tab |
| `security` | 安全配置 | `disableDevtool`（禁止 F12 等前端调试）、`isConcurrent`（false=禁止多端同时在线，新登录踢旧会话） | 前端调试需**刷新页面**；会话策略**保存后对新登录立即生效** |

**公开接口**（无需登录）：`GET /api/auth/config`，返回 `site`、`login`、`register`、`security`（仅 `disableDevtool`）等前端所需配置。

**管理接口**（需权限 `system:config:list` / `system:config:update`）：

- `GET /api/system/config-group/list`
- `GET /api/system/config-group/{groupCode}`
- `PUT /api/system/config-group/{groupCode}`，body：`{ "configValue": "{...json...}" }`
- `POST /api/system/config-group/test-payment`，body：`{ "type": "wechat" | "alipay" }`（需 `system:config:update`，**须先保存支付配置**）
- `POST /api/system/config-group/test-sms`，body：`{ "phone": "13800138000" }`（需 `system:config:update`，**须先保存短信配置**）
- `GET /api/system/config-group/sms-logs/recent?limit=5`
- `GET /api/system/config-group/sms-logs?page=1&size=10&phone=&status=`
- `POST /api/auth/sms-code`，body：`{ "phone": "13800138000", "code": "slider_verified" }`（发码；开启「发送前滑块验证」时 `code` 必填；Redis 校验，受 `rateLimit` 短信限流约束）
- `POST /api/auth/login`，body 含 `loginType`：`account`（账号+密码+图形/滑块验证码）或 `sms`（手机号+短信验证码，须为已绑定手机）

**短信登录**：在 **系统配置 → 登录认证** 开启「短信验证码登录」，并在 **短信配置** 中启用短信；登录页出现「账号登录 / 短信登录」切换，短信 Tab 仅需手机号与验证码（使用个人中心已绑定手机号）。

**短信发码前滑块**（`smsLoginSliderCaptchaEnabled`，与账号登录验证码独立）：在「短信验证码登录」开启后，可再开启「发送前滑块验证」。用户输入手机号并点击「获取验证码」时先完成滑块验证，通过后才会调用 `/auth/sms-code` 发送短信。已有库升级见 `sql/add2.sql`。

**支付回调与查单**（回调无需登录，已在 Security 白名单 `/pay/notify/**`）：

| 接口 | 说明 |
|------|------|
| `POST /api/pay/notify/wechat` | 微信 V3 异步通知，响应 `{"code":"SUCCESS","message":"成功"}` |
| `POST /api/pay/notify/alipay` | 支付宝异步通知，响应纯文本 `success` / `failure` |
| `GET /api/pay/order/{orderNo}` | 测试订单状态轮询（`PENDING` / `PAID`，需 `system:config:list`） |

`notifyUrl` 须配置为公网 HTTPS，例如 `https://域名/api/pay/notify/wechat`。本地联调可用内网穿透。

**个人中心**（登录即可，无需额外菜单权限）：

| 接口 | 说明 |
|------|------|
| `GET /api/auth/profile` | 当前用户资料（部门、角色、岗位、最近登录等） |
| `PUT /api/auth/profile` | 更新昵称、邮箱、头像（**手机号须走下方 `/mobile` 接口**） |
| `PUT /api/auth/profile/password` | 已知原密码时自助改密 |
| `POST /api/auth/profile/password/sms-code` | 发送重置密码短信（body：`{ "code": "slider_verified" }`，**发码前强制滑块**，不受登录页「发码前滑块」开关影响） |
| `PUT /api/auth/profile/password/sms-reset` | 短信验证重置密码（body：`smsCode`、`newPassword`、`confirmPassword`） |
| `POST /api/auth/profile/mobile/sms-code` | 发送绑定/更换手机号短信（body：`mobile`、`code: slider_verified`，**发码前强制滑块**） |
| `PUT /api/auth/profile/mobile` | 短信验证绑定或更换手机号（body：`mobile`、`smsCode`） |
| `POST /api/auth/profile/avatar` | 上传头像（最大 2MB） |
| `GET /api/auth/profile/login-logs` | 我的登录记录分页 |

**安全设置 · 忘记密码**：个人中心 → **安全设置** →「当前密码」右侧「忘记密码」。须先在 **基本资料** 绑定手机号且 **短信配置** 已启用；未绑定时显示提示「重置密码需先绑定手机号」。验证码发送至已绑定手机，发码前须完成滑块验证；校验逻辑与登录短信一致（Redis 优先，阿里云短信认证可 fallback）。

**基本资料 · 绑定手机号**：须 **短信配置** 已启用。未绑定时在基本资料填写手机号与验证码；已绑定后显示脱敏号码，点击 **「更换手机号」** 进入更换流程。发码前须完成滑块验证，受 `rateLimit` 短信限流约束。

`application.yml` 中 `sa-token.timeout`、`auth.security.*`、`file.storage.*` 为**缺省兜底**；库中有对应分组时以库为准（`session.tokenExpireHours` 在登录时写入 Sa-Token 超时）。

---

## 注册审核

在 **系统配置 → 注册认证** 开启「注册需审核」后：

1. 用户注册成功，账号 `status = 2`（待审核），分配配置中的默认角色；
2. 自动创建类型为 `REGISTER` 的审批单，并通知**首位可用的超级管理员**（`super_admin` 角色，站内通知 `sys_notice`）；
3. 管理员在 **流程中心 → 审批单中心** 通过或驳回（列表「处理」下拉，或打开 **详情** 抽屉底部 **通过 / 驳回**）；
4. **通过** → `status = 1`，用户出现在 **用户管理** 默认列表，可登录；
5. **驳回** → 逻辑删除账号并清理角色/岗位关联，**不出现在用户管理**；申请人收到审核结果通知；
6. 若该用户名曾在回收站（软删），**同用户名再次注册**会自动恢复账号并重新走审核流程。

**用户管理列表规则**：未指定状态筛选时，默认只展示 `status` 为 **0（停用）/ 1（启用）** 的用户；待审核（2）、审核驳回（3）不在默认列表中，请在审批单中心处理。列表中待审/驳回状态以标签展示，避免误触启用/停用开关。

用户状态约定：

| status | 含义 | 用户管理默认列表 |
|--------|------|------------------|
| `0` | 停用 | 显示 |
| `1` | 正常 | 显示 |
| `2` | 待审核 | **不显示**（审批单中心） |
| `3` | 审核驳回 | **不显示**（驳回后通常已软删） |

工作台统计含「待审核用户」数量（`userPendingCount`，按 `status = 2` 统计）。

**审批操作说明**：注册审核（`REGISTER`）需权限 `system:approval:approve`（任意可用超管均可处理）；其他审批类型须为单据指定审批人。仅 `SUBMITTED`（待审批）状态可执行通过/驳回；详情页与列表操作等效，审批后状态与通知自动更新。

---

## 消息中心

消息中心与原有 **业务收件箱**（`sys_notice`，工单/审批/注册审核触达）并存，通过顶栏铃铛统一入口展示。

### 能力一览

| 能力 | 说明 |
|------|------|
| **业务消息** | 顶栏铃铛「业务消息」Tab，数据表 `sys_notice`，点击跳转工单/审批 |
| **系统通知** | 管理员在「系统通知」页发布广播/定向公告（`sys_announce`），用户顶栏「系统通知」Tab 查看 |
| **企业IM** | 私聊 + 群聊；文本/表情/图片/**文件**；在线状态；拉黑；群管（邀请/移除/禁言/转让/解散）；**群公告**（置顶/已读/推送）；**群免打扰**（仅 @ 与公告提醒）；**@ 提醒**；**2 分钟内撤回**；**正在输入** |
| **群聊日志** | 群组详情 →「群聊日志」Tab，记录建群、邀请、退群等操作（`sys_chat_group_log`） |
| **实时推送** | WebSocket 推送新通知、私聊、群聊、**群公告**、**@ 强提醒**、**撤回**、**正在输入**；免打扰群普通消息不弹窗不计角标；顶栏与聊天页联动 |

### 菜单与页面

| 菜单 | 路由 | 组件 | 权限 |
|------|------|------|------|
| 消息中心 | `/message` | — | 目录 |
| 系统通知 | `/message/notice` | `message/notice/index` | `system:announce:list`（管理端） |
| 企业IM | `/message/chat` | `message/chat/index` | `system:chat:list` |

普通用户默认拥有 **企业IM** + 顶栏查看 **系统通知**，不含「系统通知」管理页（需 `system:announce:*`）。

### 主要 API（前缀 `/api`）

| 分类 | 路径 | 说明 |
|------|------|------|
| 汇总 | `GET /system/message/summary` | 业务 + 公告 + 聊天未读数 |
| 通知 | `/system/announce/*` | 分页、CRUD、发布、我的通知、已读、发送日志 |
| 私聊 | `POST /system/chat/send` | 发消息（body 含 `msgType`、`content`） |
| 私聊 | `GET /system/chat/history/{targetId}` | 历史记录（`pageNo` / `pageSize`，默认 50 条/页） |
| 私聊 | `POST /system/chat/read/{senderId}` | 标记已读 |
| 私聊 | `POST /system/chat/recall/{messageId}` | 撤回私聊消息（2 分钟内） |
| 私聊 | `POST /system/chat/typing/{targetUserId}` | 正在输入信号 |
| 群聊 | `/system/chat/group/*` | 建群、成员、消息、禁言、转让等 |
| 群聊 | `GET /system/chat/group/{groupId}/messages` | 群历史（分页） |
| 群聊 | `POST /system/chat/group/{groupId}/message` | 发群消息（body 可含 `mentionIds`） |
| 群聊 | `POST /system/chat/group/{groupId}/message/{messageId}/recall` | 撤回群消息 |
| 群聊 | `GET /system/chat/group/{groupId}/logs` | 群操作日志 |
| 群聊 | `GET /system/chat/can-create-group` | 是否可建群（仅 `super_admin` 为 true） |
| 群聊 | `PUT /system/chat/group/update` | 更新群名称/公告（群主/管理员） |
| 群聊 | `POST /system/chat/group/{groupId}/announcement/read` | 标记群公告已读 |
| 群聊 | `POST /system/chat/group/{groupId}/notify-muted?muted=` | 设置本群免打扰 |
| 聊天图片 | `POST /system/chat/upload/image` | 上传至 `images/chat/`，**不出现在文件管理列表** |
| 聊天文件 | `POST /system/chat/upload/file` | 上传至 `files/chat/`，受文件配置大小/扩展名约束 |
| WebSocket | `ws(s)://{host}/api/ws/message` | 握手时从 **httpOnly Cookie**（或 Header）鉴权，不再 URL 传 Token；推送：`notice` / `chat` / `groupChat` / **`groupAnnouncement`** / `typing` / `presence`；群聊可带 `atMe`；撤回带 `recall` + `messageId` |

### 前端关键文件

```
frontend/src/
├── api/message/index.ts          # 通知、聊天、群聊 API
├── constants/chat.ts             # 消息类型、分页大小
├── utils/chat-message.ts         # 文件 payload、撤回文案、@ 渲染
├── utils/message-push.ts         # 免打扰过滤、群公告/@ 提醒策略
├── store/message.ts              # 未读汇总、WebSocket、群未读角标、撤回、群公告 tick
├── types/message.ts              # 消息相关类型
├── utils/messageWebSocket.ts     # WS 连接封装
├── components/
│   ├── EmojiPicker.vue           # 表情选择器
│   ├── chat/ChatToolbarIcons.vue # 输入栏图标
│   └── MessageNotification.vue   # 新消息浮层提示
└── views/message/
    ├── notice/index.vue          # 系统通知管理（发布/发送日志）
    └── chat/
        ├── index.vue             # 路由入口（薄包装）
        ├── components/ChatPage.vue
        └── composables/useChatPage.ts、useChatRender.ts、useMention.ts
```

后端：`modules/system/api/message/`（`AnnounceController`、`ChatController`）、`framework/websocket/`（`WebSocketConfig`、`MessageWebSocketHandler`）。

### 权限标识

| 权限 | 说明 |
|------|------|
| `system:announce:list` | 进入通知管理页 |
| `system:announce:create/update/delete/publish` | 通知 CRUD 与发布 |
| `system:chat:list` | 企业IM、聊天图片/文件上传、撤回、@、正在输入等 |

> **建群**：后端强制仅 `super_admin` 可调用建群接口；前端侧栏「+」通过 `GET /can-create-group` 控制显示。

### 数据库

全量安装与已有库升级：

| 场景 | 脚本 | 命令 |
|------|------|------|
| **全新安装（空库）** | 本地 `sql/admin_platform.sql`；生产 **MySQL 5.6** `sql/admin_platform_mysql56.sql`（库名 `wuadmin`） | 空库直接执行全文 |
| **极旧库首次升级** | `admin_platform.sql` **附录段**（约 990 行起） | 补全缺表/菜单/索引（含代码生成与唯一索引迁移），可重复执行 |
| **发版增量** | **`sql/add1.sql`** … **`add11.sql`** | 生产见 **`add6_7_wuadmin.sql`**、**`add10_wuadmin.sql`**、**`add11_wuadmin.sql`** |

> 切勿对生产库直接跑 `admin_platform.sql` 全文（Part A 含 DROP，默认会被熔断拦截）。

正文已含：消息中心表（§11b）、`sys_chat_group_log`、群消息 `mention_ids`、分级组织示例、定时任务、短信配置与 `sys_sms_log`、性能索引（含清理任务相关时间索引）。升级后涉及菜单变更时请 **重新登录**；WebSocket 与新接口需 **重启后端**。

> 说明：历史聊天图片若曾走通用文件上传，可能仍出现在文件列表；升级后新发的聊天图片走 `images/chat/`、文件走 `files/chat/`，列表会自动排除。群公告/免打扰需执行 **`add9.sql`**（生产见 `add6_7_wuadmin.sql` 或 `add9_wuadmin.sql`）。

## 定时任务（系统监控 → 定时任务）

- 菜单：`/monitor/job`，权限 `monitor:job:*`
- 表：`sys_job`、`sys_job_log`；基于 Quartz 调度，支持 CRUD、暂停/恢复、立即执行
- 内置 6 项系统清理任务（过期日志、私聊/群聊消息、调度日志、已读通知、工单回收站），**默认暂停**，可在管理页启用
- 全量脚本 §7b 建表并初始化；旧库通过 `admin_platform.sql` 附录补建

---

## 组织管理 / 菜单 / 接口文档

### 组织管理（`/system/org`）

- Tab：**部门体系 | 岗位体系**
- 部门：树形、`ancestors`、拖拽；软删部门在 **回收中心** 恢复；左侧树**默认折叠**（隐藏唯一根节点后直接展示下级）
- 岗位：`sys_user_post` 关联、组织内成员；岗位树**默认展开至第 2 级**

### 菜单管理（`/system/menu`）

- 类型目录/菜单/按钮联动表单；`IconSelect` 图标选择
- `component` 以 `http(s)://` 开头 → 侧栏新窗口；`/doc.html` → iframe 内嵌

### 接口文档（`/tool/api-doc`）

- **开发工具 → 接口文档**，内嵌 Knife4j（基于 **Springdoc OpenAPI 3**）
- 开发：文档静态资源经 Vite 代理到后端 `8080`；**调试请求**默认 `http://localhost:3000/api`
- 生产：Nginx → 后端 `/api/doc.html`、`/api/v3/api-docs` 等；后端 `Knife4jIframeHeaderFilter` 将文档相关路径的 `X-Frame-Options` 设为 `SAMEORIGIN`，避免 iframe 空白
- 调试需带请求头 `Authorization: <登录 token>`，修改类接口用 **PUT/POST**，勿用 GET
- Spring Boot **3.5** 需 **springdoc ≥ 2.8.9**；`knife4j.enable` 建议为 `false`（4.5.0 增强模块与 springdoc 2.8 API 不兼容，关闭后 `doc.html` 仍正常）

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios、ECharts、Three.js |
| 后端 | Spring Boot 3.5、Spring Security 6、Sa-Token、MyBatis-Plus 3.5、Druid、Knife4j 4.5、Springdoc 2.8、微信支付/支付宝 SDK、ZXing |
| 移动端 | uni-app、Vue 3、TypeScript、Pinia（H5 / 微信小程序） |
| 数据 | MySQL 8、Redis 7、Redisson（限流/锁/缓存队列等） |
| 部署 | 静态资源 + 反向代理（如 Nginx） |

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
│  前端静态资源   │   /doc.html*    │  Spring Boot     │
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
wu-admin/
├── .github/workflows/ci.yml    # GitHub Actions：后端 mvn test + 前端 typecheck/test
├── backend/                    # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/java/cn/rbac/server/
│   │   ├── common/             # 通用 POJO、工具类
│   │   ├── framework/          # 安全、MyBatis、Redis、Web 过滤器等
│   │   └── modules/system/     # 系统业务（api / service / dal）
│   ├── src/main/resources/
│   │   ├── application.yml          # 公共配置；默认 profile=prod
│   │   ├── application-dev.yml      # 本地开发（wu-admin 库、无 Redis 密码）
│   │   ├── application-prod.yml     # 生产（wuadmin 库、Redis 密码等）
│   │   └── templates/gen/           # 代码生成 Velocity 模板
│   └── src/test/java/cn/rbac/server/
│       ├── testsupport/        # MybatisLambdaTestBase、ServiceTestFixtures、MybatisMockMatchers
│       ├── framework/web/core/ # GlobalExceptionHandlerTest 等
│       └── modules/system/service/  # 各 *ServiceImplTest
├── uniapp/                     # uni-app 移动端（H5 / 小程序）
│   ├── src/
│   │   ├── pages/              # Tab：首页、工作台、消息、我的
│   │   ├── pages-sub/          # 子包：系统管理、监控、IM、个人资料等
│   │   ├── composables/        # useH5ListPageNav、useProfileForm 等
│   │   ├── components/common/  # SubPageBackBar、H5BackButton、DataCard 等
│   │   ├── utils/              # nav-history、navigate-back、nav-from（H5 浅栈返回）
│   │   └── store/              # Pinia；h5-back-button（浮动返回显隐）
│   └── package.json
├── frontend/                   # Vue 3 + TypeScript PC 前端
│   ├── src/
│   │   ├── api/                # 接口封装（system、message、monitor 等，均为 .ts）
│   │   ├── views/              # 页面模块（按功能分子目录）
│   │   │   ├── login/、register/、layout/、dashboard/、profile/
│   │   │   ├── auth/components/    # 登录注册共享（AuthSplitLayout、AuthCaptchaField 等）
│   │   │   ├── system/{user,dict,file,menu,org,config,recycle,approval,ticket}/  # index、*Page、composables
│   │   │   ├── tool/gen/       # 代码生成 GenPage + useGenPage
│   │   │   ├── monitor/job/、cache/、server/、api-access/、online/
│   │   │   └── message/{notice,chat}/
│   │   ├── styles/admin-page.scss  # module-page 统一视觉
│   │   ├── composables/        # 跨页面复用（useDict、useMonitorBackground 等）
│   │   ├── components/         # DictSelect、ModulePageIcon、SliderCaptcha 等
│   │   ├── constants/module-page-icons.ts  # Hero 图标与 sys_menu 对齐
│   │   ├── router/             # 路由与守卫
│   │   ├── store/              # Pinia（user、message 等）
│   │   ├── types/              # TS 类型（api、message、config）
│   │   ├── utils/              # request、主题、菜单、org-tree、WebSocket 工具
│   │   └── directives/         # v-permission 等指令
│   ├── tests/unit/             # Vitest 单测（store、utils、api，与 src 分离）
│   ├── tsconfig.json           # include 含 tests/**/*.ts
│   ├── vitest.config.ts        # 合并 vite.config 别名
│   └── vite.config.ts          # 开发代理 /api → localhost:8080
├── sql/
│   ├── admin_platform.sql      # 本地全量（wu-admin，MySQL 8）+ 附录
│   ├── admin_platform_mysql56.sql  # 生产空库全量（wuadmin，MySQL 5.6.5+）
│   ├── add1.sql                # 增量补丁 #1
│   ├── add2.sql                # 增量补丁 #2（登录 smsLoginSliderCaptchaEnabled）
│   ├── add3.sql                # 增量补丁 #3（菜单「企业IM」）
│   ├── add4.sql                # 增量补丁 #4（群消息 mention_ids）
│   ├── add5.sql                # 增量补丁 #5（工单字典）
│   ├── add6.sql                # 增量补丁 #6（流程中心菜单）
│   ├── add7.sql                # 增量补丁 #7（缓存监控菜单）
│   ├── add8.sql                # 增量补丁 #8（服务监控菜单）
│   ├── add9.sql                # 增量补丁 #9（群成员 notify_muted、announcement_read_time）
│   ├── add10.sql               # 增量 #10（回收中心 + sys_file 软删）
│   ├── add11.sql               # 增量 #11+#12（代码生成 + 唯一索引，本地）
│   ├── add12.sql               # 已合并至 add11（SKIP 提示）
│   ├── add6_7_wuadmin.sql      # 生产合并 add6+7+8+9
│   ├── add9_wuadmin.sql        # 生产仅 add9 字段
│   ├── add10_wuadmin.sql       # 生产 add10
│   ├── add11_wuadmin.sql       # 生产 #11+#12（代码生成，5.6 兼容）
│   ├── add13.sql               # 增量 #13（gen_table 软删 + 回收中心）
│   ├── add13_wuadmin.sql       # 生产 #13
│   ├── add14.sql               # 增量 #14（API 访问菜单排序）
│   ├── add14_wuadmin.sql       # 生产 #14
│   ├── add15.sql               # 增量 #15（部门 leader_user_id，本地）
│   ├── add15_wuadmin.sql       # 增量 #15（生产）
│   ├── add16.sql               # 增量 #16（调度日志 duration_ms，本地）
│   ├── add17.sql               # 增量 #17（调度日志软删 + 回收，本地）
│   # 生产 #15+#16+#17 已合并至 add15_wuadmin.sql
│   └── disable_devtool_off.sql # 临时关闭「禁止前端调试」（MySQL 5.6 兼容）
├── data/                       # 本地上传目录（git 忽略，对应 file.storage.local-path）
└── README.md
```

### 后端包结构（`backend/src/main/java`，分层约定）

依赖方向：**`modules/*` → `framework` → `common`**（`framework` 禁止引用 `modules`）。

```
cn.rbac.server/
├── common/
│   ├── pojo/                         # CommonResult、PageParam、PageResult、BusinessException
│   └── util/                         # ClientIpUtils、UserAgentUtils、IpLocationUtils
├── framework/                        # 技术基础设施（可抽公共 starter）
│   ├── config/                       # DynamicConfigProvider、DevRedissonConfig（dev）
│   ├── security/
│   │   ├── api/                      # PermissionApi（SPI）
│   │   ├── config/                   # SecurityConfig（CORS 从 yml 读取）
│   │   └── core/                     # TokenService、SecurityUtils
│   ├── web/
│   │   ├── core/                     # GlobalExceptionHandler、BusinessHttpStatusMapper、ApiErrorResponseWriter
│   │   └── filter/                   # SaTokenAuthenticationFilter、AuthorizationQueryFilter 等
│   ├── security/handler/             # JsonAuthenticationEntryPoint、JsonAccessDeniedHandler（401/403 JSON）
│   ├── log/annotation/               # @Log
│   ├── mybatis/、redis/、storage/
└── modules/
    └── system/                       # 系统域业务
        ├── api/                      # 瘦 Controller：@PreAuthorize、@Validated、委托 Service
        │   ├── tool/GenController    # 代码生成 /tool/gen
        │   └── */vo/                 # 请求 VO（JSR-303 校验）
        ├── service/gen/              # GenTableServiceImpl、Velocity 渲染与写文件
        ├── pay/                      # 微信/支付宝测试下单与回调
        ├── service/                    # 业务逻辑（auth、user、role、ticket、permission 等）
        ├── dal/                        # DO、Mapper
        └── framework/                # 对本项目 framework SPI 的实现
            ├── config/               # SystemConfigProvider
            ├── security/             # SystemPermissionService（bean 名 ss）
            ├── operlog/              # LogAspect、OperLogRecorder
            └── monitor/              # API 访问采集拦截器
```

| 扩展场景 | 做法 |
|----------|------|
| 改会话/上传限制等运行时配置 | 改库表 `sys_config_group`，经 `SystemConfigHelper` → `SystemConfigProvider` |
| 新增业务接口 | 在 `service/` 写业务逻辑，Controller 只做入参校验与 `CommonResult` 封装；业务错误抛 `BusinessException` |
| 新增业务模块 | 增加 `modules/xxx`，在 `xxx/framework` 实现 SPI，勿让 `framework` 依赖业务 |
| 复用基础层 | 将 `common` + `framework` 打成 jar，供其它 Spring Boot 项目依赖 |

### 全局异常处理与错误契约

前后端统一约定：**业务可预期错误在 Service 层抛 `BusinessException`，由 `GlobalExceptionHandler` 转为 `CommonResult`；HTTP 状态码与 body.`code` 对齐**。Controller **不要** `return CommonResult.error(...)`，也不要 broad `catch (Exception)` 把内部堆栈返回前端。

#### 处理链路

```
请求 → Filter（Sa-Token 桥接）→ Security（未登录/无权限 → JSON EntryPoint）
     → Controller → Service 抛 BusinessException
     → GlobalExceptionHandler → BusinessHttpStatusMapper → ResponseEntity + CommonResult
     → 前端 request.ts 拦截器（401 跳登录 / 403 权限提示 / 429 限流 warning）
```

#### 核心类

| 类 | 路径 | 职责 |
|----|------|------|
| `BusinessException` | `common/pojo/` | 业务异常，`code` 默认 400，可传 401/403/404/429/500 等 |
| `BusinessHttpStatusMapper` | `framework/web/core/` | 业务码 → `HttpStatus`（401/403/404/405/415/429/500，其余 400） |
| `GlobalExceptionHandler` | `framework/web/core/` | `@RestControllerAdvice`，17 个 `@ExceptionHandler` |
| `ApiErrorResponseWriter` | `framework/web/core/` | 向 Servlet 响应写入 `CommonResult` JSON（供 Security 过滤器链使用） |
| `JsonAuthenticationEntryPoint` | `framework/security/handler/` | 未登录 → HTTP 401 + `CommonResult` |
| `JsonAccessDeniedHandler` | `framework/security/handler/` | 无权限 → HTTP 403 + `CommonResult` |

#### 已覆盖的异常类型（17 个）

| 异常 | HTTP | code | 典型场景 |
|------|------|------|----------|
| `BusinessException` | 动态 | 原始 code | 资源不存在、权限不足、限流等业务错误（**首选**） |
| `IllegalArgumentException` | 400 | 400 | 定时任务调用格式、文件校验、Cron 表达式等（记 warn 日志） |
| `IllegalStateException` | 400 | 400 | Quartz 任务状态异常等（记 warn 日志） |
| `MethodArgumentNotValidException` | 400 | 400 | `@RequestBody` JSR-303 校验失败 |
| `ConstraintViolationException` | 400 | 400 | 方法参数 `@Validated` 校验失败 |
| `HttpMessageNotReadableException` | 400 | 400 | JSON 解析失败 |
| `MissingServletRequestParameterException` | 400 | 400 | 缺少 `@RequestParam` |
| `MissingPathVariableException` | 400 | 400 | 缺少 `@PathVariable` |
| `MethodArgumentTypeMismatchException` | 400 | 400 | 参数类型不匹配 |
| `MaxUploadSizeExceededException` | 400 | 400 | 上传超限（文案读取 `DynamicConfigProvider`） |
| `SecurityException` | 403 | 403 | 文件路径穿越等安全拒绝（记 warn 日志 + IP） |
| `AccessDeniedException` | 403 | 403 | `@PreAuthorize` 鉴权失败 |
| `NotLoginException` | 401 | 401 | Sa-Token 注解鉴权未登录 |
| `NoHandlerFoundException` | 404 | 404 | 路由不存在（需 `spring.mvc.throw-exception-if-no-handler-found: true`） |
| `HttpRequestMethodNotSupportedException` | 405 | 405 | HTTP 方法不支持 |
| `HttpMediaTypeNotSupportedException` | 415 | 415 | Content-Type 不支持 |
| `Exception`（兜底） | 500 | 500 | 未预期异常；服务端记完整堆栈，对外仅「服务器内部错误」 |

#### 开发约定

```java
// Service 层 — 推荐
throw new BusinessException(404, "工单不存在");
throw new BusinessException(403, "仅审批人可操作");
throw new BusinessException(429, "操作过于频繁");
throw new BusinessException("用户名已存在");  // 默认 code=400

// Controller 层 — 禁止
return CommonResult.error(404, "xxx");        // ❌ HTTP 200 与 body.code 不一致
catch (Exception e) { return CommonResult.error(500, e.getMessage()); }  // ❌ 泄露内部信息
```

- **可观测性**：`handleBusiness` / `handleException` / `handleSecurity` 记录 method、URI、IP；`IllegalArgumentException` / `IllegalStateException` 记 warn；`SaTokenAuthenticationFilter` 鉴权异常记 debug（不静默吞掉）。
- **配置**：`application.yml` 已启用 `spring.mvc.throw-exception-if-no-handler-found: true`，404 走统一 JSON 而非默认错误页。

#### 前端对齐（`frontend/src/utils/request.ts`）

| body.code / HTTP status | 行为 |
|-------------------------|------|
| 200 / 0 | 成功，返回 `res.data` |
| 401 | 公开登录接口 Toast 错误文案；已登录态清会话 Cookie 并跳转 `/login` |
| 403 | Toast「权限不足」（`silent403: true` 可抑制；2 秒防抖） |
| 429 | `ElMessage.warning` 限流提示 |
| 其它 | Toast `message` 字段 |

页面 `catch` 时可用 `getErrorMessage` / `isErrorToastShown`（`utils/axiosError.ts`）避免与拦截器重复弹窗。

#### 单测

| 测试类 | 说明 |
|--------|------|
| `GlobalExceptionHandlerTest` | 约 20 用例，覆盖业务码 HTTP 映射、校验拼接、401/403/404/405/415、兜底不泄露等 |
| `BusinessHttpStatusMapperTest` | 业务码 → HTTP 状态映射 |

运行：`cd backend && mvn test -Dtest=GlobalExceptionHandlerTest`

---

## 环境要求

| 软件 | 版本建议 |
|------|----------|
| Node.js | 18+ |
| Maven | 3.6+ |
| JDK | 17 |
| Spring Boot | **3.5.13** |
| Springdoc / Knife4j | 2.8.9 / 4.5.0（见上文接口文档说明） |
| MySQL | 本地 **8.0**；生产常见 **5.6.5+**（空库用 `admin_platform_mysql56.sql`） |
| Redis | 7.x |

---

## 快速开始

### 1. 初始化数据库

```bash
# 空库全新安装（直接执行即可）
mysql -u root -p < sql/admin_platform.sql

# 已有库发版增量（按版本依次执行，本地库 wu-admin）
mysql -u root -p wu-admin < sql/add1.sql
mysql -u root -p wu-admin < sql/add2.sql
mysql -u root -p wu-admin < sql/add3.sql
mysql -u root -p wu-admin < sql/add4.sql
mysql -u root -p wu-admin < sql/add5.sql
mysql -u root -p wu-admin < sql/add6.sql
mysql -u root -p wu-admin < sql/add7.sql
mysql -u root -p wu-admin < sql/add8.sql
mysql -u root -p wu-admin < sql/add9.sql
mysql -u root -p wu-admin < sql/add10.sql
mysql -u root -p wu-admin < sql/add11.sql
# 按发版记录继续补跑 add13 … add17（见「数据库脚本」表）

# 生产空库全新安装（MySQL 5.6，库名 wuadmin）
mysql -u wuadmin -p wuadmin < sql/admin_platform_mysql56.sql

# 生产已有库增量
mysql -u wuadmin -p wuadmin < sql/add6_7_wuadmin.sql   # 若未跑过 add6–9
mysql -u wuadmin -p wuadmin < sql/add10_wuadmin.sql
mysql -u wuadmin -p wuadmin < sql/add11_wuadmin.sql
# 按发版记录继续补跑 add13_wuadmin … add15_wuadmin（见「数据库脚本」表）

# 极旧库首次补全（缺表/菜单时，执行 admin_platform.sql 附录段，约 910 行起）
```

> 空库可直接跑全文；**已有表的旧库**跑全文会被熔断拦截。旧库请用附录或 `add1.sql`；强制重装须 `SET @WU_ADMIN_ALLOW_DROP=1`。

### 2. 启动 Redis

`127.0.0.1:6379`，database **1**。本地 dev 一般**无密码**；生产（prod）见 `application-prod.yml`（如密码 `root`）。

### 3. 启动后端（8080）

**本地开发**（使用 `dev` profile，连接本地 `wu-admin` 库、无密 Redis）：

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE='dev'
mvn spring-boot:run -DskipTests
```

**生产 / 默认**（`prod` profile，见 `application-prod.yml`）：

```powershell
cd backend
mvn spring-boot:run -DskipTests
```

或打包后：`java -jar backend.jar`（默认即为 `prod`）。

### 4. 启动前端（3000）

```powershell
cd frontend
npm install
npm run dev
```

访问：**http://localhost:3000**

### 5. 启动移动端 H5（可选，5173）

```powershell
cd uniapp
npm install
npm run dev:h5
```

浏览器访问终端提示的本地地址（通常 **http://localhost:5173**）。接口走 Vite 代理至 `http://localhost:8080/api`。

### 6. 运行单元测试（可选）

```powershell
cd frontend && npm run test
cd backend && mvn test
```

说明见 [单元测试](#单元测试)。代码托管在 GitHub 时，push / PR 会触发 [持续集成（CI）](#持续集成ci) 自动跑相同检查。

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
| `spring.profiles.active` | 默认 **`prod`**（`SPRING_PROFILES_ACTIVE` 可覆盖）；本地开发用 **`dev`** |
| `spring.datasource.*` | MySQL；prod 见 `application-prod.yml`（`wuadmin`），dev 见 `application-dev.yml`（`wu-admin`） |
| `spring.redis.database` | `1` |
| `spring.data.redis.password` | **仅 `application-prod.yml`**（prod）；dev 无密码，由 `DevRedissonConfig` 处理 Redisson |
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
- 生产：`npm run build` 后将 `dist` 部署到 Web 服务器，并将 `/api` 反代到后端 `8080`（context-path 为 `/api`）

---

## 主要 API 前缀（`/api`）

| 前缀 | 说明 |
|------|------|
| `/api/auth/**` | 登录、注册、验证码、`config`（公开配置）、**profile**（个人中心）；`/auth/**` 匿名可访问，业务接口内由 Sa-Token 校验登录态 |
| `/api/system/**` | 用户、角色、菜单、组织、字典、**config-group**（含 test-payment）、审批、工单、**announce/chat** 等 |
| `/api/pay/**` | 支付回调（`/notify/*` 公开）、测试订单查单 |
| `/api/files/**` | 文件上传与访问 |
| `/api/monitor/**` | API 访问、在线用户、**定时任务**、**Redis 缓存监控**（`/monitor/cache/*`）、**服务监控**（`/monitor/server/info`） |
| `/api/dashboard/**` | 工作台统计（含 `chatUnreadCount`、`configGroupCount`、待审核用户数等）；计数类指标单 SQL 聚合 + Redis 缓存（约 2 分钟） |
| `/api/system/user/list` | 用户下拉选项（启用用户、最多 2000 条）；管理列表请用 `/api/system/user/page` |
| `/api/system/recycle/summary` | 回收中心 12 类软删数量汇总 |
| `/api/system/export/*` | 列表 Excel/CSV 导出（用户、日志、工单、审批等） |
| `/api/tool/gen/**` | 代码生成：可导入表列表、导入、配置、预览、ZIP、写入项目、建菜单 |

---

## 数据库脚本

维护 **`sql/admin_platform.sql`**（本地全量 + 附录）、**`sql/admin_platform_mysql56.sql`**（生产空库）与 **`sql/add1.sql` … `add17.sql`** 等增量：

| 场景 | 做法 |
|------|------|
| **全新安装（本地）** | 空库 `mysql -u root -p < sql/admin_platform.sql` |
| **全新安装（生产 5.6）** | 空库 `mysql -u wuadmin -p wuadmin < sql/admin_platform_mysql56.sql` |
| **极旧库首次升级** | 执行 `admin_platform.sql` 文末 **附录**（约 990 行起） |
| **发版增量（本地）** | 按版本依次 `add1.sql` … **`add17.sql`**（勿跳号） |
| **发版增量（生产）** | 按已执行版本补跑对应 `*_wuadmin.sql`（`add15_wuadmin.sql` 含 #15+#16+#17） |

`addN.sql` 体量应保持在几十行量级；全量补丁逻辑在 `admin_platform.sql` 附录。

| 增量脚本 | 内容 |
|----------|------|
| `add1.sql` | 清理旧版冗余索引等 |
| `add2.sql` | 登录配置 `smsLoginSliderCaptchaEnabled`（短信发码前滑块，默认 `false`） |
| `add3.sql` | 菜单 id=172「即时聊天」→「**企业IM**」 |
| `add4.sql` | `sys_chat_group_message.mention_ids`（群 @ 提醒，可重复执行） |
| `add5.sql` | 补全工单字典类型 `sys_ticket_status` / `sys_ticket_priority` |
| `add6.sql` | 菜单 id=8「业务中心」→「**流程中心**」（path `/workflow`）；排序至消息中心与开发工具之间；本地库 `wu-admin` |
| `add7.sql` | 系统监控新增「**缓存监控**」菜单与权限；本地库 `wu-admin` |
| `add8.sql` | 系统监控新增「**服务监控**」菜单（本机 JMX）；本地库 `wu-admin` |
| `add9.sql` | 群成员 `notify_muted`、`announcement_read_time`（群公告已读 / 免打扰）；本地库 `wu-admin` |
| `add10.sql` | **回收中心**菜单 id=163；`sys_file` 软删字段；本地 `wu-admin` |
| `add11.sql` | **代码生成** `gen_table` / `gen_table_column`、菜单 164–169/179、**`table_name` 唯一索引**（#11+#12）；本地 `wu-admin` |
| `add12.sql` | 已合并至 `add11.sql`，单独执行仅 SKIP |
| `add6_7_wuadmin.sql` | 生产 `wuadmin`：add6 + add7 + add8 + add9 |
| `add9_wuadmin.sql` | 生产仅 add9 字段 |
| `add10_wuadmin.sql` | 生产 add10 |
| `add11_wuadmin.sql` | 生产 add11+#12（`table_name VARCHAR(191)` 适配 MySQL 5.6） |
| `add13.sql` / `add13_wuadmin.sql` | `gen_table` 软删；回收中心支持代码生成表 |
| `add14.sql` / `add14_wuadmin.sql` | 系统监控「API 访问统计」菜单排序调至最底 |
| `add15.sql` | 部门表 `leader_user_id` 及按姓名回填（本地） |
| `add15_wuadmin.sql` | 生产合并 **#15 + #16 + #17**（负责人关联 + 调度日志耗时 + 软删） |
| `add16.sql` | `sys_job_log.duration_ms`（执行耗时毫秒，本地） |
| `add17.sql` | `sys_job_log` 软删字段；回收中心可恢复调度日志（本地） |
| `admin_platform_mysql56.sql` | 生产**空库全量**（38 表 + 初始数据，无附录 JSON 函数依赖） |

> 生产环境若尚未执行 add3/add4，可将 `add3.sql`、`add4.sql` 中 `USE` 改为 `wuadmin` 后逐条执行，或直接依赖已更新的 `admin_platform.sql` 全量/附录。仓库内**无**单独的 `add3_add4_wuadmin.sql` 文件。

**附录 / 增量行为（可重复执行、尽量非破坏性）：**

| 项 | 行为 |
|----|------|
| 字典数据 | `(dict_type, dict_value)` 唯一索引（**不含 deleted**）；附录先去重（保留 **id 较小** 的一条）再加索引，随后 upsert。内置字典无影响；若未来字典支持软删后同 value 再建，需调整索引或应用层约束 |
| 菜单（160–162、170–178、180–184 等） | 默认 `INSERT IGNORE`，**不覆盖**已存在行的 name/path/icon；需强制同步时 `SET @WU_ADMIN_SYNC_MENU=1` |
| 普通用户（role_id=2） | 仅 `INSERT IGNORE` 补缺失菜单，**不会 DELETE 清空**已自定义权限 |
| 内置定时任务 id 1～6 | 仅 `INSERT IGNORE` 首次插入，**不会覆盖**管理员已改的 cron / status |
| 旧版任务迁移 | 仅当 `invoke_target` 仍为 `refreshDictCache` / `refreshConfigCache` 时才改写 |
| 注册审批补单 | `content` 使用 `JSON_OBJECT` 生成，避免用户名含引号导致非法 JSON |

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

在 `modules/system/api` 的 Controller 方法上添加 `@Log`（`framework.log.annotation`），由 `modules/system/framework/operlog/LogAspect` 经 `OperLogRecorder` 写入 `sys_oper_log`。登录、注册、工单流转、审批处理、定时任务等核心写操作已补齐 `@Log`。

### 按钮权限

```html
<el-button v-permission="'system:config:update'">保存</el-button>
```

标识与 `sys_menu.permission` 一致，如 `system:approval:approve`。

### 移动端 H5 浅栈返回（uni-app）

刷新后 `getCurrentPages().length === 1` 时无法使用 `uni.navigateBack`，由以下机制兜底：

| 机制 | 文件 | 说明 |
|------|------|------|
| 父级映射 | `utils/nav-history.ts` | `recordNavParent` / `pinNavParent` 写入 `localStorage`；`navigateToParent` 解析后 `redirectTo` |
| 列表内返回 | `components/common/SubPageBackBar` | 仅浅栈时显示；与左上角 `H5BackButton` 互斥 |
| 来源参数 | `utils/nav-from.ts` | 选择页 URL 带 `from`，刷新后可解析父页 |
| 全局守卫 | `plugins/global-page-guards.ts` | Tab 页跳过浅栈同步，避免误关返回按钮 |

子包列表页调用 `useH5ListPageNav()`；编辑/详情页在 `onLoad` 调用 `pinNavParent(返回目标)`。

### 登录 / 注册页

- 路由：`/login`、`/register`；页面逻辑见 `views/login/composables/useLoginForm.ts`、`views/register/composables/useRegisterForm.ts`；共享布局与验证码见 `views/auth/components/`（`AuthSplitLayout`、`AuthCaptchaField`、`AuthParticleBackground`）。
- 左侧为 **Three.js 3D 地球**（`frontend/src/components/earth/Earth3D.vue`），透明画布透出粒子星空，支持鼠标拖拽旋转与滚轮缩放。
- 文案与验证码等行为由公开配置驱动，见下节。
- **记住我**：仅持久化「账号登录」模式下的用户名；切换 Tab 不会清空勾选状态。
- **短信 Tab**：未填手机号点击「获取验证码」会提示「请输入手机号」；开启发码前滑块时先弹 `SliderCaptcha` 再发码。
- **账号登录**：用户名或密码错误时提示「**账号或密码错误**」（不再误报「登录已过期」）；公开认证接口（`/auth/login` 等）的 401 与已登录态 token 失效区分处理。
- **注册页**：公开接口（登录/注册/验证码/config）请求**不携带**管理员 Token，避免误报 403；软删用户名再次注册由后端自动恢复账号。

### 登录页读取配置

```typescript
// frontend/src/api/system/auth/index.ts
GET /auth/config  // 实际请求 /api/auth/config
```

滑块验证码组件见 `frontend/src/components/SliderCaptcha.vue`。

---

### 前端 TypeScript

- 业务代码均为 **`.ts`**，Vue 页面使用 `<script setup lang="ts">`。
- `tsconfig.json` 已开启 **`strict: true`** 与 **`noImplicitAny: true`**。
- 业务 API（`api/system/*`、`api/message`、`api/monitor`）为各模块定义了 **VO / SaveDTO / PageQuery** 类型。
- Pinia `store/user`、`store/message` 与 API 类型对齐（`AuthInfo`、`NoticeVO`）。
- 类型检查：`cd frontend && npm run typecheck`（`vue-tsc --noEmit`）。
- 单元测试：`cd frontend && npm run test`（Vitest）；开发时监听 `npm run test:watch`。
- ESLint：`cd frontend && npm run lint`。
- 生产构建会先跑类型检查：`npm run build`。

### 单元测试

项目采用**前后端分离的单测目录**，不依赖真实 MySQL/Redis，核心 Service 与工具函数用 Mock 隔离。

#### 运行命令

```powershell
# 前端（Vitest + happy-dom）
cd frontend
npm run test          # 一次性执行
npm run test:watch    # 监听模式

# 后端（JUnit 5 + Mockito + Spring Test）
cd backend
mvn test              # 全量单测
mvn test -Dtest=AuthServiceImplTest   # 指定类
```

打包仍可跳过测试：`mvn clean package -DskipTests`（见下文「构建与打包」）。

#### 前端覆盖（`frontend/tests/unit/`，约 86 用例）

| 目录 | 文件 | 说明 |
|------|------|------|
| `store/` | `user.test.ts`、`message.test.ts`、`tagsView.test.ts` | 登录态、消息 WebSocket 未读、页签缓存 |
| `utils/` | `request.test.ts`、`menu-tree.test.ts`、`org-tree.test.ts`、`hasMenuPerm.test.ts`、`chat-message.test.ts`、`message-push.test.ts`、`loginRemember.test.ts`、`api-response.test.ts`、`axiosError.test.ts` | 拦截器、菜单/组织树、权限判断、聊天文案、推送防抖 |
| `api/` | `system/file/with-token-query.test.ts` | 带 Token 的文件 URL |

配置：`vitest.config.ts` 合并 `vite.config.ts` 的 `@` 别名；`tsconfig.json` 的 `include` 含 `tests/**/*.ts`，IDE 可正确解析测试文件中的路径别名。

#### 后端覆盖（`backend/src/test/java/`，约 87 用例）

| 测试类 | 覆盖要点 |
|--------|----------|
| `AuthServiceImplTest` | 账号登录成功/失败/限流/锁定；注册；短信登录；短信发码限流 |
| `PermissionServiceImplTest` | 权限码匹配、菜单树过滤、角色/菜单分配 |
| `UserServiceImplTest` / `RoleServiceImplTest` / `DeptServiceImplTest` | CRUD 校验、树操作、回收站 |
| `RegisterApprovalServiceImplTest` | 注册审批通过/拒绝、待审去重 |
| `ChatServiceTest` | 私聊/群聊发送、撤回、建群、成员管理、解散 |
| `TicketServiceImplTest` | 工单创建、评论、状态流转 |
| `MenuServiceImplTest` / `DictDataServiceImplTest` | 菜单树、字典缓存刷新 |
| `GlobalExceptionHandlerTest` / `BusinessHttpStatusMapperTest` | 17 类异常处理、HTTP 状态对齐、安全/兜底/校验场景 |

公共支撑：`testsupport/MybatisLambdaTestBase`（MyBatis-Plus `LambdaQueryWrapper` 元数据初始化）、`ServiceTestFixtures`（User/Role/Menu 等测试数据构造）。

> 当前以 **Service 层单元测试** 为主，尚未接入 JaCoCo 覆盖率报告与 Controller 层 `MockMvc` 集成测试；新增核心业务逻辑时建议同步补充对应 `*Test.java` / `*.test.ts`。

### 持续集成（CI）

仓库已配置 **GitHub Actions**（`.github/workflows/ci.yml`），在 `push` / `pull_request` 到 `main`、`master` 或 `dev` 时自动执行质量检查；也可在 GitHub 仓库 **Actions** 页手动 **Run workflow**（`workflow_dispatch`）。

#### 流水线结构

两个 Job **并行**执行，互不阻塞：

| Job | 环境 | 步骤 |
|-----|------|------|
| **Backend (Maven)** | Ubuntu，JDK **17**（Temurin），Maven 依赖缓存 | `mvn -B test`（约 126 用例） |
| **Frontend (Node)** | Ubuntu，Node **20**，`npm ci` 缓存 | `npm run typecheck` → `npm run test`（约 85 用例） |

同一分支有新提交时，进行中的旧 run 会被取消（`concurrency`），避免浪费 Actions 分钟。

#### 本地与 CI 对照

| 检查项 | 本地命令 | CI |
|--------|----------|-----|
| 后端单测 | `cd backend && mvn test` | ✅ Backend Job |
| 前端类型检查 | `cd frontend && npm run typecheck` | ✅ Frontend Job |
| 前端单测 | `cd frontend && npm run test` | ✅ Frontend Job |
| 前端 Lint | `cd frontend && npm run lint` | ❌ 暂未纳入（存量 `vue/no-mutating-props` 等待清理） |
| 覆盖率门禁（JaCoCo） | — | ❌ 未配置 |

#### 启用与分支保护（可选）

1. 将 `.github/workflows/ci.yml` 提交并推送到 GitHub。
2. 仓库 **Settings → Actions → General** 确认已启用 Actions。
3. （推荐）**Settings → Branches → Branch protection**：勾选 **Require status checks to pass**，选择 `Backend (Maven)` 与 `Frontend (Node)`，合并 PR 前须 CI 通过。

本地提交前仍建议执行 `npm run typecheck`、`npm run test` 与 `mvn test`；CI 作为第二道防线，在干净环境中复现结果。

### 构建与打包

```powershell
# 前端 → frontend/dist/
cd frontend
npm run build

# 后端 → backend/target/backend.jar（默认 prod profile）
cd backend
mvn clean package -DskipTests
```

**产物说明**

| 产物 | 路径 | 说明 |
|------|------|------|
| 前端静态资源 | `frontend/dist/` | 部署到 Nginx 等 Web 服务器根目录 |
| 后端可执行包 | `backend/target/backend.jar` | `java -jar backend.jar`，默认 `prod`，监听 `8080`，context-path `/api` |

**生产默认连接配置**（`application-prod.yml`，部署前请修改）：

| 项 | 默认值 |
|----|--------|
| MySQL 库名 | `wuadmin` |
| MySQL 用户 / 密码 | `wuadmin` / `root` |
| Redis 密码 | `root`（database `1`） |
| CORS | `app.cors.allowed-origins` 须改为实际域名（示例：`https://wushij.online,https://www.wushij.online`） |

**Nginx 反代要点**：静态资源走 `root` + `try_files`；`/api/` 反代到 `http://127.0.0.1:8080/api/`；WebSocket 需 `Upgrade` / `Connection` 头（消息推送）。

**仅前端发版**（如监控折线、个人中心样式）：覆盖 `frontend/dist/` 并强刷浏览器即可，**不必重启 jar**。涉及菜单/SQL/后端接口时须同步后端与数据库增量。

启动示例：

```bash
java -jar backend.jar
# 或覆盖敏感配置：
java -jar backend.jar --spring.datasource.password=xxx --spring.data.redis.password=xxx
```

### 新增页面开发约定（前端）

1. 在 `frontend/src/router` 注册路由，`path` 与 `sys_menu.path` 保持一致。
2. 页面目录：`views/<模块>/index.vue` 仅作路由入口，业务放在 `*Page.vue` + `composables/use*Page.ts`。
3. 可复用 UI 拆到同目录 `components/`；登录注册类共用 `views/auth/components/`。
4. 列表页优先用 `DictSelect` / `DictTag`；按钮权限用 `v-permission`。
5. 提交前执行 `npm run typecheck` 与 `npm run test`；涉及后端 Service 变更时执行 `mvn test`。推送到 GitHub 后由 [CI](#持续集成ci) 自动复跑。

---

## 常见问题

**Q：生产环境登录失败，接口返回 HTML 或「检查网络连接」？**  
A：① 浏览器访问 `https://域名/api/auth/config`，若返回 `index.html` 则是 **Nginx 未反代 `/api`**（见上文「生产部署排障实录」）；② 若返回 JSON 仍失败，检查 `application-prod.yml` 中 **CORS 域名**是否为实际站点（dev 相对 master 从 `*` 改为可配域名，占位符会导致异常）；③ 验证码须填写；④ 清除站点 Cookie 后重新登录（Token 已改为 **httpOnly Cookie**，不再使用 `localStorage`）。

**Q：登录后菜单为空或 403？**  
A：确认已导入 `admin_platform.sql` 或为角色分配菜单，然后重新登录。

**Q：系统配置页报错或只有 login/register？**  
A：对已有库：极旧库先跑 **admin_platform.sql 附录**（补全 site/session/file/rateLimit 等）；已跑过附录则按需依次执行 `add1.sql`、`add2.sql` 增量。

**Q：注册后无法登录？**  
A：若开启「注册需审核」，需管理员在审批单中心通过；登录提示「账号待审核」属正常。驳回后账号已软删，需重新注册或联系管理员。

**Q：用户管理里看到待审核用户，或一进页面就弹「禁用用户」？**  
A：升级后默认列表已排除待审核/驳回用户；待审用户仅在审批单中心处理。若仍为旧版，请更新前后端并刷新页面。

**Q：驳回后同用户名无法注册，或提示权限不足？**  
A：① 确认 **注册认证** 已开启开放注册；② 升级后软删用户名可自动恢复再注册；③ 若 **回收中心** 仍有该用户，可「彻底删除」后再试；④ 注册/登录请求勿带管理员 Token（新版前端已自动跳过）。

**Q：回收中心菜单不显示，或文件列表报 `Unknown column 'deleted'`？**  
A：对已有库执行 **`sql/add10.sql`**（生产 **`add10_wuadmin.sql`**），**重新登录**。全量新库已含菜单 163 与 `sys_file` 软删字段。

**Q：代码生成菜单不显示，或导入报「表已导入」/ 表不存在？**  
A：已有库执行 **`sql/add11.sql`**（生产 **`add11_wuadmin.sql`**），**重新登录**。空库请用已含 §17 的 `admin_platform.sql` 或 `admin_platform_mysql56.sql`。生产 MySQL 5.6 勿用本地 `admin_platform.sql` 全文（附录含 5.7+ JSON 函数）。

**Q：生产导入 gen_table 报 `Specified key was too long`（1071）？**  
A：MySQL 5.6 + utf8mb4 唯一索引上限 767 字节，请用 **`add11_wuadmin.sql`** 或 **`admin_platform_mysql56.sql`**（`table_name VARCHAR(191)`），勿对生产库套用本地 `VARCHAR(200)` 建表语句。

**Q：回收中心某个 Tab 看不到（如岗位）？**  
A：Tab 按各模块 **列表权限** 显示（如岗位需 `system:post:list`）；无权限的类别不会展示，但汇总接口仍可能返回计数。

**Q：文件恢复失败提示磁盘不存在？**  
A：软删仅改库表标记，若磁盘文件已被手动删除则无法恢复；可在回收中心「彻底删除」清理无效记录。超过 `app.job.file-recycle-retention-days`（默认 30 天）的记录可由定时任务 **文件回收站清理** 自动清盘（内置任务，默认暂停，需在定时任务页启用）。

**Q：如何开启短信发码前滑块？**  
A：**系统配置 → 登录认证** 开启「短信验证码登录」与「发送前滑块验证」；已有库执行 `sql/add2.sql` 补配置字段，保存后刷新登录页。

**Q：接口文档 iframe 空白或 `/v3/api-docs` 403？**  
A：① 确认后端（8080）已启动，浏览器访问 `http://127.0.0.1:8080/api/v3/api-docs` 应返回 JSON；② 开发环境重启 Vite 以加载 Knife4j 代理；③ 生产环境确认已部署含 `Knife4jIframeHeaderFilter` 的后端（响应头 `X-Frame-Options: SAMEORIGIN`）；④ 勿将 springdoc 降为 2.6（与 Spring Boot 3.5 不兼容）；⑤ `knife4j.enable` 保持 `false` 直至升级兼容的 Knife4j 版本。

**Q：Knife4j 调试 404 或返回 HTML？**  
A：已配置 OpenAPI 默认服务 `http://localhost:3000/api`；重启后端与 Vite 后，在文档页选择该服务器、方法用 PUT/POST，并填 `Authorization`。若仍 401，先登录管理端复制 token。

**Q：上传失败提示大小或类型？**  
A：在 **系统配置 → 文件存储** 调整；单文件上限不得超过 500MB。

**Q：消息中心菜单不显示或聊天 403？**  
A：对已有库：极旧库先跑 **admin_platform.sql 附录**；发版增量依次跑 `sql/add1.sql` … `add4.sql`（生产改 `USE wuadmin` 后执行 add3/add4），**重启后端**后 **重新登录**。普通用户需角色分配菜单 170/172；只读权限用户访问 `:list` 接口时会映射为 `:query`。

**Q：缓存/服务监控菜单不显示？**  
A：已有库执行 `sql/add7.sql`、`add8.sql`（本地）或生产 **`sql/add6_7_wuadmin.sql`**，**重新登录**刷新侧栏；确认角色已分配 `monitor:cache:list` / `monitor:server:list`。

**Q：监控折线图一切页或 F5 就清空？**  
A：升级至含 **全局后台采样 + sessionStorage** 的前端后，登录即有权限则后台持续采样；同一标签页 F5 仍保留最近 20 个点。关闭标签页或退出登录会清空。仅更新前端 `dist` 即可，**无需改后端 jar**。

**Q：服务监控「平均负载」显示 `-`？**  
A：**Windows** 不提供 Linux 式 load average，JMX 返回不可用，界面显示 `-` 属正常；请参考 **系统 CPU / 进程 CPU** 及 CPU 折线图。Linux 部署会显示数值。

**Q：顶栏有通知角标但列表为空？**  
A：确认 WebSocket 已连接（登录后自动初始化）；在顶栏铃铛打开「系统通知」Tab 会拉取列表。管理员发布通知需 `system:announce:publish`。

**Q：聊天图片出现在文件管理里？**  
A：升级后新图片走 `/system/chat/upload/image`（`images/chat/`）、新文件走 `/system/chat/upload/file`（`files/chat/`），文件列表已排除；历史旧数据可手动删除。

**Q：群公告保存后别人看不到置顶条，或没有弹窗提醒？**  
A：① 执行 `sql/add9.sql`（生产 `add9_wuadmin.sql` 或 `add6_7_wuadmin.sql`）；② **重启后端**；③ 前后端一并升级（需 `groupAnnouncement` WebSocket）；④ 公告变更会重置全员已读，成员进入群聊或收到推送后应显示置顶条。

**Q：开了群免打扰仍收到所有消息提醒？**  
A：确认 `sys_chat_group_member.notify_muted` 字段已入库（add9）；升级含 `message-push.ts` 的前端后，仅 **@ 我** 与 **群公告** 会弹窗并计角标，普通群消息应被过滤。

**Q：普通成员能改群名称或群公告吗？**  
A：不能。仅 **群主 / 群管理员** 可编辑；普通成员群组详情中为只读，保存按钮对其不可见或无权限。

**Q：企业IM 升级后 @ / 撤回 / 文件发送不可用？**  
A：① 已有库执行 `sql/add4.sql`（生产改 `USE wuadmin`）；② **重启后端**；③ 重新登录刷新菜单名「企业IM」；④ 确认角色有 `system:chat:list`。

**Q：群聊 @ 没有强提醒或角标？**  
A：确认 `mention_ids` 字段已入库（add4）；被 @ 时 WebSocket 推送带 `atMe: true`，顶栏通知标题为 `[有人@你]`。

**Q：撤回后对方要刷新才看到？**  
A：升级至含撤回 WebSocket 同步的版本并重启后端；私聊/群聊均通过 `recall` 事件实时更新，无需刷新。

**Q：普通用户看不到「创建群聊」？**  
A：仅 **超级管理员**（`super_admin`）可建群，属预期行为；`GET /system/chat/can-create-group` 返回 false 时侧栏不显示「+」。

**Q：聊天文件下载变成乱码或 txt？**  
A：升级后后端按扩展名返回正确 MIME 并触发下载；请重新部署含 `FileContentTypes` 的后端 jar。

**Q：开启「禁止前端调试」无效？**  
A：在 **系统配置 → 安全配置** 保存后需 **整页刷新**；缺 `security` 分组时先跑 **admin_platform.sql 附录**。此为浏览器端限制，无法替代后端鉴权。

**Q：生产已开「禁止前端调试」，F12 打不开如何排障？**  
A：执行 `sql/disable_devtool_off.sql`（库名 `wuadmin`；**MySQL 5.6 勿用 JSON_SET**），重启后端并 `Ctrl+F5` 强刷；或在系统配置关闭后保存。调试完请恢复。

**Q：企业 IM 联系人在线状态不实时更新？**  
A：升级至含 **`presence` WebSocket 推送** 的版本并重启后端；双方均须已登录且 WebSocket 已连接。旧版仅进入页面时 `loadUsers()` 拉取一次在线状态。

**Q：企业 IM 输入框多了一圈黑边框？**  
A：组件化拆分时独立 CSS 中 `:deep()` 无效所致；升级至已修复的 `chat-page.css` 后重新 `npm run build` 部署前端。

**Q：字典里多了好几个「××（副本）」？**  
A：「复制类型」每点一次生成一条（`原编码_copy_时间戳`），误点多次即多条；删除多余副本并「刷新缓存」即可，业务仍用原字典类型。

**Q：系统配置没有「第三方配置 / 支付配置 / 短信配置」Tab？**  
A：对已有库：极旧库先跑 **admin_platform.sql 附录**；发版增量跑 `sql/add1.sql`；**重启后端**并刷新页面。

**Q：登录页没有「短信登录」？**  
A：在 **系统配置 → 登录认证** 开启「短信验证码登录」，并在 **短信配置** 中启用短信；保存后刷新登录页。短信 Tab 仅支持已在个人中心绑定的手机号。

**Q：短信验证码发送失败或被限流？**  
A：检查 **接口限流** 分组中的短信防刷项（每 IP 每分钟、同号间隔、日上限）；阿里云需在控制台配置短信认证方案与模板 100001。

**Q：测试支付下单失败或支付后状态不更新？**  
A：① 先在支付配置 Tab **保存全部**再点「生成测试订单」；② 检查商户密钥是否完整；③ `notifyUrl` 须公网 HTTPS 可达（本地可用 ngrok）；④ 支付成功后弹窗会每 2 秒轮询 `GET /api/pay/order/{orderNo}`。

**Q：个人中心登录记录为空？**  
A：历史登录日志可能未写 `userId`，升级后重新登录即可；个人中心会同时按 `userId` 与 `username` 匹配历史记录。

**Q：忘记当前密码如何重置？**  
A：个人中心 → **安全设置** →「忘记密码」（须已绑定手机号且短信已启用）。发码前完成滑块验证，验证码发至绑定手机；重置成功后请用新密码登录。

**Q：如何绑定或更换个人中心手机号？**  
A：个人中心 → **基本资料**。须 **短信配置** 已启用；发码前完成滑块验证。已绑定号码显示脱敏 +「更换手机号」；`PUT /auth/profile` 不再直接修改手机号。

**Q：审批单详情里没有通过/驳回按钮？**  
A：仅 **待审批**（`SUBMITTED`）且有权时显示：注册审核需 `system:approval:approve`，其他类型须为指定审批人。详情抽屉底部与列表「处理」等效；若无按钮请确认角色权限后重新登录。

**Q：本地 Redis 报 ERR invalid password 或 AUTH 失败？**  
A：本地请用 **`dev` profile** 启动（`SPRING_PROFILES_ACTIVE=dev`），且本地 Redis 勿设密码；生产密码仅在 `application-prod.yml` 配置。

**Q：登录输错密码却提示「登录已过期」？**  
A：升级后账号/密码错误统一返回「账号或密码错误」；若仍为旧版，请更新前后端并重启后端。

**Q：分页请求 pageSize 很大导致接口变慢？**  
A：升级后 `PageParam` 自动将 `pageSize` 上限限制为 **200**；非法 `pageNo` 会修正为 1。

**Q：业务错误返回格式不统一？**  
A：统一走 [全局异常处理](#全局异常处理与错误契约)：Service 抛 `BusinessException(code, message)`，由 `GlobalExceptionHandler` 返回 `CommonResult` 且 HTTP 状态与 `code` 对齐；Controller 勿 `return CommonResult.error`、勿 broad catch 泄露 `e.getMessage()`。未登录/无权限由 Security JSON EntryPoint 返回相同结构。前端读 `res.code` 与 `res.message`（非 `msg`）。

**Q：接口 HTTP 200 但 body 里 code 是 403/404？**  
A：升级后 `BusinessException` 已通过 `ResponseEntity` 对齐 HTTP 状态；若仍出现，检查是否 Controller 直接 `return CommonResult.error` 或未抛异常的旧代码路径。

**Q：文件路径穿越或安全相关错误返回 400？**  
A：升级后 `SecurityException`（如 `LocalFileStorage` 非法路径）返回 **403** 并记 warn 日志含 IP。

**Q：部门负责人还是旧名字，或保存报 `leader_user_id` 不存在？**  
A：对已有库执行 **`sql/add15.sql`**（生产 **`add15_wuadmin.sql`**），重启后端；PC / 移动端组织编辑须通过**选择用户**设置负责人，勿手填姓名。

**Q：移动端 H5 刷新后无返回键，或返回跳到工作台？**  
A：① 升级含 **H5 浅栈导航** 的前端后 **Ctrl+F5** 强刷；② 列表页刷新后应出现页面内蓝色「‹ 返回」（`SubPageBackBar`），详情/编辑页为左上角浮动返回；③ 多级路径如「用户管理 → 详情 → 编辑 → F5」依赖 `localStorage` 父级映射，勿清理站点存储；④ 选择类子页 URL 带 `from` 参数；⑤ `FormCell` 已去除 H5 双事件导致的 `navigateTo` 取消。

**Q：移动端个人中心手机号如何更换？**  
A：与 PC 一致：基本资料展示脱敏号码，点击 **「更换手机号」** 进入 `mobile-bind` 独立页；发码前须完成滑块验证。

**Q：移动端账号状态显示「停用」但实际正常？**  
A：升级后状态码与 PC 对齐：`1=正常`、`0=已停用`、`2=待审核`、`3=审核驳回`。

**Q：Git 仓库？**  
A：https://github.com/wushij/wu-admin

---

## 相关文档

- [GitHub 上传与推送](./GitHub上传与推送全流程.md)
- [GlobalExceptionHandler 代码质量分析](./code-quality-analysis-global-exception-handler.md)

---

## 许可证

本项目仅供学习与内部使用。生产部署前请修改默认密码、数据库与 Redis 等敏感配置。
