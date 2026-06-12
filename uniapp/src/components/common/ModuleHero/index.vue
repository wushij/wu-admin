<template>

  <view class="module-hero" :class="themeClass">

    <view class="module-hero__inner" :class="{ 'module-hero__inner--stacked': subtitle || $slots.extra }">

      <view class="module-hero__main">

        <text class="module-hero__title">{{ title }}</text>

        <text v-if="subtitle" class="module-hero__subtitle">{{ subtitle }}</text>

      </view>

      <view class="module-hero__aside">

        <slot name="extra" />

        <text v-if="count != null && !$slots.extra" class="module-hero__count">{{ count }}</text>

      </view>

    </view>

  </view>

</template>



<script setup lang="ts">

import { computed } from 'vue'



const props = withDefaults(

  defineProps<{

    title: string

    count?: number | string

    subtitle?: string

    theme?: 'default' | 'server' | 'cache' | 'online' | 'job' | 'api' | 'monitor' | 'log'

  }>(),

  { theme: 'default' },

)



const themeClass = computed(() => (props.theme === 'default' ? '' : `module-hero--${props.theme}`))

</script>



<style lang="scss" scoped>

@import '@/styles/mixins.scss';



.module-hero {

  position: relative;

  margin-bottom: $card-gap;

  border-radius: $radius-lg;

  overflow: hidden;

  @include hero-gradient-profile;

  @include hero-dot-pattern;

  box-shadow: $shadow-hero;

}



.module-hero--server {

  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);

}



.module-hero--cache {

  background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);

}



.module-hero--online {

  background: linear-gradient(135deg, #0ea5e9 0%, #38bdf8 100%);

}



.module-hero--job {

  background: linear-gradient(135deg, #14b8a6 0%, #2dd4bf 100%);

}



.module-hero--api {

  background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%);

}



.module-hero--monitor {

  background: linear-gradient(135deg, #4338ca 0%, #6366f1 55%, #14b8a6 100%);

}



.module-hero--log {

  background: linear-gradient(135deg, #d97706 0%, #f59e0b 100%);

}



.module-hero__inner {

  position: relative;

  z-index: 1;

  display: flex;

  align-items: center;

  justify-content: space-between;

  min-height: 128rpx;

  padding: 28rpx 32rpx;

}



.module-hero__inner--stacked {

  align-items: flex-start;

}



.module-hero__main {

  flex: 1;

  min-width: 0;

  padding-right: 16rpx;

}



.module-hero__aside {

  display: flex;

  flex-direction: column;

  align-items: flex-end;

  gap: 8rpx;

}



.module-hero__subtitle {

  display: block;

  margin-top: 8rpx;

  font-size: $font-size-sm;

  line-height: 1.45;

  color: rgba(255, 255, 255, 0.82);

}



.module-hero__title {

  font-size: $font-size-xl;

  font-weight: $font-weight-bold;

  color: $color-text-inverse;

}



.module-hero__count {

  font-size: 44rpx;

  font-weight: $font-weight-bold;

  color: $color-text-inverse;

}

</style>


