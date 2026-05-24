import { reactive } from 'vue'

export type ConfirmTone = 'default' | 'danger'

export interface ConfirmOptions {
  title?: string
  content: string
  confirmText?: string
  cancelText?: string
  tone?: ConfirmTone
  editable?: boolean
  placeholderText?: string
  inputType?: 'text' | 'password'
  showCancel?: boolean
  contentAlign?: 'left' | 'center'
}

export interface ConfirmResult {
  confirmed: boolean
  content?: string
}

export interface ActionSheetItem {
  label: string
  danger?: boolean
}

export interface ActionSheetOptions {
  title?: string
  items: ActionSheetItem[]
  /** 选项较多时启用内部滚动（默认超过 8 项自动开启） */
  scrollable?: boolean
}

interface DialogState {
  confirmVisible: boolean
  confirmOptions: ConfirmOptions
  confirmInput: string
  confirmResolve: ((value: ConfirmResult) => void) | null
  actionSheetVisible: boolean
  actionSheetOptions: ActionSheetOptions
  actionSheetResolve: ((index: number) => void) | null
  actionSheetReject: ((reason?: unknown) => void) | null
  historyLocked: boolean
}

export const dialogState = reactive<DialogState>({
  confirmVisible: false,
  confirmOptions: { content: '' },
  confirmInput: '',
  confirmResolve: null,
  actionSheetVisible: false,
  actionSheetOptions: { items: [] },
  actionSheetResolve: null,
  actionSheetReject: null,
  historyLocked: false,
})

let suppressNextPopstate = false
let pendingConfirm: Promise<ConfirmResult> | null = null

export function consumeSuppressedPopstate() {
  if (!suppressNextPopstate) return false
  suppressNextPopstate = false
  return true
}

function lockDialogHistory() {
  // #ifdef H5
  if (typeof window === 'undefined' || dialogState.historyLocked) return
  dialogState.historyLocked = true
  history.pushState({ wuAdminDialog: true }, '', location.href)
  // #endif
}

export function syncDialogHistoryAfterClose() {
  // #ifdef H5
  if (typeof window === 'undefined' || !dialogState.historyLocked) return
  dialogState.historyLocked = false
  suppressNextPopstate = true
  history.back()
  // #endif
}

/** 表单页确认离开前：清掉选项/确认弹层残留的 H5 history 条目 */
export function forceCollapseOverlayHistory() {
  // #ifdef H5
  if (typeof window === 'undefined') return
  dialogState.historyLocked = false
  let guard = 0
  while (history.state?.wuAdminDialog && guard < 4) {
    suppressNextPopstate = true
    history.back()
    guard += 1
  }
  // #endif
}

/** 选中 ActionSheet 项后：只释放锁，不 history.back（H5 下 back 会触发 uni.navigateBack） */
function releaseDialogHistorySoft() {
  // #ifdef H5
  if (typeof window === 'undefined' || !dialogState.historyLocked) return
  dialogState.historyLocked = false
  if (history.state?.wuAdminDialog) {
    history.replaceState(null, '', location.href)
  }
  // #endif
}

function dismissDialogHistory() {
  releaseDialogHistorySoft()
}

function openDialogSurface() {
  lockDialogHistory()
}

export function showConfirm(options: ConfirmOptions): Promise<ConfirmResult> {
  if (dialogState.confirmVisible && pendingConfirm) {
    return pendingConfirm
  }
  pendingConfirm = new Promise((resolve) => {
    dialogState.confirmOptions = {
      title: '提示',
      confirmText: '确定',
      cancelText: '取消',
      tone: 'default',
      showCancel: true,
      contentAlign: 'center',
      ...options,
    }
    dialogState.confirmInput = ''
    dialogState.confirmResolve = resolve
    dialogState.confirmVisible = true
    openDialogSurface()
  })
  return pendingConfirm.finally(() => {
    pendingConfirm = null
  })
}

export function resolveConfirm(confirmed: boolean, content?: string) {
  const input = dialogState.confirmOptions.editable
    ? (content ?? dialogState.confirmInput)
    : undefined
  dialogState.confirmVisible = false
  const resolveFn = dialogState.confirmResolve
  dialogState.confirmResolve = null
  dialogState.confirmInput = ''
  dismissDialogHistory()
  resolveFn?.({
    confirmed,
    content: input,
  })
}

export function showActionSheet(options: ActionSheetOptions): Promise<number> {
  return new Promise((resolve, reject) => {
    if (!options.items.length) {
      reject(new Error('empty'))
      return
    }
    dialogState.actionSheetOptions = options
    dialogState.actionSheetResolve = resolve
    dialogState.actionSheetReject = reject
    dialogState.actionSheetVisible = true
    // 选项层不 pushState，避免 H5 多次返回才退出；关闭由 onBackPress + closeTopAppDialog 处理
  })
}

export function resolveActionSheet(index: number) {
  dialogState.actionSheetVisible = false
  const resolveFn = dialogState.actionSheetResolve
  dialogState.actionSheetResolve = null
  dialogState.actionSheetReject = null
  resolveFn?.(index)
}

export function cancelActionSheet() {
  dialogState.actionSheetVisible = false
  dialogState.actionSheetReject?.(new Error('cancel'))
  dialogState.actionSheetResolve = null
  dialogState.actionSheetReject = null
}
