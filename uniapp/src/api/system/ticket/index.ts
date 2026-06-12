import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AssigneeOptionVO,
  TicketCommentCreateDTO,
  TicketCommentVO,
  TicketPageQuery,
  TicketSaveDTO,
  TicketVO,
} from '@/types/system'

export function getTicketPage(params: TicketPageQuery) {
  return get<PageResult<TicketVO>>('/system/ticket/page', params)
}

export function getTicketAssigneeOptions() {
  return get<AssigneeOptionVO[]>('/system/ticket/assignee-options')
}

export function createTicket(data: TicketSaveDTO) {
  return post('/system/ticket/create', data)
}

export function getTicket(id: number) {
  return get<TicketVO>('/system/ticket/get', { id })
}

export function transitionTicket(data: { id: number; status: string }) {
  return put('/system/ticket/transition', data)
}

export function getTicketComments(ticketId: number) {
  return get<TicketCommentVO[]>('/system/ticket/comment/list', { ticketId })
}

export function createTicketComment(data: TicketCommentCreateDTO) {
  return post('/system/ticket/comment/create', data)
}

export function deleteTicket(id: number) {
  return del('/system/ticket/delete', { params: { id } })
}

export function getRecycleTicketPage(params: TicketPageQuery) {
  return get<PageResult<TicketVO>>('/system/ticket/recycle/page', params)
}

export function restoreTicket(id: number) {
  return put('/system/ticket/restore', null, { params: { id } })
}

export function deleteTicketPermanent(id: number) {
  return del('/system/ticket/delete-permanent', { params: { id } })
}
