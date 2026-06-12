export interface AppToastOptions {
  title: string
  duration?: number
}

/** 轻量成功提示：纯文字 + 顶部位置，避免 H5 默认大黑块遮挡页面 */
export function showSuccessToast(title: string, duration = 2000) {
  showAppToast({ title, duration })
}

export function showAppToast(options: AppToastOptions | string) {
  const opts = typeof options === 'string' ? { title: options } : options
  uni.showToast({
    title: opts.title,
    icon: 'none',
    duration: opts.duration ?? 2000,
    position: 'top',
  })
}
