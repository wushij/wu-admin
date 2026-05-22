<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-card class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="角色名称">
          <el-input v-model="queryParams.name" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
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
          <span>角色列表</span>
          <div class="header-actions">
            <el-button v-permission="'system:role:delete'" @click="openRecycleDialog">回收站</el-button>
            <el-button type="primary" v-permission="'system:role:create'" @click="handleAdd">新增角色</el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="roleList"
        v-loading="loading"
        border
        stripe
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="code" label="角色编码" width="150" />
        <el-table-column prop="sort" label="排序" width="100" />
        <el-table-column prop="status" label="状态" width="100">
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
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button 
                type="primary" 
                size="small"
                v-permission="'system:role:update'" 
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-dropdown 
                v-permission="['system:role:update', 'system:role:delete']" 
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
                      v-permission="'system:role:update'" 
                      command="assignMenu"
                    >
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
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" :lock-scroll="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限对话框 -->
    <el-dialog v-model="menuDialogVisible" title="分配权限" width="500px" :lock-scroll="false">
      <el-form label-width="100px">
        <el-form-item label="角色名称">
          <el-input v-model="currentRole.name" disabled />
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-tree
            ref="menuTreeRef"
            :data="menuOptions"
            :props="{ label: 'name', children: 'children' }"
            show-checkbox
            node-key="id"
            :default-expand-all="true"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignMenu">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recycleVisible" title="角色回收站" width="900px" :lock-scroll="false">
      <el-table
        :data="recycleList"
        v-loading="recycleLoading"
        border
        stripe
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="code" label="角色编码" width="150" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
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

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoleList, createRole, updateRole, deleteRole, assignRoleMenu, updateRoleStatus, getRoleMenuIds, getRecycleRolePage, restoreRole, deleteRolePermanent } from '@/api/system/role'
import { getMenuList } from '@/api/system/menu'
import { buildMenuTree } from '@/utils/menu-tree'

const loading = ref(false)
const roleList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const menuDialogVisible = ref(false)
const formRef = ref(null)
const menuTreeRef = ref(null)
const currentRole = ref({})
const selectedMenus = ref([])
const menuOptions = ref([])
const recycleVisible = ref(false)
const recycleLoading = ref(false)
const recycleList = ref([])
const recycleTotal = ref(0)

const queryParams = reactive({
  name: '',
  status: null
})
const recycleQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  name: '',
  status: null
})

const form = reactive({
  id: null,
  name: '',
  code: '',
  sort: 0,
  status: 1,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const getList = async () => {
  loading.value = true
  try {
    const res = await getRoleList(queryParams)
    roleList.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  getList()
}

const resetQuery = () => {
  queryParams.name = ''
  queryParams.status = null
  handleQuery()
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  resetForm()
  dialogTitle.value = '编辑角色'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该角色吗？', '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  getList()
}

const getRecycleList = async () => {
  recycleLoading.value = true
  try {
    const res = await getRecycleRolePage(recycleQuery)
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
  await restoreRole(row.id)
  ElMessage.success('恢复成功')
  await getRecycleList()
  await getList()
}

const handlePermanentDelete = async (row) => {
  await ElMessageBox.confirm('确定彻底删除该角色吗？该操作不可恢复', '提示', { type: 'warning' })
  await deleteRolePermanent(row.id)
  ElMessage.success('清除成功')
  await getRecycleList()
}

// 状态切换
const handleStatusChange = async (row) => {
  try {
    const text = row.status === 1 ? '启用' : '禁用'
    await ElMessageBox.confirm(`确认要${text}角色"${row.name}"吗？`, '提示', { type: 'warning' })
    await updateRoleStatus(row.id, row.status)
    ElMessage.success(`${text}成功`)
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
  }
}

// 操作命令分发
const handleCommand = (command, row) => {
  switch (command) {
    case 'assignMenu':
      handleAssignMenu(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

const handleAssignMenu = async (row) => {
  currentRole.value = row
  // 先加载菜单列表
  if (menuOptions.value.length === 0) {
    const menuRes = await getMenuList()
    const data = menuRes.data || []
    menuOptions.value = data[0]?.children !== undefined || data.length === 0
      ? data
      : buildMenuTree(data)
  }
  // 获取角色已有菜单
  const res = await getRoleMenuIds(row.id)
  selectedMenus.value = res.data || []
  menuDialogVisible.value = true
  // 等待 DOM 更新后设置选中状态
  await nextTick()
  if (menuTreeRef.value) {
    menuTreeRef.value.setCheckedKeys([], false)
    menuTreeRef.value.setCheckedKeys(selectedMenus.value, true)
  }
}

const submitAssignMenu = async () => {
  const menuIds = menuTreeRef.value.getCheckedKeys()
  await assignRoleMenu({ roleId: currentRole.value.id, menuIds })
  ElMessage.success('分配成功')
  menuDialogVisible.value = false
  getList()
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.code = ''
  form.sort = 0
  form.status = 1
  form.remark = ''
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (form.id) {
        await updateRole(form)
        ElMessage.success('修改成功')
      } else {
        await createRole(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      getList()
    }
  })
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.app-container {
  padding: 0;
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
</style>
