<template>
  <div class="app-container module-page menu-page">
    <PageHeader
      title="菜单管理"
      description="维护目录、菜单与按钮权限；禁用的菜单仍在此列表显示，侧栏需重新登录后更新"
      :icon="MODULE_PAGE_ICON.menu"
      :stats="[{ label: '菜单项', value: menuStats.total }]"
    />

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
import PageHeader from '@/components/ui/PageHeader.vue'
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
  margin-bottom: 0;
}
</style>
