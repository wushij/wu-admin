/**
 * AI Markdown 小节与状态矢量图标映射器
 *
 * 将标题、列表及提示前缀中的 Emoji 替换为统一风格的线性矢量 SVG 图标，
 * 并为无 Emoji 的规范化小节标题按语义自动匹配前置图标，替代原始系统 Emoji。
 */

export type SectionIconName =
  | 'pin'
  | 'list'
  | 'target'
  | 'settings'
  | 'bulb'
  | 'alert'
  | 'check'
  | 'close'
  | 'search'
  | 'chart'
  | 'link'
  | 'clock'
  | 'code'
  | 'bookmark';

export type SectionIconTone = 'primary' | 'amber' | 'warning' | 'success' | 'danger';

interface IconDefinition {
  tone: SectionIconTone;
  /** 24x24 线性 SVG 内部几何元素 */
  paths: string;
}

const ICON_DEFINITIONS: Record<SectionIconName, IconDefinition> = {
  pin: {
    tone: 'primary',
    paths:
      '<path d="M12 17v5"/><path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V7a1 1 0 0 1 1-1 2 2 0 0 0 0-4H8a2 2 0 0 0 0 4 1 1 0 0 1 1 1z"/>',
  },
  list: {
    tone: 'primary',
    paths:
      '<rect x="8" y="2" width="8" height="4" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><path d="M12 11h4"/><path d="M12 16h4"/><path d="M8 11h.01"/><path d="M8 16h.01"/>',
  },
  target: {
    tone: 'primary',
    paths:
      '<circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/>',
  },
  settings: {
    tone: 'primary',
    paths:
      '<path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/>',
  },
  bulb: {
    tone: 'amber',
    paths:
      '<path d="M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5"/><path d="M9 18h6"/><path d="M10 22h4"/>',
  },
  alert: {
    tone: 'warning',
    paths:
      '<path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3"/><path d="M12 9v4"/><path d="M12 17h.01"/>',
  },
  check: {
    tone: 'success',
    paths:
      '<circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/>',
  },
  close: {
    tone: 'danger',
    paths:
      '<circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/>',
  },
  search: {
    tone: 'primary',
    paths:
      '<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
  },
  chart: {
    tone: 'primary',
    paths:
      '<path d="M3 3v18h18"/><path d="M18 17V9"/><path d="M13 17V5"/><path d="M8 17v-3"/>',
  },
  link: {
    tone: 'primary',
    paths:
      '<path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>',
  },
  clock: {
    tone: 'primary',
    paths:
      '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
  },
  code: {
    tone: 'primary',
    paths:
      '<polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/>',
  },
  bookmark: {
    tone: 'primary',
    paths:
      '<path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>',
  },
};

/** 常见 UI / 小节修饰 Emoji -> 语义矢量图标映射（去掉变体选择符 FE0F 统一索引） */
const EMOJI_TO_ICON: Record<string, SectionIconName> = {
  '📌': 'pin',
  '📍': 'pin',
  '📎': 'link',
  '📋': 'list',
  '📝': 'list',
  '📄': 'list',
  '📃': 'list',
  '📑': 'list',
  '🗂': 'list',
  '📚': 'list',
  '📖': 'list',
  '🎯': 'target',
  '🧭': 'target',
  '🚀': 'target',
  '🚩': 'target',
  '⚙': 'settings',
  '🔧': 'settings',
  '🛠': 'settings',
  '🔩': 'settings',
  '💡': 'bulb',
  '✨': 'bulb',
  '🌟': 'bulb',
  '⭐': 'bulb',
  '💬': 'bulb',
  '⚠': 'alert',
  '🚨': 'alert',
  '❗': 'alert',
  '‼': 'alert',
  '⁉': 'alert',
  '⚡': 'alert',
  '🔥': 'alert',
  '✅': 'check',
  '☑': 'check',
  '✔': 'check',
  '🎉': 'check',
  '👍': 'check',
  '❌': 'close',
  '❎': 'close',
  '🚫': 'close',
  '⛔': 'close',
  '🔍': 'search',
  '🔎': 'search',
  '🧐': 'search',
  '🩺': 'search',
  '📊': 'chart',
  '📈': 'chart',
  '📉': 'chart',
  '🧮': 'chart',
  '🔗': 'link',
  '🌐': 'link',
  '⏱': 'clock',
  '⏳': 'clock',
  '🕒': 'clock',
  '🕐': 'clock',
  '📅': 'clock',
  '🗓': 'clock',
  '💻': 'code',
  '🖥': 'code',
  '📦': 'code',
  '🧩': 'code',
  '🔖': 'bookmark',
  '🏷': 'bookmark',
};

/** 无 Emoji 的规范 Markdown 标题关键词 -> 自动匹配语义图标 */
const HEADING_KEYWORD_RULES: Array<{ pattern: RegExp; icon: SectionIconName }> = [
  { pattern: /(?:结论|总结|小结|摘要|核心观点|总览|综述|核心结论)/, icon: 'pin' },
  { pattern: /(?:结构|明细|清单|概览|列表|目录|字段|评分细则|评分标准|题型)/, icon: 'list' },
  { pattern: /(?:步骤|参考答案|标准答案|正确答案|解析要点|解析|解题|方案|实施|操作|流程|做法|指引)/, icon: 'target' },
  { pattern: /(?:配置|参数|设置|环境|权限|菜单|部署|安装)/, icon: 'settings' },
  { pattern: /(?:提示|建议|思路|优化|亮点|技巧|拓展|延伸|点拨|说明)/, icon: 'bulb' },
  { pattern: /(?:注意|警告|风险|硬伤|易错|避坑|缺陷|局限|异常|告警)/, icon: 'alert' },
  { pattern: /(?:优点|优势|通过|验收|合规|正确)/, icon: 'check' },
  { pattern: /(?:问题|排查|定位|分析|诊断|根因|原因|检查)/, icon: 'search' },
  { pattern: /(?:统计|数据|对比|分布|趋势|指标|分值)/, icon: 'chart' },
  { pattern: /(?:参考|来源|引用|链接|文档)/, icon: 'link' },
  { pattern: /(?:时长|时间|周期|排期|计划|历史|进度)/, icon: 'clock' },
  { pattern: /(?:代码|示例|接口|API|实现|源码)/i, icon: 'code' },
];

/** 匹配单个开头的 Emoji（含可选变体选择符 U+FE0F / 肤色修饰符 / ZWJ 序列） */
const LEADING_EMOJI_RE =
  /^\s*((?:[\p{Extended_Pictographic}\u{2600}-\u{27BF}])(?:\u{FE0F}|\u{200D}[\p{Extended_Pictographic}\u{2600}-\u{27BF}]\u{FE0F}?)*)+/u;

/** 文本节点中匹配任一已知 UI Emoji */
const KNOWN_UI_EMOJI_RE = (() => {
  const keys = Object.keys(EMOJI_TO_ICON)
    .sort((a, b) => b.length - a.length)
    .map((k) => `${k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\u{FE0F}?`);
  return new RegExp(`(${keys.join('|')})`, 'gu');
})();

function normalizeEmojiKey(rawEmoji: string): string {
  return rawEmoji.replace(/\uFE0F/g, '').trim();
}

function resolveIconForEmoji(rawEmoji: string): SectionIconName {
  const normalized = normalizeEmojiKey(rawEmoji);
  if (EMOJI_TO_ICON[normalized]) return EMOJI_TO_ICON[normalized];
  // 连续多个 emoji 时按第一个有效字符匹配
  for (const ch of Array.from(normalized)) {
    if (EMOJI_TO_ICON[ch]) return EMOJI_TO_ICON[ch];
  }
  return 'bookmark';
}

function resolveIconForHeadingText(text: string): SectionIconName | null {
  const trimmed = text.trim();
  if (!trimmed) return null;
  for (const rule of HEADING_KEYWORD_RULES) {
    if (rule.pattern.test(trimmed)) return rule.icon;
  }
  return null;
}

function createIconElement(
  doc: Document,
  iconName: SectionIconName,
  inline = false,
): HTMLSpanElement {
  const def = ICON_DEFINITIONS[iconName];
  const span = doc.createElement('span');
  span.className = inline
    ? `chat-md-icon chat-md-icon--inline chat-md-icon--${def.tone}`
    : `chat-md-icon chat-md-icon--${def.tone}`;
  span.setAttribute('aria-hidden', 'true');
  span.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${def.paths}</svg>`;
  return span;
}

/** 判断节点是否处于代码块、公式或图谱等不可篡改区域 */
function isProtectedAncestor(node: Node | null): boolean {
  let cur: Node | null = node;
  while (cur) {
    if (cur.nodeType === 1) {
      const el = cur as Element;
      const tag = el.tagName.toLowerCase();
      if (tag === 'pre' || tag === 'code' || tag === 'svg' || tag === 'style' || tag === 'script') {
        return true;
      }
      if (
        el.classList.contains('katex') ||
        el.classList.contains('katex-block-wrapper') ||
        el.classList.contains('mermaid-diagram-wrapper')
      ) {
        return true;
      }
    }
    cur = cur.parentNode;
  }
  return false;
}

/** 提取容器开头文本节点中的首部装饰 Emoji */
function extractLeadingEmojiFromElement(el: Element): string | null {
  const firstNode = el.firstChild;
  if (!firstNode || firstNode.nodeType !== 3) return null;
  const text = firstNode.textContent || '';
  const match = text.match(LEADING_EMOJI_RE);
  if (!match) return null;
  const rawEmoji = match[0].trim();
  const rest = text.slice(match[0].length).replace(/^[ \t]+/, '');
  if (rest) {
    firstNode.textContent = rest;
  } else {
    el.removeChild(firstNode);
  }
  return rawEmoji;
}

/** 清理紧跟在刚插入的图标后的多余前导空格，保持由 CSS gap 控制间距 */
function trimLeadingTextSpaceAfterIcon(iconEl: Element) {
  const next = iconEl.nextSibling;
  if (next && next.nodeType === 3 && next.textContent) {
    const trimmed = next.textContent.replace(/^[ \t]+/, '');
    if (trimmed) {
      next.textContent = trimmed;
    } else {
      iconEl.parentNode?.removeChild(next);
    }
  }
}

/**
 * 将容器内的结构化 Emoji 替换为 SVG 图标，并为高频小节标题补全语义图标。
 */
export function replaceSectionEmojisWithIcons(html: string): string {
  if (!html || typeof document === 'undefined') return html;

  const doc = new DOMParser().parseFromString(`<div id="md-icon-root">${html}</div>`, 'text/html');
  const root = doc.getElementById('md-icon-root');
  if (!root) return html;

  // 1. 处理所有标题 h1~h4：替换首部 Emoji，或对无 Emoji 高频小节自动配语义图标
  root.querySelectorAll('h1, h2, h3, h4').forEach((heading) => {
    if (isProtectedAncestor(heading)) return;

    const leadingEmoji = extractLeadingEmojiFromElement(heading);
    if (leadingEmoji) {
      const iconName = resolveIconForEmoji(leadingEmoji);
      const iconEl = createIconElement(doc, iconName, false);
      heading.insertBefore(iconEl, heading.firstChild);
      trimLeadingTextSpaceAfterIcon(iconEl);
      heading.classList.add('chat-md-heading-with-icon');
      return;
    }

    const tag = heading.tagName.toLowerCase();
    if (tag === 'h2' || tag === 'h3') {
      const iconName = resolveIconForHeadingText(heading.textContent || '');
      if (iconName) {
        const iconEl = createIconElement(doc, iconName, false);
        heading.insertBefore(iconEl, heading.firstChild);
        trimLeadingTextSpaceAfterIcon(iconEl);
        heading.classList.add('chat-md-heading-with-icon');
      }
    }
  });

  // 2. 处理独立段落中的「Emoji + 加粗标题」或行首引导 Emoji（如 <p>📌 <strong class="chat-md-label">结论</strong></p>）
  root.querySelectorAll('p, li, blockquote').forEach((block) => {
    if (isProtectedAncestor(block)) return;
    // 跳过里面只有块级子元素的 li/blockquote 外壳，交由内部 <p> 自己处理
    const firstEl = block.firstElementChild;
    if (
      (block.tagName.toLowerCase() === 'li' || block.tagName.toLowerCase() === 'blockquote') &&
      firstEl &&
      firstEl.tagName.toLowerCase() === 'p' &&
      block.firstChild === firstEl
    ) {
      return;
    }

    const leadingEmoji = extractLeadingEmojiFromElement(block);
    if (!leadingEmoji) return;

    const iconName = resolveIconForEmoji(leadingEmoji);
    // 检测是否为独立一行的「Emoji + 唯一 <strong> 小节标题」（例如「🎯 **参考答案与解析要点**」）
    // 也兼容 breaks: true 下「Emoji + <strong>标题</strong><br>正文」的形态，将标题提炼为独立小节头
    const firstStrong =
      block.firstElementChild?.tagName.toLowerCase() === 'strong' ? block.firstElementChild : null;
    const isStandaloneBoldSection =
      block.tagName.toLowerCase() === 'p' && Boolean(firstStrong) && block.childNodes.length === 1;
    const isBreakAfterBoldSection =
      block.tagName.toLowerCase() === 'p' &&
      Boolean(firstStrong) &&
      firstStrong?.nextSibling?.nodeType === 1 &&
      (firstStrong.nextSibling as Element).tagName.toLowerCase() === 'br';

    if (isStandaloneBoldSection && firstStrong) {
      block.classList.add('chat-md-section-title');
      firstStrong.classList.add('chat-md-label');
      const iconEl = createIconElement(doc, iconName, false);
      block.insertBefore(iconEl, block.firstChild);
      trimLeadingTextSpaceAfterIcon(iconEl);
      return;
    }

    if (isBreakAfterBoldSection && firstStrong) {
      const titleP = doc.createElement('p');
      titleP.className = 'chat-md-section-title';
      firstStrong.classList.add('chat-md-label');
      const iconEl = createIconElement(doc, iconName, false);
      titleP.appendChild(iconEl);
      titleP.appendChild(firstStrong);
      // 移除紧随其后的 <br> 及紧邻空白
      const brEl = block.firstChild;
      if (brEl && brEl.nodeType === 1 && (brEl as Element).tagName.toLowerCase() === 'br') {
        block.removeChild(brEl);
      }
      block.parentNode?.insertBefore(titleP, block);
      if (!block.textContent?.trim() && block.children.length === 0) {
        block.parentNode?.removeChild(block);
      }
      return;
    }

    const iconEl = createIconElement(doc, iconName, true);
    block.insertBefore(iconEl, block.firstChild);
    trimLeadingTextSpaceAfterIcon(iconEl);
  });

  // 3. 兜底替换正文中残留的常见 UI 状态/指示 Emoji（仅限于已知映射表，绝不触碰代码块/公式）
  const walker = doc.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  const textNodes: Text[] = [];
  let current = walker.nextNode();
  while (current) {
    if (
      current.nodeType === 3 &&
      current.textContent &&
      !isProtectedAncestor(current.parentNode) &&
      KNOWN_UI_EMOJI_RE.test(current.textContent)
    ) {
      textNodes.push(current as Text);
    }
    KNOWN_UI_EMOJI_RE.lastIndex = 0;
    current = walker.nextNode();
  }

  for (const textNode of textNodes) {
    const text = textNode.textContent || '';
    KNOWN_UI_EMOJI_RE.lastIndex = 0;
    const frag = doc.createDocumentFragment();
    let lastIdx = 0;
    let match: RegExpExecArray | null;
    while ((match = KNOWN_UI_EMOJI_RE.exec(text)) !== null) {
      if (match.index > lastIdx) {
        frag.appendChild(doc.createTextNode(text.slice(lastIdx, match.index)));
      }
      const iconName = resolveIconForEmoji(match[0]);
      frag.appendChild(createIconElement(doc, iconName, true));
      lastIdx = match.index + match[0].length;
    }
    if (lastIdx < text.length) {
      frag.appendChild(doc.createTextNode(text.slice(lastIdx)));
    }
    textNode.parentNode?.replaceChild(frag, textNode);
  }

  return root.innerHTML;
}
