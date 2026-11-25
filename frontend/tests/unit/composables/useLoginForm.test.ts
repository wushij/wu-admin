import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const {
  mockSuccess,
  mockError,
  mockWarning,
  mockRouterPush,
  mockCurrentRoute,
  mockLoginAction,
  mockGetUserInfo,
  mockLogout,
} = vi.hoisted(() => ({
  mockSuccess: vi.fn(),
  mockError: vi.fn(),
  mockWarning: vi.fn(),
  mockRouterPush: vi.fn(),
  mockCurrentRoute: { value: { path: '/login' } },
  mockLoginAction: vi.fn(),
  mockGetUserInfo: vi.fn(),
  mockLogout: vi.fn(),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: {
      warning: mockWarning,
      success: mockSuccess,
      error: mockError,
    },
  }
})

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockRouterPush,
    currentRoute: mockCurrentRoute,
  }),
}))

vi.mock('@/store/user', () => ({
  useUserStore: () => ({
    loginAction: mockLoginAction,
    getUserInfo: mockGetUserInfo,
    logout: mockLogout,
  }),
}))

vi.mock('@/api/system/auth', () => ({
  getConfig: vi.fn().mockResolvedValue({ data: { login: { captchaEnabled: false } } }),
  getCaptcha: vi.fn().mockResolvedValue({ data: {} }),
  sendSmsCode: vi.fn(),
  sendEmailCode: vi.fn(),
}))

import { markErrorToastShown } from '@/utils/axiosError'
import { useLoginForm } from '@/views/login/composables/useLoginForm'

describe('useLoginForm 登录提交按钮状态机', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
    mockCurrentRoute.value = { path: '/login' }
    mockSuccess.mockClear()
    mockError.mockClear()
    mockWarning.mockClear()
    mockRouterPush.mockReset().mockResolvedValue(undefined)
    mockLoginAction.mockReset()
    mockGetUserInfo.mockReset()
    mockLogout.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('loginAction 成功但 getUserInfo 返回 500 时，绝不触发打勾成功态也不弹登录成功', async () => {
    mockLoginAction.mockResolvedValue({ code: 200, data: { userId: 1 } })
    const err500 = new Error('Request failed with status code 500')
    markErrorToastShown(err500)
    mockGetUserInfo.mockRejectedValue(err500)

    const { formData, captchaEnabled, loginSuccess, loading, handleLogin } = useLoginForm()
    captchaEnabled.value = false
    formData.username = 'admin'
    formData.password = 'Admin@123'

    await handleLogin()

    expect(mockLoginAction).toHaveBeenCalledTimes(1)
    expect(mockGetUserInfo).toHaveBeenCalledTimes(1)
    expect(mockLogout).toHaveBeenCalledTimes(1)
    expect(loginSuccess.value).toBe(false)
    expect(loading.value).toBe(false)
    expect(mockSuccess).not.toHaveBeenCalled()
    // 拦截器已展示过 500 提示时不重复弹窗
    expect(mockError).not.toHaveBeenCalled()
  })

  it('loginAction 返回 500 异常时保持非成功态且恢复按钮可点击', async () => {
    mockLoginAction.mockRejectedValue(new Error('Request failed with status code 500'))

    const { formData, captchaEnabled, loginSuccess, loading, handleLogin } = useLoginForm()
    captchaEnabled.value = false
    formData.username = 'admin'
    formData.password = 'Admin@123'

    await handleLogin()

    expect(loginSuccess.value).toBe(false)
    expect(loading.value).toBe(false)
    expect(mockSuccess).not.toHaveBeenCalled()
    expect(mockError).toHaveBeenCalledWith('登录失败，请检查服务或网络连接')
  })

  it('loginAction 与 getUserInfo 均成功后才置为打勾成功态，若被守卫留在登录页则自动复位', async () => {
    mockLoginAction.mockResolvedValue({ code: 200, data: { userId: 1 } })
    mockGetUserInfo.mockResolvedValue({ code: 200, data: { userId: 1, menus: [] } })

    const { formData, captchaEnabled, loginSuccess, loading, handleLogin } = useLoginForm()
    captchaEnabled.value = false
    formData.username = 'admin'
    formData.password = 'Admin@123'

    await handleLogin()

    expect(loginSuccess.value).toBe(true)
    expect(loading.value).toBe(false)
    expect(mockSuccess).toHaveBeenCalledWith({
      message: '登录成功',
      customClass: 'auth-glass-message',
    })

    // 模拟 680ms 后跳转成功离开登录页：保持成功态
    mockRouterPush.mockImplementationOnce(async () => {
      mockCurrentRoute.value = { path: '/dashboard' }
    })
    await vi.advanceTimersByTimeAsync(700)
    expect(mockRouterPush).toHaveBeenCalledWith('/')
    expect(loginSuccess.value).toBe(true)
  })
})
