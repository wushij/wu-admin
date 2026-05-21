<template>
  <div class="app-container">
    <el-card class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="标题">
          <el-input v-model="queryParams.title" placeholder="请输入审批标题" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryParams.formType" placeholder="请选择类型" clearable style="width: 160px">
            <el-option label="通用" value="GENERAL" />
            <el-option label="请假" value="LEAVE" />
            <el-option label="采购" value="PURCHASE" />
            <el-option label="报销" value="REIMBURSE" />
            <el-option label="用印" value="SEAL" />
            <el-option label="合同" value="CONTRACT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
            <el-option label="待审批" value="SUBMITTED" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
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
          <template #default="{ row }">{{ formatType(row.formType) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ formatStatus(row.status) }}</el-tag>
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
          <el-select v-model="form.formType" style="width: 100%">
            <el-option label="通用" value="GENERAL" />
            <el-option label="请假" value="LEAVE" />
            <el-option label="采购" value="PURCHASE" />
            <el-option label="报销" value="REIMBURSE" />
            <el-option label="用印" value="SEAL" />
            <el-option label="合同" value="CONTRACT" />
          </el-select>
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
          <template #default="{ row }">{{ formatType(row.formType) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ formatStatus(row.status) }}</el-tag>
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
        <el-descriptions-item label="类型">{{ formatType(current.formType) }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ formatStatus(current.status) }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ current.applicantName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ current.approverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批结果">{{ current.resultRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ current.content || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="record-title">审批记录</div>
      <el-timeline>
        <el-timeline-item v-for="item in records" :key="item.id" :timestamp="item.createTime">
          <strong>{{ item.operatorName || '-' }}</strong> {{ actionText(item.action) }}
          <span v-if="item.remark">：{{ item.remark }}</span>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getUserList } from '@/api/system/user'
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
  restoreApproval
} from '@/api/system/approval'

const userStore = useUserStore()
const route = useRoute()
const loading = ref(false)
const tableRef = ref(null)
const total = ref(0)
const list = ref([])
const userOptions = ref([])
const formVisible = ref(false)
const approveVisible = ref(false)
const recycleVisible = ref(false)
const recycleLoading = ref(false)
const recycleList = ref([])
const recycleTotal = ref(0)
const detailVisible = ref(false)
const formRef = ref(null)
const current = ref({})
const records = ref([])
const approveAction = ref('APPROVE')
const approveRemark = ref('')
const approveTargetId = ref(null)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  title: '',
  formType: '',
  status: ''
})

const form = reactive({
  formType: 'GENERAL',
  title: '',
  approverUserId: null,
  content: ''
})

const recycleQuery = reactive({
  pageNo: 1,
  pageSize: 10
})

const rules = {
  formType: [{ required: true, message: '请选择审批类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  approverUserId: [{ required: true, message: '请选择审批人', trigger: 'change' }],
  content: [{ required: true, message: '请输入审批内容', trigger: 'blur' }]
}

const formatType = (type) => {
  const map = {
    GENERAL: '通用',
    LEAVE: '请假',
    PURCHASE: '采购',
    REIMBURSE: '报销',
    SEAL: '用印',
    CONTRACT: '合同'
  }
  return map[type] || type || '-'
}

const formatStatus = (status) => {
  const map = {
    SUBMITTED: '待审批',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    ARCHIVED: '已归档'
  }
  return map[status] || status || '-'
}

const statusTagType = (status) => {
  if (status === 'SUBMITTED') return 'warning'
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

const actionText = (action) => {
  const map = {
    SUBMIT: '提交审批',
    APPROVE: '审批通过',
    REJECT: '审批驳回',
    ARCHIVE: '归档单据'
  }
  return map[action] || action
}

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

const handleCreate = () => {
  form.formType = 'GENERAL'
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

const canApproveRow = (row) => {
  return row.status === 'SUBMITTED' && row.approverUserId === userStore.userInfo?.userId
}

const canArchiveRow = (row) => {
  return (row.status === 'APPROVED' || row.status === 'REJECTED') && row.applicantUserId === userStore.userInfo?.userId
}

const openApproveDialog = (row, action) => {
  approveTargetId.value = row.id
  approveAction.value = action
  approveRemark.value = ''
  approveVisible.value = true
}

const submitApprove = async () => {
  if (!approveTargetId.value) return
  await approveApproval({
    id: approveTargetId.value,
    action: approveAction.value,
    remark: approveRemark.value
  })
  ElMessage.success('审批完成')
  approveVisible.value = false
  getList()
}

const handleArchive = async (row) => {
  await ElMessageBox.confirm(`确认归档审批单【${row.formNo}】吗？`, '提示', { type: 'warning' })
  await archiveApproval({ id: row.id, remark: '归档' })
  ElMessage.success('归档成功')
  getList()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除审批单【${row.formNo}】吗？删除后不可恢复。`, '提示', { type: 'warning' })
  await deleteApproval(row.id)
  ElMessage.success('删除成功')
  if (queryParams.pageNo > 1 && list.value.length === 1) {
    queryParams.pageNo -= 1
  }
  getList()
}

const handleRestore = async (row) => {
  await restoreApproval(row.id)
  ElMessage.success('恢复成功')
  getRecycleList()
  getList()
}

const handlePermanentDelete = async (row) => {
  await ElMessageBox.confirm(`确认彻底删除审批单【${row.formNo}】吗？该操作不可恢复。`, '警告', { type: 'warning' })
  await deleteApprovalPermanent(row.id)
  ElMessage.success('已彻底删除')
  if (recycleQuery.pageNo > 1 && recycleList.value.length === 1) {
    recycleQuery.pageNo -= 1
  }
  getRecycleList()
}

const openDetail = async (id) => {
  const [detailRes, recordRes] = await Promise.all([getApproval(id), getApprovalRecords(id)])
  current.value = detailRes.data || {}
  records.value = recordRes.data || []
  detailVisible.value = true
}

onMounted(() => {
  getList()
  loadUsers()
})

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
</style>
