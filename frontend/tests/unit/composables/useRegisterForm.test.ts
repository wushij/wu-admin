import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { nextTick } from 'vue'
import { setActivePinia, createPinia } from 'pinia'

const { mockWarning, mockRegister } = vi.hoisted(() => ({
  mockWarning: vi.fn(),
  mockRegister: vi.fn(),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: {
      warning: mockWarning,
      success: vi.fn(),
      error: vi.fn(),
    },
  }
})

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

vi.mock('@/api/system/auth', () => ({
  getConfig: vi.fn().mockResolvedValue({ data: {} }),
  getCaptcha: vi.fn().mockResolvedValue({ data: {} }),
  register: mockRegister,
}))

import { useRegisterForm } from '@/views/register/composables/useRegisterForm'

describe('useRegisterForm 用户协议与胶囊轻提示校验', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
    mockWarning.mockClear()
    mockRegister.mockClear()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('未勾选协议点击注册后触发胶囊警告并同步协议状态，勾选后自动清除', async () => {
    const { agreeTerms, agreeRowAlert, handleRegister } = useRegisterForm()

    expect(agreeTerms.value).toBe(false)
    expect(agreeRowAlert.value).toBe(false)

    await handleRegister()
    expect(agreeRowAlert.value).toBe(true)
    expect(mockWarning).toHaveBeenCalledWith('请先阅读并同意《用户协议》和《隐私政策》')

    vi.advanceTimersByTime(1000)
    await nextTick()
    expect(agreeRowAlert.value).toBe(true)

    agreeTerms.value = true
    await nextTick()
    expect(agreeRowAlert.value).toBe(false)
  })

  it('勾选协议后按顺序执行胶囊警告校验，依次提示用户名、密码与图形验证码且不发起提交', async () => {
    const { agreeTerms, formData, handleRegister } = useRegisterForm()
    agreeTerms.value = true

    await handleRegister()
    expect(mockWarning).toHaveBeenLastCalledWith('请输入用户名')

    formData.username = 'ab'
    await handleRegister()
    expect(mockWarning).toHaveBeenLastCalledWith('用户名只能包含字母、数字、下划线，长度4-12位')

    formData.username = 'valid_user'
    await handleRegister()
    expect(mockWarning).toHaveBeenLastCalledWith('请输入密码')

    formData.password = '123456'
    await handleRegister()
    expect(mockWarning).toHaveBeenLastCalledWith('请再次输入密码确认')

    formData.confirmPassword = '654321'
    await handleRegister()
    expect(mockWarning).toHaveBeenLastCalledWith('两次输入的密码不一致')

    formData.confirmPassword = '123456'
    await handleRegister()
    expect(mockWarning).toHaveBeenLastCalledWith('请完成图形验证码')
    expect(mockRegister).not.toHaveBeenCalled()
  })
})
