<template>
  <div class="app-container module-page org-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.org" />
            <span>组织架构</span>
          </div>
          <p class="module-hero-desc">管理部门与岗位体系，左侧树形导航，右侧查看成员并维护节点</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ orgNodeCount }}</div>
          <div class="stat-label">{{ orgStatLabel }}</div>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" class="org-tabs">
      <el-tab-pane label="部门体系" name="dept" />
      <el-tab-pane label="岗位体系" name="post" />
    </el-tabs>

    <div class="org-layout">
      <OrgTreePanel
        ref="treePanelRef"
        v-model:tree-search="treeSearch"
        :active-tab="activeTab"
        :dept-display-tree="deptDisplayTree"
        :post-tree="postTree"
        :filter-tree-node="filterTreeNode"
        :filter-post-tree-node="filterPostTreeNode"
        :allow-dept-drop="allowDeptDrop"
        :allow-post-drop="allowPostDrop"
        @dept-node-click="onDeptNodeClick"
        @post-node-click="onPostNodeClick"
        @dept-drop="onDeptDrop"
        @post-drop="onPostDrop"
        @add-root="handleAddRoot"
      />

      <OrgMemberPanel
        :active-tab="activeTab"
        :member-title="memberTitle"
        :selected-id="selectedId"
        :user-list="userList"
        :user-loading="userLoading"
        :user-total="userTotal"
        :user-query="userQuery"
        @go-user-manage="goUserManage"
        @edit-node="handleEditNode"
        @add-child="handleAddChild"
        @delete-node="handleDeleteNode"
        @load-users="loadUsers"
      />
    </div>

    <DeptFormDialog
      ref="deptDialogRef"
      v-model:visible="deptDialogVisible"
      :title="deptDialogTitle"
      :form="deptForm"
      :rules="deptRules"
      :tree-options="deptTreeOptions"
      :submitting="deptSubmitting"
      @submit="submitDept"
    />

    <PostFormDialog
      ref="postDialogRef"
      v-model:visible="postDialogVisible"
      :title="postDialogTitle"
      :form="postForm"
      :rules="postRules"
      :tree-options="postTreeOptions"
      :submitting="postSubmitting"
      @submit="submitPost"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect, computed } from 'vue'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import type { DeptVO } from '@/api/system/dept'
import type { PostVO } from '@/api/system/post'
import OrgTreePanel from './OrgTreePanel.vue'
import OrgMemberPanel from './OrgMemberPanel.vue'
import DeptFormDialog from './DeptFormDialog.vue'
import PostFormDialog from './PostFormDialog.vue'
import { useOrgPage } from '../composables/useOrgPage'

const treePanelRef = ref<InstanceType<typeof OrgTreePanel> | null>(null)
const deptDialogRef = ref<InstanceType<typeof DeptFormDialog> | null>(null)
const postDialogRef = ref<InstanceType<typeof PostFormDialog> | null>(null)

const {
  activeTab,
  treeSearch,
  deptTreeRef,
  postTreeRef,
  deptDisplayTree,
  postTree,
  selectedId,
  memberTitle,
  userLoading,
  userList,
  userTotal,
  userQuery,
  filterTreeNode,
  filterPostTreeNode,
  goUserManage,
  onDeptNodeClick,
  onPostNodeClick,
  allowDeptDrop,
  allowPostDrop,
  onDeptDrop,
  onPostDrop,
  handleAddRoot,
  handleAddChild,
  handleEditNode,
  handleDeleteNode,
  deptDialogVisible,
  deptDialogTitle,
  deptSubmitting,
  deptFormRef,
  deptForm,
  deptRules,
  deptTreeOptions,
  submitDept,
  postDialogVisible,
  postDialogTitle,
  postSubmitting,
  postFormRef,
  postForm,
  postRules,
  postTreeOptions,
  submitPost,
  loadUsers,
} = useOrgPage()

function countOrgNodes<T extends { children?: T[] }>(list: T[]): number {
  return list.reduce((sum, item) => sum + 1 + countOrgNodes(item.children || []), 0)
}

const orgNodeCount = computed(() =>
  activeTab.value === 'dept'
    ? countOrgNodes(deptDisplayTree.value as DeptVO[])
    : countOrgNodes(postTree.value as PostVO[]),
)

const orgStatLabel = computed(() => (activeTab.value === 'dept' ? '部门节点' : '岗位节点'))

watchEffect(() => {
  deptTreeRef.value = treePanelRef.value?.deptTreeRef ?? null
  postTreeRef.value = treePanelRef.value?.postTreeRef ?? null
  deptFormRef.value = deptDialogRef.value?.formRef
  postFormRef.value = postDialogRef.value?.formRef
})
</script>

<style scoped lang="scss">
.org-page {
  .org-tabs {
    margin-bottom: 12px;
  }
}
.org-layout {
  display: flex;
  gap: 12px;
  min-height: calc(100vh - 200px);
}
</style>
