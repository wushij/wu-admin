import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, getInfo, logout as logoutApi } from '@/api/system/auth'
import type { LoginForm } from '@/types/api'
import { useMessageStore } from '@/store/message'

interface UserInfo {
  userId?: number
  username?: string
  nickname?: string
  avatar?: string
  roles?: string[]
  permissions?: string[]
}

interface MenuItem {
  id: number
  name: string
  permission?: string
  type?: number
  sort?: number
  parentId?: number
  path?: string
  icon?: string
  status?: number
  component?: string
  children?: MenuItem[]
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo>({})
  const menus = ref<MenuItem[]>([])

  const loginAction = async (loginForm: LoginForm) => {
    const res = await login(loginForm)
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    userInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname
    }
    return res
  }

  const getUserInfo = async () => {
    const res = await getInfo()
    userInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      avatar: res.data.avatar,
      roles: res.data.roles || [],
      permissions: res.data.permissions || []
    }
    menus.value = res.data.menus || []
    return res
  }

  // 刷新页面时重新获取用户信息
  const refreshUserStore = async () => {
    if (token.value) {
      try {
        await getUserInfo()
      } catch (error) {
        console.error('刷新用户信息失败:', error)
        logout()
      }
    }
  }

  const logout = () => {
    try {
      useMessageStore().destroyWebSocket()
    } catch {
      /* store 可能尚未初始化 */
    }
    token.value = ''
    userInfo.value = {}
    menus.value = []
    localStorage.removeItem('token')
  }

  const logoutAction = async () => {
    try {
      if (token.value) {
        await logoutApi()
      }
    } catch (error) {
      console.warn('调用退出接口失败，执行本地清理', error)
    } finally {
      logout()
    }
  }

  return {
    token,
    userInfo,
    menus,
    loginAction,
    getUserInfo,
    refreshUserStore,
    logout,
    logoutAction
  }
})
