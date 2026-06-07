<template>
  <div class="app-container cache-monitor-page">
    <el-alert
      v-if="!canList"
      type="warning"
      title="当前角色未分配「缓存监控」权限，无法查看 Redis 状态。"
      :closable="false"
      show-icon
      class="no-perm-alert"
    />

    <template v-if="canList">
      <el-card shadow="never" class="info-card">
        <template #header>
          <div class="card-header">
            <span>Redis 服务概览</span>
            <div class="header-actions">
              <el-switch
                v-model="autoRefresh"
                inline-prompt
                active-text="自动"
                inactive-text="暂停"
                @change="toggleAutoRefresh"
              />
              <el-button type="primary" size="small" :loading="refreshing" @click="refreshAll">
                刷新
              </el-button>
            </div>
          </div>
        </template>
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="版本">{{ info.redisVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="模式">{{ info.redisMode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运行天数">{{ info.uptimeInDays ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="键总数">{{ info.dbSize ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="内存占用">{{ info.usedMemoryHuman || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内存峰值">{{ info.usedMemoryPeakHuman || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前连接">{{ info.connectedClients ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前 QPS">{{ liveQps ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="累计命中率">{{ liveHitRateText }}</el-descriptions-item>
          <el-descriptions-item label="累计命令">{{ info.totalCommandsProcessed ?? '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-row :gutter="16" class="charts-row">
        <el-col :xs="24" :sm="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span>内存使用</span></template>
            <div class="chart-body">
              <div ref="memoryChartRef" class="chart-box" />
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span>QPS</span></template>
            <div class="chart-body">
              <div ref="qpsChartRef" class="chart-box" />
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span>命中率趋势</span></template>
            <div class="chart-body">
              <div ref="hitRateChartRef" class="chart-box" />
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span>连接数趋势</span></template>
            <div class="chart-body">
              <div ref="clientsChartRef" class="chart-box" />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="keys-card">
        <template #header>
          <div class="card-header keys-card-header">
            <span>缓存键列表</span>
            <el-text v-show="keysTruncated" type="warning" size="small" class="keys-truncated-tip">
              已截断至扫描上限，请缩小键名模式
            </el-text>
          </div>
        </template>

        <div class="preset-tags">
          <el-check-tag
            v-for="preset in patternPresets"
            :key="preset.pattern"
            :checked="searchPattern === preset.pattern"
            @click="applyPreset(preset.pattern)"
          >
            {{ preset.label }}
          </el-check-tag>
        </div>

        <el-form inline class="search-form">
          <el-form-item label="键名模式">
            <el-input
              v-model="searchPattern"
              placeholder="如 cache:sys:* 或 *"
              clearable
              style="width: 280px"
              @keyup.enter="loadKeys"
            />
          </el-form-item>
          <el-form-item label="扫描上限">
            <el-input-number v-model="scanLimit" :min="50" :max="500" :step="50" />
          </el-form-item>
          <el-form-item label="本地筛选">
            <el-input
              v-model="keyFilter"
              placeholder="在当前结果中搜索"
              clearable
              style="width: 220px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="keysLoading" @click="loadKeys">搜索</el-button>
          </el-form-item>
        </el-form>

        <div class="keys-table-wrap" v-loading="keysLoading" element-loading-background="rgba(255, 255, 255, 0.65)">
        <el-table
          :data="pagedKeys"
          :height="keysTableHeight"
          border
          stripe
          empty-text="暂无匹配的键"
          :header-cell-style="{ textAlign: 'center' }"
          :cell-style="{ textAlign: 'center' }"
        >
          <el-table-column prop="key" label="键名" min-width="300" align="left" header-align="center">
            <template #default="{ row }">
              <span class="key-cell">{{ row.key }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag type="info" size="small">{{ row.type || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ttl" label="有效期" width="120">
            <template #default="{ row }">
              {{ formatTTL(row.ttl) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" align="center">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button type="primary" size="small" @click="handleView(row.key)">查看</el-button>
                <el-button size="small" @click="copyKey(row.key)">复制</el-button>
                <el-button
                  v-if="canDelete"
                  type="danger"
                  size="small"
                  @click="handleDelete(row.key)"
                >
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        </div>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="filteredKeys.length"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            background
          />
        </div>
      </el-card>

      <el-dialog v-model="detailVisible" title="缓存详情" width="720px" destroy-on-close>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="键名">{{ cacheDetail.key }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag type="info" size="small">{{ cacheDetail.type }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="有效期">{{ formatTTL(cacheDetail.ttl) }}</el-descriptions-item>
          <el-descriptions-item label="值">
            <el-scrollbar max-height="360px">
              <pre class="value-pre">{{ formatValue(cacheDetail.value) }}</pre>
            </el-scrollbar>
          </el-descriptions-item>
        </el-descriptions>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useCacheMonitorPage } from '../composables/useCacheMonitorPage'

const {
  canList,
  canDelete,
  info,
  keysLoading,
  keysTruncated,
  searchPattern,
  scanLimit,
  keyFilter,
  refreshing,
  autoRefresh,
  liveQps,
  liveHitRateText,
  patternPresets,
  detailVisible,
  cacheDetail,
  memoryChartRef,
  qpsChartRef,
  hitRateChartRef,
  clientsChartRef,
  pagedKeys,
  pagination,
  filteredKeys,
  keysTableHeight,
  refreshAll,
  applyPreset,
  copyKey,
  loadKeys,
  handleView,
  handleDelete,
  formatTTL,
  formatValue,
  toggleAutoRefresh,
} = useCacheMonitorPage()
</script>

<style scoped lang="scss">
.cache-monitor-page {
  .no-perm-alert {
    margin-bottom: 16px;
  }

  .info-card {
    margin-bottom: 16px;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    font-weight: 600;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .charts-row {
    margin-bottom: 16px;

    .el-col {
      margin-bottom: 16px;
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

  .keys-card {
    .keys-card-header {
      min-height: 24px;
    }

    .keys-truncated-tip {
      flex-shrink: 0;
    }

    .preset-tags {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      min-height: 32px;
      margin-bottom: 12px;

      :deep(.el-check-tag) {
        display: inline-flex;
        align-items: center;
        min-height: 28px;
        padding: 0 12px;
        box-sizing: border-box;
      }
    }

    .search-form {
      margin-bottom: 12px;
    }

    .keys-table-wrap {
      position: relative;
      min-height: 480px;
    }
  }

  .key-cell {
    display: inline-block;
    max-width: 100%;
    text-align: left;
    word-break: break-all;
  }

  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .value-pre {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-all;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 12px;
    line-height: 1.5;
  }

  .action-buttons {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: 8px;

    .el-button {
      margin: 0;
    }
  }
}
</style>
