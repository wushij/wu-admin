<template>
  <view class="log-row" @click="expanded = !expanded">
    <view class="log-row__head">
      <view class="log-row__main">
        <text class="log-row__title">{{ title }}</text>
        <text class="log-row__meta">{{ meta }}</text>
      </view>
      <DictTag v-if="statusLabel" :label="statusLabel" :effect="statusEffect" />
    </view>
    <view v-if="expanded && detail" class="log-row__detail">
      <text class="log-row__detail-text">{{ detail }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import DictTag from '@/components/common/DictTag/index.vue'

withDefaults(
  defineProps<{
    title: string
    meta?: string
    detail?: string
    statusLabel?: string
    statusEffect?: 'default' | 'success' | 'warning' | 'danger' | 'primary'
  }>(),
  { statusEffect: 'default' },
)

const expanded = ref(false)
</script>

<style lang="scss" scoped>
.log-row {
  padding: 28rpx 24rpx;
  margin-bottom: 16rpx;
  border-radius: 24rpx;
  background: #fff;
  border: 1px solid #f0f0f0;
}

.log-row__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.log-row__main {
  flex: 1;
  min-width: 0;
}

.log-row__title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #1e293b;
}

.log-row__meta {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #909399;
}

.log-row__detail {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1px solid #f0f0f0;
}

.log-row__detail-text {
  font-size: 24rpx;
  line-height: 1.6;
  color: #606266;
  word-break: break-all;
}
</style>
