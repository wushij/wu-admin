import katex from 'katex';

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/**
 * 修复模型输出里 LaTeX 命令与后续字母/变量粘连导致 KaTeX 抛出 Undefined control sequence 错误
 * 例如：\leS_n → \le S_n、\leX → \le X、\geS → \ge S、\inR → \in R、\toA → \to A 等
 */
export function repairCommonLatexSpacing(latex: string): string {
  if (!latex) return '';
  let s = latex;

  // 1. \le / \ge / \ne 紧贴大写字母（如 \leS、\geX、\neA），LaTeX 中绝无此类大写命令，必为变量粘连
  s = s.replace(/\\le(?=[A-Z])/g, '\\le ');
  s = s.replace(/\\ge(?=[A-Z])/g, '\\ge ');
  s = s.replace(/\\ne(?=[A-Z])/g, '\\ne ');

  // 2. \le 紧贴小写字母（排除 \left, \leq, \less..., \leadsto, \lbrack, \lbrace）
  s = s.replace(/\\le(?![ftq]|ss|ad|b[ra])(?=[a-z])/g, '\\le ');

  // 3. \ge 紧贴小写字母（排除 \geq, \gets）
  s = s.replace(/\\ge(?![q]|ts)(?=[a-z])/g, '\\ge ');

  // 4. \ne 紧贴小写字母（排除 \neq, \nearrow, \neg）
  s = s.replace(/\\ne(?![qg]|arrow)(?=[a-z])/g, '\\ne ');

  // 5. 集合包含与属于关系（如 x \inR, x \notinS）
  s = s.replace(/\\in(?=[A-Z])/g, '\\in ');
  s = s.replace(/\\notin(?=[A-Za-z])/g, '\\notin ');

  // 6. 趋近关系 \to（排除 \top, \tbinom, \today）
  s = s.replace(/\\to(?=[A-Z])/g, '\\to ');
  s = s.replace(/\\to(?![pbd])(?=[a-z])/g, '\\to ');

  // 7. 正负号 \pm（排除 \pmod）、\mp
  s = s.replace(/\\pm(?=[A-Z])/g, '\\pm ');
  s = s.replace(/\\pm(?!od)(?=[a-z])/g, '\\pm ');
  s = s.replace(/\\mp(?=[A-Za-z])/g, '\\mp ');

  // 8. 等价与相似关系 \sim（排除 \simeq）、\approx（排除 \approxeq）、\equiv
  s = s.replace(/\\sim(?!eq)(?=[A-Za-z])/g, '\\sim ');
  s = s.replace(/\\approx(?!eq)(?=[A-Za-z])/g, '\\approx ');
  s = s.replace(/\\equiv(?=[A-Za-z])/g, '\\equiv ');

  // 9. 独立长命令（必须保护前缀，例如 \subseteq 与 \subset）
  s = s.replace(/\\subseteq(?=[A-Za-z])/g, '\\subseteq ');
  s = s.replace(/\\subset(?!eq)(?=[A-Za-z])/g, '\\subset ');
  s = s.replace(/\\supseteq(?=[A-Za-z])/g, '\\supseteq ');
  s = s.replace(/\\supset(?!eq)(?=[A-Za-z])/g, '\\supset ');

  const otherCmds = [
    'quad',
    'qquad',
    'Rightarrow',
    'Leftarrow',
    'Leftrightarrow',
    'rightarrow',
    'leftarrow',
    'leq',
    'geq',
    'neq',
    'mid',
    'cdot',
    'times',
    'parallel',
    'perp'
  ];
  for (const cmd of otherCmds) {
    s = s.replace(new RegExp(`\\\\${cmd}(?=[A-Za-z])`, 'g'), `\\${cmd} `);
  }

  return s;
}

/**
 * 修复因 JSON/SQL/大模型多重转义导致的 LaTeX 命令双斜杠问题（如 \\frac, \\cos, \\sin, \\ln, \\sim, \\to 等）
 * 以及大模型常误输出的非法数字转义宏（如 \0 \cdot \infty、\1^\infty、\0^0 等）
 * 在 LaTeX 中，单个斜杠加字母是命令；双斜杠 \\ 紧跟字母会被 KaTeX 误当作“强制换行 + 普通英文字母”；
 * 而反斜杠后直接跟数字（如 \0, \1）在 LaTeX 中是非法未定义指令，会导致 KaTeX 抛出 Undefined control sequence 并渲染为红色报错字符。
 */
export function repairLatexDoubleEscapes(text: string): string {
  if (!text) return '';
  let s = text
    .replace(/\\{2,}([a-zA-Z]+)/g, '\\$1')
    .replace(/\\+([0-9])/g, '$1')
    // 保护 LaTeX 换行间距参数（如 \\[2pt]、\\[5pt]、\\[1em] 等），绝不可误替换为 \[
    .replace(/\\{2,}(\(|\)|\[(?!-?\d+(?:\.\d+)?(?:pt|em|ex|cm|mm|in)\b)|\])/g, '\\$1')
    // 无论模型或 JSON 转义输出 1 个、3 个或多个反斜杠，统一规范为 LaTeX 合法换行行距参数 \\[2pt]
    .replace(/\\+\[(-?\d+(?:\.\d+)?\s*(?:pt|em|ex|cm|mm|in)\s*\])/g, (_m, arg) => `\\\\[${arg}`);

  // 智能修复：Java/JSON 转义丢失反斜杠导致 \right 变成 ight 的高发问题（如 \left( ... ight)、\left. ... ight| 等）
  s = s.replace(/(?<![a-zA-Z\\])ight(?=\s*[\(\)\[\]\{\}|._^\s0-9]|$)/g, '\\right');

  return repairCommonLatexSpacing(s);
}

/**
 * =========================================================================================
 * 【重要警示：数学公式跨行识别核心判定函数 - 修改前必读防回归清单】
 * =========================================================================================
 * 背景与历史教训：
 * 1. 大模型在输出推导过程（如“裂项相消”、“链式法则”等）时，习惯用单个 $ 包裹且内部每一行以 "=" 换行书写。
 * 2. 但是，千万不可直接在全局正则中盲目使用单 $ 跨行匹配（如 `\$[\s\S]+?\$`）！
 *    因为一旦放开跨行限制，如果在长文本中存在两个孤立的美元符号（例如：“商品A售价 $10。\n商品B售价 $20。”），
 *    会导致整整大段正文及中文被错误当成公式送入 KaTeX，产生严重的排版破坏与灾难性回归！
 *
 * 【修改此函数及相关渲染逻辑时的强制防回归清单 (Checklist)】：
 * - [ ] 1. 绝对禁跨空行：两个 $ 之间若存在空行（\n\s*\n），必须认定为跨段落文本，绝不可作为公式合并！
 * - [ ] 2. 货币与价格安全：跨行的普通美元价格（如 $10 ... \n ... $20）必须保持为纯文本，绝不能被解析为 KaTeX！
 * - [ ] 3. 命令行与路径安全：CLI 提示符（$ npm run dev、$ python）与 Windows 路径必须保持为行内代码！
 * - [ ] 4. 中文句子保护：若内部包含句号、感叹号、问号等中文标点，说明是普通正文，不可当成公式吞噬！
 * - [ ] 5. 修改后必须执行测试：
 *          npm test -- src/utils/ai/chat-markdown.spec.ts
 *          npm test -- src/utils/format/render-math.spec.ts
 * =========================================================================================
 *
 * 校验跨行单 $ 包裹的内容是否为合法的多行数学公式
 */
export function isSafeMultilineMath(content: string): boolean {
  if (!content) return false;
  // 1. 决不能跨空行（即跨段落）
  if (/\n\s*\n/.test(content)) return false;

  const trimmed = content.trim();
  if (!trimmed || trimmed.length > 800) return false;
  const lines = trimmed.split('\n');
  if (lines.length > 15) return false;

  // 2. 决不能包含中文句子终结标点
  if (/[。！？；]/.test(trimmed)) return false;

  // 3. 中文字符总数限制
  const chineseChars = trimmed.match(/[\u4e00-\u9fa5]/g);
  if (chineseChars && chineseChars.length > 8) return false;

  // 4. 必须具备显著的数学特征
  const hasLatexCmd = /\\[a-zA-Z]+/.test(trimmed);
  const hasAlignedLines =
    lines.length > 1 &&
    lines.slice(1).some((l) => /^\s*(=|\+|-|\*|\\le|\\ge|\\approx|\\to|\\cdot)/.test(l));
  const hasMathSymbols = /[=+\-*/^_{}\\]/.test(trimmed);

  return (hasLatexCmd || hasAlignedLines) && hasMathSymbols;
}

/** 排除文件路径、命令提示符、程序语法的正则 */
const NON_MATH_CLI_OR_FILE_RE =
  /^[a-zA-Z]:[\\/]|^\.{1,2}[\\/]|^\/(?:bin|boot|dev|etc|home|usr|var|tmp)\/|https?:\/\/|[a-zA-Z0-9_-]+\.(?:exe|py|java|class|jar|war|ts|js|jsx|tsx|vue|css|scss|html|json|xml|yml|yaml|sh|bash|bat|cmd|ps1|c|cpp|h|hpp|go|rs|sql|ini|conf|log|csv|md|txt)\b/i;

/**
 * 校验独占一行或紧随推导引导词的单 $ 公式是否为独立演算推导等式/不等式
 */
export function isPromotableDerivationEquation(formula: string): boolean {
  if (!formula) return false;
  const t = formula.trim();
  // 长度适中（避免单个符号或超长段落）
  if (t.length < 4 || t.length > 600) return false;
  // 绝对排除 CLI 命令行或文件路径
  if (NON_MATH_CLI_OR_FILE_RE.test(t)) return false;
  // 绝对不能有句末中文标点（有标点说明是带公式的普通中文句子）
  if (/[。！？；]/.test(t)) return false;
  // 中文字符总数不超过 4 个
  const cn = t.match(/[\u4e00-\u9fa5]/g);
  if (cn && cn.length > 4) return false;

  // 必须具备明确的数学推导或关系特征：
  // 1. 等号、不等号、推导箭头、全等、相似、约等：=, ≠, ≤, ≥, <, >, \le, \ge, \ne, \approx, \sim, \implies, \to
  const hasRelation =
    /[=≠≤≥<>]|\\(?:le|leq|ge|geq|ne|neq|approx|sim|implies|Rightarrow|to|rightarrow|equiv)\b/.test(t);
  // 2. 经典微积分、分式、求和、根号、括号、上下标
  const hasLatexOrCalculus =
    /\\(?:frac|dfrac|sum|prod|int|lim|sqrt|left|right|boxed)\b|\^|_/.test(t);

  return hasRelation || hasLatexOrCalculus;
}

/**
 * 智能将大模型输出中独占一行或紧随推导引导词（如「整理得：」「化简得：」「即：」）的单 $ 推导等式提升为 $$...$$ 独立块级公式，
 * 使其获得优雅的水平居中展示与段落留白（对齐教材标准排版），彻底避免挤在行内靠左对齐。
 */
export function promoteStandaloneMathEquationsToBlock(text: string): string {
  if (!text || !text.includes('$')) return text;

  // 1. 匹配带推导引导词（如「整理得：」「两式相减得：」「即」等）且紧接单 $ 推导等式的行
  // 例如：「整理得：$a_1^2 -2a_1 +1 =0$」 -> 「整理得：\n$$a_1^2 -2a_1 +1 =0$$」
  const leadinKeywords =
    '即|整理得|化简得|代入得|因式分解得|解得|展开得|两式相减得|两式相加得|两式相除得|移项得|移项整理得|移项并整理得|由此可得|由题意得|由题意有|两边同时?平方得|中间项(?:全部|相互)?抵消[，,得：:]*';

  const leadinRe = new RegExp(
    `(^|\\n)([ \\t]*(?:[一二三四五六七八九十\\d]+[、.．]\\s*)?(?:${leadinKeywords})[：:]?[ \\t]*)\\$(?!\\$)([^$\\n]+?)(?<!\\$)\\$([。；;]?[ \\t]*)(?=\\n|$)`,
    'gu'
  );

  let s = text.replace(leadinRe, (match, prefix, leadin, formula, punctuation) => {
    if (!isPromotableDerivationEquation(formula)) {
      return match;
    }
    const cleanFormula = formula.trim();
    const punct = punctuation.trim();
    return `${prefix}${leadin.trimEnd()}\n$$${cleanFormula}${punct}$$\n`;
  });

  // 2. 匹配纯独占一行（前后无正文汉字，仅允许首尾空白及行末标点）的单 $ 演算推导公式
  // 例如：「 $4S_1 =4a_1 = (a_1 +1)^2$」 -> 「\n$$4S_1 =4a_1 = (a_1 +1)^2$$\n」
  s = s.replace(
    /(^|\n)([ \t]*)\$(?!\$)([^$\n]+?)(?<!\$)\$([。；;]?[ \t]*)(?=\n|$)/g,
    (match, prefix, _indent, formula, punctuation) => {
      if (!isPromotableDerivationEquation(formula)) {
        return match;
      }
      const cleanFormula = formula.trim();
      const punct = punctuation.trim();
      return `${prefix}\n$$${cleanFormula}${punct}$$\n`;
    }
  );

  return s;
}

function findMatchingBrace(str: string, openIndex: number): number {
  let depth = 0;
  for (let i = openIndex; i < str.length; i++) {
    if (str[i] === '\\' && i + 1 < str.length) {
      i++;
      continue;
    }
    if (str[i] === '{') depth++;
    else if (str[i] === '}') {
      depth--;
      if (depth === 0) return i;
    }
  }
  return -1;
}

/**
 * 提升含高耸/深层结构的分式为 \dfrac：
 * 当分式的分子或分母包含根号（特别是高阶开方 \sqrt[3]{...}）、嵌套分式或上下标复杂结构时，
 * 普通行内 \frac 会因 clearance 间隙过小导致根指数或字符直接与分式横线碰撞穿透（黏在一起）。
 * 自动升级为 \dfrac 可为分子、分数线与分母开辟出足够呼吸空间，彻底消除重叠。
 */
export function promoteComplexFractions(latex: string): string {
  if (!latex || !latex.includes('\\frac')) return latex;

  let result = '';
  let i = 0;
  while (i < latex.length) {
    if (latex.startsWith('\\frac', i) && (i + 5 >= latex.length || !/[a-zA-Z]/.test(latex[i + 5]))) {
      let p = i + 5;
      while (p < latex.length && /\s/.test(latex[p])) p++;
      if (p < latex.length && latex[p] === '{') {
        const numEnd = findMatchingBrace(latex, p);
        if (numEnd !== -1) {
          let denStart = numEnd + 1;
          while (denStart < latex.length && /\s/.test(latex[denStart])) denStart++;
          if (denStart < latex.length && latex[denStart] === '{') {
            const denEnd = findMatchingBrace(latex, denStart);
            if (denEnd !== -1) {
              const numerator = latex.slice(p + 1, numEnd);
              const denominator = latex.slice(denStart + 1, denEnd);
              // 如果分子或分母含有根号、嵌套分式，或含有上下标乘方等结构（如 u^2, x^2, (...)^2 等），提升为 \dfrac
              if (
                /\\(?:sqrt|frac|dfrac|cbrt)\b|\^|_/.test(numerator) ||
                /\\(?:sqrt|frac|dfrac|cbrt)\b|\^|_/.test(denominator)
              ) {
                result += '\\dfrac{' + promoteComplexFractions(numerator) + '}{' + promoteComplexFractions(denominator) + '}';
                i = denEnd + 1;
                continue;
              }
            }
          }
        }
      }
    }
    result += latex[i];
    i++;
  }
  return result;
}

/**
 * 匹配完整的 LaTeX 命令项（包含宏名、可选上下标及其后续自变量参数）：
 * 1. 包含 1 到 2 个花括号参数或可选方括号根号参数的命令（如 \frac{1}{2}、\dfrac{a}{b}、\sqrt{2}、\sqrt[3]{x}、\sqrt{x^2+1}）
 * 2. 包含可选上下标的初等函数宏，且可完整吸收后续参数（如 \sin^2 x、\cos^2 x、\sin^2(x)、\ln^2 x、\sin x）
 * 3. 包含圆括号参数的函数宏（如 \alpha(x)、\sin(x)）
 * 4. 符号宏本身及简单上下标（如 \alpha、\beta、\pi、\theta_1、\infty）
 */
const LATEX_CMD_TERM_PATTERN =
  '\\\\[a-zA-Z]+' +
  // 可选方括号参数（如 \sqrt[3]{...}）
  '(?:\\[[^\\]]*\\])?' +
  // 可选上下标（如 \sin^2、\cos^{2}、\alpha_1、\theta^2、\ln^2）
  '(?:\\^[a-zA-Z0-9]+|\\^\\{[^{}]+\\}|_[a-zA-Z0-9]+|_\\{[^{}]+\\})*' +
  // 后续参数部分（花括号参数、圆括号参数、空格自变量参数）
  '(?:' +
    // 1~2个花括号参数，如 \sqrt{2}、\frac{1}{2}、\mathbf{x}、\sin{x}
    '(?:\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}){1,2}' +
    '|' +
    // 圆括号参数，如 \alpha(x)、\sin(x)、\sin^2(x)
    '\\([a-zA-Z0-9,\\s+\\-^*_]+\\)' +
    '|' +
    // 空格后跟变量/数字/反斜杠符号，如 \sin x、\sin^2 x、\cos 2x、\sin \theta、\sin^2 \theta
    '\\s+(?:\\\\[a-zA-Z]+|[a-zA-Z0-9]+)' +
  ')?';

/**
 * 匹配简单数学操作数（普通变量、常数，可带上下标乘方，如 x^2, e^x, x_1, y^2）
 */
const SIMPLE_MATH_OPERAND_PATTERN =
  '(?:[a-zA-Z0-9]+(?:\\^[a-zA-Z0-9]+|\\^\\{[^{}]+\\}|_[a-zA-Z0-9]+|_\\{[^{}]+\\})?)';

/**
 * 智能将公式内部平铺书写的斜杠除法转换为规范的 LaTeX \frac{分子}{分母}
 * 例如：(sinx)/x -> \frac{\sin x}{x}、\sin^2 x/\cos x -> \frac{\sin^2 x}{\cos x}、x^2/y -> \frac{x^2}{y}、1/\sqrt{2} -> \frac{1}{\sqrt{2}} 等
 * 彻底消除 AI 平铺输出除号导致分子分母无法上下分层的问题，同时杜绝截断 \sqrt、\frac、\sin^2 x 等带参宏命令与乘方
 */
export function convertInlineSlashesToFractions(latex: string): string {
  if (!latex || !latex.includes('/')) return latex;

  let s = latex;

  // 1. 常见函数平铺无斜杠修复：如 sinx -> \sin x, cosx -> \cos x, tanx -> \tan x, lnx -> \ln x
  s = s.replace(/(?<![\\a-zA-Z])(sin|cos|tan|cot|sec|csc|ln|exp)([a-zA-Z])(?![a-zA-Z])/g, '\\$1 $2');
  s = s.replace(/(?<![\\a-zA-Z])(sin|cos|tan|cot|sec|csc|ln|exp)\s+([a-zA-Z0-9]+)/g, '\\$1 $2');

  // 2. 花括号指数内部的单变量分式：^{1/x} -> ^{\frac{1}{x}}、^{1/u} -> ^{\frac{1}{u}}、^{1/\alpha(x)} -> ^{\frac{1}{\alpha(x)}}、^{sin x/x} -> ^{\frac{\sin x}{x}}、^{1/\sqrt{2}} -> ^{\frac{1}{\sqrt{2}}}
  s = s.replace(
    new RegExp(`\\^\\{\\s*(${LATEX_CMD_TERM_PATTERN}|${SIMPLE_MATH_OPERAND_PATTERN})\\s*\\/\\s*(${LATEX_CMD_TERM_PATTERN}|${SIMPLE_MATH_OPERAND_PATTERN})\\s*\\}`, 'g'),
    '^{\\frac{$1}{$2}}'
  );

  // 3. 独立括号分子 / 括号分母：(A)/(B) -> \frac{A}{B}（注意防止吃掉前缀函数名）
  s = s.replace(/(?<![a-zA-Z0-9\\_^])\(([^()]+)\)\s*\/\s*\(([^()]+)\)/g, '\\frac{$1}{$2}');

  // 4. 独立括号分子 / 紧凑单项或宏命令分母：(A)/B -> \frac{A}{B}，如 (\sin x)/x -> \frac{\sin x}{x}、(1)/\sqrt{2} -> \frac{1}{\sqrt{2}}
  s = s.replace(new RegExp(`(?<![a-zA-Z0-9\\\\_^])\\(([^()]+)\\)\\s*\\/\\s*(${LATEX_CMD_TERM_PATTERN}|${SIMPLE_MATH_OPERAND_PATTERN})`, 'g'), '\\frac{$1}{$2}');

  // 5. 紧凑单项或宏命令分子 / 括号分母：A/(B) -> \frac{A}{B}，如 1/(\sin x) -> \frac{1}{\sin x}、\sqrt{2}/(x+1) -> \frac{\sqrt{2}}{x+1}
  s = s.replace(new RegExp(`(?<![a-zA-Z0-9\\\\_^])(${LATEX_CMD_TERM_PATTERN}|${SIMPLE_MATH_OPERAND_PATTERN})\\s*\\/\\s*\\(([^()]+)\\)`, 'g'), '\\frac{$1}{$2}');

  // 6. LaTeX 命令项与操作数的除法转换（完整捕获带花括号参数的 \sqrt、\frac 等命令，以及带上标自变量的 \sin^2 x）
  // 6.1 分子为 LaTeX 命令项：\sin x/x -> \frac{\sin x}{x}、\sin^2 x/\cos x -> \frac{\sin^2 x}{\cos x}、\sin^2(x)/\cos(x) -> \frac{\sin^2(x)}{\cos(x)}、\sqrt{2}/2 -> \frac{\sqrt{2}}{2}
  s = s.replace(
    new RegExp(`(?<![a-zA-Z0-9\\\\_^])(${LATEX_CMD_TERM_PATTERN})\\s*\\/\\s*(${LATEX_CMD_TERM_PATTERN}|${SIMPLE_MATH_OPERAND_PATTERN})`, 'g'),
    '\\frac{$1}{$2}'
  );
  // 6.2 分母为 LaTeX 命令项：1/\sqrt{2} -> \frac{1}{\sqrt{2}}、x/\sqrt{2} -> \frac{x}{\sqrt{2}}、x/\cos x -> \frac{x}{\cos x}、1/\frac{1}{2} -> \frac{1}{\frac{1}{2}}、1/\sin x -> \frac{1}{\sin x}
  s = s.replace(
    new RegExp(`(?<![a-zA-Z0-9\\\\_^])(${SIMPLE_MATH_OPERAND_PATTERN})\\s*\\/\\s*(${LATEX_CMD_TERM_PATTERN})`, 'g'),
    '\\frac{$1}{$2}'
  );

  // 7. 常见单变量/常数分式（支持分子分母带上下标）：如 x^2/y -> \frac{x^2}{y}, e^x/y -> \frac{e^x}{y}, x^2/y^2 -> \frac{x^2}{y^2}, 1/x -> \frac{1}{x}（排除年份如 2024/2025）
  s = s.replace(
    /(?<![a-zA-Z0-9\\_^])((?:[0-9]{1,3}|[a-zA-Z]+)(?:\^[a-zA-Z0-9]+|\^\{[^{}]+\}|_[a-zA-Z0-9]+|_\{[^{}]+\})?)\s*\/\s*([a-zA-Z](?:_[a-zA-Z0-9]+|\^[a-zA-Z0-9]+|\^\{[^{}]+\}|_\{[^{}]+\})?)(?![a-zA-Z0-9^_{}])/g,
    '\\frac{$1}{$2}'
  );

  return s;
}

/** 将常见 Unicode 希腊字母（含紧跟数字下标如 β1^t -> \beta_1^t）规范化为 KaTeX 可解析的 LaTeX 命令 */
const UNICODE_GREEK_TO_LATEX: Record<string, string> = {
  'α': '\\alpha',
  'β': '\\beta',
  'γ': '\\gamma',
  'δ': '\\delta',
  'ε': '\\varepsilon',
  'ζ': '\\zeta',
  'η': '\\eta',
  'θ': '\\theta',
  'ι': '\\iota',
  'κ': '\\kappa',
  'λ': '\\lambda',
  'μ': '\\mu',
  'ν': '\\nu',
  'ξ': '\\xi',
  'π': '\\pi',
  'ρ': '\\rho',
  'σ': '\\sigma',
  'τ': '\\tau',
  'υ': '\\upsilon',
  'φ': '\\varphi',
  'χ': '\\chi',
  'ψ': '\\psi',
  'ω': '\\omega',
  'Γ': '\\Gamma',
  'Δ': '\\Delta',
  'Θ': '\\Theta',
  'Λ': '\\Lambda',
  'Ξ': '\\Xi',
  'Π': '\\Pi',
  'Σ': '\\Sigma',
  'Φ': '\\Phi',
  'Ψ': '\\Psi',
  'Ω': '\\Omega'
};

/**
 * 将公式内部残余的 Unicode 根号 √ 与赋值左箭头 ← 规范化为 KaTeX 可识别命令
 */
function normalizeUnicodeRootAndArrows(formula: string): string {
  if (!formula || (!formula.includes('√') && !formula.includes('←'))) return formula;
  return formula
    .replace(/←/g, ' \\leftarrow ')
    .replace(/√\s*\(([^()]+)\)/g, '\\sqrt{$1}')
    .replace(/√\s*([a-zA-Z]_(?:\{[^{}]+\}|[a-zA-Z0-9]+)|[a-zA-Z0-9α-ωΑ-Ω]+)/g, '\\sqrt{$1}');
}

function normalizeUnicodeGreekInMath(formula: string): string {
  if (!formula || !/[α-ωΑ-Ω]/.test(formula)) return formula;
  // 跳过文本模式指令（如 \text{当α>0}），避免把文本中的希腊字母替换为数学模式命令
  const textBlocks: string[] = [];
  const protectedFormula = formula.replace(
    /\\(?:text|textbf|textit|textrm|mbox)\{[^{}]*\}/g,
    (m) => {
      const idx = textBlocks.length;
      textBlocks.push(m);
      return `%%%GREEK_TEXT_TOKEN_${idx}%%%`;
    }
  );
  const replaced = protectedFormula
    .replace(/([α-ωΑ-Ω])(\d+)/g, (_m, g: string, digits: string) => {
      const cmd = UNICODE_GREEK_TO_LATEX[g] || g;
      return digits.length > 1 ? `${cmd}_{${digits}}` : `${cmd}_${digits}`;
    })
    .replace(/[α-ωΑ-Ω]/g, (g) => {
      const cmd = UNICODE_GREEK_TO_LATEX[g];
      return cmd ? `${cmd} ` : g;
    });
  return replaced.replace(/%%%GREEK_TEXT_TOKEN_(\d+)%%%/g, (_m, idxStr) => textBlocks[Number(idxStr)] ?? _m);
}

/**
 * 规整重复/双重下标（如 m_{hat}_t、m_hat_t -> \hat{m}_t，x_{1}_t -> {x_{1}}_t），
 * 防止 KaTeX 抛出 Double subscript ParseError 并将整块公式降级回退为源码文本
 */
function normalizeDoubleSubscripts(formula: string): string {
  if (!formula || !formula.includes('_')) return formula;
  const textBlocks: string[] = [];
  const protectedFormula = formula.replace(
    /\\(?:text|textbf|textit|textrm|mbox)\{[^{}]*\}/g,
    (m) => {
      const idx = textBlocks.length;
      textBlocks.push(m);
      return `%%%SUB_TEXT_TOKEN_${idx}%%%`;
    }
  );

  // 1. 将 m_{hat}_t / m_hat_t 规范化为标准数学符号 \hat{m}_t
  let s = protectedFormula.replace(
    /(?<![\\a-zA-Z])([a-zA-Z])_(?:\{hat\}|hat)_(\{[^{}]+\}|[a-zA-Z0-9]+)(?![a-zA-Z0-9])/g,
    '\\hat{$1}_$2'
  );

  // 2. 兜底其余连续双重下标（如 a_i_j -> {a_i}_j、v_{old}_t -> {v_{old}}_t）
  s = s.replace(
    /(?<![\\a-zA-Z{])([a-zA-Z](?:_\{[^{}]+\}|_[a-zA-Z0-9]+))_(\{[^{}]+\}|[a-zA-Z0-9]+)/g,
    '{$1}_$2'
  );

  return s.replace(/%%%SUB_TEXT_TOKEN_(\d+)%%%/g, (_m, idxStr) => textBlocks[Number(idxStr)] ?? _m);
}

export function isNonMathFilePathFormula(formula: string): boolean {
  const t = formula.trim();
  if (!t) return false;
  if (NON_MATH_CLI_OR_FILE_RE.test(t)) return true;

  // 兜底历史数据中由后端误包裹的无盘符反斜杠路径（如 \proj、\proj\src、\proj\out\com\demo\app\Main.class）
  if (/^\\?[a-zA-Z0-9_.-]+(?:\\[a-zA-Z0-9_.-]+)*$/.test(t) && t.includes('\\')) {
    const cmds = t.match(/\\([a-zA-Z]+)/g) || [];
    const knownCmdRe = new RegExp(`^\\\\(?:${COMMON_MATH_COMMANDS}|begin|end|ddots|vdots|cdots|ldots|infty|implies|iff|gets|bmod|pmod|mod|binom|tbinom|dbinom|text|textbf|textit|textrm|mbox|operatorname|boldsymbol|bm|mathsf|mathtt|mathfrak|widehat|widetilde|cancel|not)$`);
    if (cmds.length > 0 && cmds.every((cmd) => !knownCmdRe.test(cmd))) {
      return true;
    }
  }
  return false;
}

/** Unicode 上下标与常见数学符号（仅在公式片段内部规范化，避免污染普通中文/代码） */
const UNICODE_SUPERSCRIPT_MAP: Record<string, string> = {
  '⁰': '^0',
  '¹': '^1',
  '²': '^2',
  '³': '^3',
  '⁴': '^4',
  '⁵': '^5',
  '⁶': '^6',
  '⁷': '^7',
  '⁸': '^8',
  '⁹': '^9',
  'ⁿ': '^n',
  'ˣ': '^x',
  'ᵃ': '^a',
  '₀': '_0',
  '₁': '_1',
  '₂': '_2',
  '₃': '_3',
  '₄': '_4',
  '₅': '_5',
  '₆': '_6',
  '₇': '_7',
  '₈': '_8',
  '₉': '_9',
};

/**
 * 将公式内部残余的 Unicode 上下标（x²、x³、x₀）与数学算符（→、∞、≠、≤、≥）转为标准 LaTeX
 */
export function normalizeUnicodeMathTokens(formula: string): string {
  if (!formula) return '';
  const textBlocks: string[] = [];
  const masked = formula.replace(
    /\\(?:text|textbf|textit|textrm|mbox)\{[^{}]*\}/g,
    (m) => {
      const idx = textBlocks.length;
      textBlocks.push(m);
      return `%%%UNICODE_TEXT_TOKEN_${idx}%%%`;
    }
  );
  let s = masked
    .replace(/[⁰¹²³⁴⁵⁶⁷⁸⁹ⁿˣᵃ₀₁₂₃₄₅₆₇₈₉]/g, (ch) => UNICODE_SUPERSCRIPT_MAP[ch] ?? ch)
    .replace(/−/g, '-')
    .replace(/∞/g, '\\infty')
    .replace(/→/g, ' \\to ')
    .replace(/≠/g, ' \\neq ')
    .replace(/≤/g, ' \\le ')
    .replace(/≥/g, ' \\ge ')
    .replace(/±/g, ' \\pm ')
    .replace(/×/g, ' \\times ')
    .replace(/÷/g, ' \\div ')
    .replace(/□/g, '\\square');
  for (let pass = 0; pass < 2; pass++) {
    s = s.replace(/([a-zA-Z0-9)\]}])\^\(([^()]+)\)/g, '$1^{$2}');
  }
  return s.replace(/%%%UNICODE_TEXT_TOKEN_(\d+)%%%/g, (_m, idxStr) => textBlocks[Number(idxStr)] ?? _m);
}

/**
 * 统一 KaTeX 渲染前预处理管线（供聊天 Markdown 与题库/解析渲染共用）
 */
export function prepareKatexFormula(rawFormula: string, displayMode: boolean): string {
  let cleaned = repairLatexDoubleEscapes(rawFormula.trim());
  // 纵深防御：绝不允许最外层残留 \(...\) 或 \[...\] 或 $$...$$ 或 $...$ 导致 KaTeX 抛出 ParseError
  cleaned = cleaned
    .replace(/^\\\(([\s\S]*)\\\)$/, '$1')
    .replace(/^\\\[([\s\S]*)\\\]$/, '$1')
    .replace(/^\$\$([\s\S]*)\$\$$/, '$1')
    .replace(/^\$([\s\S]*)\$$/, '$1')
    .trim();

  // 修复反斜杠丢失导致的裸 ight（如 \left( ... ight)）
  cleaned = cleaned.replace(/(?<![a-zA-Z\\])ight(?=\s*[\(\)\[\]\{\}|._^\s0-9]|$)/g, '\\right');
  // 1. 保护：如果公式中残存连续下划线（填空题符 ______），转为合法下划线，彻底防止 KaTeX Double Subscript 报错
  cleaned = cleaned.replace(/_{2,}/g, '\\underline{\\hspace{2.5em}}');
  // 1.1 将公式里的 Unicode 根号、赋值箭头、希腊字母与上下标/算符规范化为 LaTeX 命令
  cleaned = normalizeUnicodeRootAndArrows(cleaned);
  cleaned = normalizeUnicodeGreekInMath(cleaned);
  cleaned = normalizeUnicodeMathTokens(cleaned);
  // 1.2 规整双重下标（如 m_{hat}_t -> \hat{m}_t、a_i_j -> {a_i}_j），防止 KaTeX 抛出 Double subscript
  cleaned = normalizeDoubleSubscripts(cleaned);
  // 1.3 将裸多字母下标（如 m_hat、v_hat）规范为花括号形式 m_{hat}
  cleaned = cleaned.replace(/(?<![\\{])([a-zA-Z])_([a-zA-Z]{2,})(?![a-zA-Z{])/g, '$1_{$2}');
  // 2. 修复圆括号指数（如 e^(x^2) -> e^{x^2}，2^(n+1) -> 2^{n+1}）
  for (let pass = 0; pass < 2; pass++) {
    cleaned = cleaned.replace(/([a-zA-Z0-9)\]}])\^\(([^()]+)\)/g, '$1^{$2}');
  }

  if (
    !displayMode &&
    !/\\begin\{(?:matrix|cases|array|aligned|gathered|bmatrix|pmatrix|vmatrix|Vmatrix)\}/.test(cleaned)
  ) {
    // 行内数学公式中不应含有孤立的强制换行 \\，将其转为空格避免破坏 CSS 基线导致碎行上浮
    cleaned = cleaned.replace(/\\\\(?!\s*[\n\r]|\[)/g, ' ');
  }

  // 3. 将公式内部平铺书写的斜杠除法转换为规范上下结构分数 \frac{分子}{分母}
  cleaned = convertInlineSlashesToFractions(cleaned);

  // 4. 提升含根号/嵌套分式/上下标乘方等结构的分式为 \dfrac，彻底消除上下标与分数线重叠碰撞
  cleaned = promoteComplexFractions(cleaned);
  return cleaned;
}

function renderKatex(formula: string, displayMode: boolean): string {
  try {
    if (!displayMode && isNonMathFilePathFormula(formula)) {
      return escapeHtml(formula);
    }
    const cleaned = prepareKatexFormula(formula, displayMode);

    const html = katex.renderToString(cleaned, {
      throwOnError: false,
      displayMode,
      strict: 'ignore',
      errorColor: 'inherit'
    });

    // 4. 终极容错：绝不允许任何刺眼的红色报错暴露在界面上（兼容有/无 katex-error 与 style 分号）
    return html
      .replace(/style="color:\s*#cc0000\b/gi, 'style="color:inherit')
      .replace(/mathcolor="#cc0000"/gi, 'mathcolor="inherit"');
  } catch {
    return displayMode ? `$$${formula}$$` : `$${formula}$`;
  }
}

/** 修复因 JSON/后端字符串未双反斜杠转义时被解释为控制字符，导致 LaTeX 命令损坏 */
export function repairControlCharsInMathText(text: string): string {
  if (!text) return '';
  return text
    .replace(/\u000C(?=rac\b|\{)/g, '\\f')
    .replace(/[\r\u000D](?=ight\b|\{)/g, '\\r')
    .replace(/\u0009(?=(?:o|imes|an|ext|heta|frac)\b)/g, '\\t')
    .replace(/\u0008(?=(?:eta|ar|egin|matrix)\b)/g, '\\b')
    .replace(/\u000A(?=(?:eq|otin|abla|u)\b)/g, '\\n');
}

/** 模型/JSON 常输出字面量 \\n；勿误伤 LaTeX 命令如 \\neq、\\not */
export function normalizeMathTextNewlines(text: string): string {
  return repairLatexDoubleEscapes(repairControlCharsInMathText(text))
    .replace(/\r\n/g, '\n')
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n(?![a-zA-Z])/g, '\n');
}

const COMMON_MATH_COMMANDS = [
  // 初等与三角函数
  'ln',
  'log',
  'lg',
  'exp',
  'sin',
  'cos',
  'tan',
  'cot',
  'sec',
  'csc',
  'arcsin',
  'arccos',
  'arctan',
  'sinh',
  'cosh',
  'tanh',
  // 微积分与极限
  'lim',
  'inf',
  'sup',
  'max',
  'min',
  'det',
  'dim',
  'ker',
  'deg',
  'arg',
  'frac',
  'dfrac',
  'tfrac',
  'sqrt',
  'cbrt',
  'sum',
  'int',
  'iint',
  'iiint',
  'oint',
  'prod',
  'partial',
  'nabla',
  // 关系与逻辑符
  'sim',
  'approx',
  'ne',
  'neq',
  'le',
  'leq',
  'ge',
  'geq',
  'in',
  'notin',
  'subset',
  'subseteq',
  'supset',
  'supseteq',
  'cup',
  'cap',
  'to',
  'leftarrow',
  'rightarrow',
  'Rightarrow',
  'Leftarrow',
  'Leftrightarrow',
  'forall',
  'exists',
  'infty',
  'pm',
  'mp',
  'times',
  'div',
  'cdot',
  'equiv',
  // 小写希腊字母
  'alpha',
  'beta',
  'gamma',
  'delta',
  'epsilon',
  'zeta',
  'eta',
  'theta',
  'iota',
  'kappa',
  'lambda',
  'mu',
  'nu',
  'xi',
  'pi',
  'rho',
  'sigma',
  'tau',
  'upsilon',
  'phi',
  'chi',
  'psi',
  'omega',
  // 希腊字母变体（高数核心：\varepsilon-N 定义等）
  'varepsilon',
  'varphi',
  'vartheta',
  'varrho',
  'varpi',
  'varsigma',
  // 大写希腊字母
  'Gamma',
  'Delta',
  'Theta',
  'Lambda',
  'Xi',
  'Pi',
  'Sigma',
  'Upsilon',
  'Phi',
  'Psi',
  'Omega',
  // 字体与重音
  'mathrm',
  'mathbf',
  'mathit',
  'mathbb',
  'mathcal',
  'text',
  'vec',
  'hat',
  'bar',
  'tilde',
  'dot',
  'ddot',
  'overline',
  'underline',
  // 括号自适应定界符
  'left',
  'right'
].join('|');

const BARE_LATEX_RE = new RegExp(
  `(?<![\\\\$a-zA-Z0-9:_])(?:(?:[a-zA-Z](?:\\([a-zA-Z0-9,\\s]+\\))?|[a-zA-Z](?:_(?:\\{[^{}]+\\}|[a-zA-Z0-9]+))+)\\s*=\\s*)?\\\\(?:${COMMON_MATH_COMMANDS})(?:\\\\[a-zA-Z]+|\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}|\\[[^\\]]*\\]|\\([^()]*\\)|[0-9a-zA-Z+\\-*/=<>≤≥≠^_()|.,:~'\\s])*(?=[，；。！？、\\n\\u4e00-\\u9fa5]|$)`,
  'g'
);

/**
 * 安全剥离公式末尾的中文标点与填空题下划线（如 "= ______"、"______"）
 * 避免连续下划线被误卷入 LaTeX 导致 KaTeX 抛出 Double subscript 错误并变红
 */
export function stripFormulaBlanksAndPunctuation(raw: string): { formula: string; suffix: string } {
  let text = raw.trim();
  let suffix = '';

  // 1. 去除末尾中文标点
  const punctMatch = text.match(/[，；。！？、]+$/);
  if (punctMatch) {
    suffix = punctMatch[0] + suffix;
    text = text.slice(0, text.length - punctMatch[0].length).trim();
  }

  // 2. 去除末尾的填空下划线符：例如 "= ______"、"=____"、"______"（连续2个及以上下划线）
  const blankMatch = text.match(/\s*(=?\s*_{2,}[\s\S]*)$/);
  if (blankMatch) {
    suffix = blankMatch[0] + suffix;
    text = text.slice(0, text.length - blankMatch[0].length).trim();
  }

  // 3. 再次去除可能残存孤立等号或多余下划线
  const trailingEq = text.match(/\s*(=|_{2,})$/);
  if (trailingEq) {
    suffix = trailingEq[0] + suffix;
    text = text.slice(0, text.length - trailingEq[0].length).trim();
  }

  return { formula: text, suffix };
}

/**
 * 将文本中未被 $ 包裹的裸 LaTeX 命令表达式（如 \\ln(1+x) \\sim x、\\sin 2x \\sim 2x、\\varepsilon-N、\\varepsilon > 0）
 * 自动识别并包裹为 $...$，便于 KaTeX 精准解析渲染
 */
export function wrapBareLatexExpressions(text: string): string {
  if (!text || !text.includes('\\')) return text;

  // 1. 保护已有标准数学公式占位符
  const placeholders: string[] = [];
  let masked = text.replace(
    /(\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\\([\s\S]*?\\\)|\$[^$\n]+?\$)/g,
    (m) => {
      const idx = placeholders.length;
      placeholders.push(m);
      return `%%%MATH_TOKEN_${idx}%%%`;
    }
  );
  masked = masked.replace(/(?<!\$)\$(?!\$)([\s\S]+?)(?<!\$)\$(?!\$)/g, (m, formula: string) => {
    if (isSafeMultilineMath(formula)) {
      const idx = placeholders.length;
      placeholders.push(m);
      return `%%%MATH_TOKEN_${idx}%%%`;
    }
    return m;
  });

  // 2. 匹配裸 LaTeX 表达式
  masked = masked.replace(BARE_LATEX_RE, (match) => {
    const trimmed = match.trim();
    if (!trimmed || trimmed.startsWith('%%%MATH_TOKEN_')) return match;
    const { formula, suffix } = stripFormulaBlanksAndPunctuation(trimmed);
    if (!formula) return match;
    return `$${formula}$${suffix}`;
  });

  // 3. 还原占位符
  return masked.replace(/%%%MATH_TOKEN_(\d+)%%%/g, (_m, idxStr) => {
    const idx = Number(idxStr);
    return placeholders[idx] ?? _m;
  });
}

/** 将纯文本数学表达转换为适配 KaTeX 的标准 LaTeX 片段 */
function normalizePlainMathBody(raw: string): string {
  let clean = normalizeUnicodeGreekInMath(normalizeUnicodeMathTokens(raw.trim()));
  clean = clean.replace(/\blim\b(?![a-zA-Z])/g, '\\lim');
  clean = clean.replace(/(?<!\\)\b(arcsin|arccos|arctan|sin|cos|tan|cot|sec|csc|ln|log|lg|exp)\b/g, '\\$1 ');
  clean = clean.replace(/\\{2,}(arcsin|arccos|arctan|sin|cos|tan|cot|sec|csc|ln|log|lg|exp|lim)/g, '\\$1');
  clean = clean.replace(/~/g, ' \\sim ').replace(/·/g, ' \\cdot ');
  clean = clean.replace(/(?<![\\{])([a-zA-Z])_([a-zA-Z0-9]{2,})(?![a-zA-Z0-9{])/g, '$1_{$2}');
  clean = normalizeUnicodeRootAndArrows(clean);
  for (let pass = 0; pass < 2; pass++) {
    clean = clean.replace(/([a-zA-Z0-9)\]}])\^\(([^()]+)\)/g, '$1^{$2}');
  }
  return convertInlineSlashesToFractions(clean).replace(/\s{2,}/g, ' ').trim();
}

/**
 * 专门提取并格式化微积分核心表达式（未带 $ 定界符的微商、高阶导数、极限与函数导数记号）
 * 例如：\lim_{x\to 0} (e^(x^2)-1)/(1-\cos x) = ______、lim(x → 0) (e^(x²) - 1)/(1 - cos x)、d^2y/dx^2 = ______、y''(0)、y'(0) 等
 */
export function wrapCalculusExpressions(text: string): string {
  if (!text) return '';

  // 保护已有公式
  const tokens: string[] = [];
  let s = text.replace(
    /(\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\\([\s\S]*?\\\)|\$[^$\n]+?\$)/g,
    (m) => {
      const idx = tokens.length;
      tokens.push(m);
      return `%%%CALC_TOKEN_${idx}%%%`;
    }
  );
  s = s.replace(/(?<!\$)\$(?!\$)([\s\S]+?)(?<!\$)\$(?!\$)/g, (m, formula: string) => {
    if (isSafeMultilineMath(formula)) {
      const idx = tokens.length;
      tokens.push(m);
      return `%%%CALC_TOKEN_${idx}%%%`;
    }
    return m;
  });

  const CJK_OR_PUNCT_BEFORE = '(?<=[:：，,；;、。（(\\[\\s\\u4e00-\\u9fa5\\uff00-\\uffef]|^)';
  const CJK_OR_PUNCT_AFTER = '(?=[:：，,；;、。）)\\]\\s\\u4e00-\\u9fa5\\uff00-\\uffef]|$)';

  // 0. 数列记号：例如 数列{a_n}、{x_n}、{u_n} 规范化为 KaTeX 集合与数列符号 $\{a_n\}$
  s = s.replace(/(?<=数列|\s|^)\{([a-zA-Z]_[a-zA-Z0-9]+)\}/g, '$\\{$1\\}$');

  // 1. 规范化极限趋近下标中的角标：如 x0- -> x_0^-, x0+ -> x_0^+, x0 -> x_0
  s = s.replace(/(?<=lim\s*_\s*\{[^}]*)x0-/g, 'x_0^-');
  s = s.replace(/(?<=lim\s*_\s*\{[^}]*)x0\+/g, 'x_0^+');
  s = s.replace(/(?<=lim\s*_\s*\{[^}]*)x0(?![a-zA-Z0-9])/g, 'x_0');

  // 2. 带趋近下标的极限表达式及等式链（例如 lim_{x->0} x \cdot \sin(1/x)、lim_{x->x0-} f(x) = lim_{x->x0+} f(x) = A）
  s = s.replace(
    /(?<=[\s=，,；;。、(（\u4e00-\u9fa5]|^)(\\?lim\s*_\{[^}]+\}[\s\S]*?)(?=[，,；;。、\u4e00-\u9fa5（]|$)(\s*=\s*[a-zA-Z0-9]+)?/g,
    (_m, formula: string, trailingEq?: string) => {
      let { formula: cleaned, suffix } = stripFormulaBlanksAndPunctuation(formula);
      if (!cleaned) return formula;
      let prefix = '';
      if (cleaned.startsWith('=')) {
        prefix = '= ';
        cleaned = cleaned.slice(1).trim();
      }
      cleaned = cleaned.replace(/\\?lim/g, '\\lim');
      cleaned = cleaned.replace(/->|→/g, ' \\to ');
      if (trailingEq) {
        cleaned += trailingEq;
      }
      const idx = tokens.length;
      tokens.push(`$${cleaned}$`);
      return `${prefix}%%%CALC_TOKEN_${idx}%%%${suffix}`;
    }
  );

  // 2.5 纯文本括号趋近式极限（如 lim(x → 0) (e^(x²) - 1)/(1 - cos x)、lim(x → 0)(sin x - x)/x³、lim(x → 0) sin x/x）
  // 必须放在第 5 条独立 x → 0 规则之前，避免把括号内的 x → 0 单独拆碎
  s = s.replace(
    /(?<=[:：，,；;、。（(「『“"[ \t\n\u4e00-\u9fa5]|^)\\?lim[ \t]*\([ \t]*([a-zA-Z])[ \t]*(?:\\to|->|→)[ \t]*([^()\n]+?)[ \t]*\)[ \t]*([0-9a-zA-Zα-ωΑ-Ω()+\-*/^_. \t⁰¹²³⁴⁵⁶⁷⁸⁹∞\\]+?)(?=[，,；;。、！？：:”"\n\u4e00-\u9fa5]|$)/g,
    (match, varName: string, targetRaw: string, bodyRaw: string) => {
      let body = bodyRaw.trim();
      if (!body || PROSE_WORD_PAIR_RE.test(body)) return match;
      let suffix = '';
      const openParens = (body.match(/\(/g) || []).length;
      const closeParens = (body.match(/\)/g) || []).length;
      if (closeParens > openParens) {
        const diff = closeParens - openParens;
        const tailMatch = body.match(new RegExp(`\\){1,${diff}}$`));
        if (tailMatch) {
          suffix = tailMatch[0];
          body = body.slice(0, body.length - suffix.length).trim();
        }
      }
      const stripped = stripFormulaBlanksAndPunctuation(body);
      body = stripped.formula;
      suffix = stripped.suffix + suffix;
      if (!body) return match;
      const target = normalizeUnicodeMathTokens(targetRaw.trim());
      const latexBody = normalizePlainMathBody(body);
      const idx = tokens.length;
      tokens.push(`$\\lim_{${varName} \\to ${target}} ${latexBody}$`);
      return `%%%CALC_TOKEN_${idx}%%%${suffix}`;
    }
  );

  // 3. 散文中无下标的极限算子表达式（例如 若lim f(x) = 0且 lim g(x)=\infty，则lim f(x)g(x) = 0、lim x²/(x²/2) = 2、\lim(1+\alpha(x))^{1/\alpha(x)}=e）
  s = s.replace(
    /(?<=[ \t\n=，,；;。、(（\u4e00-\u9fa5]|^)(\\?lim(?:\([ \t]*(?![a-zA-Z][ \t]*(?:\\to|->|→))[^)\n]+\)|\[[^\]\n]+\]|\\|[ \t]+[a-zA-Z0-9⁰¹²³⁴⁵⁶⁷⁸⁹])[^，,；;。、！？：\n\u4e00-\u9fa5]*?(?:[ \t]*=[ \t]*[a-zA-Z0-9^_{}\\]+)?)(?=[，,；;。、！？：\n\u4e00-\u9fa5（]|$)/g,
    (match, formula: string) => {
      let { formula: cleaned, suffix } = stripFormulaBlanksAndPunctuation(formula);
      if (!cleaned || PROSE_WORD_PAIR_RE.test(cleaned)) return match;
      let prefix = '';
      if (cleaned.startsWith('=')) {
        prefix = '= ';
        cleaned = cleaned.slice(1).trim();
      }
      cleaned = normalizePlainMathBody(cleaned);
      const idx = tokens.length;
      tokens.push(`$${cleaned}$`);
      return `${prefix}%%%CALC_TOKEN_${idx}%%%${suffix}`;
    }
  );

  // 3. 微商与高阶导数记号：如 d^2y/dx^2, dy/dx, dx/dt, dy/dt, d/dx, d/dt
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}(d\\^[0-9]+[a-zA-Z]\\/d[a-zA-Z]\\^[0-9]+|d[a-zA-Z]\\/d[a-zA-Z]|d\\/d[a-zA-Z])${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (_m, deriv: string) => {
      const parts = deriv.split('/');
      return `$\\frac{${parts[0]}}{${parts[1]}}$`;
    }
  );

  // 4. 函数高阶导数记号：如 y''(0), y'(0), y'', y', f''(x), f'(x), f'(0), f''(0)（紧跟关系运算符时留给等式/不等式整体渲染）
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}([a-zA-Z]'{1,3}(?:\\([a-zA-Z0-9]+\\))?)(?![ \\t]*(?:[=≠≤≥<>]|!=|<=|>=))${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (_m, fn: string) => {
      return `$${fn.trim()}$`;
    }
  );

  // 5. 变量或分式趋近表达式：例如 n \to \infty、x \to 0、\alpha(x) \to 0、\frac{1}{n} \to 0、\frac{1}{n^2} \to 0
  s = s.replace(
    /(?<=[\s=，,；;。、(（\u4e00-\u9fa5]|^)((?:(?:\\[a-zA-Z]+|[a-zA-Z])(?:\([a-zA-Z0-9,\s]+\))?(?:_[a-zA-Z0-9+-]+)?|\\frac\{[^{}]+\}\{[^{}]+\})\s*(?:\\to|->|→)\s*(?:[+-]?(?:\\infty|\d+(?:\.\d+)?|\b[a-zA-Z]_0(?:\^[-+])?|0\^[-+]?)))(?=[，,；;。、\u4e00-\u9fa5（]|$)/g,
    (_m, formula: string) => {
      let { formula: cleaned, suffix } = stripFormulaBlanksAndPunctuation(formula);
      if (!cleaned) return formula;
      cleaned = cleaned.replace(/->|→/g, ' \\to ');
      return `$${cleaned}$${suffix}`;
    }
  );

  // 6. 包含不等号的算式：例如 1 \neq 0、x \neq 0、\alpha(x) \neq 0
  s = s.replace(
    /(?<=[\s=，,；;。、(（\u4e00-\u9fa5]|^)([a-zA-Z0-9_+^*/().\-\\\\]+\s*\\neq\s*[a-zA-Z0-9_+^*/().\-\\\\]+)(?=[，,；;。、\u4e00-\u9fa5（]|$)/g,
    (_m, formula: string) => {
      let { formula: cleaned, suffix } = stripFormulaBlanksAndPunctuation(formula);
      if (!cleaned) return formula;
      return `$${cleaned}$${suffix}`;
    }
  );

  // 还原占位符
  return s.replace(/%%%CALC_TOKEN_(\d+)%%%/g, (_m, idxStr) => {
    const idx = Number(idxStr);
    return tokens[idx] ?? _m;
  });
}

/**
 * 将混在中文散文/评语中未加 $ 的常用数学表达式（等价无穷小、等式、不等式、乘方多项式、初等函数）
 * 自动识别并包裹为 $...$，以便 KaTeX 精准渲染
 */
export function wrapProsePlainMath(text: string): string {
  if (!text) return '';
  // 纯英文自然语言句子（如 "The value of x^2 is 4"）避免过度识别，交给后续严格公式判断
  if (!/[\u4e00-\u9fa5]/.test(text) && PROSE_WORD_PAIR_RE.test(text)) {
    return text;
  }

  // 1. 保护已有标准公式、Markdown 链接、裸 URL、HTML 标签与行首结构前缀占位符
  const tokens: string[] = [];
  const protectToken = (formula: string): string => {
    const idx = tokens.length;
    tokens.push(formula);
    return `%%%PROSE_MATH_TOKEN_${idx}%%%`;
  };

  let s = text.replace(
    /(\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\\([\s\S]*?\\\)|\$[^$\n]+?\$)/g,
    (m) => protectToken(m)
  );
  s = s.replace(/(?<!\$)\$(?!\$)([\s\S]+?)(?<!\$)\$(?!\$)/g, (m, formula: string) => {
    if (isSafeMultilineMath(formula)) {
      return protectToken(m);
    }
    return m;
  });
  // 保护 Markdown 链接/图片、裸 URL 与内联 HTML 标签，避免其内部的 =、/、?、& 被误识为公式
  s = s.replace(/!?\[[^\]\n]*\]\([^)\n]+\)|https?:\/\/[^\s)）\]>]+|<[a-zA-Z/][^>\n]*>/g, (m) =>
    protectToken(m)
  );
  // 保护 Markdown 行首结构前缀（标题 #、列表 -/*/+、引用 >）以及表格分隔符与竖线，避免吞掉换行或列表减号
  s = s
    .split('\n')
    .map((line) => {
      const trimmed = line.trim();
      if (trimmed.startsWith('|') && trimmed.endsWith('|')) {
        if (/^\|(?:\s*:?-{2,}:?\s*\|)+$/.test(trimmed)) {
          return protectToken(line);
        }
        return line.replace(/\|/g, () => protectToken('|'));
      }
      return line.replace(/^([ \t]*(?:#{1,6}|[-*+•]|\d+[.)]|>))(?=[ \t])/, (prefix) => protectToken(prefix));
    })
    .join('\n');

  const CJK_OR_PUNCT_BEFORE = '(?<=[:：，,；;、。 \\t\\n“"\'‘(（\\u4e00-\\u9fa5\\uff00-\\uffef]|^)';
  const CJK_OR_PUNCT_AFTER = '(?=[:：，,；;、。 \\t\\n”"\'’)）\\u4e00-\\u9fa5\\uff00-\\uffef]|$)';

  // 1.1 分段函数：例如 f(x)={ (x^2-4)/(x-2), x ≠ 2 ; k, x = 2 } 或 y = { ... }
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}([fghuvyFGHUV]\\s*\\([a-zA-Z0-9]+\\)|[yY])\\s*=\\s*\\{\\s*([^{}]+?)\\s*\\}${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (_m, fnHeader: string, casesBody: string) => {
      const branches = casesBody.split(/\s*;\s*|\s*\\\\\s*|\n+/).filter(Boolean);
      if (branches.length >= 2) {
        const latexRows = branches.map((branch) => {
          const commaIdx = branch.indexOf(',');
          if (commaIdx !== -1) {
            let expr = branch.slice(0, commaIdx).trim();
            let cond = branch.slice(commaIdx + 1).trim();
            expr = expr.replace(/\(([^()]+)\)\s*\/\s*\(([^()]+)\)/g, '\\frac{$1}{$2}');
            expr = expr.replace(/\(([^()]+)\)\s*\/\s*([a-zA-Z0-9^_{}\\]+)/g, '\\frac{$1}{$2}');
            expr = expr.replace(/([a-zA-Z0-9^_{}\\]+)\s*\/\s*\(([^()]+)\)/g, '\\frac{$1}{$2}');
            expr = expr.replace(/([a-zA-Z0-9^]+)\s*\/\s*([a-zA-Z0-9^]+)/g, '\\frac{$1}{$2}');
            cond = cond.replace(/!=|≠/g, ' \\ne ').replace(/<=|≤/g, ' \\le ').replace(/>=|≥/g, ' \\ge ');
            return `${expr}, & ${cond}`;
          }
          return branch;
        });
        return protectToken(`$${fnHeader.trim()} = \\begin{cases} ${latexRows.join(' \\\\ ')} \\end{cases}$`);
      }
      return _m;
    }
  );

  // 1.2 散文中的零点记号：如 点 x0、x0 处、x0 的某邻域（带下划线变量留待等式匹配之后再兜底，避免截断 m_hat = ... 等整条等式）
  s = s.replace(
    /(?<=[:：，,；;、。在对于当若使设令与及和点处到从\s]|^)([xyztXYZT])0(?=[:：，,；;、。在对于当若使设令与及和点处到从的内邻域连续可导\s]|$)/g,
    (_m, v) => protectToken(`$${v}_0$`)
  );

  // 1.3 散文中明确声明为常数/参数/自变量的单字母变量或变量列表：如 常数 k的值、常数 a,b 应满足、参数 a、b、c 为正数
  s = s.replace(
    /(?<=(?:常数|参数|变量|自变量)\s*)([a-zA-Z](?:\s*[,，、和及与]\s*[a-zA-Z])*)(?=\s*(?:的值|为|是|满足|大于|小于|等于|在|的|[\u4e00-\u9fa5，,；;。、(（]|$))/g,
    (_m, varList: string) => {
      const vars = varList.match(/[a-zA-Z]/g);
      if (vars && vars.length > 0) {
        return protectToken(`$${vars.join(', ')}$`);
      }
      return _m;
    }
  );

  // 1.4 常见通用函数记号：如 设函数 f(x)、若 f(x)在、使 f(x)连续（紧跟关系运算符时留给等式/不等式整体包裹）
  s = s.replace(
    /(?<=[\u4e00-\u9fa5（(，,；;。！!？?、\s]|^)([fghuvyFGHUV]'?\s*\([a-zA-Z0-9_+\-^*/'·\\]+\))(?![ \t]*(?:[=≠≤≥<>]|!=|<=|>=))(?=[\u4e00-\u9fa5）)，,；;。！!？?、\s]|$)/g,
    (match, fn) => {
      if (fn.startsWith('%%%') || fn.includes('$')) return match;
      return protectToken(`$${fn.trim()}$`);
    }
  );

  // 1.5 散文中未加包裹的代数分式（双括号形式）：如 (x^2-4)/(x-2)
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}(\\(([a-zA-Z0-9α-ωΑ-Ω_+^*/\\-]+)\\)\\s*\\/\\s*\\(([a-zA-Z0-9α-ωΑ-Ω_+^*/\\-]+)\\))${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (_m, _full, num, den) =>
      protectToken(`$\\frac{${normalizeUnicodeGreekInMath(num)}}{${normalizeUnicodeGreekInMath(den)}}$`)
  );

  // 2. 极限箭头推导：例如 (x^3/6 + o(x^3))/x^3 ->1/6 或 原式 = ... -> 1/6，右侧通常为常数、分数、极限值或无穷
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}((?:[a-zA-Z0-9()+\\-*/^~·]|\\s*=\\s*)+\\s*(?:->|→)\\s*(?:[+-]?(?:\\d+(?:\\/\\d+)?|\\d+(?:\\.\\d+)?|\\b[A-Za-z]\\b|\\\\infty)))${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match) => {
      let expr = match.trim();
      const parts = expr.split(/->|→/);
      if (parts.length === 2) {
        const left = parts[0].trim();
        const right = parts[1].trim();
        // 排除代码指针如 p-> 或单纯无运算标识符如 A -> B（交给自然语言/状态机）
        if (!/[()+*/^=~·-]/.test(left) && /^[a-zA-Z]$/.test(right)) {
          return match;
        }
      }
      if (!/[a-zA-Z]/.test(expr)) return match;
      expr = expr.replace(/->|→/g, ' \\to ').replace(/~/g, ' \\sim ').replace(/·/g, ' \\cdot ');
      return `$${expr}$`;
    }
  );

  // 3. 括号内的数学约束/条件：例如 (t != 0)、(t ≠ 0)、(x > 0)
  s = s.replace(
    /\(([a-zA-Z0-9^~_'⁰¹²³⁴⁵⁶⁷⁸⁹]+\s*(?:!=|≠|<=|>=|<|>|≤|≥)\s*[a-zA-Z0-9^~_'⁰¹²³⁴⁵⁶⁷⁸⁹]+)\)/g,
    (_m, inner: string) => {
      const clean = normalizePlainMathBody(inner);
      return `(${protectToken(`$${clean}$`)})`;
    }
  );

  // 4. 散文中数学不等式与关系式：例如 \varepsilon > 0、0 < x < 1、x ≠ 0、sin x/x ≠ 1、f'(x) < 0（不跨越换行 \n）
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}([a-zA-Z0-9()+\\-*/^~|_·' \\t\\\\⁰¹²³⁴⁵⁶⁷⁸⁹∞]{1,}(?:[ \\t]*(?:<=|>=|<|>|≤|≥|≠|!=)[ \\t]*[a-zA-Z0-9()+\\-*/^~|_·' \\t\\\\⁰¹²³⁴⁵⁶⁷⁸⁹∞]{1,})+)${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match) => {
      const expr = match.trim();
      if (!/[a-zA-Z\\|]/.test(expr) || /[a-zA-Z]{4,}[ \t]+[a-zA-Z]{4,}/.test(expr)) return match;
      const clean = normalizePlainMathBody(expr);
      return protectToken(`$${clean}$`);
    }
  );

  // 5. 数学等式与赋值更新式：例如 f(x) = x + 1/x、f'(x) = 1 - 1/x² = (x² - 1)/x²、e^y + xy = e、lim x²/(x²/2) = 2（不跨越换行 \n）
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}([a-zA-Z0-9α-ωΑ-Ω()+\\-*/^~·'_√ \\t\\\\⁰¹²³⁴⁵⁶⁷⁸⁹∞]{1,}(?:(?:=|←)[ \\t]*[a-zA-Z0-9α-ωΑ-Ω()+\\-*/^~·'_√ \\t\\\\⁰¹²³⁴⁵⁶⁷⁸⁹∞]{1,})+)${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match) => {
      const { formula, suffix } = stripFormulaBlanksAndPunctuation(match);
      if (!formula) return match;
      if (!/[a-zA-Zα-ωΑ-Ω]/.test(formula) || /[a-zA-Z]{4,}[ \t]+[a-zA-Z]{4,}/.test(formula)) return match;
      // 避免误伤纯下划线填空题如 "= ______"
      if (/^=+[ \t]*_+$/.test(formula) || /_{2,}/.test(formula)) return match;
      const clean = normalizePlainMathBody(formula);
      return protectToken(`$${clean}$`) + suffix;
    }
  );

  // 5.05 含 Unicode 根号的括号表达式（如 (√v_hat+ε)）与孤立根号表达式（如 √v_hat、√(a+b)、√2）
  s = s.replace(
    /\(([^()\n]*√(?:[a-zA-Z0-9α-ωΑ-Ω_]|\([^()]+\))[^()\n]*)\)/g,
    (match, inner: string) => {
      if (inner.includes('%%%') || inner.includes('$') || /[\u4e00-\u9fa5]/.test(inner)) return match;
      let clean = normalizeUnicodeRootAndArrows(inner.trim()).replace(/·/g, ' \\cdot ');
      clean = normalizeUnicodeGreekInMath(clean);
      clean = clean.replace(/(?<![\\{])([a-zA-Z])_([a-zA-Z0-9]{2,})(?![a-zA-Z0-9{])/g, '$1_{$2}');
      return protectToken(`$(${clean})$`);
    }
  );
  s = s.replace(
    /√(?:\(([^()\n]+)\)|([a-zA-Z]_[a-zA-Z0-9]+|[a-zA-Z0-9α-ωΑ-Ω]))/g,
    (match, parenBody?: string, simpleBody?: string) => {
      const body = parenBody ?? simpleBody ?? '';
      if (!body || body.includes('%%%') || body.includes('$') || /[\u4e00-\u9fa5]/.test(body)) return match;
      let clean = normalizeUnicodeGreekInMath(body.trim());
      clean = clean.replace(/(?<![\\{])([a-zA-Z])_([a-zA-Z0-9]{2,})(?![a-zA-Z0-9{])/g, '$1_{$2}');
      return protectToken(`$\\sqrt{${clean}}$`);
    }
  );

  // 5.1 孤立代数分式兜底（如单独出现的 m/(1-β1^t)，放在完整等式匹配之后以免把等式右侧拆离）
  s = s.replace(
    /(?<=[\s=，,；;、。（(\u4e00-\u9fa5]|^)([a-zA-Z0-9α-ωΑ-Ω_^]+)\s*\/\s*\(([a-zA-Z0-9α-ωΑ-Ω_+^*/\-]+)\)(?=[\s，,；;、。）)\u4e00-\u9fa5]|$)/g,
    (match, num: string, den: string) => {
      if (!/[+\-*/^_]/.test(den) && !/[α-ωΑ-Ω]/.test(num + den)) return match;
      return protectToken(
        `$\\frac{${normalizeUnicodeGreekInMath(num)}}{${normalizeUnicodeGreekInMath(den)}}$`
      );
    }
  );

  // 5.2 孤立带下标数学符号兜底（如散文里的 m_hat、x_0、a_n，放在完整等式匹配之后以免把 m_hat = ... 截断）
  s = s.replace(
    /(?<=[:：，,；;、。在对于当若使设令与及和点处到从\s]|^)([a-zA-Z])_([a-zA-Z0-9]+)(?=[:：，,；;、。在对于当若使设令与及和点处到从的内邻域连续可导\s]|$)/g,
    (_m, v, sub) => protectToken(`$${v}_{${sub}}$`)
  );

  // 6. 等价无穷小：例如 e^x-1 ~ x、e^(x²) - 1 ~ x²、1 - cos x ~ x²/2、sin x ~ x
  s = s.replace(
    /(?<=[:：，,；;、。 \t\n•（\u4e00-\u9fa5]|^)([0-9a-zA-Z()+\-*/^. \t⁰¹²³⁴⁵⁶⁷⁸⁹]+?)[ \t]*~[ \t]*([0-9a-zA-Z()+\-*/^. \t⁰¹²³⁴⁵⁶⁷⁸⁹]+)(?=[,，；;。、！？：)）\n]|$)/g,
    (match, left: string, right: string) => {
      const lRaw = left.trim();
      const rRaw = right.trim();
      if (!lRaw || !rRaw || !/[a-zA-Z]/.test(lRaw + rRaw) || PROSE_WORD_PAIR_RE.test(lRaw + ' ' + rRaw)) {
        return match;
      }
      const latex = normalizePlainMathBody(`${lRaw} ~ ${rRaw}`);
      return protectToken(`$${latex}$`);
    }
  );

  // 6.5 含初等函数或乘方的纯文本分式（如 sin x/x、(sin x - x)/x³、1/x²、(e^(x²) - 1)/(1 - cos x)）
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}((?:(?:sin|cos|tan|cot|sec|csc|ln|exp)\\s*[a-zA-Z0-9]+|\\([^()\n]+\\)|[a-zA-Z0-9]+[⁰¹²³⁴⁵⁶⁷⁸⁹^][a-zA-Z0-9⁰¹²³⁴⁵⁶⁷⁸⁹{}()]*|\\d+)\\s*\\/\\s*(?:\\([^()\n]+\\)|[a-zA-Z0-9⁰¹²³⁴⁵⁶⁷⁸⁹^_{}()]+))${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match, fracExpr: string) => {
      const trimmed = fracExpr.trim();
      if (!/[a-zA-Z]/.test(trimmed) || PROSE_WORD_PAIR_RE.test(trimmed)) return match;
      if (/^[0-9]{1,4}\/[0-9]{1,4}$/.test(trimmed)) return match;
      if (!/(?:sin|cos|tan|cot|sec|csc|ln|exp|[⁰¹²³⁴⁵⁶⁷⁸⁹^])/.test(trimmed) && !/^\([^()]+\)\s*\/\s*\([^()]+\)$/.test(trimmed)) {
        return match;
      }
      return protectToken(`$${normalizePlainMathBody(trimmed)}$`);
    }
  );

  // 7. 多项式与初等函数/指数加减（必须放在孤立幂次项之前，避免先把 e^x - 1 里的 e^x 单独拆走）
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}((?:[0-9a-zA-Z^⁰¹²³⁴⁵⁶⁷⁸⁹]+[ \\t]*[-+][ \\t]*(?:sin|cos|tan|ln)[ \\t]+[a-zA-Z0-9]+)|(?:[a-zA-Z]\\^(?:\\([^()\\n]+\\)|[a-zA-Z0-9⁰¹²³⁴⁵⁶⁷⁸⁹]+)[ \\t]*[-+][ \\t]*[0-9a-zA-Z⁰¹²³⁴⁵⁶⁷⁸⁹]+))${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match) => {
      return protectToken(`$${normalizePlainMathBody(match.trim())}$`);
    }
  );

  // 8. 代数项与含括号乘积：例如 x^2(e^x-1)、o(x^3)、e^y (y')^2
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}([a-zA-Z0-9]+\\^[0-9a-zA-Z]+(?:\\([a-zA-Z0-9^/+\\- \\t~']+\\))?|o\\([a-zA-Z0-9^/+\\- \\t']+\\))${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match) => {
      const expr = match.trim();
      if (!/[a-zA-Z]/.test(expr)) return match;
      return protectToken(`$${normalizePlainMathBody(expr)}$`);
    }
  );

  // 9. 单独出现的初等函数、幂次项（含 Unicode 上标如 x²、x³）与无穷符号（+∞、-∞）
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}((?:sin|cos|tan|cot|ln|exp)\\s*(?:\\([^)]+\\)|[a-zA-Z0-9⁰¹²³⁴⁵⁶⁷⁸⁹]+)|[a-zA-Z](?:\\^[0-9a-zA-Z]+|[⁰¹²³⁴⁵⁶⁷⁸⁹]+)|[+-]?∞)${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match) => {
      return protectToken(`$${normalizePlainMathBody(match.trim())}$`);
    }
  );

  // 10. 括号内的纯文本指数/下标算式（如 (1-β1^t)、(1-β2^t)、(a+b)^2），排除代码标识符与自然语言短语
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}(\\(([0-9a-zA-Zα-ωΑ-Ω+\\-*/^_.\\s]+)\\)(?:\\^(?:[a-zA-Z0-9]+|\\{[^{}]+\\}))?)${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match, fullExpr: string, inner: string) => {
      const trimmedInner = inner.trim();
      if (!trimmedInner || PROSE_WORD_PAIR_RE.test(trimmedInner)) return match;
      if (!/[a-zA-Zα-ωΑ-Ω]/.test(trimmedInner)) return match;
      const hasPowerOrComplexSub =
        /\^/.test(trimmedInner) ||
        (/[\u03b1-\u03c9\u0391-\u03a9+\-*/]/.test(trimmedInner) && /_/.test(trimmedInner));
      if (!hasPowerOrComplexSub && !/\^/.test(fullExpr)) return match;
      const normalized = normalizeUnicodeGreekInMath(fullExpr.trim());
      return protectToken(`$${normalized}$`);
    }
  );

  // 10.1 连续 Unicode 希腊字母乘积项（如散文里的 λθ、αβ），高置信度为数学变量乘积
  s = s.replace(
    /(?<=[:：，,；;、。在对于当若使设令与及和将把由从\u4e00-\u9fa5\s]|^)([α-ωΑ-Ω]{2,})(?=[:：，,；;、。在对于当若使设令与及和并的为是\u4e00-\u9fa5\s]|$)/g,
    (match) => protectToken(`$${normalizeUnicodeGreekInMath(match).trim()}$`)
  );

  // 11. 还原占位符
  return s.replace(/%%%PROSE_MATH_TOKEN_(\d+)%%%/g, (_m, idxStr) => {
    const idx = Number(idxStr);
    return tokens[idx] ?? _m;
  });
}

const CJK_OR_FULLWIDTH_RE = /[\u3000-\u303f\u4e00-\u9fff\uff00-\uffef]/;
const BARE_FORMULA_CHARS_RE = /^[0-9A-Za-z\s+\-*/=().,;:^_|<>≤≥≠\\']+$/;
/** 连续两个 3 字母以上单词（The value / value of）基本可判定为自然语言句子，而不是公式 */
const PROSE_WORD_PAIR_RE = /[A-Za-z]{3,}\s+[A-Za-z]{3,}/;

/**
 * 纯文本公式判定：兜底渲染未加 $ 定界符的选项与答案（如 1/2、3/(4t)、-1/e、1/e^2、e^6、2^h - 1）。
 */
export function isBareFormulaSegment(text: string): boolean {
  const trimmed = text.trim();
  if (!trimmed || trimmed.length > 160) {
    return false;
  }
  if (CJK_OR_FULLWIDTH_RE.test(trimmed) || PROSE_WORD_PAIR_RE.test(trimmed)) {
    return false;
  }
  // 关键防御：若文本已包含任何标准数学定界符（$、\(、\)、\[、\]、\begin{），绝不可视为 Bare（裸）公式
  if (trimmed.includes('$') || /\\\(|\\\)|\\\[|\\\]|\\begin\{/.test(trimmed)) {
    return false;
  }
  // 1. 纯数字分数：例如 1/2, -1/2, 3/4
  if (/^[+-]?\d+\s*\/\s*\d+$/.test(trimmed)) {
    return true;
  }
  // 2. 纯代数分式：例如 3/(4t), -1/e, 1/e^2, a/b（排除类似 "B.2" 这种带选项前缀的伪代数）
  if (!/^[A-Ha-h]\./.test(trimmed) && /^[+-]?[0-9a-zA-Z()^.'_]+\s*\/\s*[0-9a-zA-Z()^.'_]+$/.test(trimmed) && /[a-zA-Z]/.test(trimmed)) {
    return true;
  }
  // 3. 含上下标、关系符或运算的紧凑公式：例如 e^6, 2^h - 1, dy/dx = 3t/2
  if (/[\^_=≠≤≥<>]/.test(trimmed)) {
    return BARE_FORMULA_CHARS_RE.test(trimmed);
  }
  return false;
}

/**
 * 对整段孤立的选项/答案纯数学分式进行安全包裹（如将选项 "1/2"、"3/(4t)" 包装为 "$\\frac{1}{2}$"）
 */
export function wrapIsolatedFormulas(text: string): string {
  let trimmed = text.trim();
  // 已经包含 $ 或 \( \) 或 \[ \] 或 \begin{ 时，严禁二次包裹
  if (!trimmed || trimmed.includes('$') || /\\\(|\\\)|\\\[|\\\]|\\begin\{/.test(trimmed)) return text;

  // 防御性剔除选项前缀（如 "B. 2/3"、"B.2/3"、"A. -2/3" 等），避免把前缀误卷入数学公式或分式分子
  const prefixMatch = trimmed.match(/^(?:[([（【]\s*([A-Ha-h])\s*[)\\]）】]\s*[.．、,:：\-—\s]*|([A-Ha-h])\s*[.．、,:：)）\-—]+\s*)/);
  if (prefixMatch) {
    const withoutPrefix = trimmed.slice(prefixMatch[0].length).trim();
    if (withoutPrefix && isBareFormulaSegment(withoutPrefix)) {
      trimmed = withoutPrefix;
    }
  }

  if (!isBareFormulaSegment(trimmed)) return text;

  // 纯分式格式化为漂亮的 \frac
  const fracMatch = trimmed.match(/^([+-]?)([0-9a-zA-Z()^.'_]+)\s*\/\s*([0-9a-zA-Z()^.'_]+)$/);
  if (fracMatch) {
    const sign = fracMatch[1] || '';
    let num = fracMatch[2].replace(/^\((.*)\)$/, '$1');
    let den = fracMatch[3].replace(/^\((.*)\)$/, '$1');

    // 若分子仍带大写选项标号（如 "B."），安全剥离
    if (/^[A-Ha-h]\./.test(num)) {
      num = num.replace(/^[A-Ha-h]\.\s*/, '');
    }

    return `$${sign}\\frac{${num}}{${den}}$`;
  }

  return `$${trimmed}$`;
}

/**
 * 修复因标题字符截断导致的未闭合 LaTeX 公式（如 "$\lim_{x \to 0} \frac{\ln(1 + 2x) - \sin(2x)..."）
 * 自动修补括号、大括号并闭合 $，保证 KaTeX 能够正常解析渲染，消除裸露的 LaTeX 源码
 */
export function repairTruncatedFormulas(text: string): string {
  if (!text || !text.includes('$')) return text;

  // 统计非转义 $ 出现的数量
  const dollarMatches = text.match(/(?<!\\)\$/g) || [];
  if (dollarMatches.length % 2 === 0) return text;

  const lastDollarIdx = text.lastIndexOf('$');
  if (lastDollarIdx === -1) return text;

  const prefix = text.slice(0, lastDollarIdx);
  let mathPart = text.slice(lastDollarIdx + 1);

  // 提取末尾的省略号标记（如 "..."、"…"）
  let ellipsis = '';
  const ellMatch = mathPart.match(/(\.{3,}|…)+$/);
  if (ellMatch) {
    ellipsis = ellMatch[0];
    mathPart = mathPart.slice(0, mathPart.length - ellipsis.length).trim();
  }

  // 统计未闭合的括号
  let openBraces = 0;
  let openParens = 0;
  for (let j = 0; j < mathPart.length; j++) {
    const ch = mathPart[j];
    if (ch === '{' && (j === 0 || mathPart[j - 1] !== '\\')) openBraces++;
    else if (ch === '}' && (j === 0 || mathPart[j - 1] !== '\\')) {
      if (openBraces > 0) openBraces--;
    } else if (ch === '(' && (j === 0 || mathPart[j - 1] !== '\\')) openParens++;
    else if (ch === ')' && (j === 0 || mathPart[j - 1] !== '\\')) {
      if (openParens > 0) openParens--;
    }
  }

  while (openParens > 0) {
    mathPart += ')';
    openParens--;
  }
  while (openBraces > 0) {
    mathPart += '}';
    openBraces--;
  }

  // 若以未闭合的 \frac 开头或结尾，补齐分母
  if (/\\frac\{[^{}]*\}\s*$/.test(mathPart)) {
    mathPart += '{}';
  } else if (/\\frac\s*$/.test(mathPart)) {
    mathPart += '{}{}';
  }

  return `${prefix}$${mathPart}$${ellipsis}`;
}

/**
 * 修复并清洗大模型经常直接以原始 JSON 字符串格式输出的 distractorAnalysis / 选项诊断
 * 例如：{"A":"正确。原极限值为 2。","B":"正确...","C":"...","D":"..."}
 * 转换为高可读性的结构化考点与干扰项诊断列表（同时安全排除普通计算机代码与数据字典）
 */
export function formatRawJsonDistractors(text: string): string {
  if (!text || !text.includes('{')) return text;

  // 寻找形如 { "A": 或 { 'A': 或 {\n"A": 的潜在选项 JSON 字典起始位置
  const markerPattern = /\{[\s\r\n]*["']?([A-H])["']?\s*:/g;
  let match: RegExpExecArray | null;
  let lastIndex = 0;
  let result = '';

  while ((match = markerPattern.exec(text)) !== null) {
    const startIndex = match.index;
    result += text.slice(lastIndex, startIndex);

    // 从 startIndex 开始寻找真正匹配的闭合右大括号 }（精确考虑引号转义与花括号嵌套深度）
    let braceDepth = 0;
    let endIndex = -1;
    let inQuotes = false;
    let quoteChar = '';

    for (let i = startIndex; i < text.length; i++) {
      const ch = text[i];

      if (inQuotes) {
        if (ch === quoteChar) {
          let backslashCount = 0;
          let k = i - 1;
          while (k >= startIndex && text[k] === '\\') {
            backslashCount++;
            k--;
          }
          if (backslashCount % 2 === 0) {
            inQuotes = false;
          }
        }
      } else {
        if (ch === '"' || ch === "'") {
          let backslashCount = 0;
          let k = i - 1;
          while (k >= startIndex && text[k] === '\\') {
            backslashCount++;
            k--;
          }
          if (backslashCount % 2 === 0) {
            inQuotes = true;
            quoteChar = ch;
          }
        } else if (ch === '{') {
          braceDepth++;
        } else if (ch === '}') {
          braceDepth--;
          if (braceDepth === 0) {
            endIndex = i;
            break;
          }
        }
      }
    }

    if (endIndex !== -1) {
      const rawBlock = text.slice(startIndex, endIndex + 1);
      const converted = convertDistractorJsonToReadableText(rawBlock);
      if (converted) {
        result += `\n\n${converted}\n\n`;
        lastIndex = endIndex + 1;
        markerPattern.lastIndex = endIndex + 1;
      } else {
        result += rawBlock;
        lastIndex = endIndex + 1;
        markerPattern.lastIndex = endIndex + 1;
      }
    } else {
      result += text.slice(startIndex, startIndex + match[0].length);
      lastIndex = startIndex + match[0].length;
    }
  }

  result += text.slice(lastIndex);
  return result;
}

const TEACHING_DIAGNOSIS_KEYWORDS_RE =
  /正确|错误|易错|干扰|考点|剖析|解析|因为|所以|由于|选项|原式|极限|结论|推导|符合|不符|矛盾|反例|排除|同理|计算|求解|代入|注意/;

function isLikelyDistractorAnalysis(entries: Array<{ key: string; val: string }>): boolean {
  if (entries.length < 2) return false;
  // 必须表现出教学诊断分析语境，或者内容较长包含解题评述；避免误伤数据结构邻接表或普通配置对象
  const hasDiagnosisKeyword = entries.some((e) => TEACHING_DIAGNOSIS_KEYWORDS_RE.test(e.val));
  const hasSubstantialExplanation = entries.some((e) => e.val.trim().length >= 8);
  return hasDiagnosisKeyword || hasSubstantialExplanation;
}

function convertDistractorJsonToReadableText(rawJson: string): string | null {
  // 1. 优先尝试直接 JSON.parse（处理标准转义）
  try {
    const parsed = JSON.parse(rawJson);
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      const keys = Object.keys(parsed).filter((k) => /^[A-H]$/i.test(k));
      if (keys.length >= 2) {
        const entries = keys.map((k) => ({ key: k.toUpperCase(), val: String(parsed[k] || '').trim() }));
        if (isLikelyDistractorAnalysis(entries)) {
          entries.sort((a, b) => a.key.localeCompare(b.key));
          const lines: string[] = ['【各选项考点诊断与干扰项剖析】'];
          for (const e of entries) {
            lines.push(`• 选项 ${e.key}：${e.val}`);
          }
          return lines.join('\n');
        }
      }
    }
  } catch {
    // 继续后续宽松解析
  }

  // 2. 尝试修复大模型输出的未双转义反斜杠（如 \lim, \frac, \to 等），使得符合 JSON 标准
  try {
    const fixedJson = rawJson.replace(/\\(?!["\\/bfnrt]|u[0-9a-fA-F]{4})/g, '\\\\');
    const parsed = JSON.parse(fixedJson);
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      const keys = Object.keys(parsed).filter((k) => /^[A-H]$/i.test(k));
      if (keys.length >= 2) {
        const entries = keys.map((k) => ({ key: k.toUpperCase(), val: String(parsed[k] || '').trim() }));
        if (isLikelyDistractorAnalysis(entries)) {
          entries.sort((a, b) => a.key.localeCompare(b.key));
          const lines: string[] = ['【各选项考点诊断与干扰项剖析】'];
          for (const e of entries) {
            lines.push(`• 选项 ${e.key}：${e.val}`);
          }
          return lines.join('\n');
        }
      }
    }
  } catch {
    // 继续正则提取
  }

  // 3. 稳健的键值对模式提取：兼容截断或严重语法偏差的伪 JSON
  const itemPattern = /["']?([A-H])["']?\s*:\s*["']([\s\S]*?)(?:["']\s*(?:,|\}))/g;
  const entries: Array<{ key: string; val: string }> = [];
  let m: RegExpExecArray | null;
  while ((m = itemPattern.exec(rawJson)) !== null) {
    entries.push({ key: m[1].toUpperCase(), val: m[2].trim() });
  }
  if (entries.length >= 2 && isLikelyDistractorAnalysis(entries)) {
    entries.sort((a, b) => a.key.localeCompare(b.key));
    const lines: string[] = ['【各选项考点诊断与干扰项剖析】'];
    for (const e of entries) {
      lines.push(`• 选项 ${e.key}：${e.val}`);
    }
    return lines.join('\n');
  }

  return null;
}

const STRUCTURED_SECTION_TITLES = [
  '核心考点',
  '考点分析',
  '解题推导步骤',
  '解题思路',
  '解题步骤',
  '详细解析',
  '易错思维误区',
  '易错警示',
  '思维误区',
  '避坑指南',
  '各选项考点诊断与干扰项剖析',
  '各选项诊断',
  '干扰项辨析',
  '干扰项剖析',
  '参考答案',
  '标准答案',
  '评分要点',
  '采分点说明',
  '得分细则'
].join('|');

const STRUCTURED_SECTION_TITLE_RE = new RegExp(`【(${STRUCTURED_SECTION_TITLES})】`, 'g');

/**
 * 确保试题解析中的核心结构化小标题独立分段，杜绝段落粘连
 * 仅在句子终结标点（。！？；;~）或已有换行后生效，避免破坏紧凑单行文本排版
 */
export function formatStructuredSectionBreaks(text: string): string {
  if (!text) return '';
  // 仅当前文紧贴在句号、叹号、分号或换行后时，将其规范拆分为独立空行段落，杜绝紧凑单行布局被撕裂
  const re = new RegExp(`(?<=[。！？；;~\\n])[ \\t]*【(${STRUCTURED_SECTION_TITLES})】`, 'g');
  return text.replace(re, '\n\n【$1】');
}

/**
 * 智能识别并修复被腰斩/错位 $ 撕裂或纯文本书写的分段函数（如 f(x)= { (1- $\cos x)/x^2... } 或 f(x)= { ... }）
 * 规范化为高标准的 LaTeX cases 语法，杜绝括号失衡导致的 KaTeX ParseError 与原样文本降级
 */
export function healAndFormatPiecewiseFunctions(text: string): string {
  if (!text || (!text.includes('{') && !text.includes('cases'))) {
    return text;
  }

  // 匹配分段函数头部：如 f(x)= {、f(x) = {、y = {、u(t) = {、f(x)=\{、f(x)=\left\{
  // 兼容分段函数前面可能带有单边 $（例如 $f(x)= { ... ）或内部包含错位 $
  const headerRegex = /(?:^|(?<=[:：，,；;、。\s\u4e00-\u9fa5\uff00-\uffef]))(?:\$)?([fghuvyFGHUV](?:\([a-zA-Z0-9]+\))?|[a-zA-Z])\s*=\s*(?:\\left\s*)?\\?\{\s*/g;

  let result = '';
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = headerRegex.exec(text)) !== null) {
    const startIndex = match.index;
    const fnHeader = match[1];
    const braceOpenIndex = match.index + match[0].length - 1; // 真正定位到 '{' 字符

    // 从 braceOpenIndex 开始寻找最外层配对的 '}'
    let depth = 1;
    let endIndex = -1;
    let i = braceOpenIndex + 1;
    while (i < text.length) {
      const ch = text[i];
      // 遇中文标点或段落换行提前终止，防止跨段落过度吃进
      if (ch === '。' || ch === '！' || ch === '？' || (ch === '\n' && text[i - 1] === '\n')) {
        break;
      }
      if (ch === '{' && (i === 0 || text[i - 1] !== '\\')) {
        depth++;
      } else if (ch === '}' && (i === 0 || text[i - 1] !== '\\')) {
        depth--;
        if (depth === 0) {
          endIndex = i;
          break;
        }
      }
      i++;
    }

    if (endIndex !== -1) {
      let rawBody = text.slice(braceOpenIndex + 1, endIndex);
      // 去除内部可能由后端或模型误插入的孤立错位 $
      rawBody = rawBody.replace(/(?<!\\)\$/g, '').trim();

      const branches = rawBody.split(/\s*;\s*|\s*\\\\\s*|\n+/).filter(Boolean);
      if (branches.length >= 2) {
        const latexRows: string[] = [];
        let isValidCases = true;

        for (const branch of branches) {
          const trimmed = branch.trim();
          if (!trimmed) continue;
          let commaIdx = trimmed.indexOf(',');
          if (commaIdx === -1) {
            commaIdx = trimmed.indexOf('，');
          }
          if (commaIdx !== -1) {
            let expr = trimmed.slice(0, commaIdx).trim();
            let cond = trimmed.slice(commaIdx + 1).trim();

            // 规范化分子分母分式
            expr = expr.replace(/\(([^()]+)\)\s*\/\s*\(([^()]+)\)/g, '\\frac{$1}{$2}');
            expr = expr.replace(/\(([^()]+)\)\s*\/\s*([a-zA-Z0-9^_{}\\]+)/g, '\\frac{$1}{$2}');
            expr = expr.replace(/([a-zA-Z0-9^_{}\\]+)\s*\/\s*\(([^()]+)\)/g, '\\frac{$1}{$2}');
            expr = expr.replace(/([a-zA-Z0-9^]+)\s*\/\s*([a-zA-Z0-9^]+)/g, '\\frac{$1}{$2}');

            // 规范化比较符
            cond = cond
              .replace(/!=|≠/g, ' \\ne ')
              .replace(/<=|≤/g, ' \\le ')
              .replace(/>=|≥/g, ' \\ge ');

            latexRows.push(`${expr}, & ${cond}`);
          } else {
            latexRows.push(trimmed);
          }
        }

        if (isValidCases && latexRows.length >= 2) {
          result += text.slice(lastIndex, startIndex);
          result += `$${fnHeader.trim()} = \\begin{cases} ${latexRows.join(' \\\\ ')} \\end{cases}$`;

          // 跳过可能紧跟的闭合 $（如果整个分段函数原本包在 $ 里）
          let nextIndex = endIndex + 1;
          if (nextIndex < text.length && text[nextIndex] === '$') {
            nextIndex++;
          }
          lastIndex = nextIndex;
          headerRegex.lastIndex = nextIndex;
          continue;
        }
      }
    }

    result += text.slice(lastIndex, startIndex + match[0].length);
    lastIndex = startIndex + match[0].length;
  }

  result += text.slice(lastIndex);
  return result;
}

/**
 * 专门提取高数未定式与复合幂次/点乘表达式，杜绝拆分碎片化与代码裸露
 * 例如：1^\infty型未定式、[(1+sin x)^{1/sin x}]^{sin x/x}、[(1+\alpha(x))^{1/\alpha(x)}]^{\alpha(x)\beta(x)}、
 * (1+sin x)^{1/x}、(1/sin x)·(sin x/x) 等
 */
export function wrapIndeterminateAndCompositeMath(text: string): string {
  if (!text) return '';

  // 1. 保护已有标准数学公式占位符
  const tokens: string[] = [];
  let s = text.replace(
    /(\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\\([\s\S]*?\\\)|\$[^$\n]+?\$)/g,
    (m) => {
      const idx = tokens.length;
      tokens.push(m);
      return `%%%COMPOSITE_TOKEN_${idx}%%%`;
    }
  );

  // 2. 高等数学核心未定式：1^\infty、0^0、\infty^0、0/0、\infty/\infty、\infty - \infty、0·\infty、0 \cdot \infty
  s = s.replace(
    /(?<=[:：，,；;、。在对于当若使设令与及和点处到从关于\s]|^)([01]\^\\infty|[01]\^0|\\infty\^0|0\/0|\\infty\/\\infty|\\infty\s*-\s*\\infty|0\s*[\*·\u22c5\u00b7]\s*\\infty)(?=[型类:：，,；;、。在对于当若使设令与及和点处到从的内邻域连续可导\s]|$)/g,
    '$$$1$$'
  );

  // 3. 带有外层嵌套幂次的复杂代数变形：例如 [(1+sin x)^{1/sin x}]^{sin x/x}、[(1+\alpha(x))^{1/\alpha(x)}]^{\alpha(x)\beta(x)}
  s = s.replace(
    /(?<=[:：，,；;、。在对于当若使设令与及和为化写\s]|^)(\[\s*\(.*?\)\^\{.*?\}\s*\]\^\{.*?\})(?=[:：，,；;、。在对于当若使设令与及和的\s]|$)/g,
    (m) => {
      let formula = m.replace(/\b(sin|cos|tan|cot|ln|exp)\b/g, '\\$1 ');
      formula = formula.replace(/\\(sin|cos|tan|cot|ln|exp)\s+/g, '\\$1 ');
      formula = convertInlineSlashesToFractions(formula);
      return `$${formula}$`;
    }
  );

  // 4. 基础圆括号幂次表达式：例如 (1+\sin x)^{1/x}、(1+sin x)^{1/x}、(1+\alpha(x))^{\beta(x)}
  s = s.replace(
    /(?<=[:：，,；;、。在对于当若使设令与及和对为化写成\s]|^)(\(\s*1\s*[+\-]\s*(?:\\[a-zA-Z]+|[a-zA-Z]+)(?:\s+[a-zA-Z0-9]+|\([a-zA-Z0-9,\s]+\))?\s*\)\^\{?[a-zA-Z0-9^_{}/().\-\\\s]+\}?)(?=[:：，,；;、。在对于当若使设令与及和的求导\u4e00-\u9fa5\s]|$)/g,
    (m) => {
      let formula = m.replace(/\b(sin|cos|tan|cot|ln|exp)\b/g, '\\$1 ');
      formula = formula.replace(/\\(sin|cos|tan|cot|ln|exp)\s+/g, '\\$1 ');
      formula = convertInlineSlashesToFractions(formula);
      return `$${formula}$`;
    }
  );

  // 5. 散文中由点乘连接的分式变形：例如 (1/sin x)·(sin x/x)、(1/x)·(x/sin x)
  s = s.replace(
    /(?<=[:：，,；;、。在对于当若使设令与及和为化写成\s]|^)(\([^()]+\)\s*[·\u22c5\u00b7\*]\s*\([^()]+\))(?=[:：，,；;、。在对于当若使设令与及和的，,\s]|$)/g,
    (m) => {
      let formula = m.replace(/·|\u22c5|\u00b7/g, ' \\cdot ');
      formula = formula.replace(/\b(sin|cos|tan|cot|ln|exp)\b/g, '\\$1 ');
      formula = formula.replace(/\\(sin|cos|tan|cot|ln|exp)\s+/g, '\\$1 ');
      formula = convertInlineSlashesToFractions(formula);
      return `$${formula}$`;
    }
  );

  // 还原占位符
  return s.replace(/%%%COMPOSITE_TOKEN_(\d+)%%%/g, (_m, idxStr) => {
    const idx = Number(idxStr);
    return tokens[idx] ?? _m;
  });
}

type MathSegment = { kind: 'text'; value: string } | { kind: 'math'; value: string; display: boolean };

function splitMathSegments(text: string): MathSegment[] {
  // 多级流水线：清洗 JSON 干扰项 -> 结构化小标题分段 -> 预清洗换行 -> 修复未闭合截断公式 -> 优先缝合与规范分段函数 -> 提取未定式与复合幂次 -> 提取微积分 -> 提取散文等式/不等式 -> 提取孤立裸LaTeX -> 提取孤立分式
  const cleanedJson = formatRawJsonDistractors(text);
  const structured = formatStructuredSectionBreaks(cleanedJson);
  const preprocessed = normalizeMathTextNewlines(structured);
  const repaired = repairTruncatedFormulas(preprocessed);
  const healedCases = healAndFormatPiecewiseFunctions(repaired);
  const withComposite = wrapIndeterminateAndCompositeMath(healedCases);
  const withCalc = wrapCalculusExpressions(withComposite);
  const withProse = wrapProsePlainMath(withCalc);
  const withBareLatex = wrapBareLatexExpressions(withProse);
  const normalized = wrapIsolatedFormulas(withBareLatex);

  const segments: MathSegment[] = [];
  let i = 0;

  while (i < normalized.length) {
    if (normalized.startsWith('$$', i)) {
      const end = normalized.indexOf('$$', i + 2);
      if (end !== -1) {
        segments.push({ kind: 'math', value: normalized.slice(i + 2, end), display: true });
        i = end + 2;
        continue;
      }
    }
    if (normalized.startsWith('\\[', i)) {
      const end = normalized.indexOf('\\]', i + 2);
      if (end !== -1) {
        segments.push({ kind: 'math', value: normalized.slice(i + 2, end), display: true });
        i = end + 2;
        continue;
      }
    }
    if (normalized.startsWith('\\(', i)) {
      const end = normalized.indexOf('\\)', i + 2);
      if (end !== -1) {
        segments.push({ kind: 'math', value: normalized.slice(i + 2, end), display: false });
        i = end + 2;
        continue;
      }
    }
    if (normalized[i] === '$' && normalized[i + 1] !== '$') {
      const end = normalized.indexOf('$', i + 1);
      if (end !== -1) {
        segments.push({ kind: 'math', value: normalized.slice(i + 1, end), display: false });
        i = end + 1;
        continue;
      }
    }

    let next = normalized.length;
    for (const marker of ['$$', '\\[', '\\(', '$']) {
      const pos = normalized.indexOf(marker, i + 1);
      if (pos !== -1 && pos < next) {
        next = pos;
      }
    }
    const chunk = normalized.slice(i, next);
    if (chunk) {
      segments.push({ kind: 'text', value: chunk });
    }
    i = next;
  }

  return segments;
}

type TopLevelSegment =
  | { kind: 'prose'; value: string }
  | { kind: 'code'; lang: string; value: string };

/**
 * 优先抽离 Markdown 围栏代码块（```lang ... ```），防止代码中的循环条件、赋值语句、
 * 下划线或变量名被后续数学公式解析流水线误伤，并完整保留代码缩进与换行。
 */
function splitFenceCodeBlocks(text: string): TopLevelSegment[] {
  const normalized = text.replace(/\r\n?/g, '\n');
  const fenceRe = /```([a-zA-Z0-9_+-]*)[ \t]*\n?([\s\S]*?)```/g;
  const segments: TopLevelSegment[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = fenceRe.exec(normalized)) !== null) {
    if (match.index > lastIndex) {
      const prose = normalized.slice(lastIndex, match.index);
      if (prose) {
        segments.push({ kind: 'prose', value: prose });
      }
    }
    const lang = (match[1] || '').trim();
    // 去掉代码块首尾多余的换行符，兼容字面量 \n 转义，完整保留内部缩进与逐行换行
    const codeBody = (match[2] || '')
      .replace(/\\r\\n/g, '\n')
      .replace(/\\n(?![a-zA-Z])/g, '\n')
      .replace(/^\n+|\n+$/g, '');
    segments.push({ kind: 'code', lang, value: codeBody });
    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < normalized.length) {
    const tail = normalized.slice(lastIndex);
    if (tail) {
      segments.push({ kind: 'prose', value: tail });
    }
  }
  return segments;
}

/**
 * 优先抽离并保护单反引号行内代码（如 `` `int a = 5; double x = a / b * 1.0;` ``），
 * 避免代码中的 `=`、`/`、`*`、`&&` 被后续裸公式识别流水线误改写为 LaTeX（导致括号或反引号配对错乱）。
 */
function protectInlineCodeTokens(text: string, tokens: string[]): string {
  if (!text || !text.includes('`')) return text;

  let out = '';
  let i = 0;
  const len = text.length;

  while (i < len) {
    // 跳过已完整闭合的块级/行内数学公式，避免误切公式内部字符
    if (text.startsWith('$$', i)) {
      const end = text.indexOf('$$', i + 2);
      if (end !== -1) {
        out += text.slice(i, end + 2);
        i = end + 2;
        continue;
      }
    }
    if (text.startsWith('\\[', i)) {
      const end = text.indexOf('\\]', i + 2);
      if (end !== -1) {
        out += text.slice(i, end + 2);
        i = end + 2;
        continue;
      }
    }
    if (text.startsWith('\\(', i)) {
      const end = text.indexOf('\\)', i + 2);
      if (end !== -1) {
        out += text.slice(i, end + 2);
        i = end + 2;
        continue;
      }
    }

    if (text[i] === '`') {
      const nextTick = text.indexOf('`', i + 1);
      const nextNewline = text.indexOf('\n', i + 1);
      if (nextTick !== -1 && (nextNewline === -1 || nextTick < nextNewline)) {
        const codeContent = text.slice(i + 1, nextTick);
        if (codeContent.length > 0) {
          // 防御模型误嵌套：例如 `$a$`、`$a/b*1.0$`，反引号内又包裹了完整 `$...$`
          const nestedMathMatch = codeContent.trim().match(/^\$([^$\n]+)\$$/);
          if (nestedMathMatch) {
            const inner = nestedMathMatch[1].trim();
            const isCodeLike = /[;"'&|]|->|\+\+|--|\b(?:printf|scanf|int|double|float|char|void|return|for|while|if)\b/.test(
              inner
            );
            if (!isCodeLike) {
              // 数学符号/表达式：剥除误套的反引号，交由后续数学公式链路渲染
              out += `$${inner}$`;
              i = nextTick + 1;
              continue;
            }
            // 带代码的表达式：剥除多余美元符号，仅保留干净代码进占位符
            const idx = tokens.length;
            tokens.push(inner);
            out += `\uE000${idx}\uE001`;
            i = nextTick + 1;
            continue;
          }
          const idx = tokens.length;
          tokens.push(codeContent);
          out += `\uE000${idx}\uE001`;
          i = nextTick + 1;
          continue;
        }
      }
    }

    out += text[i];
    i++;
  }

  return out;
}

/**
 * 判断单反引号包裹的代码是否实际是一段「被压成一行的多语句程序片段」（如编程阅读题题干）。
 *
 * <p>忽略字符串字面量与圆括号内部（如 for 循环头 `for (int i = 0; i < n; i++)`）的分号，
 * 仅统计顶层语句结束分号、预处理器指令与控制流花括号，避免误伤单个函数调用（如 `scanf("%f", &x);`）。</p>
 */
function shouldPromoteInlineCodeToBlock(code: string): boolean {
  const trimmed = code.trim();
  if (trimmed.length < 20) return false;
  if (/^\s*#include\b/m.test(trimmed)) return true;

  let topSemicolons = 0;
  let hasCodeBraceBlock = false;
  let parenDepth = 0;
  let inDoubleQuote = false;
  let inSingleQuote = false;

  for (let i = 0; i < trimmed.length; i++) {
    const ch = trimmed[i];
    const prev = i > 0 ? trimmed[i - 1] : '';
    if (inDoubleQuote) {
      if (ch === '"' && prev !== '\\') inDoubleQuote = false;
      continue;
    }
    if (inSingleQuote) {
      if (ch === '\'' && prev !== '\\') inSingleQuote = false;
      continue;
    }
    if (ch === '"') {
      inDoubleQuote = true;
    } else if (ch === '\'') {
      inSingleQuote = true;
    } else if (ch === '(') {
      parenDepth++;
    } else if (ch === ')' && parenDepth > 0) {
      parenDepth--;
    } else if (ch === ';' && parenDepth === 0) {
      topSemicolons++;
    } else if (ch === '{' && parenDepth === 0) {
      // 确保是代码语句块而非数组初始化 `{1, 2, 3}`
      const rest = trimmed.slice(i);
      if (rest.includes(';')) {
        hasCodeBraceBlock = true;
      }
    }
  }

  return topSemicolons >= 2 || hasCodeBraceBlock;
}

/**
 * 将被压成单行的多语句代码拆分为逐行缩进的可读代码块，方便程序阅读与追踪执行过程。
 */
function formatPromotedCodeBlock(code: string): string {
  const trimmed = code.trim();
  if (trimmed.includes('\n')) {
    return trimmed;
  }

  let out = '';
  let parenDepth = 0;
  let indentLevel = 0;
  let inDoubleQuote = false;
  let inSingleQuote = false;

  const pushNewline = () => {
    out = out.replace(/[ \t]+$/, '');
    out += '\n' + '  '.repeat(indentLevel);
  };

  for (let i = 0; i < trimmed.length; i++) {
    const ch = trimmed[i];
    const prev = i > 0 ? trimmed[i - 1] : '';

    if (inDoubleQuote) {
      out += ch;
      if (ch === '"' && prev !== '\\') inDoubleQuote = false;
      continue;
    }
    if (inSingleQuote) {
      out += ch;
      if (ch === '\'' && prev !== '\\') inSingleQuote = false;
      continue;
    }

    if (ch === '"') {
      inDoubleQuote = true;
      out += ch;
      continue;
    }
    if (ch === '\'') {
      inSingleQuote = true;
      out += ch;
      continue;
    }

    if (ch === '(') {
      parenDepth++;
      out += ch;
      continue;
    }
    if (ch === ')') {
      if (parenDepth > 0) parenDepth--;
      out += ch;
      continue;
    }

    // 预处理器指令 `#include <...>` / `#include "..."` 末尾如紧接代码，自动换行
    if (ch === '#' && (i === 0 || trimmed[i - 1] === ' ')) {
      const rest = trimmed.slice(i);
      const incMatch = rest.match(/^#\s*include\s*(?:<[^>]+>|"[^"]+")/);
      if (incMatch) {
        out += incMatch[0];
        i += incMatch[0].length - 1;
        if (trimmed.slice(i + 1).trim().length > 0) {
          pushNewline();
          while (i + 1 < trimmed.length && trimmed[i + 1] === ' ') i++;
        }
        continue;
      }
    }

    // 控制流/函数体花括号换行缩进（忽略数组字面量 `{1, 2, 3}`）
    if (ch === '{' && parenDepth === 0 && trimmed.slice(i).includes(';')) {
      out = out.replace(/[ \t]+$/, '') + ' {';
      indentLevel++;
      if (trimmed.slice(i + 1).trim().length > 0) {
        pushNewline();
        while (i + 1 < trimmed.length && trimmed[i + 1] === ' ') i++;
      }
      continue;
    }

    if (ch === '}' && parenDepth === 0 && indentLevel > 0) {
      indentLevel--;
      out = out.replace(/[ \t\n]+$/, '');
      if (out.length > 0) {
        pushNewline();
      }
      out += '}';
      const after = trimmed.slice(i + 1);
      if (after.trim().length > 0 && !/^\s*(?:else\b|;)/.test(after)) {
        pushNewline();
        while (i + 1 < trimmed.length && trimmed[i + 1] === ' ') i++;
      } else if (/^\s*else\b/.test(after)) {
        out += ' ';
        while (i + 1 < trimmed.length && trimmed[i + 1] === ' ') i++;
      }
      continue;
    }

    if (ch === ';' && parenDepth === 0) {
      out += ';';
      if (trimmed.slice(i + 1).trim().length > 0) {
        pushNewline();
        while (i + 1 < trimmed.length && trimmed[i + 1] === ' ') i++;
      }
      continue;
    }

    out += ch;
  }

  return out.trim();
}

function renderProseMathSegment(text: string): string {
  const inlineCodeTokens: string[] = [];
  const protectedText = protectInlineCodeTokens(text, inlineCodeTokens);
  const restorePlainCode = (s: string) =>
    s.replace(/\uE000(\d+)\uE001/g, (_m, idxStr) => `\`${inlineCodeTokens[Number(idxStr)] ?? ''}\``);

  return splitMathSegments(protectedText)
    .map((seg) => {
      if (seg.kind === 'math') {
        const html = renderKatex(restorePlainCode(seg.value), seg.display);
        return seg.display ? `<span class="math-block">${html}</span>` : html;
      }
      const parts = seg.value.replace(/\r/g, '').split(/\n{2,}/);
      return parts
        .map((part) => {
          // 保留 Markdown 标题换行（如 ## 标题\n正文、## 标题：正文）与列表项/子编号换行（如 •/·/- 、1. 、**1. 、（1）、（一）、①、要点一：）
          const listBreakPart = part
            .replace(/(?<=\n|^)(#{1,6}\s+[^：:\n]+[：:])\s*/g, '$1\n')
            .replace(/(?<=\n|^)(#{1,6}\s+[^\n]+)\n/g, '$1<br />')
            .replace(
              /\n(?=[•·\-\*]|#{1,6}\s|\*{0,2}(?:\d{1,2}(?:\.(?!\d)|、)|[（(](?:\d{1,2}|[一二三四五六七八九十]+)[)）]|[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]|要点[一二三四五六七八九十\d]+[：:]))/g,
              '<br />'
            );
          let escaped = escapeHtml(listBreakPart.replace(/\n/g, ' '));
          // 还原 <br />
          escaped = escaped.replace(/&lt;br \/&gt;/g, '<br />');
          // 处理特定结构化考点小标题加粗视觉样式（严格排除【单选题】、【例1】、【注意】等普通标签）
          escaped = escaped.replace(
            STRUCTURED_SECTION_TITLE_RE,
            '<strong class="math-section-title">【$1】</strong>'
          );
          // 处理 ***加粗斜体*** 与 **加粗** 语法，消除大模型裸露的星号标记
          escaped = escaped.replace(/\*\*\*([^*]+?)\*\*\*/g, '<strong><em>$1</em></strong>');
          escaped = escaped.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>');
          // 将采分点分项符（如「；- <strong>」、「。- <strong>」或行首「- <strong>」、「· <strong>」）统一优化为带圆点的换行分项
          escaped = escaped.replace(/([；;。])\s*[-·]\s*<strong>/g, '$1<br />&bull; <strong>');
          escaped = escaped.replace(/(?:^|<br\s*\/?>|\s)-\s*<strong>/g, '<br />&bull; <strong>');
          escaped = escaped.replace(/(?:^|<br\s*\/?>)·\s*<strong>/g, '<br />&bull; <strong>');
          // 兜底清理孤立残留的 ** 星号
          escaped = escaped.replace(/\*\*/g, '');
          // 精准处理独立成行的 Markdown 标题语法（# ~ ######），严格限定仅在行首或 <br /> 后生效，完整捕获带编号/空格的标题，杜绝误伤代码块与行内编号
          escaped = escaped.replace(
            /(^|<br\s*\/?>)#{1,6}\s+([^<：:\n]+?)(?:[：:]?\s*<br\s*\/?>|[：:]\s*|$)/g,
            (_match, prefix, title) => {
              const cleanTitle = (title || '').trim();
              if (!cleanTitle) return _match;
              const hasLeadingBreak = Boolean(prefix);
              return hasLeadingBreak
                ? `<br /><strong class="math-section-title">【${cleanTitle}】</strong><br />`
                : `<strong class="math-section-title">【${cleanTitle}】</strong><br />`;
            }
          );
          // 还原预先提取的行内代码占位符（单句/标识符用轻量行内样式，多语句程序片段自动升级为竖排独立代码块）
          escaped = escaped.replace(/\uE000(\d+)\uE001/g, (_m, idxStr) => {
            const rawCode = inlineCodeTokens[Number(idxStr)] ?? '';
            if (shouldPromoteInlineCodeToBlock(rawCode)) {
              const formatted = formatPromotedCodeBlock(rawCode);
              return `<span class="math-code-block"><code class="math-code-block__inner">${escapeHtml(formatted)}</code></span>`;
            }
            return `<code class="math-inline-code">${escapeHtml(rawCode)}</code>`;
          });
          return escaped;
        })
        .join('<br /><br />');
    })
    .join('');
}

export function renderMathText(text: string): string {
  if (!text) return '';

  return splitFenceCodeBlocks(text)
    .map((seg) => {
      if (seg.kind === 'code') {
        const langAttr = seg.lang ? ` data-lang="${escapeHtml(seg.lang)}"` : '';
        const formattedCode =
          !seg.value.includes('\n') && shouldPromoteInlineCodeToBlock(seg.value)
            ? formatPromotedCodeBlock(seg.value)
            : seg.value;
        return `<span class="math-code-block"${langAttr}><code class="math-code-block__inner">${escapeHtml(formattedCode)}</code></span>`;
      }
      return renderProseMathSegment(seg.value).replace(/^(?:<br\s*\/?>|\s)+|(?:<br\s*\/?>|\s)+$/g, (m) =>
        m.includes('<br') ? '' : m
      );
    })
    .join('');
}

/**
 * 专为 AI 追问胶囊 / 推荐探索 设计的轻量级行内数学公式渲染器
 * 1. 容错修复大模型畸变与下划线缺失（如 an-a{n-1}=2 -> a_n - a_{n-1} = 2、ana{n+2} -> a_n a_{n+2}、bn -> b_n）
 * 2. 将行内数学公式（带 $ 或未带 $ 的等式、不等式、数列变量）安全渲染为行内 KaTeX
 * 3. 对非公式正文部分进行 HTML 转义（防 XSS），保持自然语言文字与标点符号完好
 */
export function renderFollowUpPromptMath(raw: string): string {
  if (!raw?.trim()) return '';

  let text = raw.trim();

  // 1. 容错修复：裸大括号下标（如 a{n-1} -> a_{n-1}、a{n+2} -> a_{n+2}）
  text = text.replace(/([a-zA-Z])\{([a-zA-Z0-9_+-]+)\}/g, '$1_{$2}');

  // 2. 容错修复：未加下划线的常见数列通项与下标变量
  // 例如：an-a_{n-1} -> a_n - a_{n-1}、an+a_{n-1} -> a_n + a_{n-1}、ana_{n+2} -> a_n a_{n+2}
  text = text.replace(/\bana_\{/g, 'a_n a_{');
  text = text.replace(/(?<=[^a-zA-Z]|^)an(?=[+\-=<>]|[a-zA-Z]_)/g, 'a_n');
  text = text.replace(/(?<=[+\-=<>])an(?=[^a-zA-Z]|$)/g, 'a_n');
  text = text.replace(/(?<=[^a-zA-Z]|^)bn(?=[^a-zA-Z]|$)/g, 'b_n');

  // 3. 将混在散文里的未包裹数学公式转化为行内 $ ... $ 包裹
  text = wrapCalculusExpressions(text);
  text = wrapProsePlainMath(text);

  // 3.1 针对追问中常见的带下标数列算式与关系式（如 a_n - a_{n-1} = 2、a_n + a_{n-1}、a_n a_{n+2}）兜底包裹为 $...$
  text = text.replace(
    /(?<=[\u4e00-\u9fa5（(，,；;。！!？?\s]|^)((?:[a-zA-Z]_[a-zA-Z0-9{}+-]+|[a-zA-Z0-9()^]+)(?:\s*[+\-*/·=≠≤≥<>]+\s*(?:[a-zA-Z]_[a-zA-Z0-9{}+-]+|[a-zA-Z0-9()^]+)|\s+[a-zA-Z]_[a-zA-Z0-9{}+-]+)+)(?=[\u4e00-\u9fa5）)，,；;。！!？?\s]|$)/g,
    (match) => {
      if (match.includes('$') || !/[a-zA-Z]_[a-zA-Z0-9{}+-]/.test(match)) {
        return match;
      }
      return `$${match.trim()}$`;
    }
  );

  // 3.2 孤立的带下标变量（如“把 $b_n$ 分母换成...”中的 b_n）
  text = text.replace(
    /(?<=[\u4e00-\u9fa5（(，,；;。！!？?\s]|^)([a-zA-Z]_[a-zA-Z0-9{}+-]+)(?=[\u4e00-\u9fa5）)，,；;。！!？?\s]|$)/g,
    (match) => {
      if (match.includes('$')) return match;
      return `$${match.trim()}$`;
    }
  );

  // 4. 将所有 $...$ 和普通文本拆分，公式用 KaTeX 渲染，普通文本 escapeHtml
  const segments: Array<{ isMath: boolean; content: string }> = [];
  let i = 0;
  while (i < text.length) {
    if (text[i] === '$') {
      const end = text.indexOf('$', i + 1);
      if (end !== -1) {
        segments.push({ isMath: true, content: text.slice(i + 1, end) });
        i = end + 1;
        continue;
      }
    }
    const nextDollar = text.indexOf('$', i);
    if (nextDollar === -1) {
      segments.push({ isMath: false, content: text.slice(i) });
      break;
    } else {
      segments.push({ isMath: false, content: text.slice(i, nextDollar) });
      i = nextDollar;
    }
  }

  return segments
    .map((seg) => {
      if (seg.isMath) {
        return renderKatex(seg.content, false);
      }
      return escapeHtml(seg.content);
    })
    .join('');
}

