import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const menus = ref<any[]>([])

  // 登录
  const login = async (loginForm: { username: string; password: string; uuid?: string; code?: string }) => {
    const data = await loginApi(loginForm)
    token.value = data.token
    localStorage.setItem('token', data.token)
    return data
  }

  // 获取用户信息
  const getInfo = async () => {
    const data = await getUserInfo()
    userInfo.value = data.user
    roles.value = data.roles || []
    permissions.value = data.permissions || []
    menus.value = data.menus || []
    return data
  }

  // 退出登录
  const logout = async () => {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      userInfo.value = null
      roles.value = []
      permissions.value = []
      menus.value = []
      localStorage.removeItem('token')
    }
  }

  return {
    token,
    userInfo,
    roles,
    permissions,
    menus,
    login,
    getInfo,
    logout
  }
}, {
  persist: true
})
