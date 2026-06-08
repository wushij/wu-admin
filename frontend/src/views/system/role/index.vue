<template>
  <div class="app-container module-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.role" />
            <span>角色管理</span>
          </div>
          <p class="module-hero-desc">配置系统角色与权限，控制菜单访问与操作授权</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ roleList.length }}</div>
          <div class="stat-label">角色总数</div>
        </div>
      </div>
    </el-card>

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

    <!-- 数据表格 -->
    <el-card>
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
          <DictSelect
            v-model="form.status"
            dict-type="sys_normal_disable"
            value-type="number"
            :clearable="false"
            width="160px"
          />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import type { ElTree } from 'element-plus'
import {
  getRoleList,
  createRole,
  updateRole,
  deleteRole,
  assignRoleMenu,
  updateRoleStatus,
  type RoleSaveDTO,
  type RoleListQuery,
  getRoleMenuIds,
  type RoleVO,
} from '@/api/system/role'
import { getMenuList, type MenuVO } from '@/api/system/menu'
import { buildMenuTree } from '@/utils/menu-tree'
import RecycleCenterLink from '@/components/RecycleCenterLink.vue'

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const loading = ref(false)
const roleList = ref<RoleVO[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const menuDialogVisible = ref(false)
const formRef = ref<FormInstance | null>(null)
const menuTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
const currentRole = ref<Partial<RoleVO>>({})
const selectedMenus = ref<number[]>([])
const menuOptions = ref<MenuVO[]>([])

const queryParams = reactive<RoleListQuery>({
  name: '',
  status: null
})

const form = reactive<RoleSaveDTO>({
  id: null,
  name: '',
  code: '',
  sort: 0,
  status: 1,
  remark: '',
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

const handleEdit = (row: RoleVO) => {
  resetForm()
  dialogTitle.value = '编辑角色'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row: RoleVO) => {
  await ElMessageBox.confirm('确定要删除该角色吗？', '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  getList()
}

// 状态切换
const handleStatusChange = async (row: RoleVO) => {
  try {
    const text = row.status === 1 ? '启用' : '禁用'
    await ElMessageBox.confirm(`确认要${text}角色"${row.name}"吗？`, '提示', { type: 'warning' })
    if (row.id == null || row.status == null) return
    await updateRoleStatus(row.id, row.status)
    ElMessage.success(`${text}成功`)
  } catch {
    row.status = row.status === 1 ? 0 : 1
  }
}

// 操作命令分发
const handleCommand = (command: string, row: RoleVO) => {
  switch (command) {
    case 'assignMenu':
      handleAssignMenu(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

const handleAssignMenu = async (row: RoleVO) => {
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
  selectedMenus.value = (res.data || []).map((id) => Number(id))
  menuDialogVisible.value = true
  // 等待 DOM 更新后设置选中状态
  await nextTick()
  if (menuTreeRef.value) {
    menuTreeRef.value.setCheckedKeys([], false)
    menuTreeRef.value.setCheckedKeys(selectedMenus.value, true)
  }
}

const submitAssignMenu = async () => {
  if (!menuTreeRef.value || currentRole.value.id == null) return
  const menuIds = menuTreeRef.value
    .getCheckedKeys()
    .map((key) => Number(key))
    .filter((id) => !Number.isNaN(id))
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
:deep(.dropdown-item-danger) {
  color: #f56c6c !important;
}
</style>
