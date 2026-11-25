import { onMounted, onUnmounted } from 'vue'
import type * as EchartsNamespace from 'echarts'

export interface ChartPalette {
  indigo: string
  sky: string
  teal: string
  violet: string
  amber: string
  rose: string
  /** 环形图主梯度色组 [起点, 终点] */
  ringGradient: [string, string]
  /** 多序列图表推荐调色盘 */
  seriesColors: string[]
}

export interface ChartThemeTokens {
  isDark: boolean
  primary: string
  danger: string
  textPrimary: string
  textSecondary: string
  textMuted: string
  border: string
  borderSubtle: string
  surfaceBg: string
  elevatedBg: string
  palette: ChartPalette
}

/**
 * 读取当前全局 CSS Design Tokens，并生成亮/暗双模式高定 ECharts 配色方案
 * 彻底替代旧版硬编码高饱和荧光绿（#67c23a）与暗黑下不可读的默认轴色。
 */
export function readChartTheme(): ChartThemeTokens {
  const root = document.documentElement
  const isDark = root.classList.contains('dark')
  const styles = getComputedStyle(root)
  const getVar = (name: string, fallback: string) => styles.getPropertyValue(name).trim() || fallback

  const palette: ChartPalette = isDark
    ? {
        indigo: '#818cf8',
        sky: '#38bdf8',
        teal: '#2dd4bf',
        violet: '#a78bfa',
        amber: '#fbbf24',
        rose: '#fb7185',
        ringGradient: ['#818cf8', '#38bdf8'],
        seriesColors: ['#818cf8', '#38bdf8', '#a78bfa', '#2dd4bf', '#fbbf24', '#fb7185'],
      }
    : {
        indigo: '#6366f1',
        sky: '#0ea5e9',
        teal: '#0d9488',
        violet: '#8b5cf6',
        amber: '#d97706',
        rose: '#f43f5e',
        ringGradient: ['#6366f1', '#0ea5e9'],
        seriesColors: ['#6366f1', '#0ea5e9', '#8b5cf6', '#0d9488', '#d97706', '#f43f5e'],
      }

  return {
    isDark,
    primary: getVar('--el-color-primary', palette.indigo),
    danger: getVar('--color-danger', palette.rose),
    textPrimary: getVar('--color-text-primary', isDark ? '#f1f5f9' : '#18181b'),
    textSecondary: getVar('--color-text-secondary', isDark ? 'rgba(241, 245, 249, 0.72)' : '#52525b'),
    textMuted: getVar('--color-text-muted', isDark ? 'rgba(241, 245, 249, 0.45)' : '#71717a'),
    border: getVar('--color-border', isDark ? 'rgba(255, 255, 255, 0.1)' : '#e4e4e7'),
    borderSubtle: getVar('--color-border-subtle', isDark ? 'rgba(255, 255, 255, 0.06)' : '#f1f1f4'),
    surfaceBg: getVar('--color-bg-surface', isDark ? '#121218' : '#ffffff'),
    elevatedBg: getVar('--color-bg-elevated', isDark ? '#18181f' : '#ffffff'),
    palette,
  }
}

export function buildChartTooltip(
  theme: ChartThemeTokens,
  trigger: 'axis' | 'item' = 'axis',
) {
  return {
    trigger,
    backgroundColor: theme.elevatedBg,
    borderColor: theme.border,
    borderWidth: 1,
    padding: [10, 14],
    textStyle: {
      color: theme.textPrimary,
      fontSize: 12.5,
    },
    ...(trigger === 'axis'
      ? {
          axisPointer: {
            type: 'line' as const,
            lineStyle: {
              color: theme.border,
              type: 'dashed' as const,
            },
          },
        }
      : {}),
  }
}

export function buildCategoryXAxis(labels: string[], theme: ChartThemeTokens) {
  return {
    type: 'category' as const,
    data: [...labels],
    boundaryGap: false,
    axisLine: { lineStyle: { color: theme.border } },
    axisTick: { show: false },
    axisLabel: { color: theme.textMuted, fontSize: 11 },
  }
}

export function buildValueYAxis(
  theme: ChartThemeTokens,
  extra: Record<string, unknown> = {},
) {
  return {
    type: 'value' as const,
    nameGap: 12,
    nameTextStyle: { color: theme.textMuted, fontSize: 11.5 },
    axisLabel: { color: theme.textMuted, fontSize: 11 },
    axisLine: { show: false },
    axisTick: { show: false },
    splitLine: { lineStyle: { color: theme.borderSubtle, type: 'dashed' as const } },
    ...extra,
  }
}

/**
 * 构建具备垂直渐变阴影与平滑线条的现代折线图系列
 */
export function createGradientAreaLineSeries(options: {
  echarts: typeof EchartsNamespace
  name?: string
  data: Array<number | null>
  color: string
  connectNulls?: boolean
}) {
  const { echarts, name, data, color, connectNulls } = options
  return {
    ...(name ? { name } : {}),
    type: 'line' as const,
    data: [...data],
    smooth: 0.4,
    ...(connectNulls !== undefined ? { connectNulls } : {}),
    symbol: 'circle',
    symbolSize: 6,
    showSymbol: data.length <= 2,
    lineStyle: {
      width: 2.5,
      color,
    },
    itemStyle: {
      color,
      borderWidth: 2,
      borderColor: '#ffffff',
    },
    areaStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: withHexAlpha(color, 0.28) },
        { offset: 1, color: withHexAlpha(color, 0.01) },
      ]),
    },
  }
}

function withHexAlpha(hexColor: string, alpha: number): string {
  const clean = hexColor.replace('#', '')
  if (clean.length !== 6) return hexColor
  const r = parseInt(clean.slice(0, 2), 16)
  const g = parseInt(clean.slice(2, 4), 16)
  const b = parseInt(clean.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/**
 * 监听全局明暗模式与主题皮肤切换，自动触发图表重绘并在组件卸载时注销
 */
export function useChartThemeObserver(onThemeChange: () => void | Promise<void>) {
  let observer: MutationObserver | null = null

  onMounted(() => {
    observer = new MutationObserver(() => {
      void onThemeChange()
    })
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class', 'style'],
    })
  })

  onUnmounted(() => {
    observer?.disconnect()
    observer = null
  })
}
