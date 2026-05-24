import { get, put, del } from '@/utils/request'
import type { NoticeVO } from '@/types/message'

export function getMyNoticeList() {
  return get<NoticeVO[]>('/system/notice/my-list')
}

export function readNotice(id: number) {
  return put<boolean>('/system/notice/read', { id })
}

export function readAllNotice() {
  return put<boolean>('/system/notice/read-all')
}

export function deleteNotice(id: number) {
  return del<boolean>(`/system/notice/${id}`)
}
