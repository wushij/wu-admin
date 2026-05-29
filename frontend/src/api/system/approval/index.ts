import request from '@/utils/request'
import type { ApiResult, PageQuery, PageResult } from '@/types/api'

export interface ApprovalVO {
  id: number
  formNo?: string
  formType?: string
  title?: string
  content?: string
  status?: string
  applicantUserId?: number
  approverUserId?: number
  applicantName?: string
  approverName?: string
  resultRemark?: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

export interface ApprovalRecordVO {
  id: number
  formId?: number
  operatorUserId?: number
  operatorName?: string
  action?: string
  remark?: string
  createTime?: string
  [key: string]: unknown
}

export function getApprovalPage(params: PageQuery) {
  return request.get('/system/approval/page', { params }) as Promise<ApiResult<PageResult<ApprovalVO>>>
}

export function getApproval(id: number) {
  return request.get('/system/approval/get', { params: { id } }) as Promise<ApiResult<ApprovalVO>>
}

export function getApprovalRecords(formId: number) {
  return request.get('/system/approval/record/list', { params: { formId } }) as Promise<ApiResult<ApprovalRecordVO[]>>
}

export function createApproval(data: Record<string, unknown>) {
  return request.post('/system/approval/create', data)
}

export function approveApproval(data: Record<string, unknown>) {
  return request.put('/system/approval/approve', data)
}

export function archiveApproval(data: Record<string, unknown>) {
  return request.put('/system/approval/archive', data)
}

export function deleteApproval(id: number) {
  return request.delete('/system/approval/delete', { params: { id } })
}

export function getApprovalRecyclePage(params: PageQuery) {
  return request.get('/system/approval/recycle/page', { params }) as Promise<ApiResult<PageResult<ApprovalVO>>>
}

export function restoreApproval(id: number) {
  return request.put('/system/approval/restore', null, { params: { id } })
}

export function deleteApprovalPermanent(id: number) {
  return request.delete('/system/approval/delete-permanent', { params: { id } })
}
