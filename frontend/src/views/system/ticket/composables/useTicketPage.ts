import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type TableInstance, type UploadFile } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  createTicket,
  createTicketComment,
  deleteTicket,
  restoreTicket,
  getTicket,
  getTicketAssigneeOptions,
  getTicketAttachments,
  getTicketComments,
  getTicketPage,
  transitionTicket,
  uploadTicketAttachment,
  updateTicket,
  type TicketVO,
  type AssigneeOptionVO,
  type TicketCommentVO,
  type TicketAttachmentVO,
  type TicketSaveDTO,
  type TicketPageQuery,
} from '@/api/system/ticket'
import type { MenuTreeNode } from '@/types/api'
import { DICT_TYPE } from '@/constants/dict'
import { useDict, getDictDefaultValue, preloadDicts } from '@/composables/useDict'
import { showSnackbar } from '@/composables/ui/useSnackbar'

export function useTicketPage() {
  const { options: statusOptions, load: loadStatusOptions } = useDict(DICT_TYPE.TICKET_STATUS, {
    valueType: 'string',
  })

  const loading = ref(false)
  const route = useRoute()
  const userStore = useUserStore()
  const total = ref(0)
  const ticketList = ref<TicketVO[]>([])
  const tableRef = ref<TableInstance | null>(null)
  const userOptions = ref<AssigneeOptionVO[]>([])
  const formVisible = ref(false)
  const detailVisible = ref(false)
  const formRef = ref<FormInstance | null>(null)
  const comments = ref<TicketCommentVO[]>([])
  const attachments = ref<TicketAttachmentVO[]>([])
  const commentText = ref('')
  const currentTicket = ref<Partial<TicketVO>>({})

  const queryParams = reactive<TicketPageQuery>({
    pageNo: 1,
    pageSize: 10,
    title: '',
    status: '',
    priority: '',
  })

  const form = reactive<TicketSaveDTO>({
    id: null,
    title: '',
    description: '',
    priority: 'MEDIUM',
    assigneeUserId: null,
    deadline: null,
  })

  const rules = {
    title: [{ required: true, message: '请输入工单标题', trigger: 'blur' }],
  }

  const hasPermission = (permission: string) => {
    const checkPermissionFromMenus = (menus: MenuTreeNode[] | undefined) => {
      if (!menus || !Array.isArray(menus)) return false
      for (const menu of menus) {
        if (menu.permission === permission) return true
        if (menu.children?.length && checkPermissionFromMenus(menu.children)) return true
      }
      return false
    }
    return checkPermissionFromMenus(userStore.menus)
  }

  const canTransitionRow = (row: TicketVO) => {
    if (hasPermission('system:ticket:transition')) return true
    const currentUserId = userStore.userInfo?.userId
    return !!currentUserId && row?.assigneeUserId === currentUserId
  }

  const transitionOptions = (row: TicketVO) =>
    statusOptions.value.filter((opt) => String(opt.value) !== String(row.status))

  const getList = async () => {
    loading.value = true
    try {
      const res = await getTicketPage(queryParams)
      ticketList.value = res.data.list || []
      total.value = res.data.total || 0
    } finally {
      loading.value = false
    }
  }

  const loadUsers = async () => {
    try {
      const res = await getTicketAssigneeOptions()
      userOptions.value = res.data || []
    } catch {
      userOptions.value = []
    }
  }

  const handleQuery = () => {
    queryParams.pageNo = 1
    getList()
  }

  const resetQuery = () => {
    queryParams.title = ''
    queryParams.status = ''
    queryParams.priority = ''
    handleQuery()
  }

  const resetForm = () => {
    form.id = null
    form.title = ''
    form.description = ''
    form.priority = (getDictDefaultValue(DICT_TYPE.TICKET_PRIORITY) as string) || 'MEDIUM'
    form.assigneeUserId = null
    form.deadline = null
  }

  const handleCreate = async () => {
    resetForm()
    await loadUsers()
    formVisible.value = true
  }

  const openTicketDetailById = async (ticketId: number) => {
    if (!ticketId) return
    const [detailRes, commentRes, attachmentRes] = await Promise.all([
      getTicket(ticketId),
      getTicketComments(ticketId),
      getTicketAttachments(ticketId),
    ])
    currentTicket.value = detailRes.data || {}
    comments.value = commentRes.data || []
    attachments.value = attachmentRes.data || []
    commentText.value = ''
    detailVisible.value = true
  }

  const handleDetail = async (row: TicketVO) => {
    if (row.id == null) return
    await openTicketDetailById(row.id)
  }

  const handleUploadAttachment = async (uploadFile: UploadFile) => {
    if (!currentTicket.value.id || !uploadFile.raw) return
    await uploadTicketAttachment(currentTicket.value.id, uploadFile.raw)
    ElMessage.success('附件上传成功')
    const attachmentRes = await getTicketAttachments(currentTicket.value.id)
    attachments.value = attachmentRes.data || []
  }

  const downloadAttachmentUrl = (id: number) => `/api/system/ticket/attachment/download/${id}`

  const relayoutTable = async () => {
    await nextTick()
    tableRef.value?.doLayout?.()
    setTimeout(() => tableRef.value?.doLayout?.(), 260)
  }

  watch(detailVisible, relayoutTable)
  watch(formVisible, relayoutTable)

  watch(
    () => route.query.ticketId,
    async (ticketId) => {
      const parsedId = Number(ticketId)
      if (parsedId > 0) await openTicketDetailById(parsedId)
    },
    { immediate: true },
  )

  const handleStatusCommand = async (status: string, row: TicketVO) => {
    await transitionTicket({ id: row.id, status })
    ElMessage.success('状态更新成功')
    getList()
    if (detailVisible.value && currentTicket.value.id === row.id) {
      currentTicket.value.status = status
    }
  }

  /** 工单软删除进回收站：直接执行 + Snackbar 撤销恢复 */
  const handleDelete = async (row: TicketVO) => {
    await deleteTicket(row.id)
    const pageNo = queryParams.pageNo ?? 1
    if (pageNo > 1 && ticketList.value.length === 1) {
      queryParams.pageNo = pageNo - 1
    }
    await getList()
    showSnackbar({
      message: `已将工单「${row.ticketNo}」移至回收站`,
      variant: 'info',
      actionLabel: '撤销',
      onAction: async () => {
        await restoreTicket(row.id)
        ElMessage.success(`已恢复工单「${row.ticketNo}」`)
        await getList()
      },
    })
  }

  const submitForm = async () => {
    if (!formRef.value) return
    await formRef.value.validate(async (valid) => {
      if (!valid) return
      if (form.id) {
        await updateTicket(form)
        ElMessage.success('修改成功')
      } else {
        await createTicket(form)
        ElMessage.success('创建成功')
      }
      formVisible.value = false
      getList()
    })
  }

  const submitComment = async () => {
    if (!commentText.value.trim()) {
      ElMessage.warning('请输入评论内容')
      return
    }
    const ticketId = currentTicket.value.id
    if (ticketId == null) return
    await createTicketComment({ ticketId, content: commentText.value })
    ElMessage.success('评论成功')
    commentText.value = ''
    const commentRes = await getTicketComments(ticketId)
    comments.value = commentRes.data || []
  }

  onMounted(async () => {
    getList()
    loadUsers()
    try {
      await preloadDicts([DICT_TYPE.TICKET_STATUS, DICT_TYPE.TICKET_PRIORITY])
    } catch (e) {
      console.error('预加载工单字典失败', e)
    }
    await loadStatusOptions()
  })

  return {
    loading,
    total,
    ticketList,
    tableRef,
    userOptions,
    formVisible,
    detailVisible,
    formRef,
    comments,
    attachments,
    commentText,
    currentTicket,
    queryParams,
    form,
    rules,
    canTransitionRow,
    transitionOptions,
    getList,
    handleQuery,
    resetQuery,
    handleCreate,
    handleDetail,
    handleUploadAttachment,
    downloadAttachmentUrl,
    handleStatusCommand,
    handleDelete,
    submitForm,
    submitComment,
  }
}
