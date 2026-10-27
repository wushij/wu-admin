<template>
  <view class="announce-bar" @click="emit('open')">
    <text class="announce-bar__icon">📢</text>
    <text class="announce-bar__text">{{ preview }}</text>
    <text class="announce-bar__arrow">›</text>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  announcement?: string
}>()

const emit = defineEmits<{
  open: []
}>()

const preview = computed(() => {
  const text = (props.announcement || '').trim()
  if (!text) return '群公告'
  return text.length > 36 ? `${text.slice(0, 36)}…` : text
})
</script>

<style lang="scss" scoped>
.announce-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 24rpx;
  background: #fff7e6;
  border-bottom: 1px solid #ffe7ba;
}

.announce-bar__icon {
  flex-shrink: 0;
  font-size: 28rpx;
}

.announce-bar__text {
  flex: 1;
  font-size: 26rpx;
  color: #d48806;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.announce-bar__arrow {
  flex-shrink: 0;
  font-size: 32rpx;
  color: #d48806;
}
</style>
