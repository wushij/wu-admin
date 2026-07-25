import { get, del, put } from '@/utils/request'
import { uploadFile as uniUpload } from '@/utils/upload'
import { getToken } from '@/utils/auth'
import type { PageResult } from '@/types/api'
import type { FilePageQuery } from '@/types/system'
import { resolveApiBaseUrl } from '@/utils/api-base'

export interface FileRecord {
  id?: number
  name?: string
  originalName?: string
  url?: string
  fileType?: string
  size?: number
  fileSize?: number
  fileSuffix?: string
  createTime?: string
}

/** 移动端 img/video 须通过 query 携带 Token */
export function withTokenQuery(url: string): string {
  if (!url?.trim()) return ''
  const token = getToken()
  if (!token) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}Authorization=${encodeURIComponent(token)}`
}

const isH5 = import.meta.env.UNI_PLATFORM === 'h5'

function resolveApiOrigin(): string {
  const base = resolveApiBaseUrl()
  if (base.startsWith('http://') || base.startsWith('https://')) {
    return base.replace(/\/api\/?$/, '')
  }
  // #ifdef H5
  if (typeof window !== 'undefined') {
    return window.location.origin
  }
  // #endif
  return ''
}

/** 小程序/App 的 image/video 须完整 URL；H5 同域可继续用 /api 相对路径 */
function toAbsoluteApiUrl(path: string): string {
  const trimmed = path.trim()
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) return trimmed

  let apiPath = trimmed.startsWith('/') ? trimmed : `/${trimmed}`
  if (apiPath.startsWith('/files/')) apiPath = `/api${apiPath}`

  const base = resolveApiBaseUrl()
  if (isH5 && !base.startsWith('http')) {
    return apiPath
  }

  const origin = resolveApiOrigin()
  if (!origin) return apiPath
  return `${origin}${apiPath}`
}

function normalizeFileApiUrl(url: string): string {
  if (!url) return ''
  const trimmed = url.trim()
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) return trimmed
  // 后端存的路径通常已是 /api/files/...，不可再拼 VITE_API_BASE_URL，否则会 /api/api/...
  if (trimmed.startsWith('/api/') || trimmed === '/api') return toAbsoluteApiUrl(trimmed)
  if (trimmed.startsWith('api/')) return toAbsoluteApiUrl(`/${trimmed}`)
  if (trimmed.startsWith('files/')) return toAbsoluteApiUrl(`/api/${trimmed}`)
  if (trimmed.startsWith('/files/')) return toAbsoluteApiUrl(`/api${trimmed}`)
  const base = resolveApiBaseUrl()
  const merged = trimmed.startsWith('/') ? `${base}${trimmed}` : `${base}/${trimmed}`
  if (merged.startsWith('http://') || merged.startsWith('https://')) {
    return merged.replace(/\/api\/api\//, '/api/')
  }
  return toAbsoluteApiUrl(merged)
}

export function fileDisplayUrl(url?: string): string {
  if (!url?.trim()) return ''
  return withTokenQuery(normalizeFileApiUrl(url.trim()))
}

export function getPreviewApiUrl(id: number) {
  return withTokenQuery(toAbsoluteApiUrl(`/api/system/file/preview/${id}`))
}

export function getDownloadApiUrl(id: number) {
  return withTokenQuery(toAbsoluteApiUrl(`/api/system/file/download/${id}`))
}

/** 流式预览 URL（对齐 PC getStreamPreviewUrl） */
export function getStreamPreviewUrl(file: FileRecord): string {
  const type = file.fileType || ''
  if (file.url) {
    const direct = fileDisplayUrl(file.url)
    if (type.startsWith('image/') || type.startsWith('video/') || type.startsWith('audio/')) {
      return direct
    }
  }
  if (file.id) return getPreviewApiUrl(file.id)
  return file.url ? fileDisplayUrl(file.url) : ''
}

export function getFileText(id: number) {
  return get<string>(`/system/file/text/${id}`)
}

export function pageFileByGroup(params: FilePageQuery) {
  return get<PageResult<FileRecord>>('/system/file/page-by-group', params)
}

export function uploadSysFile(filePath: string, groupId?: number) {
  return uniUpload<FileRecord>({
    url: '/system/file/upload',
    filePath,
    formData: groupId ? { groupId: String(groupId) } : undefined,
  })
}

export function deleteFile(id: number) {
  return del(`/system/file/${id}`)
}

export function getRecycleFilePage(params: { pageNo?: number; pageSize?: number; originalName?: string }) {
  return get<PageResult<FileRecord>>('/system/file/recycle/page', params)
}

export function restoreFile(id: number) {
  return put('/system/file/restore', null, { params: { id } })
}

export function deleteFilePermanent(id: number) {
  return del('/system/file/delete-permanent', { params: { id } })
}
