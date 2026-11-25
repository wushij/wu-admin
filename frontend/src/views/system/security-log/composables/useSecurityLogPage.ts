import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getSecurityEvents,
  getSecurityStats,
  getRiskSubjects,
  getRiskSubjectEvents,
  unblockRiskSubject,
  blockRiskSubject,
  deleteSecurityEvent,
  clearSecurityEvents,
  type SecurityEventVO,
  type SecurityEventPageQuery,
  type RiskSubjectVO,
  type RiskSubjectPageQuery,
  type SecurityStatsVO,
} from '@/api/system/security'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
import { formatPrincipalDisplayName } from '../utils/securityLogFormatters'

export function useSecurityLogPage() {
  const activeTab = ref('events')

  const stats = reactive<SecurityStatsVO>({
    todayTotal: 0,
    todayBlocked: 0,
    todayForbidden: 0,
    todayWarning: 0,
    todayChallenge: 0,
    todayUaBlock: 0,
    todayReplayBlock: 0,
    todayHoneypot: 0,
    todayPermissionDenied: 0,
    activeBlockedCount: 0,
    activeRiskCount: 0,
    riskLevelDistribution: {},
    topRiskSubjects: [],
  })

  const queryParams = reactive<SecurityEventPageQuery>({
    pageNo: 1,
    pageSize: 10,
    ipaddr: '',
    eventType: '',
    action: '',
  })

  const eventLoading = ref(false)
  const eventList = ref<SecurityEventVO[]>([])
  const eventTotal = ref(0)

  const riskLoading = ref(false)
  const riskList = ref<RiskSubjectVO[]>([])
  const riskTotal = ref(0)
  const riskQuery = reactive<RiskSubjectPageQuery>({
    pageNo: 1,
    pageSize: 10,
    keyword: '',
    level: undefined,
    blocked: undefined,
  })

  const subjectEventsVisible = ref(false)
  const subjectEventsLoading = ref(false)
  const subjectEvents = ref<SecurityEventVO[]>([])
  const activeSubject = ref<RiskSubjectVO | null>(null)

  const detailVisible = ref(false)
  const activeEvent = ref<SecurityEventVO | null>(null)

  const blockDialogVisible = ref(false)
  const blockForm = reactive({
    ip: '',
    duration: 3600,
    reason: '管理员手动封禁',
  })

  async function loadStats() {
    try {
      const res = await getSecurityStats()
      if (res.data) Object.assign(stats, res.data)
    } catch {
      /* ignore */
    }
  }

  async function loadEventData() {
    eventLoading.value = true
    try {
      const res = await getSecurityEvents(queryParams)
      if (res.data) {
        eventList.value = res.data.list || []
        eventTotal.value = res.data.total || 0
      }
    } catch (err) {
      console.error('加载安全事件失败', err)
    } finally {
      eventLoading.value = false
    }
  }

  async function loadRiskData() {
    riskLoading.value = true
    try {
      const res = await getRiskSubjects(riskQuery)
      if (res.data) {
        riskList.value = res.data.list || []
        riskTotal.value = res.data.total || 0
      }
    } catch (err) {
      console.error('加载风险主体失败', err)
    } finally {
      riskLoading.value = false
    }
  }

  function handleRiskQuery() {
    riskQuery.pageNo = 1
    loadRiskData()
  }

  function resetRiskQuery() {
    riskQuery.keyword = ''
    riskQuery.level = undefined
    riskQuery.blocked = undefined
    riskQuery.pageNo = 1
    loadRiskData()
  }

  async function showSubjectEvents(row: RiskSubjectVO) {
    activeSubject.value = row
    subjectEvents.value = []
    subjectEventsVisible.value = true
    subjectEventsLoading.value = true
    try {
      const res = await getRiskSubjectEvents(row.principal, 50)
      if (res.data) subjectEvents.value = res.data || []
    } catch (err) {
      console.error('加载主体事件失败', err)
    } finally {
      subjectEventsLoading.value = false
    }
  }

  function handleTabChange(tab: string | number) {
    if (tab === 'events') loadEventData()
    else if (tab === 'risk') loadRiskData()
  }

  function handleQuery() {
    queryParams.pageNo = 1
    loadEventData()
  }

  function resetQuery() {
    queryParams.ipaddr = ''
    queryParams.eventType = ''
    queryParams.action = ''
    queryParams.pageNo = 1
    loadEventData()
  }

  function filterByIp(ip: string) {
    queryParams.ipaddr = ip
    handleQuery()
  }

  function showDetail(row: SecurityEventVO) {
    activeEvent.value = row
    detailVisible.value = true
  }

  async function handleUnblock(principal: string, row?: RiskSubjectVO) {
    const displayName = row ? formatPrincipalDisplayName(row) : principal
    await openAlertDialog({
      title: '解除主体封禁',
      targetName: displayName,
      message: '解除封禁后将立即清零该主体的窗口风险积分，并恢复其接口访问权限。',
      severity: 'warning',
      confirmText: '立即解封',
      onConfirm: async () => {
        await unblockRiskSubject(principal, true)
        ElMessage.success(`【${displayName}】已成功解封并清零风险分`)
        loadRiskData()
        loadStats()
      },
    })
  }

  function quickBlockIp(ip: string) {
    blockForm.ip = ip
    blockForm.duration = 3600
    blockForm.reason = '管理员从事件明细中手动拉黑'
    blockDialogVisible.value = true
  }

  function openManualBlockDialog() {
    blockForm.ip = ''
    blockForm.duration = 3600
    blockForm.reason = '管理员手动封禁'
    blockDialogVisible.value = true
  }

  async function confirmManualBlock() {
    if (!blockForm.ip.trim()) {
      ElMessage.warning('请输入待封禁的 IP 地址')
      return
    }
    try {
      await blockRiskSubject(blockForm.ip.trim(), blockForm.duration, blockForm.reason)
      ElMessage.success(`已成功将 IP【${blockForm.ip}】加入封禁黑名单`)
      blockDialogVisible.value = false
      loadRiskData()
      loadStats()
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '封禁操作失败'
      ElMessage.error(message)
    }
  }

  async function handleDeleteEvent(row: SecurityEventVO) {
    await openAlertDialog({
      title: '永久删除安全事件记录',
      targetName: `${row.ipaddr} · ${row.eventType}`,
      message: '删除后该条安全审计事件将被永久清除，无法追溯或还原。',
      severity: 'irreversible',
      confirmText: '永久删除',
      onConfirm: async () => {
        await deleteSecurityEvent(row.id)
        ElMessage.success('已永久删除安全事件')
        loadEventData()
        loadStats()
      },
    })
  }

  async function handleClearEvents() {
    await openAlertDialog({
      title: '清空全部安全拦截日志',
      targetName: `共 ${eventTotal.value} 条审计事件`,
      message: '此操作将永久清空全部安全拦截审计记录且无法找回，建议确认已完成安全归档。',
      severity: 'irreversible',
      confirmText: '清空全部日志',
      requireConfirmInput: '清空日志',
      onConfirm: async () => {
        await clearSecurityEvents()
        ElMessage.success('安全事件日志已全部清空')
        loadEventData()
        loadStats()
      },
    })
  }

  onMounted(() => {
    loadStats()
    loadEventData()
  })

  return {
    activeTab,
    stats,
    queryParams,
    eventLoading,
    eventList,
    eventTotal,
    riskLoading,
    riskList,
    riskTotal,
    riskQuery,
    subjectEventsVisible,
    subjectEventsLoading,
    subjectEvents,
    activeSubject,
    detailVisible,
    activeEvent,
    blockDialogVisible,
    blockForm,
    loadEventData,
    loadRiskData,
    handleRiskQuery,
    resetRiskQuery,
    showSubjectEvents,
    handleTabChange,
    handleQuery,
    resetQuery,
    filterByIp,
    showDetail,
    handleUnblock,
    quickBlockIp,
    openManualBlockDialog,
    confirmManualBlock,
    handleDeleteEvent,
    handleClearEvents,
  }
}
