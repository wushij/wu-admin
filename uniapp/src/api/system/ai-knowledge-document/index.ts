import { get, post, put, del } from '@/core/http'
import { uploadFile } from '@/utils/upload'
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

/** 文档分页 */
export function pageKnowledgeDocument(params: KnowledgeDocumentPageQuery) {
  return get<PageResult<KnowledgeDocumentVO>>('/system/ai-knowledge-document/page', params)
}

/** 文档详情 */
export function getKnowledgeDocument(id: number) {
  return get<KnowledgeDocumentVO>(`/system/ai-knowledge-document/${id}`)
}

/**
 * 上传单个知识文档（移动端 uni.uploadFile 仅支持单文件，多文件由页面串行调用）。
 * 服务端自动解析、切片并向量化。
 */
export function uploadKnowledgeDocument(knowledgeBaseId: number, filePath: string) {
  return uploadFile<KnowledgeUploadResult[]>({
    url: '/system/ai-knowledge-document/upload',
    filePath,
    name: 'files',
    formData: { knowledgeBaseId: String(knowledgeBaseId) },
  })
}

/** 重新解析并重建切片 */
export function reparseKnowledgeDocument(id: number) {
  return post<boolean>(`/system/ai-knowledge-document/${id}/reparse`)
}

/** 重新切片并向量化 */
export function reindexKnowledgeDocument(id: number) {
  return post<boolean>(`/system/ai-knowledge-document/${id}/reindex`)
}

/** 重建整个知识库索引 */
export function reindexKnowledgeBase(knowledgeBaseId: number) {
  return post<boolean>('/system/ai-knowledge-document/reindex-all', undefined, {
    params: { knowledgeBaseId },
  })
}

/** 启用/停用文档 */
export function updateKnowledgeDocumentStatus(id: number, status: number) {
  return put<boolean>(`/system/ai-knowledge-document/${id}/status`, undefined, {
    params: { status },
  })
}

/** 删除文档 */
export function deleteKnowledgeDocument(id: number) {
  return del<boolean>(`/system/ai-knowledge-document/${id}`)
}

/** 批量删除文档 */
export function batchDeleteKnowledgeDocuments(ids: number[]) {
  return del<boolean>('/system/ai-knowledge-document/batch', { data: ids })
}
