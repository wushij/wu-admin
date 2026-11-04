import type { App } from 'vue'
import type { ApiResult } from '@/types/api'
import { logger } from '@/core/logger'

let lastGlobalErrorToastAt = 0

/** 从 luch-request 4 的 raw / errMsg 中提取原始平台错误信息 */
function extractRawErrMsg(raw: unknown): string | null {
  if (!raw) return null
  if (typeof raw === 'string') return raw.trim() || null
  if (typeof raw === 'object') {
    const record = raw as { errMsg?: unknown; message?: unknown }
    if (typeof record.errMsg === 'string' && record.errMsg.trim()) return record.errMsg.trim()
    if (typeof record.message === 'string' && record.message.trim()) return record.message.trim()
  }
  return null
}

/** 将 uni.request 的原始失败信息映射为中文；非网络失败返回 null */
function normalizeRequestFailMessage(raw: string): string | null {
  const text = raw.trim()
  if (!/^request:fail/i.test(text)) return null
  if (/abort|cancel|interrupted/i.test(text)) return '请求已取消'
  if (/timeout/i.test(text)) return '请求超时，请稍后重试'
  return '网络连接失败，请稍后重试'
}

export function extractApiErrorMessage(source: unknown, fallback = '操作失败'): string {
  if (!source) return fallback
  if (typeof source === 'string') {
    return normalizeRequestFailMessage(source) ?? (source.trim() || fallback)
  }

  const err = source as {
    data?: ApiResult
    statusCode?: number
    errMsg?: string
    message?: string
    msg?: string
    code?: string
    raw?: unknown
    response?: { data?: ApiResult; statusCode?: number }
  }

  // luch-request 4：失败响应体挂在 error.response.data
  const responseBody = err.response?.data
  const responseMessage = responseBody?.message || responseBody?.msg
  if (responseMessage) return responseMessage

  const body = err.data
  const bodyMessage = body?.message || body?.msg
  if (bodyMessage) return bodyMessage

  // 优先用平台原始 errMsg 做中文映射，避免把 "request:fail timeout" 直接抛给用户
  const rawErrMsg = extractRawErrMsg(err.raw) || err.errMsg
  if (rawErrMsg) {
    const normalized = normalizeRequestFailMessage(rawErrMsg)
    if (normalized) return normalized
    if (err.code !== 'ERR_CANCELED') return rawErrMsg
  }

  // luch-request 4：网络类错误统一中文兜底
  if (err.code === 'ERR_NETWORK') return '网络连接失败，请稍后重试'
  if (err.code === 'ERR_CANCELED') return '请求已取消'

  // HTTP 状态类失败优先按状态码映射，避免暴露 "Request failed with status code 502"
  const statusCode = err.response?.statusCode ?? err.statusCode
  if (err.code === 'ERR_BAD_STATUS' && statusCode !== undefined) {
    if (statusCode === 401) return '登录已过期，请重新登录'
    if (statusCode === 403) return '权限不足'
    if (statusCode === 500) return '服务器内部错误，请稍后重试'
    if (statusCode === 502 || statusCode === 503) return '服务暂时不可用，请稍后重试'
    if (statusCode === 504) return '请求超时，请稍后重试'
    return '请求失败，请稍后重试'
  }

  // 业务提示或运行时错误保留原始 message
  if (err.message) return err.message
  if (err.msg) return err.msg

  if (statusCode === 500) return '服务器内部错误，请稍后重试'
  if (statusCode === 502 || statusCode === 503) return '服务暂时不可用，请稍后重试'
  if (statusCode === 504) return '请求超时，请稍后重试'

  return fallback
}

/** H5/小程序切后台时，进行中的请求常被 abort，不应弹全局错误 */
export function isBenignRequestError(source: unknown): boolean {
  if (!source) return false

  const err = source as {
    code?: string
    errMsg?: string
    message?: string
    raw?: unknown
  }

  // luch-request 4：取消统一为 ERR_CANCELED，message 为 "Request canceled"，不再含 request:fail
  if (err.code === 'ERR_CANCELED') return true

  const candidates = [err.errMsg, err.message, extractRawErrMsg(err.raw)]
  const raw = candidates
    .filter((value): value is string => typeof value === 'string' && value.trim() !== '')
    .join(' ')
    .toLowerCase()
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
