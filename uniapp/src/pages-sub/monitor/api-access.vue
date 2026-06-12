<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list api-page">
    <ModuleHero theme="api" title="API 访问统计" subtitle="近 7 日接口访问概览与日志明细">
      <template #extra>
        <view class="api-hero-extra">
          <text class="api-hero-extra__num">{{ stats.totalCount }}</text>
          <text class="api-hero-extra__label">请求总数</text>
        </view>
      </template>
    </ModuleHero>

    <MonitorToolbar
      :loading="refreshing"
      :auto-refresh="autoRefresh"
      @refresh="manualRefresh"
      @toggle-auto="toggleAuto"
    />

    <SegmentTabs v-model="activeTab" :tabs="sectionTabs" theme="teal" class="api-tabs" />

    <scroll-view
      scroll-y
      class="page-list__scroll"
      @scrolltolower="activeTab === 'logs' ? loadMore() : undefined"
    >
      <FadeIn :show="!refreshing || hasFetched">
        <StatGrid v-if="kpiItems.length && activeTab !== 'logs'" class="api-kpi" :items="kpiItems" />

        <!-- 概览 -->
        <template v-if="activeTab === 'overview'">
          <MonitorPanel title="请求方法分布">
            <view v-if="methodItems.length" class="method-list">
              <view v-for="item in methodItems" :key="item.method" class="method-item">
                <view class="method-item__head">
                  <text class="method-item__tag" :style="{ color: item.color, borderColor: item.color }">
                    {{ item.method }}
                  </text>
                  <text class="method-item__count">{{ item.count }}</text>
                </view>
                <view class="method-item__track">
                  <view
                    class="method-item__fill"
                    :style="{ width: `${item.percent}%`, background: item.color }"
                  />
                </view>
              </view>
            </view>
            <EmptyState v-else title="暂无方法统计" icon="chart-trending-o" />
          </MonitorPanel>

          <MonitorPanel title="Top10 API 路径">
            <view v-if="topPaths.length" class="rank-list">
              <view v-for="(item, index) in topPaths" :key="item.apiPath || index" class="rank-item">
                <text class="rank-item__badge" :class="rankBadgeClass(index)">{{ index + 1 }}</text>
                <view class="rank-item__body">
                  <text class="rank-item__title">{{ item.apiPath || '—' }}</text>
                  <view class="rank-item__track">
                    <view
                      class="rank-item__fill rank-item__fill--path"
                      :style="{ width: `${pathBarWidth(item.count)}%` }"
                    />
                  </view>
                </view>
                <text class="rank-item__count">{{ item.count ?? 0 }}</text>
              </view>
            </view>
            <EmptyState v-else title="暂无路径统计" icon="balance-list-o" />
          </MonitorPanel>

          <MonitorPanel title="用户访问排行">
            <template #extra>
              <text class="api-tag">近 7 天</text>
            </template>
            <view v-if="topUsers.length" class="rank-list">
              <view v-for="(item, index) in topUsers" :key="item.userId" class="rank-item">
                <text class="rank-item__badge" :class="rankBadgeClass(index)">{{ index + 1 }}</text>
                <view class="rank-item__body">
                  <text class="rank-item__title">{{ displayUser(item) }}</text>
                  <view class="rank-item__track">
                    <view
                      class="rank-item__fill rank-item__fill--user"
                      :style="{ width: `${userBarWidth(item.count)}%` }"
                    />
                  </view>
                </view>
                <text class="rank-item__count">{{ item.count ?? 0 }}</text>
              </view>
            </view>
            <EmptyState v-else title="暂无用户访问记录" icon="friends-o" />
          </MonitorPanel>
        </template>

        <!-- 趋势 -->
        <template v-if="activeTab === 'charts'">
          <MonitorPanel title="每日请求趋势">
            <template #extra>
              <text class="api-tag">近 7 天</text>
            </template>
            <view class="chart-legend">
              <text class="chart-legend__item chart-legend__item--total">总数</text>
              <text class="chart-legend__item chart-legend__item--success">成功</text>
              <text class="chart-legend__item chart-legend__item--fail">失败</text>
            </view>
            <MonitorLineChart
              title="请求总数"
              :labels="dailyLabels"
              :values="dailyTotalSeries"
              color="#6366f1"
              unit=""
              :y-max="trendYMax"
              :latest-text="`累计 ${stats.totalCount}`"
            />
            <MonitorLineChart
              title="成功请求"
              :labels="dailyLabels"
              :values="dailySuccessSeries"
              color="#67c23a"
              unit=""
              :y-max="trendYMax"
              :latest-text="`累计 ${stats.successCount}`"
            />
            <MonitorLineChart
              title="失败请求"
              :labels="dailyLabels"
              :values="dailyFailSeries"
              color="#f56c6c"
              unit=""
              :y-max="calcFailYMax"
              :latest-text="`累计 ${stats.failCount}`"
            />
          </MonitorPanel>
        </template>

        <!-- 日志 -->
        <template v-if="activeTab === 'logs'">
          <MonitorPanel title="筛选条件">
            <SearchBar
              v-model="logFilters.apiPath"
              placeholder="API 路径关键词"
              @search="applyLogFilters"
            />
            <view class="filter-group">
              <text class="filter-group__label">请求方法</text>
              <scroll-view scroll-x class="filter-scroll" :show-scrollbar="false">
                <view class="filter-row">
                  <text
                    v-for="item in methodFilterOptions"
                    :key="item.value || 'all'"
                    class="filter-chip"
                    :class="{ 'filter-chip--active': logFilters.method === item.value }"
                    @click="setMethodFilter(item.value)"
                  >
                    {{ item.label }}
                  </text>
                </view>
              </scroll-view>
            </view>
            <view class="filter-group">
              <text class="filter-group__label">请求状态</text>
              <view class="filter-row filter-row--wrap">
                <text
                  v-for="item in successFilterOptions"
                  :key="String(item.value)"
                  class="filter-chip"
                  :class="{ 'filter-chip--active': logFilters.success === item.value }"
                  @click="setSuccessFilter(item.value)"
                >
                  {{ item.label }}
                </text>
              </view>
            </view>
            <view v-if="userFilterOptions.length > 1" class="filter-group">
              <text class="filter-group__label">用户</text>
              <scroll-view scroll-x class="filter-scroll" :show-scrollbar="false">
                <view class="filter-row">
                  <text
                    v-for="item in userFilterOptions"
                    :key="String(item.value)"
                    class="filter-chip"
                    :class="{ 'filter-chip--active': logFilters.userId === item.value }"
                    @click="setUserFilter(item.value)"
                  >
                    {{ item.label }}
                  </text>
                </view>
              </scroll-view>
            </view>
            <view class="filter-actions">
              <button class="filter-actions__btn" @click="resetLogFilters">重置</button>
              <button class="filter-actions__btn filter-actions__btn--primary" @click="applyLogFilters">
                搜索
              </button>
            </view>
          </MonitorPanel>

          <MonitorPanel title="访问日志明细">
            <template #extra>
              <text class="api-log-meta">{{ list.length }} 条已加载</text>
            </template>

            <ListCard v-for="row in list" :key="row.id" class="log-card">
              <view class="log-card__head">
                <text class="log-card__method" :class="methodClass(row.method)">{{ row.method || '—' }}</text>
                <DictTag
                  :label="row.success === 1 ? '成功' : '失败'"
                  :effect="row.success === 1 ? 'success' : 'danger'"
                />
              </view>
              <text class="log-card__path">{{ row.apiPath || '—' }}</text>
              <view class="log-card__meta">
                <text>{{ row.username || '—' }}</text>
                <text>{{ row.statusCode ?? '—' }}</text>
                <text>{{ row.costTime ?? 0 }}ms</text>
                <text>{{ row.ip || '—' }}</text>
              </view>
              <text class="log-card__time">{{ formatLogTime(row) }}</text>
            </ListCard>

            <EmptyState
              v-if="!loading && !list.length"
              title="暂无访问日志"
              icon="balance-list-o"
            />
            <ListFooter
              v-else
              :loading="loading"
              :finished="finished"
              :empty="false"
            />
          </MonitorPanel>
        </template>
      </FadeIn>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleHero from '@/components/common/ModuleHero/index.vue'
import MonitorToolbar from '@/components/common/MonitorToolbar/index.vue'
import MonitorPanel from '@/components/common/MonitorPanel/index.vue'
import MonitorLineChart from '@/components/common/MonitorLineChart/index.vue'
import StatGrid from '@/components/common/StatGrid/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FadeIn from '@/components/common/FadeIn/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import type { StatItem } from '@/components/common/StatGrid/index.vue'
import { useModulePermission } from '@/composables/useModulePermission'
import { useAutoRefresh } from '@/composables/useAutoRefresh'
import { usePageList } from '@/composables/usePageList'
import {
  calcChartYMax,
  useApiAccessMonitor,
} from '@/composables/useApiAccessMonitor'
import { getApiAccessPage } from '@/api/monitor/api-access'
import { formatDateTime, formatPercent } from '@/utils/format'
import type { ApiAccessLogRow } from '@/types/system'

const { allowed } = useModulePermission('monitor:apiAccess:query')

const {
  stats,
  dailyLabels,
  dailyTotalSeries,
  dailySuccessSeries,
  dailyFailSeries,
  trendYMax,
  successRate,
  methodItems,
  topPaths,
  topUsers,
  maxPathCount,
  maxUserCount,
  fetchStats,
} = useApiAccessMonitor()

const activeTab = ref<'overview' | 'charts' | 'logs'>('overview')
const hasFetched = ref(false)
const sectionTabs = [
  { key: 'overview', label: '概览' },
  { key: 'charts', label: '趋势' },
  { key: 'logs', label: '日志' },
]

const logFilters = ref({
  apiPath: '',
  method: null as string | null,
  success: null as number | null,
  userId: null as number | null,
})

const methodFilterOptions = [
  { label: '全部', value: null },
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
]

const successFilterOptions = [
  { label: '全部', value: null },
  { label: '成功', value: 1 },
  { label: '失败', value: 0 },
]

const calcFailYMax = computed(() => calcChartYMax(dailyFailSeries.value, 3))

const kpiItems = computed<StatItem[]>(() => [
  { label: '请求总数', value: stats.value.totalCount, tone: 'primary' },
  { label: '成功', value: stats.value.successCount, tone: 'success' },
  {
    label: '失败',
    value: stats.value.failCount,
    tone: 'danger',
  },
  {
    label: '成功率',
    value: successRate.value != null ? formatPercent(successRate.value) : '—',
    tone: 'info',
  },
])

const userFilterOptions = computed(() => {
  const options = [{ label: '全部用户', value: null as number | null }]
  for (const user of topUsers.value) {
    options.push({
      label: user.username || `用户#${user.userId}`,
      value: user.userId,
    })
  }
  return options
})

const { list, loading, finished, refresh, loadMore } = usePageList<ApiAccessLogRow>(
  async (pageNo, pageSize) => {
    const path = logFilters.value.apiPath.trim()
    const res = await getApiAccessPage({
      pageNo,
      pageSize,
      userId: logFilters.value.userId ?? undefined,
      apiPath: path || undefined,
      method: logFilters.value.method ?? undefined,
      success: logFilters.value.success ?? undefined,
    })
    return { list: res.data?.list || [], total: res.data?.total || 0 }
  },
)

async function fetchAll() {
  await fetchStats()
  hasFetched.value = true
  if (activeTab.value === 'logs') {
    await refresh()
  }
}

const { autoRefresh, refreshing, toggleAuto, manualRefresh } = useAutoRefresh(fetchAll, 10000, 'api-access')

function displayUser(user: { userId: number; username?: string }) {
  return user.username || `用户#${user.userId}`
}

function rankBadgeClass(index: number) {
  if (index === 0) return 'rank-item__badge--gold'
  if (index === 1) return 'rank-item__badge--silver'
  if (index === 2) return 'rank-item__badge--bronze'
  return ''
}

function pathBarWidth(count?: number) {
  return Math.max(8, ((count ?? 0) / maxPathCount.value) * 100)
}

function userBarWidth(count?: number) {
  return Math.max(8, ((count ?? 0) / maxUserCount.value) * 100)
}

function methodClass(method?: string) {
  if (!method) return ''
  return `log-card__method--${method.toLowerCase()}`
}

function formatLogTime(row: ApiAccessLogRow) {
  return formatDateTime(row.startTime || row.createTime, true)
}

function setMethodFilter(value: string | null) {
  logFilters.value.method = value
  applyLogFilters()
}

function setSuccessFilter(value: number | null) {
  logFilters.value.success = value
  applyLogFilters()
}

function setUserFilter(value: number | null) {
  logFilters.value.userId = value
  applyLogFilters()
}

function applyLogFilters() {
  refresh()
}

function resetLogFilters() {
  logFilters.value = {
    apiPath: '',
    method: null,
    success: null,
    userId: null,
  }
  refresh()
}

watch(activeTab, (tab) => {
  if (tab === 'logs' && !list.value.length) {
    refresh()
  }
})

onMounted(manualRefresh)

onPullDownRefresh(async () => {
  await manualRefresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.api-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  box-sizing: border-box;
}

.page-list__scroll {
  flex: 1;
  min-height: 0;
  padding-bottom: 32rpx;
}

.api-tabs {
  width: 100%;
  margin-bottom: 20rpx;
}

.api-kpi {
  margin-bottom: 8rpx;
}

.api-kpi :deep(.stat-grid) {
  gap: 20rpx;
}

.api-kpi :deep(.stat-grid__item) {
  padding: 28rpx 24rpx;
}

.api-hero-extra {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4rpx;
}

.api-hero-extra__num {
  font-size: 40rpx;
  font-weight: $font-weight-bold;
  color: #fff;
  line-height: 1.1;
}

.api-hero-extra__label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.88);
}

.api-tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: $color-primary;
  background: rgba(99, 102, 241, 0.1);
}

.api-log-meta {
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.method-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.method-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.method-item__tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  border: 1px solid currentColor;
  font-size: 22rpx;
  font-weight: $font-weight-semibold;
}

.method-item__count {
  font-size: $font-size-sm;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.method-item__track {
  height: 16rpx;
  border-radius: 999rpx;
  background: $color-bg-muted;
  overflow: hidden;
}

.method-item__fill {
  height: 100%;
  min-width: 0;
  border-radius: 999rpx;
  transition: width 0.35s ease;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.rank-item {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}

.rank-item__badge {
  flex-shrink: 0;
  width: 40rpx;
  height: 40rpx;
  border-radius: 12rpx;
  background: $color-bg-muted;
  color: $color-text-secondary;
  font-size: 22rpx;
  font-weight: $font-weight-bold;
  line-height: 40rpx;
  text-align: center;
}

.rank-item__badge--gold {
  background: rgba(230, 162, 60, 0.18);
  color: #e6a23c;
}

.rank-item__badge--silver {
  background: rgba(144, 147, 153, 0.16);
  color: #909399;
}

.rank-item__badge--bronze {
  background: rgba(205, 127, 50, 0.16);
  color: #cd7f32;
}

.rank-item__body {
  flex: 1;
  min-width: 0;
}

.rank-item__title {
  display: block;
  font-size: $font-size-sm;
  color: $color-text-primary;
  word-break: break-all;
  line-height: 1.45;
}

.rank-item__track {
  height: 12rpx;
  margin-top: 12rpx;
  border-radius: 999rpx;
  background: $color-bg-muted;
  overflow: hidden;
}

.rank-item__fill {
  height: 100%;
  min-width: 0;
  border-radius: 999rpx;
  transition: width 0.35s ease;
}

.rank-item__fill--path {
  background: linear-gradient(90deg, #6366f1, #818cf8);
}

.rank-item__fill--user {
  background: linear-gradient(90deg, #67c23a, #85ce61);
}

.rank-item__count {
  flex-shrink: 0;
  min-width: 56rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  text-align: right;
}

.chart-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.chart-legend__item {
  position: relative;
  padding-left: 24rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    width: 14rpx;
    height: 14rpx;
    border-radius: 50%;
    transform: translateY(-50%);
  }
}

.chart-legend__item--total::before {
  background: #6366f1;
}

.chart-legend__item--success::before {
  background: #67c23a;
}

.chart-legend__item--fail::before {
  background: #f56c6c;
}

.filter-group {
  margin-top: 20rpx;
}

.filter-group__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.filter-scroll {
  width: 100%;
  white-space: nowrap;
}

.filter-row {
  display: inline-flex;
  gap: 12rpx;
}

.filter-row--wrap {
  display: flex;
  flex-wrap: wrap;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  background: $color-bg-muted;
  border: 1px solid $color-border-light;
}

.filter-chip--active {
  color: $color-primary;
  background: rgba(99, 102, 241, 0.1);
  border-color: rgba(99, 102, 241, 0.35);
  font-weight: $font-weight-semibold;
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

.log-card {
  margin-bottom: $card-gap;
}

.log-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.log-card__method {
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  font-weight: $font-weight-bold;
  color: $color-text-secondary;
  background: $color-bg-muted;
}

.log-card__method--get {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.12);
}

.log-card__method--post {
  color: #409eff;
  background: rgba(64, 158, 255, 0.12);
}

.log-card__method--put {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.12);
}

.log-card__method--delete {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.12);
}

.log-card__path {
  display: block;
  margin-top: 16rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  word-break: break-all;
  line-height: 1.45;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.log-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx 20rpx;
  margin-top: 16rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.log-card__time {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}

.api-page :deep(.monitor-panel) {
  margin-bottom: 24rpx;
}

.api-page :deep(.monitor-panel__head) {
  margin-bottom: 24rpx;
}

.api-page :deep(.search-bar) {
  margin-bottom: 0;
}
</style>
