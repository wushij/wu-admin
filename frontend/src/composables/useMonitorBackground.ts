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

function syncMonitorBackground(userStore: ReturnType<typeof useUserStore>) {
  const permissions = userStore.userInfo.permissions
  const menus = userStore.menus

  if (hasMonitorPerm(permissions, menus, 'monitor:cache:list')) {
    startCacheMonitorBackground()
  } else {
    stopCacheMonitorBackground()
  }

  if (hasMonitorPerm(permissions, menus, 'monitor:server:list')) {
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
