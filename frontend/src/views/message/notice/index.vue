<template>
  <div class="app-container">
    <el-card class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="请输入标题" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.noticeType" clearable placeholder="请选择类型" style="width: 120px">
            <el-option label="通知" :value="1" />
            <el-option label="公告" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="请选择状态" style="width: 120px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>通知公告列表</span>
          <div class="header-actions">
            <RecycleCenterLink tab="announce" />
            <el-button v-permission="'system:announce:create'" type="primary" @click="openForm()">新增通知</el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.noticeType === 2 ? '公告' : '通知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createName" label="创建人" width="110" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="320" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" @click="showDetail(row)">详情</el-button>
              <el-button size="small" type="primary" v-permission="'system:announce:update'" @click="openForm(row)">编辑</el-button>
              <el-button size="small" type="success" v-if="row.status !== 1" v-permission="'system:announce:publish'" @click="handlePublish(row)">发布</el-button>
              <el-button size="small" @click="showLogs(row)">发送日志</el-button>
              <el-button size="small" type="danger" v-permission="'system:announce:delete'" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNo"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑通知' : '新增通知'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="类型" prop="noticeType">
          <el-radio-group v-model="form.noticeType">
            <el-radio :value="1">通知</el-radio>
            <el-radio :value="2">公告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发送渠道">
          <el-checkbox-group v-model="form.channels">
            <el-checkbox value="station">站内信</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="通知对象">
          <el-radio-group v-model="form.targetType">
            <el-radio :value="3">全体人员</el-radio>
            <el-radio :value="1">指定用户</el-radio>
            <el-radio :value="2">指定部门</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.targetType === 1" label="选择用户">
          <el-select v-model="form.targetIds" multiple filterable style="width: 100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.nickname || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.targetType === 2" label="选择部门">
          <el-tree-select
            v-model="form.targetIds"
            :data="deptTree"
            node-key="id"
            multiple
            check-strictly
            :props="{ label: 'name', children: 'children' }"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">草稿</el-radio>
            <el-radio :value="1">立即发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="通知详情" width="640px" destroy-on-close>
      <el-descriptions v-if="detailRow" :column="1" border>
        <el-descriptions-item label="标题">{{ detailRow.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detailRow.noticeType === 2 ? '公告' : '通知' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailRow.status === 1 ? '已发布' : '草稿' }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detailRow.createName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailRow.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容">
          <div class="detail-content">{{ detailRow.content }}</div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logVisible" title="发送日志" width="560px">
      <el-table
        :data="sendLogs"
        border
        size="small"
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="channel" label="渠道" width="100" />
        <el-table-column prop="targetCount" label="目标数" width="90" />
        <el-table-column prop="successCount" label="成功数" width="90" />
        <el-table-column prop="sendTime" label="发送时间" min-width="170">
          <template #default="{ row }">
            {{ formatLogTime(row.sendTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAnnouncePage,
  createAnnounce,
  updateAnnounce,
  deleteAnnounce,
  publishAnnounce,
  getAnnounceDetail,
  getAnnounceSendLogs,
  type AnnounceSaveDTO,
  type AnnounceVO,
  type AnnounceSendLog,
  type MessagePageQuery,
} from '@/api/message/index'
import { getUserList, type UserVO } from '@/api/system/user/index'
import { getDeptTree, type DeptVO } from '@/api/system/dept/index'
import RecycleCenterLink from '@/components/RecycleCenterLink.vue'

const tableData = ref<AnnounceVO[]>([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const query = reactive<MessagePageQuery>({
  title: '',
  noticeType: null,
  status: null,
})

const formVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<AnnounceSaveDTO>({
  id: null,
  title: '',
  content: '',
  noticeType: 1,
  targetType: 3,
  targetIds: [],
  status: 0,
  channels: ['station'],
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
}

const userOptions = ref<UserVO[]>([])
const deptTree = ref<DeptVO[]>([])
const logVisible = ref(false)
const sendLogs = ref<AnnounceSendLog[]>([])
const detailVisible = ref(false)
const detailRow = ref<AnnounceVO | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await getAnnouncePage({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      ...query,
    })
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.title = ''
  query.noticeType = null
  query.status = null
  pageNo.value = 1
  loadData()
}

async function openForm(row?: AnnounceVO) {
  if (row?.id) {
    const res = await getAnnounceDetail(row.id)
    Object.assign(form, {
      id: res.data.id,
      title: res.data.title,
      content: res.data.content,
      noticeType: res.data.noticeType ?? 1,
      targetType: res.data.targetType ?? 3,
      targetIds: res.data.targetIds || [],
      status: res.data.status ?? 0,
      channels: res.data.channels || ['station'],
    })
  } else {
    Object.assign(form, {
      id: null,
      title: '',
      content: '',
      noticeType: 1,
      targetType: 3,
      targetIds: [],
      status: 0,
      channels: ['station'],
    })
  }
  formVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (form.id) await updateAnnounce({ ...form })
    else await createAnnounce({ ...form })
    ElMessage.success('操作成功')
    formVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

async function handlePublish(row: AnnounceVO) {
  await publishAnnounce(row.id)
  ElMessage.success('发布成功')
  loadData()
}

async function handleDelete(row: AnnounceVO) {
  await ElMessageBox.confirm('确定要删除该通知吗？', '提示', { type: 'warning' })
  await deleteAnnounce(row.id)
  ElMessage.success('已删除')
  loadData()
}

function formatLogTime(time: string | undefined) {
  if (!time) return '-'
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return time
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

async function showLogs(row: AnnounceVO) {
  const res = await getAnnounceSendLogs(row.id)
  sendLogs.value = res.data || []
  logVisible.value = true
}

async function showDetail(row: AnnounceVO) {
  const res = await getAnnounceDetail(row.id)
  detailRow.value = {
    ...row,
    ...res.data,
    createName: row.createName,
    createTime: row.createTime,
  } as AnnounceVO
  detailVisible.value = true
}

onMounted(async () => {
  loadData()
  const [users, depts] = await Promise.all([getUserList(), getDeptTree()])
  userOptions.value = users.data || []
  deptTree.value = depts.data || []
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.action-buttons .el-button {
  margin: 0;
}
.detail-content {
  white-space: pre-wrap;
  line-height: 1.6;
  text-align: left;
}
</style>
