import { describe, expect, it } from 'vitest'
import {
  cleanThinkingText,
  formatThinkingDuration,
  normalizeToolLabel,
} from '@/utils/ai/thinking-steps'

describe('thinking-steps 清洗与格式化', () => {
  it('清理推理包裹标签并压缩多余空行，完整保留正文与数学推导', () => {
    const raw =
      '<think>\n\n第1题：等价无穷小代换。\n\n\n第2题：$\\lim_{x\\to 0}\\frac{\\ln(1+3x)}{\\sin 2x} = \\frac{3}{2}$。\n</think>'
    const cleaned = cleanThinkingText(raw)
    expect(cleaned).not.toContain('<think>')
    expect(cleaned).not.toContain('</think>')
    expect(cleaned).toContain('第1题：等价无穷小代换。')
    expect(cleaned).toContain('\\frac{3}{2}')
    expect(cleaned).not.toContain('\n\n\n')
  })

  it('规范工具调用提示并剔除前导非文字符号', () => {
    expect(normalizeToolLabel('··· 正在查询在线用户数')).toBe('正在查询在线用户数')
    expect(normalizeToolLabel('')).toBe('正在查询系统数据')
  })

  it('格式化思考耗时为中文秒/分秒表达', () => {
    expect(formatThinkingDuration(2400)).toBe('2 秒')
    expect(formatThinkingDuration(65000)).toBe('1 分 05 秒')
  })
})
