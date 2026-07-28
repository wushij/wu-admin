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
    models: ['deepseek-v4-flash', 'deepseek-v4-pro'],
  },
  openai: {
    label: 'OpenAI',
    short: 'GPT',
    color: '#10a37f',
    bg: 'rgba(16, 163, 127, 0.12)',
    baseUrl: 'https://api.openai.com/v1',
    models: ['gpt-4o', 'gpt-4o-mini'],
  },
  qwen: {
    label: '通义千问',
    short: 'QW',
    color: '#615ced',
    bg: 'rgba(97, 92, 237, 0.12)',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    models: ['qwen3.7-plus', 'qwen3.7-max', 'qwen3.6-plus'],
  },
  kimi: {
    label: 'Kimi',
    short: 'KM',
    color: '#0f172a',
    bg: 'rgba(15, 23, 42, 0.08)',
    baseUrl: 'https://api.moonshot.cn/v1',
    models: ['kimi-k2.7-code', 'kimi-k2.6'],
  },
}

export const AI_PROVIDER_KEYS = Object.keys(AI_PROVIDER_META)

export function providerMeta(provider: string): AiProviderMeta {
  return (
    AI_PROVIDER_META[provider] || {
      label: provider,
      short: 'AI',
      color: '#64748b',
      bg: 'rgba(100, 116, 139, 0.12)',
      baseUrl: '',
      models: [],
    }
  )
}
