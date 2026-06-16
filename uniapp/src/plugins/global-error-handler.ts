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
  if (err.errMsg) {
    const raw = err.errMsg.trim()
    if (/^request:fail/i.test(raw)) return '网络连接失败，请稍后重试'
    return raw
  }

  if (err.statusCode === 500) return '服务器内部错误，请稍后重试'
  if (err.statusCode === 502 || err.statusCode === 503) return '服务暂时不可用，请稍后重试'
  if (err.statusCode === 504) return '请求超时，请稍后重试'

  return fallback
}

/** H5/小程序切后台时，进行中的请求常被 abort，不应弹全局错误 */
export function isBenignRequestError(source: unknown): boolean {
  const err = source as { errMsg?: string; message?: string }
  const raw = (err?.errMsg || err?.message || '').toLowerCase()
  if (!raw) return false
  return (
    raw.includes('request:fail') &&
    (raw.includes('abort') || raw.includes('cancel') || raw.includes('interrupted'))
  )
}

function isBenignUniRuntimeError(source: unknown): boolean {
  if (isBenignRequestError(source)) return true
  const msg = extractApiErrorMessage(source, '')
  if (/closeSocket:fail/i.test(msg) && /not connected/i.test(msg)) return true
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

  // #ifndef H5
  // 小程序/App 端：注册全局错误与未捕获 Promise 监听，与 H5 端 window 监听对齐。
  // 微信小程序的 onError 回调参数为字符串（错误消息），onUnhandledRejection 为 { reason, promise }。
  if (typeof uni !== 'undefined' && typeof uni.onError === 'function') {
    uni.onError((error) => {
      if (isBenignUniRuntimeError(error)) {
        logger.warn('[app-error:ignored]', error)
        return
      }
      logger.error('[app-error]', error)
      showGlobalErrorToast(extractApiErrorMessage(error, '应用运行异常'))
    })
  }
  if (typeof uni !== 'undefined' && typeof uni.onUnhandledRejection === 'function') {
    uni.onUnhandledRejection((res) => {
      const reason: unknown = res?.reason
      if (isBenignUniRuntimeError(reason)) {
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
