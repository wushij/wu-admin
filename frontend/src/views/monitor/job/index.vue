<template>
  <div class="app-container job-page">
    <!-- 顶部总览 -->
    <div class="job-hero">
      <div class="hero-left">
        <h2 class="hero-title">定时任务调度中心</h2>
        <p class="hero-desc">基于 Quartz 管理维护任务：日志归档、聊天消息清理、通知与回收站清理等</p>
        <div class="hero-tags">
          <el-tag type="info" effect="plain">任务 {{ overview.totalJobs ?? 0 }}</el-tag>
          <el-tag type="success" effect="plain">运行中 {{ overview.runningJobs ?? 0 }}</el-tag>
          <el-tag type="warning" effect="plain">已暂停 {{ overview.pausedJobs ?? 0 }}</el-tag>
        </div>
      </div>
      <el-row :gutter="12" class="hero-stats">
        <el-col :span="6" :xs="12">
          <div class="hero-stat">
            <div class="hero-stat-value">{{ overview.totalCount ?? 0 }}</div>
            <div class="hero-stat-label">累计执行</div>
          </div>
        </el-col>
        <el-col :span="6" :xs="12">
          <div class="hero-stat success">
            <div class="hero-stat-value">{{ overview.successCount ?? 0 }}</div>
            <div class="hero-stat-label">成功</div>
          </div>
        </el-col>
        <el-col :span="6" :xs="12">
          <div class="hero-stat fail">
            <div class="hero-stat-value">{{ overview.failCount ?? 0 }}</div>
            <div class="hero-stat-label">失败</div>
          </div>
        </el-col>
        <el-col :span="6" :xs="12">
          <div class="hero-stat rate">
            <div class="hero-stat-value">{{ overview.successRate ?? 100 }}%</div>
            <div class="hero-stat-label">成功率</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 图表 -->
    <el-row :gutter="16" class="charts-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span>执行结果分布</span></template>
          <div ref="pieChartRef" class="chart-box" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span>近 7 日执行趋势</span></template>
          <div ref="barChartRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷模板 -->
    <el-card shadow="never" class="template-card">
      <template #header>
        <div class="card-header-row">
          <span>快捷模板</span>
          <span class="header-tip">点击卡片一键创建任务（默认暂停，确认后可启用）</span>
        </div>
      </template>
      <el-row :gutter="12">
        <el-col v-for="tpl in templates" :key="tpl.key" :xs="24" :sm="12" :lg="8" :xl="6">
          <div class="template-item" @click="quickCreateFromTemplate(tpl)">
            <div class="template-icon">{{ tplIcon(tpl.icon) }}</div>
            <div class="template-body">
              <div class="template-name">{{ tpl.name }}</div>
              <div class="template-desc">{{ tpl.description }}</div>
              <div class="template-meta">
                <el-tag size="small" type="info">{{ tpl.relatedModule }}</el-tag>
                <span class="template-cron">{{ tpl.cronExpression }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 任务列表 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header-row">
          <span>任务列表</span>
          <el-button link type="primary" @click="refreshAll(true)">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>

      <el-form :model="queryParams" inline class="search-form">
        <el-form-item label="任务名称">
          <el-input v-model="queryParams.jobName" placeholder="模糊搜索" clearable style="width: 150px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="任务组">
          <el-select v-model="queryParams.jobGroup" placeholder="全部" clearable style="width: 130px">
            <el-option label="SYSTEM" value="SYSTEM" />
            <el-option label="DEFAULT" value="DEFAULT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="运行中" :value="1" />
            <el-option label="已暂停" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button v-permission="'monitor:job:add'" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>自定义任务
        </el-button>
        <el-button @click="openAllLogs">
          <el-icon><List /></el-icon>调度日志
        </el-button>
        <el-button v-permission="'monitor:job:delete'" type="danger" plain @click="handleCleanLogs">清空日志</el-button>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        class="job-table"
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="jobName" label="任务" min-width="150" align="center">
          <template #default="{ row }">
            <div class="job-name-cell">
              <span class="job-name">{{ row.jobName }}</span>
              <el-tag size="small" :type="groupTagType(row.jobGroup)">{{ row.jobGroup }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="invokeTarget" label="调用目标" min-width="220" align="center" show-overflow-tooltip />
        <el-table-column label="调度策略" min-width="160" align="center">
          <template #default="{ row }">
            <div class="cron-cell">
              <code class="cron-code">{{ row.cronExpression }}</code>
              <span v-if="row.cronHint" class="cron-hint">{{ row.cronHint }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="下次执行" min-width="170" align="center">
          <template #default="{ row }">
            <span v-if="row.status === 1 && row.nextFireTime" class="next-time">{{ row.nextFireTime }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-permission="'monitor:job:edit'"
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              active-text="开"
              inactive-text="停"
              @change="(v: string | number | boolean) => handleStatusChange(row, Number(v))"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="openJobLogs(row)">日志</el-button>
              <el-button v-permission="'monitor:job:edit'" type="primary" size="small" @click="handleRun(row)">执行</el-button>
              <el-button v-permission="'monitor:job:edit'" type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-permission="'monitor:job:delete'" type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="loadPage"
          @current-change="loadPage"
        />
      </div>
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="680px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="任务名称" prop="jobName">
          <el-input v-model="formData.jobName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务组名" prop="jobGroup">
          <el-select v-model="formData.jobGroup" allow-create filterable default-first-option style="width: 100%">
            <el-option label="SYSTEM（系统任务）" value="SYSTEM" />
            <el-option label="DEFAULT（默认）" value="DEFAULT" />
          </el-select>
        </el-form-item>
        <el-form-item label="调用目标" prop="invokeTarget">
          <el-input v-model="formData.invokeTarget" placeholder="beanName.methodName" />
        </el-form-item>
        <el-form-item label="Cron" prop="cronExpression">
          <div class="cron-row">
            <el-input
              v-model="formData.cronExpression"
              placeholder="0 30 2 * * ?"
              @blur="validateCron"
            />
            <el-select v-model="cronPreset" placeholder="常用" clearable style="width: 140px" @change="applyCronPreset">
              <el-option v-for="p in cronPresets" :key="p.value" :label="p.label" :value="p.value" />
            </el-select>
          </div>
          <div v-if="cronPreview.hint" class="cron-preview">
            <el-icon><Clock /></el-icon>
            <span>{{ cronPreview.hint }}</span>
            <span v-if="cronPreview.nextFireTimes?.length" class="preview-times">
              下次：{{ cronPreview.nextFireTimes[0] }}
            </span>
          </div>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="错误策略">
              <el-select v-model="formData.misfirePolicy" style="width: 100%">
                <el-option label="立即执行" :value="1" />
                <el-option label="执行一次" :value="2" />
                <el-option label="放弃执行" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="并发执行">
              <el-radio-group v-model="formData.concurrent">
                <el-radio :value="0">允许</el-radio>
                <el-radio :value="1">禁止</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="创建后启用">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 调度日志 -->
    <el-drawer v-model="logVisible" :title="logTitle" size="56%" destroy-on-close @closed="onLogClosed">
      <div class="log-drawer-body">
        <el-radio-group v-model="logStatusFilter" class="log-filter" @change="loadLogs">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="0">成功</el-radio-button>
          <el-radio-button value="1">失败</el-radio-button>
        </el-radio-group>
        <el-table
          :data="logData"
          v-loading="logLoading"
          border
          stripe
          size="small"
          class="log-table"
          style="width: 100%"
          :header-cell-style="{ textAlign: 'center' }"
          :cell-style="{ textAlign: 'center' }"
        >
          <el-table-column prop="jobName" label="任务" min-width="140" align="center" show-overflow-tooltip />
          <el-table-column label="结果" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
                {{ row.status === 0 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="耗时" width="90" align="center">
            <template #default="{ row }">{{ formatDuration(row) }}</template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始" min-width="170" align="center" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button type="primary" size="small" @click="showLogDetail(row)">详情</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="logQuery.pageNo"
            v-model:page-size="logQuery.pageSize"
            :total="logTotal"
            layout="total, prev, pager, next"
            small
            background
            @current-change="loadLogs"
          />
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="logDetailVisible" title="执行详情" width="560px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="任务">{{ logDetail?.jobName }}</el-descriptions-item>
        <el-descriptions-item label="调用目标">{{ logDetail?.invokeTarget }}</el-descriptions-item>
        <el-descriptions-item label="信息">{{ logDetail?.jobMessage }}</el-descriptions-item>
        <el-descriptions-item label="开始">{{ logDetail?.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束">{{ logDetail?.stopTime }}</el-descriptions-item>
        <el-descriptions-item v-if="logDetail?.exceptionInfo" label="异常">
          <pre class="exception-pre">{{ logDetail.exceptionInfo }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, List, Refresh, Clock } from '@element-plus/icons-vue'
import type { EChartsType } from 'echarts'
import {
  getJobOverview, getJobTemplates, checkJobCron,
  getJobPage, createJob, updateJob, deleteJob, changeJobStatus, runJob,
  getJobLogPage, cleanJobLogs,
  type SysJob, type SysJobLog, type JobTemplate, type JobOverview,
} from '@/api/monitor/job'

let echartsModule: typeof import('echarts') | null = null
let pieChart: EChartsType | null = null
let barChart: EChartsType | null = null

const pieChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()

const overview = reactive<JobOverview>({})
const templates = ref<JobTemplate[]>([])
const loading = ref(false)
const tableData = ref<SysJob[]>([])
const total = ref(0)
const queryParams = reactive({ pageNo: 1, pageSize: 10, jobName: '', jobGroup: '', status: null as number | null })

const formVisible = ref(false)
const formTitle = ref('新增任务')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const cronPreset = ref<string>()
const cronPreview = reactive<{ hint?: string; nextFireTimes?: string[] }>({})
const formData = reactive<SysJob>({
  jobName: '', jobGroup: 'DEFAULT', invokeTarget: '', cronExpression: '',
  misfirePolicy: 3, concurrent: 1, status: 0, remark: '',
})

const formRules: FormRules = {
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  invokeTarget: [{ required: true, message: '请输入调用目标', trigger: 'blur' }],
  cronExpression: [{ required: true, message: '请输入 Cron 表达式', trigger: 'blur' }],
}

const cronPresets = [
  { label: '每 30 秒', value: '0/30 * * * * ?' },
  { label: '每分钟', value: '0 * * * * ?' },
  { label: '每 5 分钟', value: '0 0/5 * * * ?' },
  { label: '每天 0 点', value: '0 0 0 * * ?' },
  { label: '每天 2:30', value: '0 30 2 * * ?' },
  { label: '每周一 9 点', value: '0 0 9 ? * MON' },
]

const logVisible = ref(false)
const logLoading = ref(false)
const logData = ref<SysJobLog[]>([])
const logTotal = ref(0)
const logFilter = ref<{ jobName?: string; jobGroup?: string } | null>(null)
const logStatusFilter = ref<'all' | '0' | '1'>('all')
const logQuery = reactive({ pageNo: 1, pageSize: 10 })
const logDetailVisible = ref(false)
const logDetail = ref<SysJobLog | null>(null)
const logTitle = computed(() =>
  logFilter.value?.jobName ? `调度日志 · ${logFilter.value.jobName}` : '调度日志'
)

function tplIcon(icon?: string) {
  const map: Record<string, string> = {
    Document: '📄', ChatDotRound: '💬', ChatLineRound: '👥', Timer: '⏱️', Bell: '🔔', Tickets: '🎫',
  }
  return map[icon || ''] || '📌'
}

function groupTagType(group?: string) {
  return group === 'SYSTEM' ? 'warning' : 'info'
}

function applyCronPreset(val?: string) {
  if (val) {
    formData.cronExpression = val
    cronPreset.value = undefined
    validateCron()
  }
}

async function validateCron() {
  if (!formData.cronExpression?.trim()) {
    cronPreview.hint = undefined
    cronPreview.nextFireTimes = undefined
    return
  }
  try {
    const res = await checkJobCron(formData.cronExpression.trim())
    cronPreview.hint = res.data?.hint
    cronPreview.nextFireTimes = res.data?.nextFireTimes
  } catch {
    cronPreview.hint = '表达式无效'
    cronPreview.nextFireTimes = undefined
  }
}

async function ensureEcharts() {
  if (!echartsModule) echartsModule = await import('echarts')
  return echartsModule
}

function readDailyField(row: Record<string, unknown>, ...keys: string[]) {
  for (const k of keys) {
    const v = row[k]
    if (v != null) return Number(v)
  }
  return 0
}

async function loadOverview() {
  const res = await getJobOverview()
  Object.assign(overview, res.data ?? {})
  await nextTick()
  updateCharts()
}

async function loadTemplates() {
  const res = await getJobTemplates()
  templates.value = res.data ?? []
}

async function updateCharts() {
  const echarts = await ensureEcharts()
  const execTotal = (overview.successCount ?? 0) + (overview.failCount ?? 0)

  if (pieChartRef.value) {
    if (!pieChart) pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      color: ['#52c41a', '#ff4d4f', '#e5e7eb'],
      series: [{
        type: 'pie', radius: ['45%', '72%'],
        data: execTotal > 0
          ? [{ name: '成功', value: overview.successCount }, { name: '失败', value: overview.failCount }]
          : [{ name: '暂无数据', value: 1 }],
      }],
    })
  }

  if (barChartRef.value) {
    if (!barChart) barChart = echarts.init(barChartRef.value)
    const daily = overview.dailyStats ?? []
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['成功', '失败'] },
      grid: { left: 40, right: 16, bottom: 28, top: 32 },
      xAxis: { type: 'category', data: daily.map((d) => String(d.exec_date ?? d.execDate ?? '')) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '成功', type: 'bar', stack: 'total', data: daily.map((d) => readDailyField(d, 'success_count', 'successCount')), itemStyle: { color: '#52c41a' } },
        { name: '失败', type: 'bar', stack: 'total', data: daily.map((d) => readDailyField(d, 'fail_count', 'failCount')), itemStyle: { color: '#ff4d4f' } },
      ],
    })
  }
}

async function loadPage() {
  loading.value = true
  try {
    const res = await getJobPage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      jobName: queryParams.jobName || undefined,
      jobGroup: queryParams.jobGroup || undefined,
      status: queryParams.status ?? undefined,
    })
    tableData.value = res.data?.list ?? []
    total.value = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

async function refreshAll(showToast = false) {
  await Promise.all([loadOverview(), loadPage()])
  if (showToast) ElMessage.success('已刷新')
}

function handleSearch() {
  queryParams.pageNo = 1
  loadPage()
}

function handleReset() {
  queryParams.jobName = ''
  queryParams.jobGroup = ''
  queryParams.status = null
  handleSearch()
}

function resetForm() {
  Object.assign(formData, {
    id: undefined, jobName: '', jobGroup: 'DEFAULT', invokeTarget: '', cronExpression: '',
    misfirePolicy: 3, concurrent: 1, status: 0, remark: '',
  })
  cronPreview.hint = undefined
  cronPreview.nextFireTimes = undefined
}

function handleAdd() {
  formTitle.value = '自定义任务'
  resetForm()
  formVisible.value = true
}

function handleEdit(row: SysJob) {
  formTitle.value = '编辑任务'
  Object.assign(formData, { ...row })
  formVisible.value = true
  validateCron()
}

async function quickCreateFromTemplate(tpl: JobTemplate) {
  await ElMessageBox.confirm(
    `将创建任务「${tpl.name}」\n调用：${tpl.invokeTarget}\nCron：${tpl.cronExpression}`,
    '从模板创建',
    { confirmButtonText: '创建', cancelButtonText: '取消', type: 'info' },
  )
  await createJob({
    jobName: tpl.name,
    jobGroup: tpl.jobGroup || 'DEFAULT',
    invokeTarget: tpl.invokeTarget,
    cronExpression: tpl.cronExpression,
    misfirePolicy: 3,
    concurrent: 1,
    status: 0,
    remark: tpl.description,
  })
  ElMessage.success('已从模板创建（默认暂停）')
  await refreshAll()
}

async function submitForm() {
  await formRef.value?.validate()
  await validateCron()
  if (cronPreview.hint === '表达式无效') {
    ElMessage.warning('请修正 Cron 表达式')
    return
  }
  submitting.value = true
  try {
    if (formData.id) {
      await updateJob(formData)
      ElMessage.success('更新成功')
    } else {
      await createJob(formData)
      ElMessage.success(formData.status === 1 ? '创建并已启用' : '创建成功（已暂停）')
    }
    formVisible.value = false
    await refreshAll()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysJob) {
  await ElMessageBox.confirm(`确定删除「${row.jobName}」？`, '提示', { type: 'warning' })
  await deleteJob(row.id!)
  ElMessage.success('已删除')
  await refreshAll()
}

async function handleStatusChange(row: SysJob, status: number) {
  try {
    await changeJobStatus(row.id!, status)
    ElMessage.success(status === 1 ? '已启用' : '已暂停')
    await loadPage()
    await loadOverview()
  } catch {
    row.status = status === 1 ? 0 : 1
  }
}

async function handleRun(row: SysJob) {
  await runJob(row.id!)
  ElMessage.success('已触发执行')
  setTimeout(refreshAll, 1000)
}

function openAllLogs() {
  logFilter.value = null
  logQuery.pageNo = 1
  logStatusFilter.value = 'all'
  logVisible.value = true
  loadLogs()
}

function openJobLogs(row: SysJob) {
  logFilter.value = { jobName: row.jobName, jobGroup: row.jobGroup }
  logQuery.pageNo = 1
  logVisible.value = true
  loadLogs()
}

async function loadLogs() {
  logLoading.value = true
  try {
    const res = await getJobLogPage({
      pageNo: logQuery.pageNo,
      pageSize: logQuery.pageSize,
      jobName: logFilter.value?.jobName,
      jobGroup: logFilter.value?.jobGroup,
      status: logStatusFilter.value === 'all' ? undefined : Number(logStatusFilter.value),
    })
    logData.value = res.data?.list ?? []
    logTotal.value = res.data?.total ?? 0
  } finally {
    logLoading.value = false
  }
}

function showLogDetail(row: SysJobLog) {
  logDetail.value = row
  logDetailVisible.value = true
}

function formatDuration(row: SysJobLog) {
  if (!row.startTime || !row.stopTime) return '-'
  const ms = new Date(row.stopTime).getTime() - new Date(row.startTime).getTime()
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

function onLogClosed() {
  loadOverview()
}

async function handleCleanLogs() {
  await ElMessageBox.confirm('确定清空全部调度日志？', '警告', { type: 'warning' })
  await cleanJobLogs()
  ElMessage.success('已清空')
  loadOverview()
}

onMounted(async () => {
  await Promise.all([loadOverview(), loadTemplates(), loadPage()])
  window.addEventListener('resize', updateCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateCharts)
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped>
.job-page {
  padding-bottom: 24px;
}

.job-hero {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px 28px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, #374151 100%);
  color: #fff;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
}

.hero-desc {
  margin: 0 0 12px;
  font-size: 13px;
  opacity: 0.85;
  max-width: 480px;
}

.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.hero-stats {
  flex: 1;
  min-width: 280px;
  max-width: 520px;
}

.hero-stat {
  text-align: center;
  padding: 12px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
}

.hero-stat-value {
  font-size: 24px;
  font-weight: 700;
}

.hero-stat-label {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 4px;
}

.hero-stat.success .hero-stat-value { color: #95de64; }
.hero-stat.fail .hero-stat-value { color: #ff9c9c; }
.hero-stat.rate .hero-stat-value { color: #91d5ff; }

.charts-row, .template-card, .table-card {
  margin-bottom: 16px;
}

.chart-card :deep(.el-card__header) {
  padding: 12px 16px;
  font-weight: 600;
}

.chart-box {
  height: 240px;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.header-tip {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.template-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.template-item:hover {
  border-color: var(--theme-primary, #111827);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.template-icon {
  font-size: 28px;
  line-height: 1;
  flex-shrink: 0;
}

.template-name {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}

.template-desc {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
  margin-bottom: 8px;
}

.template-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.template-cron {
  font-size: 11px;
  color: #909399;
  font-family: monospace;
}

.search-form { margin-bottom: 4px; }

.table-toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.job-name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.job-name { font-weight: 600; }

.cron-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: center;
}

.cron-code {
  font-size: 12px;
  color: #374151;
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
}

.cron-hint {
  font-size: 11px;
  color: #909399;
}

.next-time {
  font-size: 12px;
  color: #52c41a;
}

.text-muted { color: #c0c4cc; }

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.cron-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.cron-row .el-input { flex: 1; }

.cron-preview {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: #52c41a;
}

.preview-times {
  color: #909399;
  margin-left: 8px;
}

.log-filter {
  margin-bottom: 12px;
}

.log-drawer-body {
  display: flex;
  flex-direction: column;
  height: 100%;
}

:deep(.el-drawer__body) {
  display: flex;
  flex-direction: column;
  padding-top: 0;
}

.log-table {
  flex: 1;
  width: 100%;
}

.log-table :deep(.el-table__body-wrapper) {
  width: 100%;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  margin: 0;
}

.exception-pre {
  margin: 0;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
