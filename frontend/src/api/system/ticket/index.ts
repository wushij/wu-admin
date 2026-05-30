import { get, post, put, del } from '@/utils/request'
import type { PageQuery, PageResult, RecyclePageQuery } from '@/types/api'

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
}

export interface TicketPageQuery extends PageQuery {
  title?: string
  status?: string
  priority?: string
}

export interface TicketSaveDTO {
  id?: number | null
  title: string
  description?: string
  priority?: string
  assigneeUserId?: number | null
  deadline?: string | null
}

export interface TicketTransitionDTO {
  id: number
  status: string
}

export interface TicketCommentCreateDTO {
  ticketId: number
  content: string
}

export function getTicketPage(params: TicketPageQuery) {
  return get<PageResult<TicketVO>>('/system/ticket/page', params)
}

/** 工单处理人下拉（普通用户可用，无需 system:user:list） */
export function getTicketAssigneeOptions() {
  return get<AssigneeOptionVO[]>('/system/ticket/assignee-options')
}

export function getTicket(id: number) {
  return get<TicketVO>('/system/ticket/get', { id })
}

export function createTicket(data: TicketSaveDTO) {
  return post('/system/ticket/create', data)
}

export function updateTicket(data: TicketSaveDTO) {
  return put('/system/ticket/update', data)
}

export function transitionTicket(data: TicketTransitionDTO) {
  return put('/system/ticket/transition', data)
}

export function deleteTicket(id: number) {
  return del('/system/ticket/delete', { params: { id } })
}

export function getRecycleTicketPage(params: RecyclePageQuery) {
  return get<PageResult<TicketVO>>('/system/ticket/recycle/page', params)
}

export function restoreTicket(id: number) {
  return put('/system/ticket/restore', null, { params: { id } })
}

export function deleteTicketPermanent(id: number) {
  return del('/system/ticket/delete-permanent', { params: { id } })
}

export function getTicketComments(ticketId: number) {
  return get<TicketCommentVO[]>('/system/ticket/comment/list', { ticketId })
}

export function createTicketComment(data: TicketCommentCreateDTO) {
  return post('/system/ticket/comment/create', data)
}

export function getTicketAttachments(ticketId: number) {
  return get<TicketAttachmentVO[]>('/system/ticket/attachment/list', { ticketId })
}

export function uploadTicketAttachment(ticketId: number, file: File) {
  const formData = new FormData()
  formData.append('ticketId', String(ticketId))
  formData.append('file', file)
  return post('/system/ticket/attachment/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
