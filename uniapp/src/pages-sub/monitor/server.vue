<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <ModuleHero theme="server" title="服务监控" subtitle="CPU · 内存 · 磁盘 · JVM" />
    <MonitorToolbar
      :loading="refreshing"
      :auto-refresh="autoRefresh"
      @refresh="manualRefresh"
      @toggle-auto="toggleAuto"
    />

    <scroll-view
      scroll-y
      class="page-list__scroll"
    >
      <FadeIn :show="!refreshing || !!info">
        <StatGrid v-if="kpiItems.length" :items="kpiItems" />

        <MonitorPanel v-if="info" title="实时指标">
          <view class="ring-grid">
            <RingProgress label="系统 CPU" :percent="info.cpu?.systemCpuPercent" />
            <RingProgress label="堆内存" :percent="info.memory?.heapUsedPercent" />
            <RingProgress
              label="主磁盘"
              :percent="primaryDisk?.usedPercent"
              :display="primaryDisk ? `${primaryDisk.usedPercent?.toFixed(0) ?? 0}%` : '—'"
            />
          </view>
          <MiniSparkline
            title="CPU 采样趋势"
            :values="cpuHistory"
            :latest-label="`当前 ${formatPercent(info.cpu?.systemCpuPercent)}`"
          />
          <MiniSparkline
            title="堆内存趋势"
            :values="heapHistory"
            :latest-label="`当前 ${formatPercent(info.memory?.heapUsedPercent)}`"
          />
        </MonitorPanel>

        <MonitorPanel v-if="info" title="资源占用">
          <MetricBar label="进程 CPU" :percent="info.cpu?.processCpuPercent" />
          <MetricBar label="物理内存" :percent="info.memory?.physicalUsedPercent" :display="physicalMemText" />
          <MetricBar label="堆内存详情" :percent="info.memory?.heapUsedPercent" :display="heapMemText" />
          <view v-for="disk in info.disks || []" :key="disk.path">
            <MetricBar
              :label="`磁盘 ${disk.path}`"
              :percent="disk.usedPercent"
              :display="`${disk.used || '—'} / ${disk.total || '—'}`"
            />
          </view>
        </MonitorPanel>

        <MonitorPanel v-if="info" title="系统信息">
          <DetailRow label="主机名" :value="info.sys?.hostName" />
          <DetailRow label="IP 地址" :value="info.sys?.hostAddress" />
          <DetailRow label="操作系统" :value="osText" />
          <DetailRow label="CPU 核心" :value="info.cpu?.availableProcessors" />
          <DetailRow label="系统负载" :value="loadText" />
          <DetailRow label="JVM" :value="info.jvm?.name || info.jvm?.version" />
          <DetailRow label="JVM 厂商" :value="info.jvm?.vendor" />
          <DetailRow label="启动时间" :value="info.jvm?.startTime" />
          <DetailRow label="运行时长" :value="info.jvm?.uptime" />
          <DetailRow label="Java 版本" :value="info.sys?.javaVersion || info.jvm?.version" />
          <DetailRow label="工作目录" :value="info.sys?.userDir" />
        </MonitorPanel>
      </FadeIn>

      <EmptyState v-if="!refreshing && !info" title="暂无监控数据" icon="desktop-o" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleHero from '@/components/common/ModuleHero/index.vue'
import MonitorToolbar from '@/components/common/MonitorToolbar/index.vue'
import MonitorPanel from '@/components/common/MonitorPanel/index.vue'
import StatGrid from '@/components/common/StatGrid/index.vue'
import RingProgress from '@/components/common/RingProgress/index.vue'
import MetricBar from '@/components/common/MetricBar/index.vue'
import DetailRow from '@/components/common/DetailRow/index.vue'
import MiniSparkline from '@/components/common/MiniSparkline/index.vue'
import FadeIn from '@/components/common/FadeIn/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import type { StatItem } from '@/components/common/StatGrid/index.vue'
import { useModulePermission } from '@/composables/useModulePermission'
import { useAutoRefresh } from '@/composables/useAutoRefresh'
import { getServerInfo } from '@/api/monitor/server'
import { formatPercent } from '@/utils/format'
import type { ServerInfo } from '@/types/system'

const { allowed } = useModulePermission('monitor:server:list')
const info = ref<ServerInfo | null>(null)
const cpuHistory = ref<number[]>([])
const heapHistory = ref<number[]>([])
const MAX_POINTS = 12

const primaryDisk = computed(() => info.value?.disks?.[0])

const physicalMemText = computed(() => {
  const m = info.value?.memory
  if (!m) return '—'
  return `${m.physicalUsed || '—'} / ${m.physicalTotal || '—'}`
})

const heapMemText = computed(() => {
  const m = info.value?.memory
  if (!m) return '—'
  return `${m.heapUsed || '—'} / ${m.heapMax || '—'}`
})

const osText = computed(() => {
  const s = info.value?.sys
  if (!s) return '—'
  return [s.osName, s.osVersion].filter(Boolean).join(' ')
})

const loadText = computed(() => {
  const load = info.value?.cpu?.systemLoadAverage
  if (load == null || load < 0) return '—'
  return load.toFixed(2)
})

const kpiItems = computed<StatItem[]>(() => {
  if (!info.value) return []
  const disks = info.value.disks || []
  const maxDisk = disks.length ? Math.max(...disks.map((d) => d.usedPercent ?? 0)) : null
  return [
    { label: '系统 CPU', value: formatPercent(info.value.cpu?.systemCpuPercent), tone: 'primary' },
    { label: '堆内存', value: formatPercent(info.value.memory?.heapUsedPercent), tone: 'warning' },
    { label: '物理内存', value: formatPercent(info.value.memory?.physicalUsedPercent), tone: 'info' },
    { label: '磁盘峰值', value: maxDisk != null ? formatPercent(maxDisk) : '—', tone: 'danger' },
  ]
})

function pushHistory(list: number[], value?: number | null) {
  if (value == null || Number.isNaN(value)) return
  const next = [...list, value]
  if (next.length > MAX_POINTS) next.shift()
  return next
}

async function fetchData() {
  const res = await getServerInfo()
  info.value = res.data || null
  if (info.value) {
    cpuHistory.value = pushHistory(cpuHistory.value, info.value.cpu?.systemCpuPercent) || cpuHistory.value
    heapHistory.value = pushHistory(heapHistory.value, info.value.memory?.heapUsedPercent) || heapHistory.value
  }
}

const { autoRefresh, refreshing, toggleAuto, manualRefresh } = useAutoRefresh(fetchData, 10000, 'server')

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
  height: calc(100vh - 320rpx);
}

.ring-grid {
  display: flex;
  justify-content: space-around;
  gap: 16rpx;
  margin-bottom: 16rpx;
}
</style>
