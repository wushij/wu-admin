<template>
  <ModuleIcon :icon="icon" :theme="resolvedTheme" size="lg" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import type { IconName } from '@/constants/iconfont'

const props = defineProps<{
  icon: IconName
  theme: string
}>()

const resolvedTheme = computed(() => {
  if (props.theme === 'chat') return 'cyan'
  if (props.theme === 'notice') return 'rose'
  return props.theme
})
</script>
