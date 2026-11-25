<template>
  <div>
    <!-- 事件详情抽屉 -->
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
            <span class="reason-danger">{{ activeEvent.reason }}</span>
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
        <el-table v-if="subjectEvents.length" :data="subjectEvents" border stripe size="small">
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
        </el-table>
        <EmptyState v-else title="暂无主体事件" description="该主体最近没有安全事件记录" />
      </div>
    </el-drawer>

    <!-- 手动封禁对话框 -->
    <el-dialog v-model="blockDialogVisible" title="手动封禁风险主体 IP" width="460px">
      <el-form :model="blockForm" label-width="100px">
        <el-form-item label="目标 IP" required>
          <el-input v-model="blockForm.ip" placeholder="例如：192.168.1.100" />
        </el-form-item>
        <el-form-item label="封禁时长">
          <el-select v-model="blockForm.duration" placeholder="请选择封禁时长" style="width: 100%">
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
        <el-button type="danger" @click="$emit('confirmBlock')">确认封禁</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import EmptyState from '@/components/ui/EmptyState.vue'
import type { RiskSubjectVO, SecurityEventVO } from '@/api/system/security'
import {
  formatEventTypeName,
  getEventTypeTagType,
  formatRemainingSeconds,
  formatPrincipalDisplayName,
} from '../utils/securityLogFormatters'

const detailVisible = defineModel<boolean>('detailVisible', { default: false })
const subjectEventsVisible = defineModel<boolean>('subjectEventsVisible', { default: false })
const blockDialogVisible = defineModel<boolean>('blockDialogVisible', { default: false })
const blockForm = defineModel<{ ip: string; duration: number; reason: string }>('blockForm', {
  required: true,
})

defineProps<{
  activeEvent: SecurityEventVO | null
  activeSubject: RiskSubjectVO | null
  subjectEvents: SecurityEventVO[]
  subjectEventsLoading: boolean
}>()

defineEmits<{
  confirmBlock: []
}>()
</script>

<style scoped>
.reason-danger {
  color: var(--color-danger);
  font-weight: 600;
}

.ua-block {
  font-family: var(--font-mono);
  font-size: 12px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  word-break: break-all;
  white-space: pre-wrap;
  max-height: 150px;
  overflow-y: auto;
}

.subject-summary {
  margin-bottom: var(--space-3);
}
</style>
