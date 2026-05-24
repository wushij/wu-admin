import { ref, onMounted, onUnmounted } from 'vue'

const STORAGE_PREFIX = 'monitor:autoRefresh:'

export function useAutoRefresh(
  refreshFn: () => Promise<void>,
  intervalMs = 10000,
  storageKey?: string,
) {
  const storageId = storageKey ? `${STORAGE_PREFIX}${storageKey}` : ''
  const autoRefresh = ref(false)
  const refreshing = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  function readStored() {
    if (!storageId) return false
    try {
      return uni.getStorageSync(storageId) === '1'
    } catch {
      return false
    }
  }

  function persist(enabled: boolean) {
    if (!storageId) return
    try {
      uni.setStorageSync(storageId, enabled ? '1' : '0')
    } catch {
      /* ignore */
    }
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function start() {
    stop()
    timer = setInterval(() => {
      if (!refreshing.value) manualRefresh()
    }, intervalMs)
  }

  function toggleAuto() {
    autoRefresh.value = !autoRefresh.value
    persist(autoRefresh.value)
    if (autoRefresh.value) start()
    else stop()
  }

  async function manualRefresh() {
    if (refreshing.value) return
    refreshing.value = true
    try {
      await refreshFn()
    } finally {
      refreshing.value = false
    }
  }

  onMounted(() => {
    if (storageId) {
      autoRefresh.value = readStored()
      if (autoRefresh.value) start()
    }
  })

  onUnmounted(stop)

  return { autoRefresh, refreshing, toggleAuto, manualRefresh, stop }
}
