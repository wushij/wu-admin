// @vitest-environment happy-dom
import { describe, expect, it } from 'vitest'
import {
  normalizeChatMarkdown,
  renderChatMarkdown,
} from '@/utils/ai-markdown/chat-markdown'
import { replaceSectionEmojisWithIcons } from '@/utils/ai-markdown/section-icons'

describe('AI Markdown 小节排版与矢量图标替换', () => {
  it('preprocess: 标题和独立行中的 Emoji + 加粗标题不应被拆成两行', () => {
    const md = [
      '### 📌 **结论**',
      '',
      '这是一套测验。',
      '',
      '### 📋 **试卷结构**',
      '',
      '🎯 **参考答案与解析要点**',
    ].join('\n')

    const normalized = normalizeChatMarkdown(md)
    expect(normalized).toContain('### 📌 结论')
    expect(normalized).toContain('### 📋 试卷结构')
    expect(normalized).toContain('🎯 **参考答案与解析要点**')
    expect(normalized).not.toMatch(/📌\s*\n+\s*\*\*结论\*\*/)
    expect(normalized).not.toMatch(/📋\s*\n+\s*\*\*试卷结构\*\*/)
    expect(normalized).not.toMatch(/🎯\s*\n+\s*\*\*参考答案与解析要点\*\*/)
  })

  it('section-icons: 标题中的 Emoji 应在同一行替换为精致矢量 SVG 图标', () => {
    const rawHtml = '<h3>📌 结论</h3><p>正文</p><h3>📋 试卷结构</h3>'
    const iconized = replaceSectionEmojisWithIcons(rawHtml)
    const doc = new DOMParser().parseFromString(`<div>${iconized}</div>`, 'text/html')

    const headings = Array.from(doc.querySelectorAll('h3'))
    expect(headings).toHaveLength(2)
    expect(headings[0].textContent?.trim()).toBe('结论')
    expect(headings[1].textContent?.trim()).toBe('试卷结构')
    expect(headings[0].querySelector('svg')).not.toBeNull()
    expect(headings[1].querySelector('svg')).not.toBeNull()
    expect(headings[0].classList.contains('chat-md-heading-with-icon')).toBe(true)
    expect(iconized).not.toContain('📌')
    expect(iconized).not.toContain('📋')
  })

  it('renderChatMarkdown: 完整对话中的「Emoji + 加粗标题」应合并为单行小节且保留图标与主题色', () => {
    const md = [
      '以下为本次评估结果：',
      '',
      '📌 **结论**',
      '',
      '这是一套 10 题、100 分的随堂测验。',
      '',
      '📋 **试卷结构**',
      '',
      '| 题型 | 题量 |',
      '| --- | --- |',
      '| 单选 | 5 |',
      '',
      '🎯 **参考答案与解析要点**',
      '',
      '解析正文。',
    ].join('\n')

    const html = renderChatMarkdown(md)
    const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html')
    const sectionTitles = Array.from(doc.querySelectorAll('p.chat-md-section-title'))

    expect(sectionTitles).toHaveLength(3)
    expect(sectionTitles.map((el) => el.textContent?.trim())).toEqual([
      '结论',
      '试卷结构',
      '参考答案与解析要点',
    ])
    for (const title of sectionTitles) {
      expect(title.querySelector('strong.chat-md-label')).not.toBeNull()
      expect(title.querySelector('svg')).not.toBeNull()
    }
    expect(html).not.toContain('📌')
    expect(html).not.toContain('📋')
    expect(html).not.toContain('🎯')
  })

  it('无 Emoji 的规范化 ### 标题应按语义自动配对矢量图标与色调', () => {
    const md = [
      '概览说明如下：',
      '',
      '### 结论',
      '### 试卷结构',
      '### 注意事项',
      '### 核心亮点',
    ].join('\n')

    const html = renderChatMarkdown(md)
    const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html')
    const icons = Array.from(doc.querySelectorAll('h3 .chat-md-icon'))

    expect(icons).toHaveLength(4)
    expect(icons[0].classList.contains('chat-md-icon--primary')).toBe(true)
    expect(icons[1].classList.contains('chat-md-icon--primary')).toBe(true)
    expect(icons[2].classList.contains('chat-md-icon--warning')).toBe(true)
    expect(icons[3].classList.contains('chat-md-icon--amber')).toBe(true)
    expect(doc.querySelectorAll('h3 svg')).toHaveLength(4)
  })

  it('列表、提示段落中的状态 Emoji 应替换为内联矢量图标，且不破坏代码块与行内代码', () => {
    const md = [
      '状态检查结果：',
      '',
      '- ✅ 已通过校验',
      '- ❌ 参数不合法',
      '- ⚠️ 请注意边界情况',
      '',
      '```ts',
      'const pin = "📌";',
      '```',
      '',
      '行内代码 `🎯` 保留原样。',
    ].join('\n')

    const html = renderChatMarkdown(md)
    const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html')

    expect(doc.querySelectorAll('li .chat-md-icon--inline svg')).toHaveLength(3)
    expect(doc.querySelector('li .chat-md-icon--success')).not.toBeNull()
    expect(doc.querySelector('li .chat-md-icon--danger')).not.toBeNull()
    expect(doc.querySelector('li .chat-md-icon--warning')).not.toBeNull()

    // 代码块与行内代码内的 emoji 必须完整保留
    expect(doc.querySelector('pre code')?.textContent).toContain('📌')
    expect(doc.querySelector('code.chat-inline-code')?.textContent).toContain('🎯')
  })
})
