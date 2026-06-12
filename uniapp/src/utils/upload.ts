import { isApiSuccessCode } from '@/utils/api-response'
import { getToken, removeToken } from '@/utils/auth'
import type { ApiResult } from '@/types/api'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

let lastForbiddenToastAt = 0

function handleUploadAuthError(code: number, message: string) {
  if (code === 401) {
    uni.showToast({ title: '登录已过期', icon: 'none' })
    removeToken()
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (code === 403) {
    const now = Date.now()
    if (now - lastForbiddenToastAt < 2000) return
    lastForbiddenToastAt = now
    uni.showToast({ title: message || '权限不足', icon: 'none' })
  }
}

export function uploadFile<T = unknown>(options: {
  url: string
  filePath: string
  name?: string
  formData?: Record<string, string>
}): Promise<ApiResult<T>> {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: `${BASE_URL}${options.url}`,
      filePath: options.filePath,
      name: options.name || 'file',
      formData: options.formData,
      header: token ? { Authorization: token } : {},
      success: (res) => {
        try {
          const body = JSON.parse(res.data) as ApiResult<T>
          if (isApiSuccessCode(body.code)) {
            resolve(body)
            return
          }
          const msg = body.message || body.msg || '上传失败'
          handleUploadAuthError(body.code, msg)
          reject(new Error(msg))
        } catch {
          reject(new Error('上传失败'))
        }
      },
      fail: (err) => reject(err),
    })
  })
}
