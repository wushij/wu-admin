<template>
  <view class="login-logs-page">
    <view class="login-logs-page__summary">
      <view class="summary-card">
        <view class="summary-card__icon">
          <IconFont name="clock-o" :size="36" color="#ffffff" />
        </view>
        <view class="summary-card__text">
          <text class="summary-card__title">登录记录</text>
          <text class="summary-card__sub">共 {{ list.length }} 条近期记录</text>
        </view>
      </view>
    </view>

    <ListLoading v-if="loading && !list.length" />

    <scroll-view
      v-else
      scroll-y
      class="login-logs-page__scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <view v-for="(item, index) in list" :key="`${item.loginTime}-${item.ipaddr}`" class="log-item">
        <view class="log-item__rail">
          <view class="log-item__dot" :class="item.status === 0 ? 'log-item__dot--ok' : 'log-item__dot--fail'" />
          <view v-if="index < list.length - 1" class="log-item__line" />
        </view>
        <view class="log-item__card">
          <view class="log-item__head">
            <text class="log-item__time">{{ formatDateTime(item.loginTime) }}</text>
            <DictTag
              :label="item.status === 0 ? '成功' : '失败'"
              :effect="item.status === 0 ? 'success' : 'danger'"
            />
          </view>
          <view class="log-item__meta">
            <view class="log-item__row">
              <IconFont name="cluster-o" :size="26" color="#94a3b8" />
              <text>{{ item.loginLocation || item.ipaddr || '—' }}</text>
            </view>
            <view v-if="item.ipaddr" class="log-item__row">
              <IconFont name="desktop-o" :size="26" color="#94a3b8" />
              <text>{{ item.ipaddr }}</text>
            </view>
            <view v-if="item.browser || item.os" class="log-item__row">
              <IconFont name="apps-o" :size="26" color="#94a3b8" />
              <text>{{ [item.browser, item.os].filter(Boolean).join(' · ') }}</text>
            </view>
          </view>
        </view>
      </view>

      <EmptyState v-if="empty" title="暂无登录记录" icon="clock-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import IconFont from '@/components/common/IconFont/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import { usePageList } from '@/composables/usePageList'
import { getMyLoginLogs } from '@/api/system/profile'
import { formatDateTime } from '@/utils/format'
import type { LoginLogVO } from '@/types/profile'

const { list, loading, refreshing, finished, empty, refresh, loadMore } = usePageList<LoginLogVO>(
  async (pageNo, pageSize) => {
    const res = await getMyLoginLogs({ pageNo, pageSize })
    return { list: res.data?.list || [], total: res.data?.total || 0 }
  },
)

async function onRefresh() {
  await refresh()
}

onMounted(refresh)
onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/mine.scss';

.login-logs-page {
  @include mine-page-bg;
  height: 100vh;
  box-sizing: border-box;
  padding: 24rpx 24rpx 0;
}

.login-logs-page__summary {
  margin-bottom: 24rpx;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 28rpx 32rpx;
  @include mine-card;
}

.summary-card__icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4f46e5, #818cf8);
}

.summary-card__title {
  display: block;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.summary-card__sub {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.login-logs-page__scroll {
  height: calc(100% - 160rpx);
}

.log-item {
  display: flex;
  gap: 20rpx;
  margin-bottom: 8rpx;
}

.log-item__rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 24rpx;
  padding-top: 36rpx;
}

.log-item__dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  flex-shrink: 0;

  &--ok {
    background: #22c55e;
    box-shadow: 0 0 0 6rpx rgba(34, 197, 94, 0.2);
  }

  &--fail {
    background: #ef4444;
    box-shadow: 0 0 0 6rpx rgba(239, 68, 68, 0.2);
  }
}

.log-item__line {
  flex: 1;
  width: 2rpx;
  min-height: 40rpx;
  margin-top: 8rpx;
  background: $color-border-light;
}

.log-item__card {
  flex: 1;
  margin-bottom: 16rpx;
  padding: 28rpx 28rpx 24rpx;
  @include mine-card;
}

.log-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.log-item__time {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.log-item__meta {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.log-item__row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}
</style>
