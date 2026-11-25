import { describe, it, expect } from 'vitest'
import { normalizeFileExt, resolveFileBadgeText, resolveFileDocKind } from '@/utils/fileTypeIcon'

describe('fileTypeIcon 文档类型与后缀解析', () => {
  it('能根据文件名识别 PDF / Word / Excel / PPT / Markdown 类型', () => {
    expect(resolveFileDocKind('高等数学（上）· 随堂达标.pdf', 'application/pdf')).toBe('pdf')
    expect(resolveFileBadgeText('pdf')).toBe('PDF')

    expect(resolveFileDocKind('需求说明书.DOCX')).toBe('word')
    expect(resolveFileBadgeText('word')).toBe('W')

    expect(resolveFileDocKind('统计报表.xlsx')).toBe('excel')
    expect(resolveFileBadgeText('excel')).toBe('X')

    expect(resolveFileDocKind('汇报材料.pptx')).toBe('ppt')
    expect(resolveFileBadgeText('ppt')).toBe('P')

    expect(resolveFileDocKind(undefined, 'md')).toBe('markdown')
    expect(resolveFileBadgeText('markdown')).toBe('MD')
  })

  it('无扩展名的文件名可回退读取 fileSuffix 或 MIME 类型', () => {
    expect(resolveFileDocKind('高等数学（上）· 随堂达标', '.pdf')).toBe('pdf')
    expect(resolveFileDocKind('无后缀文档', 'application/pdf')).toBe('pdf')
    expect(resolveFileDocKind('合同文本', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')).toBe('word')
    expect(normalizeFileExt('readme.md?token=abc#top')).toBe('md')
  })
})
