import { ref, type Ref } from 'vue'

/** 监控页工具栏：手动刷新 + 切换全局后台自动采样（与 PC 一致，默认开启） */
export function useMonitorToolbarRefresh(
  refreshFn: () => Promise<void>,
  autoRefreshRef: Ref<boolean>,
  setAutoRefresh: (val: boolean) => void,
) {
  const refreshing = ref(false)

  async function manualRefresh() {
    if (refreshing.value) return
    refreshing.value = true
    try {
      await refreshFn()
    } finally {
      refreshing.value = false
    }
  }

  function toggleAuto() {
    setAutoRefresh(!autoRefreshRef.value)
  }

  return {
    autoRefresh: autoRefreshRef,
    refreshing,
    toggleAuto,
    manualRefresh,
  }
}
