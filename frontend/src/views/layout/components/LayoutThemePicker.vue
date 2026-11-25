<template>
  <el-popover
    v-model:visible="pickerVisible"
    trigger="click"
    placement="bottom-end"
    :width="436"
    :show-arrow="false"
    popper-class="theme-picker-popper"
    @show="$emit('activate')"
  >
    <template #reference>
      <span class="dock-segment-ref">
        <el-tooltip content="外观与主题风格" placement="bottom" :show-after="1000" :disabled="pickerVisible">
          <button
            id="tour-theme-btn"
            type="button"
            class="dock-segment-btn"
            :class="{ 'is-active': isActive }"
            aria-label="外观与主题风格"
            @click="$emit('activate')"
          >
        <svg
          class="palette-svg"
          viewBox="0 0 24 24"
          width="15"
          height="15"
          fill="none"
          stroke="currentColor"
          stroke-width="1.9"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.563-2.512 5.563-5.563C22 6.5 17.5 2 12 2z" />
          <circle cx="7.5" cy="11.5" r="1" fill="currentColor" />
          <circle cx="12" cy="7.5" r="1" fill="currentColor" />
          <circle cx="16.5" cy="11.5" r="1" fill="currentColor" />
        </svg>
      </button>
        </el-tooltip>
      </span>
    </template>

    <div class="theme-picker-content">
      <div class="theme-picker-header">
        <div class="theme-picker-title-row">
          <span class="theme-picker-title">视觉主题皮肤</span>
          <span class="theme-picker-badge">5 亮 · 3 暗夜专属</span>
        </div>
        <span class="theme-picker-hint">前 5 款为高定纸感亮色，后 3 款仅限暗夜模式呈现</span>
      </div>

      <div class="mode-switch-group" role="radiogroup" aria-label="外观模式">
        <button
          v-for="m in COLOR_MODE_OPTIONS"
          :key="m.value"
          type="button"
          class="mode-switch-btn"
          :class="{ active: colorMode === m.value }"
          @click="(e) => onSelectColorMode(m.value, e)"
        >
          <el-icon :size="14"><component :is="m.icon" /></el-icon>
          <span>{{ m.label }}</span>
        </button>
      </div>

      <div class="preset-grid">
        <button
          v-for="item in themePresetList"
          :key="item.id"
          type="button"
          class="preset-card"
          :class="{ active: activePresetId === item.id, 'is-dark-preset': item.isDark }"
          @click="(e) => onSelectPreset(item.id, e)"
        >
          <!-- 微缩画布预览：还原底色、微光、悬浮表面、渐变主辅色与文字骨架 -->
          <span
            class="preset-swatch"
            :style="{
              backgroundColor: item.preview.bg,
              backgroundImage: item.preview.mesh,
              borderColor: item.preview.border,
            }"
          >
            <span
              class="swatch-surface"
              :style="{
                background: item.preview.surface,
                borderColor: item.preview.border,
              }"
            >
              <span
                class="swatch-accent"
                :style="{
                  background: `linear-gradient(90deg, ${item.preview.accent}, ${item.preview.accent2})`,
                }"
              />
              <span class="swatch-lines">
                <span class="swatch-line" :style="{ background: item.preview.text }" />
                <span class="swatch-line is-short" :style="{ background: item.preview.text }" />
              </span>
            </span>
          </span>

          <span class="preset-meta">
            <span class="preset-label-row">
              <span class="preset-label">{{ item.label }}</span>
              <el-tooltip
                v-if="item.isDark"
                content="暗夜专属皮肤，不可切换为亮色"
                placement="top"
                :show-after="1000"
              >
                <span class="preset-dark-tag">
                  <el-icon><component :is="ElementPlusIconsVue.Moon" /></el-icon>
                  <span>暗夜</span>
                </span>
              </el-tooltip>
            </span>
            <span class="preset-tagline">{{ item.tagline }}</span>
          </span>

          <span v-if="activePresetId === item.id" class="preset-check-badge">
            <el-icon><component :is="ElementPlusIconsVue.Check" /></el-icon>
          </span>
        </button>
      </div>

      <div class="theme-custom-row">
        <span class="theme-custom-label">自定义品牌主色</span>
        <el-color-picker
          :model-value="currentColor"
          :show-alpha="false"
          size="small"
          @update:model-value="$emit('colorChange', $event)"
        />
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { themePresetList, type ColorMode, type ThemeTransitionOrigin } from '@/utils/theme'

const pickerVisible = ref(false)

const COLOR_MODE_OPTIONS: Array<{ value: ColorMode; label: string; icon: unknown }> = [
  { value: 'light', label: '亮色', icon: ElementPlusIconsVue.Sunny },
  { value: 'dark', label: '暗黑', icon: ElementPlusIconsVue.Moon },
  { value: 'system', label: '跟随系统', icon: ElementPlusIconsVue.Monitor },
]

defineProps<{
  currentColor: string
  colorMode: ColorMode
  activePresetId: string | undefined
  isActive?: boolean
}>()

const emit = defineEmits<{
  activate: []
  presetSelect: [presetId: string, origin?: ThemeTransitionOrigin]
  colorChange: [color: string | null]
  colorModeChange: [mode: ColorMode, origin?: ThemeTransitionOrigin]
}>()

function onSelectPreset(presetId: string, e: MouseEvent) {
  emit('presetSelect', presetId, { clientX: e.clientX, clientY: e.clientY })
  pickerVisible.value = false
}

function onSelectColorMode(mode: ColorMode, e: MouseEvent) {
  emit('colorModeChange', mode, { clientX: e.clientX, clientY: e.clientY })
  pickerVisible.value = false
}
</script>

<style scoped lang="scss">
.dock-segment-ref {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.palette-svg {
  display: block;
  flex-shrink: 0;
}

.theme-picker-content {
  padding: 2px;
}

.theme-picker-header {
  padding: 0 2px 12px;
  border-bottom: 1px solid var(--color-border-subtle);
  margin-bottom: 12px;
}

.theme-picker-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.theme-picker-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: -0.01em;
}

.theme-picker-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.theme-picker-hint {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.mode-switch-group {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  padding: 3px;
  border-radius: var(--radius-pill);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  margin-bottom: 12px;
}

.mode-switch-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 30px;
  border: none;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);

  &.active {
    background: var(--capsule-pearl-ice-bg);
    border: 1px solid var(--capsule-pearl-ice-border);
    color: var(--capsule-pearl-ice-text);
    box-shadow: var(--capsule-pearl-ice-shadow);
    font-weight: 600;
  }
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.preset-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  cursor: pointer;
  text-align: left;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);

  &:hover {
    border-color: var(--el-color-primary);
    background: var(--color-bg-surface);
    box-shadow: var(--shadow-xs);
  }

  &.active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    box-shadow: 0 0 0 2px var(--color-primary-muted);
  }
}

.preset-swatch {
  width: 48px;
  height: 36px;
  border-radius: var(--radius-sm);
  border: 1px solid;
  padding: 4px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.swatch-surface {
  width: 100%;
  height: 100%;
  border-radius: 5px;
  border: 1px solid;
  padding: 4px 5px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.swatch-accent {
  width: 18px;
  height: 4px;
  border-radius: var(--radius-pill);
}

.swatch-lines {
  display: flex;
  flex-direction: column;
  gap: 2.5px;
}

.swatch-line {
  width: 100%;
  height: 2.5px;
  border-radius: var(--radius-pill);
  opacity: 0.72;

  &.is-short {
    width: 64%;
    opacity: 0.42;
  }
}

.preset-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.preset-label-row {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-right: 14px;
}

.preset-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preset-dark-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 5px;
  border-radius: var(--radius-pill);
  font-size: 10px;
  font-weight: 500;
  color: var(--color-text-muted);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border-subtle);
  flex-shrink: 0;
}

.preset-tagline {
  font-size: 11px;
  color: var(--color-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preset-check-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 16px;
  height: 16px;
  border-radius: var(--radius-circle);
  background: var(--el-color-primary);
  color: var(--theme-on-primary, #ffffff);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}

.theme-custom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 4px 0;
  border-top: 1px solid var(--color-border-subtle);
}

.theme-custom-label {
  font-size: 12.5px;
  color: var(--color-text-secondary);
}
</style>
