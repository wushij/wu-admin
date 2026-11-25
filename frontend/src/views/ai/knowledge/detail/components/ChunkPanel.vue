<template>
  <div class="chunk-panel">
    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-value motion-tabular">{{ stats?.total ?? 0 }}</span>
        <span class="stat-label">切片总数</span>
      </div>
      <div class="stat-card is-success">
        <span class="stat-value motion-tabular">{{ stats?.indexed ?? 0 }}</span>
        <span class="stat-label">已向量化</span>
      </div>
      <div class="stat-card">
        <span class="stat-value motion-tabular">{{ stats?.avgTokens ?? 0 }}</span>
        <span class="stat-label">平均 Token</span>
      </div>
      <div class="stat-card" :class="{ 'is-danger': (stats?.failed ?? 0) > 0 }">
        <span class="stat-value motion-tabular">{{ (stats?.failed ?? 0) + (stats?.pending ?? 0) }}</span>
        <span class="stat-label">待处理 / 异常</span>
      </div>
    </div>

    <div class="panel-toolbar">
      <el-form :model="queryParams" inline class="panel-form">
        <el-form-item label="文档">
          <el-select
            v-model="queryParams.documentId"
            placeholder="全部文档"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option
              v-for="doc in documentOptions"
              :key="doc.id"
              :label="doc.fileName"
              :value="doc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.embedStatus" placeholder="全部" clearable style="width: 130px">
            <el-option label="待向量化" value="PENDING" />
            <el-option label="已向量化" value="INDEXED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索切片内容"
            clearable
            style="width: 180px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="loading && !list.length" class="chunk-skeleton">
      <SkeletonBlock mode="rows" :count="4" />
    </div>

    <div v-else-if="list.length" v-loading="loading" class="chunk-list">
      <article
        v-for="chunk in list"
        :key="chunk.id"
        class="chunk-row"
        role="button"
        tabindex="0"
        @click="openDetail(chunk)"
        @keydown.enter.prevent="openDetail(chunk)"
      >
        <div class="chunk-index motion-tabular">#{{ chunk.chunkIndex + 1 }}</div>
        <div class="chunk-body">
          <div class="chunk-head">
            <span class="chunk-title">{{ chunk.heading || '未命名切片' }}</span>
            <span class="status-dot" :class="embedMeta(chunk.embedStatus).cls" aria-hidden="true" />
            <span class="chunk-status">{{ embedMeta(chunk.embedStatus).label }}</span>
          </div>
          <p class="chunk-preview">{{ chunk.content }}</p>
          <div class="chunk-meta">
            <span>{{ chunk.documentName || '—' }}</span>
            <span>·</span>
            <span>{{ chunk.charCount ?? 0 }} 字</span>
            <span>·</span>
            <span>{{ chunk.tokenEstimate ?? 0 }} tokens</span>
            <span v-if="chunk.embeddingDim">· {{ chunk.embeddingDim }} 维</span>
          </div>
        </div>
        <div class="chunk-actions" @click.stop>
          <el-button link type="primary" size="small" @click="openDetail(chunk)">查看</el-button>
          <el-button
            link
            type="primary"
            size="small"
            :loading="reindexingId === chunk.id"
            v-permission="'system:ai-knowledge:reindex'"
            :disabled="chunk.embedStatus === 'PENDING' && !stats?.indexed"
            @click="handleReindexChunk(chunk)"
          >
            重建向量
          </el-button>
        </div>
      </article>
    </div>

    <EmptyState
      v-else
      title="还没有切片"
      description="上传文档并完成解析后，系统会按标题结构自动切片；也可在文档列表点击「重新切片」"
    />

    <el-pagination
      v-if="total > 0"
      v-model:current-page="queryParams.pageNo"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="table-pagination"
      @size-change="loadData()"
      @current-change="loadData()"
    />

    <ChunkViewerDrawer v-model:visible="viewerVisible" :chunk="activeChunk" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import type { KnowledgeChunkVO } from '@/api/system/ai-knowledge-chunk'
import ChunkViewerDrawer from './ChunkViewerDrawer.vue'
import { useKnowledgeChunks } from '../composables/useKnowledgeChunks'

const props = defineProps<{
  knowledgeBaseId: number
  refreshKey: number
}>()

const {
  loading,
  list,
  total,
  stats,
  documentOptions,
  queryParams,
  reindexingId,
  loadData,
  handleQuery,
  resetQuery,
  handleReindexChunk,
  embedMeta,
} = useKnowledgeChunks(() => props.knowledgeBaseId, () => props.refreshKey)

const viewerVisible = ref(false)
const activeChunk = ref<KnowledgeChunkVO | null>(null)

function openDetail(chunk: KnowledgeChunkVO) {
  activeChunk.value = chunk
  viewerVisible.value = true
}
</script>

<style scoped lang="scss">
.chunk-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-2);
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);

  &.is-success .stat-value {
    color: var(--color-success);
  }

  &.is-danger .stat-value {
    color: var(--color-danger);
  }
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.panel-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.panel-form {
  display: flex;
  flex-wrap: wrap;

  :deep(.el-form-item) {
    margin-bottom: var(--space-2);
    margin-right: var(--space-2);
  }
}

.chunk-skeleton {
  padding: var(--space-2) 0;
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-height: 80px;
}

.chunk-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out);

  &:hover {
    border-color: var(--el-color-primary-light-5);
    background: var(--color-bg-subtle);
  }

  &:focus-visible {
    outline: 2px solid var(--el-color-primary);
    outline-offset: 2px;
  }
}

.chunk-index {
  flex-shrink: 0;
  width: 42px;
  height: 26px;
  border-radius: var(--radius-pill);
  background: var(--color-bg-muted);
  color: var(--color-text-secondary);
  font-size: 11px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.chunk-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chunk-head {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.chunk-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: var(--radius-circle);
  flex-shrink: 0;
  background: var(--color-text-muted);

  &.is-success {
    background: var(--color-success);
  }

  &.is-failed {
    background: var(--color-danger);
  }

  &.is-pending {
    background: var(--color-warning);
  }
}

.chunk-status {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--color-text-secondary);
}

.chunk-preview {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.chunk-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 11px;
  color: var(--color-text-muted);
}

.chunk-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chunk-row {
    flex-wrap: wrap;
  }

  .chunk-actions {
    flex-direction: row;
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
