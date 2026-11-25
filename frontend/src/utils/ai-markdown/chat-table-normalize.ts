import { isSafeMultilineMath } from './render-math';

const FENCED_CODE_BLOCK_RE = /(````[\s\S]*?````|```[\s\S]*?```)/g;

/**
 * =========================================================================================
 * 【防回归警示：表格规范化前的代码与数学公式占位保护】
 * 作用：
 *   在对 Markdown 杂乱表格进行管道符与对齐线修复前，必须先将代码块、行内代码、独立数学块
 *   以及合法的单行/跨行单 $ 公式占位保护，杜绝公式内部的绝对值符号（|x|）、集合描述符（{x | ...}）
 *   或代码中的位或（|）被误判为表格列分隔符！
 * =========================================================================================
 */
function protectCodeSegments(content: string): { text: string; segments: string[] } {
  const segments: string[] = [];
  // 1. 先保护所有代码块、行内代码、LaTeX 公式环境及美元符公式（$$...$$、$...$、\[...\]、\(...\)、\begin{...}...\end{...}）
  let text = content.replace(
    /(````[\s\S]*?````|```[\s\S]*?```|`[^`\n]+`|\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\begin\{[a-zA-Z*]+\}[\s\S]*?\\end\{[a-zA-Z*]+\}|(?<!\$)\$(?!\$)[^$\n]+?(?<!\$)\$(?!\$)|\\\([\s\S]*?\\\))/g,
    (segment) => {
      segments.push(segment);
      return `\u0000CODE${segments.length - 1}\u0000`;
    }
  );

  // 2. 保护满足安全特征校验的跨行单 $ 公式
  text = text.replace(/(?<!\$)\$(?!\$)([\s\S]+?)(?<!\$)\$(?!\$)/g, (segment, formula: string) => {
    if (isSafeMultilineMath(formula)) {
      segments.push(segment);
      return `\u0000CODE${segments.length - 1}\u0000`;
    }
    return segment;
  });

  return { text, segments };
}

function restoreCodeSegments(text: string, segments: string[]): string {
  let result = text;
  for (let pass = 0; pass < 24; pass++) {
    const next = result.replace(/\u0000CODE(\d+)\u0000/g, (_match, index) => segments[Number(index)] ?? '');
    if (next === result) break;
    result = next;
  }
  return result;
}

function countTableColumns(row: string): number {
  const parts = row.trim().replace(/^\|/, '').replace(/\|$/, '').split('|');
  return Math.max(parts.length, 1);
}

function isMarkdownTableSep(trimmed: string): boolean {
  if (!trimmed.includes('|')) return false;
  if (!trimmed.includes('-') && !trimmed.includes(':')) return false;
  const body = trimmed.replace(/^\|/, '').replace(/\|$/, '');
  const cells = body.split('|').map((c) => c.trim());
  if (!cells.length) return false;
  return cells.every((c) => /^:?-{1,}:?$/.test(c));
}

function isMarkdownTableRow(trimmed: string): boolean {
  if (!trimmed || trimmed.startsWith('```')) return false;
  if (/^#{1,6}\s/.test(trimmed)) return false;
  // 明显为数学解析、证明步骤、题目散文的内容，绝非表格行
  if (/^(解析|分析|解答|证明|说明|答|注意|题目|第\s*\d+\s*题)[：:【]/.test(trimmed)) return false;

  const isStrictPipe = trimmed.startsWith('|') && trimmed.endsWith('|') && !trimmed.endsWith('\\|') && trimmed.length > 2;

  // 1. 首尾以 | 严格闭合的标准表格行：单元格完全允许包含中文问答/阐述标点（？、！、。等）
  if (isStrictPipe) {
    const cells = splitTableRowCells(trimmed);
    // 至少包含 2 列，杜绝单独两端有绝对值符号的单列数学片段（如 | x |）被误判
    return cells.length >= 2;
  }

  // 2. 仅以 | 开头但未闭合的非标准简写表格行：
  // 必须确保不含散文句末标点，防止普通散文推导行被误判
  if (trimmed.startsWith('|') && trimmed.includes('|', 1)) {
    if (/[。？！]/.test(trimmed)) return false;
    return true;
  }

  return false;
}

/**
 * 模型常把「## 标题 | 列1 | 列2 …」压成一行，需拆成标题 + 表格。
 * 必须确保后半段真正是表格表头（紧随分隔行，或以 | 开头结尾且拥有至少 2 个列单元格），绝不误拆数学公式（如绝对值）与普通副标题。
 */
function splitHeadingFromPipeLine(line: string, nextLine?: string): string {
  const trimmed = line.trim();
  if (!/^#{1,6}/.test(trimmed)) return line;

  const match = trimmed.match(/^([ \t]*#{1,6}[ \t]*[^\n|]+?)(\s*\|[^|\n]+\|.*)$/);
  if (!match) return line;

  const candidateTable = match[2].trim();
  const hasSepNext = nextLine ? isMarkdownTableSep(nextLine.trim()) : false;
  const pipeCount = (candidateTable.match(/\|/g) || []).length;
  // 仅当下行紧跟表格分隔行，或者该部分具有完整表头结构（以 | 开头且结尾，包含至少 2 个列）时才拆分
  const isLikelyTableHeader = candidateTable.startsWith('|') && candidateTable.endsWith('|') && pipeCount >= 3;

  if (hasSepNext || isLikelyTableHeader) {
    const heading = match[1].trim().replace(/^(#{1,6})\s*/, '$1 ');
    return `${heading}\n\n${candidateTable}`;
  }

  return line;
}

function extractLeadingHeading(cells: string[]): { heading?: string; cells: string[] } {
  if (!cells.length) return { cells };
  const first = cells[0]?.trim() ?? '';
  const headingMatch = first.match(/^(#{1,6}\s+.+)$/);
  if (headingMatch) {
    return { heading: headingMatch[1], cells: cells.slice(1) };
  }
  return { cells };
}

function buildTableSeparator(colCount: number): string {
  return `| ${Array.from({ length: colCount }, () => '---').join(' | ')} |`;
}

function splitTableRowCells(line: string): string[] {
  let trimmed = line.trim();
  if (trimmed.startsWith('|')) trimmed = trimmed.slice(1);
  if (trimmed.endsWith('|') && !trimmed.endsWith('\\|')) trimmed = trimmed.slice(0, -1);

  const cells: string[] = [];
  let current = '';
  let inBacktick = false;

  for (let i = 0; i < trimmed.length; i++) {
    const ch = trimmed[i];
    if (ch === '`') {
      inBacktick = !inBacktick;
      current += ch;
    } else if (ch === '\\' && i + 1 < trimmed.length && trimmed[i + 1] === '|') {
      current += '\\|';
      i++;
    } else if (ch === '|' && !inBacktick) {
      cells.push(current);
      current = '';
    } else {
      current += ch;
    }
  }
  cells.push(current);
  return cells;
}

function countOccurrences(str: string, substr: string): number {
  let count = 0;
  let pos = 0;
  while ((pos = str.indexOf(substr, pos)) !== -1) {
    count += 1;
    pos += substr.length;
  }
  return count;
}

function isDelimUnbalanced(str: string, delim: string): boolean {
  return countOccurrences(str, delim) % 2 !== 0;
}

function getMergeScore(left: string, right: string): number {
  let score = 500;
  if (isDelimUnbalanced(left, '**')) score -= 1500;
  if (isDelimUnbalanced(left, '~~') || isDelimUnbalanced(left, '*')) score -= 1300;
  if (countOccurrences(left, '(') - countOccurrences(left, ')') > 0) score -= 1100;
  if (countOccurrences(left, '[') - countOccurrences(left, ']') > 0) score -= 1100;
  if (left.trim() === '' || right.trim() === '') score -= 1000;
  return score;
}

function repairTableRow(line: string, expectedCols: number): string {
  if (expectedCols <= 0) return line;
  const trimmed = line.trim();
  if (isMarkdownTableSep(trimmed)) return buildTableSeparator(expectedCols);

  const cells = splitTableRowCells(trimmed);
  if (cells.length === expectedCols) {
    return `| ${cells.map((c) => c.trim()).join(' | ')} |`;
  }
  if (cells.length < expectedCols) {
    while (cells.length < expectedCols) cells.push('');
    return `| ${cells.map((c) => c.trim()).join(' | ')} |`;
  }

  while (cells.length > expectedCols) {
    let bestIdx = 0;
    let bestScore = Infinity;
    for (let i = 0; i < cells.length - 1; i++) {
      const score = getMergeScore(cells[i], cells[i + 1]);
      if (score < bestScore) {
        bestScore = score;
        bestIdx = i;
      }
    }
    cells.splice(bestIdx, 2, `${cells[bestIdx]}__EMD_PIPE_MERGE__${cells[bestIdx + 1]}`);
  }

  return `| ${cells.map((c) => c.replace(/__EMD_PIPE_MERGE__/g, '\\|').trim()).join(' | ')} |`;
}

function isPlainPipeTableSep(trimmed: string): boolean {
  if (!trimmed.includes('|')) return false;
  const cells = trimmed.split('|').map((c) => c.trim());
  return cells.length >= 2 && cells.every((c) => /^:?-{2,}:?$/.test(c));
}

function isPlainPipeTableRow(trimmed: string): boolean {
  if (/^#{1,6}\s/.test(trimmed)) return false;
  return trimmed.includes('|') && !trimmed.startsWith('|') && !isPlainPipeTableSep(trimmed);
}

function buildMarkdownTableRows(cells: string[], colCount: number, titlePrefix?: string): string {
  const rows: string[] = [];
  for (let r = 0; r < cells.length / colCount; r++) {
    const rowCells = cells.slice(r * colCount, (r + 1) * colCount);
    rows.push(`| ${rowCells.map((c) => c.trim()).join(' | ')} |`);
  }
  const table = [rows[0], buildTableSeparator(colCount), ...rows.slice(1)].join('\n');
  return titlePrefix ? `${titlePrefix.trim()}\n\n${table}` : table;
}

/** 模型常把整张表压成一行 pipe 文本，拆成标准 Markdown 表格 */
function expandCompressedPipeLine(trimmed: string): string | null {
  // 1. 如果包含中英文句号、问号、叹号，属于散文或数学推导段落，绝非表格
  if (/[。！？]/.test(trimmed)) return null;

  // 2. 包含典型的散文推导词汇、解析标记，直接拒识
  if (/(?:解析|分析|证明|因此|可知|所以|因为|由于|可导|连续|极限)/.test(trimmed)) return null;

  // 3. 严格要求以 | 开头且以 | 结尾的行
  if (!trimmed.startsWith('|') || !trimmed.endsWith('|')) return null;

  const pipeCount = (trimmed.match(/\|/g) || []).length;
  if (pipeCount < 3) return null;

  let cells = splitTableRowCells(trimmed);
  if (cells.length < 6) return null;

  // 4. 表格单元格必须短小精炼，若任一单元格内容过长则判定为散文
  if (cells.some((c) => c.trim().length > 35)) return null;

  const { heading, cells: bodyCells } = extractLeadingHeading(cells);
  cells = bodyCells;
  if (cells.length < 6) return heading ? `${heading}\n\n${trimmed}` : null;

  for (let colCount = 8; colCount >= 3; colCount--) {
    if (cells.length % colCount !== 0) continue;
    if (cells.length / colCount < 2) continue;
    return buildMarkdownTableRows(cells, colCount, heading);
  }

  const rest = cells.slice(1);
  for (let colCount = 8; colCount >= 3; colCount--) {
    if (rest.length < colCount * 2) continue;
    if (rest.length % colCount !== 0) continue;
    return buildMarkdownTableRows(rest, colCount, heading ?? cells[0]);
  }

  return null;
}

function normalizePlainPipeTables(content: string): string {
  const lines = content.split('\n');
  const out: string[] = [];
  let i = 0;

  while (i < lines.length) {
    const trimmed = lines[i].trim();
    const nextTrim = i + 1 < lines.length ? lines[i + 1].trim() : '';
    const prevTrim = i > 0 ? lines[i - 1].trim() : '';

    // 关键防御：若当前行处于多行表格环境中（其前一行或后一行也是表格数据行或表格分隔行），
    // 说明它是正常多行表格的其中一行，绝对不可误当成单行压缩表格切碎！
    const isTableRelatedLine = (t: string) =>
      Boolean(t) && (isMarkdownTableRow(t) || isPlainPipeTableRow(t) || isMarkdownTableSep(t) || isPlainPipeTableSep(t));

    const isConnectedToTable = isTableRelatedLine(trimmed) && (isTableRelatedLine(nextTrim) || isTableRelatedLine(prevTrim));

    const expanded = !isConnectedToTable ? expandCompressedPipeLine(trimmed) : null;
    if (expanded) {
      out.push(expanded);
      i += 1;
      continue;
    }

    if (isPlainPipeTableRow(trimmed) && isPlainPipeTableSep(nextTrim)) {
      const colCount = Math.max(countTableColumns(nextTrim), splitTableRowCells(trimmed).length);
      out.push(repairTableRow(trimmed, colCount));
      out.push(buildTableSeparator(colCount));
      i += 2;
      while (i < lines.length && (isPlainPipeTableRow(lines[i].trim()) || isMarkdownTableRow(lines[i].trim()))) {
        const rowTrim = lines[i].trim();
        if (isMarkdownTableSep(rowTrim) || isPlainPipeTableSep(rowTrim)) {
          i += 1;
          continue;
        }
        out.push(repairTableRow(rowTrim, colCount));
        i += 1;
      }
      continue;
    }

    out.push(lines[i]);
    i += 1;
  }

  return out.join('\n');
}

function normalizeMarkdownTables(content: string): string {
  const lines = content.split('\n');
  const out: string[] = [];
  let i = 0;

  while (i < lines.length) {
    const trimmed = lines[i].trim();
    if (!isMarkdownTableRow(trimmed) && !isMarkdownTableSep(trimmed)) {
      out.push(lines[i]);
      i++;
      continue;
    }

    // 收集候选表格行（支持松散表格：跳过表格行之间的单个空行）
    const tableLines: string[] = [];
    while (i < lines.length) {
      const curTrimmed = lines[i].trim();
      if (isMarkdownTableRow(curTrimmed) || isMarkdownTableSep(curTrimmed)) {
        tableLines.push(lines[i]);
        i++;
      } else if (!curTrimmed && i + 1 < lines.length) {
        // 跨空行预查：如果空行的下一行依然是表格行或表格分隔行，则属于松散表格行，跳过该空行继续收集
        const nextTrimmed = lines[i + 1].trim();
        if (isMarkdownTableRow(nextTrimmed) || isMarkdownTableSep(nextTrimmed)) {
          i++;
        } else {
          break;
        }
      } else {
        break;
      }
    }

    // 判断该连续块是否真正构成表格：
    // 1. 包含明确的分隔行（|---|---|）；或者
    // 2. 至少有 2 行数据行，且每行严格以 | 开头并以 | 结尾，绝不包含散文标点
    const hasSep = tableLines.some((l) => isMarkdownTableSep(l.trim()));
    const allStrictPipeRows =
      tableLines.length >= 2 &&
      tableLines.every((l) => {
        const t = l.trim();
        return t.startsWith('|') && t.endsWith('|') && !/[。？！]/.test(t) && !/^(解析|分析|解答)[：:]/.test(t);
      });
    const isRealTable = hasSep || allStrictPipeRows;

    if (!isRealTable) {
      // 绝非 Markdown 表格（如带有数学绝对值 |x| 的证明步骤），按原样输出
      for (const line of tableLines) {
        out.push(line);
      }
      continue;
    }

    // 对真正的表格块进行规范化
    // 关键修正：列数不能仅由 firstSep 决定！大模型经常写出 3 列表头但分隔行只有 2 列（|---|---|）的情况，
    // 若以 2 列表头修复，会导致表头第 1、2 列被强行合并。因此必须取数据行与分隔行中列数的最大值！
    let sawSeparator = false;
    const firstSep = tableLines.find((l) => isMarkdownTableSep(l.trim()));
    const dataRows = tableLines.filter((l) => !isMarkdownTableSep(l.trim()));
    const maxDataCols = dataRows.length > 0 ? Math.max(...dataRows.map((l) => countTableColumns(l.trim()))) : 0;
    const sepCols = firstSep ? countTableColumns(firstSep.trim()) : 0;
    let tableColCount = Math.max(maxDataCols, sepCols, 2);

    // 确保表格与前文段落之间保留空行，防止 CommonMark 规范将其当成前文普通段落的延续
    if (out.length > 0 && out[out.length - 1].trim() !== '') {
      out.push('');
    }

    let processedRowIndex = 0;
    for (const rawLine of tableLines) {
      const lineTrim = rawLine.trim();
      const isSep = isMarkdownTableSep(lineTrim);

      if (isSep) {
        if (sawSeparator) continue;
        sawSeparator = true;
        out.push(buildTableSeparator(tableColCount));
        processedRowIndex++;
        continue;
      }

      // 如果到了第 2 行（索引 1）还没有分隔行，自动插入分隔行
      if (processedRowIndex === 1 && !sawSeparator) {
        out.push(buildTableSeparator(tableColCount));
        sawSeparator = true;
        processedRowIndex++;
      }

      out.push(repairTableRow(lineTrim, tableColCount));
      processedRowIndex++;
    }

    // 表格结束后追加空行
    out.push('');
  }

  return out.join('\n');
}

/** 聊天 Markdown 表格容错：pipe 单行表、缺分隔符表、标准 GFM 表 */
export function normalizeChatTables(content: string): string {
  if (!content?.trim()) return content;
  const withoutCodeBlocks = content.split(FENCED_CODE_BLOCK_RE).map((segment, index) => {
    if (index % 2 === 1) return segment;

    // 先保护代码块、数学公式（$$...$$、$...$、\[...\]、\(...\)）
    const { text: protectedSegment, segments } = protectCodeSegments(segment);
    const lines = protectedSegment.split('\n');
    const withHeadingSplit = lines
      .map((line, idx) => splitHeadingFromPipeLine(line, lines[idx + 1]))
      .join('\n');

    let text = normalizePlainPipeTables(withHeadingSplit);
    text = normalizeMarkdownTables(text);
    let restored = restoreCodeSegments(text, segments);

    // 若非代码段末尾是以 | 结尾的表格行，确保其末尾保留空行，防止在还原代码块时与 ``` 紧贴
    if (restored.trimEnd().endsWith('|') && !restored.endsWith('\n\n')) {
      restored = restored.trimEnd() + '\n\n';
    }
    return restored;
  });
  return withoutCodeBlocks.join('');
}
