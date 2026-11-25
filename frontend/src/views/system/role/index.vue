<template>
  <div class="app-container module-page">
    <PageHeader
      title="角色管理"
      description="配置系统角色与权限，控制菜单访问与操作授权"
      :icon="MODULE_PAGE_ICON.role"
      :stats="[{ label: '角色总数', value: roleList.length }]"
    />

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="角色名称">
          <el-input v-model="queryParams.name" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect
            v-model="queryParams.status"
            dict-type="sys_normal_disable"
            value-type="number"
            placeholder="请选择状态"
            width="150px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="role-table-card">
      <template #header>
        <div class="card-header">
          <span>角色列表</span>
          <div class="header-actions">
            <RecycleCenterLink tab="role" />
            <el-button type="primary" v-permission="'system:role:create'" @click="handleAdd">新增角色</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-if="roleList.length || loading"
        :data="roleList"
        v-loading="loading"
        border
        stripe
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" header-align="center" />
        <el-table-column prop="name" label="角色名称" width="150" align="center" header-align="center" />
        <el-table-column prop="code" label="角色编码" width="150" align="center" header-align="center" />
        <el-table-column prop="sort" label="排序" width="100" align="center" header-align="center" />
        <el-table-column prop="dataScope" label="数据范围" width="130" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="dataScopeTagType(row.dataScope)" effect="plain">
              {{ dataScopeLabel(row.dataScope) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              v-permission="'system:role:update'"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" header-align="center" />
        <el-table-column label="操作" width="180" fixed="right" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" v-permission="'system:role:update'" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-dropdown
                v-permission="['system:role:update', 'system:role:delete']"
                trigger="click"
                @command="(command: string) => handleCommand(command, row)"
              >
                <el-button type="primary" size="small">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-permission="'system:role:update'" command="assignMenu">
                      分配权限
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-permission="'system:role:delete'"
                      command="delete"
                      class="dropdown-item-danger"
                    >
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-else title="暂无角色数据" description="可点击右上角新增角色并配置数据范围" />
    </el-card>

    <RoleFormDialog
      ref="roleFormDialogRef"
      v-model:visible="dialogVisible"
      v-model:form="form"
      v-model:selected-dept-ids="selectedDeptIds"
      :title="dialogTitle"
      :rules="rules"
      :dept-options="deptOptions"
      @submit="submitForm"
    />

    <RoleMenuDialog
      ref="roleMenuDialogRef"
      v-model:visible="menuDialogVisible"
      :role-name="currentRole.name"
      :menu-options="menuOptions"
      @submit="submitAssignMenu"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import RecycleCenterLink from '@/components/RecycleCenterLink.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import RoleFormDialog from './components/RoleFormDialog.vue'
import RoleMenuDialog from './components/RoleMenuDialog.vue'
import { useRolePage } from './composables/useRolePage'

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const roleFormDialogRef = ref<InstanceType<typeof RoleFormDialog> | null>(null)
const roleMenuDialogRef = ref<InstanceType<typeof RoleMenuDialog> | null>(null)

const {
  loading,
  roleList,
  dialogVisible,
  dialogTitle,
  menuDialogVisible,
  formRef,
  menuTreeRef,
  currentRole,
  menuOptions,
  deptOptions,
  selectedDeptIds,
  queryParams,
  form,
  rules,
  dataScopeLabel,
  dataScopeTagType,
  handleQuery,
  resetQuery,
  handleAdd,
  handleEdit,
  handleStatusChange,
  handleCommand,
  submitAssignMenu,
  submitForm,
} = useRolePage()

watchEffect(() => {
  formRef.value = roleFormDialogRef.value?.formRef ?? null
  menuTreeRef.value = roleMenuDialogRef.value?.treeRef ?? null
})
</script>

<style scoped>
.module-page {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.role-table-card {
  flex: 1 0 auto;
  display: flex;
  flex-direction: column;
}

.role-table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  flex: 1 0 auto;
  min-height: 0;
}

.role-table-card :deep(.el-table) {
  flex: 1 0 auto;
}

:deep(.dropdown-item-danger) {
  color: var(--color-danger) !important;
}
</style>
