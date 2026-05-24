<template>
  <view class="monitor-toolbar">
    <view class="monitor-toolbar__refresh" @click="emit('refresh')">
      <text class="monitor-toolbar__refresh-icon" :class="{ 'monitor-toolbar__refresh-icon--spin': loading }">↻</text>
      <text class="monitor-toolbar__refresh-text">{{ loading ? '刷新中' : '刷新' }}</text>
    </view>
    <view class="monitor-toolbar__auto">
      <text class="monitor-toolbar__auto-label">自动刷新</text>
      <switch :checked="autoRefresh" color="#4f46e5" @change="onAutoChange" />
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  loading?: boolean
  autoRefresh?: boolean
}>()

const emit = defineEmits<{
  refresh: []
  'toggle-auto': []
}>()

function onAutoChange() {
  emit('toggle-auto')
}
</script>

<style lang="scss" scoped>

.monitor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 16rpx 20rpx;
  margin-bottom: $card-gap;
  border-radius: $radius-md;
  background: $color-bg-card;
  border: 1px solid $color-border-light;
  box-shadow: $shadow-card;
}

.monitor-toolbar__refresh {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  border-radius: $radius-sm;
  background: $color-primary-muted;
}

.monitor-toolbar__refresh-icon {
  font-size: 32rpx;
  color: $color-primary;
  line-height: 1;
}

.monitor-toolbar__refresh-icon--spin {
  animation: spin 0.8s linear infinite;
}

.monitor-toolbar__refresh-text {
  font-size: $font-size-sm;
  color: $color-primary;
  font-weight: $font-weight-semibold;
}

.monitor-toolbar__auto {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.monitor-toolbar__auto-label {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
