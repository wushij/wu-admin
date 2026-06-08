import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { testSms, getRecentSmsLogs, getSmsLogs } from '@/api/system/config'
import { getErrorMessage } from '@/utils/axiosError'
import type { SmsLogRecord } from '@/types/config'

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
    } catch { recentSmsLogs.value = [] }
  }

  async function handleTestSms() {
    if (isDirty()) { ElMessage.warning('请先保存短信配置，再发送测试短信'); return }
    if (!testSmsPhone.value) { ElMessage.warning('请输入手机号'); return }
    if (!/^1[3-9]\d{9}$/.test(testSmsPhone.value)) { ElMessage.warning('请输入正确的手机号格式'); return }
    smsTesting.value = true
    try {
      const templateCode = getProvider() === 'aliyunAuth' ? testSmsTemplate.value : undefined
      await testSms(testSmsPhone.value, templateCode)
      ElMessage.success('测试短信发送成功')
      await loadRecentSmsLogs()
    } catch { await loadRecentSmsLogs() }
    finally { smsTesting.value = false }
  }

  function handleShowAllSmsLogs() {
    showSmsLogsModal.value = true
    smsLogsPagination.page = 1
  }

  async function loadSmsLogs() {
    smsLogsLoading.value = true
    try {
      const res = await getSmsLogs({
        page: smsLogsPagination.page, size: smsLogsPagination.size,
        phone: smsLogsSearch.phone || undefined, status: smsLogsSearch.status,
      })
      smsLogsData.value = res.data?.list || []
      smsLogsPagination.total = res.data?.total || 0
    } catch (e) { ElMessage.error(getErrorMessage(e) || '加载短信记录失败') }
    finally { smsLogsLoading.value = false }
  }

  function handleSearchSmsLogs() { smsLogsPagination.page = 1; loadSmsLogs() }
  function handleResetSmsLogsSearch() {
    smsLogsSearch.phone = ''; smsLogsSearch.status = null
    smsLogsPagination.page = 1; loadSmsLogs()
  }
  function handleSmsLogsSizeChange() { smsLogsPagination.page = 1; loadSmsLogs() }

  function syncTemplateFromConfig(templateVerifyCode?: string) {
    if (templateVerifyCode) testSmsTemplate.value = templateVerifyCode
  }

  return {
    smsTesting, testSmsPhone, testSmsTemplate, recentSmsLogs,
    showSmsLogsModal, smsLogsLoading, smsLogsData, smsLogsPagination, smsLogsSearch,
    smsStatusText, smsStatusTagType,
    loadRecentSmsLogs, handleTestSms, handleShowAllSmsLogs,
    loadSmsLogs, handleSearchSmsLogs, handleResetSmsLogsSearch, handleSmsLogsSizeChange,
    syncTemplateFromConfig,
  }
}
