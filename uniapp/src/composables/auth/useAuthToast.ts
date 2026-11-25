import { readonly, ref } from 'vue'

export type AuthToastType = 'success' | 'warning' | 'error' | 'info'

export interface AuthToastState {
  visible: boolean
  type: AuthToastType
  message: string
  seq: number
}

const state = ref<AuthToastState>({
  visible: false,
  type: 'info',
  message: '',
  seq: 0,
})

let hideTimer: ReturnType<typeof setTimeout> | null = null
let activeHostCount = 0

/** 展示深空玻璃胶囊轻提示（复刻 PC 端 .el-message.auth-glass-message） */
export function showAuthToast(
  message: string,
  type: AuthToastType = 'info',
  duration = 2200,
) {
  if (!message) return
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
  state.value = {
    visible: true,
    type,
    message,
    seq: state.value.seq + 1,
  }
  if (duration > 0) {
    hideTimer = setTimeout(() => {
      state.value = { ...state.value, visible: false }
      hideTimer = null
    }, duration)
  }
}

export function hideAuthToast() {
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
  if (state.value.visible) {
    state.value = { ...state.value, visible: false }
  }
}

export function registerAuthToastHost(): () => void {
  activeHostCount += 1
  return () => {
    activeHostCount = Math.max(0, activeHostCount - 1)
    if (activeHostCount === 0) {
      hideAuthToast()
    }
  }
}

export function hasActiveAuthToastHost(): boolean {
  return activeHostCount > 0
}

export function useAuthToastState() {
  return readonly(state)
}
