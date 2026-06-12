<template>
  <view class="page-padded page-list">
    <view class="inbox-head">
      <text class="inbox-head__hint">工单 / 审批等业务提醒</text>
      <text v-if="list.length" class="inbox-head__action" @click="onReadAll">全部已读</text>
    </view>

    <scroll-view
      scroll-y
      class="page-list__scroll"
    >
      <view
        v-for="item in list"
        :key="item.id"
        class="inbox-item"
        :class="{ 'inbox-item--unread': item.readStatus === 0 }"
        @click="onTap(item)"
      >
        <MessageListIcon icon="notes-o" theme="inbox" />
        <view class="inbox-item__body">
          <view class="inbox-item__top">
            <text class="inbox-item__title">{{ item.title }}</text>
            <view class="inbox-item__meta">
              <text v-if="inboxBizLabel(item.bizType)" class="inbox-item__tag">{{ inboxBizLabel(item.bizType) }}</text>
              <text class="inbox-item__time">{{ formatListTime(item.createTime) }}</text>
            </view>
          </view>
          <text class="inbox-item__content">{{ inboxContentPreview(item) }}</text>
        </view>
      </view>

      <EmptyState v-if="!loading && !list.length" title="暂无业务消息" icon="notes-o" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import EmptyState from '@/components/common/EmptyState/index.vue'
import MessageListIcon from '@/components/business/MessageListIcon/index.vue'
import { useInboxList } from '@/composables/useInboxList'
import { formatListTime } from '@/utils/format'
import { openInboxItem, inboxBizLabel, inboxContentPreview } from '@/utils/inbox-nav'
import type { NoticeVO } from '@/types/message'

const { list, loading, refresh, markAllRead } = useInboxList()

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

.page-list {
  height: 100vh;
  box-sizing: border-box;
}

.page-list__scroll {
  height: calc(100% - 72rpx);
}

.inbox-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.inbox-head__hint {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.inbox-head__action {
  font-size: $font-size-sm;
  color: $color-primary;
  font-weight: $font-weight-semibold;
}

.inbox-item {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  margin-bottom: 16rpx;
  padding: 28rpx 24rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
  box-shadow: $shadow-card;
}

.inbox-item__body {
  flex: 1;
  min-width: 0;
}

.inbox-item--unread {
  background: rgba(79, 70, 229, 0.06);
  border: 1px solid rgba(79, 70, 229, 0.12);
}

.inbox-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.inbox-item__meta {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-shrink: 0;
}

.inbox-item__title {
  flex: 1;
  min-width: 0;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inbox-item__time {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.inbox-item__content {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.55;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.inbox-item__tag {
  display: inline-block;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  font-size: $font-size-xs;
  color: $color-primary;
  background: $color-primary-muted;
  line-height: 1.4;
  white-space: nowrap;
}
</style>
