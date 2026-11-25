<template>
  <div>
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
          <el-select v-model="riskQuery.blocked" placeholder="全部" clearable style="width: 115px">
            <el-option label="已封禁" :value="true" />
            <el-option label="未封禁" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="$emit('query')">搜索</el-button>
          <el-button :icon="Refresh" @click="$emit('reset')">重置</el-button>
          <el-button
            type="primary"
            :icon="Plus"
            v-permission="'system:security:block'"
            @click="$emit('manualBlock')"
          >
            手动封禁
          </el-button>
          <el-button :icon="RefreshRight" @click="$emit('reload')">刷新名单</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table v-if="riskList.length || loading" :data="riskList" v-loading="loading" border stripe>
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
            <span class="risk-score-text motion-tabular">
              {{ row.riskScore }} 分 · {{ row.riskLevelText }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="eventCount" label="窗口内事件" width="110" align="center">
        <template #default="{ row }"><span class="motion-tabular">{{ row.eventCount }} 次</span></template>
      </el-table-column>
      <el-table-column prop="lastEventTime" label="最近触发" width="170" align="center">
        <template #default="{ row }">{{ row.lastEventTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="封禁状态" width="180" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.blocked" type="danger" effect="plain" class="countdown-tag motion-tabular">
            剩余 {{ formatRemainingSeconds(row.remainingSeconds) }}
          </el-tag>
          <el-tag v-else type="success" effect="plain">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button type="primary" link size="small" @click="$emit('showEvents', row)">事件</el-button>
            <el-button
              v-if="row.blocked"
              type="success"
              link
              size="small"
              v-permission="'system:security:unblock'"
              @click="$emit('unblock', row.principal, row)"
            >
              解封
            </el-button>
            <el-button
              v-else
              type="danger"
              link
              size="small"
              v-permission="'system:security:block'"
              @click="$emit('blockIp', row.principal)"
            >
              拉黑
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <EmptyState
      v-else
      title="当前无风险主体"
      description="当前没有处于风险窗口中的账号或 IP，系统运行平稳"
    />

    <el-pagination
      v-if="total > 0"
      v-model:current-page="riskQuery.pageNo"
      v-model:page-size="riskQuery.pageSize"
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
import { Search, Refresh, RefreshRight, Plus } from '@element-plus/icons-vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import type { RiskSubjectPageQuery, RiskSubjectVO } from '@/api/system/security'
import { formatRemainingSeconds } from '../utils/securityLogFormatters'

const riskQuery = defineModel<RiskSubjectPageQuery>('riskQuery', { required: true })

defineProps<{
  riskList: RiskSubjectVO[]
  loading: boolean
  total: number
}>()

defineEmits<{
  query: []
  reset: []
  reload: []
  manualBlock: []
  showEvents: [row: RiskSubjectVO]
  unblock: [principal: string, row: RiskSubjectVO]
  blockIp: [principal: string]
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

.risk-progress-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.risk-score-text {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.countdown-tag {
  font-family: var(--font-mono);
  font-weight: 600;
}

.font-mono {
  font-family: var(--font-mono);
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
  padding: 1px 8px;
  border-radius: var(--radius-pill);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-text-secondary);
}

.principal-ip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
