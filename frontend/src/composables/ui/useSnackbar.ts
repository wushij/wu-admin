import { ref } from 'vue'

export type SnackbarVariant = 'info' | 'success' | 'warning' | 'danger'

export interface SnackbarItem {
  id: number
  message: string
  description?: string
  variant: SnackbarVariant
  actionLabel?: string
  duration: number
  acting?: boolean
  onAction?: () => Promise<void> | void
}

export interface ShowSnackbarOptions {
  message: string
  description?: string
  variant?: SnackbarVariant
  actionLabel?: string
  duration?: number
  onAction?: () => Promise<void> | void
}

const snackbarQueue = ref<SnackbarItem[]>([])
const timerMap = new Map<number, ReturnType<typeof setTimeout>>()
let seed = 1

function clearItemTimer(id: number) {
  const timer = timerMap.get(id)
  if (timer) {
    clearTimeout(timer)
    timerMap.delete(id)
  }
}

function scheduleDismiss(id: number, duration: number) {
  clearItemTimer(id)
  if (duration <= 0) return
  const timer = setTimeout(() => {
    dismissSnackbar(id)
  }, duration)
  timerMap.set(id, timer)
}

export function showSnackbar(options: ShowSnackbarOptions): number {
  const id = seed++
  const duration = options.duration ?? (options.actionLabel ? 6000 : 3800)
  const item: SnackbarItem = {
    id,
    message: options.message,
    description: options.description,
    variant: options.variant ?? 'info',
    actionLabel: options.actionLabel,
    duration,
    acting: false,
    onAction: options.onAction,
  }
  // 控制同屏最多保留 3 条，防止底部堆叠过高
  if (snackbarQueue.value.length >= 3) {
    const oldest = snackbarQueue.value[0]
    if (oldest) dismissSnackbar(oldest.id)
  }
  snackbarQueue.value.push(item)
  scheduleDismiss(id, duration)
  return id
}

export function pauseSnackbarTimer(id: number): void {
  clearItemTimer(id)
}

export function resumeSnackbarTimer(id: number): void {
  const item = snackbarQueue.value.find((entry) => entry.id === id)
  if (!item || item.acting) return
  scheduleDismiss(id, item.duration)
}

export async function triggerSnackbarAction(id: number): Promise<void> {
  const item = snackbarQueue.value.find((entry) => entry.id === id)
  if (!item || item.acting) return
  // 竞态防护：点击撤销/重试时立即清除自动关闭定时器
  clearItemTimer(id)
  if (!item.onAction) {
    dismissSnackbar(id)
    return
  }
  item.acting = true
  try {
    await item.onAction()
  } finally {
    dismissSnackbar(id)
  }
}

export function dismissSnackbar(id: number): void {
  clearItemTimer(id)
  snackbarQueue.value = snackbarQueue.value.filter((entry) => entry.id !== id)
}

export function useSnackbar() {
  return {
    items: snackbarQueue,
    showSnackbar,
    dismissSnackbar,
    triggerSnackbarAction,
    pauseSnackbarTimer,
    resumeSnackbarTimer,
  }
}
