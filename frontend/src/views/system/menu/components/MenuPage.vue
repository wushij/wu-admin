<template>
  <div class="app-container module-page menu-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.menu" />
            <span>菜单管理</span>
          </div>
          <p class="module-hero-desc">维护目录、菜单与按钮权限；禁用的菜单仍在此列表显示，侧栏需重新登录后更新</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ menuStats.total }}</div>
          <div class="stat-label">菜单项</div>
        </div>
      </div>
    </el-card>

    <el-card class="menu-card" shadow="never">
      <MenuSearchToolbar
        :query-params="queryParams"
        :menu-stats="menuStats"
        :expand-all="expandAll"
        @query="handleQuery"
        @reset="resetQuery"
        @add="handleAdd()"
        @toggle-expand="toggleExpandAll"
      />

      <MenuTreeTable
        ref="tableComponentRef"
        :table-key="tableKey"
        :menu-list="menuList"
        :loading="loading"
        :expand-all="expandAll"
        :is-external-row="isExternalRow"
        @add="handleAdd"
        @edit="handleEdit"
        @delete="handleDelete"
        @status-change="handleStatusChange"
      />
    </el-card>

    <MenuFormDialog
      ref="formDialogRef"
      v-model:visible="dialogVisible"
      :title="dialogTitle"
      :form="form"
      :rules="rules"
      :parent-options="parentOptions"
      :submit-loading="submitLoading"
      @type-change="onTypeChange"
      @fill-permission-prefix="fillPermissionPrefix"
      @submit="submitForm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import MenuSearchToolbar from './MenuSearchToolbar.vue'
import MenuTreeTable from './MenuTreeTable.vue'
import MenuFormDialog from './MenuFormDialog.vue'
import { useMenuPage } from '../composables/useMenuPage'

const tableComponentRef = ref<InstanceType<typeof MenuTreeTable> | null>(null)
const formDialogRef = ref<InstanceType<typeof MenuFormDialog> | null>(null)

const {
  loading,
  submitLoading,
  menuList,
  parentOptions,
  dialogVisible,
  dialogTitle,
  formRef,
  tableRef,
  expandAll,
  tableKey,
  queryParams,
  form,
  rules,
  menuStats,
  isExternalRow,
  handleQuery,
  resetQuery,
  toggleExpandAll,
  handleAdd,
  handleEdit,
  onTypeChange,
  fillPermissionPrefix,
  handleDelete,
  handleStatusChange,
  submitForm,
} = useMenuPage()

watchEffect(() => {
  tableRef.value = tableComponentRef.value?.tableRef ?? null
  formRef.value = formDialogRef.value?.formRef ?? null
})
</script>

<style scoped>
.menu-page {
  padding: 0;
}

.menu-card {
  border-radius: 8px;
  margin-bottom: 0;
}

.menu-card :deep(.el-card__body) {
  padding: 16px 20px 20px;
}
</style>
