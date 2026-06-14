<template>
  <view class="welcome">
    <view class="welcome__pattern" />
    <view class="welcome__glow" />
    <view class="welcome__body">
      <view class="welcome__head">
        <view class="welcome__avatar">
          <image
            v-if="avatar && !avatarBroken"
            class="welcome__avatar-img"
            :src="avatar"
            mode="aspectFill"
            @error="avatarBroken = true"
          />
          <view v-else class="welcome__avatar-fallback">
            <text>{{ avatarFallback }}</text>
          </view>
        </view>
        <view class="welcome__text">
          <text class="welcome__title">欢迎回来，{{ nickname || '用户' }}</text>
          <text class="welcome__greet">{{ greetingMessage }}</text>
          <text v-if="currentTime" class="welcome__time">{{ currentTime }}</text>
        </view>
      </view>

      <view v-if="stats.length" class="welcome__stats">
        <view v-for="item in stats" :key="item.label" class="welcome__stat">
          <text class="welcome__stat-value">{{ item.value }}</text>
          <text class="welcome__stat-label">{{ item.label }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useDashboardClock } from '@/composables/useDashboardClock'

export interface HeroStatItem {
  label: string
  value: string | number
}

const props = defineProps<{
  avatar?: string
  nickname?: string
  stats?: HeroStatItem[]
}>()

const avatarBroken = ref(false)
const { greetingMessage, currentTime } = useDashboardClock()

const avatarFallback = computed(() => (props.nickname || 'U').slice(0, 1).toUpperCase())
const stats = computed(() => props.stats || [])

watch(
  () => props.avatar,
  () => {
    avatarBroken.value = false
  },
)
</script>

<style lang="scss" scoped>

.welcome {
  position: relative;
  margin-bottom: $section-gap;
  border-radius: $radius-lg;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(135deg, #010710 0%, #0f1a2e 55%, #000000 100%);
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.18);
}

.welcome__pattern {
  position: absolute;
  inset: 0;
  opacity: 0.07;
  background-image:
    radial-gradient(circle at 18% 42%, #fff 1px, transparent 1px),
    radial-gradient(circle at 82% 18%, #fff 1px, transparent 1px);
  background-size: 36rpx 36rpx;
  pointer-events: none;
}

.welcome__glow {
  position: absolute;
  top: -40%;
  right: -8%;
  width: 360rpx;
  height: 360rpx;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.12) 0%, transparent 68%);
  pointer-events: none;
}

.welcome__body {
  position: relative;
  z-index: 1;
  padding: 32rpx;
}

.welcome__head {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding-left: 12rpx;
}

.welcome__avatar {
  flex-shrink: 0;
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  overflow: hidden;
  border: 3rpx solid rgba(255, 255, 255, 0.38);
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.18);
}

.welcome__avatar-img,
.welcome__avatar-fallback {
  width: 100%;
  height: 100%;
}

.welcome__avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.12);
  font-size: 44rpx;
  font-weight: $font-weight-bold;
  color: #fff;
}

.welcome__text {
  flex: 1;
  min-width: 0;
}

.welcome__title {
  display: block;
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: #fff;
  line-height: 1.35;
}

.welcome__greet {
  display: block;
  margin-top: 10rpx;
  font-size: $font-size-sm;
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.55;
}

.welcome__time {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-xs;
  color: rgba(255, 255, 255, 0.58);
}

.welcome__stats {
  display: flex;
  margin-top: 28rpx;
  padding-top: 28rpx;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}

.welcome__stat {
  flex: 1;
  text-align: center;
  position: relative;

  &:not(:last-child)::after {
    content: '';
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 1px;
    height: 48rpx;
    background: rgba(255, 255, 255, 0.15);
  }
}

.welcome__stat-value {
  display: block;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: #fff;
}

.welcome__stat-label {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: rgba(255, 255, 255, 0.65);
}
</style>
