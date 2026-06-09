import type { AxiosError } from 'axios'

interface ErrorBody {
  msg?: string
  message?: string
}

/** 标记拦截器已展示 Toast，避免页面 catch 重复弹窗 */
export const ERROR_TOAST_SHOWN = '__toastShown'

/** 从 axios / 普通 Error 中提取可读错误信息 */
export function getErrorMessage(error: unknown): string | undefined {
  if (!error || typeof error !== 'object') return undefined

  if ('response' in error) {
    const ax = error as AxiosError<ErrorBody>
    return ax.response?.data?.msg || ax.response?.data?.message
  }

  if ('message' in error && typeof (error as Error).message === 'string') {
    return (error as Error).message
  }

  return undefined
}

/** 拦截器展示 Toast 后标记，供页面 catch 判断是否重复提示 */
export function markErrorToastShown(error: unknown): void {
  if (error && typeof error === 'object') {
    ;(error as Record<string, unknown>)[ERROR_TOAST_SHOWN] = true
  }
}

export function isErrorToastShown(error: unknown): boolean {
  return !!(error && typeof error === 'object'
    && (error as Record<string, unknown>)[ERROR_TOAST_SHOWN])
}
