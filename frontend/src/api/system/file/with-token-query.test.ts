import { beforeEach, describe, expect, it } from 'vitest'
import { withTokenQuery } from '@/api/system/file/index'

describe('withTokenQuery', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns empty string for empty url', () => {
    expect(withTokenQuery('')).toBe('')
  })

  it('returns url unchanged when no token', () => {
    expect(withTokenQuery('/api/system/file/preview/1')).toBe('/api/system/file/preview/1')
  })

  it('appends Authorization query for /api urls when token exists', () => {
    localStorage.setItem('token', 'abc 123')
    expect(withTokenQuery('/api/system/file/preview/1')).toBe(
      '/api/system/file/preview/1?Authorization=abc%20123',
    )
  })

  it('uses & when url already has query string', () => {
    localStorage.setItem('token', 't')
    expect(withTokenQuery('/api/x?w=1')).toBe('/api/x?w=1&Authorization=t')
  })

  it('does not modify non-api urls', () => {
    localStorage.setItem('token', 't')
    expect(withTokenQuery('https://cdn.example.com/a.png')).toBe('https://cdn.example.com/a.png')
  })
})
