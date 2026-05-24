<template>
  <ChatAvatar :src="displaySrc" :name="name" :online="online" :class="sizeClass" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ChatAvatar from '@/components/business/ChatAvatar/index.vue'
import { fileDisplayUrl } from '@/api/system/file/index'

const props = withDefaults(
  defineProps<{
    src?: string
    name?: string
    online?: boolean
    size?: 'sm' | 'md' | 'lg'
  }>(),
  { size: 'md' },
)

const displaySrc = computed(() => (props.src ? fileDisplayUrl(props.src) : ''))

const sizeClass = computed(() => `user-avatar--${props.size}`)
</script>

<style lang="scss" scoped>
:deep(.user-avatar--sm.chat-avatar-shell) {
  width: 64rpx;
  height: 64rpx;
}

:deep(.user-avatar--lg.chat-avatar-shell) {
  width: 120rpx;
  height: 120rpx;
}

:deep(.user-avatar--sm .chat-avatar__dot) {
  right: 2rpx;
  bottom: 2rpx;
  width: 8rpx;
  height: 8rpx;
  border-width: 1rpx;
}

:deep(.user-avatar--lg .chat-avatar__dot) {
  right: 6rpx;
  bottom: 6rpx;
  width: 14rpx;
  height: 14rpx;
}
</style>
