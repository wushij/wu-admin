<template>
  <el-card class="search-card module-search-card" shadow="never">
    <el-form :model="queryParams" inline class="module-search-form">
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
        <el-button type="primary" :icon="Search" @click="$emit('query')">搜索</el-button>
        <el-button :icon="RefreshIcon" @click="$emit('reset-query')">重置</el-button>
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
      :header-cell-style="tableHeaderStyle"
      :cell-style="tableCellStyle"
    >
      <el-table-column prop="id" label="ID" width="80" align="center" header-align="center" />
      <el-table-column prop="username" label="用户名" width="148" align="center" header-align="center">
        <template #default="{ row }">
          <div class="user-name-cell">
            <span class="user-name-cell__text">{{ row.username }}</span>
            <el-tooltip
              v-if="canUnlockLoginLock(row)"
              :content="buildUsernameLockTooltip(row)"
              placement="top"
            >
              <el-icon
                class="user-name-cell__lock"
                :class="{
                  'user-name-cell__lock--account': row.loginLocked,
                  'user-name-cell__lock--ip': !row.loginLocked && row.loginIpLocked,
                }"
              >
                <Lock />
              </el-icon>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="nickname" label="昵称" width="120" align="center" header-align="center" />
      <el-table-column prop="mobile" label="手机号" width="130" align="center" header-align="center" />
      <el-table-column prop="deptName" label="部门" width="120" align="center" header-align="center" show-overflow-tooltip />
      <el-table-column prop="postNames" label="岗位" min-width="140" align="center" header-align="center" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100" align="center" header-align="center">
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
      <el-table-column prop="createTime" label="创建时间" width="180" align="center" header-align="center" />
      <el-table-column label="操作" width="200" fixed="right" align="center" header-align="center">
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
                  <el-dropdown-item
                    v-if="canUnlockLoginLock(row)"
                    v-permission="'system:user:update'"
                    command="unlockLogin"
                    :icon="Unlock"
                  >
                    解除登录锁定
                  </el-dropdown-item>
                  <el-dropdown-item v-permission="'system:user:update'" command="resetPwd" :icon="RefreshIcon">
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
      class="table-pagination"
      @size-change="$emit('load')"
      @current-change="$emit('load')"
    />
  </el-card>
</template>

<script setup lang="ts">
import { Search, Refresh as RefreshIcon, User, Delete, Unlock, Lock } from '@element-plus/icons-vue'
import { canUnlockLoginLock, buildUsernameLockTooltip } from '@/utils/login-lock'

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }
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
:deep(.dropdown-item-danger) {
  color: #f56c6c !important;
}
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  max-width: 100%;
}

.user-name-cell__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-name-cell__lock {
  flex-shrink: 0;
  font-size: 15px;
  cursor: help;
}

.user-name-cell__lock--account {
  color: #e6a23c;
}

.user-name-cell__lock--ip {
  color: #f56c6c;
}
</style>
