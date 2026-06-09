import { describe, expect, it } from 'vitest'
import { ERROR_TOAST_SHOWN, getErrorMessage, isErrorToastShown, markErrorToastShown } from '@/utils/axiosError'

describe('getErrorMessage', () => {
  it('extracts msg from axios response body', () => {
    const error = {
      response: { data: { msg: '用户名已存在' } },
    }
    expect(getErrorMessage(error)).toBe('用户名已存在')
  })

  it('falls back to message field in response body', () => {
    const error = {
      response: { data: { message: 'bad request' } },
    }
    expect(getErrorMessage(error)).toBe('bad request')
  })

  it('reads plain Error message', () => {
    expect(getErrorMessage(new Error('network down'))).toBe('network down')
  })

  it('returns undefined for non-object input', () => {
    expect(getErrorMessage(null)).toBeUndefined()
    expect(getErrorMessage('oops')).toBeUndefined()
  })

  it('marks and detects toast shown flag', () => {
    const error = new Error('failed')
    expect(isErrorToastShown(error)).toBe(false)
    markErrorToastShown(error)
    expect(isErrorToastShown(error)).toBe(true)
    expect((error as unknown as Record<string, unknown>)[ERROR_TOAST_SHOWN]).toBe(true)
  })
})
