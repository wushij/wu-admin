<template>
  <view
    class="chat-bubble-row"
    :class="{
      'chat-bubble-row--self': self && !recalled,
      'chat-bubble-row--recalled': recalled,
    }"
  >
    <ChatAvatar
      v-if="showAvatar"
      class="chat-bubble-row__avatar"
      :src="avatar"
      :name="senderName"
    />
    <view class="chat-bubble-wrap" @longpress="emit('longpress')">
      <text v-if="showSenderLabel" class="chat-bubble__sender">{{ senderName }}</text>
      <view
        class="chat-bubble"
        :class="[
          self ? 'chat-bubble--self' : 'chat-bubble--other',
          { 'chat-bubble--recalled': recalled, 'chat-bubble--image': isImage, 'chat-bubble--file': isFile },
        ]"
        @click="onBubbleClick"
      >
        <image
          v-if="isImage && mediaUrl"
          class="chat-bubble__image"
          :src="mediaUrl"
          :style="imageDisplayStyle"
          mode="scaleToFill"
          @load="onImageLoad"
        />
        <view v-else-if="isFile && fileInfo" class="chat-bubble__file">
          <text class="chat-bubble__file-icon">📎</text>
          <text class="chat-bubble__file-name">{{ fileInfo.name }}</text>
        </view>
        <text v-else class="chat-bubble__text">{{ text }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import ChatAvatar from '@/components/business/ChatAvatar/index.vue'
import { CHAT_MSG_TYPE } from '@/constants/chat'
import { fileDisplayUrl } from '@/api/system/file/index'
import {
  previewMessageText,
  isRecalledMessage,
  resolveMediaUrl,
  getFileInfo,
  resolveEffectiveMsgType,
} from '@/utils/chat-message'
import type { ChatMessage } from '@/types/message'

const props = withDefaults(
  defineProps<{
    message: Pick<ChatMessage, 'content' | 'msgType' | 'senderAvatar'>
    self?: boolean
    senderName?: string
    avatar?: string
    mode?: 'private' | 'group'
  }>(),
  { mode: 'private' },
)

const emit = defineEmits<{ longpress: []; 'media-loaded': [] }>()

const recalled = computed(() => isRecalledMessage(props.message))
const showAvatar = computed(() => !recalled.value)
const showSenderLabel = computed(
  () => props.mode === 'group' && !props.self && !!props.senderName?.trim(),
)
const isImage = computed(() => resolveEffectiveMsgType(props.message) === CHAT_MSG_TYPE.IMAGE)
const isFile = computed(() => resolveEffectiveMsgType(props.message) === CHAT_MSG_TYPE.FILE)
const mediaUrl = computed(() => (isImage.value ? resolveMediaUrl(props.message) : ''))
const fileInfo = computed(() => getFileInfo(props.message))
const text = computed(() => previewMessageText(props.message))

const IMAGE_MAX_RPX = 320
const imageDisplayStyle = ref<Record<string, string>>({})

watch(
  () => mediaUrl.value,
  () => {
    imageDisplayStyle.value = {}
  },
)

function onImageLoad(e: { detail?: { width?: number; height?: number } }) {
  const width = e.detail?.width || 0
  const height = e.detail?.height || 0
  if (!width || !height) {
    emit('media-loaded')
    return
  }
  const maxPx = uni.upx2px(IMAGE_MAX_RPX)
  const scale = Math.min(maxPx / width, maxPx / height, 1)
  imageDisplayStyle.value = {
    width: `${Math.round(width * scale)}px`,
    height: `${Math.round(height * scale)}px`,
  }
  emit('media-loaded')
}

function onBubbleClick() {
  if (isImage.value && mediaUrl.value) {
    uni.previewImage({ urls: [mediaUrl.value] })
    return
  }
  if (isFile.value && fileInfo.value?.url) {
    const url = fileDisplayUrl(fileInfo.value.url)
    uni.showLoading({ title: '打开中' })
    uni.downloadFile({
      url,
      success: (res) => {
        uni.openDocument({
          filePath: res.tempFilePath,
          showMenu: true,
          fail: () => uni.showToast({ title: '无法打开文件', icon: 'none' }),
        })
      },
      fail: () => uni.showToast({ title: '下载失败', icon: 'none' }),
      complete: () => uni.hideLoading(),
    })
  }
}
</script>

<style lang="scss" scoped>

/* 对齐 PC：DOM 顺序始终 [头像][气泡]，自己消息 row-reverse + margin-left:auto */
.chat-bubble-row {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  max-width: min(72%, 520rpx);
  margin-bottom: 24rpx;
}

.chat-bubble-row--self {
  flex-direction: row-reverse;
  margin-left: auto;
}

.chat-bubble-row--recalled {
  justify-content: center;
  max-width: 100%;
  margin-left: 0;
  margin-right: 0;
}

.chat-bubble-row--recalled .chat-bubble-wrap {
  max-width: 100%;
}

.chat-bubble-row__avatar {
  width: 80rpx !important;
  height: 80rpx !important;
  margin-top: 4rpx;
  flex-shrink: 0;
}

.chat-bubble-wrap {
  max-width: 100%;
  min-width: 0;
}

.chat-bubble-row--self .chat-bubble-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.chat-bubble__sender {
  display: block;
  margin-bottom: 8rpx;
  font-size: $font-size-xs;
  color: #888;
}

.chat-bubble {
  display: inline-block;
  max-width: 100%;
  padding: 18rpx 24rpx;
  border-radius: $radius-sm;
  font-size: $font-size-base;
  line-height: 1.65;
  word-break: break-word;
}

.chat-bubble--image,
.chat-bubble--file {
  padding: 0;
  background: transparent !important;
  box-shadow: none;
}

.chat-bubble--other.chat-bubble--file {
  padding: 8rpx;
  background: $color-chat-bubble-other !important;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.chat-bubble--self.chat-bubble--file {
  padding: 8rpx;
  background: $color-chat-bubble-self !important;
}

.chat-bubble--self.chat-bubble--image,
.chat-bubble--other.chat-bubble--image {
  padding: 0;
  background: transparent !important;
  box-shadow: none;
  border-radius: 0;
}

.chat-bubble--other {
  background: $color-chat-bubble-other;
  color: #191919;
  border-bottom-left-radius: 4rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.chat-bubble--self {
  background: $color-chat-bubble-self;
  color: #191919;
  border-bottom-right-radius: 4rpx;
}

.chat-bubble--recalled {
  background: rgba(0, 0, 0, 0.06) !important;
  color: #888;
  font-size: $font-size-sm;
  box-shadow: none;
  padding: 8rpx 20rpx;
  border-radius: 8rpx;
}

.chat-bubble__image {
  display: block;
  max-width: 320rpx;
  max-height: 320rpx;
  border-radius: 8rpx;
  vertical-align: top;
}

.chat-bubble__file {
  display: flex;
  align-items: center;
  gap: 12rpx;
  min-width: 200rpx;
  padding: 16rpx 20rpx;
  border-radius: $radius-sm;
  background: $color-chat-bubble-other;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.chat-bubble--self .chat-bubble__file {
  background: $color-chat-bubble-self;
  box-shadow: none;
}

.chat-bubble__file-icon {
  font-size: 32rpx;
}

.chat-bubble__file-name {
  flex: 1;
  font-size: 26rpx;
  color: $color-chat-link;
  word-break: break-all;
}
</style>
