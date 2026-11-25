import {
  renderMarkdownForChat,
  renderMarkdownForReasoning,
  normalizeCodeFences,
  looksLikeMermaid,
  isSafeMultilineMath
} from './markdown';
import { cleanReasoningText } from './copilot-stream-split';
import { expandAsciiTreeToMultiline, looksLikeAsciiKnowledgeTree } from './ascii-tree-graph';

const FENCED_CODE_BLOCK_RE = /(````[\s\S]*?````|```[\s\S]*?```)/g;

function looksLikeProseNotCode(inner: string, lang = ''): boolean {
  const t = inner.trim();
  if (!t) return false;

  // 若内部仍含有子级代码围栏（```），说明是嵌套示例，绝不可解包外层围栏
  if (/```/.test(t)) return false;

  // 1. ASCII 字符画、流程图、框线图保护：含有 +---+ 或框线字符者绝非散文
  if (/\+[-=]{3,}\+/.test(t) || /^[ \t]*\+[-=]+[ \t]*$/m.test(t)) return false;
  if (/^[ \t]*[┌├└│─┼]/m.test(t)) return false;

  const langLower = (lang || '').trim().toLowerCase();
  const isExplicitMarkdown = langLower === 'markdown' || langLower === 'md';
  const cn = (t.match(/[\u4e00-\u9fa5]/g) || []).length;
  const hasBoldList = /^\s*[-*]\s+\*\*[^*\n]+?\*\*/m.test(t);

  // 2. 显式标注为 markdown / md 的围栏：
  //    当具备正规讲义结构（大纲标题、加粗要点列表或数学公式）且含中文时解包；
  //    纯粹的短列表语法示例（如 `- 第一项\n- 第二项`，无标题无加粗无公式）坚决保留源码展示
  if (isExplicitMarkdown) {
    if (
      cn >= 6 &&
      (/^#{1,6}[ \t]*[\u4e00-\u9fa5\d]/m.test(t) ||
        hasBoldList ||
        /\$\$|\$[^$\n]+\$/.test(t))
    ) {
      return true;
    }
    return false;
  }

  // 非显式 markdown 围栏若含多行管道符表格结构，保留原样
  const linesWithPipes = t.split('\n').filter((l) => /^[ \t]*\|.*\|[ \t]*$/.test(l));
  if (linesWithPipes.length >= 2) return false;

  // 3. 针对 text / txt 或未标注语言的裸围栏：
  // 若出现明确处于语句起始位置的编程语法特征（关键字起行、独立花括号块、多行分号结尾），判定为真实代码，绝不解包。
  // 关键防护：只认“行首关键字”，不可用 \b...\b 全文搜词，否则中文讲义里提到 import / class 等术语就会被误判成代码而拒绝解包。
  if (
    /^[ \t]*(?:import|from|export|class|interface|enum|function|def|public|private|protected|return|const|let|var|package|#include)\b/m.test(
      t
    )
  ) {
    return false;
  }
  if (/^[ \t]*[{][\s\S]*[}][ \t]*$/m.test(t)) return false;
  // 只有多行语句均以半角分号收尾时才视为代码，避免讲义里偶尔一句半角分号被整段误判
  const semicolonTailLines = t
    .split('\n')
    .filter((line) => /;\s*$/.test(line) && !/&[a-zA-Z]+;\s*$/.test(line));
  if (semicolonTailLines.length >= 2) return false;

  // 3.1 代码 / 配置 / 日志行特征：命中任一即判定为真实代码片段，绝不解包。
  //   a) 行首为 ASCII 命令词，其后紧跟引号 / 路径 / 选项 / 变量起始（如 echo "你好"、cd /var/log、curl -X POST）：
  //      中文讲义正文一律以汉字起行，不可能命中这一形态，因此本条不受「行内含汉字」影响。
  if (/^[ \t]*[A-Za-z][\w.-]*[ \t]+["'/$.~-]/m.test(t)) return false;
  //   b) ASCII 键名 + 半角冒号/等号 + 非汉字取值（如 timeout: 30 秒、host: 127.0.0.1）：
  //      值是数字或符号开头，说明是配置/赋值，而非「Vue: 组件化框架」这种中文说明。
  if (/^[ \t]*[A-Za-z_][\w.-]*[ \t]*[:=][ \t]*[^\s:：\u4e00-\u9fa5]/m.test(t)) return false;
  //   c) 兜底判据：存在「既不是 Markdown 结构行、也不含汉字、也不是独立成行的公式、且不以英文句末标点收尾」
  //      的纯 ASCII 行 —— 必然是命令行 / 配置 / 日志 / 目录树，而不是中文讲义。
  //      例：tasklist、pwd、total = sum(x)、host: 127.0.0.1、src/、2024-01-01 INFO started、<div>hi</div>
  const isMarkdownStructureLine = (line: string) =>
    /^[ \t]*(?:#{1,6}[ \t]|[-*+][ \t]|\d+[.、)][ \t]|>|\||```|~~~|---+|===+)/.test(line);
  // 排除成对的多行/行内公式（$$...$$、\[...\] 与 \(...\)），防止含公式的中文讲义因中间纯 LaTeX 行被误判为代码
  const textWithoutDisplayMath = t.replace(/\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\\([\s\S]*?\\\)/g, '');
  const hasPlainAsciiLine = textWithoutDisplayMath.split('\n').some((line) => {
    const s = line.trim();
    if (!s) return false;
    if (/[\u4e00-\u9fa5]/.test(s)) return false;
    if (isMarkdownStructureLine(line)) return false;
    if (/^(?:\$[^$\n]*\$|\$\$|\\\[|\\\]|\\\(.*\\\))$/.test(s)) return false;
    if (/[.!?]$/.test(s)) return false;
    return true;
  });
  if (hasPlainAsciiLine) return false;

  // 4. 针对 text / txt 或未标注语言的裸围栏：
  //    若包含强讲义结构：正规中文章节大纲（如「# 一、要点」）+ 加粗要点列表（- **重点**），或包含独立数学公式块（$$），判定为误套外壳的讲义解包；
  //    普通的命令行/配置/安装说明（如「# 可用命令\n- 安装依赖\n- 启动服务」，列表无加粗）保留为代码块，绝不误解包为正文标题
  const hasFormalChapterHeading = /^#{1,6}\s+[一二三四五六七八九十\d]+[、. ]/m.test(t);
  if (hasFormalChapterHeading && (hasBoldList || /\$\$/.test(t))) {
    return true;
  }
  if (hasBoldList && cn >= 25) {
    return true;
  }

  return false;
}

/**
 * 模型常把整段 Markdown 讲义或多章节文档包在 ``` / ```markdown / ````markdown / `````markdown 里；
 * 同时保留真正用于展示语法的短小 ```markdown 示例块（如仅含单个短标题与内嵌代码块的示例）。
 */
export function unwrapProseCodeFences(text: string): string {
  if (!text || !text.includes('```')) return text;
  let current = text;

  for (let pass = 0; pass < 3; pass++) {
    const lines = current.split(/\r?\n/);
    const out: string[] = [];
    let changed = false;
    let i = 0;

    while (i < lines.length) {
      const openMatch = lines[i].match(/^[ \t]*(`{3,})([a-zA-Z0-9_#+-]*)[ \t]*$/);
      if (!openMatch) {
        out.push(lines[i]);
        i++;
        continue;
      }

      const openTicks = openMatch[1];
      const lang = (openMatch[2] || '').trim().toLowerCase();
      const isMarkdownFence = lang === 'markdown' || lang === 'md';
      const isProseCandidate = !lang || isMarkdownFence || lang === 'text' || lang === 'txt';

      let innerDepth = 0;
      let closeIdx = -1;
      for (let j = i + 1; j < lines.length; j++) {
        const line = lines[j];
        if (line.trim().startsWith('|') && line.trim().endsWith('|')) {
          continue;
        }
        const bareClose = line.match(/^[ \t]*(`{3,})[ \t]*$/);
        const innerOpen = !bareClose ? line.match(/^[ \t]*(`{3,})([^\s`].*)$/) : null;

        if (openTicks.length >= 4) {
          if (bareClose && bareClose[1].length >= openTicks.length) {
            closeIdx = j;
            break;
          }
        } else if (isMarkdownFence) {
          if (innerOpen && innerOpen[1].length === 3) {
            innerDepth++;
            continue;
          }
          if (bareClose && bareClose[1].length === 3) {
            if (innerDepth > 0) {
              innerDepth--;
              continue;
            }
            closeIdx = j;
            break;
          }
        } else {
          if (bareClose && bareClose[1].length >= openTicks.length) {
            closeIdx = j;
            break;
          }
        }
      }

      if (closeIdx === -1) {
        out.push(lines[i]);
        i++;
        continue;
      }

      const inner = lines.slice(i + 1, closeIdx).join('\n');
      if (isProseCandidate) {
        const hasInnerFence = /```/.test(inner);
        if (hasInnerFence && isMarkdownFence) {
          const innerOutsideSubFences = inner.replace(/`{3,}[a-zA-Z0-9_#+-]*[^\n]*\n[\s\S]*?\n[ \t]*`{3,}/g, '');
          const cnOutside = (innerOutsideSubFences.match(/[\u4e00-\u9fa5]/g) || []).length;
          const headingMatches = innerOutsideSubFences.match(/^[ \t]*#{1,6}[ \t]*\S/gm) || [];
          if (headingMatches.length >= 2 || cnOutside >= 20) {
            out.push(inner.trim());
            changed = true;
            i = closeIdx + 1;
            continue;
          }
        } else if (!hasInnerFence) {
          const shouldUnwrap =
            !lang
              ? /^#{1,6}\s/m.test(inner.trim()) && looksLikeProseNotCode(inner, lang)
              : looksLikeProseNotCode(inner, lang);
          if (shouldUnwrap) {
            out.push(inner.trim());
            changed = true;
            i = closeIdx + 1;
            continue;
          }
        }
      }

      for (let k = i; k <= closeIdx; k++) {
        out.push(lines[k]);
      }
      i = closeIdx + 1;
    }

    current = out.join('\n');
    if (!changed) break;
  }

  return current;
}

/**
 * 大模型常把「1.知识地图…2.概念辨析…3.定理…」挤在同一行；
 * Markdown 无法识别，需拆行并在序号后补空格。
 * 必须严格保护带加粗的序号（如「**1. 异常体系**」），防止换行符插入 ** 与数字之间导致加粗断裂。
 */
function isMarkdownTableRowLine(line: string): boolean {
  const trimmed = line.trim();
  return trimmed.startsWith('|') && (trimmed.endsWith('|') || trimmed.includes('|', 1));
}

function hasUnclosedBrackets(text: string): boolean {
  let round = 0;
  let square = 0;
  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (ch === '(' || ch === '（') round++;
    else if ((ch === ')' || ch === '）') && round > 0) round--;
    else if (ch === '[' || ch === '【') square++;
    else if ((ch === ']' || ch === '】') && square > 0) square--;
  }
  return round > 0 || square > 0;
}

/**
 * =========================================================================================
 * 【防回归警示：数学公式预处理占位保护】
 * 作用：
 *   保护各类标准数学公式（$$...$$、\[...\]、\(...\)、$...$ 以及通过 isSafeMultilineMath 校验的跨行等式链），
 *   防止公式内部的减号（如 (1+x)-x）、换行、等号或小数点被后续的自然语言列表拆行器与段落规范器误切碎！
 * 【注意】：
 *   新增或修改匹配逻辑时，必须保证跨多行的货币金额（如 $10 ... \n ... $20）不被误伤，
 *   改动后必须运行：npm test -- src/utils/ai/chat-markdown.spec.ts
 * =========================================================================================
 */
function protectMathBlocks(text: string): { masked: string; unmask: (s: string) => string } {
  const placeholders: string[] = [];
  // 0. 先保护表格内嵌代码块与行内代码 `...`，防止代码变量内部的 $ 与后文 $ 错误跨边界配对
  let masked = text.replace(/<div class="chat-table-code-block">[\s\S]*?<\/pre><\/div>/g, (m) => {
    const key = `%%%CHAT_MD_MATH_${placeholders.length}%%%`;
    placeholders.push(m);
    return key;
  });
  masked = masked.replace(/`[^`\n]+`/g, (m) => {
    const key = `%%%CHAT_MD_MATH_${placeholders.length}%%%`;
    placeholders.push(m);
    return key;
  });
  // 1. 先保护标准块级、圆括号公式及单行单 $ 公式
  masked = masked.replace(
    /(\$\$[\s\S]+?\$\$|\\\[[\s\S]+?\\\]|\\\(.+?\\\)|\$(?!\$)[^$\n]+?\$(?!\$))/g,
    (m) => {
      const key = `%%%CHAT_MD_MATH_${placeholders.length}%%%`;
      placeholders.push(m);
      return key;
    }
  );
  // 2. 保护满足安全特征校验的跨行单 $ 公式
  masked = masked.replace(/(?<!\$)\$(?!\$)([\s\S]+?)(?<!\$)\$(?!\$)/g, (match, formula: string) => {
    if (isSafeMultilineMath(formula)) {
      const key = `%%%CHAT_MD_MATH_${placeholders.length}%%%`;
      placeholders.push(match);
      return key;
    }
    return match;
  });

  return {
    masked,
    unmask: (s: string) =>
      s.replace(/%%%CHAT_MD_MATH_(\d+)%%%/g, (_m, idx) => placeholders[Number(idx)] ?? _m)
  };
}

/**
 * 修复模型在行内公式中漏写闭合 $ 符号便直接开始写中文标点或说明文字的问题
 * 例如：「求 $\displaystyle \lim_{x\to0}\frac{...}{...}。提示：...」
 * 在中文标点（。；！？）之前自动补齐闭合 $，并修复紧跟中文说明后的孤立结尾 $
 */
function repairUnclosedMathBeforeChinesePunct(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      // 先保护行内代码 `...`、合法的 $$...$$ 独立数学块与已闭合的 $...$ 行内公式，绝不在完整公式或行内代码内部插 $
      const protectedMathList: string[] = [];
      const maskMath = (m: string) => {
        const key = `%%%BLOCK_MATH_UNCLOSED_${protectedMathList.length}%%%`;
        protectedMathList.push(m);
        return key;
      };

      let s = segment.replace(/<div class="chat-table-code-block">[\s\S]*?<\/pre><\/div>/g, maskMath);
      s = s.replace(/`[^`\n]+`/g, maskMath);
      s = s.replace(/\$\$[\s\S]+?\$\$/g, maskMath);
      s = s.replace(/(?<!\$)\$(?!\$)[^$\n]+?(?<!\$)\$(?!\$)/g, maskMath);
      s = s.replace(/(?<!\$)\$(?!\$)([\s\S]+?)(?<!\$)\$(?!\$)/g, (match, f: string) => {
        if (isSafeMultilineMath(f)) return maskMath(match);
        return match;
      });

      // 1. 匹配 $ 开头且含有 LaTeX 指令（如 \lim, \frac 等），但在遇到中文句末标点时未闭合 $ 的情况
      s = s.replace(
        /(?<![\$\\a-zA-Z0-9])(\$(?!\$)(?:[^\$\n]*?\\[a-zA-Z]+[^\$\n]*?))([。；;!?！？])/g,
        (match, mathPart: string, punc: string) => {
          const dollars = (mathPart.match(/(?<!\\)\$/g) || []).length;
          if (dollars % 2 === 0) return match;
          return `${mathPart}$${punc}`;
        }
      );

      // 2. 补齐紧随中文说明（如「结果为」）后的孤立闭合 $ 对应的开启 $（排除 $$）
      s = s.replace(
        /([\u4e00-\u9fa5][：:]?)[ \t]*([+\\-]?\\(?:frac|sqrt|[a-zA-Z]+)[^$\n]*?)(?<!\$)\$(?!\$)/g,
        (_match, prefix, expr) => `${prefix} $${expr.trim()}$`
      );

      // 还原 $$...$$ 与 $...$
      protectedMathList.forEach((m, idx) => {
        s = s.replaceAll(`%%%BLOCK_MATH_UNCLOSED_${idx}%%%`, () => m);
      });

      return s;
    })
    .join('');
}

/**
 * 大模型常把「1.知识地图…2.概念辨析…3.定理…」挤在同一行；
 * Markdown 无法识别，需拆行并在序号后补空格。
 * 必须严格保护表格行、标题行、未闭合括号及带加粗的序号，防止截断正文。
 */
function normalizeInlineNumberedLists(text: string): string {
  const { masked, unmask } = protectMathBlocks(text);
  const result = masked
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const processed = lines.map((line) => {
        // 表格行与标题行必须完整保护
        if (isMarkdownTableRowLine(line)) return line;
        if (/^#{1,6}\s/.test(line.trim())) return line;

        let s = line;

        // 1. 若同一行前文粘连了带加粗的序号小标题（不在未闭合括号内）
        s = s.replace(
          /([\p{Extended_Pictographic}\u4e00-\u9fa5a-zA-Z0-9。；;!?！？:：）)”"』」】])[ \t]*\*\*[ \t]*(\d{1,2})\.[ \t]*/gu,
          (match, char, num, offset, fullStr) => {
            const before = fullStr.slice(0, offset + char.length);
            if (hasUnclosedBrackets(before)) return match;
            const asterisks = (before.match(/\*\*/g) || []).length;
            if (asterisks % 2 === 1) {
              // 此处的 ** 实际上是前面加粗标签（如 **连续三要素：**1.）的闭合标记，绝不可吞掉闭合 **
              return `${char}**\n\n${num}. `;
            }
            return `${char}\n\n**${num}. `;
          }
        );

        // 2. 确保已在行首的加粗序号内部有标准空格（如「**1.知识体系**」->「**1. 知识体系**」），绝不拆开 ** 与数字
        s = s.replace(
          /(^|\n)([ \t]*)\*\*(\d{1,2})\.[ \t]*(?=[\p{Extended_Pictographic}\u4e00-\u9fa5（(「『【A-Za-z$\\])/gu,
          '$1$2**$3. '
        );

        // 3. 普通无加粗序号拆行：仅在标点符号、中文后粘连且不在未闭合括号内时拆行（紧凑单换行，防止生成松散列表多余段落）
        s = s.replace(
          /([\p{Extended_Pictographic}\u4e00-\u9fa5。；;!?！？:：）)”"』」】])[ \t]*(\d{1,2})\.[ \t]*(?=[\p{Extended_Pictographic}\u4e00-\u9fa5（(「『【A-Za-z$\\]|\*\*)/gu,
          (match, char, num, offset, fullStr) => {
            const before = fullStr.slice(0, offset + char.length);
            if (hasUnclosedBrackets(before)) return match;
            return `${char}\n${num}. `;
          }
        );

        // 4. 行首普通序号后补齐标准空格（支持公式 $ 与 \ 前缀）；吸收已有空白，保证二次归一化不再叠加空格
        s = s.replace(
          /^([ \t]*)(\d{1,2})\.[ \t]*(?=[\p{Extended_Pictographic}\u4e00-\u9fa5（(「『【A-Za-z$\\])/gmu,
          '$1$2. '
        );

        return s;
      });

      return processed.join('\n');
    })
    .join('');

  return unmask(result);
}

/**
 * 同一行粘连的「-两个重要极限 … -连续判定 …」拆成 Markdown 无序列表。
 * 注意：必须严格排除副标题连接符（如「题目一（数列极限 - 四则运算）」）、人名间隔号（如「约翰·冯·诺依曼」）、
 * 标题行（# 开头）以及未闭合括号内部的内容，防止错误拆行断裂语义。
 */
function normalizeInlineBulletLists(text: string): string {
  const { masked, unmask } = protectMathBlocks(text);
  const result = masked
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const processed = lines.map((line) => {
        // 表格行与 Markdown 标题行绝不可在行内拆出列表
        if (isMarkdownTableRowLine(line)) return line;
        if (/^#{1,6}\s/.test(line.trim())) return line;

        let s = line;

        // 1. 将行首（包含缩进）的实心圆点 • 或 · 转换为标准 Markdown 无序列表项 -
        s = s.replace(/^([ \t]*)[•·]\s*/gmu, '$1- ');

        // 1b. 将行首（包含缩进）的字符树分支符号转换为标准 Markdown 无序列表项 -（如「│├──三、...」或「├──三、...」）
        s = s.replace(/^([ \t]*)[│|]*[├└\+][─\-—\s]*/gmu, '$1- ');

        // 2. 行内实心圆点 • 转换为无序列表项（仅限明确的 bullet •，不包含中文人名/书名间隔号 ·，且不在未闭合括号内）
        s = s.replace(/([^\n])\s*•\s*(?=[\p{Extended_Pictographic}\*\*\u4e00-\u9fa5【「『（])/gu, (match, prefix, offset, fullStr) => {
          const before = fullStr.slice(0, offset + prefix.length);
          if (hasUnclosedBrackets(before)) return match;
          return `${prefix}\n- `;
        });

        // 2b. 行内粘连的字符树分支符号（如「2.4用 javap验证编译产物│├──三、.class文件」或「...├──三、...」）拆分为独立无序列表项
        s = s.replace(
          /([^\n\r\t -])[ \t]*(?:[│|]+[├└\+][─\-—\s]*|[├└\+][─\-—\s]+)(?=[一二三四五六七八九十百千]+[、．.]|\d+\.\d+|[A-Za-z0-9_.]+\s*：)/gu,
          (match, prevChar, offset, fullStr) => {
            const before = fullStr.slice(0, offset + prevChar.length);
            if (hasUnclosedBrackets(before)) return match;
            return `${prevChar}\n- `;
          }
        );

        // 3. 明确的列表引导标点（冒号、分号、句号、感叹号、问号、已闭合引号、中文括号、已闭合加粗、已闭合数学公式）之后紧贴的 -项
        // 严格排除英文半角括号 )（在数学公式与英文表达式中 ) - x 属于减法或复合词，绝非列表）
        // 且仅对中文、加粗、数学公式及 Emoji 放宽空格限制；英文单词/字母后必须有空格（防止误伤 -x 等变量）
        s = s.replace(
          /(?<!-)([。；;!?！？:：）”"』」】]|\*\*|%%%CHAT_MD_MATH_\d+%%%)[ \t]*-(?:[ \t]*(?=\*\*|[\p{Extended_Pictographic}\u4e00-\u9fa5【「『（])|[ \t]+(?=[A-Za-z]))/gu,
          (match, punc, offset, fullStr) => {
            const before = fullStr.slice(0, offset + punc.length);
            if (hasUnclosedBrackets(before)) return match;
            if (punc === '**' || punc.startsWith('%%%CHAT_MD_MATH_')) {
              const asterisks = (before.match(/\*\*/g) || []).length;
              if (punc === '**' && asterisks % 2 !== 0) return match; // 必须是偶数，说明是已闭合的加粗标签
              return `${punc}\n\n- `;
            }
            return `${punc}\n- `;
          }
        );

        // 4. 若当前行（或拆行后的列表行）以无序列表项开头（- 开头），则该行内后续粘连的 -列表项也允许拆行（不在未闭合括号内）
        s = s
          .split('\n')
          .map((subLine) => {
            if (/^[ \t]*-[ \t]*/.test(subLine)) {
              return subLine.replace(
                /(?<!-)([^\n\r\t -])[ \t]*-(?:[ \t]*(?=\*\*|[\p{Extended_Pictographic}\u4e00-\u9fff（(「『【])|[ \t]+(?=[A-Za-z]))/gu,
                (match, prevChar, offset, fullStr) => {
                  const before = fullStr.slice(0, offset + prevChar.length);
                  if (hasUnclosedBrackets(before)) return match;
                  // 保护常见的复合词或代码标识符，如 key-value、foo-bar
                  const afterHyphen = fullStr.slice(offset + match.length);
                  if (/[A-Za-z0-9]/.test(prevChar) && /^[A-Za-z0-9]/.test(afterHyphen)) return match;
                  return `${prevChar}\n- `;
                }
              );
            }
            return subLine;
          })
          .join('\n');

        // 4b. 若该行原本未以 - 开头，但在该行内拆出了连续的 - 列表项，且首段也是形如「xxx：yyy。」的并列结构（非冒号结尾的引导句），补全首项列表标记
        const splitSubLines = s.split('\n');
        if (
          splitSubLines.length > 1 &&
          !/^[ \t]*[-*•·]/.test(splitSubLines[0]) &&
          !/[:：]\s*$/.test(splitSubLines[0]) &&
          splitSubLines.slice(1).every((sub) => /^[ \t]*- /.test(sub)) &&
          /[:：]/.test(splitSubLines[0])
        ) {
          splitSubLines[0] = `- ${splitSubLines[0].trimStart()}`;
          s = splitSubLines.join('\n');
        }

        // 5. CommonMark 规范化：确保行首（允许带缩进空格）- 或有序列表数字. 后面有空格，支持 Emoji、中英文、粗体
        s = s.replace(/^([ \t]*)-(?=[\p{Extended_Pictographic}\u4e00-\u9fa5*（(「『【A-Za-z0-9])/gmu, '$1- ');
        s = s.replace(/^([ \t]*\d+\.)(?=[\p{Extended_Pictographic}\u4e00-\u9fa5*（(「『【A-Za-z])/gmu, '$1 ');

        // 5b. 剔除列表项末尾残留的 ASCII 树竖线字符（如「- ①语法与语法分析 -> AST|」末尾的 | 或 │）
        s = s
          .split('\n')
          .map((subLine) => {
            if (/^[ \t]*-[ \t]+/.test(subLine)) {
              return subLine.replace(/[\s│|]+$/gu, '');
            }
            return subLine;
          })
          .join('\n');

        // 6. 列表项末尾粘连的交流互动引导句自动脱离列表形成独立自然段
        s = s.replace(
          /([\u4e00-\u9fa5。；;!?！？）)”"』」】])\s*(不妨(?:先聊|告诉|直接|说说)|那么[，, ]?我们|请问(?:你|您)|你现在(?:是|想)|你是(?:刚|想))/gu,
          '$1\n\n$2'
        );

        return s;
      });

      return processed.join('\n');
    })
    .join('');

  return unmask(result);
}

/**
 * 修复排版断行造成的行首悬挂冒号（如「• 结点的度\n: 该结点拥有的子树个数。」或「候选键\n: 能唯一标识...」）
 * 将行首孤立的冒号吸收并合并至上一行概念词条末尾，彻底避免 breaks: true 模式下冒号突兀换行的排版割裂。
 */
export function normalizeOrphanLeadingColons(text: string): string {
  if (!text || (!text.includes(':') && !text.includes('：'))) return text;

  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const out: string[] = [];

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const trimmed = line.trim();
        // 匹配行首孤立冒号：形如「: 正文...」或「：正文...」
        const match = trimmed.match(/^([:：])\s*(.*)$/);
        if (match) {
          const body = match[2];
          // 向前跳过中间空行（如「**候选键**\n\n: 能唯一标识...」），定位上一行实质内容
          let prevIdx = out.length - 1;
          while (prevIdx >= 0 && !out[prevIdx].trim()) prevIdx--;
          const hasBlankGap = prevIdx >= 0 && prevIdx < out.length - 1;
          const prevLine = prevIdx >= 0 ? out[prevIdx] : '';
          const prevTrimmed = prevLine.trim();
          // 跨空行合并时仅限「短词条 / 以加粗收尾」的悬挂冒号，避免把完整段落误并
          const gapAllowed =
            !hasBlankGap ||
            (/\*\*$/.test(prevTrimmed) || prevTrimmed.replace(/[*_`#>\-\s]/g, '').length <= 20);

          // 关键防护：上一行必须是实质内容，且不能是表格行、横线分割线、块引用或标题标记
          if (
            out.length > 0 &&
            prevTrimmed &&
            gapAllowed &&
            !isMarkdownTableRowLine(prevLine) &&
            !/^[-*_—=]{3,}$/.test(prevTrimmed) &&
            !prevTrimmed.startsWith('>') &&
            !(hasBlankGap && /^#{1,6}\s/.test(prevTrimmed))
          ) {
            // 吞掉词条与冒号之间的空行
            out.length = prevIdx + 1;
            // 若上一行末尾已经带有冒号，则不重复补充冒号，直接连接正文
            if (/[:：]$/.test(prevTrimmed)) {
              out[prevIdx] = body ? `${prevLine} ${body}`.trimEnd() : prevLine;
            } else {
              // 否则将冒号合并吸附至上一行末尾，中文词条后统一使用全角中文冒号「：」呈现最佳排版
              out[prevIdx] = body ? `${prevLine}：${body}`.trimEnd() : `${prevLine}：`;
            }
            continue;
          }

          // 无法吸附（切片首行，或上一行是标题、表格、分割线）时，这个冒号是解析/切分残留的
          // 悬挂碎片：直接丢弃冒号保留正文，避免正文以「：」孤立开头。整行只有冒号时直接跳过。
          if (body) {
            const leading = line.match(/^\s*/)?.[0] ?? '';
            out.push(`${leading}${body}`);
          }
          continue;
        }
        out.push(line);
      }

      return out.join('\n');
    })
    .join('');
}

/**
 * 修复「列表项词条」与「其定义」被排版断成两行的问题。
 *
 * <p>文档解析/PDF 抽取经常把「- **实体**\n客观存在并可相互区分的事物…」拆成两行，
 * Markdown 会把第二行当成列表项内的软换行（渲染成 <br>），词条与定义视觉上被拆开。
 * 只在两种明确语义下合并：列表项以冒号收尾，或列表项本身就是加粗短词条；
 * 且下一行必须顶格正文（有缩进的是合法列表续行，绝不能吞）。</p>
 */
function normalizeListItemDefinitions(text: string): string {
  if (!text) return text;

  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const out: string[] = [];

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const match = line.match(/^([ \t]*)([-*+]|\d{1,2}[.、])[ \t]+(.+)$/);
        if (!match) {
          out.push(line);
          continue;
        }

        const [, indent, marker, content] = match;
        const trimmedContent = content.trim();
        const endsWithColon = /[:：]$/.test(trimmedContent);
        const isBoldTerm = /^\*\*[^*\n]{1,18}\*\*$/.test(trimmedContent);
        const next = lines[i + 1];

        if ((!endsWithColon && !isBoldTerm) || next == null) {
          out.push(line);
          continue;
        }

        const nextTrimmed = next.trim();
        const nextIsTopLevelBody =
          nextTrimmed.length > 0 &&
          !/^[ \t]/.test(next) &&
          !/^([-*+]|\d{1,2}[.、])[ \t]+/.test(nextTrimmed) &&
          !nextTrimmed.startsWith('#') &&
          !nextTrimmed.startsWith('>') &&
          !nextTrimmed.startsWith('|') &&
          !nextTrimmed.startsWith('```') &&
          !/^(\$\$|\\\[|\\\()/.test(nextTrimmed) &&
          !/^[-*_—=]{3,}$/.test(nextTrimmed);

        if (!nextIsTopLevelBody) {
          out.push(line);
          continue;
        }

        // 词条与定义合并为同一列表项；词条没有冒号时按中文排版习惯补全角冒号
        const merged = endsWithColon
          ? `${trimmedContent}${nextTrimmed}`
          : `${trimmedContent}：${nextTrimmed}`;
        out.push(`${indent}${marker} ${merged}`);
        i += 1;
      }

      return out.join('\n');
    })
    .join('');
}

/** 聊天 / 课节预览共用：拆粘连列表并规范行首 - 与 •（与深度思考区同源） */
export function normalizeDisplayListMarkdown(text: string): string {
  if (!text?.trim()) return text || '';
  let out = normalizeOrphanLeadingColons(text);
  out = normalizeInlineNumberedLists(out);
  out = normalizeInlineBulletLists(out);
  return out;
}

/** 代码块内的字符画知识树：拆行便于阅读与 Mermaid 转换 */
function normalizeAsciiTreeInCodeFences(text: string): string {
  return text.replace(/```[^\n]*\n([\s\S]*?)```/g, (full, body: string) => {
    if (!looksLikeAsciiKnowledgeTree(body)) return full;
    const expanded = expandAsciiTreeToMultiline(body);
    return full.replace(body, expanded);
  });
}

/** 围栏未声明 mermaid 但内容是流程图时，强制 ```mermaid（与侧边栏 AI 同源渲染） */
function normalizeMermaidInCodeFences(text: string): string {
  return text.replace(/```([^\n]*)\n([\s\S]*?)```/g, (full, langLine: string, body: string) => {
    const trimmedBody = body.trim();
    if (!looksLikeMermaid(trimmedBody)) return full;
    const lang = (langLine.trim().split(/\s+/)[0] || '').toLowerCase();
    if (lang === 'mermaid' || lang === 'graph' || lang === 'flowchart') return full;
    return `\`\`\`mermaid\n${trimmedBody}\n\`\`\``;
  });
}

/** 规范化代码块内部的板书画框（如 ┌──┐/├──┤/└──┘）与副板书多区域排版，展开粘连的行 */
function normalizeBlackboardLayoutInFences(text: string): string {
  return text.replace(
    /(```[^\n]*\n)([\s\S]*?)(```|$)/g,
    (_full, openFence: string, body: string, closeFence: string) => {
      let s = body;

      // 1. ASCII 框线板书（如 ┌───┐│1.4...│├───┤│一、...│└───┘）拆行
      if (/[┌├└─\-—]+[┐┤┘]/.test(s) || (/[┌├└]/.test(s) && /[│|]/.test(s))) {
        // a. 顶框 ┌───┐ 后面紧贴 │ 内容行拆行
        s = s.replace(/([┌─\-—]+[┐])[ \t]*(?=[│|])/g, '$1\n');
        // b. 中间分隔线 ├───┤ 后面紧贴 │ 内容行拆行
        s = s.replace(/([├─\-—]+[┤])[ \t]*(?=[│|])/g, '$1\n');
        // c. 内容行末尾 │ 后面紧贴分隔线 ├───┤ 或底框 └───┘ 或下一行 │ 拆行
        s = s.replace(/([│|][ \t]*)(?=[┌├└│|])/g, '$1\n');
        // d. 底框 └───┘ 与闭合围栏之间补换行
        s = s.replace(/([└─\-—]+[┘])[ \t]*(?=\n|```|$)/g, '$1\n');
      }

      // 2. 副板书多区域布局（如【演算区】【错例对比区】【代码时序区】）拆行与排版优化
      if (/【(?:演算区|错例对比区|代码时序区|草稿区|板书|板演区|代码区)】/.test(s)) {
        // a. 【代码时序区】等新区域若紧贴上一部分内容（如公式末尾），加双换行分隔
        s = s.replace(/([^\n\r])[ \t]*(【(?:代码时序区|草稿区|板演区|代码区)】)/g, '$1\n\n$2\n');
        // b. Python栈演示：后若紧贴代码，换行
        s = s.replace(/((?:Python|代码|时序)?[栈队树图\w]*演示[:：])[ \t]*(?=[a-zA-Z0-9_#])/g, '$1\n');
        // c. 行内注释后紧贴下一条代码语句拆行（如「#入栈stack.append(2)」->「# 入栈\nstack.append(2)」）
        s = s.replace(/(#[ \t]*[^\n\r#]+?)[ \t]*(?=(?:stack|print|def|class|return|[a-zA-Z_]\w*\s*=|[a-zA-Z_]\w*\.)\b)/g, '$1\n');
        // d. 推导对错符号「✗」「✓」后紧贴下一公式、错例或区域拆行
        s = s.replace(/([✓✗])[ \t]*(?=[一二三四五六七八九十\d\u4e00-\u9fa5A-Za-z【])/g, '$1\n');
        // e. 错解/正解前换行
        s = s.replace(/([^\n\r])[ \t]*(?=(?:错解|正解)\s*[A-Z\d]*[：:])/g, '$1\n');
      }

      return `${openFence}${s.trimEnd()}\n${closeFence}`;
    }
  );
}

/**
 * 规范化代码块内部的命令、注释与参数排版，拆分挤压在同一行的注释与命令行
 * 例如：
 * 1. 语言标记与首行紧贴：```bash# 1.xxx -> ```bash\n# 1.xxx
 * 2. 注释后紧贴命令：# 1. 看 Eden增长速度...每次耗时jstat -gcutil <pid> 1000
 *    -> # 1. 看 Eden增长速度...每次耗时\njstat -gcutil <pid> 1000
 * 3. 命令行参数后紧贴下一条注释：1000#2. GC日志 -> 1000\n\n# 2. GC日志
 * 4. 中文说明/冒号后紧贴执行命令：async-profiler：./profiler.sh -> async-profiler：\n./profiler.sh
 */
function normalizeCommandsAndCommentsInFences(text: string): string {
  // 1. 语言标记与代码首行紧贴修复：
  // 必须是完整语言名（全词匹配），后面紧贴 # 或中文字符或数字，绝不可在英文字母之间切断（例如 csharp、css、json）
  let out = text.replace(
    /(```(?:bash|sh|shell|zsh|powershell|cmd|java|python|csharp|cs|css|sql|json|yaml|xml|javascript|typescript|js|ts|text|markdown|md)\b)[ \t]*([#\/\u4e00-\u9fa5\d])/gi,
    '$1\n$2'
  );

  out = out.replace(
    /(```[^\n]*\n)([\s\S]*?)(```|$)/g,
    (full, openFence: string, body: string, closeFence: string) => {
      // 保护 ASCII 字符框线、树状图：绝不可破坏
      if (
        /\+[-=]{3,}\+/.test(body) ||
        /^[ \t]*\+[-=]+[ \t]*$/m.test(body) ||
        /^[ \t]*[┌├└│─┼]/m.test(body)
      ) {
        return full;
      }

      let s = body;

      // 1. 命令参数末尾粘连下一条编号注释：如「...1000#2. GC日志」或「...tags#3. 定位是谁在分配」
      // 严格防御：仅在 0 空格紧贴时拆分；严格排除版本号（1.0 / v1.2 / 2.0.1）与 issue/task/pr 业务标签
      s = s.replace(
        /(^|[ \t\n,;=>])([a-zA-Z0-9_.-]+)(#[ \t]*\d+[.、．\s]+[a-zA-Z\u4e00-\u9fa5]|#[ \t]*\d+[.、．\s]*[\u4e00-\u9fa5])/g,
        (match, prefix: string, token: string, comment: string, offset: number, fullStr: string) => {
          const beforeToken = fullStr.slice(Math.max(0, offset - 30), offset + prefix.length);
          if (/(?:issue|task|bug|pr|commit|version|ver|版本|v|build|release)[ \t]*$/i.test(beforeToken)) {
            return match;
          }
          if (/^v?\d+(?:\.\d+)+$/i.test(token) || /^v\d+$/i.test(token) || beforeToken.endsWith('.')) {
            return match;
          }
          if (/^(?:issue|task|bug|pr|commit|version|ver|v)$/i.test(token)) {
            return match;
          }
          return `${prefix}${token}\n\n${comment}`;
        }
      );

      // 2. 章节步骤注释末尾紧贴待执行 ./ 脚本命令（如「# 3. 定位是谁在分配...async-profiler：./profiler.sh -e alloc...」）
      // 严格防御：
      // a) 仅在带有序号的步骤注释行（# \d+. ...）中触发，普通单行注释（如「# 执行脚本：./run.sh」或「// 资源路径：./config」）绝不拆行
      // b) 排除配置代码、Java注解与对象字面量（如「output: ./dist」、「{ entry:./main.js }」、「@Value("${app.dir:./logs}")」）
      s = s.replace(
        /(^[ \t]*#[ \t]*\d+[.、．][^\n：:]*[\u4e00-\u9fa5][^\n：:]*：[ \t]*)(\.\/[a-zA-Z0-9_.-]+)/gm,
        '$1\n$2'
      );

      // 3. 中文注释文字末尾紧贴带有 - 选项的 CLI 命令（如「...每次耗时jstat -gcutil」）
      // 严格防御：汉字与命令之间必须为 0 空格紧贴（如「每次耗时jstat」），中间有空格者（如「# 用 jstat -gcutil」）为正常注释，绝不拆行
      const CLI_CMDS =
        '(?:jstat|jstack|jmap|jcmd|java|javac|javap|jar|curl|wget|docker|kubectl|git|npm|pnpm|yarn|mvn|gradle|pip|python|node|sh|bash|chmod|chown|systemctl|service|grep|awk|sed|cat|tail|head|sudo|kill|ps|top|htop|netstat|lsof|ss|ip|ping|traceroute|tar|unzip|zip|find)';

      s = s.replace(
        new RegExp(`(#[ \\t]*[^\\n\\r#]+?[\u4e00-\u9fa5])(?<![a-zA-Z0-9_.-])(${CLI_CMDS}\\b[ \\t]+-[a-zA-Z0-9_-]+)`, 'gu'),
        '$1\n$2'
      );

      // 3.5. Java/C/JS/TS/Go 代码块内单行中文注释（//...汉字）末尾紧贴下一条方法声明（含方法体 {）时拆行
      // 避免 highlight.js 将从 // 开始的整段代码全部当成单条注释而挤在同一行
      // 关键防御：
      // 1) 必须预查到完整的方法声明结构（修饰符/返回类型 + 方法名 + 参数列表 + 方法体左花括号 {）；
      // 2) 若方法体闭合花括号 } 后紧跟中文（如「// 注意子类重写void run() {}时不能...」），说明是注释内行文举例，严禁拆行；
      // 3) 支持注释末尾带有中文句号、括号等标点（如「// 非static（错误）public static void main()」）。
      s = s.replace(
        /(\/\/[^\n\r]*?[\u4e00-\u9fa5][^\n\r]*?[\u4e00-\u9fa5）)”"』」】。！!？?；;])[ \t]*(?=(?:(?:public|protected|private|static|final|abstract|synchronized)\s+)*(?:void|[a-zA-Z0-9_<>[\]]+)\s+[a-zA-Z0-9_]+\s*\([^()\n]*\)\s*(?:throws\s+[a-zA-Z0-9_,\s]+)?\{(?![^{}\n]*\}[ \t]*[\u4e00-\u9fa5]))/g,
        (match, commentPart: string, offset: number, fullStr: string) => {
          const lineStart = fullStr.lastIndexOf('\n', offset) + 1;
          const beforeComment = fullStr.slice(lineStart, offset).trim();
          const afterMatch = fullStr.slice(offset + match.length);
          const hasExplicitModifier = /^(?:public|protected|private|static|final|abstract|synchronized)\b/.test(
            afterMatch
          );
          if (!beforeComment && !hasExplicitModifier) {
            return match;
          }
          return `${commentPart}\n`;
        }
      );

      // 4. 按行清洗独立成行的编号注释，并在 # 与序号或中文之间保证标准空格（如 #1. -> # 1.，#查看 -> # 查看）
      // 白名单防御：仅匹配紧跟数字序号或汉字的教学注释行，绝不误触 #fff (CSS)、#!/bin (Shebang) 或 #include / #region (C/C#)
      const lines = s.split('\n');
      const processed: string[] = [];
      for (const line of lines) {
        if (/^[ \t]*#!/.test(line) || /^[ \t]*#(?:region|endregion|include|define|ifndef|ifdef|endif|pragma)\b/.test(line)) {
          processed.push(line);
          continue;
        }

        // 代码块内普通中文注释沿用既有行为，在 # 与文字间补一个标准空格（# 查看进程），同时严格保留行首缩进空格；
        // 关键防护：行首十六进制色值（#1a2b3c、#2f80ed）绝不能被当成序号注释插空格破坏代码，
        //          因此「# + 数字 + 字母」只有在数字后带序号标点时才成立。
        let cleaned = line.replace(
          /^([ \t]*#)(?=\d+[.、．][ \t]*[\u4e00-\u9fa5A-Za-z]|\d+[ \t]+[\u4e00-\u9fa5]|[\u4e00-\u9fa5])/g,
          '$1 '
        );
        cleaned = cleaned.replace(
          /^([ \t]*#[ \t]*\d+[.、．])([^\s.、．\d])/g,
          '$1 $2'
        );
        processed.push(cleaned);
      }

      let joined = processed.join('\n');

      // 5. 常见配置、CI 流水线与 Docker Compose 缺失换行容错（解决大模型流式输出丢失换行符导致 steps/属性 挤在同一行、滚动条撑开的问题）
      // 关键防护：仅针对 YAML 围栏（或未标注语言但具备典型 YAML 顶层结构的围栏）执行，绝不污染 JS/Java/Python/Bash/C 等常规代码
      const fenceLang = openFence.replace(/^```+/, '').trim().split(/\s+/)[0]?.toLowerCase() || '';
      const isYamlFence =
        fenceLang === 'yaml' ||
        fenceLang === 'yml' ||
        (!fenceLang && /^[ \t]*(?:name|services|version|jobs|on|packages):/m.test(joined));

      if (isYamlFence) {
        // 5a. 注释行末尾紧贴配置字段声明：
        // (1) 文件名注释紧贴 name: 或 concurrency: 等（如「# .github/workflows/ci.ymlname:」）
        joined = joined.replace(
          /(^[ \t]*#[ \t]*[^\n\r]*?\.(?:ya?ml|json|toml|ts|js|sh|py|java|xml|md|sql|txt))(?=[a-zA-Z_][a-zA-Z0-9_-]*:)/gm,
          '$1\n'
        );
        // (2a) 中文注释紧贴顶层配置关键字（如「# 并发控制：...保留一个concurrency:」或「# 配置 name: app」）
        // 关键防护：无空格紧贴时拆行；有空格时要求冒号后为非中文配置值；纯注释行有空格且以关键字收尾换行（如「# 下面定义容器的 services:」）严禁拆行
        joined = joined.replace(
          /(#[^\n\r#]*?[\u4e00-\u9fa5][^\n\r#]*?)(?:(?<=[^\s])(?=(?:name|concurrency|jobs|services):)|[ \t]+(?=(?:name|concurrency|jobs|services):[ \t]+[a-zA-Z0-9_./~"'[{<-]))/g,
          '$1\n'
        );
        // (2b) 行内尾随注释末尾粘连子级属性并换行（如「- /app/node_modules #用匿名卷... environment:」），或无空格紧贴，或注释后紧跟有效配置值
        joined = joined.replace(
          /(^[ \t]*[^\s#\n\r][^\n\r#]*#[^\n\r#]*?[\u4e00-\u9fa5][^\n\r#]*?)[ \t]+(?=(?:steps|runs-on|strategy|matrix|env|environment|permissions|volumes|ports|healthcheck|working_dir|command):(?:[ \t]*(?:\r?\n|$)|[ \t]+[a-zA-Z0-9_./~"'[{<-]))/gm,
          '$1\n    '
        );
        joined = joined.replace(
          /(^[ \t]*#[^\n\r#]*?[\u4e00-\u9fa5][^\n\r#]*?)(?:(?<=[^\s])(?=(?:steps|runs-on|strategy|matrix|env|environment|permissions|volumes|ports|healthcheck|working_dir|command):)|[ \t]+(?=(?:steps|runs-on|strategy|matrix|env|environment|permissions|volumes|ports|healthcheck|working_dir|command):[ \t]+[a-zA-Z0-9_./~"'[{<-]))/gm,
          '$1\n    '
        );

        // 5b. 字段值紧贴下一个顶层声明（严格限定在特定行首字段，避免误伤 version: 或 runs-on: 本身）：
        // (1) name: ... 紧贴 on:（如「name: buildon:」或「name: build-and-teston:」）
        joined = joined.replace(
          /(^[ \t]*name:[ \t]*['"]?[a-zA-Z0-9_.-]+?['"]?)(?=on:)/gm,
          '$1\n'
        );
        // (2) cancel-in-progress: ... 紧贴 jobs: 或 build:（如「cancel-in-progress: truejobs:」）
        joined = joined.replace(
          /(^[ \t]*cancel-in-progress:[ \t]*(?:true|false))(?=(?:jobs|build):)/gm,
          '$1\n'
        );
        // (3) runs-on: ... 同行紧贴 strategy: 或 steps:（如「runs-on: ubuntu-latest strategy:」）
        joined = joined.replace(
          /(^[ \t]*)(runs-on:[ \t]*[a-zA-Z0-9_.-]+)[ \t]+(strategy:|steps:)/gm,
          '$1$2\n$1$3'
        );
        // (4) fail-fast: ... 同行紧贴 matrix:（如「fail-fast: false matrix:」）
        joined = joined.replace(
          /(^[ \t]*)(fail-fast:[ \t]*(?:true|false))[ \t]+(matrix:)/gm,
          '$1$2\n$1$3'
        );

        // 5c. Docker Compose 常见服务属性同行紧贴分行（如「image: node:20-alpine working_dir: /app command: ...」）
        const DOCKER_SERVICE_KEYS = '(?:working_dir|command|container_name|restart|ports|volumes|environment|env_file|depends_on|healthcheck|entrypoint|user|expose|labels|networks|build|logging)';
        joined = joined.replace(
          new RegExp(`(^[ \\t]*(?:image|build|container_name|working_dir):[^\\n\\r]+?)[ \\t]+(${DOCKER_SERVICE_KEYS}:)`, 'gm'),
          '$1\n    $2'
        );
        joined = joined.replace(
          new RegExp(`(^[ \\t]*working_dir:[^\\n\\r]+?)[ \\t]+(${DOCKER_SERVICE_KEYS}:)`, 'gm'),
          '$1\n    $2'
        );

        // 5d. healthcheck 健康检查参数同行紧贴分行与补空格（如「interval:10s timeout:3s retries:5 start_period:30s」）
        const healthcheckMatch = joined.match(/^([ \t]*)healthcheck:[ \t]*$/m);
        if (healthcheckMatch) {
          const hcBaseLen = healthcheckMatch[1].length;
          const hcPropIndent = ' '.repeat(hcBaseLen > 0 ? hcBaseLen + 2 : 2);
          joined = joined.replace(
            /(^[ \t]*test:[^\n\r]+?)[ \t]+(interval:)/gm,
            `$1\n${hcPropIndent}$2`
          );
          joined = joined.replace(
            /((?:test|interval|timeout|retries|start_period):[^\n\r]+?)[ \t]+(?=(?:timeout|retries|start_period):)/g,
            `$1\n${hcPropIndent}`
          );
          joined = joined.replace(
            /(\b(?:interval|timeout|retries|start_period)):(\d+[a-zA-Z]*)/g,
            '$1: $2'
          );
          // 统一补齐 healthcheck: 内部属性（test, interval 等）缩进不足的情况，消除顶格凸出锯齿
          joined = joined.replace(
            new RegExp(
              `^([ \\t]{0,${Math.max(0, hcPropIndent.length - 1)}})(test:[ \\t]*\\S|interval:|timeout:|retries:|start_period:)`,
              'gm'
            ),
            `${hcPropIndent}$2`
          );
        }

        // 5e. CI 流水线步骤与属性自适应缩进对齐（彻底消除硬编码固定空格导致的阶梯式锯齿）
        const stepsMatch = joined.match(/^([ \t]*)steps:[ \t]*$/m);
        let stepIndent = '      ';
        if (stepsMatch) {
          const baseLen = stepsMatch[1].length;
          if (baseLen === 0) {
            const afterSteps = joined.slice((stepsMatch.index ?? 0) + stepsMatch[0].length);
            const nextStepMatch = afterSteps.match(/^\r?\n([ \t]*)- (?:uses|run|name):/);
            const existingLen = nextStepMatch ? nextStepMatch[1].length : 2;
            stepIndent = ' '.repeat(existingLen >= 2 ? existingLen : 2);
          } else {
            // 若大模型输出了 1-2 空格的浅缩进 steps:，统一步进至标准 4 空格；4 空格 steps: 步进至 6 空格，遵循标准 YAML 偶数缩进哲学
            const targetLen = baseLen <= 2 ? 4 : baseLen % 2 !== 0 ? baseLen + 3 : baseLen + 2;
            stepIndent = ' '.repeat(targetLen);
          }
        } else {
          const firstStepMatch = joined.match(/^([ \t]*)- (?:uses|run|name):/m);
          if (firstStepMatch) {
            stepIndent = firstStepMatch[1];
          }
        }
        const withIndent = stepIndent + '  ';
        const withParamIndent = withIndent + '  ';

        // (1) with: 紧贴在 uses: 步骤行末尾时拆行并对齐（如「- uses: pnpm/action-setup@v4 with:」）
        joined = joined.replace(
          /(- uses:[^\n\r#]+?)[ \t]+(with:)[ \t]*/g,
          `$1\n${withIndent}$2`
        );

        // (2) 列表项粘连拆行（CI steps「- uses:」「- run:」「- name:」）
        // 关键防护：前驱必须为实质非空白字符 [^\s]，且该行在匹配点前不含 # 注释，防止误拆注释或插空行
        joined = joined.replace(
          /([^\s])[ \t]+(- (?:uses|run|name):)/g,
          (match, prevChar: string, stepToken: string, offset: number, fullStr: string) => {
            const lineStart = fullStr.lastIndexOf('\n', offset) + 1;
            if (fullStr.slice(lineStart, offset + prevChar.length).includes('#')) {
              return match;
            }
            return `${prevChar}\n${stepIndent}${stepToken}`;
          }
        );

        // (3) 仅当前导项也是 YAML 普通值列表项时才拆分后续粘连的挂载/路径列表项，严禁误伤步骤声明（- run: / - uses:）或命令行参数
        // 关键防护：前导项严禁为 - (?:run|name|uses|id|if|with): 等步骤字典声明，避免把 - run: tar -czf - ./dist 中的 - ./dist 误当成挂载列表项拆行
        joined = joined.replace(
          /(^|\n)([ \t]*-(?![ \t]+(?:run|name|uses|id|if|with):)[ \t]+[^\n\r#]+?)[ \t]+(- (?:[./~"']|\$))/g,
          '$1$2\n    $3'
        );

        // (4) steps 关键字下方的所有步骤列表项，统一对齐至 stepIndent 缩进，消除首项与后续项缩进不一致的锯齿
        if (stepsMatch) {
          joined = joined.replace(
            /^[ \t]*(- (?:uses|run|name):)/gm,
            `${stepIndent}$1`
          );
        }

        // (5) with: 内部参数同行粘连拆行与行首缩进补齐
        // 关键防护：仅在存在 with: 块时匹配非注释、非命令行中的参数，严禁误伤注释（如「# 注意指定 version: 18」）或顶层 version:/cache:
        if (/^[ \t]*with:[ \t]*$/m.test(joined)) {
          joined = joined.replace(
            /([^\s])[ \t]+((?:version|node-version|cache):[ \t]*[a-zA-Z0-9_-]+)/g,
            (match, prevChar: string, paramToken: string, offset: number, fullStr: string) => {
              const lineStart = fullStr.lastIndexOf('\n', offset) + 1;
              const linePrefix = fullStr.slice(lineStart, offset + prevChar.length);
              if (linePrefix.includes('#') || /^[ \t]*-?[ \t]*(?:run|command):/.test(linePrefix)) {
                return match;
              }
              return `${prevChar}\n${withParamIndent}${paramToken}`;
            }
          );
          joined = joined.replace(
            new RegExp(`(^${withIndent}with:[ \\t]*\\r?\\n)[ \\t]*(version:|node-version:|cache:|name:|path:)`, 'gm'),
            `$1${withParamIndent}$2`
          );
          joined = joined.replace(
            new RegExp(`(^${withParamIndent}[a-zA-Z0-9_.-]+:[^\\n\\r]*\\r?\\n)[ \\t]*(version:|node-version:|cache:|name:|path:)`, 'gm'),
            `$1${withParamIndent}$2`
          );
        }

        // 5f. - name: 步骤内部紧贴执行命令（如「- name: Install run:」）
        joined = joined.replace(
          /(- name:[^\n\r]+?)[ \t]+(run:|uses:|with:)/g,
          `$1\n${withIndent}$2`
        );

        // 5g. 常见数值字段紧贴冒号补空格（如「version:9」->「version: 9」），增强 YAML 语法高亮
        joined = joined.replace(
          /(\b(?:version|node-version)):(\d+)/g,
          '$1: $2'
        );
      }

      // 6. Python 代码块：粘连语句拆行与缩进重构
      // 解决大模型流式输出或文档切片解析时丢失代码行换行符（如 rightdef build(pre, mid):、return None root = Node(...)、self.val = val self.left = left 等）
      // 导致多行代码被挤压在同一行以及代码块内部缩进坍塌问题
      const isPythonFence =
        fenceLang === 'python' ||
        fenceLang === 'py' ||
        (!fenceLang && /^[ \t]*(?:def|class)\s+\w+.*:|^[ \t]*self\.\w+\s*=/m.test(joined));

      if (isPythonFence) {
        joined = normalizePythonCodeInsideFence(joined);
      }

      return `${openFence}${joined}${closeFence}`;
    }
  );

  return out;
}

/**
 * 修复 Python 代码块内部多语句同行挤压、关键字粘连与缩进坍塌问题
 * （例如「self.right = rightdef build(pre, mid):」「return None root = Node(pre[0])」「self.val = val self.left = left」）
 * 无论切片源是粘连丢失换行、1~2 空格浅缩进还是坍塌为 0 缩进，均规整为 PEP 8 标准（4 空格步进、逻辑层级分明）。
 * 关键防御：
 * 1. 纯注释行（^[ \t]*#）100% 完整保护，绝对禁止任何代码关键字拆行替换，避免「# 调用 print(x) 输出结果」或「# 例如 i = 0 时」被腰斩为独立语法错误代码；
 * 2. 严格限定仅在确凿检测到控制块缩进坍塌（hasCollapsedBlock === true）时才启用状态机重构，绝不误伤字典/列表字面量、长函数调用参数跨行续行与已有健康代码；
 * 3. 复合语句控制块判断严格限定为 Python 语法关键字（def/class/if/for/while/try/with...:），避免字典键值对冒号被误判为控制块。
 */
function normalizePythonCodeInsideFence(body: string): string {
  const initialLines = body.split('\n');
  const separatedLines: string[] = [];

  for (const line of initialLines) {
    const trimmed = line.trim();

    // 关键防御 1：整行纯注释行（以 # 开头）绝对不执行代码拆行替换，原样完整保留！
    if (trimmed.startsWith('#')) {
      separatedLines.push(line);
      continue;
    }

    // 关键防御 2：空行直接保留
    if (!trimmed) {
      separatedLines.push(line);
      continue;
    }

    let processed = line;

    // 1. 拆开紧贴在上一语句或单词末尾的 def / class 关键字（如 rightdef build(pre, mid): 或 return rootdef postorder(node):）
    processed = processed.replace(/([^\s\n\r])\s*(def\s+[a-zA-Z_]\w*\s*\([^()\n\r]*\)\s*:)/g, '$1\n$2');
    processed = processed.replace(/([^\s\n\r])\s*(class\s+[a-zA-Z_]\w*(?:\([^()\n\r]*\))?\s*:)/g, '$1\n$2');

    // 2. 拆开连续的 self.xxx = yyy self.aaa = bbb 属性赋值语句
    processed = processed.replace(/(self\.[a-zA-Z_]\w*\s*=[^=\n\r]+?)[ \t]+(?=self\.[a-zA-Z_]\w*\s*=)/g, '$1\n');

    // 3a. 智能拆分常见返回值变量后粘连的下一行赋值语句（如 return restree = build(...) -> return res\ntree = build(...)）
    processed = processed.replace(
      /(return\s+(?:res|root|ans|result|ret|total|node|val|data|nums|tree|s))([a-zA-Z_]\w*\s*=)/g,
      '$1\n$2'
    );

    // 3b. 拆开 return 语句与后续同行紧贴的执行语句（如 return None root = Node(...)、return [] res, q = [] 等）
    processed = processed.replace(
      /(return\s+(?:None|True|False|\[\]|\{\}|\(\)|"[^"\n]*"|'[^'\n]*'|-?\d+(?:\.\d+)?|[a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)*))[ \t]+(?=[a-zA-Z_]\w*\s*=|if\b|for\b|while\b|def\b|class\b|print\b|return\b)/g,
      '$1\n'
    );

    // 4. 行尾注释后紧贴代码拆分：仅在代码行后紧跟行尾注释、且注释后紧贴无中文叙述的独立代码时才拆开
    // 例如：print("".join(postorder(tree))) # DEBFGCAprint("".join(levelorder(tree))) # ABCDEFG
    // 关键防御：若代码关键字之后（直到下一个 # 之前）含有汉字（如「# 调用 print(x) 输出结果」），则属于注释叙述，严禁劈裂！
    processed = processed.replace(
      /(#[^\n\r#]*?[\w\u4e00-\u9fa5)\]}”"'])[ \t]*(?=(?:print\s*\(|def\s+|class\s+|return\b|if\b|for\b|while\b|[a-zA-Z_]\w*\s*=))/g,
      (match, commentPrefix, offset, fullStr) => {
        const afterStr = fullStr.slice(offset + match.length);
        const untilNextHash = afterStr.split('#')[0];
        if (/[\u4e00-\u9fa5]/.test(untilNextHash)) {
          return match;
        }
        return commentPrefix + '\n';
      }
    );

    if (processed.includes('\n')) {
      separatedLines.push(...processed.split('\n'));
    } else {
      separatedLines.push(processed);
    }
  }

  const rawLines = separatedLines;

  // 收集非空代码行的前导缩进空格数
  interface LineInfo {
    trimmed: string;
    indent: number;
    isColon: boolean;
    isComment: boolean;
  }

  // 严格限定 Python 复合语句（控制块）开启关键字，杜绝字典键值对冒号被误判为控制块
  const PYTHON_BLOCK_START_RE =
    /^(?:def\b|class\b|if\b|elif\b|else\s*:|for\b|while\b|try\s*:|except\b|finally\s*:|with\b|match\b|case\b|async\s+(?:def|for|with)\b)/;

  const parsedLines: LineInfo[] = rawLines.map((l) => {
    const trimmed = l.trim();
    const indentMatch = l.match(/^[ \t]*/);
    const indent = indentMatch ? indentMatch[0].length : 0;
    const isComment = trimmed.startsWith('#');
    const isColon = !isComment && trimmed.endsWith(':') && PYTHON_BLOCK_START_RE.test(trimmed);
    return { trimmed, indent, isColon, isComment };
  });

  // 分析是否具备相对缩进层级
  let hasIndentedBlock = false;
  let hasCollapsedBlock = false;
  const positiveDiffs: number[] = [];

  for (let i = 0; i < parsedLines.length - 1; i++) {
    const cur = parsedLines[i];
    if (cur.isColon) {
      let nextLine: LineInfo | null = null;
      for (let j = i + 1; j < parsedLines.length; j++) {
        if (parsedLines[j].trimmed && !parsedLines[j].isComment) {
          nextLine = parsedLines[j];
          break;
        }
      }
      if (nextLine) {
        const diff = nextLine.indent - cur.indent;
        if (diff > 0) {
          hasIndentedBlock = true;
          positiveDiffs.push(diff);
        } else {
          hasCollapsedBlock = true;
        }
      }
    }
  }

  // 模式 1：健康代码防护（绝无冒号控制块坍塌）
  // 若未发生控制块缩进坍塌（不存在冒号控制块后缩进未增加的反常情况）：
  if (!hasCollapsedBlock) {
    // 检查是否存在浅缩进（1~2 空格步进）需要规范放大至 PEP 8 标准 4 空格
    if (hasIndentedBlock && positiveDiffs.length > 0) {
      const minDiff = Math.min(...positiveDiffs);
      if (minDiff > 0 && minDiff < 4) {
        return rawLines
          .map((line) => {
            const trimmed = line.trim();
            if (!trimmed) return '';
            const indentMatch = line.match(/^[ \t]*/);
            const rawIndent = indentMatch ? indentMatch[0].length : 0;
            const targetIndent = Math.round((rawIndent / minDiff) * 4);
            return ' '.repeat(targetIndent) + trimmed;
          })
          .join('\n');
      }
    }
    // 所有健康的 Python 代码（4 空格 PEP 8 缩进、无控制块平铺语句、字典/列表/元组字面量、长参数续行等）100% 保持原样！
    return rawLines.join('\n');
  }

  // 模式 3：仅当确凿检测到控制块缩进坍塌（hasCollapsedBlock === true）时，才启用 Python 语法状态机进行缩进重构
  interface StackFrame {
    type: 'class' | 'def' | 'loop' | 'branch';
    indent: number;
    statementCount: number;
  }

  const stack: StackFrame[] = [];
  const result: string[] = [];
  let inClass = false;
  let lastWasReturn = false;

  for (let i = 0; i < parsedLines.length; i++) {
    const { trimmed, isComment } = parsedLines[i];
    if (!trimmed) {
      result.push('');
      continue;
    }

    if (isComment) {
      const commentIndent = stack.length > 0 ? stack[stack.length - 1].indent + 4 : 0;
      result.push(' '.repeat(commentIndent) + trimmed);
      continue;
    }

    // A. 遇到类声明 class <Name>:
    if (/^class\s+\w+.*:/.test(trimmed)) {
      stack.length = 0;
      inClass = true;
      lastWasReturn = false;
      result.push(trimmed);
      stack.push({ type: 'class', indent: 0, statementCount: 0 });
      continue;
    }

    // B. 遇到函数/方法声明 def <name>(...):
    if (/^def\s+\w+.*:/.test(trimmed)) {
      const isMethod = inClass && /\(\s*self\b/.test(trimmed);
      if (isMethod) {
        // 保留外层 class，清空 class 内部旧帧
        stack.length = 1;
        const methodIndent = 4;
        result.push(' '.repeat(methodIndent) + trimmed);
        stack.push({ type: 'def', indent: methodIndent, statementCount: 0 });
      } else {
        inClass = false;
        stack.length = 0;
        result.push(trimmed);
        stack.push({ type: 'def', indent: 0, statementCount: 0 });
      }
      lastWasReturn = false;
      continue;
    }

    // C. 检查顶层主程序代码（在顶层函数 return 之后出现的主程序语句，如 tree = build(...)、print(...) 等）
    const isTopLevelStmt =
      lastWasReturn &&
      stack.length <= 1 &&
      !/^(?:elif\b|else:|except\b|finally:)/.test(trimmed) &&
      !trimmed.endsWith(':');

    if (isTopLevelStmt) {
      stack.length = 0;
      inClass = false;
    }

    // D. 遇到平级分支连接词 elif / else / except / finally
    if (/^(?:elif\b|else:|except\b|finally:)/.test(trimmed)) {
      // 弹出最近一层 branch
      while (stack.length > 0 && stack[stack.length - 1].type === 'branch') {
        stack.pop();
      }
      const branchIndent = stack.length > 0 ? stack[stack.length - 1].indent + 4 : 0;
      result.push(' '.repeat(branchIndent) + trimmed);
      stack.push({ type: 'branch', indent: branchIndent, statementCount: 0 });
      lastWasReturn = false;
      continue;
    }

    // E. 单语句分支回退（例如 if not pre: return None 之后，下一行语句退回与 if 同级；或者 if cur.left: q.append(...) 之后遇到平级 if cur.right:）
    if (stack.length > 0 && stack[stack.length - 1].type === 'branch') {
      const topBranch = stack[stack.length - 1];
      const isSiblingIf = /^if\s+.*:/.test(trimmed);
      const isReturnAfterBranch = /^return\b/.test(trimmed);
      if (topBranch.statementCount >= 1 && (isSiblingIf || isReturnAfterBranch || lastWasReturn)) {
        stack.pop();
      } else if (lastWasReturn) {
        stack.pop();
      }
    }

    // E2. 循环体收尾返回：在 while/for 循环后遇到最终无条件返回（如 return res），循环结束并回退至函数层级
    if (/^return\b/.test(trimmed) && (stack.length === 0 || stack[stack.length - 1].type !== 'branch')) {
      while (stack.length > 0 && stack[stack.length - 1].type === 'loop') {
        stack.pop();
      }
    }

    // F. 计算当前行的缩进
    const curIndent = stack.length > 0 ? stack[stack.length - 1].indent + 4 : 0;
    result.push(' '.repeat(curIndent) + trimmed);

    if (stack.length > 0) {
      stack[stack.length - 1].statementCount++;
    }

    // G. 状态更新
    const isBlockStart = trimmed.endsWith(':');
    if (isBlockStart) {
      const isLoop = /^(?:for\b|while\b)/.test(trimmed);
      const isBranch = /^(?:if\b|try:|with\b)/.test(trimmed);
      stack.push({
        type: isLoop ? 'loop' : isBranch ? 'branch' : 'def',
        indent: curIndent,
        statementCount: 0
      });
      lastWasReturn = false;
    } else if (/^return\b/.test(trimmed)) {
      lastWasReturn = true;
      // 若当前在循环内执行了返回（如 while q: ... return res），退出该循环
      if (stack.length > 0 && stack[stack.length - 1].type === 'loop') {
        stack.pop();
      }
    } else {
      lastWasReturn = false;
    }
  }

  return result.join('\n');
}



/** 正文里裸露的字符树包进代码围栏，避免被 Markdown 渲染成 <p> 后逐字竖排 */
function wrapBareAsciiTreeBlocks(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const out: string[] = [];
      let i = 0;

      while (i < lines.length) {
        const line = lines[i];
        if (!/[├└]/.test(line)) {
          out.push(line);
          i += 1;
          continue;
        }

        const blockLines: string[] = [];
        while (i < lines.length) {
          const ln = lines[i];
          const trimmed = ln.trim();
          if (blockLines.length > 0 && trimmed === '') {
            i += 1;
            break;
          }
          if (
            blockLines.length === 0
            || /[├└│]/.test(ln)
            || /^[ \t│]/.test(ln)
            || (trimmed.startsWith('│') && blockLines.length > 0)
          ) {
            blockLines.push(ln);
            i += 1;
          } else {
            break;
          }
        }

        const joined = blockLines.join('\n').trim();
        if (joined && looksLikeAsciiKnowledgeTree(joined)) {
          out.push('```\n' + expandAsciiTreeToMultiline(joined) + '\n```');
        } else if (blockLines.length) {
          out.push(blockLines.join('\n'));
        }
      }

      return out.join('\n');
    })
    .join('');
}

/** 正文里粘连的字符树（非代码块）拆行 */
function normalizeInlineAsciiTree(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      if (!/[├└]/.test(segment)) return segment;
      return expandAsciiTreeToMultiline(segment);
    })
    .join('');
}

/** 粘连的中文关键词引号 "" 拆行（表格行内跳过） */
function normalizeQuotedKeywordLines(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      const lines = segment.split('\n');
      const processed = lines.map((line) => {
        if (isMarkdownTableRowLine(line)) return line;
        return line.replace(/""/g, '"\n"');
      });
      return processed.join('\n');
    })
    .join('');
}

/**
 * 教学与问答高频小标题（如「**参考答案：**」「参考解析：」「【题目】」等）粘连自动拆行（表格行跳过）
 */
function normalizeTeachingHeaders(text: string): string {
  const HEADER_RE =
    /([^\n*])\s*(\*\*【?(?:参考答案|标准答案|正确答案|答案|试题解析|题目解析|试题分析|参考解析|解析|分析|解答|解题思路|解题步骤|思路点拨|考点精解|核心考点|考点|题目描述|题目|知识点|评分细则|评分标准|总结|小结|归纳)(?:(?:】|[:：])[^*\n]{0,24})?\*\*|【(?:参考答案|标准答案|正确答案|答案|试题解析|题目解析|试题分析|参考解析|解析|分析|解答|解题思路|解题步骤|思路点拨|考点精解|核心考点|考点|题目描述|题目|知识点|评分细则|评分标准|总结|小结|归纳)[:：]?】)/g;
  const HEADER_BEFORE_CIRCLED_RE =
    /(\*\*【?(?:参考答案|标准答案|正确答案|答案|试题解析|题目解析|试题分析|参考解析|解析|分析|解答|解题思路|解题步骤|思路点拨|考点精解|核心考点|考点|题目描述|题目|知识点|评分细则|评分标准|总结|小结|归纳)[:：]?】?\*\*|【(?:参考答案|标准答案|正确答案|答案|试题解析|题目解析|试题分析|参考解析|解析|分析|解答|解题思路|解题步骤|思路点拨|考点精解|核心考点|考点|题目描述|题目|知识点|评分细则|评分标准|总结|小结|归纳)[:：]?】)[ \t]*([①②③④⑤⑥⑦⑧⑨⑩])/g;

  // 句末标点（。！？）后粘连的无粗体小标题（如「...前n项和。参考解析：」或「...解答。答案：」）
  const PLAIN_HEADER_AFTER_PUNCT_RE =
    /([。；;!?！？])[ \t]*((?:参考答案|标准答案|正确答案|答案|试题解析|题目解析|试题分析|参考解析|解析|解答|解题思路|解题步骤|思路点拨|考点精解|考点|题目)[:：])/g;

  // 结构性解题步骤小标头（如「**第一步：求首项 \(a_1\)**」「**第二步：寻找递推关系**」「**Step 1:**」「**通项公式为：**」「**💡课程知识延伸：**」等）
  // 必须严格限制为带有明确阶段指示意义的标头，绝对排除普通随文加粗强调（如「是**正项数列**」）
  const STEP_HEADER_RE =
    /([^\n\s#])\s*(\*\*(?:第[一二三四五六七八九十\d]+步|步骤[一二三四五六七八九十\d]+|Step\s*\d+|解法[一二三四五六七八九十\d]+|方法[一二三四五六七八九十\d]+|通项公式(?:为)?|最终答案|结论|求解目标|已知条件)[:：\s][^*\n]+?\*\*|\*\*[\u{1F300}-\u{1F9FF}\u{2600}-\u{26FF}]?[ \t]*(?:课程知识延伸|知识拓展|拓展延伸|拓展与反思|名师点拨|名师提醒|深度剖析|高频易错|思维升华)[:：]?[ \t]*\*\*)/gu;

  // 检查同一行在目标小标题之前是否仅包含行首修饰（空白 / 列表标记 / # / 装饰性 Emoji），若是则禁止拆行
  const isOnlyLeadingDecorator = (prefixUpToMarker: string): boolean => {
    const stripped = prefixUpToMarker
      .replace(/^[ \t]*(?:#{1,6}[ \t]*|[-*+•][ \t]*|\d+[.、)][ \t]*)?/u, '')
      .replace(/(?:[\p{Extended_Pictographic}\u{2600}-\u{27BF}]|\u{FE0F}|\u{200D}|\s)/gu, '');
    return stripped.length === 0;
  };

  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      const lines = segment.split('\n');
      const processed = lines.map((line) => {
        if (isMarkdownTableRowLine(line)) return line;
        let s = line.replace(HEADER_RE, (match, p1: string, p2: string, offset: number, fullStr: string) => {
          const lineStart = fullStr.lastIndexOf('\n', offset) + 1;
          const prefixBeforeHeader = fullStr.slice(lineStart, offset + p1.length);
          if (isOnlyLeadingDecorator(prefixBeforeHeader)) return match;
          return `${p1}\n\n${p2}`;
        });
        s = s.replace(HEADER_BEFORE_CIRCLED_RE, '$1\n$2');
        s = s.replace(PLAIN_HEADER_AFTER_PUNCT_RE, '$1\n\n$2');
        s = s.replace(STEP_HEADER_RE, (match, p1: string, p2: string, offset: number, fullStr: string) => {
          const lineStart = fullStr.lastIndexOf('\n', offset) + 1;
          const prefixBeforeHeader = fullStr.slice(lineStart, offset + p1.length);
          if (isOnlyLeadingDecorator(prefixBeforeHeader)) return match;
          return `${p1}\n\n${p2}`;
        });
        s = s.replace(/([。；;!?！？])[ \t]*([①②③④⑤⑥⑦⑧⑨⑩])/g, '$1\n$2');
        return s;
      });
      return processed.join('\n');
    })
    .join('');
}

/**
 * 8. 模型常输出用 ▼ 或 ↓ 串联的字符执行链路（例如「Hello.java | javac ▼Hello.class | 类加载...」）
 * 将粘连在同一行的符号与步骤自动拆分为结构清晰的垂直分步执行流。
 */
function normalizeInlineExecutionChains(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      if (!/[▼↓]/.test(segment)) return segment;
      return segment.replace(/\s*([▼↓])\s*/g, '\n\n$1\n\n');
    })
    .join('');
}

function escapeHtmlText(s: string): string {
  if (!s || (!s.includes('&') && !s.includes('<') && !s.includes('>') && !s.includes('"'))) {
    return s || '';
  }
  // 必须严格保护各类数学公式（$$...$$、\[...\]、\(...\)、$...$ 以及通过 isSafeMultilineMath 校验的跨行单 $）：
  // 公式内部的 < 和 > 是数学关系符（如 \varepsilon>0、0<|x-x_0|、T_n < 1/2），
  // 绝不可转为 &lt; 和 &gt;，否则 KaTeX 会抛出 Misplaced & 语法错误并退化为原始红色报错文本
  const mathPlaceholders: string[] = [];
  // 1. 先保护标准公式块、定界符公式及单行公式
  let masked = s.replace(
    /(\$\$[\s\S]+?\$\$|\\\[[\s\S]+?\\\]|\\\(.+?\\\)|\$(?!\$)[^$\n]+?\$(?!\$))/g,
    (m) => {
      const key = `%%%CHAT_ESC_MATH_${mathPlaceholders.length}%%%`;
      mathPlaceholders.push(m);
      return key;
    }
  );
  // 2. 保护满足安全特征校验的跨行单 $ 公式
  masked = masked.replace(/(?<!\$)\$(?!\$)([\s\S]+?)(?<!\$)\$(?!\$)/g, (match, formula: string) => {
    if (isSafeMultilineMath(formula)) {
      const key = `%%%CHAT_ESC_MATH_${mathPlaceholders.length}%%%`;
      mathPlaceholders.push(match);
      return key;
    }
    return match;
  });

  const escaped = masked
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

  return escaped.replace(/%%%CHAT_ESC_MATH_(\d+)%%%/g, (_m, idx) => mathPlaceholders[Number(idx)] ?? _m);
}

/**
 * 清除大模型在数学例题证明末尾常输出的 Q.E.D. 证毕方块符号（如 \square、$\square$、\blacksquare、□）
 * 这些符号在中文版面上常被误判为多余的孤立「口」字或乱码方框，影响排版整洁。
 */
function cleanProofQedSymbols(text: string): string {
  let s = text;
  // 1. 块级公式内部末尾的证毕方块（如「... = 0 . \qquad \square$$」），必须严格保留结尾的 $$ 分界符
  s = s.replace(/(?:\\qquad|\\quad|[ \t])+\\(?:square|blacksquare|qedsymbol|qed|Box)\s*(?=\$\$)/g, '');
  // 2. 行内公式内部末尾的证毕方块（如「... = 0 \square$」），必须严格保留结尾的 $
  s = s.replace(/(?:\\qquad|\\quad|[ \t])+\\(?:square|blacksquare|qedsymbol|qed|Box)\s*(?=\$)/g, '');
  // 3. 中英文标点符号后成对包裹的 $\square$、$$\square$$、独立 \square 或 □
  s = s.replace(
    /([。；;!?！？.\s])[ \t]*(?:\$(?:\\(?:square|blacksquare|qedsymbol|qed|Box)|□)\$|\$\$(?:\\(?:square|blacksquare|qedsymbol|qed|Box)|□)\$\$|\\(?:square|blacksquare|qedsymbol|qed|Box)|□)+(?=[ \t]*(?:\n|$|[)）】」』”"]))/gu,
    '$1'
  );
  // 4. 句末行尾的孤立 $\square$ 或 □
  s = s.replace(/(?<=[^\s])[ \t]*(?:\$(?:\\(?:square|blacksquare|qedsymbol|qed|Box)|□)\$|\\(?:square|blacksquare|qedsymbol|qed|Box)|□)[ \t]*$/gmu, '');
  return s;
}

/**
 * 以虚词/助动词/连词开头的短语（如「能独立执行」「并说清」「会判断」「不再是」），
 * 属于模型对句中动作的强调，不是结构化小标签，不得套用标签样式。
 */
const LEADING_FUNCTION_WORD_RE =
  /^(?:能|会|可|应|须|需|要|并|且|而|则|即|就|都|也|还|再|又|不|没|未|让|使|把|被)/u;

/**
 * 将正文中的 **粗体** 预转为 <strong>，规避 CommonMark 在中文/引号/空格混排时无法闭合 emphasis 的问题。
 * 精细化区分：
 * 1. 结构化引导词/字段标签（如 **为什么错：**、**正确做法：**、**易错点：**、- **表现**：等）：赋予 class="chat-md-label"，渲染为主色调蓝；
 * 2. 句中随文普通强调（如 **可能仍然存在**、**不确定**）：赋予 class="chat-md-bold"，保持自然深黑字重加粗，不染蓝色。
 */
export function repairChatBoldMarkers(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      return segment.replace(
        /(^|[\n\r]|[-*+•]\s*|[0-9]+\.\s*)?\*\*([^*\n]+?)\*\*([：:]?)/g,
        (match, prefix = '', inner: string, colon = '') => {
          const trimmed = inner.trim();
          if (!trimmed) return match;

          // 判断是否为结构化字段标签 / 引导小标头：
          // a. 粗体内或紧随其后带中英文冒号（如 **为什么错：**、**正确做法**：、**注意：**、**Step 1:**）
          // b. 格式为标签中括号（如 **【核心考点】**、**[重点]**）
          // c. 紧跟在行首或列表项开头且短小（长度 <= 8 且无句逗符号，如 - **易错点**）
          //    但以虚词/助动词开头的是「能独立执行」「并说清」这类动词短语，不是标签，不得染蓝加粗
          const hasColon = /[:：]$/.test(trimmed) || Boolean(colon);
          const isBracketTag = /^[【\[].+[】\]]$/.test(trimmed);
          const isLeadingShortTag =
            Boolean(prefix) &&
            trimmed.length <= 8 &&
            !/[，。！？；]/.test(trimmed) &&
            !LEADING_FUNCTION_WORD_RE.test(trimmed);
          // d. 紧跟在行首或列表项开头的小节编号/条目标题（如 **（1）时钟周期时间与时钟频率**、**① 变量**、**1. 结构**）
          // e. 结构化解题步骤标头（如 **第一步：求首项 \(a_1\)**、**第二步：寻找递推关系**、**Step 1:**）
          const isNumberedTag =
            Boolean(prefix) &&
            /^(?:第[一二三四五六七八九十\d]+步|步骤[一二三四五六七八九十\d]+|Step\s*\d+|解法[一二三四五六七八九十\d]+|方法[一二三四五六七八九十\d]+|[（(][\d一二三四五六七八九十a-zA-Z]+[）)]|[①-⑳]|\d{1,2}[、.．]|[一二三四五六七八九十]+[、.．])/u.test(trimmed) &&
            trimmed.length <= 40 &&
            !/[。！？；;]/.test(trimmed);

          const isLabel = hasColon || isBracketTag || isLeadingShortTag || isNumberedTag;
          const displayInner = escapeHtmlText(trimmed);

          if (isLabel) {
            if (colon) {
              return `${prefix}<strong class="chat-md-label">${displayInner}${colon}</strong>`;
            }
            return `${prefix}<strong class="chat-md-label">${displayInner}</strong>`;
          }

          if (colon) {
            return `${prefix}<strong>${displayInner}${colon}</strong>`;
          }
          return `${prefix}<strong>${displayInner}</strong>`;
        }
      );
    })
    .join('');
}

/**
 * 深度思考区：不展示 Mermaid/大段 graph 源码，避免黑色 CODE 块；保留字符树并拆行
 */
function stripHeavyDiagramsFromReasoning(text: string): string {
  return text.replace(/```([^\n]*)\n([\s\S]*?)```/g, (full, _langLine: string, body: string) => {
    const trimmed = body.trim();
    if (!trimmed) return full;
    if (looksLikeMermaid(trimmed)) {
      return '\n\n> 流程图与拓扑图谱已在下方正文中展示，思考过程不再重复铺陈源码。\n\n';
    }
    if (looksLikeAsciiKnowledgeTree(trimmed)) {
      return full;
    }
    if (trimmed.length > 280 && /(?:-->|flowchart|graph\s+(?:TD|LR|TB))/i.test(trimmed)) {
      return '\n\n> 流程图与拓扑图谱已在下方正文中展示，思考过程不再重复铺陈源码。\n\n';
    }
    return full;
  });
}

/** 深度思考专用预处理：不走 Mermaid 围栏转换，避免思考区出现大段 CODE */
export function normalizeReasoningMarkdown(raw: string): string {
  if (!raw) return '';

  let text = normalizeCodeFences(raw);
  text = text.replace(
    /(```(?:bash|sh|shell|zsh|powershell|cmd|java|python|csharp|cs|css|sql|json|yaml|xml|javascript|typescript|js|ts|text|markdown|md)\b)[ \t]*([#\/\u4e00-\u9fa5\d])/gi,
    '$1\n$2'
  );
  text = stripHeavyDiagramsFromReasoning(text);
  text = normalizeAsciiTreeInCodeFences(text);
  text = wrapBareAsciiTreeBlocks(text);
  text = normalizeInlineAsciiTree(text);
  text = normalizeCommandsAndCommentsInFences(text);

  text = text.replace(/^\s*#{1,3}\s*(?:回答|答案|解决方案)[:：]?\s*\n+/gi, '');
  text = normalizeGluedInlineHeadings(text);
  text = padMarkdownHeadingsOutsideCode(text);
  text = normalizeGluedHeadingLines(text);

  text = normalizeInlineNumberedLists(text);
  text = normalizeOrphanLeadingColons(text);
  text = normalizeInlineBulletLists(text);
  text = normalizeQuotedKeywordLines(text);
  text = normalizeTeachingHeaders(text);
  text = fixUnclosedCodeFences(text);

  return text;
}

/** 单行 compact Java 示例（Hello.java 一类），不含中文 */
const JAVA_ONE_LINER_RE =
  /^public\s+class\s+\w+\s*\{[^{}\n]*(?:\{[^{}\n]*\}[^{}\n]*)*\}$/;

/** 模型常把 Hello.java 写成无围栏的单行代码，仅包这一行，不碰后面中文正文 */
function wrapStandaloneJavaSnippets(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      return segment
        .split('\n')
        .map((line) => {
          const trimmed = line.trim();
          if (!JAVA_ONE_LINER_RE.test(trimmed)) return line;
          if (trimmed.length > 1500 || /[\u4e00-\u9fa5]/.test(trimmed)) return line;
          return `\`\`\`java\n${trimmed}\n\`\`\``;
        })
        .join('\n');
    })
    .join('');
}

/** 从 public class 起按大括号配平，返回结束行下标（含） */
function findJavaClassBlockEndLine(lines: string[], startIdx: number): number {
  let depth = 0;
  let started = false;
  for (let i = startIdx; i < lines.length; i++) {
    for (const ch of lines[i]) {
      if (ch === '{') {
        depth += 1;
        started = true;
      } else if (ch === '}') {
        depth -= 1;
      }
    }
    if (started && depth === 0) {
      return i;
    }
  }
  return -1;
}

const HASH_COMMENT_LANGS = new Set([
  '',
  'bash',
  'sh',
  'shell',
  'zsh',
  'powershell',
  'ps1',
  'cmd',
  'python',
  'py',
  'yaml',
  'yml',
  'toml',
  'ini',
  'conf',
  'dockerfile',
  'makefile',
  'mk',
  'ruby',
  'rb',
  'perl',
  'pl',
  'r',
  'text',
  'txt'
]);

function findCodeEndLineBeforeProse(lines: string[], isHashCommentLang = false): number {
  for (let i = 1; i < lines.length; i++) {
    const rawLine = lines[i];
    const line = rawLine.trim();
    // 在以 # 作为注释语法的语言（如 yaml/bash/python/sh/dockerfile 等）中，
    // 单 # 开头（# 注释）与缩进的 # 均为代码注释，绝不可误判为 Markdown 一级标题而截断代码块；
    // 仅当顶格出现 ## 及以上多级章节标题时才视为误吞的正文标题。
    if (isHashCommentLang) {
      if (/^#{2,6}\s+(?:[一二三四五六七八九十\d]+[、.．\s]|[\u4e00-\u9fa5]{2,})/.test(rawLine)) {
        return i - 1;
      }
    } else if (/^#{1,6}\s/.test(rawLine)) {
      return i - 1;
    }
    if (/^\d+[.、．]\s*(?:\*\*|[\u4e00-\u9fa5])/.test(line)) return i - 1;
    if (
      i > 1 &&
      lines[i - 1].trim() === '' &&
      /^[\u4e00-\u9fa5`“"‘']/.test(line) &&
      !/^(?:\/\/|\/\*|\*|#|--)/.test(line)
    ) {
      const cnCount = (line.match(/[\u4e00-\u9fa5]/g) || []).length;
      if (cnCount >= 4) return i - 1;
    }
  }
  return -1;
}

/**
 * 保护机制：若代码块内部在代码之后紧跟了 Markdown 标题（## ）、列表（1. **...**）或大段中文正文，
 * 说明模型漏写了闭合的 ```（或闭合围栏缺失），在代码与正文交界处自动补齐 ``` 闭合代码块，防止正文被吞入黑色代码框。
 */
function splitProseFromCodeBlocks(text: string): string {
  return text.replace(
    /(```[a-zA-Z0-9+#-]*\n)([\s\S]*?)(```|$)/g,
    (full, openFence: string, body: string, _closeFence: string) => {
      // 保护 ASCII 字符框线、树状图、流程框与板书区域：绝不可强行拆分
      if (
        /\+[-=]{3,}\+/.test(body) ||
        /^[ \t]*\+[-=]+[ \t]*$/m.test(body) ||
        /^[ \t]*[┌├└│─┼]/m.test(body) ||
        /【(?:演算区|错例对比区|代码时序区|板书|副板书|主板书|草稿区|板演区|代码区)】/.test(body)
      ) {
        return full;
      }
      const lines = body.split('\n');
      if (lines.length < 3) return full;

      const classStart = lines.findIndex((l) =>
        /^(?:public\s+)?(?:class|interface|enum)\s+\w+/.test(l.trim())
      );
      const classEnd = classStart >= 0 ? findJavaClassBlockEndLine(lines, classStart) : -1;
      // 边界必须取「第一个类结束行」与「正文起始边界」中更靠后者。
      // 教学示例常在同一代码块里连续写多个类（Animal → Dog → Test），只按第一个类裁剪，
      // 会把后面的类当成正文甩出代码框，表现为「首个类在框内、其余类裸露」；
      // 而后续代码里只要有一处中文注释（如 //输出 Dog），下方的中文判断即成立，误拆必然发生。
      const fenceLang = openFence.replace(/^```+/, '').trim().split(/\s+/)[0]?.toLowerCase() || '';
      const isHashCommentLang = HASH_COMMENT_LANGS.has(fenceLang);
      const proseBoundary = findCodeEndLineBeforeProse(lines, isHashCommentLang);
      const endIdx = Math.max(classEnd, proseBoundary);

      if (endIdx >= 0 && endIdx < lines.length - 1) {
        const remainingLines = lines.slice(endIdx + 1);
        const remainingText = remainingLines.join('\n').trim();
        const codeText = lines.slice(0, endIdx + 1).join('\n').trim();
        // 关键保护：若裁剪出来的「代码段」本身根本不具备真实代码特征（如仅有中文标语或板书小标题），绝不可强行切块！
        const codeLooksLikeCode =
          /(?:public|private|protected|static|final|abstract|class|interface|enum|record|def|function|import|from|return|const|let|var)\s+\w+/m.test(codeText) ||
          /(?:System\.out\.println|console\.log|print\(|SELECT\s+.*FROM|INSERT\s+INTO)/i.test(codeText) ||
          /^[ \t]*(?:if|for|while|switch)\s*\(.*?\)\s*\{/m.test(codeText) ||
          ((codeText.match(/[;=]\s*$/m) || codeText.includes('{')) && /[a-zA-Z0-9_]/.test(codeText));

        if (!codeLooksLikeCode && classStart < 0) {
          return full;
        }

        // 裁剪点之后若仍有类型声明，说明边界取得偏早：此时宁可不拆，也不能截断同一段代码
        const stillCode =
          /^(?:\s*(?:public|private|protected|static|final|abstract)\s+)*(?:class|interface|enum|record)\s+\w+/m.test(
            remainingText
          );
        if (
          !stillCode &&
          (/[\u4e00-\u9fa5]/.test(remainingText) || /^#{1,6}\s/m.test(remainingText))
        ) {
          const code = lines.slice(0, endIdx + 1).join('\n').trimEnd();
          const prose = remainingLines.join('\n').trimStart();
          return `${openFence}${code}\n\`\`\`\n\n${prose}\n\n`;
        }
      }

      return full;
    }
  );
}

function closeFenceAfterJavaLine(head: string, openFence: string, body: string): string | null {
  const lines = body.split('\n');
  const classStart = lines.findIndex((l) => /^public\s+class\s+\w+/.test(l.trim()));
  if (classStart >= 0) {
    const endIdx = findJavaClassBlockEndLine(lines, classStart);
    if (endIdx >= 0) {
      const javaBlock = lines.slice(classStart, endIdx + 1).join('\n');
      const before = lines.slice(0, classStart).join('\n');
      const after = lines.slice(endIdx + 1).join('\n');
      const prefix = before ? `${before}\n` : '';
      return `${head}${prefix}${openFence}${javaBlock}\n\`\`\`\n${after}`;
    }
  }

  const oneLinerIdx = lines.findIndex((l) => JAVA_ONE_LINER_RE.test(l.trim()));
  if (oneLinerIdx >= 0) {
    const before = lines.slice(0, oneLinerIdx).join('\n');
    const javaLine = lines[oneLinerIdx].trim();
    const after = lines.slice(oneLinerIdx + 1).join('\n');
    const prefix = before ? `${before}\n` : '';
    const fence = /java/i.test(openFence) ? openFence : '```java\n';
    return `${head}${prefix}${fence}${javaLine}\n\`\`\`\n${after}`;
  }

  return null;
}

/** 行内粘连的标题，如「运行链路##5.1编写」或「洛必达法则###1.法则内容」 */
function normalizeGluedInlineHeadings(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      return segment.replace(/([^\s#\n])(#{2,6})[ \t]*(?=[^\s#\n])/g, '$1\n\n$2 ');
    })
    .join('');
}

/**
 * 仅在代码围栏外部为行首未留空格的 Markdown 标题补充空格（如「##一、」->「## 一、」）。
 * 关键防护：绝不污染代码块内部的内容，避免将 Bash 脚本的 Shebang 行（#!/bin/bash）误插空格破坏为「# !/」。
 */
function padMarkdownHeadingsOutsideCode(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      return segment.replace(
        /(^|\n)(#{1,6})(?!(?:!|include|define|ifndef|ifdef|endif|pragma|undef|error|region|endregion)\b)([^\s#\n])/g,
        '$1$2 $3'
      );
    })
    .join('');
}

/**
 * 拆分标题行粘连的无序列表、有序列表或紧随其后的正文叙述。
 * 例如：
 * 1. 标题粘连无序列表：
 *    「##八、易错点提醒- **不是未定式不能用洛必达**；」 -> 「## 八、易错点提醒\n\n- **不是未定式不能用洛必达**；」
 *    「## 重点归纳：- 极限运算」 -> 「## 重点归纳\n\n- 极限运算」
 * 2. 标题粘连有序列表：
 *    「### 2. 使用步骤1. 先代入…」 -> 「### 2. 使用步骤\n\n1. 先代入…」
 * 3. 标题粘连正文起笔：
 *    「### 1. 法则内容若\[\lim…」 -> 「### 1. 法则内容\n\n若\[\lim…」
 *    「## 四、洛必达法则的注意事项洛必达法则很强，但不是万能的。」 -> 「## 四、洛必达法则的注意事项\n\n洛必达法则很强，但不是万能的。」
 *
 * 严格保护：
 * - 括号内副标题（如「### 题目二（数列极限 - 四则运算）」）
 * - 普通副标题连字符（如「## 模块一 - 基础篇」）
 */
function normalizeGluedHeadingLines(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const processed: string[] = [];

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const m = line.match(/^([ \t]*#{1,6}[ \t]+)([^\n]+)$/);
        if (!m) {
          processed.push(line);
          continue;
        }

        const marker = m[1].trim();
        const content = m[2];

        const formatHeadingTitle = (t: string) =>
          t
            .trim()
            .replace(/[：:]$/, '')
            .replace(/^(\d{1,2}(?:\.\d+)*\.?)(?=[\u4e00-\u9fa5])/u, '$1 ');

        // 1. 标题行末尾粘连无序列表（如「##八、易错点提醒- **不是未定式…」或「## 重点：- 极限…」）
        const bulletMatch = content.match(
          /(?:([：:][ \t]*)|(?<!-)([^\s\-（(【])([ \t]*))(-[ \t]*\*\*|-[ \t]+(?=[\p{Extended_Pictographic}\u4e00-\u9fa5A-Za-z0-9]))/u
        );
        if (bulletMatch && bulletMatch.index !== undefined) {
          const splitPos = bulletMatch.index + (bulletMatch[1] ? 0 : (bulletMatch[2] ? bulletMatch[2].length : 0));
          const beforeStr = content.slice(0, splitPos);
          if (!hasUnclosedBrackets(beforeStr)) {
            const headingText = formatHeadingTitle(beforeStr);
            const listPart = content.slice(
              bulletMatch.index +
              (bulletMatch[1] ? bulletMatch[1].length : (bulletMatch[2]?.length || 0) + (bulletMatch[3]?.length || 0))
            ).trim();

            if (headingText.length >= 2 && headingText.length <= 40) {
              const isBoldList = /^-[ \t]*\*\*/.test(listPart);
              const hasColon = Boolean(bulletMatch[1]);
              const noSpaceBefore = !bulletMatch[3];
              const hasListPunct = /[；;]$/.test(listPart) || /[。！!]$/.test(listPart);

              if (
                isBoldList ||
                hasColon ||
                (noSpaceBefore && /^[一二三四五六七八九十\d]/.test(headingText)) ||
                hasListPunct
              ) {
                const normalizedList = listPart.replace(/^-[ \t]*/, '- ');
                processed.push(`${marker} ${headingText}\n\n${normalizedList}`);
                continue;
              }
            }
          }
        }

        // 2. 标题行末尾粘连有序列表序号（如「### 2. 使用步骤1. 先代入…」或「## 核心步骤1. 先求导…」或「七、变式训练1.求…」）
        const numberMatch = content.match(
          /([\u4e00-\u9fa5A-Za-z）)】])[ \t]*([1-9]\.[ \t]*(?=[\p{Extended_Pictographic}\u4e00-\u9fa5\w*（(「『【$\\]))/u
        );
        if (numberMatch && numberMatch.index !== undefined) {
          const splitPos = numberMatch.index + numberMatch[1].length;
          const beforeStr = content.slice(0, splitPos);
          if (!hasUnclosedBrackets(beforeStr)) {
            const headingText = formatHeadingTitle(beforeStr);
            const listPart = content.slice(splitPos).trim();
            if (headingText.length >= 2 && headingText.length <= 40) {
              processed.push(`${marker} ${headingText}\n\n${listPart}`);
              continue;
            }
          }
        }

        // 2b. 标题行整行末尾以「1.」结尾（如「## 七、变式训练1.」），其后另起一行写题目「求...」
        // 注意：不可将 1. 独立放在空行输出，否则 Markdown 会将其渲染为空的孤立列表项；应拼接到下一行题干开头
        const trailingNumberMatch = content.match(/^(.+?[\u4e00-\u9fa5A-Za-z）)】])[ \t]*([1-9]\.)\s*$/);
        if (trailingNumberMatch) {
          const headingText = formatHeadingTitle(trailingNumberMatch[1]);
          if (headingText.length >= 2 && headingText.length <= 40 && !hasUnclosedBrackets(headingText)) {
            const numPrefix = `${trailingNumberMatch[2]} `;
            let attached = false;
            for (let j = i + 1; j < lines.length; j++) {
              if (lines[j].trim().length > 0) {
                lines[j] = `${numPrefix}${lines[j].trimStart()}`;
                attached = true;
                break;
              }
            }
            if (attached) {
              processed.push(`${marker} ${headingText}`);
            } else {
              processed.push(`${marker} ${headingText}\n\n${numPrefix}`);
            }
            continue;
          }
        }

        // 2c. 标题行后直接粘连正文引导叙述（如「###1.求数列 \(\{a_n\}\)的通项公式已知 \(4S_n = (a_n +1)^2\)①...」或「### 1. 法则内容若\[\lim…」）
        // 关键防护：必须完整 mask 掉所有 LaTeX 块级与行内数学公式（$$...$$、\[...\]、\(...\)、$...$），确保公式内部字符不干扰正向预查定位
        const maskedContent = content
          .replace(/\$\$[\s\S]+?\$\$|\\\[[\s\S]+?\\\]/g, (m) => ' '.repeat(m.length))
          .replace(/\\\([\s\S]+?\\\)|\$(?!\$)[^$\n]+?(?<!\$)\$(?!\$)/g, (m) => ' '.repeat(m.length));

        const PROSE_LEAD_RE =
          /(?=若|如果|当(?!前|代|今|时|下|然|且|中|量|地|场|面|事|作|成|选)|设(?!计|置|定|备|施|想|立)|已知|已知条件[为是]?|题设|由已知|由题意|根据题意|除了|遇到|可以按|例如|比如|注意[，,：:]|求[$\\ \t\d]|在计算|在一定条件下|学习本[节课章讲]|要学习|首先|先看|先来看|下面|下表|如下|如下表|以下|无论|不论|不管是|动手|动笔|我们在|我们|这里|此时|这是|这一点|这属于|这一步|这一阶段|这[个段套些份篇门组种]|(?:该|本|此)[配置代码方案项目段份]|循环[体中里]|方法[体中里]|代码[层中里]|底层[上中]|本质[上为]|原因[是为]|根因[是为]|(?<=[\u4e00-\u9fa5])([A-Z][a-zA-Z0-9_.-]+(?:本身|是|在|有|可以|采用|推荐|使用|支持|不支持))|\*\*(?:第[一二三四五六七八九十\d]+步|步骤[一二三四五六七八九十\d]+|Step\s*\d+))/gu;

        let splitByProseLead = false;
        for (const leadMatch of maskedContent.matchAll(PROSE_LEAD_RE)) {
          const splitIdx = leadMatch.index ?? -1;
          if (splitIdx < 2 || splitIdx > 68) continue;
          const rawPrefix = maskedContent.slice(0, splitIdx);
          if (
            !/^(?:[一二三四五六七八九十]+[、．.]|\d+\.|\d+\.\d+|例\d+|变式)?\s*[\u4e00-\u9fa5A-Za-z0-9_\\()[\]{}（）\s，,：:、-]{2,60}$/u.test(
              rawPrefix
            )
          ) {
            continue;
          }

          const trimmedRawPrefix = rawPrefix.trimEnd();
          const headingEnd = trimmedRawPrefix.length;
          const headingText = formatHeadingTitle(content.slice(0, headingEnd));
          // 关键防护 1：必须确保标题剥离序号后仍有实质标题文本（>= 2 字），绝不能把孤立序号（如「## 二、」）误当成完整标题并把后续标题文字当成正文剥离
          const titleWithoutPrefix = headingText
            .replace(/^(?:[一二三四五六七八九十\d]+[、．.）)]|第[一二三四五六七八九十\d]+[章节讲部分篇]|（?[一二三四五六七八九十\d]+）|例\d+|变式\d*)[ \t]*/, '')
            .trim();
          if (headingText.length < 2 || titleWithoutPrefix.length < 2 || hasUnclosedBrackets(headingText)) {
            continue;
          }

          // 关键防护 2：若切分点左侧以未完结的及物动词、介词、疑问词或定语助词结尾（如「理解」「剖析」「为什么」「关于」「的」「线上故障」「看」「探究」「中」「下」），
          // 说明后面紧跟的代词/名词属于标题宾语或中心语，绝不可在此处腰斩标题，继续向后查找真正的正文起点
          if (
            /(?:理解|剖析|掌握|熟悉|了解|分析|认识|探讨|详解|介绍|对比|区分|关于|为什么|为何|如何|怎么|怎样|哪些|什么|在|对|从|把|将|与|和|及|的|线上|故障|常见|性能|优化|看|探究|探讨|研究|下|中|内|(?:本质|底层|基础)上)$/.test(
              titleWithoutPrefix
            )
          ) {
            continue;
          }

          let prosePart = content.slice(headingEnd).trim();
          if (!prosePart) continue;

          // 关键防护 2.5：若切分点左侧紧邻冒号（如「## 二、系统架构：当流量突增时如何优雅降级」「## 三、实战演练：动手实现一个简易RPC框架」「## 二、极值问题：求 $f(x)$ 的最大值」），
          // 且冒号右侧不含句号/问号/感叹号/分号且不以冒号收尾，说明是标准的「主标题：副标题」结构，绝不可从冒号处切断副标题！
          if (
            /[：:]$/.test(trimmedRawPrefix) &&
            !/[。！？；;]/.test(prosePart) &&
            !/[：:]\s*$/.test(prosePart)
          ) {
            continue;
          }

          // 关键防护 3：若后半段属于典型标题定语/中心语短语且不含句末陈述标点，属于完整标题的一部分，绝不可切分
          if (!/[，,。！？；;]/.test(prosePart) && !/[：:]\s*$/.test(prosePart)) {
            if (
              /^(?:排查|优化|方法|实践|生产|业务|实现|设计|配置|参数)[中时体里](?:常见|需要|常犯|容易|核心|关键|主要|基本|典型)/.test(
                prosePart
              ) ||
              /^在[\u4e00-\u9fa5A-Za-z0-9_]+(?:中|下|里|上)的[\u4e00-\u9fa5A-Za-z0-9_]+$/.test(prosePart) ||
              /^(?:学习|用户|开发者?|程序)(?:路线|目标|方法|指南|重点|建议|路径|计划|总结|笔记|内容|要求|阶段|认证|授权|隔离|管理|权限|中心|画像|体验|交互|手册|规范|设计|机制|原理|架构|模型|方案|实现)/.test(
                prosePart
              ) ||
              /^[A-Z][a-zA-Z0-9_.-]*(?:采用|推荐|使用|支持|不支持)[^，,。！？；;：:]+的(?:原因|原理|机制|方法|策略|方案|设计|缓存|模型|优势|意义|作用|区别|对比|过程|流程|分析|详解|总结|实践)$/.test(
                prosePart
              )
            ) {
              continue;
            }
          }

          // 关键防护 4：强题设/步骤引导词（若/设/已知/题设/求+公式/步骤标头等）可直接确认为正文；
          // 其余引导词必须满足包含真实句中逗号、句末标点（，。！？；）或句尾引导冒号（...：）才允许切分，
          // 避免把含副标题冒号（主标题：副标题）或无标点的正常长标题误腰斩
          const isStrongProseLead =
            /^(?:若|设(?!计|置|定|备|施|想|立)|已知|题设|由已知|由题意|根据题意|求[\\$]|注意[，,：:]|\*\*(?:第[一二三四五六七八九十\d]+步|步骤[一二三四五六七八九十\d]+|Step\s*\d+))/u.test(
              prosePart
            );
          const hasSentencePunctuation = /[，,。！？；;]/.test(prosePart) || /[：:]\s*$/.test(prosePart);
          const proseLooksLikeSentence = isStrongProseLead || hasSentencePunctuation;

          if (proseLooksLikeSentence) {
            // 若正文段落内部紧随题设又粘连了解题步骤小标头（如「已知...①**第一步：...**」），将其进一步拆分成独立段落
            prosePart = prosePart.replace(
              /([^\n\s#])\s*(\*\*(?:第[一二三四五六七八九十\d]+步|步骤[一二三四五六七八九十\d]+|Step\s*\d+|解法[一二三四五六七八九十\d]+|方法[一二三四五六七八九十\d]+)[:：\s][^*\n]+?\*\*)/gu,
              '$1\n\n$2'
            );
            processed.push(`${marker} ${headingText}\n\n${prosePart}`);
            splitByProseLead = true;
            break;
          }
        }
        if (splitByProseLead) {
          continue;
        }

        // 2d. 标题行末尾粘连粗体引导词/加粗小标头（如「### 2.1三个基本物理量**（1）时钟周期时间与时钟频率**」或「## 二、重点归纳**1. 概念**」）
        // 关键防护：剥离装饰性 Emoji 后实质标题文本必须 >= 2 字，防止把「### 📌 **结论**」误判为标题末尾粘连而拆成两行
        const boldMatch = content.match(
          /^([ \t]*[^\s*#\n][^\n*]*?)[ \t]*(\*\*[^*\n]+?\*\*.*)$/
        );
        if (boldMatch && boldMatch[1] && boldMatch[2]) {
          const beforeStr = boldMatch[1].trim();
          const substantiveBefore = beforeStr.replace(
            /(?:[\p{Extended_Pictographic}\u{2600}-\u{27BF}]|\u{FE0F}|\u{200D}|\s)/gu,
            ''
          );
          if (substantiveBefore.length === 0) {
            // 纯 Emoji 前缀 + 单个加粗标题（如「### 📌 **结论**」）：去掉冗余加粗保留完整标题语义
            const singleBold = boldMatch[2].trim().match(/^\*\*([^*\n]+?)\*\*$/);
            if (singleBold) {
              processed.push(`${marker} ${beforeStr} ${singleBold[1].trim()}`);
              continue;
            }
          } else if (
            beforeStr.length >= 2 &&
            beforeStr.length <= 60 &&
            !hasUnclosedBrackets(beforeStr) &&
            !/^[*_~]/.test(beforeStr)
          ) {
            const headingText = formatHeadingTitle(beforeStr);
            const boldPart = boldMatch[2].trim();
            processed.push(`${marker} ${headingText}\n\n${boldPart}`);
            continue;
          }
        }

        // 4. 标题以经典收尾词（注意事项/常见误区/解题步骤等）结尾，后面紧贴未换行的正文
        const closerMatch = content.match(
          /^((?:[一二三四五六七八九十]+[、．.]|\d+\.|\d+\.\d+|例\d+|变式)?\s*[\u4e00-\u9fa5A-Za-z0-9_]{1,25}?(?:常见基本语法错误与修正|基本语法错误与修正|语法错误与修正|创建、保存与运行|基本书写结构|安装与启动|安装与配置|下载与安装|配置与使用|创建与运行|基本数据类型|数据类型|基本组成|基本结构|基本语法|基本概念|基本原理|核心机制|核心概念|工作原理|运行机制|命名规则|组成规则|运算规则|转换规则|定义规则|使用规则|书写规范|命名规范|编码规范|环境搭建|输入输出|常见错误|错误辨析|注意事项|常见误区|核心考点|定义与性质|解题步骤|解题总策略|学习目标|归纳总结|组合示例|应用示例|使用示例|课堂小结|优先级))([^\n]+)$/u
        );
        if (closerMatch && closerMatch[1] && closerMatch[2]?.trim()) {
          const headingText = formatHeadingTitle(closerMatch[1]);
          let prosePart = closerMatch[2].trim();

          // 若紧随收尾词之后是括号说明（如「### 2.联系向关系模式的转换规则（本讲第四难点）」）：
          // 整个括号属于标题的限定性副标题/备注，整行属于合法完整标题，绝不可切为正文！
          const isParenthetical = /^[（(【「『\[][^）)】」』\]]+[）)】」』\]]$/.test(prosePart);
          if (isParenthetical) {
            processed.push(`${marker} ${formatHeadingTitle(content)}`);
            continue;
          }

          // 如果 prosePart 开头带括号说明（如「### 2.联系向关系模式的转换规则（本讲第四难点）在关系数据库中...」），将括号说明并入标题侧
          let effectiveHeadingText = headingText;
          const parenMatch = prosePart.match(/^([（(【「『\[][^）)】」』\]]+[）)】」』\]])[ \t]*(.*)$/);
          if (parenMatch) {
            effectiveHeadingText = formatHeadingTitle(`${closerMatch[1]}${parenMatch[1]}`);
            prosePart = parenMatch[2].trim();
            if (!prosePart) {
              processed.push(`${marker} ${effectiveHeadingText}`);
              continue;
            }
          }

          // 若紧随收尾词之后是冒号（如「## 三、Java编码规范：让工程可读、可维护」）：
          // 冒号后若为简短副标题短语（无句末标点，<= 22字），整行属于合法「主标题：副标题」排版，绝不可切为正文！
          const colonMatch = prosePart.match(/^[：:]\s*(.*)$/);
          if (colonMatch) {
            const tail = colonMatch[1].trim();
            const isSentenceProse =
              /[。！？]/.test(tail) ||
              /^(?:本[课节章节讲篇部分模块]|设(?!计|置|定|备|施|想|立)|若|当|如果|主要[包围绕]|围绕|核心[任务是要]|旨在|学习|首先|初学者|新手|读者|用户|程序[员员]|开发[者者]|下面|下表|如下|以下|一个|一种|无论|不论|根据|按照|通常|日常|实际|在)/.test(tail);
            if (!isSentenceProse && tail.length <= 22) {
              processed.push(line);
              continue;
            }
            prosePart = tail;
          }

          // 正文部分必须具备真正的陈述句特征（含标点、以正文词起笔、或长度足够），防止误截复合标题短语
          const isRealProse =
            /[，,。！？；;：:]/.test(prosePart) ||
            prosePart.length >= 8 ||
            /^(?:表示|用来表示|用于表示|用|用来|使用|采用|只有|是|为|指|由|在|负责|主要|可以|将|通过|用于|自带|提供|基于|针对|作为|属于|包括|它|这|此|该|一个|一种|一条|无论|不论|学习|初学者)/.test(prosePart);
          if (isRealProse && effectiveHeadingText.length >= 2 && !hasUnclosedBrackets(effectiveHeadingText)) {
            processed.push(`${marker} ${effectiveHeadingText}\n\n${prosePart}`);
            continue;
          }
        }

        processed.push(line);
      }

      return processed.join('\n');
    })
    .join('');
}

/** 去掉模型复读、未闭合的围栏行，避免页面上直接露出 ``` */
function fixUnclosedCodeFences(text: string): string {
  const count = (text.match(/```/g) || []).length;
  if (count % 2 === 0) return text;

  const lastIdx = text.lastIndexOf('```');
  const head = text.slice(0, lastIdx);
  const tailFromFence = text.slice(lastIdx);
  const openMatch = tailFromFence.match(/^```([^\n]*)\n?/);
  if (!openMatch) {
    return head + tailFromFence.replace(/^```+/, '');
  }

  const body = tailFromFence.slice(openMatch[0].length);

  const javaClosed = closeFenceAfterJavaLine(head, openMatch[0], body);
  if (javaClosed) return javaClosed;

  const chineseChars = (body.match(/[\u4e00-\u9fa5]/g) || []).length;

  if (chineseChars > 60 || /^#{1,6}\s/m.test(body) || /^\s*\d+[.、．]\s/m.test(body)) {
    return head + body;
  }

  const blankBreak = body.search(/\n\s*\n/);
  if (blankBreak > 0 && blankBreak < 2000) {
    return `${head}${openMatch[0]}${body.slice(0, blankBreak)}\n\`\`\`\n${body.slice(blankBreak)}`;
  }

  if (body.length < 1200 && chineseChars < 30) {
    return `${text}\n\`\`\``;
  }

  return head + body;
}

function tidyStrayFenceMarkers(text: string): string {
  let s = text;
  // 1) 带语言标识的连续重复开围栏（如 ```java\n```java\n）直接折叠为一个
  s = s.replace(/(```[a-zA-Z0-9+#-]+)[ \t]*\n[ \t]*\1[ \t]*\n/g, '$1\n');
  // 2) 无语言标识的连续 ```\n```，仅在全文围栏总数为奇数（存在多余未配对围栏）时折叠一次，
  //    严禁在全文围栏已成对（偶数）时把空代码块或嵌套示例结尾的 ```\n``` 吞成奇数
  if (((s.match(/```/g) || []).length) % 2 === 1) {
    s = s.replace(/(```)[ \t]*\n[ \t]*\1[ \t]*\n/, '$1\n');
  }
  s = s.replace(/^[ \t]*`[ \t]*`[ \t]*$/gm, '');
  return s
    .split('\n')
    .filter((line) => {
      const t = line.trim();
      if (t === '`' || t === '``') return false;
      return true;
    })
    .join('\n');
}

/** 围栏未配对时 markdown-it 会把 ```java 渲染成普通段落，需从 HTML 剔除 */
function stripOrphanFenceParagraphs(html: string): string {
  if (!html) return '';
  return html
    .replace(/<p>\s*```[a-zA-Z0-9+#-]*\s*<\/p>/gi, '')
    .replace(/<p>\s*```\s*<\/p>/gi, '')
    .replace(/<p>\s*`{1,2}\s*<\/p>/gi, '');
}

/**
 * 针对大模型流式输出与中文混排的专用 Markdown 预处理器（Code Compass 同款）
 */
export function normalizeChatMarkdown(raw: string): string {
  if (!raw) return '';

  // 1. 先解包可能包裹整篇文档的外层 ```markdown / ````markdown / `````markdown 围栏
  const unwrappedRaw = unwrapProseCodeFences(raw);

  // 2. 保护短小的包含内嵌代码围栏（```lang...```）的 ```markdown 语法示例块，避免内部围栏将外层拆碎，
  //    并在末尾升级为 CommonMark 规范的 4 反引号外框（````markdown ... ````）使 markdown-it 完整渲染外框与内部代码
  const nestedMdBlocks: string[] = [];
  const rawWithProtectedNestedMd = unwrappedRaw.replace(
    /(^|\n)([ \t]*)`{3,4}(markdown|md)\b([^\n]*)\n((?:(?!`{3,})[\s\S])*?\n[ \t]*```[a-zA-Z0-9_#+-]+[^\n]*\n(?:(?!`{3,})[\s\S])*?\n[ \t]*```)[ \t]*\n([ \t]*)`{3,4}(?=[ \t]*(?:\n|$))/gim,
    (_m, prefix: string, indent: string, lang: string, info: string, innerBody: string, closeIndent: string) => {
      const idx = nestedMdBlocks.length;
      nestedMdBlocks.push(`${indent}\`\`\`\`${lang}${info}\n${innerBody}\n${closeIndent}\`\`\`\``);
      return `${prefix}%%%NESTED_MD_FENCE_${idx}%%%`;
    }
  );

  let text = normalizeCodeFences(rawWithProtectedNestedMd);
  text = text.replace(/```text([A-Za-z\u4e00-\u9fa5])/gi, '```text\n$1');
  // 优先规范化所有代码块语言标记与首行代码/注释粘连（如 ```bash# 1.xxx -> ```bash\n# 1.xxx、```pythonprint -> ```python\nprint）
  text = text.replace(
    /(```(?:bash|sh|shell|zsh|powershell|cmd|java|python|csharp|cs|css|sql|json|yaml|xml|javascript|typescript|js|ts|text|markdown|md)\b)[ \t]*([^\s])/gi,
    '$1\n$2'
  );
  text = unwrapProseCodeFences(text);
  text = splitProseFromCodeBlocks(text);
  text = wrapStandaloneJavaSnippets(text);
  text = tidyStrayFenceMarkers(text);
  text = normalizeMermaidInCodeFences(text);
  text = normalizeAsciiTreeInCodeFences(text);
  text = normalizeBlackboardLayoutInFences(text);
  text = wrapBareAsciiTreeBlocks(text);
  text = normalizeInlineAsciiTree(text);
  text = normalizeCommandsAndCommentsInFences(text);

  text = text.replace(/^\s*#{1,3}\s*(?:回答|答案|解决方案)[:：]?\s*\n+/gi, '');
  text = text.replace(/^\s*#{1,3}\s*((?:针对)?您关于)/, '$1');
  // 清理大模型自作多情输出的孤立冗余标题（如 EduMind学情评语 等）
  text = text.replace(/^\s*#{0,4}\s*(?:EduMind\s*)?(?:学情评语|学情诊断|AI\s*评阅说明|AI\s*评阅建议)[:：]?\s*\n+/gi, '');
  // 折叠 3 个以上的连续换行符，避免渲染出极其刺眼的大块空白
  text = text.replace(/\n{3,}/g, '\n\n');
  text = normalizeGluedInlineHeadings(text);
  text = padMarkdownHeadingsOutsideCode(text);
  text = normalizeGluedHeadingLines(text);
  // 清除例题证明末尾多余的 Q.E.D. 方块符号（如 \square、$\square$、□），避免界面出现突兀的孤立「口」字
  text = cleanProofQedSymbols(text);

  // 只消除「加粗标记内侧」的水平空白：\s 会跨行匹配，把上一行的 **分组标题** 与下一行的
  // "- **列表项**" 误配成一对加粗，导致换行与列表标记一起被吞进加粗文本
  text = text.replace(/\*\*[ \t]+([^*\n]+?)[ \t]+\*\*/g, '**$1**');
  text = text.replace(/\*\*[ \t]+([^*\n]+?)\*\*/g, '**$1**');
  text = text.replace(/\*\*([^*\n]+?)[ \t]+\*\*/g, '**$1**');
  // 仅消除横向空格与制表符，避免误吞换行符 \n
  text = text.replace(/\*\*([^*\n]+?)\*\*[ \t]+(?=[\u4e00-\u9fa5（(「『【])/g, '**$1**');

  // 3. 修复模型漏写闭合 $ 符号直接接中文标点/说明文字的问题
  text = repairUnclosedMathBeforeChinesePunct(text);

  // 4. 同一行内粘连的「1.xxx 2.xxx 3.xxx」拆成 Markdown 有序列表（CommonMark 要求每条独占一行）
  text = normalizeInlineNumberedLists(text);
  // 合并单独成行的数字序号（防止「1.」后换行导致空列表项与题干正文剥离）
  text = text.replace(/(^|\n)([ \t]*\d{1,2}\.[ \t]*)\n+([^\n\s#])/g, '$1$2$3');
  // 4.5. 修复排版断行造成的行首悬挂冒号（如「• 结点的度\n: 该结点拥有的子树个数。」）
  text = normalizeOrphanLeadingColons(text);
  // 4.6. 列表项词条与顶格定义被断成两行时合并回同一列表项（如「- **实体**\n客观存在并可区分…」）
  text = normalizeListItemDefinitions(text);
  // 5. 同一行内粘连的「-项1 … -项2 …」拆成无序列表
  text = normalizeInlineBulletLists(text);
  // 6. 关键词引号 "" 粘连拆行
  text = normalizeQuotedKeywordLines(text);
  // 7. 教学与问答高频小标题（如「**参考答案：**」「**解析：**」「【题目】」等）粘连自动拆行
  text = normalizeTeachingHeaders(text);
  // 8. 执行链路（如用 ▼ 或 ↓ 串联的步骤）粘连自动拆分
  text = normalizeInlineExecutionChains(text);
  // 9. 数学公式后紧贴推导过渡词（如「化简得：」「整理得：」「因式分解得：」「即」）粘连自动拆行
  text = normalizeGluedMathDerivationSteps(text);

  text = fixUnclosedCodeFences(text);

  // 补齐末尾流式未闭合的单个 $ 符号（仅在代码围栏、表格内嵌代码块与行内代码外部的 Markdown 正文中，$ 数量为奇数且末尾不是闭合代码围栏时补在末尾）
  const proseForDollar = text
    .replace(FENCED_CODE_BLOCK_RE, '')
    .replace(/<div class="chat-table-code-block">[\s\S]*?<\/pre><\/div>/g, '')
    .replace(/`[^`\n]+`/g, '');
  const dollarMatches = proseForDollar.replace(/\$\$/g, '').match(/(?<!\\)\$/g);
  if (dollarMatches && dollarMatches.length % 2 !== 0 && !/```\s*$/.test(text)) {
    // 关键防护 1：仅当最后一个非转义 $ 处于当前文本末尾行/尾部段落（距离文末不超过 120 字符且不跨空行 \n\n）时才考虑补齐，
    // 严禁在长文中段包含孤立 $ 时直接在文档末尾追加 $ 产生多余乱码符号
    const lastDollarIdx = proseForDollar.lastIndexOf('$');
    const tailAfterLastDollar = proseForDollar.slice(lastDollarIdx + 1);
    const isAtTrailingProse = !tailAfterLastDollar.includes('\n\n') && tailAfterLastDollar.length <= 120;

    // 关键防护 2：货币金额里的 $ 不是公式定界符（如「价格为 $5」「费用 $12.5 元」「预算在 $5左右」「花了 $100买的」）
    const isCurrencyAmount =
      /^\d[\d,]*(?:\.\d+)?(?:[ \t]*(?:[kKmMbBwW]|元|美元|万元|块|刀|美金|RMB|CNY|USD))?(?:[ \t]*[\u4e00-\u9fa5]|[ \t]*(?:\/|per)[ \t]*(?:mo|yr|month|year|人|天|月|年)|[，。；、！？：,.;!?）)\]】」』/~-]|$)/i.test(
        tailAfterLastDollar
      ) && !/^\d[\d,]*(?:\.\d+)?[ \t]+[=+\-*\\<>~≤≥≠]/.test(tailAfterLastDollar);

    // 关键防护 2.5：正文中未加反引号的编程变量/API 标识符（如 $emit、$refs、$HOME）且后续无数学运算符，绝不误补 $
    const isProseCodeVar =
      /^[a-zA-Z_][a-zA-Z0-9_]+(?=[ \t\u4e00-\u9fa5，。；、！？：,.;!?:）)\]】」』]|$)/.test(tailAfterLastDollar) &&
      !/[=+\-*/^_\\{}<>]/.test(tailAfterLastDollar);

    // 关键防护 3：未闭合内容必须具有数学公式特征（含有 LaTeX 指令、运算符、变量等），非纯自然语言
    const looksLikeMathFormula =
      /\\[a-zA-Z]+|[=+\-*/^_{}\[\]()><~]|\b[a-zA-Z]\b/.test(tailAfterLastDollar) ||
      /^[ \t]*[a-zA-Z0-9_.\s]+$/.test(tailAfterLastDollar);

    if (isAtTrailingProse && !isCurrencyAmount && !isProseCodeVar && looksLikeMathFormula) {
      text += '$';
    }
  }

  nestedMdBlocks.forEach((block, idx) => {
    text = text.replaceAll(`%%%NESTED_MD_FENCE_${idx}%%%`, () => block);
  });

  return text;
}

/**
 * 拆分大模型输出中数学公式之后紧贴的推导过渡词（如「化简得：」「整理得：」「因式分解得：」「即」）
 * 解决例如「即 $4a_n = ...$化简得：$a_n^2 - ... = 0$因式分解得：...」这类严重影响排版与可读性的粘连挤压，
 * 并将方程带圈编号（①-⑩）后紧贴的操作推导句（如「...^2)②用①式减去②式得：」）拆分为独立新行。
 */
export function normalizeGluedMathDerivationSteps(text: string): string {
  if (!text) return '';
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      // 1. 公式（含带圈方程编号 ①-⑩）后紧贴推导操作句（如「...^2)②用①式减去②式得：」或「...^2)①代入②式得：」）
      const equationOpRe =
        /(\$|\\\)|(?:\$\$|\\\]))[ \t]*([①②③④⑤⑥⑦⑧⑨⑩]?)[ \t]*((?:用[①②③④⑤⑥⑦⑧⑨⑩\d]+式(?:减去|加上|除以|乘以|代入)[①②③④⑤⑥⑦⑧⑨⑩\d]+式|由[①②③④⑤⑥⑦⑧⑨⑩\d]+(?:式)?(?:[和与及][①②③④⑤⑥⑦⑧⑨⑩\d]+(?:式)?)?|将[①②③④⑤⑥⑦⑧⑨⑩\d]+(?:式)?代入[①②③④⑤⑥⑦⑧⑨⑩\d]+(?:式)?|联立[①②③④⑤⑥⑦⑧⑨⑩\d]+(?:式)?(?:[和与及][①②③④⑤⑥⑦⑧⑨⑩\d]+(?:式)?)?|两式相减|两式相加|两式相除|两边同时?平方)[，,可]*(?:得|知|有)[：:]?)/gu;
      let s = segment.replace(equationOpRe, (_m, delimiter, circled, opPart) => {
        const circledStr = circled ? ` ${circled}` : '';
        const opStr = /[:：]$/.test(opPart) ? opPart : `${opPart}：`;
        return `${delimiter}${circledStr}\n\n${opStr}`;
      });

      // 2. 公式后紧贴标准推导过渡词（化简得、整理得、代入得、因式分解得、解得、即 等）
      const keywords =
        '化简得|整理得|代入得|因式分解得|解得|两式相减得|两式相加得|两式相除得|移项得|移项整理得|移项并整理得|展开得|由此可得|由题意得|由题意有|即';
      s = s.replace(
        new RegExp(`(\\$|\\\\\\)|\\$\\$|\\\\\\])[ \\t]*(${keywords})[：:]`, 'gu'),
        '$1\n\n$2：'
      );

      // 3. 公式末尾直接粘连带圈编号时，补充一个空格以美化排版（如「...^2)①」->「...^2) ①」）
      s = s.replace(/(\$|\\\)|\$\$|\\\])([①②③④⑤⑥⑦⑧⑨⑩])/g, '$1 $2');

      return s;
    })
    .join('');
}

/** 行内 <code> 保留等宽样式（严格跳过 <pre>...</pre> 代码块内部，避免无语言代码块被误加行内背景） */
export function unwrapChatInlineCode(html: string): string {
  if (!html) return '';
  return html.replace(/<pre\b[\s\S]*?<\/pre>|<code>([^<]*)<\/code>/gi, (match, inner: string | undefined) => {
    if (inner === undefined) {
      return match;
    }
    return `<code class="chat-inline-code">${inner}</code>`;
  });
}

/** 渲染通用 Markdown 正文（不含 Copilot 引用角标替换，供聊天与知识库切片复用） */
export function renderPlainChatMarkdown(raw: string): string {
  if (!raw?.trim()) return '';

  const normalized = normalizeChatMarkdown(raw);
  const withBold = repairChatBoldMarkers(normalized);
  let html = renderMarkdownForChat(withBold);

  html = unwrapChatInlineCode(html);
  html = stripOrphanFenceParagraphs(html);

  return html;
}

/** 渲染聊天消息 HTML */
export function renderChatMarkdown(raw: string): string {
  const html = renderPlainChatMarkdown(raw);
  if (!html) return '';

  return html.replace(/\[([1-9]\d*)\]/g, (_match, id) => {
    return `<sup class="copilot-citation-sup" data-citation-idx="${id}">[${id}]</sup>`;
  });
}

/** 深度思考区专用：不走正文表格容错，避免 {x | ...} 被误拆成表格 */
export function renderReasoningMarkdown(raw: string): string {
  if (!raw?.trim()) return '';
  const normalized = normalizeReasoningMarkdown(cleanReasoningText(raw));
  const withBold = repairChatBoldMarkers(normalized);
  let html = renderMarkdownForReasoning(withBold);
  html = unwrapChatInlineCode(html);
  return html;
}

export {
  bindMarkdownCodeCopy,
  cleanupOrphanMermaidDom,
  renderMermaidInElement,
  normalizeCodeFences
} from './markdown';
