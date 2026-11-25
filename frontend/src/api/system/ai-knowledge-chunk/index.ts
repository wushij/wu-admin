import { get, post } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'
import type { KnowledgeChunkStatsVO } from '@/api/system/ai-knowledge-base'

export type ChunkEmbedStatus = 'PENDING' | 'INDEXED' | 'FAILED'

export interface KnowledgeChunkVO {
  id: number
  knowledgeBaseId: number
  documentId: number
  documentName?: string
  chunkIndex: number
  heading?: string
  content: string
  charCount?: number
  tokenEstimate?: number
  embedStatus?: ChunkEmbedStatus | string
  embeddingModel?: string
  embeddingDim?: number
  errorMsg?: string
  createTime?: string
}

export interface KnowledgeChunkPageQuery extends PageQuery {
  knowledgeBaseId: number
  documentId?: number | null
  keyword?: string
  embedStatus?: string
}

export function pageKnowledgeChunk(params: KnowledgeChunkPageQuery) {
  return get<PageResult<KnowledgeChunkVO>>('/system/ai-knowledge-chunk/page', params)
}

export function getKnowledgeChunk(id: number) {
  return get<KnowledgeChunkVO>(`/system/ai-knowledge-chunk/${id}`)
}

export function getKnowledgeChunkStats(knowledgeBaseId: number) {
  return get<KnowledgeChunkStatsVO>('/system/ai-knowledge-chunk/stats', { knowledgeBaseId })
}

export function reindexKnowledgeChunk(id: number) {
  return post(`/system/ai-knowledge-chunk/${id}/reindex`, undefined, { timeout: 60000 })
}
