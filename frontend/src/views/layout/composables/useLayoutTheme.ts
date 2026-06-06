import { ref, computed } from 'vue'
import {
  themePresets,
  applyTheme,
  saveTheme,
  getCurrentTheme,
  buildThemeConfig,
  findPresetIdByPrimary,
  switchTheme,
} from '@/utils/theme'

export function useLayoutTheme() {
  const currentColor = ref(getCurrentTheme().primaryColor)
  const activePresetId = computed(() => findPresetIdByPrimary(currentColor.value))

  function handlePresetSelect(presetId: string) {
    switchTheme(presetId)
    currentColor.value = themePresets[presetId]?.primaryColor ?? themePresets.slate.primaryColor
  }

  function handleColorChange(color: string | null) {
    if (!color) return
    currentColor.value = color
    const presetId = findPresetIdByPrimary(color)
    if (presetId && themePresets[presetId]) {
      switchTheme(presetId)
      return
    }
    const customTheme = buildThemeConfig(color)
    applyTheme(customTheme)
    saveTheme(customTheme)
  }

  function initTheme() {
    handleColorChange(currentColor.value)
  }

  return {
    currentColor,
    activePresetId,
    handlePresetSelect,
    handleColorChange,
    initTheme,
  }
}
