<template>
  <el-dialog v-model="visible" :title="title" width="600px" :lock-scroll="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="角色名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入角色名称" />
      </el-form-item>
      <el-form-item label="角色编码" prop="code">
        <el-input v-model="form.code" placeholder="请输入角色编码" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="form.sort" :min="0" />
      </el-form-item>
      <el-form-item label="数据范围" prop="dataScope">
        <el-select v-model="form.dataScope" placeholder="请选择数据范围" style="width: 100%">
          <el-option
            v-for="item in DATA_SCOPE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.dataScope === 2" label="可见部门" prop="dataScopeDeptIds">
        <el-tree-select
          v-model="selectedDeptIds"
          :data="deptOptions"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          multiple
          show-checkbox
          check-strictly
          default-expand-all
          placeholder="请选择可见部门"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <DictSelect
          v-model="form.status"
          dict-type="sys_normal_disable"
          value-type="number"
          :clearable="false"
          width="160px"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
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
import type { FormInstance, FormRules } from 'element-plus'
import type { RoleSaveDTO } from '@/api/system/role'
import type { DeptVO } from '@/api/system/dept'
import { DATA_SCOPE_OPTIONS } from '../composables/useRolePage'

const visible = defineModel<boolean>('visible', { default: false })
const form = defineModel<RoleSaveDTO>('form', { required: true })
const selectedDeptIds = defineModel<number[]>('selectedDeptIds', { default: () => [] })
const formRef = ref<FormInstance | null>(null)

defineProps<{
  title: string
  rules: FormRules
  deptOptions: DeptVO[]
}>()

defineEmits<{
  submit: []
}>()

defineExpose({ formRef })
</script>
