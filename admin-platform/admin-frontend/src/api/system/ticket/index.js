import request from '@/utils/request'

export function getTicketPage(params) {
  return request({
    url: '/system/ticket/page',
    method: 'get',
    params
  })
}

export function getTicket(id) {
  return request({
    url: '/system/ticket/get',
    method: 'get',
    params: { id }
  })
}

export function createTicket(data) {
  return request({
    url: '/system/ticket/create',
    method: 'post',
    data
  })
}

export function updateTicket(data) {
  return request({
    url: '/system/ticket/update',
    method: 'put',
    data
  })
}

export function transitionTicket(data) {
  return request({
    url: '/system/ticket/transition',
    method: 'put',
    data
  })
}

export function deleteTicket(id) {
  return request({
    url: '/system/ticket/delete',
    method: 'delete',
    params: { id }
  })
}

export function getRecycleTicketPage(params) {
  return request({
    url: '/system/ticket/recycle/page',
    method: 'get',
    params
  })
}

export function restoreTicket(id) {
  return request({
    url: '/system/ticket/restore',
    method: 'put',
    params: { id }
  })
}

export function deleteTicketPermanent(id) {
  return request({
    url: '/system/ticket/delete-permanent',
    method: 'delete',
    params: { id }
  })
}

export function getTicketComments(ticketId) {
  return request({
    url: '/system/ticket/comment/list',
    method: 'get',
    params: { ticketId }
  })
}

export function createTicketComment(data) {
  return request({
    url: '/system/ticket/comment/create',
    method: 'post',
    data
  })
}

export function getTicketAttachments(ticketId) {
  return request({
    url: '/system/ticket/attachment/list',
    method: 'get',
    params: { ticketId }
  })
}

export function uploadTicketAttachment(ticketId, file) {
  const formData = new FormData()
  formData.append('ticketId', ticketId)
  formData.append('file', file)
  return request({
    url: '/system/ticket/attachment/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
