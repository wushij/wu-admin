<template>
  <view class="about-page">
    <view class="about-page__glow about-page__glow--1" />
    <view class="about-page__glow about-page__glow--2" />

    <view class="about-card">
      <view class="about-card__logo-wrap">
        <AppBrandLogo :size="120" />
      </view>
      <text class="about-card__name">{{ appStore.platformName }}</text>
      <text v-if="appStore.platformSubtitle" class="about-card__sub">{{ appStore.platformSubtitle }}</text>

      <view class="about-tags">
        <text class="about-tags__item">移动端</text>
        <text class="about-tags__item">数据互通</text>
        <text class="about-tags__item">运维管理</text>
      </view>

      <view class="about-card__ver">
        <text class="about-card__ver-label">当前版本</text>
        <text class="about-card__ver-value">{{ versionName }}</text>
      </view>
    </view>

    <text class="about-footer">© {{ year }} Wu-Admin</text>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppBrandLogo from '@/components/common/AppBrandLogo/index.vue'
import { useAppStore } from '@/store/app'
import { getAppVersionName } from '@/utils/app-version'

const appStore = useAppStore()
const versionName = getAppVersionName()
const year = computed(() => new Date().getFullYear())
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/mine.scss';

.about-page {
  @include mine-page-bg;
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: calc(88rpx + env(safe-area-inset-top)) 40rpx calc(48rpx + env(safe-area-inset-bottom));
  overflow: hidden;
}

.about-page__glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(60rpx);
  pointer-events: none;
}

.about-page__glow--1 {
  width: 400rpx;
  height: 400rpx;
  top: 6%;
  left: -100rpx;
  background: rgba(99, 102, 241, 0.35);
}

.about-page__glow--2 {
  width: 320rpx;
  height: 320rpx;
  bottom: 22%;
  right: -80rpx;
  background: rgba(167, 139, 250, 0.3);
}

.about-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 600rpx;
  padding: 56rpx 40rpx 48rpx;
  text-align: center;
  @include mine-card;
}

.about-card__logo-wrap {
  display: flex;
  justify-content: center;
  margin: 0 auto 32rpx;
}

.about-card__name {
  display: block;
  font-size: 40rpx;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.about-card__sub {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.55;
}

.about-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12rpx;
  margin-top: 32rpx;
}

.about-tags__item {
  padding: 8rpx 20rpx;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  color: $color-primary;
  background: $color-primary-muted;
}

.about-card__ver {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 40rpx;
  padding: 24rpx 28rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
}

.about-card__ver-label {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.about-card__ver-value {
  font-size: $font-size-md;
  font-weight: $font-weight-bold;
  color: $color-primary;
}

.about-footer {
  position: relative;
  z-index: 1;
  margin-top: auto;
  padding-top: 56rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}
</style>
