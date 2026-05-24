import { get, post, put, del } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

/** 知识分类 */
export type AiKnowledgeCategory = 'faq' | 'manual' | 'module' | 'other'

export interface AiKnowledgeVO {
  id: number
  title: string
  keywords?: string
  content: string
  category?: AiKnowledgeCategory | string
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

export interface AiKnowledgePageQuery extends PageQuery {
  keyword?: string
  category?: string
  status?: number | null
}

export function pageAiKnowledge(params: AiKnowledgePageQuery) {
  return get<PageResult<AiKnowledgeVO>>('/system/ai-knowledge/page', params)
}

export function getAiKnowledge(id: number) {
  return get<AiKnowledgeVO>(`/system/ai-knowledge/${id}`)
}

export function createAiKnowledge(data: AiKnowledgeSaveDTO) {
  return post('/system/ai-knowledge', data)
}

export function updateAiKnowledge(data: AiKnowledgeSaveDTO) {
  return put('/system/ai-knowledge', data)
}

export function deleteAiKnowledge(id: number) {
  return del(`/system/ai-knowledge/${id}`)
}
