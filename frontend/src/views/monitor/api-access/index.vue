<template>
  <div class="app-container module-page api-access-page">
    <el-alert
      v-if="!canQuery"
      type="warning"
      title="当前角色未分配「访问统计查询」权限，无法查看统计数据与日志列表。"
      :closable="false"
      show-icon
      class="no-query-alert"
    />

    <template v-if="canQuery">
      <el-card class="search-card module-hero-card" shadow="never">
        <div class="module-hero-row">
          <div class="module-hero-text">
            <div class="module-hero-title">
              <ModulePageIcon :icon="MODULE_PAGE_ICON.apiAccess" />
              <span>API 访问统计</span>
            </div>
            <p class="module-hero-desc">近 7 日接口访问概览，支持路径、方法与用户维度分析</p>
          </div>
        </div>
      </el-card>

      <el-row :gutter="16" class="stats-cards">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ stats.totalCount }}</div>
            <div class="stat-label">请求总数</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card success">
            <div class="stat-value">{{ stats.successCount }}</div>
            <div class="stat-label">成功</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card fail">
            <div class="stat-value">{{ stats.failCount }}</div>
            <div class="stat-label">失败</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="charts-row">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span>请求方法分布</span></template>
            <div ref="methodChartRef" class="chart-box"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span>Top10 API 路径</span></template>
            <div ref="pathChartRef" class="chart-box"></div>
          </el-card>
        </el-col>
        <el-col :span="24" class="chart-col-full">
          <el-card shadow="never">
            <template #header><span>每日请求趋势（近 7 天）</span></template>
            <div ref="lineChartRef" class="chart-box chart-box-tall"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="user-rank-card" shadow="never">
        <template #header>
          <span>用户访问排行（近 7 天）</span>
        </template>
        <el-table
          :data="stats.topUsers"
          border
          stripe
          empty-text="暂无用户访问记录"
          :header-cell-style="tableHeaderStyle"
          :cell-style="tableCellStyle"
        >
          <el-table-column label="排名" width="80" align="center" header-align="center">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" min-width="140" align="center" header-align="center" show-overflow-tooltip />
          <el-table-column prop="count" label="访问次数" width="120" align="center" header-align="center" sortable />
        </el-table>
      </el-card>

      <el-card class="search-card module-search-card" shadow="never">
        <el-form :model="queryParams" inline class="module-search-form">
          <el-form-item label="用户">
            <el-select
              v-model="queryParams.userId"
              placeholder="全部用户"
              clearable
              filterable
              style="width: 160px"
            >
              <el-option
                v-for="u in stats.topUsers"
                :key="u.userId"
                :label="displayUser(u)"
                :value="u.userId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="API 路径">
            <el-input v-model="queryParams.apiPath" placeholder="请输入路径" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="请求方法">
            <el-select v-model="queryParams.method" placeholder="请选择" clearable style="width: 120px">
              <el-option label="GET" value="GET" />
              <el-option label="POST" value="POST" />
              <el-option label="PUT" value="PUT" />
              <el-option label="DELETE" value="DELETE" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.success" placeholder="请选择" clearable style="width: 100px">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD HH:mm:ss"
              clearable
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="table-card">
        <template #header>
          <div class="card-header">
            <span>访问日志明细</span>
            <ListExportButton
              module="api-access"
              :query-params="queryParams"
              :extra-params="exportExtraParams"
              permission="monitor:apiAccess:query"
            />
          </div>
        </template>

        <el-table
          :data="tableData"
          v-loading="loading"
          border
          stripe
          :header-cell-style="tableHeaderStyle"
          :cell-style="tableCellStyle"
        >
          <el-table-column prop="id" label="ID" width="70" align="center" header-align="center" />
          <el-table-column prop="username" label="用户名" width="120" align="center" header-align="center" show-overflow-tooltip />
          <el-table-column prop="apiPath" label="API 路径" min-width="220" align="center" header-align="center" show-overflow-tooltip />
          <el-table-column prop="method" label="方法" width="80" align="center" header-align="center" />
          <el-table-column prop="statusCode" label="状态码" width="90" align="center" header-align="center" />
          <el-table-column prop="success" label="成功" width="80" align="center" header-align="center">
            <template #default="{ row }">
              <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
                {{ row.success === 1 ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="costTime" label="耗时(ms)" width="100" align="center" header-align="center" />
          <el-table-column prop="ip" label="IP" width="130" align="center" header-align="center" />
          <el-table-column prop="startTime" label="请求时间" width="180" align="center" header-align="center" />
        </el-table>

        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="table-pagination"
          @size-change="loadPage"
          @current-change="loadPage"
        />
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import ListExportButton from '@/components/ListExportButton.vue'
import type { ECharts } from 'echarts'
import type { CallbackDataParams } from 'echarts/types/dist/shared'
import { useUserStore } from '@/store/user'
import { hasMenuPermission } from '@/directives/permission'
import {
  getApiAccessPage,
  getApiAccessStatistics,
  createEmptyApiAccessStats,
  toApiAccessStatsView,
  type ApiAccessPageQuery,
  type ApiAccessStatisticsQuery,
  type ApiAccessLogRow,
  type ApiAccessStatsView,
  type ApiAccessTopUser,
} from '@/api/monitor/api-access'

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const userStore = useUserStore()
const canQuery = computed(() => hasMenuPermission(userStore.menus, 'monitor:apiAccess:query'))

const stats = reactive<ApiAccessStatsView>(createEmptyApiAccessStats())

const methodChartRef = ref<HTMLElement | null>(null)
const pathChartRef = ref<HTMLElement | null>(null)
const lineChartRef = ref<HTMLElement | null>(null)
let methodChart: ECharts | null = null
let pathChart: ECharts | null = null
let lineChart: ECharts | null = null
let echartsModule: typeof import('echarts') | null = null

const queryParams = reactive<ApiAccessPageQuery>({
  pageNo: 1,
  pageSize: 20,
  userId: null,
  apiPath: '',
  method: null,
  success: null,
})
const dateRange = ref<[string, string] | null>(null)
const exportExtraParams = computed(() => {
  if (!dateRange.value) return {}
  return {
    startTime: dateRange.value[0],
    endTime: dateRange.value[1],
  }
})
const tableData = ref<ApiAccessLogRow[]>([])
const loading = ref(false)
const total = ref(0)

/** 本地日期 YYYY-MM-DD（避免 toISOString 用 UTC 导致「今天」偏差一天） */
function formatLocalDate(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const startDate = computed(() => {
  const d = new Date()
  d.setDate(d.getDate() - 6)
  return formatLocalDate(d)
})
const endDate = computed(() => formatLocalDate(new Date()))

function parseLocalDate(str: string) {
  const [y, m, d] = str.split('-').map(Number)
  return new Date(y, m - 1, d)
}

/** 近 7 天完整日期轴（含无访问的 0） */
function buildDailyDateKeys() {
  const keys: string[] = []
  let cur = parseLocalDate(startDate.value)
  const end = parseLocalDate(endDate.value)
  while (cur <= end) {
    keys.push(formatLocalDate(cur))
    cur = new Date(cur.getFullYear(), cur.getMonth(), cur.getDate() + 1)
  }
  return keys
}

function displayUser(row: ApiAccessTopUser) {
  return row?.username || '-'
}

async function ensureEcharts() {
  if (!echartsModule) {
    echartsModule = await import('echarts')
  }
  return echartsModule
}

async function loadStatistics() {
  try {
    const statParams: ApiAccessStatisticsQuery = {
      startDate: startDate.value,
      endDate: endDate.value,
    }
    const res = await getApiAccessStatistics(statParams)
    Object.assign(stats, toApiAccessStatsView(res.data))
    await nextTick()
    updateCharts()
  } catch (e) {
    console.error(e)
  }
}

async function updateCharts() {
  const echarts = await ensureEcharts()

  if (methodChartRef.value && stats.methodCount) {
    if (!methodChart) methodChart = echarts.init(methodChartRef.value)
    const data = Object.entries(stats.methodCount).map(([name, value]) => ({ name, value }))
    methodChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: '60%', data }]
    })
  }

  if (pathChartRef.value && stats.topPaths?.length) {
    if (!pathChart) pathChart = echarts.init(pathChartRef.value)
    const paths = stats.topPaths
    const xData = paths.map((p) => {
      const path = p.apiPath || ''
      return path.length > 20 ? path.slice(0, 17) + '...' : path
    })
    const yData = paths.map((p) => p.count)
    pathChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter(params: CallbackDataParams | CallbackDataParams[]) {
          const list = Array.isArray(params) ? params : [params]
          const idx = list[0]?.dataIndex
          const full = paths[idx as number]?.apiPath ?? list[0]?.name
          return `${full}<br/>次数: ${list[0]?.value ?? 0}`
        }
      },
      grid: { left: 50, right: 24, bottom: 88, top: 24 },
      xAxis: {
        type: 'category',
        data: xData,
        axisLabel: { rotate: 35, interval: 0, fontSize: 11 }
      },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', data: yData }]
    })
  }

  if (lineChartRef.value) {
    if (!lineChart) lineChart = echarts.init(lineChartRef.value)
    const keys = buildDailyDateKeys()
    const totalData = keys.map((k) => stats.dailyStats[k]?.total ?? 0)
    const successData = keys.map((k) => stats.dailyStats[k]?.success ?? 0)
    const failData = keys.map((k) => stats.dailyStats[k]?.fail ?? 0)
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['总数', '成功', '失败'] },
      xAxis: { type: 'category', data: keys },
      yAxis: { type: 'value' },
      series: [
        { name: '总数', type: 'line', smooth: true, data: totalData },
        { name: '成功', type: 'line', smooth: true, data: successData },
        { name: '失败', type: 'line', smooth: true, data: failData }
      ]
    })
  }
}

async function loadPage() {
  loading.value = true
  try {
    const params: ApiAccessPageQuery = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      userId: queryParams.userId ?? undefined,
      apiPath: queryParams.apiPath || undefined,
      method: queryParams.method ?? undefined,
      success: queryParams.success ?? undefined,
    }
    if (dateRange.value?.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    const res = await getApiAccessPage(params)
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNo = 1
  loadPage()
}

function resetQuery() {
  queryParams.userId = null
  queryParams.apiPath = ''
  queryParams.method = null
  queryParams.success = null
  dateRange.value = null
  queryParams.pageNo = 1
  loadPage()
}

function handleResize() {
  methodChart?.resize()
  pathChart?.resize()
  lineChart?.resize()
}

let statsTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  if (!canQuery.value) return
  loadStatistics()
  loadPage()
  statsTimer = setInterval(loadStatistics, 10000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (statsTimer) clearInterval(statsTimer)
  window.removeEventListener('resize', handleResize)
  methodChart?.dispose()
  pathChart?.dispose()
  lineChart?.dispose()
})
</script>

<style scoped lang="scss">
.api-access-page {
  .no-query-alert {
    margin-bottom: 16px;
  }
  .stats-cards {
    margin-bottom: 16px;
  }
  .stat-card {
    text-align: center;
    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: var(--el-text-color-primary);
    }
    .stat-label {
      margin-top: 8px;
      font-size: 14px;
      color: var(--el-text-color-secondary);
    }
    &.success .stat-value {
      color: var(--el-color-success);
    }
    &.fail .stat-value {
      color: var(--el-color-danger);
    }
  }
  .charts-row {
    margin-bottom: 16px;
  }
  .chart-col-full {
    margin-top: 16px;
  }
  .chart-box {
    height: 260px;
    width: 100%;
  }
  .chart-box-tall {
    height: 300px;
  }
  .user-rank-card {
    margin-bottom: 16px;
  }
}
</style>
