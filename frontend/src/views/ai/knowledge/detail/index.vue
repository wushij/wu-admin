<template>
  <div class="app-container module-page knowledge-detail-page">
    <div class="detail-hero">
      <div class="hero-top">
        <el-button class="back-btn" text :icon="ArrowLeft" @click="goBack">返回知识库</el-button>
        <span class="hero-status" :class="indexStatusMeta.cls">{{ indexStatusMeta.label }}</span>
      </div>

      <div class="hero-main">
        <div class="hero-title-wrap">
          <h2 class="hero-title">{{ detail?.name || '知识库详情' }}</h2>
          <p class="hero-desc">{{ detail?.description || '暂无描述' }}</p>
        </div>
        <div class="hero-actions">
          <el-button
            type="primary"
            :icon="Upload"
            v-permission="'system:ai-knowledge:upload'"
            @click="uploadVisible = true"
          >
            上传文档
          </el-button>
          <el-button
            :icon="RefreshRight"
            :loading="reindexing"
            v-permission="'system:ai-knowledge:reindex'"
            @click="handleReindexAll"
          >
            重建索引
          </el-button>
          <el-button
            type="danger"
            plain
            :icon="Delete"
            v-permission="'system:ai-knowledge:delete'"
            @click="handleDelete"
          >
            删除
          </el-button>
        </div>
      </div>

      <div class="hero-stats">
        <div class="stat-item">
          <span class="stat-value motion-tabular">{{ detail?.documentCount ?? 0 }}</span>
          <span class="stat-label">文档</span>
        </div>
        <div class="stat-item">
          <span class="stat-value motion-tabular">{{ detail?.chunkCount ?? 0 }}</span>
          <span class="stat-label">切片</span>
        </div>
        <div class="stat-item">
          <span class="stat-value motion-tabular">{{ stats?.indexed ?? detail?.indexedChunkCount ?? 0 }}</span>
          <span class="stat-label">已向量化</span>
        </div>
        <div class="stat-item">
          <span class="stat-value motion-tabular">{{ stats?.totalTokens ?? 0 }}</span>
          <span class="stat-label">Token 估算</span>
        </div>
        <div class="stat-item is-model">
          <span class="stat-value">{{ detail?.embeddingModelName || '未配置向量模型' }}</span>
          <span class="stat-label">向量模型</span>
        </div>
      </div>
    </div>

    <el-card v-loading="loading" class="detail-tabs-card">
      <el-tabs v-model="activeTab" class="detail-tabs" @tab-change="onTabChange">
        <el-tab-pane label="文档" name="documents">
          <DocumentPanel
            :knowledge-base-id="knowledgeBaseId"
            :refresh-key="refreshTick"
            @upload="uploadVisible = true"
            @changed="handleChanged"
          />
        </el-tab-pane>
        <el-tab-pane label="切片" name="chunks">
          <ChunkPanel :knowledge-base-id="knowledgeBaseId" :refresh-key="refreshTick" />
        </el-tab-pane>
        <el-tab-pane label="检索测试" name="retrieval">
          <RetrievalPanel
            :knowledge-base-id="knowledgeBaseId"
            :embedding-ready="Boolean(detail?.embeddingModelName)"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <DocumentUploadDialog
      v-if="detail"
      v-model:visible="uploadVisible"
      :knowledge-base-id="knowledgeBaseId"
      @uploaded="handleChanged"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Delete, RefreshRight, Upload } from '@element-plus/icons-vue'
import DocumentPanel from './components/DocumentPanel.vue'
import ChunkPanel from './components/ChunkPanel.vue'
import RetrievalPanel from './components/RetrievalPanel.vue'
import DocumentUploadDialog from './components/DocumentUploadDialog.vue'
import { useKnowledgeDetail } from './composables/useKnowledgeDetail'

const route = useRoute()
const router = useRouter()

const {
  knowledgeBaseId,
  loading,
  detail,
  stats,
  reindexing,
  indexStatusMeta,
  loadDetail,
  handleReindexAll,
  handleDelete,
  goBack,
} = useKnowledgeDetail()

const uploadVisible = ref(false)
const refreshTick = ref(0)

const validTabs = ['documents', 'chunks', 'retrieval']
const activeTab = ref(
  validTabs.includes(String(route.query.tab)) ? String(route.query.tab) : 'documents',
)

function onTabChange(name: string | number) {
  router.replace({ query: { ...route.query, tab: String(name) } })
  // 切换 Tab 时重新拉取最新数据：文档/切片可能已在后台完成解析或向量化
  refreshTick.value += 1
  loadDetail()
}

/** 文档上传/删除、重建索引后刷新顶部统计与当前列表 */
function handleChanged() {
  refreshTick.value += 1
  loadDetail()
}

watch(
  () => route.query.tab,
  (tab) => {
    const next = validTabs.includes(String(tab)) ? String(tab) : 'documents'
    if (next !== activeTab.value) activeTab.value = next
  },
)

const pageTitle = computed(() => detail.value?.name || '知识库详情')
watch(pageTitle, (title) => {
  document.title = `${title} - Admin Platform`
})
</script>

<style scoped lang="scss">
.detail-hero {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-surface);
  margin-bottom: var(--space-4);
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-btn {
  padding-left: 0;
  color: var(--color-text-secondary);
}

.hero-status {
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
}

.hero-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.hero-title-wrap {
  min-width: 0;
}

.hero-title {
  margin: 0;
  font-size: var(--text-h2);
  font-weight: 700;
  color: var(--color-text-primary);
}

.hero-desc {
  margin: 4px 0 0;
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.hero-actions {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.hero-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.stat-item {
  display: inline-flex;
  flex-direction: column;
  gap: 2px;
  min-width: 88px;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);

  &.is-model {
    min-width: 160px;
  }
}

.stat-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stat-label {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.detail-tabs-card {
  min-height: 320px;
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--space-3);
}

@media (max-width: 768px) {
  .hero-main {
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
  }

  .stat-item {
    flex: 1 1 calc(50% - var(--space-2));
  }
}
</style>
