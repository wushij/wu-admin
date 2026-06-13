<template>
  <view class="menu-grid">
    <view
      v-for="item in items"
      :key="item.key"
      class="menu-grid__item"
      @click="emit('select', item)"
    >
      <ModuleIcon :icon="item.icon" :theme="item.theme" size="lg" class="menu-grid__icon" />
      <text class="menu-grid__label">{{ item.label }}</text>
      <text v-if="item.hint" class="menu-grid__hint">{{ item.hint }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import type { IconName } from '@/constants/iconfont'

export interface MineMenuItem {
  key: string
  label: string
  hint?: string
  icon: IconName
  theme: 'indigo' | 'cyan' | 'violet' | 'amber' | 'rose' | 'slate'
}

defineProps<{ items: MineMenuItem[] }>()
const emit = defineEmits<{ select: [item: MineMenuItem] }>()
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/mine.scss';

.menu-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  margin-bottom: $section-gap;
}

.menu-grid__item {
  @include mine-card;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 28rpx 24rpx;
  transition: transform 0.15s ease;

  &:active {
    transform: scale(0.97);
  }
}

.menu-grid__icon {
  margin-bottom: 20rpx;
}

.menu-grid__label {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.menu-grid__hint {
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}
</style>
