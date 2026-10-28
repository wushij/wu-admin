<template>
  <view class="detail-row">
    <text class="detail-row__label">{{ label }}</text>
    <view class="detail-row__value">
      <slot name="value">{{ displayValue }}</slot>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
  value?: string | number | null
}>()

/** 仅 null / undefined / 空串视为缺失，数字 0 等有效值照常展示 */
const displayValue = computed(() =>
  props.value === null || props.value === undefined || props.value === '' ? '—' : props.value
)
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
