<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="600px"
    :lock-scroll="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item v-if="!form.id" label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
      <el-form-item label="手机号" prop="mobile">
        <el-input v-model="form.mobile" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="部门" prop="deptId">
        <el-tree-select
          v-model="form.deptId"
          :data="deptSelectOptions"
          node-key="id"
          :props="{ label: 'name', children: 'children', disabled: 'disabled' }"
          placeholder="请选择部门"
          check-strictly
          clearable
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="岗位" prop="postIds">
        <el-select
          v-model="form.postIds"
          multiple
          filterable
          clearable
          collapse-tags
          collapse-tags-tooltip
          placeholder="请选择岗位（可多选）"
          style="width: 100%"
        >
          <el-option
            v-for="item in postOptions"
            :key="item.id"
            :label="`${item.postName}（${item.postCode}）`"
            :value="item.id"
          />
        </el-select>
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
      <el-form-item label="角色" prop="roleId">
        <el-radio-group v-model="form.roleId">
          <el-radio v-for="role in roleOptions" :key="role.id" :label="role.id">
            {{ role.name }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="$emit('submit')">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { UserSaveDTO } from '@/api/system/user'
import type { RoleVO } from '@/api/system/role'
import type { DeptVO } from '@/api/system/dept'
import type { PostVO } from '@/api/system/post'

defineProps<{
  visible: boolean
  title: string
  form: UserSaveDTO
  rules: FormRules
  deptSelectOptions: DeptVO[]
  postOptions: PostVO[]
  roleOptions: RoleVO[]
}>()

defineEmits<{
  'update:visible': [value: boolean]
  submit: []
}>()

const formRef = ref<FormInstance | null>(null)
defineExpose({ formRef })
</script>
