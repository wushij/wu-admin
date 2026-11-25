/**
 * 深度思考辅助工具
 *
 * 负责思维链文本清洗、工具轨迹文案规整与思考用时格式化；
 * 不再对模型的自然思考正文做机械分步切割，避免产生大量碎片伪步骤。
 */

export interface AiToolStepInput {
  id?: string
  message?: string
}

/** 清洗思维链文本中的冗余空行与标签包裹 */
export function cleanThinkingText(raw: string): string {
  return (raw || '')
    .replace(/\r\n/g, '\n')
    .replace(/<\/?(?:think|thought|reasoning)>/gi, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

/** 规整工具状态提示，剔除前导装饰符号 */
export function normalizeToolLabel(message?: string): string {
  const cleaned = (message || '')
    .replace(/^[^\u4e00-\u9fa5A-Za-z0-9]+/, '')
    .trim()
  return cleaned || '正在查询系统数据'
}

/** 思考用时格式化：12 秒 / 1 分 05 秒 */
export function formatThinkingDuration(durationMs: number): string {
  const totalSeconds = Math.max(0, Math.round(durationMs / 1000))
  if (totalSeconds < 60) return `${totalSeconds} 秒`
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes} 分 ${String(seconds).padStart(2, '0')} 秒`
}
