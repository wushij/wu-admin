import { describe, expect, it } from 'vitest'
import { isApiSuccessCode } from '@/utils/api-response'

describe('isApiSuccessCode', () => {
  it('accepts legacy 0 and standard 200', () => {
    expect(isApiSuccessCode(0)).toBe(true)
    expect(isApiSuccessCode(200)).toBe(true)
  })

  it('rejects other codes', () => {
    expect(isApiSuccessCode(401)).toBe(false)
    expect(isApiSuccessCode(undefined)).toBe(false)
    expect(isApiSuccessCode('200')).toBe(false)
  })
})
