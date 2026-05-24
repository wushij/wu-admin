/**
 * 全局主题配置：Element Plus 色阶 + 语义化 CSS 变量
 */
import { generateColors } from '@element-plus/colors'
import { TinyColor } from '@ctrl/tinycolor'

export interface ThemePresetMeta {
  id: string
  label: string
  primary: string
}

export interface ThemeConfig {
  id?: string
  primaryColor: string
  primaryColorHover: string
  primaryColorActive: string
  /** 主色 8% 透明，用于菜单 hover、未读背景等 */
  primaryMuted: string
  /** 主色 14% 透明，用于子菜单底、打开态等 */
  primaryMutedStrong: string
  /** Logo 区渐变终点 */
  logoGradientEnd: string
  textColorBase: string
  textColor2: string
  borderColor: string
  bgColor: string
  sidebarBg: string
}

/** 精选预设（B）：低饱和、偏 SaaS 管理台气质 */
export const themePresetList: ThemePresetMeta[] = [
  { id: 'slate', label: '石墨', primary: '#010710' },
  { id: 'indigo', label: '靛蓝', primary: '#4f46e5' },
  { id: 'ocean', label: '海蓝', primary: '#2563eb' },
  { id: 'teal', label: '青绿', primary: '#0d9488' },
  { id: 'emerald', label: '翠绿', primary: '#059669' },
  { id: 'amber', label: '琥珀', primary: '#d97706' },
  { id: 'rose', label: '玫红', primary: '#e11d48' },
  { id: 'violet', label: '紫韵', primary: '#7c3aed' },
]

type PresetOverrides = Partial<
  Pick<ThemeConfig, 'primaryColorHover' | 'primaryColorActive' | 'logoGradientEnd' | 'bgColor' | 'sidebarBg'>
>

const presetOverrides: Record<string, PresetOverrides> = {
  slate: {
    primaryColorHover: '#0f1a2e',
    primaryColorActive: '#000000',
    logoGradientEnd: '#000000',
    bgColor: '#f3f5f8',
    sidebarBg: '#ffffff',
  },
  indigo: {
    logoGradientEnd: '#6366f1',
    bgColor: '#f5f3ff',
  },
  ocean: {
    logoGradientEnd: '#3b82f6',
    bgColor: '#eff6ff',
  },
  teal: {
    logoGradientEnd: '#14b8a6',
    bgColor: '#f0fdfa',
  },
  emerald: {
    logoGradientEnd: '#10b981',
    bgColor: '#ecfdf5',
  },
  amber: {
    logoGradientEnd: '#f59e0b',
    bgColor: '#fffbeb',
  },
  rose: {
    logoGradientEnd: '#f43f5e',
    bgColor: '#fff1f2',
  },
  violet: {
    logoGradientEnd: '#8b5cf6',
    bgColor: '#f5f3ff',
  },
}

/** 将颜色统一为 #RRGGBB */
export function toHexColor(color: string): string {
  return new TinyColor(color).toHexString()
}

/** Element Plus primary-dark-2 */
export function getPrimaryDark2(primaryColor: string): string {
  return new TinyColor(primaryColor).mix('#000000', 20).toHexString()
}

function primaryAlpha(primary: string, alpha: number): string {
  const { r, g, b } = new TinyColor(primary).toRgb()
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/** 主色微量混入页面背景（C） */
function tintedPageBg(primary: string, fallback = '#f8fafc'): string {
  return new TinyColor(fallback).mix(primary, 4).toHexString()
}

/**
 * 由主色生成完整主题配置（预设与自定义共用）
 */
export function buildThemeConfig(primaryColor: string, options?: PresetOverrides & { id?: string }): ThemeConfig {
  const base = toHexColor(primaryColor)
  const derived = deriveThemeFromPrimary(base, {
    primaryColorHover: options?.primaryColorHover,
    primaryColorActive: options?.primaryColorActive,
  })

  const logoGradientEnd =
    options?.logoGradientEnd ??
    new TinyColor(base).mix('#ffffff', 12).toHexString()

  return {
    id: options?.id,
    primaryColor: derived.primaryColor,
    primaryColorHover: derived.primaryColorHover,
    primaryColorActive: derived.primaryColorActive,
    primaryMuted: primaryAlpha(base, 0.08),
    primaryMutedStrong: primaryAlpha(base, 0.14),
    logoGradientEnd,
    textColorBase: '#1e293b',
    textColor2: '#64748b',
    borderColor: '#e2e8f0',
    bgColor: options?.bgColor ?? tintedPageBg(base),
    sidebarBg: options?.sidebarBg ?? '#ffffff',
  }
}

export const themePresets: Record<string, ThemeConfig> = Object.fromEntries(
  themePresetList.map((meta) => [
    meta.id,
    buildThemeConfig(meta.primary, { id: meta.id, ...presetOverrides[meta.id] }),
  ])
)

/** @deprecated 使用 themePresets.slate */
export const defaultTheme = themePresets.slate

export function applyElementPlusPrimaryPalette(primaryColor: string): void {
  const { primary } = generateColors(primaryColor)
  if (!primary?.length) return

  const root = document.documentElement
  const base = toHexColor(primary[0])

  root.style.setProperty('--el-color-primary', base)
  for (let i = 1; i <= 9; i++) {
    root.style.setProperty(`--el-color-primary-light-${i}`, toHexColor(primary[i]))
  }
  root.style.setProperty('--el-color-primary-dark-2', getPrimaryDark2(base))
  const { r, g, b } = new TinyColor(base).toRgb()
  root.style.setProperty('--el-color-primary-rgb', `${r}, ${g}, ${b}`)
}

export function deriveThemeFromPrimary(
  primaryColor: string,
  overrides?: Partial<Pick<ThemeConfig, 'primaryColorHover' | 'primaryColorActive'>>
): Pick<ThemeConfig, 'primaryColor' | 'primaryColorHover' | 'primaryColorActive'> {
  const { primary } = generateColors(primaryColor)
  const base = toHexColor(primary?.[0] ?? primaryColor)
  const hover = toHexColor(primary?.[2] ?? primaryColor)
  const active = getPrimaryDark2(base)

  return {
    primaryColor: base,
    primaryColorHover: overrides?.primaryColorHover ?? hover,
    primaryColorActive: overrides?.primaryColorActive ?? active,
  }
}

/** 兼容 localStorage 旧数据（按主色重新推导语义变量） */
export function normalizeThemeConfig(raw: Partial<ThemeConfig>): ThemeConfig {
  if (!raw.primaryColor) return themePresets.slate
  const primary = toHexColor(raw.primaryColor)
  const presetId = raw.id ?? findPresetIdByPrimary(primary)
  // 旧版石墨色自动升级到当前默认 #010710
  if (presetId && legacyPrimaryToPreset[primary] === presetId) {
    return themePresets[presetId]
  }
  return buildThemeConfig(primary, {
    id: presetId,
    ...(presetId ? presetOverrides[presetId] : {}),
  })
}

export function applyTheme(themeConfig: ThemeConfig): void {
  const theme = normalizeThemeConfig(themeConfig)
  const root = document.documentElement

  applyElementPlusPrimaryPalette(theme.primaryColor)

  root.style.setProperty('--theme-primary', theme.primaryColor)
  root.style.setProperty('--theme-primary-hover', theme.primaryColorHover)
  root.style.setProperty('--theme-primary-active', theme.primaryColorActive)
  root.style.setProperty('--theme-primary-muted', theme.primaryMuted)
  root.style.setProperty('--theme-primary-muted-strong', theme.primaryMutedStrong)
  root.style.setProperty('--theme-logo-end', theme.logoGradientEnd)
  root.style.setProperty('--theme-text-base', theme.textColorBase)
  root.style.setProperty('--theme-text-secondary', theme.textColor2)
  root.style.setProperty('--theme-border', theme.borderColor)
  root.style.setProperty('--theme-bg', theme.bgColor)
  root.style.setProperty('--theme-sidebar-bg', theme.sidebarBg)

  const { r, g, b } = new TinyColor(theme.primaryColor).toRgb()
  root.style.setProperty('--theme-primary-rgb', `${r}, ${g}, ${b}`)

  root.style.setProperty('--admin-radius-sm', '6px')
  root.style.setProperty('--admin-radius-md', '8px')
  root.style.setProperty('--admin-radius-lg', '12px')
  root.style.setProperty('--el-border-radius-base', '8px')
  root.style.setProperty('--el-border-radius-small', '6px')
}

export function getCurrentTheme(): ThemeConfig {
  const savedTheme = localStorage.getItem('theme-config')
  if (savedTheme) {
    try {
      return normalizeThemeConfig(JSON.parse(savedTheme))
    } catch {
      return themePresets.slate
    }
  }
  return themePresets.slate
}

export function saveTheme(themeConfig: ThemeConfig): void {
  const normalized = normalizeThemeConfig(themeConfig)
  localStorage.setItem('theme-config', JSON.stringify(normalized))
}

export function switchTheme(themeId: string): void {
  const themeConfig = themePresets[themeId] ?? themePresets.slate
  applyTheme(themeConfig)
  saveTheme(themeConfig)
}

/** 旧版 Ant Design 色板 → 新预设 id */
const legacyPrimaryToPreset: Record<string, string> = {
  '#1e293b': 'slate',
  '#111827': 'slate',
  '#1890ff': 'ocean',
  '#52c41a': 'emerald',
  '#fa8c16': 'amber',
  '#eb2f96': 'rose',
  '#722ed1': 'violet',
  '#13c2c2': 'teal',
  '#faad14': 'amber',
  '#f5222d': 'rose',
  '#6932c7': 'violet',
}

export function findPresetIdByPrimary(color: string): string | undefined {
  const hex = toHexColor(color)
  const matched = themePresetList.find((p) => toHexColor(p.primary) === hex)?.id
  if (matched) return matched
  return legacyPrimaryToPreset[hex]
}

/** @deprecated */
export function adjustColor(color: string, amount: number): string {
  const num = parseInt(color.slice(1), 16)
  const r = Math.min(255, Math.max(0, (num >> 16) + amount))
  const g = Math.min(255, Math.max(0, ((num >> 8) & 0x00ff) + amount))
  const b = Math.min(255, Math.max(0, (num & 0x0000ff) + amount))
  return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`
}
