import { ref, computed } from 'vue'
import {
  getDashboardStats,
  getRecentLogins,
  recordVisit as apiRecordVisit,
  type RecentLogin,
  type DashboardStats,
} from '@/api/dashboard'
import type { DashboardTrends } from '../constants/statCards'

const defaultStats = (): DashboardStats => ({
  userCount: 0,
  roleCount: 0,
  menuCount: 0,
  deptCount: 0,
  postCount: 0,
  onlineCount: 0,
  todayVisits: 0,
  yesterdayVisits: 0,
  userPendingCount: 0,
  userDisabledCount: 0,
  todayLoginSuccess: 0,
  todayLoginFail: 0,
  fileCount: 0,
  fileMaxSizeMb: 50,
  fileAllowedExtensions: '',
  tokenExpireHours: 24,
  loginCaptchaEnabled: true,
  loginCaptchaType: 'image',
  loginRememberMe: true,
  loginMaxRetryCount: 5,
  loginLockTimeMinutes: 10,
  registerEnabled: true,
  registerNeedAudit: false,
  ticketOpenCount: 0,
  ticketOverdueCount: 0,
  approvalPendingCount: 0,
})

function captchaTypeLabel(type: string | undefined) {
  if (type === 'slider') return '滑块'
  if (type === 'sms') return '短信'
  if (type === 'image') return '图形'
  return type || '-'
}

export function useDashboardData() {
  const platformName = ref('Admin Platform')
  const platformSubtitle = ref('')
  const stats = ref<DashboardStats>(defaultStats())
  const recentLogins = ref<RecentLogin[]>([])
  const trends = ref<DashboardTrends>({ user: 0, role: 0, menu: 0, dept: 0 })

  const systemMetaList = computed(() => {
    const s = stats.value
    const items: Array<{ label: string; value: string; tag?: string }> = [
      { label: '平台名称', value: platformName.value || '—' },
      { label: '在线用户', value: `${s.onlineCount ?? 0} 人` },
      { label: '今日访问', value: `${s.todayVisits ?? 0} 次` },
      { label: '会话有效期', value: `${s.tokenExpireHours ?? 24} 小时` },
      {
        label: '登录验证码',
        value: s.loginCaptchaEnabled ? captchaTypeLabel(s.loginCaptchaType) : '未启用',
        tag: s.loginCaptchaEnabled ? 'info' : undefined,
      },
      {
        label: '注册审核',
        value: s.registerNeedAudit ? '需审核' : '免审核',
        tag: s.registerNeedAudit ? 'warning' : 'success',
      },
      { label: '上传限制', value: `单文件 ≤ ${s.fileMaxSizeMb ?? 50}MB` },
      { label: '文件数量', value: `${s.fileCount ?? 0} 个` },
    ]
    if (s.registerNeedAudit && (s.userPendingCount ?? 0) > 0) {
      items.splice(2, 0, {
        label: '待审注册',
        value: `${s.userPendingCount} 人`,
        tag: 'warning',
      })
    }
    return items
  })

  const showBizAlerts = computed(
    () => (stats.value.ticketOpenCount ?? 0) > 0 || (stats.value.approvalPendingCount ?? 0) > 0,
  )

  async function loadStats() {
    try {
      const res = await getDashboardStats()
      const data = res.data || {}
      stats.value = { ...stats.value, ...data }
      if (data.platformName) platformName.value = data.platformName
      if (data.platformSubtitle) platformSubtitle.value = data.platformSubtitle
      trends.value.user = data.userTrend ?? 0
      trends.value.role = data.roleTrend ?? 0
      trends.value.dept = data.deptTrend ?? 0
      trends.value.menu = data.menuTrend ?? 0
    } catch (error) {
      console.error('获取统计数据失败', error)
    }
  }

  async function loadRecentLogins() {
    try {
      const res = await getRecentLogins()
      recentLogins.value = res.data || []
    } catch (e) {
      console.error(e)
    }
  }

  async function recordVisit() {
    try {
      await apiRecordVisit()
    } catch (error) {
      console.error('记录访问失败', error)
    }
  }

  async function initDashboard() {
    await Promise.all([loadStats(), loadRecentLogins(), recordVisit()])
  }

  return {
    platformName,
    platformSubtitle,
    stats,
    trends,
    recentLogins,
    systemMetaList,
    showBizAlerts,
    initDashboard,
  }
}
