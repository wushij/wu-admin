import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type TableInstance } from 'element-plus'
import {
  getMenuList,
  createMenu,
  updateMenu,
  deleteMenu,
  updateMenuStatus,
  getRecycleMenuPage,
  restoreMenu,
  deleteMenuPermanent,
  type MenuVO,
  type MenuSaveDTO,
  type MenuListQuery,
  type MenuRecycleQuery,
} from '@/api/system/menu'
import {
  buildParentMenuOptions,
  countMenuTypes,
  flattenMenuTree,
  isExternalMenuComponent,
} from '@/utils/menu-tree'

export function useMenuPage() {
  const loading = ref(false)
  const submitLoading = ref(false)
  const menuList = ref<MenuVO[]>([])
  const parentOptions = ref<MenuVO[]>([])
  const dialogVisible = ref(false)
  const dialogTitle = ref('')
  const formRef = ref<FormInstance | null>(null)
  const tableRef = ref<TableInstance | null>(null)
  const expandAll = ref(false)

  const recycleVisible = ref(false)
  const recycleLoading = ref(false)
  const recycleList = ref<MenuVO[]>([])
  const recycleTotal = ref(0)

  const queryParams = reactive<MenuListQuery>({
    name: '',
    status: null,
    type: null,
  })
  const recycleQuery = reactive<MenuRecycleQuery>({
    pageNo: 1,
    pageSize: 10,
    name: '',
    status: null,
  })

  const form = reactive<MenuSaveDTO & { isFrame: number }>({
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
    isFrame: 0,
  })

  const rules = {
    name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
    type: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  }

  const menuStats = computed(() => countMenuTypes(menuList.value))
  const isExternalRow = (row: MenuVO) => isExternalMenuComponent(row.component)

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

  const handleAdd = (row?: MenuVO) => {
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

  const handleEdit = (row: MenuVO) => {
    resetForm()
    dialogTitle.value = '编辑菜单'
    Object.assign(form, {
      ...row,
      parentId: row.parentId ?? 0,
      isFrame: isExternalMenuComponent(row.component) ? 1 : 0,
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
      const base = String(parent.permission).replace(/:list$/, '')
      form.permission = `${base}:`
    } else {
      ElMessage.warning('上级菜单无权限标识，请手动填写')
    }
  }

  const handleDelete = async (row: MenuVO) => {
    await ElMessageBox.confirm(`确定删除菜单「${row.name}」吗？`, '提示', { type: 'warning' })
    await deleteMenu(row.id)
    ElMessage.success('删除成功')
    getList()
  }

  const handleStatusChange = async (row: MenuVO) => {
    const text = row.status === 1 ? '启用' : '禁用'
    try {
      await ElMessageBox.confirm(`确认要${text}菜单「${row.name}」吗？`, '提示', { type: 'warning' })
      if (row.id == null || row.status == null) return
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

  const handleRestore = async (row: MenuVO) => {
    await restoreMenu(row.id)
    ElMessage.success('恢复成功')
    await getRecycleList()
    await getList()
  }

  const handlePermanentDelete = async (row: MenuVO) => {
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

  const buildSubmitPayload = (): MenuSaveDTO => {
    const payload: MenuSaveDTO = {
      id: form.id,
      parentId: form.parentId === 0 ? 0 : form.parentId,
      name: form.name,
      type: form.type,
      sort: form.sort,
      status: form.status,
      icon: form.type === 3 ? '' : form.icon,
      path: form.type === 3 ? '' : form.path,
      component: form.type === 3 ? '' : form.component,
      permission: form.permission || '',
      isFrame: form.isFrame,
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

  return {
    loading,
    submitLoading,
    menuList,
    parentOptions,
    dialogVisible,
    dialogTitle,
    formRef,
    tableRef,
    expandAll,
    recycleVisible,
    recycleLoading,
    recycleList,
    recycleTotal,
    queryParams,
    recycleQuery,
    form,
    rules,
    menuStats,
    isExternalRow,
    handleQuery,
    resetQuery,
    toggleExpandAll,
    handleAdd,
    handleEdit,
    onTypeChange,
    fillPermissionPrefix,
    handleDelete,
    handleStatusChange,
    openRecycleDialog,
    getRecycleList,
    handleRestore,
    handlePermanentDelete,
    submitForm,
  }
}
