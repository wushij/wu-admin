<template>
  <view class="list-loading" :class="`list-loading--${variant}`">
    <template v-if="variant === 'dashboard'">
      <view class="sk sk-hero" />
      <view class="sk-grid">
        <view v-for="i in 4" :key="i" class="sk sk-stat" />
      </view>
      <view v-for="i in 2" :key="`todo-${i}`" class="sk sk-row" />
    </template>
    <template v-else-if="variant === 'message'">
      <view v-for="i in 5" :key="i" class="sk-msg">
        <view class="sk sk-avatar" />
        <view class="sk-msg__lines">
          <view class="sk sk-line sk-line--lg" />
          <view class="sk sk-line sk-line--sm" />
        </view>
      </view>
    </template>
    <template v-else>
      <view v-for="i in count" :key="i" class="sk sk-card" />
    </template>
  </view>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'list' | 'dashboard' | 'message'
    count?: number
  }>(),
  {
    variant: 'list',
    count: 3,
  },
)
</script>

<style lang="scss" scoped>
@keyframes shimmer {
  0% { opacity: 0.55; }
  50% { opacity: 1; }
  100% { opacity: 0.55; }
}

.sk {
  background: #f0f0f0;
  border-radius: 24rpx;
  animation: shimmer 1.2s linear infinite;
}

.list-loading--dashboard .sk-hero {
  height: 280rpx;
  margin-bottom: 24rpx;
  border-radius: 32rpx;
}

.sk-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.sk-stat {
  height: 200rpx;
}

.sk-row {
  height: 96rpx;
  margin-bottom: 24rpx;
}

.sk-card {
  height: 120rpx;
  margin-bottom: 24rpx;
}

.sk-msg {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 0;
}

.sk-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
}

.sk-msg__lines {
  flex: 1;
}

.sk-line {
  height: 28rpx;
  border-radius: 8rpx;
  margin-bottom: 12rpx;
}

.sk-line--lg { width: 60%; }
.sk-line--sm { width: 40%; }
</style>
