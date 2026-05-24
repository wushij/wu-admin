<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 左侧部门树 -->
      <el-col :span="4">
        <el-card class="dept-card">
          <template #header>
            <span>部门</span>
          </template>
          <el-tree
            ref="deptTreeRef"
            :data="deptOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            highlight-current
            default-expand-all
            @node-click="handleDeptClick"
          />
        </el-card>
      </el-col>
      
      <!-- 右侧用户列表 -->
      <el-col :span="20">
        <!-- 搜索区域 -->
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
              <el-button type="primary" @click="handleQuery">搜索</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 数据表格 -->
        <el-card>
          <template #header>
            <div class="card-header">
              <span>用户列表</span>
              <div class="header-actions">
                <el-button v-permission="'system:user:delete'" @click="openRecycleDialog">回收站</el-button>
                <el-button type="primary" v-permission="'system:user:create'" @click="handleAdd">新增用户</el-button>
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
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-switch
                  v-model="row.status"
                  :active-value="1"
                  :inactive-value="0"
                  v-permission="'system:user:update'"
                  @change="handleStatusChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button 
                    type="primary" 
                    size="small"
                    v-permission="'system:user:update'" 
                    @click="handleEdit(row)"
                  >
                    编辑
                  </el-button>
                  <el-dropdown 
                    v-permission="['system:user:update', 'system:user:delete']" 
                    @command="(command) => handleCommand(command, row)"
                    trigger="click"
                  >
                    <el-button 
                      type="primary" 
                      size="small"
                    >
                      更多
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item 
                          v-permission="'system:user:update'" 
                          command="resetPwd"
                          :icon="Refresh"
                        >
                          重置密码
                        </el-dropdown-item>
                        <el-dropdown-item 
                          v-permission="'system:user:update'" 
                          command="assignRole"
                          :icon="User"
                        >
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
            @size-change="getList"
            @current-change="getList"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" :lock-scroll="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptSelectOptions"
            node-key="id"
            :props="{ label: 'name', children: 'children', disabled: 'disabled' }"
            placeholder="请选择部门"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="岗位" prop="postIds">
          <el-select
            v-model="form.postIds"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择岗位（可多选）"
            style="width: 100%"
          >
            <el-option
              v-for="item in postOptions"
              :key="item.id"
              :label="`${item.postName}（${item.postCode}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <DictSelect
            v-model="form.status"
            dict-type="sys_normal_disable"
            value-type="number"
            :clearable="false"
            width="160px"
          />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-radio-group v-model="form.roleId">
            <el-radio v-for="role in roleOptions" :key="role.id" :label="role.id">
              {{ role.name }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="500px" :lock-scroll="false">
      <el-form label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="currentUser.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="selectedRole">
            <el-radio v-for="role in roleOptions" :key="role.id" :label="role.id">
              {{ role.name }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignRole">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="400px" :lock-scroll="false">
      <el-form :model="resetPwdForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="resetPwdForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetPwdForm.password" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResetPwd">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recycleVisible" title="用户回收站" width="980px" :lock-scroll="false">
      <el-table
        :data="recycleList"
        v-loading="recycleLoading"
        border
        stripe
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="mobile" label="手机号" width="130" />
        <el-table-column prop="deptName" label="部门" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <DictTag :value="row.status" dict-type="sys_normal_disable" />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="删除时间" width="180" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="success" @click="handleRestore(row)">恢复</el-button>
              <el-button size="small" type="danger" @click="handlePermanentDelete(row)">清除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="recycleQuery.pageNo"
        v-model:page-size="recycleQuery.pageSize"
        :total="recycleTotal"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="getRecycleList"
        @current-change="getRecycleList"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, User, Delete } from '@element-plus/icons-vue'
import { getUserPage, createUser, updateUser, deleteUser, assignUserRole, updateUserStatus, resetUserPassword, getUserRoleIds, getRecycleUserPage, restoreUser, deleteUserPermanent } from '@/api/system/user'
import { getRoleList } from '@/api/system/role'
import { getDeptTree } from '@/api/system/dept'
import { getPostList } from '@/api/system/post'

const route = useRoute()
const loading = ref(false)
const total = ref(0)
const userList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const roleDialogVisible = ref(false)
const resetPwdVisible = ref(false)
const formRef = ref(null)
const deptTreeRef = ref(null)
const currentUser = ref<Partial<import('@/api/system/user').UserVO>>({})
const selectedRole = ref(null)
const roleOptions = ref([])
const deptOptions = ref([])
const deptSelectOptions = ref([])
const postOptions = ref([])
const recycleVisible = ref(false)
const recycleLoading = ref(false)
const recycleList = ref([])
const recycleTotal = ref(0)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  username: '',
  mobile: '',
  status: null,
  deptId: null
})

const form = reactive({
  id: null,
  username: '',
  nickname: '',
  password: '',
  mobile: '',
  email: '',
  deptId: null,
  status: 1,
  roleId: null,
  postIds: [],
  remark: ''
})

const resetPwdForm = reactive({
  id: null,
  username: '',
  password: ''
})

const recycleQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  username: '',
  mobile: '',
  status: null,
  deptId: null
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const getList = async () => {
  loading.value = true
  try {
    const res = await getUserPage(queryParams)
    userList.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 点击部门树筛选用户
const handleDeptClick = (data) => {
  // 如果点击的是顶级节点（parentId为null或0），则显示全部
  if (data.parentId === null || data.parentId === 0) {
    queryParams.deptId = null
  } else {
    queryParams.deptId = data.id
  }
  queryParams.pageNo = 1
  getList()
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.username = ''
  queryParams.mobile = ''
  queryParams.status = null
  handleQuery()
}

// 状态切换
const handleStatusChange = async (row) => {
  try {
    const text = row.status === 1 ? '启用' : '禁用'
    await ElMessageBox.confirm(`确认要${text}用户"${row.username}"吗？`, '提示', { type: 'warning' })
    await updateUserStatus(row.id, row.status)
    ElMessage.success(`${text}成功`)
  } catch (error) {
    // 取消时恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

// 操作命令分发
const handleCommand = (command, row) => {
  switch (command) {
    case 'resetPwd':
      handleResetPwd(row)
      break
    case 'assignRole':
      handleAssignRole(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

// 重置密码
const handleResetPwd = (row) => {
  resetPwdForm.id = row.id
  resetPwdForm.username = row.username
  resetPwdForm.password = ''
  resetPwdVisible.value = true
}

const submitResetPwd = async () => {
  if (!resetPwdForm.password) {
    ElMessage.warning('请输入新密码')
    return
  }
  try {
    await resetUserPassword(resetPwdForm.id, resetPwdForm.password)
    ElMessage.success('重置密码成功')
    resetPwdVisible.value = false
  } catch (error) {
    console.error(error)
  }
}

const handleAdd = async () => {
  resetForm()
  dialogTitle.value = '新增用户'
  // 加载角色列表
  if (roleOptions.value.length === 0) {
    const res = await getRoleList()
    roleOptions.value = res.data || []
  }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  resetForm()
  dialogTitle.value = '编辑用户'
  Object.assign(form, row)
  // 确保角色列表已加载
  if (roleOptions.value.length === 0) {
    const res = await getRoleList()
    roleOptions.value = res.data || []
  }
  // 设置用户当前角色（取第一个角色ID）
  if (row.roleIds && row.roleIds.length > 0) {
    form.roleId = [...row.roleIds][0]
  }
  form.postIds = row.postIds ? [...row.postIds] : []
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该用户吗？', '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  getList()
}

const getRecycleList = async () => {
  recycleLoading.value = true
  try {
    const res = await getRecycleUserPage(recycleQuery)
    recycleList.value = res.data?.list || []
    recycleTotal.value = res.data?.total || 0
  } finally {
    recycleLoading.value = false
  }
}

const openRecycleDialog = async () => {
  recycleQuery.pageNo = 1
  recycleVisible.value = true
  await getRecycleList()
}

const handleRestore = async (row) => {
  await restoreUser(row.id)
  ElMessage.success('恢复成功')
  await getRecycleList()
  await getList()
}

const handlePermanentDelete = async (row) => {
  await ElMessageBox.confirm('确定彻底删除该用户吗？该操作不可恢复', '提示', { type: 'warning' })
  await deleteUserPermanent(row.id)
  ElMessage.success('清除成功')
  await getRecycleList()
}

const handleAssignRole = async (row) => {
  currentUser.value = row
  // 获取用户已有角色
  const res = await getUserRoleIds(row.id)
  // 取第一个角色ID（单选模式）
  const roleIds = res.data || []
  selectedRole.value = roleIds.length > 0 ? roleIds[0] : null
  // 加载角色列表
  if (roleOptions.value.length === 0) {
    const roleRes = await getRoleList()
    roleOptions.value = roleRes.data || []
  }
  roleDialogVisible.value = true
}

const submitAssignRole = async () => {
  // 单选模式：将单个角色ID转为数组
  const roleIds = selectedRole.value ? [selectedRole.value] : []
  await assignUserRole({ userId: currentUser.value.id, roleIds })
  ElMessage.success('分配成功')
  roleDialogVisible.value = false
  getList()
}

const resetForm = () => {
  form.id = null
  form.username = ''
  form.nickname = ''
  form.password = ''
  form.mobile = ''
  form.email = ''
  form.deptId = null
  form.status = 1
  form.roleId = null
  form.postIds = []
  form.remark = ''
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (form.id) {
        await updateUser(form)
        ElMessage.success('修改成功')
      } else {
        await createUser(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      getList()
    }
  })
}

const loadDeptTree = async () => {
  const res = await getDeptTree()
  deptOptions.value = res.data || []
  deptSelectOptions.value = res.data || []
}

const loadPostOptions = async () => {
  const res = await getPostList()
  postOptions.value = res.data || []
}

onMounted(async () => {
  await loadDeptTree()
  loadPostOptions()
  const deptIdFromRoute = route.query.deptId
  if (deptIdFromRoute) {
    const deptId = Number(deptIdFromRoute)
    if (!Number.isNaN(deptId) && deptId > 0) {
      queryParams.deptId = deptId
      await nextTick()
      deptTreeRef.value?.setCurrentKey(deptId)
    }
  }
  getList()
})
</script>

<style scoped>
.app-container {
  padding: 0;
}
.dept-card {
  height: calc(100vh - 150px);
  overflow: auto;
}
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

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-buttons .el-button {
  margin: 0;
}

/* 下拉菜单危险操作样式 */
:deep(.dropdown-item-danger) {
  color: #f56c6c !important;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
