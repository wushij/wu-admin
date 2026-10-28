import http, { get, post, put, del } from '@/core/http'
import type { ApiResult, PageResult } from '@/types/api'

/** 供应商标识 */
export type AiProvider = 'deepseek' | 'kimi' | 'zhipu' | 'openai' | 'custom' | string

/** 接入协议 */
export type AiProtocol = 'openai' | 'anthropic'

export interface AiModelVO {
  id: number
  name: string
  provider: AiProvider
  /** 接入协议：openai（兼容协议）/ anthropic（Messages 协议） */
  protocol?: AiProtocol | string
  modelName: string
  baseUrl: string
  /** 自定义请求路径模板，支持 {model} 占位 */
  pathTemplate?: string
  /** 额外请求头 JSON */
  extraHeaders?: string
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
  protocol?: AiProtocol | string
  modelName: string
  baseUrl: string
  pathTemplate?: string
  extraHeaders?: string
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
  protocol?: AiProtocol | string
  modelName?: string
  baseUrl?: string
  pathTemplate?: string
  extraHeaders?: string
  apiKey?: string
}

/** 拉取供应商模型列表入参：字段含义与测试连接一致，无需 modelName */
export interface AiModelModelsDTO {
  id?: number
  provider?: string
  protocol?: AiProtocol | string
  baseUrl?: string
  pathTemplate?: string
  extraHeaders?: string
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

/** 拉取供应商可用模型列表，供表单「获取模型」使用 */
export function listAiModels(data: AiModelModelsDTO) {
  return http.post('/system/ai-model/models', data as unknown as Record<string, unknown>, {
    timeout: 30000,
  }) as Promise<ApiResult<string[]>>
}
