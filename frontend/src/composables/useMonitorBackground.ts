import { useUserStore } from '@/store/user'
import { hasMonitorPerm } from '@/utils/hasMenuPerm'
import {
  onMonitorVisibilityChange,
  resetCacheMonitorBackground,
  startCacheMonitorBackground,
  stopCacheMonitorBackground,
} from '@/views/monitor/cache/cacheMonitorChart'
import {
  onServerMonitorVisibilityChange,
  resetServerMonitorBackground,
  startServerMonitorBackground,
  stopServerMonitorBackground,
} from '@/views/monitor/server/serverMonitorChart'

let visibilityBound = false

function handleVisibilityChange() {
  const hidden = document.hidden
  onMonitorVisibilityChange(hidden)
  onServerMonitorVisibilityChange(hidden)
}

export function isAdmin(userStore: ReturnType<typeof useUserStore>): boolean {
  const roles = userStore.userInfo.roles || []
  return roles.includes('admin') || roles.includes('super_admin')
}

function syncMonitorBackground(userStore: ReturnType<typeof useUserStore>) {
  // 仅超级管理员登录后自动启动全局后台轮询，普通用户进入监控页才按需采样
  if (!isAdmin(userStore)) {
    stopCacheMonitorBackground()
    stopServerMonitorBackground()
    return
  }

  const permissions = userStore.userInfo.permissions
  const menus = userStore.menus

  if (hasMonitorPerm(permissions, menus, 'monitor:cache:query')) {
    startCacheMonitorBackground()
  } else {
    stopCacheMonitorBackground()
  }

  if (hasMonitorPerm(permissions, menus, 'monitor:server:query')) {
    startServerMonitorBackground()
  } else {
    stopServerMonitorBackground()
  }
}

export function startMonitorBackground(userStore: ReturnType<typeof useUserStore>) {
  syncMonitorBackground(userStore)
  if (!visibilityBound) {
    document.addEventListener('visibilitychange', handleVisibilityChange)
    visibilityBound = true
  }
}

export function stopMonitorBackground() {
  stopCacheMonitorBackground()
  stopServerMonitorBackground()
  if (visibilityBound) {
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    visibilityBound = false
  }
}

export function resetMonitorBackground() {
  stopMonitorBackground()
  resetCacheMonitorBackground()
  resetServerMonitorBackground()
}
