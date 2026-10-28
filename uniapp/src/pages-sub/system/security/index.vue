<template>
  <PermissionBlock v-if="!canAccess" />
  <view v-else class="page-padded page-list security-page">
    <ModuleDarkHero
      title="安全风控"
      subtitle="反爬拦截 · 防重放 · 风险封禁与一键解封"
      icon="shield-o"
      theme="log"
      :count="stats.todayTotal"
      count-label="今日拦截"
    />

    <scroll-view scroll-y class="page-list__scroll" @scrolltolower="onScrollLower">
      <FadeIn :show="hasFetched">
        <StatGrid :items="statItems" :columns="3" />

        <SegmentTabs v-model="activeTab" :tabs="tabs" theme="teal" class="security-tabs" />

        <!-- 安全拦截事件 -->
        <!-- 安全拦截事件 -->
        <template v-if="activeTab === 'events'">
          <MonitorPanel title="筛选条件">
            <SearchBar v-model="filters.ipaddr" placeholder="搜索客户端 IP" @search="applyFilters" />

            <view class="security-filter">
              <text class="security-filter__label">事件类型</text>
              <view class="security-chips security-chips--row">
                <text
                  v-for="item in primaryEventTypeOptions"
                  :key="item.value"
                  class="security-chip"
                  :class="{ 'security-chip--active': filters.eventType === item.value }"
                  @click="setEventType(item.value)"
                >
                  {{ item.label }}
                </text>
                <text
                  class="security-chip security-chip--more"
                  :class="{ 'security-chip--active': isExtraEventTypeActive }"
                  @click="pickExtraEventType"
                >
                  {{ moreEventTypeLabel }}
                </text>
              </view>
            </view>


          </MonitorPanel>

          <MonitorPanel title="拦截事件明细">
            <template #extra>
              <view class="security-panel-extra">
                <text class="security-log-meta">{{ list.length }} 条已加载</text>
                <button v-if="canClear" class="security-toolbar-btn security-toolbar-btn--danger" @click="confirmClear">
                  清空
                </button>
              </view>
            </template>

            <ListCard v-for="item in list" :key="item.id" class="security-card" @click="openDetail(item)">
              <view class="security-card__head">
                <view class="security-card__tags">
                  <DictTag :label="formatEventTypeName(item.eventType)" :effect="eventTypeTone(item.eventType)" />
                  <DictTag :label="formatAction(item.action)" :effect="actionTone(item.action)" />
                </view>
                <text class="security-card__score">+{{ item.riskDelta }}</text>
              </view>

              <view class="security-card__row">
                <text class="security-card__ip">{{ item.ipaddr }}</text>
                <text class="security-card__time">{{ formatDateTime(item.createTime, true) }}</text>
              </view>

              <text class="security-card__reason">{{ item.reason || '—' }}</text>
              <text class="security-card__endpoint">{{ item.requestMethod }} {{ item.requestUri }}</text>
              <text class="security-card__total">当前风险分 {{ item.totalRiskScore }} 分</text>
            </ListCard>

            <EmptyState v-if="empty" title="暂无安全拦截事件" icon="shield-o" />
            <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
          </MonitorPanel>
        </template>

        <!-- 风险主体 -->
        <template v-else>
          <MonitorPanel title="筛选条件">
            <SearchBar v-model="riskFilters.keyword" placeholder="搜索用户名 / 昵称 / ID / IP" @search="applyRiskFilters" />

            <view class="security-filter">
              <text class="security-filter__label">风险等级</text>
              <view class="security-chips">
                <text
                  v-for="item in riskLevelOptions"
                  :key="String(item.value)"
                  class="security-chip"
                  :class="{ 'security-chip--active': riskFilters.level === item.value }"
                  @click="setRiskLevel(item.value)"
                >
                  {{ item.label }}
                </text>
              </view>
            </view>

            <view class="security-filter">
              <text class="security-filter__label">封禁状态</text>
              <view class="security-chips">
                <text
                  v-for="item in riskBlockedOptions"
                  :key="String(item.value)"
                  class="security-chip"
                  :class="{ 'security-chip--active': riskFilters.blocked === item.value }"
                  @click="setRiskBlocked(item.value)"
                >
                  {{ item.label }}
                </text>
              </view>
            </view>


          </MonitorPanel>

          <MonitorPanel title="风险主体明细">
            <template #extra>
              <view class="security-panel-extra">
                <text class="security-log-meta">{{ riskList.length }} 条已加载</text>
                <button v-if="canBlock" class="security-toolbar-btn" @click="openBlock()">手动封禁</button>
              </view>
            </template>

            <ListCard v-for="item in riskList" :key="item.principal" class="security-risk">
              <view class="security-risk__head">
                <view class="security-risk__title">
                  <DictTag
                    :label="item.principalType === 'user' ? '账号' : 'IP'"
                    :effect="item.principalType === 'user' ? 'primary' : 'default'"
                  />
                  <text class="security-risk__ip">{{ formatSubjectTitle(item) }}</text>
                </view>
                <DictTag
                  :label="item.blocked ? `剩余 ${formatRemainingSeconds(item.remainingSeconds)}` : '正常'"
                  :effect="item.blocked ? 'danger' : 'success'"
                />
              </view>

              <view class="security-risk__bar">
                <view class="security-risk__track">
                  <view
                    class="security-risk__fill"
                    :class="{ 'security-risk__fill--danger': item.riskLevel >= 4 }"
                    :style="{ width: `${Math.min(100, Math.round((item.riskScore / 120) * 100))}%` }"
                  />
                </view>
                <text class="security-risk__score">{{ item.riskScore }} 分 · {{ item.riskLevelText }}</text>
              </view>

              <view class="security-risk__meta">
                <text class="security-risk__meta-item">窗口内事件 {{ item.eventCount ?? 0 }} 次</text>
                <text class="security-risk__meta-item">最近触发 {{ item.lastEventTime || '—' }}</text>
              </view>

              <text v-if="item.ipaddr && item.principalType === 'user'" class="security-risk__meta-item">
                来源 IP：{{ item.ipaddr }}
              </text>
              <text v-if="item.reason" class="security-risk__reason">{{ item.reason }}</text>

              <view class="security-card__actions">
                <button class="outline-btn" @click="openSubjectEvents(item)">事件</button>
                <button
                  v-if="item.blocked && canUnblock"
                  class="outline-btn outline-btn--primary"
                  @click="handleUnblock(item)"
                >
                  解封
                </button>
                <button
                  v-else-if="canBlock"
                  class="outline-btn outline-btn--danger"
                  @click="openBlock(item.principal)"
                >
                  拉黑
                </button>
              </view>
            </ListCard>

            <EmptyState
              v-if="!riskLoading && !riskList.length"
              title="当前没有处于风险窗口中的主体"
              icon="shield-o"
            />
            <ListFooter v-else :loading="riskLoading" :finished="riskFinished" :empty="riskEmpty" />
          </MonitorPanel>
        </template>
      </FadeIn>
    </scroll-view>

    <!-- 事件详情 -->
    <view v-if="detailVisible && activeEvent" class="security-modal" @click="detailVisible = false">
      <view class="security-modal__card" @click.stop>
        <text class="security-modal__title">安全拦截详情</text>

        <view class="security-detail__row">
          <text class="security-detail__label">事件类型</text>
          <DictTag :label="formatEventTypeName(activeEvent.eventType)" :effect="eventTypeTone(activeEvent.eventType)" />
        </view>
        <view class="security-detail__row">
          <text class="security-detail__label">客户端 IP</text>
          <text class="security-detail__value">{{ activeEvent.ipaddr }}</text>
        </view>
        <view class="security-detail__row">
          <text class="security-detail__label">请求端点</text>
          <text class="security-detail__value">{{ activeEvent.requestMethod }} {{ activeEvent.requestUri }}</text>
        </view>
        <view class="security-detail__row">
          <text class="security-detail__label">风险分值</text>
          <text class="security-detail__value">本次 +{{ activeEvent.riskDelta }}，当前 {{ activeEvent.totalRiskScore }} 分</text>
        </view>
        <view class="security-detail__row">
          <text class="security-detail__label">拦截时间</text>
          <text class="security-detail__value">{{ formatDateTime(activeEvent.createTime, true) }}</text>
        </view>

        <text class="security-detail__label security-detail__label--block">触发原因</text>
        <text class="security-detail__reason">{{ activeEvent.reason || '—' }}</text>

        <text class="security-detail__label security-detail__label--block">User-Agent</text>
        <view class="security-detail__ua">
          <text>{{ activeEvent.userAgent || '无 User-Agent 头' }}</text>
        </view>

        <view class="security-modal__actions">
          <button v-if="canBlock" class="security-action-btn security-action-btn--danger" @click="blockFromDetail">
            拉黑主体
          </button>
          <button v-if="canDelete" class="security-action-btn" @click="deleteFromDetail">删除</button>
          <button class="security-action-btn" @click="detailVisible = false">关闭</button>
        </view>
      </view>
    </view>

    <!-- 风险主体最近事件 -->
    <view v-if="subjectEventsVisible" class="security-modal" @click="subjectEventsVisible = false">
      <view class="security-modal__card" @click.stop>
        <text class="security-modal__title">最近安全事件</text>

        <view v-if="activeSubject" class="security-subject-summary">
          <DictTag
            :label="activeSubject.principalType === 'user' ? '账号' : 'IP'"
            :effect="activeSubject.principalType === 'user' ? 'primary' : 'default'"
          />
          <text class="security-risk__ip">{{ formatSubjectTitle(activeSubject) }}</text>
          <text class="security-risk__score">{{ activeSubject.riskScore }} 分 · {{ activeSubject.riskLevelText }}</text>
        </view>

        <view v-for="item in subjectEvents" :key="item.id" class="security-subject-event">
          <view class="security-subject-event__head">
            <DictTag :label="formatEventTypeName(item.eventType)" :effect="eventTypeTone(item.eventType)" />
            <text class="security-card__score">+{{ item.riskDelta }}</text>
          </view>
          <text class="security-card__reason">{{ item.reason || '—' }}</text>
          <view class="security-card__row">
            <text class="security-card__time">{{ formatDateTime(item.createTime, true) }}</text>
            <text class="security-card__time">累计 {{ item.totalRiskScore }} 分</text>
          </view>
        </view>

        <text v-if="subjectEventsLoading" class="security-subject-loading">加载中…</text>
        <EmptyState
          v-if="!subjectEventsLoading && !subjectEvents.length"
          title="该主体最近没有安全事件"
          icon="shield-o"
        />

        <view class="security-modal__actions">
          <button class="security-action-btn" @click="subjectEventsVisible = false">关闭</button>
        </view>
      </view>
    </view>

    <!-- 手动封禁 -->
    <view v-if="blockVisible" class="security-modal" @click="blockVisible = false">
      <view class="security-modal__card" @click.stop>
        <text class="security-modal__title">手动封禁风险主体</text>

        <text class="security-form__label">目标主体</text>
        <input
          v-model="blockForm.principal"
          class="security-input"
          placeholder="用户 ID 或 IP，例如 user:12 / 1.2.3.100"
        />

        <text class="security-form__label">封禁时长</text>
        <view class="security-input security-input--picker" @click="pickDuration">
          <text>{{ blockDurationLabel }}</text>
          <text class="security-input__arrow">›</text>
        </view>

        <text class="security-form__label">封禁原因</text>
        <input v-model="blockForm.reason" class="security-input" placeholder="例如：恶意扫描 / 批量刷接口" />

        <view class="security-modal__actions">
          <button class="security-action-btn" @click="blockVisible = false">取消</button>
          <button
            class="security-action-btn security-action-btn--danger"
            :loading="blocking"
            @click="confirmBlock"
          >
            确认封禁
          </button>
        </view>
      </view>
    </view>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/layout/ModuleDarkHero/index.vue'
import MonitorPanel from '@/components/layout/MonitorPanel/index.vue'
import SearchBar from '@/components/base/SearchBar/index.vue'
import ListFooter from '@/components/base/ListFooter/index.vue'
import ListCard from '@/components/base/ListCard/index.vue'
import DictTag from '@/components/base/DictTag/index.vue'
import FadeIn from '@/components/base/FadeIn/index.vue'
import EmptyState from '@/components/base/EmptyState/index.vue'
import SegmentTabs from '@/components/base/SegmentTabs/index.vue'
import StatGrid from '@/components/base/StatGrid/index.vue'
import type { StatItem } from '@/components/base/StatGrid/index.vue'
import PermissionBlock from '@/components/base/PermissionBlock/index.vue'
import AppDialogHost from '@/components/base/AppDialogHost/index.vue'
import { usePageList } from '@/composables/base/usePageList'
import { useModulePermission } from '@/composables/base/useModulePermission'
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
  type SecurityStatsVO,
  type RiskSubjectVO,
  type RiskSubjectPageQuery,
} from '@/api/system/security'
import { formatDateTime } from '@/utils/display/format'
import { showActionSheet, showConfirm } from '@/utils/app-dialog'
import { showSuccessToast } from '@/utils/app-toast'

const { allowed, hasPerm } = useModulePermission('system:security:query')
// 菜单权限与查询权限任一命中即可进入，后端接口仍会做严格校验
const canAccess = computed(() => allowed.value || hasPerm('system:security:list'))
const canUnblock = computed(() => hasPerm('system:security:unblock'))
const canBlock = computed(() => hasPerm('system:security:block'))
const canDelete = computed(() => hasPerm('system:security:delete'))
const canClear = computed(() => hasPerm('system:security:clear'))

const tabs = [
  { key: 'events', label: '拦截事件' },
  { key: 'risk', label: '风险主体' },
]
const activeTab = ref<'events' | 'risk'>('events')
const hasFetched = ref(false)

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

const statItems = computed<StatItem[]>(() => [
  { label: '今日拦截', value: stats.todayTotal, tone: 'primary' },
  { label: '爬虫嗅探', value: stats.todayUaBlock, tone: 'warning' },
  { label: '重放攻击', value: stats.todayReplayBlock, tone: 'info' },
  { label: '蜜罐击中', value: stats.todayHoneypot, tone: 'success' },
  { label: '自动封禁', value: stats.todayForbidden, tone: 'danger' },
  { label: '当前封禁', value: stats.activeBlockedCount, tone: 'danger' },
])

const filters = reactive({
  ipaddr: '',
  eventType: '',
})

const primaryEventTypeOptions = [
  { label: '全部', value: '' },
  { label: '爬虫风控', value: 'UA_BLOCK' },
  { label: '蜜罐诱导', value: 'HONEYPOT' },
]

const extraEventTypeOptions = [
  { label: '重放攻击', value: 'ANTI_REPLAY' },
  { label: '自动封禁', value: 'RISK_BLOCK' },
  { label: '越权探测', value: 'PERMISSION_DENIED' },
  { label: '签名篡改', value: 'SIGN_FAIL' },
  { label: '登录失败', value: 'LOGIN_FAIL' },
  { label: '滑块失败', value: 'CAPTCHA_FAIL' },
  { label: '时间戳超窗', value: 'TIMESTAMP_INVALID' },
  { label: '超大请求体', value: 'BODY_TOO_LARGE' },
  { label: '翻页遍历', value: 'PAGE_SCAN' },
  { label: '批量导出', value: 'BULK_EXPORT' },
  { label: '风控挑战', value: 'RISK_CHALLENGE' },
]

const isExtraEventTypeActive = computed(() => {
  return extraEventTypeOptions.some((item) => item.value === filters.eventType)
})

const moreEventTypeLabel = computed(() => {
  const match = extraEventTypeOptions.find((item) => item.value === filters.eventType)
  return match ? `${match.label} ▾` : '更多 ▾'
})

async function pickExtraEventType() {
  try {
    const index = await showActionSheet({
      title: '选择细分事件类型',
      items: extraEventTypeOptions.map((item) => ({ label: item.label })),
    })
    const selected = extraEventTypeOptions[index]
    if (selected) {
      setEventType(selected.value)
    }
  } catch {
    /* 用户取消选择 */
  }
}

const {
  list,
  loading,
  finished,
  empty,
  refresh,
  loadMore,
  total,
} = usePageList<SecurityEventVO>(async (pageNo, pageSize) => {
  const res = await getSecurityEvents({
    pageNo,
    pageSize,
    ipaddr: filters.ipaddr.trim() || undefined,
    eventType: filters.eventType || undefined,
  })
  return { list: res.data?.list || [], total: res.data?.total || 0 }
})


const riskFilters = reactive<RiskSubjectPageQuery>({
  keyword: '',
  level: undefined,
  blocked: undefined,
})

const riskLevelOptions: Array<{ label: string; value: number | undefined }> = [
  { label: '全部', value: undefined },
  { label: '关注', value: 1 },
  { label: '警告', value: 2 },
  { label: '挑战', value: 3 },
  { label: '临时封禁', value: 4 },
  { label: '最高档封禁', value: 5 },
]

const riskBlockedOptions: Array<{ label: string; value: boolean | undefined }> = [
  { label: '全部', value: undefined },
  { label: '已封禁', value: true },
  { label: '未封禁', value: false },
]

const {
  list: riskList,
  loading: riskLoading,
  finished: riskFinished,
  empty: riskEmpty,
  refresh: refreshRisk,
  loadMore: loadMoreRisk,
} = usePageList<RiskSubjectVO>(async (pageNo, pageSize) => {
  const res = await getRiskSubjects({
    pageNo,
    pageSize,
    keyword: (riskFilters.keyword || '').trim() || undefined,
    level: riskFilters.level,
    blocked: riskFilters.blocked,
  })
  return { list: res.data?.list || [], total: res.data?.total || 0 }
})

const subjectEventsVisible = ref(false)
const subjectEventsLoading = ref(false)
const subjectEvents = ref<SecurityEventVO[]>([])
const activeSubject = ref<RiskSubjectVO | null>(null)

const detailVisible = ref(false)
const activeEvent = ref<SecurityEventVO | null>(null)

const blockVisible = ref(false)
const blocking = ref(false)
const blockForm = reactive({
  principal: '',
  duration: 3600,
  reason: '',
})
const blockDurationOptions = [
  { label: '1 小时', value: 3600 },
  { label: '6 小时', value: 21600 },
  { label: '24 小时', value: 86400 },
  { label: '7 天', value: 604800 },
  { label: '30 天', value: 2592000 },
]
const blockDurationLabel = computed(
  () => blockDurationOptions.find((item) => item.value === blockForm.duration)?.label || '1 小时',
)

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

function eventTypeTone(type: string): 'warning' | 'danger' | 'primary' | 'default' {
  switch (type) {
    case 'UA_BLOCK':
    case 'CAPTCHA_FAIL':
    case 'LOGIN_FAIL':
      return 'warning'
    case 'TIMESTAMP_INVALID':
    case 'BODY_TOO_LARGE':
    case 'RISK_CHALLENGE':
      return 'primary'
    default:
      return 'danger'
  }
}

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

function actionTone(action: string): 'warning' | 'danger' | 'primary' | 'default' {
  switch (action) {
    case 'FORBIDDEN':
      return 'danger'
    case 'CHALLENGE':
    case 'WARNING':
      return 'warning'
    default:
      return 'default'
  }
}

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

async function loadStats() {
  try {
    const res = await getSecurityStats()
    if (res.data) Object.assign(stats, res.data)
  } catch {
    /* 全局错误处理已提示 */
  }
}

async function loadRiskList() {
  await refreshRisk({ silent: true })
}

function applyRiskFilters() {
  void loadRiskList()
}

function setRiskLevel(value: number | undefined) {
  riskFilters.level = value
  void loadRiskList()
}

function setRiskBlocked(value: boolean | undefined) {
  riskFilters.blocked = value
  void loadRiskList()
}

async function openSubjectEvents(item: RiskSubjectVO) {
  activeSubject.value = item
  subjectEvents.value = []
  subjectEventsVisible.value = true
  subjectEventsLoading.value = true
  try {
    const res = await getRiskSubjectEvents(item.principal, 50)
    subjectEvents.value = res.data || []
  } catch {
    /* 全局错误处理已提示 */
  } finally {
    subjectEventsLoading.value = false
  }
}

async function loadEvents() {
  await refresh()
  hasFetched.value = true
}

function applyFilters() {
  activeTab.value = 'events'
  void loadEvents()
}

function resetFilters() {
  filters.ipaddr = ''
  filters.eventType = ''
  void loadEvents()
}

function resetRiskFilters() {
  riskFilters.keyword = ''
  riskFilters.level = undefined
  riskFilters.blocked = undefined
  void loadRiskList()
}

function setEventType(value: string) {
  filters.eventType = value
  void loadEvents()
}

function onScrollLower() {
  if (activeTab.value === 'events') {
    loadMore()
  } else {
    loadMoreRisk()
  }
}

watch(activeTab, (tab) => {
  if (tab === 'events') {
    void loadEvents()
  } else {
    void loadRiskList()
  }
})

function openDetail(item: SecurityEventVO) {
  activeEvent.value = item
  detailVisible.value = true
}

function blockFromDetail() {
  if (!activeEvent.value) return
  const ip = activeEvent.value.ipaddr
  detailVisible.value = false
  openBlock(ip)
}

async function deleteFromDetail() {
  if (!activeEvent.value?.id) return
  const id = activeEvent.value.id
  detailVisible.value = false
  await confirmDelete(id)
}

async function confirmDelete(id: number) {
  const { confirmed } = await showConfirm({
    title: '删除事件',
    content: '确定删除这条安全拦截事件记录吗？',
    confirmText: '删除',
    tone: 'danger',
  })
  if (!confirmed) return
  await deleteSecurityEvent(id)
  showSuccessToast('删除成功')
  await loadEvents()
  await loadStats()
}

async function confirmClear() {
  const { confirmed } = await showConfirm({
    title: '清空安全事件',
    content: '确定清空全部安全拦截事件审计日志吗？清空后不可恢复。',
    confirmText: '确认清空',
    tone: 'danger',
  })
  if (!confirmed) return
  await clearSecurityEvents()
  showSuccessToast('安全事件已清空')
  await loadEvents()
  await loadStats()
}

function formatSubjectTitle(item: RiskSubjectVO) {
  if (item.principalType === 'user' && (item.username || item.nickname)) {
    const name = item.nickname ? `${item.nickname} (${item.username || ''})` : item.username
    return `${name} · ID: ${item.principalId}`
  }
  return item.principal
}

async function handleUnblock(item: RiskSubjectVO) {
  const title = formatSubjectTitle(item)
  const { confirmed } = await showConfirm({
    title: '解除封禁',
    content: `确认解除对风险主体「${title}」的封禁并清零风险分吗？`,
    confirmText: '立即解封',
  })
  if (!confirmed) return
  await unblockRiskSubject(item.principal, true)
  showSuccessToast(`【${title}】已解封并清零风险分`)
  await loadRiskList()
  await loadStats()
}

function openBlock(principal = '') {
  blockForm.principal = principal
  blockForm.duration = 3600
  blockForm.reason = principal ? '管理员从事件明细中手动拉黑' : '管理员手动封禁'
  blockVisible.value = true
}

async function pickDuration() {
  try {
    const index = await showActionSheet({
      title: '选择封禁时长',
      items: blockDurationOptions.map((item) => ({ label: item.label })),
    })
    const option = blockDurationOptions[index]
    if (option) blockForm.duration = option.value
  } catch {
    /* 取消选择 */
  }
}

async function confirmBlock() {
  const principal = blockForm.principal.trim()
  if (!principal) {
    uni.showToast({ title: '请输入待封禁的主体或 IP', icon: 'none' })
    return
  }
  blocking.value = true
  try {
    await blockRiskSubject(principal, blockForm.duration, blockForm.reason.trim() || '管理员手动封禁')
    showSuccessToast('已加入封禁名单')
    blockVisible.value = false
    await loadRiskList()
    await loadStats()
  } finally {
    blocking.value = false
  }
}

onMounted(async () => {
  await loadEvents()
  await Promise.all([loadStats(), loadRiskList()])
})

onPullDownRefresh(async () => {
  if (activeTab.value === 'events') {
    await loadEvents()
  } else {
    await loadRiskList()
  }
  await loadStats()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.security-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* #ifdef H5 */
  height: calc(100vh - var(--window-top, 0px));
  /* #endif */
  box-sizing: border-box;
  overflow: hidden;
}

.page-list__scroll {
  flex: 1;
  min-height: 0;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }
}

.security-tabs {
  margin-bottom: 16rpx;
}

.security-toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 56rpx;
  padding: 0 24rpx;
  margin: 0;
  font-size: 22rpx;
  color: $color-primary;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.28);
  border-radius: 999rpx;

  &::after {
    border: none;
  }
}

.security-toolbar-btn--danger {
  color: $color-danger;
  background: rgba(245, 108, 108, 0.08);
  border-color: rgba(245, 108, 108, 0.28);
}

.security-filter {
  margin-top: 20rpx;
}

.security-filter__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.security-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 4rpx;
}

.security-chips--row {
  flex-wrap: nowrap;
}

.security-chips--row .security-chip {
  flex: 1;
  padding: 10rpx 4rpx;
  font-size: 21rpx;
  text-align: center;
  white-space: nowrap;
}

.security-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: $color-text-secondary;
  background: $color-bg-muted;
  border: 1px solid rgba(226, 232, 240, 0.85);
  transition: all 0.2s ease;
}

.security-chip--active {
  color: $color-primary;
  background: rgba(99, 102, 241, 0.1);
  border-color: rgba(99, 102, 241, 0.35);
  font-weight: $font-weight-semibold;
}

.security-chip--more {
  border-style: dashed;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 24rpx;
}

.filter-actions__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 64rpx;
  padding: 0 32rpx;
  margin: 0;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  background: $color-bg-muted;
  border: none;
  border-radius: 32rpx;

  &::after {
    border: none;
  }
}

.filter-actions__btn--primary {
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #818cf8);
}

.security-panel-extra {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.security-log-meta {
  font-size: 22rpx;
  color: #909399;
}

.security-card {
  margin-top: 16rpx;
}

.security-list-footer {
  padding: 32rpx 0 16rpx;
  text-align: center;
}

.security-footer-text {
  font-size: 22rpx;
  color: $color-text-placeholder;
}

.security-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.security-card__tags {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-wrap: wrap;
}

.security-card__score {
  flex-shrink: 0;
  font-size: $font-size-sm;
  font-weight: $font-weight-bold;
  color: $color-danger;
}

.security-card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-top: 12rpx;
}

.security-card__ip {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.security-card__time {
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.security-card__reason {
  display: block;
  margin-top: 10rpx;
  font-size: $font-size-sm;
  color: $color-text-regular;
  line-height: 1.5;
}

.security-card__endpoint {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  word-break: break-all;
}

.security-card__total {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}

.security-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  margin-top: 16rpx;
}

.security-risk {
  margin-top: 16rpx;
}

.security-risk__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.security-risk__ip {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  font-family: monospace;
}

.security-risk__bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 16rpx;
}

.security-risk__track {
  flex: 1;
  height: 12rpx;
  border-radius: 999rpx;
  background: $color-bg-muted;
  overflow: hidden;
}

.security-risk__fill {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #f59e0b, #f97316);
}

.security-risk__fill--danger {
  background: linear-gradient(90deg, #ef4444, #dc2626);
}

.security-risk__score {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.security-risk__reason {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-regular;
  line-height: 1.5;
}

.security-risk__title {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
}

.security-risk__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx 24rpx;
  margin-top: 12rpx;
}

.security-risk__meta-item {
  display: block;
  margin-top: 4rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.security-subject-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.security-subject-event {
  padding: 20rpx 0;
  border-bottom: 1px solid $color-border-light;
}

.security-subject-event:last-of-type {
  border-bottom: none;
}

.security-subject-event__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.security-subject-loading {
  display: block;
  padding: 32rpx 0;
  text-align: center;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.security-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  background: rgba(0, 0, 0, 0.45);
}

.security-modal__card {
  width: 100%;
  max-height: 80vh;
  overflow-y: auto;
  padding: 36rpx 28rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
  box-sizing: border-box;
}

.security-modal__title {
  display: block;
  margin-bottom: 24rpx;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  text-align: center;
  color: $color-text-primary;
}

.security-detail__row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  padding: 14rpx 0;
  border-bottom: 1px solid $color-border-light;
}

.security-detail__label {
  flex-shrink: 0;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.security-detail__label--block {
  display: block;
  margin-top: 20rpx;
}

.security-detail__value {
  flex: 1;
  text-align: right;
  font-size: $font-size-sm;
  color: $color-text-primary;
  word-break: break-all;
}

.security-detail__reason {
  display: block;
  margin-top: 10rpx;
  font-size: $font-size-sm;
  color: $color-danger;
  line-height: 1.5;
}

.security-detail__ua {
  margin-top: 10rpx;
  padding: 16rpx;
  border-radius: $radius-sm;
  background: $color-bg-muted;
  font-size: 20rpx;
  color: $color-text-secondary;
  line-height: 1.5;
  word-break: break-all;
}

.security-form__label {
  display: block;
  margin: 20rpx 0 10rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.security-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 80rpx;
  padding: 0 20rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-md;
  background: $color-bg-card;
  font-size: $font-size-sm;
  color: $color-text-primary;
  box-sizing: border-box;
}

.security-input--picker {
  cursor: pointer;
}

.security-input__arrow {
  color: $color-text-placeholder;
  font-size: 28rpx;
}

.security-modal__actions {
  display: flex;
  gap: 16rpx;
  margin-top: 28rpx;
}

.security-action-btn {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  color: $color-text-regular;
  font-size: $font-size-sm;
}

.security-action-btn::after {
  border: none;
}

.security-action-btn--danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
}
</style>
