import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getInfo, logout as logoutApi, sessionSignInit } from '@/api/system/auth'
import type { LoginForm, MenuTreeNode, AuthInfo } from '@/types/api'
import { getToken, setToken, removeToken } from '@/core/auth'
import { useMessageStore } from '@/store/message'
import { logger } from '@/core/logger'
import { resetMonitorBackground } from '@/composables/monitor/useMonitorBackground'
import { setSecurityConfig, getClientId, requestSessionSignKey, resetSecurityConfig } from '@/utils/security/security-config'
import http from '@/core/http'

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
    // 优先使用服务端随登录响应原子下发的会话临时密钥（零时延、零网络竞态）
    const loginData = res.data as typeof res.data & { sm3SignKey?: string; sm4Key?: string }
    if (loginData.sm3SignKey || loginData.sm4Key) {
      setSecurityConfig({
        sm3SignKey: loginData.sm3SignKey,
        sm4Key: loginData.sm4Key,
      })
    } else {
      // 兼容兜底：若未返回则再异步尝试协商拉取
      await initSessionKey()
    }
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
    // 登出/会话失效时必须清空内存中的会话密钥，否则下次登录会用残留旧密钥加密请求，
    // 而登录接口会在服务端轮换密钥，导致登录响应 GCM Tag 校验失败。
    resetSecurityConfig()
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

  /** 管理员（admin / super_admin）拥有全部权限 */
  const isSuperAdmin = (): boolean => {
    const roles = userInfo.value.roles || []
    return roles.includes('admin') || roles.includes('super_admin')
  }

  /** 通配权限：拥有 '*:*:*' 等同拥有全部权限 */
  const hasWildcardPermission = (): boolean => {
    const codes = userInfo.value.permissions || []
    return codes.includes('*:*:*')
  }

  /**
   * 权限判断核心方法（与 PC 端保持一致）
   * 1. 管理员（admin / super_admin 角色）拥有所有权限
   * 2. 具备通配符 '*:*:*' 权限拥有所有权限
   * 3. 匹配 userInfo.permissions 权限码集合（支持按钮与接口级权限）
   * 4. 匹配 menus 菜单树（支持页面级权限）
   */
  const hasPermission = (perm: string): boolean => {
    if (isSuperAdmin() || hasWildcardPermission()) return true
    if (userInfo.value.permissions?.includes(perm)) return true
    return checkMenuPermission(menus.value, perm)
  }

  /**
   * 工作台入口：优先匹配启用菜单树，停用菜单不会出现在侧栏/工作台。
   * 说明：后端下发的 menus 仅包含目录与页面（已过滤 type=3 按钮），
   * 若菜单树未命中，回退检查权限码与超级管理员角色，避免按钮权限被误判为无权限。
   */
  const hasMenuPermission = (perm: string): boolean => {
    if (checkMenuPermission(menus.value, perm)) return true
    if (isSuperAdmin() || hasWildcardPermission()) return true
    return userInfo.value.permissions?.includes(perm) ?? false
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
