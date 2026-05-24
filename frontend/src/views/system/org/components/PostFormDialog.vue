<template>
  <el-dialog :model-value="visible" :title="title" width="520px" destroy-on-close @update:model-value="$emit('update:visible', $event)">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="上级岗位">
        <el-tree-select
          v-model="form.parentId"
          :data="treeOptions"
          node-key="id"
          :props="{ label: 'postName', children: 'children' }"
          check-strictly
          clearable
          placeholder="顶级岗位"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="岗位编码" prop="postCode">
        <el-input v-model="form.postCode" :disabled="!!form.id" />
      </el-form-item>
      <el-form-item label="岗位名称" prop="postName">
        <el-input v-model="form.postName" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import type { PostSaveDTO, PostVO } from '@/api/system/post'

type PostTreeOption = Pick<PostVO, 'id' | 'postName'> & { children?: PostVO[] }

defineProps<{
  visible: boolean
  title: string
  form: PostSaveDTO
  rules: FormRules
  treeOptions: PostTreeOption[]
  submitting: boolean
}>()

defineEmits<{
  'update:visible': [value: boolean]
  submit: []
}>()

const formRef = ref()
defineExpose({ formRef })
</script>
