import type { AxiosError } from 'axios'

interface ErrorBody {
  msg?: string
  message?: string
}

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
