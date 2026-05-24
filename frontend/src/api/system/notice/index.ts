import request from '@/utils/request'
import type { ApiResult, PageQuery, PageResult } from '@/types/api'

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
  [key: string]: unknown
}

export function getUnreadNoticeCount() {
  return request.get('/system/notice/unread-count') as Promise<ApiResult<number>>
}

export function getMyNoticeList() {
  return request.get('/system/notice/my-list') as Promise<ApiResult<NoticeVO[]>>
}

export function readNotice(id: number) {
  return request.put('/system/notice/read', { id })
}

export function readAllNotice() {
  return request.put('/system/notice/read-all')
}

export function getNoticePage(params: PageQuery) {
  return request.get('/system/notice/page', { params }) as Promise<ApiResult<PageResult<NoticeVO>>>
}
