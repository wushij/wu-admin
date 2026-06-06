import { ref, computed, onMounted, onActivated, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { InputInstance } from 'element-plus'
import {
  Search, Plus, MoreFilled, FullScreen, Setting, ChatDotRound, Document,
} from '@element-plus/icons-vue'
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
  formatFilePayload, previewMessageText,
  canRecallMessage, isRecalledMessage,
  markMessageRecalled, resolveRecallMessageId,
} from '@/utils/chat-message'
import {
  buildChatRenderItems, formatSearchPreview, renderTextContent, fileDisplayName,
  scrollBottom, groupAvatarStyle, formatTime, formatListTime,
  openChatFile, scrollToMessage as scrollToMessageEl, openImagePreview as openImagePreviewFn,
} from './useChatRender'
import { useMention } from './useMention'
import { useUserStore } from '@/store/user'
import { useMessageStore } from '@/store/message'
import { onMessageWebSocket, type WsPushMessage } from '@/utils/messageWebSocket'
import type { ChatUser, ChatGroup, ChatMessage, GroupMember, ChatGroupLogItem } from '@/types/message'
import { avatarFallback, buildUserAvatarMap, resolveChatAvatar } from '@/utils/chat-avatar'


export function useChatPage() {
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
  
  const groupInputRef = ref<InputInstance | null>(null)
  
  const {
    mentionVisible, mentionKeyword, mentionIndex, pendingMentionIds,
    mentionCandidates, onGroupInput, pickMention,
    parseMentionIds, onGroupInputKeydown, resetMention,
  } = useMention(() => groupMembers.value, () => groupInput.value, (v) => { groupInput.value = v }, () => currentUserId.value || 0)
  
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
  
  const privateChatItems = computed(() => buildChatRenderItems(messages.value, currentUserId.value || 0))
  const groupChatItems = computed(() => buildChatRenderItems(groupMessages.value, currentUserId.value || 0, true))
  
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
    resetMention()
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
    resetMention()
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
  
  // scrollBottom, buildChatRenderItems, formatSearchPreview, renderTextContent, fileDisplayName, groupAvatarStyle
  // are imported from ./composables/useChatRender
  
  function scrollToMessage(msgId: number) {
    const container = selectedGroup.value ? groupListRef.value : messageListRef.value
    scrollToMessageEl(msgId, container)
    messageSearchKeyword.value = ''
  }
  
  function openImagePreview(url: string | undefined) {
    openImagePreviewFn(url, (u) => { previewUrl.value = u }, (v) => { previewVisible.value = v })
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

  return {
    route, userStore, messageStore, currentUserId, userAvatarMap,
    contactAvatar, messageAvatar, chatMode, searchKeyword, users, groups,
    selectedUser, selectedGroup, messages, groupMessages, inputContent, groupInput,
    loadingHistory, onlineMap, sidebarWidth, isFullscreen, canCreateGroup,
    privateHasMore, groupHasMore, loadingMore, typingFromUserId,
    mentionVisible, mentionKeyword, mentionIndex, groupInputRef, pendingMentionIds, mentionCandidates, pickMention, onGroupInput,
    msgMenu, messageSearchKeyword, messageListRef, groupListRef, previewVisible,
    previewUrl, showCreateGroup, newGroupName, newGroupMembers, showGroupDetail,
    groupMembers, groupLogs, groupTab, membersLoading, logsLoading, editGroupName,
    editGroupAnnouncement, addMemberIds, filteredUsers, filteredGroups,
    privateTabUnread, groupTabUnread, searchMessageResults, privateChatItems,
    groupChatItems, userOptions, isGroupOwner, canEditGroup, availableAddUsers,
    selectUser, selectGroup, loadMorePrivate, loadMoreGroup, onPrivateListScroll,
    onGroupListScroll, handleSend, handleGroupSend, handleUploadImage, handleUploadGroupImage,
    handleUploadFile, handleUploadGroupFile, handleCreateGroup, openGroupDetail,
    handleQuit, handleDissolve, handlePrivateAction, handleUpdateGroup, handleAddMembers,
    canManageMember, handleMemberAction, handleGroupTabChange, openChatFile,
    openMessageMenu, handleRecallMessage, onPrivateInput, onGroupEnterKeydown,
    scrollToMessage, openImagePreview, formatTime, formatListTime, startResize,
    CHAT_MSG_TYPE, Search, Plus, MoreFilled, FullScreen, Setting, ChatDotRound, Document,
    formatSearchPreview, renderTextContent, fileDisplayName, groupAvatarStyle, avatarFallback, resolveChatAvatar,
  }
}
