<template>
  <div class="app-container">
    <el-card class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="模块名称">
          <el-input v-model="queryParams.title" placeholder="请输入模块名称" clearable />
        </el-form-item>
        <el-form-item label="操作人员">
          <el-input v-model="queryParams.operName" placeholder="请输入操作人员" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="正常" :value="0" />
            <el-option label="异常" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" v-permission="'system:operLog:query'" @click="handleQuery">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作日志列表</span>
          <div class="header-actions">
            <ListExportButton module="oper-log" :query-params="queryParams" permission="system:operLog:query" />
            <el-button
              type="danger"
              v-permission="'system:operLog:clear'"
              @click="handleClean"
            >
              清空日志
            </el-button>
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
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="模块" min-width="120" show-overflow-tooltip />
        <el-table-column label="业务类型" width="90">
          <template #default="{ row }">{{ businessTypeLabel(row.businessType) }}</template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="请求方式" width="90" />
        <el-table-column prop="operName" label="操作人员" width="110" />
        <el-table-column prop="operIp" label="IP地址" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ row.costTime }}ms</template>
        </el-table-column>
        <el-table-column prop="operTime" label="操作时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="openDetail(row)">详情</el-button>
              <el-button
                v-permission="'system:operLog:delete'"
                type="danger"
                size="small"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
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
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="日志详情" width="760px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="模块名称">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ businessTypeLabel(detail.businessType) }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="操作人员">{{ detail.operName }}</el-descriptions-item>
        <el-descriptions-item label="操作地址">{{ detail.operIp }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costTime }}ms</el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ detail.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="方法名称" :span="2">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <el-tag :type="detail.status === 0 ? 'success' : 'danger'" size="small">
            {{ detail.status === 0 ? '正常' : '异常' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detail.operTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误信息" :span="2">
          <span class="error-text">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="code-block">{{ formatJson(detail.operParam) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="返回参数" :span="2">
          <pre class="code-block">{{ formatJson(detail.jsonResult) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ListExportButton from '@/components/ListExportButton.vue'
import { pageOperLog, deleteOperLog, cleanOperLog, type OperLogVO, type OperLogPageQuery } from '@/api/system/oper-log'

const loading = ref(false)
const total = ref(0)
const tableData = ref<OperLogVO[]>([])

const queryParams = reactive<OperLogPageQuery>({
  pageNo: 1,
  pageSize: 10,
  title: '',
  operName: '',
  status: null
})

const businessTypeMap: Record<number, string> = {
  0: '其他',
  1: '新增',
  2: '修改',
  3: '删除',
  4: '查询',
  5: '导出',
  6: '导入',
}

function businessTypeLabel(type: number | undefined) {
  if (type == null) return '其他'
  return businessTypeMap[type] ?? '其他'
}

const detailVisible = ref(false)
const detail = ref<Partial<import('@/api/system/oper-log').OperLogVO>>({})

function formatJson(str: string | undefined) {
  if (!str) return ''
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageOperLog({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      title: queryParams.title || undefined,
      operName: queryParams.operName || undefined,
      status: queryParams.status
    })
    tableData.value = res.data?.list || []
    total.value = Number(res.data?.total) || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNo = 1
  loadData()
}

function resetQuery() {
  queryParams.title = ''
  queryParams.operName = ''
  queryParams.status = null
  handleQuery()
}

function openDetail(row: OperLogVO) {
  detail.value = { ...row }
  detailVisible.value = true
}

async function handleDelete(row: OperLogVO) {
  await ElMessageBox.confirm('确定要删除该日志吗？', '提示', { type: 'warning' })
  await deleteOperLog(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleClean() {
  await ElMessageBox.confirm('确定要清空所有操作日志吗？此操作不可恢复！', '警告', { type: 'warning' })
  await cleanOperLog()
  ElMessage.success('清空成功')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.search-card {
  margin-bottom: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.el-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.code-block {
  margin: 0;
  max-height: 200px;
  overflow: auto;
  padding: 8px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  text-align: left;
}
.error-text {
  color: var(--el-color-danger);
}
</style>
