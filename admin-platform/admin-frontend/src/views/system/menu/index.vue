<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-card class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="菜单名称">
          <el-input v-model="queryParams.name" placeholder="请输入菜单名称" clearable />
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
          <span>菜单列表</span>
          <div class="header-actions">
            <el-button v-permission="'system:menu:delete'" @click="openRecycleDialog">回收站</el-button>
            <el-button type="primary" v-permission="'system:menu:create'" @click="handleAdd(null)">新增菜单</el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="menuList"
        v-loading="loading"
        row-key="id"
        border
        :tree-props="{ children: 'children' }"
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="name" label="菜单名称" width="200" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'primary' : row.type === 2 ? 'success' : 'warning'">
              {{ row.type === 1 ? '目录' : row.type === 2 ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" width="150" />
        <el-table-column prop="component" label="组件路径" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              v-permission="'system:menu:update'"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button 
                type="primary" 
                size="small"
                v-permission="'system:menu:create'" 
                @click="handleAdd(row)"
              >
                新增
              </el-button>
              <el-button 
                type="primary" 
                size="small"
                v-permission="'system:menu:update'" 
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button 
                type="danger" 
                size="small"
                v-permission="'system:menu:delete'" 
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" :lock-scroll="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="menuOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择上级菜单"
            check-strictly
            clearable
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">目录</el-radio>
            <el-radio :label="2">菜单</el-radio>
            <el-radio :label="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="路由路径" prop="path" v-if="form.type !== 3">
          <el-input v-model="form.path" placeholder="请输入路由路径" />
        </el-form-item>
        <el-form-item label="组件路径" prop="component" v-if="form.type === 2">
          <el-input v-model="form.component" placeholder="请输入组件路径" />
        </el-form-item>
        <el-form-item label="权限标识" prop="permission" v-if="form.type === 3">
          <el-input v-model="form.permission" placeholder="请输入权限标识" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="图标" prop="icon" v-if="form.type !== 3">
          <IconSelect v-model="form.icon" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recycleVisible" title="菜单回收站" width="980px" :lock-scroll="false">
      <el-table
        :data="recycleList"
        v-loading="recycleLoading"
        border
        stripe
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="菜单名称" width="180" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'primary' : row.type === 2 ? 'success' : 'warning'">
              {{ row.type === 1 ? '目录' : row.type === 2 ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" width="180" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMenuList, createMenu, updateMenu, deleteMenu, updateMenuStatus, getRecycleMenuPage, restoreMenu, deleteMenuPermanent } from '@/api/system/menu'
import IconSelect from '@/components/IconSelect.vue'

const loading = ref(false)
const menuList = ref([])
const menuOptions = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
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
  parentId: null,
  name: '',
  type: 2,
  path: '',
  component: '',
  permission: '',
  sort: 0,
  icon: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

const getList = async () => {
  loading.value = true
  try {
    const res = await getMenuList(queryParams)
    menuList.value = res.data || []
    menuOptions.value = [{ id: 0, name: '根目录', children: menuList.value }]
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

const handleAdd = (row) => {
  resetForm()
  if (row) {
    form.parentId = row.id
  }
  dialogTitle.value = '新增菜单'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  resetForm()
  dialogTitle.value = '编辑菜单'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该菜单吗？', '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  getList()
}

const getRecycleList = async () => {
  recycleLoading.value = true
  try {
    const res = await getRecycleMenuPage(recycleQuery)
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
  await restoreMenu(row.id)
  ElMessage.success('恢复成功')
  await getRecycleList()
  await getList()
}

const handlePermanentDelete = async (row) => {
  await ElMessageBox.confirm('确定彻底删除该菜单吗？该操作不可恢复', '提示', { type: 'warning' })
  await deleteMenuPermanent(row.id)
  ElMessage.success('清除成功')
  await getRecycleList()
}

// 状态切换
const handleStatusChange = async (row) => {
  try {
    const text = row.status === 1 ? '启用' : '禁用'
    await ElMessageBox.confirm(`确认要${text}菜单"${row.name}"吗？`, '提示', { type: 'warning' })
    await updateMenuStatus(row.id, row.status)
    ElMessage.success(`${text}成功`)
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
  }
}

const resetForm = () => {
  form.id = null
  form.parentId = null
  form.name = ''
  form.type = 2
  form.path = ''
  form.component = ''
  form.permission = ''
  form.sort = 0
  form.icon = ''
  form.status = 1
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (form.id) {
        await updateMenu(form)
        ElMessage.success('修改成功')
      } else {
        await createMenu(form)
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
</style>
