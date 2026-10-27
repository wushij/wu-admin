<template>
  <el-card shadow="never" class="doc-table-card">
    <template #header>
      <div class="card-header-bar">
        <div class="header-left">
          <span class="header-title">Markdown 文档清单</span>
          <transition name="el-fade-in">
            <div v-if="selectedRows.length > 0" class="batch-bar">
              <span class="batch-tip">已选中 <b>{{ selectedRows.length }}</b> 篇文档</span>
              <el-button
                v-permission="'system:ai-knowledge:update'"
                size="small"
                type="success"
                plain
                @click="$emit('batch-status', 1)"
              >
                批量启用
              </el-button>
              <el-button
                v-permission="'system:ai-knowledge:update'"
                size="small"
                type="warning"
                plain
                @click="$emit('batch-status', 0)"
              >
                批量停用
              </el-button>
              <el-button
                v-permission="'system:ai-knowledge:delete'"
                size="small"
                type="danger"
                plain
                @click="$emit('batch-delete')"
              >
                批量删除
              </el-button>
            </div>
          </transition>
        </div>
        <div class="header-right">
          <el-tooltip content="刷新文档列表" placement="top">
            <el-button circle :icon="Refresh" size="small" @click="$emit('refresh')" />
          </el-tooltip>
        </div>
      </div>
    </template>

    <el-table
      v-loading="loading"
      :data="tableData"
      row-key="id"
      class="doc-table"
      @selection-change="(rows) => $emit('selection-change', rows)"
    >
      <el-table-column type="selection" width="46" align="center" />

      <!-- Markdown 文档信息列 -->
      <el-table-column label="文档名称" min-width="320" align="left">
        <template #default="{ row }">
          <div class="doc-title-row" @click="$emit('view', row)">
            <div class="md-badge-icon">
              <span>MD</span>
            </div>
            <div class="doc-text-group">
              <span class="doc-title-text">{{ row.title }}</span>
              <span class="doc-summary-text">{{ getCleanSnippet(row.content) }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <!-- 业务分类与字数规格 -->
      <el-table-column label="规章分类" width="140" align="center">
        <template #default="{ row }">
          <el-tag
            size="small"
            :type="getCategoryTagType(row.category)"
            effect="plain"
            class="category-tag"
          >
            {{ CATEGORY_LABELS[row.category] || row.category || '通用制度' }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 字数统计 -->
      <el-table-column label="文档字数" width="120" align="center">
        <template #default="{ row }">
          <span class="words-pill">{{ (row.content || '').length.toLocaleString() }} 字</span>
        </template>
      </el-table-column>

      <!-- 启用状态切换 -->
      <el-table-column label="检索生效" width="110" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :loading="row._statusLoading"
            inline-prompt
            active-text="生效"
            inactive-text="停用"
            active-color="#10b981"
            @change="(val) => $emit('status-change', row, val ? 1 : 0)"
          />
        </template>
      </el-table-column>

      <!-- 更新时间 -->
      <el-table-column prop="updateTime" label="更新时间" width="170" align="center">
        <template #default="{ row }">
          <span class="time-text">{{ row.updateTime || row.createTime || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 操作动作列 -->
      <el-table-column label="操作" width="190" align="center" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button
              type="primary"
              link
              size="small"
              :icon="View"
              @click="$emit('view', row)"
            >
              预览
            </el-button>
            <el-button
              v-permission="'system:ai-knowledge:update'"
              type="primary"
              link
              size="small"
              :icon="Edit"
              @click="$emit('edit', row)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'system:ai-knowledge:delete'"
              type="danger"
              link
              size="small"
              :icon="Delete"
              @click="$emit('delete', row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页器 -->
    <div class="pagination-footer">
      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="$emit('refresh')"
        @current-change="$emit('refresh')"
      />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { Refresh, View, Edit, Delete } from '@element-plus/icons-vue'
import type { KnowledgeRowItem } from '../composables/useKnowledgeList'
import type { AiKnowledgePageQuery } from '@/api/system/ai-knowledge'
import { CATEGORY_LABELS, getCategoryTagType } from '../constants/knowledge'

defineProps<{
  loading: boolean
  total: number
  tableData: KnowledgeRowItem[]
  selectedRows: KnowledgeRowItem[]
  queryParams: AiKnowledgePageQuery
}>()

defineEmits<{
  (e: 'refresh'): void
  (e: 'selection-change', rows: KnowledgeRowItem[]): void
  (e: 'status-change', row: KnowledgeRowItem, status: number): void
  (e: 'batch-status', status: number): void
  (e: 'batch-delete'): void
  (e: 'view', row: KnowledgeRowItem): void
  (e: 'edit', row: KnowledgeRowItem): void
  (e: 'delete', row: KnowledgeRowItem): void
}>()

// 提取清洗后的 Markdown 文本纯文本片段
function getCleanSnippet(content?: string): string {
  if (!content) return '暂无正文描述'
  const text = content
    .replace(/^#+\s+/gm, '') // 去除标题符
    .replace(/[*_`~>\[\]\(\)]/g, '') // 去除强调与链接字符
    .replace(/```[\s\S]*?```/g, ' [代码块] ') // 去除代码块
    .replace(/\s+/g, ' ')
    .trim()
  if (text.length > 70) {
    return text.slice(0, 70) + '...'
  }
  return text || '暂无正文描述'
}
</script>

<style scoped lang="scss">
.doc-table-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #f1f5f9;
  }
  :deep(.el-card__body) {
    padding: 0;
  }
}

.card-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .header-title {
      font-size: 15px;
      font-weight: 600;
      color: #1e293b;
    }
  }
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f1f5f9;
  padding: 4px 12px;
  border-radius: 8px;

  .batch-tip {
    font-size: 12.5px;
    color: #475569;
    b {
      color: #2563eb;
    }
  }
}

.doc-table {
  :deep(.el-table__row) {
    transition: background 0.15s ease;
  }
}

.doc-title-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
  cursor: pointer;

  .md-badge-icon {
    width: 32px;
    height: 32px;
    min-width: 32px;
    border-radius: 7px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    color: #475569;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.5px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
    margin-top: 2px;
    transition: all 0.2s ease;
  }

  .doc-text-group {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .doc-title-text {
      font-size: 14px;
      font-weight: 600;
      color: #0f172a;
      transition: color 0.15s ease;

      &:hover {
        color: #2563eb;
      }
    }

    .doc-summary-text {
      font-size: 12px;
      line-height: 1.5;
      color: #64748b;
    }
  }
}

.category-tag {
  font-weight: 500;
  border-radius: 6px;
}

.words-pill {
  font-size: 12.5px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: #64748b;
  background: #f8fafc;
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid #f1f5f9;
}

.time-text {
  font-size: 12.5px;
  color: #64748b;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex-wrap: nowrap;
  white-space: nowrap;

  :deep(.el-button) {
    margin: 0;
    padding: 0 4px;
    font-size: 12px;

    .el-icon {
      font-size: 13px;
      margin-right: 2px;
    }
  }
}

.pagination-footer {
  display: flex;
  justify-content: flex-start;
  padding: 16px 20px;
  border-top: 1px solid #f1f5f9;
}
</style>
