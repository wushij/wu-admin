/**
 * 全局主题配置
 * 统一管理应用的主题色和样式变量
 */

// 主题色配置接口
export interface ThemeConfig {
  primaryColor: string
  primaryColorHover: string
  primaryColorActive: string
  textColorBase: string
  textColor2: string
  borderColor: string
  bgColor: string
}

// 预设主题色配置
export const themePresets: Record<string, ThemeConfig> = {
  default: {
    primaryColor: '#111827',      // 深灰蓝
    primaryColorHover: '#000000', // 纯黑（hover）
    primaryColorActive: '#374151', // 激活状态
    textColorBase: '#1F2937',     // 主要文字
    textColor2: '#6B7280',        // 次要文字
    borderColor: '#E5E7EB',       // 边框
    bgColor: '#F9FAFB'            // 背景色
  },
  blue: {
    primaryColor: '#1890ff',
    primaryColorHover: '#40a9ff',
    primaryColorActive: '#096dd9',
    textColorBase: '#1F2937',
    textColor2: '#6B7280',
    borderColor: '#E5E7EB',
    bgColor: '#F9FAFB'
  },
  green: {
    primaryColor: '#52c41a',
    primaryColorHover: '#73d13d',
    primaryColorActive: '#389e0d',
    textColorBase: '#1F2937',
    textColor2: '#6B7280',
    borderColor: '#E5E7EB',
    bgColor: '#F9FAFB'
  },
  orange: {
    primaryColor: '#fa8c16',
    primaryColorHover: '#ffa940',
    primaryColorActive: '#d46b08',
    textColorBase: '#1F2937',
    textColor2: '#6B7280',
    borderColor: '#E5E7EB',
    bgColor: '#F9FAFB'
  },
  pink: {
    primaryColor: '#eb2f96',
    primaryColorHover: '#f759ab',
    primaryColorActive: '#c41d7f',
    textColorBase: '#1F2937',
    textColor2: '#6B7280',
    borderColor: '#E5E7EB',
    bgColor: '#F9FAFB'
  },
  purple: {
    primaryColor: '#722ed1',
    primaryColorHover: '#9254de',
    primaryColorActive: '#531dab',
    textColorBase: '#1F2937',
    textColor2: '#6B7280',
    borderColor: '#E5E7EB',
    bgColor: '#F9FAFB'
  },
  cyan: {
    primaryColor: '#13c2c2',
    primaryColorHover: '#36cfc9',
    primaryColorActive: '#08979c',
    textColorBase: '#1F2937',
    textColor2: '#6B7280',
    borderColor: '#E5E7EB',
    bgColor: '#F9FAFB'
  },
  gold: {
    primaryColor: '#faad14',
    primaryColorHover: '#ffc53d',
    primaryColorActive: '#d48806',
    textColorBase: '#1F2937',
    textColor2: '#6B7280',
    borderColor: '#E5E7EB',
    bgColor: '#F9FAFB'
  },
  red: {
    primaryColor: '#f5222d',
    primaryColorHover: '#ff4d4f',
    primaryColorActive: '#cf1322',
    textColorBase: '#1F2937',
    textColor2: '#6B7280',
    borderColor: '#E5E7EB',
    bgColor: '#F9FAFB'
  },
  violet: {
    primaryColor: '#6932c7',
    primaryColorHover: '#8559d6',
    primaryColorActive: '#5125a8',
    textColorBase: '#1F2937',
    textColor2: '#6B7280',
    borderColor: '#E5E7EB',
    bgColor: '#F9FAFB'
  }
}

// 颜色调整函数
export function adjustColor(color: string, amount: number): string {
  const num = parseInt(color.slice(1), 16)
  const r = Math.min(255, Math.max(0, (num >> 16) + amount))
  const g = Math.min(255, Math.max(0, ((num >> 8) & 0x00FF) + amount))
  const b = Math.min(255, Math.max(0, (num & 0x0000FF) + amount))
  return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`
}

/**
 * 应用主题到DOM
 * @param themeConfig 主题配置
 */
export function applyTheme(themeConfig: ThemeConfig): void {
  const root = document.documentElement
  
  // Element Plus 主题变量
  root.style.setProperty('--el-color-primary', themeConfig.primaryColor)
  root.style.setProperty('--el-color-primary-light-3', themeConfig.primaryColorHover)
  root.style.setProperty('--el-color-primary-dark-2', themeConfig.primaryColorActive)
  
  // 自定义主题变量
  root.style.setProperty('--theme-primary', themeConfig.primaryColor)
  root.style.setProperty('--theme-primary-hover', themeConfig.primaryColorHover)
  root.style.setProperty('--theme-primary-active', themeConfig.primaryColorActive)
  root.style.setProperty('--theme-text-base', themeConfig.textColorBase)
  root.style.setProperty('--theme-text-secondary', themeConfig.textColor2)
  root.style.setProperty('--theme-border', themeConfig.borderColor)
  root.style.setProperty('--theme-bg', themeConfig.bgColor)

  // 管理页圆角（与 admin-page.scss 一致）
  root.style.setProperty('--admin-radius-sm', '6px')
  root.style.setProperty('--admin-radius-md', '8px')
  root.style.setProperty('--admin-radius-lg', '12px')
  root.style.setProperty('--el-border-radius-base', '8px')
  root.style.setProperty('--el-border-radius-small', '6px')
}

/**
 * 获取当前主题
 */
export function getCurrentTheme(): ThemeConfig {
  const savedTheme = localStorage.getItem('theme-config')
  if (savedTheme) {
    try {
      return JSON.parse(savedTheme)
    } catch {
      return themePresets.default
    }
  }
  return themePresets.default
}

/**
 * 保存主题配置
 */
export function saveTheme(themeConfig: ThemeConfig): void {
  localStorage.setItem('theme-config', JSON.stringify(themeConfig))
}

/**
 * 切换主题
 */
export function switchTheme(themeName: string): void {
  const themeConfig = themePresets[themeName] || themePresets.default
  applyTheme(themeConfig)
  saveTheme(themeConfig)
}
