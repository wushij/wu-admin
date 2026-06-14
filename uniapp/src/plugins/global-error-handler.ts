import type { App } from 'vue'
import type { ApiResult } from '@/types/api'
import { logger } from '@/utils/logger'

let lastGlobalErrorToastAt = 0

export function extractApiErrorMessage(source: unknown, fallback = '操作失败'): string {
  if (!source) return fallback
  if (typeof source === 'string' && source.trim()) return source.trim()
  if (source instanceof Error && source.message) return source.message

  const err = source as {
    data?: ApiResult
    statusCode?: number
    errMsg?: string
    message?: string
    msg?: string
  }

  const body = err.data
  const bodyMessage = body?.message || body?.msg
  if (bodyMessage) return bodyMessage

  if (err.message) return err.message
  if (err.msg) return err.msg
  if (err.errMsg) return err.errMsg

  if (err.statusCode === 500) return '服务器内部错误，请稍后重试'
  if (err.statusCode === 502 || err.statusCode === 503) return '服务暂时不可用，请稍后重试'
  if (err.statusCode === 504) return '请求超时，请稍后重试'

  return fallback
}

function isBenignUniRuntimeError(source: unknown): boolean {
  const msg = extractApiErrorMessage(source, '')
  if (msg.includes('scrollTop') && msg.toLowerCase().includes('null')) return true
  if (msg.includes('navigateBack:fail') && msg.includes('onBackPress')) return true
  if (msg.includes('Maximum call stack size exceeded')) return true
  if (msg.includes('Stack overflow')) return true
  return false
}

export function showGlobalErrorToast(message: string) {
  const text = message.trim()
  if (!text) return

  const now = Date.now()
  if (now - lastGlobalErrorToastAt < 1200) return
  lastGlobalErrorToastAt = now

  uni.showToast({ title: text, icon: 'none', duration: 2600 })
}

/** 移动端全局异常：Vue 运行时错误 + 未捕获 Promise + 请求层统一提示 */
export function installGlobalErrorHandler(app: App) {
  app.config.errorHandler = (err, _instance, info) => {
    if (isBenignUniRuntimeError(err)) {
      logger.warn('[vue-error:ignored]', info, err)
      return
    }
    logger.error('[vue-error]', info, err)
    showGlobalErrorToast(extractApiErrorMessage(err, '页面运行异常'))
  }

  // #ifdef H5
  if (typeof window !== 'undefined') {
    window.addEventListener('unhandledrejection', (event) => {
      const reason = event.reason
      if (isBenignUniRuntimeError(reason)) {
        event.preventDefault()
        logger.warn('[unhandledrejection:ignored]', reason)
        return
      }
      if (reason instanceof Error && (reason as Error & { __toastShown?: boolean }).__toastShown) {
        return
      }
      logger.error('[unhandledrejection]', reason)
      showGlobalErrorToast(extractApiErrorMessage(reason, '请求处理失败'))
    })
  }
  // #endif
}

export function markErrorToastShown(error: Error) {
  ;(error as Error & { __toastShown?: boolean }).__toastShown = true
}
