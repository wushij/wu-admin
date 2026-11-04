<template>
  <div class="app-container module-page security-log-page">
    <!-- 顶部概览 Hero 卡片 -->
    <SecurityStatsHero :stats="stats" />

    <!-- 标签页导航 -->
    <el-card class="content-card" shadow="never">
      <el-tabs v-model="activeTab" class="security-tabs" @tab-change="handleTabChange">
        <!-- 标签页 1：安全拦截审计事件 -->
        <el-tab-pane label="安全拦截事件" name="events">
          <!-- 一体化检索与操作工具栏 -->
          <div class="table-filter-bar">
            <el-form :model="queryParams" inline class="inline-filter-form">
              <el-form-item label="客户端 IP">
                <el-input v-model="queryParams.ipaddr" placeholder="请输入 IP 地址" clearable style="width: 140px" />
              </el-form-item>
              <el-form-item label="事件类型">
                <el-select v-model="queryParams.eventType" placeholder="全部类型" clearable style="width: 140px">
                  <el-option label="自动化爬虫 UA" value="UA_BLOCK" />
                  <el-option label="重放攻击 (Nonce)" value="ANTI_REPLAY" />
                  <el-option label="数字签名篡改" value="SIGN_FAIL" />
                  <el-option label="时间戳过期" value="TIMESTAMP_INVALID" />
                  <el-option label="超大请求体" value="BODY_TOO_LARGE" />
                  <el-option label="风控自动封禁" value="RISK_BLOCK" />
                  <el-option label="蜜罐诱捕触发" value="HONEYPOT" />
                  <el-option label="登录失败" value="LOGIN_FAIL" />
                  <el-option label="滑块验证失败" value="CAPTCHA_FAIL" />
                  <el-option label="越权探测" value="PERMISSION_DENIED" />
                  <el-option label="翻页遍历" value="PAGE_SCAN" />
                  <el-option label="批量导出" value="BULK_EXPORT" />
                  <el-option label="风控挑战" value="RISK_CHALLENGE" />
                </el-select>
              </el-form-item>
              <el-form-item label="处置动作">
                <el-select v-model="queryParams.action" placeholder="全部动作" clearable style="width: 120px">
                  <el-option label="直接阻断 (403)" value="BLOCKED" />
                  <el-option label="触发封禁" value="FORBIDDEN" />
                  <el-option label="安全告警" value="WARNING" />
                  <el-option label="要求验证" value="CHALLENGE" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
                <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
                <el-button
                  type="danger"
                  plain
                  :icon="Delete"
                  v-permission="'system:security:clear'"
                  @click="handleClearEvents"
                >
                  清空事件
                </el-button>
                <el-button :icon="RefreshRight" @click="loadEventData">刷新</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 事件数据表格 -->
          <el-table
            :data="eventList"
            v-loading="eventLoading"
            border
            stripe
            class="security-table"
          >
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="ipaddr" label="客户端 IP" width="140" align="center">
              <template #default="{ row }">
                <el-link type="primary" :underline="false" @click="filterByIp(row.ipaddr)">
                  {{ row.ipaddr }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column prop="eventType" label="事件类型" width="150" align="center">
              <template #default="{ row }">
                <el-tag :type="getEventTypeTagType(row.eventType)" effect="light">
                  {{ formatEventTypeName(row.eventType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="触发规则与原因" min-width="200" show-overflow-tooltip />
            <el-table-column label="请求端点" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="method-badge">{{ row.requestMethod }}</span>
                <span class="uri-text">{{ row.requestUri }}</span>
              </template>
            </el-table-column>
            <el-table-column label="风险分值" width="130" align="center">
              <template #default="{ row }">
                <span class="risk-delta">+{{ row.riskDelta }}</span>
                <span class="risk-total">({{ row.totalRiskScore }}分)</span>
              </template>
            </el-table-column>
            <el-table-column prop="action" label="处置结果" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getActionTagType(row.action)" size="small">
                  {{ formatAction(row.action) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="拦截时间" width="170" align="center" />
            <el-table-column label="操作" width="140" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
                  <el-button
                    type="danger"
                    link
                    size="small"
                    v-permission="'system:security:block'"
                    @click="quickBlockIp(row.ipaddr)"
                  >
                    拉黑
                  </el-button>
                  <el-button
                    type="danger"
                    link
                    size="small"
                    v-permission="'system:security:delete'"
                    @click="handleDeleteEvent(row)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="queryParams.pageNo"
            v-model:page-size="queryParams.pageSize"
            :total="eventTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            class="table-pagination"
            @size-change="loadEventData"
            @current-change="loadEventData"
          />
        </el-tab-pane>

        <!-- 标签页 2：风险主体管控 -->
        <el-tab-pane label="风险主体" name="risk">
          <!-- 一体化检索与操作工具栏 -->
          <div class="table-filter-bar">
            <el-form :model="riskQuery" inline class="inline-filter-form">
              <el-form-item label="主体">
                <el-input
                  v-model="riskQuery.keyword"
                  placeholder="用户名 / 昵称 / ID / IP"
                  clearable
                  style="width: 170px"
                />
              </el-form-item>
              <el-form-item label="风险等级">
                <el-select v-model="riskQuery.level" placeholder="全部等级" clearable style="width: 150px">
                  <el-option label="关注 (1)" :value="1" />
                  <el-option label="警告 (2)" :value="2" />
                  <el-option label="挑战 (3)" :value="3" />
                  <el-option label="临时封禁 (4)" :value="4" />
                  <el-option label="最高档封禁 (5)" :value="5" />
                </el-select>
              </el-form-item>
              <el-form-item label="封禁状态">
                <el-select v-model="riskQuery.blocked" placeholder="全部" clearable style="width: 110px">
                  <el-option label="已封禁" :value="true" />
                  <el-option label="未封禁" :value="false" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleRiskQuery">搜索</el-button>
                <el-button :icon="Refresh" @click="resetRiskQuery">重置</el-button>
                <el-button
                  type="primary"
                  :icon="Plus"
                  v-permission="'system:security:block'"
                  @click="openManualBlockDialog"
                >
                  手动封禁
                </el-button>
                <el-button :icon="RefreshRight" @click="loadRiskData">刷新名单</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table :data="riskList" v-loading="riskLoading" border stripe class="security-table">
            <el-table-column label="风险主体" min-width="220">
              <template #default="{ row }">
                <div class="principal-cell">
                  <div class="principal-title">
                    <el-tag
                      :type="row.principalType === 'user' ? 'primary' : 'info'"
                      size="small"
                      class="principal-type-tag"
                    >
                      {{ row.principalType === 'user' ? '账号' : 'IP' }}
                    </el-tag>
                    <template v-if="row.principalType === 'user'">
                      <span v-if="row.username" class="font-bold">
                        {{ row.nickname ? `${row.nickname} (${row.username})` : row.username }}
                      </span>
                      <span v-else class="font-mono font-bold">{{ row.principal }}</span>
                      <span class="principal-id-badge">ID: {{ row.principalId }}</span>
                    </template>
                    <template v-else>
                      <span class="font-mono font-bold">{{ row.principalId || row.principal }}</span>
                    </template>
                  </div>
                  <div v-if="row.ipaddr && row.principalType === 'user'" class="principal-ip">
                    来源 IP：{{ row.ipaddr }}
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="风险分与等级" width="230">
              <template #default="{ row }">
                <div class="risk-progress-wrap">
                  <el-progress
                    :percentage="Math.min(100, Math.round((row.riskScore / 120) * 100))"
                    :status="row.riskLevel >= 4 ? 'exception' : row.riskLevel >= 2 ? 'warning' : ''"
                    :stroke-width="8"
                  />
                  <span class="risk-score-text">
                    {{ row.riskScore }} 分 · {{ row.riskLevelText }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="eventCount" label="窗口内事件" width="110" align="center">
              <template #default="{ row }">{{ row.eventCount }} 次</template>
            </el-table-column>
            <el-table-column prop="lastEventTime" label="最近触发" width="170" align="center">
              <template #default="{ row }">{{ row.lastEventTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="封禁状态" width="180" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.blocked" type="danger" effect="plain" class="countdown-tag">
                  剩余 {{ formatRemainingSeconds(row.remainingSeconds) }}
                </el-tag>
                <el-tag v-else type="success" effect="plain">正常</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button type="primary" link size="small" @click="showSubjectEvents(row)">
                    事件
                  </el-button>
                  <el-button
                    v-if="row.blocked"
                    type="success"
                    link
                    size="small"
                    v-permission="'system:security:unblock'"
                    @click="handleUnblock(row.principal, row)"
                  >
                    解封
                  </el-button>
                  <el-button
                    v-else
                    type="danger"
                    link
                    size="small"
                    v-permission="'system:security:block'"
                    @click="quickBlockIp(row.principal)"
                  >
                    拉黑
                  </el-button>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="当前没有处于风险窗口中的主体，系统运行平稳" />
            </template>
          </el-table>

          <el-pagination
            v-model:current-page="riskQuery.pageNo"
            v-model:page-size="riskQuery.pageSize"
            :total="riskTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            class="table-pagination"
            @size-change="loadRiskData"
            @current-change="loadRiskData"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 事件详情抽屉 Drawer -->
    <el-drawer v-model="detailVisible" title="安全拦截事件详情" size="520px">
      <div v-if="activeEvent" class="detail-container">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="事件 ID">{{ activeEvent.id }}</el-descriptions-item>
          <el-descriptions-item label="事件类型">
            <el-tag :type="getEventTypeTagType(activeEvent.eventType)">
              {{ formatEventTypeName(activeEvent.eventType) }} ({{ activeEvent.eventType }})
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="客户端 IP">{{ activeEvent.ipaddr }}</el-descriptions-item>
          <el-descriptions-item label="请求端点">
            {{ activeEvent.requestMethod }} {{ activeEvent.requestUri }}
          </el-descriptions-item>
          <el-descriptions-item label="风险分值">
            本次增量 +{{ activeEvent.riskDelta }}，当前总计 {{ activeEvent.totalRiskScore }} 分
          </el-descriptions-item>
          <el-descriptions-item label="处置动作">
            {{ activeEvent.action === 'FORBIDDEN' ? '自动临时封禁' : '直接 403 阻断' }}
          </el-descriptions-item>
          <el-descriptions-item label="拦截时间">{{ activeEvent.createTime }}</el-descriptions-item>
          <el-descriptions-item label="详细触发原因">
            <span class="text-danger font-bold">{{ activeEvent.reason }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="完整 User-Agent">
            <div class="ua-block">{{ activeEvent.userAgent || '无 User-Agent 头' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <!-- 风险主体事件抽屉 -->
    <el-drawer
      v-model="subjectEventsVisible"
      :title="`风险主体事件 - ${formatPrincipalDisplayName(activeSubject)}`"
      size="600px"
    >
      <div v-loading="subjectEventsLoading">
        <el-descriptions v-if="activeSubject" :column="2" border size="small" class="subject-summary">
          <el-descriptions-item label="风险主体" :span="2">
            {{ formatPrincipalDisplayName(activeSubject) }}
          </el-descriptions-item>
          <el-descriptions-item label="风险分">{{ activeSubject.riskScore }}</el-descriptions-item>
          <el-descriptions-item label="等级">{{ activeSubject.riskLevelText }}</el-descriptions-item>
          <el-descriptions-item label="窗口内事件">{{ activeSubject.eventCount }} 次</el-descriptions-item>
          <el-descriptions-item label="封禁状态">
            {{ activeSubject.blocked ? `剩余 ${formatRemainingSeconds(activeSubject.remainingSeconds)}` : '正常' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-table :data="subjectEvents" border stripe size="small">
          <el-table-column prop="eventType" label="类型" width="150">
            <template #default="{ row }">
              <el-tag :type="getEventTypeTagType(row.eventType)" size="small">
                {{ formatEventTypeName(row.eventType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="riskDelta" label="加分" width="70" align="center">
            <template #default="{ row }">+{{ row.riskDelta }}</template>
          </el-table-column>
          <el-table-column prop="totalRiskScore" label="累计" width="70" align="center" />
          <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" width="170" />
          <template #empty>
            <el-empty description="该主体最近没有安全事件" />
          </template>
        </el-table>
      </div>
    </el-drawer>

    <!-- 手动封禁对话框 Dialog -->
    <el-dialog v-model="blockDialogVisible" title="手动封禁风险主体 IP" width="460px">
      <el-form :model="blockForm" label-width="100px">
        <el-form-item label="目标 IP" required>
          <el-input v-model="blockForm.ip" placeholder="例如：192.168.1.100" />
        </el-form-item>
        <el-form-item label="封禁时长">
          <el-select v-model="blockForm.duration" style="width: 100%">
            <el-option label="1 小时" :value="3600" />
            <el-option label="6 小时" :value="21600" />
            <el-option label="24 小时 (1天)" :value="86400" />
            <el-option label="7 天" :value="604800" />
            <el-option label="30 天" :value="2592000" />
          </el-select>
        </el-form-item>
        <el-form-item label="封禁原因">
          <el-input v-model="blockForm.reason" placeholder="例如：恶意扫描 / 批量刷接口" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="blockDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmManualBlock">确认封禁</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  RefreshRight,
  Delete,
  Plus,
} from '@element-plus/icons-vue'
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
  type SecurityEventPageQuery,
  type RiskSubjectVO,
  type RiskSubjectPageQuery,
  type SecurityStatsVO,
} from '@/api/system/security'
import SecurityStatsHero from './components/SecurityStatsHero.vue'
import {
  formatEventTypeName,
  getEventTypeTagType,
  formatAction,
  getActionTagType,
  formatRemainingSeconds,
  formatPrincipalDisplayName,
} from './utils/securityLogFormatters'

const activeTab = ref('events')

// 大盘指标数据
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

// 拦截事件查询参数
const queryParams = reactive<SecurityEventPageQuery>({
  pageNo: 1,
  pageSize: 10,
  ipaddr: '',
  eventType: '',
  action: '',
})

const eventLoading = ref(false)
const eventList = ref<SecurityEventVO[]>([])
const eventTotal = ref(0)

// 风险主体
const riskLoading = ref(false)
const riskList = ref<RiskSubjectVO[]>([])
const riskTotal = ref(0)
const riskQuery = reactive<RiskSubjectPageQuery>({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  level: undefined,
  blocked: undefined,
})

// 主体事件抽屉
const subjectEventsVisible = ref(false)
const subjectEventsLoading = ref(false)
const subjectEvents = ref<SecurityEventVO[]>([])
const activeSubject = ref<RiskSubjectVO | null>(null)

// 详情抽屉
const detailVisible = ref(false)
const activeEvent = ref<SecurityEventVO | null>(null)

// 手动封禁弹窗
const blockDialogVisible = ref(false)
const blockForm = reactive({
  ip: '',
  duration: 3600,
  reason: '管理员手动封禁',
})

// 加载大盘统计
async function loadStats() {
  try {
    const res = await getSecurityStats()
    if (res.data) {
      Object.assign(stats, res.data)
    }
  } catch {}
}

// 加载事件列表
async function loadEventData() {
  eventLoading.value = true
  try {
    const res = await getSecurityEvents(queryParams)
    if (res.data) {
      eventList.value = res.data.list || []
      eventTotal.value = res.data.total || 0
    }
  } catch (err) {
    console.error('加载安全事件失败', err)
  } finally {
    eventLoading.value = false
  }
}

// 加载风险主体
async function loadRiskData() {
  riskLoading.value = true
  try {
    const res = await getRiskSubjects(riskQuery)
    if (res.data) {
      riskList.value = res.data.list || []
      riskTotal.value = res.data.total || 0
    }
  } catch (err) {
    console.error('加载风险主体失败', err)
  } finally {
    riskLoading.value = false
  }
}

function handleRiskQuery() {
  riskQuery.pageNo = 1
  loadRiskData()
}

function resetRiskQuery() {
  riskQuery.keyword = ''
  riskQuery.level = undefined
  riskQuery.blocked = undefined
  riskQuery.pageNo = 1
  loadRiskData()
}

// 查看主体最近事件
async function showSubjectEvents(row: RiskSubjectVO) {
  activeSubject.value = row
  subjectEvents.value = []
  subjectEventsVisible.value = true
  subjectEventsLoading.value = true
  try {
    const res = await getRiskSubjectEvents(row.principal, 50)
    if (res.data) {
      subjectEvents.value = res.data || []
    }
  } catch (err) {
    console.error('加载主体事件失败', err)
  } finally {
    subjectEventsLoading.value = false
  }
}

function handleTabChange(tab: string | number) {
  if (tab === 'events') {
    loadEventData()
  } else if (tab === 'risk') {
    loadRiskData()
  }
}

function handleQuery() {
  queryParams.pageNo = 1
  loadEventData()
}

function resetQuery() {
  queryParams.ipaddr = ''
  queryParams.eventType = ''
  queryParams.action = ''
  queryParams.pageNo = 1
  loadEventData()
}

function filterByIp(ip: string) {
  queryParams.ipaddr = ip
  handleQuery()
}

function showDetail(row: SecurityEventVO) {
  activeEvent.value = row
  detailVisible.value = true
}

// 一键解封
async function handleUnblock(principal: string, row?: RiskSubjectVO) {
  const displayName = row ? formatPrincipalDisplayName(row) : principal
  try {
    await ElMessageBox.confirm(
      `确认解除对风险主体【${displayName}】的封禁并清零风险分吗？解除后该主体可正常访问。`,
      '确认解封',
      {
        type: 'warning',
        confirmButtonText: '立即解封',
        cancelButtonText: '取消',
      },
    )
    await unblockRiskSubject(principal, true)
    ElMessage.success(`【${displayName}】已成功解封并清零风险分`)
    loadRiskData()
    loadStats()
  } catch {}
}

// 快速拉黑 IP
function quickBlockIp(ip: string) {
  blockForm.ip = ip
  blockForm.duration = 3600
  blockForm.reason = '管理员从事件明细中手动拉黑'
  blockDialogVisible.value = true
}

function openManualBlockDialog() {
  blockForm.ip = ''
  blockForm.duration = 3600
  blockForm.reason = '管理员手动封禁'
  blockDialogVisible.value = true
}

async function confirmManualBlock() {
  if (!blockForm.ip.trim()) {
    ElMessage.warning('请输入待封禁的 IP 地址')
    return
  }
  try {
    await blockRiskSubject(blockForm.ip.trim(), blockForm.duration, blockForm.reason)
    ElMessage.success(`已成功将 IP【${blockForm.ip}】加入封禁黑名单！`)
    blockDialogVisible.value = false
    loadRiskData()
    loadStats()
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : '封禁操作失败'
    ElMessage.error(message)
  }
}

// 删除单条事件
async function handleDeleteEvent(row: SecurityEventVO) {
  try {
    await ElMessageBox.confirm(`确认删除此条安全拦截事件记录吗？`, '提示', {
      type: 'warning',
    })
    await deleteSecurityEvent(row.id)
    ElMessage.success('删除成功')
    loadEventData()
    loadStats()
  } catch {}
}

// 清空所有事件
async function handleClearEvents() {
  try {
    await ElMessageBox.confirm('确认清空所有的安全拦截事件审计日志吗？清空后不可恢复！', '高危警告', {
      type: 'error',
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
    })
    await clearSecurityEvents()
    ElMessage.success('安全事件日志已清空')
    loadEventData()
    loadStats()
  } catch {}
}

onMounted(() => {
  loadStats()
  loadEventData()
})
</script>

<style scoped>
.security-log-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-card {
  border-radius: 8px;
}

/* 一体化检索操作工具栏 */
.table-filter-bar {
  background: var(--el-fill-color-lighter, #f8fafc);
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 14px;
}

.inline-filter-form.el-form--inline {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.inline-filter-form.el-form--inline .el-form-item {
  margin-right: 0;
  margin-bottom: 0;
}

.method-badge {
  display: inline-block;
  padding: 2px 6px;
  font-size: 11px;
  font-weight: 700;
  border-radius: 4px;
  background: var(--el-fill-color-darker);
  color: var(--el-text-color-regular);
  margin-right: 6px;
}

.uri-text {
  font-family: monospace;
  font-size: 12px;
}

.risk-delta {
  font-weight: 700;
  color: var(--el-color-danger);
  margin-right: 4px;
}

.risk-total {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.risk-progress-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.risk-score-text {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.countdown-tag {
  font-family: monospace;
  font-weight: 600;
}

.ua-block {
  font-family: monospace;
  font-size: 12px;
  background: var(--el-fill-color-light);
  padding: 8px;
  border-radius: 4px;
  word-break: break-all;
  white-space: pre-wrap;
  max-height: 150px;
  overflow-y: auto;
}

.font-mono {
  font-family: monospace;
}
.font-bold {
  font-weight: 700;
}

.principal-cell {
  display: flex;
  flex-direction: column;
}

.principal-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 6px;
}

.principal-type-tag {
  margin-right: 2px;
}

.principal-id-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 500;
  padding: 1px 5px;
  border-radius: 4px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
}

.principal-ip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.subject-summary {
  margin-bottom: 12px;
}
</style>
