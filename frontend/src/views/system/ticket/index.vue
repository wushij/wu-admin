<template>
  <div class="app-container">
    <el-card class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="标题">
          <el-input v-model="queryParams.title" placeholder="请输入工单标题" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect
            v-model="queryParams.status"
            :dict-type="DICT_TYPE.TICKET_STATUS"
            value-type="string"
            placeholder="请选择状态"
            width="160px"
          />
        </el-form-item>
        <el-form-item label="优先级">
          <DictSelect
            v-model="queryParams.priority"
            :dict-type="DICT_TYPE.TICKET_PRIORITY"
            value-type="string"
            placeholder="请选择优先级"
            width="160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>工单列表</span>
          <div class="header-actions">
            <el-button v-permission="'system:ticket:delete'" @click="openRecycleDialog">回收站</el-button>
            <el-button type="primary" v-permission="'system:ticket:create'" @click="handleCreate">新建工单</el-button>
          </div>
        </div>
      </template>
      <el-table
        ref="tableRef"
        :data="ticketList"
        border
        stripe
        v-loading="loading"
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="ticketNo" label="编号" width="190" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <DictTag :value="row.priority" :dict-type="DICT_TYPE.TICKET_PRIORITY" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <DictTag :value="row.status" :dict-type="DICT_TYPE.TICKET_STATUS" />
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="120" />
        <el-table-column prop="assigneeName" label="处理人" width="120" />
        <el-table-column prop="deadline" label="截止时间" width="180" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right" align="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button size="small" @click="handleDetail(row)">详情</el-button>
              <el-dropdown
                v-if="canTransitionRow(row)"
                trigger="click"
                teleported
                @command="(command) => handleStatusCommand(command, row)"
              >
                <el-button size="small" type="primary">流转状态</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="opt in transitionOptions(row)"
                      :key="String(opt.value)"
                      :command="opt.value"
                    >{{ opt.label }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button v-permission="'system:ticket:delete'" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="getList"
        @current-change="getList"
      />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑工单' : '新建工单'" width="680px" :lock-scroll="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入工单标题" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入工单描述" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <DictSelect
            v-model="form.priority"
            :dict-type="DICT_TYPE.TICKET_PRIORITY"
            value-type="string"
            apply-default
            :clearable="false"
          />
        </el-form-item>
        <el-form-item label="处理人" prop="assigneeUserId">
          <el-select v-model="form.assigneeUserId" clearable filterable style="width: 100%">
            <el-option label="全部人员（全员通知）" :value="0" />
            <el-option v-for="u in userOptions" :key="u.id" :label="`${u.username}(${u.nickname})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recycleVisible" title="工单回收站" width="920px" :lock-scroll="false">
      <el-table :data="recycleList" border stripe v-loading="recycleLoading" :header-cell-style="{ textAlign: 'center' }" :cell-style="{ textAlign: 'center' }">
        <el-table-column prop="ticketNo" label="编号" width="190" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <DictTag :value="row.priority" :dict-type="DICT_TYPE.TICKET_PRIORITY" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <DictTag :value="row.status" :dict-type="DICT_TYPE.TICKET_STATUS" />
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="110" />
        <el-table-column prop="assigneeName" label="处理人" width="110" />
        <el-table-column prop="updateTime" label="删除时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button size="small" type="success" @click="handleRestore(row)">恢复</el-button>
              <el-button size="small" type="danger" @click="handleDeletePermanent(row)">彻底删除</el-button>
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

    <el-drawer v-model="detailVisible" title="工单详情" size="45%" :lock-scroll="false">
      <el-descriptions :column="1" border v-if="currentTicket.id">
        <el-descriptions-item label="编号">{{ currentTicket.ticketNo }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ currentTicket.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <DictTag :value="currentTicket.status" :dict-type="DICT_TYPE.TICKET_STATUS" />
        </el-descriptions-item>
        <el-descriptions-item label="优先级">
          <DictTag :value="currentTicket.priority" :dict-type="DICT_TYPE.TICKET_PRIORITY" />
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ currentTicket.description || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="comment-header">评论记录</div>
      <el-timeline>
        <el-timeline-item v-for="comment in comments" :key="comment.id" :timestamp="comment.createTime">
          <strong>{{ comment.username || '-' }}</strong>: {{ comment.content }}
        </el-timeline-item>
      </el-timeline>
      <div class="comment-header">附件</div>
      <el-upload :show-file-list="false" :auto-upload="false" :on-change="handleUploadAttachment" accept="*">
        <el-button type="primary" plain>上传附件</el-button>
      </el-upload>
      <div class="attachment-list" v-if="attachments.length">
        <div class="attachment-item" v-for="attachment in attachments" :key="attachment.id">
          <a :href="downloadAttachmentUrl(attachment.id)" target="_blank">{{ attachment.fileName }}</a>
          <span class="attachment-meta">({{ attachment.uploaderName || '-' }}，{{ attachment.createTime }})</span>
        </div>
      </div>
      <el-input v-model="commentText" type="textarea" :rows="3" placeholder="请输入评论内容" />
      <div class="comment-actions">
        <el-button type="primary" @click="submitComment">发表评论</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type TableInstance } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  createTicket,
  createTicketComment,
  deleteTicket,
  deleteTicketPermanent,
  getTicket,
  getTicketAssigneeOptions,
  getTicketAttachments,
  getTicketComments,
  getTicketPage,
  getRecycleTicketPage,
  restoreTicket,
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
import type { RecyclePageQuery, MenuTreeNode } from '@/types/api'
import type { UploadFile } from 'element-plus'
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'
import { DICT_TYPE } from '@/constants/dict'
import { useDict, getDictDefaultValue, preloadDicts } from '@/composables/useDict'

const { options: statusOptions, load: loadStatusOptions } = useDict(DICT_TYPE.TICKET_STATUS, { valueType: 'string' })

const loading = ref(false)
const route = useRoute()
const userStore = useUserStore()
const total = ref(0)
const ticketList = ref<TicketVO[]>([])
const tableRef = ref<TableInstance | null>(null)
const userOptions = ref<AssigneeOptionVO[]>([])
const formVisible = ref(false)
const recycleVisible = ref(false)
const recycleLoading = ref(false)
const recycleList = ref<TicketVO[]>([])
const recycleTotal = ref(0)
const detailVisible = ref(false)
const formRef = ref<FormInstance | null>(null)
const comments = ref<TicketCommentVO[]>([])
const attachments = ref<TicketAttachmentVO[]>([])
const commentText = ref('')
const currentTicket = ref<Partial<import('@/api/system/ticket').TicketVO>>({})

const queryParams = reactive<TicketPageQuery>({
  pageNo: 1,
  pageSize: 10,
  title: '',
  status: '',
  priority: ''
})

const form = reactive<TicketSaveDTO>({
  id: null,
  title: '',
  description: '',
  priority: 'MEDIUM',
  assigneeUserId: null,
  deadline: null,
})

const recycleQuery = reactive<RecyclePageQuery>({
  pageNo: 1,
  pageSize: 10
})

const rules = {
  title: [{ required: true, message: '请输入工单标题', trigger: 'blur' }]
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

/** 可流转的目标状态（排除当前状态） */
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

const getRecycleList = async () => {
  recycleLoading.value = true
  try {
    const res = await getRecycleTicketPage(recycleQuery)
    recycleList.value = res.data.list || []
    recycleTotal.value = res.data.total || 0
  } finally {
    recycleLoading.value = false
  }
}

const openRecycleDialog = () => {
  recycleVisible.value = true
  recycleQuery.pageNo = 1
  getRecycleList()
}

const openTicketDetailById = async (ticketId: number) => {
  if (!ticketId) return
  const detailRes = await getTicket(ticketId)
  currentTicket.value = detailRes.data || {}
  const commentRes = await getTicketComments(ticketId)
  comments.value = commentRes.data || []
  const attachmentRes = await getTicketAttachments(ticketId)
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
  setTimeout(() => {
    tableRef.value?.doLayout?.()
  }, 260)
}

watch(detailVisible, async () => {
  await relayoutTable()
})

watch(formVisible, async () => {
  await relayoutTable()
})

watch(
  () => route.query.ticketId,
  async (ticketId) => {
    const parsedId = Number(ticketId)
    if (parsedId > 0) {
      await openTicketDetailById(parsedId)
    }
  },
  { immediate: true }
)

const handleStatusCommand = async (status: string, row: TicketVO) => {
  await transitionTicket({ id: row.id, status })
  ElMessage.success('状态更新成功')
  getList()
  if (detailVisible.value && currentTicket.value.id === row.id) {
    currentTicket.value.status = status
  }
}

const handleDelete = async (row: TicketVO) => {
  await ElMessageBox.confirm(`确定删除工单【${row.ticketNo}】吗？删除后不可恢复。`, '提示', { type: 'warning' })
  await deleteTicket(row.id)
  ElMessage.success('删除成功')
  const pageNo = queryParams.pageNo ?? 1
  if (pageNo > 1 && ticketList.value.length === 1) {
    queryParams.pageNo = pageNo - 1
  }
  getList()
}

const handleRestore = async (row: TicketVO) => {
  await restoreTicket(row.id)
  ElMessage.success('恢复成功')
  getRecycleList()
  getList()
}

const handleDeletePermanent = async (row: TicketVO) => {
  await ElMessageBox.confirm(`确定彻底删除工单【${row.ticketNo}】吗？该操作不可恢复。`, '警告', { type: 'warning' })
  await deleteTicketPermanent(row.id)
  ElMessage.success('已彻底删除')
  if (recycleQuery.pageNo > 1 && recycleList.value.length === 1) {
    recycleQuery.pageNo -= 1
  }
  getRecycleList()
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
  gap: 10px;
}
.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
.action-cell {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}
.comment-header {
  margin: 18px 0 12px;
  font-weight: 600;
}
.attachment-list {
  margin-top: 10px;
}
.attachment-item {
  margin-bottom: 6px;
}
.attachment-meta {
  margin-left: 6px;
  color: #909399;
  font-size: 12px;
}
.comment-actions {
  margin-top: 10px;
  text-align: right;
}
</style>
