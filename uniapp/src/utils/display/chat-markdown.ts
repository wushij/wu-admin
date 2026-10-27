/**
 * AI 对话 Markdown 渲染（与 PC 端 chat-markdown.ts 同源，模仿 AgentOne）
 * markdown-it + highlight.js 代码高亮；html:false 杜绝 XSS 注入
 * 仅 H5 端使用（v-html），小程序端组件内降级为纯文本
 */
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/github-dark.css'

function highlightCode(str: string, lang: string): string {
  if (lang && hljs.getLanguage(lang)) {
    try {
      return `<pre><code class="hljs language-${lang}">${hljs.highlight(str, { language: lang }).value}</code></pre>`
    } catch {
      /* fall through */
    }
  }
  const escaped = str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  return `<pre><code class="hljs">${escaped}</code></pre>`
}

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight: highlightCode,
})

// 修复 linkify-it 对中文括号（）及标点符号的过度匹配 bug
const CJK_AND_FULL_WIDTH_PUNCTUATION_RE =
  /[\u4e00-\u9fa5\u3002\uff0c\u3001\uff1b\uff1a\uff1f\uff01\u201c\u201d\u2018\u2019\u300c\u300d\u3010\u3011\uff08\uff09]+$/

if (md.linkify) {
  const originalMatch = md.linkify.match.bind(md.linkify)
  md.linkify.match = function (text: string) {
    const matches = originalMatch(text)
    if (!matches) return null
    for (const m of matches) {
      const match = m.raw.match(CJK_AND_FULL_WIDTH_PUNCTUATION_RE)
      if (match) {
        const trimLen = match[0].length
        m.raw = m.raw.slice(0, -trimLen)
        m.text = m.text.slice(0, -trimLen)
        m.url = m.url.slice(0, -trimLen)
        m.lastIndex -= trimLen
      }
    }
    return matches.filter((m) => m.raw.length > 0)
  }

  const originalMatchAtStart = md.linkify.matchAtStart.bind(md.linkify)
  md.linkify.matchAtStart = function (text: string) {
    const link = originalMatchAtStart(text)
    if (!link) return null
    const match = link.raw.match(CJK_AND_FULL_WIDTH_PUNCTUATION_RE)
    if (match) {
      const trimLen = match[0].length
      link.raw = link.raw.slice(0, -trimLen)
      link.text = link.text.slice(0, -trimLen)
      link.url = link.url.slice(0, -trimLen)
    }
    return link.raw.length > 0 ? link : null
  }
}

// 外链统一新标签页打开
const defaultLinkOpen =
  md.renderer.rules.link_open ||
  ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  tokens[idx].attrSet('target', '_blank')
  tokens[idx].attrSet('rel', 'noopener noreferrer')
  return defaultLinkOpen(tokens, idx, options, env, self)
}

// uni-app H5 对标签选择器会做平台改写，表格元素渲染时注入类名，样式用类选择器稳定命中
const TABLE_TOKEN_CLASS: Record<string, string> = {
  table_open: 'md-table',
  thead_open: 'md-thead',
  tbody_open: 'md-tbody',
  tr_open: 'md-tr',
  th_open: 'md-th',
  td_open: 'md-td',
}

for (const [tokenName, cls] of Object.entries(TABLE_TOKEN_CLASS)) {
  md.renderer.rules[tokenName] = (tokens, idx, options, _env, self) => {
    tokens[idx].attrJoin('class', cls)
    return self.renderToken(tokens, idx, options)
  }
}

const TRAILING_URL_PUNCT_RE = /[.,;:!?)]+$/

/** 模型常把 URL 包在反引号里，会渲染成 code 而非链接 */
function unwrapUrlInlineCode(content: string): string {
  return content.replace(/`((?:https?:\/\/)[^`\n]+)`/gi, (_, raw: string) => {
    const url = raw.trim().replace(TRAILING_URL_PUNCT_RE, '')
    if (!url) return `\`${raw}\``
    return `[${url}](${url})`
  })
}

export function renderMarkdown(content: string): string {
  if (!content.trim()) return ''
  return md.render(unwrapUrlInlineCode(normalizeMarkdown(content)))
}

// 中文全角标点（紧贴 ** 时会让 CommonMark 强调边界判定失败，导致 ** 原样显示）
const CJK_PUNCT = '，。、；：？！“”‘’「」『』（）《》【】…—～·'
const BOLD_CLOSE_FIX_RE = new RegExp(`([${CJK_PUNCT}])\\*\\*(?![\\s*])`, 'g')
const BOLD_OPEN_FIX_RE = new RegExp(`(^|[^\\s*])\\*\\*([${CJK_PUNCT}])`, 'gm')

/** 模型输出的常见不规范 Markdown 归一化（不碰围栏代码块内容） */
function normalizeSegment(seg: string): string {
  return (
    seg
      // 标题 # 后缺空格：###一、xxx → ### 一、xxx
      .replace(/^(#{1,6})([^#\s])/gm, '$1 $2')
      // 列表符 - 后缺空格（后接中文/emoji/加粗等）：-销售管理、-📌**协同** → - xxx；排除负数与 --- 分隔线
      .replace(/^(\s{0,3})-(?=[^\s\d\-*>]|\*\*)/gm, '$1- ')
      // 在紧贴全角标点的 ** 旁插入零宽空格，修复加粗边界判定
      .replace(BOLD_CLOSE_FIX_RE, '$1\u200b**')
      .replace(BOLD_OPEN_FIX_RE, '$1**\u200b$2')
  )
}

function normalizeMarkdown(content: string): string {
  // 按围栏切分，奇数段为代码块（含流式未闭合围栏）保持原样
  return content
    .split(/(```[\s\S]*?(?:```|$))/)
    .map((seg, i) => (i % 2 === 1 ? seg : normalizeSegment(seg)))
    .join('')
}
