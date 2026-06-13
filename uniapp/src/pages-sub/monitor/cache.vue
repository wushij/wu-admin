<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list cache-page">
    <ModuleHero theme="cache" title="缓存监控" subtitle="Redis 运行状态与键值管理">
      <template #extra>
        <view class="cache-hero-extra">
          <text class="cache-hero-extra__num">{{ info?.dbSize ?? '—' }}</text>
          <text class="cache-hero-extra__label">键总数</text>
        </view>
      </template>
    </ModuleHero>

    <MonitorToolbar
      :loading="refreshing"
      :auto-refresh="autoRefresh"
      @refresh="manualRefresh"
      @toggle-auto="toggleAuto"
    />

    <SegmentTabs v-model="activeTab" :tabs="sectionTabs" theme="teal" class="cache-tabs" />

    <scroll-view scroll-y class="page-list__scroll">
      <FadeIn :show="!refreshing || !!info">
        <StatGrid v-if="kpiItems.length" class="cache-kpi" :items="kpiItems" />

        <!-- 概览 -->
        <template v-if="info && activeTab === 'overview'">
          <MonitorPanel title="Redis 服务概览">
            <template #extra>
              <text class="cache-tag">实时采集</text>
            </template>
            <view class="info-grid">
              <view class="info-grid__item">
                <text class="info-grid__label">版本</text>
                <text class="info-grid__value">{{ info.redisVersion || '—' }}</text>
              </view>
              <view class="info-grid__item">
                <text class="info-grid__label">模式</text>
                <text class="info-grid__value">{{ info.redisMode || '—' }}</text>
              </view>
              <view class="info-grid__item">
                <text class="info-grid__label">运行天数</text>
                <text class="info-grid__value">{{ info.uptimeInDays ?? '—' }}</text>
              </view>
              <view class="info-grid__item">
                <text class="info-grid__label">键总数</text>
                <text class="info-grid__value">{{ info.dbSize ?? '—' }}</text>
              </view>
              <view class="info-grid__item">
                <text class="info-grid__label">内存占用</text>
                <text class="info-grid__value">{{ info.usedMemoryHuman || '—' }}</text>
              </view>
              <view class="info-grid__item">
                <text class="info-grid__label">内存峰值</text>
                <text class="info-grid__value">{{ info.usedMemoryPeakHuman || '—' }}</text>
              </view>
              <view class="info-grid__item">
                <text class="info-grid__label">当前连接</text>
                <text class="info-grid__value">{{ info.connectedClients ?? '—' }}</text>
              </view>
              <view class="info-grid__item">
                <text class="info-grid__label">当前 QPS</text>
                <text class="info-grid__value">{{ liveQps ?? '—' }}</text>
              </view>
              <view class="info-grid__item">
                <text class="info-grid__label">累计命中率</text>
                <text class="info-grid__value">{{ liveHitRateText }}</text>
              </view>
              <view class="info-grid__item">
                <text class="info-grid__label">累计命令</text>
                <text class="info-grid__value">{{ info.totalCommandsProcessed ?? '—' }}</text>
              </view>
            </view>
          </MonitorPanel>

          <MonitorPanel v-if="stats" title="内存占用">
            <MetricBar
              label="Redis 内存"
              :percent="memoryPercent"
              :display="memoryDisplay"
              :hint="memoryHint"
            />
            <view v-if="stats.keyspaceHits != null || stats.keyspaceMisses != null" class="cache-kv-stats">
              <view class="cache-kv-stats__item">
                <text class="cache-kv-stats__num">{{ stats.keyspaceHits ?? 0 }}</text>
                <text class="cache-kv-stats__label">命中次数</text>
              </view>
              <view class="cache-kv-stats__item">
                <text class="cache-kv-stats__num">{{ stats.keyspaceMisses ?? 0 }}</text>
                <text class="cache-kv-stats__label">未命中次数</text>
              </view>
            </view>
          </MonitorPanel>
        </template>

        <!-- 趋势 -->
        <template v-if="activeTab === 'charts'">
          <MonitorPanel title="采样趋势">
            <MonitorLineChart
              title="QPS"
              :labels="timeLabels"
              :values="qpsHistory"
              color="#67c23a"
              unit=""
              :y-max="qpsYMax"
              :latest-text="liveQps != null ? `当前 ${liveQps}` : undefined"
            />
            <MonitorLineChart
              title="命中率趋势"
              :labels="timeLabels"
              :values="hitRateHistory"
              color="#409eff"
              :latest-text="`累计 ${liveHitRateText}`"
            />
            <MonitorLineChart
              title="连接数趋势"
              :labels="timeLabels"
              :values="clientsHistory"
              color="#e6a23c"
              unit=""
              :y-max="clientsYMax"
              :latest-text="`当前 ${info?.connectedClients ?? stats?.connectedClients ?? '—'}`"
            />
          </MonitorPanel>
        </template>

        <!-- 键列表 -->
        <template v-if="activeTab === 'keys'">
          <MonitorPanel title="缓存键列表">
            <template #extra>
              <text v-if="keysTruncated" class="cache-truncated">已截断</text>
            </template>

            <scroll-view scroll-x class="preset-scroll" :show-scrollbar="false">
              <view class="preset-row">
                <text
                  v-for="preset in patternPresets"
                  :key="preset.pattern"
                  class="preset-chip"
                  :class="{ 'preset-chip--active': searchPattern === preset.pattern }"
                  @click="applyPreset(preset.pattern)"
                >
                  {{ preset.label }}
                </text>
              </view>
            </scroll-view>

            <view class="keys-search">
              <SearchBar
                v-model="searchPattern"
                placeholder="键名模式，如 cache:sys:*"
                @search="loadKeys"
              />
            </view>
            <view class="keys-filter">
              <SearchBar
                v-model="keyFilter"
                placeholder="在当前结果中搜索"
                @search="() => {}"
              />
              <button class="keys-filter__btn" @click="resetKeyFilter">重置</button>
            </view>

            <view class="keys-toolbar">
              <text class="keys-toolbar__meta">
                {{ filteredKeys.length }} / {{ keysResult?.count ?? 0 }} 条
              </text>
              <view class="keys-toolbar__limit">
                <text
                  v-for="limit in scanLimits"
                  :key="limit"
                  class="limit-chip"
                  :class="{ 'limit-chip--active': scanLimit === limit }"
                  @click="setScanLimit(limit)"
                >
                  {{ limit }}
                </text>
              </view>
            </view>

            <ListCard v-for="item in filteredKeys" :key="item.key" class="key-card">
              <view class="key-card__head">
                <text class="key-card__key">{{ item.key }}</text>
                <DictTag :label="item.type || '—'" effect="primary" />
              </view>
              <text class="key-card__meta">TTL · {{ formatCacheTtl(item.ttl) }}</text>
              <view class="key-card__actions">
                <button class="outline-btn outline-btn--primary" @click="viewKey(item.key)">查看</button>
                <button class="outline-btn" @click="copyKey(item.key)">复制</button>
                <button
                  v-if="canDelete"
                  class="outline-btn outline-btn--danger"
                  @click="confirmDeleteKey(item.key)"
                >
                  删除
                </button>
              </view>
            </ListCard>

            <EmptyState
              v-if="!keysLoading && !filteredKeys.length"
              title="暂无匹配的键"
              icon="balance-list-o"
            />
            <ListFooter
              v-else-if="keysLoading"
              :loading="true"
              :finished="false"
              :empty="false"
            />
          </MonitorPanel>
        </template>
      </FadeIn>

      <EmptyState v-if="!refreshing && !info" title="暂无缓存数据" icon="balance-list-o" />
    </scroll-view>

    <AppDialogHost />
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
import MetricBar from '@/components/common/MetricBar/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import FadeIn from '@/components/common/FadeIn/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import type { StatItem } from '@/components/common/StatGrid/index.vue'
import { useModulePermission } from '@/composables/useModulePermission'
import { useAutoRefresh } from '@/composables/useAutoRefresh'
import {
  CACHE_PATTERN_PRESETS,
  calcChartYMax,
  formatCacheTtl,
  useCacheMonitor,
} from '@/composables/useCacheMonitor'
import { deleteCacheKey, scanCacheKeys } from '@/api/monitor/cache'
import { formatPercent } from '@/utils/format'
import { showConfirm } from '@/utils/app-dialog'
import { showSuccessToast } from '@/utils/app-toast'
import type { CacheKeyItem, CacheKeysResult } from '@/types/system'

const { allowed, hasPerm } = useModulePermission('monitor:cache:list')
const canDelete = computed(() => hasPerm('monitor:cache:delete'))

const {
  info,
  stats,
  timeLabels,
  qpsHistory,
  hitRateHistory,
  clientsHistory,
  liveQps,
  liveHitRateText,
  memoryPercent,
  memoryDisplay,
  memoryHint,
  fetchCacheData,
} = useCacheMonitor()

const activeTab = ref<'overview' | 'charts' | 'keys'>('overview')
const sectionTabs = [
  { key: 'overview', label: '概览' },
  { key: 'charts', label: '趋势' },
  { key: 'keys', label: '键列表' },
]

const patternPresets = CACHE_PATTERN_PRESETS
const keys = ref<CacheKeyItem[]>([])
const keysResult = ref<CacheKeysResult | null>(null)
const keysLoading = ref(false)
const keysTruncated = ref(false)
const searchPattern = ref('*')
const keyFilter = ref('')
const scanLimit = ref(200)
const scanLimits = [100, 200, 300, 500]

const qpsYMax = computed(() => calcChartYMax(qpsHistory.value, 10))
const clientsYMax = computed(() => calcChartYMax(clientsHistory.value, 5))

const filteredKeys = computed(() => {
  const q = keyFilter.value.trim().toLowerCase()
  if (!q) return keys.value
  return keys.value.filter((item) => item.key.toLowerCase().includes(q))
})

const kpiItems = computed<StatItem[]>(() => {
  if (!info.value && !stats.value) return []
  const items: StatItem[] = []
  if (stats.value?.cumulativeHitRate != null) {
    items.push({
      label: '累计命中率',
      value: formatPercent(stats.value.cumulativeHitRate * 100),
      tone: 'success',
    })
  } else if (stats.value?.hitRate != null) {
    items.push({
      label: '周期命中率',
      value: formatPercent(stats.value.hitRate * 100),
      tone: 'success',
    })
  }
  if (liveQps.value != null) {
    items.push({ label: '当前 QPS', value: liveQps.value, tone: 'primary' })
  }
  if (info.value?.dbSize != null) {
    items.push({
      label: '键总数',
      value: info.value.dbSize,
      hint: info.value.usedMemoryHuman || undefined,
      tone: 'info',
    })
  }
  if (info.value?.connectedClients != null) {
    items.push({
      label: '连接数',
      value: info.value.connectedClients,
      tone: 'warning',
    })
  }
  return items
})

async function loadKeys() {
  keysLoading.value = true
  try {
    const pattern = searchPattern.value.trim() || '*'
    const res = await scanCacheKeys(pattern, scanLimit.value)
    keysResult.value = res.data || null
    keys.value = res.data?.items || []
    keysTruncated.value = !!res.data?.truncated
  } finally {
    keysLoading.value = false
  }
}

async function fetchAll() {
  await fetchCacheData()
  if (activeTab.value === 'keys') {
    await loadKeys()
  }
}

const { autoRefresh, refreshing, toggleAuto, manualRefresh } = useAutoRefresh(fetchAll, 3000, 'cache')

function applyPreset(pattern: string) {
  if (searchPattern.value === pattern) return
  searchPattern.value = pattern
  keyFilter.value = ''
  loadKeys()
}

function resetKeyFilter() {
  keyFilter.value = ''
}

function setScanLimit(limit: number) {
  if (scanLimit.value === limit) return
  scanLimit.value = limit
  loadKeys()
}

function viewKey(key: string) {
  uni.navigateTo({
    url: `/pages-sub/monitor/cache-key?key=${encodeURIComponent(key)}`,
  })
}

function copyKey(key: string) {
  uni.setClipboardData({
    data: key,
    success: () => showSuccessToast('已复制键名'),
  })
}

async function confirmDeleteKey(key: string) {
  const { confirmed } = await showConfirm({
    title: '删除确认',
    content: `确定删除缓存键「${key}」？此操作不可恢复。`,
    tone: 'danger',
    confirmText: '删除',
  })
  if (!confirmed) return
  await deleteCacheKey(key)
  showSuccessToast('已删除')
  keys.value = keys.value.filter((item) => item.key !== key)
  await fetchCacheData()
}

watch(activeTab, (tab) => {
  if (tab === 'keys' && !keys.value.length && info.value) {
    loadKeys()
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

.cache-page {
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

.cache-tabs {
  width: 100%;
  margin-bottom: 20rpx;
}

.cache-kpi {
  margin-bottom: 8rpx;
}

.cache-kpi :deep(.stat-grid) {
  gap: 20rpx;
}

.cache-kpi :deep(.stat-grid__item) {
  padding: 28rpx 24rpx;
}

.cache-hero-extra {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4rpx;
}

.cache-hero-extra__num {
  font-size: 40rpx;
  font-weight: $font-weight-bold;
  color: #fff;
  line-height: 1.1;
}

.cache-hero-extra__label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.88);
}

.cache-tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: $color-primary;
  background: rgba(99, 102, 241, 0.1);
}

.cache-truncated {
  font-size: $font-size-xs;
  color: $color-warning;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 28rpx 32rpx;
}

.info-grid__item {
  padding: 16rpx 20rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
}

.info-grid__label {
  display: block;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
  margin-bottom: 6rpx;
}

.info-grid__value {
  display: block;
  font-size: $font-size-sm;
  color: $color-text-primary;
  word-break: break-all;
  line-height: 1.45;
}

.cache-kv-stats {
  display: flex;
  gap: 20rpx;
  margin-top: 8rpx;
  padding-top: 20rpx;
  border-top: 1px solid $color-border-light;
}

.cache-kv-stats__item {
  flex: 1;
  padding: 20rpx 16rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  text-align: center;
}

.cache-kv-stats__num {
  display: block;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.cache-kv-stats__label {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.preset-scroll {
  width: 100%;
  margin-bottom: 20rpx;
  white-space: nowrap;
}

.preset-row {
  display: inline-flex;
  gap: 12rpx;
  padding: 2rpx 0;
}

.preset-chip {
  display: inline-flex;
  align-items: center;
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  background: $color-bg-muted;
  border: 1px solid $color-border-light;
}

.preset-chip--active {
  color: $color-primary;
  background: rgba(99, 102, 241, 0.1);
  border-color: rgba(99, 102, 241, 0.35);
  font-weight: $font-weight-semibold;
}

.keys-search {
  margin-bottom: 16rpx;

  :deep(.search-bar) {
    margin-bottom: 0;
  }
}

.keys-filter {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;

  :deep(.search-bar) {
    flex: 1;
    margin-bottom: 0;
  }
}

.keys-filter__btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 72rpx;
  padding: 0 28rpx;
  margin: 0;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  background: $color-bg-muted;
  border: none;
  border-radius: 36rpx;

  &::after {
    border: none;
  }
}

.keys-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.keys-toolbar__meta {
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.keys-toolbar__limit {
  display: flex;
  gap: 8rpx;
}

.limit-chip {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: $color-text-secondary;
  background: $color-bg-muted;
}

.limit-chip--active {
  color: $color-primary;
  background: rgba(99, 102, 241, 0.12);
  font-weight: $font-weight-semibold;
}

.key-card {
  margin-bottom: $card-gap;
}

.key-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.key-card__key {
  flex: 1;
  min-width: 0;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  word-break: break-all;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  line-height: 1.45;
}

.key-card__meta {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.key-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1px solid $color-border-light;

  .outline-btn {
    min-width: auto;
    height: 56rpx;
    padding: 0 24rpx;
    font-size: 22rpx;
  }
}

.cache-page :deep(.monitor-panel) {
  margin-bottom: 24rpx;
}

.cache-page :deep(.monitor-panel__head) {
  margin-bottom: 24rpx;
}
</style>
