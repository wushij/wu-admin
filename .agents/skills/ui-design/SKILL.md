---
name: ui-design
description: 生产级 UI/UX 设计方法论，涵盖页面目标与用户任务、信息架构、视觉层级、Typography 与色彩间距体系、形状语言与图标体系、动效设计与沟通词库、响应式信息策略、Design Token。用于新页面无设计稿的情况、Dashboard 与详情页设计、UI Redesign、页面视觉优化、Design System 建立与视觉 Review。具体 CSS 与组件实现请改用 senior-frontend。
---

# UI Design

<Purpose>
本 Skill 定义了生产级 UI/UX 设计的完整工作流程，确保页面设计达到现代商业应用标准：目标明确、信息架构清晰、视觉系统完整、避免 AI 模板化。

这不是关于"让页面漂亮一点"，而是系统化地从需求到设计方案的完整流程。
</Purpose>

<Use_When>
适用于以下设计任务：
- 新页面没有成熟设计稿（从需求直接设计）
- 新产品页面设计
- Homepage / Landing Page Design
- Dashboard Design  
- Detail Page Design
- UI Redesign
- 现有页面视觉优化
- 页面"太丑/太普通/不高级"
- Design System 建立
- Visual Review（美学/风格/一致性）
- 页面视觉方向设计
</Use_When>

<Do_Not_Use_When>
跳过设计流程：
- 已有成熟设计稿且只需忠实实现
- 单纯 CSS Bug
- 单纯 Responsive Bug
- Type Error
- Backend Task
- API Task
- Trivial frontend fix
</Do_Not_Use_When>

<Why_This_Exists>
AI 生成的 UI 常见问题：
- 直接开始画 Card，没有思考页面目标
- 所有页面都是圆角白卡堆叠
- 默认蓝紫渐变 + Glassmorphism
- 信息层级混乱，所有内容视觉权重相同
- 字号随意（13/14/15/16/17/18 无规律）
- 间距随意（magic numbers 泛滥）
- 4 个同级 Primary Button 并列
- Card inside Card inside Card
- 过度发光 Border + 无意义网格背景
- 用 Emoji 充当按钮、状态、菜单图标，跨平台渲染不一致
- 典型 AI 模板感，缺乏品牌特征

本 Skill 通过系统化设计方法论消除这些问题。
</Why_This_Exists>

<Workflow>
设计工作流程：

1. **需求理解（Requirement）** — 理解业务目标
2. **页面目标（Page Goal）** — 明确页面核心任务
3. **用户任务（User Task）** — 用户为什么来这个页面
4. **信息架构（Information Architecture）** — 内容优先级和组织
5. **视觉方向（Visual Direction）** — 确定视觉语言（气质定位 + 形状语言 + 边框策略，见第 10 节）
6. **Design System** — 建立或复用设计系统
7. **Component Visual Language** — 定义组件视觉规范（含图标体系与来源）
8. **Motion Design** — 确定交互动效选型与克制边界（按需，见第 17 节）
9. **Responsive Information Strategy** — 响应式信息策略
10. **UI Review** — 设计审查
11. **DESIGN READY** — 设计交付

不要跳过前 4 步直接画 UI。
</Workflow>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 核心设计原则                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Core_Design_Principles>

## 1. 页面目标与用户任务（Page Goal & User Task）

**开始设计前必须回答：**

- 用户为什么进入这个页面？
- 最重要的任务是什么？
- 第一眼应该看到什么？
- 第二步应该做什么？
- 什么信息属于辅助信息？
- 哪些操作是核心 CTA？

**禁止：**
```
需求：做一个用户列表页
设计：立即开始画表格和卡片
```

**正确：**
```
1. 页面目标：管理员快速找到目标用户并执行操作
2. 核心任务：搜索 → 筛选 → 查看详情 → 操作
3. 第一优先级：搜索框 + 快速筛选器
4. 第二优先级：用户列表（状态/关键数据可见）
5. 辅助信息：分页/批量操作
```

## 2. 信息架构（Information Architecture）

**建立清晰的内容层级：**

```
Primary Information
  ↓
Secondary Information
  ↓
Supporting Details
  ↓
Metadata
```

**示例 - Dashboard：**
- Primary：核心指标（今日收入/订单数）
- Secondary：趋势图表
- Supporting：数据明细表
- Metadata：更新时间/筛选条件

**禁止：**
所有信息平铺，无优先级。

## 3. 视觉层级（Visual Hierarchy）

**明确定义：**

| 层级 | 用途 | Typography | 示例 |
|------|------|------------|------|
| **Page Title** | 页面主标题 | 28-32px, Bold | "用户管理" |
| **Section Title** | 区块标题 | 20-24px, Semibold | "最近活跃用户" |
| **Primary Content** | 主要内容（含正文与卡片主标题） | 16px, Regular / Semibold | 正文内容、卡片标题 |
| **Secondary Content** | 次要内容 | 14px, Regular | 辅助说明、正文默认字号 |
| **Caption** | 标注/元数据 | 12px, Regular | 更新时间 |

**与本表第 4 节的关系：** 第 4 节给出的是字号体系（Token 档位），本表给出的是层级到字号的映射。16px 在体系中对应 H3，在层级中承担 Primary Content；14px 对应 Body，是正文默认字号。同一档位在不同语义下承担不同层级，不冲突。

**禁止：**
```css
/* 反模式：所有内容视觉权重相同 */
.title, .content, .label, .value {
  font-size: 14px;
  font-weight: 400;
  color: #333;
}
```

## 4. Typography System

**建立规律的字体系统：**

**推荐基础体系：**
```
Display: 32px / 1.2 / Bold
H1:      24px / 1.3 / Semibold
H2:      20px / 1.4 / Semibold
H3:      16px / 1.5 / Semibold
Body:    14px / 1.5 / Regular
Small:   12px / 1.5 / Regular
```

**避免：**
```
13px, 15px, 17px, 18px, 19px 无规律堆积
```

**优先：**
复用项目现有 Typography System。

如果项目无体系，提议最小化 token 集合。

## 5. Color System

**定义系统化调色板：**

**基础色（Foundation）：**
- Background: #FFFFFF / #F5F7FA
- Surface: #FFFFFF (卡片/对话框)
- Border: #E4E7EB

**文本色（Text）：**
- Primary: #1F2937 (主要文字)
- Secondary: #6B7280 (次要文字)
- Muted: #9CA3AF (辅助文字)

**功能色（Functional）：**
- Primary: #3B82F6 (主要操作)
- Success: #10B981
- Warning: #F59E0B
- Error: #EF4444

**主题与暗黑模式（Dark Mode）适配：**
- 以上色值仅为亮色基线参考，实际必须映射为语义化 Design Token（如 `--color-bg-surface`、`--color-text-primary`）
- 若项目支持亮/暗主题切换（Dark Mode），所有背景色、表面色、边框色、文本色必须定义双主题变量映射，禁止设计仅在纯白背景下成立的硬编码配色

**禁止默认：**
- 蓝紫渐变作为默认主题
- 所有按钮都发光
- 无意义的霓虹色

**调色板选择应基于：**
- 品牌特征
- 产品定位
- 目标用户
- 行业惯例

## 6. Spacing System

**建立规律的间距体系：**

**推荐基础体系：**
```
4px   - 极小间距（图标与文字）
8px   - 小间距（表单元素内部）
12px  - 紧凑间距（列表项）
16px  - 标准间距（段落/卡片内边距）
24px  - 中等间距（区块间距）
32px  - 大间距（Section 间距）
48px  - 超大间距（Page Section）
```

**优先：**
复用项目现有 Spacing System。

**禁止：**
```
margin-top: 17px;
padding: 23px 19px 21px 27px;
```

无规律魔法数字。

## 7. 密度策略（Density Strategy）

**根据页面类型选择信息密度：**

**High Density（高密度）：**
- 后台管理表格
- 数据密集型 Dashboard
- 专业工具界面
- 特征：紧凑间距、信息量大、效率优先

**Medium Density（中密度）：**
- 内容管理页面
- 标准业务表单
- 列表页
- 特征：平衡信息量与可读性

**Low Density（低密度）：**
- Landing Page
- Marketing 页面
- 引导流程
- 特征：宽松间距、突出重点、体验优先

**禁止：**
所有页面使用相同密度。

## 8. Card 使用策略（Card Usage）

**Card 的合理使用：**

**适合 Card 的场景：**
- 独立内容单元（文章预览、产品卡片）
- 需要视觉分组的内容
- 可交互的内容块

**不适合 Card 的场景：**
- 简单文本信息（用 section 或 list）
- 已经在容器内的内容
- 过度嵌套（Card inside Card inside Card）

**禁止：**
```html
<!-- 反模式：所有内容都包 Card -->
<Card>
  <Card>
    <Card>
      <p>Some text</p>
    </Card>
  </Card>
</Card>
```

**合理：**
```html
<!-- 根据内容性质决定是否使用 Card -->
<div class="page">
  <section class="stats"> <!-- 统计数据不需要 Card -->
    <StatItem />
  </section>
  
  <Card> <!-- 独立内容单元使用 Card -->
    <ArticlePreview />
  </Card>
</div>
```

## 9. CTA 层级（CTA Hierarchy）

**一个区域明确 CTA 优先级：**

| 层级 | 用途 | 样式 | 示例 |
|------|------|------|------|
| **Primary** | 核心操作 | Filled, 主色 | "提交订单" |
| **Secondary** | 次要操作 | Outlined / Ghost | "保存草稿" |
| **Tertiary** | 辅助操作 | Text link | "取消" |
| **Danger** | 危险操作 | Filled, 红色 | "删除账户" |

**禁止：**
```html
<!-- 反模式：4 个同级 Primary Button -->
<div class="actions">
  <Button type="primary">提交</Button>
  <Button type="primary">保存</Button>
  <Button type="primary">导出</Button>
  <Button type="primary">分享</Button>
</div>
```

**正确：**
```html
<div class="actions">
  <Button type="primary">提交</Button>
  <Button type="secondary">保存草稿</Button>
  <Button type="text">取消</Button>
</div>
```

**危险操作确认与轻量撤销分级（Danger Confirmation & Undo Hierarchy）：**
- **分级原则**：“能通过撤销解决的，就不弹出确认框打断用户连贯流程。”
  - **可逆/轻量操作（如移至回收站、单条归档、列表软删除）**：优先直接执行并弹出带「撤销 / 重试」入口的 **Snackbar**，允许用户立即恢复。
  - **高风险或不可逆操作（如永久删除、批量删除、清空、注销账号、作废、重置数据）**：严禁单击直接执行，必须使用 **Alert Dialog（警告对话框）** 或轻量确认气泡（`Popconfirm`）进行二次确认：
    - **后果明确告知**：禁止仅写“确定要继续吗？”，必须说明操作对象、具体后果与是否不可恢复；极高风险（如注销账号、清空资产）可要求用户输入指定文本（如项目名称）完成二次确认。
    - **交互阻断防误触**：不可逆操作必须**禁用点击遮罩层关闭弹窗**，防止误触或意外退出。
    - **明确动词文案**：确认按钮使用 Danger 红色样式并写明后果动词（如「永久删除」「确认注销」），而非模糊的「确定」，默认焦点或安全退出位留给「取消」。

## 10. 视觉方向与形状语言（Visual Direction & Shape Language）

**这是工作流第 5 步的落地产出，也是 DESIGN READY 的前置条件。**

视觉方向回答一个整体问题：这个产品给人的气质是什么？克制精密，还是轻松友好？业务属性、目标用户、品牌调性共同决定它。

**视觉方向至少包含：**
- 气质定位（如：专业克制 / 温暖亲和 / 高效工具 / 高端质感）
- 形状语言（见下）
- 边框与阴影策略（见下）
- 图标体系（来源、风格、尺寸档位；见第 14 节）
- 与现有 Design Token 的关系（复用还是新增）

### 形状语言（Shape Language）

**圆角不是一个装饰参数，而是一种语义信号。** 它告诉用户"这是可点击的"还是"这是一个容器"。

**核心美学原则：推崇「长圆边框（胶囊状）」与柔和圆角，严禁生硬直角（0px）。**

**按圆角尺度分四类：**

| 形状 | 圆角取值 | 语义 | 典型用途 |
|------|----------|------|----------|
| **长圆（胶囊状 Pill）** | ≥ 高度的一半（常用 `9999px`） | 可交互、现代、呼吸感、灵动 | **按钮、标签（Tag/Badge）、筛选胶囊（Filter Chip）、搜索框、分段控制器、开关、状态胶囊** |
| **中圆角** | 12-16px | 温和、现代、承载感 | 卡片（Card）、弹窗（Dialog/Modal）、浮层面板、大容器 |
| **小圆角** | 6-10px | 克制、专业、紧凑 | 多行文本框（Textarea）、表格外框、代码块、紧凑型内部区块 |
| **圆形** | 50% | 单点元素 | 头像、纯图标按钮、状态圆点、数字角标 |

> **"长圆边框"是胶囊形（Capsule / Pill）的核心特征**：当圆角半径 ≥ 元素高度的一半时，左右两端呈现为完整半圆，形成流畅的长椭圆外观。它是现代 UI 中交互元素最精致、最明确的形状信号。

### 判断规则

**按元素的职能选择形状，拒绝直角边框：**

- **交互与触发元素 → 优先长圆（胶囊状 `9999px`）**：主次按钮、搜索输入框、分类/状态标签（Tag）、筛选药丸（Chip）、顶部切换 Tab。长圆胶囊边框能赋予界面极佳的现代感与亲和力。
- **承载内容容器 → 中圆角（`12px-16px`）**：卡片、弹窗、下拉菜单、通知浮层。
- **多行输入与数据容器 → 小圆角（`6px-10px`）**：多行文本域、表格外围容器、代码预览框（即使是数据密集区也应保持微圆角，**严禁使用 `border-radius: 0` 的生硬直角边框**）。
- **单点标识 → 圆形（`50%`）**：头像、圆形图标按钮、状态指示点。

**同一产品内应保持「交互件长圆胶囊 + 容器中圆角」的清晰层级，而不是逐页随意决策。**

### 一致性约束

**分层搭配是高级感的核心，随意混用或出现生硬直角是破坏性的：**

```
✅ 推荐的现代分层美学：
   交互元素（按钮 / 标签 / 筛选器 / 搜索框）→ 长圆胶囊边框（9999px）
   容器元素（卡片 / 弹窗 / 面板）          → 中圆角（12-16px）
   紧凑内嵌块（代码块 / 表格容器）         → 小圆角（6-10px）

❌ 禁止的反模式：
   1. 使用生硬的直角边框（border-radius: 0），让界面显得粗糙过时
   2. 同一页里按钮有的用胶囊、有的用直角、有的用小圆角，毫无职能依据
```

**判断方法：** 形状是否与元素职能严格对应？是否存在未设圆角的生硬直角组件？

**禁止：**
- **禁止对卡片、按钮、输入框、标签、弹窗、表格外框使用 `0px` 直角边框**
- 同一类元素（如所有按钮或所有标签）在不同页面使用不同圆角
- 为了让某个模块"看起来特别"而单独引入额外的圆角尺度
- 未确认项目既有 token 就随意写死无规律的圆角数值

### 边框与阴影策略

**三者选其一作为主要分隔手段，不要同时叠加：**

| 策略 | 表达 | 适用 |
|------|------|------|
| **无边框 + 阴影** | 轻浮起、现代 | 卡片、弹窗、浮层 |
| **细边框（1px）** | 分隔清晰、结构感 | 表格、表单、分区、紧凑列表 |
| **全边框 + 无阴影** | 平面、数据导向 | 高密度后台、表格容器 |

**避免：**
- 既加边框又加明显阴影（视觉重量过载）
- 发光边框（Border Glow）作为常规分隔手段（仅单点重点强调可按第 17 节受控选用 `Border Beam`）
- 同一区域内混用多种分隔策略

**落地交给实现层：** 具体圆角数值、边框颜色、阴影层级由 senior-frontend 按项目既有 Design Token 实现。

## 11. Visual Rhythm（视觉节奏）

**控制页面视觉节奏：**

**通过以下元素建立节奏：**
- 大区块 vs 小区块
- 密集信息 vs 留白
- 粗标题 vs 细文本
- 图片 vs 纯文本
- 数据可视化 vs 表格

**避免：**
```
Card Card Card Card Card Card Card Card
（单调重复，无节奏感）
```

**追求：**
```
Hero Section
↓
3-Column Cards
↓
Full-width Chart
↓
2-Column Content
↓
CTA Section
```

**节奏变化创造视觉兴趣。**

## 12. Anti-AI-Looking Design

**避免典型 AI 模板化设计：**

**禁止默认使用（除非有明确品牌理由）：**
- 蓝紫渐变背景
- Neon Glow 效果
- 大量 Glassmorphism
- 巨型 Hero Section（占满整屏）
- Floating Orb（浮动光球）
- 无意义光晕（若作局部交互焦点，需按第 17 节受控使用 `Spotlight`）
- 所有模块都是圆角白卡
- 过度阴影
- 发光 Border（禁止全页铺开；仅推荐/会员卡单点可按第 17 节选用 `Border Beam`）
- 无意义网格背景
- 装饰性科技线条泛滥

**"技术感"应通过以下建立：**
- 精确的 Typography
- 清晰的 Grid System
- 适当的信息密度
- 高质量 Imagery
- Precise Spacing
- 简洁的 Motion
- 高质量内容
- 成熟的 Layout

**而不是单纯靠蓝紫发光。**

**判断标准：**
"这个设计在当前现代商业产品中看起来是否专业且非模板化？"

## 13. Component Visual Language

**定义组件视觉规范：**

**具体参数应服从第 10 节确定的形状语言与边框策略，本节负责把语言落到每个组件的尺寸与取值上。**

**Button：**
- 高度（32px / 36px / 40px）
- 圆角（优先使用长圆胶囊边框 `9999px` 或跟随项目既有规范，禁止直角）
- 内边距（胶囊按钮左右内边距需略宽，如 `12px 20px` / `16px 24px`，避免两端文字贴边）
- 字重（Medium / Semibold）

**Input / Tag / Filter Chip：**
- 搜索框、筛选胶囊、状态标签优先采用长圆胶囊边框（`9999px`）
- 常规表单输入框采用胶囊或温和圆角（`8px-12px`），禁止 `0px` 直角
- Border 颜色（default / focus / error）
- Label 位置（top / floating / inline）

**Card：**
- 圆角（容器类，通常为中圆角）
- 阴影（subtle / medium）
- 边框（有 / 无，与阴影二选一为主）
- 内边距（16px / 20px / 24px）

**Table：**
- 行高（40px / 48px）
- Stripe（有 / 无）
- Border style（全边框 / 横线 / 无边框）
- Header style（填充 / 加粗 / 颜色区分）

**交互、反馈与移动端标准组件选型（7 种标准组件词库）：**

> **端侧适配边界**：以下 `Bottom Sheet` 与 `Action Sheet` 为**移动端 / 小屏优先组件**（强调单手可达性与手势交互）；在纯桌面端 B 端后台场景中，应对应映射为 `Drawer` / `Modal` 或 `Dropdown Menu`，避免将手机端手势交互硬套至桌面端。其余 5 类组件全端通用，移动端需重点落实触控与焦点设计。

| 标准组件术语 | 核心定位与适用场景 | 设计规范要点 |
| :--- | :--- | :--- |
| **Alert Dialog（警告对话框）** | 永久删除、注销账号等**不可逆高风险操作**确认 | 明确说明操作内容、后果与能否恢复（高危可要求输入指定内容确认）；不可逆场景**禁用点击遮罩层关闭**；确认按钮用明确后果动词（如「永久删除」而非「确定」） |
| **Bottom Sheet（底部面板）** | 移动端短视频评论区、地图路线详情、外卖购物车等底部承载长内容场景 | 支持手势拖动与多档高度切换：**Peek（收起态）**仅露少量提示、**Half（半屏态）**展示核心信息、**Full（全屏态）**展示完整内容（如 30% / 60% / 90%）；需确保内容滚动与面板拖动手势不冲突 |
| **Action Sheet（底部操作菜单）** | 移动端点击更换头像、选择拍摄/相册等**单手快捷选择**场景 | 区别于 Bottom Sheet：仅用于快速点选操作，不承载复杂长内容；选项贴近屏幕底部保障单手可达；破坏性选项（如删除）使用 Danger 警示色区分 |
| **Snackbar（底部操作反馈条）** | “邮件已删除”“文件已移至回收站”等轻量状态反馈与即时补救 | 区别于 Toast：除提示外可附带**快捷操作（如「撤销」「重试」）**；遵循“能通过撤销解决的就不弹确认框”，减少打断用户连贯流程 |
| **Tour（漫游式引导）** | 新功能上线、产品改版或复杂流程的分步教学 | 通过**聚光灯高亮（Spotlight 聚焦）**使其他区域变暗，仅高亮当前入口并配合提示气泡说明；步数保持精简，**必须提供「跳过」入口**，禁止强制培训 |
| **Lightbox（图片灯箱）** | 商品详情、图片社区、作品集点击缩略图后的沉浸式媒体预览 | 点击后背景变暗放大展示，支持左右滑动切换及双指缩放查看，消除多余界面元素让用户完全聚焦媒体内容 |
| **Inline Alert（行内提示）** | 表单校验（如输入框下方邮箱格式错误）与局部上下文状态提示 | 区别于几秒自动消失的 Toast：**必须在问题存在期间持续显示**直至用户修正或状态改变；就近展示具体原因与修改指引，避免一闪而过引发猜测 |

**保持项目内一致性。**

## 14. Icon & Image Style

**保持视觉一致性：**

**Icon：**
- 风格统一（Line / Filled / Duotone，选其一）
- 线宽一致（1.5px / 2px）
- 尺寸规范（16px / 20px / 24px）
- 视觉重量一致

**Icon 来源优先级：**
```
1. 项目既有图标体系（UI Library Icons / 项目图标组件）
2. 项目内既有 SVG 资源
3. 按统一风格新增 SVG 图标
```

**禁止混用：**
```
一个页面同时出现：
- Line icons
- Filled icons
- Emoji
- 3D icons
```

**禁止用 Emoji 充当 UI 图标：**

Emoji 是文本字符，不是图标。它无法统一控制尺寸、颜色、线宽、对齐和视觉重量，且在不同平台渲染差异极大，会直接破坏视觉一致性。因此界面中**不得**使用 Unicode Emoji 作为功能性图形。

**禁止使用 Emoji 的位置：**
```
按钮、菜单项、导航、Tab、面包屑
状态徽标、标签、表格状态列、卡片角标
空状态、错误状态、加载状态的插图或图标位
表单 label、placeholder、toast、弹窗文案中的装饰图形
CSS content 中作为图标
纯文字徽标冒充品牌图标（如用「PDF」「WORD」大字块代替图标）
```

**必须使用：**
```
图标体系中的已有图标（UI Library Icons）
项目内 SVG 图标 / 图标组件
统一视觉容器 + 矢量图标（如渐变底 + SVG）
```

**判定标准：** 该图形是否承担「按钮 / 状态 / 分类 / 操作」的语义符号职能？是，则必须是矢量图标；仅作为正文语气符号时也应优先删除，而非保留 Emoji。

**统一交给实现层：** 具体图标组件名、SVG 引入方式由 senior-frontend 按项目既有约定落地。

**Image：**
- 风格一致（照片 / 插画 / 3D，选择统一风格）
- 色调一致
- 质量统一（避免混合高清与模糊）

## 15. Responsive Information Strategy

**定义响应式信息组织策略：**

**Desktop（1440px+）：**
- 信息如何布局？（3-column / 2-column / sidebar）
- 主要内容区宽度？（固定 / 流式）
- 导航如何呈现？（横向 / 侧边）

**Tablet（768px-1024px）：**
- 哪些列合并？（3 → 2）
- 哪些内容重新组织？
- 导航如何调整？（折叠 / 简化）

**Mobile（<768px）：**
- 哪些内容优先显示？
- 哪些功能折叠/隐藏？
- 哪些区块堆叠？
- 导航如何处理？（汉堡菜单 / 底部 Tab）
- 移动端浮层与反馈选型（长内容优先 `Bottom Sheet`，快捷选择优先 `Action Sheet`，可逆操作用 `Snackbar` 撤销，见第 13 节）

**注意：**
ui-design 定义信息策略，具体 CSS 实现由 senior-frontend 完成。

## 16. Design Token

**优先复用现有 Design Tokens：**

**检查项目是否已有：**
- Color tokens
- Typography tokens
- Spacing tokens
- Radius tokens
- Shadow tokens
- Breakpoint tokens
- Motion tokens（duration / easing）

**如果已存在：**
禁止随意重新定义，必须复用。

**如果不存在：**
可以提议最小化 Design Token 集合，但不要单方面重构整个项目。

**提议格式：**
```javascript
// 颜色 tokens
--color-text-primary: #1F2937
--color-text-secondary: #6B7280
--color-primary: #3B82F6

// 间距 tokens
--spacing-xs: 4px
--spacing-sm: 8px
--spacing-md: 16px
--spacing-lg: 24px

// 圆角 tokens（按第 10 节形状语言定义）
--radius-sm: 6px        // 小圆角
--radius-md: 12px       // 中圆角（容器）
--radius-full: 9999px   // 长圆 / 胶囊（交互元素）

// 图标尺寸 tokens（图标一律用矢量图标，不用 Emoji）
--icon-sm: 16px
--icon-md: 20px
--icon-lg: 24px

// 字号 tokens
--text-display: 32px
--text-h1: 24px
--text-body: 14px

// 动效 tokens（时长克制 + 自然缓动）
--duration-fast: 150ms      // 按钮反馈、悬停等微交互（建议 100-200ms）
--duration-normal: 250ms    // 卡片入场、浮层/弹窗显隐
--duration-slow: 400ms      // 复杂滚动叙事或数值过渡
--ease-out: cubic-bezier(0.16, 1, 0.3, 1)
```

## 17. 动效设计与沟通规范（Motion Design & Vocabulary）

**动效的核心在于辅助交互、引导注意力与建立空间连贯性，而非盲目堆砌炫技。**

### 一、动效设计 3 大核心原则

1. **严格控制动效时长（Duration Control）**：
   - 按钮点击、Hover 等微交互建议控制在 **100~200ms**，避免操作迟滞感。
   - 进场与展开可适当略长（如 200~350ms），离场与收起适当加快；避免长动画阻塞用户连续操作。
2. **设计约束优先高性能属性（GPU Compositor-First）**：
   - 动效设计应围绕位移、缩放、旋转与透明度变化展开（对应工程层的 `Transform` 与 `Opacity`），避免设计依赖宽高变化或频繁重排的布局跳变。
3. **尊重系统“减少动态效果”设置（Reduced Motion）**：
   - 所有非必要装饰性动效、视差、持续滚动与大幅位移都必须允许在系统开启“减少动态效果”时降级关闭，且静态状态下信息仍完整可读。

### 二、常见 UI 动效标准词库与场景选型

与研发或 AI 沟通动效需求时，禁止使用“酷炫”“丝滑”等模糊表述，应直接选用下列标准名称并明确克制边界：

#### 1. 微交互反馈与缓动（通用基础）

| 标准名称 | 视觉与交互行为 | 适用场景 | 设计约束 |
|------|------|------|------|
| **Ease Out（缓动效果）** | 开始较快、接近终点逐渐减速停止，比线性匀速更自然 | 几乎所有进场、浮层展开、状态切换（离场可适当加快） | 作为界面默认缓动基线，不建议高频交互用 `linear` |

#### 2. 视觉聚焦与卡片质感（局部强调，严禁滥用）

| 标准名称 | 视觉与交互行为 | 适用场景 | 设计约束 |
|------|------|------|------|
| **Spotlight（鼠标聚光灯）** | 移入卡片时跟随鼠标位置呈现柔和光晕照亮与明暗变化 | 产品特性卡、功能模块展示、暗色/质感官网卡片 | 仅用于需要探索感的功能展示区；常规 B 端列表与表单禁用 |
| **Border Beam（边缘流光）** | 一束光线沿卡片边框循环移动形成流动光效 | **推荐套餐、会员升级卡、核心主推功能**的单点视觉强调 | **严禁全页卡片铺开**；必须控制单屏出现数量（≤1~2 处），避免退化为 AI 霓虹模板风 |

#### 3. 滚动与页面入场节奏

| 标准名称 | 视觉与交互行为 | 适用场景 | 设计约束 |
|------|------|------|------|
| **Scroll Reveal（滚动显示）** | 向下滚动时内容依次淡入或滑入视野，而非一次全部呈现 | 长篇官网、产品介绍页、Landing Page 营造阅读节奏 | 高频操作后台与数据表格禁用（需首屏即见、零等待） |
| **Stagger Animation（错峰动画）** | 一组同级元素按顺序间隔入场，避免齐刷刷出现造成视觉杂乱 | 卡片网格、导航菜单项、榜单列表首次入场 | 控制单组总延时上限（如前几项间隔 40~80ms），避免尾部元素久等 |
| **Scroll Storytelling（滚动叙事）** | 指定区域固定在视口内，文字、图片或演示随滚动进度同步切换 | 品牌官网、旗舰产品发布页 | 必须保证逻辑清晰、步进可控，且移动端/降级态可读 |
| **Parallax Scrolling（视差滚动）** | 滚动时前景与背景以不同速率移动（通常背景更慢），增强空间层次 | 大幅头图、品牌宣传背景、视觉分隔区 | 仅用于装饰背景与展示页；控制速率差，避免引发眩晕感 |

#### 4. 文本、数字、加载与循环陈列

| 标准名称 | 视觉与交互行为 | 适用场景 | 设计约束 |
|------|------|------|------|
| **Shimmer（流光扫过）** | 高光从元素表面快速划过 | 骨架屏加载（提示处理中）、极少数重点 CTA 按钮吸引注意力 | 骨架屏常规使用；重点按钮需极度克制，避免与 `Border Beam` 叠加 |
| **Count Up（数字递增）** | 数值从初始值快速滚动递增或递减至目标结果 | 数据看板、核心指标卡、活动倒计时 | 仅用于首屏或关键里程碑指标；实时高频刷新表格禁用反复重播 |
| **Typewriter（打字机效果）** | 文字逐字符依次打出并伴随闪烁光标 | AI 对话流式输出界面、Hero 区动态切换标语 | 不可用于大段静态说明文字（会强迫用户等待阅读） |
| **Text Scramble（文字解码）** | 先显示跳动随机字符再逐个还原为目标文本，配荧光边缘呈科技感 | 科技/赛博风品牌主标题、数字标语短句 | 严格限制在**极短主标题**且符合品牌气质时使用，正文与按钮严禁使用 |
| **Marquee（跑马灯）** | 内容沿水平方向无缝匀速循环滚动 | 合作品牌 Logo 墙、用户评价条、精选陈列带 | **必须明确要求支持鼠标悬停暂停（及聚焦暂停）**，速度保持平稳可读 |

### 三、AI 与团队动效沟通原则

**在设计文档或与 `senior-frontend` 协作时，采用「标准动效名 + 目标元素 + 时长/缓动曲线 + 降级预期」描述：**

```
❌ 模糊需求：给这个页面加点酷炫、丝滑的高级动画。
✅ 精准需求：
   - 按钮悬停与状态切换使用 Ease Out（150ms）
   - 会员推荐卡片单点使用 Border Beam 强调
   - 数据看板核心 KPI 使用 Count Up，数据加载态使用 Shimmer 骨架屏
   - 合作品牌 Logo 区使用 Marquee，支持悬停暂停
```

**职责落地边界：** 本 Skill 负责确定动效选型、时长与克制边界；具体的 `transform` / `opacity` 性能优化、`IntersectionObserver` 与 `prefers-reduced-motion` 工程实现由 `senior-frontend`（第 16 节）落地。

</Core_Design_Principles>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- UI REVIEW                                                                    -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<UI_Review>

**设计完成后进行 UI Review：**

**检查维度：**

✅ **页面目标清晰** — 用户知道这是什么页面，做什么用

✅ **信息架构清晰** — Primary/Secondary/Supporting 层级明确

✅ **视觉层级清晰** — 重要内容视觉突出

✅ **Typography 一致** — 遵循统一字体系统

✅ **Color System 一致** — 颜色使用符合定义的调色板

✅ **Spacing 一致** — 间距遵循统一体系

✅ **Density 合理** — 信息密度符合页面类型

✅ **CTA 层级清晰且确认/反馈分级合理** — Primary/Secondary 明确；不可逆高风险使用 `Alert Dialog`（禁用遮罩关闭、明确后果与动词文案），可逆操作优先 `Snackbar` 撤销，表单校验使用常驻 `Inline Alert`

✅ **Component Visual Language 一致** — 组件风格统一，移动端浮层/反馈选型符合第 13 节规范（区分 `Bottom Sheet` 与 `Action Sheet` 等端侧边界）

✅ **形状语言一致（胶囊交互 + 圆角容器，无生硬直角）** — 按钮/标签/筛选器等交互元素采用长圆胶囊边框，容器采用温和圆角，无 `0px` 直角边框

✅ **边框/阴影策略一致** — 分隔手段统一，无边框与重阴影叠加

✅ **无明显 AI 模板化** — 不是典型蓝紫渐变 + 圆角卡片堆叠，也未全页滥用 `Border Beam` / `Spotlight` 等强炫技特效

✅ **动效克制且命名清晰** — 按第 17 节选用标准动效名称与缓动曲线，微交互时长控制在 100~200ms，无干扰核心任务的冗余动画

✅ **Icon/Image 风格一致** — 视觉风格统一

✅ **无 Emoji 充当图标** — 按钮/状态/标签/空状态/文案中无 Emoji 图形，均使用项目图标体系或 SVG

✅ **Responsive Strategy 清晰** — Desktop/Tablet/Mobile 信息组织明确

✅ **无明显视觉冲突** — 颜色/字体/间距无明显不协调

✅ **无 P0/P1 UI 问题**

**问题优先级：**

**P0（Critical）：**
- 页面目标不清晰
- 信息架构混乱
- 核心 CTA 不明显
- 严重视觉冲突

**P1（Major）：**
- 视觉层级不清晰
- Typography/Spacing 不一致
- 明显 AI 模板化
- CTA 层级混乱

**P2（Minor）：**
- 轻微视觉优化
- 非关键细节调整

**原则：**
P0/P1 必须修复，P2 不要无限扩大范围。

</UI_Review>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- DESIGN READY                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<DESIGN_READY_Definition>

**仅当满足所有条件时才声明 DESIGN READY：**

✅ **Page Goal Clear** — 页面目标明确

✅ **Information Architecture Clear** — 信息架构清晰

✅ **Visual Direction Clear** — 视觉方向明确（气质定位 + 形状语言 + 边框策略）

✅ **Hierarchy Clear** — 视觉层级清晰

✅ **Typography Consistent** — 字体系统一致

✅ **Color System Consistent** — 调色板一致

✅ **Spacing Consistent** — 间距系统一致

✅ **Density Appropriate** — 信息密度合理

✅ **CTA Hierarchy & Confirmation Grading Clear** — CTA 层级清晰；不可逆危险操作用 `Alert Dialog` 阻断误触，可逆操作优先 `Snackbar` 撤销，表单报错使用常驻 `Inline Alert`

✅ **Component Visual Language Consistent** — 组件视觉语言一致（含第 13 节 7 种反馈/浮层/移动端标准组件选型边界）

✅ **Shape Language Consistent** — 形状语言一致（交互件长圆胶囊 + 容器圆角，严禁生硬直角边框）

✅ **Border & Shadow Strategy Consistent** — 边框与阴影策略统一

✅ **Icon System Consistent** — 图标体系统一，且界面中不存在充当 UI 图标的 Emoji

✅ **Responsive Information Strategy Clear** — 响应式信息策略明确

✅ **No Obvious AI Template Pattern** — 无明显 AI 模板化

✅ **Motion Spec Clear & Restrained** — 动效选型使用第 17 节标准词汇，微交互时长克制（100~200ms），无滥用炫技特效

✅ **No Obvious Visual Conflict** — 无明显视觉冲突

✅ **No P0/P1 UI Issue** — 无 P0/P1 问题

**设计交付内容：**
- 视觉方向说明（气质定位、形状语言、边框与阴影策略）
- Design Tokens（如果创建，含 Motion Tokens）
- Component Visual Specs
- 图标体系约定（图标来源、风格、尺寸档位；界面不使用 Emoji 充当图标）
- Motion Specs（按需：使用的标准动效名、作用元素、时长/缓动建议及克制边界）
- Responsive Information Strategy
- 关键设计决策说明

</DESIGN_READY_Definition>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 与 senior-frontend 协作                                                      -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Collaboration_With_Senior_Frontend>

**ui-design 与 senior-frontend 的关系：**

```
ui-design        = What should it look like?（设计决策）
senior-frontend  = How should we build it correctly?（工程实现）
```

**路由规则（按任务类型选择入口）：**

```
无设计稿的新页面        → 本 Skill 定义设计 → DESIGN READY → senior-frontend 实现
已有成熟设计稿          → senior-frontend 忠实实现，本 Skill 不介入
现有页面视觉升级        → 本 Skill Review / Redesign → senior-frontend 实现
纯技术问题（Layout / Responsive / CSS Bug） → senior-frontend 直接修复
```

**职责边界清晰，避免重复工作。** 本 Skill 只做设计决策，不产出 CSS 与组件代码。

</Collaboration_With_Senior_Frontend>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 与已有设计的关系                                                             -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Respect_Existing_Design>

**如果用户提供了设计输入：**

**Design / Screenshot / Figma / Prototype**

**首先判断任务类型：**

**A. Fidelity Implementation（忠实实现）**
- 用户明确要求还原设计
- 设计稿已经成熟
- → **优先交给 senior-frontend**
- → **ui-design 不擅自 Redesign**

**B. Visual Optimization（视觉优化）**
- 用户要求"优化视觉"
- 设计稿存在明显问题
- → **ui-design 可以提出改进方案**

**C. Redesign（重新设计）**
- 用户明确要求重新设计
- 现有设计不符合需求
- → **ui-design 负责 Redesign**

**原则：**
尊重已有设计，不要擅自修改用户提供的设计方案。

</Respect_Existing_Design>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 与外部设计插件的关系                                                          -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Relationship_With_External_Design_Plugin>

**若环境中存在外部设计插件（如 Claude 官方 `frontend-design`）：**
- 提供品牌独特性指导
- 美学方向（aesthetic direction）
- Typography signature
- 避免模板化设计
- 设计风险决策

**ui-design（本 Skill）：**
- 提供系统化设计流程
- Information Architecture
- Design System 建立
- Component Visual Specs
- 跨 Agent 可携带（Cursor / Claude Code / Codex）

**关系：**
- 两者可共存，外部插件提供艺术指导，本 Skill 提供工程化设计流程
- 本 Skill 必须能独立工作，不依赖任何插件
- 环境中不存在该插件时，本 Skill 自行完成视觉方向定义

</Relationship_With_External_Design_Plugin>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 最终注释                                                                     -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Final_Notes>

## 本 Skill 的定位

**ui-design 负责：**
设计决策（What should it look like?）

**不负责：**
代码实现（How to build it?）

**协作伙伴：**
senior-frontend（实现设计）

## 适用边界

**本 Skill 覆盖：** 页面目标与用户任务、信息架构、视觉方向、形状语言、边框策略、Design System、组件视觉规范、动效选型与沟通词库、图标体系、响应式信息策略。

**本 Skill 不覆盖：** CSS 与组件实现、状态管理、API 与路由改动（均使用 `senior-frontend`）、后端业务实现（使用 `backend-development`）、数据库设计（使用 `mysql-design`）。

**方案中禁止使用 Emoji 充当图标**，应指定项目既有图标体系或 SVG 图标。

## 兼容性

本 Skill 设计为跨 Agent 可用：
- Cursor
- Claude Code
- Codex

核心设计方法论保持工具无关。

</Final_Notes>
