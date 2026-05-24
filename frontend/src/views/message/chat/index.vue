<template>
  <div class="chat-page">
    <el-card
      class="chat-card"
      :class="{ fullscreen: isFullscreen }"
      :style="{ width: cardWidth + 'px', height: cardHeight + 'px' }"
    >
      <div class="card-resize-handle" @mousedown="startCardResize" />
      <div class="chat-wrapper">
        <div class="chat-sidebar" :style="{ width: sidebarWidth + 'px' }">
          <div class="sidebar-header">
            <el-input v-model="searchKeyword" placeholder="搜索" clearable size="small" :prefix-icon="Search" />
          </div>
          <div class="sidebar-tabs">
            <span
              class="tab-item tab-with-dot"
              :class="{ active: chatMode === 'private' }"
              @click="chatMode = 'private'"
            >
              私聊
              <span v-if="privateTabUnread" class="tab-unread-dot" />
            </span>
            <span
              class="tab-item tab-with-dot"
              :class="{ active: chatMode === 'group' }"
              @click="chatMode = 'group'"
            >
              群聊
              <span v-if="groupTabUnread" class="tab-unread-dot" />
            </span>
            <el-button v-if="chatMode === 'group'" link type="primary" size="small" @click="showCreateGroup = true">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
          <div v-if="chatMode === 'private'" class="contact-list">
            <div
              v-for="user in filteredUsers"
              :key="user.id"
              class="contact-item"
              :class="{ active: selectedUser?.id === user.id, blocked: user.isBlocked }"
              @click="selectUser(user)"
            >
              <div class="avatar-wrap">
                <el-avatar :size="36">{{ (user.nickname || user.username || 'U').charAt(0) }}</el-avatar>
                <span v-if="onlineMap[user.id] && !user.isBlocked" class="online-dot" />
              </div>
              <div class="contact-info">
                <div class="contact-row">
                  <span class="contact-name">{{ user.nickname || user.username }}</span>
                  <el-badge v-if="user.unreadCount" :value="user.unreadCount" class="unread-badge" />
                  <span v-if="user.lastMessageTime" class="contact-time">{{ formatListTime(user.lastMessageTime) }}</span>
                </div>
                <div class="contact-msg">
                  <span v-if="user.isBlocked" class="blocked">已屏蔽</span>
                  <span v-else>{{ user.lastMessage || '暂无消息' }}</span>
                </div>
              </div>
            </div>
            <el-empty v-if="!filteredUsers.length" description="暂无联系人" :image-size="60" />
          </div>
          <div v-else class="contact-list">
            <div
              v-for="group in filteredGroups"
              :key="group.id"
              class="contact-item"
              :class="{ active: selectedGroup?.id === group.id }"
              @click="selectGroup(group)"
            >
              <el-avatar :size="36" style="background:#52c41a">{{ (group.name || 'G').charAt(0) }}</el-avatar>
              <div class="contact-info">
                <div class="contact-row">
                  <span class="contact-name">{{ group.name }}</span>
                  <el-badge
                    v-if="messageStore.getGroupUnread(group.id)"
                    :value="messageStore.getGroupUnread(group.id)"
                    class="unread-badge"
                  />
                  <span class="member-badge">{{ group.memberCount }}人</span>
                </div>
                <div class="contact-msg">{{ group.lastMessage || '暂无消息' }}</div>
              </div>
            </div>
            <el-empty v-if="!filteredGroups.length" description="暂无群组" :image-size="60" />
          </div>
        </div>
        <div class="resize-handle" @mousedown="startResize" />
        <div class="chat-main">
          <template v-if="selectedUser && !selectedGroup">
            <div class="chat-header">
              <el-avatar :size="40">{{ (selectedUser.nickname || 'U').charAt(0) }}</el-avatar>
              <div class="header-info">
                <div class="header-name">{{ selectedUser.nickname || selectedUser.username }}</div>
                <div class="header-status">
                  <span :class="onlineMap[selectedUser.id] ? 'online' : 'offline'">
                    {{ onlineMap[selectedUser.id] ? '在线' : '离线' }}
                  </span>
                </div>
              </div>
              <div class="header-actions">
                <el-popover placement="bottom-end" :width="300" trigger="click">
                  <template #reference>
                    <el-button link title="搜索消息"><el-icon><Search /></el-icon></el-button>
                  </template>
                  <div class="search-message-panel">
                    <el-input v-model="messageSearchKeyword" placeholder="输入关键词搜索消息" clearable size="small" :prefix-icon="Search" />
                    <div v-if="messageSearchKeyword.trim()" class="search-results">
                      <div v-for="msg in searchMessageResults" :key="msg.id" class="search-result-item" @click="scrollToMessage(msg.id)">
                        <span class="result-content">{{ formatSearchPreview(msg.content) }}</span>
                        <span class="result-time">{{ formatTime(msg.sendTime) }}</span>
                      </div>
                      <el-empty v-if="!searchMessageResults.length" description="未找到相关消息" :image-size="48" />
                    </div>
                  </div>
                </el-popover>
                <el-dropdown trigger="click" @command="handlePrivateAction">
                  <el-button link><el-icon><MoreFilled /></el-icon></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="clear">清空聊天记录</el-dropdown-item>
                      <el-dropdown-item v-if="selectedUser.isBlocked" command="unblock">解除屏蔽</el-dropdown-item>
                      <el-dropdown-item v-else command="block" divided>屏蔽用户</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button link @click="isFullscreen = !isFullscreen"><el-icon><FullScreen /></el-icon></el-button>
              </div>
            </div>
            <div ref="messageListRef" class="message-list" v-loading="loadingHistory">
              <div v-for="msg in messages" :key="msg.id" :data-msg-id="msg.id" class="message-item" :class="{ self: msg.senderId === currentUserId }">
                <el-avatar :size="32">{{ (msg.senderName || 'U').charAt(0) }}</el-avatar>
                <div class="message-body">
                  <div v-if="msg.msgType === 2" class="msg-image" @click="openImagePreview(msg.content)">
                    <img :src="msg.content" alt="图片" />
                  </div>
                  <div v-else class="msg-bubble">{{ msg.content }}</div>
                  <div class="message-time">{{ formatTime(msg.sendTime) }}</div>
                </div>
              </div>
              <el-empty v-if="!messages.length && !loadingHistory" description="暂无消息" :image-size="60" />
            </div>
            <div class="chat-input">
              <div class="input-toolbar">
                <el-popover placement="top-start" :width="260" trigger="click" popper-class="emoji-popover">
                  <template #reference>
                    <el-button link class="toolbar-btn"><el-icon><Sunny /></el-icon></el-button>
                  </template>
                  <div class="emoji-grid">
                    <span v-for="e in emojis" :key="e" class="emoji" @click="inputContent += e">{{ e }}</span>
                  </div>
                </el-popover>
                <el-upload :show-file-list="false" accept="image/*" :http-request="handleUploadImage">
                  <el-button link class="toolbar-btn"><el-icon><Picture /></el-icon></el-button>
                </el-upload>
              </div>
              <div class="chat-input-row">
                <el-input
                  v-model="inputContent"
                  type="textarea"
                  :autosize="{ minRows: 1, maxRows: 4 }"
                  resize="none"
                  placeholder="输入消息，按 Enter 发送"
                  @keydown.enter.exact.prevent="handleSend"
                />
                <el-button
                  type="primary"
                  class="send-btn"
                  :disabled="!inputContent.trim() || selectedUser.isBlocked"
                  @click="handleSend"
                >
                  发送
                </el-button>
              </div>
            </div>
          </template>
          <template v-else-if="selectedGroup">
            <div class="chat-header">
              <el-avatar :size="40" style="background:#52c41a">{{ (selectedGroup.name || 'G').charAt(0) }}</el-avatar>
              <div class="header-info">
                <div class="header-name">{{ selectedGroup.name }}</div>
                <div class="header-status">{{ selectedGroup.memberCount }}人</div>
              </div>
              <div class="header-actions">
                <el-popover placement="bottom-end" :width="300" trigger="click">
                  <template #reference><el-button link title="搜索消息"><el-icon><Search /></el-icon></el-button></template>
                  <div class="search-message-panel">
                    <el-input v-model="messageSearchKeyword" placeholder="输入关键词搜索消息" clearable size="small" :prefix-icon="Search" />
                    <div v-if="messageSearchKeyword.trim()" class="search-results">
                      <div v-for="msg in searchMessageResults" :key="msg.id" class="search-result-item" @click="scrollToMessage(msg.id)">
                        <span class="result-content">{{ formatSearchPreview(msg.content) }}</span>
                        <span class="result-time">{{ formatTime(msg.sendTime) }}</span>
                      </div>
                      <el-empty v-if="!searchMessageResults.length" description="未找到相关消息" :image-size="48" />
                    </div>
                  </div>
                </el-popover>
                <el-button link title="群设置" @click="openGroupDetail"><el-icon><Setting /></el-icon></el-button>
                <el-button link @click="isFullscreen = !isFullscreen"><el-icon><FullScreen /></el-icon></el-button>
              </div>
            </div>
            <div ref="groupListRef" class="message-list" v-loading="loadingHistory">
              <template v-for="msg in groupMessages" :key="msg.id">
                <div v-if="msg.msgType === 4" class="system-msg">{{ msg.content }}</div>
                <div v-else class="message-item" :data-msg-id="msg.id" :class="{ self: msg.senderId === currentUserId }">
                  <el-avatar :size="32">{{ (msg.senderName || 'U').charAt(0) }}</el-avatar>
                  <div class="message-body">
                    <div v-if="msg.senderId !== currentUserId" class="sender-name">{{ msg.senderName }}</div>
                    <div v-if="msg.msgType === 2" class="msg-image" @click="openImagePreview(msg.content)">
                      <img :src="msg.content" alt="图片" />
                    </div>
                    <div v-else class="msg-bubble">{{ msg.content }}</div>
                    <div class="message-time">{{ formatTime(msg.sendTime) }}</div>
                  </div>
                </div>
              </template>
              <el-empty v-if="!groupMessages.length && !loadingHistory" description="暂无消息" :image-size="60" />
            </div>
            <div class="chat-input">
              <div class="input-toolbar">
                <el-popover placement="top-start" :width="260" trigger="click" popper-class="emoji-popover">
                  <template #reference>
                    <el-button link class="toolbar-btn"><el-icon><Sunny /></el-icon></el-button>
                  </template>
                  <div class="emoji-grid">
                    <span v-for="e in emojis" :key="e" class="emoji" @click="groupInput += e">{{ e }}</span>
                  </div>
                </el-popover>
                <el-upload :show-file-list="false" accept="image/*" :http-request="handleUploadGroupImage">
                  <el-button link class="toolbar-btn"><el-icon><Picture /></el-icon></el-button>
                </el-upload>
              </div>
              <div class="chat-input-row">
                <el-input
                  v-model="groupInput"
                  type="textarea"
                  :autosize="{ minRows: 1, maxRows: 4 }"
                  resize="none"
                  placeholder="输入消息，按 Enter 发送"
                  @keydown.enter.exact.prevent="handleGroupSend"
                />
                <el-button type="primary" class="send-btn" :disabled="!groupInput.trim()" @click="handleGroupSend">
                  发送
                </el-button>
              </div>
            </div>
          </template>
          <div v-else class="chat-empty"><el-empty description="请选择一个聊天开始对话" /></div>
        </div>
      </div>
    </el-card>
    <el-dialog v-model="previewVisible" title="图片预览" width="auto">
      <img :src="previewUrl" alt="图片" style="max-width:100%;max-height:70vh" />
    </el-dialog>
    <el-dialog v-model="showCreateGroup" title="创建群组" width="520px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="群名称"><el-input v-model="newGroupName" maxlength="20" placeholder="请输入群名称" /></el-form-item>
        <el-form-item label="群成员">
          <el-select v-model="newGroupMembers" multiple filterable style="width:100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.nickname || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateGroup = false">取消</el-button>
        <el-button type="primary" @click="handleCreateGroup">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showGroupDetail" title="群组详情" width="600px" destroy-on-close @closed="groupLogs = []">
      <div v-if="selectedGroup" class="group-detail">
        <el-tabs v-model="groupTab" @tab-change="handleGroupTabChange">
          <el-tab-pane label="基本信息" name="info">
            <el-form label-width="80px" class="group-form">
              <el-form-item label="群名称">
                <el-input v-model="editGroupName" :disabled="!canEditGroup" />
              </el-form-item>
              <el-form-item label="群公告">
                <el-input v-model="editGroupAnnouncement" type="textarea" :rows="3" :disabled="!canEditGroup" />
              </el-form-item>
              <el-form-item v-if="canEditGroup">
                <el-button type="primary" size="small" @click="handleUpdateGroup">保存修改</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="成员管理" name="members">
            <div v-if="canEditGroup" class="member-toolbar">
              <el-select
                v-model="addMemberIds"
                multiple
                filterable
                fit-input-width
                collapse-tags
                collapse-tags-tooltip
                placeholder="选择要添加的用户"
                class="member-select"
                popper-class="member-select-popper"
              >
                <el-option v-for="u in availableAddUsers" :key="u.id" :label="u.nickname || u.username" :value="u.id" />
              </el-select>
              <el-button
                type="primary"
                size="small"
                class="member-add-btn"
                :disabled="!addMemberIds.length"
                @click="handleAddMembers"
              >
                添加成员
              </el-button>
            </div>
            <div class="member-list" v-loading="membersLoading">
              <div v-for="m in groupMembers" :key="m.id" class="member-row">
                <div class="member-row-left">
                  <span class="member-name">{{ m.nickname || m.userNickname || m.username }}</span>
                  <el-tag v-if="m.role === 2" size="small" type="warning">群主</el-tag>
                  <el-tag v-else-if="m.role === 1" size="small" type="info">管理员</el-tag>
                  <el-tag v-if="m.muted" size="small" type="danger">禁言</el-tag>
                </div>
                <el-dropdown v-if="canManageMember(m)" trigger="click" @command="(cmd: string) => handleMemberAction(cmd, m)">
                  <el-button size="small">操作</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-if="isGroupOwner && m.role === 0" command="setAdmin">设为管理员</el-dropdown-item>
                      <el-dropdown-item v-if="isGroupOwner && m.role === 1" command="removeAdmin">取消管理员</el-dropdown-item>
                      <el-dropdown-item v-if="!m.muted" command="mute">禁言</el-dropdown-item>
                      <el-dropdown-item v-else command="unmute">解除禁言</el-dropdown-item>
                      <el-dropdown-item v-if="isGroupOwner && m.role !== 2" command="transfer">转让群主</el-dropdown-item>
                      <el-dropdown-item command="remove" divided>移除成员</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <el-empty v-if="!groupMembers.length && !membersLoading" description="暂无成员" :image-size="48" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="群聊日志" name="logs">
            <div class="group-log-panel" v-loading="logsLoading">
              <el-timeline v-if="groupLogs.length">
                <el-timeline-item
                  v-for="log in groupLogs"
                  :key="log.id"
                  :timestamp="formatTime(log.createTime)"
                  placement="top"
                >
                  <div class="log-content">{{ log.content }}</div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无群聊日志" :image-size="60" />
            </div>
          </el-tab-pane>
        </el-tabs>
        <el-divider />
        <div class="group-btns action-buttons">
          <el-button v-if="isGroupOwner" type="danger" size="small" @click="handleDissolve">解散群组</el-button>
          <el-button v-else type="warning" size="small" @click="handleQuit">退出群组</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Plus, MoreFilled, FullScreen, Sunny, Picture, Setting,
} from '@element-plus/icons-vue'
import {
  sendChat, getChatHistory, getChatUsers, readChat,
  clearChatHistory, blockUser, unblockUser,
  createChatGroup, getChatGroups, sendGroupMessage, getGroupMessages,
  getGroupMembers, getGroupLogs, quitGroup, dissolveGroup,
  updateChatGroup, addGroupMembers, removeGroupMember,
  setGroupAdmin, setGroupMuted, transferGroupOwner,
  uploadChatImage,
} from '@/api/message/index'
import { useUserStore } from '@/store/user'
import { useMessageStore } from '@/store/message'
import { onMessageWebSocket } from '@/utils/messageWebSocket'
import type { ChatUser, ChatGroup, ChatMessage, GroupMember, ChatGroupLogItem } from '@/types/message'

const route = useRoute()
const userStore = useUserStore()
const messageStore = useMessageStore()
const currentUserId = computed(() => userStore.userInfo?.userId)

const chatMode = ref('private')
const searchKeyword = ref('')
const users = ref<ChatUser[]>([])
const groups = ref<ChatGroup[]>([])
const selectedUser = ref<ChatUser | null>(null)
const selectedGroup = ref<ChatGroup | null>(null)
const messages = ref<ChatMessage[]>([])
const groupMessages = ref<ChatMessage[]>([])
const inputContent = ref('')
const groupInput = ref('')
const loadingHistory = ref(false)
const onlineMap = ref<Record<number, boolean>>({})
const sidebarWidth = ref(parseInt(localStorage.getItem('chat-sidebar-width') || '260', 10))
const cardWidth = ref(parseInt(localStorage.getItem('chat-card-width') || '1000', 10))
const cardHeight = ref(parseInt(localStorage.getItem('chat-card-height') || '600', 10))
const isFullscreen = ref(false)
const messageSearchKeyword = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const groupListRef = ref<HTMLElement | null>(null)
const previewVisible = ref(false)
const previewUrl = ref('')
const showCreateGroup = ref(false)
const newGroupName = ref('')
const newGroupMembers = ref<number[]>([])
const showGroupDetail = ref(false)
const groupMembers = ref<GroupMember[]>([])
const groupLogs = ref<ChatGroupLogItem[]>([])
const groupTab = ref('info')
const membersLoading = ref(false)
const logsLoading = ref(false)
const editGroupName = ref('')
const editGroupAnnouncement = ref('')
const addMemberIds = ref<number[]>([])
let offWs: (() => void) | null = null

const emojis = [
  '😊', '😂', '🥰', '😎', '🤔', '👍', '🎉', '❤️', '😢', '😡', '🤗', '😱', '🥳', '😴', '🤝',
  '😀', '😁', '😅', '🤣', '😇', '🙂', '🙃', '😉', '😍', '🥺', '😭', '😤', '👏', '🙏', '💪', '🔥',
]

const filteredUsers = computed(() => {
  const kw = searchKeyword.value.trim()
  if (!kw) return users.value
  return users.value.filter(u =>
    (u.nickname || '').includes(kw) || (u.username || '').includes(kw)
  )
})

const filteredGroups = computed(() => {
  const kw = searchKeyword.value.trim()
  if (!kw) return groups.value
  return groups.value.filter(g => (g.name || '').includes(kw))
})

const privateTabUnread = computed(() => {
  const fromList = users.value.reduce((sum, u) => sum + (Number(u.unreadCount) || 0), 0)
  return Math.max(fromList, messageStore.privateChatCount)
})

const groupTabUnread = computed(() => messageStore.groupChatUnread > 0)

const searchMessageResults = computed(() => {
  const keyword = messageSearchKeyword.value.trim().toLowerCase()
  if (!keyword) return []
  const list = selectedGroup.value ? groupMessages.value : messages.value
  return list.filter(m =>
    m.msgType === 1 && (m.content || '').toLowerCase().includes(keyword)
  )
})

const userOptions = computed(() =>
  users.value.filter(u => u.id !== currentUserId.value)
)

const isGroupOwner = computed(() =>
  selectedGroup.value?.ownerId === currentUserId.value
)

const myGroupMember = computed(() =>
  groupMembers.value.find(m => m.userId === currentUserId.value)
)

const canEditGroup = computed(() => {
  const role = myGroupMember.value?.role
  return role != null && role >= 1
})

const availableAddUsers = computed(() => {
  const memberIds = groupMembers.value.map(m => m.userId)
  return users.value.filter(u => !memberIds.includes(u.id) && u.id !== currentUserId.value)
})

async function loadUsers() {
  const res = await getChatUsers()
  users.value = res.data ?? []
  users.value.forEach((u) => {
    onlineMap.value[u.id] = !!u.online
  })
}

async function loadGroups() {
  const res = await getChatGroups()
  groups.value = res.data ?? []
}

async function selectUser(user: ChatUser) {
  selectedGroup.value = null
  selectedUser.value = user
  messageStore.setActiveChatTarget({ type: 'user', id: user.id })
  messageSearchKeyword.value = ''
  messages.value = []
  await loadMessages()
  await readChat(user.id)
  user.unreadCount = 0
  await messageStore.refreshSummary()
}

async function loadMessages() {
  if (!selectedUser.value) return
  loadingHistory.value = true
  try {
    const res = await getChatHistory(selectedUser.value.id, { pageNo: 1, pageSize: 50 })
    messages.value = res.data?.list || []
    await nextTick()
    scrollBottom(messageListRef)
  } finally {
    loadingHistory.value = false
  }
}

async function selectGroup(group: ChatGroup) {
  selectedUser.value = null
  selectedGroup.value = group
  messageStore.setActiveChatTarget({ type: 'group', id: group.id })
  messageStore.clearGroupUnread(group.id)
  messageSearchKeyword.value = ''
  groupMessages.value = []
  await loadGroupMessages()
  await loadGroupMembersList()
}

async function loadGroupMessages() {
  if (!selectedGroup.value) return
  loadingHistory.value = true
  try {
    const res = await getGroupMessages(selectedGroup.value.id, { pageNo: 1, pageSize: 50 })
    groupMessages.value = res.data?.list || []
    await nextTick()
    scrollBottom(groupListRef)
  } finally {
    loadingHistory.value = false
  }
}

async function loadGroupMembersList() {
  if (!selectedGroup.value) return
  membersLoading.value = true
  try {
    const res = await getGroupMembers(selectedGroup.value.id)
    groupMembers.value = res.data ?? []
  } finally {
    membersLoading.value = false
  }
}

async function loadGroupLogs() {
  if (!selectedGroup.value) return
  logsLoading.value = true
  try {
    const res = await getGroupLogs(selectedGroup.value.id)
    groupLogs.value = res.data ?? []
  } finally {
    logsLoading.value = false
  }
}

function handleGroupTabChange(name: string | number) {
  if (name === 'logs') loadGroupLogs()
  if (name === 'members') loadGroupMembersList()
}

async function handleSend() {
  if (!inputContent.value.trim() || !selectedUser.value || selectedUser.value.isBlocked) return
  const res = await sendChat({
    receiverId: selectedUser.value.id,
    content: inputContent.value.trim(),
    msgType: 1,
  })
  const message = res.data
  if (!message) return
  messages.value.push(message)
  updateUserLastMsg(selectedUser.value.id, message.content, 1)
  inputContent.value = ''
  await nextTick()
  scrollBottom(messageListRef)
}

async function handleGroupSend() {
  if (!groupInput.value.trim() || !selectedGroup.value) return
  const res = await sendGroupMessage(selectedGroup.value.id, {
    content: groupInput.value.trim(),
    msgType: 1,
  })
  const message = res.data
  if (!message) return
  groupMessages.value.push(message)
  groupInput.value = ''
  await nextTick()
  scrollBottom(groupListRef)
}

async function handleUploadImage({ file }: { file: File }) {
  if (!selectedUser.value) return
  const res = await uploadChatImage(file)
  const url = res.data?.url
  if (!url) {
    ElMessage.error('图片上传失败')
    return
  }
  const msg = await sendChat({ receiverId: selectedUser.value.id, content: url, msgType: 2 })
  if (!msg.data) return
  messages.value.push(msg.data)
  updateUserLastMsg(selectedUser.value.id, '[图片]', 2)
  await nextTick()
  scrollBottom(messageListRef)
}

async function handleUploadGroupImage({ file }: { file: File }) {
  if (!selectedGroup.value) return
  const res = await uploadChatImage(file)
  const url = res.data?.url
  if (!url) {
    ElMessage.error('图片上传失败')
    return
  }
  const msg = await sendGroupMessage(selectedGroup.value.id, { content: url, msgType: 2 })
  groupMessages.value.push(msg.data)
  await nextTick()
  scrollBottom(groupListRef)
}

async function handleCreateGroup() {
  if (!newGroupName.value.trim() || !newGroupMembers.value.length) {
    ElMessage.warning('请输入群名称并选择成员')
    return
  }
  const res = await createChatGroup({ name: newGroupName.value.trim(), memberIds: newGroupMembers.value })
  if (!res.data) return
  ElMessage.success('群组创建成功')
  showCreateGroup.value = false
  newGroupName.value = ''
  newGroupMembers.value = []
  chatMode.value = 'group'
  await loadGroups()
  selectGroup(res.data)
}

async function openGroupDetail() {
  editGroupName.value = selectedGroup.value?.name || ''
  editGroupAnnouncement.value = selectedGroup.value?.announcement || ''
  addMemberIds.value = []
  groupTab.value = 'info'
  showGroupDetail.value = true
  await loadGroupMembersList()
}

async function handleQuit() {
  const group = selectedGroup.value
  if (!group) return
  await ElMessageBox.confirm('确定要退出该群组吗？', '提示', { type: 'warning' })
  await quitGroup(group.id)
  ElMessage.success('已退出群组')
  showGroupDetail.value = false
  selectedGroup.value = null
  groupMessages.value = []
  await loadGroups()
}

async function handleDissolve() {
  const group = selectedGroup.value
  if (!group) return
  await ElMessageBox.confirm('确定要解散该群组吗？此操作不可撤销！', '警告', { type: 'warning' })
  await dissolveGroup(group.id)
  ElMessage.success('群组已解散')
  showGroupDetail.value = false
  selectedGroup.value = null
  groupMessages.value = []
  await loadGroups()
}

async function handlePrivateAction(cmd: string) {
  if (!selectedUser.value) return
  if (cmd === 'clear') {
    await ElMessageBox.confirm('确定要清空与该用户的聊天记录吗？', '提示', { type: 'warning' })
    await clearChatHistory(selectedUser.value.id)
    messages.value = []
    ElMessage.success('聊天记录已清空')
  } else if (cmd === 'block') {
    await blockUser(selectedUser.value.id)
    selectedUser.value.isBlocked = true
    ElMessage.success('已屏蔽该用户')
  } else if (cmd === 'unblock') {
    await unblockUser(selectedUser.value.id)
    selectedUser.value.isBlocked = false
    ElMessage.success('已解除屏蔽')
  }
}

function updateGroupLastMsg(groupId: number, senderName: string | undefined, content: string | undefined, msgType: number) {
  const g = groups.value.find(x => x.id === groupId)
  if (g) {
    const display = msgType === 2 ? '[图片]' : content
    g.lastMessage = `${senderName || ''}: ${display}`
  }
}

async function handleUpdateGroup() {
  if (!selectedGroup.value) return
  await updateChatGroup({
    id: selectedGroup.value.id,
    name: editGroupName.value,
    announcement: editGroupAnnouncement.value,
  })
  selectedGroup.value.name = editGroupName.value
  ElMessage.success('群组信息已更新')
  await loadGroups()
  if (groupTab.value === 'logs') await loadGroupLogs()
}

async function handleAddMembers() {
  const group = selectedGroup.value
  if (!group || !addMemberIds.value.length) return
  await addGroupMembers(group.id, addMemberIds.value)
  ElMessage.success('成员已添加')
  addMemberIds.value = []
  await loadGroupMembersList()
  await loadGroups()
  if (groupTab.value === 'logs') await loadGroupLogs()
}

function canManageMember(m: GroupMember) {
  if (m.userId === currentUserId.value) return false
  const me = myGroupMember.value
  if (!me) return false
  if (me.role === 2) return m.role !== 2
  if (me.role === 1) return m.role === 0
  return false
}

async function handleMemberAction(cmd: string, m: GroupMember) {
  if (!selectedGroup.value) return
  const gid = selectedGroup.value.id
  if (cmd === 'setAdmin') await setGroupAdmin(gid, m.userId, true)
  if (cmd === 'removeAdmin') await setGroupAdmin(gid, m.userId, false)
  if (cmd === 'mute') await setGroupMuted(gid, m.userId, true)
  if (cmd === 'unmute') await setGroupMuted(gid, m.userId, false)
  if (cmd === 'transfer') {
    await ElMessageBox.confirm(`确定要转让群主给 ${m.userNickname || m.nickname} 吗？`, '警告', { type: 'warning' })
    await transferGroupOwner(gid, m.userId)
    selectedGroup.value.ownerId = m.userId
  }
  if (cmd === 'remove') {
    await ElMessageBox.confirm('确定要移除该成员吗？', '提示', { type: 'warning' })
    await removeGroupMember(gid, m.userId)
  }
  ElMessage.success('操作成功')
  await loadGroupMembersList()
  await loadGroups()
  if (groupTab.value === 'logs') await loadGroupLogs()
}

function updateUserLastMsg(userId: number, content: string | undefined, msgType: number) {
  const u = users.value.find(x => x.id === userId)
  if (u) u.lastMessage = msgType === 2 ? '[图片]' : content
}

function scrollBottom(refEl: { value: HTMLElement | null }) {
  const el = refEl.value
  if (el) el.scrollTop = el.scrollHeight
}

function formatSearchPreview(content: string | undefined) {
  const text = content || ''
  return text.length > 30 ? `${text.slice(0, 30)}...` : text
}

function scrollToMessage(msgId: number) {
  const container = selectedGroup.value ? groupListRef.value : messageListRef.value
  const msgElement = container?.querySelector(`[data-msg-id="${msgId}"]`)
  if (msgElement) {
    msgElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    msgElement.classList.add('message-highlight')
    setTimeout(() => msgElement.classList.remove('message-highlight'), 2000)
  }
  messageSearchKeyword.value = ''
}

function startCardResize(e: MouseEvent) {
  e.preventDefault()
  const startX = e.clientX
  const startY = e.clientY
  const startWidth = cardWidth.value
  const startHeight = cardHeight.value
  const onMove = (ev: MouseEvent) => {
    cardWidth.value = Math.min(Math.max(startWidth + ev.clientX - startX, 700), window.innerWidth - 100)
    cardHeight.value = Math.min(Math.max(startHeight + ev.clientY - startY, 400), window.innerHeight - 150)
  }
  const onUp = () => {
    localStorage.setItem('chat-card-width', String(cardWidth.value))
    localStorage.setItem('chat-card-height', String(cardHeight.value))
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
  document.body.style.cursor = 'nwse-resize'
  document.body.style.userSelect = 'none'
}

function formatTime(time: string | number | undefined) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatListTime(time: string | number | undefined) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const target = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  if (target.getTime() === today.getTime()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return `${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getDate().toString().padStart(2, '0')}`
}

function startResize(e: MouseEvent) {
  const startX = e.clientX
  const startW = sidebarWidth.value
  const onMove = (ev: MouseEvent) => {
    sidebarWidth.value = Math.min(Math.max(startW + ev.clientX - startX, 200), 400)
  }
  const onUp = () => {
    localStorage.setItem('chat-sidebar-width', String(sidebarWidth.value))
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

function openImagePreview(url: string | undefined) {
  previewUrl.value = url || ''
  previewVisible.value = true
}

function setupWs() {
  offWs = onMessageWebSocket((data) => {
    if (data.type === 'chat' && data.senderId != null) {
      const senderId = data.senderId
      updateUserLastMsg(senderId, data.content, data.msgType || 1)
      const u = users.value.find(x => x.id === senderId)
      if (u && selectedUser.value?.id !== senderId) {
        u.unreadCount = (u.unreadCount || 0) + 1
      }
      if (selectedUser.value?.id === senderId) {
        messages.value.push({
          id: Date.now(),
          senderId,
          senderName: data.senderName,
          content: data.content,
          msgType: data.msgType || 1,
          sendTime: new Date().toISOString(),
        })
        nextTick(() => scrollBottom(messageListRef))
        readChat(senderId).then(() => messageStore.refreshSummary())
      }
    }
    if (data.type === 'groupChat' && data.groupId != null) {
      const groupId = data.groupId
      updateGroupLastMsg(groupId, data.senderName, data.content, data.msgType || 1)
      if (selectedGroup.value?.id === groupId) {
        groupMessages.value.push({
          id: Date.now(),
          groupId,
          senderId: data.senderId,
          senderName: data.senderName,
          content: data.content,
          msgType: data.msgType || 1,
          sendTime: new Date().toISOString(),
        })
        nextTick(() => scrollBottom(groupListRef))
      }
    }
  })
}

onMounted(async () => {
  await loadUsers()
  await loadGroups()
  setupWs()
  await applyRouteQuery()
})

watch(
  () => [route.query.userId, route.query.groupId],
  () => {
    applyRouteQuery()
  },
)

async function applyRouteQuery() {
  const groupId = route.query.groupId
  if (groupId) {
    const g = groups.value.find(x => String(x.id) === String(groupId))
    if (g) {
      chatMode.value = 'group'
      await selectGroup(g)
    }
    return
  }
  const userId = route.query.userId
  if (userId) {
    const u = users.value.find(x => String(x.id) === String(userId))
    if (u) {
      chatMode.value = 'private'
      await selectUser(u)
    }
  }
}

onUnmounted(() => {
  offWs?.()
  messageStore.setActiveChatTarget(null)
})
</script>

<style scoped>
.chat-page {
  height: 100%;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-card {
  position: relative;
  width: 100%;
  max-width: none;
  min-width: 700px;
  min-height: 400px;
}

.chat-card :deep(.el-card__body) {
  padding: 0;
  height: 100%;
}

.chat-card.fullscreen {
  position: fixed;
  inset: 0;
  width: 100vw !important;
  height: 100vh !important;
  min-width: 0;
  min-height: 0;
  z-index: 1000;
  border-radius: 0;
}

.chat-card.fullscreen .card-resize-handle {
  display: none;
}

.card-resize-handle {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 16px;
  height: 16px;
  cursor: nwse-resize;
  z-index: 10;
}

.card-resize-handle::before {
  content: '';
  position: absolute;
  right: 3px;
  bottom: 3px;
  width: 8px;
  height: 8px;
  border-right: 2px solid #ccc;
  border-bottom: 2px solid #ccc;
}

.card-resize-handle:hover::before {
  border-color: var(--theme-primary, #111827);
}

.chat-wrapper {
  display: flex;
  height: 100%;
}

.chat-sidebar {
  min-width: 200px;
  max-width: 400px;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid #eee;
  background: #fff;
}

.sidebar-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
}

.tab-item {
  padding: 4px 12px;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  color: #666;
}

.tab-item.active {
  background: rgba(17, 24, 39, 0.08);
  color: var(--theme-primary, #111827);
  font-weight: 500;
}

.tab-with-dot {
  position: relative;
}

.tab-unread-dot {
  position: absolute;
  top: 0;
  right: 0;
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
  border: 1px solid #fff;
}

.contact-list {
  flex: 1;
  overflow-y: auto;
}

.contact-item {
  display: flex;
  gap: 10px;
  padding: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.contact-item:hover { background: #f0f0f0; }
.contact-item.active { background: #eef2ff; }
.contact-item.blocked { opacity: 0.7; }

.avatar-wrap { position: relative; flex-shrink: 0; }
.online-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background: #52c41a;
  border: 2px solid #fff;
  border-radius: 50%;
}

.contact-info { flex: 1; min-width: 0; }
.contact-row { display: flex; justify-content: space-between; gap: 6px; }
.contact-name { font-weight: 500; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.contact-time { font-size: 11px; color: #999; flex-shrink: 0; }
.contact-msg { font-size: 12px; color: #999; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.contact-msg .blocked { color: #f56c6c; }
.member-badge { font-size: 11px; color: #999; background: #f0f0f0; padding: 1px 6px; border-radius: 8px; }

.resize-handle {
  width: 4px;
  cursor: col-resize;
  flex-shrink: 0;
}
.resize-handle:hover { background: #e8e8e8; }

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.header-info { flex: 1; }
.header-name { font-size: 16px; font-weight: 500; }
.header-status { font-size: 12px; color: #999; }
.header-status .online { color: #52c41a; }

.header-actions { display: flex; gap: 4px; }

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  gap: 10px;
  max-width: 75%;
}

.message-item.self {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-item.message-highlight {
  background: #fff3cd;
  border-radius: 8px;
  padding: 4px;
  margin: -4px;
}

.message-body { display: flex; flex-direction: column; }
.message-item.self .message-body { align-items: flex-end; }

.sender-name { font-size: 12px; color: #999; margin-bottom: 4px; }

.msg-bubble {
  padding: 8px 12px;
  background: #fff;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}

.message-item.self .msg-bubble {
  background: var(--theme-primary, #111827);
  color: #fff;
}

.msg-image {
  max-width: 200px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}
.msg-image img { width: 100%; display: block; }

.msg-time,
.message-time { font-size: 11px; color: #999; margin-top: 4px; }

.system-msg {
  text-align: center;
  font-size: 12px;
  color: #999;
  padding: 4px 12px;
  background: #eee;
  border-radius: 4px;
  align-self: center;
}

.chat-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 16px 12px;
  background: #fff;
  border-top: 1px solid #eee;
}

.input-toolbar {
  display: flex;
  gap: 4px;
  align-items: center;
}

.toolbar-btn {
  padding: 4px;
  font-size: 18px;
}

.chat-input-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.chat-input-row :deep(.el-textarea) {
  flex: 1;
}

.chat-input-row :deep(.el-textarea__inner) {
  min-height: 40px;
  padding: 8px 12px;
  line-height: 1.5;
}

.send-btn {
  flex-shrink: 0;
  height: 40px;
  min-width: 72px;
  padding: 0 20px;
}

.chat-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 6px;
  max-height: 220px;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 4px;
}

.emoji {
  font-size: 20px;
  cursor: pointer;
  text-align: center;
  padding: 4px;
  border-radius: 4px;
}
.emoji:hover { background: #f0f0f0; }

.group-btns {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.group-form {
  padding-top: 4px;
}

.group-log-panel {
  max-height: 360px;
  overflow-y: auto;
  padding: 4px 8px 0;
}

.log-content {
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
}

.unread-badge { margin: 0 4px; }

.member-toolbar {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 12px;
}

.member-select {
  flex: 1;
  min-width: 0;
}

.member-add-btn {
  flex-shrink: 0;
  margin-top: 1px;
}

.member-list {
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.member-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.member-row:last-child {
  border-bottom: none;
}

.member-row-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.member-name {
  font-weight: 500;
  color: #303133;
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.action-buttons .el-button {
  margin: 0;
}

.search-message-panel {
  width: 280px;
}

.search-results {
  margin-top: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  cursor: pointer;
  border-radius: 4px;
}

.search-result-item:hover {
  background: #f5f5f5;
}

.result-content {
  font-size: 13px;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-time {
  font-size: 11px;
  color: #999;
  margin-left: 8px;
  flex-shrink: 0;
}
</style>

<style>
.member-select-popper .el-select-dropdown__wrap {
  max-height: 200px;
}
</style>
