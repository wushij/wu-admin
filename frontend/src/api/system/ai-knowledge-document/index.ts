import { get, post, put, del } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export type KnowledgeParseStatus = 'PENDING' | 'PARSING' | 'SUCCESS' | 'FAILED'
export type KnowledgeDocumentIndexStatus = 'PENDING' | 'INDEXING' | 'INDEXED' | 'PARTIAL' | 'FAILED'

export interface KnowledgeDocumentVO {
  id: number
  knowledgeBaseId: number
  fileId?: number
  fileName: string
  fileType?: string
  fileSize?: number
  charCount?: number
  chunkCount?: number
  parseStatus?: KnowledgeParseStatus | string
  indexStatus?: KnowledgeDocumentIndexStatus | string
  errorMsg?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface KnowledgeDocumentPageQuery extends PageQuery {
  knowledgeBaseId: number
  fileName?: string
  parseStatus?: string
  indexStatus?: string
}

/** 单个文件的上传处理结果 */
export interface KnowledgeUploadResult {
  fileName: string
  success: boolean
  message: string
}

export function pageKnowledgeDocument(params: KnowledgeDocumentPageQuery) {
  return get<PageResult<KnowledgeDocumentVO>>('/system/ai-knowledge-document/page', params)
}

export function getKnowledgeDocument(id: number) {
  return get<KnowledgeDocumentVO>(`/system/ai-knowledge-document/${id}`)
}

/** 批量上传文档，服务端自动解析、切片并向量化 */
export function uploadKnowledgeDocuments(knowledgeBaseId: number, files: File[]) {
  const formData = new FormData()
  formData.append('knowledgeBaseId', String(knowledgeBaseId))
  files.forEach((file) => formData.append('files', file))
  return post<KnowledgeUploadResult[]>('/system/ai-knowledge-document/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })
}

export function reparseKnowledgeDocument(id: number) {
  return post(`/system/ai-knowledge-document/${id}/reparse`)
}

export function reindexKnowledgeDocument(id: number) {
  return post(`/system/ai-knowledge-document/${id}/reindex`)
}

export function reindexKnowledgeBase(knowledgeBaseId: number) {
  return post(`/system/ai-knowledge-document/reindex-all?knowledgeBaseId=${knowledgeBaseId}`, undefined, {
    timeout: 60000,
  })
}

export function updateKnowledgeDocumentStatus(id: number, status: number) {
  return put(`/system/ai-knowledge-document/${id}/status?status=${status}`)
}

export function deleteKnowledgeDocument(id: number) {
  return del(`/system/ai-knowledge-document/${id}`)
}

export function batchDeleteKnowledgeDocuments(ids: number[]) {
  return del('/system/ai-knowledge-document/batch', { data: ids })
}
