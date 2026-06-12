<template>
  <view class="brand-logo" :style="sizeStyle">
    <view class="brand-logo__bars">
      <view class="brand-logo__bar brand-logo__bar--1" />
      <view class="brand-logo__bar brand-logo__bar--2" />
      <view class="brand-logo__bar brand-logo__bar--3" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ size?: number }>(), { size: 120 })

const sizeStyle = computed(() => ({
  width: `${props.size}rpx`,
  height: `${props.size}rpx`,
}))
</script>

<style lang="scss" scoped>
.brand-logo {
  border-radius: 28%;
  background: linear-gradient(135deg, #010710 0%, #374151 100%);
  box-shadow: 0 12rpx 32rpx rgba(1, 7, 16, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-logo__bars {
  display: flex;
  align-items: flex-end;
  gap: 14%;
  width: 52%;
  height: 52%;
}

.brand-logo__bar {
  flex: 1;
  border-radius: 4rpx;
  background: #fff;
}

.brand-logo__bar--1 {
  height: 55%;
}

.brand-logo__bar--2 {
  height: 100%;
}

.brand-logo__bar--3 {
  height: 72%;
}
</style>
