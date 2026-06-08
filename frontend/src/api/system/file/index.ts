import axios from 'axios'
import { get, post, put, del } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

const BLOCKED_EXTENSIONS = ['exe', 'bat', 'cmd', 'sh', 'ps1', 'msi', 'dll', 'com', 'scr']

export interface UploadPolicy {
  maxSizeMb: number
  allowedExtensions: string[]
  platformMaxMb: number
}

export interface FileGroupVO {
  id: number
  name: string
  sort?: number
  fileCount?: number
  createTime?: string
  updateTime?: string
}

export interface FileGroupListResult {
  groups: FileGroupVO[]
  ungroupedCount: number
}

export interface FileRecord {
  id?: number
  name?: string
  originalName?: string
  url?: string
  fileType?: string
  size?: number
  fileSize?: number
  fileSuffix?: string
  groupId?: number
  createTime?: string
}

/** 由接口 /system/file/upload-policy 加载，保存系统配置后立即更新 */
export const uploadPolicy: UploadPolicy = {
  maxSizeMb: 50,
  allowedExtensions: [],
  platformMaxMb: 500,
}

export function getFileUploadPolicy() {
  return get<Partial<UploadPolicy>>('/system/file/upload-policy')
}

export function setUploadPolicyFromApi(data: Partial<UploadPolicy> | null | undefined) {
  if (!data) return
  if (data.maxSizeMb != null) uploadPolicy.maxSizeMb = Number(data.maxSizeMb) || 50
  if (data.platformMaxMb != null) uploadPolicy.platformMaxMb = Number(data.platformMaxMb) || 500
  if (data.allowedExtensions) {
    uploadPolicy.allowedExtensions = String(data.allowedExtensions)
      .split(',')
      .map(s => s.trim().toLowerCase())
      .filter(Boolean)
  }
}

function getAllowedExtensions(): string[] {
  if (uploadPolicy.allowedExtensions?.length) {
    return uploadPolicy.allowedExtensions
  }
  return [
    'jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'pdf', 'doc', 'docx', 'xls', 'xlsx',
    'ppt', 'pptx', 'txt', 'md', 'json', 'xml', 'zip', 'rar', 'mp4', 'mp3', 'wav', 'avi', 'mov',
  ]
}

export function getFileExtension(fileName?: string): string {
  if (!fileName || !fileName.includes('.')) return ''
  return fileName.slice(fileName.lastIndexOf('.') + 1).toLowerCase()
}

/** 上传前校验，返回错误文案；通过返回 null */
export function validateFileBeforeUpload(file?: File | null): string | null {
  const ext = getFileExtension(file?.name)
  if (!ext) return '文件名需包含扩展名'
  if (BLOCKED_EXTENSIONS.includes(ext)) return `不允许上传可执行文件：.${ext}`
  if (!getAllowedExtensions().includes(ext)) return `不允许上传该类型文件：.${ext}`
  const maxBytes = uploadPolicy.maxSizeMb * 1024 * 1024
  if (file && file.size > maxBytes) {
    return `文件不能超过 ${uploadPolicy.maxSizeMb}MB（系统配置-文件存储可调）`
  }
  return null
}

export function getFileGroupList(fileCategory?: string) {
  return get<FileGroupListResult>('/system/file-group/list', fileCategory ? { fileCategory } : undefined)
}

export interface FileGroupSaveDTO {
  id?: number
  name: string
}

export interface FilePageByGroupQuery extends PageQuery {
  fileCategory?: string
  originalName?: string
  groupId?: number
  ungrouped?: boolean
}

export function createFileGroup(data: FileGroupSaveDTO) {
  return post('/system/file-group', data)
}

export function updateFileGroup(data: FileGroupSaveDTO) {
  return put('/system/file-group', data)
}

export function deleteFileGroup(id: number) {
  return del(`/system/file-group/${id}`)
}

export function pageFileByGroup(params: FilePageByGroupQuery) {
  return get<PageResult<FileRecord>>('/system/file/page-by-group', params)
}

export function uploadFile(file: File, groupId?: number | null) {
  const formData = new FormData()
  formData.append('file', file)
  if (groupId != null && groupId > 0) {
    formData.append('groupId', String(groupId))
  }
  return post('/system/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteFile(id: number) {
  return del(`/system/file/${id}`)
}

export function getRecycleFilePage(params: { pageNo: number; pageSize: number; originalName?: string }) {
  return get<PageResult<FileRecord>>('/system/file/recycle/page', params)
}

export function restoreFile(id: number) {
  return put('/system/file/restore', null, { params: { id } })
}

export function deleteFilePermanent(id: number) {
  return del('/system/file/delete-permanent', { params: { id } })
}

export function deleteFileBatch(ids: number[]) {
  return del('/system/file/batch', { data: ids })
}

export function moveFiles(fileIds: number[], groupId: number) {
  return post('/system/file/move', { fileIds, groupId })
}

export function renameFile(id: number, newName: string) {
  return put(`/system/file/${id}/rename`, { newName })
}

export function getFileText(id: number) {
  return get<string>(`/system/file/text/${id}`)
}

/**
 * 为静态资源 URL 附加 Sa-Token（存于 localStorage token）。
 * img 标签通过 ?Authorization= 传 token，后端 AuthorizationQueryFilter 会写入请求头。
 */
export function withTokenQuery(url: string): string {
  if (!url) return ''
  const token = localStorage.getItem('token')
  if (!token || !url.startsWith('/api')) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}Authorization=${encodeURIComponent(token)}`
}

function normalizeFileApiUrl(url: string): string {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/api')) return url
  return url.startsWith('/') ? `/api${url}` : `/api/${url}`
}

/** 流式预览 URL（video/audio/img 直连，支持 Range，避免整文件 blob） */
export function getStreamPreviewUrl(file?: FileRecord | null): string {
  if (!file) return ''
  const type = file.fileType || ''
  const direct = normalizeFileApiUrl(file.url || '')
  if (direct && (type.startsWith('video/') || type.startsWith('audio/') || type.startsWith('image/'))) {
    return withTokenQuery(direct)
  }
  if (file.id) {
    return getPreviewApiUrl(file.id)
  }
  return withTokenQuery(direct)
}

/** 列表缩略图 / 视频封面（优先 /files/ 直链流式加载） */
export function fileDisplayUrl(file?: FileRecord | null): string {
  return getStreamPreviewUrl(file)
}

export function getPreviewApiUrl(id: number) {
  return withTokenQuery(`/api/system/file/preview/${id}`)
}

export function getDownloadApiUrl(id: number) {
  return withTokenQuery(`/api/system/file/download/${id}`)
}

/** 带鉴权下载/预览（blob，不走 JSON 拦截器） */
export async function fetchFileBlob(path: string): Promise<Blob> {
  const token = localStorage.getItem('token')
  const res = await axios.get(`/api${path}`, {
    responseType: 'blob',
    headers: token ? { Authorization: token } : {},
    withCredentials: true,
  })
  return res.data
}
