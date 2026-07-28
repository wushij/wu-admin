import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'

const mockPush = vi.fn()
const mockGet = vi.fn()
const mockPost = vi.fn()
const mockLogout = vi.fn()

type RequestInterceptor = (config: InternalAxiosRequestConfig) => InternalAxiosRequestConfig | Promise<InternalAxiosRequestConfig>
type ResponseInterceptor = (response: AxiosResponse) => unknown
type ResponseErrorInterceptor = (error: unknown) => Promise<never>

let requestInterceptor: RequestInterceptor
let responseSuccess: ResponseInterceptor
let responseError: ResponseErrorInterceptor

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      get: mockGet,
      post: mockPost,
      put: vi.fn(),
      delete: vi.fn(),
      interceptors: {
        request: {
          use: vi.fn((fn: RequestInterceptor) => {
            requestInterceptor = fn
          }),
        },
        response: {
          use: vi.fn((onFulfilled: ResponseInterceptor, onRejected: ResponseErrorInterceptor) => {
            responseSuccess = onFulfilled
            responseError = onRejected
          }),
        },
      },
    })),
  },
}))

vi.mock('@/router', () => ({
  default: { push: mockPush },
}))

vi.mock('@/store/user', () => ({
  useUserStore: () => ({
    logout: mockLogout,
  }),
}))

const mockElMessageError = vi.fn()
const mockElMessageWarning = vi.fn()
vi.mock('element-plus', () => ({
  ElMessage: {
    error: mockElMessageError,
    warning: mockElMessageWarning,
  },
}))

async function loadRequestModule() {
  return import('@/utils/request')
}

describe('request', () => {
  beforeEach(async () => {
    vi.resetModules()
    vi.clearAllMocks()
    mockGet.mockResolvedValue({ code: 200, data: { ok: true } })
    mockPost.mockResolvedValue({ code: 200, data: null })
    await loadRequestModule()
  })

  describe('get / post helpers', () => {
    it('get forwards url, params and config to axios instance', async () => {
      const { get } = await loadRequestModule()
      await get('/demo', { pageNo: 1 }, { timeout: 1000 })
      expect(mockGet).toHaveBeenCalledWith('/demo', {
        timeout: 1000,
        params: { pageNo: 1 },
      })
    })

    it('post forwards body and config', async () => {
      const { post } = await loadRequestModule()
      await post('/demo', { name: 'a' }, { headers: { 'X-Test': '1' } })
      expect(mockPost).toHaveBeenCalledWith(
        '/demo',
        { name: 'a' },
        { headers: { 'X-Test': '1' } },
      )
    })
  })

  describe('request interceptor', () => {
    it('passes config through without Authorization header', async () => {
      const config = { url: '/system/user/list', headers: {} } as InternalAxiosRequestConfig
      const result = await requestInterceptor(config)
      expect(result.headers!['Authorization']).toBeUndefined()
    })
  })

  describe('response interceptor', () => {
    it('returns body when code is success', () => {
      const body = { code: 200, data: { id: 1 } }
      const result = responseSuccess({ data: body, config: { url: '/x' } } as AxiosResponse)
      expect(result).toEqual(body)
    })

    it('redirects to login on 401 for protected routes', async () => {
      await expect(
        responseSuccess({
          data: { code: 401, message: '未授权' },
          config: { url: '/system/user/list' },
        } as AxiosResponse),
      ).rejects.toThrow('未授权')
      expect(mockLogout).toHaveBeenCalled()
      expect(mockPush).toHaveBeenCalledWith('/login')
      expect(mockElMessageError).toHaveBeenCalledWith('登录已过期，请重新登录')
    })

    it('shows auth error without redirect on public login 401', async () => {
      await expect(
        responseSuccess({
          data: { code: 401, message: '账号或密码错误' },
          config: { url: '/auth/login' },
        } as AxiosResponse),
      ).rejects.toThrow('账号或密码错误')
      expect(mockPush).not.toHaveBeenCalled()
      expect(mockElMessageError).toHaveBeenCalledWith('账号或密码错误')
    })

    it('rejects 403 and shows permission toast', async () => {
      await expect(
        responseSuccess({
          data: { code: 403, message: '权限不足' },
          config: { url: '/system/role/list' },
        } as AxiosResponse),
      ).rejects.toThrow('权限不足')
      expect(mockElMessageError).toHaveBeenCalledWith('权限不足')
    })

    it('respects silent403 and skips toast', async () => {
      await expect(
        responseSuccess({
          data: { code: 403, message: '权限不足' },
          config: { url: '/system/role/list', silent403: true },
        } as AxiosResponse),
      ).rejects.toThrow('权限不足')
      expect(mockElMessageError).not.toHaveBeenCalled()
    })
  })

  describe('response error interceptor', () => {
    it('shows network error when no response', async () => {
      await expect(responseError({ message: 'Network Error' })).rejects.toBeDefined()
      expect(mockElMessageError).toHaveBeenCalledWith('网络异常，请检查网络连接')
    })

    it('handles HTTP 401 on protected route', async () => {
      await expect(
        responseError({
          response: { status: 401, data: { msg: '未登录' } },
          config: { url: '/system/user/list' },
        }),
      ).rejects.toBeDefined()
      expect(mockLogout).toHaveBeenCalled()
      expect(mockPush).toHaveBeenCalledWith('/login')
    })
  })
})
