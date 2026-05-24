<template>
  <view class="detail-row">
    <text class="detail-row__label">{{ label }}</text>
    <text class="detail-row__value">{{ value || '—' }}</text>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value?: string | number | null
}>()
</script>

<style lang="scss" scoped>
.detail-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
  padding: 28rpx 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-row__label {
  flex-shrink: 0;
  width: 180rpx;
  font-size: 28rpx;
  color: #909399;
}

.detail-row__value {
  flex: 1;
  font-size: 28rpx;
  color: #1e293b;
  text-align: right;
  word-break: break-all;
}
</style>
