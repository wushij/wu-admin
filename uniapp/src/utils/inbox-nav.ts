import { readNotice } from '@/api/system/notice'
import { useMessageStore } from '@/store/message'
import { summarizeText } from '@/utils/format'
import type { NoticeVO } from '@/types/message'

const INBOX_CACHE_PREFIX = 'inbox_detail_'

export function cacheInboxItem(item: NoticeVO) {
  uni.setStorageSync(`${INBOX_CACHE_PREFIX}${item.id}`, JSON.stringify(item))
}

export function readInboxCache(id: number): NoticeVO | null {
  try {
    const raw = uni.getStorageSync(`${INBOX_CACHE_PREFIX}${id}`)
    return raw ? (JSON.parse(String(raw)) as NoticeVO) : null
  } catch {
    return null
  }
}

export async function markInboxRead(item: NoticeVO) {
  if (item.readStatus === 1) return
  await readNotice(item.id)
  item.readStatus = 1
  await useMessageStore().refreshSummary()
}

/** 打开站内信：对齐 PC，支持跳转工单 / 审批 */
export async function openInboxItem(item: NoticeVO) {
  await markInboxRead(item)

  if (item.bizType === 'TICKET' && item.bizId) {
    uni.navigateTo({ url: `/pages-sub/system/ticket/detail?id=${item.bizId}` })
    return
  }
  if (item.bizType === 'APPROVAL' && item.bizId) {
    uni.navigateTo({ url: `/pages-sub/system/approval/detail?id=${item.bizId}` })
    return
  }

  cacheInboxItem(item)
  uni.navigateTo({ url: `/pages-sub/msg/inbox/detail?id=${item.id}` })
}

export function inboxBizLabel(bizType?: string) {
  if (bizType === 'TICKET') return '工单'
  if (bizType === 'APPROVAL') return '审批'
  return ''
}

/** 列表摘要：避免展示原始 JSON */
export function inboxContentPreview(item: Pick<NoticeVO, 'content' | 'bizType' | 'title'>) {
  const content = item.content?.trim()
  if (!content) return '暂无详情'
  if (content.startsWith('{') && content.endsWith('}')) {
    if (item.bizType === 'APPROVAL') return '点击查看审批详情'
    if (item.bizType === 'TICKET') return '点击查看工单详情'
    return '点击查看详情'
  }
  return summarizeText(content)
}
