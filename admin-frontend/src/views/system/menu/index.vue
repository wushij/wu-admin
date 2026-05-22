<template>
  <div class="app-container menu-page">
    <el-card class="menu-card" shadow="never">
      <!-- 搜索 -->
      <el-form :model="queryParams" inline class="search-form">
        <el-form-item label="菜单名称">
          <el-input
            v-model="queryParams.name"
            placeholder="请输入菜单名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-select v-model="queryParams.type" placeholder="全部类型" clearable style="width: 130px">
            <el-option label="目录" :value="1" />
            <el-option label="菜单" :value="2" />
            <el-option label="按钮" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 统计 -->
      <div class="menu-stats">
        <span class="stat-item"><el-tag type="info" size="small">目录</el-tag> {{ menuStats.dir }}</span>
        <span class="stat-item"><el-tag type="success" size="small">菜单</el-tag> {{ menuStats.menu }}</span>
        <span class="stat-item"><el-tag type="warning" size="small">按钮</el-tag> {{ menuStats.button }}</span>
        <span class="stat-hint">共 {{ menuStats.total }} 项 · 改菜单后需重新登录侧栏才会更新</span>
      </div>

      <!-- 工具栏 -->
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button v-permission="'system:menu:create'" type="primary" :icon="Plus" @click="handleAdd()">
            新增菜单
          </el-button>
          <el-button :icon="Sort" @click="toggleExpandAll">{{ expandAll ? '全部折叠' : '全部展开' }}</el-button>
        </div>
        <div class="toolbar-right">
          <el-button v-permission="'system:menu:delete'" @click="openRecycleDialog">回收站</el-button>
        </div>
      </div>

      <!-- 树表 -->
      <el-table
        ref="tableRef"
        :data="menuList"
        v-loading="loading"
        row-key="id"
        border
        stripe
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children' }"
        class="menu-tree-table"
      >
        <el-table-column prop="name" label="菜单名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.type]?.tag" size="small" effect="light">
              {{ typeTagMap[row.type]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图标" width="150" align="center">
          <template #default="{ row }">
            <span v-if="row.type === 3 || !row.icon" class="text-muted">-</span>
            <span v-else class="icon-cell">
              <el-icon :size="18"><component :is="resolveMenuIcon(row.icon)" /></el-icon>
              <span class="icon-name">{{ row.icon }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由地址" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <template v-if="row.type !== 3">
              <el-tag v-if="isExternalRow(row)" type="warning" size="small" class="ext-tag">外链</el-tag>
              {{ row.path || '-' }}
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="component" label="组件/外链" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.component || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column prop="status" label="状态" width="88" align="center">
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
                v-if="row.type !== 3"
                v-permission="'system:menu:create'"
                type="primary"
                size="small"
                @click="handleAdd(row)"
              >
                新增
              </el-button>
              <el-button
                v-permission="'system:menu:update'"
                type="primary"
                size="small"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'system:menu:delete'"
                type="danger"
                size="small"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="680px"
      :lock-scroll="false"
      destroy-on-close
      class="menu-form-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择上级菜单"
            check-strictly
            default-expand-all
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="form.type" @change="onTypeChange">
            <el-radio :label="1">目录</el-radio>
            <el-radio :label="2">菜单</el-radio>
            <el-radio :label="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入菜单名称" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item v-if="form.type !== 3" label="是否外链">
          <el-switch v-model="form.isFrame" :active-value="1" :inactive-value="0" />
          <span class="form-tip">开启后组件路径填写完整 URL，在新窗口打开</span>
        </el-form-item>

        <el-form-item v-if="form.type !== 3 && !form.isFrame" label="路由地址" prop="path">
          <el-input v-model="form.path" placeholder="如 /system/user" />
        </el-form-item>

        <el-form-item
          v-if="form.type !== 3 && form.isFrame"
          label="外链地址"
          prop="component"
        >
          <el-input v-model="form.component" placeholder="https://example.com" />
        </el-form-item>

        <el-form-item
          v-if="form.type === 2 && !form.isFrame"
          label="组件路径"
          prop="component"
        >
          <el-input v-model="form.component" placeholder="如 system/user/index">
            <template #append>
              <el-dropdown trigger="click" @command="(c) => (form.component = c)">
                <el-button>常用</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="item in componentPresets"
                      :key="item"
                      :command="item"
                    >
                      {{ item }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-input>
          <div class="form-tip">对应 views 下路径，不含 .vue</div>
        </el-form-item>

        <el-form-item v-if="form.type === 3" label="权限标识" prop="permission">
          <el-input
            v-model="form.permission"
            placeholder="如 system:user:create"
          >
            <template #append>
              <el-button @click="fillPermissionPrefix">按上级生成</el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="form.type === 2" label="列表权限" prop="permission">
          <el-input v-model="form.permission" placeholder="侧栏与路由权限，如 system:user:list" />
        </el-form-item>

        <el-form-item v-if="form.type !== 3" label="图标" prop="icon">
          <IconSelect v-model="form.icon" />
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" controls-position="right" style="width: 100%" />
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
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 回收站 -->
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
            <el-tag :type="typeTagMap[row.type]?.tag" size="small">
              {{ typeTagMap[row.type]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由" width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="删除时间" width="180" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="handleRestore(row)">恢复</el-button>
            <el-button size="small" type="danger" @click="handlePermanentDelete(row)">清除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="recycleQuery.pageNo"
        v-model:page-size="recycleQuery.pageSize"
        :total="recycleTotal"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="recycle-pagination"
        @size-change="getRecycleList"
        @current-change="getRecycleList"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Sort } from '@element-plus/icons-vue'
import {
  getMenuList,
  createMenu,
  updateMenu,
  deleteMenu,
  updateMenuStatus,
  getRecycleMenuPage,
  restoreMenu,
  deleteMenuPermanent
} from '@/api/system/menu'
import IconSelect from '@/components/IconSelect.vue'
import { resolveMenuIcon } from '@/utils/menu-icon'
import {
  buildParentMenuOptions,
  countMenuTypes,
  flattenMenuTree,
  isExternalMenuComponent
} from '@/utils/menu-tree'

const typeTagMap = {
  1: { label: '目录', tag: 'info' },
  2: { label: '菜单', tag: 'success' },
  3: { label: '按钮', tag: 'warning' }
}

const componentPresets = [
  'system/user/index',
  'system/role/index',
  'system/menu/index',
  'system/org/index',
  'system/dict/index',
  'system/oper-log/index',
  'system/login-log/index',
  'system/file/index'
]

const loading = ref(false)
const submitLoading = ref(false)
const menuList = ref([])
const parentOptions = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const tableRef = ref(null)
const expandAll = ref(false)

const recycleVisible = ref(false)
const recycleLoading = ref(false)
const recycleList = ref([])
const recycleTotal = ref(0)

const queryParams = reactive({
  name: '',
  status: null,
  type: null
})
const recycleQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  name: '',
  status: null
})

const form = reactive({
  id: null,
  parentId: 0,
  name: '',
  type: 1,
  path: '',
  component: '',
  permission: '',
  sort: 0,
  icon: '',
  status: 1,
  isFrame: 0
})

const rules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

const menuStats = computed(() => countMenuTypes(menuList.value))

const isExternalRow = (row) => isExternalMenuComponent(row.component)

const getList = async () => {
  loading.value = true
  try {
    const res = await getMenuList(queryParams)
    menuList.value = res.data || []
    parentOptions.value = buildParentMenuOptions(menuList.value)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => getList()

const resetQuery = () => {
  queryParams.name = ''
  queryParams.status = null
  queryParams.type = null
  handleQuery()
}

const toggleExpandAll = async () => {
  expandAll.value = !expandAll.value
  await nextTick()
  const flat = flattenMenuTree(menuList.value)
  flat.forEach((row) => {
    tableRef.value?.toggleRowExpansion(row, expandAll.value)
  })
}

const handleAdd = (row) => {
  resetForm()
  if (row) {
    form.parentId = row.id
    if (row.type === 1) {
      form.type = 2
    } else if (row.type === 2) {
      form.type = 3
    }
  }
  dialogTitle.value = row ? `新增子项 · ${row.name}` : '新增菜单'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  resetForm()
  dialogTitle.value = '编辑菜单'
  Object.assign(form, {
    ...row,
    parentId: row.parentId ?? 0,
    isFrame: isExternalMenuComponent(row.component) ? 1 : 0
  })
  dialogVisible.value = true
}

const onTypeChange = () => {
  if (form.type === 3) {
    form.icon = ''
    form.path = ''
    form.isFrame = 0
    if (!form.component) form.component = ''
  }
}

const fillPermissionPrefix = () => {
  const flat = flattenMenuTree(menuList.value)
  const parent = flat.find((m) => m.id === form.parentId)
  if (parent?.permission) {
    const base = parent.permission.replace(/:list$/, '')
    form.permission = `${base}:`
  } else {
    ElMessage.warning('上级菜单无权限标识，请手动填写')
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除菜单「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  getList()
}

const handleStatusChange = async (row) => {
  const text = row.status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认要${text}菜单「${row.name}」吗？`, '提示', { type: 'warning' })
    await updateMenuStatus(row.id, row.status)
    ElMessage.success(`${text}成功`)
  } catch {
    row.status = row.status === 1 ? 0 : 1
  }
}

const openRecycleDialog = async () => {
  recycleQuery.pageNo = 1
  recycleVisible.value = true
  await getRecycleList()
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

const handleRestore = async (row) => {
  await restoreMenu(row.id)
  ElMessage.success('恢复成功')
  await getRecycleList()
  await getList()
}

const handlePermanentDelete = async (row) => {
  await ElMessageBox.confirm('彻底删除后不可恢复，确定继续？', '提示', { type: 'warning' })
  await deleteMenuPermanent(row.id)
  ElMessage.success('已清除')
  await getRecycleList()
}

const resetForm = () => {
  form.id = null
  form.parentId = 0
  form.name = ''
  form.type = 1
  form.path = ''
  form.component = ''
  form.permission = ''
  form.sort = 0
  form.icon = ''
  form.status = 1
  form.isFrame = 0
}

const buildSubmitPayload = () => {
  const payload = {
    id: form.id,
    parentId: form.parentId === 0 ? 0 : form.parentId,
    name: form.name,
    type: form.type,
    sort: form.sort,
    status: form.status,
    icon: form.type === 3 ? '' : form.icon,
    path: form.type === 3 ? '' : form.path,
    component: form.type === 3 ? '' : form.component,
    permission: form.permission || ''
  }
  if (form.type === 3) {
    payload.path = ''
    payload.component = ''
    payload.icon = ''
  } else if (form.isFrame) {
    payload.path = payload.path || payload.component
  } else if (form.type === 1) {
    payload.component = ''
  }
  return payload
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload = buildSubmitPayload()
      if (form.id) {
        await updateMenu(payload)
        ElMessage.success('修改成功')
      } else {
        await createMenu(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      getList()
    } finally {
      submitLoading.value = false
    }
  })
}

onMounted(() => getList())
</script>

<style scoped>
.menu-page {
  padding: 0;
}

.menu-card {
  border-radius: 8px;
}

.menu-card :deep(.el-card__body) {
  padding: 16px 20px 20px;
}

.search-form {
  margin-bottom: 4px;
}

.menu-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  padding: 10px 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  font-size: 13px;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.stat-hint {
  color: var(--el-text-color-secondary);
  margin-left: auto;
  font-size: 12px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-tree-table {
  width: 100%;
}

.icon-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.icon-name {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.text-muted {
  color: var(--el-text-color-placeholder);
}

.ext-tag {
  margin-right: 4px;
  vertical-align: middle;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  margin: 0;
}

.recycle-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .stat-hint {
    margin-left: 0;
    width: 100%;
  }
}
</style>
