<template>
  <el-dialog
    :model-value="visible"
    title="分配角色"
    width="500px"
    :lock-scroll="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form label-width="100px">
      <el-form-item label="用户名">
        <el-input :model-value="currentUser.username" disabled />
      </el-form-item>
      <el-form-item label="角色">
        <el-radio-group :model-value="selectedRole" @update:model-value="onRoleChange">
          <el-radio v-for="role in roleOptions" :key="role.id" :label="role.id">
            {{ role.name }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="$emit('submit')">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { UserVO } from '@/api/system/user'
import type { RoleVO } from '@/api/system/role'

defineProps<{
  visible: boolean
  currentUser: Partial<UserVO>
  selectedRole: number | undefined
  roleOptions: RoleVO[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'update:selectedRole': [value: number | undefined]
  submit: []
}>()

function onRoleChange(val: string | number | boolean | undefined) {
  emit('update:selectedRole', typeof val === 'number' ? val : undefined)
}
</script>
