import { get, post, put, del } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

/** 供应商标识 */
export type AiProvider = 'deepseek' | 'kimi' | 'zhipu' | 'openai' | 'custom' | string

/** 接入协议 */
export type AiProtocol = 'openai' | 'anthropic'

/** 模型类型：对话模型 / 向量模型 */
export type AiModelType = 'chat' | 'embedding'

export interface AiModelVO {
  id: number
  name: string
  provider: AiProvider
  /** 接入协议：openai（兼容协议）/ anthropic（Messages 协议） */
  protocol?: AiProtocol | string
  /** 模型类型：chat=对话模型 / embedding=向量模型 */
  modelType?: AiModelType | string
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
  /** 向量维度（向量模型） */
  dimension?: number
  /** 向量化批量大小（向量模型） */
  batchSize?: number
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
  /** 模型类型：chat=对话模型 / embedding=向量模型 */
  modelType?: AiModelType | string
  modelName: string
  baseUrl: string
  pathTemplate?: string
  extraHeaders?: string
  /** 明文 API Key，编辑时留空表示不修改 */
  apiKey?: string
  temperature?: number
  maxTokens?: number
  /** 向量维度（向量模型必填） */
  dimension?: number
  /** 向量化批量大小（向量模型可选） */
  batchSize?: number
  systemPrompt?: string
  isDefault?: number
  status?: number
  remark?: string
}

export interface AiModelTestDTO {
  id?: number
  provider?: string
  protocol?: AiProtocol | string
  /** 模型类型与维度用于向量模型连通性测试 */
  modelType?: AiModelType | string
  dimension?: number
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

export interface AiModelPageQuery extends PageQuery {
  name?: string
  provider?: string
  modelType?: AiModelType | string
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
  return post<number>('/system/ai-model/test', data, { timeout: 40000 })
}

/** 拉取供应商可用模型列表，供表单「获取模型」按钮使用 */
export function listAiModels(data: AiModelModelsDTO) {
  return post<string[]>('/system/ai-model/models', data, { timeout: 30000 })
}
