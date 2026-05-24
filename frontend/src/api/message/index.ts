import request, { get } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'
import type { FileRecord } from '@/api/system/file/index'
import type { ChatGroup, ChatGroupLogItem, ChatMessage, ChatUser, GroupMember } from '@/types/message'

export interface AnnounceSendLog {
  channel?: string
  targetCount?: number
  successCount?: number
  sendTime?: string
}

export interface MessageSummary {
  inboxCount?: number
  announceCount?: number
  chatCount?: number
  noticeCount?: number
  total?: number
}

export interface AnnouncePayload {
  id?: number | null
  title: string
  content: string
  noticeType?: number
  channels?: string[]
  targetType?: number
  targetIds?: number[]
  status?: number
}

export interface ChatSendPayload {
  receiverId?: number
  content: string
  msgType?: number
}

export interface CreateGroupPayload {
  name: string
  memberIds: number[]
}

export interface MessagePageQuery extends PageQuery {
  title?: string
  noticeType?: number | null
  status?: number | null
  isRead?: number | null
}

export function getMessageSummary() {
  return get<MessageSummary>('/system/message/summary')
}

export function getAnnouncePage(params: MessagePageQuery) {
  return get<PageResult<AnnouncePayload & { id: number; createName?: string; createTime?: string }>>('/system/announce/page', params)
}

export function getMyAnnounce(params?: MessagePageQuery) {
  return get<PageResult<AnnouncePayload & { id: number; isRead?: number; createTime?: string }>>('/system/announce/my', params)
}

export function getAnnounceDetail(id: number) {
  return get<AnnouncePayload & { id: number }>(`/system/announce/${id}`)
}

export function createAnnounce(data: AnnouncePayload) {
  return request.post('/system/announce', data)
}

export function updateAnnounce(data: AnnouncePayload) {
  return request.put('/system/announce', data)
}

export function deleteAnnounce(id: number) {
  return request.delete(`/system/announce/${id}`)
}

export function publishAnnounce(id: number) {
  return request.post(`/system/announce/${id}/publish`)
}

export function readAnnounce(id: number) {
  return request.post(`/system/announce/${id}/read`)
}

export function readAllAnnounce() {
  return request.post('/system/announce/read-all')
}

export function getAnnounceUnreadCount() {
  return get<number>('/system/announce/unread-count')
}

export function getAnnounceSendLogs(id: number) {
  return get<AnnounceSendLog[]>(`/system/announce/${id}/send-logs`)
}

export function sendChat(data: ChatSendPayload) {
  return request.post('/system/chat/send', data)
}

export function uploadChatImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<FileRecord>('/system/chat/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getChatHistory(targetId: number, params?: MessagePageQuery) {
  return get<PageResult<ChatMessage>>(`/system/chat/history/${targetId}`, params)
}

export function getChatUsers() {
  return get<ChatUser[]>('/system/chat/users')
}

export function readChat(senderId: number) {
  return request.post(`/system/chat/read/${senderId}`)
}

export function getChatUnreadCount() {
  return get<number>('/system/chat/unread-count')
}

export function isUserOnline(userId: number) {
  return get<boolean>(`/system/chat/online/${userId}`)
}

export function clearChatHistory(targetId: number) {
  return request.delete(`/system/chat/clear/${targetId}`)
}

export function blockUser(targetId: number) {
  return request.post(`/system/chat/block/${targetId}`)
}

export function unblockUser(targetId: number) {
  return request.delete(`/system/chat/block/${targetId}`)
}

export function createChatGroup(data: CreateGroupPayload) {
  return request.post('/system/chat/group/create', data)
}

export function getChatGroups() {
  return get<ChatGroup[]>('/system/chat/group/list')
}

export function sendGroupMessage(groupId: number, data: ChatSendPayload) {
  return request.post(`/system/chat/group/${groupId}/message`, data)
}

export function getGroupMessages(groupId: number, params?: MessagePageQuery) {
  return get<PageResult<ChatMessage>>(`/system/chat/group/${groupId}/messages`, params)
}

export function getGroupMembers(groupId: number) {
  return get<GroupMember[]>(`/system/chat/group/${groupId}/members`)
}

export function getGroupLogs(groupId: number) {
  return get<ChatGroupLogItem[]>(`/system/chat/group/${groupId}/logs`)
}

export function getGroupDetail(groupId: number) {
  return get<ChatGroup>(`/system/chat/group/${groupId}`)
}

export interface UpdateGroupPayload {
  id: number
  name?: string
  announcement?: string
}

export function updateChatGroup(data: UpdateGroupPayload) {
  return request.put('/system/chat/group/update', data)
}

export function addGroupMembers(groupId: number, userIds: number[]) {
  return request.post(`/system/chat/group/${groupId}/members`, { userIds })
}

export function removeGroupMember(groupId: number, memberUserId: number) {
  return request.delete(`/system/chat/group/${groupId}/members/${memberUserId}`)
}

export function setGroupAdmin(groupId: number, memberUserId: number, admin = true) {
  return request.post(`/system/chat/group/${groupId}/admin/${memberUserId}`, null, { params: { admin } })
}

export function setGroupMuted(groupId: number, memberUserId: number, muted = true) {
  return request.post(`/system/chat/group/${groupId}/mute/${memberUserId}`, null, { params: { muted } })
}

export function transferGroupOwner(groupId: number, newOwnerId: number) {
  return request.post(`/system/chat/group/${groupId}/transfer/${newOwnerId}`)
}

export function quitGroup(groupId: number) {
  return request.post(`/system/chat/group/${groupId}/quit`)
}

export function dissolveGroup(groupId: number) {
  return request.delete(`/system/chat/group/${groupId}`)
}
