import * as ElementPlusIconsVue from '@element-plus/icons-vue'

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
  dashboard: 'HomeFilled',
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
  swagger2: 'Connection'
}

export function resolveMenuIcon(iconName?: string | null) {
  if (!iconName || !iconName.trim()) {
    return ElementPlusIconsVue.Menu
  }
  const raw = iconName.trim()
  if (ElementPlusIconsVue[raw as keyof typeof ElementPlusIconsVue]) {
    return ElementPlusIconsVue[raw as keyof typeof ElementPlusIconsVue]
  }
  const legacyKey = raw.toLowerCase()
  const mapped = LEGACY_ICON_MAP[legacyKey] ?? LEGACY_ICON_MAP[raw]
  if (mapped && ElementPlusIconsVue[mapped as keyof typeof ElementPlusIconsVue]) {
    return ElementPlusIconsVue[mapped as keyof typeof ElementPlusIconsVue]
  }
  return ElementPlusIconsVue.Menu
}
