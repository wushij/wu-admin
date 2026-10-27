<div align="center">

# 🛡️ Admin Platform · Wu-Admin

### 企业级后台 · RBAC 权限 · PC + 移动端一体

[![GitHub Repo](https://img.shields.io/badge/GitHub-wushij%2Fwu--admin-181717?style=flat-square&logo=github)](https://github.com/wushij/wu-admin)
[![Gitee Repo](https://img.shields.io/badge/Gitee-wusj17%2Fwu--admin-C71D23?style=flat-square&logo=gitee)](https://gitee.com/wusj17/wu-admin)
[![Online Demo](https://img.shields.io/badge/Demo-wushij.com-0078D4?style=flat-square&logo=googlechrome)](https://wushij.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5+-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.4+-4FC08D?style=flat-square&logo=vuedotjs)](https://vuejs.org)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?style=flat-square&logo=mysql)](https://www.mysql.com)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?style=flat-square&logo=redis)](https://redis.io)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](LICENSE)

<p align="center">
  <a href="https://github.com/wushij/wu-admin"><b>🔗 GitHub</b></a> &nbsp;•&nbsp;
  <a href="https://gitee.com/wusj17/wu-admin"><b>🔴 Gitee</b></a> &nbsp;•&nbsp;
  <a href="https://wushij.com"><b>🌐 在线演示</b></a> &nbsp;•&nbsp;
  <a href="https://app.wushij.com"><b>📱 移动端 H5</b></a> &nbsp;•&nbsp;
  <a href="#-快速开始"><b>🚀 快速开始</b></a> &nbsp;•&nbsp;
  <a href="#-系统架构"><b>🏗 系统架构</b></a> &nbsp;•&nbsp;
  <a href="#-目录结构"><b>📁 目录结构</b></a>
</p>

</div>

---

**Admin Platform（Wu-Admin）** 是一套基于 **Vue 3 + Spring Boot** 的企业级后台管理系统，配套 **uni-app 移动端（H5 / 微信小程序）**。覆盖组织（部门）数据隔离、RBAC 权限、工单审批、企业 IM、AI 智能助手与知识库、系统监控大屏、多引擎文件存储、代码生成、支付/短信集成等场景，已部署上线运行。

> 🔗 **在线体验**：PC 端 <https://wushij.com> · 移动端 H5 <https://app.wushij.com> · 体验账号 `zhangsan` / `zhangsan123`

---

## 目录

- [功能特性](#-功能特性)
- [技术栈](#-技术栈)
- [系统架构](#-系统架构)
- [快速开始](#-快速开始)
- [目录结构](#-目录结构)
- [配置说明](#-配置说明)
- [测试与 CI](#-测试与-ci)
- [构建与部署](#-构建与部署)
- [源码仓库](#-源码仓库)
- [开源协议](#-开源协议)

---

## ✨ 功能特性

### 安全体系

- **统一鉴权**：Sa-Token 统一承担登录态与接口鉴权；`@RequirePermission` 注解由 AOP 切面求值（权限码来自数据库角色/菜单），未登录返回 401、无权限返回 403，响应结构与前端拦截器完全对齐
- **BCrypt 密码哈希 + SM2 传输加密**：密码经 BCrypt 哈希落库；前端默认使用服务端下发的 SM2 公钥加密口令后提交（`sm2:` 前缀 C1C3C2 密文，可在安全配置关闭），服务端私钥解密后校验，私钥永不下发；明文提交保持兼容
- **httpOnly Cookie 传 Token**（防 XSS 窃取）+ 全局安全响应头（`nosniff` / `X-XSS-Protection` / 禁缓存 / HTTPS 下 HSTS；CSP 与 `X-Frame-Options` 由 Nginx 层配置）
- **滑块验证码**：服务端渲染带缺口的背景图与拼图块，缺口 X 坐标仅存 Redis；提交拖动轨迹与耗时完成行为校验（事件数、耗时区间、终点一致性）后签发 2 分钟一次性票据；票据绑定业务场景与客户端 IP，跨场景/跨 IP 不可复用，`profile` 场景要求登录态；同一 challenge 连续失败 3 次作废。响应不包含答案，客户端无法在本地判定后伪造通过
- **登录锁定**：账号级 + IP 级失败计数，阈值可配置，管理员远程解锁；图形/滑块验证码错误不再计入密码失败次数，避免攻击者用空验证码锁死任意账号
- **验证码防爆破**：短信、邮箱验证码按「场景 + 手机号/邮箱/用户」记录失败次数，5 次失败后 10 分钟内直接拒绝；核验与找回密码接口额外叠加按 IP 的请求频率限制
- **客户端 IP 信任链**：仅当请求来自可信代理（默认回环与内网网段）时才读取 `X-Real-IP` / `X-Forwarded-For` / `CF-Connecting-IP`，直连来源伪造代理头不会生效；Nginx 模板统一用 `$remote_addr` 覆盖 `CF-Connecting-IP`
- **国密 API 安全防线**：全链路支持国密 SM4 接口传输加解密（SM4-GCM 认证加密，`IV[12] + Ciphertext + Tag[16]`，认证失败直接拒绝、不降级）与数字签名防篡改（HMAC-SM3 / SM2），整合 5 分钟滑动时间戳（`X-Timestamp`）、Redis 随机数防重放（`X-Nonce`）及 `Encrypt-then-Sign` 签名校验；会话密钥仅保存在内存闭包，F5 刷新后通过 `session-sign-init` 重新协商，避免密钥落盘
- **行为风控与安全审计**：`AntiBotGateFilter` 全路径执行 IP 封禁与爬虫 UA 指纹（python-requests / curl / headless / selenium 等）拦截；`ApiSecurityFilter` 对时间戳缺失、重放、签名不匹配等行为累计风险分（Redis 1 小时滚动），默认 80 分自动临时封禁 1 小时、120 分封禁 24 小时，阈值可在「安全配置」中调整；拦截明细异步落库 `sys_security_event`，提供今日拦截/爬虫/重放/封禁统计与一键解封面板
- **安全防刷与防调试**：前端反调试禁用 Devtools（系统配置可一键开关）、Sa-Token 多端同时在线控制（`isConcurrent`）；Nginx 层按路径分级限流（认证 15r/s、API 50r/s、文件 100r/s）+ 应用层 Redis 滑动窗口兜底（业务 API 240 次/分钟/IP、认证 60 次/分钟/IP、文件 600 次/分钟/IP，均可配置）+ 应用层注册/短信/邮箱防刷
- **邮件服务与防垃圾投递**：支持 SMTP 多服务商（QQ/163/Gmail/自定义）、RFC 2046 规范 `multipart/alternative` 双格式（Plain Text + HTML 卡片）、跨客户端兼容内联矢量 Header，实时持久化发信日志与投递回执（`sys_email_log`）
- **操作审计**：`@Log` 注解 + AOP 全接口记录（操作人/IP/归属地/参数/耗时），`@Async` 异步写入
- **异常统一处理**：`BusinessException` + `GlobalExceptionHandler`（17 类异常），401/403 统一 JSON 响应

### 组织与权限

- **组织（部门）数据隔离**：按角色配置五级数据范围（全部/自定义部门/本部门/本部门及以下/仅本人），用户、组织、文件、工单、审批、登录日志等列表在服务层统一拼接部门范围条件，超管可查看全部数据。系统不设租户概念，数据统一归属组织架构。
- **标准组织与岗位**：内置「总公司 / 研发部 / 运营部」三级部门与「总经理 / 技术总监 / 研发工程师」三岗位，开箱即用
- **五维组织架构**：用户 → 角色 → 菜单 → 部门 → 岗位，全关联管理
- **数据权限五级范围**：全部/自定义/本部门/本部门及以下/仅本人，角色编辑页可视化配置，后端按可见部门集合过滤列表数据
- **RBAC 权限模型**：菜单按角色动态渲染侧栏，按钮级权限指令 `v-permission`/`v-role`
- **系统配置热更新**：12 组配置（下表），Redis 缓存 + 数据库双写，运行时即时生效

| 分组 | 名称 | 关键能力 |
|------|------|----------|
| `site` | 基础信息 | 平台名称/副标题/版权，登录页与工作台展示 |
| `session` | 会话配置 | `tokenExpireHours`（1～720），覆盖 Sa-Token 超时 |
| `file` | 文件与存储 | 本地 / MinIO / 腾讯云 COS / 阿里云 OSS 四引擎热切换、连通性测试、`maxSizeMb`、`allowedExtensions`（上限 500MB） |
| `rateLimit` | 接口限流 | 验证码/登录/注册每 IP 每分钟；短信/邮箱防刷（间隔/日上限）；AI 对话每用户每分钟 |
| `login` | 登录认证 | 验证码开关/类型、短信登录、邮箱登录、发码前滑块、记住我、重试锁定 |
| `register` | 注册认证 | 开放注册、验证码、默认角色、需审核、密码最小长度 |
| `thirdParty` | 第三方配置 | 微信/支付宝/GitHub/Google OAuth 密钥 |
| `payment` | 支付配置 | 微信 Native/支付宝当面付，支持**测试订单**与回调验签 |
| `sms` | 短信配置 | 阿里云/腾讯云双通道（策略模式），模板管理、测试发送与发送日志回执 (`sys_sms_log`) |
| `email` | 邮件配置 | QQ/163/Gmail/自定义 SMTP，发件人名称、SSL/TLS/STARTTLS、测试发送与投递日志回执 (`sys_email_log`) |
| `security` | 安全配置 | 前端反调试（禁用 Devtools）、Sa-Token 多端控制（`isConcurrent`）、国密 SM4-GCM 接口加密、HMAC-SM3/SM2 签名、SM2 密码公钥、风控时间戳/Nonce 开关 |
| `ai` | AI 助手配置 | 助手开关、回答边界（聚焦本系统/开放问答）、全局项目知识、单用户每日 Token 配额与角色配额 |

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
- **AI 模型配置**：供应商/模型/baseUrl/温度等可视化管理，支持连通性测试与启用停用；**API Key 使用国密 SM4-GCM 加密存储**，回显仅掩码，历史 CBC 密文只读兼容
- **AI 知识库（RAG-lite）**：独立菜单 `/ai/knowledge`，支持知识 CRUD、分类/关键词、权重排序、批量启用停用/删除、JSON 批量导入（同标题自动更新）、全量导出；内置 Markdown 双栏编辑与实时预览
- **检索演练与召回测试**：`POST /api/system/ai-knowledge/test-retrieve` 返回命中条目、`<reference_data>` 参考块、字符数与预估 Token，便于调优关键词与内容
- **检索策略**：ngram 全文索引优先（相关度 + 权重排序 Top-3），索引不可用或异常时自动降级 `title/keywords/content` LIKE；命中内容单条截断 900 字、总注入上限 2200 字，并对未闭合代码围栏做兼容处理
- **对话自动注入知识**：`PromptAssembler` 按用户最新提问调用 `KnowledgeRetriever`，把参考资料注入 system 提示词；用户上下文与菜单结构按 userId 走 Redis 10 分钟缓存，权限变更事件主动失效
- **AI 对话日志**：问答全量审计（问题/回答/Token 用量/耗时/状态/来源 pc·mobile），`@Async` 异步落库 `sys_ai_chat_log`，PC 与移动端均可查看明细
- **流式 Markdown 渲染**：markdown-it + highlight.js 代码高亮，`useStreamingMarkdown` 打字机式增量渲染
- **成本与安全防护**：单轮上下文最多 20 条、单条 4000 字截断，提问内容脱敏（`AiSanitizerUtil`），异常统一降级提示；支持按用户与角色维度设置每日 Token 配额
- **独立顶级菜单「AI 管理」**：菜单 ID 210（`/ai`，图标 `Compass`），下挂「AI 模型配置」（`/ai/model`）、「AI 对话日志」（`/ai/log`）与「AI 知识库」（`/ai/knowledge`，图标 `Reading`），权限标识 `system:ai-model:*` / `system:ai-log:*` / `system:ai-knowledge:*`

### 系统监控大屏

| 模块 | 后端 | 前端特性 |
|------|------|----------|
| **服务监控** | Actuator + JMX 采集 CPU/内存/JVM/磁盘 | ECharts 折线趋势图（sessionStorage 保留最近 20 个点，F5 不丢） |
| **缓存监控** | Redis INFO 统计 + SCAN 键 | 内存/QPS/命中率/连接数四宫格图表，键详情的查看与删除（黑名单保护） |
| **API 访问统计** | AOP 拦截器采集 → Redis 队列 → 10s 批量落库 | 统计卡片（请求总数/成功/失败）+ 日志列表 + Excel/CSV 导出 |
| **在线用户** | WebSocket + Sa-Token 会话追踪 | ip2region IP 归属地、浏览器/OS 解析，强制下线 |
| **定时任务** | Quartz 动态调度 | Cron 可视化配置、立即执行/暂停/恢复；内置 7 项清理任务（日志归档、私聊/群聊消息、调度日志、已读通知、工单回收站、文件回收站，默认暂停） |

**采样策略**：超级管理员登录后全局后台轮询（缓存 3s、服务 5s）；普通用户进入监控页才采集，离页即停。请求防堆叠（上次未完成跳过本次）。

### 开发工具

- **代码生成器**：Velocity 模板引擎，导入表结构 → 配置字段 → 一键生成 Controller/Service/Mapper/Entity/Vue 页面完整代码。支持预览（标记新建/覆盖）、ZIP 下载、写入项目并创建菜单；`moduleName` 驱动 API 路径与权限前缀
- **Knife4j 接口文档**：Springdoc OpenAPI 3，Controller 完整 `@Tag`/`@Operation` 注解，生产环境自动关闭。`Knife4jIframeHeaderFilter` 处理 iframe 内嵌
- **列表导出**：EasyExcel 实现 Excel/CSV 双格式导出（`scope=filtered|all`），覆盖用户/日志/工单/审批/API 访问/在线用户，权限独立控制

### 文件与回收

- **多引擎文件存储**：本地磁盘 / MinIO / 腾讯云 COS / 阿里云 OSS 四引擎，配置中心热切换、保存前连通性测试、失败自动回滚；`sys_file` 记录每次上传的 `storage_type` 与 `bucket_name`，读取时优先当前引擎、未命中自动回退其他已配置引擎，删除按记录中的历史引擎路由，热切换后旧文件仍可访问
- **统一流式访问**：`StorageFileController` 提供 `/api/storage/files/{bucket}/**`，图片直出、音视频 Range 断点续传、危险类型强制下载；具备路径穿越校验、bucket 白名单与目录尾斜杠拒绝
- **分层对象键**：`StorageObjectKeyBuilder` 按「业务模块 → 归属对象 → 分类/日期 → 唯一资源」生成对象键（如 `files/images/20261018/<hash>_logo.png`），便于 OSS/COS/MinIO 控制台统一治理
- **文件管理**：MIME 类型校验 + 白名单后缀 + 大小限制，图片/PDF/Office 预览与文本预览，分组管理，重命名/移动/回收站
- **回收中心**：14 类软删数据统一汇总（用户/角色/菜单/部门/岗位/工单/审批/字典类型/字典数据/系统通知/定时任务/调度日志/文件/代码生成），支持恢复与彻底删除，定时自动清理
- **兼容旧路径**：历史文件仍保留 `/api/files/**` 本地直读接口与 Nginx alias，不受存储引擎切换影响

### 跨端移动端

- **uni-app（Vue 3 + Pinia + TypeScript）**：H5 + 微信小程序双端覆盖，与 PC 共用 `/api`
- **安全能力同步**：滑块验证码（服务端图片 + 轨迹/耗时行为校验）、SM4-GCM 请求加密与 HMAC-SM3 签名、SM2 密码公钥加密均已对齐 PC 端
- **H5 浅栈导航**：刷新后 `localStorage` 父级映射 + `SubPageBackBar` 返回条，列表页/详情页均可正常返回
- **移动端 IM**：☺/⌨ 表情键盘切换、H5 `visualViewport` 键盘高度适配、发送后保持键盘；「最近」表情本地记录
- **个人中心**：资料编辑、头像上传、短信验证绑定/更换手机号（发码前强制滑块）、自助改密、忘记密码
- **监控运维**：移动端同步支持服务/缓存/在线用户/定时任务监控、工单审批、代码生成
- **AI 助手同款体验**：`AiWuAssistant` 流式对话 + Markdown 渲染，工作台「AI 管理」分组直达模型配置与对话日志页面（知识库管理暂仅 PC 端提供）
- **API 地址自动解析**：`resolveApiBaseUrl()` 区分 H5 线上（同域 `/api`）与小程序（完整 HTTPS），避免局域网 IP 误打包

### 第三方集成

- **支付**：微信支付 APIv3（Native）+ 支付宝 SDK，含回调验签、测试订单
- **短信**：阿里云/腾讯云双通道（策略模式），短信验证码注册/登录/找回密码/绑定手机号，全量发送日志与状态 (`sys_sms_log`)
- **邮件**：QQ/163/Gmail/自定义 SMTP 协议，HTML 卡片验证码、RFC 2046 双格式防垃圾投递、发件日志与回执明细 (`sys_email_log`)
- **IP 归属地**：ip2region（`ip2region.xdb`），用于登录日志、在线用户解析

---

## 🏗 技术栈

**后端**
- Spring Boot **3.5.13** · Sa-Token **1.39.0**（登录态 + `@RequirePermission` AOP 鉴权）
- `spring-security-crypto`（仅保留 BCrypt，不引入 Web 安全过滤器）
- MyBatis Plus 3.5.9（分页上限 500）· Druid 1.2.24 · Redis + Redisson 3.41.0
- BouncyCastle **1.78.1**（SM4-GCM / SM2）· MinIO Java SDK **8.5.10**（S3 通道兼容阿里云 OSS / 腾讯云 COS）
- Quartz · Knife4j 4.5.0 + Springdoc 2.8.9
- Hutool / EasyExcel / ZXing / ip2region

**前端**
- Vue **3.4** + Composition API + TypeScript 5.3
- Pinia 2.1.7 · Element Plus 2.4.0 · Vite 5
- ECharts 5.6 · Three.js 0.184 · tsparticles（engine/slim 3.9 + vue3 3.0）· @iconify/vue 5
- sm-crypto 0.5（浏览器端 SM2/SM3/SM4）· markdown-it 14 + highlight.js 11（AI 对话流式 Markdown 渲染与代码高亮）

**移动端**：uni-app 3.0（Vue 3）· uv-ui 1.1.20 · luch-request 3.1.1 · sm-crypto · markdown-it/highlight.js

**运行时**：JDK **17** · MySQL **8.0** · Redis **7.x** · Node.js 18+ · Maven 3.6+

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
                              │  │ AntiBot 风控 │ │
                              │  │ ApiSecurity  │ │
                              │  │ SaTokenAuth  │ │
                              │  │ DataScope    │ │
                              │  │ Controller   │ │
                              │  │ ↓            │ │
                              │  │ ServiceImpl  │ │
                              │  │ ↓            │ │
                              │  │ Mapper       │ │
                              │  └─────────────┘ │
                              └──┬───────────┬───┬─────────────┘
                                 │           │   │
                                 ▼           ▼   ▼
                          ┌──────────┐ ┌──────────┐ ┌────────────────────────┐
                          │  MySQL   │ │  Redis   │ │ 对象存储（可热切换）     │
                          │ wu-admin │ │  db=1    │ │ local / MinIO / OSS/COS │
                          └──────────┘ └──────────┘ └────────────────────────┘
```

| 维度 | 说明 |
|------|------|
| **分层约定** | `modules/*` → `framework` → `common`（`framework` 禁止引用 `modules`） |
| **安全链** | `AuthorizationQueryFilter`（Cookie/Header/URL Token 解析）→ `SecurityHeadersFilter`（安全响应头）→ `AntiBotGateFilter`（全路径 IP 封禁 / UA 指纹）→ `ApiSecurityFilter`（时间戳 / Nonce / 签名 / SM4-GCM）→ `SaTokenAuthenticationFilter`（登录态兜底校验）→ `RequirePermissionAspect`（`@RequirePermission` 权限求值）→ `GlobalExceptionHandler`（401/403 JSON） |
| **组织隔离** | 登录用户 → 角色 `data_scope` → 可见部门集合 → 用户/组织/文件/工单/审批/日志列表统一过滤；超级管理员不受限制 |
| **文件存储** | `RoutingFileStorageService` 统一门面（local/MinIO/OSS/COS），写走当前引擎、读跨引擎回退、删按记录引擎；`StorageFileController` 统一流式输出 |
| **Nginx 限流** | 认证 15r/s+burst=10 / 通用 API 50r/s+burst=20 / 文件 100r/s+burst=200 |
| **性能要点** | Async 线程池 (8/32/500) · Druid max-active=50 · 在线心跳 45s 节流 · 工作台单 SQL 聚合 + Redis 2min 缓存 · 监控 in-flight 防堆叠 · API 日志 10s 批量落库 · 业务分页上限 200（插件硬上限 500） · AI 上下文缓存 10min |
| **容量参考** | 2 核 2G Windows 单机 **30～60 人**同时在线；4 核 8G 约 80～150 人 |

---

## 🚀 快速开始

### 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| Node.js | 18+ | 前端 & uni-app |
| JDK | 17 | 后端 |
| Maven | 3.6+ | 后端构建 |
| MySQL | 8.0 | 本地库名 `wu-admin`，生产 `wuadmin` |
| Redis | 7.x | database 1 |

### 1. 初始化数据库

```bash
# 本地开发（库名 wu-admin）
mysql -u root -p < sql/admin_platform_dev.sql

# 生产环境（库名 wuadmin）
mysql -u wuadmin -p wuadmin < sql/admin_platform_prod.sql
```

> 切勿对已有表的生产库执行全量脚本（含 DROP）。已有库升级前请先备份，并按发布包内的增量脚本处理。

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
| **默认组织** | 内置「总公司 / 研发部 / 运营部」，普通用户默认数据范围为「本部门及以下」 |
| **存储引擎** | 默认 `local`（`./data/uploads`）；可在「系统配置 → 文件与存储」切换 MinIO / COS / OSS，保存时先做连通性测试再热重载 |
| **文件访问** | 新文件统一走 `/api/storage/files/{bucket}/**`（GET 公开，支持 Range）；历史本地文件仍可走 `/api/files/**` |
| **ip2region.xdb** | IP 归属地库，10MB；放 `backend/src/main/resources/ip2region/`，仓库 git 忽略；缺失时公网 IP 解析显示「未知」 |
| **API 文档（开发）** | <http://localhost:3000/doc.html>（Vite 代理到后端 8080） |
| **API 文档（生产）** | 默认关闭（`application-prod.yml` 中 `springdoc.enabled=false`）；如需开放，启用后经 Nginx 反代 `/doc.html` 访问（依赖 `Knife4jIframeHeaderFilter`） |
| **WebSocket** | `wss://域名/api/ws/message`（PC 走 httpOnly Cookie，H5/小程序 URL 带 `?Authorization=<token>`） |
| **小程序前置** | 微信开发者工具 + 合法 AppID；`build:mp-weixin` + 配置 `VITE_API_PRODUCTION_ORIGIN`（详见 [配置说明](#-配置说明)） |
| **CI 范围** | 仓库 `.github/workflows/ci.yml` 在 `main`/`master` 的 push 与 PR 上跑 `mvn test` + `npm run typecheck/test`，**未纳入 ESLint**（存量 `vue/no-mutating-props` 警告待清理） |
| **列表导出 scope** | `scope=filtered`（当前筛选） / `scope=all`（权限范围内全量，受 `PageParam` 200 上限约束）；`scope=page` 即当前页 |

---

## 📁 目录结构

```
wu-admin/
├── .github/workflows/ci.yml       # GitHub Actions CI（mvn test + npm typecheck/test）
├── backend/                       # Spring Boot 后端
│   ├── pom.xml
│   ├── data/                      # 本地开发态上传与数据目录（git 忽略）
│   └── src/
│       ├── main/java/com/admin/server/
│       │   ├── WuAdminApplication.java  # Spring Boot 启动主入口
│       │   ├── common/            # 通用层：通用结果、异常定义、通用工具（零业务依赖）
│       │   │   ├── core/          # CommonResult、PageParam、PageResult
│       │   │   ├── exception/     # BusinessException、ErrorCode、GlobalErrorCode
│       │   │   ├── mybatis/       # 通用 MyBatis 拓展基类
│       │   │   └── util/          # 可信代理 IP 解析、Bean 映射、加解密工具类等
│       │   ├── framework/         # 技术基础设施层（技术组件封装，可抽独立 Starter）
│       │   │   ├── config/        # 异步线程池、Sa-Token Cookie、跨域、密码等基础配置
│       │   │   ├── export/        # EasyExcel / CSV 通用导出引擎
│       │   │   ├── log/           # @Log 操作审计注解与 AOP 切面
│       │   │   ├── mybatis/       # MyBatis-Plus 配置（分页插件、审计字段自动填充、BaseDO 基类）
│       │   │   ├── openapi/       # Knife4j / Springdoc OpenAPI 配置与安全拦截
│       │   │   ├── quartz/        # Quartz 定时任务调度框架
│       │   │   ├── redis/         # RedisTemplate 序列化配置与缓存操作
│       │   │   ├── security/      # Sa-Token 鉴权、国密 SM2/SM4-GCM、蜜罐路径防御、反爬风控
│       │   │   ├── storage/       # 多引擎存储路由门面（local / MinIO / COS / OSS）与流式访问
│       │   │   ├── web/           # 全局异常捕获、Token 提取解析、安全响应头等 Web 支撑
│       │   │   └── websocket/     # WebSocket 连接鉴权与实时消息推送
│       │   └── modules/           # 业务模块层（按高内聚业务领域垂直拆分，内聚四层架构）
│       │       ├── system/        # 系统管理（用户/角色/菜单/组织/岗位/字典/配置/安全风控/审计日志等）
│       │       ├── infra/         # 基础设施（文件管理/代码生成/任务调度/系统监控/回收站/通用导出等）
│       │       ├── trade/         # 交易集成（微信 Native/支付宝支付、短信双通道、邮件 SMTP 发信）
│       │       ├── ticket/        # 工单审批（工单全生命周期流转、通用审批流引擎、用户注册审批流）
│       │       ├── message/       # 消息通讯（站内信公告、企业 IM 私聊/群聊、@提及、群公告）
│       │       └── ai/            # AI 管理（SSE 流式对话、模型配置、知识库 RAG、对话日志审计）
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
│       ├── core/                  # 前端核心基础设施层（全局唯一 Axios 封装、SM4 报文加密、防重放、Token 鉴权）
│       │   └── http/              # request.ts（请求拦截器与实例封装）、api-response.ts
│       ├── api/                   # API 接口定义层（按业务模块划分，只声明入参/出参与请求地址）
│       ├── views/                 # 页面视图层（按业务路由编排，复杂大页面组件化与 composables 拆分）
│       │   ├── ai/                # AI 模块（模型配置、对话日志、知识库管理）
│       │   ├── system/            # 系统管理（用户/角色/菜单/组织/字典/配置/安全风控/回收站等）
│       │   ├── monitor/           # 系统监控（服务/缓存/在线用户/API 访问/定时任务）
│       │   ├── message/           # 消息中心（企业 IM、公告通知）
│       │   ├── dashboard/         # 仪表盘工作台（统计卡片、趋势图表、快捷入口）
│       │   └── tool/              # 工具中心（代码生成、接口文档）
│       ├── components/            # 全局通用组件与领域组件（AiWu 悬浮助手、SliderCaptcha、DictSelect 等）
│       ├── composables/           # 组合式函数（跨页面业务逻辑复用，如 useDict、useStreamingMarkdown）
│       ├── router/                # 路由配置与动态权限守卫
│       ├── store/                 # Pinia 全局状态管理（user / message / site / tagsView / aiWu）
│       ├── utils/                 # 纯函数工具库（按领域严格聚拢，无 Vue 响应式依赖）
│       │   ├── chat/              # IM 聊天渲染与消息时间格式化
│       │   ├── security/          # 国密 SM2/SM4 密码与报文加解密、安全配置辅助
│       │   ├── tree/              # 菜单树、部门树构建与平铺
│       │   └── permission.ts      # 纯权限判定函数（彻底解耦 Router 路由与 Directives 指令）
│       ├── directives/            # 自定义指令（v-permission / v-role 按钮级鉴权）
│       ├── constants/             # 全局业务常量与字典枚举
│       ├── styles/                # SCSS 全局样式系统、CSS 变量、主题与 Element Plus 覆盖
│       └── types/                 # TypeScript 类型定义声明
├── uniapp/                        # uni-app 移动端（Vue 3 + Pinia + TS，H5 / 微信小程序）
│   ├── .env / .env.example / .env.production
│   ├── index.html / package.json / package-lock.json / tsconfig.json / shims-uni.d.ts
│   ├── vite.config.ts
│   └── src/
│       ├── manifest.json / pages.json / uni.scss / env.d.ts
│       ├── api/                   # 接口定义层（按业务模块划分，只声明地址/入参/返回类型）
│       ├── pages/                 # 主包页面（首页 / 工作台 / 消息 / 我的 + 登录 / 注册）
│       ├── pages-sub/             # 分包页面（system / monitor / msg / ai / log / mine）
│       ├── components/            # 移动端三层组件体系
│       │   ├── base/              # 纯 UI 原子组件（FormCell、ListCard、EmptyState、IconFont、FabButton 等）
│       │   ├── layout/            # 页面骨架与容器（ModuleHero、ModuleDarkHero、PageTabShell、MonitorPanel）
│       │   └── business/          # 业务组件（按领域划分：ai/auth/chat/dashboard/earth/file/message/mine/monitor/system/work/shared）
│       ├── composables/           # 组合式函数（按领域分组：base/auth/chat/dashboard/monitor/profile/system）
│       ├── core/                  # 移动端基础设施层（唯一 HTTP 实例 http.ts、ws.ts、storage.ts、auth.ts、cache.ts、api-base.ts、logger.ts）
│       ├── utils/                 # 纯工具库（分组收口：nav / tree / display / security 及无领域依赖工具）
│       ├── constants/             # 全局常量（TabBar 配置、图标映射等）
│       ├── store/                 # Pinia 状态管理
│       ├── config/                # 基础运行配置
│       ├── plugins/               # 全局插件（全局错误捕获、页面守卫、Pinia 挂载）
│       ├── custom-tab-bar/        # 自定义底部导航栏
│       ├── styles/                # 全局样式
│       ├── types/                 # TypeScript 类型声明
│       └── static/                # 静态资源（图片、图标、滑块背景图等）
├── scripts/                       # 生产环境自动化部署脚本（deploy-backend.sh / deploy-frontend.sh）
├── docs/                          # Nginx 生产配置模板、渗透测试报告等
├── sql/                           # 数据库全量脚本（admin_platform_dev.sql / admin_platform_prod.sql）
└── data/                          # 本地开发态上传与数据目录（git 忽略）
```

### 后端包分层与四层架构约定

后端采用 **「全局单向分层 + 业务模块四层内聚」** 的架构组织规范。

#### 1. 全局分层依赖规则
- **依赖方向**：`modules/*` → `framework` → `common`，严格单向依赖。
- **解耦约束**：
  - `framework` 严禁反向依赖 `modules`（保证通用基础设施可独立抽取为公共 Starter）；
  - `common` 保持 0 业务依赖，仅提供基础通用规范与纯工具类；
  - 业务模块之间禁止双向或循环依赖，模块间业务协作通过各自暴露的 Service 接口解耦。

#### 2. 业务模块内部标准四层结构（modules/<module>/）
每个业务模块内部遵循标准的领域四层架构，职责边界高度清晰：

```
modules/<module_name>/
├── api/             # ① 表现层（Controller / Web API）
│   ├── <domain>/    # 接收 HTTP 请求，进行参数校验，调用 Service 业务，组装统一响应体 CommonResult
│   └── vo/          # 视图传输对象（ReqVO 入参、RespVO 出参、PageReqVO 分页入参）
├── service/         # ② 业务逻辑层（Service）
│   ├── <domain>/    # 业务接口定义（单一职责，清晰表达业务行为）
│   └── <domain>/impl/ # 业务实现类（编排业务逻辑、事务控制 @Transactional、多源数据装配、外部调用）
├── dal/             # ③ 数据访问层（DAL: Data Access Layer）
│   ├── dataobject/  # DO 实体类（对应数据库表，继承 BaseDO，承载持久化状态）
│   ├── mysql/       # MyBatis-Plus Mapper 接口及 XML（按领域分包，纯 SQL 与条件构造）
│   └── redis/       # 模块专属的 Redis 缓存操作 / DAO（如适用）
├── convert/         # ④ 实体转换层（Convert）
│   └── <domain>/    # DO 与 VO/DTO 之间的属性互转映射（MapStruct / BeanMappingUtils）
└── framework/       # 模块专属扩展（模块级特定配置、适配器，如 Security 蜜罐、配置中心 Provider 等）
```

- **调用流转红线**：调用链路严格按 `api` → `service` → `dal` 单向流转；严禁 Controller 绕过 Service 直接注入 Mapper 进行数据读写；严禁 `dal` 反向引用 `service`。
- **实体收拢规范**：数据库实体必须放置在各模块的 `dal/dataobject/` 目录下，Mapper 接口与 XML 统一置于 `dal/mysql/`，保持各模块目录骨架一致。

### PC 前端目录分层约定

PC 前端（Vue 3 + TypeScript）采用 **「核心基础设施下沉 + 纯工具领域聚拢 + 页面组件化编排」** 的架构组织规范：

```
frontend/src/
├── core/                  # 基础设施层：全局唯一 HTTP 客户端，报文加解密、防重放与拦截器收口
│   └── http/              # request.ts（Axios 实例与拦截器）、api-response.ts
├── api/                   # 接口定义层：按业务模块组织，纯函数声明（URL、Method、入参与出参类型），不写 UI 逻辑
├── views/                 # 页面视图层：按路由编排，复杂大页面（>500 行）必须抽离私有 components/ 与 composables/
├── components/            # 全局通用组件：
│   ├── AiWu/              # AI 助手悬浮球与对话面板
│   ├── auth/              # 登录认证与滑块验证码
│   ├── chat/              # 企业 IM 聊天公用组件
│   ├── earth/             # 3D 监控地球（Three.js）
│   └── ...                # DictSelect、DictTag、IconSelect 等通用原子组件
├── composables/           # 组合式函数：跨页面状态与业务逻辑复用（useDict、useStreamingMarkdown 等）
├── utils/                 # 领域纯工具库：无 Vue 响应式依赖的纯函数（禁止包含 ref/reactive）
│   ├── chat/              # 聊天渲染、消息气泡时间格式化纯函数
│   ├── security/          # 国密 SM2/SM4 密码与数据加解密、安全配置辅助
│   ├── tree/              # 菜单树、组织树的递归构建与平铺
│   └── permission.ts      # 纯权限判断工具（彻底解耦 Router 路由守卫与 Directives 自定义指令）
├── directives/            # 自定义指令：v-permission / v-role（按钮级鉴权）
├── router/                # 路由配置、静态路由与动态权限路由守卫
├── store/                 # Pinia 全局状态管理
├── constants/             # 全局常量、配置常量与枚举映射
├── styles/                # SCSS 全局样式系统与主题变量
└── types/                 # 全局 TypeScript 接口与类型定义
```

- **前端架构约束**：
  1. `core/http/` 承载唯一 Axios 实例与安全加解密，禁止在页面或工具层重复创建请求客户端；
  2. 页面之间禁止相对路径跨模块引用彼此内部私有组件，跨页面复用组件必须提升到 `components/`；
  3. `utils/` 严格为无副作用纯函数，严禁引入 Vue 响应式 API（`ref` / `reactive` / `computed`）或 Pinia store；
  4. 路由守卫鉴权必须依赖 `utils/permission.ts` 纯函数，彻底切断路由对指令层的反向依赖。

### 移动端目录分层约定

```
uniapp/src/
├── pages/ pages-sub/      # 路由级页面：只做布局编排与子组件引用，不堆业务细节
├── components/
│   ├── base/              # 纯 UI，无业务语义（FormCell、ListCard、EmptyState、IconFont、FabButton 等）
│   ├── layout/            # 页面骨架与容器（ModuleHero、ModuleDarkHero、PageTabShell、MonitorPanel）
│   └── business/
│       ├── shared/        # 有业务语义、跨 ≥2 个领域复用（SliderCaptcha、UserAvatar）
│       └── <domain>/      # 单领域业务组件（ai/auth/chat/dashboard/earth/file/message/mine/monitor/system/work）
├── composables/           # 组合式函数，按领域分目录，base/ 放跨域复用逻辑
├── core/                  # 唯一基础设施层：http.ts、ws.ts、storage.ts、auth.ts、cache.ts、api-base.ts、logger.ts
├── utils/                 # 纯函数，按 nav / tree / display / security 分组收口
├── api/                   # 只放接口定义（地址、方法、入参、返回类型）
├── types/                 # 数据结构统一收口
├── constants/             # 常量
└── store/                 # Pinia 全局状态
```

- **移动端架构约束**：
  1. 页面之间禁止互相 import 私有 UI；跨页面复用的 UI 必须下沉到 `components/`；
  2. `core/http.ts` 是唯一 HTTP 实例，禁止在其他层重复创建请求实例；
  3. `utils/` 只放无 Vue 响应式依赖的纯函数；带响应式或生命周期的一律放 `composables/`；
  4. `api/` 不写业务编排、不操作页面状态、不引用组件；
  5. 新增组件先判断归属：纯 UI → `base`；页面骨架 → `layout`；单领域 → `business/<domain>`；跨领域业务语义 → `business/shared`；
  6. 同一目录出现第二个业务概念时必须下钻子目录，不允许平铺堆积；
  7. 单个 `.vue` 超过 500 行应按业务块拆分子组件；目前 `pages-sub/system/config`、`security`、`file` 与 `pages-sub/monitor/api-access` 仍是待拆的超大页面；
  8. 分包 `pages-sub/ai` 已完整集成模型配置（`model/`）、对话日志（`log/`）与知识库管理（`knowledge/`）。

---

## 🔧 配置说明

### 数据库设计规范

所有表遵循统一范式：

| 维度 | 规范 |
|------|------|
| 主键 | `BIGINT AUTO_INCREMENT PRIMARY KEY` |
| 组织字段 | 用户归属 `dept_id`，文件与文件分组归属 `dept_id`；列表按角色数据范围过滤 |
| 审计字段 | `create_time` / `update_time` / `creator` / `updater`（MyBatis Plus 自动填充） |
| 逻辑删除 | `deleted TINYINT DEFAULT 0`，MyBatis Plus `logic-delete-value: 1` |
| 字符集 | `utf8mb4_unicode_ci`（支持 emoji） |
| 字段注释 | 所有字段含 `COMMENT` |

### 后端 YAML 配置要点

| 配置项 | 说明 |
|--------|------|
| `server.port` / `context-path` | `8080` / `/api` |
| `spring.profiles.active` | 默认 `prod`，本地开发 `-Dspring.profiles.active=dev` |
| `spring.datasource.*` | prod 库 `wuadmin`，dev 库 `wu-admin` |
| `spring.redis.database` | `1`；密码仅写在 `application-prod.yml` |
| `sa-token.timeout` | 缺省 86400s，运行时由系统配置「会话配置」覆盖 |
| `file.storage.type` | 缺省存储引擎：`local` / `minio` / `cos` / `oss`；启动后由 `sys_config_group.file` 覆盖并热加载 |
| `file.storage.local-path` | dev: `./data/uploads`；**prod: `/www/server/wuadmin/data/uploads`**（本地引擎根目录，自动创建） |
| `file.storage.minio.*` | MinIO 缺省端点/AccessKey/SecretKey/Bucket（仅作为启动兜底） |
| `knife4j.enable` | 建议 `false`（4.5.0 + springdoc 2.8 兼容性） |
| `springdoc.api-docs.path` | `/v3/api-docs`（生产 `application-prod.yml` 关闭） |

**运行时配置优先读库**：`sys_config_group` 表 → `SystemConfigHelper` → `DynamicConfigProvider`，YAML 仅作缺省兜底。存储引擎在应用启动时由 `StorageConfigInitializer` 从库加载并注册驱动，修改后立即热重载。

**敏感配置**：`security` 分组的 `sm4SecretKey` / `sm3SignKey` / `sm2PrivateKey` 与 `file` 分组的 OSS/COS/MinIO 密钥在读取接口中统一掩码；SM2 公钥可公开下发，私钥永不出现在响应中。

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
| `/api/auth/**` | 登录/注册/验证码/config/profile/滑块挑战与校验/session-sign-init | 🌐 匿名 |
| `/api/system/**` | 用户/角色/菜单/组织/字典/配置/工单/审批/消息 | 🔒 登录+权限 |
| `/api/system/security/**` | 安全风控：拦截事件、统计、风险名单、封禁/解封 | 🔒 登录+权限 |
| `/api/system/config-group/storage` | 存储引擎配置读取、保存、连通性测试与热重载 | 🔒 登录+权限 |
| `/api/ai/chat/**` | AI wu助手流式对话（SSE）与启用模型列表 | 🔒 登录 |
| `/api/system/ai-model/**` `/api/system/ai-log/**` | AI 模型配置、AI 对话日志管理 | 🔒 登录+权限 |
| `/api/system/ai-knowledge/**` | AI 知识库 CRUD、批量导入导出、检索演练 | 🔒 登录+权限 |
| `/api/monitor/**` | API 访问/在线用户/定时任务/缓存/服务监控 | 🔒 登录+权限 |
| `/api/pay/notify/**` | 微信/支付宝回调 | 🌐 白名单 |
| `/api/storage/files/{bucket}/**` | 多引擎文件统一流式访问（Range/下载） | 🌐 GET 公开 |
| `/api/files/**` | 历史本地文件直读（Nginx alias） | 🌐 GET 公开 |
| `/api/tool/gen/**` | 代码生成器 | 🔒 登录+权限 |
| `/api/dashboard/**` | 工作台统计 | 🔒 登录 |
| `/api/excel/**` | 列表 Excel/CSV 导出 | 🔒 登录+权限 |

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
| 核心 | 登录态、消息推送、页签缓存、权限判断 | 认证（登录/注册/限流）、权限匹配、组织数据权限、CRUD |
| 安全 | 请求签名/加密工具（SM4）、滑块组件、登录记忆 | 滑块行为校验、风控风险分/封禁、SM2/SM4-GCM 工具、签名过滤器 |
| 工具 | 拦截器、菜单树、组织树、聊天文案、防抖、定时任务 Cron 预设 | 异常处理（17 类）、HTTP 状态映射、对象键生成、存储路由 |
| 业务 | — | 工单流转、审批、IM（私聊/群聊/撤回）、字典缓存、知识库检索降级 |

### GitHub Actions CI

推送到 `main`/`master` 或开 PR 时自动执行：

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

本地存储引擎的默认根目录为 `/www/server/wuadmin/data/uploads/`（与 jar 同目录，**后端会自动创建**，无需手动 mkdir；可在系统配置中修改）：

| 组件 | 路径 | 说明 |
|------|------|------|
| `application-prod.yml` / 系统配置 `file.localPath` | `/www/server/wuadmin/data/uploads` | 本地引擎实际落盘目录 |
| Nginx `alias`（两站点） | `/www/server/wuadmin/data/uploads/` | **仅服务历史 `/api/files/` 直读** |
| 新文件统一入口 | 后端 `/api/storage/files/{bucket}/**` | 由 Spring Boot 流式输出，Nginx 反代即可 |

> 切换到 MinIO / OSS / COS 后，新文件不再落本地磁盘；本地目录仅保留历史文件。无论使用哪种引擎，`/api/storage/files/` 始终可用。

### 生产默认连接

| 项 | 值 | 说明 |
|----|-----|------|
| MySQL 库/用户/密码 | `wuadmin` / `wuadmin` / `root` | `application-prod.yml`，部署前修改 |
| Redis 密码 | `root`（db=1） | 同上 |
| 存储引擎 | `local`（`/www/server/wuadmin/data/uploads`） | 可在「系统配置 → 文件与存储」热切换为 MinIO/COS/OSS |
| SM2 密钥对 | 运行时生成或系统配置指定 | 私钥仅服务端持有，公钥通过 `/auth/config` 下发 |
| CORS | 须改为实际域名 | 示例：`https://wushij.com,https://www.wushij.com,https://app.wushij.com` |

---

## 🔗 源码仓库

| 平台 | 仓库地址 |
|------|------|
| 🐙 GitHub | <https://github.com/wushij/wu-admin> |
| 🔴 Gitee | <https://gitee.com/wusj17/wu-admin> |

---

## 📜 开源协议

本项目采用 [Apache-2.0 License](LICENSE) 开源协议。

- 允许免费用于个人学习、学术研究以及商业项目二次开发；
- 衍生修改或二次分发请保留原项目的版权声明、许可条款与免责声明；
- 生产环境部署上线前请务必修改默认密码、JWT 密钥以及数据库/Redis 等敏感配置。
