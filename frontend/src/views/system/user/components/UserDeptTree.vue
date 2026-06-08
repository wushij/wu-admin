<template>
  <el-card class="dept-card">
    <template #header>
      <span>部门</span>
    </template>
    <el-tree
      ref="treeRef"
      :data="deptOptions"
      :props="{ label: 'name', children: 'children' }"
      node-key="id"
      highlight-current
      @node-click="$emit('dept-click', $event)"
    />
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ElTree } from 'element-plus'
import type { DeptVO } from '@/api/system/dept'

defineProps<{ deptOptions: DeptVO[] }>()
defineEmits<{ 'dept-click': [data: DeptVO] }>()

const treeRef = ref<InstanceType<typeof ElTree> | null>(null)
defineExpose({ treeRef })
</script>

<style scoped>
.dept-card {
  height: calc(100vh - 150px);
  overflow: auto;
}
</style>
