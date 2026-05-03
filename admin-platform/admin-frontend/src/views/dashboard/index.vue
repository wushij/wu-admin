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
              <el-icon><Top v-if="trends.menu >= 0" /><Bottom v-else /><Right v-if="trends.menu === 0" /></el-icon>
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

    <el-row :gutter="24" class="stats-row ticket-stats-row">
      <el-col :xs="24" :sm="8">
        <div class="stat-card ticket">
          <div class="stat-card-header">
            <div class="stat-icon-box ticket-total">
              <el-icon :size="24"><Tickets /></el-icon>
            </div>
          </div>
          <div class="stat-card-body">
            <div class="stat-number">{{ stats.ticketTotal }}</div>
            <div class="stat-title">工单总数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card ticket">
          <div class="stat-card-header">
            <div class="stat-icon-box ticket-open">
              <el-icon :size="24"><Warning /></el-icon>
            </div>
          </div>
          <div class="stat-card-body">
            <div class="stat-number">{{ stats.ticketOpenCount }}</div>
            <div class="stat-title">待处理工单</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card ticket">
          <div class="stat-card-header">
            <div class="stat-icon-box ticket-overdue">
              <el-icon :size="24"><Timer /></el-icon>
            </div>
          </div>
          <div class="stat-card-body">
            <div class="stat-number">{{ stats.ticketOverdueCount }}</div>
            <div class="stat-title">超时工单</div>
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
            <div class="quick-item" @click="$router.push('/system/user')">
              <div class="quick-icon-wrapper user">
                <el-icon :size="26"><User /></el-icon>
              </div>
              <div class="quick-info">
                <div class="quick-name">用户管理</div>
                <div class="quick-desc">管理系统用户</div>
              </div>
              <el-icon class="quick-arrow"><ArrowRight /></el-icon>
            </div>
            
            <div class="quick-item" @click="$router.push('/system/role')">
              <div class="quick-icon-wrapper role">
                <el-icon :size="26"><UserFilled /></el-icon>
              </div>
              <div class="quick-info">
                <div class="quick-name">角色管理</div>
                <div class="quick-desc">配置角色权限</div>
              </div>
              <el-icon class="quick-arrow"><ArrowRight /></el-icon>
            </div>
            
            <div class="quick-item" @click="$router.push('/system/menu')">
              <div class="quick-icon-wrapper menu">
                <el-icon :size="26"><Menu /></el-icon>
              </div>
              <div class="quick-info">
                <div class="quick-name">菜单管理</div>
                <div class="quick-desc">管理菜单结构</div>
              </div>
              <el-icon class="quick-arrow"><ArrowRight /></el-icon>
            </div>
            
            <div class="quick-item" @click="$router.push('/system/dept')">
              <div class="quick-icon-wrapper dept">
                <el-icon :size="26"><OfficeBuilding /></el-icon>
              </div>
              <div class="quick-info">
                <div class="quick-name">部门管理</div>
                <div class="quick-desc">组织架构管理</div>
              </div>
              <el-icon class="quick-arrow"><ArrowRight /></el-icon>
            </div>
            
            <div class="quick-item" @click="$router.push('/system/login-log')">
              <div class="quick-icon-wrapper log">
                <el-icon :size="26"><Document /></el-icon>
              </div>
              <div class="quick-info">
                <div class="quick-name">登录日志</div>
                <div class="quick-desc">查看登录记录</div>
              </div>
              <el-icon class="quick-arrow"><ArrowRight /></el-icon>
            </div>
            
            <div class="quick-item" @click="$router.push('/dashboard')">
              <div class="quick-icon-wrapper analytics">
                <el-icon :size="26"><DataAnalysis /></el-icon>
              </div>
              <div class="quick-info">
                <div class="quick-name">数据分析</div>
                <div class="quick-desc">系统数据统计</div>
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
          <div class="system-info">
            <div class="info-row">
              <span class="info-label">系统名称</span>
              <span class="info-value">RBAC管理系统</span>
            </div>
            <div class="info-row">
              <span class="info-label">系统版本</span>
              <span class="info-value">v1.0.0</span>
            </div>
            <div class="info-row">
              <span class="info-label">技术架构</span>
              <span class="info-value">Vue3 + Spring Boot</span>
            </div>
            <div class="info-row">
              <span class="info-label">运行环境</span>
              <span class="info-value">Production</span>
            </div>
            <div class="info-row">
              <span class="info-label">数据库</span>
              <span class="info-value">MySQL 8.0</span>
            </div>
            <div class="info-row">
              <span class="info-label">缓存服务</span>
              <span class="info-value">Redis 7.0</span>
            </div>
            <div class="info-row">
              <span class="info-label">当前用户</span>
              <span class="info-value">{{ userStore.userInfo.nickname || '管理员' }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getDashboardStats, recordVisit as apiRecordVisit } from '@/api/dashboard'
import { 
  User, 
  UserFilled, 
  Menu, 
  OfficeBuilding, 
  Document, 
  DataAnalysis,
  Grid,
  Top,
  Right,
  Bottom,
  Avatar,
  View,
  ArrowRight,
  InfoFilled,
  CircleCheck,
  Tickets,
  Warning,
  Timer
} from '@element-plus/icons-vue'

const userStore = useUserStore()

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

// 统计数据
const stats = ref({
  userCount: 0,
  roleCount: 0,
  menuCount: 0,
  deptCount: 0,
  onlineCount: 0,
  todayVisits: 0,
  yesterdayVisits: 0,
  ticketTotal: 0,
  ticketOpenCount: 0,
  ticketResolvedCount: 0,
  ticketOverdueCount: 0
})

// 增长趋势
const trends = ref({
  user: 0,
  role: 0,
  menu: 0,
  dept: 0
})

// 当前时间
const currentTime = ref('')
let timeTimer = null

const updateTime = () => {
  const now = new Date()
  const options = { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric', 
    weekday: 'long',
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit' 
  }
  currentTime.value = now.toLocaleString('zh-CN', options)
}

// 获取统计数据
const getStats = async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data || {}
    
    // 计算增长趋势
    if (stats.value.yesterdayVisits > 0) {
      const visitGrowth = ((stats.value.todayVisits - stats.value.yesterdayVisits) / stats.value.yesterdayVisits * 100).toFixed(1)
      trends.value.visit = parseFloat(visitGrowth)
    }
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
  recordVisit() // 记录本次访问
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

.welcome-greeting {
  margin: 0;
  font-size: 13px;
  opacity: 0.75;
  font-weight: 400;
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

.ticket-stats-row {
  margin-top: -10px;
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
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  padding: 24px;
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

.quick-icon-wrapper.analytics {
  background: linear-gradient(135deg, #30cfd0 0%, #330867 100%);
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
.system-info {
  padding: 8px 24px 24px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 14px;
  color: var(--theme-text-secondary, #6B7280);
  font-weight: 400;
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--theme-text-base, #1F2937);
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
