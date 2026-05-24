import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { MenuTreeNode } from '@/types/api'
import { login, getInfo, logout as logoutApi } from '@/api/system/auth'
import type { LoginForm, AuthInfo } from '@/types/api'
import { useMessageStore } from '@/store/message'
import { useTagsViewStore } from '@/store/tagsView'
import { resetMonitorBackground } from '@/composables/useMonitorBackground'
import { resetSecurityConfig, requestSessionSignKey } from '@/utils/security-config'
import http from '@/utils/request'

/** 登录后内存中的用户信息（与 /auth/info 字段子集一致） */
export type UserInfo = Partial<
  Pick<AuthInfo, 'userId' | 'username' | 'nickname' | 'avatar' | 'roles' | 'permissions'>
>

export type { MenuTreeNode as MenuItem } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo>({})
  const menus = ref<MenuTreeNode[]>([])

  const isLoggedIn = computed(() => userInfo.value.userId != null)

  const initSessionKey = async () => {
    try {
      await requestSessionSignKey(http)
    } catch {
      // 忽略
    }
  }

  const loginAction = async (loginForm: LoginForm) => {
    const res = await login(loginForm)
    // 登录成功后，立刻尝试初始化签名密钥。即使获取失败也绝不打断登录主流程（由后续拦截器自愈机制兜底）
    try {
      await initSessionKey()
    } catch (e) {
      console.warn('初始化会话密钥失败，由后续拦截器自愈机制兜底:', e)
    }
    userInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
    }
    return res
  }

  const getUserInfo = async () => {
    // 刷新页面或进入前，优先初始化签名密钥
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
      console.error('刷新用户信息失败:', error)
      logout()
      throw error
    }
  }

  const logout = () => {
    // 登出时，清空签名密钥闭包配置！
    resetSecurityConfig()
    try {
      useMessageStore().destroyWebSocket()
    } catch {
      /* store 可能尚未初始化 */
    }
    resetMonitorBackground()
    try {
      useTagsViewStore().resetTags()
    } catch {
      /* store 可能尚未初始化 */
    }
    userInfo.value = {}
    menus.value = []
  }

  const logoutAction = async () => {
    try {
      if (isLoggedIn.value) {
        await logoutApi()
      }
    } catch (error) {
      console.warn('调用退出接口失败，执行本地清理', error)
    } finally {
      logout()
    }
  }

  const patchUserInfo = (partial: Partial<UserInfo>) => {
    userInfo.value = { ...userInfo.value, ...partial }
  }

  return {
    userInfo,
    menus,
    isLoggedIn,
    loginAction,
    getUserInfo,
    refreshUserStore,
    logout,
    logoutAction,
    patchUserInfo,
  }
})
