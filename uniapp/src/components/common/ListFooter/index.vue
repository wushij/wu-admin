<template>
  <view class="list-footer">
    <text v-if="loading">加载中…</text>
    <text v-else-if="finished && !empty">没有更多了</text>
    <text v-else-if="empty">暂无数据</text>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  loading?: boolean
  finished?: boolean
  empty?: boolean
}>()
</script>

<style lang="scss" scoped>
.list-footer {
  padding: 32rpx;
  text-align: center;
  font-size: 24rpx;
  color: #909399;
}
</style>
