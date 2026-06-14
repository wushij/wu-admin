<template>
  <view v-if="detail" class="inbox-detail">
    <view class="detail-hero detail-hero--inbox">
      <view class="detail-hero__pattern" />
      <view class="detail-hero__glow" />
      <view class="detail-hero__body">
        <ModuleIcon icon="notes-o" theme="inbox" size="lg" />
        <view class="detail-hero__text">
          <view v-if="bizLabel || detail.readStatus === 0" class="detail-hero__tags">
            <text v-if="bizLabel" class="detail-hero__tag">{{ bizLabel }}</text>
            <text v-if="detail.readStatus === 0" class="detail-hero__tag detail-hero__tag--unread">未读</text>
          </view>
          <text class="detail-hero__title">{{ detail.title }}</text>
          <text class="detail-hero__time">{{ formatDateTime(detail.createTime, true) }}</text>
        </view>
      </view>
    </view>

    <view class="detail-section card--elevated">
      <view class="detail-section__head">
        <ModuleIcon icon="chat-o" theme="inbox" size="sm" />
        <text class="detail-section__title">消息内容</text>
      </view>
      <view class="detail-section__content">
        <text class="detail-section__text">{{ detail.content }}</text>
      </view>
    </view>

    <view v-if="hasBizAction" class="detail-section card--elevated">
      <button class="detail-action-btn" @click="goBiz">{{ bizActionText }}</button>
    </view>
  </view>

  <view v-else class="inbox-detail inbox-detail--empty">
    <EmptyState title="消息不存在" icon="notes-o" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { formatDateTime } from '@/utils/format'
import { readInboxCache, inboxBizLabel } from '@/utils/inbox-nav'
import type { NoticeVO } from '@/types/message'

const detail = ref<NoticeVO | null>(null)

const bizLabel = computed(() => inboxBizLabel(detail.value?.bizType))
const hasBizAction = computed(
  () =>
    (detail.value?.bizType === 'TICKET' || detail.value?.bizType === 'APPROVAL') &&
    !!detail.value?.bizId,
)
const bizActionText = computed(() =>
  detail.value?.bizType === 'TICKET' ? '查看关联工单' : '查看关联审批',
)

function goBiz() {
  const item = detail.value
  if (!item?.bizId) return
  if (item.bizType === 'TICKET') {
    uni.navigateTo({ url: `/pages-sub/system/ticket/detail?id=${item.bizId}` })
    return
  }
  if (item.bizType === 'APPROVAL') {
    uni.navigateTo({ url: `/pages-sub/system/approval/detail?id=${item.bizId}` })
  }
}

onLoad((options) => {
  const id = Number(options?.id)
  if (!id) return
  detail.value = readInboxCache(id)
  if (detail.value?.title) {
    uni.setNavigationBarTitle({ title: detail.value.title })
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/mine.scss' as *;

.inbox-detail {
  @include mine-page-bg;
  min-height: 100vh;
  padding: $page-padding-y $page-padding-x 48rpx;
  box-sizing: border-box;

  &--empty {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.detail-hero {
  @include mine-dark-hero-shell;

  &--inbox .detail-hero__glow {
    @include mine-dark-hero-glow(rgba(99, 102, 241, 0.28));
  }
}

.detail-hero__pattern {
  @include mine-dark-hero-pattern;
}

.detail-hero__glow {
  @include mine-dark-hero-glow;
}

.detail-hero__body {
  @include mine-dark-hero-body;
  align-items: flex-start;
}

.detail-hero__text {
  @include mine-dark-hero-text;
}

.detail-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-bottom: 12rpx;
}

.detail-hero__tag {
  padding: 6rpx 18rpx;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: #c7d2fe;
  background: rgba(99, 102, 241, 0.2);

  &--unread {
    color: #fde68a;
    background: rgba(245, 158, 11, 0.18);
  }
}

.detail-hero__title {
  @include mine-dark-hero-title;
  font-size: $font-size-lg;
  line-height: 1.4;
}

.detail-hero__time {
  @include mine-dark-hero-sub;
}

.detail-section {
  padding: 28rpx 28rpx 24rpx;
  margin-bottom: $card-gap;
}

.detail-section__head {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.detail-section__title {
  font-size: $font-size-md;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.detail-section__content {
  padding: 24rpx;
  border-radius: $radius-md;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.04) 0%, $color-bg-muted 100%);
}

.detail-section__text {
  display: block;
  font-size: $font-size-base;
  color: $color-text-primary;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-action-btn {
  @include mine-primary-btn;
  width: 100%;
}
</style>
