import { ref, computed } from 'vue'
import { getEmailLogs } from '@/api/system/config'
import type { EmailLogRecord } from '@/types/config-types'

export function useEmailLogs() {
  const list = ref<EmailLogRecord[]>([])
  const loading = ref(false)
  const refreshing = ref(false)
  const finished = ref(false)
  const total = ref(0)
  const page = ref(1)
  const email = ref('')
  const status = ref<number | null>(null)

  const empty = computed(() => !loading.value && list.value.length === 0)

  function emailStatusText(s: number | undefined) {
    if (s === 1) return '成功'
    if (s === 2) return '失败'
    return '发送中'
  }

  async function load(append = false) {
    if (loading.value) return
    loading.value = true
    try {
      const res = await getEmailLogs({
        page: page.value,
        size: 15,
        email: email.value.trim() || undefined,
        status: status.value,
      })
      const rows = res.data?.list || []
      list.value = append ? [...list.value, ...rows] : rows
      total.value = res.data?.total || 0
      finished.value = list.value.length >= total.value
    } finally {
      loading.value = false
      refreshing.value = false
    }
  }

  async function refresh() {
    page.value = 1
    finished.value = false
    refreshing.value = true
    await load()
  }

  async function loadMore() {
    if (finished.value || loading.value) return
    page.value += 1
    await load(true)
  }

  function setStatusFilter(key: string) {
    if (key === 'success') status.value = 1
    else if (key === 'fail') status.value = 2
    else status.value = null
    refresh()
  }

  return {
    list,
    loading,
    refreshing,
    finished,
    empty,
    total,
    email,
    emailStatusText,
    refresh,
    loadMore,
    setStatusFilter,
  }
}
