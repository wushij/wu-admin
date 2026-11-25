import type { AiProvider } from '@/api/system/ai-model'

export interface ProviderMetaItem {
  label: string
  short: string
  color: string
  bg: string
  baseUrl: string
  portalUrl: string
  models: string[]
}

export const PROVIDER_META: Record<string, ProviderMetaItem> = {
  deepseek: {
    label: 'DeepSeek',
    short: 'DS',
    color: '#4d6bfe',
    bg: 'rgba(77, 107, 254, 0.12)',
    baseUrl: 'https://api.deepseek.com/v1',
    portalUrl: 'https://platform.deepseek.com',
    models: ['deepseek-v4.1-flash', 'deepseek-v4-pro'],
  },
  kimi: {
    label: 'Kimi',
    short: 'KM',
    color: '#64748b',
    bg: 'rgba(100, 116, 139, 0.12)',
    baseUrl: 'https://api.moonshot.cn/v1',
    portalUrl: 'https://platform.kimi.com',
    models: ['kimi-k3.1-pro', 'kimi-k3.1-pro-highspeed', 'kimi-k3', 'kimi-k2.6'],
  },
  zhipu: {
    label: 'GLM',
    short: 'GLM',
    color: '#3485ff',
    bg: 'rgba(52, 133, 255, 0.12)',
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    portalUrl: 'https://bigmodel.cn',
    models: ['glm-5.3', 'glm-5.3-flash', 'glm-5.2'],
  },
  custom: {
    label: '自定义',
    short: 'AI',
    color: '#8b5cf6',
    bg: 'rgba(139, 92, 246, 0.12)',
    baseUrl: '',
    portalUrl: '',
    models: [],
  },
}

export const PROVIDER_OPTIONS = Object.entries(PROVIDER_META).map(([value, meta]) => ({
  value: value as AiProvider,
  label: meta.label,
}))

export const PROTOCOL_OPTIONS = [
  { value: 'openai', label: 'OpenAI 兼容' },
  { value: 'anthropic', label: 'Anthropic Messages' },
]

/** 模型类型选项：对话模型 / 向量模型 */
export const MODEL_TYPE_OPTIONS = [
  { value: 'chat', label: '对话模型', desc: '用于 AI 助手对话生成' },
  { value: 'embedding', label: '向量模型', desc: '用于知识库切片向量化与语义检索' },
]

/** 常用向量模型预设，选择后自动填充维度，减少手填错误 */
export const EMBEDDING_MODEL_PRESETS = [
  { value: 'text-embedding-3-small', label: 'OpenAI text-embedding-3-small', dimension: 1536 },
  { value: 'text-embedding-3-large', label: 'OpenAI text-embedding-3-large', dimension: 3072 },
  { value: 'bge-large-zh-v1.5', label: 'BGE bge-large-zh-v1.5', dimension: 1024 },
  { value: 'bge-m3', label: 'BGE bge-m3', dimension: 1024 },
]

/** 按模型名猜测维度，找不到返回 0 表示不修改 */
export function guessEmbeddingDimension(modelName?: string): number {
  if (!modelName) return 0
  const matched = EMBEDDING_MODEL_PRESETS.find((item) => item.value === modelName)
  return matched?.dimension ?? 0
}

export function providerMeta(provider: string): ProviderMetaItem {
  if (PROVIDER_META[provider]) return PROVIDER_META[provider]
  return {
    label: provider || '未知',
    short: 'AI',
    color: '#64748b',
    bg: 'rgba(100, 116, 139, 0.12)',
    baseUrl: '',
    portalUrl: '',
    models: [],
  }
}
