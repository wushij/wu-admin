import { get, post } from '@/core/http'
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

/** 切片分页 */
export function pageKnowledgeChunk(params: KnowledgeChunkPageQuery) {
  return get<PageResult<KnowledgeChunkVO>>('/system/ai-knowledge-chunk/page', params)
}

/** 切片详情 */
export function getKnowledgeChunk(id: number) {
  return get<KnowledgeChunkVO>(`/system/ai-knowledge-chunk/${id}`)
}

/** 切片统计 */
export function getKnowledgeChunkStats(knowledgeBaseId: number) {
  return get<KnowledgeChunkStatsVO>('/system/ai-knowledge-chunk/stats', { knowledgeBaseId })
}

/** 重建单个切片向量 */
export function reindexKnowledgeChunk(id: number) {
  return post<boolean>(`/system/ai-knowledge-chunk/${id}/reindex`)
}
