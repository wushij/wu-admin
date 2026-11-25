import { get, post, put, del } from '@/utils/request'
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

export function pageKnowledgeBase(params: KnowledgeBasePageQuery) {
  return get<PageResult<KnowledgeBaseVO>>('/system/ai-knowledge-base/page', params)
}

export function getKnowledgeBase(id: number) {
  return get<KnowledgeBaseVO>(`/system/ai-knowledge-base/${id}`)
}

export function listEnabledKnowledgeBase() {
  return get<KnowledgeBaseVO[]>('/system/ai-knowledge-base/list-enabled')
}

export function listEmbeddingModels() {
  return get<EmbeddingModelOption[]>('/system/ai-knowledge-base/embedding-models')
}

export function createKnowledgeBase(data: KnowledgeBaseSaveDTO) {
  return post<number>('/system/ai-knowledge-base', data)
}

export function updateKnowledgeBase(data: KnowledgeBaseSaveDTO) {
  return put('/system/ai-knowledge-base', data)
}

export function deleteKnowledgeBase(id: number) {
  return del(`/system/ai-knowledge-base/${id}`)
}

export function getKnowledgeChunkStats(id: number) {
  return get<KnowledgeChunkStatsVO>(`/system/ai-knowledge-base/${id}/chunk-stats`)
}

export function retrieveKnowledgeTest(data: KnowledgeRetrieveDTO) {
  return post<KnowledgeRetrieveResult>('/system/ai-knowledge-base/retrieve-test', data, {
    timeout: 60000,
  })
}
