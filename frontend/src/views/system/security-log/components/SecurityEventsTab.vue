<template>
  <div>
    <div class="table-filter-bar">
      <el-form :model="queryParams" inline class="inline-filter-form">
        <el-form-item label="客户端 IP">
          <el-input v-model="queryParams.ipaddr" placeholder="请输入 IP 地址" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="queryParams.eventType" placeholder="全部类型" clearable style="width: 150px">
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
          <el-select v-model="queryParams.action" placeholder="全部动作" clearable style="width: 130px">
            <el-option label="直接阻断 (403)" value="BLOCKED" />
            <el-option label="触发封禁" value="FORBIDDEN" />
            <el-option label="安全告警" value="WARNING" />
            <el-option label="要求验证" value="CHALLENGE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="$emit('query')">搜索</el-button>
          <el-button :icon="Refresh" @click="$emit('reset')">重置</el-button>
          <el-button
            type="danger"
            plain
            :icon="Delete"
            v-permission="'system:security:clear'"
            @click="$emit('clearAll')"
          >
            清空事件
          </el-button>
          <el-button :icon="RefreshRight" @click="$emit('reload')">刷新</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table
      v-if="eventList.length || loading"
      :data="eventList"
      v-loading="loading"
      border
      stripe
      class="security-table"
    >
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="ipaddr" label="客户端 IP" width="140" align="center">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" class="motion-tabular" @click="$emit('filterByIp', row.ipaddr)">
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
          <span class="risk-delta motion-tabular">+{{ row.riskDelta }}</span>
          <span class="risk-total motion-tabular">({{ row.totalRiskScore }}分)</span>
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
      <el-table-column label="操作" width="148" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button type="primary" link size="small" @click="$emit('detail', row)">详情</el-button>
            <el-button
              type="danger"
              link
              size="small"
              v-permission="'system:security:block'"
              @click="$emit('blockIp', row.ipaddr)"
            >
              拉黑
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              v-permission="'system:security:delete'"
              @click="$emit('deleteEvent', row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <EmptyState v-else title="暂无安全拦截事件" description="系统当前运行平稳，未检测到异常拦截事件" />

    <el-pagination
      v-if="total > 0"
      v-model:current-page="queryParams.pageNo"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="table-pagination"
      @size-change="$emit('reload')"
      @current-change="$emit('reload')"
    />
  </div>
</template>

<script setup lang="ts">
import { Search, Refresh, RefreshRight, Delete } from '@element-plus/icons-vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import type { SecurityEventPageQuery, SecurityEventVO } from '@/api/system/security'
import {
  formatEventTypeName,
  getEventTypeTagType,
  formatAction,
  getActionTagType,
} from '../utils/securityLogFormatters'

const queryParams = defineModel<SecurityEventPageQuery>('queryParams', { required: true })

defineProps<{
  eventList: SecurityEventVO[]
  loading: boolean
  total: number
}>()

defineEmits<{
  query: []
  reset: []
  reload: []
  clearAll: []
  filterByIp: [ip: string]
  detail: [row: SecurityEventVO]
  blockIp: [ip: string]
  deleteEvent: [row: SecurityEventVO]
}>()
</script>

<style scoped>
.table-filter-bar {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-4);
}

.inline-filter-form.el-form--inline {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2) var(--space-3);
}

.inline-filter-form.el-form--inline .el-form-item {
  margin-right: 0;
  margin-bottom: 0;
}

.method-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 700;
  border-radius: var(--radius-pill);
  background: var(--color-bg-muted);
  color: var(--color-text-secondary);
  margin-right: 6px;
}

.uri-text {
  font-family: var(--font-mono);
  font-size: 12px;
}

.risk-delta {
  font-weight: 700;
  color: var(--color-danger);
  margin-right: 4px;
}

.risk-total {
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
