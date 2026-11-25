import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  testSms,
  getRecentSmsLogs,
  getSmsLogs,
  deleteSmsLog,
  deleteBatchSmsLogs,
  cleanSmsLogs,
} from '@/api/system/config'
import { getErrorMessage } from '@/utils/axiosError'
import type { SmsLogRecord } from '@/types/config'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'

export function useSmsTest(isDirty: () => boolean, getProvider: () => string) {
  const smsTesting = ref(false)
  const testSmsPhone = ref('')
  const testSmsTemplate = ref('100001')
  const recentSmsLogs = ref<SmsLogRecord[]>([])
  const showSmsLogsModal = ref(false)
  const smsLogsLoading = ref(false)
  const smsLogsData = ref<SmsLogRecord[]>([])
  const smsLogsPagination = reactive({ page: 1, size: 10, total: 0 })
  const smsLogsSearch = reactive({ phone: '', status: null as number | null })

  function smsStatusText(status: number) {
    if (status === 1) return '成功'
    if (status === 2) return '失败'
    return '发送中'
  }

  function smsStatusTagType(status: number): 'success' | 'danger' | 'warning' {
    if (status === 1) return 'success'
    if (status === 2) return 'danger'
    return 'warning'
  }

  async function loadRecentSmsLogs() {
    try {
      const res = await getRecentSmsLogs(5)
      recentSmsLogs.value = res.data || []
    } catch {
      recentSmsLogs.value = []
    }
  }

  async function handleTestSms() {
    if (isDirty()) {
      ElMessage.warning('请先保存短信配置，再发送测试短信')
      return
    }
    if (!testSmsPhone.value) {
      ElMessage.warning('请输入手机号')
      return
    }
    if (!/^1[3-9]\d{9}$/.test(testSmsPhone.value)) {
      ElMessage.warning('请输入正确的手机号格式')
      return
    }
    smsTesting.value = true
    try {
      const templateCode = getProvider() === 'aliyunAuth' ? testSmsTemplate.value : undefined
      await testSms(testSmsPhone.value, templateCode)
      ElMessage.success('测试短信发送成功')
      await loadRecentSmsLogs()
    } catch {
      await loadRecentSmsLogs()
    } finally {
      smsTesting.value = false
    }
  }

  function handleShowAllSmsLogs() {
    showSmsLogsModal.value = true
    smsLogsPagination.page = 1
  }

  async function loadSmsLogs() {
    smsLogsLoading.value = true
    try {
      const res = await getSmsLogs({
        page: smsLogsPagination.page,
        size: smsLogsPagination.size,
        phone: smsLogsSearch.phone || undefined,
        status: smsLogsSearch.status,
      })
      smsLogsData.value = res.data?.list || []
      smsLogsPagination.total = res.data?.total || 0
    } catch (e) {
      ElMessage.error(getErrorMessage(e) || '加载短信记录失败')
    } finally {
      smsLogsLoading.value = false
    }
  }

  function handleSearchSmsLogs() {
    smsLogsPagination.page = 1
    loadSmsLogs()
  }
  function handleResetSmsLogsSearch() {
    smsLogsSearch.phone = ''
    smsLogsSearch.status = null
    smsLogsPagination.page = 1
    loadSmsLogs()
  }
  function handleSmsLogsSizeChange() {
    smsLogsPagination.page = 1
    loadSmsLogs()
  }

  const selectedSmsLogIds = ref<(number | string)[]>([])

  function handleSmsSelectionChange(rows: SmsLogRecord[]) {
    selectedSmsLogIds.value = rows.map((r) => r.id).filter(Boolean) as (number | string)[]
  }

  async function handleDeleteSmsLog(id: number | string) {
    await openAlertDialog({
      title: '永久删除短信记录',
      targetName: `记录 #${id}`,
      message: '删除后该条短信发送记录将被永久清除，无法恢复。',
      severity: 'irreversible',
      confirmText: '永久删除',
      onConfirm: async () => {
        await deleteSmsLog(id)
        ElMessage.success('删除成功')
        await Promise.all([
          loadRecentSmsLogs(),
          showSmsLogsModal.value ? loadSmsLogs() : Promise.resolve(),
        ])
      },
    })
  }

  async function handleBatchDeleteSmsLogs() {
    if (!selectedSmsLogIds.value.length) return
    const count = selectedSmsLogIds.value.length
    await openAlertDialog({
      title: '批量删除短信记录',
      targetName: `选中的 ${count} 条记录`,
      message: '删除后这些短信发送记录将被永久清除且无法找回。',
      severity: 'irreversible',
      confirmText: `永久删除 (${count})`,
      onConfirm: async () => {
        await deleteBatchSmsLogs(selectedSmsLogIds.value)
        ElMessage.success('批量删除成功')
        selectedSmsLogIds.value = []
        await Promise.all([loadRecentSmsLogs(), loadSmsLogs()])
      },
    })
  }

  async function handleCleanSmsLogs() {
    await openAlertDialog({
      title: '清空全部短信发送记录',
      targetName: `共 ${smsLogsPagination.total} 条记录`,
      message: '此操作将永久清空全部短信发送日志且不可恢复。',
      severity: 'irreversible',
      confirmText: '清空全部记录',
      onConfirm: async () => {
        await cleanSmsLogs()
        ElMessage.success('已清空全部短信记录')
        selectedSmsLogIds.value = []
        await Promise.all([loadRecentSmsLogs(), loadSmsLogs()])
      },
    })
  }

  function syncTemplateFromConfig(templateVerifyCode?: string) {
    if (templateVerifyCode) testSmsTemplate.value = templateVerifyCode
  }

  return {
    smsTesting,
    testSmsPhone,
    testSmsTemplate,
    recentSmsLogs,
    showSmsLogsModal,
    smsLogsLoading,
    smsLogsData,
    smsLogsPagination,
    smsLogsSearch,
    smsStatusText,
    smsStatusTagType,
    loadRecentSmsLogs,
    handleTestSms,
    handleShowAllSmsLogs,
    loadSmsLogs,
    handleSearchSmsLogs,
    handleResetSmsLogsSearch,
    handleSmsLogsSizeChange,
    syncTemplateFromConfig,
    selectedSmsLogIds,
    handleSmsSelectionChange,
    handleDeleteSmsLog,
    handleBatchDeleteSmsLogs,
    handleCleanSmsLogs,
  }
}
