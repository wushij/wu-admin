import http, { get, post, put, del } from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

/** 供应商标识 */
export type AiProvider = 'deepseek' | 'openai' | 'qwen' | 'kimi'

export interface AiModelVO {
  id: number
  name: string
  provider: AiProvider
  modelName: string
  baseUrl: string
  /** API Key 掩码（如 sk-a***x9f2） */
  apiKeyMasked?: string
  /** 是否已配置 API Key */
  hasApiKey?: boolean
  temperature?: number
  maxTokens?: number
  systemPrompt?: string
  isDefault?: number
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface AiModelSaveDTO {
  id?: number
  name: string
  provider: AiProvider | string
  modelName: string
  baseUrl: string
  /** 明文 API Key，编辑时留空表示不修改 */
  apiKey?: string
  temperature?: number
  maxTokens?: number
  systemPrompt?: string
  isDefault?: number
  status?: number
  remark?: string
}

export interface AiModelTestDTO {
  id?: number
  provider?: string
  modelName?: string
  baseUrl?: string
  apiKey?: string
}

export interface AiModelPageQuery {
  pageNo?: number
  pageSize?: number
  name?: string
  provider?: string
  status?: number | null
}

export function pageAiModel(params: AiModelPageQuery) {
  return get<PageResult<AiModelVO>>('/system/ai-model/page', params)
}

export function getAiModel(id: number) {
  return get<AiModelVO>(`/system/ai-model/${id}`)
}

export function createAiModel(data: AiModelSaveDTO) {
  return post('/system/ai-model', data)
}

export function updateAiModel(data: AiModelSaveDTO) {
  return put('/system/ai-model', data)
}

export function deleteAiModel(id: number) {
  return del(`/system/ai-model/${id}`)
}

export function setDefaultAiModel(id: number) {
  return put(`/system/ai-model/${id}/default`)
}

/** 连通性测试，返回延迟毫秒数（耗时较长，放宽超时） */
export function testAiModel(data: AiModelTestDTO) {
  return http.post('/system/ai-model/test', data as unknown as Record<string, unknown>, {
    timeout: 40000,
  }) as Promise<ApiResult<number>>
}
