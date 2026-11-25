// @vitest-environment happy-dom
import { describe, expect, it } from 'vitest'
import { renderChatMarkdown } from '@/utils/ai-markdown/chat-markdown'

describe('AI Markdown 数学公式渲染与纯文本兜底（对齐 EduMind）', () => {
  it('用户截图中的纯文本微积分与高数公式应完整渲染为 KaTeX 且无报错', () => {
    const sample = [
      '该题推理的典型错误是：用 "x ≠ 0 时 sin x/x ≠ 1" 这一点值事实，去否定极限值。',
      '',
      '极限只与 x 趋于该点时的变化趋势有关，与该点及个别点的函数取值无关。例如 sin x/x 在 x ≠ 0 处恒不等于 1，但 lim(x → 0) sin x/x 仍等于 1。同理，求 lim(x → 0)(sin x - x)/x³ 也不能因分子在 x = 0 处取值为 0 就断定极限为 0，正确结果为 -1/6。',
      '',
      '### 第 9 题',
      '',
      "f(x) = x + 1/x (x > 0) , f'(x) = 1 - 1/x² = (x² - 1)/x²。",
      '',
      "- x > 0 时, f'(x) < 0 当且仅当 0 < x < 1, 故单调递减区间为 (0, 1);",
      '- x = 1 处导数由负变正, 为极小值点, f(1) = 1 + 1 = 2。',
      '',
      '### 第 10 题',
      '',
      'lim(x → 0) (e^(x²) - 1)/(1 - cos x):',
      '',
      '- e^(x²) - 1 ~ x²;',
      '- 1 - cos x ~ x²/2;',
      '',
      '故原式 = lim x²/(x²/2) = 2。',
      '',
      '说明：卷面要求填 "最简数值"，故按分子为 e^(x²) - 1 还原；若原式分子确为 e^x - 1，则该极限趋于 +∞，不符合填空要求。',
    ].join('\n')

    const html = renderChatMarkdown(sample)
    const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html')
    const katexEls = doc.querySelectorAll('.katex')
    const errorEls = doc.querySelectorAll('.katex-error')

    expect(errorEls).toHaveLength(0)
    // 关键公式均应被 KaTeX 渲染
    expect(katexEls.length).toBeGreaterThanOrEqual(14)

    // 剥离 KaTeX 渲染节点后，剩余普通散文里不应再有未转换的 Unicode 公式碎片
    const clone = doc.body.firstElementChild!.cloneNode(true) as HTMLElement
    clone.querySelectorAll('.katex').forEach((el) => el.remove())
    const remainingProse = clone.textContent || ''
    expect(remainingProse).not.toContain('x²')
    expect(remainingProse).not.toContain('x³')
    expect(remainingProse).not.toContain('→')
    expect(remainingProse).not.toContain('≠')
    expect(remainingProse).not.toContain('+∞')
    expect(remainingProse).not.toContain('e^(')
    expect(remainingProse).not.toContain('lim(')
    expect(remainingProse).not.toContain('x > 0')

    // 极限下标、正体三角函数与分式结构应正确生成
    expect(html).toContain('lim')
    expect(html).toContain('sin')
    expect(html).toContain('cos')
    expect(html).toContain('mfrac')
    expect(doc.querySelectorAll('h3')).toHaveLength(2)
    expect(doc.querySelectorAll('ul li')).toHaveLength(4)
  })

  it('标准 LaTeX 行内/块级公式及圆括号指数应完整渲染', () => {
    const sample = [
      '设 $f(x) = e^{x^2} - 1$，则：',
      '',
      '$$',
      '\\lim_{x \\to 0} \\frac{e^{x^2} - 1}{1 - \\cos x} = \\lim_{x \\to 0} \\frac{x^2}{x^2/2} = 2',
      '$$',
      '',
      '最终结论：',
      '',
      '$$\\boxed{2}$$',
    ].join('\n')

    const html = renderChatMarkdown(sample)
    const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html')

    expect(doc.querySelectorAll('.katex-error')).toHaveLength(0)
    expect(doc.querySelectorAll('.katex-block-wrapper')).toHaveLength(2)
    expect(doc.querySelectorAll('.katex').length).toBeGreaterThanOrEqual(3)
  })

  it('不应误伤代码块、文件路径、环境变量、货币金额与 Markdown 表格', () => {
    const sample = [
      '路径 `C:\\Users\\admin\\app.ts` 与变量 `$HOME`、`$emit` 保持原样。',
      '',
      '商品 A 售价 $5，优惠后 $10 左右。',
      '',
      '| 变量 | 条件 | 链接 |',
      '| --- | --- | --- |',
      '| x | x > 0 | [查看](https://example.com?a=1&b=2) |',
      '',
      '```python',
      'x = 1 / 2',
      'if x > 0:',
      '    print("e^(x^2)")',
      '```',
    ].join('\n')

    const html = renderChatMarkdown(sample)
    const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html')

    expect(doc.querySelectorAll('.katex-error')).toHaveLength(0)
    // 代码块内不被公式渲染破坏
    const preText = doc.querySelector('pre code')?.textContent || ''
    expect(preText).toContain('x = 1 / 2')
    expect(preText).toContain('e^(x^2)')
    // 货币金额不被解析为公式
    expect(html).toContain('$5')
    expect(html).toContain('$10')
    // Markdown 表格结构完整
    expect(doc.querySelectorAll('table tr')).toHaveLength(2)
    // 链接完整保留查询参数
    expect(doc.querySelector('a')?.getAttribute('href')).toBe('https://example.com?a=1&b=2')
  })
})
