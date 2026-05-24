<template>
  <view class="page-tab" :class="{ 'page-tab--bare': bare }">
    <slot />
    <custom-tab-bar />
    <AiWuAssistant />
  </view>
</template>

<script setup lang="ts">
import AiWuAssistant from '@/components/business/AiWuAssistant/index.vue'

withDefaults(defineProps<{ bare?: boolean }>(), { bare: false })
</script>

<style lang="scss" scoped>
.page-tab--bare {
  padding-top: 0;
  padding-left: 16rpx;
  padding-right: 16rpx;
  background: transparent;
}
</style>
