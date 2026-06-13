<template>
  <view class="entry-panel card--elevated">
    <view v-if="title" class="entry-panel__head">
      <view class="entry-panel__accent" />
      <text class="entry-panel__title">{{ title }}</text>
      <text class="entry-panel__count">{{ items.length }}</text>
    </view>

    <view class="entry-grid" :style="gridStyle">
      <view
        v-for="item in items"
        :key="item.key"
        class="entry-grid__item"
        @click="emit('select', item)"
      >
        <ModuleIcon :icon="item.icon" :theme="item.theme" size="lg" class="entry-grid__icon" />
        <text class="entry-grid__name">{{ item.name }}</text>
        <text v-if="showDesc && item.desc" class="entry-grid__desc">{{ item.desc }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import type { QuickEntry } from '@/constants/quickEntries'

withDefaults(
  defineProps<{
    items: QuickEntry[]
    title?: string
    showDesc?: boolean
  }>(),
  { showDesc: false },
)

const emit = defineEmits<{ select: [item: QuickEntry] }>()

const gridStyle = computed(() => {
  const width = uni.getSystemInfoSync().windowWidth || 375
  let columns = 4
  if (width < 360) columns = 3
  else if (width >= 768) columns = 5
  return { gridTemplateColumns: `repeat(${columns}, 1fr)` }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.entry-panel {
  padding: 0 8rpx 12rpx;
  margin-bottom: $card-gap;
  overflow: hidden;
}

.entry-panel__head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 24rpx 20rpx 8rpx;
}

.entry-panel__accent {
  width: 6rpx;
  height: 28rpx;
  border-radius: 3rpx;
  background: linear-gradient(180deg, $color-primary 0%, #8b5cf6 100%);
  flex-shrink: 0;
}

.entry-panel__title {
  flex: 1;
  min-width: 0;
  font-size: $font-size-md;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  letter-spacing: 0.02em;
}

.entry-panel__count {
  flex-shrink: 0;
  min-width: 36rpx;
  height: 36rpx;
  padding: 0 12rpx;
  border-radius: $radius-full;
  background: $color-primary-muted;
  font-size: 20rpx;
  font-weight: $font-weight-semibold;
  color: $color-primary;
  line-height: 36rpx;
  text-align: center;
}

.entry-grid {
  display: grid;
  gap: 4rpx 0;
}

.entry-grid__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 8rpx 16rpx;
  border-radius: $radius-md;
  transition: transform 0.18s ease, background 0.18s ease;
}

.entry-grid__item:active {
  transform: scale(0.94);
  background: rgba(79, 70, 229, 0.04);
}

.entry-grid__icon {
  margin-bottom: 12rpx;
}

.entry-grid__name {
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  line-height: 1.35;
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.entry-grid__desc {
  margin-top: 4rpx;
  font-size: 20rpx;
  color: $color-text-placeholder;
  line-height: 1.3;
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
