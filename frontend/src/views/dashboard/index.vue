<template>
  <div class="dashboard-container">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <div class="welcome-left">
          <div class="welcome-avatar">
            <el-icon :size="40"><Avatar /></el-icon>
          </div>
          <div class="welcome-text">
            <h1>欢迎回来，{{ userStore.userInfo.nickname || '管理员' }}</h1>
            <p class="welcome-platform">{{ platformName }} · {{ platformSubtitle }}</p>
            <p class="welcome-time">{{ currentTime }}</p>
            <p class="welcome-greeting">{{ greetingMessage }}</p>
          </div>
        </div>
        <div class="welcome-right">
          <div class="welcome-stat-item">
            <div class="stat-icon-wrapper online">
              <el-icon :size="20"><UserFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.onlineCount }}</div>
              <div class="stat-label">在线用户</div>
            </div>
          </div>
          <div class="welcome-stat-divider"></div>
          <div class="welcome-stat-item">
            <div class="stat-icon-wrapper visit">
              <el-icon :size="20"><View /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayVisits }}</div>
              <div class="stat-label">今日访问</div>
            </div>
          </div>
          <div class="welcome-stat-divider"></div>
          <div class="welcome-stat-item">
            <div class="stat-icon-wrapper login-ok">
              <el-icon :size="20"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayLoginSuccess }}</div>
              <div class="stat-label">今日登录成功</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="24" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-card-header">
            <div class="stat-icon-box user">
              <el-icon :size="24"><User /></el-icon>
            </div>
            <div class="stat-trend-badge" :class="trends.user >= 0 ? 'up' : 'down'">
              <el-icon><Top v-if="trends.user >= 0" /><Bottom v-else /></el-icon>
              <span>{{ Math.abs(trends.user) }}%</span>
            </div>
          </div>
          <div class="stat-card-body">
            <div class="stat-number">{{ stats.userCount }}</div>
            <div class="stat-title">用户总数</div>
          </div>
          <div class="stat-card-footer">
            <span>较昨日 {{ trends.user >= 0 ? '+' : '' }}{{ trends.user }}%</span>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-card-header">
            <div class="stat-icon-box role">
              <el-icon :size="24"><UserFilled /></el-icon>
            </div>
            <div class="stat-trend-badge" :class="trends.role >= 0 ? 'up' : 'down'">
              <el-icon><Top v-if="trends.role >= 0" /><Bottom v-else /></el-icon>
              <span>{{ Math.abs(trends.role) }}%</span>
            </div>
          </div>
          <div class="stat-card-body">
            <div class="stat-number">{{ stats.roleCount }}</div>
            <div class="stat-title">角色总数</div>
          </div>
          <div class="stat-card-footer">
            <span>较昨日 {{ trends.role >= 0 ? '+' : '' }}{{ trends.role }}%</span>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-card-header">
            <div class="stat-icon-box menu">
              <el-icon :size="24"><Menu /></el-icon>
            </div>
            <div class="stat-trend-badge" :class="trends.menu >= 0 ? 'up' : 'down'">
              <el-icon><Top v-if="trends.menu >= 0" /><Bottom v-else /></el-icon>
              <span>{{ Math.abs(trends.menu) }}%</span>
            </div>
          </div>
          <div class="stat-card-body">
            <div class="stat-number">{{ stats.menuCount }}</div>
            <div class="stat-title">菜单总数</div>
          </div>
          <div class="stat-card-footer">
            <span>{{ trends.menu === 0 ? '与昨日持平' : `较昨日 ${trends.menu >= 0 ? '+' : ''}${trends.menu}%` }}</span>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-card-header">
            <div class="stat-icon-box dept">
              <el-icon :size="24"><OfficeBuilding /></el-icon>
            </div>
            <div class="stat-trend-badge" :class="trends.dept >= 0 ? 'up' : 'down'">
              <el-icon><Top v-if="trends.dept >= 0" /><Bottom v-else /></el-icon>
              <span>{{ Math.abs(trends.dept) }}%</span>
            </div>
          </div>
          <div class="stat-card-body">
            <div class="stat-number">{{ stats.deptCount }}</div>
            <div class="stat-title">部门总数</div>
          </div>
          <div class="stat-card-footer">
            <span>较昨日 {{ trends.dept >= 0 ? '+' : '' }}{{ trends.dept }}%</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24" class="stats-row ops-stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div
          class="stat-card ops clickable"
          v-permission="'system:approval:list'"
          @click="$router.push({ path: '/system/approval', query: { formType: 'REGISTER', status: 'SUBMITTED' } })"
        >
          <div class="stat-card-header">
            <div class="stat-icon-box pending-user">
              <el-icon :size="24"><User /></el-icon>
            </div>
          </div>
          <div class="stat-card-body">
            <div class="stat-number">{{ stats.userPendingCount }}</div>
            <div class="stat-title">待审核用户</div>
          </div>
          <div class="stat-card-footer"><span>注册审核</span></div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card ops clickable" @click="$router.push('/system/login-log')">
          <div class="stat-card-header">
            <div class="stat-icon-box login-success">
              <el-icon :size="24"><CircleCheck /></el-icon>
            </div>
          </div>
          <div class="stat-card-body">
            <div class="stat-number">{{ stats.todayLoginSuccess }}</div>
            <div class="stat-title">今日登录成功</div>
          </div>
          <div class="stat-card-footer">
            <span>失败 {{ stats.todayLoginFail }} 次</span>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card ops clickable" @click="$router.push('/system/file')">
          <div class="stat-card-header">
            <div class="stat-icon-box file-store">
              <el-icon :size="24"><FolderOpened /></el-icon>
            </div>
          </div>
          <div class="stat-card-body">
            <div class="stat-number">{{ stats.fileCount }}</div>
            <div class="stat-title">文件存储</div>
          </div>
          <div class="stat-card-footer"><span>单文件上限 {{ stats.fileMaxSizeMb }}MB</span></div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card ops clickable" @click="$router.push('/system/org')">
          <div class="stat-card-header">
            <div class="stat-icon-box post">
              <el-icon :size="24"><Briefcase /></el-icon>
            </div>
          </div>
          <div class="stat-card-body">
            <div class="stat-number">{{ stats.postCount }}</div>
            <div class="stat-title">岗位数</div>
          </div>
          <div class="stat-card-footer"><span>组织管理 · 部门 {{ stats.deptCount }}</span></div>
        </div>
      </el-col>
    </el-row>

    <el-row
      v-if="(stats.ticketOpenCount ?? 0) > 0 || (stats.approvalPendingCount ?? 0) > 0"
      :gutter="24"
      class="stats-row biz-stats-row"
    >
      <el-col v-if="(stats.ticketOpenCount ?? 0) > 0 || (stats.ticketOverdueCount ?? 0) > 0" :xs="24" :sm="12" :lg="6">
        <div
          v-permission="'system:ticket:list'"
          class="stat-card biz clickable"
          @click="$router.push('/system/ticket')"
        >
          <div class="stat-card-body inline-biz">
            <span class="biz-label">待处理工单</span>
            <span class="biz-value warn">{{ stats.ticketOpenCount }}</span>
          </div>
        </div>
      </el-col>
      <el-col v-if="(stats.ticketOverdueCount ?? 0) > 0" :xs="24" :sm="12" :lg="6">
        <div
          v-permission="'system:ticket:list'"
          class="stat-card biz clickable"
          @click="$router.push('/system/ticket')"
        >
          <div class="stat-card-body inline-biz">
            <span class="biz-label">超时工单</span>
            <span class="biz-value danger">{{ stats.ticketOverdueCount }}</span>
          </div>
        </div>
      </el-col>
      <el-col v-if="(stats.approvalPendingCount ?? 0) > 0" :xs="24" :sm="12" :lg="6">
        <div
          v-permission="'system:approval:list'"
          class="stat-card biz clickable"
          @click="$router.push('/system/approval')"
        >
          <div class="stat-card-body inline-biz">
            <span class="biz-label">待审批</span>
            <span class="biz-value">{{ stats.approvalPendingCount }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷入口和系统信息 -->
    <el-row :gutter="24" class="content-row">
      <el-col :xs="24" :lg="16">
        <div class="section-card">
          <div class="section-header">
            <div class="section-title-wrapper">
              <div class="section-icon">
                <el-icon :size="18"><Grid /></el-icon>
              </div>
              <span class="section-title">快捷入口</span>
            </div>
            <el-tag type="info" size="small" effect="plain">常用功能</el-tag>
          </div>
          <div class="quick-grid">
            <div
              v-for="item in quickEntries"
              :key="item.key"
              v-permission="item.permission"
              class="quick-item"
              @click="goQuick(item)"
            >
              <div class="quick-icon-wrapper" :class="item.theme">
                <el-icon :size="26"><component :is="item.icon" /></el-icon>
              </div>
              <div class="quick-info">
                <div class="quick-name">{{ item.name }}</div>
                <div class="quick-desc">{{ item.desc }}</div>
              </div>
              <el-icon class="quick-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="24" :lg="8">
        <div class="section-card">
          <div class="section-header">
            <div class="section-title-wrapper">
              <div class="section-icon">
                <el-icon :size="18"><InfoFilled /></el-icon>
              </div>
              <span class="section-title">系统信息</span>
            </div>
            <el-tag type="success" size="small" effect="plain">
              <el-icon style="margin-right: 4px;"><CircleCheck /></el-icon>
              运行正常
            </el-tag>
          </div>
          <p v-if="platformSubtitle" class="sys-subtitle">{{ platformSubtitle }}</p>
          <ul class="sys-meta-list">
            <li v-for="item in systemMetaList" :key="item.label" class="sys-meta-item">
              <span class="sys-meta-label">{{ item.label }}</span>
              <span class="sys-meta-value">
                <el-tag v-if="item.tag" :type="item.tag as 'success' | 'primary' | 'warning' | 'info' | 'danger'" size="small" effect="plain">{{ item.value }}</el-tag>
                <template v-else>{{ item.value }}</template>
              </span>
            </li>
          </ul>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24" class="content-row">
      <el-col :span="24">
        <div class="section-card">
          <div class="section-header">
            <div class="section-title-wrapper">
              <div class="section-icon">
                <el-icon :size="18"><Promotion /></el-icon>
              </div>
              <span class="section-title">最近登录</span>
            </div>
            <el-button link type="primary" @click="$router.push('/system/login-log')">查看全部</el-button>
          </div>
          <el-table :data="recentLogins" size="small" stripe empty-text="暂无登录记录">
            <el-table-column prop="username" label="用户" width="120" />
            <el-table-column prop="ipaddr" label="IP" width="140" />
            <el-table-column prop="loginLocation" label="地点" min-width="120" show-overflow-tooltip />
            <el-table-column prop="browser" label="浏览器" width="100" show-overflow-tooltip />
            <el-table-column label="结果" width="88">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
                  {{ row.status === 0 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="170">
              <template #default="{ row }">
                {{ formatLoginTime(row.loginTime) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getDashboardStats, getRecentLogins, recordVisit as apiRecordVisit, type RecentLogin, type DashboardStats } from '@/api/dashboard'
import {
  User,
  UserFilled,
  Menu,
  Collection,
  OfficeBuilding,
  Promotion,
  Grid,
  Top,
  Bottom,
  Avatar,
  View,
  ArrowRight,
  InfoFilled,
  CircleCheck,
  Tickets,
  Checked,
  FolderOpened,
  Tools,
  Monitor,
  ChatDotRound,
  Timer,
  Notification,
  EditPen
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

interface QuickEntry {
  key: string
  name: string
  desc: string
  path: string
  permission: string
  icon: typeof User
  theme: string
  query?: Record<string, string>
}

/** 工作台固定 12 个核心快捷入口（3×4）— RBAC 权限 + 业务协同 + 运维监控 */
const quickEntries: QuickEntry[] = [
  { key: 'user', name: '用户管理', desc: '账号与状态维护', path: '/system/user', permission: 'system:user:list', icon: User, theme: 'user' },
  { key: 'role', name: '角色管理', desc: '配置角色权限', path: '/system/role', permission: 'system:role:list', icon: UserFilled, theme: 'role' },
  { key: 'menu', name: '菜单管理', desc: '目录菜单按钮', path: '/system/menu', permission: 'system:menu:list', icon: Menu, theme: 'menu' },
  { key: 'dict', name: '字典管理', desc: '业务枚举维护', path: '/system/dict', permission: 'system:dict:list', icon: Collection, theme: 'dict' },
  { key: 'config', name: '系统配置', desc: '登录注册与会话', path: '/system/config', permission: 'system:config:list', icon: Tools, theme: 'config' },
  { key: 'approval', name: '审批单中心', desc: '流程单审批归档', path: '/system/approval', permission: 'system:approval:list', icon: Checked, theme: 'approval' },
  { key: 'ticket', name: '工单管理', desc: '处理跟踪工单', path: '/system/ticket', permission: 'system:ticket:list', icon: Tickets, theme: 'ticket' },
  {
    key: 'chat',
    name: '即时聊天',
    desc: '私聊与群聊消息',
    path: '/message/chat',
    permission: 'system:chat:list',
    icon: ChatDotRound,
    theme: 'chat'
  },
  {
    key: 'notice',
    name: '系统通知',
    desc: '公告与消息推送',
    path: '/message/notice',
    permission: 'system:announce:list',
    icon: Notification,
    theme: 'notice'
  },
  {
    key: 'job',
    name: '定时任务',
    desc: '调度与日志清理',
    path: '/monitor/job',
    permission: 'monitor:job:list',
    icon: Timer,
    theme: 'job'
  },
  { key: 'online', name: '在线用户', desc: '会话与强退', path: '/monitor/online', permission: 'monitor:online:list', icon: Monitor, theme: 'monitor' },
  {
    key: 'oper-log',
    name: '操作日志',
    desc: '行为审计追溯',
    path: '/system/oper-log',
    permission: 'system:operLog:list',
    icon: EditPen,
    theme: 'log'
  }
]

const goQuick = (item: QuickEntry) => {
  if (item.query) {
    router.push({ path: item.path, query: item.query })
  } else {
    router.push(item.path)
  }
}

// 问候语
const greetingMessage = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了，注意休息'
  if (hour < 9) return '早上好，开启美好的一天'
  if (hour < 12) return '上午好，工作顺利'
  if (hour < 14) return '中午好，记得休息'
  if (hour < 18) return '下午好，继续加油'
  if (hour < 22) return '晚上好，辛苦了'
  return '夜深了，早点休息'
})

const captchaTypeLabel = (type: string | undefined) => {
  if (type === 'slider') return '滑块'
  if (type === 'sms') return '短信'
  if (type === 'image') return '图形'
  return type || '-'
}

/** 来自工作台统计与系统配置的运行概况（非写死技术栈） */
const systemMetaList = computed(() => {
  const s = stats.value
  const items = [
    { label: '平台名称', value: platformName.value || '—' },
    { label: '在线用户', value: `${s.onlineCount ?? 0} 人` },
    { label: '今日访问', value: `${s.todayVisits ?? 0} 次` },
    { label: '会话有效期', value: `${s.tokenExpireHours ?? 24} 小时` },
    {
      label: '登录验证码',
      value: s.loginCaptchaEnabled ? captchaTypeLabel(s.loginCaptchaType) : '未启用',
      tag: s.loginCaptchaEnabled ? 'info' : undefined
    },
    {
      label: '注册审核',
      value: s.registerNeedAudit ? '需审核' : '免审核',
      tag: s.registerNeedAudit ? 'warning' : 'success'
    },
    { label: '上传限制', value: `单文件 ≤ ${s.fileMaxSizeMb ?? 50}MB` },
    { label: '文件数量', value: `${s.fileCount ?? 0} 个` }
  ]
  if (s.registerNeedAudit && (s.userPendingCount ?? 0) > 0) {
    items.splice(2, 0, {
      label: '待审注册',
      value: `${s.userPendingCount} 人`,
      tag: 'warning'
    })
  }
  return items
})

const platformName = ref('Admin Platform')
const platformSubtitle = ref('')

const stats = ref<DashboardStats>({
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
  approvalPendingCount: 0
})

const recentLogins = ref<RecentLogin[]>([])

// 增长趋势
const trends = ref({
  user: 0,
  role: 0,
  menu: 0,
  dept: 0
})

// 当前时间
const currentTime = ref('')
let timeTimer: ReturnType<typeof setInterval> | null = null

const updateTime = () => {
  const now = new Date()
  const options = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  } as const
  currentTime.value = now.toLocaleString('zh-CN', options)
}

// 获取统计数据
function formatLoginTime(t: string | undefined) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

const loadRecentLogins = async () => {
  try {
    const res = await getRecentLogins()
    recentLogins.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const getStats = async () => {
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

// 记录访问
const recordVisit = async () => {
  try {
    await apiRecordVisit()
  } catch (error) {
    console.error('记录访问失败', error)
  }
}

onMounted(() => {
  getStats()
  loadRecentLogins()
  recordVisit()
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeTimer) {
    clearInterval(timeTimer)
  }
})
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, var(--theme-primary-hover, #374151) 50%, var(--theme-primary-active, #4b5563) 100%);
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
  color: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  position: relative;
  overflow: hidden;
}

.welcome-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  pointer-events: none;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 32px;
  position: relative;
  z-index: 1;
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.welcome-avatar {
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  flex-shrink: 0;
}

.welcome-text h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.welcome-time {
  margin: 0 0 4px 0;
  font-size: 14px;
  opacity: 0.9;
  font-weight: 400;
}

.welcome-platform {
  margin: 4px 0 0;
  font-size: 14px;
  opacity: 0.9;
}

.welcome-greeting {
  margin: 0;
  font-size: 13px;
  opacity: 0.75;
  font-weight: 400;
}

.stat-icon-wrapper.login-ok {
  background: rgba(34, 197, 94, 0.35);
}

.welcome-right {
  display: flex;
  align-items: center;
  gap: 24px;
  background: rgba(255, 255, 255, 0.1);
  padding: 16px 24px;
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.welcome-stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-info .stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-info .stat-label {
  font-size: 12px;
  opacity: 0.8;
}

.welcome-stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.3);
}

/* 统计卡片行 */
.stats-row {
  margin-bottom: 32px;
}

.ops-stats-row {
  margin-top: -16px;
}

.biz-stats-row {
  margin-top: -16px;
}

.stat-card.ops {
  min-height: 160px;
}

.stat-card.biz {
  min-height: auto;
  padding: 16px 20px;
  cursor: pointer;
}

.stat-card.biz .inline-biz {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0;
}

.biz-label {
  font-size: 14px;
  color: #6b7280;
}

.biz-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--theme-primary, #111827);
}

.biz-value.warn {
  color: #d97706;
}

.biz-value.danger {
  color: #dc2626;
}

.stat-icon-box.pending-user {
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%);
}

.stat-icon-box.login-success {
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
}

.stat-icon-box.file-store {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
}

.stat-icon-box.post {
  background: linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%);
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  min-height: 180px;
  display: flex;
  flex-direction: column;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: var(--theme-primary, #111827);
}

.stat-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.stat-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-icon-box.user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon-box.role {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon-box.menu {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon-box.dept {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-icon-box.ticket-total {
  background: linear-gradient(135deg, #4b6cb7 0%, #182848 100%);
}

.stat-icon-box.ticket-open {
  background: linear-gradient(135deg, #f7971e 0%, #ffd200 100%);
}

.stat-icon-box.ticket-overdue {
  background: linear-gradient(135deg, #e53935 0%, #e35d5b 100%);
}

.stat-trend-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.stat-trend-badge.up {
  background: #f0fdf4;
  color: #16a34a;
}

.stat-trend-badge.down {
  background: #fef2f2;
  color: #dc2626;
}

.stat-card-body {
  flex: 1;
  margin-bottom: 12px;
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  color: var(--theme-text-base, #1F2937);
  line-height: 1;
  margin-bottom: 8px;
}

.stat-title {
  font-size: 14px;
  color: var(--theme-text-secondary, #6B7280);
  font-weight: 500;
}

.stat-card-footer {
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: var(--theme-text-secondary, #6B7280);
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 内容行 */
.content-row {
  margin-top: 8px;
}

.section-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  overflow: hidden;
  height: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, var(--theme-primary-hover, #374151) 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--theme-text-base, #1F2937);
}

/* 快捷入口 */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 24px;
}

@media (max-width: 1200px) {
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }
}

.quick-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
  gap: 14px;
  position: relative;
}

.quick-item:hover {
  background: #fff;
  border-color: var(--theme-primary, #111827);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.quick-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  transition: all 0.3s;
}

.quick-item:hover .quick-icon-wrapper {
  transform: scale(1.05);
}

.quick-icon-wrapper.user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.quick-icon-wrapper.role {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.quick-icon-wrapper.menu {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.quick-icon-wrapper.dept {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.quick-icon-wrapper.log {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.quick-icon-wrapper.ticket {
  background: linear-gradient(135deg, #f7971e 0%, #ffd200 100%);
}

.quick-icon-wrapper.approval {
  background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%);
}

.quick-icon-wrapper.config {
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
}

.quick-icon-wrapper.file {
  background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
}

.quick-icon-wrapper.chat {
  background: linear-gradient(135deg, #f97316 0%, #fb923c 100%);
}

.quick-icon-wrapper.monitor {
  background: linear-gradient(135deg, #0ea5e9 0%, #38bdf8 100%);
}

.quick-icon-wrapper.api {
  background: linear-gradient(135deg, #14b8a6 0%, #2dd4bf 100%);
}

.quick-icon-wrapper.dict {
  background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);
}

.quick-icon-wrapper.notice {
  background: linear-gradient(135deg, #ec4899 0%, #f472b6 100%);
}

.quick-icon-wrapper.job {
  background: linear-gradient(135deg, #14b8a6 0%, #2dd4bf 100%);
}

.quick-info {
  flex: 1;
  min-width: 0;
}

.quick-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--theme-text-base, #1F2937);
  margin-bottom: 4px;
}

.quick-desc {
  font-size: 12px;
  color: var(--theme-text-secondary, #6B7280);
}

.quick-arrow {
  color: #d0d5dd;
  transition: all 0.3s;
  flex-shrink: 0;
}

.quick-item:hover .quick-arrow {
  color: var(--theme-primary, #111827);
  transform: translateX(4px);
}

/* 系统信息 */
.sys-subtitle {
  margin: 0 24px 4px;
  padding-bottom: 12px;
  font-size: 13px;
  color: var(--theme-text-secondary, #6b7280);
  line-height: 1.5;
  border-bottom: 1px solid #f3f4f6;
}

.sys-meta-list {
  list-style: none;
  margin: 0;
  padding: 8px 24px 24px;
}

.sys-meta-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 11px 0;
  font-size: 14px;
  line-height: 1.5;
  border-bottom: 1px solid #f3f4f6;
}

.sys-meta-item:last-child {
  border-bottom: none;
}

.sys-meta-label {
  flex-shrink: 0;
  color: var(--theme-text-secondary, #6b7280);
}

.sys-meta-label::after {
  content: '：';
}

.sys-meta-value {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  color: var(--theme-text-base, #1f2937);
  word-break: break-word;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .welcome-banner {
    padding: 20px;
  }
  
  .welcome-content {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .welcome-text h1 {
    font-size: 20px;
  }
  
  .welcome-right {
    width: 100%;
    justify-content: space-around;
  }
  
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
    padding: 16px;
  }
}
</style>
