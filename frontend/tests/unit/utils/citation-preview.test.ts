import { describe, expect, it } from 'vitest'
import {
  extractCitationDomain,
  formatCitationSnippet,
  resolveCitationPreviewText,
} from '@/utils/ai-markdown/citation-preview'

describe('citation-preview', () => {
  it('清洗 Markdown 标题与 HTML 实体为可读纯文本', () => {
    const raw = '您可以为产品系列设置发布日期。 #### 步骤： 1. 在 Shopify 后台中，转至 产品 &gt; 产品系列。'
    expect(formatCitationSnippet(raw)).toBe(
      '您可以为产品系列设置发布日期。 步骤： 1. 在 Shopify 后台中，转至 产品 > 产品系列。',
    )
  })

  it('清洗斜体编号与表格符号', () => {
    expect(formatCitationSnippet('_1_广东福建等地有暴雨 _2_火热升级!')).toBe(
      '1 广东福建等地有暴雨 2 火热升级!',
    )
    expect(
      formatCitationSnippet('|||||||||||| -------------- | 时间 | 08:00 | 11:00 | 14:00 | 风向 | 西北风 |'),
    ).toBe('时间 · 08:00 · 11:00 · 14:00 · 风向 · 西北风')
  })

  it('识别 JSON 与代码碎片并回退展示域名', () => {
    const jsonRaw = '}, "displayDate": { "year": 2025, "month": 2, "day": 14 }, "type": "SCATTERED_SHOWERS" }'
    expect(formatCitationSnippet(jsonRaw)).toBe('')
    expect(
      resolveCitationPreviewText(jsonRaw, 'https://developers.google.com/maps/documentation/weather'),
    ).toBe('developers.google.com')
  })

  it('提取域名兜底', () => {
    expect(extractCitationDomain('https://support.apple.com/zh-cn/guide/mac-help')).toBe('support.apple.com')
    expect(resolveCitationPreviewText(undefined, 'https://www.weather.com.cn/weather/101010100.shtml')).toBe(
      'www.weather.com.cn',
    )
  })
})
