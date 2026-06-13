<template>
  <view class="inbox-page">
    <view class="msg-hero msg-hero--inbox">
      <view class="msg-hero__pattern" />
      <view class="msg-hero__glow" />
      <view class="msg-hero__body">
        <ModuleIcon icon="notes-o" theme="inbox" size="lg" />
        <view class="msg-hero__text">
          <text class="msg-hero__title">站内信</text>
          <text class="msg-hero__sub">共 {{ list.length }} 条全部消息</text>
          <view class="msg-hero__extras">
            <text v-if="unreadCount" class="msg-hero__badge">{{ unreadCount }} 条未读</text>
            <text v-if="list.length" class="msg-hero__action" @click="onReadAll">全部已读</text>
          </view>
        </view>
      </view>
    </view>

    <ListLoading v-if="loading && !list.length" variant="message" />

    <scroll-view v-else scroll-y class="inbox-page__scroll">
      <view
        v-for="item in list"
        :key="item.id"
        class="msg-card card--elevated"
        :class="{ 'msg-card--unread': item.readStatus === 0 }"
        @click="onTap(item)"
      >
        <view class="msg-card__head">
          <ModuleIcon icon="notes-o" theme="inbox" size="sm" />
          <view class="msg-card__head-main">
            <text class="msg-card__title">{{ item.title }}</text>
            <text class="msg-card__time">{{ formatListTime(item.createTime) }}</text>
          </view>
          <view v-if="item.readStatus === 0" class="msg-card__dot" />
        </view>
        <text class="msg-card__content">{{ inboxContentPreview(item) }}</text>
        <view v-if="inboxBizLabel(item.bizType)" class="msg-card__footer">
          <text class="msg-card__chip">{{ inboxBizLabel(item.bizType) }}</text>
        </view>
      </view>

      <EmptyState v-if="!loading && !list.length" title="暂无业务消息" icon="notes-o" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { useInboxList } from '@/composables/useInboxList'
import { formatListTime } from '@/utils/format'
import { openInboxItem, inboxBizLabel, inboxContentPreview } from '@/utils/inbox-nav'
import type { NoticeVO } from '@/types/message'

const { list, loading, refresh, markAllRead } = useInboxList()

const unreadCount = computed(() => list.value.filter((item) => item.readStatus === 0).length)

function onTap(item: NoticeVO) {
  openInboxItem(item)
}

async function onReadAll() {
  await markAllRead()
  uni.showToast({ title: '已全部标记已读', icon: 'success' })
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

.inbox-page {
  @include mine-page-bg;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: $page-padding-y $page-padding-x calc(24rpx + env(safe-area-inset-bottom));
}

.msg-hero {
  @include mine-dark-hero-shell;

  &--inbox .msg-hero__glow {
    @include mine-dark-hero-glow(rgba(99, 102, 241, 0.28));
  }
}

.msg-hero__pattern {
  @include mine-dark-hero-pattern;
}

.msg-hero__glow {
  @include mine-dark-hero-glow;
}

.msg-hero__body {
  @include mine-dark-hero-body;
}

.msg-hero__text {
  @include mine-dark-hero-text;
}

.msg-hero__title {
  @include mine-dark-hero-title;
}

.msg-hero__sub {
  @include mine-dark-hero-sub;
}

.msg-hero__extras {
  @include mine-dark-hero-extras;
}

.msg-hero__badge {
  padding: 6rpx 20rpx;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: #c7d2fe;
  background: rgba(99, 102, 241, 0.2);
}

.msg-hero__action {
  padding: 6rpx 20rpx;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.inbox-page__scroll {
  flex: 1;
  min-height: 0;
}

.msg-card {
  padding: 28rpx 24rpx;
  margin-bottom: $card-gap;
  border-left: 4rpx solid transparent;
}

.msg-card--unread {
  border-left-color: $color-primary;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.04) 0%, $color-bg-card 48%);
}

.msg-card__head {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.msg-card__head-main {
  flex: 1;
  min-width: 0;
}

.msg-card__title {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-card--unread .msg-card__title {
  color: $color-primary;
}

.msg-card__time {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.msg-card__dot {
  width: 14rpx;
  height: 14rpx;
  margin-top: 8rpx;
  border-radius: 50%;
  background: $color-primary;
  flex-shrink: 0;
}

.msg-card__content {
  display: block;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.55;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.msg-card__footer {
  margin-top: 16rpx;
}

.msg-card__chip {
  display: inline-block;
  padding: 6rpx 16rpx;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: $color-primary;
  background: $color-primary-muted;
}
</style>
