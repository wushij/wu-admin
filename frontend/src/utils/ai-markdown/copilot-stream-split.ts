export interface CopilotStreamSplit {
  thinking: string;
  answer: string;
  inThinkingPhase: boolean;
}

const THINK_HEADERS = [
  '## 简要思考',
  '### 简要思考',
  '# 简要思考',
  '【简要思考】',
  '## 思考过程',
  '### 思考过程',
  '## 思考',
  '### 思考'
];

const ANSWER_HEADERS = [
  '## 回答',
  '### 回答',
  '# 回答',
  '【回答】',
  '## 答案',
  '### 答案',
  '## 解决方案',
  '## 代码实现'
];

export function splitCopilotStream(raw: string): CopilotStreamSplit {
  const text = raw || '';
  if (!text.trim()) {
    return { thinking: '', answer: '', inThinkingPhase: false };
  }

  let thinkStart = -1;
  let thinkHeaderLen = 0;
  for (const h of THINK_HEADERS) {
    const idx = text.indexOf(h);
    if (idx >= 0 && (thinkStart === -1 || idx < thinkStart)) {
      thinkStart = idx;
      thinkHeaderLen = h.length;
    }
  }

  if (thinkStart === -1) {
    let cleanAnswer = text;
    cleanAnswer = cleanAnswer.replace(/^#{1,3}\s*(?:回答|答案|解决方案)[:：]?\s*/i, '');
    return { thinking: '', answer: cleanAnswer, inThinkingPhase: false };
  }

  let answerStart = -1;
  let answerHeaderLen = 0;
  for (const h of ANSWER_HEADERS) {
    const idx = text.indexOf(h);
    if (idx >= 0 && (answerStart === -1 || idx < answerStart)) {
      answerStart = idx;
      answerHeaderLen = h.length;
    }
  }

  if (answerStart >= 0 && answerStart >= thinkStart) {
    const thinkingBlock = text.slice(thinkStart + thinkHeaderLen, answerStart).trim();
    const answerBlock = text.slice(answerStart + answerHeaderLen).trim();
    return { thinking: thinkingBlock, answer: answerBlock, inThinkingPhase: false };
  }

  const afterThink = text.slice(thinkStart + thinkHeaderLen);
  const bodyStart = findAnswerBodyStartInThinkTail(afterThink);
  if (bodyStart >= 0) {
    return {
      thinking: afterThink.slice(0, bodyStart).trim(),
      answer: afterThink.slice(bodyStart).trim(),
      inThinkingPhase: false
    };
  }

  return { thinking: afterThink.trim(), answer: '', inThinkingPhase: true };
}

/**
 * 思考段结束后正文常见起笔（勿用 ``` 切段，否则链路示意会把整篇回答包进代码块）。
 */
function findAnswerBodyStartInThinkTail(afterThink: string): number {
  const patterns = [
    /\n+## (?:回答|答案|解决方案)[:：]?\s*\n/i,
    /\n+### [\d.]+\s+[\u4e00-\u9fa5A-Za-z]/,
    /\n+## [一二三四五六七八九十百千]+、/,
    /\n+\*\*[^*\n]{2,40}：\*\*\s*\n/
  ];
  let best = -1;
  for (const re of patterns) {
    const m = re.exec(afterThink);
    if (!m || m.index < 0) continue;
    const leadingNewlines = m[0].match(/^(\n+)/)?.[0].length ?? 0;
    const idx = m.index + leadingNewlines;
    if (best === -1 || idx < best) {
      best = idx;
    }
  }
  return best;
}

export function cleanReasoningText(raw: string): string {
  if (!raw) return '';
  return raw.replace(/\n{3,}/g, '\n\n').trim();
}

/** 手动停止生成时展示在回答区的提示文案 */
export const STOPPED_GENERATION_MARKER = '*(已停止生成)*';

/**
 * 流式链路中断（网络 / 网关超时掐断连接 / 服务端提前结束流）导致回答未生成完整时的落盘标记。
 *
 * <p>与「已停止生成」共用一套本地草稿挽留机制：命中该标记的助手消息在
 * {@code mergeServerWithLocalDrafts} 中会被保留，但服务端已保存同轮回答时优先采用服务端版本，
 * 不会出现重复气泡。</p>
 */
export const INTERRUPTED_GENERATION_MARKER = '*(回答生成中断)*';

export function buildStoppedGenerationContent(answer: string): string {
  const trimmed = answer.trim();
  return trimmed ? `${trimmed}\n\n${STOPPED_GENERATION_MARKER}` : STOPPED_GENERATION_MARKER;
}

export function buildInterruptedGenerationContent(answer: string): string {
  const trimmed = answer.trim();
  return trimmed ? `${trimmed}\n\n${INTERRUPTED_GENERATION_MARKER}` : INTERRUPTED_GENERATION_MARKER;
}

/** 本地草稿是否为「未生成完整」的助手消息（手动停止 / 链路中断），用于刷新与切会话时挽留内容 */
export function isIncompleteAssistantContent(content?: string): boolean {
  if (!content) return false;
  return content.includes(STOPPED_GENERATION_MARKER) || content.includes(INTERRUPTED_GENERATION_MARKER);
}

export function formatInlineMarkdown(text: string): string {
  if (!text) return '';
  let s = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
  s = s.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  s = s.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');
  s = s.replace(/\n/g, '<br/>');
  return s;
}
