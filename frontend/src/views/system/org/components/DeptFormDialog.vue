<template>
  <el-dialog :model-value="visible" :title="title" width="560px" destroy-on-close @update:model-value="$emit('update:visible', $event)">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="上级部门">
        <el-tree-select
          v-model="form.parentId"
          :data="treeOptions"
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          check-strictly
          clearable
          placeholder="主目录（顶级）"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="部门名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="负责人">
        <DeptLeaderSelect v-model="form.leaderUserId" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="$emit('submit')">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FormRules } from 'element-plus'
import type { DeptSaveDTO, DeptVO } from '@/api/system/dept'
import DeptLeaderSelect from '@/components/DeptLeaderSelect.vue'

defineProps<{
  visible: boolean
  title: string
  form: DeptSaveDTO
  rules: FormRules
  treeOptions: DeptVO[]
  submitting: boolean
}>()

defineEmits<{
  'update:visible': [value: boolean]
  submit: []
}>()

const formRef = ref()
defineExpose({ formRef })
</script>
