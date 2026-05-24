import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from '@/store/user'
import { login, getInfo, logout as logoutApi } from '@/api/system/auth'

vi.mock('@/api/system/auth', () => ({
  login: vi.fn(),
  getInfo: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('@/store/message', () => ({
  useMessageStore: () => ({
    destroyWebSocket: vi.fn(),
  }),
}))

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('loginAction stores token and user info', async () => {
    vi.mocked(login).mockResolvedValue({
      code: 200,
      data: { token: 'tok', userId: 1, username: 'admin', nickname: '管理员' },
    })
    const store = useUserStore()
    await store.loginAction({ username: 'admin', password: 'x' })
    expect(store.token).toBe('tok')
    expect(localStorage.getItem('token')).toBe('tok')
    expect(store.userInfo.username).toBe('admin')
  })

  it('getUserInfo loads menus and permissions', async () => {
    vi.mocked(getInfo).mockResolvedValue({
      code: 200,
      data: {
        userId: 2,
        username: 'u1',
        nickname: '用户1',
        roles: ['admin'],
        permissions: ['system:user:list'],
        menus: [{ id: 10, parentId: 0, name: '首页', type: 2, path: '/home' }],
      },
    })
    const store = useUserStore()
    await store.getUserInfo()
    expect(store.menus).toHaveLength(1)
    expect(store.userInfo.permissions).toContain('system:user:list')
  })

  it('logout clears local session', () => {
    const store = useUserStore()
    store.token = 'old'
    store.userInfo = { username: 'a' }
    store.menus = [{ id: 1, parentId: 0, name: 'x', type: 2, path: '/' }]
    localStorage.setItem('token', 'old')
    store.logout()
    expect(store.token).toBe('')
    expect(store.userInfo).toEqual({})
    expect(store.menus).toEqual([])
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('logoutAction calls API then clears state', async () => {
    vi.mocked(logoutApi).mockResolvedValue({ code: 200, data: null })
    const store = useUserStore()
    store.token = 'tok'
    await store.logoutAction()
    expect(logoutApi).toHaveBeenCalled()
    expect(store.token).toBe('')
  })
})
