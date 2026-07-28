import { get, del } from '@/utils/request'
import type { PageResult } from '@/types/api'

export interface AiChatLogVO {
  id: number
  userId: number
  username: string
  conversationId: string
  modelId: number
  provider: string
  modelName: string
  question: string
  answer: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  durationMs: number
  /** 1:成功 0:失败 2:用户中断 */
  chatStatus: number
  errorMsg?: string
  source: string
  createTime?: string
}

export interface AiChatLogPageQuery {
  pageNo?: number
  pageSize?: number
  username?: string
  provider?: string
  chatStatus?: number | null
  conversationId?: string
}

export function pageAiChatLog(params: AiChatLogPageQuery) {
  return get<PageResult<AiChatLogVO>>('/system/ai-log/page', params)
}

export function deleteAiChatLog(id: number) {
  return del(`/system/ai-log/${id}`)
}

export function cleanAiChatLog() {
  return del('/system/ai-log/clean')
}
