/**
 * Mermaid 临时 DOM 清理工具。
 *
 * 原先定义在 utils/markdown.ts 中。该函数只做「查询并移除 mermaid 渲染遗留的临时节点」，
 * 本身不依赖 markdown / mermaid / katex / highlight.js 等重型库；
 * 抽离到纯 DOM 工具层后，布局组件等只用到清理能力的调用方无需再静态引入整个 markdown 渲染管线，
 * 避免重型渲染库被首屏入口包连带打包。
 */

/** 清理 mermaid.render 遗留在 body 上的临时节点（会导致任意页面出现炸弹图标） */
export function cleanupOrphanMermaidDom(): void {
  if (typeof document === 'undefined') return;
  document.querySelectorAll<HTMLElement>('body > div[id^="dmermaid-"]').forEach((el) => el.remove());
  document.querySelectorAll<HTMLElement>('body > iframe[id^="imermaid-"]').forEach((el) => el.remove());
}
