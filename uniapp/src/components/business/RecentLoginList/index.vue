<template>
  <view v-if="rows.length" class="section-block">
    <view class="recent card--elevated">
      <view class="recent__head">
        <text class="section-title recent__title">最近登录</text>
        <text class="recent__link" @click="goAll">查看全部</text>
      </view>
      <view v-for="(row, idx) in rows" :key="idx" class="recent__row">
        <view class="recent__avatar-wrap">
          <ChatAvatar :src="row.avatar" :name="row.nickname || row.username" />
        </view>
        <view class="recent__main">
          <text class="recent__user">{{ row.nickname || row.username || '—' }}</text>
          <text class="recent__ip">{{ row.ipaddr || '—' }}</text>
        </view>
        <view class="recent__tail">
          <DictTag :label="row.status === 0 ? '成功' : '失败'" :effect="row.status === 0 ? 'success' : 'danger'" />
          <text class="recent__time">{{ formatListTime(row.loginTime) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import ChatAvatar from '@/components/business/ChatAvatar/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import { formatListTime } from '@/utils/format'
import type { RecentLogin } from '@/api/dashboard'

defineProps<{
  rows: RecentLogin[]
}>()

function goAll() {
  uni.navigateTo({ url: '/pages-sub/log/login-log' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.recent {
  padding: 28rpx 32rpx 16rpx;
}

.recent__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.recent__title {
  margin-bottom: 0;
}

.recent__link {
  font-size: 26rpx;
  color: $color-chat-link;
}

.recent__row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 0;
  border-top: 1px solid $color-border-light;
}

.recent__avatar-wrap :deep(.chat-avatar) {
  width: 72rpx;
  height: 72rpx;
}

.recent__main {
  flex: 1;
  min-width: 0;
}

.recent__user {
  display: block;
  font-size: $font-size-base;
  font-weight: 500;
  color: $color-text-primary;
}

.recent__ip {
  display: block;
  margin-top: 4rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.recent__tail {
  flex-shrink: 0;
  text-align: right;
}

.recent__time {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}
</style>
