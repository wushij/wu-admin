import { get, post, put, del } from '@/core/http'
import type { PageResult } from '@/types/api'

export interface AiKnowledgeVO {
  id: number
  title: string
  keywords?: string
  content: string
  category?: string
  sort?: number
  status?: number
  creator?: string
  updater?: string
  createTime?: string
  updateTime?: string
}

export interface AiKnowledgeSaveDTO {
  id?: number
  title: string
  keywords?: string
  content: string
  category?: string
  sort?: number
  status?: number
}

export interface AiKnowledgePageQuery {
  pageNo?: number
  pageSize?: number
  keyword?: string
  category?: string
  status?: number | null
}

export interface AiKnowledgeTestRetrieveResult {
  query: string
  searchTerms: string[]
  matchMode: 'FULLTEXT' | 'LIKE' | 'NONE'
  hitList: AiKnowledgeVO[]
  referenceBlock: string
  charCount: number
  estimatedTokens: number
}

export interface AiKnowledgeStats {
  total: number
  activeCount: number
  disabledCount: number
  totalWords: number
}

export function pageAiKnowledge(params: AiKnowledgePageQuery) {
  return get<PageResult<AiKnowledgeVO>>('/system/ai-knowledge/page', params)
}

export function getAiKnowledge(id: number) {
  return get<AiKnowledgeVO>(`/system/ai-knowledge/${id}`)
}

export function createAiKnowledge(data: AiKnowledgeSaveDTO) {
  return post<boolean>('/system/ai-knowledge', data)
}

export function updateAiKnowledge(data: AiKnowledgeSaveDTO) {
  return put<boolean>('/system/ai-knowledge', data)
}

export function deleteAiKnowledge(id: number) {
  return del<boolean>(`/system/ai-knowledge/${id}`)
}

export function updateAiKnowledgeStatus(id: number, status: number) {
  return put<boolean>(`/system/ai-knowledge/${id}/status?status=${status}`)
}

/** 知识库检索演练与召回测试 */
export function testRetrieveAiKnowledge(query: string) {
  return post<AiKnowledgeTestRetrieveResult>('/system/ai-knowledge/test-retrieve', { query })
}

/** 获取知识库统计指标 */
export function getAiKnowledgeStats() {
  return get<AiKnowledgeStats>('/system/ai-knowledge/stats')
}
