import { ref } from 'vue'
import { getMyNoticeList, readNotice, readAllNotice } from '@/api/system/notice'
import { useMessageStore } from '@/store/message'
import { useNoticeWs } from '@/composables/useNoticeWs'
import type { NoticeVO } from '@/types/message'

export function useInboxList() {
  const list = ref<NoticeVO[]>([])
  const loading = ref(false)

  async function refresh() {
    loading.value = true
    try {
      const res = await getMyNoticeList()
      list.value = res.data || []
      await useMessageStore().refreshSummary()
    } finally {
      loading.value = false
    }
  }

  async function markRead(item: NoticeVO) {
    if (item.readStatus === 1) return
    await readNotice(item.id)
    item.readStatus = 1
    await useMessageStore().refreshSummary()
  }

  async function markAllRead() {
    await readAllNotice()
    list.value.forEach((item) => {
      item.readStatus = 1
    })
    await useMessageStore().refreshSummary()
  }

  useNoticeWs(refresh)

  return { list, loading, refresh, markRead, markAllRead }
}
