import { beforeEach, describe, expect, it, vi } from 'vitest'

const mockGet = vi.fn()
const mockPost = vi.fn()

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      get: mockGet,
      post: mockPost,
      put: vi.fn(),
      delete: vi.fn(),
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
    })),
  },
}))

vi.mock('@/router', () => ({
  default: { push: vi.fn() },
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn() },
}))

describe('request helpers', () => {
  beforeEach(async () => {
    vi.resetModules()
    vi.clearAllMocks()
    mockGet.mockResolvedValue({ code: 200, data: { ok: true } })
    mockPost.mockResolvedValue({ code: 200, data: null })
  })

  it('get forwards url, params and config to axios instance', async () => {
    const { get } = await import('@/utils/request')
    await get('/demo', { pageNo: 1 }, { timeout: 1000 })
    expect(mockGet).toHaveBeenCalledWith('/demo', {
      timeout: 1000,
      params: { pageNo: 1 },
    })
  })

  it('post forwards body and config', async () => {
    const { post } = await import('@/utils/request')
    await post('/demo', { name: 'a' }, { headers: { 'X-Test': '1' } })
    expect(mockPost).toHaveBeenCalledWith(
      '/demo',
      { name: 'a' },
      { headers: { 'X-Test': '1' } },
    )
  })
})
