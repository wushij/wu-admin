/**
 * 联网检索参考来源摘要清洗工具
 *
 * 将搜索引擎返回的原始 Markdown、表格、HTML 实体与代码碎片整理为紧凑可读的纯文本摘要；
 * 当摘要内容属于不可读的 JSON / 表格符号碎片时，自动降级为空字符串以回退显示站点域名。
 */

const HTML_ENTITY_MAP: Record<string, string> = {
  '&gt;': '>',
  '&lt;': '<',
  '&amp;': '&',
  '&quot;': '"',
  '&#39;': "'",
  '&apos;': "'",
  '&nbsp;': ' ',
}

/** 从完整 URL 中提取可读主机名 */
export function extractCitationDomain(rawUrl: string): string {
  try {
    return new URL(rawUrl).hostname
  } catch {
    return rawUrl
  }
}

/** 解码常见 HTML 字符实体 */
function decodeBasicHtmlEntities(text: string): string {
  return text.replace(/&(gt|lt|amp|quot|#39|apos|nbsp);/gi, (match) => {
    return HTML_ENTITY_MAP[match.toLowerCase()] ?? match
  })
}

/** 判断清洗后的文本是否仍为无意义的 JSON、代码或表格符号碎片 */
function isLowQualityFragment(text: string): boolean {
  if (!text || text.length < 8) return true
  if (/^[}\]),;:{[\]|]+/.test(text)) return true
  const jsonKeyMatches = text.match(/"[A-Za-z0-9_]+"?\s*:/g)
  if (jsonKeyMatches && jsonKeyMatches.length >= 2) return true
  const symbolCount = (text.match(/[{}[\]|`~^\\]/g) || []).length
  if (symbolCount >= 4 && symbolCount / text.length > 0.08) return true
  return false
}

/**
 * 清洗参考来源摘要；若无有效自然语言摘要则返回空串，供卡片回退展示域名。
 */
export function formatCitationSnippet(rawSnippet?: string, maxChars = 160): string {
  if (!rawSnippet) return ''

  let text = decodeBasicHtmlEntities(rawSnippet)
    .replace(/\r\n/g, '\n')
    // 剔除 Markdown 图片与链接语法，保留可读文字
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    // 剔除代码块围栏与行内反引号
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]*)`/g, '$1')
    // 剔除标题井号、引用符与列表前缀
    .replace(/(^|\n)\s*#{1,6}\s*/g, ' ')
    .replace(/(^|\n)\s*>\s*/g, ' ')
    // 剔除 Markdown 表格分隔行（如 |---|---|）与连续竖线
    .replace(/\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)+\|?/g, ' ')
    .replace(/\|{2,}/g, ' ')
    .replace(/\s*\|\s*/g, ' · ')
    // 剔除加粗/斜体编号包裹符号（如 _1_、**文本**），避免误伤下划线变量名
    .replace(/\*\*([^*]+)\*\*/g, '$1')
    .replace(/(^|\s)_+(\d+)_+/g, '$1$2 ')
    .replace(/#{1,6}\s*/g, '')
    // 折叠多余空格、中间分隔符与换行
    .replace(/(?:\s*·\s*){2,}/g, ' · ')
    .replace(/^[\s·\-—]+|[\s·\-—]+$/g, '')
    .replace(/\s+/g, ' ')
    .trim()

  if (isLowQualityFragment(text)) {
    return ''
  }

  if (text.length > maxChars) {
    text = `${text.slice(0, maxChars).trimEnd()}…`
  }
  return text
}

/** 获取参考卡片第二行展示文本：优先返回清洗后的自然语言摘要，否则回退站点域名 */
export function resolveCitationPreviewText(rawSnippet: string | undefined, url: string): string {
  const cleaned = formatCitationSnippet(rawSnippet)
  return cleaned || extractCitationDomain(url)
}
