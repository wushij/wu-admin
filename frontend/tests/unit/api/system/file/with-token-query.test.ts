import { describe, expect, it } from 'vitest'
import { withTokenQuery } from '@/api/system/file/index'

describe('withTokenQuery', () => {
  it('returns empty string for empty url', () => {
    expect(withTokenQuery('')).toBe('')
  })

  it('returns url unchanged (Cookie 鉴权)', () => {
    expect(withTokenQuery('/api/system/file/preview/1')).toBe('/api/system/file/preview/1')
  })

  it('preserves existing query string', () => {
    expect(withTokenQuery('/api/x?w=1')).toBe('/api/x?w=1')
  })

  it('does not modify non-api urls', () => {
    expect(withTokenQuery('https://cdn.example.com/a.png')).toBe('https://cdn.example.com/a.png')
  })
})
