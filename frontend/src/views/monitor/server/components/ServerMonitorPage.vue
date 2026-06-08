<template>
  <div class="app-container server-monitor-page">
    <el-alert
      v-if="!canList"
      type="warning"
      title="当前角色未分配「服务监控」权限，无法查看本机状态。"
      :closable="false"
      show-icon
      class="no-perm-alert"
    />

    <template v-if="canList">
      <div class="hero-row">
        <el-card shadow="never" class="stat-card stat-cpu">
          <div class="stat-label">CPU</div>
          <div class="stat-value">{{ cpuDisplay }}</div>
          <div class="stat-sub">{{ info.cpu?.availableProcessors ?? '-' }} 核心</div>
        </el-card>
        <el-card shadow="never" class="stat-card stat-heap">
          <div class="stat-label">JVM 堆内存</div>
          <div class="stat-value">{{ heapDisplay }}</div>
          <div class="stat-sub">{{ info.memory?.heapUsed || '-' }} / {{ info.memory?.heapMax || '-' }}</div>
        </el-card>
        <el-card shadow="never" class="stat-card stat-physical">
          <div class="stat-label">物理内存</div>
          <div class="stat-value">{{ physicalDisplay }}</div>
          <div class="stat-sub">{{ info.memory?.physicalFree || '-' }} 可用</div>
        </el-card>
        <el-card shadow="never" class="stat-card stat-disk">
          <div class="stat-label">磁盘峰值</div>
          <div class="stat-value">{{ maxDiskPercent.toFixed(1) }}%</div>
          <div class="stat-sub">运行 {{ info.jvm?.uptime || '-' }}</div>
        </el-card>
      </div>

      <el-card shadow="never" class="toolbar-card">
        <div class="card-header">
          <div class="header-left">
            <span class="title">本机服务状态</span>
            <el-tag size="small" type="info" effect="plain">JMX 实时采集</el-tag>
          </div>
          <div class="header-actions">
            <el-switch
              v-model="autoRefresh"
              inline-prompt
              active-text="自动"
              inactive-text="暂停"
              @change="toggleAutoRefresh"
            />
            <el-button
              type="primary"
              size="small"
              class="monitor-refresh-btn"
              :class="{ 'is-refreshing': refreshing }"
              @click="refreshByUser"
            >
              <span class="monitor-refresh-label-wrap">
                <el-icon v-if="refreshing" class="is-loading monitor-refresh-spinner"><Loading /></el-icon>
                刷新
              </span>
            </el-button>
          </div>
        </div>
        <el-descriptions :column="4" border size="small" class="host-desc">
          <el-descriptions-item label="主机名">{{ info.sys?.hostName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ info.sys?.hostAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作系统">{{ info.sys?.osName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="系统版本">{{ info.sys?.osVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Java">{{ info.sys?.javaVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="JVM 启动">{{ info.jvm?.startTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="工作目录" :span="2">{{ info.sys?.userDir || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-row :gutter="16" class="detail-row">
        <el-col :xs="24" :sm="12">
          <el-card shadow="never" class="detail-card">
            <template #header>
              <div class="detail-header">
                <el-icon class="detail-icon cpu"><Cpu /></el-icon>
                <span>CPU 信息</span>
              </div>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="操作系统">{{ info.cpu?.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="架构">{{ info.cpu?.arch || '-' }}</el-descriptions-item>
              <el-descriptions-item label="核心数">{{ info.cpu?.availableProcessors ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="系统 CPU">{{ formatPercent(info.cpu?.systemCpuPercent) }}</el-descriptions-item>
              <el-descriptions-item label="进程 CPU">{{ formatPercent(info.cpu?.processCpuPercent) }}</el-descriptions-item>
              <el-descriptions-item label="平均负载">
                {{ formatLoad(info.cpu?.systemLoadAverage) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="never" class="detail-card">
            <template #header>
              <div class="detail-header">
                <el-icon class="detail-icon memory"><Coin /></el-icon>
                <span>内存信息</span>
              </div>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="堆已用">{{ info.memory?.heapUsed || '-' }}</el-descriptions-item>
              <el-descriptions-item label="堆最大">{{ info.memory?.heapMax || '-' }}</el-descriptions-item>
              <el-descriptions-item label="堆提交">{{ info.memory?.heapCommitted || '-' }}</el-descriptions-item>
              <el-descriptions-item label="非堆已用">{{ info.memory?.nonHeapUsed || '-' }}</el-descriptions-item>
              <el-descriptions-item label="物理总量">{{ info.memory?.physicalTotal || '-' }}</el-descriptions-item>
              <el-descriptions-item label="物理可用">{{ info.memory?.physicalFree || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="never" class="detail-card">
            <template #header>
              <div class="detail-header">
                <el-icon class="detail-icon jvm"><Monitor /></el-icon>
                <span>JVM 信息</span>
              </div>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="名称">{{ info.jvm?.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="厂商">{{ info.jvm?.vendor || '-' }}</el-descriptions-item>
              <el-descriptions-item label="版本">{{ info.jvm?.version || '-' }}</el-descriptions-item>
              <el-descriptions-item label="规范版本">{{ info.jvm?.specVersion || '-' }}</el-descriptions-item>
              <el-descriptions-item label="启动时间">{{ info.jvm?.startTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="运行时长">{{ info.jvm?.uptime || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="never" class="detail-card">
            <template #header>
              <div class="detail-header">
                <el-icon class="detail-icon server"><Platform /></el-icon>
                <span>服务器信息</span>
              </div>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="主机名">{{ info.sys?.hostName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="IP 地址">{{ info.sys?.hostAddress || '-' }}</el-descriptions-item>
              <el-descriptions-item label="操作系统">{{ info.sys?.osName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="系统版本">{{ info.sys?.osVersion || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Java 版本">{{ info.sys?.javaVersion || '-' }}</el-descriptions-item>
              <el-descriptions-item label="工作目录">{{ info.sys?.userDir || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="charts-row">
        <el-col :xs="24" :sm="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span>CPU 使用率趋势</span></template>
            <div class="chart-body">
              <div ref="cpuChartRef" class="chart-box" />
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span>JVM 堆内存趋势</span></template>
            <div class="chart-body">
              <div ref="memoryChartRef" class="chart-box" />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="disk-card">
        <template #header><span>磁盘信息</span></template>
        <el-table
          :data="info.disks || []"
          v-loading="tableLoading"
          border
          stripe
          empty-text="暂无磁盘数据"
          :header-cell-style="{ textAlign: 'center' }"
          :cell-style="{ textAlign: 'center' }"
        >
          <el-table-column prop="path" label="盘符" width="100" />
          <el-table-column prop="total" label="总容量" min-width="120" />
          <el-table-column prop="used" label="已用" min-width="120" />
          <el-table-column prop="free" label="可用" min-width="120" />
          <el-table-column label="使用率" min-width="220">
            <template #default="{ row }">
              <div class="disk-progress">
                <el-progress
                  :percentage="roundPercent(row.usedPercent)"
                  :format="(p) => `${p.toFixed(2)}%`"
                  :status="diskProgressStatus(row.usedPercent ?? 0)"
                  :stroke-width="14"
                  striped
                  striped-flow
                />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { Coin, Cpu, Loading, Monitor, Platform } from '@element-plus/icons-vue'
import { useServerMonitorPage } from '../composables/useServerMonitorPage'

const {
  canList,
  info,
  refreshing,
  tableLoading,
  autoRefresh,
  cpuDisplay,
  heapDisplay,
  physicalDisplay,
  maxDiskPercent,
  cpuChartRef,
  memoryChartRef,
  refreshByUser,
  toggleAutoRefresh,
  diskProgressStatus,
} = useServerMonitorPage()

function formatPercent(val?: number | null) {
  return val != null ? `${val.toFixed(2)}%` : '-'
}

function roundPercent(val?: number | null) {
  if (val == null) return 0
  return Math.round(val * 100) / 100
}

function formatLoad(val?: number | null) {
  if (val == null || val < 0) return '-'
  return val.toFixed(2)
}
</script>

<style scoped lang="scss">
.server-monitor-page {
  .no-perm-alert {
    margin-bottom: 16px;
  }

  .hero-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 16px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, 1fr);
    }

    @media (max-width: 640px) {
      grid-template-columns: 1fr;
    }
  }

  .stat-card {
    border-radius: 8px;
    border-left: 4px solid transparent;

    :deep(.el-card__body) {
      padding: 18px 20px;
    }

    .stat-label {
      font-size: 13px;
      color: var(--el-text-color-secondary);
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      line-height: 1.2;
      color: var(--el-text-color-primary);
    }

    .stat-sub {
      margin-top: 8px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    &.stat-cpu {
      border-left-color: #409eff;
      .stat-value { color: #409eff; }
    }

    &.stat-heap {
      border-left-color: #67c23a;
      .stat-value { color: #67c23a; }
    }

    &.stat-physical {
      border-left-color: #e6a23c;
      .stat-value { color: #e6a23c; }
    }

    &.stat-disk {
      border-left-color: #f56c6c;
      .stat-value { color: #f56c6c; }
    }
  }

  .toolbar-card {
    margin-bottom: 16px;

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 16px;
      flex-wrap: wrap;
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .title {
        font-size: 16px;
        font-weight: 600;
      }
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-shrink: 0;

      :deep(.el-switch) {
        min-width: 56px;
      }

      .monitor-refresh-btn {
        min-width: 80px;

        :deep(.monitor-refresh-label-wrap) {
          position: relative;
          display: inline-block;
          line-height: 1;
        }

        :deep(.monitor-refresh-spinner) {
          position: absolute;
          right: calc(100% + 3px);
          top: 0;
          bottom: 0;
          height: 14px;
          margin: auto 0;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          font-size: 14px;
        }
      }
    }
  }

  .detail-row,
  .charts-row {
    margin-bottom: 16px;

    .el-col {
      margin-bottom: 16px;
    }
  }

  .detail-card {
    height: 100%;

    :deep(.el-card__header) {
      padding: 14px 20px;
      font-weight: 600;
    }
  }

  .detail-header {
    display: flex;
    align-items: center;
    gap: 8px;

    .detail-icon {
      font-size: 18px;

      &.cpu { color: #409eff; }
      &.memory { color: #67c23a; }
      &.jvm { color: #909399; }
      &.server { color: #e6a23c; }
    }
  }

  .chart-card {
    :deep(.el-card__header) {
      padding: 14px 20px;
      font-weight: 600;
    }

    :deep(.el-card__body) {
      padding: 0;
    }
  }

  .chart-body {
    padding: 12px 16px 16px;
    box-sizing: border-box;
  }

  .chart-box {
    height: 268px;
    width: 100%;
  }

  .disk-card {
    :deep(.el-card__header) {
      padding: 14px 20px;
      font-weight: 600;
    }
  }

  .disk-progress {
    padding: 0 8px;
  }
}
</style>
