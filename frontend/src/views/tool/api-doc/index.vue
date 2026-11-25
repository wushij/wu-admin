<template>
  <div class="api-doc-page">
    <div class="api-doc-toolbar">
      <div class="toolbar-title">
        <el-icon :size="18"><Connection /></el-icon>
        <span>接口文档</span>
        <el-tag size="small" type="info">Knife4j</el-tag>
      </div>
      <div class="toolbar-actions">
        <el-button size="small" :icon="Refresh" @click="reloadFrame">刷新</el-button>
        <el-button size="small" type="primary" :icon="TopRight" @click="openInNewTab">新窗口打开</el-button>
      </div>
    </div>
    <div class="api-doc-body" v-loading="loading" element-loading-text="文档加载中...">
      <iframe
        ref="iframeRef"
        :key="frameKey"
        :src="docUrl"
        class="api-doc-iframe"
        frameborder="0"
        allowfullscreen
        @load="loading = false"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Connection, Refresh, TopRight } from '@element-plus/icons-vue'

/**
 * Knife4j 页面引用 /webjars、/swagger-ui 等同源根路径，
 * 开发环境由 Vite 代理到 backend:8080/api；生产由 nginx 转发
 */
const docUrl = '/doc.html'

const iframeRef = ref<HTMLIFrameElement | null>(null)
defineExpose({ iframeRef })
const loading = ref(true)
const frameKey = ref(0)

function reloadFrame() {
  loading.value = true
  frameKey.value += 1
}

function openInNewTab() {
  window.open(docUrl, '_blank')
}

onMounted(() => {
  setTimeout(() => {
    loading.value = false
  }, 8000)
})
</script>

<style scoped>
.api-doc-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 540px;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.api-doc-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-bg-surface);
  flex-shrink: 0;
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.api-doc-body {
  flex: 1;
  min-height: 0;
  position: relative;
}

.api-doc-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}
</style>
