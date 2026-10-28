/** AI 供应商元信息（与 PC 端 PROVIDER_META 对齐） */
export interface AiProviderMeta {
  label: string
  short: string
  color: string
  bg: string
  baseUrl: string
  models: string[]
}

export const AI_PROVIDER_META: Record<string, AiProviderMeta> = {
  deepseek: {
    label: 'DeepSeek',
    short: 'DS',
    color: '#4d6bfe',
    bg: 'rgba(77, 107, 254, 0.12)',
    baseUrl: 'https://api.deepseek.com/v1',
    models: ['deepseek-v4.1-flash', 'deepseek-v4-pro'],
  },
  kimi: {
    label: 'Kimi',
    short: 'KM',
    color: '#0f172a',
    bg: 'rgba(15, 23, 42, 0.08)',
    baseUrl: 'https://api.moonshot.cn/v1',
    models: ['kimi-k3.1-pro', 'kimi-k3.1-pro-highspeed', 'kimi-k3', 'kimi-k2.6'],
  },
  zhipu: {
    label: 'GLM',
    short: 'GLM',
    color: '#3485ff',
    bg: 'rgba(52, 133, 255, 0.12)',
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    models: ['glm-5.3', 'glm-5.3-flash', 'glm-5.2'],
  },
  custom: {
    label: '自定义',
    short: 'AI',
    color: '#8b5cf6',
    bg: 'rgba(139, 92, 246, 0.12)',
    baseUrl: '',
    models: [],
  },
}

export const AI_PROVIDER_KEYS = Object.keys(AI_PROVIDER_META)

/** 接入协议元信息（与 PC 端 PROTOCOL_META 对齐） */
export interface AiProtocolMeta {
  label: string
}

export const AI_PROTOCOL_META: Record<string, AiProtocolMeta> = {
  openai: {
    label: 'OpenAI 兼容',
  },
  anthropic: {
    label: 'Anthropic Messages',
  },
}

export const AI_PROTOCOL_KEYS = Object.keys(AI_PROTOCOL_META)

export function protocolMeta(protocol: string): AiProtocolMeta {
  return AI_PROTOCOL_META[protocol] || AI_PROTOCOL_META.openai
}

export function providerMeta(provider: string): AiProviderMeta {
  if (AI_PROVIDER_META[provider]) return AI_PROVIDER_META[provider]
  return {
    label: provider || '未知',
    short: 'AI',
    color: '#64748b',
    bg: 'rgba(100, 116, 139, 0.12)',
    baseUrl: '',
    models: [],
  }
}
