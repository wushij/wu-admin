import MarkdownIt from 'markdown-it';
import multimdTable from 'markdown-it-multimd-table';
import hljs from 'highlight.js/lib/common';
import katex from 'katex';
import mermaid from 'mermaid';
import DOMPurify from 'dompurify';
// 代码高亮与公式样式统一在应用层引入（见 app/bootstrap.ts）：
// 此处不做 CSS 副作用导入，避免打包器把本模块判定为“有副作用”，
// 使仅引用本文件任意导出（如一个常量或清理函数）的代码被迫在首屏同步加载整套渲染库。
import { normalizeChatTables } from './chat-table-normalize';
import { replaceSectionEmojisWithIcons } from './section-icons';
import {
  asciiTreeToMermaid,
  expandAsciiTreeToMultiline,
  looksLikeAsciiKnowledgeTree
} from './ascii-tree-graph';
import {
  repairCommonLatexSpacing,
  wrapCalculusExpressions,
  wrapProsePlainMath,
  isSafeMultilineMath,
  promoteStandaloneMathEquationsToBlock,
  isNonMathFilePathFormula,
  normalizeUnicodeMathTokens,
  convertInlineSlashesToFractions,
  prepareKatexFormula
} from './render-math';

export { isSafeMultilineMath, promoteStandaloneMathEquationsToBlock };

const FENCED_CODE_BLOCK_RE = /(````[\s\S]*?````|```[\s\S]*?```)/g;

const PLAIN_MATH_HINT =
  /(?:~|[\u2192\u221e\u00b2\u00b3]|lim|sin|cos|tan|ln|arcsin|arctan|\^|\/\s*x|□)/i;

/** 模型输出的纯文本公式 → 可给 KaTeX 的 LaTeX */
function plainMathToLatex(raw: string): string {
  let s = raw.trim();
  // lim x→0 / limₓ→0 须在全局 → 替换之前处理
  s = s.replace(/\blimₓ→(0|∞|x₀|x_0)/gi, (_m, target: string) => {
    const t = normalizeUnicodeMathTokens(target);
    return `\\lim_{x \\to ${t}}`;
  });
  s = s.replace(/\blim\s+x→(0|∞|x₀|x_0)/gi, (_m, target: string) => {
    const t = normalizeUnicodeMathTokens(target);
    return `\\lim_{x \\to ${t}}`;
  });
  s = s.replace(/\blim\s+x\s*→\s*(\S+)/gi, (_m, target: string) => {
    const t = normalizeUnicodeMathTokens(target);
    return `\\lim_{x \\to ${t}}`;
  });
  s = normalizeUnicodeMathTokens(s);
  s = s.replace(/\s*~\s*/g, ' \\sim ');
  s = s.replace(/\blim\s*\(/gi, '\\lim(');
  s = s.replace(/(?<!\\)\b(arcsin|arctan|sin|cos|tan|cot|sec|csc|ln|exp)(?![a-zA-Z])/g, '\\$1 ');
  s = s.replace(/\\{2,}(arcsin|arctan|sin|cos|tan|cot|sec|csc|ln|exp)/g, '\\$1');
  s = convertInlineSlashesToFractions(s);
  // 规范化多余空格：(1 + x) → (1+x)
  s = s.replace(/\(\s+/g, '(').replace(/\s+\)/g, ')').replace(/\s*\+\s*/g, '+');
  return s;
}

function wrapPlainMath(expr: string): string {
  const trimmed = expr.trim();
  if (!trimmed || trimmed.includes('$') || !PLAIN_MATH_HINT.test(trimmed)) {
    return expr;
  }
  return `$${plainMathToLatex(trimmed)}$`;
}

/**
 * 模型常输出无 $ 包裹的纯文本公式（如 sin x ~ x、lim x→0 sin x/x =1），KaTeX 无法识别，需补 $ 并转 LaTeX。
 */
function normalizePlainMathExpressions(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      let s = segment;

      // limₓ→0 / lim x→0 / lim x→x₀ f(x)=f(x₀)
      s = s.replace(
        /(?<![\\$])\blimₓ→(?:0|∞|x₀|x_0)\s+[^；;，。\n]+?(?=[；;，。]|$)/gi,
        (match) => wrapPlainMath(match)
      );
      s = s.replace(
        /(?<![\\$])\blim\s+x→(?:0|∞|x₀|x_0)\s+[^；;，。\n]+?(?=[；;，。]|$)/gi,
        (match) => wrapPlainMath(match)
      );
      s = s.replace(
        /(?<![\\$])\blim\s+x\s*→\s*\S+\s+[^；;，。\n]+?(?=[；;，。]|$)/gi,
        (match) => wrapPlainMath(match)
      );

      // lim(sin x/x)=1; lim(1+1/x)^x=e（括号后可有 ^x 等）
      s = s.replace(
        /(?<![\\$])\blim\s*\([^)]+\)[^；;，。\s=]*=\s*[^；;，。\s]+/gi,
        (match) => wrapPlainMath(match)
      );

      // 等价无穷小：sin x ~ x, 1-cos x ~ x²/2, e^(x²) - 1 ~ x²
      s = s.replace(
        /(?<=[:：，,；;、。 \t\n•（\u4e00-\u9fa5]|^)([0-9a-zA-Z()+\-^/ \t⁰¹²³⁴⁵⁶⁷⁸⁹]+?)[ \t]*~[ \t]*([0-9a-zA-Z()+\-^/ \t⁰¹²³⁴⁵⁶⁷⁸⁹]+)(?=[,，；;。\n]|$)/g,
        (match, left: string, right: string) => {
          const cleanLeft = left.trim();
          const cleanRight = right.trim();
          if (!cleanLeft || !cleanRight || !/[a-zA-Z]/.test(cleanLeft + cleanRight)) return match;
          return wrapPlainMath(`${cleanLeft} ~ ${cleanRight}`);
        }
      );

      // 结构模板：sin/□→1,□→0；(1+□)^(1/□)→e
      s = s.replace(/(?<![\\$])sin\/□→\d+(?:,□→\d+)*/g, (match) => wrapPlainMath(match));
      s = s.replace(/(?<![\\$])\(\d+\+□\)\^\(\d+\/□\)→e/g, (match) => wrapPlainMath(match));

      // 数学绝对值符号转换：|x|、| x |、|x - 0|、|f(x)|、|x^2 sin(1/x)|、|h sin(1/h)|
      // 必须排除 Markdown 表格行（以 | 开头且以 | 结尾），避免将表格单元格误当成绝对值破坏表格结构
      s = s
        .split('\n')
        .map((line) => {
          const trimmed = line.trim();
          if (trimmed.startsWith('|') && trimmed.endsWith('|') && trimmed.length > 2) {
            return line;
          }
          return line.replace(
            /(?<![\\$|])\|\s*([a-zA-Z0-9_\-\+\*\^\.\,\s\\()\/]+?)\s*\|(?![\\$|])/g,
            (match, inner: string) => {
              const t = inner.trim();
              if (t.length > 0 && t.length <= 35 && /[a-zA-Z0-9]/.test(t) && !/[，。！？\n]/.test(t)) {
                return `$|${t}|$`;
              }
              return match;
            }
          );
        })
        .join('\n');

      return s;
    })
    .join('');
}

function closingDelimiterForLeft(open: string): string {
  switch (open) {
    case '(':
      return ')';
    case '[':
      return ']';
    case '{':
      return '}';
    case '|':
      return '|';
    case '.':
      return '.';
    default:
      return ']';
  }
}

function inferLeftDelimiter(beforeRight: string): string {
  for (let i = beforeRight.length - 1; i >= 0; i--) {
    if (beforeRight.startsWith('\\left', i)) {
      const next = beforeRight[i + 5];
      if (next === '(' || next === '[' || next === '{' || next === '|' || next === '.') {
        return next;
      }
      if (next === '-') return '[';
    }
  }
  return '[';
}

/** 修复模型常输出的非法 \\left / \\right */
function repairMalformedLeftRight(latex: string): string {
  let s = latex;

  // \left-\frac{a}{b},1\right\cup → \left[-\frac{a}{b},1\right]\cup
  s = s.replace(
    /\\left-(\\frac\{[^}]+\}\{[^}]+\}(?:,[^\\]+?)?)\\right(?![\]\).|])/g,
    '\\left[-$1\\right]'
  );

  // \left-\frac{a}{b}\right（无逗号区间）
  s = s.replace(
    /\\left-(\\frac\{[^}]+\}\{[^}]+\})\\right(?![\]\).|])/g,
    '\\left[-$1\\right]'
  );

  // \left- 缺定界符
  s = s.replace(/\\left-(?=\\frac|\d)/g, '\\left[-');

  // \left( … \right\cup → \right)\cup
  s = s.replace(/\\left\(((?:[^\\]|\\.)*?)\\right(?=\\cup)/g, '\\left($1\\right)\\cup');
  // \left[ … \right\cup → \right]\cup
  s = s.replace(/\\left\[((?:[^\\]|\\.)*?)\\right(?=\\cup)/g, '\\left[$1\\right]\\cup');

  // 兜底：\right 后接 \cup 时按最近的 \left 补全闭合符
  s = s.replace(/\\right(?=\\cup)/g, (_match, offset: number, whole: string) => {
    const before = whole.slice(0, offset);
    const open = inferLeftDelimiter(before);
    return `\\right${closingDelimiterForLeft(open)}`;
  });

  // \left( … \right 行末缺 )
  s = s.replace(/\\left\(((?:[^\\]|\\.)*?)\\right(?![\]\).|])/g, '\\left($1\\right)');

  return repairCommonLatexSpacing(s);
}

/** 将常见 Unicode 数学符号转为 LaTeX（仅在即将进入公式的片段内使用） */
function normalizeUnicodeMathSymbols(text: string): string {
  return text
    .replace(/⊆/g, '\\subseteq ')
    .replace(/⊂/g, '\\subset ')
    .replace(/≥/g, '\\geq ')
    .replace(/≤/g, '\\leq ')
    .replace(/≠/g, '\\neq ')
    .replace(/∈/g, '\\in ')
    .replace(/∪/g, '\\cup ')
    .replace(/∩/g, '\\cap ')
    .replace(/²/g, '^2')
    .replace(/³/g, '^3')
    .replace(/₀/g, '_0')
    .replace(/₁/g, '_1');
}

/** 将 Unicode 区间写法 a∈(-½,1)∪(3,+∞) 转为 $...$ LaTeX */
function normalizeUnicodeIntervalNotation(text: string): string {
  const fracMap: Record<string, string> = { '½': '\\frac{1}{2}', '¼': '\\frac{1}{4}', '¾': '\\frac{3}{4}' };

  const toLatexNum = (raw: string): string => {
    let t = raw.trim();
    for (const [u, lx] of Object.entries(fracMap)) {
      t = t.replace(new RegExp(u, 'g'), lx);
    }
    return t.replace(/∞/g, '\\infty').replace(/−/g, '-');
  };

  return text.replace(
    /([a-zA-Z]?)\s*∈\s*\(([^)]+)\)\s*∪\s*\(([^)]+)\)/g,
    (_m, varName: string, interval1: string, interval2: string) => {
      const v = varName || 'x';
      return `$${v} \\in (${toLatexNum(interval1)}) \\cup (${toLatexNum(interval2)})$`;
    }
  );
}

/** 未加 $ 的 \\in\\left... 片段自动包裹 */
function wrapRawLatexFragments(text: string): string {
  return text.replace(
    /(?<!\$)(\\(?:in|notin|subset|cup|cap)(?![a-zA-Z])[^$\n]*\\left[^$\n]+?\\right(?:[\]\).]|\\right)(?:\\cup[^$\n]+?)?)/g,
    (match) => `$${repairMalformedLeftRight(match.trim())}$`
  );
}

/**
 * 编程代码特征正则：满足该正则的行内代码绝对保留为 <code>，绝不误当作数学公式
 */
const PROGRAMMING_CODE_RE =
  /\b(public|private|protected|static|final|class|interface|enum|extends|implements|import|export|from|package|void|int|double|float|long|short|byte|char|boolean|String|const|let|var|function|def|return|new|this|super|if|else|switch|case|for|while|do|break|continue|try|catch|finally|throw|throws|typeof|instanceof|null|true|false|undefined|select|insert|update|delete|table|npm|git|yarn|pnpm|npx|cd|ls|mkdir|docker|kubectl|pip|pip3|python|python3|mvn|mvnw|gradle|conda|curl|wget)\b|<[a-zA-Z/][^>]*>|=>|===|!==|;|console\./;

/**
 * 文件路径、目录、系统命令与 URL 特征正则：满足该特征的行内代码绝非数学公式，绝对保留为 <code>
 */
export const FILE_PATH_OR_CLI_RE =
  /^[a-zA-Z]:[\\/]|^\.{1,2}[\\/]|^\/(?:bin|boot|dev|etc|home|lib|media|mnt|opt|proc|root|run|sbin|srv|sys|tmp|usr|var|Users|Applications|System|Library)\/|https?:\/\/|file:\/\/|[a-zA-Z0-9_.\u4e00-\u9fa5]+\.(?:exe|py|java|ts|js|jsx|tsx|vue|css|scss|less|html|htm|json|xml|yaml|yml|md|txt|sh|bash|bat|cmd|ps1|dll|so|dylib|bin|log|csv|tsv|sql|zip|tar|gz|7z|png|jpg|jpeg|gif|svg|ico|pdf|doc|docx|ppt|pptx|xls|xlsx|toml|ini|env|conf|properties|gradle|mvnw|lock|wasm|class|jar|war)(?:$|[?#\s])|^--[a-zA-Z][a-zA-Z0-9-]*|\b(pip|python|python3|pip3|mvn|gradle|conda|curl|wget)\b/i;

/**
 * 常见合法 LaTeX 数学指令白名单（排除形如 \Users、\AppData、\python 等 Windows 路径伪宏）
 */
export const LATEX_MATH_COMMANDS_RE =
  /\\(?:frac|dfrac|tfrac|sqrt|cbrt|sum|prod|int|iint|iiint|oint|partial|nabla|lim|infty|sim|approx|ne|neq|le|leq|ge|geq|in|notin|subset|subseteq|supset|supseteq|cup|cap|to|leftarrow|rightarrow|Rightarrow|Leftarrow|Leftrightarrow|pm|mp|times|div|cdot|equiv|alpha|beta|gamma|delta|epsilon|varepsilon|zeta|eta|theta|vartheta|iota|kappa|lambda|mu|nu|xi|pi|varpi|rho|varrho|sigma|varsigma|tau|upsilon|phi|varphi|chi|psi|omega|Gamma|Delta|Theta|Lambda|Xi|Pi|Sigma|Upsilon|Phi|Psi|Omega|mathrm|mathbf|mathit|mathbb|mathcal|sin|cos|tan|cot|sec|csc|ln|log|lg|exp|arcsin|arccos|arctan|sinh|cosh|tanh|left|right|vec|hat|bar|tilde|dot|ddot|underline|overline|boxed|displaystyle|text)\b|\\[a-zA-Z]+\s*\{/;

/** 连续两个 3 字母以上英文单词（如 "The value"、"value of"），用于区分英文自然语言句子与数学表达式 */
const PROSE_WORD_PAIR_RE = /[A-Za-z]{3,}\s+[A-Za-z]{3,}/;

/**
 * 判断单反引号 `...` 内部是否为被大模型误加代码围栏的数学公式或推导表达式
 */
export function looksLikeMathNotInlineCode(raw: string): boolean {
  const t = raw.trim();
  if (!t || t.length > 250) return false;
  // 1. 若含有典型编程语法/保留字、文件路径或系统命令，绝对是代码
  if (PROGRAMMING_CODE_RE.test(t) || FILE_PATH_OR_CLI_RE.test(t)) return false;

  // 1b. 若包含 shell / awk / 脚本变量（如 $1, $2, $USER, ${{ github... }}），绝对保留为代码，绝非数学公式
  if (/\$(?:[0-9@?*#]|\b[A-Z_][A-Z0-9_]*\b)|\$\{[^}\n]*\}/.test(t)) return false;

  // 2. 内部本来就写了 $...$（大模型误在公式外又套了反引号，如 `$x+1$`）
  // 关键防护：必须首尾完整匹配 $...$，绝不可误匹配包含两个 $ 变量的行内代码（如 `echo $USER $HOME` 或 `${{ a }}-${{ b }}`）
  if (/^\$[^$\n]+\$$/.test(t)) {
    const inner = t.slice(1, -1).trim();
    // 即使套了 $...$，若实质是文件路径（如 $C:\Users\...$）或模板插值语法，也绝不可作为数学公式
    if (!inner || FILE_PATH_OR_CLI_RE.test(inner) || /\$\{|\}/.test(inner)) return false;
    return true;
  }

  // 3. 含有 LaTeX 典型语法标记：如 e^{-2t}、x_{0}、\frac、\cos、\sin 等（严格排除 Windows 路径伪宏）
  if (
    LATEX_MATH_COMMANDS_RE.test(t) ||
    /\^\{[^{}]+\}|_\{[^{}]+\}|\^[0-9a-zA-Z+-]+/.test(t)
  ) {
    return true;
  }

  // 4. 含有函数导数记号：如 y'、f'(x)、(e^{-2t})'、(cos3t)'、dy/dx
  if (/[a-zA-Z)\]]'{1,3}|\b(d\^[0-9]+[a-zA-Z]\/d[a-zA-Z]\^[0-9]+|d[a-zA-Z]\/d[a-zA-Z])\b/.test(t)) {
    return true;
  }

  // 5. 含有初等三角、极限、对数函数且包含数学参数：如 +3sin3t、-3sin3t、sin2x、cos(3t)、-sin(3t)、ln(1+x)、lim x->0
  // 关键防御：函数名前后必须具备非字母边界（(?<![a-zA-Z]) 与 (?![a-zA-Z])），既支持紧跟数字/参数无空格的无括号写法（如 3sin3t、sin2x），
  // 又能彻底杜绝 Instant (含 tan)、constant (含 tan)、explain/export (含 exp) 等常规编程类名/英文单词被误判为三角函数
  if (
    /(?<![a-zA-Z])[+\-]?\s*\d*(?:sin|cos|tan|cot|sec|csc|ln|exp|lim)(?![a-zA-Z])\s*(?:\([a-zA-Z0-9+\-*^/]+\)|[a-zA-Z0-9]+)/i.test(
      t
    )
  ) {
    return true;
  }

  // 6. 纯数学等式/不等式链（由变量、数字、运算符号、括号组成，且含有等号或不等号）：
  // 如 (e^{-2t})'=-2e^{-2t}、y'=-2e^{-2t}cos3t-3e^{-2t}sin3t=-e^{-2t}(2cos3t+3sin3t)、x > 0
  if (/^[a-zA-Z0-9'()+\-*^/_\\·\s]+[=≠≤≥<>][a-zA-Z0-9'()+\-*^/_\\·=≠≤≥<>\s]+$/.test(t)) {
    if (!PROSE_WORD_PAIR_RE.test(t) && /[a-zA-Z]/.test(t)) {
      return true;
    }
  }

  // 7. 纯数学正负单项式：例如 +3sin3t、-3sin3t、-e^{-2t}、-2
  if (/^[+\-]\s*[0-9a-zA-Z^_{}()+\-*/\\·]+$/.test(t) && (/[a-zA-Z]/.test(t) || /^[+\-]\d+$/.test(t))) {
    return true;
  }

  return false;
}

/**
 * 将被大模型误包裹在单反引号 `...` 内的数学公式（如 `+3sin3t`、`cos(3t)`、`y'=(e^{-2t})'cos3t`）
 * 解除代码包裹并转换为标准 $...$ LaTeX 公式，使 KaTeX 能够正常渲染，消除残留的灰底代码方框
 */
export function unwrapInlineMathCodeFences(text: string): string {
  if (!text || !text.includes('`')) return text;
  return text.replace(/`([^`\n]+?)`/g, (match, inner: string) => {
    const trimmed = inner.trim();
    if (!looksLikeMathNotInlineCode(trimmed)) {
      return match;
    }
    if (trimmed.startsWith('$') && trimmed.endsWith('$') && trimmed.length > 2) {
      return trimmed;
    }
    const latex = plainMathToLatex(trimmed);
    return `$${latex}$`;
  });
}

/** 将 \(...\)/\[...\] 及模型常用的 [latex] 转为 $...$ / $$...$$ */
function normalizeLatexDelimiters(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      // 0a. 先隔离表格内嵌代码块（<div class="chat-table-code-block">...</div>），
      // 严禁将表格代码单元格内部的 \[...\]、\(...\)、$VAR、a_1、2^n 等误当作数学公式改写
      const tableCodeBlockPlaceholders: string[] = [];
      const segmentWithoutTableCode = segment.replace(
        /<div class="chat-table-code-block">[\s\S]*?<\/pre><\/div>/g,
        (htmlBlock) => {
          const key = `%%%PRE_TABLE_CODE_BLOCK_${tableCodeBlockPlaceholders.length}%%%`;
          tableCodeBlockPlaceholders.push(htmlBlock);
          return key;
        }
      );

      let s = unwrapInlineMathCodeFences(segmentWithoutTableCode)
        // 无论模型或 JSON 转义输出 1 个、3 个或多个反斜杠，统一规范为 LaTeX 合法换行行距参数 \\[2pt]
        .replace(/\\+\[(-?\d+(?:\.\d+)?\s*(?:pt|em|ex|cm|mm|in)\s*\])/g, (_m, arg) => `\\\\[${arg}`)
        // 保护 LaTeX 换行行距参数（如 \\[2pt]、\\[5pt]、\\[1em] 等），绝不可误替换为 \[
        .replace(/\\\\(\(|\)|\[(?!-?\d+(?:\.\d+)?(?:pt|em|ex|cm|mm|in)\b)|\])/g, '\\$1')
        .replace(/\\\[([\s\S]+?)\\\]/g, (_m, formula: string) => `\n$$${repairMalformedLeftRight(formula.trim())}$$\n`)
        .replace(/\\\(([\s\S]+?)\\\)/g, (_m, formula: string) =>
          `$${repairMalformedLeftRight(formula.replace(/\$/g, '').trim())}$`
        );

      // 0b. 保护经 unwrapInlineMathCodeFences 过滤后留存的真实行内代码（如 `$PATH`、`$HOME`、`${{ github.workflow }}`），
      // 严禁其内部的 $ 与后文其他 $ 跨反引号错误配对成数学公式
      const inlineCodePlaceholders: string[] = [];
      s = s.replace(/`[^`\n]+`/g, (codeSpan) => {
        const key = `%%%PRE_INLINE_CODE_${inlineCodePlaceholders.length}%%%`;
        inlineCodePlaceholders.push(codeSpan);
        return key;
      });

      s = wrapCalculusExpressions(s);
      s = wrapProsePlainMath(s);
      s = promoteStandaloneMathEquationsToBlock(s);

      // =========================================================================================
      // 1. 【防回归保护】：保护已有合法 $$...$$ 独立块与 $...$ 行内/跨行公式
      // 【警示】：
      //   - 严禁省略或打乱本占位步骤！若未先行占位，后续针对裸 LaTeX（如 \frac、\lim）的包裹规则
      //     会把公式内部已经换行的行拆碎并二次套上 $$，造成灾难性排版错乱。
      //   - 跨行公式必须经过 isSafeMultilineMath 严格过滤，绝不可无条件匹配跨段落字符。
      // =========================================================================================
      const mathPlaceholders: string[] = [];
      const maskMath = (raw: string): string => {
        const key = `%%%PRE_MATH_TOKEN_${mathPlaceholders.length}%%%`;
        mathPlaceholders.push(raw);
        return key;
      };

      // 修复已有的 $$...$$ 与 $...$ 内部语法，并将其占位保护起来
      s = s.replace(/\$\$([\s\S]+?)\$\$/g, (_m, formula: string) =>
        maskMath(`$$${repairMalformedLeftRight(formula)}$$`)
      );
      s = s.replace(/(?<!\$)\$(?!\$)([^$\n]+?)(?<!\$)\$(?!\$)/g, (_m, formula: string) =>
        maskMath(`$${repairMalformedLeftRight(formula)}$`)
      );
      // 保护合法的跨行推导单 $ 公式（如裂项相消等式链）
      s = s.replace(/(?<!\$)\$(?!\$)([\s\S]+?)(?<!\$)\$(?!\$)/g, (match, formula: string) => {
        if (isSafeMultilineMath(formula)) {
          return maskMath(`$${repairMalformedLeftRight(formula)}$`);
        }
        return match;
      });

      // 2. 裸多行环境识别（如未包裹 $$ 的 \begin{aligned}...\end{aligned}、\boxed{...}）自动包裹为 $$...$$
      s = s.replace(
        /(^|\n)[ \t]*(\\begin\{(?:aligned|cases|matrix|bmatrix|pmatrix|gathered)\}[\s\S]*?\\end\{(?:aligned|cases|matrix|bmatrix|pmatrix|gathered)\})(?=[ \t]*(?:\n|$))/g,
        (_m, prefix: string, formula: string) => `${prefix}\n$$${formula.trim()}$$\n`
      );
      s = s.replace(
        /(^|\n)[ \t]*(\\boxed\s*\{[\s\S]*?\n?[ \t]*\})(?=[ \t]*(?:\n|$))/g,
        (_m, prefix: string, formula: string) => `${prefix}\n$$${formula.trim()}$$\n`
      );

      // 3. 独占一行的裸 LaTeX 独立数学公式（如模型直接输出未包裹的 \lim_{x\to\infty}...\right)^x=e）自动包裹为 $$...$$
      s = s.replace(
        /(^|\n)[ \t]*(\\(?:lim|frac|sqrt|sum|int|iint|iiint|oint|prod|left|displaystyle|aligned)(?![a-zA-Z])[^\$\n]+?)(?=[ \t]*(?:\n|$))/g,
        (_m, prefix: string, formula: string) => `${prefix}\n$$${formula.trim()}$$\n`
      );

      // 4. 模型常输出 [ f'(x)=\frac{...}{...} ] 而非 $...$
      // 注意：必须排除 LaTeX 语法本身的 \left[ ... \right]、\Big[ ... \Big] 等括号！
      s = s.replace(
        /(\\?(?:left|right|big|Big|bigg|Bigg)\s*)?\[\s*([^[\n]*\\(?:frac|left|right|boxed|lim|ln|le|ge|Rightarrow|infty|varepsilon|delta)[^[\n]*?)\s*(\\?(?:left|right|big|Big|bigg|Bigg)\s*)?\]/g,
        (match, prefixCmd, formula, suffixCmd) => {
          if (prefixCmd || suffixCmd || match.startsWith('\\[') || match.startsWith('\\[')) {
            return match;
          }
          return `$${repairMalformedLeftRight(formula.trim())}$`;
        }
      );

      s = normalizeUnicodeIntervalNotation(s);
      s = wrapRawLatexFragments(s);
      s = normalizePlainMathExpressions(s);

      // 5. 集合描述 {x | ...}、区间 [1,3] 等纯文本数学片段补 $ 包裹
      s = s.replace(
        /(?<!\$)([A-Z]\s*=\s*\{[^{}\n|]+\|\s*[^}\n]+\})(?!\$)/g,
        (_m, expr: string) => `$${normalizeUnicodeMathSymbols(expr.trim())}$`
      );
      s = s.replace(
        /(?<!\$)(\d+\s*≤\s*[a-zA-Z]\s*≤\s*\d+)(?!\$)/g,
        (_m, expr: string) => `$${normalizeUnicodeMathSymbols(expr.trim())}$`
      );

      // 6. 还原占位符（使用函数回调防止 $$ 被 replace 机制吞并为单个 $）
      mathPlaceholders.forEach((mathStr, idx) => {
        s = s.replaceAll(`%%%PRE_MATH_TOKEN_${idx}%%%`, () => mathStr);
      });
      inlineCodePlaceholders.forEach((codeSpan, idx) => {
        s = s.replaceAll(`%%%PRE_INLINE_CODE_${idx}%%%`, () => codeSpan);
      });
      tableCodeBlockPlaceholders.forEach((htmlBlock, idx) => {
        s = s.replaceAll(`%%%PRE_TABLE_CODE_BLOCK_${idx}%%%`, () => htmlBlock);
      });

      return s;
    })
    .join('');
}

const KNOWN_CODE_LANGS = [
  'c\\+\\+', 'cpp', 'csharp', 'c#', 'python', 'py', 'javascript', 'jsx', 'js',
  'typescript', 'tsx', 'ts', 'java', 'golang', 'go', 'rust', 'rs', 'kotlin', 'kt',
  'swift', 'scala', 'sql', 'html', 'css', 'scss', 'less', 'bash', 'shell', 'sh', 'zsh',
  'powershell', 'ps1', 'cmd', 'bat', 'dockerfile', 'makefile', 'cmake',
  'jsonc', 'json5', 'json', 'xml', 'yaml', 'yml', 'toml', 'ini', 'conf', 'properties', 'env',
  'markdown', 'mdx', 'md', 'mermaid', 'r', 'matlab',
  'php', 'ruby', 'rb', 'perl', 'lua', 'dart', 'groovy', 'graphql', 'vue', 'svelte', 'csv', 'tsv', 'diff', 'c',
  'text', 'plain', 'txt', 'console', 'output', 'code'
];

/**
 * 修复大模型输出代码块时缺失换行导致 Markdown 语法颠倒的问题：
 * 1. ``` 前缺失换行（如「### 例题3：混合嵌套循环```c」或「嵌套混合```」）
 *    -> 若前面紧贴非换行内容，CommonMark 无法识别开头的代码围栏，会导致后方的闭合 ``` 被误判为开头，
 *       把正文讲解/公式全部吞进代码框，代码反而裸露在外。
 * 2. ```lang 与首行代码粘连（如 ```cfor 或 ```pythondef）
 *    -> 拆开语言声明与代码文本，确保语法高亮与代码独立换行。
 * 3. 未声明语言但紧贴代码（如 ```for 或 ```i=1）
 *    -> 拆开围栏与首行代码。
 * 4. 闭合 ``` 后面粘连中文小标题/正文（如 ```思路：或 ```解析：）
 *    -> 确保代码块闭合后换行。
 */
/**
 * 保护并安全规范化 Markdown 表格行内部内嵌的代码块。
 * 大模型常在表格单元格内用 <br>```lang...```<br> 嵌入代码段；
 * 若直接对其进行围栏换行切分，会导致整行表格被截断，且后续所有表格行被误吞进代码块。
 * 本函数将其转换为表格单元格专用的安全行内代码块结构，既保留语法排版，又确保表格结构不瓦解。
 */
function normalizeCodeBlocksInTableRows(text: string): string {
  if (!text || !text.includes('|') || !text.includes('```')) return text;
  return text
    .split('\n')
    .map((line) => {
      const trimmed = line.trim();
      // 必须是真正的表格数据行或表头行（前后带有 | 管道符）
      if (!trimmed.startsWith('|') || !trimmed.endsWith('|')) {
        return line;
      }
      if (!trimmed.includes('```')) {
        return line;
      }
      return line.replace(
        /```([a-zA-Z0-9_#+-]*)(?:<br\s*\/?>|[ \t])([\s\S]*?)```/gi,
        (_m, langRaw: string, codeRaw: string) => {
          const lang = (langRaw || 'text').trim().toLowerCase();
          let code = codeRaw.replace(/<br\s*\/?>/gi, '%%%TABLE_CODE_BR%%%');
          code = code
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
          code = code.replace(/%%%TABLE_CODE_BR%%%/g, '<br>');
          return `<div class="chat-table-code-block"><div class="chat-table-code-header"><span class="chat-table-code-lang">${lang.toUpperCase()}</span></div><pre class="chat-table-code-pre"><code class="language-${lang}">${code}</code></pre></div>`;
        }
      );
    })
    .join('\n');
}

/**
 * 统一规范代码块围栏标记（```），彻底消除大模型各种语法毛刺与排版缺陷：
 * 1. 紧随表格行的围栏（如 | xxx |\n```）
 *    -> 强制插入空行，避免 CommonMark 误将 ``` 吞进表格最后一行。
 * 2. 紧贴非换行字符的 ```（如 示例```java 或 如下：```sql）
 *    -> 强制换行，确保代码块正确开启。
 * 3. ```lang 紧贴代码无换行（如 ```cfor 或 ```pythondef）
 *    -> 拆开语言声明与代码文本，确保语法高亮与代码独立换行。
 * 4. 未声明语言但紧贴代码（如 ```for 或 ```i=1）
 *    -> 拆开围栏与首行代码。
 * 5. 闭合 ``` 后面粘连中文小标题/正文（如 ```思路：或 ```解析：）
 *    -> 确保代码块闭合后换行。
 */
export function normalizeCodeFences(raw: string, externalMentionMap?: Map<string, string>): string {
  if (!raw) return '';
  let s = normalizeCodeBlocksInTableRows(raw);

  // 1.0 先行规整所有语言声明与首行代码紧贴粘连的情况（如 ```pythonprint、```textHello, Python!、```python"""）
  // 必须优先于任何随文提及或行内代码判断，确保进入后续处理时代码块围栏已具备标准换行结构，彻底杜绝围栏破坏引发的正文倒置
  // 1.0.1 C/C++ 预处理指令与语言粘连（如 ```c#include 或 ```cpp#include 或 ```c#define 等）
  s = s.replace(
    /(^|\n)([`~]{3,})(c|cpp)[ \t]*#(include|define|undef|ifdef|ifndef|if|elif|else|endif|line|error|pragma)\b/gi,
    '$1$2$3\n#$4'
  );

  // 1.0.2 C# 语言粘连代码文本（如 ```c#using 或 ```c#class 或 ```c#// 等）：
  s = s.replace(
    /(^|\n)([`~]{3,})(c#)[ \t]*([a-zA-Z0-9_$/\\*+<>!&|^~@\-{}()[\]"':;,;%?=\u4e00-\u9fa5])/gi,
    '$1$2$3\n$4'
  );

  // 1.0.3 ```lang 后面在同一行直接粘连代码文本（如 ```cfor、```pythonprint、```textHello、```python"""、```python#）
  // 涵盖字母、数字、所有编程符号、引号（" ' `）、冒号、分号及中文注释等任何合法代码起始字符。
  const sortedKnownLangs = KNOWN_CODE_LANGS.slice().sort((a, b) => b.length - a.length);
  const knownLangsPattern = sortedKnownLangs.join('|');
  const codeStartChar = '[^\\s]';
  const langGluedRegex = new RegExp(`(^|\\n)([\`~]{3,})(${knownLangsPattern})[ \\t]*(${codeStartChar})`, 'gi');
  s = s.replace(langGluedRegex, (match, p1, p2, lang, nextChar, offset, str) => {
    // 关键防御 1：若该行直到行尾原本就是完整的单独成行语言声明（如 ```csharp\n 或 ```css\n），绝非行内粘连代码，绝不截断
    const restLine = str.slice(offset + p1.length + p2.length).split(/\r?\n/)[0];
    if (/^[a-zA-Z0-9_#+-]+[ \t]*$/.test(restLine)) {
      return match;
    }

    // 关键防御 2：长语言优先保护，防止短语言在长语言单词内部腰斩（如避免将 python""" 切割为 py + thon"""，或将 csharp 切割为 c + sharp）
    if (/^[a-zA-Z]/.test(nextChar)) {
      const restAfterFence = str.slice(offset + p1.length + p2.length);
      const matchingLonger = sortedKnownLangs.find(
        (other) => other.length > lang.length && restAfterFence.toLowerCase().startsWith(other.toLowerCase())
      );
      if (matchingLonger) {
        return match;
      }
    }

    const langLower = lang.toLowerCase();
    if (langLower === 'c') {
      const restOfMatchAndLine = str.slice(offset + p1.length + p2.length + lang.length);
      // 若紧随的是 c# 或 c++ 且已换行（如 ```c#\n 或 ```c++\n），说明本就是完整语言声明，绝不可拆分
      if (/^(?:#|\+\+)[ \t]*(\r?\n|$)/.test(restOfMatchAndLine)) {
        return match;
      }
      // 若紧随非换行的 #（例如其它预处理指令 #custom），语言为 c，代码从 # 起起始换行
      if (nextChar === '#') {
        return `${p1}${p2}c\n#`;
      }
    }
    return `${p1}${p2}${lang}\n${nextChar}`;
  });

  // 1.0.4 ``` 后面没有任何已知语言声明直接紧贴代码（如 ```for 或 ```i=1 或 ```#注释）
  // 必须加上 (?![`~])，严禁拆碎 4 反引号外层围栏（````markdown）
  s = s.replace(
    /(^|\n)([`~]{3,})(?![`~]|[a-zA-Z0-9_#+-]+(?:\s|\n|$))([a-zA-Z0-9_#$/\\*+<>!&|^~@\-{}()[\]"':;,;%?=\u4e00-\u9fa5]+)/g,
    '$1$2\n$3'
  );

  // 1.1 占位保护自然语言行内随文提及的代码块标记（如「注意排版规范：代码块用 ```c，」或「用 ``` 包裹」），
  // 严禁将随文说明文字中的反引号误判为多行代码块的开启。
  // 【防回归关键约束】：
  //   1) 语言名必须是简短合法名称（<=10字符），严禁贪婪吞噬整行英文代码；
  //   2) 随文提及必须紧随中文全角标点，严禁包含英文半角逗号 ',' 或右括号 ')'，避免误伤代码实参和语法；
  //   3) 严禁误伤整行独立成行的代码块开启围栏。
  const mentionMap = externalMentionMap || new Map<string, string>();
  let mentionCounter = mentionMap.size;

  // 1.1.1 紧跟中文标点的随文提及：如 ```c， 或 ```java。
  s = s.replace(/([`~]{3,}(?:[a-zA-Z0-9_#+-]{1,10})?)([，。！？；：“”（）、《》【】…—])/g, (_m, code: string, punct: string) => {
    const token = `%%%FENCE_MENTION_${mentionCounter++}%%%`;
    mentionMap.set(token, `<code class="chat-inline-code">${mdInstance().utils.escapeHtml(code).replace(/`/g, '&#96;')}</code>`);
    return `${token}${punct}`;
  });

  // 1.1.2 句中跟随中文字词的随文提及：如 ```c 或者 ```java
  s = s.replace(/([`~]{3,}(?:[a-zA-Z0-9_#+-]{1,10})?)[ \t]+(或者|以及|或|和|比如|等|均可|即可)/g, (_m, code: string, word: string) => {
    const token = `%%%FENCE_MENTION_${mentionCounter++}%%%`;
    mentionMap.set(token, `<code class="chat-inline-code">${mdInstance().utils.escapeHtml(code).replace(/`/g, '&#96;')}</code>`);
    return `${token} ${word}`;
  });

  // 2. 如果 ``` 或 ~~~ 紧随表格行（以 | 结尾），必须用空行分隔，避免被表格误吞为最后一列单元格
  s = s.replace(/(\|[ \t]*)\n*(```+|~~~+)/g, '$1\n\n$2');

  // 3. ``` 或 ~~~ 前面紧贴非换行且非围栏字符，强制换行（仅行内空白，不吞多行换行，且绝不拆碎 4 反引号 ````）
  s = s.replace(/([^\n\r`~])[ \t]*(```+|~~~+)/g, '$1\n$2');

  // 4. 闭合代码块 ``` 后面直接紧贴中文或小标题（如 ```思路：外层 或 ```解析：）
  s = s.replace(/(^|\n)([\`~]{3,})([ \t]*)([\u4e00-\u9fa5【「『（\(（])/g, '$1$2\n$4');

  // 5. 若未传入外部延迟还原 Map，则在此原位还原
  if (!externalMentionMap && mentionMap.size > 0) {
    s = s.replace(/%%%FENCE_MENTION_\d+%%%/g, (token) => mentionMap.get(token) || token);
  }

  return s;
}

let mermaidInitialized = false;
let mermaidThemeKey = '';

/** 读取项目主题令牌，保证流程图随亮 / 暗主题与换肤主色实时适配 */
function readThemeVar(name: string, fallback: string): string {
  if (typeof document === 'undefined') return fallback;
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return value || fallback;
}

function resolveMermaidTheme() {
  const isDark =
    typeof document !== 'undefined' && document.documentElement.classList.contains('dark');
  const primary = readThemeVar('--el-color-primary', isDark ? '#409eff' : '#2563eb');
  const surface = readThemeVar('--color-bg-surface', isDark ? '#121218' : '#ffffff');
  const subtle = readThemeVar('--color-bg-subtle', isDark ? '#0c0c10' : '#f8fafc');
  const text = readThemeVar('--color-text-primary', isDark ? '#f1f5f9' : '#0f172a');
  const secondaryText = readThemeVar(
    '--color-text-secondary',
    isDark ? 'rgba(241, 245, 249, 0.72)' : '#334155'
  );
  const border = readThemeVar('--color-border', isDark ? 'rgba(255, 255, 255, 0.12)' : '#cbd5e1');
  const primarySoft = readThemeVar(
    '--el-color-primary-light-9',
    isDark ? 'rgba(64, 158, 255, 0.16)' : '#eff6ff'
  );
  const primaryBorder = readThemeVar(
    '--el-color-primary-light-7',
    isDark ? 'rgba(64, 158, 255, 0.32)' : '#93c5fd'
  );

  return {
    darkMode: isDark,
    primary,
    surface,
    subtle,
    text,
    secondaryText,
    border,
    primarySoft,
    primaryBorder
  };
}

/**
 * 幂等初始化 Mermaid。
 * 首次真正渲染图谱时惰性执行；亮暗主题或主色变化后自动重新初始化。
 */
function ensureMermaidInitialized() {
  const theme = resolveMermaidTheme();
  const themeKey = `${theme.darkMode}|${theme.primary}|${theme.surface}`;
  if (mermaidInitialized && themeKey === mermaidThemeKey) return;
  mermaid.initialize({
    startOnLoad: false,
    // Mermaid 12 默认会把错误 SVG 插入 document.body；开启后失败时清理并抛错，避免污染全站页面
    suppressErrorRendering: true,
    securityLevel: 'strict',
    theme: 'base',
    themeVariables: {
      darkMode: theme.darkMode,
      background: 'transparent',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      fontSize: '13px',
      primaryColor: theme.primarySoft,
      primaryTextColor: theme.text,
      primaryBorderColor: theme.primaryBorder,
      secondaryColor: theme.subtle,
      secondaryTextColor: theme.secondaryText,
      secondaryBorderColor: theme.border,
      tertiaryColor: theme.surface,
      tertiaryTextColor: theme.text,
      tertiaryBorderColor: theme.border,
      clusterBkg: theme.subtle,
      clusterBorder: theme.border,
      lineColor: theme.primary,
      nodeTextColor: theme.text,
      edgeLabelBackground: theme.surface
    },
    flowchart: {
      htmlLabels: true,
      useMaxWidth: false,
      curve: 'basis'
    }
  });
  mermaidInitialized = true;
  mermaidThemeKey = themeKey;
}

/**
 * Markdown 渲染引擎实例。
 *
 * 原先在模块顶层直接 new MarkdownIt 并注册插件、渲染规则，会被打包器判定为本模块存在副作用，
 * 导致任何引用本文件任意导出（哪怕只是引入一个常量）的代码都必须在首屏同步加载
 * highlight.js / katex / mermaid / markdown-it 等全部重型依赖。
 * 改为首次真正渲染时才构建实例与规则，运行结果与原先完全一致，仅初始化时机后移。
 */
let md: InstanceType<typeof MarkdownIt> | null = null;

function ensureMarkdownEngine(): InstanceType<typeof MarkdownIt> {
  if (md) return md;

  const instance = new MarkdownIt({
    html: true,
    linkify: true,
    typographer: true,
    breaks: true
  });

  if (!(instance.utils as unknown as { assign?: typeof Object.assign }).assign) {
    (instance.utils as unknown as { assign: typeof Object.assign }).assign = Object.assign;
  }

  instance.use(multimdTable, {
    multiline: false,
    rowspan: false,
    headerless: false
  });

  instance.renderer.rules.fence = (tokens, idx, _options, env) => {
    const token = tokens[idx];
    const info = token.info ? instance.utils.unescapeAll(token.info).trim() : '';
    const lang = info.split(/\s+/g)[0] || '';
    const enableMermaid = env?.enableMermaid !== false;
    return renderCodeBlock(token.content, lang, enableMermaid);
  };

  instance.renderer.rules.table_open = () => '<div class="table-wrap"><table>';
  instance.renderer.rules.table_close = () => '</table></div>';

  const defaultLinkOpen =
    instance.renderer.rules.link_open ||
    ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options));

  instance.renderer.rules.link_open = (tokens, idx, options, env, self) => {
    const token = tokens[idx];
    const href = String(token.attrGet('href') ?? '').trim();
    const isHash = href.startsWith('#');
    token.attrSet('class', 'chat-link');
    if (!isHash && href) {
      token.attrSet('target', '_blank');
      token.attrSet('rel', 'noopener noreferrer');
    }
    return defaultLinkOpen(tokens, idx, options, env, self);
  };

  bindGlobalMarkdownDelegates();
  md = instance;
  return instance;
}

/** 访问已初始化的渲染器实例（惰性构建） */
function mdInstance(): InstanceType<typeof MarkdownIt> {
  return ensureMarkdownEngine();
}

/**
 * 全局事件委托：无论组件是否处于流式高频重绘，点击复制按钮均能可靠触发。
 * 原先在模块顶层注册，现改为随渲染引擎首次初始化时注册一次，行为不变。
 */
let markdownDelegatesBound = false;

/** 向宿主组件派发图谱放大事件，由 AI 助手内的查看器组件接管展示 */
function requestMermaidViewer(btn: HTMLButtonElement) {
  if (typeof window === 'undefined') return;
  const wrapper = btn.closest<HTMLElement>('.mermaid-diagram-wrapper');
  const svgEl = wrapper?.querySelector<SVGSVGElement>('.mermaid-diagram svg');
  let svgMarkup = '';
  if (svgEl) {
    // 卡片内为控制高度给 SVG 写了 max-height/max-width 内联样式；
    // 全屏查看必须剥离这些限制，否则无论怎么缩放都只有一小块
    const clone = svgEl.cloneNode(true) as SVGSVGElement;
    clone.removeAttribute('style');
    const viewBox = svgEl.viewBox?.baseVal;
    if (viewBox && viewBox.width > 0 && viewBox.height > 0) {
      clone.setAttribute('width', String(viewBox.width));
      clone.setAttribute('height', String(viewBox.height));
    }
    svgMarkup = clone.outerHTML;
  }
  const source = wrapper ? readMermaidSource(wrapper) : '';
  window.dispatchEvent(new CustomEvent('aiwu:mermaid-zoom', { detail: { svg: svgMarkup, source } }));
}

function bindGlobalMarkdownDelegates() {
  if (markdownDelegatesBound || typeof document === 'undefined') return;
  markdownDelegatesBound = true;

  // 1. 单击事件委托：Mermaid 放大按钮与代码块复制按钮
  document.addEventListener('click', async (e) => {
    const zoomBtn = (e.target as HTMLElement)?.closest<HTMLButtonElement>('.mermaid-btn--zoom');
    if (zoomBtn && !zoomBtn.disabled) {
      e.preventDefault();
      e.stopPropagation();
      requestMermaidViewer(zoomBtn);
      return;
    }

    const btn = (e.target as HTMLElement)?.closest<HTMLButtonElement>('.code-copy-btn');
    if (!btn) return;
    e.preventDefault();
    e.stopPropagation();
    const code = btn.closest('.code-block-wrapper')?.querySelector('code')?.textContent || '';
    if (!code) return;
    try {
      await navigator.clipboard.writeText(code);
      const prevText = btn.textContent || '复制';
      btn.textContent = '✓ 已复制';
      btn.classList.add('is-copied');
      setTimeout(() => {
        btn.textContent = prevText;
        btn.classList.remove('is-copied');
      }, 1500);
    } catch {
      /* clipboard blocked */
    }
  });

  // 2. 双击事件委托：双击 Mermaid 容器快速打开全屏大图查看器（独立注册，杜绝嵌套注册与内存泄漏）
  document.addEventListener('dblclick', (e) => {
    const wrap = (e.target as HTMLElement)?.closest<HTMLElement>('.mermaid-diagram-wrapper');
    if (wrap) {
      const zoomBtn = wrap.querySelector<HTMLButtonElement>('.mermaid-btn--zoom');
      if (zoomBtn && !zoomBtn.disabled) {
        e.preventDefault();
        e.stopPropagation();
        requestMermaidViewer(zoomBtn);
      }
    }
  });
}

const GENERIC_LANGS = new Set(['', 'text', 'plain', 'txt', 'code', 'output', 'console']);

function normalizeDeclaredLang(declared: string): string {
  const decl = declared.toLowerCase().trim();
  if (GENERIC_LANGS.has(decl)) return '';
  if (decl === 'py' || decl === 'python3') return 'python';
  if (decl === 'js' || decl === 'jsx') return 'javascript';
  if (decl === 'ts' || decl === 'tsx') return 'typescript';
  if (decl === 'jsonc' || decl === 'json5') return 'json';
  if (decl === 'sh' || decl === 'shell' || decl === 'zsh') return 'bash';
  return decl;
}

function formatLangLabel(lang: string): string {
  const map: Record<string, string> = {
    py: 'python',
    python3: 'python',
    js: 'javascript',
    jsx: 'jsx',
    ts: 'typescript',
    tsx: 'tsx',
    jsonc: 'json',
    json5: 'json',
    sh: 'bash',
    shell: 'bash',
    zsh: 'bash',
    htm: 'html',
    md: 'markdown',
    mermaid: 'mermaid'
  };
  const key = lang.toLowerCase();
  if (GENERIC_LANGS.has(key)) return 'code';
  return map[key] || key || 'code';
}

const MERMAID_HINT =
  /^(?:graph |flowchart |sequenceDiagram|classDiagram|stateDiagram|erDiagram|gantt|pie |gitGraph|journey|mindmap|timeline)/m;

export function looksLikeMermaid(code: string): boolean {
  const trimmed = code.trim();
  if (MERMAID_HINT.test(trimmed)) return true;
  if (/^\s*(?:graph|flowchart)\s+(?:TD|TB|BT|RL|LR)\b/im.test(trimmed)) return true;
  if (/^\s*subgraph\b/im.test(trimmed)) return true;
  const hasArrow = /(?:-->|==>|-\.->)/.test(trimmed);
  const nodeDefs = (trimmed.match(/\b[A-Za-z][\w-]*\s*\[[^\]]+\]/g) || []).length;
  return hasArrow && nodeDefs >= 2;
}

function isMermaidErrorSvg(svg: string): boolean {
  if (!svg?.trim()) return true;
  return /Syntax error in text|aria-roledescription=['"]error['"]|class=['"]error-icon/i.test(svg);
}

function cleanupMermaidTempDom(id: string) {
  if (typeof document === 'undefined') return;
  document.getElementById(`d${id}`)?.remove();
  document.getElementById(`i${id}`)?.remove();
}

// 清理函数已抽离到纯 DOM 工具层（utils/dom/mermaid-dom-cleanup.ts）：
// 本文件内部仍直接调用它（renderMermaidInElement 前后各清理一次），
// 同时保持同名重导出，既有调用方（含下方 chat-markdown 的重导出）行为完全不变，
// 而只需要清理能力的模块可改为直接引入该工具，不必再引入本文件的重型渲染依赖。
import { cleanupOrphanMermaidDom } from './mermaid-dom-cleanup';
export { cleanupOrphanMermaidDom } from './mermaid-dom-cleanup';

export function sanitizeMermaidSvg(svg: string): string {
  if (!svg) return '';

  // 保护 Mermaid 的 foreignObject 标签及其内部节点（包含节点文字与排版），
  // 严格剥除 script、iframe、onerror、onload 等恶意属性，但完整保留安全的 div、span 与文本。
  // DOMPurify 默认在 SVG 模式下会清空 foreignObject 内部子节点，导致图谱节点文字全部变成空框，
  // 采用标准合法 SVG <desc> 标签进行安全隔离占位，消毒完毕后原位安全还原。
  const foreignObjectMap = new Map<string, string>();
  let tokenCounter = 0;

  const masked = svg.replace(/<foreignObject\b[\s\S]*?<\/foreignObject>/gi, (match) => {
    // 对 foreignObject 内部进行快速安全检查：严禁包含任何可执行脚本或跨站属性
    if (
      /<(?:script|iframe|object|embed|form)\b/i.test(match) ||
      /\bon[a-z]+\s*=/i.test(match) ||
      /javascript:/i.test(match)
    ) {
      return '';
    }
    const token = `__MERMAID_FO_${tokenCounter++}__`;
    foreignObjectMap.set(token, match);
    return `<desc data-mermaid-fo="${token}"></desc>`;
  });

  const sanitized = DOMPurify.sanitize(masked, {
    USE_PROFILES: { svg: true, html: true },
    ADD_TAGS: ['style', 'desc'],
    ADD_ATTR: [
      'target',
      'rel',
      'viewBox',
      'aria-hidden',
      'preserveAspectRatio',
      'data-mermaid-fo'
    ],
    FORBID_TAGS: ['script', 'iframe', 'object', 'embed', 'form'],
    FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover', 'onfocus', 'onblur']
  });

  return sanitized.replace(
    /<desc\s+data-mermaid-fo="(__MERMAID_FO_\d+__)"><\/desc>/g,
    (_m, token: string) => {
      return foreignObjectMap.get(token) || '';
    }
  );
}

export async function renderMermaidSvg(id: string, code: string) {
  ensureMermaidInitialized();
  try {
    const result = await mermaid.render(id, code);
    if (isMermaidErrorSvg(result.svg)) {
      throw new Error('Mermaid syntax error');
    }
    return {
      svg: sanitizeMermaidSvg(result.svg),
      bindFunctions: result.bindFunctions
    };
  } finally {
    cleanupMermaidTempDom(id);
  }
}

function quoteMermaidLabel(label: string): string {
  let trimmed = label.trim();
  if (trimmed.length > 44) {
    trimmed = `${trimmed.slice(0, 43)}…`;
  }
  if (!trimmed) return '""';
  if (trimmed.startsWith('"') && trimmed.endsWith('"')) return trimmed;
  return `"${trimmed.replace(/"/g, '\\"')}"`;
}

function normalizeMermaidNodeLine(line: string, anonymousSubgraphSeq: { value: number }): string {
  if (/^\s*(?:%%|classDef|class |style |end\b)/i.test(line)) return line;

  if (/^\s*subgraph\b/i.test(line)) {
    const withId = line.replace(
      /subgraph\s+([A-Za-z][\w-]*)\s*\[([^\]]+)\]/i,
      (_m, id: string, label: string) => `subgraph ${id}[${quoteMermaidLabel(label)}]`
    );
    if (withId !== line) return withId;
    return line.replace(/subgraph\s+\[([^\]]+)\]/i, (_m, label: string) => {
      anonymousSubgraphSeq.value += 1;
      return `subgraph sg${anonymousSubgraphSeq.value}[${quoteMermaidLabel(label)}]`;
    });
  }

  return line
    .replace(/\b([A-Za-z][\w-]*)\s+\[([^\]]+)\]/g, (_m, nodeId: string, label: string) => {
      return `${nodeId}[${quoteMermaidLabel(label)}]`;
    })
    .replace(/\b([A-Za-z][\w-]*)\[([^\]]+)\]/g, (_m, nodeId: string, label: string) => {
      const trimmed = label.trim();
      if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
        return `${nodeId}[${trimmed}]`;
      }
      if (/[\u4e00-\u9fa5\s()（）]/.test(trimmed)) {
        return `${nodeId}[${quoteMermaidLabel(trimmed)}]`;
      }
      return `${nodeId}[${trimmed}]`;
    });
}

/** 模型常只列出节点不写连线，Mermaid 12 无法解析；在 subgraph 块内自动串联 */
function autoLinkMermaidNodes(text: string): string {
  const lines = text.split('\n');
  const out: string[] = [];
  let pending: string[] = [];
  const nodeLineRe = /^([A-Za-z][\w-]*)(\[.*\])?$/;

  const flush = () => {
    if (pending.length === 0) return;
    if (pending.length === 1) {
      out.push(pending[0]);
    } else if (pending.length <= 6) {
      for (let i = 0; i < pending.length - 1; i++) {
        out.push(`${pending[i]} --> ${pending[i + 1]}`);
      }
    } else {
      // 大量孤立节点：以首个为锚点放射连接，避免 LR 下排成超长横条
      const hub = pending[0];
      out.push(hub);
      for (let i = 1; i < pending.length; i++) {
        out.push(`${hub} --> ${pending[i]}`);
      }
    }
    pending = [];
  };

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) {
      flush();
      out.push(line);
      continue;
    }
    if (
      /^(flowchart|graph)\s/i.test(trimmed)
      || /^subgraph\b/i.test(trimmed)
      || /^end\s*$/i.test(trimmed)
      || /^%%/.test(trimmed)
      || /^classDef\b/i.test(trimmed)
      || /^class\b/i.test(trimmed)
      || /^style\b/i.test(trimmed)
    ) {
      flush();
      out.push(line);
      continue;
    }
    if (/-->|==>|-\.-?>/.test(trimmed)) {
      flush();
      out.push(line);
      continue;
    }
    if (nodeLineRe.test(trimmed)) {
      pending.push(trimmed);
      continue;
    }
    flush();
    out.push(line);
  }
  flush();
  return out.join('\n');
}

/**
 * 转换 Mermaid 流程图/拓扑图的方向（横向分支 LR ⇄ 竖向树状分支 TD）
 */
export function toggleMermaidDirection(code: string, targetDirection?: 'LR' | 'TD'): string {
  if (!code?.trim()) return code;
  const isCurrentlyTD = /^flowchart\s+(?:TD|TB)\b/im.test(code) || /^graph\s+(?:TD|TB)\b/im.test(code);
  const toLR = targetDirection ? targetDirection === 'LR' : isCurrentlyTD;
  let s = code;
  if (toLR) {
    s = s.replace(/^(?:flowchart|graph)\s+(?:TD|TB|LR|RL)\b/im, 'flowchart LR');
    // 仅有无连接子图切为看板时才保留子图内部方向；已有树根扇出的分支图保持顺畅流向
    if (!/\bEM_TREE_ROOT\b/.test(s)) {
      s = s.replace(/(\bsubgraph\s+[^\n]+\n)(?!\s*direction\b)/gi, '$1  direction TB\n');
    }
  } else {
    s = s.replace(/^(?:flowchart|graph)\s+(?:TD|TB|LR|RL)\b/im, 'flowchart TD');
    s = s.replace(/\n\s*direction\s+(?:TB|TD|LR|RL)\s*\n/gi, '\n');
  }
  return s;
}

function extractNodeIdToken(rawExpr: string): string | null {
  const m = rawExpr.trim().match(/^([A-Za-z][\w-]*)/);
  return m ? m[1] : null;
}

/**
 * 若图谱存在多个彼此完全孤立（0 跨子图边且无公共树根）的 subgraph，
 * Mermaid Dagre 引擎会竖直堆叠成一条高瘦竖列导致左右大片留白。
 * 本函数自动生成顶部总纲树根 EM_TREE_ROOT 并向各分支首节点平行连边，
 * 将孤立多块升级为自顶向下水平展开的「树状分支拓扑图谱」。
 */
function ensureBalancedTreeSubgraphs(text: string): string {
  const lines = text.split('\n');
  interface SgBlock {
    nodeIds: Set<string>;
    headNodeId: string | null;
  }
  const subgraphs: SgBlock[] = [];
  let currentSg: SgBlock | null = null;
  const edgeSourcesToTargets = new Map<string, Set<string>>();

  const recordEdge = (srcRaw: string, dstRaw: string) => {
    const src = extractNodeIdToken(srcRaw);
    const dst = extractNodeIdToken(dstRaw);
    if (!src || !dst) return;
    if (currentSg) {
      currentSg.nodeIds.add(src);
      currentSg.nodeIds.add(dst);
      if (!currentSg.headNodeId) currentSg.headNodeId = src;
    }
    if (!edgeSourcesToTargets.has(src)) {
      edgeSourcesToTargets.set(src, new Set());
    }
    edgeSourcesToTargets.get(src)!.add(dst);
  };

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || /^%%/.test(trimmed)) continue;
    if (/^subgraph\b/i.test(trimmed)) {
      currentSg = { nodeIds: new Set<string>(), headNodeId: null };
      subgraphs.push(currentSg);
      continue;
    }
    if (/^end\b/i.test(trimmed)) {
      currentSg = null;
      continue;
    }
    const edgeMatch = trimmed.match(
      /^([A-Za-z][\w-]*(?:\[[^\]]*\]|\([^)]*\)|\{[^}]*\})?)\s*(?:-->|-\.->|==>|<-->|~~~)(?:\|[^|]*\|)?\s*([A-Za-z][\w-]*(?:\[[^\]]*\]|\([^)]*\)|\{[^}]*\})?)/
    );
    if (edgeMatch) {
      recordEdge(edgeMatch[1], edgeMatch[2]);
      continue;
    }
    const standalone = extractNodeIdToken(trimmed);
    if (standalone && !/^(?:flowchart|graph|direction|classDef|class|style)\b/i.test(standalone) && currentSg) {
      currentSg.nodeIds.add(standalone);
      if (!currentSg.headNodeId) currentSg.headNodeId = standalone;
    }
  }

  const validSubgraphs = subgraphs.filter((sg) => Boolean(sg.headNodeId));
  if (validSubgraphs.length < 2) {
    return text;
  }

  const findSubgraphIndex = (nodeId: string): number =>
    validSubgraphs.findIndex((sg) => sg.nodeIds.has(nodeId));

  let hasCrossOrRootBridge = false;
  for (const [src, targets] of edgeSourcesToTargets.entries()) {
    const srcSg = findSubgraphIndex(src);
    const touchedTargetSgs = new Set<number>();
    for (const dst of targets) {
      const dstSg = findSubgraphIndex(dst);
      if (srcSg >= 0 && dstSg >= 0 && srcSg !== dstSg) {
        hasCrossOrRootBridge = true;
        break;
      }
      if (dstSg >= 0) {
        touchedTargetSgs.add(dstSg);
      }
    }
    if (hasCrossOrRootBridge || (srcSg < 0 && touchedTargetSgs.size >= 2)) {
      hasCrossOrRootBridge = true;
      break;
    }
  }

  if (hasCrossOrRootBridge) {
    // 已有跨子图桥接或树根扇出：若原本强加了子图 direction TB 导致整体被压扁，统一保持 TD 树形展平
    return text;
  }

  // 多个孤立子图完全无桥接：补齐顶部树根并在 TD 模式下并行展开各分支
  let updated = text
    .replace(/^(?:flowchart|graph)\s+(?:TD|TB|LR|RL)\b/im, 'flowchart TD')
    .replace(/\n\s*direction\s+(?:TB|TD|LR|RL)\s*\n/gi, '\n');

  const rootEdges = validSubgraphs.map((sg) => `EM_TREE_ROOT --> ${sg.headNodeId}`).join('\n');
  return `${updated.trimEnd()}\nEM_TREE_ROOT(["结构总纲"])\n${rootEdges}`;
}

/** 优化 Mermaid 图谱默认方向：多子图/分支图优先以「自顶向下树状分支」自然铺开，仅超长横向单链收敛为纵向 */
function preferVerticalCourseFlowchart(text: string): string {
  const subgraphCount = (text.match(/\bsubgraph\b/gi) || []).length;
  if (subgraphCount >= 2) {
    return ensureBalancedTreeSubgraphs(text);
  }

  const nodeCount = (text.match(/\[[^\]]+\]/g) || []).length;
  const edgeCount = (text.match(/-->/g) || []).length;
  const isWideLayout = /^flowchart\s+(?:LR|RL)\b/im.test(text) || /^graph\s+(?:LR|RL)\b/im.test(text);

  if (isWideLayout && (nodeCount > 6 || edgeCount > 8)) {
    return text
      .replace(/^flowchart\s+(?:LR|RL)\b/im, 'flowchart TD')
      .replace(/^graph\s+(?:LR|RL)\b/im, 'flowchart TD');
  }

  return text;
}

let mermaidSubgraphRepairSeq = 0;

/** 修复模型常输出的粘连语法：flowchart TDsubgraph基础层、-.推广.-> 等 */
function repairMermaidGluedSyntax(text: string): string {
  mermaidSubgraphRepairSeq = 0;
  let s = text.replace(/\r\n/g, '\n');

  // 1. 修复图方向与 subgraph 粘连（如「flowchart TDsubgraph」或「graph LRsubgraph」）
  s = s.replace(/\b((?:flowchart|graph)\s*(?:TD|TB|BT|RL|LR)?)\s*(subgraph\b)/gi, '$1\n$2 ');

  // 2. 修复节点末尾（如引文或括号结尾）紧贴 end 或 endsubgraph（如「A3[...]endsubgraph」）
  s = s.replace(/([\]\)"'])\s*endsubgraph\b/gi, '$1\nend\nsubgraph ');
  s = s.replace(/([\]\)"'])\s*(end\b)/gi, '$1\n$2');

  // 3. 修复 end 与 subgraph 粘连（如「endsubgraph S2」）
  s = s.replace(/\bend\s*(subgraph\b)/gi, 'end\n$1 ');

  s = s.replace(/\bsubgraph([\u4e00-\u9fa5][\u4e00-\u9fa5A-Za-z0-9_]*)(?=[ \t]|$)/g, (_m, title: string) => {
    mermaidSubgraphRepairSeq += 1;
    return `subgraph SG${mermaidSubgraphRepairSeq}["${title}"]`;
  });

  s = s
    .split('\n')
    .map((line) => {
      const onlyTitle = line.match(/^(\s*)subgraph\s+([\u4e00-\u9fa5][^\[\n"]+)\s*$/);
      if (onlyTitle) {
        mermaidSubgraphRepairSeq += 1;
        return `${onlyTitle[1]}subgraph SG${mermaidSubgraphRepairSeq}["${onlyTitle[2].trim()}"]`;
      }
      return line;
    })
    .join('\n');

  s = s.replace(/(subgraph\s+SG\d+\[[^\]]+\])\s+([A-Za-z_])/g, '$1\n  $2');
  s = s.replace(/(subgraph\s+"[^"]+")\s+([A-Za-z_])/g, '$1\n  $2');

  s = s.replace(/-\.([^.\n>-]+)\.-?>/g, '-.->|$1|');
  s = s.replace(/([^\s])-->/g, '$1 -->');
  s = s.replace(/-->([^\s|\[])/g, '--> $1');
  s = s.replace(/([^\n])\s+(subgraph\s+)/gi, '$1\n$2');
  s = s.replace(/([^\n])\s+\b(end\b)\s*$/gim, '$1\n$2');

  return s;
}

/** 去掉行首中文说明、注释化纯文案行、修正链式边与边标签写法 */
function sanitizeMermaidContentLines(text: string): string {
  const out: string[] = [];
  for (const line of text.split('\n')) {
    let s = line.trimEnd();
    const trimmed = s.trim();
    if (!trimmed) {
      out.push(s);
      continue;
    }

    if (/^(flowchart|graph)\s/i.test(trimmed) || /^%%/.test(trimmed)) {
      out.push(s);
      continue;
    }
    if (/^(subgraph|end|classDef|class|style)\b/i.test(trimmed)) {
      out.push(s);
      continue;
    }

    s = s.replace(/--\|([^|\n]+)\|>/g, '-->|$1|');
    s = s.replace(/==\|([^|\n]+)\|>/g, '==>|$1|');

    const nodeStart = trimmed.search(/\b[A-Za-z][\w-]*(\s*(?:\[|\(|\{)|-->|-\.->|==>|$)/);
    if (nodeStart > 0 && /[\u4e00-\u9fa5（）、：；。]/.test(trimmed.slice(0, nodeStart))) {
      s = trimmed.slice(nodeStart);
    }

    if (!/\b[A-Za-z][\w-]*(\s*(?:\[|\(|\{)|-->|-\.->|==>)/.test(s.trim()) && /[\u4e00-\u9fa5]/.test(s)) {
      out.push(`%% ${s.trim()}`);
      continue;
    }

    const chainParts = s.trim().split(/\s*-->\s*/);
    if (chainParts.length > 2) {
      for (let i = 0; i < chainParts.length - 1; i++) {
        out.push(`${chainParts[i].trim()} --> ${chainParts[i + 1].trim()}`);
      }
      continue;
    }

    out.push(s);
  }
  return out.join('\n');
}

function repairMermaidAggressive(code: string): string {
  const normalized = normalizeMermaidCode(code);
  const filtered = normalized
    .split('\n')
    .filter((line) => {
      const tr = line.trim();
      if (!tr) return true;
      if (/^(flowchart|graph|subgraph|end|%%|classDef|class|style)/i.test(tr)) return true;
      return /\b[A-Za-z][\w-]*(\[|-->|==>|-\.->)/.test(tr);
    })
    .join('\n');
  return normalizeMermaidCode(filtered);
}

export function normalizeMermaidCode(raw: string): string {
  let text = repairMermaidGluedSyntax(raw.trim().replace(/\r\n/g, '\n'));
  if (!text) return text;

  text = text.replace(
    /^(graph\s+(?:TD|TB|BT|RL|LR)|flowchart\s+(?:TD|TB|BT|RL|LR))\s+(?=\S)/im,
    (header) => `${header.trim()}\n`
  );
  text = text.replace(/\bend\s+subgraph\b/gi, 'end\nsubgraph');
  text = text.replace(/([^\n])\s+(subgraph\s+)/gi, '$1\n$2');
  text = text.replace(/([^\n])\s+end\s*$/gim, '$1\nend');

  if (!MERMAID_HINT.test(text)) {
    text = `flowchart TD\n${text}`;
  }

  text = text.replace(/^graph\s+(TD|TB|BT|RL|LR)\b/im, 'flowchart $1');

  const anonymousSubgraphSeq = { value: 0 };
  text = text
    .split('\n')
    .map((line) => normalizeMermaidNodeLine(line, anonymousSubgraphSeq))
    .join('\n');

  text = sanitizeMermaidContentLines(text);
  text = autoLinkMermaidNodes(text);

  // 模型常漏写 subgraph 的 end，先补齐 end 再分析子图连通性与树状结构
  const subgraphCount = (text.match(/\bsubgraph\b/gi) || []).length;
  const endCount = (text.match(/^\s*end\s*$/gim) || []).length;
  if (subgraphCount > endCount) {
    text += '\n' + 'end\n'.repeat(subgraphCount - endCount);
  }

  text = preferVerticalCourseFlowchart(text);

  return text;
}

export function readMermaidSource(wrapper: HTMLElement): string {
  const sourceEl = wrapper.querySelector<HTMLElement>('.mermaid-source');
  if (sourceEl?.textContent?.trim()) {
    return sourceEl.textContent.trim();
  }
  const diagram = wrapper.querySelector<HTMLElement>('.mermaid-diagram');
  const legacy = diagram?.dataset.mermaidCode || diagram?.dataset.code;
  if (legacy) {
    try {
      return decodeURIComponent(legacy).trim();
    } catch {
      return legacy.trim();
    }
  }
  return '';
}

function sanitizeMermaidFallbackLabel(label: string): string | null {
  let t = label.replace(/```+/g, '').replace(/^text/i, '').trim();
  // 彻底剔除首尾残留的 ASCII 树枝与竖线连接符（如末尾残留的 | 或 │，开头的 ─、+ 等）
  t = t.replace(/^[─\-—+|│\s]+/, '').replace(/[\s|│─\-—+]+$/, '').trim();
  if (!t || t.length < 2) return null;
  if (/^`+$/.test(t)) return null;
  return t;
}

function buildPlainHierarchyFallback(rawCode: string): string | null {
  const expanded = expandAsciiTreeToMultiline(rawCode);
  const lines = expanded
    .split('\n')
    .map((l) => l.replace(/```+/g, '').trim())
    .filter((l) => l && !/^text$/i.test(l));
  if (lines.length < 2 || lines.length > 36) return null;
  if (/(?:^|\n)\s*(?:graph|flowchart)\s/im.test(rawCode)) return null;
  if (/(?:-->|==>)/.test(rawCode)) return null;
  const items = lines
    .map((l) => l.replace(/^[│|\s]*[├└\+][─\-—\s]*/, '').replace(/^[-*•]\s*/, ''))
    .flatMap((l) => l.split(/(?:[│|]+[├└\+][─\-—\s]*)+/u))
    .map((l) => sanitizeMermaidFallbackLabel(l))
    .filter((l): l is string => Boolean(l));
  if (items.length < 2) return null;
  const lis = items.map((l) => `<li>${mdInstance().utils.escapeHtml(l)}</li>`).join('');
  return `<div class="mermaid-outline-fallback"><p class="mermaid-outline-title">层级结构概要（渲染失败时的可读版）</p><ul>${lis}</ul></div>`;
}

function buildMermaidOutlineFallback(rawCode: string): string {
  const plain = buildPlainHierarchyFallback(rawCode);
  if (plain) return plain;

  const labels: string[] = [];
  const seen = new Set<string>();
  const re = /\["((?:\\.|[^"\\])*)"\]|\[([^\]"]+)\]/g;
  let match: RegExpExecArray | null;
  while ((match = re.exec(rawCode)) !== null) {
    const rawLabel = (match[1] ?? match[2] ?? '').replace(/\\"/g, '"');
    const subParts = rawLabel
      .split(/(?:[│|]*[├└\+][─\-—\s]*)+/u)
      .map((p) => sanitizeMermaidFallbackLabel(p))
      .filter((p): p is string => Boolean(p));

    for (const part of subParts) {
      if (seen.has(part)) continue;
      seen.add(part);
      labels.push(part);
    }
  }
  if (labels.length === 0) {
    return `<div class="mermaid-fallback-box"><pre><code>${mdInstance().utils.escapeHtml(rawCode)}</code></pre></div>`;
  }
  const items = labels.map((l) => `<li>${mdInstance().utils.escapeHtml(l)}</li>`).join('');
  return `<div class="mermaid-outline-fallback"><p class="mermaid-outline-title">流程图结构概要（渲染失败时的可读版）</p><ul>${items}</ul></div>`;
}

function looksLikePlainHierarchyDiagram(code: string): boolean {
  const t = code.trim();
  if (!t || /(?:^|\n)\s*(?:graph|flowchart)\s/im.test(t)) return false;
  if (/(?:-->|==>)/.test(t)) return false;
  // ASCII 框线图、表格或流程框绝不属于平铺层级结构（应保留等宽代码框）
  if (/\+[-=]{3,}\+/.test(t) || /^[ \t]*\|.*\|[ \t]*$/m.test(t)) return false;
  if (/[┌├└│─┼]/.test(t)) return false;

  const lines = t.split('\n').map((l) => l.trim()).filter(Boolean);
  if (lines.length < 2 || lines.length > 24) return false;
  // 必须具有子级列表缩进或连字符特征
  const hasIndentOrBullet = lines.some((l) => /^[-*•]\s+/.test(l) || /^[ \t]{2,}/.test(l));
  if (!hasIndentOrBullet) return false;

  const joined = lines.join(' ');
  return /(?:JDK|JRE|JVM|字节码)/.test(joined);
}

function renderPlainHierarchyBlock(code: string): string {
  const html = buildPlainHierarchyFallback(code);
  if (html) return html;
  const escaped = mdInstance().utils.escapeHtml(code.trim());
  return `<div class="mermaid-outline-fallback"><p class="mermaid-outline-title">层级结构</p><pre class="hierarchy-plain-pre">${escaped}</pre></div>`;
}

const MERMAID_ZOOM_BTN =
  '<button type="button" class="mermaid-btn mermaid-btn--zoom" disabled title="双击图谱或点击此处全屏放大查看" aria-label="全屏放大查看">' +
  '<svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true">' +
  '<path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"/></svg><span>全屏</span></button>';

function buildMermaidWrapper(code: string): string {
  const trimmed = code.trim();
  const escaped = mdInstance().utils.escapeHtml(trimmed);
  const safeCode = encodeURIComponent(trimmed);
  return [
    '<div class="mermaid-diagram-wrapper" title="双击或点击右上角按钮全屏放大查看">',
    '<div class="mermaid-header">',
    '<span class="mermaid-header-title">系统流程图 · 全景预览</span>',
    `<div class="mermaid-toolbar">${MERMAID_ZOOM_BTN}</div>`,
    '</div>',
    `<pre class="mermaid-source" hidden aria-hidden="true">${escaped}</pre>`,
    `<div class="mermaid-diagram" data-mermaid-code="${safeCode}"><span class="mermaid-loading">流程图渲染中…</span></div>`,
    '</div>'
  ].join('');
}

function inferOrphanCodeLang(code: string): string {
  if (looksLikeAsciiKnowledgeTree(code)) return 'ascii-tree';
  if (looksLikeMermaid(code)) return 'mermaid';
  if (/<!DOCTYPE html>|<html\b|<\/?(?:head|body|form|table|div|p|h[1-6])\b/i.test(code)) {
    return 'html';
  }
  if (/(?:\/\*|\*\/|\b(?:font-family|margin|padding|background|color)\s*:|#[0-9a-fA-F]{3,8}\b)/i.test(code)) {
    return 'css';
  }
  if (/(?:^import \{|^export const|interface \w+|type \w+\s*=)/m.test(code)) return 'typescript';
  if (/(?:console\.log|document\.|const |let |var |function\b)/m.test(code)) return 'javascript';
  if (/(?:^def |^import |^from |print\(|asyncio\.)/m.test(code)) return 'python';
  if (/^(\$|curl|npm|pnpm|yarn|mvn|docker|git)\s/m.test(code)) return 'bash';
  if (/(?:public class|System\.out\.println|@SpringBootApplication)/m.test(code)) return 'java';
  if (/(?:SELECT .* FROM|INSERT INTO|CREATE TABLE)/i.test(code)) return 'sql';
  return 'text';
}

function inferCodeFenceLanguage(code: string, declared: string): string {
  const decl = normalizeDeclaredLang(declared);
  const text = code.trim();
  if (decl === 'mermaid' || decl === 'graph' || decl === 'flowchart') return 'mermaid';
  if (!decl && looksLikeMermaid(text)) return 'mermaid';
  if (decl === 'json' || (!decl && /^\s*[[{]/.test(text) && /"[\w_]+"\s*:/.test(text))) return 'json';
  if (decl && hljs.getLanguage(decl)) return decl;
  return inferOrphanCodeLang(text);
}

let mermaidCounter = 0;
const mermaidFailureLogged = new Set<string>();

function getStoredPreference<T>(_key: string, defaultValue: T): T {
  // 本项目无需运行时渲染偏好开关：公式、代码高亮与流程图统一默认开启
  return defaultValue;
}

function renderAsciiTreeBlock(code: string): string {
  const expanded = expandAsciiTreeToMultiline(code);
  const escaped = mdInstance().utils.escapeHtml(expanded);
  const copyBtn = `<button type="button" class="code-copy-btn" title="复制结构">复制</button>`;
  const langLabel = `<span class="code-lang">结构树</span>`;
  return [
    '<div class="code-block-wrapper ascii-tree-wrapper">',
    `<div class="code-header">${langLabel}${copyBtn}</div>`,
    `<pre class="ascii-tree-pre"><code>${escaped}</code></pre>`,
    '</div>'
  ].join('');
}

function renderAsciiDiagramBlock(code: string): string {
  const escaped = mdInstance().utils.escapeHtml(code.trimEnd());
  const copyBtn = `<button type="button" class="code-copy-btn" title="复制板书内容">复制</button>`;
  const langLabel = `<span class="code-lang">板书结构</span>`;
  return [
    '<div class="code-block-wrapper ascii-diagram-wrapper">',
    `<div class="code-header">${langLabel}${copyBtn}</div>`,
    `<pre class="ascii-diagram-pre"><code>${escaped}</code></pre>`,
    '</div>'
  ].join('');
}

export function looksLikeAsciiDiagram(code: string): boolean {
  const t = code.trim();
  // 含有标准制表框线符号、双横线、流程跳转箭头或经典 +---+ ASCII 框线
  return /[┌├└│─┼┬┴┐┤┘═]/.test(t) || (/[▼▲⇄]/.test(t) && /[|│]/.test(t)) || /\+[-=]{3,}\+/.test(t);
}

function renderCodeBlock(code: string, declaredLang: string, enableMermaid = true): string {
  if (!code?.trim()) return '';

  const mermaidAllowed = enableMermaid && getStoredPreference('mermaidEnabled', true);
  let lang = inferCodeFenceLanguage(code, declaredLang);
  if (!mermaidAllowed && lang === 'mermaid') {
    lang = 'text';
  }

  const decl = normalizeDeclaredLang(declaredLang);
  if (
    (decl === '' || decl === 'text' || decl === 'plain' || decl === 'txt') &&
    looksLikePlainHierarchyDiagram(code)
  ) {
    return renderPlainHierarchyBlock(code);
  }

  if (looksLikeAsciiKnowledgeTree(code)) {
    const mermaidSource = asciiTreeToMermaid(code);
    if (mermaidSource && mermaidAllowed) {
      return buildMermaidWrapper(mermaidSource);
    }
    return renderAsciiTreeBlock(code);
  }

  // 纯文本板书框线图、流程图专属美化渲染（免 Mermaid，原生等宽精准对齐）
  if (
    (decl === '' || decl === 'text' || decl === 'plain' || decl === 'txt' || decl === 'code') &&
    looksLikeAsciiDiagram(code)
  ) {
    return renderAsciiDiagramBlock(code);
  }

  if (lang === 'mermaid' && mermaidAllowed) {
    if (looksLikePlainHierarchyDiagram(code) && !looksLikeMermaid(code)) {
      return renderPlainHierarchyBlock(code);
    }
    return buildMermaidWrapper(code);
  }

  const highlightAllowed = getStoredPreference('codeHighlightEnabled', true);
  let highlighted = '';
  if (highlightAllowed && lang && hljs.getLanguage(lang)) {
    try {
      highlighted = hljs.highlight(code, { language: lang, ignoreIllegals: true }).value;
    } catch {
      highlighted = mdInstance().utils.escapeHtml(code);
    }
  } else if (highlightAllowed) {
    try {
      highlighted = hljs.highlightAuto(code).value;
    } catch {
      highlighted = mdInstance().utils.escapeHtml(code);
    }
  } else {
    highlighted = mdInstance().utils.escapeHtml(code);
  }

  const langLabel = `<span class="code-lang">${formatLangLabel(lang)}</span>`;
  const copyBtn = `<button type="button" class="code-copy-btn" title="复制代码">复制</button>`;

  return `<div class="code-block-wrapper"><div class="code-header">${langLabel}${copyBtn}</div><pre class="hljs"><code class="language-${lang || 'text'}">${highlighted}</code></pre></div>`;
}

/* 渲染规则与全局事件委托已随 ensureMarkdownEngine() 惰性注册，行为与原先一致 */

/** 表格内行内 code 去掉标签包裹（Code Compass 同款） */
export function unwrapInlineCodeInTables(html: string): string {
  if (!html || typeof document === 'undefined') return html;
  const doc = new DOMParser().parseFromString(`<div id="md-root">${html}</div>`, 'text/html');
  const root = doc.getElementById('md-root');
  if (!root) return html;

  root.querySelectorAll('td code, th code').forEach((codeEl) => {
    if (codeEl.closest('pre')) return;
    codeEl.replaceWith(doc.createTextNode(codeEl.textContent || ''));
  });

  root.querySelectorAll('td .code-block-wrapper, th .code-block-wrapper').forEach((wrapper) => {
    const codeEl = wrapper.querySelector('code');
    const text = (codeEl?.textContent || wrapper.querySelector('pre')?.textContent || '').trim();
    const pre = doc.createElement('pre');
    pre.className = 'table-cell-code';
    pre.textContent = text;
    wrapper.replaceWith(pre);
  });

  return root.innerHTML;
}

/**
 * Copilot 聊天专用 Markdown 渲染（Code Compass renderMarkdownForChat + 文章同款 KaTeX 占位流程）
 */
export function renderMarkdownForChat(content: string): string {
  return renderMarkdownPipeline(content, true, true);
}

/** 深度思考区：不走表格容错，且不渲染 Mermaid 图谱（仅正文渲染） */
export function renderMarkdownForReasoning(content: string): string {
  return renderMarkdownPipeline(content, false, false);
}

/**
 * 纯行内 Markdown + KaTeX 渲染（适用于列表行 / 折叠摘要等受限行数场景，仅输出行内安全 HTML）。
 */
export function renderInlineMarkdown(content: string): string {
  if (!content?.trim()) return '';
  const blockKatexMap = new Map<string, string>();
  const inlineKatexMap = new Map<string, string>();
  const counters = { blockIdx: 0, inlineIdx: 0 };

  const processed = extractMathPlaceholders(
    normalizeLatexDelimiters(content),
    blockKatexMap,
    inlineKatexMap,
    counters,
    false
  );

  let renderedHtml = mdInstance().renderInline(processed);
  blockKatexMap.forEach((htmlVal, key) => {
    renderedHtml = renderedHtml.replaceAll(key, htmlVal);
  });
  inlineKatexMap.forEach((htmlVal, key) => {
    renderedHtml = renderedHtml.replaceAll(key, htmlVal);
  });
  return sanitizeRenderedHtml(renderedHtml);
}

function stripKatexErrorRed(html: string): string {
  return html
    .replace(/style="color:\s*#cc0000\b/gi, 'style="color:inherit')
    .replace(/mathcolor="#cc0000"/gi, 'mathcolor="inherit"');
}

function renderKatexHtml(formula: string, displayMode: boolean): string {
  if (!getStoredPreference('katexEnabled', true)) {
    return displayMode
      ? `<pre class="math-raw">${mdInstance().utils.escapeHtml(formula)}</pre>`
      : `<code class="math-raw">${mdInstance().utils.escapeHtml(formula)}</code>`;
  }
  const fixed = prepareKatexFormula(repairMalformedLeftRight(formula.trim()), displayMode);
  const html = katex.renderToString(fixed, {
    displayMode,
    throwOnError: false,
    strict: 'ignore',
    errorColor: 'inherit'
  });
  if (!html.includes('katex-error')) return stripKatexErrorRed(html);

  const fallback = fixed
    .replace(/\\left[\[\({|.]/g, (m) => (m.endsWith('[') ? '[' : m.endsWith('(') ? '(' : m.endsWith('{') ? '{' : ''))
    .replace(/\\right[\]\)}.|]/g, (m) => (m.endsWith(']') ? ']' : m.endsWith(')') ? ')' : m.endsWith('}') ? '}' : ''));
  const retry = katex.renderToString(fallback, {
    displayMode,
    throwOnError: false,
    strict: 'ignore',
    errorColor: 'inherit'
  });
  return stripKatexErrorRed(retry.includes('katex-error') ? html : retry);
}

function extractMathPlaceholders(
  segment: string,
  blockKatexMap: Map<string, string>,
  inlineKatexMap: Map<string, string>,
  counters: { blockIdx: number; inlineIdx: number },
  blockDisplayMode: boolean
): string {
  let s = segment;
  // 1. 隔离表格内嵌代码块（<div class="chat-table-code-block">...</div>），
  // 避免表格单元格代码块内部的成对 $、环境变量或模板语法被误当成 KaTeX 数学公式渲染
  const tableCodeBlockPlaceholders: string[] = [];
  s = s.replace(/<div class="chat-table-code-block">[\s\S]*?<\/pre><\/div>/g, (htmlBlock) => {
    const key = `%%%CHAT_TABLE_CODE_BLOCK_${tableCodeBlockPlaceholders.length}%%%`;
    tableCodeBlockPlaceholders.push(htmlBlock);
    return key;
  });

  // 2. 隔离真实行内代码（`...`），防止行内代码中的 $PATH、$HOME、${{ ... }} 与外部 $ 符号被错误配对为 KaTeX 公式
  const inlineCodeSpanList: string[] = [];
  s = s.replace(/`[^`\n]+`/g, (codeSpan) => {
    const key = `%%%PIPELINE_INLINE_CODE_${inlineCodeSpanList.length}%%%`;
    inlineCodeSpanList.push(codeSpan);
    return key;
  });

  // 3. 隔离纯货币金额（如 $5、$12.5 元/月、$100，另计），防止正文中多个独立价格中的 $ 跨文本错误配对为 KaTeX 公式。
  // 【防回归关键防护】：
  //   - 显式带货币单位或周期（如 $5元、$12.5 元/月、$100 万元、$5k、$10/mo）：直接判定为货币。
  //   - 未带单位的纯数字价格（如 $5、$100）：仅在后接中文标点（$5，）、中文字词（$5左右）、价格区间（$5 - $10）或行尾时判定为货币，
  //     严禁误伤以数字开头的合法数学公式（如 $1 + 2 = 3$、$1 \le x$、$0 \le P$、$1 / 2$、$2 \pi r$）！
  const currencySpanList: string[] = [];
  s = s.replace(
    /(?<![\$\\a-zA-Z0-9])\$(?:\d[\d,]*(?:\.\d+)?[ \t]*(?:[kKmMbBwW]|元|美元|万元|块|刀|美金|RMB|CNY|USD)|\d[\d,]*(?:\.\d+)?(?=[ \t]*(?:[\u4e00-\u9fa5]|[-~至到][ \t]*\$\d|(?:\/|per)[ \t]*(?:mo|yr|month|year|day|week)\b)|[，。；、！？：）)\]】」』]|(?:\r?\n|$)))(?!\$)/g,
    (currencySpan) => {
      const key = `%%%PIPELINE_CURRENCY_${currencySpanList.length}%%%`;
      currencySpanList.push(currencySpan);
      return key;
    }
  );

  s = s.replace(/\$\$([\s\S]+?)\$\$/g, (_match, math: string) => {
    const key = `%%%KATEX_BLOCK_${counters.blockIdx++}%%%`;
    const normalizedMath = math.replace(/，/g, ', ').replace(/；/g, '; ');
    const rendered = renderKatexHtml(normalizedMath, blockDisplayMode);
    blockKatexMap.set(
      key,
      blockDisplayMode ? `<div class="katex-block-wrapper">${rendered}</div>` : rendered
    );
    return key;
  });

  // 阶段 1：先提取常规单行单 $ 公式
  s = s.replace(/(?<!\$)\$(?!\$)([^$\n]+?)(?<!\$)\$(?!\$)/g, (_match, math: string) => {
    const trimmed = math.trim();
    if (FILE_PATH_OR_CLI_RE.test(trimmed) || isNonMathFilePathFormula(trimmed)) {
      return `<code class="chat-inline-code">${mdInstance().utils.escapeHtml(trimmed)}</code>`;
    }
    // 关键防护 1：排除含句子终结符（句号、问号、感叹号）的跨句子长自然语言段落
    if (/[。！？]/.test(trimmed)) {
      return _match;
    }
    // 关键防护 2：若包含全角逗号或分号，必须具备明确数学运算/变量/等号特征且长度合理，防止正文中多个独立金额间错误配对
    if (/[，；]/.test(trimmed)) {
      const hasMathChars = /[=<>+\-*/^_\\≤≥≠≈~()]|\b[a-zA-Z]\b/.test(trimmed);
      if (!hasMathChars || trimmed.length > 80) {
        return _match;
      }
    }
    // 将公式内部的全角中文逗号与分号归一化为半角标点，使 KaTeX 能够正常解析并呈现高品质排版
    const normalizedMath = math.replace(/，/g, ', ').replace(/；/g, '; ');
    const key = `%%%KATEX_INLINE_${counters.inlineIdx++}%%%`;
    inlineKatexMap.set(key, renderKatexHtml(normalizedMath, false));
    return key;
  });

  // 阶段 2：再提取经过严格特征校验的合法跨行单 $ 公式（如连续推导等式链、多行公式环境）
  s = s.replace(/(?<!\$)\$(?!\$)([\s\S]+?)(?<!\$)\$(?!\$)/g, (match, math: string) => {
    if (!isSafeMultilineMath(math)) {
      return match;
    }
    const trimmed = math.trim();
    if (FILE_PATH_OR_CLI_RE.test(trimmed)) {
      return `<code class="chat-inline-code">${mdInstance().utils.escapeHtml(trimmed)}</code>`;
    }
    const normalizedMath = math.replace(/，/g, ', ').replace(/；/g, '; ');
    const key = `%%%KATEX_INLINE_${counters.inlineIdx++}%%%`;
    inlineKatexMap.set(key, renderKatexHtml(normalizedMath, false));
    return key;
  });

  currencySpanList.forEach((currencySpan, idx) => {
    s = s.replaceAll(`%%%PIPELINE_CURRENCY_${idx}%%%`, () => currencySpan);
  });

  inlineCodeSpanList.forEach((codeSpan, idx) => {
    s = s.replaceAll(`%%%PIPELINE_INLINE_CODE_${idx}%%%`, () => codeSpan);
  });

  tableCodeBlockPlaceholders.forEach((htmlBlock, idx) => {
    s = s.replaceAll(`%%%CHAT_TABLE_CODE_BLOCK_${idx}%%%`, () => htmlBlock);
  });

  return s;
}

function renderMarkdownPipeline(content: string, normalizeTables: boolean, enableMermaid = true): string {
  if (!content?.trim()) return '';

  const fenceMentionMap = new Map<string, string>();
  const fenced = normalizeCodeFences(content, fenceMentionMap);
  let processed = normalizeTables
    ? normalizeLatexDelimiters(normalizeChatTables(fenced))
    : normalizeLatexDelimiters(fenced);

  const blockKatexMap = new Map<string, string>();
  const inlineKatexMap = new Map<string, string>();
  const counters = { blockIdx: 0, inlineIdx: 0 };

  // 必须只在非代码块的正文中提取 KaTeX 公式，代码块（``` ... ```）内部绝对不提取，
  // 避免将代码块内的变量语法破坏性渲染为 KaTeX 数学公式。
  processed = processed
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      return extractMathPlaceholders(segment, blockKatexMap, inlineKatexMap, counters, true);
    })
    .join('');

  let renderedHtml = mdInstance().render(processed, { enableMermaid });

  blockKatexMap.forEach((htmlVal, key) => {
    renderedHtml = renderedHtml.replace(`<p>${key}</p>`, htmlVal);
    renderedHtml = renderedHtml.replaceAll(`<br>\n${key}`, htmlVal);
    renderedHtml = renderedHtml.replaceAll(`<br>${key}`, htmlVal);
    renderedHtml = renderedHtml.replaceAll(`${key}\n<br>`, htmlVal);
    renderedHtml = renderedHtml.replaceAll(`${key}<br>`, htmlVal);
    renderedHtml = renderedHtml.replaceAll(key, htmlVal);
  });
  inlineKatexMap.forEach((htmlVal, key) => {
    renderedHtml = renderedHtml.replaceAll(key, htmlVal);
  });
  if (fenceMentionMap.size > 0) {
    fenceMentionMap.forEach((htmlVal, key) => {
      renderedHtml = renderedHtml.replaceAll(key, htmlVal);
    });
  }

  const unwrapHtml = unwrapInlineCodeInTables(renderedHtml);
  const iconizedHtml = replaceSectionEmojisWithIcons(unwrapHtml);
  return sanitizeRenderedHtml(iconizedHtml);
}

export function sanitizeRenderedHtml(html: string): string {
  if (!html) return '';
  return DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true, mathMl: true, svg: true },
    ADD_TAGS: ['semantics', 'annotation', 'use', 'sup'],
    ADD_ATTR: [
      'target', 'rel', 'data-mermaid-code', 'data-bound', 'data-citation-idx',
      'aria-hidden', 'viewBox', 'fill', 'stroke', 'stroke-width',
      'stroke-linecap', 'stroke-linejoin'
    ],
    FORBID_TAGS: ['script', 'iframe', 'object', 'embed', 'form'],
    FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover', 'onfocus', 'onblur']
  });
}

export async function renderMermaidInElement(root: HTMLElement | null) {
  if (!root || typeof document === 'undefined') return;

  // 事件委托兜底：存在「只调用本方法、从未渲染过 Markdown 正文」的页面，
  // 此时委托尚未随引擎初始化挂载，放大按钮会点击无响应，故在此补挂一次（幂等）。
  bindGlobalMarkdownDelegates();
  ensureMermaidInitialized();
  cleanupOrphanMermaidDom();

  const wrappers = root.querySelectorAll<HTMLElement>(
    '.mermaid-diagram-wrapper:not([data-mermaid-done="true"])'
  );

  for (const wrapper of Array.from(wrappers)) {
    if (wrapper.dataset.mermaidRendering === '1') continue;

    // 深度思考区、流式输出中：不渲染图谱（避免半截语法反复失败并污染 body）
    if (
      wrapper.closest('.reasoning-card')
      || wrapper.closest('.is-streaming-bubble')
      || wrapper.closest('.is-streaming')
    ) {
      continue;
    }

    const el = wrapper.querySelector<HTMLElement>('.mermaid-diagram');
    if (!el) {
      wrapper.setAttribute('data-mermaid-done', 'true');
      continue;
    }

    const rawCode = readMermaidSource(wrapper);
    if (!rawCode) {
      el.innerHTML = '<span class="mermaid-error">图谱内容为空</span>';
      wrapper.setAttribute('data-mermaid-done', 'true');
      continue;
    }

    wrapper.dataset.mermaidRendering = '1';
    const id = `mermaid-graph-${Date.now()}-${mermaidCounter++}`;

    /** 应用渲染结果：统一收敛 SVG 尺寸、放大按钮可用态与完成标记 */
    const applyRenderedSvg = (
      svgMarkup: string,
      bindFunctions?: (element: Element) => void
    ) => {
      el.innerHTML = sanitizeMermaidSvg(svgMarkup);
      bindFunctions?.(el);
      wrapper.setAttribute('data-mermaid-done', 'true');
      const zoomBtn = wrapper.querySelector<HTMLButtonElement>('.mermaid-btn--zoom');
      if (zoomBtn) {
        zoomBtn.disabled = false;
        zoomBtn.title = '全屏放大查看';
      }
      const svgEl = el.querySelector('svg');
      if (svgEl) {
        svgEl.style.maxHeight = '320px';
        svgEl.style.maxWidth = '100%';
        svgEl.style.height = 'auto';
        svgEl.style.width = 'auto';
      }
      el.style.maxHeight = '350px';
    };

    try {
      // 原始源码优先渲染：合法的 Mermaid 语法零损保真（尤其带标签的边与 TD/LR 方向）
      const result = await renderMermaidSvg(id, rawCode);
      applyRenderedSvg(
        result.svg,
        result.bindFunctions as ((element: Element) => void) | undefined
      );
    } catch (firstErr) {
      const normalized = normalizeMermaidCode(rawCode);
      try {
        const result = await renderMermaidSvg(`${id}-normalized`, normalized);
        applyRenderedSvg(
          result.svg,
          result.bindFunctions as ((element: Element) => void) | undefined
        );
      } catch {
        const simplified = normalized
          .split('\n')
          .filter((line) => !/^\s*subgraph\b/i.test(line) && !/^\s*end\s*$/i.test(line))
          .join('\n');
        try {
          const result = await renderMermaidSvg(`${id}-retry`, simplified);
          applyRenderedSvg(
            result.svg,
            result.bindFunctions as ((element: Element) => void) | undefined
          );
        } catch {
          try {
            const aggressive = repairMermaidAggressive(rawCode);
            const result = await renderMermaidSvg(`${id}-aggressive`, aggressive);
            applyRenderedSvg(
              result.svg,
              result.bindFunctions as ((element: Element) => void) | undefined
            );
          } catch {
            const logKey = normalized.slice(0, 160);
            if (!mermaidFailureLogged.has(logKey)) {
              mermaidFailureLogged.add(logKey);
              console.warn('[Mermaid] render failed:', firstErr);
              console.warn('[Mermaid] normalized snippet:', normalized.slice(0, 400));
            }
            el.innerHTML = buildMermaidOutlineFallback(rawCode);
            wrapper.setAttribute('data-mermaid-done', 'true');
          }
        }
      }
    } finally {
      delete wrapper.dataset.mermaidRendering;
    }
  }

  cleanupOrphanMermaidDom();
}

export function bindMarkdownCodeCopy(
  root: HTMLElement | null,
  options?: { renderMermaid?: boolean }
) {
  if (!root) return;
  // 事件委托兜底：存在「只调用本方法、从未渲染过 Markdown 正文」的页面，
  // 此时委托尚未随引擎初始化挂载，复制按钮会点击无响应，故在此补挂一次（幂等）。
  bindGlobalMarkdownDelegates();
  root.querySelectorAll<HTMLButtonElement>('.code-copy-btn').forEach((btn) => {
    if (btn.dataset.bound === '1') return;
    btn.dataset.bound = '1';
    btn.type = 'button';
    btn.addEventListener('click', async (e) => {
      e.preventDefault();
      e.stopPropagation();
      const code = btn.closest('.code-block-wrapper')?.querySelector('code')?.textContent || '';
      try {
        await navigator.clipboard.writeText(code);
        const prevText = btn.textContent || '复制';
        btn.textContent = '✓ 已复制';
        btn.classList.add('is-copied');
        setTimeout(() => {
          btn.textContent = prevText;
          btn.classList.remove('is-copied');
        }, 1500);
      } catch {
        /* clipboard blocked */
      }
    });
  });

  if (options?.renderMermaid !== false) {
    void renderMermaidInElement(root);
  }
}

/* 全局复制/放大委托监听已随 ensureMarkdownEngine() 惰性注册，行为与原先一致 */
