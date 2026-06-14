<template>
  <view class="module-dark-hero">
    <view class="module-dark-hero__pattern" />
    <view class="module-dark-hero__glow" :style="glowStyle" />
    <view
      class="module-dark-hero__body"
      :class="{ 'module-dark-hero__body--aside': showAside }"
    >
      <ModuleIcon :icon="icon" :theme="theme" size="lg" />
      <view class="module-dark-hero__text">
        <text class="module-dark-hero__title">{{ title }}</text>
        <text v-if="subtitle" class="module-dark-hero__sub">{{ subtitle }}</text>
        <view v-if="$slots.extra" class="module-dark-hero__extras">
          <slot name="extra" />
        </view>
      </view>
      <view v-if="showAside" class="module-dark-hero__aside">
        <slot name="aside">
          <view v-if="count != null" class="module-dark-hero__stat">
            <text class="module-dark-hero__stat-num">{{ count }}</text>
            <text v-if="countLabel" class="module-dark-hero__stat-label">{{ countLabel }}</text>
          </view>
        </slot>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import { moduleHeroGlow } from '@/constants/module-dark-hero'
import type { IconName } from '@/constants/iconfont'

const props = withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    icon: IconName
    theme: string
    glow?: string
    count?: number | string
    countLabel?: string
  }>(),
  {},
)

const slots = useSlots()

const glowStyle = computed(() => ({
  background: `radial-gradient(circle, ${props.glow ?? moduleHeroGlow(props.theme)} 0%, transparent 70%)`,
}))

const showAside = computed(() => slots.aside != null || props.count != null)
</script>

<style lang="scss" scoped>
@use '@/styles/mine.scss' as *;

.module-dark-hero {
  @include mine-dark-hero-shell;
  flex-shrink: 0;
}

.module-dark-hero__pattern {
  @include mine-dark-hero-pattern;
}

.module-dark-hero__glow {
  @include mine-dark-hero-glow;
  background: none;
}

.module-dark-hero__body {
  @include mine-dark-hero-body;

  &--aside {
    align-items: flex-start;
  }
}

.module-dark-hero__text {
  @include mine-dark-hero-text;
}

.module-dark-hero__title {
  @include mine-dark-hero-title;
}

.module-dark-hero__sub {
  @include mine-dark-hero-sub;
}

.module-dark-hero__extras {
  @include mine-dark-hero-extras;
}

.module-dark-hero__aside {
  flex-shrink: 0;
  align-self: stretch;
  display: flex;
  align-items: stretch;
}

.module-dark-hero__stat {
  min-width: 112rpx;
  padding: 16rpx 20rpx;
  border-radius: $radius-lg;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
  text-align: center;
}

.module-dark-hero__stat-num {
  display: block;
  font-size: 40rpx;
  font-weight: $font-weight-bold;
  color: #fff;
  line-height: 1.1;
}

.module-dark-hero__stat-label {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.78);
}

:deep(.module-dark-hero__chip) {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.14);
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.88);
}

:deep(.module-dark-hero__stats-row) {
  display: flex;
  gap: 10rpx;
  align-items: stretch;
}

:deep(.module-dark-hero__mini-stat) {
  min-width: 72rpx;
  padding: 12rpx 14rpx;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
  text-align: center;
}

:deep(.module-dark-hero__mini-stat-num) {
  display: block;
  font-size: 30rpx;
  font-weight: $font-weight-bold;
  color: #fff;
  line-height: 1.1;
}

:deep(.module-dark-hero__mini-stat-label) {
  display: block;
  margin-top: 4rpx;
  font-size: 18rpx;
  color: rgba(255, 255, 255, 0.72);
}
</style>
