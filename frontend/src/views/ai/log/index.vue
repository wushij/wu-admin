<template>
  <div class="app-container module-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.aiLog" />
            <span>AI 对话日志</span>
          </div>
          <p class="module-hero-desc">记录 AI wu助手的问答明细与 Token 消耗，支持按用户、供应商与状态检索</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ total }}</div>
          <div class="stat-label">对话总数</div>
        </div>
      </div>
    </el-card>

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable style="width: 160px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="queryParams.provider" placeholder="全部供应商" clearable style="width: 150px">
            <el-option v-for="(label, key) in PROVIDER_LABELS" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.chatStatus" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
            <el-option label="用户中断" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="会话ID">
          <el-input v-model="queryParams.conversationId" placeholder="请输入会话ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>对话日志列表</span>
          <el-button
            v-permission="'system:ai-log:delete'"
            type="danger"
            plain
            size="small"
            :icon="Delete"
            @click="handleClean"
          >
            清空日志
          </el-button>
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
        <el-table-column prop="id" label="ID" width="80" align="center" header-align="center" />
        <el-table-column prop="username" label="用户" width="110" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column label="提问内容" min-width="220" align="left" header-align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ row.question || '—' }}</template>
        </el-table-column>
        <el-table-column label="供应商" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ PROVIDER_LABELS[row.provider] || row.provider }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="模型" min-width="130" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column label="Tokens" width="100" align="center" header-align="center">
          <template #default="{ row }">{{ row.totalTokens || 0 }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="center" header-align="center">
          <template #default="{ row }">{{ formatDuration(row.durationMs) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="80" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.source === 'mobile' ? 'warning' : 'info'" effect="plain">
              {{ row.source === 'mobile' ? '移动端' : 'PC端' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.chatStatus).type" size="small">{{ statusMeta(row.chatStatus).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="对话时间" width="170" align="center" header-align="center" />
        <el-table-column label="操作" width="160" fixed="right" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="openDetail(row)">详情</el-button>
              <el-button
                v-permission="'system:ai-log:delete'"
                type="danger"
                size="small"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <el-drawer v-model="detailVisible" title="对话详情" size="560px" destroy-on-close>
      <el-descriptions :column="2" border size="small" class="detail-desc">
        <el-descriptions-item label="用户">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ detail.source === 'mobile' ? '移动端' : 'PC端' }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ PROVIDER_LABELS[detail.provider || ''] || detail.provider }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ detail.modelName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMeta(detail.chatStatus).type" size="small">{{ statusMeta(detail.chatStatus).label }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ formatDuration(detail.durationMs) }}</el-descriptions-item>
        <el-descriptions-item label="提问 Tokens">{{ detail.promptTokens || 0 }}</el-descriptions-item>
        <el-descriptions-item label="回答 Tokens">{{ detail.completionTokens || 0 }}</el-descriptions-item>
        <el-descriptions-item label="会话ID" :span="2">
          <span class="mono">{{ detail.conversationId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="对话时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
      </el-descriptions>

      <div class="chat-detail">
        <div class="chat-block question">
          <div class="chat-block-title">
            <el-icon><User /></el-icon>
            <span>用户提问</span>
          </div>
          <pre class="chat-content">{{ detail.question || '—' }}</pre>
        </div>
        <div class="chat-block answer">
          <div class="chat-block-title">
            <el-icon><MagicStick /></el-icon>
            <span>AI 回答</span>
          </div>
          <div v-if="detail.answer" class="chat-content-markdown">
            <AiWuMarkdown :content="detail.answer" />
          </div>
          <div v-else class="chat-content-empty">—</div>
        </div>
        <el-alert
          v-if="detail.errorMsg"
          type="error"
          :closable="false"
          show-icon
          title="错误信息"
          :description="detail.errorMsg"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, User, MagicStick, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import AiWuMarkdown from '@/components/AiWu/AiWuMarkdown.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import { pageAiChatLog, deleteAiChatLog, cleanAiChatLog, type AiChatLogVO, type AiChatLogPageQuery } from '@/api/system/ai-log'

const PROVIDER_LABELS: Record<string, string> = {
  deepseek: 'DeepSeek',
  openai: 'OpenAI',
  qwen: '通义千问',
  kimi: 'Kimi',
}

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const loading = ref(false)
const total = ref(0)
const tableData = ref<AiChatLogVO[]>([])
const detailVisible = ref(false)
const detail = ref<Partial<AiChatLogVO>>({})

const queryParams = reactive<AiChatLogPageQuery>({
  pageNo: 1,
  pageSize: 10,
  username: '',
  provider: '',
  chatStatus: null,
  conversationId: '',
})

function statusMeta(status: number | undefined): { label: string; type: 'success' | 'danger' | 'warning' } {
  if (status === 1) return { label: '成功', type: 'success' }
  if (status === 2) return { label: '用户中断', type: 'warning' }
  return { label: '失败', type: 'danger' }
}

function formatDuration(ms: number | undefined) {
  if (!ms || ms <= 0) return '—'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageAiChatLog({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      username: queryParams.username || undefined,
      provider: queryParams.provider || undefined,
      chatStatus: queryParams.chatStatus,
      conversationId: queryParams.conversationId || undefined,
    })
    tableData.value = res.data?.list || []
    total.value = Number(res.data?.total) || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNo = 1
  loadData()
}

function resetQuery() {
  queryParams.username = ''
  queryParams.provider = ''
  queryParams.chatStatus = null
  queryParams.conversationId = ''
  handleQuery()
}

function openDetail(row: AiChatLogVO) {
  detail.value = { ...row }
  detailVisible.value = true
}

async function handleDelete(row: AiChatLogVO) {
  await ElMessageBox.confirm('确定要删除该对话日志吗？', '提示', { type: 'warning' })
  await deleteAiChatLog(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleClean() {
  try {
    await ElMessageBox.confirm('确定要清空所有 AI 对话日志数据？此操作不可撤销！', '清空确认', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await cleanAiChatLog()
    ElMessage.success('已清空所有对话日志')
    loadData()
  } catch {
    /* cancel */
  }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mono {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

.detail-desc {
  margin-bottom: 16px;
}

.chat-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-block {
  border-radius: 10px;
  padding: 12px 14px;

  &.question {
    background: var(--el-color-primary-light-9);
  }

  &.answer {
    background: var(--el-fill-color-lighter);
  }
}

.chat-block-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}

.chat-content {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 320px;
  overflow: auto;
}
</style>
