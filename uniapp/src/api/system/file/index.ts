import { get, del, put } from '@/utils/request'
import { uploadFile as uniUpload } from '@/utils/upload'
import { getToken } from '@/utils/auth'
import type { PageResult } from '@/types/api'
import type { FilePageQuery } from '@/types/system'

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

function normalizeFileApiUrl(url: string): string {
  if (!url) return ''
  const trimmed = url.trim()
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) return trimmed
  if (trimmed.startsWith('/api')) return trimmed
  if (trimmed.startsWith('/files/')) return `/api${trimmed}`
  if (trimmed.startsWith('files/')) return `/api/${trimmed}`
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  return trimmed.startsWith('/') ? `${base}${trimmed}` : `${base}/${trimmed}`
}

export function fileDisplayUrl(url?: string): string {
  if (!url?.trim()) return ''
  return withTokenQuery(normalizeFileApiUrl(url.trim()))
}

export function getPreviewApiUrl(id: number) {
  return withTokenQuery(`/api/system/file/preview/${id}`)
}

export function getDownloadApiUrl(id: number) {
  return withTokenQuery(`/api/system/file/download/${id}`)
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
