import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type TableInstance } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  approveApproval,
  archiveApproval,
  createApproval,
  deleteApproval,
  restoreApproval,
  getApproval,
  getApprovalApproverOptions,
  getApprovalPage,
  getApprovalRecords,
  type ApprovalApproverOptionVO,
  type ApprovalVO,
  type ApprovalRecordVO,
  type ApprovalCreateDTO,
  type ApprovalPageQuery,
} from '@/api/system/approval'
import { DICT_TYPE } from '@/constants/dict'
import { getDictDefaultValue, preloadDicts } from '@/composables/useDict'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
import { showSnackbar } from '@/composables/ui/useSnackbar'

interface RegisterDetailParsed {
  userId?: number
  username?: string
  nickname?: string
  mobile?: string
}

const ACTION_LABELS = {
  SUBMIT: '提交审批',
  APPROVE: '审批通过',
  REJECT: '审批驳回',
  ARCHIVE: '归档单据',
} as const

export function useApprovalPage() {
  const userStore = useUserStore()
  const route = useRoute()
  const loading = ref(false)
  const tableRef = ref<TableInstance | null>(null)
  const total = ref(0)
  const list = ref<ApprovalVO[]>([])
  const userOptions = ref<ApprovalApproverOptionVO[]>([])
  const formVisible = ref(false)
  const approveVisible = ref(false)
  const detailVisible = ref(false)
  const formRef = ref<FormInstance | null>(null)
  const current = ref<Partial<ApprovalVO>>({})
  const records = ref<ApprovalRecordVO[]>([])
  const approveAction = ref('APPROVE')
  const approveRemark = ref('')
  const approveTargetId = ref<number | null>(null)
  const registerDetail = ref<RegisterDetailParsed>({})

  const queryParams = reactive<ApprovalPageQuery>({
    pageNo: 1,
    pageSize: 10,
    title: '',
    formType: '',
    status: '',
  })

  const form = reactive<ApprovalCreateDTO>({
    formType: 'GENERAL',
    title: '',
    approverUserId: null,
    content: '',
  })

  const rules = {
    formType: [{ required: true, message: '请选择审批类型', trigger: 'change' }],
    title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
    approverUserId: [{ required: true, message: '请选择审批人', trigger: 'change' }],
    content: [{ required: true, message: '请输入审批内容', trigger: 'blur' }],
  }

  const parseRegisterContent = (content: string | undefined): RegisterDetailParsed => {
    if (!content) return {}
    try {
      const obj = JSON.parse(content) as RegisterDetailParsed
      return {
        userId: obj.userId,
        username: obj.username,
        nickname: obj.nickname,
        mobile: obj.mobile,
      }
    } catch {
      return {}
    }
  }

  const hasPerm = (perm: string) => (userStore.userInfo?.permissions || []).includes(perm)

  const actionText = (action: string | undefined) => {
    if (action && action in ACTION_LABELS) {
      return ACTION_LABELS[action as keyof typeof ACTION_LABELS]
    }
    return action || '-'
  }

  const canDeleteRow = (row: ApprovalVO) => row.formType !== 'REGISTER'

  const getList = async () => {
    loading.value = true
    try {
      const res = await getApprovalPage(queryParams)
      list.value = res.data.list || []
      total.value = res.data.total || 0
    } finally {
      loading.value = false
    }
  }

  const loadUsers = async () => {
    const res = await getApprovalApproverOptions()
    userOptions.value = res.data || []
  }

  const handleQuery = () => {
    queryParams.pageNo = 1
    getList()
  }

  const resetQuery = () => {
    queryParams.title = ''
    queryParams.formType = ''
    queryParams.status = ''
    handleQuery()
  }

  const applyRouteQuery = () => {
    const { formType, status, title } = route.query
    if (typeof formType === 'string' && formType) queryParams.formType = formType
    if (typeof status === 'string' && status) queryParams.status = status
    if (typeof title === 'string') queryParams.title = title
  }

  const handleCreate = () => {
    form.formType = (getDictDefaultValue(DICT_TYPE.APPROVAL_FORM_TYPE) as string) || 'GENERAL'
    form.title = ''
    form.approverUserId = null
    form.content = ''
    formVisible.value = true
  }

  const submitForm = async () => {
    if (!formRef.value) return
    await formRef.value.validate(async (valid) => {
      if (!valid) return
      await createApproval(form)
      ElMessage.success('提交成功')
      formVisible.value = false
      getList()
    })
  }

  const canApproveRow = (row: ApprovalVO) => {
    if (row.status !== 'SUBMITTED') return false
    if (row.formType === 'REGISTER') return hasPerm('system:approval:approve')
    return row.approverUserId === userStore.userInfo?.userId
  }

  const canArchiveRow = (row: ApprovalVO) => {
    if (row.status !== 'APPROVED' && row.status !== 'REJECTED') return false
    if (row.formType === 'REGISTER') return hasPerm('system:approval:archive')
    return row.applicantUserId === userStore.userInfo?.userId
  }

  const openApproveDialog = (row: ApprovalVO, action: string) => {
    approveTargetId.value = row.id
    approveAction.value = action
    approveRemark.value = ''
    approveVisible.value = true
  }

  const submitApprove = async () => {
    if (!approveTargetId.value) return
    const targetId = approveTargetId.value
    await approveApproval({
      id: targetId,
      action: approveAction.value,
      remark: approveRemark.value,
    })
    ElMessage.success('审批完成')
    approveVisible.value = false
    await getList()
    if (detailVisible.value && current.value.id === targetId) {
      await openDetail(targetId)
    }
  }

  const handleArchive = async (row: ApprovalVO) => {
    await openAlertDialog({
      title: '归档审批单',
      targetName: row.formNo,
      message: '归档后该审批单将锁定为已归档状态，不可继续流转。',
      severity: 'warning',
      confirmText: '确认归档',
      onConfirm: async () => {
        await archiveApproval({ id: row.id, remark: '归档' })
        ElMessage.success('归档成功')
        await getList()
        if (detailVisible.value && current.value.id === row.id) {
          await openDetail(row.id)
        }
      },
    })
  }

  /** 审批单软删除进回收站：直接执行 + Snackbar 一键撤销 */
  const handleDelete = async (row: ApprovalVO) => {
    await deleteApproval(row.id)
    const pageNo = queryParams.pageNo ?? 1
    if (pageNo > 1 && list.value.length === 1) {
      queryParams.pageNo = pageNo - 1
    }
    await getList()
    showSnackbar({
      message: `已将审批单「${row.formNo}」移至回收站`,
      variant: 'info',
      actionLabel: '撤销',
      onAction: async () => {
        await restoreApproval(row.id)
        ElMessage.success(`已恢复审批单「${row.formNo}」`)
        await getList()
      },
    })
  }

  const openDetail = async (id: number) => {
    const [detailRes, recordRes] = await Promise.all([getApproval(id), getApprovalRecords(id)])
    current.value = detailRes.data || {}
    registerDetail.value =
      current.value.formType === 'REGISTER'
        ? parseRegisterContent(
            typeof current.value.content === 'string' ? current.value.content : undefined,
          )
        : {}
    records.value = recordRes.data || []
    detailVisible.value = true
  }

  onMounted(() => {
    preloadDicts([DICT_TYPE.APPROVAL_FORM_TYPE, DICT_TYPE.APPROVAL_STATUS])
    applyRouteQuery()
    getList()
    loadUsers()
  })

  watch(
    () => [route.query.formType, route.query.status, route.query.title],
    () => {
      applyRouteQuery()
      queryParams.pageNo = 1
      getList()
    },
  )

  watch(
    () => route.query.approvalId,
    async (approvalId) => {
      const parsedId = Number(approvalId)
      if (parsedId > 0) await openDetail(parsedId)
    },
    { immediate: true },
  )

  const relayoutTable = async () => {
    await nextTick()
    tableRef.value?.doLayout?.()
    setTimeout(() => tableRef.value?.doLayout?.(), 260)
  }

  watch(formVisible, relayoutTable)
  watch(approveVisible, relayoutTable)
  watch(detailVisible, relayoutTable)

  return {
    loading,
    tableRef,
    total,
    list,
    userOptions,
    formVisible,
    approveVisible,
    detailVisible,
    formRef,
    current,
    records,
    approveAction,
    approveRemark,
    registerDetail,
    queryParams,
    form,
    rules,
    actionText,
    canDeleteRow,
    getList,
    handleQuery,
    resetQuery,
    handleCreate,
    submitForm,
    canApproveRow,
    canArchiveRow,
    openApproveDialog,
    submitApprove,
    handleArchive,
    handleDelete,
    openDetail,
  }
}
