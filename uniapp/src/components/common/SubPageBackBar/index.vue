<template>
  <!-- #ifdef H5 -->
  <view v-if="visible" class="sub-page-back" @click="onBack">
    <view class="sub-page-back__row">
      <IconFont name="arrow" :size="28" color="currentColor" class="sub-page-back__icon" />
      <text class="sub-page-back__text">返回</text>
    </view>
  </view>
  <!-- #endif -->
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import IconFont from '@/components/common/IconFont/index.vue'
import { navigateToParent } from '@/utils/nav-history'
import { scheduleSyncH5BackButton } from '@/store/h5-back-button'

const visible = ref(true)

function syncVisible() {
  visible.value = true
  // #ifdef H5
  scheduleSyncH5BackButton()
  // #endif
}

onShow(() => {
  syncVisible()
})

function onBack() {
  navigateToParent()
}
</script>

<style lang="scss" scoped>

.sub-page-back {
  margin: -8rpx 0 12rpx;
}

.sub-page-back__row {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  gap: 6rpx;
  height: 40rpx;
  color: $color-primary;
}

.sub-page-back__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28rpx;
  height: 28rpx;
  transform: rotate(180deg);
  flex-shrink: 0;
}

.sub-page-back__text {
  font-size: 28rpx;
  line-height: 40rpx;
  font-weight: $font-weight-semibold;
  color: $color-primary;
}
</style>
