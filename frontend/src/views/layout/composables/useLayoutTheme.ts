import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  DEFAULT_THEME_ID,
  themePresets,
  applyTheme,
  saveTheme,
  getCurrentTheme,
  buildThemeConfig,
  findPresetIdByPrimary,
  switchThemeWithTransition,
  getColorMode,
  switchColorModeWithTransition,
  resolveIsDark,
  type ColorMode,
  type ThemeTransitionOrigin,
} from '@/utils/theme'

export function useLayoutTheme() {
  const initialTheme = getCurrentTheme()
  const currentColor = ref(initialTheme.primaryColor)
  const activePresetIdRef = ref<string | undefined>(
    initialTheme.id && themePresets[initialTheme.id]
      ? initialTheme.id
      : findPresetIdByPrimary(initialTheme.primaryColor),
  )

  const storedColorMode =
    typeof localStorage !== 'undefined' ? localStorage.getItem('theme-color-mode') : null
  const colorMode = ref<ColorMode>(
    storedColorMode === 'light' || storedColorMode === 'dark' || storedColorMode === 'system'
      ? storedColorMode
      : initialTheme.id && typeof initialTheme.isDark === 'boolean'
        ? initialTheme.isDark
          ? 'dark'
          : 'light'
        : getColorMode(),
  )
  const isDark = ref(resolveIsDark(colorMode.value))
  const activePresetId = computed(() => activePresetIdRef.value)

  let mediaQuery: MediaQueryList | null = null

  function syncStateFromStorage() {
    const activeTheme = getCurrentTheme()
    currentColor.value = activeTheme.primaryColor
    activePresetIdRef.value =
      activeTheme.id && themePresets[activeTheme.id]
        ? activeTheme.id
        : findPresetIdByPrimary(activeTheme.primaryColor)
    isDark.value = resolveIsDark(colorMode.value)
  }

  function syncTheme() {
    isDark.value = resolveIsDark(colorMode.value)
    const presetId = activePresetIdRef.value ?? findPresetIdByPrimary(currentColor.value)
    if (presetId && themePresets[presetId]) {
      const preset = themePresets[presetId]
      if (Boolean(preset.isDark) !== isDark.value) {
        // 跟随系统明暗变化时，自动切到对应明暗的高定皮肤
        void switchColorModeWithTransition(colorMode.value).then(syncStateFromStorage)
        return
      }
      applyTheme(preset, colorMode.value)
      return
    }
    applyTheme(buildThemeConfig(currentColor.value), colorMode.value)
  }

  async function handlePresetSelect(presetId: string, origin?: ThemeTransitionOrigin) {
    const preset = themePresets[presetId] ?? themePresets[DEFAULT_THEME_ID]
    activePresetIdRef.value = preset.id
    currentColor.value = preset.primaryColor
    colorMode.value = preset.isDark ? 'dark' : 'light'
    isDark.value = Boolean(preset.isDark)
    await switchThemeWithTransition(presetId, origin)
  }

  function handleColorChange(color: string | null) {
    if (!color) return
    currentColor.value = color
    const presetId = findPresetIdByPrimary(color)
    if (presetId && themePresets[presetId]) {
      void handlePresetSelect(presetId)
      return
    }
    activePresetIdRef.value = undefined
    const customTheme = buildThemeConfig(color)
    applyTheme(customTheme, colorMode.value)
    saveTheme(customTheme)
    isDark.value = resolveIsDark(colorMode.value)
  }

  async function handleColorModeChange(nextMode: ColorMode, origin?: ThemeTransitionOrigin) {
    colorMode.value = nextMode
    await switchColorModeWithTransition(nextMode, origin)
    syncStateFromStorage()
  }

  async function toggleDarkMode(origin?: ThemeTransitionOrigin) {
    const next: ColorMode = isDark.value ? 'light' : 'dark'
    await handleColorModeChange(next, origin)
  }

  function initTheme() {
    syncTheme()
  }

  function onSystemSchemeChange() {
    if (colorMode.value === 'system') {
      syncTheme()
    }
  }

  onMounted(() => {
    if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
      mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      mediaQuery.addEventListener?.('change', onSystemSchemeChange)
    }
  })

  onUnmounted(() => {
    mediaQuery?.removeEventListener?.('change', onSystemSchemeChange)
  })

  return {
    currentColor,
    colorMode,
    isDark,
    activePresetId,
    handlePresetSelect,
    handleColorChange,
    handleColorModeChange,
    toggleDarkMode,
    initTheme,
  }
}
