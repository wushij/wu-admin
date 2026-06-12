<template>
  <view class="announce-detail" v-if="detail">
    <view class="article card--elevated">
      <view class="article__field">
        <text class="article__label">标题</text>
        <text class="article__title">{{ detail.title }}</text>
      </view>

      <view class="article__field">
        <text class="article__label">内容</text>
        <view class="article__content-box">
          <text class="article__content">{{ detail.content }}</text>
        </view>
      </view>

      <view class="article__field article__field--last">
        <text class="article__label">发布人</text>
        <view class="article__meta">
          <ChatAvatar :src="detail.createAvatar" :name="publisherName" />
          <view class="article__meta-text">
            <text class="article__publisher">{{ publisherName }}</text>
            <text v-if="publishTime" class="article__time">{{ publishTime }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
  <view v-else class="page-padded">
    <EmptyState title="加载中…" icon="bell" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
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

.announce-detail {
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
  background: $color-bg-page;
}

.article {
  padding: 32rpx;
}

.article__field {
  padding-bottom: 28rpx;
  margin-bottom: 28rpx;
  border-bottom: 1px solid $color-border-light;
}

.article__field--last {
  padding-bottom: 0;
  margin-bottom: 0;
  border-bottom: none;
}

.article__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
}

.article__title {
  display: block;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.45;
}

.article__meta {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
}

.article__meta :deep(.chat-avatar) {
  width: 80rpx;
  height: 80rpx;
}

.article__meta-text {
  flex: 1;
  min-width: 0;
}

.article__publisher {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.article__time {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.article__content-box {
  padding: 20rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
}

.article__content {
  display: block;
  font-size: $font-size-base;
  color: $color-text-primary;
  line-height: 1.65;
  white-space: pre-wrap;
}
</style>
