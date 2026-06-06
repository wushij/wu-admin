<template>
  <el-dialog
    :model-value="visible"
    title="重置密码"
    width="400px"
    :lock-scroll="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="用户名">
        <el-input v-model="form.username" disabled />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="form.password" type="password" placeholder="请输入新密码" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="$emit('submit')">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  form: { id: number | null; username: string; password: string }
}>()

defineEmits<{
  'update:visible': [value: boolean]
  submit: []
}>()
</script>
