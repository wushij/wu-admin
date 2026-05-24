import { useUserStore } from '@/store/user'
import {
  onCacheMonitorVisibilityChange,
  resetCacheMonitorBackground,
  startCacheMonitorBackground,
  stopCacheMonitorBackground,
} from '@/composables/monitor/cacheMonitorChart'
import {
  onServerMonitorVisibilityChange,
  resetServerMonitorBackground,
  startServerMonitorBackground,
  stopServerMonitorBackground,
} from '@/composables/monitor/serverMonitorChart'

export function isMonitorAdmin(): boolean {
  const userStore = useUserStore()
  const roles = userStore.userInfo.roles || []
  return roles.includes('admin') || roles.includes('super_admin')
}

function syncMonitorBackground() {
  const userStore = useUserStore()
  const roles = userStore.userInfo.roles || []

  // 仅超级管理员登录后自动启动全局后台轮询，普通用户进入监控页才按需采样
  if (!roles.includes('admin') && !roles.includes('super_admin')) {
    stopCacheMonitorBackground()
    stopServerMonitorBackground()
    return
  }

  if (userStore.hasPermission('monitor:cache:query')) {
    startCacheMonitorBackground()
  } else {
    stopCacheMonitorBackground()
  }

  if (userStore.hasPermission('monitor:server:query')) {
    startServerMonitorBackground()
  } else {
    stopServerMonitorBackground()
  }
}

export function startMonitorBackground() {
  syncMonitorBackground()
}

export function stopMonitorBackground() {
  stopCacheMonitorBackground()
  stopServerMonitorBackground()
}

export function resetMonitorBackground() {
  stopMonitorBackground()
  resetCacheMonitorBackground()
  resetServerMonitorBackground()
}

export function onMonitorAppShow() {
  onCacheMonitorVisibilityChange(false)
  onServerMonitorVisibilityChange(false)
}

export function onMonitorAppHide() {
  onCacheMonitorVisibilityChange(true)
  onServerMonitorVisibilityChange(true)
}
