<template>
  <text class="iconfont" :style="iconStyle">{{ char }}</text>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ICON_CHARS, type IconName } from '@/constants/iconfont'

const props = withDefaults(
  defineProps<{
    name: IconName
    size?: string | number
    color?: string
  }>(),
  {
    size: '32rpx',
    color: 'inherit',
  },
)

const char = computed(() => ICON_CHARS[props.name] || '')

const iconStyle = computed(() => {
  const size = typeof props.size === 'number' ? `${props.size}rpx` : props.size
  return {
    fontSize: size,
    color: props.color,
    lineHeight: 1,
  }
})
</script>
