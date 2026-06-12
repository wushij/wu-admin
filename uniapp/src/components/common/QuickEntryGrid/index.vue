<template>
  <view class="entry-panel card--elevated">
    <view class="entry-grid" :style="gridStyle">
      <view
        v-for="item in items"
        :key="item.key"
        class="entry-grid__item"
        @click="emit('select', item)"
      >
        <view class="entry-grid__icon" :class="`grad-${item.theme}`">
          <IconFont :name="item.icon" :size="40" color="#ffffff" />
        </view>
        <text class="entry-grid__name">{{ item.name }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'
import type { QuickEntry } from '@/constants/quickEntries'

defineProps<{ items: QuickEntry[] }>()
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
  padding: 16rpx 8rpx 20rpx;
  margin-bottom: $card-gap;
}

.entry-grid {
  display: grid;
  gap: 8rpx 0;
}

.entry-grid__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 8rpx;
  border-radius: $radius-md;
  transition: background 0.15s ease;
}

.entry-grid__item:active {
  background: $color-bg-muted;
}

.entry-grid__icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10rpx;
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
</style>
