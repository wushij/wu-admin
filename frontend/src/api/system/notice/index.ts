import { get, put } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface NoticeVO {
  id: number
  title: string
  content: string
  bizType?: string
  bizId?: number
  userId?: number
  isRead?: number
  readStatus?: number
  createTime?: string
}

export interface NoticePageQuery extends PageQuery {
  title?: string
  bizType?: string
  isRead?: number | null
}

export function getUnreadNoticeCount() {
  return get<number>('/system/notice/unread-count')
}

export function getMyNoticeList() {
  return get<NoticeVO[]>('/system/notice/my-list')
}

export function readNotice(id: number) {
  return put('/system/notice/read', { id })
}

export function readAllNotice() {
  return put('/system/notice/read-all')
}

export function getNoticePage(params: NoticePageQuery) {
  return get<PageResult<NoticeVO>>('/system/notice/page', params)
}
