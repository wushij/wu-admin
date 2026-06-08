import { describe, expect, it } from 'vitest'
import { getErrorMessage } from '@/utils/axiosError'

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
})
