<template>
  <div class="app-container module-page user-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.user" />
            <span>用户管理</span>
          </div>
          <p class="module-hero-desc">管理系统账号，支持部门筛选、角色分配与状态控制</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ total }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
    </el-card>

    <div class="user-layout">
      <aside class="user-layout__side">
        <UserDeptTree ref="deptTreeComponentRef" :dept-options="deptOptions" @dept-click="handleDeptClick" />
      </aside>
      <section class="user-layout__main">
        <UserMainPanel
          v-model:status-filter="statusFilter"
          :query-params="queryParams"
          :user-list="userList"
          :loading="loading"
          :total="total"
          @query="handleQuery"
          @reset-query="resetQuery"
          @add="handleAdd"
          @load="getList"
          @status-change="handleStatusChange"
          @edit="handleEdit"
          @command="handleCommand"
        />
      </section>
    </div>

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

  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import UserDeptTree from './UserDeptTree.vue'
import UserMainPanel from './UserMainPanel.vue'
import UserFormDialog from './UserFormDialog.vue'
import UserRoleDialog from './UserRoleDialog.vue'
import UserResetPwdDialog from './UserResetPwdDialog.vue'
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
  queryParams,
  statusFilter,
  form,
  resetPwdForm,
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
  submitAssignRole,
  submitForm,
} = useUserPage()

watchEffect(() => {
  deptTreeRef.value = deptTreeComponentRef.value?.treeRef ?? null
  formRef.value = formDialogRef.value?.formRef ?? null
})
</script>

<style scoped lang="scss">
.user-page {
  padding: 0;
}

.user-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.user-layout__side {
  width: 280px;
  flex-shrink: 0;
}

.user-layout__main {
  flex: 1;
  min-width: 0;
}
</style>
