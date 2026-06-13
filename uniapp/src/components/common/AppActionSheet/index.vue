<template>
  <view v-if="visible" class="app-sheet" @touchmove.stop.prevent>
    <view class="app-sheet__mask" @tap.stop="onCancel" />
    <view class="app-sheet__panel" @tap.stop>
      <text v-if="title" class="app-sheet__title">{{ title }}</text>

      <view class="app-sheet__group" :class="{ 'app-sheet__group--scroll': scrollable }">
        <scroll-view v-if="scrollable" scroll-y class="app-sheet__scroll" :show-scrollbar="true">
          <view
            v-for="(item, index) in items"
            :key="`${item.label}-${index}`"
            class="app-sheet__item"
            :class="{
              'app-sheet__item--danger': item.danger,
              'app-sheet__item--border': index < items.length - 1,
            }"
            @tap.stop="onPick(index)"
          >
            <text class="app-sheet__label">{{ item.label }}</text>
          </view>
        </scroll-view>
        <template v-else>
          <view
            v-for="(item, index) in items"
            :key="`${item.label}-${index}`"
            class="app-sheet__item"
            :class="{
              'app-sheet__item--danger': item.danger,
              'app-sheet__item--border': index < items.length - 1,
            }"
            @tap.stop="onPick(index)"
          >
            <text class="app-sheet__label">{{ item.label }}</text>
          </view>
        </template>
      </view>

      <view class="app-sheet__cancel" @tap.stop="onCancel">{{ cancelText }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ActionSheetItem } from '@/store/dialog'

const props = withDefaults(
  defineProps<{
    visible: boolean
    title?: string
    items: ActionSheetItem[]
    cancelText?: string
    scrollable?: boolean
  }>(),
  { cancelText: '取消' },
)

const scrollable = computed(() => {
  if (props.scrollable != null) return props.scrollable
  return props.items.length > 8
})

const emit = defineEmits<{
  pick: [index: number]
  cancel: []
}>()

function onPick(index: number) {
  emit('pick', index)
}

function onCancel() {
  emit('cancel')
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.app-sheet {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.app-sheet__mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.46);
  backdrop-filter: blur(8px);
}

.app-sheet__panel {
  position: relative;
  z-index: 1;
  width: 100%;
  padding: 0 24rpx calc(24rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  animation: sheet-in 0.24s ease;
}

.app-sheet__title {
  display: block;
  margin-bottom: 16rpx;
  padding: 0 12rpx;
  text-align: center;
  font-size: $font-size-sm;
  color: rgba(255, 255, 255, 0.92);
}

.app-sheet__group {
  overflow: hidden;
  border-radius: 28rpx;
  background: $color-bg-card;
  box-shadow: 0 20rpx 48rpx rgba(15, 23, 42, 0.16);
}

.app-sheet__group--scroll {
  overflow: hidden;
}

.app-sheet__scroll {
  max-height: 62vh;
}

.app-sheet__item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  box-sizing: border-box;
  min-height: 104rpx;
  padding: 0 32rpx;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;

  &:active {
    background: $color-bg-muted;
  }

  &--border {
    border-bottom: 1px solid $color-border-light;
  }

  &--danger {
    color: $color-danger;
  }
}

.app-sheet__label {
  display: block;
  width: 100%;
  text-align: center;
  line-height: 1.45;
}

.app-sheet__cancel {
  margin-top: 16rpx;
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  border-radius: 28rpx;
  background: $color-bg-card;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.1);

  &:active {
    background: $color-bg-muted;
  }
}

@keyframes sheet-in {
  from {
    opacity: 0;
    transform: translateY(100%);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
