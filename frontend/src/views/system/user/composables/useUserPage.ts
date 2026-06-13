import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import type { ElTree } from 'element-plus'
import {
  getUserPage,
  createUser,
  updateUser,
  deleteUser,
  assignUserRole,
  updateUserStatus,
  resetUserPassword,
  unlockUserLogin,
  getUserRoleIds,
  type UserVO,
  type UserSaveDTO,
  type UserPageQuery,
} from '@/api/system/user'
import { getRoleList, type RoleVO } from '@/api/system/role'
import { getDeptTree, type DeptVO } from '@/api/system/dept'
import { displayOrgTree } from '@/utils/org-tree'
import { buildUnlockLoginConfirm } from '@/utils/login-lock'
import { getPostList, type PostVO } from '@/api/system/post'

export function useUserPage() {
  const route = useRoute()
  const loading = ref(false)
  const total = ref(0)
  const userList = ref<UserVO[]>([])
  const dialogVisible = ref(false)
  const dialogTitle = ref('')
  const roleDialogVisible = ref(false)
  const resetPwdVisible = ref(false)
  const formRef = ref<FormInstance | null>(null)
  const deptTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
  const currentUser = ref<Partial<UserVO>>({})
  const selectedRole = ref<number | undefined>(undefined)
  const roleOptions = ref<RoleVO[]>([])
  const deptOptions = ref<DeptVO[]>([])
  const deptSelectOptions = ref<DeptVO[]>([])
  const postOptions = ref<PostVO[]>([])

  const queryParams = reactive<UserPageQuery>({
    pageNo: 1,
    pageSize: 10,
    username: '',
    mobile: '',
    status: null,
    deptId: null,
  })

  const form = reactive<UserSaveDTO>({
    id: null,
    username: '',
    nickname: '',
    password: '',
    mobile: '',
    email: '',
    deptId: null,
    status: 1,
    roleId: undefined,
    postIds: [],
    remark: '',
  })

  const resetPwdForm = reactive<{ id: number | null; username: string; password: string }>({
    id: null,
    username: '',
    password: '',
  })

  const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
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

  const handleDeptClick = (data: DeptVO | null) => {
    queryParams.deptId = data?.id ?? null
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

  const handleStatusChange = async (row: UserVO) => {
    if (row.status !== 0 && row.status !== 1) return
    try {
      const text = row.status === 1 ? '启用' : '禁用'
      await ElMessageBox.confirm(`确认要${text}用户"${row.username}"吗？`, '提示', { type: 'warning' })
      if (row.id == null || row.status == null) return
      await updateUserStatus(row.id, row.status)
      ElMessage.success(`${text}成功`)
    } catch {
      row.status = row.status === 1 ? 0 : 1
    }
  }

  const handleCommand = (command: string, row: UserVO) => {
    switch (command) {
      case 'resetPwd':
        handleResetPwd(row)
        break
      case 'assignRole':
        handleAssignRole(row)
        break
      case 'unlockLogin':
        handleUnlockLogin(row)
        break
      case 'delete':
        handleDelete(row)
        break
    }
  }

  const handleResetPwd = (row: UserVO) => {
    resetPwdForm.id = row.id
    resetPwdForm.username = row.username
    resetPwdForm.password = ''
    resetPwdVisible.value = true
  }

  const handleUnlockLogin = async (row: UserVO) => {
    try {
      const { title, content } = buildUnlockLoginConfirm(row)
      await ElMessageBox.confirm(content, title, {
        type: 'warning',
        confirmButtonText: '解除锁定',
      })
      await unlockUserLogin(row.id)
      ElMessage.success('已解除登录锁定')
      getList()
    } catch {
      /* 用户取消 */
    }
  }

  const submitResetPwd = async () => {
    if (!resetPwdForm.password) {
      ElMessage.warning('请输入新密码')
      return
    }
    if (resetPwdForm.id == null) return
    try {
      await resetUserPassword(resetPwdForm.id, resetPwdForm.password)
      ElMessage.success('重置密码成功')
      resetPwdVisible.value = false
    } catch (error) {
      console.error(error)
    }
  }

  const ensureRoleOptions = async () => {
    if (roleOptions.value.length === 0) {
      const res = await getRoleList()
      roleOptions.value = res.data || []
    }
  }

  const handleAdd = async () => {
    resetForm()
    dialogTitle.value = '新增用户'
    await ensureRoleOptions()
    dialogVisible.value = true
  }

  const handleEdit = async (row: UserVO) => {
    resetForm()
    dialogTitle.value = '编辑用户'
    Object.assign(form, row)
    await ensureRoleOptions()
    if (row.roleIds && row.roleIds.length > 0) {
      form.roleId = [...row.roleIds][0]
    }
    form.postIds = row.postIds ? [...row.postIds] : []
    dialogVisible.value = true
  }

  const handleDelete = async (row: UserVO) => {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    getList()
  }

  const handleAssignRole = async (row: UserVO) => {
    currentUser.value = row
    const res = await getUserRoleIds(row.id)
    const roleIds = res.data || []
    selectedRole.value = roleIds.length > 0 ? roleIds[0] : undefined
    await ensureRoleOptions()
    roleDialogVisible.value = true
  }

  const submitAssignRole = async () => {
    const userId = currentUser.value.id
    if (userId == null) return
    const roleIds = selectedRole.value != null ? [selectedRole.value] : []
    await assignUserRole({ userId, roleIds })
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
    form.roleId = undefined
    form.postIds = []
    form.remark = ''
  }

  const submitForm = async () => {
    if (!formRef.value) return
    try {
      await formRef.value.validate()
    } catch {
      return
    }
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

  const loadDeptTree = async () => {
    const res = await getDeptTree()
    deptOptions.value = displayOrgTree(res.data || [])
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
    const statusFromRoute = route.query.status
    if (statusFromRoute !== undefined && statusFromRoute !== '') {
      const status = Number(statusFromRoute)
      if (!Number.isNaN(status)) {
        queryParams.status = status
      }
    }
    getList()
  })

  return {
    loading,
    total,
    userList,
    dialogVisible,
    dialogTitle,
    roleDialogVisible,
    resetPwdVisible,
    formRef,
    deptTreeRef,
    currentUser,
    selectedRole,
    roleOptions,
    deptOptions,
    deptSelectOptions,
    postOptions,
    queryParams,
    form,
    resetPwdForm,
    rules,
    getList,
    handleDeptClick,
    handleQuery,
    resetQuery,
    handleStatusChange,
    handleCommand,
    submitResetPwd,
    handleAdd,
    handleEdit,
    submitAssignRole,
    submitForm,
  }
}
