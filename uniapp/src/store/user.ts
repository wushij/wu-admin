import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getInfo, logout as logoutApi, sessionSignInit } from '@/api/system/auth'
import type { LoginForm, MenuTreeNode, AuthInfo } from '@/types/api'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { useMessageStore } from '@/store/message'
import { logger } from '@/utils/logger'
import { resetMonitorBackground } from '@/composables/useMonitorBackground'
import { setSecurityConfig, getClientId, requestSessionSignKey } from '@/utils/security-config'
import http from '@/utils/request'

export type UserInfo = Partial<
  Pick<AuthInfo, 'userId' | 'username' | 'nickname' | 'avatar' | 'roles' | 'permissions'>
>

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo>({})
  const menus = ref<MenuTreeNode[]>([])
  const token = ref<string>(getToken())

  const isLoggedIn = computed(() => !!token.value && userInfo.value.userId != null)

  const initSessionKey = async () => {
    try {
      await requestSessionSignKey(http)
    } catch {
      /* 忽略获取异常 */
    }
  }

  const loginAction = async (form: LoginForm) => {
    const res = await login(form)
    const tkn = res.data.token || ''
    token.value = tkn
    if (tkn) setToken(tkn)
    userInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
    }
    await initSessionKey()
    return res
  }

  const getUserInfo = async () => {
    await initSessionKey()
    const res = await getInfo()
    userInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      avatar: res.data.avatar,
      roles: res.data.roles || [],
      permissions: res.data.permissions || [],
    }
    menus.value = res.data.menus || []
    return res
  }

  const refreshUserStore = async () => {
    try {
      await getUserInfo()
    } catch (error) {
      logger.error('刷新用户信息失败:', error)
      const msg = error instanceof Error ? error.message : String(error || '')
      // 满足以下任一条件均视为会话已失效，必须清理本地 Storage，打破死锁状态：
      // 1. 明确的未授权或登录已过期错误
      // 2. 要求具备登录态（会话密钥初始化在 Token 失效时的报错）
      // 3. 冷启动恢复场景下（内存中尚无有效 userId）获取用户信息失败
      const isSessionInvalid =
        msg.includes('未授权') ||
        msg.includes('登录已过期') ||
        msg.includes('具备登录态') ||
        userInfo.value.userId == null
      if (isSessionInvalid) {
        logout()
      }
      throw error
    }
  }

  let loadingPromise: Promise<any> | null = null
  const ensureUserLoaded = async () => {
    if (!token.value) return null
    if (userInfo.value.userId != null) return userInfo.value
    if (!loadingPromise) {
      loadingPromise = refreshUserStore().finally(() => {
        loadingPromise = null
      })
    }
    return loadingPromise
  }

  const patchUserInfo = (partial: Partial<UserInfo>) => {
    userInfo.value = { ...userInfo.value, ...partial }
  }

  const logout = () => {
    try {
      useMessageStore().destroyWebSocket()
    } catch {
      /* store 可能尚未初始化 */
    }
    resetMonitorBackground()
    userInfo.value = {}
    menus.value = []
    token.value = ''
    removeToken()
  }

  const logoutAction = async () => {
    try {
      if (token.value) {
        await logoutApi()
      }
    } catch (error) {
      logger.warn('调用退出接口失败，执行本地清理', error)
    } finally {
      logout()
      uni.reLaunch({ url: '/pages/login/index' })
    }
  }

  const hasPermission = (perm: string): boolean => {
    if (userInfo.value.roles?.includes('admin')) return true
    if (userInfo.value.permissions?.includes(perm)) return true
    return checkMenuPermission(menus.value, perm)
  }

  /** 工作台入口：仅以启用菜单树为准，停用菜单不会出现在侧栏/工作台 */
  const hasMenuPermission = (perm: string): boolean => {
    if (userInfo.value.roles?.includes('admin')) return true
    return checkMenuPermission(menus.value, perm)
  }

  return {
    userInfo,
    menus,
    token,
    isLoggedIn,
    ensureUserLoaded,
    loginAction,
    getUserInfo,
    refreshUserStore,
    patchUserInfo,
    logout,
    logoutAction,
    hasPermission,
    hasMenuPermission,
  }
})

function checkMenuPermission(items: MenuTreeNode[], perm: string): boolean {
  for (const menu of items) {
    if (menu.permission === perm) return true
    if (menu.children?.length && checkMenuPermission(menu.children, perm)) return true
  }
  return false
}
