import { defineComponent, h, type Component } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

/**
 * 现代高辨识度 AI 大模型智能微光星芒图标组件（AI 模型配置专属）
 */
export const AiModel = defineComponent({
  name: 'AiModel',
  render() {
    return h(
      'svg',
      {
        viewBox: '0 0 1024 1024',
        width: '1em',
        height: '1em',
        fill: 'currentColor',
        style: { verticalAlign: '-0.15em' }
      },
      [
        h('path', {
          d: 'M469.333 85.333c-14.933 170.667-119.466 313.6-288 381.867 168.534 68.267 273.067 211.2 288 381.867 14.934-170.667 119.467-313.6 288-381.867-168.533-68.267-273.066-211.2-288-381.867zm341.334 469.334c-8.534 85.333-59.734 157.866-145.067 192 85.333 34.133 136.533 106.667 145.067 192 8.533-85.333 59.733-157.867 145.066-192-85.333-34.134-136.533-106.667-145.066-192z'
        })
      ]
    )
  }
})

/**
 * 现代工作流流转与分支拓扑图标组件（流程中心专属）
 */
export const Workflow = defineComponent({
  name: 'Workflow',
  render() {
    return h(
      'svg',
      {
        viewBox: '0 0 1024 1024',
        width: '1em',
        height: '1em',
        fill: 'currentColor',
        style: { verticalAlign: '-0.15em' }
      },
      [
        // 起点节点 -> 分支流转 -> 审批节点经典拓扑
        h('path', {
          d: 'M341.333 213.333c0-70.688-57.312-128-128-128s-128 57.312-128 128c0 58.741 39.573 108.235 93.333 123.136v450.39c-53.76 14.901-93.333 64.395-93.333 123.136 0 70.688 57.312 128 128 128s128-57.312 128-128c0-58.741-39.573-108.235-93.333-123.136V454.4c64.128-24.896 114.773-77.941 136.32-144.384 41.536 29.867 92.416 47.984 148.672 47.984h136.32c14.901 53.76 64.395 93.333 123.136 93.333 70.688 0 128-57.312 128-128s-57.312-128-128-128c-58.741 0-108.235 39.573-123.136 93.333H626.667c-65.419 0-123.819-35.37-156.459-88.747 4.267-15.019 6.459-30.827 6.459-47.253 0-2.88-0.085-5.739-0.256-8.587 3.328-1.579 6.741-3.008 10.24-4.267zM213.333 149.333c35.349 0 64 28.651 64 64s-28.651 64-64 64-64-28.651-64-64 28.651-64 64-64zm0 650.667c35.349 0 64 28.651 64 64s-28.651 64-64 64-64-28.651-64-64 28.651-64 64-64zm597.334-512c35.349 0 64 28.651 64 64s-28.651 64-64 64-64-28.651-64-64 28.651-64 64-64z'
        })
      ]
    )
  }
})

/** 自定义图标集合（统一支持驼峰与小写索引） */
export const CUSTOM_ICONS: Record<string, Component> = {
  // 工作台：Element Plus 官方数据看板图标（风格与全站菜单完全统一）
  dashboard: ElementPlusIconsVue.DataAnalysis,
  Dashboard: ElementPlusIconsVue.DataAnalysis,
  // 安全风控：彻底弃用笨重黑实心盾牌，换为 Element Plus 官方经典的优雅安全锁 Lock
  Shield: ElementPlusIconsVue.Lock,
  shield: ElementPlusIconsVue.Lock,
  security: ElementPlusIconsVue.Lock,
  Security: ElementPlusIconsVue.Lock,
  Lock: ElementPlusIconsVue.Lock,
  lock: ElementPlusIconsVue.Lock,
  // AI 模型配置：现代 AI 智能微光星芒
  AiModel,
  aimodel: AiModel,
  sparkles: AiModel,
  Sparkles: AiModel,
  // 流程中心：标准工作流分支流转图标
  Workflow,
  workflow: Workflow,
  guide: ElementPlusIconsVue.Guide,
  Guide: ElementPlusIconsVue.Guide
}

/**
 * 菜单图标：兼容数据库中的旧命名（若依 / vue-element-admin 风格）与 Element Plus 组件名
 */
const LEGACY_ICON_MAP: Record<string, string> = {
  system: 'Setting',
  user: 'User',
  role: 'UserFilled',
  key: 'UserFilled',
  Key: 'UserFilled',
  peoples: 'UserFilled',
  'tree-table': 'Menu',
  tree: 'OfficeBuilding',
  document: 'Document',
  dashboard: 'DataAnalysis',
  monitor: 'Monitor',
  'data-line': 'DataLine',
  suitcase: 'Suitcase',
  checked: 'Checked',
  guide: 'Guide',
  list: 'List',
  form: 'Document',
  chart: 'TrendCharts',
  table: 'Grid',
  example: 'Collection',
  nested: 'FolderOpened',
  component: 'Grid',
  international: 'Place',
  theme: 'Brush',
  clipboard: 'DocumentCopy',
  excel: 'Document',
  zip: 'Folder',
  Folder: 'Folder',
  folder: 'Folder',
  folderopened: 'FolderOpened',
  FolderOpened: 'FolderOpened',
  files: 'Files',
  Files: 'Files',
  pdf: 'Document',
  tab: 'Menu',
  message: 'Message',
  email: 'Message',
  lock: 'Lock',
  bug: 'Warning',
  skill: 'Star',
  swagger: 'Connection',
  log: 'Document',
  online: 'Monitor',
  job: 'Timer',
  druid: 'Coin',
  server: 'Monitor',
  redis: 'Coin',
  build: 'Tools',
  hammer: 'Tools',
  Hammer: 'Tools',
  HammerOutline: 'Tools',
  DocumentOutline: 'Document',
  code: 'Document',
  swagger2: 'Connection',
  magicstick: 'Compass',
  MagicStick: 'Compass',
  ai: 'Compass',
  aicompass: 'Compass',
  operation: 'Operation',
  notebook: 'Notebook'
}

export function resolveMenuIcon(iconName?: string | null): Component {
  if (!iconName || !iconName.trim()) {
    return ElementPlusIconsVue.Menu
  }
  const raw = iconName.trim()
  if (CUSTOM_ICONS[raw]) {
    return CUSTOM_ICONS[raw]
  }
  const legacyKey = raw.toLowerCase()
  if (CUSTOM_ICONS[legacyKey]) {
    return CUSTOM_ICONS[legacyKey]
  }
  if (ElementPlusIconsVue[raw as keyof typeof ElementPlusIconsVue]) {
    return ElementPlusIconsVue[raw as keyof typeof ElementPlusIconsVue]
  }
  const mapped = LEGACY_ICON_MAP[legacyKey] ?? LEGACY_ICON_MAP[raw]
  if (mapped) {
    if (CUSTOM_ICONS[mapped]) {
      return CUSTOM_ICONS[mapped]
    }
    if (ElementPlusIconsVue[mapped as keyof typeof ElementPlusIconsVue]) {
      return ElementPlusIconsVue[mapped as keyof typeof ElementPlusIconsVue]
    }
  }
  return ElementPlusIconsVue.Menu
}
