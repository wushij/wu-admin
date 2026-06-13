<template>
  <view v-if="detail" class="announce-detail">
    <view class="detail-hero detail-hero--announce">
      <view class="detail-hero__pattern" />
      <view class="detail-hero__glow" />
      <view class="detail-hero__body">
        <ModuleIcon icon="bell" theme="announce" size="lg" />
        <view class="detail-hero__text">
          <text class="detail-hero__title">{{ detail.title }}</text>
          <text v-if="publishTime" class="detail-hero__time">{{ publishTime }}</text>
        </view>
      </view>
    </view>

    <view class="detail-section card--elevated">
      <view class="detail-section__head">
        <ModuleIcon icon="notes-o" theme="announce" size="sm" />
        <text class="detail-section__title">公告内容</text>
      </view>
      <view class="detail-section__content">
        <text class="detail-section__text">{{ detail.content }}</text>
      </view>
    </view>

    <view class="detail-section card--elevated">
      <view class="detail-section__head">
        <ModuleIcon icon="contact-o" theme="indigo" size="sm" />
        <text class="detail-section__title">发布信息</text>
      </view>
      <view class="detail-publisher">
        <ChatAvatar :src="detail.createAvatar" :name="publisherName" />
        <view class="detail-publisher__text">
          <text class="detail-publisher__name">{{ publisherName }}</text>
          <text class="detail-publisher__sub">系统公告发布人</text>
        </view>
      </view>
    </view>
  </view>

  <view v-else class="announce-detail announce-detail--empty">
    <EmptyState title="加载中…" icon="bell" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import ChatAvatar from '@/components/business/ChatAvatar/index.vue'
import { getAnnounceDetail, readAnnounce, type AnnounceDetail } from '@/api/message'
import { useMessageStore } from '@/store/message'
import { formatDateTime } from '@/utils/format'

const detail = ref<AnnounceDetail | null>(null)

const publisherName = computed(() => detail.value?.createName?.trim() || '系统')
const publishTime = computed(() => formatDateTime(detail.value?.createTime, true))

onLoad(async (options) => {
  const id = Number(options?.id)
  if (!id) return
  try {
    const res = await getAnnounceDetail(id)
    detail.value = res.data
    await readAnnounce(id)
    await useMessageStore().refreshSummary()
  } catch (e) {
    console.error(e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/mine.scss';

.announce-detail {
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

  &--announce .detail-hero__glow {
    @include mine-dark-hero-glow(rgba(245, 158, 11, 0.28));
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
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.04) 0%, $color-bg-muted 100%);
}

.detail-section__text {
  display: block;
  font-size: $font-size-base;
  color: $color-text-primary;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-publisher {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx;
  border-radius: $radius-md;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.04) 0%, $color-bg-muted 100%);
}

.detail-publisher :deep(.chat-avatar-shell) {
  width: 80rpx;
  height: 80rpx;
}

.detail-publisher__text {
  flex: 1;
  min-width: 0;
}

.detail-publisher__name {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.detail-publisher__sub {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}
</style>
