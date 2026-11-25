import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  alertDialogState,
  openAlertDialog,
  cancelAlertDialog,
  confirmAlertDialog,
} from '@/composables/ui/useAlertDialog'
import {
  useSnackbar,
  showSnackbar,
  triggerSnackbarAction,
  dismissSnackbar,
} from '@/composables/ui/useSnackbar'

describe('useAlertDialog 危险确认分级', () => {
  beforeEach(() => {
    alertDialogState.visible = false
    alertDialogState.loading = false
    alertDialogState.inputValue = ''
    alertDialogState.errorText = ''
    alertDialogState.options = null
    alertDialogState.resolve = null
  })

  it('不可逆操作默认禁用遮罩关闭与 Esc 退出', async () => {
    const p = openAlertDialog({
      title: '彻底清除用户',
      targetName: 'test_user',
      message: '清除后不可恢复',
      severity: 'irreversible',
      confirmText: '彻底清除',
    })
    expect(alertDialogState.visible).toBe(true)
    expect(alertDialogState.options?.closeOnClickModal).toBe(false)
    expect(alertDialogState.options?.closeOnEscape).toBe(false)

    cancelAlertDialog()
    await expect(p).resolves.toBe(false)
    expect(alertDialogState.visible).toBe(false)
  })

  it('高危输入校验未通过时阻止确认并提示错误', async () => {
    const onConfirm = vi.fn()
    const p = openAlertDialog({
      title: '清空全部日志',
      message: '不可恢复',
      severity: 'irreversible',
      confirmText: '清空日志',
      requireConfirmInput: '清空日志',
      onConfirm,
    })

    alertDialogState.inputValue = '错误输入'
    await confirmAlertDialog()
    expect(onConfirm).not.toHaveBeenCalled()
    expect(alertDialogState.errorText).toContain('清空日志')

    alertDialogState.inputValue = '清空日志'
    await confirmAlertDialog()
    expect(onConfirm).toHaveBeenCalledTimes(1)
    await expect(p).resolves.toBe(true)
  })

  it('异步确认回调报错时不重复写入弹窗内 errorText，并解除 loading', async () => {
    const p = openAlertDialog({
      title: '彻底清除文件',
      targetName: 'test.png',
      message: '彻底清除后不可恢复',
      severity: 'irreversible',
      confirmText: '彻底清除',
      onConfirm: async () => {
        throw new Error('物理文件删除失败，文件记录已保留，请稍后重试')
      },
    })

    await confirmAlertDialog()
    expect(alertDialogState.visible).toBe(true)
    expect(alertDialogState.loading).toBe(false)
    expect(alertDialogState.errorText).toBe('')

    cancelAlertDialog()
    await expect(p).resolves.toBe(false)
  })
})

describe('useSnackbar 撤销与定时器竞态保护', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    const { items } = useSnackbar()
    items.value.forEach((i) => dismissSnackbar(i.id))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('点击撤销按钮立即执行补偿回调并取消定时器关闭', async () => {
    const undoFn = vi.fn().mockResolvedValue(undefined)
    const { items } = useSnackbar()

    const id = showSnackbar({
      message: '已移至回收站',
      actionLabel: '撤销',
      duration: 3000,
      onAction: undoFn,
    })

    expect(items.value).toHaveLength(1)
    await triggerSnackbarAction(id)
    expect(undoFn).toHaveBeenCalledTimes(1)
    expect(items.value).toHaveLength(0)

    vi.advanceTimersByTime(5000)
    expect(undoFn).toHaveBeenCalledTimes(1)
  })
})
