import { get, post, put, del } from '@/utils/request'
import { uploadFile } from '@/utils/upload'
import type { PageQuery, PageResult } from '@/types/api'
import type { FileRecord } from '@/api/system/file/index'
import type {
  AnnounceMyVO,
  AnnounceSaveDTO,
  AnnounceVO,
  ChatGroup,
  ChatGroupLogItem,
  ChatMessage,
  ChatUser,
  GroupMember,
} from '@/types/message'

export interface MessageSummary {
  inboxCount?: number
  announceCount?: number
  chatCount?: number
  noticeCount?: number
  total?: number
}

export interface AnnounceDetail {
  id: number
  title: string
  content: string
  noticeType?: number
  createName?: string
  createTime?: string
  createAvatar?: string
}

export interface MessagePageQuery extends PageQuery {
  title?: string
  noticeType?: number | null
  status?: number | null
  isRead?: number | null
}

export interface AnnounceSendLog {
  channel?: string
  targetCount?: number
  successCount?: number
  sendTime?: string
}

export function getMessageSummary() {
  return get<MessageSummary>('/system/message/summary')
}

export function getAnnouncePage(params: MessagePageQuery) {
  return get<PageResult<AnnounceVO>>('/system/announce/page', params)
}

export function getMyAnnounce(params?: MessagePageQuery) {
  return get<PageResult<AnnounceMyVO>>('/system/announce/my', params)
}

export function createAnnounce(data: AnnounceSaveDTO) {
  return post('/system/announce', data)
}

export function updateAnnounce(data: AnnounceSaveDTO) {
  return put('/system/announce', data)
}

export function deleteAnnounce(id: number) {
  return del(`/system/announce/${id}`)
}

export function publishAnnounce(id: number) {
  return post(`/system/announce/${id}/publish`)
}

export function getAnnounceSendLogs(id: number) {
  return get<AnnounceSendLog[]>(`/system/announce/${id}/send-logs`)
}

export function getRecycleAnnouncePage(params: { pageNo: number; pageSize: number; title?: string }) {
  return get<PageResult<AnnounceVO>>('/system/announce/recycle/page', params)
}

export function restoreAnnounce(id: number) {
  return put('/system/announce/restore', null, { params: { id } })
}

export function deleteAnnouncePermanent(id: number) {
  return del('/system/announce/delete-permanent', { params: { id } })
}

export function getAnnounceDetail(id: number) {
  return get<AnnounceDetail>(`/system/announce/${id}`)
}

export function getAnnounceAdminDetail(id: number) {
  return get<AnnounceVO>(`/system/announce/${id}`)
}

export function readAnnounce(id: number) {
  return post<boolean>(`/system/announce/${id}/read`)
}

export function readAllAnnounce() {
  return post<boolean>('/system/announce/read-all')
}

export function getChatUsers() {
  return get<ChatUser[]>('/system/chat/users')
}

export interface CreateGroupPayload {
  name: string
  memberIds: number[]
}

export interface UpdateGroupPayload {
  id: number
  name?: string
  announcement?: string
}

export function createChatGroup(data: CreateGroupPayload) {
  return post<ChatGroup>('/system/chat/group/create', data)
}

export function updateChatGroup(data: UpdateGroupPayload) {
  return put('/system/chat/group/update', data)
}

export function addGroupMembers(groupId: number, userIds: number[]) {
  return post(`/system/chat/group/${groupId}/members`, { userIds })
}

export function getChatGroups() {
  return get<ChatGroup[]>('/system/chat/group/list')
}

export function getChatHistory(targetId: number, params?: PageQuery) {
  return get<PageResult<ChatMessage>>(`/system/chat/history/${targetId}`, params)
}

export function getGroupMessages(groupId: number, params?: PageQuery) {
  return get<PageResult<ChatMessage>>(`/system/chat/group/${groupId}/messages`, params)
}

export function readChat(senderId: number) {
  return post<boolean>(`/system/chat/read/${senderId}`)
}

export interface ChatSendPayload {
  receiverId?: number
  content: string
  msgType?: number
  mentionIds?: number[]
}

export function sendChat(data: ChatSendPayload & { receiverId: number }) {
  return post<ChatMessage>('/system/chat/send', data)
}

export function sendGroupMessage(groupId: number, data: ChatSendPayload) {
  return post<ChatMessage>(`/system/chat/group/${groupId}/message`, data)
}

export function uploadChatImage(filePath: string) {
  return uploadFile<FileRecord>({ url: '/system/chat/upload/image', filePath })
}

export function uploadChatFile(filePath: string) {
  return uploadFile<FileRecord>({ url: '/system/chat/upload/file', filePath })
}

export function recallPrivateMessage(messageId: number) {
  return post<ChatMessage>(`/system/chat/recall/${messageId}`)
}

export function recallGroupMessage(groupId: number, messageId: number) {
  return post<ChatMessage>(`/system/chat/group/${groupId}/message/${messageId}/recall`)
}

export function sendTypingSignal(targetUserId: number) {
  return post(`/system/chat/typing/${targetUserId}`)
}

export function getGroupMembers(groupId: number) {
  return get<GroupMember[]>(`/system/chat/group/${groupId}/members`)
}

export function getGroupDetail(groupId: number) {
  return get<ChatGroup>(`/system/chat/group/${groupId}`)
}

export function markGroupAnnouncementRead(groupId: number) {
  return post<boolean>(`/system/chat/group/${groupId}/announcement/read`)
}

export function setGroupNotifyMuted(groupId: number, muted = true) {
  return post<boolean>(`/system/chat/group/${groupId}/notify-muted`, null, { params: { muted } })
}

export function getGroupLogs(groupId: number) {
  return get<ChatGroupLogItem[]>(`/system/chat/group/${groupId}/logs`)
}

export function removeGroupMember(groupId: number, memberUserId: number) {
  return del(`/system/chat/group/${groupId}/members/${memberUserId}`)
}

export function setGroupAdmin(groupId: number, memberUserId: number, admin = true) {
  return post(`/system/chat/group/${groupId}/admin/${memberUserId}`, null, { params: { admin } })
}

export function setGroupMuted(groupId: number, memberUserId: number, muted = true) {
  return post(`/system/chat/group/${groupId}/mute/${memberUserId}`, null, { params: { muted } })
}

export function transferGroupOwner(groupId: number, newOwnerId: number) {
  return post(`/system/chat/group/${groupId}/transfer/${newOwnerId}`)
}

export function quitGroup(groupId: number) {
  return post(`/system/chat/group/${groupId}/quit`)
}

export function dissolveGroup(groupId: number) {
  return del(`/system/chat/group/${groupId}`)
}

export function canCreateChatGroup() {
  return get<boolean>('/system/chat/can-create-group')
}
