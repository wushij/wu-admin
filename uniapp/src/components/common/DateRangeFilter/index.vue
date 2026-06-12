<template>
  <view class="date-range">
    <picker mode="date" :value="beginDate" @change="onBeginChange">
      <view class="date-range__item" :class="{ 'date-range__item--active': beginDate }">
        {{ beginDate || '开始日期' }}
      </view>
    </picker>
    <text class="date-range__sep">至</text>
    <picker mode="date" :value="endDate" @change="onEndChange">
      <view class="date-range__item" :class="{ 'date-range__item--active': endDate }">
        {{ endDate || '结束日期' }}
      </view>
    </picker>
    <text v-if="beginDate || endDate" class="date-range__clear" @click="clear">清除</text>
  </view>
</template>

<script setup lang="ts">
const beginDate = defineModel<string>('beginDate', { default: '' })
const endDate = defineModel<string>('endDate', { default: '' })

const emit = defineEmits<{ change: [] }>()

function onBeginChange(e: { detail: { value: string } }) {
  beginDate.value = e.detail.value
  emit('change')
}

function onEndChange(e: { detail: { value: string } }) {
  endDate.value = e.detail.value
  emit('change')
}

function clear() {
  beginDate.value = ''
  endDate.value = ''
  emit('change')
}
</script>

<style lang="scss" scoped>
.date-range {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
  flex-wrap: wrap;
}

.date-range__item {
  padding: 12rpx 20rpx;
  border-radius: 12rpx;
  background: #fff;
  border: 1px solid #ebeef5;
  font-size: 26rpx;
  color: #909399;
}

.date-range__item--active {
  color: #1e293b;
  border-color: #6366f1;
}

.date-range__sep {
  font-size: 26rpx;
  color: #909399;
}

.date-range__clear {
  font-size: 26rpx;
  color: #6366f1;
  margin-left: auto;
}
</style>
