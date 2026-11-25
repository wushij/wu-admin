import { describe, expect, it } from 'vitest'
import { normalizeWebSearchCitations } from '@/api/ai'

describe('normalizeWebSearchCitations', () => {
  it('filters non-http links and keeps valid citations with default source fallback', () => {
    const input = [
      {
        title: 'DeepSeek 官方文档',
        url: 'https://api-docs.deepseek.com/',
        snippet: 'API 说明',
        source: 'DeepSeek 联网搜索',
      },
      {
        title: '危险脚本链接',
        url: 'javascript:alert(1)',
        snippet: 'xss',
      },
      {
        title: '   ',
        url: 'https://example.com/empty-title',
      },
      {
        title: '无来源标签链接',
        url: 'http://spring.io/projects/spring-boot',
      },
    ]

    const result = normalizeWebSearchCitations(input)
    expect(result).toHaveLength(2)
    expect(result[0]).toEqual({
      title: 'DeepSeek 官方文档',
      url: 'https://api-docs.deepseek.com/',
      snippet: 'API 说明',
      source: 'DeepSeek 联网搜索',
    })
    expect(result[1]).toEqual({
      title: '无来源标签链接',
      url: 'http://spring.io/projects/spring-boot',
      snippet: undefined,
      source: '联网检索',
    })
  })

  it('returns empty array for invalid payloads', () => {
    expect(normalizeWebSearchCitations(null)).toEqual([])
    expect(normalizeWebSearchCitations({})).toEqual([])
  })
})
