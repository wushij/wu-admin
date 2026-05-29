<template>
  <div class="app-container org-page">
    <el-tabs v-model="activeTab" class="org-tabs">
      <el-tab-pane label="部门体系" name="dept" />
      <el-tab-pane label="岗位体系" name="post" />
    </el-tabs>

    <div class="org-layout">
      <!-- 左侧树 -->
      <el-card class="org-tree-card" shadow="never">
        <template #header>
          <span class="org-tree-title">{{ activeTab === 'dept' ? '部门体系' : '岗位体系' }}</span>
        </template>
        <el-input
          v-model="treeSearch"
          placeholder="搜索名称"
          clearable
          class="org-tree-search"
        />
        <el-tree
          v-if="activeTab === 'dept'"
          ref="deptTreeRef"
          :data="deptDisplayTree"
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          :filter-node-method="filterTreeNode"
          highlight-current
          draggable
          :allow-drop="allowDeptDrop"
          default-expand-all
          @node-click="onDeptNodeClick"
          @node-drop="onDeptDrop"
        >
          <template #default="{ data }">
            <span class="tree-node-label">{{ data.name }}</span>
          </template>
        </el-tree>
        <el-tree
          v-else
          ref="postTreeRef"
          :data="postTree"
          node-key="id"
          :props="{ label: 'postName', children: 'children' }"
          :filter-node-method="filterPostTreeNode"
          highlight-current
          draggable
          :allow-drop="allowPostDrop"
          default-expand-all
          @node-click="onPostNodeClick"
          @node-drop="onPostDrop"
        >
          <template #default="{ data }">
            <span class="tree-node-label">{{ data.postName }}</span>
          </template>
        </el-tree>
        <el-button
          v-permission="activeTab === 'dept' ? 'system:dept:create' : 'system:post:create'"
          class="org-add-root"
          type="primary"
          plain
          @click="handleAddRoot"
        >
          {{ activeTab === 'dept' ? '新增一级部门' : '新增顶级岗位' }}
        </el-button>
      </el-card>

      <!-- 右侧成员 -->
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
                @click="goUserManage"
              >
                用户管理
                <el-icon class="link-icon"><ArrowRight /></el-icon>
              </el-link>
              <template v-if="selectedId">
                <el-button
                  v-permission="activeTab === 'dept' ? 'system:dept:update' : 'system:post:update'"
                  type="primary"
                  size="small"
                  @click="handleEditNode"
                >
                  编辑
                </el-button>
                <el-button
                  v-permission="activeTab === 'dept' ? 'system:dept:create' : 'system:post:create'"
                  type="primary"
                  size="small"
                  @click="handleAddChild"
                >
                  新增子级
                </el-button>
                <el-button
                  v-if="activeTab === 'dept'"
                  v-permission="'system:dept:delete'"
                  size="small"
                  @click="openRecycle"
                >
                  回收站
                </el-button>
                <el-button
                  v-permission="activeTab === 'dept' ? 'system:dept:delete' : 'system:post:delete'"
                  type="danger"
                  size="small"
                  @click="handleDeleteNode"
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
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </el-card>
    </div>

    <!-- 部门表单 -->
    <el-dialog v-model="deptDialogVisible" :title="deptDialogTitle" width="560px" destroy-on-close>
      <el-form ref="deptFormRef" :model="deptForm" :rules="deptRules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="deptForm.parentId"
            :data="deptTreeOptions"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            clearable
            placeholder="主目录（顶级）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="deptForm.name" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="deptForm.leaderName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="deptForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="deptForm.email" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="deptForm.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="deptForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="deptSubmitting" @click="submitDept">确定</el-button>
      </template>
    </el-dialog>

    <!-- 岗位表单 -->
    <el-dialog v-model="postDialogVisible" :title="postDialogTitle" width="520px" destroy-on-close>
      <el-form ref="postFormRef" :model="postForm" :rules="postRules" label-width="90px">
        <el-form-item label="上级岗位">
          <el-tree-select
            v-model="postForm.parentId"
            :data="postTreeOptions"
            node-key="id"
            :props="{ label: 'postName', children: 'children' }"
            check-strictly
            clearable
            placeholder="顶级岗位"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="岗位编码" prop="postCode">
          <el-input v-model="postForm.postCode" :disabled="!!postForm.id" />
        </el-form-item>
        <el-form-item label="岗位名称" prop="postName">
          <el-input v-model="postForm.postName" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="postForm.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="postForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="postForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="postDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="postSubmitting" @click="submitPost">确定</el-button>
      </template>
    </el-dialog>

    <!-- 部门回收站 -->
    <el-drawer v-model="recycleVisible" title="部门回收站" size="720px">
      <el-table :data="recycleList" v-loading="recycleLoading" border stripe>
        <el-table-column prop="name" label="部门名称" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleRestore(row)">恢复</el-button>
            <el-button type="danger" size="small" @click="handlePermanentDelete(row)">彻底删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="recycleQuery.pageNo"
        v-model:page-size="recycleQuery.pageSize"
        :total="recycleTotal"
        layout="total, prev, pager, next"
        class="recycle-pagination"
        @current-change="loadRecycle"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import type { ElTree } from 'element-plus'
import { getUserPage, type UserVO, type UserPageQuery } from '@/api/system/user'
import type { DeptVO, DeptSaveDTO, DeptRecycleQuery } from '@/api/system/dept'
import type { PostVO, PostSaveDTO } from '@/api/system/post'
import {
  getDeptTree,
  getDept,
  createDept,
  updateDept,
  deleteDept,
  moveDept,
  getRecycleDeptPage,
  restoreDept,
  deleteDeptPermanent
} from '@/api/system/dept'
import {
  getPostTree,
  getPost,
  createPost,
  updatePost,
  deletePost,
  movePost
} from '@/api/system/post'

import { unwrapOrgTreeNode } from '@/types/org'
import { displayOrgTree, resolveDeptRootParentId } from '@/utils/org-tree'

const router = useRouter()
const activeTab = ref('dept')
const treeSearch = ref('')
const deptTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
const postTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
/** 部门完整树（含隐藏的本部根） */
const deptTreeRaw = ref<DeptVO[]>([])
const deptDisplayTree = computed(() => displayOrgTree(deptTreeRaw.value))
const postTree = ref<PostVO[]>([])

const selectedId = ref<number | null>(null)
const selectedName = ref('')

const userLoading = ref(false)
const userList = ref<UserVO[]>([])
const userTotal = ref(0)
const userQuery = reactive<UserPageQuery>({ pageNo: 1, pageSize: 10 })

const memberTitle = computed(() => {
  if (!selectedId.value) return '所有用户'
  const label = activeTab.value === 'dept' ? '部门成员' : '岗位成员'
  return `【${selectedName.value}】${label}`
})

watch(activeTab, () => {
  selectedId.value = null
  selectedName.value = ''
  treeSearch.value = ''
  userQuery.pageNo = 1
  loadTree()
  loadUsers()
})

watch(treeSearch, (val) => {
  if (activeTab.value === 'dept') {
    deptTreeRef.value?.filter(val)
  } else {
    postTreeRef.value?.filter(val)
  }
})

function filterTreeNode(value: string, data: unknown) {
  const node = data as DeptVO
  if (!value) return true
  return String(node.name ?? '').includes(value)
}

function filterPostTreeNode(value: string, data: unknown) {
  const node = data as PostVO
  if (!value) return true
  const label = String(node.postName ?? node.name ?? '')
  return label.includes(value)
}

async function loadTree() {
  if (activeTab.value === 'dept') {
    const res = await getDeptTree()
    deptTreeRaw.value = res.data || []
  } else {
    const res = await getPostTree()
    postTree.value = res.data || []
  }
}

async function loadUsers() {
  userLoading.value = true
  try {
    const params: UserPageQuery = {
      pageNo: userQuery.pageNo,
      pageSize: userQuery.pageSize,
    }
    if (selectedId.value) {
      if (activeTab.value === 'dept') params.deptId = selectedId.value
      else params.postId = selectedId.value
    }
    const res = await getUserPage(params)
    userList.value = res.data?.list || []
    userTotal.value = Number(res.data?.total) || 0
  } finally {
    userLoading.value = false
  }
}

function goUserManage() {
  const query: Record<string, string> = {}
  if (selectedId.value && activeTab.value === 'dept') {
    query.deptId = String(selectedId.value)
  }
  router.push({ path: '/system/user', query })
}

function onDeptNodeClick(data: DeptVO) {
  if (selectedId.value === data.id) {
    selectedId.value = null
    selectedName.value = ''
  } else {
    selectedId.value = data.id
    selectedName.value = data.name
  }
  userQuery.pageNo = 1
  loadUsers()
}

function onPostNodeClick(data: PostVO) {
  if (selectedId.value === data.id) {
    selectedId.value = null
    selectedName.value = ''
  } else {
    selectedId.value = data.id
    selectedName.value = String(data.postName ?? data.name ?? '')
  }
  userQuery.pageNo = 1
  loadUsers()
}

function allowDeptDrop(dragging: unknown, drop: unknown, type: string) {
  const dragData = unwrapOrgTreeNode<DeptVO>(dragging)
  const dropData = unwrapOrgTreeNode<DeptVO>(drop)
  if (type === 'inner' && dragData.id === dropData.id) return false
  return true
}

function allowPostDrop(dragging: unknown, drop: unknown, type: string) {
  const dragData = unwrapOrgTreeNode<PostVO>(dragging)
  const dropData = unwrapOrgTreeNode<PostVO>(drop)
  if (type === 'inner' && dragData.id === dropData.id) return false
  return true
}

async function onDeptDrop(
  dragging: unknown,
  drop: unknown,
  dropType: 'before' | 'after' | 'inner',
  _evt?: DragEvent
) {
  const dragData = unwrapOrgTreeNode<DeptVO>(dragging)
  const dropData = unwrapOrgTreeNode<DeptVO>(drop)
  const id = dragData.id
  const parentId = dropType === 'inner' ? dropData.id : (dropData.parentId || 0)
  await moveDept(id, parentId, 0)
  ElMessage.success('移动成功')
  loadTree()
}

async function onPostDrop(
  dragging: unknown,
  drop: unknown,
  dropType: 'before' | 'after' | 'inner',
  _evt?: DragEvent
) {
  const dragData = unwrapOrgTreeNode<PostVO>(dragging)
  const dropData = unwrapOrgTreeNode<PostVO>(drop)
  const id = dragData.id
  const parentId = dropType === 'inner' ? dropData.id : (dropData.parentId || 0)
  await movePost(id, parentId)
  ElMessage.success('移动成功')
  loadTree()
}

// ---------- 部门表单 ----------
const deptDialogVisible = ref(false)
const deptDialogTitle = ref('')
const deptSubmitting = ref(false)
const deptFormRef = ref()
const deptForm = reactive<DeptSaveDTO>({
  id: undefined,
  parentId: 0,
  name: '',
  leaderName: '',
  phone: '',
  email: '',
  sort: 0,
  status: 1,
})
const deptRules = { name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }
const deptTreeOptions = computed(() => [{ id: 0, name: '主目录', children: deptTreeRaw.value }])

// ---------- 岗位表单 ----------
const postDialogVisible = ref(false)
const postDialogTitle = ref('')
const postSubmitting = ref(false)
const postFormRef = ref()
const postForm = reactive<PostSaveDTO>({
  id: undefined,
  parentId: 0,
  postCode: '',
  postName: '',
  sort: 0,
  status: 1,
  remark: '',
})
const postRules = {
  postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  postName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }]
}
const postTreeOptions = computed(() => [{ id: 0, postName: '顶级', children: postTree.value }])

function handleAddRoot() {
  if (activeTab.value === 'dept') {
    openDeptForm(null, resolveDeptRootParentId(deptTreeRaw.value))
  } else {
    openPostForm(null, 0)
  }
}

function handleAddChild() {
  if (activeTab.value === 'dept') openDeptForm(null, selectedId.value)
  else openPostForm(null, selectedId.value)
}

async function handleEditNode() {
  if (selectedId.value == null) return
  const nodeId = selectedId.value
  if (activeTab.value === 'dept') {
    const res = await getDept(nodeId)
    openDeptForm(res.data, res.data?.parentId ?? 0)
  } else {
    const res = await getPost(nodeId)
    openPostForm(res.data, res.data?.parentId ?? 0)
  }
}

function openDeptForm(row: DeptVO | null, parentId?: number | null) {
  deptDialogTitle.value = row?.id ? '编辑部门' : '新增部门'
  Object.assign(deptForm, {
    id: row?.id,
    parentId: row?.parentId ?? parentId ?? 0,
    name: row?.name || '',
    leaderName: row?.leaderName || '',
    phone: row?.phone || '',
    email: row?.email || '',
    sort: row?.sort ?? 0,
    status: row?.status ?? 1
  })
  if (deptForm.parentId == null) deptForm.parentId = 0
  deptDialogVisible.value = true
}

function openPostForm(row: PostVO | null, parentId?: number | null) {
  postDialogTitle.value = row?.id ? '编辑岗位' : '新增岗位'
  Object.assign(postForm, {
    id: row?.id,
    parentId: row?.parentId ?? parentId ?? 0,
    postCode: row?.postCode || '',
    postName: row?.postName || '',
    sort: row?.sort ?? 0,
    status: row?.status ?? 1,
    remark: row?.remark || ''
  })
  if (postForm.parentId == null) postForm.parentId = 0
  postDialogVisible.value = true
}

async function submitDept() {
  await deptFormRef.value?.validate()
  deptSubmitting.value = true
  try {
    const payload = { ...deptForm, parentId: deptForm.parentId || 0 }
    if (deptForm.id) {
      await updateDept(payload)
      ElMessage.success('更新成功')
    } else {
      await createDept(payload)
      ElMessage.success('创建成功')
    }
    deptDialogVisible.value = false
    loadTree()
  } finally {
    deptSubmitting.value = false
  }
}

async function submitPost() {
  await postFormRef.value?.validate()
  postSubmitting.value = true
  try {
    const payload = { ...postForm, parentId: postForm.parentId || 0 }
    if (postForm.id) {
      await updatePost(payload)
      ElMessage.success('更新成功')
    } else {
      await createPost(payload)
      ElMessage.success('创建成功')
    }
    postDialogVisible.value = false
    loadTree()
  } finally {
    postSubmitting.value = false
  }
}

async function handleDeleteNode() {
  if (selectedId.value == null) return
  const nodeId = selectedId.value
  const name = selectedName.value
  await ElMessageBox.confirm(`确定删除「${name}」？`, '提示', { type: 'warning' })
  if (activeTab.value === 'dept') {
    await deleteDept(nodeId)
  } else {
    await deletePost(nodeId)
  }
  ElMessage.success('删除成功')
  selectedId.value = null
  selectedName.value = ''
  loadTree()
  loadUsers()
}

// ---------- 回收站 ----------
const recycleVisible = ref(false)
const recycleLoading = ref(false)
const recycleList = ref<DeptVO[]>([])
const recycleTotal = ref(0)
const recycleQuery = reactive<DeptRecycleQuery>({ pageNo: 1, pageSize: 10 })

function openRecycle() {
  recycleVisible.value = true
  loadRecycle()
}

async function loadRecycle() {
  recycleLoading.value = true
  try {
    const res = await getRecycleDeptPage(recycleQuery)
    recycleList.value = res.data?.list || []
    recycleTotal.value = Number(res.data?.total) || 0
  } finally {
    recycleLoading.value = false
  }
}

async function handleRestore(row: DeptVO) {
  await restoreDept(row.id)
  ElMessage.success('已恢复')
  loadRecycle()
  loadTree()
}

async function handlePermanentDelete(row: DeptVO) {
  await ElMessageBox.confirm('彻底删除后不可恢复，是否继续？', '警告', { type: 'warning' })
  await deleteDeptPermanent(row.id)
  ElMessage.success('已删除')
  loadRecycle()
}

onMounted(() => {
  loadTree()
  loadUsers()
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
.org-tree-card {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    flex: 1;
    padding-bottom: 12px;
  }
}
.org-tree-title {
  font-weight: 600;
}
.org-tree-search {
  margin-bottom: 10px;
}
.org-tree-card :deep(.el-tree) {
  flex: 1;
  overflow: auto;
  max-height: calc(100vh - 320px);
}
.org-add-root {
  width: 100%;
  margin-top: 12px;
}
.org-member-card {
  flex: 1;
  min-width: 0;
}
.tree-node-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
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
.recycle-pagination {
  margin-top: 12px;
}
</style>
