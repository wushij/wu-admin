import request, { get, del, put } from '@/utils/request'

const BLOCKED_EXTENSIONS = ['exe', 'bat', 'cmd', 'sh', 'ps1', 'msi', 'dll', 'com', 'scr']

/** 由接口 /system/file/upload-policy 加载，保存系统配置后立即更新 */
export const uploadPolicy = {
  maxSizeMb: 50,
  allowedExtensions: [],
  platformMaxMb: 500
}

export function getFileUploadPolicy() {
  return get('/system/file/upload-policy')
}

export function setUploadPolicyFromApi(data) {
  if (!data) return
  if (data.maxSizeMb != null) uploadPolicy.maxSizeMb = Number(data.maxSizeMb) || 50
  if (data.platformMaxMb != null) uploadPolicy.platformMaxMb = Number(data.platformMaxMb) || 500
  if (data.allowedExtensions) {
    uploadPolicy.allowedExtensions = String(data.allowedExtensions)
      .split(',')
      .map((s) => s.trim().toLowerCase())
      .filter(Boolean)
  }
}

function getAllowedExtensions() {
  if (uploadPolicy.allowedExtensions?.length) {
    return uploadPolicy.allowedExtensions
  }
  return [
    'jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'pdf', 'doc', 'docx', 'xls', 'xlsx',
    'ppt', 'pptx', 'txt', 'md', 'json', 'xml', 'zip', 'rar', 'mp4', 'mp3', 'wav', 'avi', 'mov'
  ]
}

export function getFileExtension(fileName) {
  if (!fileName || !fileName.includes('.')) return ''
  return fileName.slice(fileName.lastIndexOf('.') + 1).toLowerCase()
}

/** 上传前校验，返回错误文案；通过返回 null */
export function validateFileBeforeUpload(file) {
  const ext = getFileExtension(file?.name)
  if (!ext) {
    return '文件名需包含扩展名'
  }
  if (BLOCKED_EXTENSIONS.includes(ext)) {
    return `不允许上传可执行文件：.${ext}`
  }
  if (!getAllowedExtensions().includes(ext)) {
    return `不允许上传该类型文件：.${ext}`
  }
  const maxBytes = uploadPolicy.maxSizeMb * 1024 * 1024
  if (file?.size > maxBytes) {
    return `文件不能超过 ${uploadPolicy.maxSizeMb}MB（系统配置-文件存储可调）`
  }
  return null
}

export function getFileGroupList() {
  return get('/system/file-group/list')
}

export function createFileGroup(data) {
  return request.post('/system/file-group', data)
}

export function updateFileGroup(data) {
  return request.put('/system/file-group', data)
}

export function deleteFileGroup(id) {
  return del(`/system/file-group/${id}`)
}

export function pageFileByGroup(params) {
  return get('/system/file/page-by-group', params)
}

export function uploadFile(file, groupId) {
  const formData = new FormData()
  formData.append('file', file)
  if (groupId != null && groupId > 0) {
    formData.append('groupId', String(groupId))
  }
  return request.post('/system/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteFile(id) {
  return del(`/system/file/${id}`)
}

export function deleteFileBatch(ids) {
  return request.delete('/system/file/batch', { data: ids })
}

export function moveFiles(fileIds, groupId) {
  return request.post('/system/file/move', { fileIds, groupId })
}

export function renameFile(id, newName) {
  return put(`/system/file/${id}/rename`, { newName })
}

export function getFileText(id) {
  return get(`/system/file/text/${id}`)
}

import axios from 'axios'

/**
 * 为静态资源 URL 附加 Sa-Token（与网关、后端共用，存于 localStorage token）。
 * 网关校验通过后原样转发 Authorization，img 才能正常显示。
 */
export function withGatewayTokenQuery(url) {
  if (!url) return ''
  const token = localStorage.getItem('token')
  if (!token || !url.startsWith('/api')) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}Authorization=${encodeURIComponent(token)}`
}

/** @deprecated 使用 withGatewayTokenQuery */
export const withAuthQuery = withGatewayTokenQuery

/** 列表缩略图 / 视频封面（走 /api/files 或 preview，依赖网关转发 Authorization） */
export function fileDisplayUrl(file) {
  if (!file) return ''
  let u = file.url || ''
  if (u && !u.startsWith('http') && !u.startsWith('/api')) {
    u = u.startsWith('/') ? `/api${u}` : `/api/${u}`
  }
  if (!u && file.id) {
    u = `/api/system/file/preview/${file.id}`
  }
  return withGatewayTokenQuery(u)
}

export function getPreviewApiUrl(id) {
  return withGatewayTokenQuery(`/api/system/file/preview/${id}`)
}

export function getDownloadApiUrl(id) {
  return withGatewayTokenQuery(`/api/system/file/download/${id}`)
}

/** 带鉴权下载/预览（blob，不走 JSON 拦截器） */
export async function fetchFileBlob(path) {
  const token = localStorage.getItem('token')
  const res = await axios.get(`/api${path}`, {
    responseType: 'blob',
    headers: token ? { Authorization: token } : {},
    withCredentials: true
  })
  return res.data
}
