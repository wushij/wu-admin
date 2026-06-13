<template>
  <view class="login-logs-page">
    <view class="login-logs-hero">
      <view class="login-logs-hero__pattern" />
      <view class="login-logs-hero__glow" />
      <view class="login-logs-hero__body">
        <ModuleIcon icon="clock-o" theme="cyan" size="lg" />
        <view class="login-logs-hero__text">
          <text class="login-logs-hero__title">登录记录</text>
          <text class="login-logs-hero__sub">共 {{ total }} 条全部记录</text>
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

        <view class="log-item__card card--elevated">
          <view class="log-item__head">
            <text class="log-item__time">{{ formatDateTime(item.loginTime) }}</text>
            <DictTag
              :label="item.status === 0 ? '成功' : '失败'"
              :effect="item.status === 0 ? 'success' : 'danger'"
            />
          </view>

          <view class="log-item__meta">
            <view class="log-item__chip">
              <ModuleIcon icon="cluster-o" theme="dept" size="xs" />
              <text class="log-item__chip-text">{{ item.loginLocation || item.ipaddr || '—' }}</text>
            </view>
            <view v-if="item.ipaddr" class="log-item__chip">
              <ModuleIcon icon="desktop-o" theme="indigo" size="xs" />
              <text class="log-item__chip-text">{{ item.ipaddr }}</text>
            </view>
            <view v-if="item.browser || item.os" class="log-item__chip">
              <ModuleIcon icon="apps-o" theme="violet" size="xs" />
              <text class="log-item__chip-text">{{ [item.browser, item.os].filter(Boolean).join(' · ') }}</text>
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
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import { usePageList } from '@/composables/usePageList'
import { getMyLoginLogs } from '@/api/system/profile'
import { formatDateTime } from '@/utils/format'
import type { LoginLogVO } from '@/types/profile'

const { list, loading, refreshing, finished, empty, total, refresh, loadMore } = usePageList<LoginLogVO>(
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
  padding: $page-padding-y $page-padding-x 0;
}

.login-logs-hero {
  @include mine-dark-hero-shell;
}

.login-logs-hero__pattern {
  @include mine-dark-hero-pattern;
}

.login-logs-hero__glow {
  @include mine-dark-hero-glow(rgba(8, 145, 178, 0.28));
}

.login-logs-hero__body {
  @include mine-dark-hero-body;
}

.login-logs-hero__text {
  @include mine-dark-hero-text;
}

.login-logs-hero__title {
  @include mine-dark-hero-title;
}

.login-logs-hero__sub {
  @include mine-dark-hero-sub;
}

.login-logs-page__scroll {
  height: calc(100% - 220rpx);
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
  padding-top: 40rpx;
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
  background: linear-gradient(180deg, $color-border-light 0%, transparent 100%);
}

.log-item__card {
  flex: 1;
  margin-bottom: 16rpx;
  padding: 28rpx 24rpx 24rpx;
}

.log-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 20rpx;
  padding-bottom: 20rpx;
  border-bottom: 1px solid $color-border-light;
}

.log-item__time {
  font-size: $font-size-base;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.log-item__meta {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.log-item__chip {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 14rpx 16rpx;
  border-radius: $radius-md;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.03) 0%, $color-bg-muted 100%);
}

.log-item__chip-text {
  flex: 1;
  min-width: 0;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  word-break: break-all;
  line-height: 1.45;
}
</style>
