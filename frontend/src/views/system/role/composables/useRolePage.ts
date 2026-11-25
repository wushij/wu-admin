import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import type { ElTree } from 'element-plus'
import {
  getRoleList,
  createRole,
  updateRole,
  deleteRole,
  restoreRole,
  assignRoleMenu,
  updateRoleStatus,
  type RoleSaveDTO,
  type RoleListQuery,
  getRoleMenuIds,
  type RoleVO,
} from '@/api/system/role'
import { getMenuList, type MenuVO } from '@/api/system/menu'
import { getDeptTree, type DeptVO } from '@/api/system/dept'
import { buildMenuTree } from '@/utils/menu-tree'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
import { showSnackbar } from '@/composables/ui/useSnackbar'

export const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '自定义部门' },
  { value: 3, label: '本部门' },
  { value: 4, label: '本部门及以下' },
  { value: 5, label: '仅本人' },
]

export function useRolePage() {
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
  const deptOptions = ref<DeptVO[]>([])
  const selectedDeptIds = ref<number[]>([])

  const queryParams = reactive<RoleListQuery>({
    name: '',
    status: null,
  })

  const form = reactive<RoleSaveDTO>({
    id: null,
    name: '',
    code: '',
    sort: 0,
    status: 1,
    remark: '',
    dataScope: 1,
    dataScopeDeptIds: '',
  })

  const rules = {
    name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
    code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  }

  const dataScopeLabel = (scope?: number) =>
    DATA_SCOPE_OPTIONS.find((item) => item.value === scope)?.label || '全部数据'

  const dataScopeTagType = (scope?: number) => {
    if (scope === 5) return 'info'
    if (scope === 4 || scope === 3) return 'warning'
    if (scope === 2) return 'success'
    return 'primary'
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

  const handleQuery = () => getList()

  const resetQuery = () => {
    queryParams.name = ''
    queryParams.status = null
    handleQuery()
  }

  const loadDeptOptions = async () => {
    if (deptOptions.value.length > 0) return
    try {
      const res = await getDeptTree()
      deptOptions.value = res.data || []
    } catch (error) {
      console.error(error)
    }
  }

  const resetForm = () => {
    form.id = null
    form.name = ''
    form.code = ''
    form.sort = 0
    form.status = 1
    form.remark = ''
    form.dataScope = 1
    form.dataScopeDeptIds = ''
    selectedDeptIds.value = []
  }

  const handleAdd = () => {
    resetForm()
    loadDeptOptions()
    dialogTitle.value = '新增角色'
    dialogVisible.value = true
  }

  const handleEdit = (row: RoleVO) => {
    resetForm()
    loadDeptOptions()
    dialogTitle.value = '编辑角色'
    Object.assign(form, row)
    form.dataScope = row.dataScope ?? 1
    form.dataScopeDeptIds = row.dataScopeDeptIds || ''
    selectedDeptIds.value = (row.dataScopeDeptIds || '')
      .split(',')
      .map((id) => Number(id.trim()))
      .filter((id) => Number.isFinite(id) && id > 0)
    dialogVisible.value = true
  }

  /** 角色软删除进回收站：直接执行 + Snackbar 撤销恢复 */
  const handleDelete = async (row: RoleVO) => {
    await deleteRole(row.id)
    getList()
    showSnackbar({
      message: `已将角色「${row.name}」移至回收站`,
      variant: 'info',
      actionLabel: '撤销',
      onAction: async () => {
        await restoreRole(row.id)
        ElMessage.success(`已恢复角色「${row.name}」`)
        getList()
      },
    })
  }

  const handleStatusChange = async (row: RoleVO) => {
    const nextStatus = row.status
    const prevStatus = nextStatus === 1 ? 0 : 1
    if (nextStatus === 0) {
      const ok = await openAlertDialog({
        title: '禁用角色',
        targetName: row.name,
        message: '禁用后，持有该角色的用户将失去其关联的功能与数据访问权限。',
        severity: 'warning',
        confirmText: '确认禁用',
        onConfirm: async () => {
          if (row.id == null || nextStatus == null) return
          await updateRoleStatus(row.id, nextStatus)
        },
      })
      if (!ok) row.status = prevStatus
      else ElMessage.success('禁用成功')
      return
    }
    try {
      if (row.id == null || nextStatus == null) return
      await updateRoleStatus(row.id, nextStatus)
      showSnackbar({
        message: `已启用角色「${row.name}」`,
        variant: 'success',
        actionLabel: '撤销',
        onAction: async () => {
          await updateRoleStatus(row.id, prevStatus)
          row.status = prevStatus
        },
      })
    } catch {
      row.status = prevStatus
    }
  }

  const handleCommand = (command: string, row: RoleVO) => {
    if (command === 'assignMenu') {
      handleAssignMenu(row)
    } else if (command === 'delete') {
      handleDelete(row)
    }
  }

  const handleAssignMenu = async (row: RoleVO) => {
    currentRole.value = row
    if (menuOptions.value.length === 0) {
      const menuRes = await getMenuList()
      const data = menuRes.data || []
      menuOptions.value =
        data[0]?.children !== undefined || data.length === 0 ? data : buildMenuTree(data)
    }
    const res = await getRoleMenuIds(row.id)
    selectedMenus.value = (res.data || []).map((id) => Number(id))
    menuDialogVisible.value = true
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

  const submitForm = async () => {
    if (!formRef.value) return
    await formRef.value.validate(async (valid) => {
      if (!valid) return
      if (form.dataScope === 2 && selectedDeptIds.value.length === 0) {
        ElMessage.warning('请选择自定义数据范围的部门')
        return
      }
      const payload: RoleSaveDTO = {
        ...form,
        dataScopeDeptIds: form.dataScope === 2 ? selectedDeptIds.value.join(',') : '',
      }
      if (form.id) {
        await updateRole(payload)
        ElMessage.success('修改成功')
      } else {
        await createRole(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      getList()
    })
  }

  onMounted(() => getList())

  return {
    loading,
    roleList,
    dialogVisible,
    dialogTitle,
    menuDialogVisible,
    formRef,
    menuTreeRef,
    currentRole,
    menuOptions,
    deptOptions,
    selectedDeptIds,
    queryParams,
    form,
    rules,
    dataScopeLabel,
    dataScopeTagType,
    getList,
    handleQuery,
    resetQuery,
    handleAdd,
    handleEdit,
    handleStatusChange,
    handleCommand,
    submitAssignMenu,
    submitForm,
  }
}
