<template>
  <view
    v-if="messageStore.showNotification && messageStore.currentNotification"
    class="msg-notify"
    @click="handleView"
  >
    <view class="msg-notify__head">
      <IconFont
        :name="messageStore.currentNotification.type === 'notice' ? 'bell' : 'chat-o'"
        :size="36"
        color="#010710"
      />
      <text class="msg-notify__title">{{ messageStore.currentNotification.title }}</text>
      <text class="msg-notify__close" @click.stop="messageStore.closeNotification()">×</text>
    </view>
    <text class="msg-notify__content">{{ messageStore.currentNotification.content }}</text>
    <text class="msg-notify__hint">点击查看</text>
  </view>
</template>

<script setup lang="ts">
import IconFont from '@/components/common/IconFont/index.vue'
import { readAnnounce } from '@/api/message'
import { useMessageStore } from '@/store/message'

const messageStore = useMessageStore()

async function handleView() {
  const n = messageStore.currentNotification
  if (!n) return
  messageStore.closeNotification()

  if (n.type === 'notice') {
    if (n.announceId) {
      try {
        await readAnnounce(n.announceId)
        await messageStore.refreshSummary()
        messageStore.announceListTick++
      } catch {
        /* ignore */
      }
      uni.navigateTo({ url: `/pages-sub/msg/announce/detail?id=${n.announceId}` })
      return
    }
    uni.switchTab({ url: '/pages/message/index' })
    return
  }

  if (n.groupId != null) {
    uni.navigateTo({
      url: `/pages-sub/msg/chat/detail?type=group&id=${n.groupId}&name=${encodeURIComponent(n.title)}`,
    })
    return
  }

  if (n.senderId != null) {
    uni.navigateTo({
      url: `/pages-sub/msg/chat/detail?type=user&id=${n.senderId}&name=${encodeURIComponent(n.title)}`,
    })
    return
  }

  uni.navigateTo({ url: '/pages-sub/msg/chat/index' })
}
</script>

<style lang="scss" scoped>
.msg-notify {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(120rpx + env(safe-area-inset-bottom));
  z-index: 9999;
  padding: 24rpx;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.12);
}

.msg-notify__head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.msg-notify__title {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-notify__close {
  font-size: 40rpx;
  line-height: 1;
  color: #909399;
  padding: 0 8rpx;
}

.msg-notify__content {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #606266;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.msg-notify__hint {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #6366f1;
}
</style>
