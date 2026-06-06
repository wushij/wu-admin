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
import ChatToolbarIcons from '@/components/chat/ChatToolbarIcons.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'
import { useChatPage } from '../composables/useChatPage'
import '../styles/chat-page.css'

const {
  messageStore,
  currentUserId,
  userAvatarMap,
  contactAvatar,
  messageAvatar,
  chatMode,
  searchKeyword,
  selectedUser,
  selectedGroup,
  messages,
  groupMessages,
  inputContent,
  groupInput,
  loadingHistory,
  onlineMap,
  sidebarWidth,
  isFullscreen,
  canCreateGroup,
  privateHasMore,
  groupHasMore,
  loadingMore,
  typingFromUserId,
  mentionVisible,
  mentionIndex,
  groupInputRef,
  mentionCandidates,
  pickMention,
  onGroupInput,
  msgMenu,
  messageSearchKeyword,
  messageListRef,
  groupListRef,
  previewVisible,
  previewUrl,
  showCreateGroup,
  newGroupName,
  newGroupMembers,
  showGroupDetail,
  groupMembers,
  groupLogs,
  groupTab,
  membersLoading,
  logsLoading,
  editGroupName,
  editGroupAnnouncement,
  addMemberIds,
  filteredUsers,
  filteredGroups,
  privateTabUnread,
  groupTabUnread,
  searchMessageResults,
  privateChatItems,
  groupChatItems,
  userOptions,
  isGroupOwner,
  canEditGroup,
  availableAddUsers,
  selectUser,
  selectGroup,
  loadMorePrivate,
  loadMoreGroup,
  onPrivateListScroll,
  onGroupListScroll,
  handleSend,
  handleGroupSend,
  handleUploadImage,
  handleUploadGroupImage,
  handleUploadFile,
  handleUploadGroupFile,
  handleCreateGroup,
  openGroupDetail,
  handleQuit,
  handleDissolve,
  handlePrivateAction,
  handleUpdateGroup,
  handleAddMembers,
  canManageMember,
  handleMemberAction,
  handleGroupTabChange,
  openChatFile,
  openMessageMenu,
  handleRecallMessage,
  onPrivateInput,
  onGroupEnterKeydown,
  scrollToMessage,
  openImagePreview,
  formatTime,
  formatListTime,
  startResize,
  CHAT_MSG_TYPE,
  Search,
  Plus,
  MoreFilled,
  FullScreen,
  Setting,
  ChatDotRound,
  Document,
  formatSearchPreview,
  renderTextContent,
  fileDisplayName,
  groupAvatarStyle,
  avatarFallback,
  resolveChatAvatar,
} = useChatPage()
</script>
