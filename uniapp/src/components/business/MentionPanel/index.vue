<template>
  <scroll-view v-if="candidates.length" scroll-y class="mention-panel">
    <view
      v-for="m in candidates"
      :key="m.userId"
      class="mention-panel__item"
      @click="emit('pick', m)"
    >
      <text class="mention-panel__name">{{ displayName(m) }}</text>
    </view>
  </scroll-view>
</template>

<script setup lang="ts">
import { groupMemberDisplayName } from '@/utils/chat-message'
import type { GroupMember } from '@/types/message'

defineProps<{
  candidates: GroupMember[]
}>()

const emit = defineEmits<{ pick: [member: GroupMember] }>()

function displayName(m: GroupMember) {
  return groupMemberDisplayName(m) || '用户'
}
</script>

<style lang="scss" scoped>
.mention-panel {
  max-height: 320rpx;
  margin: 0 24rpx 8rpx;
  border-radius: 12rpx;
  background: #fff;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);
}

.mention-panel__item {
  padding: 24rpx 28rpx;
  border-bottom: 1px solid #f0f0f0;
}

.mention-panel__name {
  font-size: 28rpx;
  color: #576b95;
}
</style>
