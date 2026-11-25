import { get, post, put, del } from '@/core/http'
import type { PageQuery, PageResult } from '@/types/api'

/** 知识库索引状态 */
export type KnowledgeIndexStatus = 'IDLE' | 'INDEXING' | 'INDEXED' | 'FAILED'

/** 向量模型（用于知识库下拉） */
export interface EmbeddingModelOption {
  id: number
  name: string
  modelName: string
  dimension?: number
  isDefault?: number
}

export interface KnowledgeBaseVO {
  id: number
  name: string
  description?: string
  embeddingModelId?: number
  /** 实际生效的向量模型名称 */
  embeddingModelName?: string
  chunkSize?: number
  chunkOverlap?: number
  documentCount?: number
  chunkCount?: number
  indexedChunkCount?: number
  indexStatus?: KnowledgeIndexStatus | string
  status?: number
  sort?: number
  createTime?: string
  updateTime?: string
}

export interface KnowledgeBaseSaveDTO {
  id?: number
  name: string
  description?: string
  embeddingModelId?: number
  chunkSize?: number
  chunkOverlap?: number
  status?: number
  sort?: number
}

export interface KnowledgeBasePageQuery extends PageQuery {
  name?: string
  status?: number | null
  indexStatus?: string
}

export interface KnowledgeChunkStatsVO {
  total: number
  indexed: number
  pending: number
  failed: number
  avgTokens: number
  totalTokens: number
}

export interface KnowledgeRetrieveDTO {
  knowledgeBaseId: number
  query: string
  topK?: number
  minScore?: number
}

export interface KnowledgeRetrieveItem {
  chunkId: number
  documentId: number
  documentName: string
  heading: string
  content: string
  score: number
}

export interface KnowledgeRetrieveResult {
  query: string
  matchMode: 'VECTOR' | 'KEYWORD' | 'NONE' | string
  topK: number
  minScore: number
  items: KnowledgeRetrieveItem[]
}

/** 知识库分页 */
export function pageKnowledgeBase(params: KnowledgeBasePageQuery) {
  return get<PageResult<KnowledgeBaseVO>>('/system/ai-knowledge-base/page', params)
}

/** 知识库详情 */
export function getKnowledgeBase(id: number) {
  return get<KnowledgeBaseVO>(`/system/ai-knowledge-base/${id}`)
}

/** 启用中的知识库列表 */
export function listEnabledKnowledgeBase() {
  return get<KnowledgeBaseVO[]>('/system/ai-knowledge-base/list-enabled')
}

/** 启用中的向量模型列表 */
export function listEmbeddingModels() {
  return get<EmbeddingModelOption[]>('/system/ai-knowledge-base/embedding-models')
}

/** 新增知识库 */
export function createKnowledgeBase(data: KnowledgeBaseSaveDTO) {
  return post<number>('/system/ai-knowledge-base', data)
}

/** 修改知识库 */
export function updateKnowledgeBase(data: KnowledgeBaseSaveDTO) {
  return put<boolean>('/system/ai-knowledge-base', data)
}

/** 删除知识库及其文档与切片 */
export function deleteKnowledgeBase(id: number) {
  return del<boolean>(`/system/ai-knowledge-base/${id}`)
}

/** 知识库切片统计 */
export function getKnowledgeBaseChunkStats(id: number) {
  return get<KnowledgeChunkStatsVO>(`/system/ai-knowledge-base/${id}/chunk-stats`)
}

/** 知识库检索测试 */
export function retrieveKnowledgeTest(data: KnowledgeRetrieveDTO) {
  return post<KnowledgeRetrieveResult>('/system/ai-knowledge-base/retrieve-test', data)
}
