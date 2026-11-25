import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getErrorMessage, isErrorToastShown } from '@/core/http/axiosError'

export interface AlertDialogOptions {
  /** 警告标题（例如：彻底清除用户数据） */
  title: string
  /** 操作对象摘要，醒目展示 */
  targetName?: string
  /** 具体后果与是否不可恢复说明 */
  message: string
  /** 风险级别：irreversible 不可逆极高危 / danger 危险 / warning 提醒 */
  severity?: 'irreversible' | 'danger' | 'warning'
  /** 确认按钮明确后果动词文案（禁止仅写「确定」，如「永久删除」「彻底清除」「强制下线」） */
  confirmText: string
  /** 取消按钮文案 */
  cancelText?: string
  /** 极高危二次输入校验：要求用户输入指定文本后才允许点击确认 */
  requireConfirmInput?: string
  /** 输入提示 */
  inputPlaceholder?: string
  /** 是否允许点击遮罩关闭（不可逆操作强制 false） */
  closeOnClickModal?: boolean
  /** 是否允许 Esc 关闭（不可逆操作默认 false 防误触） */
  closeOnEscape?: boolean
  /** 异步确认回调，返回 Promise 时确认按钮自动进入 loading 防连点 */
  onConfirm?: () => Promise<void> | void
}

interface AlertDialogState {
  visible: boolean
  loading: boolean
  inputValue: string
  errorText: string
  options: AlertDialogOptions | null
  resolve: ((confirmed: boolean) => void) | null
}

export const alertDialogState = reactive<AlertDialogState>({
  visible: false,
  loading: false,
  inputValue: '',
  errorText: '',
  options: null,
  resolve: null,
})

/**
 * 不可逆 / 高风险操作阻断式 Alert Dialog
 * 若传入 onConfirm，则在确认点击时自动加 loading 并在完成后关闭；
 * 返回 Promise<boolean>：用户确认并执行成功返回 true，取消返回 false。
 */
export function openAlertDialog(options: AlertDialogOptions): Promise<boolean> {
  // 若上一个未关闭，先安全结束
  if (alertDialogState.resolve) {
    alertDialogState.resolve(false)
    alertDialogState.resolve = null
  }
  const severity = options.severity ?? 'danger'
  alertDialogState.options = {
    ...options,
    severity,
    cancelText: options.cancelText ?? '取消',
    closeOnClickModal: severity === 'irreversible' ? false : (options.closeOnClickModal ?? false),
    closeOnEscape: severity === 'irreversible' ? false : (options.closeOnEscape ?? true),
  }
  alertDialogState.inputValue = ''
  alertDialogState.errorText = ''
  alertDialogState.loading = false
  alertDialogState.visible = true

  return new Promise<boolean>((resolve) => {
    alertDialogState.resolve = resolve
  })
}

export function cancelAlertDialog(): void {
  if (alertDialogState.loading) return
  alertDialogState.visible = false
  const resolver = alertDialogState.resolve
  alertDialogState.resolve = null
  alertDialogState.options = null
  resolver?.(false)
}

export async function confirmAlertDialog(): Promise<void> {
  const opts = alertDialogState.options
  if (!opts || alertDialogState.loading) return

  if (opts.requireConfirmInput && alertDialogState.inputValue.trim() !== opts.requireConfirmInput) {
    alertDialogState.errorText = `请输入「${opts.requireConfirmInput}」以确认此操作`
    return
  }

  alertDialogState.errorText = ''

  if (opts.onConfirm) {
    alertDialogState.loading = true
    try {
      await opts.onConfirm()
      alertDialogState.visible = false
      const resolver = alertDialogState.resolve
      alertDialogState.resolve = null
      alertDialogState.options = null
      resolver?.(true)
    } catch (err) {
      // 接口错误已由 HTTP 拦截器统一在顶层 Toast 提示，仅对未弹窗的本地异常补充一次顶层提示，避免弹窗内重复展示
      if (!isErrorToastShown(err)) {
        ElMessage.error(getErrorMessage(err) || '操作失败，请稍后重试')
      }
    } finally {
      alertDialogState.loading = false
    }
    return
  }

  alertDialogState.visible = false
  const resolver = alertDialogState.resolve
  alertDialogState.resolve = null
  alertDialogState.options = null
  resolver?.(true)
}
