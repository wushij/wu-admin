<template>
  <view class="announce-page">
    <view class="msg-hero msg-hero--announce">
      <view class="msg-hero__pattern" />
      <view class="msg-hero__glow" />
      <view class="msg-hero__body">
        <ModuleIcon icon="bell" theme="notice" size="lg" />
        <view class="msg-hero__text">
          <text class="msg-hero__title">系统公告</text>
          <text class="msg-hero__sub">共 {{ total }} 条全部公告</text>
          <view class="msg-hero__extras">
            <text v-if="unreadCount" class="msg-hero__badge">{{ unreadCount }} 条未读</text>
            <text v-if="list.length" class="msg-hero__action" @click="onReadAll">全部已读</text>
          </view>
        </view>
      </view>
    </view>

    <ListLoading v-if="loading && !list.length" variant="message" />

    <scroll-view
      v-else
      scroll-y
      class="announce-page__scroll"
      @scrolltolower="loadMore"
    >
      <view
        v-for="item in list"
        :key="item.id"
        class="msg-card card--elevated"
        :class="{ 'msg-card--unread': item.isRead === 0 }"
        @click="goDetail(item.id)"
      >
        <view class="msg-card__head">
          <ModuleIcon icon="bell" theme="notice" size="sm" />
          <view class="msg-card__head-main">
            <text class="msg-card__title">{{ item.title }}</text>
            <text class="msg-card__time">{{ formatListTime(item.createTime) }}</text>
          </view>
          <view v-if="item.isRead === 0" class="msg-card__dot" />
        </view>
        <text class="msg-card__content">{{ summarizeText(item.content) }}</text>
        <view v-if="item.createName" class="msg-card__footer">
          <text class="msg-card__chip">{{ item.createName }}</text>
        </view>
      </view>

      <EmptyState v-if="empty && !loading" title="暂无公告" icon="bell" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useNoticeWs } from '@/composables/useNoticeWs'
import { getMyAnnounce, readAllAnnounce } from '@/api/message'
import { useMessageStore } from '@/store/message'
import { formatListTime, summarizeText } from '@/utils/format'

const { list, loading, finished, empty, total, refresh, loadMore } = usePageList(
  async (pageNo, pageSize) => {
    const res = await getMyAnnounce({ pageNo, pageSize })
    return { list: res.data?.list || [], total: res.data?.total || 0 }
  },
)

const unreadCount = computed(() => list.value.filter((item) => item.isRead === 0).length)

useNoticeWs(refresh)

async function onReadAll() {
  await readAllAnnounce()
  list.value.forEach((item) => {
    item.isRead = 1
  })
  await useMessageStore().refreshSummary()
  uni.showToast({ title: '已全部标记已读', icon: 'success' })
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages-sub/msg/announce/detail?id=${id}` })
}

onMounted(refresh)

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/mine.scss' as *;

.announce-page {
  @include mine-page-bg;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: $page-padding-y $page-padding-x calc(24rpx + env(safe-area-inset-bottom));
}

.msg-hero {
  @include mine-dark-hero-shell;

  &--announce .msg-hero__glow {
    @include mine-dark-hero-glow(rgba(236, 72, 153, 0.28));
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
  color: #fde68a;
  background: rgba(245, 158, 11, 0.18);
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

.announce-page__scroll {
  flex: 1;
  min-height: 0;
}

.msg-card {
  padding: 28rpx 24rpx;
  margin-bottom: $card-gap;
  border-left: 4rpx solid transparent;
}

.msg-card--unread {
  border-left-color: #f59e0b;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.04) 0%, $color-bg-card 48%);
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
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.msg-card--unread .msg-card__title {
  color: #b45309;
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
  background: #f59e0b;
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
  color: #b45309;
  background: rgba(245, 158, 11, 0.1);
}
</style>
