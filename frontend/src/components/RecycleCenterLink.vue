<template>
  <el-button
    v-permission="'system:recycle:list'"
    :size="size"
    @click="go"
  >
    <el-icon class="btn-icon"><Delete /></el-icon>
    回收中心
  </el-button>
</template>

<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import type { RecycleTypeKey } from '@/views/system/recycle/recycle-config'

import type { ComponentSize } from 'element-plus'

const props = defineProps<{
  tab?: RecycleTypeKey
  size?: ComponentSize
}>()

const router = useRouter()

function go() {
  router.push({
    path: '/system/recycle',
    query: props.tab ? { tab: props.tab } : undefined,
  })
}
</script>

<style scoped>
.btn-icon {
  margin-right: 4px;
}
</style>
