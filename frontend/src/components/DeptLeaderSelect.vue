<template>
  <el-select
    :model-value="modelValue"
    filterable
    clearable
    placeholder="请选择负责人"
    style="width: 100%"
    @update:model-value="$emit('update:modelValue', $event ?? null)"
  >
    <el-option
      v-for="user in userOptions"
      :key="user.id"
      :label="userLabel(user)"
      :value="user.id"
    />
  </el-select>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserList, type UserVO } from '@/api/system/user'

defineProps<{
  modelValue?: number | null
}>()

defineEmits<{
  'update:modelValue': [value: number | null]
}>()

const userOptions = ref<UserVO[]>([])

function userLabel(user: UserVO) {
  const name = user.nickname || user.username
  return user.deptName ? `${name}（${user.deptName}）` : name
}

onMounted(async () => {
  try {
    const res = await getUserList()
    userOptions.value = (res.data || []).filter((u) => u.status !== 0)
  } catch {
    userOptions.value = []
  }
})
</script>
