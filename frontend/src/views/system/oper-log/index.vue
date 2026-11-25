<template>
  <div class="app-container module-page">
    <PageHeader
      title="操作日志"
      description="记录系统操作行为，支持按模块、人员与状态检索"
      :icon="MODULE_PAGE_ICON.operLog"
      :stats="[{ label: '日志总数', value: total }]"
    />

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
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
          <el-button type="primary" :icon="Search" v-permission="'system:operLog:query'" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
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
              plain
              v-permission="'system:operLog:clear'"
              @click="handleClean"
            >
              清空日志
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        v-if="tableData.length || loading"
        :data="tableData"
        v-loading="loading"
        border
        stripe
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" header-align="center" />
        <el-table-column prop="title" label="模块" min-width="140" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column label="操作摘要" min-width="240" align="left" header-align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ getActionSummary(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="业务类型" width="90" align="center" header-align="center">
          <template #default="{ row }">{{ businessTypeLabel(row.businessType) }}</template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="请求方式" width="90" align="center" header-align="center" />
        <el-table-column prop="operName" label="操作人员" width="110" align="center" header-align="center" />
        <el-table-column prop="operIp" label="IP地址" width="130" align="center" header-align="center" />
        <el-table-column label="状态" width="80" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.status === 0 ? '正常' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="center" header-align="center">
          <template #default="{ row }">
            <span class="motion-tabular">{{ row.costTime }}ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="operTime" label="操作时间" width="170" align="center" header-align="center" />
        <el-table-column label="操作" width="160" fixed="right" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="openDetail(row)">详情</el-button>
              <el-button
                v-permission="'system:operLog:delete'"
                type="danger"
                plain
                size="small"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else title="暂无操作日志" description="当前筛选条件下没有操作审计记录" />

      <el-pagination
        v-if="total > 0"
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
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
        <el-descriptions-item v-if="parsedAction" label="操作内容" :span="2">
          <InlineAlert type="info" :title="parsedAction" compact />
        </el-descriptions-item>
        <el-descriptions-item v-if="parsedDiffItems.length" label="变更明细" :span="2">
          <div class="diff-tags-wrapper">
            <el-tag v-for="(item, idx) in parsedDiffItems" :key="idx" type="warning" class="diff-tag">
              {{ item }}
            </el-tag>
          </div>
        </el-descriptions-item>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import InlineAlert from '@/components/ui/InlineAlert.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import ListExportButton from '@/components/ListExportButton.vue'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
import { pageOperLog, deleteOperLog, cleanOperLog, type OperLogVO, type OperLogPageQuery } from '@/api/system/oper-log'

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const loading = ref(false)
const total = ref(0)
const tableData = ref<OperLogVO[]>([])
const detailVisible = ref(false)
const detail = ref<Partial<OperLogVO>>({})

const queryParams = reactive<OperLogPageQuery>({
  pageNo: 1,
  pageSize: 10,
  title: '',
  operName: '',
  status: null,
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

function extractSummary(row: Partial<OperLogVO>, emptyFallback: string) {
  const cleanTitle = (t: string | undefined) => (t ? (t.endsWith('管理') ? t.slice(0, -2) : t) : '')
  const t = cleanTitle(row.title)
  if (!row.operParam) {
    if (t) {
      const typeLabel = row.businessType === 0 ? '' : businessTypeLabel(row.businessType)
      return `${typeLabel}${t}`
    }
    return emptyFallback
  }
  try {
    const obj = JSON.parse(row.operParam)
    let name = ''
    if (obj && typeof obj === 'object') {
      if (obj.action) return String(obj.action)
      const params = obj.params || obj
      if (Array.isArray(params)) {
        name = 'ID: ' + params.join(', ')
      } else if (params && typeof params === 'object') {
        name =
          params.username ||
          params.nickname ||
          params.name ||
          params.label ||
          params.title ||
          params.groupName ||
          params.dictType ||
          params.id ||
          ''
        if (typeof name === 'object') name = ''
      } else if (params != null && typeof params !== 'function') {
        name = String(params)
      }
    } else if (obj != null && typeof obj !== 'function') {
      name = String(obj)
    }
    const typeLabel = row.businessType === 0 ? '' : businessTypeLabel(row.businessType)
    if (t) return `${typeLabel}${t}${name ? `「${name}」` : ''}`
  } catch {}
  if (t) {
    const typeLabel = row.businessType === 0 ? '' : businessTypeLabel(row.businessType)
    return `${typeLabel}${t}`
  }
  return emptyFallback
}

function getActionSummary(row: OperLogVO): string {
  return extractSummary(row, '—')
}

const parsedAction = computed(() => extractSummary(detail.value, ''))

const parsedDiffItems = computed<string[]>(() => {
  if (!detail.value.operParam) return []
  try {
    const obj = JSON.parse(detail.value.operParam)
    if (obj && typeof obj === 'object' && Array.isArray(obj.diffItems)) {
      return obj.diffItems.map(String)
    }
  } catch {}
  return []
})

function formatJson(str: string | undefined) {
  if (!str) return ''
  try {
    const obj = JSON.parse(str)
    if (obj && typeof obj === 'object' && 'params' in obj) {
      return JSON.stringify(obj.params, null, 2)
    }
    return JSON.stringify(obj, null, 2)
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
      status: queryParams.status,
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
  await openAlertDialog({
    title: '永久删除操作日志',
    targetName: `${row.title} · #${row.id}`,
    message: '删除后该条操作审计记录将被永久销毁，无法找回。',
    severity: 'irreversible',
    confirmText: '永久删除',
    onConfirm: async () => {
      await deleteOperLog(row.id)
      ElMessage.success('已永久删除')
      loadData()
    },
  })
}

async function handleClean() {
  await openAlertDialog({
    title: '清空全部操作日志',
    targetName: `共 ${total.value} 条操作记录`,
    message: '此操作将永久清空全部操作审计日志且不可恢复，请确认已完成审计留档。',
    severity: 'irreversible',
    confirmText: '清空全部日志',
    requireConfirmInput: '清空日志',
    onConfirm: async () => {
      await cleanOperLog()
      ElMessage.success('已清空全部操作日志')
      loadData()
    },
  })
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.diff-tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.diff-tag {
  white-space: normal;
  height: auto;
  padding: 4px 10px;
}

.code-block {
  margin: 0;
  max-height: 200px;
  overflow: auto;
  padding: var(--space-3);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  text-align: left;
  color: var(--color-text-primary);
}

.error-text {
  color: var(--color-danger);
}
</style>
