import request from '@/utils/request'

export function getApprovalPage(params) {
  return request({
    url: '/system/approval/page',
    method: 'get',
    params
  })
}

export function getApproval(id) {
  return request({
    url: '/system/approval/get',
    method: 'get',
    params: { id }
  })
}

export function getApprovalRecords(formId) {
  return request({
    url: '/system/approval/record/list',
    method: 'get',
    params: { formId }
  })
}

export function createApproval(data) {
  return request({
    url: '/system/approval/create',
    method: 'post',
    data
  })
}

export function approveApproval(data) {
  return request({
    url: '/system/approval/approve',
    method: 'put',
    data
  })
}

export function archiveApproval(data) {
  return request({
    url: '/system/approval/archive',
    method: 'put',
    data
  })
}

export function deleteApproval(id) {
  return request({
    url: '/system/approval/delete',
    method: 'delete',
    params: { id }
  })
}

export function getApprovalRecyclePage(params) {
  return request({
    url: '/system/approval/recycle/page',
    method: 'get',
    params
  })
}

export function restoreApproval(id) {
  return request({
    url: '/system/approval/restore',
    method: 'put',
    params: { id }
  })
}

export function deleteApprovalPermanent(id) {
  return request({
    url: '/system/approval/delete-permanent',
    method: 'delete',
    params: { id }
  })
}
