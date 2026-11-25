<template>
  <el-drawer v-model="visible" title="切片详情" size="560px" :with-header="true">
    <div v-if="chunk" class="chunk-viewer">
      <div class="viewer-head">
        <h3 class="viewer-title">{{ chunk.heading || `切片 #${chunk.chunkIndex + 1}` }}</h3>
        <span class="status-pill" :class="embedMeta.cls">{{ embedMeta.label }}</span>
      </div>

      <el-descriptions :column="2" size="small" border class="viewer-desc">
        <el-descriptions-item label="所属文档">{{ chunk.documentName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="切片序号">#{{ chunk.chunkIndex + 1 }}</el-descriptions-item>
        <el-descriptions-item label="字符数">{{ chunk.charCount ?? chunk.content.length }}</el-descriptions-item>
        <el-descriptions-item label="Token 估算">{{ chunk.tokenEstimate ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="向量模型">{{ chunk.embeddingModel || '—' }}</el-descriptions-item>
        <el-descriptions-item label="向量维度">{{ chunk.embeddingDim || '—' }}</el-descriptions-item>
      </el-descriptions>

      <InlineAlert
        v-if="chunk.errorMsg"
        type="warning"
        title="向量化失败原因"
        :description="chunk.errorMsg"
        compact
      />

      <div class="viewer-content-label">
        <span>切片正文</span>
        <el-button text :icon="CopyDocument" @click="copyContent">复制</el-button>
      </div>
      <pre class="viewer-content">{{ chunk.content }}</pre>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import InlineAlert from '@/components/ui/InlineAlert.vue'
import type { KnowledgeChunkVO } from '@/api/system/ai-knowledge-chunk'

const visible = defineModel<boolean>('visible', { default: false })
const props = defineProps<{
  chunk: KnowledgeChunkVO | null
}>()

const embedMeta = computed(() => {
  const map: Record<string, { label: string; cls: string }> = {
    PENDING: { label: '待向量化', cls: 'is-pending' },
    INDEXED: { label: '已向量化', cls: 'is-success' },
    FAILED: { label: '向量化失败', cls: 'is-failed' },
  }
  return map[String(props.chunk?.embedStatus || 'PENDING')] || {
    label: String(props.chunk?.embedStatus || '—'),
    cls: 'is-pending',
  }
})

async function copyContent() {
  if (!props.chunk) return
  try {
    await navigator.clipboard.writeText(props.chunk.content)
    ElMessage.success('已复制切片正文')
  } catch {
    ElMessage.warning('当前浏览器不支持自动复制，请手动选择文本')
  }
}
</script>

<style scoped lang="scss">
.chunk-viewer {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.viewer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.viewer-title {
  margin: 0;
  font-size: var(--text-h3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.viewer-desc {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.viewer-content-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--text-caption);
  font-weight: 600;
  color: var(--color-text-secondary);
}

.viewer-content {
  margin: 0;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  font-family: var(--font-sans);
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 52vh;
  overflow-y: auto;
}

.status-pill {
  padding: 1px 9px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 600;

  &.is-pending {
    background: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    border: 1px solid var(--color-border);
  }

  &.is-success {
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
</style>
