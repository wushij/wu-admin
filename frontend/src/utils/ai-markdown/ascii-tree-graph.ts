/** 检测是否为「├─ / └─ / │」字符画知识树（非真实源码） */
export function looksLikeAsciiKnowledgeTree(code: string): boolean {
  const t = code.trim();
  // 1. 严格排除框线表格/板书框架（包含右上角 ┐、中右 ┤、右下角 ┘ 或较长的连续横框线 ┌──/├──/└──）
  if (/[┐┤┘]/.test(t) || /[┌├└][─\-—]{4,}/.test(t)) {
    return false;
  }
  if (!/[├└]─/.test(t) && !/[├└]/.test(t)) return false;
  const branchCount = (t.match(/[├└]/g) || []).length;
  if (branchCount < 2) return false;
  if (/^(?:public\s+class|import\s+|#include|package\s+|def\s+|function\s+|const\s+\w+\s*=)/m.test(t)) {
    return false;
  }
  if (/;\s*$|;\s*\n/m.test(t) && !/：|←/.test(t)) return false;
  return true;
}

/** 将粘连在一行的字符树拆成多行 */
export function expandAsciiTreeToMultiline(text: string): string {
  let s = text.replace(/\r\n/g, '\n').trim();
  if (!/[├└│|]/.test(s)) return text;

  // 1. 拆分粘连在正文末尾的分支标记（如「...验证编译产物│├──三、...」或「...产物├──三、...」）
  // 必须确保前面有实际内容字符（非换行符、非纯缩进），避免拆断本就处于行首的多层缩进（如「  │   ├── 子节点」）
  let prev = '';
  while (prev !== s) {
    prev = s;
    s = s.replace(
      /([^\n\r│|\s][^\n\r]*?)[ \t]*([│|]*[├└\+][─\-—\s]*)(?=[^\n\r─\-—\s])/gu,
      (match, content, branch) => {
        // 如果 branch 只是一个单独的加号 +（且不带横线），避免误伤数学公式加法
        if (branch.startsWith('+') && !branch.includes('-') && !branch.includes('─')) {
          return match;
        }
        return `${content}\n${branch}`;
      }
    );
  }

  // 2. 顶层 ├─ / └─ 若与上一行内容紧贴且未换行，补充换行（保留本行行首的多层缩进）
  s = s.replace(/([^\n\r])([├└][─\-—]?)/g, (full, before: string, branch: string, offset: number, whole: string) => {
    const head = whole.slice(0, offset + before.length);
    const lastNewline = head.lastIndexOf('\n');
    const currentLineHead = lastNewline >= 0 ? head.slice(lastNewline + 1) : head;
    if (/^[ \t│|]*$/.test(currentLineHead)) return full;
    return `${before}\n${branch}`;
  });

  return s;
}

function escapeMermaidLabel(label: string): string {
  return label
    .replace(/\\/g, '\\\\')
    .replace(/"/g, '\\"')
    .replace(/\n/g, ' ')
    .trim()
    .slice(0, 44);
}

interface ParsedBranch {
  label: string;
  indent: number;
}

function parseBranchLine(line: string): ParsedBranch | null {
  // 匹配前缀缩进（空格、制表符、竖线）以及分支标志（├、└、+、|）和横线
  // 如 "│   └── com/" 或 "    ├── src/" 或 "├─── EnvDiagnostics.java"
  const m = line.match(/^([\s│\|]*)([├└\+\|][─\-\s]*)(.*)$/);
  if (!m) return null;

  const prefix = m[1];
  let rawLabel = m[3] || '';

  // 彻底剔除标签开头的残留连接符（如 ─、-、—、-- 以及多余空格）与末尾残留竖线（Unicode │ 与 ASCII |）
  rawLabel = rawLabel
    .replace(/^[─\-—\s]+/, '')
    .replace(/[\s│|]+$/g, '')
    .trim();
  if (!rawLabel) return null;

  // 计算视觉缩进：制表符转4空格，竖线与普通字符等宽
  const visualIndent = prefix.replace(/\t/g, '    ').length;

  return {
    label: rawLabel,
    indent: visualIndent
  };
}

/** 将字符树转为 Mermaid flowchart；失败时返回 null */
export function asciiTreeToMermaid(code: string): string | null {
  const expanded = expandAsciiTreeToMultiline(code);
  const rawLines = expanded.split('\n').map((l) => l.trimEnd()).filter((l) => l.trim());

  if (rawLines.length < 2) return null;

  let rootLabel = '';
  let bodyLines = rawLines;
  if (!/^[│\s]*[├└\+\|]/.test(rawLines[0])) {
    rootLabel = rawLines[0].trim();
    bodyLines = rawLines.slice(1);
  }

  const nodeDecls: string[] = [];
  const edgeDecls: string[] = [];
  const stack: { id: string; indent: number }[] = [];
  let counter = 0;

  if (rootLabel) {
    const rootId = `T${counter++}`;
    nodeDecls.push(`${rootId}["${escapeMermaidLabel(rootLabel)}"]`);
    stack.push({ id: rootId, indent: -1 });
  }

  for (const line of bodyLines) {
    const branch = parseBranchLine(line);
    if (!branch) continue;

    const id = `T${counter++}`;
    nodeDecls.push(`${id}["${escapeMermaidLabel(branch.label)}"]`);

    // 如果当前缩进小于等于栈顶节点的缩进，说明当前节点是栈顶节点的兄弟或更浅层节点，依次退栈
    while (stack.length > 1 && stack[stack.length - 1].indent >= branch.indent) {
      stack.pop();
    }

    const parent = stack[stack.length - 1];
    if (parent) {
      edgeDecls.push(`${parent.id} --> ${id}`);
    }

    stack.push({ id, indent: branch.indent });
  }

  if (nodeDecls.length < 2 || edgeDecls.length < 1) return null;

  return ['flowchart TD', ...nodeDecls, ...edgeDecls].join('\n');
}
