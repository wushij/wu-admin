# Admin Platform

基于 **Vue 3 + Spring Boot** 的企业级后台管理系统，提供**工作台**、用户权限、组织岗位、业务工单与审批、系统监控、日志审计、文件与字典、**分组系统配置**（含第三方/支付）、**个人中心**、**注册审核**等能力，支持本地开发调试与自建部署。

---

## 功能概览

| 模块 | 说明 |
|------|------|
| **工作台** | 首页统计（用户/角色/部门/文件等）、待办提醒（待审用户、工单、审批）、12 项快捷入口、最近登录；`/dashboard/*` |
| **系统管理** | 用户、角色、菜单、组织、字典（含**类型复制**）、**系统配置**；用户列表默认仅展示**已入库**用户（启用/停用）；待审核/驳回在审批单中心处理 |
| **组织管理** | 部门 + 岗位；左树右表、拖拽调整、岗位成员、部门回收站 |
| **菜单管理** | 树形表格；目录/菜单/按钮联动；图标选择器；外链新窗口 / iframe 内嵌 |
| **系统配置** | 十分组 Tab：基础信息、会话、文件、限流、登录/注册认证、**第三方配置**、**支付配置**、**短信配置**、安全配置；支付支持**测试订单**与异步回调；短信支持**测试发送**与发送记录 |
| **个人中心** | 顶栏入口 `/profile`：资料编辑、头像上传、**短信验证绑定/更换手机号**（发码前强制滑块）、自助改密、**短信验证重置密码**（忘记当前密码时）、我的登录记录 |
| **回收站** | 用户、角色、菜单、部门、工单、审批单逻辑删除，支持恢复与彻底删除 |
| **开发工具** | 内嵌 Knife4j 接口文档（`doc.html`） |
| **系统日志** | 操作日志（AOP，含详情）；登录日志（**ip2region IP 归属地**、浏览器解析） |
| **系统监控** | API 访问统计（ECharts 图表 + 日志列表）、在线用户与强退、**定时任务**（Quartz 调度、内置清理任务，默认暂停） |
| **文件管理** | 分组 CRUD、按类型筛选；图片/PDF/Office 预览；大小与扩展名受**系统配置**约束 |
| **业务中心** | **工单**：优先级、截止/超时、评论附件、指派与全员通知（非超管仅看本人相关）；**审批**：请假/采购/报销/用印/合同/通用 + `REGISTER` 注册审核，**详情抽屉内可直接通过/驳回**，支持归档 |
| **消息中心** | **业务消息**（`sys_notice`，工单/审批触达）、系统通知（全员/用户/部门定向、发送日志）、即时聊天（私聊/群聊）、WebSocket |
| **认证安全** | 图片/滑块验证码、**短信验证码登录**（独立开关，与账号验证码分离）、**短信发码前滑块**（可选）、登录失败锁定（用户+IP）、记住我、登录/注册/短信**限流防刷**；账号密码错误统一提示「账号或密码错误」；Sa-Token 会话（Redis db=1） |
| **界面体验** | 主题色切换；登录/注册页 Three.js 地球 + 粒子背景；顶栏消息铃铛三 Tab |

侧栏菜单由 `sys_menu` 按角色动态渲染（超级管理员默认全部）；页面路由在 `frontend/src/router` **静态注册**，新增菜单时需保证 `path` 与路由一致。修改菜单或角色后需**重新登录**刷新侧栏。

---

## 近期优化与增强

以下为近期迭代的主要能力，便于对照部署与联调。

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

后端：`ProfileSmsMobileBindService`；前端：`frontend/src/views/profile/index.vue`。

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
3. 管理员在 **业务中心 → 审批单中心** 通过或驳回（列表「处理」下拉，或打开 **详情** 抽屉底部 **通过 / 驳回**）；
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

全量安装与已有库升级：

| 场景 | 脚本 | 命令 |
|------|------|------|
| **全新安装（空库）** | `sql/admin_platform.sql` | `mysql -u root -p < sql/admin_platform.sql`（空库自动放行） |
| **极旧库首次升级** | `admin_platform.sql` **附录段**（约 910 行起） | 补全缺表/菜单/索引，可重复执行 |
| **发版增量** | **`sql/add1.sql`**、`sql/add2.sql` … | `mysql -u root -p wu-admin < sql/add2.sql`（仅本版新增项） |

> 切勿对生产库直接跑 `admin_platform.sql` 全文（Part A 含 DROP，默认会被熔断拦截）。

正文已含：消息中心表（§11b）、`sys_chat_group_log`、分级组织示例、定时任务、短信配置与 `sys_sms_log`、性能索引（含清理任务相关时间索引）。升级后涉及菜单变更时请 **重新登录**；WebSocket 与新接口需 **重启后端**。

> 说明：历史聊天图片若曾走通用文件上传，可能仍出现在文件列表；升级后新发的聊天图片走专用目录，列表会自动排除。

## 定时任务（系统监控 → 定时任务）

- 菜单：`/monitor/job`，权限 `monitor:job:*`
- 表：`sys_job`、`sys_job_log`；基于 Quartz 调度，支持 CRUD、暂停/恢复、立即执行
- 内置 6 项系统清理任务（过期日志、私聊/群聊消息、调度日志、已读通知、工单回收站），**默认暂停**，可在管理页启用
- 全量脚本 §7b 建表并初始化；旧库通过 `admin_platform.sql` 附录补建

---

## 组织管理 / 菜单 / 接口文档

### 组织管理（`/system/org`）

- Tab：**部门体系 | 岗位体系**
- 部门：树形、`ancestors`、拖拽、回收站；左侧树**默认折叠**（隐藏唯一根节点后直接展示下级）
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
├── backend/                    # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/java/cn/rbac/server/
│   │   ├── common/             # 通用 POJO、工具类
│   │   ├── framework/          # 安全、MyBatis、Redis、Web 过滤器等
│   │   └── modules/system/     # 系统业务（api / service / dal）
│   └── src/main/resources/
│       ├── application.yml          # 公共配置；默认 profile=prod
│       ├── application-dev.yml      # 本地开发（wu-admin 库、无 Redis 密码）
│       └── application-prod.yml     # 生产（wuadmin 库、Redis 密码等）
├── frontend/                   # Vue 3 + TypeScript 前端
│   ├── src/
│   │   ├── api/                # 接口封装（system、message、monitor 等，均为 .ts）
│   │   ├── views/              # 页面（system、message、monitor、profile、login 等）
│   │   ├── components/         # 公共组件（DictSelect、SliderCaptcha、MessageNotification、earth/Earth3D 等）
│   │   ├── router/             # 路由与守卫
│   │   ├── store/              # Pinia（user、message 等）
│   │   ├── types/              # TS 类型（api、message、config）
│   │   ├── utils/              # request、主题、菜单、org-tree、WebSocket 工具
│   │   └── directives/         # v-permission 等指令
│   ├── tsconfig.json
│   └── vite.config.ts          # 开发代理 /api → localhost:8080
├── sql/
│   ├── admin_platform.sql      # 全量安装 + 文末附录（旧库首次补丁）
│   ├── add1.sql                # 增量补丁 #1
│   └── add2.sql                # 增量补丁 #2（登录 smsLoginSliderCaptchaEnabled）
├── data/                       # 本地上传目录（git 忽略，对应 file.storage.local-path）
└── README.md
```

### 后端包结构（`backend/src/main/java`，分层约定）

依赖方向：**`modules/*` → `framework` → `common`**（`framework` 禁止引用 `modules`）。

```
cn.rbac.server/
├── common/
│   ├── pojo/                         # CommonResult、PageParam、PageResult
│   └── util/                         # ClientIpUtils、UserAgentUtils、IpLocationUtils
├── framework/                        # 技术基础设施（可抽公共 starter）
│   ├── config/                       # DynamicConfigProvider、DevRedissonConfig（dev）
│   ├── security/
│   │   ├── api/                      # PermissionApi（SPI）
│   │   ├── config/                   # SecurityConfig
│   │   └── core/                     # TokenService、SecurityUtils
│   ├── web/
│   │   ├── core/                     # GlobalExceptionHandler
│   │   └── filter/                   # SaTokenAuthenticationFilter、Knife4jIframeHeaderFilter
│   ├── log/annotation/               # @Log
│   ├── mybatis/、redis/、storage/
└── modules/
    └── system/                       # 系统域业务
        ├── api/                      # REST Controller（含 pay、auth/profile）
        ├── pay/                      # 微信/支付宝测试下单与回调
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

---

## 快速开始

### 1. 初始化数据库

```bash
# 空库全新安装（直接执行即可）
mysql -u root -p < sql/admin_platform.sql

# 已有库发版增量（按版本依次执行）
mysql -u root -p wu-admin < sql/add1.sql
mysql -u root -p wu-admin < sql/add2.sql

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
| `/api/monitor/**` | API 访问、在线用户、**定时任务** |
| `/api/dashboard/**` | 工作台统计（含配置摘要、待审核用户数） |

---

## 数据库脚本

维护 **`sql/admin_platform.sql`**（全量 + 附录）与 **`sql/add1.sql`、`sql/add2.sql`** 等增量补丁：

| 场景 | 做法 |
|------|------|
| **全新安装** | 空库直接 `mysql -u root -p < sql/admin_platform.sql` |
| **极旧库首次升级** | 执行 `admin_platform.sql` 文末 **附录**（约 910 行起） |
| **发版增量** | 依次 `mysql -u root -p wu-admin < sql/add1.sql`、`sql/add2.sql` …（仅含该版本新增 SQL） |

`addN.sql` 体量应保持在几十行量级；全量补丁逻辑在 `admin_platform.sql` 附录。

| 增量脚本 | 内容 |
|----------|------|
| `add1.sql` | 清理旧版冗余索引等 |
| `add2.sql` | 登录配置 `smsLoginSliderCaptchaEnabled`（短信发码前滑块，默认 `false`） |

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

在 `modules/system/api` 的 Controller 方法上添加 `@Log`（`framework.log.annotation`），由 `modules/system/framework/operlog/LogAspect` 经 `OperLogRecorder` 写入 `sys_oper_log`。

### 按钮权限

```html
<el-button v-permission="'system:config:update'">保存</el-button>
```

标识与 `sys_menu.permission` 一致，如 `system:approval:approve`。

### 登录 / 注册页

- 路由：`/login`、`/register`；左侧为 **Three.js 3D 地球**（`frontend/src/components/earth/Earth3D.vue`），透明画布透出粒子星空，支持鼠标拖拽旋转与滚轮缩放。
- 文案与验证码等行为由公开配置驱动，见下节。
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
- 单元测试：`cd frontend && npm run test`（Vitest）。
- ESLint：`cd frontend && npm run lint`。
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
A：对已有库：极旧库先跑 **admin_platform.sql 附录**（补全 site/session/file/rateLimit 等）；已跑过附录则按需依次执行 `add1.sql`、`add2.sql` 增量。

**Q：注册后无法登录？**  
A：若开启「注册需审核」，需管理员在审批单中心通过；登录提示「账号待审核」属正常。驳回后账号已软删，需重新注册或联系管理员。

**Q：用户管理里看到待审核用户，或一进页面就弹「禁用用户」？**  
A：升级后默认列表已排除待审核/驳回用户；待审用户仅在审批单中心处理。若仍为旧版，请更新前后端并刷新页面。

**Q：驳回后同用户名无法注册，或提示权限不足？**  
A：① 确认 **注册认证** 已开启开放注册；② 升级后软删用户名可自动恢复再注册；③ 若回收站仍有该用户，可「清除」彻底删除后再试；④ 注册/登录请求勿带管理员 Token（新版前端已自动跳过）。

**Q：如何开启短信发码前滑块？**  
A：**系统配置 → 登录认证** 开启「短信验证码登录」与「发送前滑块验证」；已有库执行 `sql/add2.sql` 补配置字段，保存后刷新登录页。

**Q：接口文档 iframe 空白或 `/v3/api-docs` 403？**  
A：① 确认后端（8080）已启动，浏览器访问 `http://127.0.0.1:8080/api/v3/api-docs` 应返回 JSON；② 开发环境重启 Vite 以加载 Knife4j 代理；③ 生产环境确认已部署含 `Knife4jIframeHeaderFilter` 的后端（响应头 `X-Frame-Options: SAMEORIGIN`）；④ 勿将 springdoc 降为 2.6（与 Spring Boot 3.5 不兼容）；⑤ `knife4j.enable` 保持 `false` 直至升级兼容的 Knife4j 版本。

**Q：Knife4j 调试 404 或返回 HTML？**  
A：已配置 OpenAPI 默认服务 `http://localhost:3000/api`；重启后端与 Vite 后，在文档页选择该服务器、方法用 PUT/POST，并填 `Authorization`。若仍 401，先登录管理端复制 token。

**Q：上传失败提示大小或类型？**  
A：在 **系统配置 → 文件存储** 调整；单文件上限不得超过 500MB。

**Q：消息中心菜单不显示或聊天 403？**  
A：对已有库：极旧库先跑 **admin_platform.sql 附录**；发版增量跑 `sql/add1.sql`，重启后端后 **重新登录**。普通用户需角色分配菜单 170/172；只读权限用户访问 `:list` 接口时会映射为 `:query`。

**Q：顶栏有通知角标但列表为空？**  
A：确认 WebSocket 已连接（登录后自动初始化）；在顶栏铃铛打开「系统通知」Tab 会拉取列表。管理员发布通知需 `system:announce:publish`。

**Q：聊天图片出现在文件管理里？**  
A：升级后新图片走 `/system/chat/upload/image`，存储于 `images/chat/` 且文件列表已排除；历史旧数据可手动删除。

**Q：开启「禁止前端调试」无效？**  
A：在 **系统配置 → 安全配置** 保存后需 **整页刷新**；缺 `security` 分组时先跑 **admin_platform.sql 附录**。此为浏览器端限制，无法替代后端鉴权。

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

**Q：Git 仓库？**  
A：https://github.com/wushij/wu-admin

---

## 相关文档

- [GitHub 上传与推送](./GitHub上传与推送全流程.md)

---

## 许可证

本项目仅供学习与内部使用。生产部署前请修改默认密码、数据库与 Redis 等敏感配置。
