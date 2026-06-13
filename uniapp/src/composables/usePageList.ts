import { ref, computed, shallowRef } from 'vue'

interface PageFetcher<T> {
  (pageNo: number, pageSize: number): Promise<{ list: T[]; total: number }>
}

/** 分页列表：下拉刷新 + 触底加载（pageNo 与后端 PageParam 一致） */
export function usePageList<T>(fetcher: PageFetcher<T>, pageSize = 15) {
  const list = shallowRef<T[]>([])
  const loading = ref(false)
  const refreshing = ref(false)
  const pageNo = ref(1)
  const total = ref(0)

  const finished = computed(() => list.value.length >= total.value && total.value > 0)
  const empty = computed(() => !loading.value && !refreshing.value && list.value.length === 0)

  async function loadPage(reset = false) {
    if (loading.value && !reset) return
    if (!reset && finished.value) return
    loading.value = true
    try {
      const current = reset ? 1 : pageNo.value
      const res = await fetcher(current, pageSize)
      const rows = res.list || []
      total.value = res.total || 0
      if (reset) {
        list.value = rows
        pageNo.value = 2
      } else {
        list.value = [...list.value, ...rows]
        pageNo.value += 1
      }
    } finally {
      loading.value = false
      refreshing.value = false
    }
  }

  async function refresh() {
    refreshing.value = true
    pageNo.value = 1
    total.value = 0
    loading.value = false
    try {
      await loadPage(true)
    } catch {
      refreshing.value = false
      loading.value = false
    }
  }

  function loadMore() {
    if (loading.value || refreshing.value) return
    loadPage(false)
  }

  return {
    list,
    loading,
    refreshing,
    finished,
    empty,
    total,
    refresh,
    loadMore,
  }
}
