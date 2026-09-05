<div align="center">

# 🛡️ Admin Platform · Wu-Admin

### 企业级后台 · RBAC 权限 · PC + 移动端一体

[![GitHub Repo](https://img.shields.io/badge/GitHub-wushij%2Fwu--admin-181717?style=flat-square&logo=github)](https://github.com/wushij/wu-admin)
[![Online Demo](https://img.shields.io/badge/Demo-wushij.com-0078D4?style=flat-square&logo=googlechrome)](https://wushij.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5+-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.4+-4FC08D?style=flat-square&logo=vuedotjs)](https://vuejs.org)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?style=flat-square&logo=mysql)](https://www.mysql.com)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?style=flat-square&logo=redis)](https://redis.io)

<p align="center">
  <a href="https://github.com/wushij"><b>🔗 GitHub 主页</b></a> &nbsp;•&nbsp;
  <a href="https://wushij.com"><b>🌐 在线演示</b></a> &nbsp;•&nbsp;
  <a href="https://app.wushij.com"><b>📱 移动端 H5</b></a> &nbsp;•&nbsp;
  <a href="#-快速开始"><b>🚀 快速开始</b></a> &nbsp;•&nbsp;
  <a href="#-系统架构"><b>🏗 系统架构</b></a> &nbsp;•&nbsp;
  <a href="#-目录结构"><b>📁 目录结构</b></a>
</p>

</div>

---

**Admin Platform（Wu-Admin）** 是一套基于 **Vue 3 + Spring Boot** 的企业级后台管理系统，配套 **uni-app 移动端（H5 / 微信小程序）**。覆盖 RBAC 权限、工单审批、企业 IM、AI 智能助手、系统监控大屏、代码生成、支付/短信集成等场景，已部署上线运行。

> 🔗 **在线体验**：PC 端 <https://wushij.com> · 移动端 H5 <https://app.wushij.com> · 体验账号 `lisi` / `lisi123`

---

## 目录

- [功能特性](#-功能特性)
- [技术栈](#-技术栈)
- [系统架构](#-系统架构)
- [快速开始](#-快速开始)
- [目录结构](#-目录结构)
- [配置说明](#-配置说明)
- [开发指南](#-开发指南)
- [测试与 CI](#-测试与-ci)
- [构建与部署](#-构建与部署)
- [常见问题](#-常见问题)
- [相关文档](#-相关文档)

---

## ✨ 功能特性

### 安全体系

- **双重认证鉴权**：Sa-Token 认证 + Spring Security 授权拦截，通过 `SaTokenAuthenticationFilter` 桥接至 SecurityContext
- **BCrypt 密码加密** + httpOnly Cookie 传 Token（防 XSS 窃取）
- **滑块验证码**：服务端生成 challenge + Redis 存储缺口位置，校验后一次性消费 token
- **登录锁定**：账号级 + IP 级失败计数，阈值可配置，管理员远程解锁
- **国密 API 安全防线**：全链路支持国密 SM4 接口传输加解密（CBC 模式 + 16 字节随机 IV 向量）与数字签名防篡改（HMAC-SM3 / SM2），整合 5 分钟滑动时间戳（`X-Timestamp`）、Redis 随机数防重放（`X-Nonce`）及 `Encrypt-then-Sign` 签名校验；前端与移动端具备 `sessionStorage` / `uni.setStorageSync` 会话密钥恢复能力，保证 F5 刷新及跨端无缝衔接。
- **安全防刷与防调试**：前端反调试禁用 Devtools（系统配置可一键开关）、Sa-Token 多端同时在线控制（`isConcurrent`），Nginx 层按路径分级限流（认证 5r/s、API 20r/s、文件 100r/s）+ 应用层注册/短信/邮箱防刷
- **邮件服务与防垃圾投递**：支持 SMTP 多服务商（QQ/163/Gmail/自定义）、RFC 2046 规范 `multipart/alternative` 双格式（Plain Text + HTML 卡片）、跨客户端兼容内联矢量 Header，实时持久化发信日志与投递回执（`sys_email_log`）
- **操作审计**：`@Log` 注解 + AOP 全接口记录（操作人/IP/归属地/参数/耗时），`@Async` 异步写入
- **异常统一处理**：`BusinessException` + `GlobalExceptionHandler`（17 类异常），401/403 统一 JSON 响应

### 组织与权限

- **五维组织架构**：用户 → 角色 → 菜单 → 部门 → 岗位，全关联管理
- **数据权限五级范围**：全部/自定义/本部门/本部门及以下/仅本人，通过 MyBatis-Plus 拦截器在 SQL 执行前动态拼接 data_scope 条件
- **RBAC 权限模型**：菜单按角色动态渲染侧栏，按钮级权限指令 `v-permission`/`v-role`
- **系统配置热更新**：11 组配置（下表），Redis 缓存 + 数据库双写，运行时即时生效

| 分组 | 名称 | 关键能力 |
|------|------|----------|
| `site` | 基础信息 | 平台名称/副标题/版权，登录页与工作台展示 |
| `session` | 会话配置 | `tokenExpireHours`（1～720），覆盖 Sa-Token 超时 |
| `file` | 文件配置 | `maxSizeMb`、`allowedExtensions`，上限不超过 500MB |
| `rateLimit` | 接口限流 | 验证码/登录每 IP 每分钟；短信/邮箱防刷（间隔/日上限） |
| `login` | 登录认证 | 验证码开关/类型、短信登录、邮箱登录、发码前滑块、记住我、重试锁定 |
| `register` | 注册认证 | 开放注册、验证码、默认角色、需审核、密码最小长度 |
| `thirdParty` | 第三方配置 | 微信/支付宝/GitHub/Google OAuth 密钥 |
| `payment` | 支付配置 | 微信 Native/支付宝当面付，支持**测试订单**与回调验签 |
| `sms` | 短信配置 | 阿里云/腾讯云双通道（策略模式），模板管理、测试发送与发送日志回执 (`sys_sms_log`) |
| `email` | 邮件配置 | QQ/163/Gmail/自定义 SMTP，发件人名称、SSL/TLS/STARTTLS、测试发送与投递日志回执 (`sys_email_log`) |
| `security` | 安全配置 | 前端反调试（禁用 Devtools）、Sa-Token 多端控制（`isConcurrent`）、国密 SM4 接口加密与 SM2/HMAC-SM3 签名防重放 |

- **字典管理**：字典类型 + 字典数据两级维护，Redis 缓存，前端封装 `DictSelect` / `DictTag` 全局组件

### 流程中心

- **工单系统**：创建 → 指派 → 处理 → 关闭全生命周期，支持优先级、截止时间、评论附件、全员通知
- **审批流引擎**：请假/采购/报销/用印/合同/通用 + `REGISTER` 注册审核，通过/驳回实时通知
- **注册审核**：注册 → 自动创建审批单 → 超管审批 → 激活账号，驳回自动软删

### 消息中心 & 企业 IM

- **业务消息**：工单/审批触达（`sys_notice`），顶栏铃铛「业务消息」Tab，点击跳转直达
- **系统通知**：管理员广播/定向发布（全员/按部门/指定用户），发送日志追踪
- **企业 IM**：私聊 + 群聊，文本/图片/文件/表情发送，历史分页（每页 50 条）
- **@ 提及**：输入 `@` 弹出成员列表，被 @ 用户 WebSocket 强提醒 `[有人@你]` 并计入角标
- **消息撤回**：2 分钟内可撤回，私聊/群聊均实时同步，无需刷新
- **群管理**：仅超管可建群；群主转让、成员禁言/移除/解散；群公告（持久化 + 置顶条 + 实时推送）
- **群免打扰**：普通消息不弹窗不计角标，**@ 我和群公告**始终提醒
- **在线状态**：WebSocket `presence` 推送，联系人列表上线/离线实时更新；PC 走 httpOnly Cookie，H5 URL 参数鉴权
- **正在输入…**：私聊输入时推送 typing 事件，顶栏实时显示
- **WebSocket 推送类型**：`notice` / `chat` / `groupChat` / `groupAnnouncement` / `typing` / `presence`

### AI wu 助手（AI 管理）

- **全局悬浮球助手**：PC 端登录后右下角悬浮球（`AiWuFloatBtn` + `AiWuChatPanel`）随时唤起对话，移动端提供同款 `AiWuAssistant` 组件，登录即可使用、无需菜单权限
- **SSE 流式对话**：`POST /api/ai/chat/stream` 基于 `SseEmitter` 增量推送（事件 `delta` / `done` / `error`），专用线程池不占用 Web 容器线程，客户端断开自动停止拉流
- **多供应商接入**：DeepSeek / OpenAI / Qwen（通义千问）/ Kimi（月之暗面），统一走 OpenAI 兼容协议 `/chat/completions`（策略模式 `AiProviderFactory`），仅 baseUrl 与模型名不同
- **AI 模型配置**：供应商/模型/baseUrl/温度等可视化管理，支持连通性测试与启用停用；**API Key 使用国密 SM4 加密存储**，回显仅掩码
- **AI 对话日志**：问答全量审计（问题/回答/Token 用量/耗时/状态/来源 pc·mobile），`@Async` 异步落库 `sys_ai_chat_log`，PC 与移动端均可查看明细
- **流式 Markdown 渲染**：markdown-it + highlight.js 代码高亮，`useStreamingMarkdown` 打字机式增量渲染
- **成本与安全防护**：单轮上下文最多 20 条、单条 4000 字截断，提问内容脱敏（`AiSanitizerUtil`），异常统一降级提示
- **独立顶级菜单「AI 管理」**：菜单 ID 210（`/ai`），下挂「AI 模型配置」（`/ai/model`）与「AI 对话日志」（`/ai/log`），权限标识 `system:ai-model:*` / `system:ai-log:*`

### 系统监控大屏

| 模块 | 后端 | 前端特性 |
|------|------|----------|
| **服务监控** | Actuator + JMX 采集 CPU/内存/JVM/磁盘 | ECharts 折线趋势图（sessionStorage 保留最近 20 个点，F5 不丢） |
| **缓存监控** | Redis INFO 统计 + SCAN 键 | 内存/QPS/命中率/连接数四宫格图表，键详情的查看与删除（黑名单保护） |
| **API 访问统计** | AOP 拦截器采集 → Redis 队列 → 10s 批量落库 | 统计卡片（请求总数/成功/失败）+ 日志列表 + Excel/CSV 导出 |
| **在线用户** | WebSocket + Sa-Token 会话追踪 | ip2region IP 归属地、浏览器/OS 解析，强制下线 |
| **定时任务** | Quartz 动态调度 | Cron 可视化配置、立即执行/暂停/恢复；内置 6 项清理任务（默认暂停） |

**采样策略**：超级管理员登录后全局后台轮询（缓存 3s、服务 5s）；普通用户进入监控页才采集，离页即停。请求防堆叠（上次未完成跳过本次）。

### 开发工具

- **代码生成器**：Velocity 模板引擎，导入表结构 → 配置字段 → 一键生成 Controller/Service/Mapper/Entity/Vue 页面完整代码。支持预览（标记新建/覆盖）、ZIP 下载、写入项目并创建菜单；`moduleName` 驱动 API 路径与权限前缀
- **Knife4j 接口文档**：Springdoc OpenAPI 3，Controller 完整 `@Tag`/`@Operation` 注解，生产环境自动关闭。`Knife4jIframeHeaderFilter` 处理 iframe 内嵌
- **列表导出**：EasyExcel 实现 Excel/CSV 双格式导出（`scope=filtered|all`），覆盖用户/日志/工单/审批/API 访问/在线用户，权限独立控制

### 文件与回收

- **文件管理**：MIME 类型校验 + 白名单后缀 + 大小限制，图片/PDF/Office 预览，分组管理
- **回收中心**：12 类软删数据统一汇总（用户/角色/菜单/部门/岗位/工单/审批/字典/通知/任务/文件），支持恢复与彻底删除，定时自动清理

### 跨端移动端

- **uni-app（Vue 3 + Pinia + TypeScript）**：H5 + 微信小程序双端覆盖，与 PC 共用 `/api`
- **H5 浅栈导航**：刷新后 `localStorage` 父级映射 + `SubPageBackBar` 返回条，列表页/详情页均可正常返回
- **移动端 IM**：☺/⌨ 表情键盘切换、H5 `visualViewport` 键盘高度适配、发送后保持键盘；「最近」表情本地记录
- **个人中心**：资料编辑、头像上传、短信验证绑定/更换手机号（发码前强制滑块）、自助改密、忘记密码
- **监控运维**：移动端同步支持服务/缓存/在线用户/定时任务监控、工单审批、代码生成
- **AI 助手同款体验**：`AiWuAssistant` 流式对话 + Markdown 渲染，工作台「AI 管理」分组直达模型配置与对话日志页面
- **API 地址自动解析**：`resolveApiBaseUrl()` 区分 H5 线上（同域 `/api`）与小程序（完整 HTTPS），避免局域网 IP 误打包

### 第三方集成

- **支付**：微信支付 APIv3（Native）+ 支付宝 SDK，含回调验签、测试订单
- **短信**：阿里云/腾讯云双通道（策略模式），短信验证码注册/登录/找回密码/绑定手机号，全量发送日志与状态 (`sys_sms_log`)
- **邮件**：QQ/163/Gmail/自定义 SMTP 协议，HTML 卡片验证码、RFC 2046 双格式防垃圾投递、发件日志与回执明细 (`sys_email_log`)
- **IP 归属地**：ip2region（`ip2region.xdb`），用于登录日志、在线用户解析

---

## 🏗 技术栈

**后端**
- Spring Boot **3.5.13** · Spring Security 6 · Sa-Token **1.39.0**
- MyBatis Plus 3.5.9 · Druid 1.2.24 · Redis + Redisson 3.41.0
- Quartz · Knife4j 4.5.0 + Springdoc 2.8.9
- Hutool / EasyExcel / ZXing / ip2region

**前端**
- Vue **3.4** + Composition API + TypeScript 5.3
- Pinia 2.1.7 · Element Plus 2.4.0 · Vite 5
- ECharts 5.6 · Three.js 0.184 · tsparticles（engine/slim 3.9 + vue3 3.0）
- markdown-it 14 + highlight.js 11（AI 对话流式 Markdown 渲染与代码高亮）

**移动端**：uni-app (Vue 3) + uv-ui 1.1.20 + luch-request 3.1.1 + markdown-it/highlight.js

**运行时**：JDK **17** · MySQL **8.0**（兼容 5.6.5+）· Redis **7.x** · Node.js 18+ · Maven 3.6+

**测试**：Vitest（前端）· JUnit 5 + Mockito（后端）——具体用例数随发版变化，以 `npm run test` / `mvn test` 实际跑出为准

---

## 🖥 系统架构

```
┌────────────────┐            ┌──────────────────────┐
│  浏览器 / H5    │   HTTPS    │       Nginx           │
│  wushij.com  │ ────────►  │ 反向代理 / 静态资源    │
│ app.wushij.com│           │ gzip / 缓存 / 限流     │
└────────────────┘            └──────────┬───────────┘
                                         │ /api
                                         ▼
                              ┌──────────────────┐
                              │   Spring Boot    │
                              │   :8080 (/api)   │
                              │  ┌─────────────┐ │
                              │  │ Filter Chain │ │
                              │  │ SaTokenAuth  │ │
                              │  │ SecurityCtx  │ │
                              │  │ Controller   │ │
                              │  │ ↓            │ │
                              │  │ ServiceImpl  │ │
                              │  │ ↓            │ │
                              │  │ Mapper       │ │
                              │  └─────────────┘ │
                              └──┬───────────┬───┘
                                 │           │
                                 ▼           ▼
                          ┌──────────┐ ┌──────────┐
                          │  MySQL   │ │  Redis   │
                          │ wu-admin │ │  db=1    │
                          └──────────┘ └──────────┘
```

| 维度 | 说明 |
|------|------|
| **分层约定** | `modules/*` → `framework` → `common`（`framework` 禁止引用 `modules`） |
| **安全链** | `AuthorizationQueryFilter`（Cookie/Header/URL Token 解析）→ `SaTokenAuthenticationFilter`（桥接 SecurityContext）→ `SecurityFilterChain`（@PreAuthorize）→ `GlobalExceptionHandler`（401/403 JSON） |
| **Nginx 限流** | 认证 5r/s+burst=10 / 通用 API 20r/s+burst=20 / 文件 100r/s+burst=200 |
| **性能要点** | Async 线程池 (8/32/500) · Druid max-active=50 · 在线心跳 45s 节流 · 工作台单 SQL 聚合 + Redis 2min 缓存 · 监控 in-flight 防堆叠 · API 日志 10s 批量落库 |
| **容量参考** | 2 核 2G Windows 单机 **30～60 人**同时在线；4 核 8G 约 80～150 人 |

---

## 🚀 快速开始

### 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| Node.js | 18+ | 前端 & uni-app |
| JDK | 17 | 后端 |
| Maven | 3.6+ | 后端构建 |
| MySQL | 8.0（生产兼容 5.6.5+） | 本地库名 `wu-admin`，生产 `wuadmin` |
| Redis | 7.x | database 1 |

### 1. 初始化数据库

```bash
# 空库全新安装（本地 MySQL 8.0 · dev）
mysql -u root -p < sql/admin_platform_dev.sql

# 生产空库全新安装（MySQL 8.0 · prod，库名 wuadmin）
mysql -u wuadmin -p wuadmin < sql/admin_platform_prod.sql

# 生产空库（MySQL 5.6+，库名 wuadmin，legacy）
mysql -u wuadmin -p wuadmin < sql/admin_platform_mysql56.sql

# 已有库升级：极旧库执行 admin_platform_prod.sql / admin_platform_dev.sql 文末附录段（约 990 行起）
# 后续发版增量按版本依次：
#   mysql -u root -p wu-admin < sql/migration/add1.sql    # 在线用户查询权限（monitor:online:query）
#   mysql -u root -p wu-admin < sql/migration/add2.sql    # 回收中心 query/restore/delete 权限
#   mysql -u root -p wu-admin < sql/migration/add3_api_security.sql     # 国密 API 安全配置
#   mysql -u root -p wu-admin < sql/migration/add4_email_config.sql     # 邮件配置与发信日志
#   mysql -u root -p wu-admin < sql/migration/add5_ai_wu_assistant.sql  # AI wu助手（模型配置/对话日志/AI 管理菜单）
```

> 切勿对已有表的生产库跑 `admin_platform_prod.sql` 全文（含 DROP，默认熔断拦截）。旧库升级用附录或 `sql/migration/` 下增量脚本。

### 2. 启动 Redis

`127.0.0.1:6379`，database **1**。本地 dev 无密码；生产见 `application-prod.yml`。

### 3. 启动后端（8080）

```powershell
# 本地开发（dev profile，库 wu-admin，无密 Redis）
cd backend
$env:SPRING_PROFILES_ACTIVE='dev'
mvn spring-boot:run -DskipTests

# 生产（prod，库 wuadmin，需 Redis 密码）
mvn spring-boot:run -DskipTests
# 或打包：mvn clean package -DskipTests → java -jar backend.jar
```

### 4. 启动 PC 前端（3000）

```powershell
cd frontend
npm install
npm run dev
```

访问：**http://localhost:3000**

### 5. 启动移动端 H5（5174，可选）

```powershell
cd uniapp
npm install
npm run dev:h5
```

浏览器访问终端提示的本地地址（通常 **http://localhost:5174**）。

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | 超级管理员 |
| `zhangsan` | `admin123` | 普通用户 |

### 开发与连接补充

| 项 | 说明 |
|----|------|
| **本地 MySQL** | 默认 `root` / `root`，库名 `wu-admin`（`application-dev.yml`） |
| **ip2region.xdb** | IP 归属地库，10MB；放 `backend/src/main/resources/ip2region/`，仓库 git 忽略；缺失时公网 IP 解析显示「未知」 |
| **API 文档（开发）** | <http://localhost:3000/doc.html>（Vite 代理到后端 8080） |
| **API 文档（生产）** | `https://域名/doc.html`（Nginx 反代 + Knife4jIframeHeaderFilter） |
| **WebSocket** | `wss://域名/api/ws/message`（PC 走 httpOnly Cookie，H5/小程序 URL 带 `?Authorization=<token>`） |
| **小程序前置** | 微信开发者工具 + 合法 AppID；`build:mp-weixin` + 配置 `VITE_API_PRODUCTION_ORIGIN`（详见 [配置说明](#-配置说明)） |
| **CI 范围** | 仓库 `.github/workflows/ci.yml` 目前跑 `mvn test` + `npm run typecheck/test`，**未纳入 ESLint**（存量 `vue/no-mutating-props` 警告待清理） |
| **列表导出 scope** | `scope=filtered`（当前筛选） / `scope=all`（权限范围内全量，受 `PageParam` 200 上限约束）；`scope=page` 即当前页 |

---

## 📁 目录结构

```
wu-admin/
├── .github/workflows/ci.yml       # GitHub Actions CI（mvn test + npm typecheck/test）
├── backend/                       # Spring Boot 后端
│   ├── pom.xml
│   ├── data/                      # 开发态上传目录（git 忽略）
│   └── src/
│       ├── main/java/com/admin/server/
│       │   ├── RbacServerApplication.java
│       │   ├── common/            # 通用层：CommonResult、BusinessException、工具类
│       │   ├── framework/         # 框架层：安全、MyBatis、Redis、Filter、WebSocket
│       │   └── modules/           # 业务模块（系统、基础设施、支付、工单审批、即时通讯、AI 等）
│       │       ├── system/        # 系统管理与核心认证
│       │       ├── infra/         # 基础设施（文件、生成、任务监控、日志、回收站、导出）
│       │       ├── trade/         # 支付、短信、邮件三方集成
│       │       ├── ticket/        # 工单与审批流管理
│       │       ├── message/       # 站内消息与即时聊天 IM
│       │       └── ai/            # AI wu助手（流式对话、模型配置、对话日志、供应商策略）
│       ├── main/resources/
│       │   ├── application.yml / application-dev.yml / application-prod.yml
│       │   ├── ip2region/         # IP 归属地 xdb 库（git 忽略）
│       │   └── templates/         # Velocity 代码生成模板
│       └── test/java/             # JUnit 5 单元测试
├── frontend/                      # Vue 3 + TypeScript PC 前端
│   ├── public/                    # 静态资源（favicon、登录背景等）
│   ├── tests/                     # Vitest 单元测试
│   ├── env.d.ts / eslint.config.js / index.html / vitest.config.ts
│   ├── package.json / package-lock.json / tsconfig.json / tsconfig.node.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/                   # API 接口层（按模块分目录）
│       ├── views/                 # 页面视图（login/dashboard/system/ai/monitor/tool/message）
│       ├── components/            # 全局组件（DictSelect、SliderCaptcha、EmojiPicker、AiWu 悬浮球助手等）
│       ├── composables/           # 组合式函数（useDict、useMonitorBackground、useStreamingMarkdown 等）
│       ├── store/                 # Pinia（user/message/site/tagsView/aiWu）
│       ├── router/                # 路由与守卫
│       ├── utils/                 # request、主题、菜单树、WebSocket 工具
│       ├── directives/            # v-permission / v-role 指令
│       ├── constants/             # 全局常量（权限码、字典 key 等）
│       ├── styles/                # 全局样式与主题变量
│       ├── types/                 # TypeScript 类型定义
│       ├── App.vue / main.ts
├── scripts/                       # 生产环境部署 Shell 脚本（deploy-backend.sh / deploy-frontend.sh）
├── uniapp/                        # uni-app 移动端（H5 / 微信小程序）
│   ├── .env / .env.example / .env.production
│   ├── index.html / package.json / package-lock.json / tsconfig.json / shims-uni.d.ts
│   ├── vite.config.ts
│   └── src/
│       ├── manifest.json / pages.json / uni.scss / env.d.ts
│       ├── api/                   # 移动端 API 接口层
│       ├── pages/                 # 主包页面：首页、工作台、消息、我的
│       ├── pages-sub/             # 子包：系统管理、监控、IM、AI 管理、个人中心
│       ├── components/            # 通用与业务组件
│       ├── composables/           # useH5ListPageNav、useChatKeyboardInset 等
│       ├── constants/             # 常量定义（TabBar 配置、图标映射等）
│       ├── store/                 # Pinia stores
│       ├── utils/                 # api-base、nav-history、webSocket、security-config 等
│       ├── config/                # 路由/请求等配置
│       ├── plugins/               # uView/luch-request 等插件接入
│       ├── custom-tab-bar/        # 自定义底部导航
│       ├── styles/                # 全局样式
│       ├── types/                 # TypeScript 类型定义
│       ├── static/                # 静态资源（图片、图标等）
│       ├── App.vue / main.ts
├── sql/
│   ├── admin_platform_prod.sql    # 生产全量脚本（wuadmin，MySQL 8）+ 附录
│   ├── admin_platform_dev.sql     # 本地全量脚本（wu-admin，MySQL 8）+ 附录
│   ├── admin_platform_mysql56.sql # 生产空库全量（wuadmin，MySQL 5.6，legacy）
│   └── migration/                 # 版本增量与迁移 SQL 脚本（dev/ prod/）
└── data/                          # 本地上传目录（git 忽略）
```

### 后端包分层约定

```
com.admin.server/
├── common/           # 通用层（0 业务依赖）
│   ├── core/         # CommonResult、PageParam、PageResult
│   ├── exception/    # BusinessException、ErrorCode
│   └── util/         # ClientIpUtils、IpLocationUtils、UserAgentUtils、BeanMappingUtils 等
├── framework/        # 技术基础设施（可抽公共 starter）
│   ├── security/     # SecurityConfig、TokenService、SaTokenAuthenticationFilter、JsonEntryPoint
│   ├── web/          # GlobalExceptionHandler（17 类异常）、AuthorizationQueryFilter
│   ├── config/       # AsyncConfig、SaTokenCookieConfig（httpOnly）、DevRedissonConfig
│   ├── storage/      # 本地文件存储（含路径穿越校验）
│   ├── quartz/       # Quartz 任务调度
│   ├── mybatis/      # MyBatisPlusConfig、MyMetaObjectHandler、BaseDO 实体基类
│   ├── redis/        # RedisTemplate 序列化配置
│   ├── websocket/    # WebSocket 消息推送
│   ├── log/          # @Log 操作日志注解
│   ├── openapi/      # Knife4j/Springdoc OpenAPI 配置
│   └── export/       # Excel/CSV 导出工具
└── modules/          # 业务模块（高内聚低耦合拆分）
    ├── system/       # 用户、角色、菜单、部门、岗位、字典、系统配置、通知、权限、登录日志、仪表盘
    ├── infra/        # 文件存储、代码生成、定时任务、监控运维、操作日志、回收站、通用导出、系统任务调度
    ├── trade/        # 微信/支付宝支付接入、短信平台通道、发信通道、三方渠道对接
    ├── ticket/       # 工单与审批流（工单管理、流程审批表单、用户注册审核流）
    ├── message/      # 消息触达（站内公告推送、IM 聊天、消息撤回、@提及提醒、群聊管理）
    └── ai/           # AI wu助手（SSE 流式对话、模型配置与连通测试、对话日志审计、SM4 密钥加密）
```

---

## 🔧 配置说明

### 数据库设计规范

所有表遵循统一范式：

| 维度 | 规范 |
|------|------|
| 主键 | `BIGINT AUTO_INCREMENT PRIMARY KEY` |
| 审计字段 | `create_time` / `update_time` / `creator` / `updater`（MyBatis Plus 自动填充） |
| 逻辑删除 | `deleted TINYINT DEFAULT 0`，MyBatis Plus `logic-delete-value: 1` |
| 字符集 | `utf8mb4_unicode_ci`（支持 emoji） |
| 字段注释 | 所有字段含 `COMMENT` |

### 数据库脚本管理

| 脚本 | 用途 | 目标库 |
|------|------|--------|
| `admin_platform_prod.sql` | 生产全量（Part A 建表 + Part B 初始数据 + 附录补丁） | `wuadmin`（MySQL 8） |
| `admin_platform_dev.sql` | 本地全量（Part A 建表 + Part B 初始数据 + 附录补丁） | `wu-admin`（MySQL 8） |
| `admin_platform_mysql56.sql` | 生产空库全量（`VARCHAR(191)` 等 5.6 适配） | `wuadmin`（MySQL 5.6+） |
| `addN.sql` / `addN_wuadmin.sql` | 发版增量补丁 | 本地/生产 |
| `disable_devtool_off.sql` | 临时关闭前端反调试 | — |

> 附录/增量可重复执行、尽量非破坏性：`INSERT IGNORE` 不覆盖已有菜单/任务/用户权限。

### 后端 YAML 配置要点

| 配置项 | 说明 |
|--------|------|
| `server.port` / `context-path` | `8080` / `/api` |
| `spring.profiles.active` | 默认 `prod`，本地开发 `-Dspring.profiles.active=dev` |
| `spring.datasource.*` | prod 库 `wuadmin`，dev 库 `wu-admin` |
| `spring.redis.database` | `1`；密码仅写在 `application-prod.yml` |
| `sa-token.timeout` | 缺省 86400s，运行时由系统配置「会话配置」覆盖 |
| `file.storage.local-path` | dev: `./data/uploads`；**prod: `/www/server/wuadmin/data/uploads`**（与 Nginx alias 一致，自动创建） |
| `knife4j.enable` | 建议 `false`（4.5.0 + springdoc 2.8 兼容性） |
| `springdoc.api-docs.path` | `/v3/api-docs`（生产 `application-prod.yml` 关闭） |

**运行时配置优先读库**：`sys_config_group` 表 → `SystemConfigHelper` → `DynamicConfigProvider`。YAML 为缺省兜底。

### 前端

- 开发：`vite.config.ts` 中 `/api` 代理到 `localhost:8080`
- 生产：`npm run build` 后 `dist/` 部署到 Nginx，`/api` 反代到后端 8080

### 移动端 uni-app 环境变量

| 文件 | 场景 | `VITE_API_BASE_URL` |
|------|------|---------------------|
| `.env` | 本地开发 | `http://127.0.0.1:8080/api` |
| `.env.production` | H5 生产打包 | **`/api`**（同域 Nginx 反代） |
| `.env.production` | 小程序生产 | 另设 `VITE_API_PRODUCTION_ORIGIN=https://app.wushij.com` |

> **切勿**用含局域网 IP 的 `.env` 直接 `build:h5` 上传服务器，会被 Vite 编译进 JS。

### API 前缀总览

| 前缀 | 说明 | 认证 |
|------|------|:--:|
| `/api/auth/**` | 登录/注册/验证码/config/profile（公开 `config` 无需登录） | 🌐 匿名 |
| `/api/system/**` | 用户/角色/菜单/组织/字典/配置/工单/审批/消息 | 🔒 登录+权限 |
| `/api/ai/chat/**` | AI wu助手流式对话（SSE）与启用模型列表 | 🔒 登录 |
| `/api/system/ai-model/**` `/api/system/ai-log/**` | AI 模型配置、AI 对话日志管理 | 🔒 登录+权限 |
| `/api/monitor/**` | API 访问/在线用户/定时任务/缓存/服务监控 | 🔒 登录+权限 |
| `/api/pay/notify/**` | 微信/支付宝回调 | 🌐 白名单 |
| `/api/files/**` | 文件上传与访问（GET 走 Nginx alias 直出） | 🌐 GET 公开 |
| `/api/tool/gen/**` | 代码生成器 | 🔒 登录+权限 |
| `/api/dashboard/**` | 工作台统计 | 🔒 登录 |
| `/api/excel/**` | 列表 Excel/CSV 导出 | 🔒 登录+权限 |

---

## 📖 开发指南

### 后端：Controller → Service → Mapper

- **Controller** 只做参数校验（`@Validated` + `@RequestBody`）和权限注解（`@PreAuthorize`），委托 Service
- **Service** 写业务逻辑，错误抛 `BusinessException(code, message)`（如 `throw new BusinessException(404, "工单不存在")`）
- **禁止** Controller 直接 `return CommonResult.error()`，禁止 broad `catch (Exception)` 泄露堆栈
- 新增接口：在 `modules/system/service/` 写实现 → `api/` 写 Controller 暴露

### 前端：组件化约定

路由入口 `index.vue`（薄包装）→ `*Page.vue`（页面骨架）+ `composables/use*Page.ts`（状态与业务）+ `components/`（展示子组件）。

| 模块 | 路由 | 主要文件 |
|------|------|----------|
| 工作台 | `/dashboard` | `WelcomeBanner`、`CoreStatsRow` + `useDashboardData` |
| 系统管理 | `/system/user\|dict\|role\|menu\|...` | 各 `*Page.vue` + `use*Page.ts`，统一 `module-page` 样式 |
| 企业IM | `/message/chat` | `ChatPage.vue` + `useChatPage` / `useChatRender` / `useMention` |
| 监控 | `/monitor/cache\|server\|job\|...` | `CacheMonitorPage` / `ServerMonitorPage` 等 |

### 字典组件

```html
<DictSelect v-model="form.status" dict-type="sys_normal_disable" value-type="number" />
<DictTag :value="row.status" dict-type="sys_normal_disable" />
```

字典管理页 → 修改数据 → 点「刷新缓存」即时生效。脚本方式：`useDict('sys_user_sex')` + `onMounted(() => load())`。

### 按钮权限

```html
<el-button v-permission="'system:config:update'">保存</el-button>
<el-button v-role="'super_admin'">仅超管可见</el-button>
```

标识与 `sys_menu.permission` 一致，登录后 `store/user.ts` 自动加载所有权限标识。

### 操作日志

Controller 方法上加 `@Log`（`framework.log.annotation`），AOP 自动记录操作人/IP/归属地/请求参数/响应结果/耗时，`@Async` 异步写入 `sys_oper_log`。

### 全局异常处理

前后端统一约定：*Service 抛 `BusinessException`，由 `GlobalExceptionHandler` 转为 `CommonResult`，HTTP 状态码与 body.`code` 对齐*。

**处理链路**：
```
Filter（Sa-Token）→ Security（未登录/无权限→JSON EntryPoint）
  → Controller → Service 抛 BusinessException
  → GlobalExceptionHandler（17 类异常）→ BusinessHttpStatusMapper → JSON 响应
  → 前端 request.ts 拦截器（401/403/429）
```

**核心规范**：
```java
// ✅ Service 层 — 推荐
throw new BusinessException(404, "工单不存在");
throw new BusinessException(403, "仅审批人可操作");
throw new BusinessException("用户名已存在");  // 默认 code=400

// ❌ Controller 层 — 禁止
return CommonResult.error(404, "xxx");         // HTTP 200 与 body.code 不一致
catch (Exception e) { return CommonResult.error(500, e.getMessage()); }  // 泄露内部信息
```

**已覆盖异常**（17 个）：`BusinessException`（动态码）、校验失败（JSR-303/参数/JSON）、`AccessDeniedException`（403）、`NotLoginException`（401）、`NoHandlerFoundException`（404）、`MaxUploadSizeExceededException`（400）、`Exception` 兜底（500，不泄露堆栈）等。

### 新增页面开发约定

1. `router/` 注册路由，`path` 与 `sys_menu.path` 一致
2. 页面放 `views/<模块>/`，入口 `index.vue`，业务放 `*Page.vue` + `composables/`
3. 提交前执行 `npm run typecheck` 与 `npm run test`（后端 `mvn test`）
4. 涉及菜单变更需执行 SQL 增量（`addN.sql`）并**重新登录**
5. 侧栏菜单由 `sys_menu` 按角色动态渲染，超级管理员默认全部

---

## 🧪 测试与 CI

### 运行测试

```powershell
# 前端（Vitest + happy-dom）
cd frontend && npm run test

# 后端（JUnit 5 + Mockito）
cd backend && mvn test

# 指定测试类
cd backend && mvn test -Dtest=AuthServiceImplTest
```

### 测试覆盖概要

| 层 | 前端 | 后端 |
|----|------|------|
| 核心 | 登录态、消息推送、页签缓存、权限判断 | 认证（登录/注册/限流）、权限匹配、CRUD |
| 工具 | 拦截器、菜单树、组织树、聊天文案、防抖 | 异常处理（17 类）、HTTP 状态映射 |
| 业务 | — | 工单流转、审批、IM（私聊/群聊/撤回）、字典缓存 |

### GitHub Actions CI

推送到 `main`/`master`/`dev` 或开 PR 时自动执行：

| Job | 环境 | 步骤 |
|-----|------|------|
| Backend (Maven) | Ubuntu, JDK 17 | `mvn -B test` |
| Frontend (Node) | Ubuntu, Node 20 | `npm run typecheck` → `npm run test` |

> 并发控制：同一分支有新提交时旧 run 自动取消。配置见 `.github/workflows/ci.yml`。

---

## 📦 构建与部署

### 构建命令

```powershell
# 前端（生产）→ frontend/dist/
cd frontend && npm run build

# 移动端 H5 → uniapp/dist/build/h5/（读 .env.production）
cd uniapp && npm run build:h5

# 微信小程序 → uniapp/dist/build/mp-weixin/
cd uniapp && npm run build:mp-weixin

# 后端 → backend/target/backend.jar
cd backend && mvn clean package -DskipTests
```

### 产物说明

| 产物 | 路径 | 部署方式 |
|------|------|----------|
| PC 前端 | `frontend/dist/` | 上传至 `wushij.com` 根目录 |
| 移动端 H5 | `uniapp/dist/build/h5/` | 上传至 `app.wushij.com` 根目录 |
| 后端 jar | `backend/target/backend.jar` | 上传至 `/www/server/wuadmin/backend.jar` 并重启 |

### 上传目录（Linux 生产）

后端、Nginx **必须共用同一路径** `/www/server/wuadmin/data/uploads/`（与 jar 同目录，**后端会自动创建**，无需手动 mkdir）：

| 组件 | 路径 |
|------|------|
| `application-prod.yml` | `/www/server/wuadmin/data/uploads` |
| Nginx `alias`（两站点） | `/www/server/wuadmin/data/uploads/` |

### 生产默认连接

| 项 | 值 | 说明 |
|----|-----|------|
| MySQL 库/用户/密码 | `wuadmin` / `wuadmin` / `root` | `application-prod.yml`，部署前修改 |
| Redis 密码 | `root`（db=1） | 同上 |
| CORS | 须改为实际域名 | 示例：`https://wushij.com,https://www.wushij.com,https://app.wushij.com` |

### Nginx 核心配置（最小可用版）

```nginx
# 静态资源 + SPA 回退
location / {
    try_files $uri $uri/ @spa;
}
location @spa {
    rewrite ^ /index.html break;
}

# 上传文件 Nginx 直出（须在 /api/ 反代之前；与 application-prod.yml local-path 一致）
location ^~ /api/files/ {
    alias /www/server/wuadmin/data/uploads/;
    expires 7d;
    add_header Cache-Control "public, max-age=604800";
}

# API 反向代理（含 WebSocket）
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

# 静态资源长缓存（index.html 不缓存）
location = /index.html {
    add_header Cache-Control "no-cache, no-store, must-revalidate";
}
location ~* \.(js|css|woff2?|ttf|png|jpg|gif|svg|ico)$ {
    expires 7d;
    add_header Cache-Control "public, immutable";
}
```

完整限流/SSL/双站点配置模板见 `docs/wushij.com域名配置文件-linux.txt`、`docs/app.wushij.com域名配置文件-linux.txt`、`docs/nginx配置文件.txt`。

### 部署 checklist

1. 创建上传目录并赋权（见上）→ 部署新 jar 并重启
2. `mysql -u wuadmin -p wuadmin < sql/admin_platform_prod.sql` → 初始化空库
3. `mvn clean package -DskipTests` → 上传 jar → **重启**
4. `npm run build` → 上传 `dist/` → 强刷
5. `npm run build:h5`（读 `.env.production`）→ 上传 H5
6. Nginx 粘贴模板 → `nginx -t` → 重载
7. 自测：`GET https://域名/api/auth/config` 返回 JSON；上传头像后 `GET /api/files/...` 为 200

> 仅改 Nginx → 重载即可；改 Java/YAML → 重打 jar 重启；仅改前端 → 覆盖 dist/H5 强刷。

---

## ❓ 常见问题

### 部署与登录

**Q：生产登录失败，接口返回 HTML？**
A：访问 `https://域名/api/auth/config`，若返回 `index.html` 则是 **Nginx 未反代 `/api`**（请求落入 `try_files`）。配置 `location ^~ /api/ { proxy_pass http://127.0.0.1:8080/api/; ... }`。若返回 JSON 正常，检查 `application-prod.yml` CORS 域名。

**Q：登录输错密码提示「登录已过期」？**
A：已修复为统一返回「账号或密码错误」。若仍为旧版，更新前后端并重启。

**Q：本地 Redis 报 AUTH 失败？**
A：本地用 `dev` profile（`SPRING_PROFILES_ACTIVE=dev`），本地 Redis 勿设密码。生产密码仅写在 `application-prod.yml`。

**Q：头像/文件上传成功但访问 404？**
A：后端 `local-path` 与 Nginx `alias` 路径不一致。Linux 生产统一用 `/www/server/wuadmin/data/uploads/`，改 Nginx 两处 alias 后 reload，部署新 jar 重启。

**Q：H5 部署后请求局域网 IP 或 SSL 错误？**
A：生产包误用 `.env` 局域网 IP 打包。用 `.env.production`（`VITE_API_BASE_URL=/api`）重新 `build:h5` 上传并强刷。

**Q：H5 刷新后无返回键？**
A：升级含浅栈导航的前端后强刷。依赖 `localStorage` 父级映射 + `SubPageBackBar` 兜底。

### 菜单与权限

**Q：登录后菜单为空或 403？**
A：确认已导入 `admin_platform_prod.sql` / `admin_platform_dev.sql` 或为角色分配菜单，**重新登录**。修改菜单/角色后也需重新登录。

**Q：缓存/服务监控菜单不显示？**
A：极旧库执行附录 → 重启 → 重新登录。角色需分配 `monitor:cache:list`（菜单）和 `monitor:cache:query`（查询权限）。

**Q：消息中心菜单不显示或聊天 403？**
A：极旧库跑附录 → 重启 → 重新登录。普通用户需 `system:chat:list` 权限。

**Q：回收中心菜单不显示或文件列表报 `Unknown column 'deleted'`？**
A：极旧库执行附录 → **重新登录**。全量脚本已含菜单 163 和文件软删字段。

**Q：回收中心普通用户查不到数据？**
A：执行 `add2.sql` → 勾选「回收中心查询」（`system:recycle:query`）→ 重启 → 重新登录。

### 注册与审核

**Q：注册后无法登录？**
A：若开启「注册需审核」→ 超管在审批单中心通过后才可登录。驳回后账号软删，需重新注册。

**Q：驳回后同用户名无法注册？**
A：升级后软删用户名可自动恢复注册。若回收中心仍有该用户，先彻底删除。

### 企业 IM

**Q：联系人「在线/离线」不实时变？**
A：需 WebSocket 连接正常。H5 需 WS URL 带 Token + 切前台重连。后端日志应有 `WS connected userId=...`。

**Q：群聊撤回后对方要刷新才看到？**
A：升级至含撤回 WS 同步的版本并重启，2 分钟内撤回实时生效。

**Q：群免打扰仍收到所有消息提醒？**
A：确认 `notify_muted` 字段已入库（全量脚本已含）；仅 @ 我和群公告会提醒。

**Q：聊天图片出现在文件管理里？**
A：新图片走 `images/chat/`、文件走 `files/chat/`，列表已排除；历史旧数据可手动删除。

### 系统配置与功能

**Q：系统配置页报错或缺少支付/短信/邮件 Tab？**
A：极旧库跑附录 → 后续 `addN.sql` 补跑 → **重启后端**。

**Q：登录页没有「短信登录」或「邮箱登录」？**
A：系统配置 → 登录认证 开启「短信验证码登录」/「邮箱验证码登录」+ 对应短信/邮件配置启用。仅支持已绑定手机/邮箱账号。

**Q：短信/邮件验证码发送失败或被限流？**
A：检查接口限流分组（每 IP 每分钟、同号/单箱间隔、日上限）。阿里云需配置认证方案；邮件需要正确的 SMTP 主机、端口（465 SSL/587 TLS）及授权码。

**Q：邮件验证码容易进垃圾箱或收不到？**
A：系统已遵循 RFC 2046 规范生成 `multipart/alternative` 双格式正文（HTML + Plain text）与高优先级响应头，采用跨客户端兼容 HTML 矢量 Logo；如果使用个人 QQ/163 邮箱 SMTP，建议在【邮件配置】中设置【发件人显示名称】为平台名，并在接收端垃圾箱点击一次「这不是垃圾邮件」直通收件箱。

**Q：如何查看邮件/短信的发送历史与报错明细？**
A：系统配置 -> 短信配置/邮件配置 右侧均配有【发送记录】表格与【查看全部】日志弹窗，实时记录接收账号、验证码摘要、状态与异常明细（分别由 `sys_sms_log` 与 `sys_email_log` 自动持久化）。

**Q：忘记当前密码如何重置？**
A：个人中心 → 安全设置 →「忘记密码」，须已绑定手机号/邮箱且对应服务已启用，发码前完成滑块验证。

**Q：开启「禁止前端调试」后 F12 打不开？**
A：系统配置 → 安全配置 中可实时开启/关闭「禁止前端调试 (disableDevtool)」。若因误操作锁定，可执行 `sql/disable_devtool_off.sql` → 重启后端 → 强刷页面恢复。

**Q：系统配置里的「安全配置」还支持哪些控制？**
A：除了前端反调试之外，还支持 Sa-Token 账号多端同时在线/互踢控制（`isConcurrent`），可按需开启账号单端强踢或多端同时登录。

**Q：上传失败提示大小或类型？**
A：系统配置 → 文件存储 调整限制，上限不超过 500MB。聊天文件共享文件配置约束。

### AI wu 助手

**Q：「AI 管理」菜单不显示或悬浮球不出现？**
A：旧库需执行 `sql/migration/add5_ai_wu_assistant.sql`（建表 `sys_ai_model` / `sys_ai_chat_log` + 菜单）→ 重启后端 → **重新登录**。悬浮球对所有登录用户可见；若面板提示「暂无可用模型」，需管理员在 AI 模型配置中启用至少一个模型。

**Q：AI 对话提示「AI 服务暂时不可用」？**
A：到 AI 管理 → AI 模型配置 点「测试」验证连通性：检查 API Key 是否有效、baseUrl 是否可达（服务器需能访问供应商 API）、账户余额是否充足。具体报错可在「AI 对话日志」失败记录的错误明细中查看。

**Q：支持哪些大模型供应商？**
A：DeepSeek / OpenAI / Qwen（通义千问）/ Kimi（月之暗面），均通过 OpenAI 兼容协议接入，不支持 Coze 等智能体平台。其他兼容 `/chat/completions` 协议的服务可尝试以上述供应商类型 + 自定义 baseUrl 接入。

**Q：API Key 安全吗？**
A：入库前经国密 SM4 加密，列表/详情接口仅返回掩码；对话日志中的提问内容会先脱敏再落库。

### 监控与性能

**Q：服务监控「平均负载」显示 `-`？**
A：Windows 不提供 Linux load average，正常现象。参考 CPU/进程 CPU 和折线图。

**Q：API 访问统计页报 500？**
A：旧版统计 SQL 误用 `deleted = 0`（表无该字段），升级后端 jar 重启即可。

**Q：监控折线图切页或 F5 清空？**
A：已改为 sessionStorage 持久化（F5 保留 20 点）。管理员全局轮询；普通用户进页采、离页停。

**Q：分页 pageSize 很大导致接口慢？**
A：`PageParam` 已限制上限 200，非法 pageNo 修正为 1。

**Q：上传成功但文件网格图片慢？**
A：网格拉原图 URL（非缩略图），大 PNG 下载慢。确认 Nginx `location ^~ /api/files/` alias 指向正确。

### 代码生成与数据库

**Q：代码生成菜单不显示或导入报「表已导入」？**
A：极旧库执行附录 → 重新登录。MySQL 5.6 生产用 `admin_platform_mysql56.sql`。

**Q：如何升级已有库？**
A：极旧库执行 `admin_platform_prod.sql` / `admin_platform_dev.sql` 文末附录（~990 行）；后续按 `sql/migration/` 序号补跑。**切勿**对已有表跑全文。

**Q：字典多了好几个「××（副本）」？**
A：误点「复制类型」所致，删除多余副本并刷新缓存。

**Q：Knife4j 调试 404 或 iframe 空白？**
A：开发环境重启 Vite；生产需 `Knife4jIframeHeaderFilter`。`knife4j.enable` 保持 `false`。

---

## 📄 相关文档

| 类别 | 文档 |
|------|------|
| 🚀 部署配置 | `docs/wushij.com域名配置文件-linux.txt` · `docs/app.wushij.com域名配置文件-linux.txt` · `docs/nginx配置文件.txt` |
| 📋 项目分析 | `docs/项目分析.txt` · `docs/项目审查报告.md` |
| 🔒 安全审计 | `docs/安全防护与限流专项审计.txt` |
| 🐙 GitHub | <https://github.com/wushij/wu-admin> |

---

## 📜 许可证

本项目仅供学习与内部使用。生产部署前请修改默认密码、数据库与 Redis 等敏感配置。
