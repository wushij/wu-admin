import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyNoticeList, readAllNotice, readNotice } from '@/api/system/notice'
import { getMyAnnounce, readAnnounce, readAllAnnounce } from '@/api/message/index'
import { useMessageStore, type InboxNoticeItem } from '@/store/message'
import type { AnnounceMyVO } from '@/types/message'

export function useLayoutMessages() {
  const router = useRouter()
  const messageStore = useMessageStore()

  const messageTab = ref('inbox')
  const inboxList = ref<InboxNoticeItem[]>([])
  const announceList = ref<AnnounceMyVO[]>([])

  const loadAnnounceList = async () => {
    try {
      const res = await getMyAnnounce({ pageNo: 1, pageSize: 20 })
      announceList.value = res.data?.list || []
    } catch (error) {
      console.error('加载系统通知失败', error)
    }
  }

  const loadMessages = async () => {
    try {
      const [inboxRes] = await Promise.all([getMyNoticeList(), loadAnnounceList()])
      inboxList.value = inboxRes.data || []
      await messageStore.refreshSummary()
    } catch (error) {
      console.error('加载消息失败', error)
    }
  }

  const handleReadInbox = async (item: InboxNoticeItem) => {
    if (item.readStatus === 0) {
      await readNotice(item.id)
      item.readStatus = 1
      messageStore.inboxCount = Math.max(0, messageStore.inboxCount - 1)
    }
    if (item.bizType === 'TICKET' && item.bizId) {
      router.push({ path: '/system/ticket', query: { ticketId: item.bizId } })
      return
    }
    if (item.bizType === 'APPROVAL' && item.bizId) {
      router.push({ path: '/system/approval', query: { approvalId: item.bizId } })
    }
  }

  const handleReadAnnounce = async (item: AnnounceMyVO) => {
    if (!item.isRead) {
      await readAnnounce(item.id)
      item.isRead = 1
      messageStore.announceCount = Math.max(0, messageStore.announceCount - 1)
    }
    await ElMessageBox.alert(item.content || '无内容', item.title || '系统通知', {
      confirmButtonText: '知道了',
    })
  }

  const handleReadAllInbox = async () => {
    await readAllNotice()
    ElMessage.success('业务消息已全部已读')
    inboxList.value = inboxList.value.map((item) => ({ ...item, readStatus: 1 }))
    await messageStore.refreshSummary()
  }

  const handleReadAllAnnounce = async () => {
    await readAllAnnounce()
    ElMessage.success('系统通知已全部已读')
    announceList.value = announceList.value.map((item) => ({ ...item, isRead: 1 }))
    await messageStore.refreshSummary()
  }

  watch(() => messageStore.announceListTick, () => {
    loadAnnounceList()
  })

  watch(messageTab, (tab) => {
    if (tab === 'announce') loadAnnounceList()
  })

  return {
    messageTab,
    inboxList,
    announceList,
    loadMessages,
    handleReadInbox,
    handleReadAnnounce,
    handleReadAllInbox,
    handleReadAllAnnounce,
  }
}
