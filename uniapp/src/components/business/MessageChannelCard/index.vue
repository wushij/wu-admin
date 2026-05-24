<template>
  <view class="channel-card card" @click="emit('click')">
    <ModuleIcon :icon="icon" :theme="resolvedTheme" size="lg" />
    <view class="channel-card__body">
      <text class="channel-card__title">{{ title }}</text>
      <text class="channel-card__desc">{{ desc }}</text>
    </view>
    <view class="channel-card__tail">
      <view v-if="(badge ?? 0) > 0" class="channel-card__badge">
        {{ (badge ?? 0) > 99 ? '99+' : badge }}
      </view>
      <IconFont name="arrow" :size="28" color="#c0c4cc" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import type { IconName } from '@/constants/iconfont'

const props = defineProps<{
  title: string
  desc: string
  icon: IconName
  theme: string
  badge?: number
}>()

const emit = defineEmits<{ click: [] }>()

const resolvedTheme = computed(() => {
  if (props.theme === 'chat') return 'emerald'
  return props.theme
})
</script>

<style lang="scss" scoped>
.channel-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 20rpx;
}

.channel-card__body {
  flex: 1;
  min-width: 0;
}

.channel-card__title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #1e293b;
}

.channel-card__desc {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #64748b;
}

.channel-card__tail {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.channel-card__badge {
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 10rpx;
  border-radius: 999rpx;
  background: #fa5151;
  color: #fff;
  font-size: 20rpx;
  line-height: 32rpx;
  text-align: center;
}
</style>
