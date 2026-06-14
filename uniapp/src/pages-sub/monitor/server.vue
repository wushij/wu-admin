<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list server-page">
    <ModuleDarkHero
      title="服务器监控"
      subtitle="JMX 实时采集本机 CPU、内存、磁盘与 JVM"
      icon="desktop-o"
      theme="server"
      :count="cpuDisplay"
      count-label="CPU"
    />

    <MonitorToolbar
      :loading="refreshing"
      :auto-refresh="autoRefresh"
      @refresh="manualRefresh"
      @toggle-auto="toggleAuto"
    />

    <SegmentTabs v-model="activeTab" :tabs="sectionTabs" theme="teal" class="server-tabs" />

    <scroll-view scroll-y class="page-list__scroll">
      <FadeIn :show="!refreshing || !!info">
        <StatGrid v-if="kpiItems.length" class="server-kpi" :items="kpiItems" />

        <MonitorPanel v-if="info && activeTab === 'overview'" title="本机服务状态">
          <template #extra>
            <text class="server-tag">JMX 实时采集</text>
          </template>
          <view class="host-grid">
            <view class="host-grid__item">
              <text class="host-grid__label">主机名</text>
              <text class="host-grid__value">{{ info.sys?.hostName || '—' }}</text>
            </view>
            <view class="host-grid__item">
              <text class="host-grid__label">IP</text>
              <text class="host-grid__value">{{ info.sys?.hostAddress || '—' }}</text>
            </view>
            <view class="host-grid__item">
              <text class="host-grid__label">操作系统</text>
              <text class="host-grid__value">{{ info.sys?.osName || '—' }}</text>
            </view>
            <view class="host-grid__item">
              <text class="host-grid__label">Java</text>
              <text class="host-grid__value">{{ info.sys?.javaVersion || '—' }}</text>
            </view>
          </view>
        </MonitorPanel>

        <!-- 概览 -->
        <template v-if="info && activeTab === 'overview'">
          <MonitorPanel title="趋势采样">
            <MonitorLineChart
              title="CPU 使用率趋势"
              :labels="timeLabels"
              :values="cpuHistory"
              color="#409eff"
              :latest-text="`当前 ${cpuDisplay}`"
            />
            <MonitorLineChart
              title="JVM 堆内存趋势"
              :labels="timeLabels"
              :values="heapHistory"
              color="#67c23a"
              :latest-text="`当前 ${heapDisplay}`"
            />
          </MonitorPanel>

          <MonitorPanel title="实时占用">
            <MetricBar label="系统 CPU" :percent="info.cpu?.systemCpuPercent" />
            <MetricBar label="进程 CPU" :percent="info.cpu?.processCpuPercent" />
            <MetricBar
              label="物理内存"
              :percent="info.memory?.physicalUsedPercent"
              :hint="physicalMemText"
            />
            <MetricBar
              label="JVM 堆内存"
              :percent="info.memory?.heapUsedPercent"
              :hint="heapMemText"
            />
          </MonitorPanel>
        </template>

        <!-- 详情（对齐 PC 四卡片） -->
        <template v-if="info && activeTab === 'detail'">
          <MonitorPanel title="CPU 信息" class="server-section server-section--cpu">
            <DetailRow label="操作系统" :value="info.cpu?.name" />
            <DetailRow label="架构" :value="info.cpu?.arch" />
            <DetailRow label="核心数" :value="info.cpu?.availableProcessors" />
            <DetailRow label="系统 CPU" :value="formatPercent(info.cpu?.systemCpuPercent)" />
            <DetailRow label="进程 CPU" :value="formatPercent(info.cpu?.processCpuPercent)" />
            <DetailRow label="平均负载" :value="loadText" />
          </MonitorPanel>

          <MonitorPanel title="内存信息" class="server-section server-section--memory">
            <DetailRow label="堆已用" :value="info.memory?.heapUsed" />
            <DetailRow label="堆最大" :value="info.memory?.heapMax" />
            <DetailRow label="堆提交" :value="info.memory?.heapCommitted" />
            <DetailRow label="非堆已用" :value="info.memory?.nonHeapUsed" />
            <DetailRow label="物理总量" :value="info.memory?.physicalTotal" />
            <DetailRow label="物理可用" :value="info.memory?.physicalFree" />
          </MonitorPanel>

          <MonitorPanel title="JVM 信息" class="server-section server-section--jvm">
            <DetailRow label="名称" :value="info.jvm?.name" />
            <DetailRow label="厂商" :value="info.jvm?.vendor" />
            <DetailRow label="版本" :value="info.jvm?.version" />
            <DetailRow label="规范版本" :value="info.jvm?.specVersion" />
            <DetailRow label="启动时间" :value="info.jvm?.startTime" />
            <DetailRow label="运行时长" :value="info.jvm?.uptime" />
          </MonitorPanel>

          <MonitorPanel title="服务器信息" class="server-section server-section--sys">
            <DetailRow label="主机名" :value="info.sys?.hostName" />
            <DetailRow label="IP 地址" :value="info.sys?.hostAddress" />
            <DetailRow label="操作系统" :value="info.sys?.osName" />
            <DetailRow label="系统版本" :value="info.sys?.osVersion" />
            <DetailRow label="Java 版本" :value="info.sys?.javaVersion" />
            <DetailRow label="工作目录" :value="info.sys?.userDir" />
          </MonitorPanel>
        </template>

        <!-- 磁盘 -->
        <template v-if="info && activeTab === 'disk'">
          <MonitorPanel title="磁盘信息">
            <EmptyState
              v-if="!(info.disks || []).length"
              title="暂无磁盘数据"
              icon="desktop-o"
            />
            <view v-for="disk in info.disks || []" :key="disk.path || disk.total" class="disk-card">
              <view class="disk-card__head">
                <text class="disk-card__path">{{ disk.path || '—' }}</text>
                <text class="disk-card__pct" :class="`disk-card__pct--${diskProgressTone(disk.usedPercent)}`">
                  {{ formatDiskPercent(disk.usedPercent) }}
                </text>
              </view>
              <view class="disk-card__track">
                <view
                  class="disk-card__fill"
                  :class="`disk-card__fill--${diskProgressTone(disk.usedPercent)}`"
                  :style="{ width: `${Math.min(100, Math.max(0, disk.usedPercent ?? 0))}%` }"
                />
              </view>
              <view class="disk-card__meta">
                <view class="disk-card__meta-item">
                  <text class="disk-card__meta-label">总容量</text>
                  <text class="disk-card__meta-value">{{ disk.total || '—' }}</text>
                </view>
                <view class="disk-card__meta-item">
                  <text class="disk-card__meta-label">已用</text>
                  <text class="disk-card__meta-value">{{ disk.used || '—' }}</text>
                </view>
                <view class="disk-card__meta-item">
                  <text class="disk-card__meta-label">可用</text>
                  <text class="disk-card__meta-value">{{ disk.free || '—' }}</text>
                </view>
              </view>
            </view>
          </MonitorPanel>
        </template>
      </FadeIn>

      <EmptyState v-if="!refreshing && !info" title="暂无监控数据" icon="desktop-o" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import MonitorToolbar from '@/components/common/MonitorToolbar/index.vue'
import MonitorPanel from '@/components/common/MonitorPanel/index.vue'
import MonitorLineChart from '@/components/common/MonitorLineChart/index.vue'
import StatGrid from '@/components/common/StatGrid/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import MetricBar from '@/components/common/MetricBar/index.vue'
import DetailRow from '@/components/common/DetailRow/index.vue'
import FadeIn from '@/components/common/FadeIn/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import type { StatItem } from '@/components/common/StatGrid/index.vue'
import { useModulePermission } from '@/composables/useModulePermission'
import { useMonitorToolbarRefresh } from '@/composables/useMonitorToolbarRefresh'
import { diskProgressTone, useServerMonitor } from '@/composables/useServerMonitor'
import { formatPercent } from '@/utils/format'

const { allowed } = useModulePermission('monitor:server:list')

const {
  info,
  timeLabels,
  cpuHistory,
  heapHistory,
  cpuDisplay,
  heapDisplay,
  physicalDisplay,
  maxDiskPercent,
  fetchServerInfo,
  autoRefresh,
  setAutoRefresh,
} = useServerMonitor()

const activeTab = ref<'overview' | 'detail' | 'disk'>('overview')

const sectionTabs = [
  { key: 'overview', label: '概览' },
  { key: 'detail', label: '详情' },
  { key: 'disk', label: '磁盘' },
]

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

const loadText = computed(() => {
  const load = info.value?.cpu?.systemLoadAverage
  if (load == null || load < 0) return '—'
  return load.toFixed(2)
})

const kpiItems = computed<StatItem[]>(() => {
  if (!info.value) return []
  return [
    {
      label: 'CPU',
      value: cpuDisplay.value,
      hint: `${info.value.cpu?.availableProcessors ?? '—'} 核心`,
      tone: 'primary',
    },
    {
      label: 'JVM 堆内存',
      value: heapDisplay.value,
      hint: `${info.value.memory?.heapUsed || '—'} / ${info.value.memory?.heapMax || '—'}`,
      tone: 'success',
    },
    {
      label: '物理内存',
      value: physicalDisplay.value,
      hint: `${info.value.memory?.physicalFree || '—'} 可用`,
      tone: 'warning',
    },
    {
      label: '磁盘峰值',
      value: maxDiskPercent.value ? `${maxDiskPercent.value.toFixed(1)}%` : '—',
      hint: `运行 ${info.value.jvm?.uptime || '—'}`,
      tone: 'danger',
    },
  ]
})

function formatDiskPercent(val?: number | null) {
  if (val == null) return '—'
  return `${val.toFixed(2)}%`
}

const { refreshing, toggleAuto, manualRefresh } = useMonitorToolbarRefresh(
  fetchServerInfo,
  autoRefresh,
  setAutoRefresh,
)

onMounted(async () => {
  if (!info.value) await manualRefresh()
})

onPullDownRefresh(async () => {
  await manualRefresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.server-page {
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

.server-tabs {
  width: 100%;
  margin-bottom: 20rpx;
}

.server-kpi {
  margin-bottom: 8rpx;
}

.server-kpi :deep(.stat-grid) {
  gap: 20rpx;
}

.server-kpi :deep(.stat-grid__item) {
  padding: 28rpx 24rpx;
}

.server-kpi :deep(.stat-grid__hint) {
  margin-top: 8rpx;
  line-height: 1.45;
}

.server-page :deep(.monitor-panel) {
  margin-bottom: 24rpx;
}

.server-page :deep(.monitor-panel__head) {
  margin-bottom: 24rpx;
}

.server-tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: $color-primary;
  background: rgba(99, 102, 241, 0.1);
}

.host-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 28rpx 32rpx;
}

.host-grid__item {
  min-width: 0;
  padding: 16rpx 20rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
}

.host-grid__label {
  display: block;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
  margin-bottom: 6rpx;
}

.host-grid__value {
  display: block;
  font-size: $font-size-sm;
  color: $color-text-primary;
  word-break: break-all;
  line-height: 1.45;
}

.server-section :deep(.monitor-panel__title) {
  position: relative;
  padding-left: 20rpx;
}

.server-section--cpu :deep(.monitor-panel__title)::before {
  background: #409eff;
}
.server-section--memory :deep(.monitor-panel__title)::before {
  background: #67c23a;
}
.server-section--jvm :deep(.monitor-panel__title)::before {
  background: #909399;
}
.server-section--sys :deep(.monitor-panel__title)::before {
  background: #e6a23c;
}

.server-section :deep(.monitor-panel__title)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 8rpx;
  height: 28rpx;
  border-radius: 4rpx;
}

.disk-card {
  padding: 28rpx 0;

  &:not(:last-child) {
    margin-bottom: 8rpx;
    border-bottom: 1px solid $color-border-light;
  }
}

.disk-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.disk-card__path {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.disk-card__pct {
  font-size: $font-size-sm;
  font-weight: $font-weight-bold;
}

.disk-card__pct--success {
  color: $color-success;
}

.disk-card__pct--warning {
  color: $color-warning;
}

.disk-card__pct--danger {
  color: $color-danger;
}

.disk-card__track {
  height: 20rpx;
  border-radius: 999rpx;
  background: #eef1f5;
  overflow: hidden;
}

.disk-card__fill {
  height: 100%;
  min-width: 0;
  border-radius: 999rpx;
  transition: width 0.35s ease;
}

.disk-card__fill--success {
  background: linear-gradient(90deg, #85ce61, #67c23a);
}

.disk-card__fill--warning {
  background: linear-gradient(90deg, #f3c76a, #e6a23c);
}

.disk-card__fill--danger {
  background: linear-gradient(90deg, #f78989, #f56c6c);
}

.disk-card__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 20rpx;
}

.disk-card__meta-item {
  padding: 16rpx 12rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  text-align: center;
}

.disk-card__meta-label {
  display: block;
  font-size: 22rpx;
  color: $color-text-placeholder;
  margin-bottom: 8rpx;
}

.disk-card__meta-value {
  display: block;
  font-size: $font-size-xs;
  color: $color-text-primary;
  font-weight: $font-weight-semibold;
  word-break: break-all;
  line-height: 1.4;
}
</style>
