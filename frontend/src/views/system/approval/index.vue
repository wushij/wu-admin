<template>
  <div class="app-container">
    <el-card class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="标题">
          <el-input v-model="queryParams.title" placeholder="请输入审批标题" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <DictSelect
            v-model="queryParams.formType"
            :dict-type="DICT_TYPE.APPROVAL_FORM_TYPE"
            value-type="string"
            placeholder="请选择类型"
            width="160px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect
            v-model="queryParams.status"
            :dict-type="DICT_TYPE.APPROVAL_STATUS"
            value-type="string"
            placeholder="请选择状态"
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
          <span>审批单列表</span>
          <div class="header-actions">
            <el-button v-permission="'system:approval:delete'" @click="openRecycleDialog">回收站</el-button>
            <el-button type="primary" v-permission="'system:approval:create'" @click="handleCreate">提交审批单</el-button>
          </div>
        </div>
      </template>
      <el-table ref="tableRef" :data="list" border stripe v-loading="loading" :header-cell-style="{ textAlign: 'center' }" :cell-style="{ textAlign: 'center' }">
        <el-table-column prop="formNo" label="单号" width="190" />
        <el-table-column prop="title" label="标题" min-width="170" />
        <el-table-column prop="formType" label="类型" width="110">
          <template #default="{ row }">
            <DictTag :value="row.formType" :dict-type="DICT_TYPE.APPROVAL_FORM_TYPE" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <DictTag :value="row.status" :dict-type="DICT_TYPE.APPROVAL_STATUS" />
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="110" />
        <el-table-column prop="approverName" label="审批人" width="110" />
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button size="small" @click="openDetail(row.id)">详情</el-button>
              <el-dropdown v-if="canApproveRow(row) || canArchiveRow(row)" trigger="click">
                <el-button size="small" type="primary">处理</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-if="canApproveRow(row)"
                      v-permission="'system:approval:approve'"
                      @click="openApproveDialog(row, 'APPROVE')"
                    >通过</el-dropdown-item>
                    <el-dropdown-item
                      v-if="canApproveRow(row)"
                      v-permission="'system:approval:approve'"
                      @click="openApproveDialog(row, 'REJECT')"
                    >驳回</el-dropdown-item>
                    <el-dropdown-item
                      v-if="canArchiveRow(row)"
                      v-permission="'system:approval:archive'"
                      @click="handleArchive(row)"
                    >归档</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button
                v-if="canDeleteRow(row)"
                size="small"
                type="danger"
                v-permission="'system:approval:delete'"
                @click="handleDelete(row)"
              >删除</el-button>
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

    <el-dialog v-model="formVisible" title="提交审批单" width="720px" :lock-scroll="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="审批类型" prop="formType">
          <DictSelect
            v-model="form.formType"
            :dict-type="DICT_TYPE.APPROVAL_FORM_TYPE"
            value-type="string"
            apply-default
            :exclude-values="['REGISTER']"
            :clearable="false"
          />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="审批人" prop="approverUserId">
          <el-select v-model="form.approverUserId" filterable style="width: 100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="`${u.username}(${u.nickname})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入审批说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="approveVisible" :title="approveAction === 'APPROVE' ? '审批通过' : '审批驳回'" width="560px" :lock-scroll="false">
      <el-input v-model="approveRemark" type="textarea" :rows="4" placeholder="请输入审批意见（可选）" />
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApprove">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recycleVisible" title="审批单回收站" width="920px" :lock-scroll="false">
      <el-table :data="recycleList" border stripe v-loading="recycleLoading" :header-cell-style="{ textAlign: 'center' }" :cell-style="{ textAlign: 'center' }">
        <el-table-column prop="formNo" label="单号" width="190" />
        <el-table-column prop="title" label="标题" min-width="170" />
        <el-table-column prop="formType" label="类型" width="110">
          <template #default="{ row }">
            <DictTag :value="row.formType" :dict-type="DICT_TYPE.APPROVAL_FORM_TYPE" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <DictTag :value="row.status" :dict-type="DICT_TYPE.APPROVAL_STATUS" />
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="110" />
        <el-table-column prop="approverName" label="审批人" width="110" />
        <el-table-column prop="updateTime" label="删除时间" width="170" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
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

    <el-drawer v-model="detailVisible" title="审批单详情" size="45%" :lock-scroll="false">
      <el-descriptions :column="1" border v-if="current.id">
        <el-descriptions-item label="单号">{{ current.formNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <DictTag :value="current.formType" :dict-type="DICT_TYPE.APPROVAL_FORM_TYPE" />
        </el-descriptions-item>
        <el-descriptions-item label="标题">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <DictTag :value="current.status" :dict-type="DICT_TYPE.APPROVAL_STATUS" />
        </el-descriptions-item>
        <el-descriptions-item label="申请人">{{ current.applicantName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ current.approverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批意见">{{ String(current.resultRemark || '').trim() || '无' }}</el-descriptions-item>
        <el-descriptions-item v-if="current.formType === 'REGISTER'" label="注册账号">
          {{ registerDetail.username || '-' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="current.formType === 'REGISTER'" label="昵称">
          {{ registerDetail.nickname || '-' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="current.formType === 'REGISTER'" label="手机号">
          {{ registerDetail.mobile || '-' }}
        </el-descriptions-item>
        <el-descriptions-item v-else label="内容">{{ current.content || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="record-title">审批记录</div>
      <el-timeline>
        <el-timeline-item v-for="item in records" :key="item.id" :timestamp="item.createTime">
          <strong>{{ item.operatorName || '-' }}</strong> {{ actionText(item.action) }}
          <span v-if="item.remark">：{{ item.remark }}</span>
        </el-timeline-item>
      </el-timeline>
      <template #footer>
        <div v-if="current.id && (canApproveRow(current as ApprovalVO) || canArchiveRow(current as ApprovalVO))" class="detail-actions">
          <el-button
            v-if="canApproveRow(current as ApprovalVO)"
            v-permission="'system:approval:approve'"
            plain
            class="detail-action-btn detail-action-btn--approve"
            @click="openApproveDialog(current as ApprovalVO, 'APPROVE')"
          >通过</el-button>
          <el-button
            v-if="canApproveRow(current as ApprovalVO)"
            v-permission="'system:approval:approve'"
            plain
            type="danger"
            class="detail-action-btn"
            @click="openApproveDialog(current as ApprovalVO, 'REJECT')"
          >驳回</el-button>
          <el-button
            v-if="canArchiveRow(current as ApprovalVO)"
            v-permission="'system:approval:archive'"
            plain
            class="detail-action-btn detail-action-btn--approve"
            @click="handleArchive(current as ApprovalVO)"
          >归档</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type TableInstance } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getUserList, type UserVO } from '@/api/system/user'
import {
  approveApproval,
  archiveApproval,
  createApproval,
  deleteApproval,
  deleteApprovalPermanent,
  getApproval,
  getApprovalPage,
  getApprovalRecords,
  getApprovalRecyclePage,
  restoreApproval,
  type ApprovalVO,
  type ApprovalRecordVO,
  type ApprovalCreateDTO,
  type ApprovalPageQuery,
} from '@/api/system/approval'
import type { RecyclePageQuery } from '@/types/api'
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'
import { DICT_TYPE } from '@/constants/dict'
import { getDictDefaultValue, preloadDicts } from '@/composables/useDict'

const userStore = useUserStore()
const route = useRoute()
const loading = ref(false)
const tableRef = ref<TableInstance | null>(null)
const total = ref(0)
const list = ref<ApprovalVO[]>([])
const userOptions = ref<UserVO[]>([])
const formVisible = ref(false)
const approveVisible = ref(false)
const recycleVisible = ref(false)
const recycleLoading = ref(false)
const recycleList = ref<ApprovalVO[]>([])
const recycleTotal = ref(0)
const detailVisible = ref(false)
const formRef = ref<FormInstance | null>(null)
const current = ref<Partial<import('@/api/system/approval').ApprovalVO>>({})
const records = ref<ApprovalRecordVO[]>([])
const approveAction = ref('APPROVE')
const approveRemark = ref('')
const approveTargetId = ref<number | null>(null)

const queryParams = reactive<ApprovalPageQuery>({
  pageNo: 1,
  pageSize: 10,
  title: '',
  formType: '',
  status: ''
})

const form = reactive<ApprovalCreateDTO>({
  formType: 'GENERAL',
  title: '',
  approverUserId: null,
  content: '',
})

const recycleQuery = reactive<RecyclePageQuery>({
  pageNo: 1,
  pageSize: 10
})

const rules = {
  formType: [{ required: true, message: '请选择审批类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  approverUserId: [{ required: true, message: '请选择审批人', trigger: 'change' }],
  content: [{ required: true, message: '请输入审批内容', trigger: 'blur' }]
}

interface RegisterDetailParsed {
  userId?: number
  username?: string
  nickname?: string
  mobile?: string
}

const registerDetail = ref<RegisterDetailParsed>({})

const ACTION_LABELS = {
  SUBMIT: '提交审批',
  APPROVE: '审批通过',
  REJECT: '审批驳回',
  ARCHIVE: '归档单据',
} as const

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
  const res = await getUserList()
  userOptions.value = (res.data || []).filter(u => u.id !== userStore.userInfo?.userId)
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

/** 支持工作台等入口通过 query 预置筛选（如 formType=REGISTER&status=SUBMITTED） */
const applyRouteQuery = () => {
  const { formType, status, title } = route.query
  if (typeof formType === 'string' && formType) {
    queryParams.formType = formType
  }
  if (typeof status === 'string' && status) {
    queryParams.status = status
  }
  if (typeof title === 'string') {
    queryParams.title = title
  }
}

const handleCreate = () => {
  form.formType = (getDictDefaultValue(DICT_TYPE.APPROVAL_FORM_TYPE) as string) || 'GENERAL'
  form.title = ''
  form.approverUserId = null
  form.content = ''
  formVisible.value = true
}

const getRecycleList = async () => {
  recycleLoading.value = true
  try {
    const res = await getApprovalRecyclePage(recycleQuery)
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
  if (row.formType === 'REGISTER') {
    return hasPerm('system:approval:approve')
  }
  return row.approverUserId === userStore.userInfo?.userId
}

const canArchiveRow = (row: ApprovalVO) => {
  if (row.status !== 'APPROVED' && row.status !== 'REJECTED') return false
  if (row.formType === 'REGISTER') {
    return hasPerm('system:approval:archive')
  }
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
    remark: approveRemark.value
  })
  ElMessage.success('审批完成')
  approveVisible.value = false
  await getList()
  if (detailVisible.value && current.value.id === targetId) {
    await openDetail(targetId)
  }
}

const handleArchive = async (row: ApprovalVO) => {
  await ElMessageBox.confirm(`确认归档审批单【${row.formNo}】吗？`, '提示', { type: 'warning' })
  await archiveApproval({ id: row.id, remark: '归档' })
  ElMessage.success('归档成功')
  await getList()
  if (detailVisible.value && current.value.id === row.id) {
    await openDetail(row.id)
  }
}

const handleDelete = async (row: ApprovalVO) => {
  await ElMessageBox.confirm(`确认删除审批单【${row.formNo}】吗？删除后不可恢复。`, '提示', { type: 'warning' })
  await deleteApproval(row.id)
  ElMessage.success('删除成功')
  const pageNo = queryParams.pageNo ?? 1
  if (pageNo > 1 && list.value.length === 1) {
    queryParams.pageNo = pageNo - 1
  }
  getList()
}

const handleRestore = async (row: ApprovalVO) => {
  await restoreApproval(row.id)
  ElMessage.success('恢复成功')
  getRecycleList()
  getList()
}

const handlePermanentDelete = async (row: ApprovalVO) => {
  await ElMessageBox.confirm(`确认彻底删除审批单【${row.formNo}】吗？该操作不可恢复。`, '警告', { type: 'warning' })
  await deleteApprovalPermanent(row.id)
  ElMessage.success('已彻底删除')
  if (recycleQuery.pageNo > 1 && recycleList.value.length === 1) {
    recycleQuery.pageNo -= 1
  }
  getRecycleList()
}

const openDetail = async (id: number) => {
  const [detailRes, recordRes] = await Promise.all([getApproval(id), getApprovalRecords(id)])
  current.value = detailRes.data || {}
  registerDetail.value = current.value.formType === 'REGISTER'
    ? parseRegisterContent(
        typeof current.value.content === 'string' ? current.value.content : undefined
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
  }
)

watch(
  () => route.query.approvalId,
  async (approvalId) => {
    const parsedId = Number(approvalId)
    if (parsedId > 0) {
      await openDetail(parsedId)
    }
  },
  { immediate: true }
)

const relayoutTable = async () => {
  await nextTick()
  tableRef.value?.doLayout?.()
  setTimeout(() => {
    tableRef.value?.doLayout?.()
  }, 260)
}

watch(formVisible, relayoutTable)
watch(approveVisible, relayoutTable)
watch(detailVisible, relayoutTable)
watch(recycleVisible, relayoutTable)
</script>

<style scoped>
.app-container { padding: 0; }
.search-card { margin-bottom: 20px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  gap: 10px;
}
.action-cell {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}
.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
.record-title {
  margin: 16px 0 10px;
  font-weight: 600;
}
.detail-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  width: 100%;
}
.detail-action-btn {
  min-width: 88px;
}
.detail-action-btn--approve {
  --el-button-border-color: #303133;
  --el-button-text-color: #303133;
  --el-button-hover-border-color: #303133;
  --el-button-hover-text-color: #303133;
  --el-button-hover-bg-color: #f5f7fa;
  --el-button-active-border-color: #303133;
  --el-button-active-text-color: #303133;
  --el-button-active-bg-color: #eef0f3;
}
</style>
