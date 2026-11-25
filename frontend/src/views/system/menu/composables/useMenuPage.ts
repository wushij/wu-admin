import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, type FormInstance, type TableInstance } from 'element-plus'
import {
  getMenuList,
  createMenu,
  updateMenu,
  deleteMenu,
  restoreMenu,
  updateMenuStatus,
  type MenuVO,
  type MenuSaveDTO,
  type MenuListQuery,
} from '@/api/system/menu'
import {
  applyMenuAdminDisplayTree,
  buildParentMenuOptions,
  countMenuTypes,
  flattenMenuTree,
  isExternalMenuComponent,
} from '@/utils/menu-tree'
import { useUserStore } from '@/store/user'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
import { showSnackbar } from '@/composables/ui/useSnackbar'

export function useMenuPage() {
  const userStore = useUserStore()
  const loading = ref(false)
  const submitLoading = ref(false)
  const menuList = ref<MenuVO[]>([])
  const parentOptions = ref<MenuVO[]>([])
  const dialogVisible = ref(false)
  const dialogTitle = ref('')
  const formRef = ref<FormInstance | null>(null)
  const tableRef = ref<TableInstance | null>(null)
  const expandAll = ref(false)
  const tableKey = ref(0)

  const queryParams = reactive<MenuListQuery>({
    name: '',
    status: null,
    type: null,
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
      menuList.value = applyMenuAdminDisplayTree(res.data || [])
      parentOptions.value = buildParentMenuOptions(menuList.value)
      tableKey.value += 1
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

  /** 菜单软删除进回收站：提供 Snackbar 一键撤销 */
  const handleDelete = async (row: MenuVO) => {
    await deleteMenu(row.id)
    await getList()
    showSnackbar({
      message: `已将菜单「${row.name}」移至回收站`,
      variant: 'info',
      actionLabel: '撤销',
      onAction: async () => {
        await restoreMenu(row.id)
        ElMessage.success(`已恢复菜单「${row.name}」`)
        await getList()
      },
    })
  }

  const handleStatusChange = async (row: MenuVO) => {
    const nextStatus = row.status
    const prevStatus = nextStatus === 1 ? 0 : 1
    const text = nextStatus === 1 ? '启用' : '禁用'
    if (nextStatus === 0) {
      const ok = await openAlertDialog({
        title: '禁用菜单项',
        targetName: row.name,
        message: '禁用后该菜单及其路由权限将被收敛，相关用户将无法访问对应功能。',
        severity: 'warning',
        confirmText: '确认禁用',
        onConfirm: async () => {
          if (row.id == null || nextStatus == null) return
          await updateMenuStatus(row.id, nextStatus)
          await getList()
          await userStore.refreshUserStore().catch(() => {})
        },
      })
      if (!ok) row.status = prevStatus
      else ElMessage.success(`${text}成功`)
      return
    }
    try {
      if (row.id == null || nextStatus == null) return
      await updateMenuStatus(row.id, nextStatus)
      await getList()
      await userStore.refreshUserStore().catch(() => {})
      showSnackbar({
        message: `已启用菜单「${row.name}」`,
        variant: 'success',
        actionLabel: '撤销',
        onAction: async () => {
          await updateMenuStatus(row.id, prevStatus)
          await getList()
          await userStore.refreshUserStore().catch(() => {})
        },
      })
    } catch {
      row.status = prevStatus
    }
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
        await userStore.refreshUserStore().catch(() => {})
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
    tableKey,
    queryParams,
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
    submitForm,
  }
}
