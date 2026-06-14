<template>
  <view class="account-meta card--elevated">
    <view v-if="title" class="account-meta__head">
      <IconFont name="contact-o" :size="32" color="#6366f1" />
      <text class="account-meta__title">{{ title }}</text>
    </view>
    <view class="account-meta__grid">
      <view v-for="item in items" :key="item.label" class="account-meta__item">
        <text class="account-meta__label">{{ item.label }}</text>
        <text class="account-meta__value">{{ item.value || '—' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import IconFont from '@/components/common/IconFont/index.vue'

export interface AccountMetaItem {
  label: string
  value?: string
}

withDefaults(
  defineProps<{
    title?: string
    items: AccountMetaItem[]
    tips?: string[]
  }>(),
  { title: '' },
)
</script>

<style lang="scss" scoped>

.account-meta {
  padding: 28rpx 32rpx;
  margin-bottom: $section-gap;
}

.account-meta__head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.account-meta__title {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.account-meta__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx 24rpx;
}

.account-meta__item {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  padding: 20rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
}

.account-meta__label {
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.account-meta__value {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  word-break: break-all;
  line-height: 1.4;
}
</style>
