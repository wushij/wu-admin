import request from '@/utils/request'
import type { ApiResult, PageQuery, PageResult } from '@/types/api'

export interface TicketVO {
  id: number
  ticketNo?: string
  title?: string
  description?: string
  priority?: string
  status?: string
  creatorUserId?: number
  assigneeUserId?: number
  creatorName?: string
  assigneeName?: string
  deadline?: string
  closedTime?: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

export interface AssigneeOptionVO {
  id: number
  username?: string
  nickname?: string
}

export interface TicketCommentVO {
  id: number
  ticketId?: number
  userId?: number
  username?: string
  content?: string
  createTime?: string
  [key: string]: unknown
}

export interface TicketAttachmentVO {
  id: number
  ticketId?: number
  uploaderUserId?: number
  fileName?: string
  filePath?: string
  fileSize?: number
  uploaderName?: string
  createTime?: string
  [key: string]: unknown
}

export function getTicketPage(params: PageQuery) {
  return request.get('/system/ticket/page', { params }) as Promise<ApiResult<PageResult<TicketVO>>>
}

/** 工单处理人下拉（普通用户可用，无需 system:user:list） */
export function getTicketAssigneeOptions() {
  return request.get('/system/ticket/assignee-options') as Promise<ApiResult<AssigneeOptionVO[]>>
}

export function getTicket(id: number) {
  return request.get('/system/ticket/get', { params: { id } }) as Promise<ApiResult<TicketVO>>
}

export function createTicket(data: Record<string, unknown>) {
  return request.post('/system/ticket/create', data)
}

export function updateTicket(data: Record<string, unknown>) {
  return request.put('/system/ticket/update', data)
}

export function transitionTicket(data: Record<string, unknown>) {
  return request.put('/system/ticket/transition', data)
}

export function deleteTicket(id: number) {
  return request.delete('/system/ticket/delete', { params: { id } })
}

export function getRecycleTicketPage(params: PageQuery) {
  return request.get('/system/ticket/recycle/page', { params }) as Promise<ApiResult<PageResult<TicketVO>>>
}

export function restoreTicket(id: number) {
  return request.put('/system/ticket/restore', null, { params: { id } })
}

export function deleteTicketPermanent(id: number) {
  return request.delete('/system/ticket/delete-permanent', { params: { id } })
}

export function getTicketComments(ticketId: number) {
  return request.get('/system/ticket/comment/list', { params: { ticketId } }) as Promise<ApiResult<TicketCommentVO[]>>
}

export function createTicketComment(data: Record<string, unknown>) {
  return request.post('/system/ticket/comment/create', data)
}

export function getTicketAttachments(ticketId: number) {
  return request.get('/system/ticket/attachment/list', { params: { ticketId } }) as Promise<ApiResult<TicketAttachmentVO[]>>
}

export function uploadTicketAttachment(ticketId: number, file: File) {
  const formData = new FormData()
  formData.append('ticketId', String(ticketId))
  formData.append('file', file)
  return request.post('/system/ticket/attachment/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
