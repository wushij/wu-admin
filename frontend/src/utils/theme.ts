/**
 * 全局主题配置：复刻 EduMind 官方视觉主题体系（6 套原生 + 2 套同源高定拓展）
 * 支持专属纸感/暗夜表面色、微光背景渐变、Element Plus 色阶与亮/暗双模式自适应
 */
import { generateColors } from '@element-plus/colors'
import { TinyColor } from '@ctrl/tinycolor'

export type ColorMode = 'light' | 'dark' | 'system'

export interface ThemeTransitionOrigin {
  clientX: number
  clientY: number
}

export interface ThemePreviewPalette {
  bg: string
  surface: string
  accent: string
  accent2: string
  text: string
  border: string
  mesh: string
}

export interface ThemePresetMeta {
  id: string
  label: string
  tagline: string
  isDark: boolean
  primary: string
  accent2: string
  preview: ThemePreviewPalette
}

export interface ThemeConfig {
  id?: string
  isDark?: boolean
  primaryColor: string
  primaryColorHover: string
  primaryColorActive: string
  /** 主色按钮/徽章上的高对比前景色 */
  onPrimary: string
  /** 主色 8% 透明，用于菜单 hover、未读背景等 */
  primaryMuted: string
  /** 主色 14% 透明，用于子菜单底、打开态等 */
  primaryMutedStrong: string
  /** Logo 区背景渐变（起止色） */
  logoGradientStart: string
  logoGradientEnd: string
  textColorBase: string
  textColor2: string
  textColorMuted: string
  borderColor: string
  borderSubtle: string
  bgColor: string
  surfaceColor: string
  elevatedColor: string
  subtleColor: string
  sidebarBg: string
  headerBg: string
  bgMesh: string
}

const COLOR_MODE_STORAGE_KEY = 'theme-color-mode'
const THEME_CONFIG_STORAGE_KEY = 'theme-config'
const LAST_LIGHT_PRESET_KEY = 'theme-last-light-id'
const LAST_DARK_PRESET_KEY = 'theme-last-dark-id'

/** 亮色 ↔ 暗夜专属高定皮肤配对表（防止暗夜专属皮肤误切白底、亮色纸感误染黑底） */
const PAIRED_THEME_MAP: Record<string, string> = {
  'nordic-slate': 'obsidian-aurora',
  'swiss-editorial': 'obsidian-aurora',
  'iris-atelier': 'obsidian-aurora',
  'emerald-lab': 'midnight-terminal',
  'amber-humanities': 'obsidian-gold',
  'obsidian-aurora': 'nordic-slate',
  'midnight-terminal': 'emerald-lab',
  'obsidian-gold': 'amber-humanities',
}

/**
 * 8 套高定视觉主题预设：
 * 1~6 源自 EduMind 官方视觉主题，7~8 为同源高定拓展（亮色莫兰迪暮紫 + 暗夜黑金）
 */
export const themePresetList: ThemePresetMeta[] = [
  {
    id: 'nordic-slate',
    label: '北欧极简蓝',
    tagline: '浅蓝灰底 · 深海靛',
    isDark: false,
    primary: '#2563eb',
    accent2: '#0ea5e9',
    preview: {
      bg: '#f1f5f9',
      surface: '#ffffff',
      accent: '#2563eb',
      accent2: '#0ea5e9',
      text: '#0f172a',
      border: '#dbe4f0',
      mesh: 'radial-gradient(circle at 85% 15%, rgba(37, 99, 235, 0.16), transparent 60%)',
    },
  },
  {
    id: 'swiss-editorial',
    label: '瑞士学术白',
    tagline: '暖白纸面 · 极简炭黑',
    isDark: false,
    primary: '#18181b',
    accent2: '#1d4ed8',
    preview: {
      bg: '#fafaf9',
      surface: '#ffffff',
      accent: '#18181b',
      accent2: '#1d4ed8',
      text: '#18181b',
      border: '#e4e4e7',
      mesh: 'radial-gradient(circle at 88% 12%, rgba(29, 78, 216, 0.1), transparent 55%)',
    },
  },
  {
    id: 'emerald-lab',
    label: '松石实验室',
    tagline: '温润青白 · 祖母绿',
    isDark: false,
    primary: '#059669',
    accent2: '#0d9488',
    preview: {
      bg: '#f2fbf8',
      surface: '#ffffff',
      accent: '#059669',
      accent2: '#0d9488',
      text: '#064e3b',
      border: '#d1fae5',
      mesh: 'radial-gradient(circle at 85% 18%, rgba(16, 185, 129, 0.16), transparent 60%)',
    },
  },
  {
    id: 'amber-humanities',
    label: '暖金讲堂',
    tagline: '象牙纸感 · 琥珀绛红',
    isDark: false,
    primary: '#b45309',
    accent2: '#991b1b',
    preview: {
      bg: '#fdf9f3',
      surface: '#ffffff',
      accent: '#b45309',
      accent2: '#991b1b',
      text: '#292524',
      border: '#ede0d0',
      mesh: 'radial-gradient(circle at 85% 18%, rgba(217, 119, 6, 0.15), transparent 60%)',
    },
  },
  {
    id: 'iris-atelier',
    label: '暮紫画廊',
    tagline: '丁香雾纸 · 莫兰迪紫',
    isDark: false,
    primary: '#5b46e5',
    accent2: '#8b5cf6',
    preview: {
      bg: '#f6f5fc',
      surface: '#ffffff',
      accent: '#5b46e5',
      accent2: '#8b5cf6',
      text: '#1e1b4b',
      border: '#e4e0f5',
      mesh: 'radial-gradient(circle at 85% 18%, rgba(91, 70, 229, 0.14), transparent 60%)',
    },
  },
  {
    id: 'obsidian-aurora',
    label: '黑曜极光',
    tagline: '曜石深黑 · 冰蓝极光',
    isDark: true,
    primary: '#38bdf8',
    accent2: '#818cf8',
    preview: {
      bg: '#060608',
      surface: '#121218',
      accent: '#38bdf8',
      accent2: '#818cf8',
      text: '#f1f5f9',
      border: 'rgba(255, 255, 255, 0.14)',
      mesh: 'radial-gradient(circle at 18% 18%, rgba(56, 189, 248, 0.22), transparent 60%), radial-gradient(circle at 86% 85%, rgba(129, 140, 248, 0.18), transparent 60%)',
    },
  },
  {
    id: 'midnight-terminal',
    label: '极客终端夜',
    tagline: '暗夜工程 · 荧光青绿',
    isDark: true,
    primary: '#10b981',
    accent2: '#06b6d4',
    preview: {
      bg: '#080b12',
      surface: '#111623',
      accent: '#10b981',
      accent2: '#06b6d4',
      text: '#f1f5f9',
      border: 'rgba(51, 65, 85, 0.65)',
      mesh: 'radial-gradient(circle at 22% 18%, rgba(16, 185, 129, 0.24), transparent 60%), radial-gradient(circle at 85% 85%, rgba(6, 182, 212, 0.22), transparent 60%)',
    },
  },
  {
    id: 'obsidian-gold',
    label: '黑曜鎏金',
    tagline: '碳晶玄黑 · 香槟琥珀',
    isDark: true,
    primary: '#f59e0b',
    accent2: '#ea580c',
    preview: {
      bg: '#0a0908',
      surface: '#161412',
      accent: '#f59e0b',
      accent2: '#fb923c',
      text: '#fafaf9',
      border: 'rgba(245, 158, 11, 0.24)',
      mesh: 'radial-gradient(circle at 20% 18%, rgba(245, 158, 11, 0.22), transparent 60%), radial-gradient(circle at 86% 85%, rgba(234, 88, 12, 0.2), transparent 60%)',
    },
  },
]

export const DEFAULT_THEME_ID = 'swiss-editorial'

type PresetOverrides = Partial<ThemeConfig>

const presetOverrides: Record<string, PresetOverrides> = {
  'nordic-slate': {
    isDark: false,
    primaryColorHover: '#1d4ed8',
    primaryColorActive: '#1e40af',
    onPrimary: '#ffffff',
    logoGradientStart: '#1e3a8a',
    logoGradientEnd: '#2563eb',
    textColorBase: '#0f172a',
    textColor2: '#475569',
    textColorMuted: '#64748b',
    borderColor: '#dbe4f0',
    borderSubtle: '#eaf0f7',
    bgColor: '#f1f5f9',
    surfaceColor: '#ffffff',
    elevatedColor: '#ffffff',
    subtleColor: '#f8fafc',
    sidebarBg: '#ffffff',
    headerBg: 'rgba(255, 255, 255, 0.88)',
    bgMesh:
      'radial-gradient(ellipse 70% 50% at 88% 8%, rgba(37, 99, 235, 0.08), transparent 55%), radial-gradient(ellipse 60% 50% at 10% 92%, rgba(14, 165, 233, 0.06), transparent 55%)',
  },
  'swiss-editorial': {
    isDark: false,
    primaryColorHover: '#27272a',
    primaryColorActive: '#09090b',
    onPrimary: '#ffffff',
    logoGradientStart: '#18181b',
    logoGradientEnd: '#27272a',
    textColorBase: '#18181b',
    textColor2: '#52525b',
    textColorMuted: '#71717a',
    borderColor: '#e4e4e7',
    borderSubtle: '#f1f1f4',
    bgColor: '#fafaf9',
    surfaceColor: '#ffffff',
    elevatedColor: '#ffffff',
    subtleColor: '#f4f4f5',
    sidebarBg: '#ffffff',
    headerBg: 'rgba(255, 255, 255, 0.9)',
    bgMesh:
      'radial-gradient(circle at 92% 8%, rgba(29, 78, 216, 0.045), transparent 45%), radial-gradient(circle at 8% 92%, rgba(24, 24, 27, 0.03), transparent 45%)',
  },
  'emerald-lab': {
    isDark: false,
    primaryColorHover: '#047857',
    primaryColorActive: '#065f46',
    onPrimary: '#ffffff',
    logoGradientStart: '#065f46',
    logoGradientEnd: '#0d9488',
    textColorBase: '#064e3b',
    textColor2: '#2c5e52',
    textColorMuted: '#588278',
    borderColor: '#d1fae5',
    borderSubtle: '#e6f8f0',
    bgColor: '#f2fbf8',
    surfaceColor: '#ffffff',
    elevatedColor: '#ffffff',
    subtleColor: '#ecfdf5',
    sidebarBg: '#ffffff',
    headerBg: 'rgba(255, 255, 255, 0.88)',
    bgMesh:
      'radial-gradient(ellipse 75% 55% at 88% 12%, rgba(16, 185, 129, 0.09), transparent 55%), radial-gradient(ellipse 60% 50% at 12% 88%, rgba(20, 184, 166, 0.07), transparent 55%)',
  },
  'amber-humanities': {
    isDark: false,
    primaryColorHover: '#92400e',
    primaryColorActive: '#78350f',
    onPrimary: '#ffffff',
    logoGradientStart: '#78350f',
    logoGradientEnd: '#991b1b',
    textColorBase: '#292524',
    textColor2: '#57534e',
    textColorMuted: '#78716c',
    borderColor: '#ede0d0',
    borderSubtle: '#f5ede2',
    bgColor: '#fdf9f3',
    surfaceColor: '#ffffff',
    elevatedColor: '#ffffff',
    subtleColor: '#fef7ed',
    sidebarBg: '#ffffff',
    headerBg: 'rgba(255, 253, 249, 0.9)',
    bgMesh:
      'radial-gradient(ellipse 75% 50% at 88% 10%, rgba(217, 119, 6, 0.08), transparent 55%), radial-gradient(ellipse 60% 45% at 10% 90%, rgba(185, 28, 28, 0.05), transparent 55%)',
  },
  'iris-atelier': {
    isDark: false,
    primaryColorHover: '#4c38c7',
    primaryColorActive: '#3b28a8',
    onPrimary: '#ffffff',
    logoGradientStart: '#3730a3',
    logoGradientEnd: '#6d28d9',
    textColorBase: '#1e1b4b',
    textColor2: '#4c4675',
    textColorMuted: '#78739e',
    borderColor: '#e4e0f5',
    borderSubtle: '#efecf9',
    bgColor: '#f6f5fc',
    surfaceColor: '#ffffff',
    elevatedColor: '#ffffff',
    subtleColor: '#f3f1fb',
    sidebarBg: '#ffffff',
    headerBg: 'rgba(255, 255, 255, 0.88)',
    bgMesh:
      'radial-gradient(ellipse 75% 50% at 88% 10%, rgba(91, 70, 229, 0.085), transparent 55%), radial-gradient(ellipse 60% 45% at 12% 88%, rgba(139, 92, 246, 0.065), transparent 55%)',
  },
  'obsidian-aurora': {
    isDark: true,
    primaryColorHover: '#7dd3fc',
    primaryColorActive: '#0284c7',
    onPrimary: '#082f49',
    logoGradientStart: '#0c0c10',
    logoGradientEnd: '#0c2a42',
    textColorBase: '#f1f5f9',
    textColor2: 'rgba(241, 245, 249, 0.72)',
    textColorMuted: 'rgba(241, 245, 249, 0.45)',
    borderColor: 'rgba(255, 255, 255, 0.08)',
    borderSubtle: 'rgba(255, 255, 255, 0.05)',
    bgColor: '#060608',
    surfaceColor: '#121218',
    elevatedColor: '#18181f',
    subtleColor: '#0c0c10',
    sidebarBg: '#09090d',
    headerBg: 'rgba(12, 12, 16, 0.86)',
    bgMesh:
      'radial-gradient(ellipse 75% 50% at 14% 6%, rgba(56, 189, 248, 0.085), transparent 58%), radial-gradient(ellipse 65% 48% at 88% 90%, rgba(129, 140, 248, 0.075), transparent 58%)',
  },
  'midnight-terminal': {
    isDark: true,
    primaryColorHover: '#34d399',
    primaryColorActive: '#059669',
    onPrimary: '#022c22',
    logoGradientStart: '#090d16',
    logoGradientEnd: '#064e3b',
    textColorBase: '#f1f5f9',
    textColor2: '#cbd5e1',
    textColorMuted: '#94a3b8',
    borderColor: 'rgba(51, 65, 85, 0.55)',
    borderSubtle: 'rgba(51, 65, 85, 0.34)',
    bgColor: '#080b12',
    surfaceColor: '#111623',
    elevatedColor: '#171e2f',
    subtleColor: '#0e131e',
    sidebarBg: '#0b101b',
    headerBg: 'rgba(11, 16, 27, 0.88)',
    bgMesh:
      'radial-gradient(ellipse 80% 55% at 20% 15%, rgba(16, 185, 129, 0.12), transparent 58%), radial-gradient(ellipse 65% 50% at 85% 88%, rgba(6, 182, 212, 0.11), transparent 58%)',
  },
  'obsidian-gold': {
    isDark: true,
    primaryColorHover: '#fbbf24',
    primaryColorActive: '#d97706',
    onPrimary: '#1c1917',
    logoGradientStart: '#141210',
    logoGradientEnd: '#451a03',
    textColorBase: '#fafaf9',
    textColor2: '#d6d3d1',
    textColorMuted: '#a8a29e',
    borderColor: 'rgba(217, 170, 99, 0.2)',
    borderSubtle: 'rgba(217, 170, 99, 0.11)',
    bgColor: '#0a0908',
    surfaceColor: '#161412',
    elevatedColor: '#1e1b18',
    subtleColor: '#12100e',
    sidebarBg: '#100f0d',
    headerBg: 'rgba(16, 15, 13, 0.88)',
    bgMesh:
      'radial-gradient(ellipse 75% 55% at 18% 12%, rgba(245, 158, 11, 0.12), transparent 58%), radial-gradient(ellipse 60% 50% at 88% 86%, rgba(234, 88, 12, 0.1), transparent 58%)',
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

/** 主色微量混入页面背景 */
function tintedPageBg(primary: string, fallback = '#f4f6f9'): string {
  return new TinyColor(fallback).mix(primary, 3).toHexString()
}

/** 根据主色亮度自动计算高对比按钮文字色 */
function resolveOnPrimary(primaryColor: string): string {
  return new TinyColor(primaryColor).getLuminance() > 0.34 ? '#0f172a' : '#ffffff'
}

/**
 * 解析当前生效的是否为暗黑模式
 */
export function resolveIsDark(mode: ColorMode = getColorMode()): boolean {
  if (mode === 'dark') return true
  if (mode === 'light') return false
  if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  return false
}

export function getColorMode(): ColorMode {
  if (typeof localStorage === 'undefined') return 'light'
  const raw = localStorage.getItem(COLOR_MODE_STORAGE_KEY)
  if (raw === 'dark' || raw === 'light' || raw === 'system') return raw
  return 'light'
}

export function saveColorMode(mode: ColorMode): void {
  if (typeof localStorage === 'undefined') return
  localStorage.setItem(COLOR_MODE_STORAGE_KEY, mode)
}

/**
 * 在暗黑模式下将极暗主色提升为高对比可读主色（避免黑底黑按钮）
 */
function adaptPrimaryForDark(primaryColor: string): string {
  const tc = new TinyColor(primaryColor)
  if (tc.getLuminance() < 0.06) {
    return '#38bdf8'
  }
  if (tc.getLuminance() < 0.14) {
    return tc.lighten(16).toHexString()
  }
  return toHexColor(primaryColor)
}

/**
 * 在亮色模式下将高亮主色加深为高对比可读主色（避免浅底亮字看不清）
 */
function adaptPrimaryForLight(primaryColor: string): string {
  const tc = new TinyColor(primaryColor)
  if (tc.getLuminance() > 0.36) {
    return tc.darken(16).toHexString()
  }
  return toHexColor(primaryColor)
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
    isDark: options?.isDark,
    primaryColor: derived.primaryColor,
    primaryColorHover: derived.primaryColorHover,
    primaryColorActive: derived.primaryColorActive,
    onPrimary: options?.onPrimary ?? resolveOnPrimary(derived.primaryColor),
    primaryMuted: primaryAlpha(base, options?.isDark ? 0.16 : 0.08),
    primaryMutedStrong: primaryAlpha(base, options?.isDark ? 0.24 : 0.14),
    logoGradientStart: options?.logoGradientStart ?? derived.primaryColor,
    logoGradientEnd,
    textColorBase: options?.textColorBase ?? '#0f172a',
    textColor2: options?.textColor2 ?? '#475569',
    textColorMuted: options?.textColorMuted ?? '#94a3b8',
    borderColor: options?.borderColor ?? '#e2e8f0',
    borderSubtle: options?.borderSubtle ?? '#eef2f6',
    bgColor: options?.bgColor ?? tintedPageBg(base),
    surfaceColor: options?.surfaceColor ?? '#ffffff',
    elevatedColor: options?.elevatedColor ?? '#ffffff',
    subtleColor: options?.subtleColor ?? '#f8fafc',
    sidebarBg: options?.sidebarBg ?? '#ffffff',
    headerBg: options?.headerBg ?? 'rgba(255, 255, 255, 0.88)',
    bgMesh: options?.bgMesh ?? 'none',
  }
}

export const themePresets: Record<string, ThemeConfig> = Object.fromEntries(
  themePresetList.map((meta) => [
    meta.id,
    buildThemeConfig(meta.primary, { id: meta.id, isDark: meta.isDark, ...presetOverrides[meta.id] }),
  ]),
)

/** @deprecated 使用 themePresets[DEFAULT_THEME_ID] */
export const defaultTheme = themePresets[DEFAULT_THEME_ID]

export function applyElementPlusPrimaryPalette(
  primaryColor: string,
  isDark = false,
  surfaceMixBase = '#111827',
): void {
  const effectivePrimary = isDark ? adaptPrimaryForDark(primaryColor) : primaryColor
  const { primary } = generateColors(effectivePrimary)
  if (!primary?.length) return

  const root = document.documentElement
  const base = toHexColor(primary[0])

  root.style.setProperty('--el-color-primary', base)
  if (isDark) {
    // 暗黑模式下 light-x 与当前主题深表面混色，杜绝刺眼浅白悬停块
    for (let i = 1; i <= 9; i++) {
      const ratio = i * 10
      const darkStep = new TinyColor(base).mix(surfaceMixBase, ratio).toHexString()
      root.style.setProperty(`--el-color-primary-light-${i}`, darkStep)
    }
    root.style.setProperty('--el-color-primary-dark-2', new TinyColor(base).lighten(12).toHexString())
  } else {
    for (let i = 1; i <= 9; i++) {
      root.style.setProperty(`--el-color-primary-light-${i}`, toHexColor(primary[i]))
    }
    root.style.setProperty('--el-color-primary-dark-2', getPrimaryDark2(base))
  }

  const { r, g, b } = new TinyColor(base).toRgb()
  root.style.setProperty('--el-color-primary-rgb', `${r}, ${g}, ${b}`)
}

export function deriveThemeFromPrimary(
  primaryColor: string,
  overrides?: Partial<Pick<ThemeConfig, 'primaryColorHover' | 'primaryColorActive'>>,
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

/** 兼容 localStorage 旧数据（按主色或历史预设 id 重新推导高定主题） */
export function normalizeThemeConfig(raw: Partial<ThemeConfig>): ThemeConfig {
  if (!raw.primaryColor) return defaultTheme
  const primary = toHexColor(raw.primaryColor)
  const rawId = raw.id ? (legacyIdToPreset[raw.id] ?? raw.id) : undefined
  const presetId = (rawId && themePresets[rawId] ? rawId : undefined) ?? findPresetIdByPrimary(primary)

  if (presetId && themePresets[presetId]) {
    // 如果旧版保存了旧 presetId 或旧主色，直接升级到对应的全新高定预设
    if (raw.id !== presetId || legacyPrimaryToPreset[primary] === presetId) {
      return themePresets[presetId]
    }
    return themePresets[presetId]
  }

  return buildThemeConfig(primary)
}

export function applyColorModeClass(mode: ColorMode = getColorMode()): boolean {
  const isDark = resolveIsDark(mode)
  if (typeof document !== 'undefined') {
    document.documentElement.classList.toggle('dark', isDark)
    document.documentElement.setAttribute('data-color-mode', mode)
  }
  return isDark
}

export function applyTheme(themeConfig: ThemeConfig, mode?: ColorMode): void {
  const theme = normalizeThemeConfig(themeConfig)
  const isNativePreset = Boolean(theme.id && themePresets[theme.id])
  // 官方预设皮肤严格锁定其原生态明暗气质（尤其是后 3 款暗夜专属皮肤禁止切亮色）
  const resolvedMode: ColorMode =
    isNativePreset && typeof theme.isDark === 'boolean'
      ? theme.isDark
        ? 'dark'
        : 'light'
      : (mode ?? getColorMode())
  const isDark = applyColorModeClass(resolvedMode)
  const root = document.documentElement

  const presetMatchesMode = isNativePreset && theme.isDark === isDark

  const effectivePrimary = isDark ? adaptPrimaryForDark(theme.primaryColor) : adaptPrimaryForLight(theme.primaryColor)
  const effectiveHover = presetMatchesMode
    ? theme.primaryColorHover
    : isDark
      ? new TinyColor(effectivePrimary).lighten(8).toHexString()
      : theme.primaryColorHover
  const effectiveActive = presetMatchesMode
    ? theme.primaryColorActive
    : isDark
      ? new TinyColor(effectivePrimary).darken(8).toHexString()
      : theme.primaryColorActive

  const effectiveSurface = presetMatchesMode
    ? theme.surfaceColor
    : isDark
      ? new TinyColor('#121218').mix(effectivePrimary, 2).toHexString()
      : theme.surfaceColor
  const effectiveElevated = presetMatchesMode
    ? theme.elevatedColor
    : isDark
      ? new TinyColor('#18181f').mix(effectivePrimary, 3).toHexString()
      : theme.elevatedColor
  const effectiveSubtle = presetMatchesMode
    ? theme.subtleColor
    : isDark
      ? new TinyColor('#0c0c10').mix(effectivePrimary, 2).toHexString()
      : theme.subtleColor
  const effectiveBg = presetMatchesMode
    ? theme.bgColor
    : isDark
      ? new TinyColor('#060608').mix(effectivePrimary, 2).toHexString()
      : theme.bgColor
  const effectiveSidebarBg = presetMatchesMode
    ? theme.sidebarBg
    : isDark
      ? new TinyColor('#09090d').mix(effectivePrimary, 2).toHexString()
      : theme.sidebarBg
  const effectiveHeaderBg = presetMatchesMode
    ? theme.headerBg
    : isDark
      ? 'rgba(12, 12, 16, 0.86)'
      : 'rgba(255, 255, 255, 0.88)'
  const effectiveBorder = presetMatchesMode
    ? theme.borderColor
    : isDark
      ? 'rgba(255, 255, 255, 0.08)'
      : theme.borderColor
  const effectiveBorderSubtle = presetMatchesMode
    ? theme.borderSubtle
    : isDark
      ? 'rgba(255, 255, 255, 0.05)'
      : theme.borderSubtle
  const effectiveTextBase = presetMatchesMode ? theme.textColorBase : isDark ? '#f1f5f9' : theme.textColorBase
  const effectiveText2 = presetMatchesMode
    ? theme.textColor2
    : isDark
      ? 'rgba(241, 245, 249, 0.72)'
      : theme.textColor2
  const effectiveTextMuted = presetMatchesMode
    ? theme.textColorMuted
    : isDark
      ? 'rgba(241, 245, 249, 0.45)'
      : theme.textColorMuted
  const effectiveLogoStart = presetMatchesMode
    ? theme.logoGradientStart
    : isDark
      ? '#0f172a'
      : effectivePrimary
  const effectiveLogoEnd = presetMatchesMode
    ? theme.logoGradientEnd
    : isDark
      ? new TinyColor(effectivePrimary).darken(18).toHexString()
      : theme.logoGradientEnd
  const effectiveOnPrimary = resolveOnPrimary(effectivePrimary)

  applyElementPlusPrimaryPalette(effectivePrimary, isDark, effectiveSurface)

  root.style.setProperty('--theme-primary', effectivePrimary)
  root.style.setProperty('--theme-primary-hover', effectiveHover)
  root.style.setProperty('--theme-primary-active', effectiveActive)
  root.style.setProperty('--theme-on-primary', effectiveOnPrimary)
  root.style.setProperty('--theme-primary-muted', primaryAlpha(effectivePrimary, isDark ? 0.16 : 0.08))
  root.style.setProperty('--theme-primary-muted-strong', primaryAlpha(effectivePrimary, isDark ? 0.24 : 0.14))
  root.style.setProperty('--theme-logo-start', effectiveLogoStart)
  root.style.setProperty('--theme-logo-end', effectiveLogoEnd)
  root.style.setProperty('--theme-text-base', effectiveTextBase)
  root.style.setProperty('--theme-text-secondary', effectiveText2)
  root.style.setProperty('--theme-text-muted', effectiveTextMuted)
  root.style.setProperty('--theme-border', effectiveBorder)
  root.style.setProperty('--theme-border-subtle', effectiveBorderSubtle)
  root.style.setProperty('--theme-bg', effectiveBg)
  root.style.setProperty('--theme-bg-dark', effectiveBg)
  root.style.setProperty('--theme-bg-mesh', presetMatchesMode ? theme.bgMesh : 'none')
  root.style.setProperty('--theme-surface', effectiveSurface)
  root.style.setProperty('--theme-elevated', effectiveElevated)
  root.style.setProperty('--theme-subtle', effectiveSubtle)
  root.style.setProperty('--theme-sidebar-bg', effectiveSidebarBg)
  root.style.setProperty('--theme-header-bg', effectiveHeaderBg)

  const { r, g, b } = new TinyColor(effectivePrimary).toRgb()
  root.style.setProperty('--theme-primary-rgb', `${r}, ${g}, ${b}`)

  root.style.setProperty('--admin-radius-sm', '8px')
  root.style.setProperty('--admin-radius-md', '12px')
  root.style.setProperty('--admin-radius-lg', '16px')
  root.style.setProperty('--el-border-radius-base', '12px')
  root.style.setProperty('--el-border-radius-small', '8px')
  root.style.setProperty('--el-border-radius-round', '9999px')
}

export function getCurrentTheme(): ThemeConfig {
  const savedTheme = typeof localStorage !== 'undefined' ? localStorage.getItem(THEME_CONFIG_STORAGE_KEY) : null
  if (savedTheme) {
    try {
      return normalizeThemeConfig(JSON.parse(savedTheme))
    } catch {
      return defaultTheme
    }
  }
  return defaultTheme
}

export function saveTheme(themeConfig: ThemeConfig): void {
  if (typeof localStorage === 'undefined') return
  const normalized = normalizeThemeConfig(themeConfig)
  localStorage.setItem(THEME_CONFIG_STORAGE_KEY, JSON.stringify(normalized))
}

function rememberPresetByMode(preset: ThemeConfig): void {
  if (typeof localStorage === 'undefined' || !preset.id) return
  if (preset.isDark) {
    localStorage.setItem(LAST_DARK_PRESET_KEY, preset.id)
  } else {
    localStorage.setItem(LAST_LIGHT_PRESET_KEY, preset.id)
  }
}

export function resolvePairedPresetId(currentPresetId: string, targetIsDark: boolean): string {
  if (typeof localStorage !== 'undefined') {
    const remembered = localStorage.getItem(targetIsDark ? LAST_DARK_PRESET_KEY : LAST_LIGHT_PRESET_KEY)
    if (remembered && themePresets[remembered] && Boolean(themePresets[remembered].isDark) === targetIsDark) {
      return remembered
    }
  }
  const paired = PAIRED_THEME_MAP[currentPresetId]
  if (paired && themePresets[paired]) {
    return paired
  }
  return targetIsDark ? 'obsidian-aurora' : DEFAULT_THEME_ID
}

export function switchTheme(themeId: string): void {
  const themeConfig = themePresets[themeId] ?? defaultTheme
  const mode: ColorMode = themeConfig.isDark ? 'dark' : 'light'
  rememberPresetByMode(themeConfig)
  saveColorMode(mode)
  applyTheme(themeConfig, mode)
  saveTheme(themeConfig)
}

export function switchColorMode(mode: ColorMode): void {
  const current = getCurrentTheme()
  const targetIsDark = resolveIsDark(mode)

  // 官方预设皮肤（含 3 款暗夜专属皮肤）切换明暗时，自动切换到对应明暗的高定皮肤，绝不强行变色破坏美感
  if (current.id && themePresets[current.id] && Boolean(current.isDark) !== targetIsDark) {
    const nextPresetId = resolvePairedPresetId(current.id, targetIsDark)
    const nextTheme = themePresets[nextPresetId] ?? defaultTheme
    rememberPresetByMode(nextTheme)
    saveColorMode(mode)
    applyTheme(nextTheme, nextTheme.isDark ? 'dark' : 'light')
    saveTheme(nextTheme)
    return
  }

  saveColorMode(mode)
  applyTheme(current, mode)
}

let activeThemeTransitionTimer: number | null = null

/**
 * 带 View Transitions 圆形扩散动画的主题切换容器（支持点击圆心扩散与低动效/旧浏览器降级）
 */
export async function runWithThemeTransition(
  applyFn: () => void,
  origin?: ThemeTransitionOrigin,
): Promise<void> {
  if (typeof document === 'undefined' || typeof window === 'undefined') {
    applyFn()
    return
  }

  const prefersReduced =
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches

  if (prefersReduced) {
    applyFn()
    return
  }

  const root = document.documentElement
  const x = origin?.clientX ?? Math.max(window.innerWidth - 130, window.innerWidth * 0.85)
  const y = origin?.clientY ?? 30
  const maxR = Math.hypot(
    Math.max(x, window.innerWidth - x),
    Math.max(y, window.innerHeight - y),
  )

  root.style.setProperty('--theme-vt-x', `${x}px`)
  root.style.setProperty('--theme-vt-y', `${y}px`)
  root.style.setProperty('--theme-vt-r', `${maxR}px`)

  const doc = document as Document & {
    startViewTransition?: (cb: () => void) => { finished: Promise<void> }
  }

  if (typeof doc.startViewTransition === 'function') {
    try {
      const transition = doc.startViewTransition(() => {
        applyFn()
      })
      await transition.finished
    } catch {
      // 忽略快速连击引起的 AbortError，保证状态已正确落盘
    }
    return
  }

  root.classList.add('theme-transitioning')
  applyFn()
  if (activeThemeTransitionTimer !== null) {
    window.clearTimeout(activeThemeTransitionTimer)
  }
  activeThemeTransitionTimer = window.setTimeout(() => {
    root.classList.remove('theme-transitioning')
    activeThemeTransitionTimer = null
  }, 320)
}

export async function switchThemeWithTransition(
  themeId: string,
  origin?: ThemeTransitionOrigin,
): Promise<void> {
  await runWithThemeTransition(() => {
    switchTheme(themeId)
  }, origin)
}

export async function switchColorModeWithTransition(
  mode: ColorMode,
  origin?: ThemeTransitionOrigin,
): Promise<void> {
  await runWithThemeTransition(() => {
    switchColorMode(mode)
  }, origin)
}

/** 历史预设 id 平滑升级映射 */
const legacyIdToPreset: Record<string, string> = {
  slate: 'swiss-editorial',
  indigo: 'iris-atelier',
  ocean: 'nordic-slate',
  teal: 'emerald-lab',
  emerald: 'emerald-lab',
  amber: 'amber-humanities',
  rose: 'amber-humanities',
  violet: 'iris-atelier',
}

/** 历史主色十六进制 → 新高定预设 id */
const legacyPrimaryToPreset: Record<string, string> = {
  '#010710': 'swiss-editorial',
  '#0f172a': 'swiss-editorial',
  '#1e293b': 'swiss-editorial',
  '#111827': 'swiss-editorial',
  '#1890ff': 'nordic-slate',
  '#4f46e5': 'iris-atelier',
  '#7c3aed': 'iris-atelier',
  '#722ed1': 'iris-atelier',
  '#6932c7': 'iris-atelier',
  '#52c41a': 'emerald-lab',
  '#0d9488': 'emerald-lab',
  '#13c2c2': 'emerald-lab',
  '#fa8c16': 'amber-humanities',
  '#d97706': 'amber-humanities',
  '#faad14': 'amber-humanities',
  '#e11d48': 'amber-humanities',
  '#eb2f96': 'iris-atelier',
  '#f5222d': 'amber-humanities',
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
