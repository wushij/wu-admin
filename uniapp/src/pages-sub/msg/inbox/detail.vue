<template>
  <view class="inbox-detail" v-if="detail">
    <view class="inbox-hero card--elevated">
      <view class="inbox-hero__top">
        <DictTag v-if="bizLabel" :label="bizLabel" effect="primary" />
        <view v-if="detail.readStatus === 0" class="inbox-hero__unread">未读</view>
      </view>
      <text class="inbox-hero__title">{{ detail.title }}</text>
      <text class="inbox-hero__time">{{ formatDateTime(detail.createTime, true) }}</text>
    </view>

    <view class="section card--elevated">
      <text class="section__title">消息内容</text>
      <view class="content-box">
        <text class="content-box__text">{{ detail.content }}</text>
      </view>
    </view>

    <view v-if="hasBizAction" class="section card--elevated">
      <button class="action-btn" @click="goBiz">{{ bizActionText }}</button>
    </view>
  </view>
  <view v-else class="page-padded">
    <EmptyState title="消息不存在" icon="notes-o" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
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
@import '@/styles/variables.scss';

.inbox-detail {
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
  background: $color-bg-page;
}

.inbox-hero {
  padding: 32rpx;
  margin-bottom: $card-gap;
}

.inbox-hero__top {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.inbox-hero__unread {
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  font-size: $font-size-xs;
  color: $color-primary;
  background: $color-primary-muted;
}

.inbox-hero__title {
  display: block;
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.45;
}

.inbox-hero__time {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.section {
  padding: 28rpx 32rpx 32rpx;
  margin-bottom: $card-gap;
}

.section__title {
  display: block;
  margin-bottom: 20rpx;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.content-box {
  padding: 24rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
}

.content-box__text {
  font-size: $font-size-base;
  color: $color-text-primary;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.action-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: $radius-md;
  background: $color-primary;
  color: #fff;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
}
</style>
