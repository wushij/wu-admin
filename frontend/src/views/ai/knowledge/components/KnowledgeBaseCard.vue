<template>
  <article class="kb-card motion-hover-lift" :class="{ 'is-disabled': item.status === 0 }">
    <div class="kb-card-head">
      <span class="kb-icon" aria-hidden="true">
        <el-icon :size="20"><Reading /></el-icon>
      </span>
      <div class="kb-title">
        <span class="kb-name" :title="item.name">{{ item.name }}</span>
        <span class="kb-desc" :title="item.description">{{ item.description || '暂无描述' }}</span>
      </div>
      <span class="kb-status" :class="statusMeta.cls">{{ statusMeta.label }}</span>
    </div>

    <div class="kb-metrics">
      <div class="metric">
        <span class="metric-value motion-tabular">{{ item.documentCount ?? 0 }}</span>
        <span class="metric-label">文档</span>
      </div>
      <div class="metric">
        <span class="metric-value motion-tabular">{{ item.chunkCount ?? 0 }}</span>
        <span class="metric-label">切片</span>
      </div>
      <div class="metric">
        <span class="metric-value motion-tabular">{{ item.indexedChunkCount ?? 0 }}</span>
        <span class="metric-label">已向量化</span>
      </div>
    </div>

    <div class="kb-meta">
      <span class="meta-item" :title="item.embeddingModelName || '未配置向量模型'">
        <el-icon :size="12"><Histogram /></el-icon>
        <span>{{ item.embeddingModelName || '未配置向量模型' }}</span>
      </span>
      <span class="meta-item" :title="item.updateTime">
        <el-icon :size="12"><Clock /></el-icon>
        <span>{{ formatTime(item.updateTime) }}</span>
      </span>
    </div>

    <div class="kb-card-foot">
      <el-button size="small" type="primary" plain :icon="FolderOpened" @click="$emit('open', item)">
        进入管理
      </el-button>
      <el-button
        size="small"
        :icon="Edit"
        v-permission="'system:ai-knowledge:update'"
        @click="$emit('edit', item)"
      >
        编辑
      </el-button>
      <el-button
        size="small"
        type="danger"
        plain
        :icon="Delete"
        v-permission="'system:ai-knowledge:delete'"
        @click="$emit('delete', item)"
      >
        删除
      </el-button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Clock, Delete, Edit, FolderOpened, Histogram, Reading } from '@element-plus/icons-vue'
import type { KnowledgeBaseVO } from '@/api/system/ai-knowledge-base'

const props = defineProps<{
  item: KnowledgeBaseVO
}>()

defineEmits<{
  open: [item: KnowledgeBaseVO]
  edit: [item: KnowledgeBaseVO]
  delete: [item: KnowledgeBaseVO]
}>()

const statusMeta = computed(() => {
  const raw = String(props.item.indexStatus || 'IDLE')
  const map: Record<string, { label: string; cls: string }> = {
    IDLE: { label: '待索引', cls: 'is-idle' },
    INDEXING: { label: '索引中', cls: 'is-indexing' },
    INDEXED: { label: '已就绪', cls: 'is-ready' },
    FAILED: { label: '索引异常', cls: 'is-failed' },
  }
  if (props.item.status === 0) {
    return { label: '已停用', cls: 'is-disabled' }
  }
  return map[raw] || { label: raw, cls: 'is-idle' }
})

function formatTime(value?: string) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped lang="scss">
.kb-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-surface);

  &.is-disabled {
    opacity: 0.7;
  }
}

.kb-card-head {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.kb-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  background: var(--color-primary-muted);
  color: var(--color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.kb-title {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.kb-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-desc {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-status {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 600;

  &.is-idle {
    background: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    border: 1px solid var(--color-border);
  }

  &.is-indexing {
    background: var(--color-warning-soft);
    color: var(--color-warning);
    border: 1px solid var(--color-warning-border);
  }

  &.is-ready {
    background: var(--color-success-soft);
    color: var(--color-success);
    border: 1px solid var(--color-success-border);
  }

  &.is-failed {
    background: var(--color-danger-soft);
    color: var(--color-danger);
    border: 1px solid var(--color-danger-border);
  }

  &.is-disabled {
    background: var(--color-bg-muted);
    color: var(--color-text-muted);
    border: 1px solid var(--color-border-subtle);
  }
}

.kb-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-2);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.metric {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.metric-value {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.metric-label {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.kb-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.kb-card-foot {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border-subtle);

  > .el-button {
    margin-left: 0;
  }
}

@media (max-width: 480px) {
  .kb-card-foot {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}
</style>
