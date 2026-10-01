<template>
  <el-popover
    trigger="click"
    placement="bottom-end"
    :width="320"
    :show-arrow="false"
    popper-class="theme-picker-popper"
  >
    <template #reference>
      <div class="header-icon-btn" title="主题风格">
        <svg
          class="palette-svg"
          viewBox="0 0 24 24"
          width="18"
          height="18"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.563-2.512 5.563-5.563C22 6.5 17.5 2 12 2z" />
          <circle cx="7.5" cy="11.5" r="1" fill="currentColor" />
          <circle cx="12" cy="7.5" r="1" fill="currentColor" />
          <circle cx="16.5" cy="11.5" r="1" fill="currentColor" />
        </svg>
      </div>
    </template>
    <div class="theme-picker-content">
      <div class="theme-picker-header">
        <span class="theme-picker-title">主题风格</span>
        <span class="theme-picker-hint">选择品牌主色</span>
      </div>
      <div class="preset-colors">
        <button
          v-for="item in themePresetList"
          :key="item.id"
          type="button"
          class="preset-color-btn"
          :class="{ active: activePresetId === item.id }"
          @click="$emit('presetSelect', item.id)"
        >
          <span class="preset-swatch" :style="{ background: item.primary }">
            <el-icon v-if="activePresetId === item.id" class="preset-check">
              <component :is="ElementPlusIconsVue.Check" />
            </el-icon>
          </span>
          <span class="preset-label">{{ item.label }}</span>
        </button>
      </div>
      <div class="theme-custom-row">
        <span class="theme-custom-label">自定义</span>
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
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { themePresetList } from '@/utils/theme'

defineProps<{
  currentColor: string
  activePresetId: string | undefined
}>()

defineEmits<{
  presetSelect: [presetId: string]
  colorChange: [color: string | null]
}>()
</script>

<style scoped>
.header-icon-btn {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 1px solid var(--theme-border, #e2e8f0);
  background: #ffffff;
  color: var(--theme-text-base, #1F2937);
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.header-icon-btn:hover {
  border-color: var(--theme-primary, #3b82f6);
  color: var(--theme-primary, #3b82f6);
  background: var(--theme-primary-muted, rgba(59, 130, 246, 0.06));
  transform: translateY(-1px);
}

.palette-svg {
  display: block;
  flex-shrink: 0;
}

.theme-picker-content { padding: 4px 2px 8px; }

.theme-picker-header {
  padding: 0 4px 14px;
  border-bottom: 1px solid var(--theme-border, #e2e8f0);
  margin-bottom: 14px;
}

.theme-picker-title {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: var(--theme-text-base, #1e293b);
}

.theme-picker-hint {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--theme-text-secondary, #64748b);
}

.preset-colors {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px 8px;
  margin-bottom: 14px;
}

.preset-color-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 10px;
}

.preset-color-btn.active .preset-swatch {
  box-shadow: 0 0 0 2px #fff, 0 0 0 4px var(--theme-primary, #010710);
}

.preset-swatch {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
}

.preset-check {
  font-size: 18px;
  color: #fff;
}

.preset-label {
  font-size: 11px;
  color: var(--theme-text-secondary, #64748b);
}

.theme-custom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 4px 0;
  border-top: 1px solid var(--theme-border, #e2e8f0);
}

.theme-custom-label {
  font-size: 13px;
  color: var(--theme-text-secondary, #64748b);
}
</style>
