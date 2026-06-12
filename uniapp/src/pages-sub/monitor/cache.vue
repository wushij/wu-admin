<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <ModuleHero theme="cache" title="缓存监控" subtitle="Redis 状态与 Key 管理" />
    <MonitorToolbar
      :loading="refreshing"
      :auto-refresh="autoRefresh"
      @refresh="manualRefresh"
      @toggle-auto="toggleAuto"
    />

    <SegmentTabs v-model="patternKey" :tabs="patternTabs" scroll />
    <SearchBar v-model="keyword" placeholder="Key 通配符，如 cache:sys:*" @search="loadKeys" />

    <scroll-view
      scroll-y
      class="page-list__scroll"
    >
      <FadeIn :show="!!info">
        <StatGrid v-if="statItems.length" :items="statItems" />

        <MonitorPanel v-if="info" title="Redis 详情">
          <DetailRow label="版本" :value="info.redisVersion" />
          <DetailRow label="模式" :value="info.redisMode" />
          <DetailRow label="端口" :value="info.tcpPort" />
          <DetailRow label="运行天数" :value="info.uptimeInDays" />
          <DetailRow label="连接数" :value="info.connectedClients" />
          <DetailRow label="Key 数量" :value="info.dbSize" />
          <DetailRow label="内存占用" :value="info.usedMemoryHuman" />
          <DetailRow label="峰值内存" :value="info.usedMemoryPeakHuman" />
          <DetailRow label="QPS" :value="info.instantaneousOpsPerSec" />
          <DetailRow label="累计命令" :value="info.totalCommandsProcessed" />
        </MonitorPanel>
      </FadeIn>

      <view v-if="keysResult" class="keys-head">
        <text class="section-title">Key 列表</text>
        <text class="keys-head__meta">{{ keysResult.count }} 条{{ keysResult.truncated ? '（已截断）' : '' }}</text>
      </view>

      <ListCard v-for="item in keys" :key="item.key" @click="onKeyTap(item)" @longpress="onKeyLongPress(item)">
        <view class="list-card__top">
          <text class="key-row__key">{{ item.key }}</text>
          <DictTag :label="item.type || '—'" effect="primary" />
        </view>
        <text class="key-row__meta">TTL {{ formatTtl(item.ttl) }}</text>
        <text v-if="canDelete" class="key-row__hint">点击查看 · 长按删除</text>
      </ListCard>

      <EmptyState v-if="!refreshing && !info" title="暂无缓存数据" icon="balance-list-o" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleHero from '@/components/common/ModuleHero/index.vue'
import MonitorToolbar from '@/components/common/MonitorToolbar/index.vue'
import MonitorPanel from '@/components/common/MonitorPanel/index.vue'
import StatGrid from '@/components/common/StatGrid/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import DetailRow from '@/components/common/DetailRow/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FadeIn from '@/components/common/FadeIn/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import type { StatItem } from '@/components/common/StatGrid/index.vue'
import { useModulePermission } from '@/composables/useModulePermission'
import { useAutoRefresh } from '@/composables/useAutoRefresh'
import { deleteCacheKey, getCacheInfo, getCacheStats, getCacheValue, scanCacheKeys } from '@/api/monitor/cache'
import { formatPercent } from '@/utils/format'
import type { CacheInfo, CacheKeyItem, CacheKeysResult, CacheStats } from '@/types/system'

const { allowed, hasPerm } = useModulePermission('monitor:cache:list')
const canDelete = computed(() => hasPerm('monitor:cache:delete'))

const info = ref<CacheInfo | null>(null)
const stats = ref<CacheStats | null>(null)
const keys = ref<CacheKeyItem[]>([])
const keysResult = ref<CacheKeysResult | null>(null)
const keyword = ref('*')
const patternKey = ref('all')
const scanLimit = 200

const patternTabs = [
  { key: 'all', label: '全部' },
  { key: 'sys', label: '系统' },
  { key: 'user', label: '用户' },
  { key: 'custom', label: '自定义' },
]

const statItems = computed<StatItem[]>(() => {
  const items: StatItem[] = []
  if (stats.value?.hitRate != null) {
    items.push({ label: '命中率', value: formatPercent(stats.value.hitRate * 100), tone: 'success' })
  }
  if (stats.value?.ops != null) {
    items.push({ label: 'OPS', value: stats.value.ops, tone: 'primary' })
  }
  if (info.value?.dbSize != null) {
    items.push({ label: 'Key 数', value: info.value.dbSize, tone: 'info' })
  }
  if (info.value?.connectedClients != null) {
    items.push({ label: '连接数', value: info.value.connectedClients, tone: 'warning' })
  }
  return items
})

function formatTtl(ttl?: number) {
  if (ttl == null || ttl < 0) return '永久'
  if (ttl === 0) return '即将过期'
  return `${ttl}s`
}

function onPatternChange(key: string) {
  if (key === 'all') keyword.value = '*'
  else if (key === 'sys') keyword.value = 'cache:sys:*'
  else if (key === 'user') keyword.value = 'cache:user:*'
  loadKeys()
}

watch(patternKey, (key) => onPatternChange(key))

async function loadKeys() {
  const p = keyword.value.trim() || '*'
  const keysRes = await scanCacheKeys(p, scanLimit)
  keysResult.value = keysRes.data || null
  keys.value = keysRes.data?.items || []
}

async function fetchData() {
  const [infoRes, statsRes] = await Promise.all([getCacheInfo(), getCacheStats()])
  info.value = infoRes.data || null
  stats.value = statsRes.data || null
  await loadKeys()
}

const { autoRefresh, refreshing, toggleAuto, manualRefresh } = useAutoRefresh(fetchData, 10000, 'cache')

async function onKeyTap(item: CacheKeyItem) {
  try {
    const res = await getCacheValue(item.key)
    const detail = res.data
    const text = detail?.value != null ? JSON.stringify(detail.value, null, 2) : '—'
    uni.showModal({
      title: item.key,
      content: `类型：${detail?.type || item.type}\nTTL：${formatTtl(detail?.ttl ?? item.ttl)}\n\n${text.slice(0, 800)}`,
      showCancel: false,
    })
  } catch {
    uni.showToast({ title: '读取失败', icon: 'none' })
  }
}

function onKeyLongPress(item: CacheKeyItem) {
  if (!canDelete.value) return
  uni.showModal({
    title: '删除 Key',
    content: `确定删除 ${item.key}？`,
    confirmColor: '#f56c6c',
    success: async (res) => {
      if (!res.confirm) return
      await deleteCacheKey(item.key)
      uni.showToast({ title: '已删除', icon: 'success' })
      await loadKeys()
    },
  })
}

onMounted(manualRefresh)
onPullDownRefresh(async () => {
  await manualRefresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page-list__scroll {
  height: calc(100vh - 420rpx);
}

.keys-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 8rpx 0 16rpx;
}

.keys-head__meta {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.key-row__key {
  flex: 1;
  font-size: $font-size-sm;
  color: $color-text-primary;
  word-break: break-all;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.key-row__meta {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.key-row__hint {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}
</style>
