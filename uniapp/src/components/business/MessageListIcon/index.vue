<template>

  <view class="msg-list-icon" :class="`msg-list-icon--${theme}`">

    <IconFont :name="icon" :size="38" color="#ffffff" />

  </view>

</template>



<script setup lang="ts">

import IconFont from '@/components/common/IconFont/index.vue'

import type { IconName } from '@/constants/iconfont'



defineProps<{

  icon: IconName

  theme: string

}>()

</script>



<style lang="scss" scoped>

.msg-list-icon {

  width: 88rpx;

  height: 88rpx;

  border-radius: 50%;

  display: flex;

  align-items: center;

  justify-content: center;

  flex-shrink: 0;

}



.msg-list-icon--notice {

  background: linear-gradient(135deg, #f43f5e, #fb7185);

}



.msg-list-icon--inbox {

  background: linear-gradient(135deg, #4f46e5, #818cf8);

}



.msg-list-icon--chat {

  background: linear-gradient(135deg, #0891b2, #22d3ee);

}

</style>

