<template>
  <div class="doc-panel">
    <div class="panel-toolbar">
      <el-form :model="queryParams" inline class="panel-form">
        <el-form-item label="文件名">
          <el-input
            v-model="queryParams.fileName"
            placeholder="搜索文件名"
            clearable
            style="width: 180px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="解析状态">
          <el-select v-model="queryParams.parseStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="待解析" value="PENDING" />
            <el-option label="解析中" value="PARSING" />
            <el-option label="已解析" value="SUCCESS" />
            <el-option label="解析失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="向量状态">
          <el-select v-model="queryParams.indexStatus" placeholder="全部" clearable style="width: 130px">
            <el-option label="待向量化" value="PENDING" />
            <el-option label="向量化中" value="INDEXING" />
            <el-option label="已向量化" value="INDEXED" />
            <el-option label="部分完成" value="PARTIAL" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button
        type="primary"
        plain
        :icon="Upload"
        v-permission="'system:ai-knowledge:upload'"
        @click="$emit('upload')"
      >
        上传文档
      </el-button>
    </div>

    <el-table v-loading="loading" :data="list" class="doc-table" border>
      <el-table-column label="文档" min-width="240">
        <template #default="{ row }">
          <div class="doc-cell">
            <FileTypeIcon :file-name="row.fileName" size="sm" />
            <div class="doc-info">
              <span class="doc-name" :title="row.fileName">{{ row.fileName }}</span>
              <span class="doc-meta">
                {{ (row.fileType || '').toUpperCase() }} · {{ formatSize(row.fileSize) }} ·
                {{ row.charCount ?? 0 }} 字
              </span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="解析状态" width="100" align="center">
        <template #default="{ row }">
          <span class="status-pill" :class="parseMeta(row.parseStatus).cls">
            {{ parseMeta(row.parseStatus).label }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="切片" width="70" align="center">
        <template #default="{ row }">
          <span class="motion-tabular">{{ row.chunkCount ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="向量状态" width="110" align="center">
        <template #default="{ row }">
          <el-tooltip
            :disabled="!row.errorMsg"
            :content="row.errorMsg"
            placement="top"
            :show-after="300"
          >
            <span class="status-pill" :class="indexMeta(row.indexStatus).cls">
              {{ indexMeta(row.indexStatus).label }}
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="70" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            v-permission="'system:ai-knowledge:update'"
            @change="(v: boolean | string | number) => toggleStatus(row, Boolean(v))"
          />
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" min-width="140" align="center">
        <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button
              link
              type="primary"
              size="small"
              v-permission="'system:ai-knowledge:reindex'"
              @click="handleReparse(row)"
            >
              重新解析
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              v-permission="'system:ai-knowledge:reindex'"
              @click="handleReindex(row)"
            >
              重新切片
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              v-permission="'system:ai-knowledge:delete'"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
      <template #empty>
        <EmptyState
          title="还没有文档"
          description="上传 PDF、Word、Markdown 或 TXT 文档后，系统会自动解析、切片并向量化"
        />
      </template>
    </el-table>

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
  </div>
</template>

<script setup lang="ts">
import { Search, Refresh, Upload } from '@element-plus/icons-vue'
import FileTypeIcon from '@/components/ui/FileTypeIcon.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import type { KnowledgeDocumentVO } from '@/api/system/ai-knowledge-document'
import { useKnowledgeDocuments } from '../composables/useKnowledgeDocuments'

const props = defineProps<{
  knowledgeBaseId: number
  refreshKey: number
}>()

const emit = defineEmits<{
  upload: []
  changed: []
}>()

const {
  loading,
  list,
  total,
  queryParams,
  parseMeta,
  indexMeta,
  loadData,
  handleQuery,
  resetQuery,
  handleReparse,
  handleReindex,
  toggleStatus,
  handleDelete: removeDocument,
} = useKnowledgeDocuments(() => props.knowledgeBaseId, () => props.refreshKey)

async function handleDelete(row: KnowledgeDocumentVO) {
  await removeDocument(row)
  emit('changed')
}

function formatSize(size?: number) {
  if (!size) return '—'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function formatTime(value?: string) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped lang="scss">
.doc-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
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
  gap: 0;

  :deep(.el-form-item) {
    margin-bottom: var(--space-2);
    margin-right: var(--space-2);
  }
}

.doc-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.doc-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.doc-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 1px 9px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;

  &.is-pending {
    background: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    border: 1px solid var(--color-border);
  }

  &.is-running {
    background: var(--color-primary-muted);
    color: var(--color-primary);
    border: 1px solid var(--color-border-subtle);
  }

  &.is-success {
    background: var(--color-success-soft);
    color: var(--color-success);
    border: 1px solid var(--color-success-border);
  }

  &.is-warning {
    background: var(--color-warning-soft);
    color: var(--color-warning);
    border: 1px solid var(--color-warning-border);
  }

  &.is-failed {
    background: var(--color-danger-soft);
    color: var(--color-danger);
    border: 1px solid var(--color-danger-border);
  }
}
</style>
