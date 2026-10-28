---
name: backend-development
description: 生产级 Java/Spring 后端工程方法论，涵盖代码组织与包分层、业务建模、API 设计、事务边界、数据一致性、并发与幂等、外部副作用、缓存、MQ、定时任务、可观测性、性能与测试。用于后端业务开发、新增或修改 API、订单／支付／审核等状态流转、并发与幂等设计、后端重构与 Code Review。单纯的 DDL、索引、SQL 深度优化请改用 mysql-design。
---

# 高级后端工程（Senior Backend Engineering）

<Purpose>
本 Skill 用于生产级后端业务开发。

目标不是：

"把接口写出来"。

而是：

正确理解业务
+
建立清晰边界
+
保证数据一致性
+
处理异常与并发
+
控制副作用
+
保证可观测性
+
兼顾性能
+
保证可测试
+
降低长期维护成本

最终交付必须同时满足：

Business Correct
+ Data Correct
+ Concurrency Safe
+ Failure Safe
+ Observable
+ Maintainable
+ Production Ready
</Purpose>

<Use_When>
适用于以下后端开发任务：

- Java / Spring Boot 业务开发
- Controller / Application / Service / Domain 开发
- 新增或修改 API
- 业务流程实现
- 订单 / 支付 / 会员 / 审核 / 库存等状态流转
- Transaction 设计
- 并发业务
- 幂等设计
- Cache 使用
- MQ / Event Driven 业务
- Scheduled Job / XXL-Job
- 第三方接口集成
- WebSocket / 长连接 / 实时通信
- Backend Refactor
- Backend Code Review
- 性能优化
- 生产级异常处理
- 数据一致性处理
</Use_When>

<Do_Not_Use_When>
跳过完整流程的平凡修改：

- Typo
- 单一常量修改
- 明确的字段名称调整
- Isolated one-line fix
- 单纯 UI / CSS
- 单纯 DDL / Index 设计（应使用 mysql-design）
- 单纯服务器部署
</Do_Not_Use_When>

<Why_This_Exists>
AI 生成的后端代码常见问题：

- "接口能调通" = 任务完成
- Controller → Service → Mapper → Finish
- 不考虑并发
- 不考虑幂等
- 不考虑事务边界
- 不考虑外部副作用
- 不考虑状态流转
- 不考虑失败路径
- select → 判断 → update（假装线程安全）
- @Transactional 当作"保险按钮"
- 看到并发立即 Redis 分布式锁
- 随意使用公共 ForkJoinPool
- 日志只有 "failed"
- 只测试 Happy Path
- 异常吞掉只打日志
- 随意修改生产接口
- 所有逻辑堆在单个 Service / 单个方法
- 不同业务类平铺在同一包

本 Skill 通过系统化工程方法论消除这些问题。
</Why_This_Exists>

<Workflow>
非平凡后端任务默认执行：

1. **Requirement Understanding** — 理解业务需求
2. **Codebase Audit** — 审计现有代码基线
3. **Business Invariant Analysis** — 识别业务不变量
4. **Domain / Boundary Analysis** — 明确领域边界
5. **Data & State Transition Analysis** — 分析数据和状态流转
6. **Consistency / Transaction Design** — 设计一致性和事务边界
7. **Concurrency / Idempotency Analysis** — 并发和幂等分析
8. **Failure Path Analysis** — 失败路径分析
9. **代码组织与分层确认（Package & Layering Decision）** — 确定每个类应落在哪一层（见第 26 节）
10. **Implementation** — 实现
11. **Test / Build** — 测试和构建
12. **Senior Backend Review** — 高级后端审查
13. **Fix P0 / P1** — 修复关键和重大问题
14. **READY** — 所有标准满足

不要为了流程而产生大量文档。

但这些判断必须在实现中体现出来。
</Workflow>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 核心工程规则                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Core_Engineering_Rules>

## 1. Pre-Implementation Audit（Existing Baseline First）

修改前必须先阅读现有代码基线。

**Existing Baseline First（现有基线优先）：**

优先复用项目已有能力，禁止因为"更优雅"而在同一项目内制造第二套平行基础设施。

**至少检查：**
- Controller
- Service / Manager / Domain
- Mapper / Repository
- DTO / VO / Entity
- Converter
- **Existing Exception（现有统一异常体系）**
- **Existing Response（现有统一响应包装）**
- Existing Enum
- **Transaction usage（现有事务模式）**
- **Cache usage（现有缓存模板）**
- **Permission（现有 @DataScope / @Anonymous）**
- MQ/Event usage
- Scheduler
- Existing API Contract
- Existing Tests
- Existing Config
- Common Utilities
- **当前模块的包结构与分层约定**

**同时确认：**
- 当前模块的职责
- 上下游调用关系
- 当前数据模型
- 当前状态模型
- 当前异常体系
- 当前代码风格
- 当前公共能力
- **当前模块使用的 ORM 风格（XML 映射 or 注解式）**

**优先复用现有能力。**

**禁止未审计代码基线就重新创建：**

Result / Response
Exception
Lock
Retry
Cache Wrapper
MQ Wrapper
Converter
ThreadPool
HTTP Client
Permission Annotation

**审计结论应当回答一个问题：这个项目已经提供了哪些基础设施？**

典型的基础设施清单（按项目实际情况记录）：
- 统一响应包装与统一异常类型
- 全局异常处理器
- 重复提交 / 限流 / 操作日志 / 数据权限等注解
- 缓存模板与缓存 key 规范
- 数据源与连接池配置
- 线程池、HTTP Client、MQ 封装
- 对象转换器（Converter / Mapper Struct）

**不要一个需求造一套基础设施。**

**如果审计发现项目已有同类能力：**

复用。即使它不够优雅、命名不合你的习惯、封装层次不理想。改造既有基础设施是独立的重构任务，不能顺手夹带在业务需求里。

**如果项目确实没有该能力：**

先确认这是不是真的缺失（搜索关键词、查看依赖、询问用户），再创建最小实现，并放在与项目既有能力一致的位置。

## 2. Business Invariant First

任何核心业务开发前，必须先识别 Business Invariant（业务不变量）。

**例如订单：**
- 已支付订单不能再次支付
- 已取消订单不能支付
- 已退款金额不能超过实付金额

**会员：**
- 有效期不能无理由倒退
- 重复支付不能重复增加权益

**审核：**
- 已审核记录不能被旧请求覆盖
- 状态流转必须符合合法路径

**库存：**
- 可用库存不能小于 0

**业务规则必须由代码显式保护。**

禁止仅依赖：

"正常情况下前端不会这么调用"。

前端不是业务安全边界。

## 3. 状态机意识

凡是存在：

status / state / phase / audit_status / payment_status / order_status

都必须主动判断：

这是不是一个状态机问题。

**不要只写：**
```java
status = "SUCCESS"
```

**需要考虑：**

当前状态
→ 允许进入哪些状态
→ 谁能触发
→ 什么条件允许
→ 重复触发怎么办
→ 非法状态怎么办

**例如：**
```
CREATED
  → PAYING
  → PAID
  → REFUNDING
  → REFUNDED

或：

CREATED
  → CANCELLED
```

禁止：

任意状态直接更新成任意状态。

对重要业务应明确合法 Transition。

## 4. Transaction Boundary

@Transactional 不是"保险按钮"。

**使用事务前必须明确：**

事务保护的业务不变量是什么。

**事务范围应该：**

尽量小
+
包含必须原子提交的数据修改

**避免在长事务中执行：**
- HTTP 调用
- RPC
- 文件上传
- 大量循环
- MQ 阻塞等待
- 慢查询

**必须考虑：**
- Rollback 条件
- Checked Exception
- 自调用导致事务失效
- Proxy Boundary
- Propagation
- Isolation
- 锁等待
- Deadlock
- Transaction Timeout

**禁止：**

为了"安全"给整个 Service 全部加 @Transactional。

## 5. 数据库一致性

后端开发必须考虑：

读取到的数据是不是最新的？
更新是否可能覆盖别人的修改？
写入顺序是否重要？
失败后是否存在半完成状态？

**常见手段包括：**
- Unique Constraint
- Conditional Update
- Optimistic Lock
- Pessimistic Lock
- Transaction
- State Guard
- Version
- CAS

**不要默认：**

```java
select
→ 判断
→ update
```

在并发场景下一定安全。

**例如：**
```java
if (stock > 0) {
    stock--;
}
```

不等于线程安全。

必须分析数据库层最终是否能够保证正确性。

## 6. Concurrency

遇到：

抢购 / 支付 / 库存 / 重复提交 / 审核 / 任务调度 / 余额 / 积分 / 券 / 权益

必须主动进行并发分析。

**需要判断：**
- 是否存在并发写
- 是否存在 Lost Update
- 是否可能重复消费
- 是否需要 Lock
- Lock 粒度是什么
- 锁竞争是否可接受
- 是否可以利用数据库原子更新
- 是否真的需要 Distributed Lock

**禁止：**

看到并发
→ 第一反应 Redis Distributed Lock。

**优先寻找最小正确方案。**

**例如：**

Unique Constraint
Conditional Update
CAS
Optimistic Lock
Database Atomic Operation
Version
Pessimistic Lock

能够解决时，不要引入不必要的分布式锁。

**如果确需分布式锁，必须分析：**
- Lock Key
- Lock Scope
- Timeout
- Lease
- Release Safety
- Failure
- Contention

## 7. Idempotency

所有可能被重复调用的核心操作，都必须考虑幂等。

**典型场景：**
- 创建订单
- 支付回调
- 退款回调
- MQ Consumer
- Retry
- Scheduler
- Third-party Callback
- User Double Click

**必须明确：**

幂等 Key 是什么？

幂等状态存在哪里？

重复请求返回什么？

正在处理中怎么办？

第一次失败后是否允许重新执行？

**禁止简单认为：**

"接口前端只调用一次"。

支付回调、MQ 等场景尤其必须默认：

可能重复执行。

## 8. External Side Effect

调用外部系统时必须识别 Side Effect。

**例如：**
- 支付
- 发短信
- 发邮件
- 发券
- MQ
- 文件上传
- 调第三方 API

**需要区分：**

本地数据库事务

和：

外部副作用

不是同一个事务。

**必须考虑：**

DB 成功 + 第三方失败

第三方成功 + DB 失败

Timeout 但实际对方成功

Retry 导致重复操作

**禁止假装：**

@Transactional

能够回滚外部 HTTP / MQ / SMS。

## 9. Distributed Consistency

跨系统业务需要根据场景选择一致性方案。

**可以考虑：**
- Local Transaction + Outbox
- Reliable Event
- Retry
- Compensation
- Reconciliation
- State Machine
- Transaction Message
- TCC / Saga

**但禁止：**

为了显得高级，简单业务直接引入复杂分布式事务。

**优先遵循：**

业务可接受的一致性级别。

**需要区分：**

Strong Consistency
Eventual Consistency

**并明确：**

如果最终一致，系统通过什么机制最终收敛。

## 10. MQ / Event Driven

**如果项目使用 MQ，必须明确：**

**Producer：**
- 什么时候发送
- DB 和消息之间如何保证一致
- Send Failure 怎么办

**Consumer：**
- 是否可能重复消费
- 幂等如何保证
- Retry 策略
- Poison Message
- Dead Letter
- 消费失败是否影响后续消息
- 顺序性是否真的需要

**禁止假设：**

MQ = Exactly Once。

**默认应该认为：**

消息可能：

重复 / 延迟 / 乱序 / 丢失后重试

业务必须能够承受设计允许范围内的这些情况。

## 11. Cache

Cache 是性能优化工具，不是默认数据源。

**使用缓存前必须明确：**
- 为什么需要缓存
- Cache Key
- TTL
- 更新策略
- 删除策略
- 一致性要求
- Cache Penetration
- Cache Breakdown
- Cache Avalanche
- Hot Key

**需要判断：**

Cache Aside
Read Through
Write Through
Local Cache
Distributed Cache

是否符合场景。

**禁止：**

"数据库查询慢一点"
→ 直接上 Redis。

也禁止为了追求绝对一致性，把缓存体系设计得比业务本身更复杂。

## 12. API Design

API 设计必须围绕业务语义。

不要简单把数据库 CRUD 暴露出去。

**需要关注：**
- Resource / Action 语义
- Request DTO
- Response VO
- Validation
- Pagination
- Sorting
- Filtering
- Error Code
- Idempotency
- Backward Compatibility

**Controller 应保持轻量。**

**Controller 主要负责：**

协议转换
参数校验
认证上下文获取
调用业务层
返回协议结果

禁止把大量核心业务逻辑写进 Controller。

## 13. DTO / Entity / VO Boundary

禁止所有层共用一个对象。

**Entity：**

用于 Persistence。

**Request DTO：**

用于输入协议。

**Response VO：**

用于输出协议。

**Domain Object：**

根据项目实际需要使用。

**不要机械追求：**

Entity / DTO / BO / DO / PO / VO / Command / Query

全部齐全。

**对象数量应服务于：**

边界隔离
语义清晰
变化控制

而不是为了所谓分层而分层。

## 14. Exception Handling

异常必须有业务语义。

**区分：**

Business Exception
Validation Error
Infrastructure Error
Third-party Error
System Error

**禁止：**
```java
catch (Exception e) {
    return false;
}
```

**也禁止：**

catch 后只打印日志然后吞掉异常。

**需要明确：**

谁负责处理？
是否重试？
是否回滚？
用户应该看到什么？
日志应该记录什么？

日志中保留 Root Cause。

## 15. Validation

参数校验分两层：

**Protocol Validation**

例如：

@NotNull
@NotBlank
@Size

**Business Validation**

例如：

订单是否存在
订单是否属于当前用户
状态是否允许操作
金额是否一致
库存是否足够

**禁止把业务规则全部塞进 Bean Validation。**

**也禁止：**

仅依赖前端校验。

## 16. Security Boundary

后端默认不信任客户端输入。

**必须检查：**

Authentication
Authorization
Data Ownership
Horizontal Privilege Escalation
Vertical Privilege Escalation
Sensitive Data
Input Validation
File Upload
Injection
Mass Assignment

**例如：**

```
GET /order/detail?id=1001
```

不能因为 ID 合法，就允许当前用户查看。

还要确认：

order.userId == currentUserId。

## 17. Performance

性能优化必须基于证据。

**重点检查：**
- **N+1 Query** — 循环中重复查询
- **Large Result Set** — 未分页的大量数据返回
- **Missing Pagination** — 应该分页但未分页
- **Repeated RPC** — 同一请求内重复调用外部服务
- **Repeated DB Query** — 可以批量但单条查询
- **Batch Access** — 应该用 IN/批量但用循环单查
- **Lock / Transaction** — 长事务、锁等待
- Serialization Cost
- Large Object
- Unbounded Collection
- Thread Pool
- Lock Contention
- Slow Transaction

**职责边界：** 本 Skill 负责识别业务层面的数据访问问题（N+1、缺少分页、循环单条查询、重复查询、事务与锁使用不当）。

全表扫描判定、索引设计、EXPLAIN 分析、执行计划、慢 SQL 深度优化由 `mysql-design` 负责，本 Skill 不重复展开。

**禁止：**

Premature Optimization。

同时禁止明显的生产性能风险进入主干。

## 18. Thread Pool / Async

**禁止随意：**

```java
CompletableFuture.runAsync()
```

使用公共 ForkJoinPool 承担生产业务任务。

**涉及异步时必须考虑：**
- Executor
- Queue Capacity
- Core / Max Threads
- Rejection Policy
- Context Propagation
- MDC
- Trace
- Exception Handling
- Shutdown
- Back Pressure

线程池不是：

"让接口变快"的按钮。

## 19. Scheduler / Job

定时任务必须考虑：

- 是否会重复执行
- 多实例是否同时执行
- Job Idempotency
- Retry
- Timeout
- Misfire
- Batch Size
- Pagination
- Checkpoint
- Failure Recovery

**禁止：**

一次性 SELECT 全表
→ for 循环处理几十万数据。

**长任务应考虑：**

Batch
Cursor / Pagination
Checkpoint
Split

## 20. Realtime / Long Connection

**如果项目包含实时通信（WebSocket / Netty / Server-Sent Events / Long Polling），必须考虑：**

**连接管理：**
- Channel / Connection 注册与注销
- 连接超时
- 心跳机制
- 重连策略
- 连接池管理

**消息处理：**
- 消息顺序性
- 消息幂等性
- 消息确认（ACK）
- 消息重试
- Dead Letter / 失败处理

**状态同步：**
- 在线状态维护
- Session 管理
- 跨节点状态共享（如 Redis）
- 断线重连后状态恢复

**资源管理：**
- Channel / Connection 资源释放
- 内存泄漏风险
- EventLoop / 工作线程占用
- Backpressure 控制

**安全：**
- Token / Session 校验
- 权限验证
- 消息签名（如需要）
- 频率限制 / 防刷

**典型场景：**
- IM / 聊天
- 推送通知
- 实时数据更新
- 游戏 / 协作应用
- 监控面板

## 21. Observability

生产级业务必须可追踪。

**核心路径应具备：**
- Structured Log
- Trace ID
- Business ID
- Error Context
- Important State Transition

**例如支付问题：**

日志不能只有：

payment failed

应该能够定位：

orderId
paymentOrderId
userId
channel
currentStatus
requestId
thirdPartyCode

**但禁止在日志中输出：**

Password
Token
Full ID Card
Sensitive Personal Data

## 22. Logging

日志不是越多越好。

**INFO：**

关键业务状态变化。

**WARN：**

可恢复但异常的业务情况。

**ERROR：**

真正需要人工关注的失败。

**DEBUG：**

诊断细节。

**禁止：**

循环中大量 INFO
完整打印超大对象
完整打印敏感 Request
正常业务分支 ERROR

## 23. Testing

根据业务风险决定测试层级。

**优先测试：**

Business Invariant
State Transition
Boundary Condition
Concurrency-sensitive Logic
Idempotency
Failure Path

**不要只测：**

Happy Path。

**核心业务至少思考：**

正常成功
重复调用
非法状态
不存在数据
权限错误
第三方失败
超时
边界值

测试数量不是目标。

保护业务规则才是目标。

## 24. Backward Compatibility

修改已有生产接口时必须考虑：

Old Client
Existing Data
Existing Consumer
Existing MQ Message
Existing Enum Value
Database Compatibility

**禁止随意：**

删除字段
修改字段语义
修改枚举值含义
修改 API Response Structure

如果需要破坏性变更，必须显式说明。

## 25. Refactoring Boundary

开发新需求时禁止：

"顺便把整个模块重构一下"。

**必须区分：**

Required Refactor
Nice-to-have Refactor

**只有：**

当前实现已经阻塞正确开发

或者：

存在明显 P0/P1 设计问题

才扩大重构范围。

不要为了代码洁癖扩大 Scope。

## 26. 代码组织与目录分层（Code Organization & Package Layout）

业务正确性达标，不等于工程结构达标。
代码放在哪个包、职责由谁承担、一个类承担多少，属于同等重要的交付标准。

**三条铁律：**

```
1. 禁止平铺（No Flat Layout）
2. 必须分层（Layered by Responsibility）
3. 单一职责（One Class, One Job）
```

### 1. 禁止平铺

把大量分属不同业务的类直接堆在同一目录，属于架构违规。

**禁止：**
```
service/
├── UserService.java
├── RoleService.java
├── CourseService.java
├── OrderService.java
├── PayService.java
├── StockService.java
├── CouponService.java
├── ReviewService.java
├── NotifyService.java
└── ReportService.java
```

**正确：** 先按业务领域（Maven 模块 / 业务包）划分，再在每个领域内部按职责分层。

```
user/
├── controller/
├── service/
├── dao/
├── mapper/
├── entity/
├── dto/
├── vo/
└── converter/
```

领域继续膨胀时，必须继续下钻二级业务模块，而不是把新类继续丢进同一目录：

```
order/
├── create/
├── pay/
├── cancel/
├── refund/
└── query/
```

**判断标准：** 当同一目录中的类已经分属多个不相关业务，说明该拆目录，而不是"再放一个进去"。

### 2. 必须分层

分层指按职责划分包与类，且调用方向单向收敛：

```
Controller
  → Service
  → Domain / DAO
  → Mapper
  → Entity
  → Database
```

**各层职责边界：**

```
controller    接收请求、参数校验、调用 Service、返回统一响应
service       业务编排、事务边界、领域规则、跨模块 API 调用
dao           业务层统一数据入口，可组合多个 Mapper
mapper        单表 / 单 SQL 数据访问
entity        数据库映射，禁止作为出入参
dto / vo      输入协议 / 输出协议
converter     对象转换
```

反向依赖必须通过 VO / Converter 或模块 API 完成，禁止把 Entity 或 Mapper 暴露给上层或跨模块。

分层的目的不是增加文件数量，而是让每一层的变化原因保持单一。

**如果宿主项目已有自己的分层约定：**遵循项目既有约定，本节的层名只是通用参照。

### 3. 单 Maven 与多 Maven 工程落地范式（Architecture Selection）

根据工程架构形态，团队应统一遵循以下两套分层标准，禁止层级变形与随意命名：

#### 形态一：单 Maven 业务模块标准骨架（7 包黄金模型）
适用于：中大型单体管理系统、敏捷交付项目。特点是**职责外显、无多余套娃、开发效率最高**。

```
<module>/
├── controller/        # 仅 HTTP 入出口，协议转换与参数基础校验，禁止写业务分支
├── dto/               # 前端入参：表单提交、查询条件，打参数校验注解
├── vo/                # 前端出参：展示用数据，必须裁剪物理字段、敏感信息脱敏
├── service/           # 业务逻辑接口契约，单一职责
│   └── impl/          # 业务核心实现类
├── dao/               # 【按需出现】仅在复杂模块存在：负责多级缓存收拢、多表数据聚合
├── mapper/            # MyBatis-Plus 单表持久化接口，纯 SQL 操作
├── entity/            # 与数据库物理表一一对应，严禁直接返回前端
└── converter/         # DTO / Entity / VO 互转与数据清洗
```

#### 形态二：多 Maven 独立子工程标准骨架（9 包工业级模型）
适用于：多子模块物理硬隔离、微服务预备工程。特点是**强防腐、杜绝编译循环依赖、随时无缝微服务化**。

```
<project>-<module>/
└── src/main/java/com/<project>/<module>/
    ├── api/               # 跨模块内部调用契约实现（供兄弟模块内部调用的纯 Java 接口/RPC，打破编译循环依赖）
    ├── controller/        # 面向前端的 HTTP / RESTful 入出口，面向外部客户端
    ├── dto/               # 前端入参数据传输对象
    ├── vo/                # 前端出参视图展示对象
    ├── service/           # 核心业务逻辑编排契约（面向接口编程）
    │   └── impl/          # 业务具体实现类
    ├── dao/               # 常驻数据防腐门面：收拢缓存、多表动态 SQL，保护业务层不受底层表结构干扰
    ├── mapper/            # 底层持久化映射（MyBatis-Plus BaseMapper）
    ├── entity/            # 数据库物理表实体
    └── converter/         # DTO / Entity / VO 映射转换器
```

#### 两者核心裁剪与取舍原则：
1. **单 Maven 砍掉内部调用 `api/`**：兄弟模块在同一进程和 Classpath 下，直接依赖注入 Service，避免接口与实现类多写一套；
2. **单 Maven 的 `dao/` 采用“按需出现”**：80% 单表常规 CRUD 业务允许 Service 直接调用 Mapper，杜绝写纯透传的空壳 DAO；
3. **多 Maven 必须常驻 `api/` 与 `dao/`**：`api` 彻底解决多 Maven 模块间循环依赖死锁并支持平滑微服务化，`dao` 筑牢数据访问防腐墙。

### 4. 禁止把所有逻辑写在一个类里

**禁止：**
- 一个 `XxxService` 同时承担参数处理、业务规则、SQL、事务、缓存、外部调用、结果拼装
- 一个实现类明显超出项目约定的文件行数上限
- 一个方法把多个业务步骤顺序内联
- 用 `CommonUtils` / `XxxHelper` / `XxxManager` 作为兜底容器承接无处安放的逻辑

**出现以下信号，必须按职责拆分，而不是继续追加代码：**

```
□ 单个类里出现了两个以上互不相关的业务概念
□ 单个方法超过项目行数约定
□ 类名无法用一句话说清职责
□ 新增功能只能继续往同一个类里加方法
□ 同一个类同时依赖 Mapper、缓存、MQ、外部 SDK
□ 用 else if 分支树描述业务类型（考虑策略 / 状态机）
```

**按业务职责拆，而不是按"一个方法一个类"拆：**

```
OrderCreateService   下单主流程
OrderPaymentService  支付处理
OrderRefundService   退款处理
OrderQueryService    订单查询
```

**不是：**
```
OrderHelper
OrderManager
OrderUtil
OrderProcessor
```

拆分不得与本 Skill 第 25 节冲突：只调整当前需求涉及的结构，不顺手重构无关模块。

### 5. 交付前自检

```
□ 新增类是否放进了正确的业务领域目录，而不是丢进公共目录
□ 目录中的类是否仍属于同一业务领域
□ Controller 是否只做协议层工作
□ 数据访问是否符合规范（多 Maven 架构或复杂多表/缓存必须通过 DAO；单 Maven 简单单表允许直接注入 Mapper）
□ 是否把所有逻辑堆在单个类或单个方法中
□ 是否又制造了 XxxUtil / XxxHelper 这类无边界容器
□ 新增目录是否有明确职责，是否与既有目录重复
```

**违反上述规则时：**不要为了快速交付继续扩大错误结构。优先把当前功能涉及的部分调整为正确结构，再继续开发。

## 27. Design Pattern / Abstraction

高级后端工程允许并鼓励合理使用设计模式。

设计模式的目标是：

降低真实业务复杂度
+
隔离变化
+
提升扩展性
+
减少重复分支
+
提高代码语义

而不是增加文件数量。

遇到以下情况，应主动评估设计模式：

### Strategy

当同一业务能力存在多种可替换算法 / 渠道 / 规则，例如：

- Payment Channel
- Discount Rule
- Recommendation Strategy
- Competition Format
- Notification Channel

如果逻辑会继续扩展，优先考虑 Strategy。

### Factory

当对象或策略的创建过程：

- 存在类型选择
- 创建逻辑复杂
- 调用方不应关心具体实现

可以考虑 Factory / Registry。

避免调用方出现大量：

```java
if (type == A)
else if (type == B)
else if (type == C)
```

### Template Method

当多个业务流程：

整体流程一致，但局部步骤存在差异，

例如：

validate
→ prepare
→ execute
→ postProcess

可以考虑 Template Method。

### Chain of Responsibility

当一个请求需要经过多个独立处理节点：

Validation
→ Risk Check
→ Permission
→ Business Rule
→ Enrichment

并且节点：

可以扩展
可以组合
可以按顺序执行

可以考虑责任链。

### State / State Machine

当业务存在：

复杂状态
+
大量状态转换规则
+
不同状态具有不同行为

可以考虑 State Pattern / State Machine。

但简单状态判断不要强行状态模式化。

### Adapter

第三方系统接入时：

External Model
≠
Internal Domain Model

优先通过 Adapter 隔离外部协议。

不要让：

支付宝 DTO
微信 DTO
第三方 SDK Object

直接污染核心业务层。

### Observer / Domain Event

当一个核心业务动作完成后：

多个后续动作不应该强耦合在主流程中，

例如：

Order Paid
→ Member Growth
→ Coupon
→ Notification
→ Analytics

可以考虑 Event / Observer。

但必须结合：

事务一致性
消息可靠性
失败补偿

不能因为用了事件就忽略一致性问题。

### Pattern Decision Rule

使用设计模式前回答：

1. 当前是否真的存在变化点？
2. 这个变化点未来是否可能继续扩展？
3. 当前 if/else 是否已经开始影响维护性？
4. 模式是否让业务语义更加清晰？
5. 新增一个实现时是否能减少修改已有代码？
6. 引入模式后的理解成本是否低于原来的复杂度？

如果答案大部分为否：

保持简单。

如果答案大部分为是：

应主动进行合理抽象。

**原则：**

```
Simple Business
→ Simple Code

Growing Complexity
→ Appropriate Abstraction

Stable Variation Point
→ Design Pattern
```

**业务语义优先于模式名称。**

不要为了证明使用了模式而创造：

AbstractStrategyFactoryManager
CommonProcessorFactory
BaseHandlerManager

**优先使用业务语言：**

PaymentProcessor
DiscountPolicy
QualificationRule
OrderValidator
RefundHandler
NotificationSender

设计模式可以存在，但代码首先应该表达业务。

## 28. ORM 使用（项目既有约定）

**重要：ORM 风格是模块既有约定，不是根据场景自由选择。**

**一个项目内可能存在多种并存的 ORM 风格。**

例如同一系统的不同模块，有的用 XML 映射、有的用注解式 ORM。这通常是历史演进的结果，属于既有架构事实。

**审计时记录每个模块的实际 ORM 风格：**
- 使用 XML 映射还是注解式 ORM
- 是否使用了代码生成器，生成产物放在哪里
- 分页、批量、多表关联分别由什么承载

**新增功能：**
- 遵循所在模块已有的 ORM 风格

**禁止：**
- 在既有模块中引入该模块未使用的 ORM（除非明确架构迁移任务）
- 因为"某场景更适合某 ORM"而打破模块约定
- 顺手把既有模块迁移到"更现代"的 ORM

**原则：**

模块一致性 > 场景适配性

保持架构统一 > 局部最优选择

**如果项目只使用一种 ORM：** 遵循项目约定。

**如果所在模块允许在同一模块内并存多种 ORM 风格，且这是该模块的既有约定：** 按既有分工使用，不要新增第三种风格。

**禁止：** 以"这个场景更适合某种 ORM"为理由，在既定模块内自行切换或引入 ORM 风格。哪怕是局部最优选择，也属于架构决定，不是业务需求的一部分。

</Core_Engineering_Rules>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- Senior Backend Review                                                        -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Senior_Backend_Review>

实现完成后，把这份实现当作另一位高级工程师写的代码来审查，不要因为是自己刚写的就假设正确。

**重新 Review：**

Business Correctness
Domain Boundary
State Transition
Transaction
Consistency
Concurrency
Idempotency
Failure Path
External Side Effect
Cache
MQ
API
Exception
Security
Performance
Observability
Testing
Maintainability
Compatibility
Package Layout & Layering

**问题分级：**

**P0（Critical）：**
- 数据错误
- 资金错误
- 权限漏洞
- 核心业务不可用
- 严重数据一致性问题
- 严重并发错误

**P1（Major）：**
- 明确业务边界遗漏
- 幂等缺失
- 事务边界错误
- 明显性能风险
- 异常路径不可恢复
- 明显维护风险
- 所有逻辑堆在单个类或方法中
- 业务类平铺在同一包 / 目录职责混杂

**P2（Minor）：**
- 代码风格
- 非关键抽象优化
- 可选性能优化
- 轻量重构建议

**P0 / P1 必须处理。**

P2 不允许无限扩大当前任务。

</Senior_Backend_Review>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- READY 标准                                                                   -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<READY_Definition>

只有同时满足以下条件才能 READY：

**一、Review 维度全部通过**

以 `<Senior_Backend_Review>` 列出的维度为准，逐项确认无 P0 / P1：

```
Business Correctness / Domain Boundary / State Transition / Transaction
Consistency / Concurrency / Idempotency / Failure Path / External Side Effect
Cache / MQ / API / Exception / Security / Performance / Observability
Testing / Maintainability / Compatibility / Package Layout & Layering
```

**二、交付条件**

✅ **Validation Complete** — 参数校验与业务校验完整

✅ **Build / Test Passed** — 构建/测试通过

✅ **No P0** — 无 P0 问题

✅ **No P1** — 无 P1 问题

**核心语义：**

```
Build Passed != Backend Accepted.

"接口能调通"
也不等于：
Production Ready。
```

</READY_Definition>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 与其他 Skills 协作                                                          -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Collaboration_With_Other_Skills>

**职责边界：**

```
ui-design
= What should it look like?（视觉设计决策）

senior-frontend
= How should frontend be built correctly?（前端工程实现）

backend-development
= How should backend business be built correctly?（后端业务开发）

mysql-design
= How should data model and queries be designed?（数据模型与查询设计）
```

**backend-development 负责：**

- 业务如何使用数据
- Transaction / Consistency / State Transition
- Repository / Mapper 调用方式与数据访问边界
- Concurrency Semantics
- 识别 N+1 Query、缺少分页、批量访问机会、重复查询

**mysql-design 负责：**

- Data Model / Table Design、Column / Data Type
- Index / Constraint、DDL
- EXPLAIN / Execution Plan、Query Rewrite、SQL 深度优化

**协作场景：**

```
新业务数据模型：mysql-design（DDL / Index）→ backend-development（业务使用）

纯 Java 业务：backend-development

SQL 性能：backend-development（识别问题）→ mysql-design（Index / EXPLAIN / Rewrite）
```

</Collaboration_With_Other_Skills>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 最终注释                                                                     -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Final_Notes>

## 本 Skill 的定位

**backend-development 负责：**

高级后端业务工程（How to build correctly）

**不负责：**

数据库设计（mysql-design）
前端实现（senior-frontend）
UI 设计（ui-design）

## 适用边界

**本 Skill 覆盖：** 后端业务正确性、事务与一致性、并发与幂等、外部副作用、安全与校验、可观测性、性能风险识别、代码组织与分层。

**本 Skill 不覆盖：** 数据库 DDL 与索引设计、SQL 深度优化（使用 `mysql-design`）、前端实现（使用 `senior-frontend`）、视觉设计决策（使用 `ui-design`）。

涉及上述内容时，本 Skill 只负责识别问题并交接口，不重复展开对应方法论。

## 兼容性

本 Skill 设计为跨项目通用：
- 商城
- 赛事
- SaaS
- 企业系统
- 后台管理
- 即时零售
- AI Application
- 普通业务系统

核心工程方法论保持项目无关。

不绑定任何具体项目、框架版本或组织内部的命名约定。

</Final_Notes>
