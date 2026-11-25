import { ref, reactive, computed, onMounted, onUnmounted, nextTick, type Ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
import { showSnackbar } from '@/composables/ui/useSnackbar'
import type { EChartsType } from 'echarts'
import {
  getJobOverview,
  getJobTemplates,
  checkJobCron,
  getJobPage,
  createJob,
  updateJob,
  deleteJob,
  restoreJob,
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
import { readChartTheme, useChartThemeObserver } from '@/utils/chartTheme'

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
    const theme = readChartTheme()
    const execTotal = (overview.successCount ?? 0) + (overview.failCount ?? 0)

    if (pieChartRef.value) {
      if (!pieChart || pieChart.isDisposed()) {
        pieChart = echarts.init(pieChartRef.value)
      }
      if (execTotal > 0) {
        pieChart.setOption(
          {
            tooltip: {
              trigger: 'item',
              backgroundColor: theme.elevatedBg,
              borderColor: theme.border,
              textStyle: { color: theme.textPrimary },
            },
            color: [theme.palette.indigo, theme.palette.rose],
            series: [
              {
                type: 'pie',
                radius: ['48%', '74%'],
                itemStyle: {
                  borderColor: theme.surfaceBg,
                  borderWidth: 2,
                  borderRadius: 4,
                },
                label: {
                  color: theme.textSecondary,
                },
                labelLine: {
                  lineStyle: { color: theme.border },
                },
                data: [
                  { name: '成功', value: overview.successCount },
                  { name: '失败', value: overview.failCount },
                ],
              },
            ],
          },
          true,
        )
      } else {
        pieChart.setOption(
          {
            tooltip: { show: false },
            color: [theme.border],
            series: [
              {
                type: 'pie',
                radius: ['52%', '70%'],
                silent: true,
                label: {
                  show: true,
                  position: 'center',
                  formatter: '暂无执行记录',
                  color: theme.textMuted,
                  fontSize: 13,
                  fontWeight: 500,
                },
                labelLine: { show: false },
                itemStyle: { color: theme.border },
                data: [{ name: '暂无数据', value: 1 }],
              },
            ],
          },
          true,
        )
      }
      pieChart.resize()
    }

    if (barChartRef.value) {
      if (!barChart || barChart.isDisposed()) {
        barChart = echarts.init(barChartRef.value)
      }
      const daily = overview.dailyStats ?? []
      barChart.setOption(
        {
          tooltip: {
            trigger: 'axis',
            backgroundColor: theme.elevatedBg,
            borderColor: theme.border,
            textStyle: { color: theme.textPrimary },
          },
          legend: {
            data: ['成功', '失败'],
            textStyle: { color: theme.textSecondary },
          },
          grid: { left: 40, right: 16, bottom: 28, top: 32 },
          xAxis: {
            type: 'category',
            data: daily.map((d) => String(d.exec_date ?? d.execDate ?? '')),
            axisLabel: { color: theme.textMuted },
            axisLine: { lineStyle: { color: theme.border } },
            axisTick: { show: false },
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            axisLabel: { color: theme.textMuted },
            splitLine: { lineStyle: { color: theme.borderSubtle, type: 'dashed' } },
          },
          series: [
            {
              name: '成功',
              type: 'bar',
              stack: 'total',
              barMaxWidth: 28,
              data: daily.map((d) => readDailyField(d, 'success_count', 'successCount')),
              itemStyle: { color: theme.palette.indigo, borderRadius: [4, 4, 0, 0] },
            },
            {
              name: '失败',
              type: 'bar',
              stack: 'total',
              barMaxWidth: 28,
              data: daily.map((d) => readDailyField(d, 'fail_count', 'failCount')),
              itemStyle: { color: theme.palette.rose, borderRadius: [4, 4, 0, 0] },
            },
          ],
        },
        true,
      )
      barChart.resize()
    }
  }

  useChartThemeObserver(() => updateCharts())

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
    await openAlertDialog({
      title: '从模板创建定时任务',
      targetName: `${tpl.name} (${tpl.cronExpression})`,
      message: `将使用调用目标「${tpl.invokeTarget}」创建新任务，默认处于暂停状态。`,
      severity: 'warning',
      confirmText: '确认创建',
      onConfirm: async () => {
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
      },
    })
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
    await deleteJob(row.id!)
    await refreshAll()
    showSnackbar({
      message: `已将任务「${row.jobName}」移至回收中心`,
      variant: 'info',
      actionLabel: '撤销',
      onAction: async () => {
        await restoreJob(row.id!)
        ElMessage.success(`已恢复任务「${row.jobName}」`)
        await refreshAll()
      },
    })
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
    if (row.durationMs != null && row.durationMs >= 0) {
      return formatDurationMs(row.durationMs)
    }
    if (!row.startTime || !row.stopTime) return '-'
    const ms = new Date(row.stopTime).getTime() - new Date(row.startTime).getTime()
    return formatDurationMs(ms)
  }

  function formatDurationMs(ms: number) {
    if (Number.isNaN(ms) || ms < 0) return '-'
    if (ms === 0) return '<1s'
    if (ms < 1000) return `${ms}ms`
    if (ms < 60_000) return `${(ms / 1000).toFixed(ms < 10_000 ? 1 : 0)}s`
    const minutes = Math.floor(ms / 60_000)
    const seconds = Math.round((ms % 60_000) / 1000)
    return seconds > 0 ? `${minutes}分${seconds}秒` : `${minutes}分`
  }

  function onLogClosed() {
    loadOverview()
  }

  async function handleCleanLogs() {
    const scoped = logFilter.value?.jobName
    await openAlertDialog({
      title: '清空全部调度日志',
      targetName: scoped ? `全部任务日志（不仅限 ${scoped}）` : '全部定时任务调度日志',
      message: '清空后所有任务的调度日志将移入回收站或清理，确定继续执行？',
      severity: 'irreversible',
      confirmText: '清空全部日志',
      onConfirm: async () => {
        await cleanJobLogs()
        ElMessage.success('全部日志已清空')
        loadOverview()
        if (logVisible.value) loadLogs()
      },
    })
  }

  async function handleCleanScopedLogs() {
    const name = logFilter.value?.jobName
    if (!name) return
    await openAlertDialog({
      title: '清空指定任务调度日志',
      targetName: name,
      message: `将清空任务「${name}」的全部调度历史记录。`,
      severity: 'irreversible',
      confirmText: '确认清空',
      onConfirm: async () => {
        await cleanJobLogs({
          jobName: logFilter.value?.jobName,
          jobGroup: logFilter.value?.jobGroup,
        })
        ElMessage.success('本任务日志已清空')
        loadOverview()
        loadLogs()
      },
    })
  }

  let themeObserver: MutationObserver | null = null

  onMounted(async () => {
    await Promise.all([loadOverview(), loadTemplates(), loadPage()])
    await nextTick()
    await updateCharts()
    window.addEventListener('resize', updateCharts)
    themeObserver = new MutationObserver(() => {
      void updateCharts()
    })
    themeObserver.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class', 'style'],
    })
  })

  onUnmounted(() => {
    window.removeEventListener('resize', updateCharts)
    themeObserver?.disconnect()
    themeObserver = null
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
    handleCleanScopedLogs,
    logFilter,
    resetForm,
  }
}
