import { ref, reactive, computed, onMounted, onUnmounted, nextTick, type Ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { EChartsType } from 'echarts'
import {
  getJobOverview,
  getJobTemplates,
  checkJobCron,
  getJobPage,
  createJob,
  updateJob,
  deleteJob,
  changeJobStatus,
  runJob,
  getJobLogPage,
  cleanJobLogs,
  type SysJob,
  type SysJobLog,
  type JobTemplate,
  type JobOverview,
} from '@/api/monitor/job'
import { cronPresets } from '../constants/cronPresets'

let echartsModule: typeof import('echarts') | null = null

export function useJobPage(chartRefs: {
  pieChartRef: Ref<HTMLElement | undefined>
  barChartRef: Ref<HTMLElement | undefined>
}) {
  const { pieChartRef, barChartRef } = chartRefs
  let pieChart: EChartsType | null = null
  let barChart: EChartsType | null = null

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
    jobName: '',
    jobGroup: 'DEFAULT',
    invokeTarget: '',
    cronExpression: '',
    misfirePolicy: 3,
    concurrent: 1,
    status: 0,
    remark: '',
  })

  const formRules: FormRules = {
    jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
    invokeTarget: [{ required: true, message: '请输入调用目标', trigger: 'blur' }],
    cronExpression: [{ required: true, message: '请输入 Cron 表达式', trigger: 'blur' }],
  }

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

  function disposeCharts() {
    pieChart?.dispose()
    barChart?.dispose()
    pieChart = null
    barChart = null
  }

  async function updateCharts() {
    await nextTick()
    const echarts = await ensureEcharts()
    const execTotal = (overview.successCount ?? 0) + (overview.failCount ?? 0)

    if (pieChartRef.value) {
      if (!pieChart || pieChart.isDisposed()) {
        pieChart = echarts.init(pieChartRef.value)
      }
      pieChart.setOption({
        tooltip: { trigger: 'item' },
        color: ['#52c41a', '#ff4d4f', '#e5e7eb'],
        series: [
          {
            type: 'pie',
            radius: ['45%', '72%'],
            data:
              execTotal > 0
                ? [
                    { name: '成功', value: overview.successCount },
                    { name: '失败', value: overview.failCount },
                  ]
                : [{ name: '暂无数据', value: 1 }],
          },
        ],
      })
      pieChart.resize()
    }

    if (barChartRef.value) {
      if (!barChart || barChart.isDisposed()) {
        barChart = echarts.init(barChartRef.value)
      }
      const daily = overview.dailyStats ?? []
      barChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['成功', '失败'] },
        grid: { left: 40, right: 16, bottom: 28, top: 32 },
        xAxis: { type: 'category', data: daily.map((d) => String(d.exec_date ?? d.execDate ?? '')) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          {
            name: '成功',
            type: 'bar',
            stack: 'total',
            data: daily.map((d) => readDailyField(d, 'success_count', 'successCount')),
            itemStyle: { color: '#52c41a' },
          },
          {
            name: '失败',
            type: 'bar',
            stack: 'total',
            data: daily.map((d) => readDailyField(d, 'fail_count', 'failCount')),
            itemStyle: { color: '#ff4d4f' },
          },
        ],
      })
      barChart.resize()
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
      id: undefined,
      jobName: '',
      jobGroup: 'DEFAULT',
      invokeTarget: '',
      cronExpression: '',
      misfirePolicy: 3,
      concurrent: 1,
      status: 0,
      remark: '',
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
      { confirmButtonText: '创建', cancelButtonText: '取消', type: 'info' }
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
    await nextTick()
    await updateCharts()
    window.addEventListener('resize', updateCharts)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', updateCharts)
    disposeCharts()
  })

  return {
    overview,
    templates,
    loading,
    tableData,
    total,
    queryParams,
    formVisible,
    formTitle,
    submitting,
    formRef,
    cronPreset,
    cronPreview,
    formData,
    formRules,
    cronPresets,
    logVisible,
    logLoading,
    logData,
    logTotal,
    logStatusFilter,
    logQuery,
    logDetailVisible,
    logDetail,
    logTitle,
    applyCronPreset,
    validateCron,
    refreshAll,
    handleSearch,
    handleReset,
    handleAdd,
    handleEdit,
    quickCreateFromTemplate,
    submitForm,
    handleDelete,
    handleStatusChange,
    handleRun,
    openAllLogs,
    openJobLogs,
    loadLogs,
    loadPage,
    showLogDetail,
    formatDuration,
    onLogClosed,
    handleCleanLogs,
    resetForm,
  }
}
