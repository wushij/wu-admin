import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useUserStore } from '@/store/user'
import { useMessageStore } from '@/store/message'
import { CHAT_MSG_TYPE, CHAT_PAGE_SIZE } from '@/constants/chat'
import {
  getChatHistory,
  getChatUsers,
  getGroupDetail,
  getGroupMembers,
  getGroupMessages,
  markGroupAnnouncementRead,
  readChat,
  sendChat,
  sendGroupMessage,
  setGroupNotifyMuted,
  uploadChatImage,
  uploadChatFile,
  recallPrivateMessage,
  recallGroupMessage,
  sendTypingSignal,
} from '@/api/message'
import {
  canRecallMessage,
  formatFilePayload,
  groupMemberDisplayName,
  markMessageRecalled,
  resolveRecallMessageId,
  isImageFileMeta,
} from '@/utils/chat-message'
import { useMention } from '@/composables/useMention'
import { showActionSheet } from '@/utils/app-dialog'
import { onMessageWebSocket, type WsPushMessage } from '@/utils/webSocket'
import { buildUserAvatarMap, resolveChatAvatar } from '@/utils/chat-avatar'
import { isUserCancelError } from '@/utils/file-preview'
import { compressChatImage } from '@/utils/image-compress'
import type { ChatGroup, ChatMessage, ChatUser, GroupMember } from '@/types/message'

const TYPING_HIDE_MS = 4000
let tempMessageSeq = 0

function nextTempMessageId() {
  tempMessageSeq += 1
  return -(Date.now() + tempMessageSeq)
}

function mapWsToMessage(data: WsPushMessage): ChatMessage {
  const time =
    typeof data.time === 'number'
      ? new Date(data.time).toISOString()
      : data.time || new Date().toISOString()
  return {
    id: Number(data.id || data.messageId || Date.now()),
    senderId: data.senderId,
    senderName: data.senderName,
    senderAvatar: data.senderAvatar,
    receiverId: data.receiverId,
    groupId: data.groupId,
    content: data.content,
    msgType: data.msgType ?? CHAT_MSG_TYPE.TEXT,
    sendTime: String(time),
  }
}

export function useChatDetail() {
  const userStore = useUserStore()
  const messageStore = useMessageStore()
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)
  const loadingMore = ref(false)
  const prependingHistory = ref(false)
  const pageNo = ref(1)
  const messageTotal = ref(0)
  const sending = ref(false)
  const input = ref('')
  const typingHint = ref('')
  const targetType = ref<'user' | 'group'>('user')
  const targetId = ref(0)
  const targetName = ref('')
  const peerOnline = ref(false)
  const groupInfo = ref<ChatGroup | null>(null)
  const groupMembers = ref<GroupMember[]>([])
  const peerUser = ref<Pick<ChatUser, 'id' | 'avatar' | 'nickname' | 'username'> | null>(null)
  const showAnnouncementModal = ref(false)
  const emojiVisible = ref(false)

  const mention = useMention(
    () => groupMembers.value,
    () => input.value,
    (v) => {
      input.value = v
    },
    () => selfId.value ?? 0,
  )

  let offWs: (() => void) | null = null
  let typingTimer: ReturnType<typeof setTimeout> | null = null
  let typingSignalTimer: ReturnType<typeof setTimeout> | null = null

  const selfId = computed(() => userStore.userInfo.userId)

  const chatMode = computed(() => (targetType.value === 'group' ? 'group' : 'private') as 'group' | 'private')

  const memberCount = computed(() => {
    const count = groupInfo.value?.memberCount
    if (count != null && count > 0) return count
    return groupMembers.value.length || 0
  })

  const hasMoreHistory = computed(() => messages.value.length < messageTotal.value)

  const avatarMap = computed(() => {
    const entries: Array<{ userId: number; avatar?: string | null }> = []
    const self = userStore.userInfo
    if (self.userId && self.avatar) entries.push({ userId: self.userId, avatar: self.avatar })
    if (peerUser.value?.id && peerUser.value.avatar) {
      entries.push({ userId: peerUser.value.id, avatar: peerUser.value.avatar })
    }
    groupMembers.value.forEach((m) => {
      if (m.userId && m.avatar) entries.push({ userId: m.userId, avatar: m.avatar })
    })
    return buildUserAvatarMap(entries)
  })

  function resolveMessageAvatar(msg: ChatMessage) {
    return resolveChatAvatar(msg.senderId, avatarMap.value, msg.senderAvatar)
  }

  function resolveMessageSenderName(msg: ChatMessage) {
    if (msg.senderId != null && msg.senderId === selfId.value) {
      return userStore.userInfo.nickname || userStore.userInfo.username || '我'
    }
    if (msg.senderName?.trim()) return msg.senderName.trim()
    if (targetType.value === 'group') {
      const member = groupMembers.value.find((m) => m.userId === msg.senderId)
      if (member) return groupMemberDisplayName(member)
    }
    if (peerUser.value && msg.senderId === peerUser.value.id) {
      return peerUser.value.nickname || peerUser.value.username || targetName.value || '对方'
    }
    return '成员'
  }

  const showAnnouncementBar = computed(() => {
    const g = groupInfo.value
    return !!g?.announcement?.trim() && !!g.announcementUnread
  })

  const announcementPublisher = computed(() => {
    const g = groupInfo.value
    if (!g) return ''
    if (g.announcementPublisherId) {
      const member = groupMembers.value.find((m) => m.userId === g.announcementPublisherId)
      if (member) return groupMemberDisplayName(member)
    }
    return g.announcementPublisherName?.trim() || ''
  })

  const notifyMuted = computed(() => !!groupInfo.value?.notifyMuted)

  function hasMessageId(id: number) {
    return messages.value.some((m) => Number(m.id) === id)
  }

  function replaceMessageById(messageId: number, next: ChatMessage) {
    const idx = messages.value.findIndex((m) => Number(m.id) === messageId)
    if (idx < 0) {
      if (!hasMessageId(Number(next.id))) messages.value.push(next)
      return
    }
    messages.value[idx] = next
  }

  function markMessageFailed(messageId: number) {
    const idx = messages.value.findIndex((m) => Number(m.id) === messageId)
    if (idx < 0) return
    messages.value[idx] = { ...messages.value[idx], sendStatus: 'failed' }
  }

  function absorbPendingSelfMessage(data: WsPushMessage): boolean {
    if (data.senderId !== selfId.value) return false
    const id = Number(data.id || data.messageId || 0)
    if (id && hasMessageId(id)) return true
    const idx = messages.value.findIndex(
      (m) => m.id < 0 && m.sendStatus === 'pending' && m.senderId === selfId.value,
    )
    if (idx < 0) return false
    messages.value[idx] = mapWsToMessage(data)
    return true
  }

  function buildOptimisticImageMessage(localPreview: string): ChatMessage {
    const self = userStore.userInfo
    return {
      id: nextTempMessageId(),
      senderId: self.userId,
      senderName: self.nickname || self.username || '我',
      senderAvatar: self.avatar,
      content: localPreview,
      msgType: CHAT_MSG_TYPE.IMAGE,
      localPreview,
      sendStatus: 'pending',
      sendTime: new Date().toISOString(),
    }
  }

  async function sendChatImageOptimistic(rawPath: string) {
    const optimistic = buildOptimisticImageMessage(rawPath)
    const tempId = optimistic.id
    messages.value.push(optimistic)

    try {
      const filePath = await compressChatImage(rawPath)
      const uploadRes = await uploadChatImage(filePath)
      const url = uploadRes.data?.url
      if (!url) throw new Error('图片上传失败')

      const res =
        targetType.value === 'user'
          ? await sendChat({ receiverId: targetId.value, content: url, msgType: CHAT_MSG_TYPE.IMAGE })
          : await sendGroupMessage(targetId.value, { content: url, msgType: CHAT_MSG_TYPE.IMAGE })

      if (!res.data) throw new Error('消息发送失败')
      const serverMsg: ChatMessage = {
        ...res.data,
        msgType: res.data.msgType ?? CHAT_MSG_TYPE.IMAGE,
        content: res.data.content ?? url,
      }
      replaceMessageById(tempId, serverMsg)
    } catch {
      markMessageFailed(tempId)
      uni.showToast({ title: '图片发送失败', icon: 'none' })
    }
  }

  function updateNavTitle(name: string) {
    if (targetType.value === 'group') {
      uni.setNavigationBarTitle({ title: '\u200b' })
      return
    }
    if (targetType.value === 'user' && peerOnline.value) {
      uni.setNavigationBarTitle({ title: `${name} (在线)` })
      return
    }
    uni.setNavigationBarTitle({ title: name || '聊天' })
  }

  async function refreshGroupContext() {
    if (targetType.value !== 'group' || !targetId.value) return
    const res = await getGroupDetail(targetId.value)
    if (res.data) {
      groupInfo.value = { ...(groupInfo.value || { id: targetId.value, name: targetName.value }), ...res.data }
      if (res.data.name) targetName.value = res.data.name
      updateNavTitle(targetName.value)
    }
  }

  function applyGroupAnnouncementPush(data: WsPushMessage) {
    if (data.type !== 'groupAnnouncement' || data.groupId !== targetId.value) return
    const patch: Partial<ChatGroup> = {
      announcementUnread: true,
      announcementPublisherId: data.senderId,
      announcementPublisherName: data.senderName,
    }
    if (data.announcement != null) patch.announcement = data.announcement
    groupInfo.value = { ...(groupInfo.value || { id: targetId.value, name: targetName.value }), ...patch }
  }

  function clearTypingHint() {
    typingHint.value = ''
    if (typingTimer) {
      clearTimeout(typingTimer)
      typingTimer = null
    }
  }

  function handleWs(data: WsPushMessage) {
    if (data.recall) {
      const mid = resolveRecallMessageId(data)
      if (mid) messages.value = markMessageRecalled(messages.value, mid)
      return
    }

    if (data.type === 'groupAnnouncement') {
      applyGroupAnnouncementPush(data)
      return
    }

    if (data.type === 'presence' && targetType.value === 'user' && data.userId === targetId.value) {
      peerOnline.value = !!data.online
      updateNavTitle(targetName.value)
      return
    }

    if (data.type === 'typing' && targetType.value === 'user' && data.fromUserId === targetId.value) {
      if (data.active === false) {
        clearTypingHint()
        return
      }
      typingHint.value = '对方正在输入…'
      if (typingTimer) clearTimeout(typingTimer)
      typingTimer = setTimeout(clearTypingHint, TYPING_HIDE_MS)
      return
    }

    if (data.type === 'chat' && targetType.value === 'user') {
      const peerId = targetId.value
      const fromPeer = data.senderId === peerId
      const fromSelf = data.senderId === selfId.value
      if (!fromPeer && !fromSelf) return

      const id = Number(data.id || data.messageId || 0)
      if (id && hasMessageId(id)) return
      if (fromSelf && absorbPendingSelfMessage(data)) return

      if (fromPeer) clearTypingHint()
      messages.value.push(mapWsToMessage(data))
      if (fromPeer) {
        readChat(peerId).then(() => messageStore.refreshSummary())
      }
      return
    }

    if (data.type === 'groupChat' && targetType.value === 'group' && data.groupId === targetId.value) {
      const id = Number(data.id || data.messageId || 0)
      if (id && hasMessageId(id)) return
      if (data.senderId === selfId.value && absorbPendingSelfMessage(data)) return
      messages.value.push(mapWsToMessage(data))
    }
  }

  function bindWs() {
    offWs?.()
    offWs = onMessageWebSocket(handleWs)
  }

  async function init(type: 'user' | 'group', id: number, name: string, online = false) {
    targetType.value = type
    targetId.value = id
    targetName.value = name
    peerOnline.value = online
    messages.value = []
    pageNo.value = 1
    messageTotal.value = 0
    loadingMore.value = false
    prependingHistory.value = false
    groupMembers.value = []
    peerUser.value = null
    groupInfo.value = null
    showAnnouncementModal.value = false
    emojiVisible.value = false
    mention.resetMention()
    clearTypingHint()
    updateNavTitle(name)
    messageStore.setActiveChatTarget({ type, id })
    bindWs()
    if (type === 'user') {
      const userRes = await getChatUsers()
      const peer = (userRes.data || []).find((u) => u.id === id)
      peerUser.value = peer || { id, nickname: name, username: name }
      await readChat(id)
      messageStore.markPrivateChatRead(id)
      messageStore.refreshSummary()
    } else {
      messageStore.clearGroupUnread(id)
      const [memberRes, detailRes] = await Promise.all([
        getGroupMembers(id),
        getGroupDetail(id),
      ])
      groupMembers.value = memberRes.data || []
      groupInfo.value = detailRes.data || { id, name }
      if (groupInfo.value.name) targetName.value = groupInfo.value.name
      updateNavTitle(targetName.value)
    }
    await loadHistory()
  }

  async function loadHistory() {
    loading.value = true
    pageNo.value = 1
    try {
      const res =
        targetType.value === 'user'
          ? await getChatHistory(targetId.value, { pageNo: 1, pageSize: CHAT_PAGE_SIZE })
          : await getGroupMessages(targetId.value, { pageNo: 1, pageSize: CHAT_PAGE_SIZE })
      messages.value = res.data?.list || []
      messageTotal.value = res.data?.total ?? messages.value.length
    } finally {
      loading.value = false
    }
  }

  async function loadMoreHistory() {
    if (loading.value || loadingMore.value || !hasMoreHistory.value) return
    loadingMore.value = true
    prependingHistory.value = true
    try {
      const nextPage = pageNo.value + 1
      const res =
        targetType.value === 'user'
          ? await getChatHistory(targetId.value, { pageNo: nextPage, pageSize: CHAT_PAGE_SIZE })
          : await getGroupMessages(targetId.value, { pageNo: nextPage, pageSize: CHAT_PAGE_SIZE })
      const older = res.data?.list || []
      if (!older.length) {
        messageTotal.value = messages.value.length
        return
      }
      pageNo.value = nextPage
      messageTotal.value = res.data?.total ?? messageTotal.value
      messages.value = [...older, ...messages.value]
    } finally {
      loadingMore.value = false
      prependingHistory.value = false
    }
  }

  function closeEmojiPanel() {
    emojiVisible.value = false
  }

  function toggleEmoji() {
    const next = !emojiVisible.value
    emojiVisible.value = next
    if (next) uni.hideKeyboard()
  }

  function pickEmoji(emoji: string) {
    input.value += emoji
  }

  function openAnnouncement() {
    showAnnouncementModal.value = true
  }

  async function dismissAnnouncement() {
    if (!groupInfo.value) return
    await markGroupAnnouncementRead(groupInfo.value.id)
    groupInfo.value = { ...groupInfo.value, announcementUnread: false }
    showAnnouncementModal.value = false
  }

  async function toggleNotifyMuted() {
    if (!groupInfo.value) return
    const next = !groupInfo.value.notifyMuted
    const prev = groupInfo.value.notifyMuted
    groupInfo.value = { ...groupInfo.value, notifyMuted: next }
    try {
      await setGroupNotifyMuted(groupInfo.value.id, next)
      messageStore.setGroupNotifyMutedLocal(groupInfo.value.id, next)
      uni.showToast({ title: next ? '已开启免打扰' : '已关闭免打扰', icon: 'none' })
    } catch {
      groupInfo.value = { ...groupInfo.value, notifyMuted: prev }
      uni.showToast({ title: '设置失败', icon: 'none' })
    }
  }

  function goGroupDetail() {
    if (!groupInfo.value?.id) return
    uni.navigateTo({ url: `/pages-sub/msg/chat/group-detail?id=${groupInfo.value.id}` })
  }

  async function send() {
    const content = input.value.trim()
    if (!content) return
    sending.value = true
    try {
      const res =
        targetType.value === 'user'
          ? await sendChat({ receiverId: targetId.value, content })
          : await sendGroupMessage(targetId.value, {
              content,
              mentionIds: mention.parseMentionIds(content),
            })
      if (res.data) {
        const id = Number(res.data.id)
        if (!id || !hasMessageId(id)) messages.value.push(res.data)
      }
      input.value = ''
      mention.resetMention()
      clearTypingHint()
      // 发送后保持键盘；表情面板仅收起面板本身
      if (emojiVisible.value) closeEmojiPanel()
    } finally {
      sending.value = false
    }
  }

  async function sendMediaMessage(content: string, msgType: number) {
    const res =
      targetType.value === 'user'
        ? await sendChat({ receiverId: targetId.value, content, msgType })
        : await sendGroupMessage(targetId.value, { content, msgType })
    if (res.data) {
      const id = Number(res.data.id)
      const msg: ChatMessage = {
        ...res.data,
        msgType: res.data.msgType ?? msgType,
        content: res.data.content ?? content,
      }
      if (!id || !hasMessageId(id)) messages.value.push(msg)
    }
  }

  async function pickImage() {
    let filePath: string | undefined
    try {
      const choose = await uni.chooseImage({ count: 1, sizeType: ['compressed'] })
      filePath = choose.tempFilePaths?.[0]
    } catch (err) {
      if (isUserCancelError(err)) return
      uni.showToast({ title: '图片发送失败', icon: 'none' })
      return
    }
    if (!filePath) return
    closeEmojiPanel()
    await sendChatImageOptimistic(filePath)
  }

  async function pickFile() {
    let filePath: string | undefined
    let fileName: string | undefined
    let fileMime: string | undefined
    try {
      const picked = await new Promise<{
        path?: string
        name?: string
        mime?: string
      }>((resolve, reject) => {
        uni.chooseFile({
          count: 1,
          success: (res) => {
            const file = (res.tempFiles as { path?: string; name?: string; type?: string }[])?.[0]
            resolve({
              path: res.tempFilePaths?.[0] || file?.path,
              name: file?.name,
              mime: file?.type,
            })
          },
          fail: reject,
        })
      })
      filePath = picked.path
      fileName = picked.name || filePath?.split(/[/\\]/).pop()
      fileMime = picked.mime
    } catch (err) {
      if (isUserCancelError(err)) return
      uni.showToast({ title: '文件发送失败', icon: 'none' })
      return
    }
    if (!filePath) return

    const asImage = isImageFileMeta(fileName, fileMime)
    if (asImage) {
      closeEmojiPanel()
      await sendChatImageOptimistic(filePath)
      return
    }

    sending.value = true
    try {
      const uploadRes = await uploadChatFile(filePath)
      if (!uploadRes.data?.url) {
        uni.showToast({ title: '文件上传失败', icon: 'none' })
        return
      }
      const payload = formatFilePayload({ ...uploadRes.data, url: uploadRes.data.url })
      await sendMediaMessage(payload, CHAT_MSG_TYPE.FILE)
    } catch {
      uni.showToast({ title: '文件发送失败', icon: 'none' })
    } finally {
      sending.value = false
    }
  }

  async function pickAttachment() {
    emojiVisible.value = false
    try {
      const index = await showActionSheet({
        items: [{ label: '图片' }, { label: '文件' }],
      })
      if (index === 0) await pickImage()
      else await pickFile()
    } catch {
      /* cancelled */
    }
  }

  function copyMessageText(msg: ChatMessage) {
    const type = msg.msgType ?? CHAT_MSG_TYPE.TEXT
    if (type !== CHAT_MSG_TYPE.TEXT || !msg.content?.trim()) return
    uni.setClipboardData({
      data: msg.content,
      success: () => uni.showToast({ title: '已复制', icon: 'success' }),
    })
  }

  async function recallMessage(msg: ChatMessage) {
    if (!canRecallMessage(msg, selfId.value)) return
    try {
      if (targetType.value === 'user') {
        await recallPrivateMessage(msg.id)
      } else {
        await recallGroupMessage(targetId.value, msg.id)
      }
      messages.value = markMessageRecalled(messages.value, msg.id)
    } catch {
      uni.showToast({ title: '撤回失败', icon: 'none' })
    }
  }

  async function onMessageLongPress(msg: ChatMessage) {
    const actions: string[] = []
    const type = msg.msgType ?? CHAT_MSG_TYPE.TEXT
    if (type === CHAT_MSG_TYPE.TEXT && msg.content?.trim()) actions.push('复制')
    if (canRecallMessage(msg, selfId.value)) actions.push('撤回')
    if (!actions.length) return
    try {
      const index = await showActionSheet({
        items: actions.map((label) => ({
          label,
          danger: label === '撤回',
        })),
      })
      const action = actions[index]
      if (action === '复制') copyMessageText(msg)
      else if (action === '撤回') await recallMessage(msg)
    } catch {
      /* cancelled */
    }
  }

  function isSelf(msg: ChatMessage) {
    return msg.senderId != null && msg.senderId === selfId.value
  }

  watch(input, (val) => {
    if (targetType.value === 'group') {
      mention.onGroupInput()
      return
    }
    if (!targetId.value || !val.trim()) return
    if (typingSignalTimer) clearTimeout(typingSignalTimer)
    typingSignalTimer = setTimeout(() => {
      sendTypingSignal(targetId.value).catch(() => {})
    }, 400)
  })

  onBeforeUnmount(() => {
    offWs?.()
    offWs = null
    clearTypingHint()
    if (typingSignalTimer) clearTimeout(typingSignalTimer)
    messageStore.setActiveChatTarget(null)
    messageStore.refreshSummary()
  })

  return {
    messages,
    loading,
    loadingMore,
    prependingHistory,
    hasMoreHistory,
    sending,
    input,
    typingHint,
    targetType,
    targetId,
    targetName,
    groupInfo,
    memberCount,
    chatMode,
    selfId,
    resolveMessageAvatar,
    resolveMessageSenderName,
    showAnnouncementBar,
    showAnnouncementModal,
    announcementPublisher,
    emojiVisible,
    init,
    loadMoreHistory,
    refreshGroupContext,
    send,
    pickAttachment,
    closeEmojiPanel,
    toggleEmoji,
    pickEmoji,
    openAnnouncement,
    dismissAnnouncement,
    goGroupDetail,
    onMessageLongPress,
    isSelf,
    mentionVisible: mention.mentionVisible,
    mentionCandidates: mention.mentionCandidates,
    pickMention: mention.pickMention,
  }
}
