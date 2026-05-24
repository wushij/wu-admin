import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { testEmail as apiTestEmail, getRecentEmailLogs, getEmailLogs, deleteEmailLog, deleteBatchEmailLogs, cleanEmailLogs } from '@/api/system/config'
import { getErrorMessage } from '@/utils/axiosError'
import type { EmailLogRecord } from '@/types/config'

export function useEmailTest(isDirty: () => boolean) {
  const emailTesting = ref(false)
  const recentEmailLogs = ref<EmailLogRecord[]>([])
  const showEmailLogsModal = ref(false)
  const emailLogsLoading = ref(false)
  const emailLogsData = ref<EmailLogRecord[]>([])
  const emailLogsPagination = reactive({ page: 1, size: 10, total: 0 })
  const emailLogsSearch = reactive({ email: '', status: null as number | null })

  function emailStatusText(status: number) {
    if (status === 1) return '成功'
    if (status === 2) return '失败'
    return '发送中'
  }

  function emailStatusTagType(status: number): 'success' | 'danger' | 'warning' {
    if (status === 1) return 'success'
    if (status === 2) return 'danger'
    return 'warning'
  }

  async function loadRecentEmailLogs() {
    try {
      const res = await getRecentEmailLogs(5)
      recentEmailLogs.value = res.data || []
    } catch {
      recentEmailLogs.value = []
    }
  }

  async function handleTestEmail(toEmail: string) {
    if (isDirty()) {
      ElMessage.warning('配置已修改，请先保存全部后再测试发送')
      return
    }
    if (!toEmail || !toEmail.includes('@')) {
      ElMessage.warning('请输入正确的接收测试邮箱')
      return
    }
    emailTesting.value = true
    try {
      await apiTestEmail(toEmail)
      ElMessage.success('测试邮件已发送，请登录接收邮箱查收')
      await loadRecentEmailLogs()
    } catch {
      await loadRecentEmailLogs()
    } finally {
      emailTesting.value = false
    }
  }

  function handleShowAllEmailLogs() {
    showEmailLogsModal.value = true
    emailLogsPagination.page = 1
    loadEmailLogs()
  }

  async function loadEmailLogs() {
    emailLogsLoading.value = true
    try {
      const res = await getEmailLogs({
        page: emailLogsPagination.page,
        size: emailLogsPagination.size,
        email: emailLogsSearch.email || undefined,
        status: emailLogsSearch.status,
      })
      emailLogsData.value = res.data?.list || []
      emailLogsPagination.total = res.data?.total || 0
    } catch (e) {
      ElMessage.error(getErrorMessage(e) || '加载邮件记录失败')
    } finally {
      emailLogsLoading.value = false
    }
  }

  function handleSearchEmailLogs() {
    emailLogsPagination.page = 1
    loadEmailLogs()
  }

  function handleResetEmailLogsSearch() {
    emailLogsSearch.email = ''
    emailLogsSearch.status = null
    emailLogsPagination.page = 1
    loadEmailLogs()
  }

  function handleEmailLogsSizeChange() {
    emailLogsPagination.page = 1
    loadEmailLogs()
  }

  const selectedEmailLogIds = ref<(number | string)[]>([])

  function handleEmailSelectionChange(rows: EmailLogRecord[]) {
    selectedEmailLogIds.value = rows.map((r) => r.id).filter(Boolean) as (number | string)[]
  }

  async function handleDeleteEmailLog(id: number | string) {
    try {
      await deleteEmailLog(id)
      ElMessage.success('删除成功')
      await Promise.all([
        loadRecentEmailLogs(),
        showEmailLogsModal.value ? loadEmailLogs() : Promise.resolve(),
      ])
    } catch (e) {
      ElMessage.error(getErrorMessage(e) || '删除失败')
    }
  }

  async function handleBatchDeleteEmailLogs() {
    if (!selectedEmailLogIds.value.length) return
    try {
      await ElMessageBox.confirm(`确定删除选中的 ${selectedEmailLogIds.value.length} 条邮件记录吗？`, '批量删除确认', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      })
      await deleteBatchEmailLogs(selectedEmailLogIds.value)
      ElMessage.success('批量删除成功')
      selectedEmailLogIds.value = []
      await Promise.all([
        loadRecentEmailLogs(),
        loadEmailLogs(),
      ])
    } catch (e) {
      if (e !== 'cancel') {
        ElMessage.error(getErrorMessage(e) || '批量删除失败')
      }
    }
  }

  async function handleCleanEmailLogs() {
    try {
      await ElMessageBox.confirm('确定要清空全部邮件发送记录吗？该操作不可恢复！', '清空警告', {
        type: 'warning',
        confirmButtonText: '清空',
        cancelButtonText: '取消',
      })
      await cleanEmailLogs()
      ElMessage.success('已清空全部邮件记录')
      selectedEmailLogIds.value = []
      await Promise.all([
        loadRecentEmailLogs(),
        loadEmailLogs(),
      ])
    } catch (e) {
      if (e !== 'cancel') {
        ElMessage.error(getErrorMessage(e) || '清空失败')
      }
    }
  }

  return {
    emailTesting,
    recentEmailLogs,
    showEmailLogsModal,
    emailLogsLoading,
    emailLogsData,
    emailLogsPagination,
    emailLogsSearch,
    emailStatusText,
    emailStatusTagType,
    loadRecentEmailLogs,
    handleTestEmail,
    handleShowAllEmailLogs,
    loadEmailLogs,
    handleSearchEmailLogs,
    handleResetEmailLogsSearch,
    handleEmailLogsSizeChange,
    selectedEmailLogIds,
    handleEmailSelectionChange,
    handleDeleteEmailLog,
    handleBatchDeleteEmailLogs,
    handleCleanEmailLogs,
  }
}
