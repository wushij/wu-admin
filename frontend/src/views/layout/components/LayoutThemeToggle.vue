<template>
  <el-tooltip :content="tooltipText" placement="bottom" :show-after="1000">
    <button
      id="tour-dark-btn"
      type="button"
      class="dock-segment-btn"
      :class="{ 'is-active': isActive, 'is-sun': !isDark }"
      :aria-pressed="isDark"
      :aria-label="isDark ? '切换为亮色模式' : '切换为暗黑模式'"
      @click="onClick"
    >
      <Transition name="toggle-glyph" mode="out-in">
        <svg
          v-if="isDark"
          key="moon"
          class="theme-glyph"
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
          <path d="M20.354 15.354A9 9 0 0 1 8.646 3.646 9.003 9.003 0 0 0 12 21a9.003 9.003 0 0 0 8.354-5.646z" />
        </svg>
        <svg
          v-else
          key="sun"
          class="theme-glyph"
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
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32 1.41 1.41M2 12h2m16 0h2M4.93 19.07l1.41-1.41m11.32-11.32 1.41-1.41" />
        </svg>
      </Transition>
    </button>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { themePresetList, type ThemeTransitionOrigin } from '@/utils/theme'

const props = defineProps<{
  isDark: boolean
  isActive?: boolean
  activePresetId?: string
}>()

const emit = defineEmits<{
  toggle: [origin: ThemeTransitionOrigin]
}>()

const tooltipText = computed(() => {
  const presetName = themePresetList.find((item) => item.id === props.activePresetId)?.label
  const themeSuffix = presetName ? ` · ${presetName}` : ''
  return props.isDark
    ? `暗夜模式${themeSuffix}（点击切换浅色）`
    : `浅色模式${themeSuffix}（点击切换暗夜）`
})

function onClick(e: MouseEvent) {
  emit('toggle', { clientX: e.clientX, clientY: e.clientY })
}
</script>

<style scoped lang="scss">
.dock-segment-btn.is-active.is-sun {
  color: #d97706;
}

.theme-glyph {
  display: block;
  flex-shrink: 0;
}

.toggle-glyph-enter-active,
.toggle-glyph-leave-active {
  transition:
    opacity var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out);
}

.toggle-glyph-enter-from {
  opacity: 0;
  transform: rotate(-28deg) scale(0.72);
}

.toggle-glyph-leave-to {
  opacity: 0;
  transform: rotate(28deg) scale(0.72);
}

@media (prefers-reduced-motion: reduce) {
  .toggle-glyph-enter-active,
  .toggle-glyph-leave-active {
    transition: none !important;
  }
}
</style>
