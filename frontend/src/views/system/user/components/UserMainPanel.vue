<template>
  <el-card class="search-card">
    <el-form :model="queryParams" inline>
      <el-form-item label="用户名">
        <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="queryParams.mobile" placeholder="请输入手机号" clearable />
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
        <el-button type="primary" @click="$emit('query')">搜索</el-button>
        <el-button @click="$emit('reset-query')">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>

  <el-card>
    <template #header>
      <div class="card-header">
        <span>用户列表</span>
        <div class="header-actions">
          <ListExportButton module="user" :query-params="queryParams" permission="system:user:list" />
          <RecycleCenterLink tab="user" />
          <el-button type="primary" v-permission="'system:user:create'" @click="$emit('add')">新增用户</el-button>
        </div>
      </div>
    </template>
    <el-table
      :data="userList"
      v-loading="loading"
      border
      stripe
      :header-cell-style="{ textAlign: 'center' }"
      :cell-style="{ textAlign: 'center' }"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="mobile" label="手机号" width="130" />
      <el-table-column prop="deptName" label="部门" width="120" show-overflow-tooltip />
      <el-table-column prop="postNames" label="岗位" min-width="140" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.status === 2" type="warning">待审核</el-tag>
          <el-tag v-else-if="row.status === 3" type="danger">审核驳回</el-tag>
          <el-switch
            v-else
            v-model="row.status"
            :active-value="1"
            :inactive-value="0"
            v-permission="'system:user:update'"
            @change="$emit('status-change', row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button type="primary" size="small" v-permission="'system:user:update'" @click="$emit('edit', row)">
              编辑
            </el-button>
            <el-dropdown
              v-permission="['system:user:update', 'system:user:delete']"
              @command="(command) => $emit('command', command, row)"
              trigger="click"
            >
              <el-button type="primary" size="small">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-permission="'system:user:update'" command="resetPwd" :icon="Refresh">
                    重置密码
                  </el-dropdown-item>
                  <el-dropdown-item v-permission="'system:user:update'" command="assignRole" :icon="User">
                    分配角色
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-permission="'system:user:delete'"
                    command="delete"
                    :icon="Delete"
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
    <el-pagination
      v-model:current-page="queryParams.pageNo"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="$emit('load')"
      @current-change="$emit('load')"
    />
  </el-card>
</template>

<script setup lang="ts">
import { Refresh, User, Delete } from '@element-plus/icons-vue'
import ListExportButton from '@/components/ListExportButton.vue'
import RecycleCenterLink from '@/components/RecycleCenterLink.vue'
import type { UserVO, UserPageQuery } from '@/api/system/user'

defineProps<{
  queryParams: UserPageQuery
  userList: UserVO[]
  loading: boolean
  total: number
}>()

defineEmits<{
  query: []
  'reset-query': []
  add: []
  load: []
  'status-change': [row: UserVO]
  edit: [row: UserVO]
  command: [command: string, row: UserVO]
}>()
</script>

<style scoped>
.search-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.action-buttons .el-button {
  margin: 0;
}
:deep(.dropdown-item-danger) {
  color: #f56c6c !important;
}
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
