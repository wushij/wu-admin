<template>
  <div class="app-container user-page">
    <el-row :gutter="20">
      <el-col :span="4">
        <UserDeptTree ref="deptTreeComponentRef" :dept-options="deptOptions" @dept-click="handleDeptClick" />
      </el-col>
      <el-col :span="20">
        <UserMainPanel
          :query-params="queryParams"
          :user-list="userList"
          :loading="loading"
          :total="total"
          @query="handleQuery"
          @reset-query="resetQuery"
          @open-recycle="openRecycleDialog"
          @add="handleAdd"
          @load="getList"
          @status-change="handleStatusChange"
          @edit="handleEdit"
          @command="handleCommand"
        />
      </el-col>
    </el-row>

    <UserFormDialog
      ref="formDialogRef"
      v-model:visible="dialogVisible"
      :title="dialogTitle"
      :form="form"
      :rules="rules"
      :dept-select-options="deptSelectOptions"
      :post-options="postOptions"
      :role-options="roleOptions"
      @submit="submitForm"
    />

    <UserRoleDialog
      v-model:visible="roleDialogVisible"
      v-model:selected-role="selectedRole"
      :current-user="currentUser"
      :role-options="roleOptions"
      @submit="submitAssignRole"
    />

    <UserResetPwdDialog
      v-model:visible="resetPwdVisible"
      :form="resetPwdForm"
      @submit="submitResetPwd"
    />

    <UserRecycleDialog
      v-model:visible="recycleVisible"
      :list="recycleList"
      :loading="recycleLoading"
      :total="recycleTotal"
      :query="recycleQuery"
      @load="getRecycleList"
      @restore="handleRestore"
      @permanent-delete="handlePermanentDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import UserDeptTree from './UserDeptTree.vue'
import UserMainPanel from './UserMainPanel.vue'
import UserFormDialog from './UserFormDialog.vue'
import UserRoleDialog from './UserRoleDialog.vue'
import UserResetPwdDialog from './UserResetPwdDialog.vue'
import UserRecycleDialog from './UserRecycleDialog.vue'
import { useUserPage } from '../composables/useUserPage'

const deptTreeComponentRef = ref<InstanceType<typeof UserDeptTree> | null>(null)
const formDialogRef = ref<InstanceType<typeof UserFormDialog> | null>(null)

const {
  loading,
  total,
  userList,
  dialogVisible,
  dialogTitle,
  roleDialogVisible,
  resetPwdVisible,
  formRef,
  deptTreeRef,
  currentUser,
  selectedRole,
  roleOptions,
  deptOptions,
  deptSelectOptions,
  postOptions,
  recycleVisible,
  recycleLoading,
  recycleList,
  recycleTotal,
  queryParams,
  form,
  resetPwdForm,
  recycleQuery,
  rules,
  getList,
  handleDeptClick,
  handleQuery,
  resetQuery,
  handleStatusChange,
  handleCommand,
  submitResetPwd,
  handleAdd,
  handleEdit,
  openRecycleDialog,
  getRecycleList,
  handleRestore,
  handlePermanentDelete,
  submitAssignRole,
  submitForm,
} = useUserPage()

watchEffect(() => {
  deptTreeRef.value = deptTreeComponentRef.value?.treeRef ?? null
  formRef.value = formDialogRef.value?.formRef ?? null
})
</script>

<style scoped>
.user-page {
  padding: 0;
}
</style>
