<template>
  <view class="chat-page">
    <view class="chat-page__main">
      <view v-if="targetType === 'group'" class="chat-header" @click="goGroupDetail">
        <ChatAvatar :name="targetName" :group-name="targetName" />
        <view class="chat-header__info">
          <text class="chat-header__name">{{ targetName }}</text>
          <text class="chat-header__meta">{{ memberCount }}人</text>
        </view>
        <view class="chat-header__more" @click.stop="goGroupDetail">
          <text class="chat-header__more-icon">⋯</text>
        </view>
      </view>

      <GroupAnnouncementBar
        v-if="showAnnouncementBar"
        :announcement="groupInfo?.announcement"
        @open="openAnnouncement"
      />

      <scroll-view
        scroll-y
        class="chat-page__messages"
        :scroll-top="scrollTop"
        :scroll-with-animation="scrollAnimated"
        :show-scrollbar="false"
        upper-threshold="80"
        @scrolltoupper="onScrollToUpper"
        @click="closeEmojiPanel"
      >
        <view
          id="chat-scroll-content"
          class="chat-page__list"
          :style="{ paddingBottom: `${messagesPadBottom}px` }"
        >
          <ChatMessageFeed
            :messages="messages"
            :mode="chatMode"
            :current-user-id="selfId"
            :loading="loading"
            :loading-more="loadingMore"
            :has-more="hasMoreHistory"
            :resolve-avatar="resolveMessageAvatar"
            :resolve-sender-name="resolveMessageSenderName"
            :empty-text="chatMode === 'group' ? '暂无消息，在群里说点什么吧' : '暂无消息，发一句打个招呼吧'"
            @load-more="onLoadMore"
            @longpress="onMessageLongPress"
            @media-loaded="onMediaLoaded"
          />
        </view>
      </scroll-view>
    </view>

    <view class="chat-page__footer" :style="{ bottom: `${footerBottom}px` }">
      <view v-if="typingHint" class="chat-page__typing">{{ typingHint }}</view>
      <MentionPanel
        v-if="mentionVisible && targetType === 'group'"
        :candidates="mentionCandidates"
        @pick="pickMention"
      />
      <ChatComposer
        ref="composerRef"
        v-model="input"
        :loading="sending"
        :emoji-active="emojiVisible"
        :placeholder="chatMode === 'group' ? '输入消息，@ 提醒成员…' : '输入消息…'"
        @send="onSend"
        @pick-attach="pickAttachment"
        @toggle-emoji="onToggleEmoji"
        @close-panels="closeEmojiPanel"
      />
      <EmojiPicker v-if="emojiVisible" :visible="emojiVisible" @pick="pickEmoji" />
    </view>

    <AppDialogHost />

    <view v-if="showAnnouncementModal" class="announce-modal" @click.self="showAnnouncementModal = false">
      <view class="announce-modal__panel">
        <text class="announce-modal__title">群公告</text>
        <text v-if="announcementPublisher" class="announce-modal__meta">发布人：{{ announcementPublisher }}</text>
        <scroll-view scroll-y class="announce-modal__body">
          <text class="announce-modal__content">{{ groupInfo?.announcement || '暂无公告' }}</text>
        </scroll-view>
        <button class="announce-modal__btn" @click="dismissAnnouncement">我知道了</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import ChatAvatar from '@/components/business/ChatAvatar/index.vue'
import ChatComposer from '@/components/business/ChatComposer/index.vue'
import ChatMessageFeed from '@/components/business/ChatMessageFeed/index.vue'
import MentionPanel from '@/components/business/MentionPanel/index.vue'
import EmojiPicker from '@/components/business/EmojiPicker/index.vue'
import GroupAnnouncementBar from '@/components/business/GroupAnnouncementBar/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useChatDetail } from '@/composables/useChatDetail'
import { useChatKeyboardInset } from '@/composables/useChatKeyboardInset'
import { useChatScroll } from '@/composables/useChatScroll'

const COMPOSER_H = uni.upx2px(104)
const EMOJI_PANEL_H = uni.upx2px(540)

const composerRef = ref<{ focusInput?: () => Promise<void> | void } | null>(null)
const { keyboardHeight } = useChatKeyboardInset()

const {
  messages,
  loading,
  loadingMore,
  prependingHistory,
  hasMoreHistory,
  sending,
  input,
  typingHint,
  targetType,
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
  pickMention,
  onMessageLongPress,
  mentionVisible,
  mentionCandidates,
} = useChatDetail()

const footerBottom = computed(() => (emojiVisible.value ? 0 : keyboardHeight.value))

const messagesPadBottom = computed(() => {
  if (emojiVisible.value) return COMPOSER_H + EMOJI_PANEL_H
  return COMPOSER_H + keyboardHeight.value
})

const { scrollTop, scrollAnimated, scrollToBottom, scrollToBottomSettle, onMediaLoaded, preserveScrollAfterPrepend } =
  useChatScroll()

async function onSend() {
  await send()
  scrollToBottomSettle()
  const refocus = () => composerRef.value?.focusInput?.()
  await nextTick()
  await refocus()
  setTimeout(refocus, 160)
}

async function onLoadMore() {
  await preserveScrollAfterPrepend(() => loadMoreHistory())
}

function onScrollToUpper() {
  if (!loadingMore.value && hasMoreHistory.value) onLoadMore()
}

async function onToggleEmoji() {
  const closing = emojiVisible.value
  toggleEmoji()
  if (closing) {
    await nextTick()
    await composerRef.value?.focusInput?.()
  }
}

watch(
  () => emojiVisible.value,
  (visible) => {
    if (visible) scrollToBottom(true)
  },
)

watch(
  () => messages.value[messages.value.length - 1]?.id,
  (id, prevId) => {
    if (!id || id === prevId) return
    if (prependingHistory.value || loadingMore.value || loading.value) return
    scrollToBottom(false)
  },
)

watch(
  () => loading.value,
  (val, prev) => {
    if (prev && !val) scrollToBottomSettle()
  },
)

onLoad(async (options) => {
  const type = options?.type === 'group' ? 'group' : 'user'
  const id = Number(options?.id)
  const name = decodeURIComponent(options?.name || '')
  const online = options?.online === '1'
  if (!id) return
  await init(type, id, name, online)
  scrollToBottomSettle()
})

onShow(() => {
  if (targetType.value === 'group') refreshGroupContext()
})
</script>

<style lang="scss">
page {
  height: 100%;
  overflow: hidden;
  background: #ededed;
}
</style>

<style lang="scss" scoped>
/* 对齐 PC：消息区 flex:1 滚动，输入区 flex-shrink:0 常驻底部 */
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: #ededed;
}

.chat-page__main {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 16rpx;
  flex-shrink: 0;
  min-height: 112rpx;
  padding: 12rpx 20rpx 12rpx 24rpx;
  background: #f7f7f7;
  border-bottom: 1px solid #e7e7e7;
}

.chat-header__info {
  flex: 1;
  min-width: 0;
}

.chat-header__name {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-header__meta {
  display: block;
  margin-top: 4rpx;
  font-size: 24rpx;
  color: #909399;
}

.chat-header__more {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72rpx;
  height: 72rpx;
  border-radius: 12rpx;
}

.chat-header__more-icon {
  font-size: 44rpx;
  font-weight: 600;
  line-height: 1;
  color: #576b95;
  letter-spacing: 0;
}

.chat-page__messages {
  flex: 1;
  height: 0;
  min-height: 0;
  background: #ededed;
}

.chat-page__list {
  padding: 24rpx 24rpx 16rpx;
  box-sizing: border-box;
}

.chat-page__footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  background: #f7f7f7;
  border-top: 1px solid #e5e5e5;
}

.chat-page__typing {
  padding: 8rpx 24rpx 0;
  font-size: 24rpx;
  color: #909399;
}

.announce-modal {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
}

.announce-modal__panel {
  width: 84%;
  max-height: 70vh;
  padding: 32rpx;
  border-radius: 24rpx;
  background: #fff;
  box-sizing: border-box;
}

.announce-modal__title {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #303133;
}

.announce-modal__meta {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #909399;
}

.announce-modal__body {
  max-height: 40vh;
  margin-top: 24rpx;
}

.announce-modal__content {
  font-size: 28rpx;
  line-height: 1.6;
  color: #606266;
  white-space: pre-wrap;
}

.announce-modal__btn {
  margin-top: 32rpx;
  background: #07c160;
  color: #fff;
  font-size: 28rpx;
  border-radius: 8rpx;
}
</style>
