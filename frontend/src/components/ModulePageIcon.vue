<template>
  <el-icon :size="size"><component :is="iconComponent" /></el-icon>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { resolveMenuIcon } from '@/utils/menu-icon'

const props = withDefaults(
  defineProps<{
    /** 与 sys_menu.icon 一致，支持旧版别名（由 resolveMenuIcon 解析） */
    icon: string
    size?: number
  }>(),
  { size: 22 },
)

const iconComponent = computed(() => resolveMenuIcon(props.icon))
</script>
