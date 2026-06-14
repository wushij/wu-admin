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

function syncMonitorBackground() {
  const userStore = useUserStore()

  if (userStore.hasPermission('monitor:cache:list')) {
    startCacheMonitorBackground()
  } else {
    stopCacheMonitorBackground()
  }

  if (userStore.hasPermission('monitor:server:list')) {
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
