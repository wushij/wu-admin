<template>
  <el-dialog v-model="visible" title="分配权限" width="500px" :lock-scroll="false">
    <el-form label-width="100px">
      <el-form-item label="角色名称">
        <el-input :model-value="roleName" disabled />
      </el-form-item>
      <el-form-item label="菜单权限">
        <el-tree
          ref="treeRef"
          :data="menuOptions"
          :props="{ label: 'name', children: 'children' }"
          show-checkbox
          node-key="id"
          :default-expand-all="true"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="$emit('submit')">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ElTree } from 'element-plus'
import type { MenuVO } from '@/api/system/menu'

const visible = defineModel<boolean>('visible', { default: false })
const treeRef = ref<InstanceType<typeof ElTree> | null>(null)

defineProps<{
  roleName?: string
  menuOptions: MenuVO[]
}>()

defineEmits<{
  submit: []
}>()

defineExpose({ treeRef })
</script>
