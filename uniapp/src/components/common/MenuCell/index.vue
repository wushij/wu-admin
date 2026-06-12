<template>
  <view class="menu-cell" @click="emit('click')">
    <view class="menu-cell__left">
      <view class="menu-cell__icon-wrap" :class="themeClass">
        <IconFont :name="icon" :size="36" color="#ffffff" />
      </view>
      <view class="menu-cell__texts">
        <text class="menu-cell__label">{{ label }}</text>
        <text v-if="desc" class="menu-cell__desc">{{ desc }}</text>
      </view>
    </view>
    <view class="menu-cell__right">
      <text v-if="value" class="menu-cell__value">{{ value }}</text>
      <IconFont v-if="arrow" name="arrow" :size="28" color="#c0c4cc" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'
import type { IconName } from '@/constants/iconfont'

const props = withDefaults(
  defineProps<{
    icon: IconName
    label: string
    desc?: string
    value?: string
    arrow?: boolean
    theme?: 'indigo' | 'cyan' | 'violet' | 'slate' | 'default'
  }>(),
  { arrow: true, theme: 'default' },
)

const emit = defineEmits<{ click: [] }>()

const themeClass = computed(() => `menu-cell__icon-wrap--${props.theme}`)
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.menu-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 112rpx;
  padding: 20rpx 32rpx;
  border-bottom: 1px solid $color-border-light;
  transition: background 0.15s ease;
}

.menu-cell:active {
  background: $color-bg-muted;
}

.menu-cell:last-child {
  border-bottom: none;
}

.menu-cell__left {
  display: flex;
  align-items: center;
  gap: 20rpx;
  flex: 1;
  min-width: 0;
}

.menu-cell__icon-wrap {
  flex-shrink: 0;
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #64748b, #94a3b8);
  box-shadow: $shadow-elevated;
}

.menu-cell__icon-wrap--indigo {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
}

.menu-cell__icon-wrap--cyan {
  background: linear-gradient(135deg, #0891b2, #22d3ee);
}

.menu-cell__icon-wrap--violet {
  background: linear-gradient(135deg, #7c3aed, #a78bfa);
}

.menu-cell__icon-wrap--slate {
  background: linear-gradient(135deg, #334155, #64748b);
}

.menu-cell__texts {
  flex: 1;
  min-width: 0;
}

.menu-cell__label {
  display: block;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.menu-cell__desc {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.4;
}

.menu-cell__right {
  display: flex;
  align-items: center;
  gap: 8rpx;
  flex-shrink: 0;
}

.menu-cell__value {
  font-size: 26rpx;
  color: $color-text-secondary;
}
</style>
