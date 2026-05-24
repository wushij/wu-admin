<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'

onLoad((options) => {
  const type = options?.type ? `?type=${options.type}` : ''
  uni.redirectTo({ url: `/pages-sub/system/recycle/index${type}` })
})
</script>

<template>
  <view />
</template>
