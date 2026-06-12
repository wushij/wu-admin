<template>
  <view class="menu-grid">
    <view
      v-for="item in items"
      :key="item.key"
      class="menu-grid__item"
      @click="emit('select', item)"
    >
      <view class="menu-grid__icon" :class="`menu-grid__icon--${item.theme}`">
        <IconFont :name="item.icon" :size="44" color="#ffffff" />
      </view>
      <text class="menu-grid__label">{{ item.label }}</text>
      <text v-if="item.hint" class="menu-grid__hint">{{ item.hint }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import IconFont from '@/components/common/IconFont/index.vue'
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
  width: 80rpx;
  height: 80rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
}

.menu-grid__icon--indigo { background: linear-gradient(135deg, #4f46e5, #818cf8); }
.menu-grid__icon--cyan { background: linear-gradient(135deg, #0891b2, #22d3ee); }
.menu-grid__icon--violet { background: linear-gradient(135deg, #7c3aed, #c084fc); }
.menu-grid__icon--amber { background: linear-gradient(135deg, #d97706, #fbbf24); }
.menu-grid__icon--rose { background: linear-gradient(135deg, #e11d48, #fb7185); }
.menu-grid__icon--slate { background: linear-gradient(135deg, #475569, #94a3b8); }

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
