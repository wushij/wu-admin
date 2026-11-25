/**
 * AI 对话 Markdown 渲染（与 PC 端 chat-markdown.ts 同源，模仿 AgentOne）
 * markdown-it + highlight.js 代码高亮；html:false 杜绝 XSS 注入
 * 仅 H5 端使用（v-html），小程序端组件内降级为纯文本
 */
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/github-dark.css'
import katex from 'katex'
import 'katex/dist/katex.min.css'

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

const FENCED_CODE_BLOCK_RE = /(````[\s\S]*?````|```[\s\S]*?```)/g

/** 统一将 \[...\] 与 \(...\) 定界符转换为 $$...$$ 与 $...$ */
function normalizeLatexDelimiters(text: string): string {
  return text
    .replace(/\\\[([\s\S]+?)\\\]/g, (_m, inner: string) => `$$\n${inner.trim()}\n$$`)
    .replace(/\\\(([^()\n]+?)\\\)/g, (_m, inner: string) => `$${inner.trim()}$`)
}

function replaceLiteral(source: string, search: string, replacement: string): string {
  return source.split(search).join(replacement)
}

function renderKatexHtml(formula: string, displayMode: boolean): string {
  const normalized = formula.trim().replace(/，/g, ', ').replace(/；/g, '; ')
  return katex.renderToString(normalized, {
    displayMode,
    throwOnError: false,
    strict: 'ignore',
    errorColor: 'inherit',
  })
}

function extractMathPlaceholders(
  segment: string,
  blockKatexMap: Map<string, string>,
  inlineKatexMap: Map<string, string>,
  counters: { blockIdx: number; inlineIdx: number },
): string {
  let s = normalizeLatexDelimiters(segment)

  // 1. 先隔离行内代码 `...`，防止代码里的 $PATH / ${var} 与正文 $ 误配对
  const inlineCodeList: string[] = []
  s = s.replace(/`[^`\n]+`/g, (codeSpan) => {
    const key = `%%%MD_INLINE_CODE_${inlineCodeList.length}%%%`
    inlineCodeList.push(codeSpan)
    return key
  })

  // 2. 隔离常见货币价格（如 $100、$12.5 元），防止多个价格之间的 $ 跨文配对
  const currencyList: string[] = []
  s = s.replace(
    /(?<![$\\a-zA-Z0-9])\$(?:\d[\d,]*(?:\.\d+)?[ \t]*(?:[kKmMbBwW]|元|美元|万元|块|刀|美金|RMB|CNY|USD)|\d[\d,]*(?:\.\d+)?(?=[ \t]*(?:[\u4e00-\u9fa5]|[-~至到][ \t]*\$\d)|[，。；、！？：）)\]】」』]|(?:\r?\n|$)))(?!\$)/g,
    (currencySpan) => {
      const key = `%%%MD_CURRENCY_${currencyList.length}%%%`
      currencyList.push(currencySpan)
      return key
    },
  )

  // 3. 提取块级公式 $$ ... $$
  s = s.replace(/\$\$([\s\S]+?)\$\$/g, (_m, math: string) => {
    const key = `%%%KATEX_BLOCK_${counters.blockIdx++}%%%`
    blockKatexMap.set(key, `<div class="katex-block-wrapper">${renderKatexHtml(math, true)}</div>`)
    return key
  })

  // 4. 提取单行行内公式 $ ... $（排除跨句自然语言与含全角句末标点的文本）
  s = s.replace(/(?<!\$)\$(?!\$)([^$\n]+?)(?<!\$)\$(?!\$)/g, (match, math: string) => {
    const trimmed = math.trim()
    if (!trimmed || /[。！？]/.test(trimmed)) {
      return match
    }
    if (/[，；]/.test(trimmed) && !/[=<>+\-*/^_\\≤≥≠≈()]/.test(trimmed)) {
      return match
    }
    const key = `%%%KATEX_INLINE_${counters.inlineIdx++}%%%`
    inlineKatexMap.set(key, renderKatexHtml(trimmed, false))
    return key
  })

  currencyList.forEach((val, idx) => {
    s = replaceLiteral(s, `%%%MD_CURRENCY_${idx}%%%`, val)
  })
  inlineCodeList.forEach((val, idx) => {
    s = replaceLiteral(s, `%%%MD_INLINE_CODE_${idx}%%%`, val)
  })
  return s
}

export function renderMarkdown(content: string): string {
  if (!content.trim()) return ''
  const normalized = unwrapUrlInlineCode(normalizeMarkdown(content))

  const blockKatexMap = new Map<string, string>()
  const inlineKatexMap = new Map<string, string>()
  const counters = { blockIdx: 0, inlineIdx: 0 }

  // 仅对非围栏代码块部分解析 LaTeX 数学公式，避免误伤源码里的 $ 变量
  const processed = normalized
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment
      return extractMathPlaceholders(segment, blockKatexMap, inlineKatexMap, counters)
    })
    .join('')

  let html = md.render(processed)
  blockKatexMap.forEach((htmlVal, key) => {
    html = replaceLiteral(html, `<p>${key}</p>`, htmlVal)
    html = replaceLiteral(html, `<br>\n${key}`, htmlVal)
    html = replaceLiteral(html, `<br>${key}`, htmlVal)
    html = replaceLiteral(html, `${key}\n<br>`, htmlVal)
    html = replaceLiteral(html, `${key}<br>`, htmlVal)
    html = replaceLiteral(html, key, htmlVal)
  })
  inlineKatexMap.forEach((htmlVal, key) => {
    html = replaceLiteral(html, key, htmlVal)
  })
  return html
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
