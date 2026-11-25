/**
 * AI 对话 Markdown 渲染兼容转发层
 *
 * 核心渲染管线已升级为与 EduMind 侧边栏助手同源：
 * Markdown-It + KaTeX 数学公式 + Mermaid 流程图 + highlight.js 代码高亮 + DOMPurify 消毒。
 * 保留原 `renderMarkdown` 导出，避免既有调用方改动。
 */
import 'katex/dist/katex.min.css'
import 'highlight.js/styles/github-dark.css'

export {
  renderChatMarkdown,
  renderChatMarkdown as renderMarkdown,
  renderReasoningMarkdown,
  renderPlainChatMarkdown,
  bindMarkdownCodeCopy,
  renderMermaidInElement,
  normalizeCodeFences,
  cleanupOrphanMermaidDom
} from '@/utils/ai-markdown/chat-markdown'
