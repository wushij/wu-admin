<template>
  <div class="retrieval-panel">
    <InlineAlert
      v-if="!embeddingReady"
      type="warning"
      title="尚未配置可用的向量模型"
      description="当前检索会自动回退为关键词匹配。请在「AI 模型配置」中新增类型为「向量模型」的配置，并对文档执行「重建索引」后启用语义检索。"
    />

    <div class="query-card">
      <el-input
        v-model="query"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="输入一条用户可能会问的问题，例如：忘记密码怎么找回？"
        @keydown.ctrl.enter.prevent="runSearch"
      />
      <div class="query-controls">
        <div class="control">
          <span class="control-label">Top-K</span>
          <el-input-number v-model="topK" :min="1" :max="20" :step="1" size="small" />
        </div>
        <div class="control is-slider">
          <span class="control-label">相似度阈值 {{ minScore.toFixed(2) }}</span>
          <el-slider v-model="minScore" :min="0" :max="1" :step="0.05" size="small" style="flex: 1" />
        </div>
        <el-button type="primary" :icon="Search" :loading="loading" @click="runSearch">
          执行检索
        </el-button>
      </div>
    </div>

    <div v-if="result" class="result-block">
      <div class="result-head">
        <span class="result-summary">
          命中 {{ result.items.length }} 条 · {{ matchModeText }}
        </span>
        <span class="result-params">Top-K {{ result.topK }} · 阈值 {{ (result.minScore ?? 0).toFixed(2) }}</span>
      </div>

      <div v-if="result.items.length" class="result-list">
        <article v-for="(item, idx) in result.items" :key="item.chunkId" class="result-card">
          <div class="result-card-head">
            <span class="result-index motion-tabular">{{ idx + 1 }}</span>
            <span class="result-title">{{ item.heading || item.documentName || '未命名切片' }}</span>
            <span v-if="result.matchMode === 'VECTOR'" class="result-score motion-tabular">
              {{ (item.score * 100).toFixed(1) }}%
            </span>
            <span v-else class="result-score is-keyword">关键词命中</span>
          </div>
          <p class="result-content">{{ item.content }}</p>
          <div class="result-meta">
            <span>{{ item.documentName || '—' }}</span>
            <span v-if="item.heading">· {{ item.heading }}</span>
          </div>
        </article>
      </div>

      <EmptyState
        v-else
        title="没有召回结果"
        description="尝试降低相似度阈值、换一种问法，或确认文档已完成向量化"
      />
    </div>

    <EmptyState
      v-else
      title="输入问题开始检索测试"
      description="检索测试与 AI 助手使用同一套召回逻辑，可用于验证知识库是否覆盖目标问题"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import InlineAlert from '@/components/ui/InlineAlert.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import {
  retrieveKnowledgeTest,
  type KnowledgeRetrieveResult,
} from '@/api/system/ai-knowledge-base'

const props = defineProps<{
  knowledgeBaseId: number
  embeddingReady: boolean
}>()

const query = ref('')
const topK = ref(5)
const minScore = ref(0.25)
const loading = ref(false)
const result = ref<KnowledgeRetrieveResult | null>(null)

const matchModeText = computed(() => {
  const mode = result.value?.matchMode
  if (mode === 'VECTOR') return '向量语义检索'
  if (mode === 'KEYWORD') return '关键词兜底检索'
  return '无命中'
})

async function runSearch() {
  const text = query.value.trim()
  if (!text) {
    ElMessage.warning('请先输入检索问题')
    return
  }
  loading.value = true
  try {
    const res = await retrieveKnowledgeTest({
      knowledgeBaseId: props.knowledgeBaseId,
      query: text,
      topK: topK.value,
      minScore: minScore.value,
    })
    result.value = res.data || null
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.retrieval-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.query-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
}

.query-controls {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.control {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);

  &.is-slider {
    flex: 1;
    min-width: 220px;
  }
}

.control-label {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.result-block {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}

.result-summary {
  font-weight: 600;
  color: var(--color-text-primary);
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.result-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
}

.result-card-head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.result-index {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-circle);
  background: var(--color-primary-muted);
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.result-title {
  flex: 1;
  min-width: 0;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-score {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-success);

  &.is-keyword {
    color: var(--color-text-secondary);
    font-weight: 500;
  }
}

.result-content {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 160px;
  overflow-y: auto;
}

.result-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .query-controls {
    align-items: stretch;
  }

  .control.is-slider {
    min-width: 100%;
  }
}
</style>
