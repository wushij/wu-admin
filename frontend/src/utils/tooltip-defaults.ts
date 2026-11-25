import {
  defineComponent,
  h,
  onUnmounted,
  ref,
  type App,
} from 'vue'
import { ElTooltip, type TooltipInstance } from 'element-plus'

const DEFAULT_TOOLTIP_SHOW_AFTER_MS = 1000

const activeHoverDismissers = new Set<() => void>()
let listenersBound = false

function dismissAllHoverTooltips() {
  activeHoverDismissers.forEach((dismiss) => {
    try {
      dismiss()
    } catch {
      // 忽略已卸载实例异常
    }
  })
}

function bindGlobalDismissListeners() {
  if (listenersBound || typeof window === 'undefined' || typeof document === 'undefined') return
  listenersBound = true

  // 鼠标按下（含点击按钮打开文件框/弹窗前）立即取消未触发的悬浮定时器并收起提示
  document.addEventListener(
    'pointerdown',
    (event) => {
      const target = event.target as HTMLElement | null
      if (target?.closest?.('.el-popper')) return
      dismissAllHoverTooltips()
    },
    true,
  )

  // 弹出操作系统原生文件选择窗或切换窗口失焦时，防止鼠标离开事件丢失导致提示残留
  window.addEventListener('blur', dismissAllHoverTooltips)
}

function isHoverTrigger(attrs: Record<string, unknown>): boolean {
  if (attrs.visible !== null && attrs.visible !== undefined) return false
  const trigger = attrs.trigger ?? 'hover'
  if (Array.isArray(trigger)) return trigger.includes('hover')
  return trigger === 'hover'
}

/**
 * 业务侧 <el-tooltip> 包装组件：
 * - 仅替换全局模板组件注册，绝不修改 ElTooltip 原型或 props 定义，
 *   从而与内部复用 ElTooltip 的 ElPopover / ElDropdown / ElSelect / ElDatePicker 等内置组件完全隔离。
 */
const SafeElTooltip = defineComponent({
  name: 'ElTooltip',
  inheritAttrs: false,
  setup(_props, { attrs, slots, expose }) {
    const innerRef = ref<TooltipInstance | null>(null)

    const dismiss = () => {
      innerRef.value?.onClose?.(undefined, 0)
      innerRef.value?.hide?.()
    }

    if (isHoverTrigger(attrs)) {
      activeHoverDismissers.add(dismiss)
    }

    onUnmounted(() => {
      activeHoverDismissers.delete(dismiss)
    })

    expose({
      get popperRef() {
        return innerRef.value?.popperRef
      },
      get contentRef() {
        return innerRef.value?.contentRef
      },
      isFocusInsideContent: (event?: FocusEvent) => innerRef.value?.isFocusInsideContent?.(event),
      updatePopper: () => innerRef.value?.updatePopper?.(),
      onOpen: (event?: Event, delay?: number) => innerRef.value?.onOpen?.(event, delay),
      onClose: (event?: Event, delay?: number) => innerRef.value?.onClose?.(event, delay),
      hide: (event?: Event) => innerRef.value?.hide?.(event),
    })

    return () => {
      const hasShowAfter = Object.prototype.hasOwnProperty.call(attrs, 'showAfter')
        || Object.prototype.hasOwnProperty.call(attrs, 'show-after')
      const mergedProps = hasShowAfter
        ? { ...attrs, ref: innerRef }
        : { ...attrs, showAfter: DEFAULT_TOOLTIP_SHOW_AFTER_MS, ref: innerRef }
      return h(ElTooltip, mergedProps, slots)
    }
  },
})

/**
 * 在 app.use(ElementPlus) 之后注册全局 <el-tooltip> 包装组件：
 * 1. 默认悬浮延迟 1000ms，避免鼠标路过或快速操作误触闪烁
 * 2. 点击触发元素或窗口失焦时，立即清除待触发定时器并关闭提示
 * 3. 完全不触碰 Element Plus 原生模块导出，不影响 ElPopover / ElDropdown / ElSelect 等任何内置组件
 */
export function setupGlobalTooltip(app: App) {
  bindGlobalDismissListeners()
  const components = (app as unknown as { _context?: { components?: Record<string, unknown> } })._context?.components
  if (components) {
    components.ElTooltip = SafeElTooltip
  } else {
    app.component('ElTooltip', SafeElTooltip)
  }
}
