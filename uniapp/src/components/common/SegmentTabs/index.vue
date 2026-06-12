<template>
  <scroll-view
    v-if="scroll"
    scroll-x
    class="segment-tabs-scroll"
    :show-scrollbar="false"
    :enable-flex="true"
  >
    <view class="segment-tabs segment-tabs--scroll">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="segment-tabs__item"
        :class="{ 'segment-tabs__item--active': modelValue === tab.key }"
        @click="emit('update:modelValue', tab.key)"
      >
        <text class="segment-tabs__label">{{ tab.label }}</text>
        <view v-if="tab.badge && tab.badge > 0" class="segment-tabs__badge">
          {{ tab.badge > 99 ? '99+' : tab.badge }}
        </view>
      </view>
    </view>
  </scroll-view>

  <view v-else class="segment-tabs" :class="{ 'segment-tabs--compact': compact }">
    <view
      v-for="tab in tabs"
      :key="tab.key"
      class="segment-tabs__item"
      :class="{ 'segment-tabs__item--active': modelValue === tab.key }"
      @click="emit('update:modelValue', tab.key)"
    >
      <text class="segment-tabs__label">{{ tab.label }}</text>
      <view v-if="tab.badge && tab.badge > 0" class="segment-tabs__badge">
        {{ tab.badge > 99 ? '99+' : tab.badge }}
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue: string
    tabs: Array<{ key: string; label: string; badge?: number }>
    compact?: boolean
    scroll?: boolean
  }>(),
  { compact: false, scroll: false },
)

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.segment-tabs-scroll {
  width: 100%;
  margin-bottom: 24rpx;
  white-space: nowrap;
}

.segment-tabs {
  display: flex;
  gap: 8rpx;
  padding: 8rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
  border: 1px solid $color-border-light;
  box-shadow: $shadow-card;
}

.segment-tabs--compact {
  margin-bottom: 0;
}

.segment-tabs--scroll {
  display: inline-flex;
  flex-wrap: nowrap;
  min-width: max-content;

  .segment-tabs__item {
    flex: 0 0 auto;
    min-width: 112rpx;
    padding: 0 24rpx;
  }
}

.segment-tabs__item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  height: 72rpx;
  padding: 0 12rpx;
  border-radius: $radius-md;
  transition: all 0.15s ease;
}

.segment-tabs__label {
  font-size: 28rpx;
  color: $color-text-secondary;
  line-height: 1;
  white-space: nowrap;
}

.segment-tabs__badge {
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 8rpx;
  border-radius: $radius-full;
  background: #fa5151;
  color: #fff;
  font-size: 20rpx;
  line-height: 32rpx;
  text-align: center;
}

.segment-tabs__item--active {
  background: linear-gradient(135deg, #4f46e5, #6366f1);

  .segment-tabs__label {
    color: #fff;
    font-weight: $font-weight-semibold;
  }

  .segment-tabs__badge {
    background: rgba(255, 255, 255, 0.92);
    color: #ef4444;
  }
}
</style>
