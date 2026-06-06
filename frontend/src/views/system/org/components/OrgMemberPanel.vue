<template>
  <el-card class="org-member-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span>{{ memberTitle }}</span>
        <div class="header-actions">
          <el-link
            v-permission="'system:user:list'"
            type="primary"
            :underline="false"
            class="user-mgmt-link"
            @click="$emit('go-user-manage')"
          >
            用户管理
            <el-icon class="link-icon"><ArrowRight /></el-icon>
          </el-link>
          <template v-if="selectedId">
            <el-button
              v-permission="activeTab === 'dept' ? 'system:dept:update' : 'system:post:update'"
              type="primary"
              size="small"
              @click="$emit('edit-node')"
            >
              编辑
            </el-button>
            <el-button
              v-permission="activeTab === 'dept' ? 'system:dept:create' : 'system:post:create'"
              type="primary"
              size="small"
              @click="$emit('add-child')"
            >
              新增子级
            </el-button>
            <el-button
              v-if="activeTab === 'dept'"
              v-permission="'system:dept:delete'"
              size="small"
              @click="$emit('open-recycle')"
            >
              回收站
            </el-button>
            <el-button
              v-permission="activeTab === 'dept' ? 'system:dept:delete' : 'system:post:delete'"
              type="danger"
              size="small"
              @click="$emit('delete-node')"
            >
              删除
            </el-button>
          </template>
        </div>
      </div>
    </template>

    <el-table
      :data="userList"
      v-loading="userLoading"
      border
      stripe
      :header-cell-style="{ textAlign: 'center' }"
      :cell-style="{ textAlign: 'center' }"
    >
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="deptName" label="部门" min-width="110" show-overflow-tooltip />
      <el-table-column prop="postNames" label="岗位" min-width="120" show-overflow-tooltip />
      <el-table-column prop="mobile" label="手机号" width="120" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <DictTag :value="row.status" dict-type="sys_normal_disable" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
    </el-table>
    <el-pagination
      v-model:current-page="userQuery.pageNo"
      v-model:page-size="userQuery.pageSize"
      :total="userTotal"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="$emit('load-users')"
      @current-change="$emit('load-users')"
    />
  </el-card>
</template>

<script setup lang="ts">
import { ArrowRight } from '@element-plus/icons-vue'
import type { UserVO, UserPageQuery } from '@/api/system/user'

defineProps<{
  activeTab: string
  memberTitle: string
  selectedId: number | null
  userList: UserVO[]
  userLoading: boolean
  userTotal: number
  userQuery: UserPageQuery
}>()

defineEmits<{
  'go-user-manage': []
  'edit-node': []
  'add-child': []
  'open-recycle': []
  'delete-node': []
  'load-users': []
}>()
</script>

<style scoped lang="scss">
.org-member-card {
  flex: 1;
  min-width: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.user-mgmt-link {
  display: inline-flex;
  align-items: center;
  font-size: 14px;
  margin-right: 4px;
  .link-icon {
    margin-left: 2px;
    font-size: 12px;
  }
}
.el-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
