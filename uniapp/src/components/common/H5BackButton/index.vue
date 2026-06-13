<template>
  <!-- #ifdef H5 -->
  <view
    v-if="h5BackButtonState.visible"
    class="h5-back-btn"
    :style="{ paddingTop: statusBarHeight + 'px' }"
  >
    <view class="h5-back-btn__hit" @click="onBack">
      <text class="h5-back-btn__icon">‹</text>
    </view>
  </view>
  <!-- #endif -->
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { h5BackButtonState, scheduleSyncH5BackButton } from '@/store/h5-back-button'
import { navigateToParent } from '@/utils/nav-history'

const statusBarHeight = ref(0)

function onBack() {
  navigateToParent()
}

onMounted(() => {
  // #ifdef H5
  scheduleSyncH5BackButton()
  try {
    statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 0
  } catch {
    statusBarHeight.value = 0
  }
  // #endif
})
</script>

<style lang="scss" scoped>
.h5-back-btn {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 10000;
  pointer-events: none;
}

.h5-back-btn__hit {
  pointer-events: auto;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.h5-back-btn__icon {
  font-size: 32px;
  line-height: 1;
  font-weight: 300;
  color: #1e293b;
  margin-top: -2px;
}
</style>
