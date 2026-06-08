<template>
  <div class="app-container org-page">
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
        :post-default-expanded-keys="postDefaultExpandedKeys"
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
import { ref, watchEffect } from 'vue'
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
  postDefaultExpandedKeys,
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
