<template>
  <div class="chat-page">
    <div class="chat-shell" :class="{ 'is-fullscreen': isFullscreen }">
      <div class="chat-wrapper">
        <div class="chat-sidebar" :style="{ width: sidebarWidth + 'px' }">
          <div class="sidebar-header">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索联系人、群聊"
              clearable
              size="default"
              :prefix-icon="Search"
              class="sidebar-search"
            />
          </div>
          <div class="sidebar-tabs">
            <div class="tab-segment">
              <button
                type="button"
                class="tab-segment-item"
                :class="{ active: chatMode === 'private' }"
                @click="chatMode = 'private'"
              >
                私聊
                <span v-if="privateTabUnread" class="tab-unread-badge" />
              </button>
              <button
                type="button"
                class="tab-segment-item"
                :class="{ active: chatMode === 'group' }"
                @click="chatMode = 'group'"
              >
                群聊
                <span v-if="groupTabUnread" class="tab-unread-badge" />
              </button>
            </div>
            <el-button
              v-if="chatMode === 'group' && canCreateGroup"
              circle
              size="small"
              class="tab-add-btn"
              title="创建群聊（仅管理员）"
              @click="showCreateGroup = true"
            >
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
                <el-avatar
                  :size="40"
                  :src="contactAvatar(user)"
                >
                  {{ avatarFallback(user.nickname || user.username) }}
                </el-avatar>
                <span v-if="onlineMap[user.id] && !user.isBlocked" class="online-dot" />
              </div>
              <div class="contact-info">
                <div class="contact-row">
                  <span class="contact-name">{{ user.nickname || user.username }}</span>
                  <span v-if="user.lastMessageTime" class="contact-time">{{ formatListTime(user.lastMessageTime) }}</span>
                  <span v-if="user.unreadCount" class="contact-unread">{{ user.unreadCount > 99 ? '99+' : user.unreadCount }}</span>
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
              <el-avatar :size="40" :style="groupAvatarStyle(group.name)">{{ (group.name || 'G').charAt(0) }}</el-avatar>
              <div class="contact-info">
                <div class="contact-row">
                  <span class="contact-name">{{ group.name }}</span>
                  <span class="member-badge">{{ group.memberCount }}人</span>
                  <span
                    v-if="messageStore.getGroupUnread(group.id)"
                    class="contact-unread"
                  >{{ messageStore.getGroupUnread(group.id) > 99 ? '99+' : messageStore.getGroupUnread(group.id) }}</span>
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
              <el-avatar :size="42" :src="contactAvatar(selectedUser)">
                {{ avatarFallback(selectedUser.nickname || selectedUser.username) }}
              </el-avatar>
              <div class="header-info">
                <div class="header-name">{{ selectedUser.nickname || selectedUser.username }}</div>
                <div class="header-status">
                  <span class="status-dot" :class="onlineMap[selectedUser.id] ? 'is-online' : 'is-offline'" />
                  <span v-if="typingFromUserId === selectedUser.id">对方正在输入…</span>
                  <span v-else>{{ onlineMap[selectedUser.id] ? '在线' : '离线' }}</span>
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
                        <span class="result-content">{{ formatSearchPreview(msg.content, msg.msgType) }}</span>
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
            <div
              ref="messageListRef"
              class="message-list"
              v-loading="loadingHistory && !loadingMore"
              @scroll="onPrivateListScroll"
            >
              <div v-if="loadingMore" class="load-more-hint">加载中…</div>
              <div v-else-if="privateHasMore && messages.length" class="load-more-hint load-more-btn" @click="loadMorePrivate">
                查看更多历史消息
              </div>
              <template v-for="item in privateChatItems" :key="item.id">
                <div v-if="item.kind === 'time'" class="msg-time-divider">
                  <span>{{ item.text }}</span>
                </div>
                <div v-else-if="item.kind === 'recall'" class="system-msg">{{ item.text }}</div>
                <div
                  v-else-if="item.kind === 'message'"
                  :data-msg-id="item.message.id"
                  class="message-item"
                  :class="{
                    self: item.message.senderId === currentUserId,
                    'is-continued': !item.showAvatar,
                  }"
                  @contextmenu.prevent="openMessageMenu($event, item.message, 'private')"
                >
                  <div v-if="item.showAvatar" class="message-avatar">
                    <el-avatar :size="40" :src="messageAvatar(item.message)">
                      {{ avatarFallback(item.message.senderName) }}
                    </el-avatar>
                  </div>
                  <div v-else class="message-avatar is-placeholder" aria-hidden="true" />
                  <div class="message-body">
                    <div
                      v-if="item.message.msgType === CHAT_MSG_TYPE.IMAGE"
                      class="msg-image"
                      @click="openImagePreview(item.message.content)"
                    >
                      <img :src="item.message.content" alt="图片" />
                    </div>
                    <div
                      v-else-if="item.message.msgType === CHAT_MSG_TYPE.FILE"
                      class="msg-file"
                      @click="openChatFile(item.message.content)"
                    >
                      <el-icon class="msg-file-icon"><Document /></el-icon>
                      <span class="msg-file-name">{{ fileDisplayName(item.message.content) }}</span>
                    </div>
                    <div v-else class="msg-bubble" v-html="renderTextContent(item.message.content)" />
                  </div>
                </div>
              </template>
              <div v-if="!messages.length && !loadingHistory" class="message-list-empty">
                <p>暂无消息，发一句打个招呼吧</p>
              </div>
            </div>
            <div class="chat-input">
              <div class="chat-input-panel">
                <div class="input-toolbar">
                  <EmojiPicker @pick="(e) => (inputContent += e)" />
                  <el-upload :show-file-list="false" accept="image/*" :http-request="handleUploadImage">
                    <el-button link class="toolbar-btn" title="发送图片">
                      <ChatToolbarIcons name="image" />
                    </el-button>
                  </el-upload>
                  <el-upload :show-file-list="false" :http-request="handleUploadFile">
                    <el-button link class="toolbar-btn" title="发送文件">
                      <ChatToolbarIcons name="file" />
                    </el-button>
                  </el-upload>
                </div>
                <div class="chat-input-row">
                  <el-input
                    v-model="inputContent"
                    type="textarea"
                    :autosize="{ minRows: 2, maxRows: 5 }"
                    resize="none"
                    placeholder="输入消息…"
                    class="chat-textarea"
                    @keydown.enter.exact.prevent="handleSend"
                    @input="onPrivateInput"
                  />
                </div>
                <div class="chat-input-footer">
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
            </div>
          </template>
          <template v-else-if="selectedGroup">
            <div class="chat-header">
              <el-avatar :size="42" :style="groupAvatarStyle(selectedGroup.name)">{{ (selectedGroup.name || 'G').charAt(0) }}</el-avatar>
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
                        <span class="result-content">{{ formatSearchPreview(msg.content, msg.msgType) }}</span>
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
            <div
              ref="groupListRef"
              class="message-list"
              v-loading="loadingHistory && !loadingMore"
              @scroll="onGroupListScroll"
            >
              <div v-if="loadingMore" class="load-more-hint">加载中…</div>
              <div v-else-if="groupHasMore && groupMessages.length" class="load-more-hint load-more-btn" @click="loadMoreGroup">
                查看更多历史消息
              </div>
              <template v-for="item in groupChatItems" :key="item.id">
                <div v-if="item.kind === 'time'" class="msg-time-divider">
                  <span>{{ item.text }}</span>
                </div>
                <div v-else-if="item.kind === 'system'" class="system-msg">{{ item.text }}</div>
                <div v-else-if="item.kind === 'recall'" class="system-msg">{{ item.text }}</div>
                <div
                  v-else-if="item.kind === 'message'"
                  :data-msg-id="item.message.id"
                  class="message-item"
                  :class="{
                    self: item.message.senderId === currentUserId,
                    'is-continued': !item.showAvatar,
                  }"
                  @contextmenu.prevent="openMessageMenu($event, item.message, 'group')"
                >
                  <div v-if="item.showAvatar" class="message-avatar">
                    <el-avatar :size="40" :src="messageAvatar(item.message)">
                      {{ avatarFallback(item.message.senderName) }}
                    </el-avatar>
                  </div>
                  <div v-else class="message-avatar is-placeholder" aria-hidden="true" />
                  <div class="message-body">
                    <div
                      v-if="item.message.senderId !== currentUserId && item.showAvatar"
                      class="sender-name"
                    >
                      {{ item.message.senderName }}
                    </div>
                    <div
                      v-if="item.message.msgType === CHAT_MSG_TYPE.IMAGE"
                      class="msg-image"
                      @click="openImagePreview(item.message.content)"
                    >
                      <img :src="item.message.content" alt="图片" />
                    </div>
                    <div
                      v-else-if="item.message.msgType === CHAT_MSG_TYPE.FILE"
                      class="msg-file"
                      @click="openChatFile(item.message.content)"
                    >
                      <el-icon class="msg-file-icon"><Document /></el-icon>
                      <span class="msg-file-name">{{ fileDisplayName(item.message.content) }}</span>
                    </div>
                    <div v-else class="msg-bubble" v-html="renderTextContent(item.message.content)" />
                  </div>
                </div>
              </template>
              <div v-if="!groupMessages.length && !loadingHistory" class="message-list-empty">
                <p>暂无消息，在群里说点什么吧</p>
              </div>
            </div>
            <div class="chat-input mention-wrap">
              <ul v-if="mentionVisible" class="mention-dropdown">
                <li
                  v-for="(m, idx) in mentionCandidates"
                  :key="m.userId"
                  :class="{ active: mentionIndex === idx }"
                  @mousedown.prevent="pickMention(m)"
                >
                  <el-avatar :size="28" :src="resolveChatAvatar(m.userId, userAvatarMap, m.avatar)">
                    {{ avatarFallback(m.nickname || m.username) }}
                  </el-avatar>
                  <span>{{ m.nickname || m.username }}</span>
                </li>
                <li v-if="!mentionCandidates.length" class="mention-empty">无匹配成员</li>
              </ul>
              <div class="chat-input-panel">
                <div class="input-toolbar">
                  <EmojiPicker @pick="(e) => (groupInput += e)" />
                  <el-upload :show-file-list="false" accept="image/*" :http-request="handleUploadGroupImage">
                    <el-button link class="toolbar-btn" title="发送图片">
                      <ChatToolbarIcons name="image" />
                    </el-button>
                  </el-upload>
                  <el-upload :show-file-list="false" :http-request="handleUploadGroupFile">
                    <el-button link class="toolbar-btn" title="发送文件">
                      <ChatToolbarIcons name="file" />
                    </el-button>
                  </el-upload>
                </div>
                <div class="chat-input-row">
                  <el-input
                    ref="groupInputRef"
                    v-model="groupInput"
                    type="textarea"
                    :autosize="{ minRows: 2, maxRows: 5 }"
                    resize="none"
                    placeholder="输入消息，@ 提醒成员…"
                    class="chat-textarea"
                    @keydown="onGroupEnterKeydown"
                    @input="onGroupInput"
                  />
                </div>
                <div class="chat-input-footer">
                  <el-button type="primary" class="send-btn" :disabled="!groupInput.trim()" @click="handleGroupSend">
                    发送
                  </el-button>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="chat-empty">
            <div class="chat-empty-inner">
              <div class="chat-empty-icon" aria-hidden="true">
                <el-icon :size="48"><ChatDotRound /></el-icon>
              </div>
              <p class="chat-empty-title">企业 IM</p>
              <p class="chat-empty-desc">选择左侧联系人或群组，开始聊天</p>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      v-if="msgMenu.visible"
      class="msg-context-menu"
      :style="{ left: msgMenu.x + 'px', top: msgMenu.y + 'px' }"
      @click.stop
    >
      <button v-if="msgMenu.canRecall" type="button" class="msg-menu-item" @click="handleRecallMessage">
        撤回
      </button>
    </div>

    <el-image-viewer
      v-if="previewVisible && previewUrl"
      :url-list="[previewUrl]"
      teleported
      @close="previewVisible = false"
    />
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
import { ref, computed, onMounted, onActivated, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { InputInstance } from 'element-plus'
import {
  Search, Plus, MoreFilled, FullScreen, Setting, ChatDotRound, Document,
} from '@element-plus/icons-vue'
import ChatToolbarIcons from '@/components/chat/ChatToolbarIcons.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'
import {
  sendChat, getChatHistory, getChatUsers, readChat,
  clearChatHistory, blockUser, unblockUser,
  createChatGroup, getChatGroups, sendGroupMessage, getGroupMessages,
  getGroupMembers, getGroupLogs, quitGroup, dissolveGroup,
  updateChatGroup, addGroupMembers, removeGroupMember,
  setGroupAdmin, setGroupMuted, transferGroupOwner,
  uploadChatImage, uploadChatFile, recallPrivateMessage, recallGroupMessage,
  sendTypingSignal, canCreateChatGroup,
} from '@/api/message/index'
import { CHAT_MSG_TYPE, CHAT_PAGE_SIZE } from '@/constants/chat'
import {
  formatFilePayload, parseFilePayload, previewMessageText,
  canRecallMessage, renderMentionHtml, isRecalledMessage,
  markMessageRecalled, resolveRecallMessageId, recallNoticeText,
} from '@/utils/chat-message'
import { useUserStore } from '@/store/user'
import { useMessageStore } from '@/store/message'
import { onMessageWebSocket, type WsPushMessage } from '@/utils/messageWebSocket'
import type { ChatUser, ChatGroup, ChatMessage, GroupMember, ChatGroupLogItem } from '@/types/message'
import { avatarFallback, buildUserAvatarMap, resolveChatAvatar } from '@/utils/chat-avatar'

const route = useRoute()
const userStore = useUserStore()
const messageStore = useMessageStore()
const currentUserId = computed(() => userStore.userInfo?.userId)

const userAvatarMap = computed(() => {
  const entries: Array<{ userId: number; avatar?: string | null }> = []
  if (currentUserId.value) {
    entries.push({ userId: currentUserId.value, avatar: userStore.userInfo.avatar })
  }
  users.value.forEach((u) => entries.push({ userId: u.id, avatar: u.avatar }))
  groupMembers.value.forEach((m) => entries.push({ userId: m.userId, avatar: m.avatar }))
  return buildUserAvatarMap(entries)
})

function contactAvatar(user?: Pick<ChatUser, 'id' | 'avatar' | 'nickname' | 'username'> | null) {
  if (!user) return undefined
  return resolveChatAvatar(user.id, userAvatarMap.value, user.avatar)
}

function messageAvatar(msg: ChatMessage) {
  return resolveChatAvatar(msg.senderId, userAvatarMap.value, msg.senderAvatar)
}

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
const sidebarWidth = ref(parseInt(localStorage.getItem('chat-sidebar-width') || '280', 10))
const isFullscreen = ref(false)

const CHAT_TIME_GAP_MS = 5 * 60 * 1000
const GROUP_AVATAR_COLORS = ['#576b95', '#10aeff', '#07c160', '#fa9d3b', '#6467f0', '#354b70']

type ChatRenderItem =
  | { kind: 'time'; id: string; text: string }
  | { kind: 'system'; id: string; text: string }
  | { kind: 'recall'; id: string; text: string }
  | { kind: 'message'; id: string; message: ChatMessage; showAvatar: boolean }

const canCreateGroup = ref(false)
const privatePageNo = ref(1)
const privateTotal = ref(0)
const groupPageNo = ref(1)
const groupTotal = ref(0)
const loadingMore = ref(false)
const privateHasMore = computed(() => messages.value.length < privateTotal.value)
const groupHasMore = computed(() => groupMessages.value.length < groupTotal.value)

const typingFromUserId = ref<number | null>(null)
let typingHideTimer: ReturnType<typeof setTimeout> | null = null
let typingSendTimer: ReturnType<typeof setTimeout> | null = null

const mentionVisible = ref(false)
const mentionKeyword = ref('')
const mentionIndex = ref(0)
const groupInputRef = ref<InputInstance | null>(null)
const pendingMentionIds = ref<number[]>([])

const msgMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  message: null as ChatMessage | null,
  scope: 'private' as 'private' | 'group',
  canRecall: false,
})
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
  return list.filter(m => {
    if (isRecalledMessage(m)) return false
    const text = previewMessageText(m).toLowerCase()
    return text.includes(keyword)
  })
})

const privateChatItems = computed(() => buildChatRenderItems(messages.value))
const groupChatItems = computed(() => buildChatRenderItems(groupMessages.value, true))

const mentionCandidates = computed(() => {
  const kw = mentionKeyword.value.trim().toLowerCase()
  return groupMembers.value
    .filter(m => m.userId !== currentUserId.value)
    .filter(m => {
      const name = (m.nickname || m.userNickname || m.username || '').toLowerCase()
      return !kw || name.includes(kw)
    })
    .slice(0, 10)
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
  privatePageNo.value = 1
  privateTotal.value = 0
  typingFromUserId.value = null
  await loadMessages()
  await readChat(user.id)
  user.unreadCount = 0
  await messageStore.refreshSummary()
}

async function loadMessages() {
  if (!selectedUser.value) return
  privatePageNo.value = 1
  loadingHistory.value = true
  try {
    const res = await getChatHistory(selectedUser.value.id, { pageNo: 1, pageSize: CHAT_PAGE_SIZE })
    messages.value = res.data?.list || []
    privateTotal.value = res.data?.total ?? messages.value.length
    await nextTick()
    scrollBottom(messageListRef)
  } finally {
    loadingHistory.value = false
  }
}

async function loadMorePrivate() {
  if (!selectedUser.value || loadingMore.value || !privateHasMore.value) return
  const el = messageListRef.value
  const prevHeight = el?.scrollHeight ?? 0
  const prevTop = el?.scrollTop ?? 0
  loadingMore.value = true
  try {
    const nextPage = privatePageNo.value + 1
    const res = await getChatHistory(selectedUser.value.id, { pageNo: nextPage, pageSize: CHAT_PAGE_SIZE })
    const older = res.data?.list || []
    if (!older.length) return
    privatePageNo.value = nextPage
    privateTotal.value = res.data?.total ?? privateTotal.value
    messages.value = [...older, ...messages.value]
    await nextTick()
    if (el) {
      el.scrollTop = el.scrollHeight - prevHeight + prevTop
    }
  } finally {
    loadingMore.value = false
  }
}

function onPrivateListScroll() {
  const el = messageListRef.value
  if (!el || loadingMore.value || !privateHasMore.value) return
  if (el.scrollTop <= 80) loadMorePrivate()
}

async function selectGroup(group: ChatGroup) {
  selectedUser.value = null
  selectedGroup.value = group
  messageStore.setActiveChatTarget({ type: 'group', id: group.id })
  messageStore.clearGroupUnread(group.id)
  messageSearchKeyword.value = ''
  groupMessages.value = []
  groupPageNo.value = 1
  groupTotal.value = 0
  mentionVisible.value = false
  await loadGroupMessages()
  await loadGroupMembersList()
}

async function loadGroupMessages() {
  if (!selectedGroup.value) return
  groupPageNo.value = 1
  loadingHistory.value = true
  try {
    const res = await getGroupMessages(selectedGroup.value.id, { pageNo: 1, pageSize: CHAT_PAGE_SIZE })
    groupMessages.value = res.data?.list || []
    groupTotal.value = res.data?.total ?? groupMessages.value.length
    await nextTick()
    scrollBottom(groupListRef)
  } finally {
    loadingHistory.value = false
  }
}

async function loadMoreGroup() {
  if (!selectedGroup.value || loadingMore.value || !groupHasMore.value) return
  const el = groupListRef.value
  const prevHeight = el?.scrollHeight ?? 0
  const prevTop = el?.scrollTop ?? 0
  loadingMore.value = true
  try {
    const nextPage = groupPageNo.value + 1
    const res = await getGroupMessages(selectedGroup.value.id, { pageNo: nextPage, pageSize: CHAT_PAGE_SIZE })
    const older = res.data?.list || []
    if (!older.length) return
    groupPageNo.value = nextPage
    groupTotal.value = res.data?.total ?? groupTotal.value
    groupMessages.value = [...older, ...groupMessages.value]
    await nextTick()
    if (el) {
      el.scrollTop = el.scrollHeight - prevHeight + prevTop
    }
  } finally {
    loadingMore.value = false
  }
}

function onGroupListScroll() {
  const el = groupListRef.value
  if (!el || loadingMore.value || !groupHasMore.value) return
  if (el.scrollTop <= 80) loadMoreGroup()
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
    msgType: CHAT_MSG_TYPE.TEXT,
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
  const mentionIds = parseMentionIds(groupInput.value)
  const res = await sendGroupMessage(selectedGroup.value.id, {
    content: groupInput.value.trim(),
    msgType: CHAT_MSG_TYPE.TEXT,
    mentionIds: mentionIds.length ? mentionIds : undefined,
  })
  const message = res.data
  if (!message) return
  groupMessages.value.push(message)
  groupInput.value = ''
  mentionVisible.value = false
  pendingMentionIds.value = []
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
  const msg = await sendChat({ receiverId: selectedUser.value.id, content: url, msgType: CHAT_MSG_TYPE.IMAGE })
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
  const msg = await sendGroupMessage(selectedGroup.value.id, { content: url, msgType: CHAT_MSG_TYPE.IMAGE })
  if (msg.data) groupMessages.value.push(msg.data)
  await nextTick()
  scrollBottom(groupListRef)
}

async function handleUploadFile({ file }: { file: File }) {
  if (!selectedUser.value) return
  try {
    const res = await uploadChatFile(file)
    if (!res.data?.url) {
      ElMessage.error('文件上传失败')
      return
    }
    const payload = formatFilePayload({ ...res.data, url: res.data.url! })
    const msg = await sendChat({
      receiverId: selectedUser.value.id,
      content: payload,
      msgType: CHAT_MSG_TYPE.FILE,
    })
    if (!msg.data) return
    messages.value.push(msg.data)
    updateUserLastMsg(selectedUser.value.id, previewMessageText(msg.data), CHAT_MSG_TYPE.FILE)
    await nextTick()
    scrollBottom(messageListRef)
  } catch (e: unknown) {
    const err = e as { message?: string }
    ElMessage.error(err?.message || '文件上传失败')
  }
}

async function handleUploadGroupFile({ file }: { file: File }) {
  if (!selectedGroup.value) return
  try {
    const res = await uploadChatFile(file)
    if (!res.data?.url) {
      ElMessage.error('文件上传失败')
      return
    }
    const payload = formatFilePayload({ ...res.data, url: res.data.url! })
    const msg = await sendGroupMessage(selectedGroup.value.id, {
      content: payload,
      msgType: CHAT_MSG_TYPE.FILE,
    })
    if (msg.data) groupMessages.value.push(msg.data)
    await nextTick()
    scrollBottom(groupListRef)
  } catch (e: unknown) {
    const err = e as { message?: string }
    ElMessage.error(err?.message || '文件上传失败')
  }
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
    const display = previewMessageText({ content, msgType })
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
  if (u) u.lastMessage = previewMessageText({ content, msgType })
}

function escapeRegExp(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function memberDisplayName(m: GroupMember) {
  return m.nickname || m.userNickname || m.username || ''
}

function parseMentionIds(text: string): number[] {
  const ids: number[] = []
  for (const m of groupMembers.value) {
    const name = memberDisplayName(m)
    if (!name) continue
    const re = new RegExp(`@${escapeRegExp(name)}(?:\\s|$|[，。！？,.!?])`)
    if (re.test(text)) ids.push(m.userId)
  }
  return [...new Set(ids)]
}

function renderTextContent(text?: string) {
  return renderMentionHtml(text || '')
}

function fileDisplayName(content?: string) {
  return parseFilePayload(content)?.name || '[文件]'
}

async function openChatFile(content?: string) {
  const file = parseFilePayload(content)
  if (!file) return
  if (file.fileId) {
    try {
      const { fetchFileBlob } = await import('@/api/system/file/index')
      const blob = await fetchFileBlob(`/system/file/download/${file.fileId}`)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = file.name || 'download'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      return
    } catch {
      /* 无文件管理权限时回退直链 */
    }
  }
  let url = file.url || ''
  if (url && !url.startsWith('http') && !url.startsWith('/api')) {
    url = url.startsWith('/') ? `/api${url}` : `/api/${url}`
  }
  if (!url) return
  const params = new URLSearchParams({ disposition: 'attachment' })
  if (file.name) params.set('filename', file.name)
  const sep = url.includes('?') ? '&' : '?'
  window.open(`${url}${sep}${params}`, '_blank', 'noopener')
}

function applyRecallInList(list: ChatMessage[], messageId?: number | string) {
  return markMessageRecalled(list, messageId ?? 0)
}

function handleRecallWs(data: WsPushMessage) {
  const messageId = resolveRecallMessageId(data)
  if (!messageId) return

  if (data.groupId != null) {
    if (selectedGroup.value?.id === data.groupId) {
      groupMessages.value = applyRecallInList(groupMessages.value, messageId)
    }
    const g = groups.value.find(x => x.id === data.groupId)
    if (g) {
      const who = data.senderId === currentUserId.value ? '你' : (data.senderName || '')
      g.lastMessage = who ? `${who}: [撤回了一条消息]` : '[撤回了一条消息]'
    }
    return
  }

  const inCurrentPrivateChat = selectedUser.value && (
    data.senderId === selectedUser.value.id || data.senderId === currentUserId.value
  )
  if (inCurrentPrivateChat) {
    messages.value = applyRecallInList(messages.value, messageId)
  }
  if (data.senderId != null) {
    const u = users.value.find(x => x.id === data.senderId)
    if (u) u.lastMessage = '[撤回了一条消息]'
  }
}

function openMessageMenu(e: MouseEvent, message: ChatMessage, scope: 'private' | 'group') {
  if (isRecalledMessage(message)) return
  if (!canRecallMessage(message, currentUserId.value)) return
  msgMenu.value = {
    visible: true,
    x: e.clientX,
    y: e.clientY,
    message,
    scope,
    canRecall: true,
  }
}

function closeMessageMenu() {
  msgMenu.value.visible = false
}

async function handleRecallMessage() {
  const { message, scope } = msgMenu.value
  closeMessageMenu()
  if (!message?.id) return
  try {
    if (scope === 'private') {
      await recallPrivateMessage(message.id)
      messages.value = applyRecallInList(messages.value, message.id)
    } else if (selectedGroup.value) {
      await recallGroupMessage(selectedGroup.value.id, message.id)
      groupMessages.value = applyRecallInList(groupMessages.value, message.id)
    }
    ElMessage.success('已撤回')
  } catch (e: unknown) {
    const err = e as { message?: string }
    ElMessage.error(err?.message || '撤回失败')
  }
}

function onPrivateInput() {
  if (!selectedUser.value || selectedUser.value.isBlocked) return
  if (typingSendTimer) clearTimeout(typingSendTimer)
  typingSendTimer = setTimeout(() => {
    sendTypingSignal(selectedUser.value!.id).catch(() => {})
  }, 400)
}

function onGroupInput() {
  const val = groupInput.value
  const at = val.lastIndexOf('@')
  if (at >= 0 && (at === 0 || /[\s\n]/.test(val.charAt(at - 1)))) {
    const tail = val.slice(at + 1)
    if (!tail.includes('\n') && !tail.includes(' ')) {
      mentionKeyword.value = tail
      mentionVisible.value = true
      mentionIndex.value = 0
      return
    }
  }
  mentionVisible.value = false
}

function pickMention(m: GroupMember) {
  const name = memberDisplayName(m)
  const val = groupInput.value
  const at = val.lastIndexOf('@')
  if (at < 0) return
  groupInput.value = val.slice(0, at) + `@${name} `
  mentionVisible.value = false
  if (!pendingMentionIds.value.includes(m.userId)) {
    pendingMentionIds.value.push(m.userId)
  }
}

function onGroupInputKeydown(e: KeyboardEvent) {
  if (!mentionVisible.value || !mentionCandidates.value.length) return
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    mentionIndex.value = (mentionIndex.value + 1) % mentionCandidates.value.length
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    mentionIndex.value = (mentionIndex.value - 1 + mentionCandidates.value.length) % mentionCandidates.value.length
  } else if (e.key === 'Escape') {
    mentionVisible.value = false
  }
}

function onGroupEnterKeydown(e: Event | KeyboardEvent) {
  if (!(e instanceof KeyboardEvent)) return
  if (e.key === 'Enter' && !e.shiftKey) {
    if (mentionVisible.value && mentionCandidates.value.length) {
      e.preventDefault()
      pickMention(mentionCandidates.value[mentionIndex.value])
      return
    }
    e.preventDefault()
    handleGroupSend()
    return
  }
  onGroupInputKeydown(e)
}

function scrollBottom(refEl: { value: HTMLElement | null }) {
  const el = refEl.value
  if (el) el.scrollTop = el.scrollHeight
}

function formatSearchPreview(content: string | undefined, msgType?: number) {
  const text = previewMessageText({ content, msgType })
  return text.length > 40 ? `${text.slice(0, 40)}…` : text
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

function groupAvatarStyle(name?: string) {
  const s = name || 'G'
  let hash = 0
  for (let i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash)
  return { background: GROUP_AVATAR_COLORS[Math.abs(hash) % GROUP_AVATAR_COLORS.length] }
}

function parseMsgTimestamp(time: string | number | undefined) {
  if (time == null || time === '') return 0
  const ts = new Date(time).getTime()
  return Number.isNaN(ts) ? 0 : ts
}

function shouldSplitMessageTime(prev: ChatMessage, curr: ChatMessage) {
  const ta = parseMsgTimestamp(prev.sendTime)
  const tb = parseMsgTimestamp(curr.sendTime)
  if (!ta || !tb) return true
  if (new Date(ta).toDateString() !== new Date(tb).toDateString()) return true
  return tb - ta > CHAT_TIME_GAP_MS
}

function formatTimeDivider(time: string | number | undefined) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const target = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  const hm = d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (target.getTime() === today.getTime()) return hm
  if (target.getTime() === yesterday.getTime()) return `昨天 ${hm}`
  if (d.getFullYear() === now.getFullYear()) {
    return `${d.getMonth() + 1}月${d.getDate()}日 ${hm}`
  }
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function buildChatRenderItems(list: ChatMessage[], withSystem = false): ChatRenderItem[] {
  const recallMode: 'private' | 'group' = withSystem ? 'group' : 'private'
  const items: ChatRenderItem[] = []
  let prev: ChatMessage | undefined
  for (const message of list) {
    if (withSystem && message.msgType === CHAT_MSG_TYPE.SYSTEM) {
      items.push({ kind: 'system', id: `sys-${message.id}`, text: message.content || '' })
      prev = message
      continue
    }
    if (isRecalledMessage(message)) {
      if (!prev || shouldSplitMessageTime(prev, message)) {
        items.push({ kind: 'time', id: `time-${message.id}`, text: formatTimeDivider(message.sendTime) })
      }
      items.push({
        kind: 'recall',
        id: `recall-${message.id}`,
        text: recallNoticeText(message, currentUserId.value, recallMode),
      })
      prev = message
      continue
    }
    if (!prev || (withSystem && prev.msgType === CHAT_MSG_TYPE.SYSTEM) || isRecalledMessage(prev) || shouldSplitMessageTime(prev, message)) {
      items.push({ kind: 'time', id: `time-${message.id}`, text: formatTimeDivider(message.sendTime) })
    }
    const prevMsg = prev && !isRecalledMessage(prev) && (!withSystem || prev.msgType !== CHAT_MSG_TYPE.SYSTEM)
      ? prev
      : undefined
    const showAvatar =
      !prevMsg ||
      prevMsg.senderId !== message.senderId ||
      shouldSplitMessageTime(prevMsg, message)
    items.push({ kind: 'message', id: `msg-${message.id}`, message, showAvatar })
    prev = message
  }
  return items
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
    // 撤回由 messageStore + recallTick watch 统一处理，此处不可再 push 消息
    if (data.recall) return

    if (data.type === 'typing' && data.fromUserId != null) {
      if (selectedUser.value?.id === data.fromUserId) {
        typingFromUserId.value = data.fromUserId
        if (typingHideTimer) clearTimeout(typingHideTimer)
        typingHideTimer = setTimeout(() => {
          typingFromUserId.value = null
        }, 3000)
      }
      return
    }
    if (data.type === 'chat' && data.senderId != null) {
      const senderId = data.senderId
      updateUserLastMsg(senderId, data.content, data.msgType || CHAT_MSG_TYPE.TEXT)
      const u = users.value.find(x => x.id === senderId)
      if (u && selectedUser.value?.id !== senderId) {
        u.unreadCount = (u.unreadCount || 0) + 1
      }
      if (selectedUser.value?.id === senderId) {
        messages.value.push({
          id: data.id || data.messageId || Date.now(),
          senderId,
          senderName: data.senderName,
          senderAvatar: data.senderAvatar,
          content: data.content,
          msgType: data.msgType || CHAT_MSG_TYPE.TEXT,
          sendTime: new Date().toISOString(),
        })
        nextTick(() => scrollBottom(messageListRef))
        readChat(senderId).then(() => messageStore.refreshSummary())
      }
    }
    if (data.type === 'groupChat' && data.groupId != null) {
      const groupId = data.groupId
      updateGroupLastMsg(groupId, data.senderName, data.content, data.msgType || CHAT_MSG_TYPE.TEXT)
      if (selectedGroup.value?.id === groupId) {
        groupMessages.value.push({
          id: data.id || data.messageId || Date.now(),
          groupId,
          senderId: data.senderId,
          senderName: data.senderName,
          senderAvatar: data.senderAvatar,
          content: data.content,
          msgType: data.msgType || CHAT_MSG_TYPE.TEXT,
          sendTime: new Date().toISOString(),
        })
        nextTick(() => scrollBottom(groupListRef))
      }
    }
  })
}

onMounted(async () => {
  document.addEventListener('click', closeMessageMenu)
  try {
    const res = await canCreateChatGroup()
    canCreateGroup.value = !!res.data
  } catch {
    canCreateGroup.value = userStore.userInfo?.roles?.includes('super_admin') ?? false
  }
  await loadUsers()
  await loadGroups()
  setupWs()
  await applyRouteQuery()
})

onActivated(() => {
  loadUsers()
})

watch(
  () => messageStore.recallTick,
  () => {
    const data = messageStore.lastRecall
    if (data) handleRecallWs(data)
  },
)

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
  document.removeEventListener('click', closeMessageMenu)
  if (typingHideTimer) clearTimeout(typingHideTimer)
  if (typingSendTimer) clearTimeout(typingSendTimer)
  offWs?.()
  messageStore.setActiveChatTarget(null)
})
</script>

<style scoped>
/* 铺满主内容区，抵消 layout main-content 的 padding */
.chat-page {
  margin: -20px;
  width: calc(100% + 40px);
  height: calc(100% + 40px);
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chat-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--theme-border, #e7e7e7);
  border-radius: var(--admin-radius-lg, 12px);
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.chat-shell.is-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 1000;
  width: 100vw;
  height: 100vh;
  border-radius: 0;
  border: none;
}

.chat-wrapper {
  display: flex;
  flex: 1;
  min-height: 0;
}

/* —— 左侧会话列表（微信风） —— */
.chat-sidebar {
  min-width: 240px;
  max-width: 360px;
  border-right: 1px solid #e7e7e7;
  display: flex;
  flex-direction: column;
  background: #f7f7f7;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 14px 12px 10px;
  background: #f7f7f7;
}

.sidebar-search :deep(.el-input__wrapper) {
  border-radius: 8px;
  background: #ededed;
  box-shadow: none;
}

.sidebar-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px 12px;
}

.tab-segment {
  flex: 1;
  display: flex;
  padding: 3px;
  background: #ededed;
  border-radius: 8px;
}

.tab-segment-item {
  position: relative;
  flex: 1;
  border: none;
  background: transparent;
  padding: 6px 0;
  font-size: 13px;
  color: #666;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.tab-segment-item.active {
  background: #fff;
  color: #191919;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.tab-unread-badge {
  position: absolute;
  top: 4px;
  right: 18%;
  width: 7px;
  height: 7px;
  background: #fa5151;
  border-radius: 50%;
}

.tab-add-btn {
  flex-shrink: 0;
  border-color: #e7e7e7;
}

.contact-list {
  flex: 1;
  overflow-y: auto;
  background: #fff;
}

.contact-item {
  display: flex;
  gap: 12px;
  padding: 12px 14px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f5f5f5;
}

.contact-item:hover {
  background: #f5f5f5;
}

.contact-item.active {
  background: #ededed;
}

.contact-item.blocked {
  opacity: 0.65;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.online-dot {
  position: absolute;
  bottom: 1px;
  right: 1px;
  width: 11px;
  height: 11px;
  background: #07c160;
  border: 2px solid #fff;
  border-radius: 50%;
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.contact-name {
  flex: 1;
  font-weight: 500;
  font-size: 15px;
  color: #191919;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-time {
  font-size: 11px;
  color: #b2b2b2;
  flex-shrink: 0;
}

.contact-unread {
  flex-shrink: 0;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  color: #fff;
  background: #fa5151;
  border-radius: 9px;
}

.contact-msg {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-msg .blocked {
  color: #fa5151;
}

.member-badge {
  font-size: 11px;
  color: #888;
  flex-shrink: 0;
}

.resize-handle {
  width: 5px;
  cursor: col-resize;
  flex-shrink: 0;
  background: transparent;
  transition: background 0.2s;
}

.resize-handle:hover {
  background: #e7e7e7;
}

/* —— 右侧聊天区 —— */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ededed;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  height: 56px;
  background: #f7f7f7;
  border-bottom: 1px solid #e7e7e7;
  flex-shrink: 0;
}

.header-info {
  flex: 1;
  min-width: 0;
}

.header-name {
  font-size: 16px;
  font-weight: 600;
  color: #191919;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #888;
  margin-top: 2px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.is-online {
  background: #07c160;
}

.status-dot.is-offline {
  background: #c8c8c8;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.header-actions :deep(.el-button) {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: #576b95;
}

.header-actions :deep(.el-button:hover) {
  background: rgba(0, 0, 0, 0.05);
}

/* —— 消息列表 —— */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.load-more-hint {
  text-align: center;
  font-size: 12px;
  color: #999;
  padding: 8px 0 4px;
}

.load-more-btn {
  cursor: pointer;
  color: #576b95;
}

.load-more-btn:hover {
  text-decoration: underline;
}

.message-list-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
}

.msg-time-divider {
  display: flex;
  justify-content: center;
  margin: 12px 0 8px;
}

.msg-time-divider span {
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.18);
  padding: 4px 10px;
  border-radius: 4px;
  line-height: 1.4;
}

.message-item {
  display: flex;
  gap: 10px;
  max-width: min(72%, 520px);
  margin-bottom: 4px;
  align-items: flex-start;
}

.message-item.is-continued {
  margin-bottom: 2px;
}

.message-item.self {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-item.message-highlight {
  outline: 2px solid #ffc300;
  outline-offset: 2px;
  border-radius: 8px;
}

.message-avatar {
  flex-shrink: 0;
  width: 40px;
}

.message-avatar.is-placeholder {
  visibility: hidden;
}

.message-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.message-item.self .message-body {
  align-items: flex-end;
}

.sender-name {
  font-size: 12px;
  color: #888;
  margin-bottom: 4px;
  padding-left: 2px;
}

.msg-bubble {
  position: relative;
  padding: 9px 12px;
  background: #fff;
  border-radius: 4px;
  font-size: 15px;
  line-height: 1.45;
  color: #191919;
  word-break: break-word;
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.05);
}

.message-item:not(.self) .msg-bubble::before {
  content: '';
  position: absolute;
  left: -6px;
  top: 12px;
  border: 6px solid transparent;
  border-right-color: #fff;
}

.message-item.self .msg-bubble {
  background: #95ec69;
  color: #191919;
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.06);
}

.message-item.self .msg-bubble::after {
  content: '';
  position: absolute;
  right: -6px;
  top: 12px;
  border: 6px solid transparent;
  border-left-color: #95ec69;
}

.message-item.is-continued:not(.self) .msg-bubble::before,
.message-item.is-continued.self .msg-bubble::after {
  display: none;
}

.msg-image {
  max-width: min(240px, 56vw);
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  line-height: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.msg-image img {
  width: 100%;
  display: block;
  vertical-align: top;
}

.msg-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  max-width: 280px;
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.05);
}

.msg-file:hover {
  background: #f5f5f5;
}

.msg-file-icon {
  font-size: 28px;
  color: #576b95;
  flex-shrink: 0;
}

.msg-file-name {
  font-size: 14px;
  color: #191919;
  word-break: break-all;
}

.msg-bubble :deep(.msg-mention) {
  color: #576b95;
  font-weight: 500;
}

.msg-context-menu {
  position: fixed;
  z-index: 3000;
  min-width: 88px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  padding: 4px 0;
}

.msg-menu-item {
  display: block;
  width: 100%;
  border: none;
  background: none;
  padding: 8px 16px;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  color: #303133;
}

.msg-menu-item:hover {
  background: #f5f5f5;
}

.mention-wrap {
  position: relative;
}

.mention-dropdown {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 100%;
  margin: 0 0 6px;
  padding: 4px 0;
  list-style: none;
  background: #fff;
  border: 1px solid #e7e7e7;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
}

.mention-dropdown li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 14px;
}

.mention-dropdown li.active,
.mention-dropdown li:hover {
  background: #f0f0f0;
}

.mention-empty {
  color: #999;
  cursor: default;
}

.system-msg {
  align-self: center;
  max-width: 90%;
  text-align: center;
  font-size: 12px;
  color: #888;
  padding: 4px 12px;
  margin: 8px 0;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 4px;
}

/* —— 输入区 —— */
.chat-input {
  flex-shrink: 0;
  padding: 10px 16px 14px;
  background: #f7f7f7;
  border-top: 1px solid #e7e7e7;
}

.chat-input-panel {
  background: #fff;
  border: 1px solid #e7e7e7;
  border-radius: 8px;
  overflow: hidden;
}

.input-toolbar {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
  gap: 6px;
  padding: 6px 10px 0;
}

.input-toolbar :deep(.el-upload) {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}

.toolbar-btn {
  width: 32px;
  height: 32px;
  padding: 0 !important;
  margin: 0 !important;
  font-size: 20px;
  color: #576b95;
  border: none !important;
  border-radius: 6px;
  background: transparent !important;
  box-shadow: none !important;
}

.toolbar-btn:hover {
  background: #f5f5f5 !important;
  color: #576b95 !important;
}

.chat-input-row {
  padding: 4px 12px 0;
}

.chat-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 8px 0 10px;
  font-size: 15px;
  line-height: 1.5;
  background: transparent;
  resize: none;
}

.chat-input-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 6px 12px 10px;
}

.send-btn {
  flex-shrink: 0;
  min-width: 80px;
  height: 36px;
  padding: 0 20px;
  border: none;
  border-radius: 6px;
  background: #07c160 !important;
  font-weight: 500;
}

.send-btn:hover:not(:disabled) {
  background: #06ad56 !important;
}

.send-btn:disabled {
  background: #9ee6b8 !important;
}

/* —— 空状态 —— */
.chat-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ededed;
}

.chat-empty-inner {
  text-align: center;
  padding: 40px 24px;
}

.chat-empty-icon {
  width: 88px;
  height: 88px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 50%;
  color: #c8c8c8;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.chat-empty-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #191919;
}

.chat-empty-desc {
  margin: 0;
  font-size: 14px;
  color: #888;
}

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
