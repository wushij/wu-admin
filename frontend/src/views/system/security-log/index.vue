<template>
  <div class="app-container module-page security-log-page">
    <!-- 顶部概览 Hero 卡片 -->
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.securityLog" />
            <span>安全风控</span>
          </div>
          <p class="module-hero-desc">
            全链路反爬虫嗅探、防重放攻击、HMAC-SM3 国密验签拦截与动态行为风控闭环
          </p>
        </div>
        <div class="security-stats-group">
          <!-- 1. 今日拦截总览 -->
          <div class="stat-card stat-sky">
            <div class="stat-indicator"></div>
            <div class="stat-body">
              <div class="stat-num">{{ stats.todayTotal }}</div>
              <div class="stat-meta">
                <span class="stat-dot"></span>
                <span class="stat-label">今日拦截</span>
              </div>
            </div>
          </div>

          <!-- 2. 爬虫拦截 -->
          <div class="stat-card stat-amber">
            <div class="stat-indicator"></div>
            <div class="stat-body">
              <div class="stat-num">{{ stats.todayUaBlock }}</div>
              <div class="stat-meta">
                <span class="stat-dot"></span>
                <span class="stat-label">爬虫嗅探</span>
              </div>
            </div>
          </div>

          <!-- 3. 重放攻击 / 国密验签 -->
          <div class="stat-card stat-purple">
            <div class="stat-indicator"></div>
            <div class="stat-body">
              <div class="stat-num">{{ stats.todayReplayBlock }}</div>
              <div class="stat-meta">
                <span class="stat-dot"></span>
                <span class="stat-label">重放攻击</span>
              </div>
            </div>
          </div>

          <!-- 4. 蜜罐诱捕击中 -->
          <div class="stat-card stat-teal">
            <div class="stat-indicator"></div>
            <div class="stat-body">
              <div class="stat-num">{{ stats.todayHoneypot }}</div>
              <div class="stat-meta">
                <span class="stat-dot"></span>
                <span class="stat-label">蜜罐击中</span>
              </div>
            </div>
          </div>

          <!-- 5. 自动封禁处置 -->
          <div class="stat-card stat-rose">
            <div class="stat-indicator"></div>
            <div class="stat-body">
              <div class="stat-num">{{ stats.todayForbidden }}</div>
              <div class="stat-meta">
                <span class="stat-dot"></span>
                <span class="stat-label">自动封禁</span>
              </div>
            </div>
          </div>

          <!-- 6. 当前受控封禁主体 -->
          <div class="stat-card stat-banned" :class="{ 'has-banned': stats.activeBlockedCount > 0 }">
            <div class="stat-indicator"></div>
            <div class="stat-body">
              <div class="stat-num">{{ stats.activeBlockedCount }}</div>
              <div class="stat-meta">
                <span class="live-pulse-dot" :class="{ danger: stats.activeBlockedCount > 0 }"></span>
                <span class="stat-label">当前封禁</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 标签页导航 -->
    <el-card class="content-card" shadow="never">
      <el-tabs v-model="activeTab" class="security-tabs" @tab-change="handleTabChange">
        <!-- 标签页 1：安全拦截审计事件 -->
        <el-tab-pane label="安全拦截事件" name="events">
          <!-- 一体化检索与操作工具栏 -->
          <div class="table-filter-bar">
            <el-form :model="queryParams" inline class="inline-filter-form">
              <el-form-item label="客户端 IP">
                <el-input v-model="queryParams.ipaddr" placeholder="请输入 IP 地址" clearable style="width: 140px" />
              </el-form-item>
              <el-form-item label="事件类型">
                <el-select v-model="queryParams.eventType" placeholder="全部类型" clearable style="width: 140px">
                  <el-option label="自动化爬虫 UA" value="UA_BLOCK" />
                  <el-option label="重放攻击 (Nonce)" value="ANTI_REPLAY" />
                  <el-option label="数字签名篡改" value="SIGN_FAIL" />
                  <el-option label="时间戳过期" value="TIMESTAMP_INVALID" />
                  <el-option label="超大请求体" value="BODY_TOO_LARGE" />
                  <el-option label="风控自动封禁" value="RISK_BLOCK" />
                  <el-option label="蜜罐诱捕触发" value="HONEYPOT" />
                  <el-option label="登录失败" value="LOGIN_FAIL" />
                  <el-option label="滑块验证失败" value="CAPTCHA_FAIL" />
                  <el-option label="越权探测" value="PERMISSION_DENIED" />
                  <el-option label="翻页遍历" value="PAGE_SCAN" />
                  <el-option label="批量导出" value="BULK_EXPORT" />
                  <el-option label="风控挑战" value="RISK_CHALLENGE" />
                </el-select>
              </el-form-item>
              <el-form-item label="处置动作">
                <el-select v-model="queryParams.action" placeholder="全部动作" clearable style="width: 120px">
                  <el-option label="直接阻断 (403)" value="BLOCKED" />
                  <el-option label="触发封禁" value="FORBIDDEN" />
                  <el-option label="安全告警" value="WARNING" />
                  <el-option label="要求验证" value="CHALLENGE" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
                <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
                <el-button
                  type="danger"
                  plain
                  :icon="Delete"
                  v-permission="'system:security:clear'"
                  @click="handleClearEvents"
                >
                  清空事件
                </el-button>
                <el-button :icon="RefreshRight" @click="loadEventData">刷新</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 事件数据表格 -->
          <el-table
            :data="eventList"
            v-loading="eventLoading"
            border
            stripe
            class="security-table"
          >
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="ipaddr" label="客户端 IP" width="140" align="center">
              <template #default="{ row }">
                <el-link type="primary" :underline="false" @click="filterByIp(row.ipaddr)">
                  {{ row.ipaddr }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column prop="eventType" label="事件类型" width="150" align="center">
              <template #default="{ row }">
                <el-tag :type="getEventTypeTagType(row.eventType)" effect="light">
                  {{ formatEventTypeName(row.eventType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="触发规则与原因" min-width="200" show-overflow-tooltip />
            <el-table-column label="请求端点" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="method-badge">{{ row.requestMethod }}</span>
                <span class="uri-text">{{ row.requestUri }}</span>
              </template>
            </el-table-column>
            <el-table-column label="风险分值" width="130" align="center">
              <template #default="{ row }">
                <span class="risk-delta">+{{ row.riskDelta }}</span>
                <span class="risk-total">({{ row.totalRiskScore }}分)</span>
              </template>
            </el-table-column>
            <el-table-column prop="action" label="处置结果" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getActionTagType(row.action)" size="small">
                  {{ formatAction(row.action) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="拦截时间" width="170" align="center" />
            <el-table-column label="操作" width="140" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
                  <el-button
                    type="danger"
                    link
                    size="small"
                    v-permission="'system:security:block'"
                    @click="quickBlockIp(row.ipaddr)"
                  >
                    拉黑
                  </el-button>
                  <el-button
                    type="danger"
                    link
                    size="small"
                    v-permission="'system:security:delete'"
                    @click="handleDeleteEvent(row)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="queryParams.pageNo"
            v-model:page-size="queryParams.pageSize"
            :total="eventTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            class="table-pagination"
            @size-change="loadEventData"
            @current-change="loadEventData"
          />
        </el-tab-pane>

        <!-- 标签页 2：风险主体管控 -->
        <el-tab-pane label="风险主体" name="risk">
          <!-- 一体化检索与操作工具栏 -->
          <div class="table-filter-bar">
            <el-form :model="riskQuery" inline class="inline-filter-form">
              <el-form-item label="主体">
                <el-input
                  v-model="riskQuery.keyword"
                  placeholder="用户ID / IP"
                  clearable
                  style="width: 140px"
                />
              </el-form-item>
              <el-form-item label="风险等级">
                <el-select v-model="riskQuery.level" placeholder="全部等级" clearable style="width: 120px">
                  <el-option label="关注 (1)" :value="1" />
                  <el-option label="警告 (2)" :value="2" />
                  <el-option label="挑战 (3)" :value="3" />
                  <el-option label="封禁 (4)" :value="4" />
                </el-select>
              </el-form-item>
              <el-form-item label="封禁状态">
                <el-select v-model="riskQuery.blocked" placeholder="全部" clearable style="width: 110px">
                  <el-option label="已封禁" :value="true" />
                  <el-option label="未封禁" :value="false" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleRiskQuery">搜索</el-button>
                <el-button :icon="Refresh" @click="resetRiskQuery">重置</el-button>
                <el-button
                  type="primary"
                  :icon="Plus"
                  v-permission="'system:security:block'"
                  @click="openManualBlockDialog"
                >
                  手动封禁
                </el-button>
                <el-button :icon="RefreshRight" @click="loadRiskData">刷新名单</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table :data="riskList" v-loading="riskLoading" border stripe class="security-table">
            <el-table-column label="风险主体" min-width="200">
              <template #default="{ row }">
                <el-tag
                  :type="row.principalType === 'user' ? 'primary' : 'info'"
                  size="small"
                  class="principal-type-tag"
                >
                  {{ row.principalType === 'user' ? '账号' : 'IP' }}
                </el-tag>
                <span class="font-mono font-bold">{{ row.principal }}</span>
                <div v-if="row.ipaddr && row.principalType === 'user'" class="principal-ip">
                  来源 IP：{{ row.ipaddr }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="风险分与等级" width="230">
              <template #default="{ row }">
                <div class="risk-progress-wrap">
                  <el-progress
                    :percentage="Math.min(100, Math.round((row.riskScore / 120) * 100))"
                    :status="row.riskLevel >= 4 ? 'exception' : row.riskLevel >= 2 ? 'warning' : ''"
                    :stroke-width="8"
                  />
                  <span class="risk-score-text">
                    {{ row.riskScore }} 分 · {{ row.riskLevelText }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="eventCount" label="窗口内事件" width="110" align="center">
              <template #default="{ row }">{{ row.eventCount }} 次</template>
            </el-table-column>
            <el-table-column prop="lastEventTime" label="最近触发" width="170" align="center">
              <template #default="{ row }">{{ row.lastEventTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="封禁状态" width="180" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.blocked" type="danger" effect="plain" class="countdown-tag">
                  剩余 {{ formatRemainingSeconds(row.remainingSeconds) }}
                </el-tag>
                <el-tag v-else type="success" effect="plain">正常</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button type="primary" link size="small" @click="showSubjectEvents(row)">
                    事件
                  </el-button>
                  <el-button
                    v-if="row.blocked"
                    type="success"
                    link
                    size="small"
                    v-permission="'system:security:unblock'"
                    @click="handleUnblock(row.principal)"
                  >
                    解封
                  </el-button>
                  <el-button
                    v-else
                    type="danger"
                    link
                    size="small"
                    v-permission="'system:security:block'"
                    @click="quickBlockIp(row.principal)"
                  >
                    拉黑
                  </el-button>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="当前没有处于风险窗口中的主体，系统运行平稳" />
            </template>
          </el-table>

          <el-pagination
            v-model:current-page="riskQuery.pageNo"
            v-model:page-size="riskQuery.pageSize"
            :total="riskTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            class="table-pagination"
            @size-change="loadRiskData"
            @current-change="loadRiskData"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 事件详情抽屉 Drawer -->
    <el-drawer v-model="detailVisible" title="安全拦截事件详情" size="520px">
      <div v-if="activeEvent" class="detail-container">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="事件 ID">{{ activeEvent.id }}</el-descriptions-item>
          <el-descriptions-item label="事件类型">
            <el-tag :type="getEventTypeTagType(activeEvent.eventType)">
              {{ formatEventTypeName(activeEvent.eventType) }} ({{ activeEvent.eventType }})
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="客户端 IP">{{ activeEvent.ipaddr }}</el-descriptions-item>
          <el-descriptions-item label="请求端点">
            {{ activeEvent.requestMethod }} {{ activeEvent.requestUri }}
          </el-descriptions-item>
          <el-descriptions-item label="风险分值">
            本次增量 +{{ activeEvent.riskDelta }}，当前总计 {{ activeEvent.totalRiskScore }} 分
          </el-descriptions-item>
          <el-descriptions-item label="处置动作">
            {{ activeEvent.action === 'FORBIDDEN' ? '自动临时封禁' : '直接 403 阻断' }}
          </el-descriptions-item>
          <el-descriptions-item label="拦截时间">{{ activeEvent.createTime }}</el-descriptions-item>
          <el-descriptions-item label="详细触发原因">
            <span class="text-danger font-bold">{{ activeEvent.reason }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="完整 User-Agent">
            <div class="ua-block">{{ activeEvent.userAgent || '无 User-Agent 头' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <!-- 风险主体事件抽屉 -->
    <el-drawer
      v-model="subjectEventsVisible"
      :title="`风险主体事件 - ${activeSubject?.principal || ''}`"
      size="600px"
    >
      <div v-loading="subjectEventsLoading">
        <el-descriptions v-if="activeSubject" :column="2" border size="small" class="subject-summary">
          <el-descriptions-item label="风险分">{{ activeSubject.riskScore }}</el-descriptions-item>
          <el-descriptions-item label="等级">{{ activeSubject.riskLevelText }}</el-descriptions-item>
          <el-descriptions-item label="窗口内事件">{{ activeSubject.eventCount }} 次</el-descriptions-item>
          <el-descriptions-item label="封禁状态">
            {{ activeSubject.blocked ? `剩余 ${formatRemainingSeconds(activeSubject.remainingSeconds)}` : '正常' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-table :data="subjectEvents" border stripe size="small">
          <el-table-column prop="eventType" label="类型" width="150">
            <template #default="{ row }">
              <el-tag :type="getEventTypeTagType(row.eventType)" size="small">
                {{ formatEventTypeName(row.eventType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="riskDelta" label="加分" width="70" align="center">
            <template #default="{ row }">+{{ row.riskDelta }}</template>
          </el-table-column>
          <el-table-column prop="totalRiskScore" label="累计" width="70" align="center" />
          <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" width="170" />
          <template #empty>
            <el-empty description="该主体最近没有安全事件" />
          </template>
        </el-table>
      </div>
    </el-drawer>

    <!-- 手动封禁对话框 Dialog -->
    <el-dialog v-model="blockDialogVisible" title="手动封禁风险主体 IP" width="460px">
      <el-form :model="blockForm" label-width="100px">
        <el-form-item label="目标 IP" required>
          <el-input v-model="blockForm.ip" placeholder="例如：192.168.1.100" />
        </el-form-item>
        <el-form-item label="封禁时长">
          <el-select v-model="blockForm.duration" style="width: 100%">
            <el-option label="1 小时" :value="3600" />
            <el-option label="6 小时" :value="21600" />
            <el-option label="24 小时 (1天)" :value="86400" />
            <el-option label="7 天" :value="604800" />
            <el-option label="30 天" :value="2592000" />
          </el-select>
        </el-form-item>
        <el-form-item label="封禁原因">
          <el-input v-model="blockForm.reason" placeholder="例如：恶意扫描 / 批量刷接口" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="blockDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmManualBlock">确认封禁</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  RefreshRight,
  Delete,
  Plus,
} from '@element-plus/icons-vue'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
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

const activeTab = ref('events')

// 大盘指标数据
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

// 拦截事件查询参数
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

// 风险主体
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

// 主体事件抽屉
const subjectEventsVisible = ref(false)
const subjectEventsLoading = ref(false)
const subjectEvents = ref<SecurityEventVO[]>([])
const activeSubject = ref<RiskSubjectVO | null>(null)

// 详情抽屉
const detailVisible = ref(false)
const activeEvent = ref<SecurityEventVO | null>(null)

// 手动封禁弹窗
const blockDialogVisible = ref(false)
const blockForm = reactive({
  ip: '',
  duration: 3600,
  reason: '管理员手动封禁',
})

// 格式化事件类型名称
function formatEventTypeName(type: string): string {
  switch (type) {
    case 'UA_BLOCK':
      return '爬虫工具拦截'
    case 'ANTI_REPLAY':
      return '重放攻击拦截'
    case 'SIGN_FAIL':
      return '数字签名篡改'
    case 'TIMESTAMP_INVALID':
      return '时间戳超窗'
    case 'BODY_TOO_LARGE':
      return '超大请求体'
    case 'RISK_BLOCK':
      return '风控累计封禁'
    case 'HONEYPOT':
      return '蜜罐诱捕触发'
    case 'LOGIN_FAIL':
      return '登录失败'
    case 'CAPTCHA_FAIL':
      return '滑块验证失败'
    case 'CAPTCHA_ABUSE':
      return '滑块连续失败'
    case 'PERMISSION_DENIED':
      return '越权探测'
    case 'PAGE_SCAN':
      return '翻页遍历'
    case 'BULK_EXPORT':
      return '批量导出'
    case 'RISK_CHALLENGE':
      return '风控挑战'
    default:
      return type || '未知事件'
  }
}

// 事件类型标签样式
function getEventTypeTagType(type: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined {
  switch (type) {
    case 'UA_BLOCK':
    case 'CAPTCHA_FAIL':
    case 'LOGIN_FAIL':
      return 'warning'
    case 'ANTI_REPLAY':
    case 'SIGN_FAIL':
    case 'RISK_BLOCK':
    case 'HONEYPOT':
    case 'CAPTCHA_ABUSE':
    case 'PERMISSION_DENIED':
    case 'PAGE_SCAN':
    case 'BULK_EXPORT':
      return 'danger'
    case 'TIMESTAMP_INVALID':
    case 'BODY_TOO_LARGE':
    case 'RISK_CHALLENGE':
      return 'info'
    default:
      return undefined
  }
}

// 处置动作文案
function formatAction(action: string): string {
  switch (action) {
    case 'FORBIDDEN':
      return '触发封禁'
    case 'CHALLENGE':
      return '要求验证'
    case 'WARNING':
      return '安全告警'
    case 'BLOCKED':
      return '已阻断(403)'
    default:
      return action || '未知'
  }
}

// 处置动作标签样式
function getActionTagType(action: string): 'danger' | 'warning' | 'info' | 'success' {
  switch (action) {
    case 'FORBIDDEN':
      return 'danger'
    case 'CHALLENGE':
      return 'warning'
    case 'WARNING':
      return 'warning'
    default:
      return 'info'
  }
}

// 格式化剩余秒数
function formatRemainingSeconds(sec: number): string {
  if (!sec || sec <= 0) return '已到期'
  if (sec < 60) return `${sec} 秒`
  const minutes = Math.floor(sec / 60)
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const remMinutes = minutes % 60
  if (hours < 24) return `${hours} 小时 ${remMinutes} 分`
  const days = Math.floor(hours / 24)
  const remHours = hours % 24
  return `${days} 天 ${remHours} 小时`
}

// 加载大盘统计
async function loadStats() {
  try {
    const res = await getSecurityStats()
    if (res.data) {
      Object.assign(stats, res.data)
    }
  } catch {}
}

// 加载事件列表
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

// 加载风险主体
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

// 查看主体最近事件
async function showSubjectEvents(row: RiskSubjectVO) {
  activeSubject.value = row
  subjectEvents.value = []
  subjectEventsVisible.value = true
  subjectEventsLoading.value = true
  try {
    const res = await getRiskSubjectEvents(row.principal, 50)
    if (res.data) {
      subjectEvents.value = res.data || []
    }
  } catch (err) {
    console.error('加载主体事件失败', err)
  } finally {
    subjectEventsLoading.value = false
  }
}

function handleTabChange(tab: string | number) {
  if (tab === 'events') {
    loadEventData()
  } else if (tab === 'risk') {
    loadRiskData()
  }
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

// 一键解封
async function handleUnblock(principal: string) {
  try {
    await ElMessageBox.confirm(
      `确认解除对风险主体【${principal}】的封禁并清零风险分吗？解除后该主体可正常访问。`,
      '确认解封',
      {
        type: 'warning',
        confirmButtonText: '立即解封',
        cancelButtonText: '取消',
      }
    )
    await unblockRiskSubject(principal, true)
    ElMessage.success(`【${principal}】已成功解封并清零风险分`)
    loadRiskData()
    loadStats()
  } catch {}
}

// 快速拉黑 IP
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
    ElMessage.success(`已成功将 IP【${blockForm.ip}】加入封禁黑名单！`)
    blockDialogVisible.value = false
    loadRiskData()
    loadStats()
  } catch (err: any) {
    ElMessage.error(err?.message || '封禁操作失败')
  }
}

// 删除单条事件
async function handleDeleteEvent(row: SecurityEventVO) {
  try {
    await ElMessageBox.confirm(`确认删除此条安全拦截事件记录吗？`, '提示', {
      type: 'warning',
    })
    await deleteSecurityEvent(row.id)
    ElMessage.success('删除成功')
    loadEventData()
    loadStats()
  } catch {}
}

// 清空所有事件
async function handleClearEvents() {
  try {
    await ElMessageBox.confirm('确认清空所有的安全拦截事件审计日志吗？清空后不可恢复！', '高危警告', {
      type: 'error',
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
    })
    await clearSecurityEvents()
    ElMessage.success('安全事件日志已清空')
    loadEventData()
    loadStats()
  } catch {}
}

onMounted(() => {
  loadStats()
  loadEventData()
})
</script>

<style scoped>
.security-log-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-stats-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}

/* 核心微光指标卡片 */
.stat-card {
  position: relative;
  min-width: 98px;
  padding: 9px 12px 9px;
  border-radius: 10px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.18), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  user-select: none;
}

.stat-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.22);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.16);
}

/* 顶部细致微弧光条 */
.stat-indicator {
  position: absolute;
  top: 0;
  left: 10px;
  right: 10px;
  height: 2px;
  border-radius: 0 0 2px 2px;
  opacity: 0.9;
  transition: opacity 0.25s ease;
}

.stat-card:hover .stat-indicator {
  opacity: 1;
  filter: drop-shadow(0 0 4px currentColor);
}

.stat-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.stat-num {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.5px;
  font-feature-settings: 'tnum';
  transition: transform 0.25s ease;
}

.stat-card:hover .stat-num {
  transform: scale(1.05);
}

.stat-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 4px;
}

.stat-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  opacity: 0.85;
}

.stat-label {
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 0.3px;
  white-space: nowrap;
}

/* 1. 天蓝 - 今日拦截 */
.stat-sky .stat-indicator {
  background: linear-gradient(90deg, transparent, #38bdf8, transparent);
  color: #38bdf8;
}
.stat-sky .stat-num {
  color: #38bdf8;
  text-shadow: 0 0 16px rgba(56, 189, 248, 0.35);
}
.stat-sky .stat-dot {
  background: #38bdf8;
  box-shadow: 0 0 6px #38bdf8;
}
.stat-sky:hover {
  background: linear-gradient(180deg, rgba(56, 189, 248, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(56, 189, 248, 0.35);
}

/* 2. 金黄 - 爬虫嗅探 */
.stat-amber .stat-indicator {
  background: linear-gradient(90deg, transparent, #fbbf24, transparent);
  color: #fbbf24;
}
.stat-amber .stat-num {
  color: #fbbf24;
  text-shadow: 0 0 16px rgba(251, 191, 36, 0.35);
}
.stat-amber .stat-dot {
  background: #fbbf24;
  box-shadow: 0 0 6px #fbbf24;
}
.stat-amber:hover {
  background: linear-gradient(180deg, rgba(251, 191, 36, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(251, 191, 36, 0.35);
}

/* 3. 紫罗兰 - 重放反篡改 */
.stat-purple .stat-indicator {
  background: linear-gradient(90deg, transparent, #c084fc, transparent);
  color: #c084fc;
}
.stat-purple .stat-num {
  color: #c084fc;
  text-shadow: 0 0 16px rgba(192, 132, 252, 0.35);
}
.stat-purple .stat-dot {
  background: #c084fc;
  box-shadow: 0 0 6px #c084fc;
}
.stat-purple:hover {
  background: linear-gradient(180deg, rgba(192, 132, 252, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(192, 132, 252, 0.35);
}

/* 4. 极光翠青 - 蜜罐诱捕 */
.stat-teal .stat-indicator {
  background: linear-gradient(90deg, transparent, #2dd4bf, transparent);
  color: #2dd4bf;
}
.stat-teal .stat-num {
  color: #2dd4bf;
  text-shadow: 0 0 16px rgba(45, 212, 191, 0.35);
}
.stat-teal .stat-dot {
  background: #2dd4bf;
  box-shadow: 0 0 6px #2dd4bf;
}
.stat-teal:hover {
  background: linear-gradient(180deg, rgba(45, 212, 191, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(45, 212, 191, 0.35);
}

/* 5. 珊瑚红 - 自动封禁 */
.stat-rose .stat-indicator {
  background: linear-gradient(90deg, transparent, #fb7185, transparent);
  color: #fb7185;
}
.stat-rose .stat-num {
  color: #fb7185;
  text-shadow: 0 0 16px rgba(251, 113, 133, 0.35);
}
.stat-rose .stat-dot {
  background: #fb7185;
  box-shadow: 0 0 6px #fb7185;
}
.stat-rose:hover {
  background: linear-gradient(180deg, rgba(251, 113, 133, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(251, 113, 133, 0.35);
}

/* 6. 动态在封黑名单态 (带呼吸状态点) */
.stat-banned .stat-indicator {
  background: linear-gradient(90deg, transparent, #34d399, transparent);
  color: #34d399;
}
.stat-banned .stat-num {
  color: #34d399;
  text-shadow: 0 0 16px rgba(52, 211, 153, 0.35);
}
.stat-banned:hover {
  background: linear-gradient(180deg, rgba(52, 211, 153, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(52, 211, 153, 0.35);
}

/* 当有主体在封时，激活高危警报态 */
.stat-banned.has-banned {
  background: linear-gradient(180deg, rgba(239, 68, 68, 0.15) 0%, rgba(239, 68, 68, 0.04) 100%);
  border-color: rgba(239, 68, 68, 0.3);
}
.stat-banned.has-banned .stat-indicator {
  background: linear-gradient(90deg, transparent, #ef4444, transparent);
  color: #ef4444;
}
.stat-banned.has-banned .stat-num {
  color: #f87171;
  text-shadow: 0 0 16px rgba(239, 68, 68, 0.45);
}
.stat-banned.has-banned:hover {
  background: linear-gradient(180deg, rgba(239, 68, 68, 0.22) 0%, rgba(255, 255, 255, 0.04) 100%);
  border-color: rgba(239, 68, 68, 0.5);
  box-shadow: 0 8px 24px rgba(239, 68, 68, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.16);
}

/* 呼吸指示灯 */
.live-pulse-dot {
  position: relative;
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #34d399;
  box-shadow: 0 0 6px #34d399;
}

.live-pulse-dot.danger {
  background-color: #ef4444;
  box-shadow: 0 0 6px #ef4444;
}

.live-pulse-dot::after {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: inherit;
  opacity: 0.6;
  animation: pulse-ring 2s cubic-bezier(0.24, 0, 0.38, 1) infinite;
}

@keyframes pulse-ring {
  0% {
    transform: scale(0.6);
    opacity: 0.8;
  }
  100% {
    transform: scale(2.4);
    opacity: 0;
  }
}

.content-card {
  border-radius: 8px;
}

/* 一体化检索操作工具栏 */
.table-filter-bar {
  background: var(--el-fill-color-lighter, #f8fafc);
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 14px;
}

.inline-filter-form.el-form--inline {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.inline-filter-form.el-form--inline .el-form-item {
  margin-right: 0;
  margin-bottom: 0;
}

.method-badge {
  display: inline-block;
  padding: 2px 6px;
  font-size: 11px;
  font-weight: 700;
  border-radius: 4px;
  background: var(--el-fill-color-darker);
  color: var(--el-text-color-regular);
  margin-right: 6px;
}

.uri-text {
  font-family: monospace;
  font-size: 12px;
}

.risk-delta {
  font-weight: 700;
  color: var(--el-color-danger);
  margin-right: 4px;
}

.risk-total {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.risk-progress-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.risk-score-text {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.countdown-tag {
  font-family: monospace;
  font-weight: 600;
}

.ua-block {
  font-family: monospace;
  font-size: 12px;
  background: var(--el-fill-color-light);
  padding: 8px;
  border-radius: 4px;
  word-break: break-all;
  white-space: pre-wrap;
  max-height: 150px;
  overflow-y: auto;
}

.font-mono {
  font-family: monospace;
}
.font-bold {
  font-weight: 700;
}

.principal-type-tag {
  margin-right: 6px;
}

.principal-ip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.subject-summary {
  margin-bottom: 12px;
}

/* 响应式适配 */
@media (max-width: 1360px) {
  .module-hero-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .security-stats-group {
    width: 100%;
    justify-content: space-between;
  }
  .stat-card {
    flex: 1;
    min-width: 0;
    padding: 10px 8px;
  }
}

@media (max-width: 768px) {
  .security-stats-group {
    flex-wrap: wrap;
    gap: 10px;
  }
  .stat-card {
    flex: calc(50% - 10px);
    min-width: 120px;
  }
}
</style>
