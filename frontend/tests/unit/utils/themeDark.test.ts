import { describe, it, expect, beforeEach } from 'vitest'
import {
  getColorMode,
  switchColorMode,
  switchTheme,
  resolveIsDark,
  applyTheme,
  themePresets,
} from '@/utils/theme'

describe('theme 亮/暗黑双模式主题引擎', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  it('切换暗黑模式时正确挂载 html.dark 并自动适配极深主色', () => {
    switchTheme('swiss-editorial')
    switchColorMode('dark')

    expect(getColorMode()).toBe('dark')
    expect(resolveIsDark('dark')).toBe(true)
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    const primaryInDark = document.documentElement.style.getPropertyValue('--el-color-primary')
    expect(primaryInDark).not.toBe('#010710')
    expect(primaryInDark).not.toBe('#18181b')
  })

  it('切换暗夜高定主题 obsidian-aurora 与亮色主题 iris-atelier 正确切换亮暗态', () => {
    switchTheme('obsidian-aurora')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    expect(document.documentElement.style.getPropertyValue('--el-color-primary')).toBe('#38bdf8')

    applyTheme(themePresets['iris-atelier'], 'light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
    expect(document.documentElement.style.getPropertyValue('--el-color-primary')).toBe('#5b46e5')
  })
})
