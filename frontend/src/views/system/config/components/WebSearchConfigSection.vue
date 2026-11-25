<template>
  <!-- 5. Tavily 联网搜索（对齐 EduMind 折叠面板与额度看板样式） -->
  <el-collapse-item name="tavily">
    <template #title>
      <span class="collapse-title-with-icon">
        <AiProviderIcon provider="tavily" :size="18" />
        <span>Tavily 联网搜索</span>
      </span>
    </template>

    <div class="tavily-config-content">
      <el-form :model="draft.tavily" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
        <el-form-item label="联网检索开关">
          <div class="switch-row">
            <el-switch v-model="draft.tavily.enabled" :disabled="!canEdit" />
            <span class="item-tip">开启后，在 AI 助手中勾选「联网搜索」时将调用 Tavily 获取全网实时动态</span>
          </div>
        </el-form-item>

        <el-form-item label="API Key">
          <div class="key-input-group">
            <el-input
              v-model="draft.tavily.apiKey"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请输入 Tavily API Key（如 tvly-dev-...，留空或保持掩码则沿用原密钥）"
              :disabled="!canEdit"
            />
            <el-button
              type="primary"
              plain
              :icon="Refresh"
              :loading="usageLoading"
              :disabled="!canEdit"
              class="test-btn"
              @click="handleQueryTavilyUsage(false)"
            >
              测试连通与查额度
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="搜索深度模式">
          <el-radio-group v-model="draft.tavily.searchDepth" :disabled="!canEdit">
            <el-radio value="basic">
              <span>基础检索 (basic)</span>
              <span class="radio-hint">急速响应，单次消耗 1 点额度</span>
            </el-radio>
            <el-radio value="advanced">
              <span>深度研究 (advanced)</span>
              <span class="radio-hint">深入检索复杂资料，单次消耗 2 点额度</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="检索主题类别">
          <el-radio-group v-model="draft.tavily.topic" :disabled="!canEdit">
            <el-radio value="general">通用网页 (general)</el-radio>
            <el-radio value="news">实时新闻 (news)</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="单次返回条数">
          <el-input-number v-model="draft.tavily.maxResults" :min="1" :max="10" :disabled="!canEdit" />
        </el-form-item>

        <el-form-item label="AI 事实综述">
          <div class="switch-row">
            <el-switch v-model="draft.tavily.includeAnswer" :disabled="!canEdit" />
            <span class="item-tip">由 Tavily 先生成一段事实总结并注入提示词，可降低大模型幻觉</span>
          </div>
        </el-form-item>
      </el-form>

      <!-- Tavily 官方账户实时额度看板（对标 EduMind Quota Dashboard） -->
      <div v-loading="usageLoading" class="quota-dashboard-card">
        <div class="dashboard-header">
          <div class="header-left">
            <span class="badge-indicator" :class="quotaStatusClass" />
            <span class="dashboard-title">Tavily 官方账户实时额度看板</span>
            <span v-if="tavilyUsage?.currentPlan" class="plan-tag">{{ tavilyUsage.currentPlan }}</span>
          </div>
          <el-button
            link
            type="primary"
            size="small"
            :icon="Refresh"
            :loading="usageLoading"
            :disabled="!canEdit"
            @click="handleQueryTavilyUsage(false)"
          >
            刷新实时用量
          </el-button>
        </div>

        <div v-if="tavilyUsage && tavilyUsage.valid" class="dashboard-body">
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-label">当月剩余可用额度</div>
              <div class="stat-value highlight-green">
                {{ planRemaining }}
                <span class="stat-unit">次</span>
              </div>
              <div class="stat-hint">次月自动重置刷新</div>
            </div>

            <div class="stat-card">
              <div class="stat-label">当月已消耗额度</div>
              <div class="stat-value">
                {{ tavilyUsage.usage ?? 0 }}
                <span class="stat-unit">次</span>
              </div>
              <div class="stat-hint">
                <span v-if="(tavilyUsage.limit ?? 0) > 0">总额度：{{ tavilyUsage.limit }} 次/月</span>
                <span v-else>总额度：按量计费/动态配额</span>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-label">细分调用分布</div>
              <div class="sub-metrics-list">
                <div class="sub-metric-item">
                  <span class="sub-metric-label">Search</span>
                  <span class="sub-metric-val"><b>{{ tavilyUsage.searchUsage ?? 0 }}</b> 次</span>
                </div>
                <div class="sub-metric-item">
                  <span class="sub-metric-label">Extract</span>
                  <span class="sub-metric-val"><b>{{ tavilyUsage.extractUsage ?? 0 }}</b> 次</span>
                </div>
                <div class="sub-metric-item">
                  <span class="sub-metric-label">Crawl / Research</span>
                  <span class="sub-metric-val">
                    <b>{{ (tavilyUsage.crawlUsage ?? 0) + (tavilyUsage.researchUsage ?? 0) }}</b> 次
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div class="progress-section">
            <div class="progress-meta">
              <span>额度消耗占比：<b>{{ planUsagePercent }}%</b></span>
              <span v-if="(tavilyUsage.paygoLimit ?? 0) > 0">
                按量补充：{{ tavilyUsage.paygoUsage ?? 0 }} / {{ tavilyUsage.paygoLimit }}
              </span>
            </div>
            <el-progress
              :percentage="planUsagePercent"
              :color="progressColors"
              :stroke-width="8"
              :show-text="false"
            />
          </div>

          <div class="dashboard-footer">
            <span class="footer-tip">状态提示：连接正常，已同步官方配额（官方用量统计约有 15~30 分钟延迟）</span>
            <a href="https://tavily.com" target="_blank" rel="noopener noreferrer" class="portal-link">
              前往 Tavily 官方后台 ↗
            </a>
          </div>
        </div>

        <div v-else class="dashboard-empty">
          <el-icon class="empty-icon"><Compass /></el-icon>
          <div class="empty-text">
            {{ tavilyUsage?.message || '输入或配置 API Key 后，点击上方「测试连通与查额度」即可查看实时用量。' }}
          </div>
          <div class="empty-actions">
            <a href="https://tavily.com" target="_blank" rel="noopener noreferrer" class="portal-link">
              还没有密钥？前往 Tavily 免费注册获取 1,000 次/月 ↗
            </a>
          </div>
        </div>
      </div>
    </div>
  </el-collapse-item>

  <!-- 6. 自定义联网搜索引擎 -->
  <el-collapse-item name="web-search">
    <template #title>
      <span class="collapse-title-with-icon">
        <el-icon class="collapse-title-icon"><Connection /></el-icon>
        <span>自定义联网搜索引擎</span>
        <span v-if="sortedProviders.length" class="collapse-count-badge">
          {{ sortedProviders.length }} 个
        </span>
      </span>
    </template>

    <div class="web-search-pane">
      <el-form :model="draft.webSearch" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
        <el-form-item label="启用联网能力">
          <div class="switch-row">
            <el-switch v-model="draft.webSearch.enabled" :disabled="!canEdit" />
            <span class="item-tip">{{ draft.webSearch.enabled ? 'AI wu 助手可开启联网检索' : '全局停用联网' }}</span>
          </div>
        </el-form-item>

        <el-form-item label="调度优先策略">
          <el-select
            v-model="draft.webSearch.strategy"
            placeholder="请选择调度策略"
            :disabled="!canEdit || !draft.webSearch.enabled"
            style="width: 240px"
          >
            <el-option label="自定义引擎优先" value="custom_first" />
            <el-option label="Tavily 官方优先" value="tavily_first" />
          </el-select>
        </el-form-item>

        <el-form-item label="免 Key 网页兜底">
          <div class="switch-row">
            <el-switch
              v-model="draft.webSearch.fallbackToDuckDuckGo"
              :disabled="!canEdit || !draft.webSearch.enabled"
            />
            <span class="item-tip">前序引擎均无果时尝试 DuckDuckGo 网页检索</span>
          </div>
        </el-form-item>
      </el-form>

      <div class="engine-toolbar">
        <span class="engine-toolbar__label">已配置的自定义引擎</span>
        <el-button
          v-if="canEdit"
          type="primary"
          size="small"
          :icon="Plus"
          @click="openCreateDialog"
        >
          新增搜索引擎
        </el-button>
      </div>

      <div v-if="sortedProviders.length" class="ws-engine-list">
        <div
          v-for="item in sortedProviders"
          :key="item.id"
          class="ws-engine-card"
          :class="{ 'is-disabled': !item.enabled }"
        >
          <div class="ws-engine-card__brand" :style="{ background: resolveEngineMeta(item).bg }">
            <AiProviderIcon :provider="resolveEngineMeta(item).icon" :size="20" />
          </div>
          <div class="ws-engine-card__main">
            <div class="ws-engine-card__top">
              <span class="ws-engine-card__name">{{ item.name }}</span>
              <el-tag size="small" :type="item.protocol === 'openai_search' ? 'primary' : 'success'" effect="plain">
                {{ item.protocol === 'openai_search' ? 'OpenAI 联网' : 'HTTP JSON' }}
              </el-tag>
              <el-tag size="small" type="info" effect="plain">优先级 #{{ item.priority }}</el-tag>
            </div>
            <div class="ws-engine-card__url">{{ item.endpointUrl }}</div>
          </div>

          <div class="ws-engine-card__actions">
            <el-switch v-model="item.enabled" size="small" :disabled="!canEdit" />
            <el-button
              size="small"
              plain
              :loading="testingEngineId === item.id"
              :disabled="!canEdit"
              @click="handleQuickTest(item)"
            >
              试搜
            </el-button>
            <el-button v-if="canEdit" size="small" plain @click="openEditDialog(item)">
              编辑
            </el-button>
            <el-button v-if="canEdit" size="small" type="danger" plain @click="handleRemoveProvider(item.id)">
              删除
            </el-button>
          </div>
        </div>
      </div>

      <div v-else class="engine-empty-bar">
        暂无自定义联网搜索引擎，可点击右侧「新增搜索引擎」快速接入 DeepSeek、Tavily 或自定义接口
      </div>
    </div>

    <CustomWebSearchDialog
      v-model="dialogVisible"
      :editing-engine="editingEngine"
      :default-priority="nextPriority"
      @save="handleSaveEngine"
    />
  </el-collapse-item>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Compass, Connection, Plus, Refresh } from '@element-plus/icons-vue'
import AiProviderIcon from '@/components/AiProviderIcon.vue'
import { queryTavilyUsage, testCustomWebSearchEngine } from '@/api/system/config'
import type { ConfigGroupMap, CustomWebSearchEngine, TavilyUsageVO } from '@/types/config'
import { getErrorMessage } from '@/utils/axiosError'
import CustomWebSearchDialog from './CustomWebSearchDialog.vue'

const draft = defineModel<ConfigGroupMap['thirdParty']>('draft', { required: true })

const props = defineProps<{
  canEdit: boolean
  activeCollapse?: string[]
}>()

const dialogVisible = ref(false)
const editingEngine = ref<CustomWebSearchEngine | null>(null)
const testingEngineId = ref<string>('')
const usageLoading = ref(false)
const tavilyUsage = ref<TavilyUsageVO | null>(null)

const progressColors = [
  { color: '#10b981', percentage: 60 },
  { color: '#3b82f6', percentage: 80 },
  { color: '#f59e0b', percentage: 95 },
  { color: '#ef4444', percentage: 100 },
]

const sortedProviders = computed(() => {
  const list = draft.value.webSearch?.customProviders || []
  return [...list].sort((a, b) => (a.priority ?? 10) - (b.priority ?? 10))
})

const nextPriority = computed(() => {
  const list = draft.value.webSearch?.customProviders || []
  if (!list.length) return 10
  return Math.max(...list.map((i) => i.priority ?? 10)) + 10
})

const planRemaining = computed(() => {
  if (!tavilyUsage.value) return 0
  // 后端已按官方配额算好剩余次数，优先直接使用；字段缺失时本地兜底
  if (typeof tavilyUsage.value.remaining === 'number') return tavilyUsage.value.remaining
  const limit = tavilyUsage.value.limit ?? 0
  const used = tavilyUsage.value.usage ?? 0
  return Math.max(0, limit - used)
})

const planUsagePercent = computed(() => {
  if (!tavilyUsage.value) return 0
  // 后端返回的 percent 带一位小数，进度条组件需要整数
  if (typeof tavilyUsage.value.percent === 'number') {
    return Math.min(100, Math.max(0, Math.round(tavilyUsage.value.percent)))
  }
  const limit = tavilyUsage.value.limit ?? 0
  if (limit <= 0) return 0
  const pct = Math.round(((tavilyUsage.value.usage || 0) / limit) * 100)
  return Math.min(100, Math.max(0, pct))
})

const quotaStatusClass = computed(() => {
  if (!tavilyUsage.value) return 'indicator--idle'
  if (!tavilyUsage.value.valid) return 'indicator--error'
  return planUsagePercent.value >= 90 ? 'indicator--warning' : 'indicator--success'
})

function resolveEngineMeta(item: CustomWebSearchEngine): { icon: string; bg: string } {
  const preset = (item.preset || '').toLowerCase()
  if (preset === 'deepseek') {
    return { icon: 'deepseek', bg: 'rgba(77, 107, 254, 0.12)' }
  }
  if (preset === 'tavily' || preset === 'tavily_proxy') {
    return { icon: 'tavily', bg: 'rgba(14, 165, 233, 0.12)' }
  }
  return { icon: 'custom', bg: 'rgba(139, 92, 246, 0.12)' }
}

function openCreateDialog() {
  if (!props.canEdit) return
  editingEngine.value = null
  dialogVisible.value = true
}

function openEditDialog(engine: CustomWebSearchEngine) {
  if (!props.canEdit) return
  editingEngine.value = engine
  dialogVisible.value = true
}

function handleSaveEngine(saved: CustomWebSearchEngine) {
  if (!draft.value.webSearch) {
    draft.value.webSearch = {
      enabled: true,
      strategy: 'custom_first',
      fallbackToDuckDuckGo: true,
      customProviders: [],
    }
  }
  const list = draft.value.webSearch.customProviders
  const idx = list.findIndex((item) => item.id === saved.id)
  if (idx >= 0) {
    list.splice(idx, 1, saved)
  } else {
    list.push(saved)
  }
  ElMessage.success('已加入配置草稿，请点击页面底部「保存全部」使其生效')
}

async function handleRemoveProvider(id: string) {
  try {
    await ElMessageBox.confirm('确定删除该自定义联网搜索引擎吗？保存配置后将永久移除。', '删除确认', {
      type: 'warning',
    })
  } catch {
    return
  }
  const list = draft.value.webSearch?.customProviders
  if (!list) return
  const idx = list.findIndex((item) => item.id === id)
  if (idx >= 0) {
    list.splice(idx, 1)
  }
}

async function handleQuickTest(engine: CustomWebSearchEngine) {
  testingEngineId.value = engine.id
  try {
    const res = await testCustomWebSearchEngine(engine, 'Vue 3 最新版本特性')
    const data = res.data
    if (data?.success) {
      ElMessage.success(`「${engine.name}」连通成功（${data.latencyMs}ms，命中 ${data.results?.length || 0} 条）`)
    } else {
      ElMessage.warning(data?.message || `「${engine.name}」未检索到有效结果`)
    }
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || `「${engine.name}」测试失败`)
  } finally {
    testingEngineId.value = ''
  }
}

async function handleQueryTavilyUsage(silent = false) {
  if (usageLoading.value) return
  usageLoading.value = true
  try {
    const res = await queryTavilyUsage(draft.value.tavily?.apiKey)
    tavilyUsage.value = res.data
    if (!silent) {
      if (res.data?.valid) {
        ElMessage.success(`Tavily 连通正常（当前套餐：${res.data.currentPlan || 'Active'}）`)
      } else {
        ElMessage.warning(res.data?.message || '未能获取 Tavily 额度信息')
      }
    }
  } catch (e) {
    if (!silent) {
      ElMessage.error(getErrorMessage(e) || '查询 Tavily 额度失败')
    }
  } finally {
    usageLoading.value = false
  }
}

watch(
  () => props.activeCollapse,
  (list = [], prev = []) => {
    if (list.includes('tavily') && !prev.includes('tavily') && draft.value.tavily?.apiKey?.trim()) {
      void handleQueryTavilyUsage(true)
    }
  },
)
</script>

<style scoped lang="scss">
.collapse-title-with-icon {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.collapse-title-icon {
  font-size: 16px;
  color: var(--el-color-primary);
}

.collapse-count-badge {
  display: inline-flex;
  align-items: center;
  line-height: 1.4;
  font-size: 11.5px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.tavily-config-content,
.web-search-pane {
  padding: 6px 4px 12px;
}

.switch-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.item-tip {
  color: var(--color-text-secondary);
  font-size: 12.5px;
  line-height: 1.45;
}

.key-input-group {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.test-btn {
  flex-shrink: 0;
}

.radio-hint {
  margin-left: 6px;
  color: var(--color-text-secondary);
  font-size: 12px;
  font-weight: normal;
}

/* 额度看板卡片（对齐 EduMind Quota Dashboard，采用全局主题变量适配亮/暗双模式） */
.quota-dashboard-card {
  margin-top: 16px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px 18px;
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.indicator--success { background: var(--color-success); }
.indicator--warning { background: var(--color-warning); }
.indicator--error { background: var(--color-danger); }
.indicator--idle { background: var(--color-text-muted); }

.dashboard-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.plan-tag {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: var(--radius-pill);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.stat-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.stat-value.highlight-green {
  color: var(--color-success);
}

.stat-unit {
  font-size: 12px;
  font-weight: normal;
  color: var(--color-text-secondary);
  margin-left: 2px;
}

.stat-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.sub-metrics-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sub-metric-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.sub-metric-label {
  color: var(--color-text-secondary);
}

.sub-metric-val {
  color: var(--color-text-primary);
}

.progress-section {
  margin-top: 14px;
}

.progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}

.dashboard-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--color-border);
  font-size: 12px;
}

.footer-tip {
  color: var(--color-text-secondary);
}

.portal-link {
  color: var(--el-color-primary);
  text-decoration: none;
  font-size: 12px;

  &:hover {
    text-decoration: underline;
  }
}

.dashboard-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 14px;
  text-align: center;
}

.empty-icon {
  font-size: 26px;
  color: var(--color-text-muted);
  margin-bottom: 6px;
}

.empty-text {
  font-size: 12.5px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.empty-actions {
  margin-top: 8px;
}

.engine-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 14px 0 10px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);

  &__label {
    font-size: 13px;
    font-weight: 600;
    color: var(--color-text-primary);
  }
}

.ws-engine-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ws-engine-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  transition: border-color var(--duration-fast) var(--ease-out), opacity var(--duration-fast) var(--ease-out);

  &.is-disabled {
    opacity: 0.65;
  }

  &:hover {
    border-color: var(--el-color-primary-light-5);
  }

  &__brand {
    width: 34px;
    height: 34px;
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__main {
    min-width: 0;
    flex: 1;
  }

  &__top {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__name {
    font-size: 13.5px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  &__url {
    margin-top: 3px;
    font-size: 12px;
    color: var(--color-text-secondary);
    word-break: break-all;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }
}

.engine-empty-bar {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px dashed var(--color-border);
  background: var(--color-bg-subtle);
  font-size: 12.5px;
  color: var(--color-text-secondary);
  text-align: center;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .ws-engine-card {
    flex-wrap: wrap;

    &__actions {
      width: 100%;
      justify-content: flex-end;
    }
  }
}
</style>
