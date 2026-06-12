<template>
  <view class="chat-feed">
    <view v-if="loadingMore" class="chat-feed__hint">加载中…</view>
    <view
      v-else-if="hasMore && messages.length"
      class="chat-feed__hint chat-feed__hint--btn"
      @click="emit('load-more')"
    >
      查看更多历史消息
    </view>

    <template v-for="item in items" :key="item.id">
      <view v-if="item.kind === 'time'" class="chat-feed__time">
        <text>{{ item.text }}</text>
      </view>
      <view v-else-if="item.kind === 'system' || item.kind === 'recall'" class="chat-feed__system">
        <text>{{ item.text }}</text>
      </view>
      <ChatBubble
        v-else
        :message="item.message"
        :self="isSelf(item.message)"
        :mode="mode"
        :sender-name="displayName(item.message)"
        :avatar="displayAvatar(item.message)"
        @longpress="emit('longpress', item.message)"
        @media-loaded="emit('media-loaded')"
      />
    </template>

    <view v-if="!messages.length && !loading" class="chat-feed__empty">
      <text>{{ emptyText }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ChatBubble from '@/components/business/ChatBubble/index.vue'
import { buildChatRenderItems } from '@/utils/chat-render'
import type { ChatMessage } from '@/types/message'

const props = withDefaults(
  defineProps<{
    messages: ChatMessage[]
    mode: 'private' | 'group'
    currentUserId?: number
    loading?: boolean
    loadingMore?: boolean
    hasMore?: boolean
    emptyText?: string
    resolveAvatar?: (msg: ChatMessage) => string | undefined
    resolveSenderName?: (msg: ChatMessage) => string
  }>(),
  {
    loading: false,
    loadingMore: false,
    hasMore: false,
    emptyText: '暂无消息',
  },
)

const emit = defineEmits<{
  'load-more': []
  longpress: [message: ChatMessage]
  'media-loaded': []
}>()

const items = computed(() =>
  buildChatRenderItems(props.messages, props.currentUserId || 0, props.mode === 'group'),
)

function isSelf(msg: ChatMessage) {
  return msg.senderId != null && msg.senderId === props.currentUserId
}

function displayName(msg: ChatMessage) {
  return props.resolveSenderName?.(msg) || msg.senderName || ''
}

function displayAvatar(msg: ChatMessage) {
  return props.resolveAvatar?.(msg)
}
</script>

<style lang="scss" scoped>
.chat-feed__hint {
  padding: 16rpx 0 8rpx;
  text-align: center;
  font-size: 24rpx;
  color: #576b95;
}

.chat-feed__hint--btn {
  padding: 20rpx 0;
}

.chat-feed__time {
  display: flex;
  justify-content: center;
  margin: 16rpx 0 12rpx;
}

.chat-feed__time text {
  padding: 8rpx 20rpx;
  border-radius: 8rpx;
  background: rgba(0, 0, 0, 0.18);
  color: #fff;
  font-size: 22rpx;
  line-height: 1.4;
}

.chat-feed__system {
  display: flex;
  justify-content: center;
  margin: 8rpx 0 16rpx;
}

.chat-feed__system text {
  max-width: 88%;
  padding: 8rpx 20rpx;
  border-radius: 8rpx;
  background: rgba(0, 0, 0, 0.06);
  color: #888;
  font-size: 24rpx;
  line-height: 1.45;
  text-align: center;
}

.chat-feed__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240rpx;
  color: #999;
  font-size: 28rpx;
}
</style>
