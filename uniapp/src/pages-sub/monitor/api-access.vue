<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <ModuleHero theme="api" title="API 访问统计" :count="stats?.totalCount" subtitle="近 7 日趋势与访问日志" />

    <SegmentTabs v-model="logFilter" :tabs="filterTabs" compact />

    <scroll-view
      scroll-y
      class="page-list__scroll"
      @scrolltolower="loadMore"
    >
      <FadeIn :show="!!stats">
        <ApiAccessTrendChart v-if="stats?.dailyStats" :daily-stats="stats.dailyStats" />

        <StatGrid v-if="summaryItems.length" :items="summaryItems" />

        <MonitorPanel v-if="topPaths.length" title="TOP 路径">
          <view v-for="(p, i) in topPaths" :key="i" class="rank-row">
            <text class="rank-row__rank">{{ i + 1 }}</text>
            <text class="rank-row__main">{{ p.apiPath }}</text>
            <text class="rank-row__count">{{ p.count }}</text>
          </view>
        </MonitorPanel>

        <MonitorPanel v-if="topUsers.length" title="TOP 用户">
          <view v-for="(u, i) in topUsers" :key="u.userId" class="rank-row">
            <text class="rank-row__rank">{{ i + 1 }}</text>
            <text class="rank-row__main">{{ u.username || `用户#${u.userId}` }}</text>
            <text class="rank-row__count">{{ u.count }}</text>
          </view>
        </MonitorPanel>

        <MonitorPanel v-if="methodItems.length" title="请求方法">
          <view v-for="item in methodItems" :key="item.label" class="method-row">
            <text class="method-row__label">{{ item.label }}</text>
            <view class="method-row__track">
              <view class="method-row__fill" :style="{ width: item.width }" />
            </view>
            <text class="method-row__count">{{ item.count }}</text>
          </view>
        </MonitorPanel>
      </FadeIn>

      <text v-if="list.length" class="section-title">访问记录</text>
      <ListCard v-for="row in list" :key="row.id">
        <view class="list-card__top">
          <text class="log-row__title">{{ row.method }} {{ row.apiPath }}</text>
          <DictTag
            :label="row.success === 1 ? '成功' : '失败'"
            :effect="row.success === 1 ? 'success' : 'danger'"
          />
        </view>
        <text class="log-row__sub">
          {{ row.username || '—' }} · {{ row.ip || '—' }} · {{ formatDateTime(row.createTime) }} · {{ row.costTime }}ms
        </text>
      </ListCard>

      <ListFooter :loading="loading" :finished="finished" :empty="empty && !stats" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleHero from '@/components/common/ModuleHero/index.vue'
import MonitorPanel from '@/components/common/MonitorPanel/index.vue'
import StatGrid from '@/components/common/StatGrid/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FadeIn from '@/components/common/FadeIn/index.vue'
import ApiAccessTrendChart from '@/components/business/ApiAccessTrendChart/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import type { StatItem } from '@/components/common/StatGrid/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
import { getApiAccessPage, getApiAccessStatistics } from '@/api/monitor/api-access'
import { formatDate, formatDateTime } from '@/utils/format'
import type { ApiAccessLogRow, ApiAccessStatistics } from '@/types/system'

const { allowed } = useModulePermission('monitor:apiAccess:query')
const stats = ref<ApiAccessStatistics | null>(null)
const logFilter = ref('all')

const filterTabs = [
  { key: 'all', label: '全部' },
  { key: 'success', label: '成功' },
  { key: 'fail', label: '失败' },
]

function dateRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 6)
  return { startDate: formatDate(start), endDate: formatDate(end) }
}

const topPaths = computed(() => (stats.value?.topPaths || []).slice(0, 8))
const topUsers = computed(() => (stats.value?.topUsers || []).slice(0, 8))

const summaryItems = computed<StatItem[]>(() => {
  if (!stats.value) return []
  return [
    { label: '总请求', value: stats.value.totalCount ?? 0, tone: 'primary' },
    { label: '成功', value: stats.value.successCount ?? 0, tone: 'success' },
    { label: '失败', value: stats.value.failCount ?? 0, tone: 'danger' },
  ]
})

const methodItems = computed(() => {
  const map = stats.value?.methodCount || {}
  const entries = Object.entries(map)
  if (!entries.length) return []
  const max = Math.max(...entries.map(([, v]) => v), 1)
  return entries.map(([label, count]) => ({
    label,
    count,
    width: `${Math.max(8, (count / max) * 100)}%`,
  }))
})

const successFilter = computed<number | null>(() => {
  if (logFilter.value === 'success') return 1
  if (logFilter.value === 'fail') return 0
  return null
})

const { list, loading, finished, empty, refresh, loadMore } = usePageList<ApiAccessLogRow>(
  async (pageNo, pageSize) => {
    const res = await getApiAccessPage({
      pageNo,
      pageSize,
      success: successFilter.value,
      ...dateRange(),
    })
    return { list: res.data?.list || [], total: res.data?.total || 0 }
  },
)

watch(logFilter, () => refresh())

async function loadStats() {
  const res = await getApiAccessStatistics(dateRange())
  stats.value = res.data || null
}

async function onRefresh() {
  await Promise.all([refresh(), loadStats()])
}

onMounted(onRefresh)
onPullDownRefresh(async () => {
  await onRefresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page-list__scroll {
  height: calc(100vh - 280rpx);
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 0;
  border-top: 1px solid $color-border-light;

  &:first-child {
    border-top: none;
    padding-top: 0;
  }
}

.rank-row__rank {
  width: 36rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-primary;
}

.rank-row__main {
  flex: 1;
  font-size: $font-size-sm;
  color: $color-text-regular;
  word-break: break-all;
}

.rank-row__count {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.method-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.method-row__label {
  width: 88rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.method-row__track {
  flex: 1;
  height: 16rpx;
  border-radius: $radius-full;
  background: $color-bg-muted;
  overflow: hidden;
}

.method-row__fill {
  height: 100%;
  border-radius: $radius-full;
  background: linear-gradient(90deg, #6366f1, #818cf8);
}

.method-row__count {
  width: 64rpx;
  text-align: right;
  font-size: $font-size-sm;
  color: $color-text-primary;
}

.log-row__title {
  flex: 1;
  font-size: $font-size-sm;
  color: $color-text-primary;
  word-break: break-all;
}

.log-row__sub {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}
</style>
