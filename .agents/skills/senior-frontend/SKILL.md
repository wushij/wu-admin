---
name: senior-frontend
description: 生产级前端工程实现方法论，涵盖目录分层与代码组织、布局工程、响应式实现、CSS 质量、交互状态、动效工程实现、图标体系与视觉验收。用于新页面开发、设计稿或截图还原、页面重构与视觉优化、Responsive 调整、CSS 与布局问题排查、前端 Code Review。视觉方向与设计系统的定义请改用 ui-design。
---

# Senior Frontend Engineering

<Purpose>
本 Skill 定义了 Senior Frontend Engineering 的完整工作流程和质量标准，确保 AI 生成的前端代码达到生产级标准：功能正确、视觉成熟、响应式、可维护、且经过正确验收。

**职责边界：**
- **senior-frontend（本 Skill）：** How should we build it correctly?（如何正确构建）
- **ui-design（设计 Skill）：** What should it look like?（应该如何呈现）

本 Skill 负责实现设计，而非创造设计。优先消费现有设计输入（Design/Screenshot/Design System/ui-design 输出）。

这不是关于教 AI 如何写 Vue/React 语法，而是建立从设计到实现的工程流程。
</Purpose>

<Use_When>
适用于非平凡前端任务：
- 新前端页面开发
- UI 页面实现
- 截图/设计稿还原
- 页面重构或视觉优化
- Responsive 设计调整
- CSS/Layout 问题排查
- Component 设计决策
- 前端架构调整
- 页面交互状态实现
- 前端性能优化
- Visual Review
- 前端 Code Review
</Use_When>

<Do_Not_Use_When>
对于平凡修改跳过完整流程：
- 拼写错误修复
- 单行文本修改
- 常量调整
- 单个类型错误修复
- 明确孤立的单行修复

避免：改一个字符串却重新审计整个前端架构。

同样跳过：
- Backend 任务
- 纯 API 开发
- 数据库操作
- 非 UI 工作
</Do_Not_Use_When>

<Why_This_Exists>
AI 生成的前端代码常见问题：
- "能用就行"心态，缺乏视觉成熟度
- 基于截图堆砌 HTML/CSS
- 到处是魔法数字
- 过度使用绝对定位
- 随机 margin-left/margin-top/translate 调整
- CSS 补丁无限堆叠
- 桌面端可用但 Responsive 崩溃
- 只实现 Happy Path
- 随意提取 Component
- 过度抽象
- 所有代码塞进单个页面文件
- 不同业务的文件平铺在同一目录
- Typography 不一致
- 间距不一致
- 信息层级混乱
- 卡片堆叠无呼吸感
- 不自然的空白
- 缺乏商业产品光泽
- 用 Emoji 充当按钮、状态、菜单图标，破坏视觉一致性
- "Build passed = 任务完成"假设
- 没有实际 Browser Review

本 Skill 通过系统化工程规范消除这些反模式。
</Why_This_Exists>

<Workflow>
对于非平凡前端任务，执行此工作流程：

1. **实施前审计（Pre-Implementation Audit）** — 理解当前代码库基线
2. **Layout 分析（Layout Analysis）** — 编码前选择布局模型
3. **Component 与状态设计（Component & State Design）** — 规划结构和交互
4. **目录与职责归属（Directory & Responsibility Placement）** — 决定每个文件应落在哪一层（见第 15 节）
5. **实施（Implementation）** — 遵循项目约定编写代码
6. **Type Check & Build** — 验证编译
7. **视觉验收（Visual Verification）** — 在浏览器中打开，测试多视口
8. **Senior Frontend Review** — 像审查别人的代码一样审查
9. **修复 P0/P1 问题** — 处理关键和重大问题
10. **重新验证（Re-verify）** — 确认修复有效
11. **READY** — 所有标准满足

不要为每个步骤生成冗长文档。内部完成它们。仅向用户传达关键发现和决策。
</Workflow>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 核心工程规则                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Core_Rules>

## 1. 实施前审计（Pre-Implementation Audit）

在编写任何非平凡前端任务代码之前，理解当前基线。

**最小检查项：**
- 当前页面（如果修改现有页面）
- 父 Layout
- Router 配置
- Store/State 管理
- API 集成
- 全局 CSS
- Design Tokens
- 现有 Components
- 现有 Composables/Hooks
- Responsive 规则
- Typography 系统
- 间距系统
- 现有图标体系（图标库 / SVG 资源 / 领域图标组件）
- 现有 UI Library
- **现有目录约定与分层规范**

**原则：** 优先复用现有能力。

**绝不在未检查是否已存在的情况下创建：**
- Button, Card, Modal, Tabs, Table, Pagination
- Typography 系统
- Layout 系统
- Design tokens
- 图标组件 / 图标资源（同语义图标必须先查重）

**绝不为每个页面重新发明设计系统。**

## 2. Layout 工程（Layout Engineering）

在实施页面之前，确定布局模型。

**考虑：**
1. 页面主布局结构
2. 主容器最大宽度
3. 页面内边距
4. Flexbox vs CSS Grid
5. 正常文档流
6. Sticky/Fixed 定位
7. absolute 的适当使用
8. 自适应宽度 vs 固定尺寸
9. 内容换行
10. 长文本处理
11. 溢出行为
12. 列数变化（responsive）

**优先使用：**
- 正常文档流（Normal Document Flow）
- Flexbox
- CSS Grid

**仅对真正需要定位的元素使用 `position: absolute`：**
- 遮罩层（Overlays）
- 徽章（Badges）
- 装饰元素（Decorations）
- 浮动控件（Floating controls）
- 特殊定位需求

**禁止模式：**
```css
/* 禁止：用随机偏移强制元素定位 */
.element {
  margin-left: 47px;  /* 为什么是 47？ */
  margin-top: 23px;   /* 为什么是 23？ */
  position: absolute;
  left: 234px;        /* 硬编码匹配截图 */
  top: 567px;
  transform: translate(-13px, 8px);  /* 更多魔法 */
}
```

**禁止思维：**
"截图是 1440px 宽，所以我将硬编码所有宽度来匹配。"

使用响应式、基于流的布局，适应内容和视口。

## 3. UI 根因分析（Root Cause Analysis for UI Problems）

这是**核心规则**。

当发现 UI 问题时：
- 错位
- 过于拥挤
- 过于宽松
- 重叠
- 溢出
- 卡片错位
- 文本换行问题
- Responsive 崩溃
- Footer 错位
- Fixed 元素错位

**不要立即添加 CSS 补丁。**

**首先确定根本原因：**
1. Layout Model 问题？
2. Container Width 问题？
3. Grid Distribution 问题？
4. Flex Distribution 问题？
5. 父元素对齐问题？
6. Typography 问题？
7. Line Height 问题？
8. Content Flow 问题？
9. Responsive Breakpoint 问题？
10. Component Structure 问题？
11. Design Token 问题？
12. DOM Structure 问题？

**如果是结构性问题：修复根本原因。**

**不要无限堆叠补丁：**
```css
/* 反模式：无限补丁 */
.element { margin-top: 10px; }
.element { margin-top: 13px !important; }  /* 仍然错误 */
.element { margin-top: 16px !important; transform: translateY(-2px); }  /* 绝望 */
```

## 4. 视觉层级验证（Visual Hierarchy Verification）

页面实现必须保真设计的信息层级。

**验证项：**
- Page Title 视觉突出
- Section Title 明确区分
- Primary/Secondary/Supporting 内容层级清晰
- 核心数据醒目
- 状态指示器可见
- Primary/Secondary Actions 层级明确

**如果存在设计输入（Design/Screenshot/ui-design）：**
- 必须忠实还原设计的视觉层级
- 不得擅自改变信息优先级

**如果无设计输入：**
- 根据业务逻辑合理建立层级
- 避免所有内容视觉权重相同

**禁止：所有内容看起来一样**
```css
/* 反模式：无层级 */
.title, .content, .label, .value {
  font-size: 14px;
  color: #333;
  font-weight: 400;
}
```

**关键：**
本规则负责验证层级实现正确性，不负责定义层级体系（由 ui-design 负责）。

## 5. Typography 实现（Typography Implementation）

**优先消费现有 Typography System。**

**检查顺序：**
1. 项目 Design Tokens（CSS Variables / SCSS Variables）
2. UI Library 提供的 Typography
3. ui-design 定义的 Typography System
4. 设计稿标注的字体规范

**如果已有 Typography System：**
```
必须复用，禁止随意创建新字号。
```

**验证项：**
- font-size 符合体系
- font-weight 正确
- line-height 合理
- letter-spacing 正确（如有定义）
- text color 符合语义（primary/secondary/muted）
- 长文本换行正常
- Responsive 下字号合理调整

**禁止：**
```css
/* 反模式：随意创建字号 */
.element-1 { font-size: 13px; }
.element-2 { font-size: 15px; }
.element-3 { font-size: 17px; }
.element-4 { font-size: 19px; }
```

**关键：**
本规则负责正确实现 Typography，不负责定义 Typography System（由 ui-design 负责）。

## 6. 间距实现（Spacing Implementation）

**优先消费现有 Spacing System。**

**检查顺序：**
1. 项目 Design Tokens（--spacing-* / $spacing-*）
2. UI Library 提供的 spacing utilities
3. ui-design 定义的 Spacing System
4. 设计稿标注的间距规范

**如果项目已有 Spacing System：**
```
4, 8, 12, 16, 24, 32, 48（及可选扩展档位 20, 40）
```

**必须复用它们。**

无项目既定体系时，以 `ui-design` 第 6 节定义的间距尺度为默认基准（4 / 8 / 12 / 16 / 24 / 32 / 48），必要时再按实际对齐需求增补档位。不要在两个位置各自维护一套默认值。

**如果遇到非标准尺寸（13px, 17px, 19px, 27px）：**

**首先确定来源：**
- 设计规范要求
- 视觉对齐需求
- 浏览器渲染修复
- 现有 Token
- 遗留约束

**如果无设计依据：**
收敛到统一 Spacing System。

**禁止：无理由的随机魔法数字。**

**关键：**
本规则负责正确使用 Spacing Tokens，不负责定义 Spacing System（由 ui-design 负责）。

## 7. Responsive 实现（Responsive Implementation）

**优先消费现有 Responsive 策略。**

**检查顺序：**
1. 项目现有 breakpoints 定义
2. ui-design 定义的 Responsive Information Strategy
3. 设计稿标注的响应式规范
4. UI Library 提供的 responsive utilities

**如果项目已定义 breakpoints：**
```javascript
breakpoints: {
  mobile: 768,
  tablet: 1024,
  desktop: 1440
}
```

**必须复用它们。**

**如果项目无统一标准，Visual Review 必须至少覆盖：**
- 1440 (Desktop)
- 1024 (Tablet)
- 768 (Tablet/Large Mobile)
- 390 (Mobile)

**实现职责：**

根据 ui-design 定义的信息组织策略，使用正确的 CSS 技术实现：
- Media Queries
- Flexbox (flex-wrap, flex-direction)
- CSS Grid (grid-template-columns 调整)
- Container Queries（如适用）
- min/max-width
- Overflow 处理
- Hidden utilities

**表格 Responsive 实现：**
- 横向滚动（Horizontal Scroll）
- 隐藏列（Hidden Columns）
- 紧凑布局（Compact Layout）
- 响应式列优先级

**Header Responsive 实现：**
```
Desktop Navigation → Compact Navigation → Mobile Navigation
```

**移动端交互与安全区实现：**
- 移动端的长内容抽屉、快捷菜单优先按 `ui-design` 第 13 节落地为 `Bottom Sheet` / `Action Sheet`（见第 9 节手势与滚动冲突规范）
- 底部固定操作栏与浮层必须预留安全区：`padding-bottom: env(safe-area-inset-bottom)`

**关键：**
本规则负责技术实现，Responsive Information Strategy 由 ui-design 定义。

## 8. Component 设计（Component Design）

基于以下因素提取 components：
- **可复用性（Reusability）**
- **业务语义（Business Semantics）**
- **独立职责（Independent Responsibility）**
- **复杂度（Complexity）**
- **可维护性（Maintainability）**

**好的示例：**
```
MatchCard
UserProfileCard
OrderStatus
EmptyState
Pagination
```

这些具有清晰的业务或通用语义。

**避免创建大量：**
```vue
<Box />
<Wrapper />
<Container1 />
<Container2 />
<LeftBox />
<RightBox />
<TopWrapper />
```

没有清晰职责的 components。

**同样禁止：**
"为了节省几十行代码而将所有内容提取为 components。"

**Component 设计目标：**
- 高内聚（High cohesion）
- 低耦合（Low coupling）
- 语义清晰（Clear semantics）

不是：Components 越多 = 工程越好。

**关键：**
本规则负责代码组件架构，Component Visual Language（视觉规范）由 ui-design 定义。

## 9. 交互状态（Interaction State）

页面不能只实现 Happy Path。

**根据业务场景检查：**
- Loading
- Empty
- Error
- Disabled
- Hover
- Active
- Focus
- Submitting
- Success
- Failed
- Permission Denied

**示例 - 请求流程：**
```
Request
  → Loading
  → Success / Empty / Error
```

**提交流程：**
```
Click
  → Submitting（防止重复提交）
  → Success / Failed
  → Recover / Retry
```

**危险/破坏性操作二次确认与撤销分级（Destructive Action Confirmation & Undo - 强制铁律）：**
- **严禁单击直接删除或执行不可逆操作**：任何涉及**永久删除、批量删除、清空、作废、注销、停用、重置密码/数据**等高风险操作，绝不允许点击后直接调用 API 执行！
- **轻量可逆操作 → 优先 `Snackbar` 撤销**：对于移至回收站、归档等可恢复操作，优先执行后展示带「撤销 / 重试」按钮的 `Snackbar`，避免频繁弹窗打断用户。
- **高风险不可逆操作 → 必须 `Alert Dialog` 阻断确认**：
  ```
  Click Delete/Clear/Revoke
    → 弹出 Alert Dialog（Modal.confirm / ElMessageBox.confirm / 自定义警告弹窗）
    → 明确提示操作对象、具体后果与是否不可恢复（极高危可要求输入指定名称确认）
    → 不可逆操作禁用点击遮罩层关闭（maskClosable / closeOnClickModal = false）
    → 确认按钮使用明确后果动词（如「永久删除」）+ Danger 样式，点击后加 loading 防连点
    → 用户点击「取消」则安全中止，不触发任何副作用
  ```

**7 种高频交互与移动端标准组件工程规范：**

对应 `ui-design` 第 13 节组件选型词库，前端落地需遵守以下工程约束：

| 标准组件 | 工程实现要点与避坑铁律 |
| :--- | :--- |
| **Alert Dialog（警告对话框）** | 不可逆操作禁用遮罩点击关闭（及防误触 Esc 退出）；确认按钮加 `loading` 状态防连点；支持输入指定文本二次校验；管理焦点陷阱（Focus Trap） |
| **Bottom Sheet（底部面板）** | 支持 **Peek / Half / Full** 多档高度吸附（Snap Points）；**处理拖动手势与内部列表滚动冲突**（仅当内部滚动区 `scrollTop === 0` 且向下拖拽时触发面板下滑，配合 `overscroll-behavior: contain`）；底部适配 `env(safe-area-inset-bottom)` |
| **Action Sheet（底部操作菜单）** | 贴近底部布局并适配安全区；危险项使用 Danger 语义 token；提供独立「取消」项与遮罩关闭；仅承载单手点选操作，不塞入复杂长表单 |
| **Snackbar（底部操作反馈条）** | 区别于纯展示 `Toast`：正确处理**「撤销 / 重试」点击与自动关闭定时器的竞态**（点击撤销时立即清除定时器并执行补偿回滚或取消延迟请求）；移动端避开底部导航/安全区遮挡 |
| **Tour（漫游式引导）** | 计算目标元素位置前先确保滚动进视口（`scrollIntoView`）并监听 `resize` / `scroll` 同步聚光灯坐标；**必须提供「跳过」按钮与 `Esc` 退出**；组件卸载或完成后销毁遮罩与事件监听 |
| **Lightbox（图片灯箱）** | 打开时锁定背景页面滚动穿透（Scroll Lock）；支持左右滑动/按钮切换、双指缩放及双击放大；补充键盘方向键切换与 `Esc` 关闭；大图按需加载并展示占位态 |
| **Inline Alert（行内提示）** | **严禁使用自动消失的 Toast 替代表单字段校验报错**；错误态在问题修复或输入变更前必须持续渲染；输入框绑定 `aria-invalid="true"` 与 `aria-describedby` 指向行内错误文案 |

**不要为了形式主义给所有 components 添加所有状态。**

根据实际业务需求确定。

## 10. CSS 质量与形状/主题规范（CSS Quality, Shape & Theme）

**禁止模式：**
```css
/* 反模式：覆盖级联 */
.xxx { color: red; }
.xxx { color: blue; }
.xxx { color: green; }
.xxx { color: yellow !important; }
```

**避免：**
- 深层选择器嵌套（>3 层）
- 过度使用 !important
- 重复样式
- 无意义的魔法数字
- 重复的 media queries
- 随机 z-index 值
- 死 CSS
- CSS 补丁堆叠

**形状与圆角实现（推崇长圆胶囊边框，拒绝生硬直角）：**
- 遵循 `ui-design` 第 10 节形状语言：按钮、标签（Tag/Badge）、筛选器（Filter Chip）、搜索框、状态药丸等可交互元素，优先使用**长圆边框（胶囊状 `border-radius: 9999px`）**；卡片、弹窗等容器使用温和中圆角（`12px-16px`）；内嵌块或表格容器使用小圆角（`6px-10px`）。
- **严禁出现 `border-radius: 0` 的生硬直角组件**（除非项目既有 Design System 强制要求）。

**主题与暗黑模式（Dark Mode）防硬编码：**
- 当项目存在 CSS 变量或支持亮/暗主题切换（Dark Mode）时，组件内的 `background`、`color`、`border-color`、`box-shadow` **必须使用语义化 CSS 变量**（如 `var(--el-bg-color)`、`var(--color-text-primary)`）。
- **严禁在组件局部样式中硬编码 `#fff`、`#ffffff`、`#000`、`#333` 等固定色值**，防止切换到暗黑模式时出现刺眼白块或文字隐形。

**CSS 问题：优先修复结构。**

仅在真正需要时使用局部补丁。

## 11. Accessibility

根据实际页面场景考虑：
- 语义化 HTML（`<button>`, `<a>`, `<nav>` 等）
- Button vs link 语义
- 图片 Alt 文本
- 输入框 Label
- 键盘导航
- Focus 指示器
- Disabled 状态
- 动效降级（`@media (prefers-reduced-motion: reduce)`，见第 16 节）
- ARIA 属性（需要时）

**不要所有交互都用 `<div>` + click。**

但也不要追求理论上的完整 WCAG 合规性而过度复杂化普通业务页面。

**遵循合理的 Production 标准。**

## 12. 图标与符号实现（Icon & Symbol Implementation）

**禁止用 Emoji 充当 UI 图标。**

Emoji 是文本字符而非图标：无法统一控制尺寸、颜色、线宽、基线与视觉重量，跨平台/跨字体渲染结果差异极大，并且会被屏幕阅读器读成语气词。因此**任何承担 UI 语义的图形都必须是矢量图标**。

**禁止使用的位置：**
- 按钮、菜单、导航、Tab、面包屑前缀
- 状态标识（成功 / 失败 / 警告 / 进行中）、标签、表格状态列、卡片角标
- 空状态、错误状态、加载状态的图标位或插图位
- 表单 label、placeholder、toast、弹窗文案中的装饰图形
- CSS `content` 中作为图标
- 用纯文字徽标冒充品牌/文件类型图标（如「PDF」「WORD」大字块代替图标）

**必须使用：**
1. 项目既有图标体系的图标组件（UI Library Icons）
2. 项目内既有 SVG 图标 / 领域图标组件
3. 确需新增时，按项目统一图标风格补充 SVG，并集中管理

**实现要求：**
- 尺寸走统一规格（16 / 20 / 24 等），不随文字字号漂移
- 颜色跟随语义 token（如 `currentColor` 或状态色变量），不写死
- 图标与文字使用 Flex 对齐 + 统一间距，不用空格或手动 margin 硬凑
- 装饰性图标加 `aria-hidden`，纯图标按钮必须有可访问名称（`aria-label` 或视觉隐藏文本）
- 新增图标前先检查项目是否已有同语义图标，禁止重复引入

**免责边界：** 具体使用哪套图标库、哪个组件名，以当前项目既有约定为准，本 Skill 不指定特定库。

## 13. Performance

根据实际问题检查：
- 图片大小
- 懒加载（Lazy Loading）
- 大列表
- 虚拟滚动（Virtual Scrolling）
- 重复请求
- 不必要的 Watchers
- 不必要的 Computed Properties
- 不必要的重新渲染
- 动效属性性能（优先使用 `transform` / `opacity` 合成层属性，禁止过渡/动画化 `width`、`height`、`top`、`left` 等触发重排的属性，见第 16 节）
- Bundle Size
- 大型依赖

**原则：不要过早优化。**

仅处理：
- **明确存在的问题**
- **明显可能导致问题的情况**

## 14. 修改现有页面的范围边界（Scope Boundary）

**UI 任务禁止擅自修改：**
- API Contract
- Router
- Permissions
- Store
- Business Logic
- Domain Rules

除非当前任务明确要求。

**禁止：**
"为了改一个 UI 元素，我重构整个模块。"

**禁止：Scope Creep（范围蔓延）。**

## 15. 代码组织与目录分层（Code Organization & Directory Layout）

页面视觉与交互达标，不等于代码结构达标。
新增页面时把整个页面写进一个文件、或把不同业务的文件平铺在同一目录，属于工程缺陷，即使它"能跑"。

**三条铁律：**

```
1. 禁止平铺（No Flat Layout）
2. 必须分层（Layered by Responsibility）
3. 禁止把所有代码写在一个页面里（No Giant Single-File Page）
```

**前提：** 遵循宿主项目既有的目录规范。若项目已有目录约定，按其执行；若项目没有明确规范，按下述原则组织。本节不定义任何项目专属目录树。

### 1. 禁止平铺

新增文件前先判断它属于哪个业务领域。当某个目录已经混入多个不相关业务，说明该继续下钻，而不是继续往里加文件。

**禁止：**
```
views/
├── UserList.vue
├── RoleList.vue
├── CourseList.vue
├── OrderList.vue
├── QuestionList.vue
└── ReportList.vue
```

**正确：** 先按业务领域建目录，领域内再按模块 / 页面下钻。

```
views/
├── user/
├── course/
├── order/
└── report/
```

按领域分类的目录都适用此规则：

```
components/    api/       types/
composables/   stores/    utils/
```

**判断标准：** 一个目录里如果同时存在 `course`、`order`、`report` 三种业务的文件，需要拆目录，而不是"再放一个进去"。

### 2. 必须分层

前端目录的核心是职责分层，每一层只能做自己那一层的事：

```
views / pages         路由级页面
components            可复用 UI
composables / hooks   可复用响应式逻辑（Vue 为 composables，React 为 hooks）
features              跨页面业务能力
services              业务编排
api                   后端接口定义
stores                跨页面全局状态
types                 数据类型
core                  基础设施（HTTP / SSE / storage）
utils                 纯工具函数
constants             常量
```

**典型调用方向：**

```
views / pages
  ↓
components / composables (hooks)
  ↓
services
  ↓
api
  ↓
core/http
  ↓
Backend
```

**关键约束：**

- 只有完整路由页面放 `views`（或 `pages`）；页面内部复用的 UI 放 `components`
- 无框架响应式依赖的纯函数放 `utils`，不得放 `composables` / `hooks`
- `api` 只负责地址、方法、参数、返回值类型，不写业务编排、不操作 DOM、不管页面状态
- `types` 统一收口数据结构，禁止在每个页面里重复声明大型 Interface
- HTTP 实例只能在 `core/http`（或 `utils/request`）维护一套，禁止在 `views` / `components` / `api` 重复创建
- 禁止 `views` 之间互相 import 复用 UI

**不强制为形式而分层：** 简单 CRUD 允许 `views → api` 直连，不必为了"结构完整"凭空造一个没有业务逻辑的 service。

### 3. 禁止把所有代码写在一个页面里

**一个页面文件里堆积以下内容，属于违规：**

```
整套表格 / 表单 / 弹窗 / 详情 / 图表
+ 全部 API 调用
+ 全部类型声明
+ 全部状态管理
+ 数百行 scoped SCSS / CSS
```

**要求：**

- 页面文件保持为路由壳，负责布局编排与子组件引用
- 独立 UI 块进 `components`
- 复用逻辑进 `composables`（或 `hooks`）
- 接口进 `api`，类型进 `types`，样式 token 与公共样式进全局，不在页面内重复定义
- 出现多分支业务编排时，下沉到 `services` 或 `features`

**出现以下信号，必须按职责拆分，而不是继续往页面里加代码：**

```
□ 单个 .vue / .tsx 文件明显超出项目约定行数（无约定时，超过 500 行应先评估拆分）
□ 页面里出现第二个业务概念
□ 页面里出现可被其他页面复用的 UI 或逻辑
□ 同一段接口调用逻辑在页面内重复三次以上
□ 巨型状态集合（data / ref / useState）+ 巨型处理函数块同时存在
```

**按职责拆分，而不是按"每个小块一个组件"拆：**

```
CourseList.vue          列表编排
CourseFilterBar.vue     筛选
CourseTable.vue         表格
CourseFormDialog.vue    新增 / 编辑
useCourseList.ts        列表数据
```

**禁止为了拆而拆：** 不得产出 `<Box />`、`<Wrapper />`、`<TopWrapper />` 这类无职责组件（见第 8 节）。拆分标准是职责清晰，不是文件数量。

### 4. 迁移边界（纯搬迁，禁止顺手重做）

把已有页面拆分到 `components` / `composables` 时，默认任务是**搬迁**，不是**重做**。

**只允许改变：** 代码所在目录与 import 路径。

**不允许改变（必须像素级、行为级一致）：**

```
DOM 结构（标签层级、class 名、data-*、aria）
样式（scoped SCSS 规则、变量、颜色、响应式断点）
交互（按钮、表单、弹窗、路由跳转、校验、loading / 空状态）
业务行为（API 入参、调用时机、成功 / 失败处理、权限判断）
```

**迁移期间禁止：**

- 把自定义 UI 换成 UI Library 默认样式
- 重命名原 CSS class
- 「顺便」调整布局比例、配色、文案、图标
- 在迁移同时做性能优化或接口改造
- 删除模板仍在引用的函数 / 按钮

**迁移完成自检：**

```
□ 原页面每个 @click / @submit / v-model 在新结构中仍有等价绑定
□ 原页面每个 class 名拆分后仍能命中样式（注意 scoped 与 :deep）
□ 原页面 API 调用次数、参数、触发时机未变
□ 浏览器并排对比无可肉眼识别的差异
□ Build / Type Check 通过
□ 无 views 之间互相 import
```

若迁移过程中发现原页面已有 bug，可在同一任务内修复，但必须单独说明，不得与迁移混为一谈。

### 5. 交付前自检

```
□ 新增文件是否放进了正确的业务领域目录，而不是丢进公共目录
□ 目录内的文件是否仍属于同一业务领域
□ 页面是否只承担布局编排，未堆积全部逻辑
□ 可复用 UI 是否进了 components，可复用逻辑是否进了 composables
□ 接口是否在 api，类型是否在 types，是否与页面内重复声明冲突
□ 是否重复创建了第二个 HTTP 实例 / 第二套同名类型
□ 拆分是否产生了无职责的 Wrapper 组件
```

## 16. 动效工程实现（Motion & Animation Engineering）

**工程目标：精准还原 `ui-design`（第 17 节）定义的动效方案，同时保证 60fps 流畅度、零布局重排与完整无障碍降级。**

### 一、三条工程铁律

1. **控制时长与复用 Token**：
   - 按钮、标签、切换开关等微交互严格控制在 **100~200ms**，优先复用项目既有动效变量或 `--duration-fast` / `--ease-out`。
   - **禁止使用 `transition: all ...`**：必须显式声明过渡属性（如 `transition: transform 150ms cubic-bezier(0.16, 1, 0.3, 1), opacity 150ms ease`）。
2. **仅动画化高性能合成层属性**：
   - 位移、缩放、显隐必须通过 `transform` 和 `opacity` 实现。
   - **严禁动画化触发布局重排（Reflow）的属性**（如 `width`、`height`、`top`、`left`、`margin`、`padding`、`font-size`）。
   - `will-change` 仅用于明确存在高频交互的特定元素，禁止全局挂载。
3. **强制尊重系统无障碍设置与设备能力**：
   - 全局或组件级必须适配 `@media (prefers-reduced-motion: reduce)`：关闭装饰性循环动画、视差、错峰位移与持续滚动，改为直接切换或瞬间淡入。
   - 指针悬停类特效（如 `Spotlight`）仅在 `@media (hover: hover) and (pointer: fine)` 下启用，触屏端自动跳过。
   - 任何使用 `requestAnimationFrame`、`IntersectionObserver`、定时器的动效，**必须在组件卸载时清理注销**，防止内存泄漏。

### 二、常见 UI 动效工程实现指南

| 标准名称 | 推荐实现机制 | 关键工程约束与避坑点 |
|------|------|------|
| **Ease Out（缓动效果）** | CSS `cubic-bezier(0.16, 1, 0.3, 1)` 或框架等价曲线 | 进场减速柔和；离场/收起可将时长缩短 20%~30% |
| **Spotlight（鼠标聚光灯）** | `pointermove` 更新局部 CSS 变量 `--x`/`--y`，驱动 `radial-gradient` 遮罩/背景层 | 仅更新 CSS 变量，不触发 DOM 重排；禁止挂在大型滚动列表每一项上 |
| **Border Beam（边缘流光）** | 伪元素 + `conic-gradient` 旋转或 `offset-path` 沿边框路径移动 | 严禁同屏大面积卡片同时运行，避免 GPU 合成与重绘过载 |
| **Scroll Reveal（滚动显示）** | `IntersectionObserver` 监听进入视口后切换入场类（`opacity` + `translateY`） | **禁止**监听高频率 `scroll` 手算位置；触发一次后即 `unobserve` |
| **Stagger Animation（错峰动画）** | 通过 CSS 变量 `--stagger-index` 计算 `transition-delay` / `animation-delay` | 设置最大延迟封顶（如前 6~8 项递增），防止长列表末尾久等 |
| **Scroll Storytelling（滚动叙事）** | `position: sticky` 吸顶容器 + 滚动进度（CSS Scroll-driven 或 `rAF` 映射进度）驱动图层 `transform`/`opacity` | 降级状态（小屏/关闭动效）下保证各章节文案仍顺序可读 |
| **Parallax Scrolling（视差滚动）** | 基于滚动进度更新背景/装饰层的 `transform: translate3d(0, y, 0)` | 禁用修改 `background-position` 或 `top`；`reduced-motion` 下回归静态 |
| **Shimmer（流光扫过）** | 伪元素高光层配合 `transform: translateX(-100% → 100%)` 动画 | 优于动画化 `background-position`，大幅减少重绘开销 |
| **Count Up（数字递增）** | `requestAnimationFrame` 插值递增，到达目标值后立即停止 | 数值容器必须加 `font-variant-numeric: tabular-nums`（等宽数字），防止数字跳动导致宽度抖动 |
| **Typewriter（打字机效果）** | 流式追加或步进切片渲染文本，光标闪烁仅使用 `opacity` 动画 | 屏幕阅读器区域合理使用 `aria-live`，卸载时清理定时器 |
| **Text Scramble（文字解码）** | `rAF` 按步进替换字符并在目标文本处收敛锁定 | 为完整目标文本保留稳定 `aria-label`，避免读屏器连续播报乱码；严格用于极短标题 |
| **Marquee（跑马灯）** | 双份无缝轨道配合 `@keyframes` 的 `transform: translateX(-50%)` 匀速循环 | **必须实现 `hover` 与 `focus-within` 暂停**（`animation-play-state: paused`）；副本轨道加 `aria-hidden="true"`；`reduced-motion` 下停止滚动 |

### 三、标准 CSS 降级模板参考

```css
/* 全局尊重系统减少动态效果设置 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

</Core_Rules>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 验证                                                                         -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Visual_Verification>

**这是强制步骤。**

**Build Passed ≠ Visual Accepted。**

如果当前环境具备：
- Browser
- 截图能力
- Preview
- Playwright
- Dev Server
- 其他页面检查能力

则页面实施完成后：**必须实际打开页面。**

**不能只看源码。**

**最小验证：**
- Desktop（1440 或类似）
- Tablet（1024 或类似）
- Mobile（390 或类似）

**检查：**
- Layout 完整性
- 对齐
- 间距
- Typography
- Visual Hierarchy
- 文本换行
- 长内容处理
- 溢出行为
- 横向滚动（除非有意为之，否则不应出现）
- 重叠元素
- Sticky 元素
- Fixed 元素
- Modals/Dialogs
- 表格
- Empty State
- Loading State
- 图标与符号（是否存在 Emoji 充当图标、图标风格是否混用）
- Footer
- Header

**如果发现明确问题：直接修复。**

**修复后：重新验证。**

**不能假设："我认为应该没问题。"**

</Visual_Verification>

<Senior_Frontend_Review>

页面实施完成后：

**不要因为是自己刚写的代码就假设正确。**

**切换视角：**

"将此实现视为另一个工程师编写的代码。"

**重新进行 Senior Frontend Review。**

**检查维度：**
- **保真度（Fidelity）** — 是否忠实实现设计（如有设计输入）
- **Layout 正确性** — Flexbox/Grid/Flow 实现正确
- **间距实现** — 符合 Spacing System
- **Typography 实现** — 符合 Typography System
- **Visual Hierarchy 保真度** — 层级还原正确
- **Responsive 稳定性** — 各视口下正常工作
- **Component 架构合理性** — 高内聚低耦合
- **目录与分层合规** — 无平铺目录、无单文件巨型页面、无重复职责文件（见第 15 节）
- **Interaction State 完整性** — Loading/Empty/Error/Hover 已处理；表单校验使用常驻 `Inline Alert`；可逆操作支持 `Snackbar` 撤销；不可逆破坏性操作使用 `Alert Dialog` 阻断确认（禁用遮罩关闭、防连点）；移动端 `Bottom Sheet` / `Action Sheet` / `Lightbox` / `Tour` 无滚动冲突且适配安全区
- **CSS 质量与形状/主题** — 无过度嵌套/!important/魔法数字；交互元素采用长圆胶囊边框、容器圆角，无生硬直角；暗黑模式下无硬编码颜色冲突
- **图标实现** — 无 Emoji 充当 UI 图标，图标来自项目既有体系，尺寸/颜色/对齐统一
- **Accessibility 充分性** — 语义 HTML/键盘导航/Focus/尊重 `prefers-reduced-motion`
- **Performance & Motion 合规** — 无明显性能问题；动效仅动画化 `transform` / `opacity`，微交互 100~200ms，无卸载内存泄漏，跑马灯支持悬停/聚焦暂停
- **可维护性** — 代码清晰易维护
- **溢出处理** — 无意外 overflow
- **业务回归** — 未破坏现有功能

**不负责审查（由 ui-design 负责）：**
- 设计美学（Aesthetic）
- 品牌一致性（Brand Consistency）
- 设计决策合理性（Design Decision Rationale）

**问题优先级：**

**P0（Critical - 关键）：**
- 功能不可用
- 页面严重崩坏
- 严重业务回归
- 严重重叠/不可操作

**P1（Major - 重大）：**
- 明显保真度问题（与设计稿偏差过大）
- Responsive 崩溃
- Layout 错误
- 明显交互缺失
- 高风险可维护性问题

**P2（Minor - 次要）：**
- 轻微视觉优化
- 非关键代码清理
- 可选体验增强

**原则：**
- **P0 / P1 必须修复。**
- **P2：不要为追求完美而无限扩大范围。**

</Senior_Frontend_Review>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- READY 标准                                                                   -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<READY_Definition>

**仅当满足所有条件时才声明 READY：**

✅ **功能正确（Function Correct）** — 所有必需功能正常工作

✅ **视觉成熟（Visual Mature）** — 看起来像精致的商业产品

✅ **Responsive 稳定（Responsive Stable）** — Desktop/Tablet/Mobile 均可用

✅ **无明显溢出（No Obvious Overflow）** — 内容不破坏容器

✅ **无明显重叠（No Obvious Overlap）** — 元素不会错误覆盖彼此

✅ **Typography 一致（Typography Consistent）** — 遵循统一字体系统

✅ **间距一致（Spacing Consistent）** — 遵循统一间距系统

✅ **Component 结构合理（Component Structure Reasonable）** — 高内聚，低耦合

✅ **目录分层合规（Directory Layering Compliant）** — 无平铺目录、无所有代码堆在一个页面、无重复职责文件

✅ **CSS 可维护且形状/主题合规（CSS, Shape & Theme Maintainable）** — 无过度嵌套、!important、魔法数字；遵循长圆胶囊边框与圆角体系（无生硬直角）；无破坏暗黑模式的硬编码色值

✅ **无 Emoji 充当图标（No Emoji as Icon）** — 界面按钮、菜单、标签、状态等 UI 元素中无 Emoji 充当图标或装饰图形，均使用项目图标体系或 SVG

✅ **交互状态与危险确认完备（Interaction State & Destructive Confirm）** — Loading/Empty/Error/Hover/Focus 已处理；表单报错使用常驻 `Inline Alert`；不可逆危险操作通过 `Alert Dialog` 二次确认且禁用遮罩误关、绝不单击直删；移动端 `Bottom Sheet` / `Action Sheet` / `Lightbox` 等无滚动穿透或手势冲突且适配安全区

✅ **动效性能与无障碍合规（Motion Performance & Reduced Motion）** — 动效仅使用 `transform` / `opacity` 合成层属性，微交互时长 100~200ms，支持 `@media (prefers-reduced-motion: reduce)` 降级，`Marquee` 支持悬停/聚焦暂停，组件卸载已清理 Observer/rAF

✅ **无 P0 问题（No P0 Issues）** — 无关键阻塞

✅ **无 P1 问题（No P1 Issues）** — 无重大问题

✅ **Build Passed** — 编译成功

✅ **Type Check Passed** — 无 TypeScript 错误（如适用）

✅ **Visual Verification Passed** — 实际在浏览器中打开并验证（如环境支持）

✅ **无破坏性变更（No Breaking Changes）** — 未破坏 API/Router/Store/Permission/Business Logic

</READY_Definition>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 与 ui-design 协作                                                            -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Collaboration_With_UI_Design>

**senior-frontend 与 ui-design 的职责边界：**

```
ui-design        = What should it look like?（应该如何呈现）
senior-frontend  = How should we build it correctly?（如何正确构建）
```

**路由规则（按任务类型选择入口）：**

```
无设计稿的新页面        → ui-design 定义后可 DESIGN READY → 本 Skill 实现
已有成熟设计稿          → 本 Skill 忠实实现，ui-design 不介入
现有页面视觉升级        → ui-design Review / Redesign → 本 Skill 实现
纯技术问题（Layout / Responsive / CSS Bug） → 本 Skill 直接修复
```

**设计输入优先级：**

senior-frontend 优先消费以下设计输入（按优先级）：
1. 用户提供的设计稿（Design / Screenshot / Figma）
2. ui-design 输出（DESIGN READY）
3. 项目现有 Design System
4. UI Library 默认规范

**禁止擅自：**
- 重新定义视觉方向
- 改变品牌色
- 重新设计整体视觉语言
- 改变信息架构
- 重新设计页面风格

除非用户明确要求或无任何设计输入。

</Collaboration_With_UI_Design>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 与项目设计体系的关系                                                          -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Integration_With_Project_Design_System>

**senior-frontend 是："高级前端工程方法论"**

**senior-frontend 不是："特定项目的视觉设计"**

因此**禁止将特定业务风格硬编码到 Skill 中。**

**禁止示例内容：**
```
电商必须是红色
体育网站必须是深色
管理后台必须使用特定 Layout
Card 必须有特定圆角
```

**项目实际视觉规范应来自：**
- 当前项目 Design System
- 当前业务需求
- 当前设计稿 / 截图

**关系应该是：**

```
Senior Frontend Skill = 如何构建（How to build）
项目 Design System = 应该如何呈现（How it should look）
任务 Prompt = 构建什么（What to build）
```

**在实际项目中工作时：**
1. 阅读项目现有 Design Tokens
2. 阅读项目现有 Components
3. 阅读项目现有 Layout 模式
4. 遵循项目约定
5. 按照本 Skill 的工程标准实施

</Integration_With_Project_Design_System>

<Integration_With_External_Design_Skill>

如果环境中存在 `frontend-design` 一类外部设计插件：

**关系：**

```
frontend-design（可选）→ 视觉方向、调色板、Typography 特征
         ↓
senior-frontend（必需）→ 工程实现、质量、验证
         ↓
    生产级 UI
```

**frontend-design 提供：** 美学方向、品牌独特性、设计决策

**senior-frontend 提供：** Layout 工程、响应式实现、CSS 质量、验证工作流

**它们是互补的，而非冗余的。** 该插件不存在时，视觉方向由 `ui-design` 提供；本 Skill 始终只负责工程实现。

</Integration_With_External_Design_Skill>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 最终注释                                                                     -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Final_Notes>

## 适用边界

**本 Skill 覆盖：** 布局工程、响应式实现、CSS 质量、组件拆分、交互状态、动效工程实现（性能与无障碍降级）、图标体系落地、视觉验收。

**本 Skill 不覆盖：** 视觉方向与设计系统定义（使用 `ui-design`）、后端业务实现（使用 `backend-development`）、数据库设计（使用 `mysql-design`）。

**禁止：** 擅自重新定义视觉方向、更换品牌色、改动信息架构或页面风格——除非用户明确要求，或任务本身没有任何设计输入。

## 兼容性

核心方法论保持工具无关，不依赖特定 IDE 或 Agent 实现：
- Cursor
- Claude Code
- Codex

不在内容中写入仅某一种工具才能理解的语法或环境假设。

</Final_Notes>
