<template>
  <Teleport to="body">
    <Transition name="tour-fade">
      <div
        v-if="active && currentStep"
        class="ui-tour-layer"
        role="dialog"
        aria-modal="true"
        aria-label="功能漫游引导"
      >
        <!-- 四边遮罩 + Spotlight 镂空高亮框 -->
        <div class="ui-tour-spotlight" :style="spotlightStyle" />

        <!-- 引导提示气泡 -->
        <div ref="popoverRef" class="ui-tour-popover" :style="popoverStyle">
          <div class="ui-tour-head">
            <span class="ui-tour-step-pill">
              步骤 {{ currentIndex + 1 }} / {{ totalSteps }}
            </span>
            <button type="button" class="ui-tour-skip" @click="skipTour">
              跳过引导
            </button>
          </div>
          <h4 class="ui-tour-title">{{ currentStep.title }}</h4>
          <p class="ui-tour-desc">{{ currentStep.description }}</p>
          <div class="ui-tour-footer">
            <el-button v-if="!isFirst" size="small" @click="prevStep">上一步</el-button>
            <el-button type="primary" size="small" @click="nextStep">
              {{ isLast ? '完成引导' : '下一步' }}
            </el-button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onUnmounted } from 'vue'
import { useTour } from '@/composables/ui/useTour'

const {
  active,
  currentIndex,
  currentStep,
  totalSteps,
  isFirst,
  isLast,
  nextStep,
  prevStep,
  skipTour,
} = useTour()

const popoverRef = ref<HTMLElement | null>(null)
const targetRect = ref<DOMRect | null>(null)
const targetRadius = ref('16px')
const targetEdgeFlush = ref(false)
const popoverBox = ref({ width: 320, height: 196 })

const spotlightPadding = computed(() => {
  if (targetEdgeFlush.value) return 0
  const r = targetRect.value
  if (r && Math.min(r.width, r.height) <= 44) return 4
  return 6
})

const spotlightStyle = computed(() => {
  const r = targetRect.value
  if (!r) {
    return {
      top: '50%',
      left: '50%',
      width: '0px',
      height: '0px',
      borderRadius: '16px',
    }
  }
  const pad = spotlightPadding.value
  const vw = typeof window !== 'undefined' ? window.innerWidth : 1920
  const vh = typeof window !== 'undefined' ? window.innerHeight : 1080
  const top = Math.max(0, r.top - pad)
  const left = Math.max(0, r.left - pad)
  const maxW = Math.max(0, vw - left)
  const maxH = Math.max(0, vh - top)

  return {
    top: `${top}px`,
    left: `${left}px`,
    width: `${Math.min(r.width + pad * 2, maxW)}px`,
    height: `${Math.min(r.height + pad * 2, maxH)}px`,
    borderRadius: targetRadius.value,
  }
})

const popoverStyle = computed(() => {
  const r = targetRect.value
  const margin = 16
  const gap = 14
  if (!r || typeof window === 'undefined') {
    return { top: '50%', left: '50%', transform: 'translate(-50%, -50%)' }
  }
  const vw = window.innerWidth
  const vh = window.innerHeight
  const popWidth = Math.min(popoverBox.value.width || 320, vw - margin * 2)
  const popHeight = Math.min(popoverBox.value.height || 196, vh - margin * 2)
  const pad = spotlightPadding.value
  const preferred = currentStep.value?.placement ?? 'bottom'

  let top = r.bottom + pad + gap
  let left = r.left + r.width / 2 - popWidth / 2

  if (preferred === 'right' && r.right + pad + gap + popWidth <= vw - margin) {
    left = r.right + pad + gap
    top = Math.max(margin, targetEdgeFlush.value ? Math.min(r.top + 84, vh - popHeight - margin) : r.top)
  } else if (preferred === 'left' && r.left - pad - gap - popWidth >= margin) {
    left = r.left - pad - gap - popWidth
    top = r.bottom + 40 > vh ? r.bottom - popHeight : r.top
  } else if (preferred === 'top' || (top + popHeight > vh - margin && r.top - pad - gap - popHeight >= margin)) {
    top = r.top - pad - gap - popHeight
  }

  left = Math.max(margin, Math.min(left, vw - popWidth - margin))
  top = Math.max(margin, Math.min(top, vh - popHeight - margin))

  return {
    top: `${top}px`,
    left: `${left}px`,
  }
})

function resolveSpotlightShape(el: HTMLElement, rect: DOMRect) {
  const vh = window.innerHeight
  const isSidebarFlush = rect.left <= 2 && rect.height >= vh - 8
  targetEdgeFlush.value = isSidebarFlush
  if (isSidebarFlush) {
    targetRadius.value = '0 14px 14px 0'
    return
  }

  const computedStyle = window.getComputedStyle(el)
  const rawRadius = parseFloat(computedStyle.borderTopLeftRadius || '0') || 0
  if (rawRadius >= Math.min(rect.width, rect.height) / 2 - 1) {
    targetRadius.value = '9999px'
  } else if (rawRadius > 0) {
    const pad = Math.min(rect.width, rect.height) <= 44 ? 4 : 6
    targetRadius.value = `${Math.round(rawRadius + pad)}px`
  } else {
    targetRadius.value = '14px'
  }
}

async function measureTarget(scrollInto = true) {
  await nextTick()
  const step = currentStep.value
  if (!step) return
  const el = document.querySelector<HTMLElement>(step.selector)
  if (!el) {
    targetRect.value = null
    return
  }
  if (scrollInto) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })
  }
  requestAnimationFrame(() => {
    const rect = el.getBoundingClientRect()
    targetRect.value = rect
    resolveSpotlightShape(el, rect)
    nextTick(() => {
      if (popoverRef.value) {
        popoverBox.value = {
          width: popoverRef.value.offsetWidth || 320,
          height: popoverRef.value.offsetHeight || 196,
        }
      }
    })
  })
}

function onViewportChange() {
  if (active.value) measureTarget(false)
}

function onKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape' && active.value) {
    e.preventDefault()
    skipTour()
  }
}

let listenersBound = false

function bindListeners() {
  if (listenersBound) return
  window.addEventListener('resize', onViewportChange)
  window.addEventListener('scroll', onViewportChange, true)
  window.addEventListener('keydown', onKeyDown)
  listenersBound = true
}

function unbindListeners() {
  if (!listenersBound) return
  window.removeEventListener('resize', onViewportChange)
  window.removeEventListener('scroll', onViewportChange, true)
  window.removeEventListener('keydown', onKeyDown)
  listenersBound = false
}

watch(
  [active, currentIndex],
  ([isActive]) => {
    if (isActive) {
      bindListeners()
      measureTarget(true)
    } else {
      unbindListeners()
    }
  },
  { immediate: true },
)

onUnmounted(unbindListeners)
</script>

<style scoped lang="scss">
.ui-tour-layer {
  position: fixed;
  inset: 0;
  z-index: var(--z-tour, 2600);
  pointer-events: auto;
}

.ui-tour-spotlight {
  box-sizing: border-box;
  position: fixed;
  border-radius: var(--radius-lg);
  box-shadow:
    0 0 0 9999px var(--color-backdrop),
    0 0 0 1.5px var(--el-color-primary),
    0 0 0 5px color-mix(in srgb, var(--el-color-primary) 16%, transparent),
    0 0 18px color-mix(in srgb, var(--el-color-primary) 24%, transparent);
  pointer-events: none;
  transition:
    top var(--duration-normal) var(--ease-out),
    left var(--duration-normal) var(--ease-out),
    width var(--duration-normal) var(--ease-out),
    height var(--duration-normal) var(--ease-out),
    border-radius var(--duration-normal) var(--ease-out);
}

:global(html.dark) .ui-tour-spotlight {
  box-shadow:
    0 0 0 9999px var(--color-backdrop),
    0 0 0 1.5px color-mix(in srgb, var(--el-color-primary) 58%, #e0e7ff),
    0 0 0 5px color-mix(in srgb, var(--el-color-primary) 20%, transparent),
    0 0 22px color-mix(in srgb, var(--el-color-primary) 32%, transparent);
}

.ui-tour-popover {
  box-sizing: border-box;
  position: fixed;
  width: min(320px, calc(100vw - 32px));
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-float);
  padding: var(--space-4) var(--space-5);
  color: var(--color-text-primary);
  transition:
    top var(--duration-normal) var(--ease-out),
    left var(--duration-normal) var(--ease-out);
}

:global(html.dark) .ui-tour-popover {
  border-color: rgba(148, 163, 184, 0.22);
  box-shadow:
    var(--shadow-float),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.ui-tour-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-2);
}

.ui-tour-step-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  font-size: var(--text-caption);
  font-weight: 600;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-variant-numeric: tabular-nums;
}

:global(html.dark) .ui-tour-step-pill {
  background: color-mix(in srgb, var(--el-color-primary) 18%, transparent);
  color: color-mix(in srgb, var(--el-color-primary) 62%, #eef2ff);
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 34%, transparent);
}

.ui-tour-skip {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: var(--text-caption);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-pill);

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-text-primary);
  }
}

.ui-tour-title {
  margin: 0 0 var(--space-1);
  font-size: var(--text-h3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.ui-tour-desc {
  margin: 0 0 var(--space-4);
  font-size: 13px;
  line-height: 1.55;
  color: var(--color-text-secondary);
}

.ui-tour-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

.tour-fade-enter-active,
.tour-fade-leave-active {
  transition: opacity var(--duration-fast) var(--ease-out);
}

.tour-fade-enter-from,
.tour-fade-leave-to {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .ui-tour-spotlight,
  .ui-tour-popover,
  .tour-fade-enter-active,
  .tour-fade-leave-active {
    transition: none;
  }
}
</style>
