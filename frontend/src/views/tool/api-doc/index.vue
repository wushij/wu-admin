<template>
  <div class="api-doc-page">
    <div class="api-doc-toolbar">
      <div class="toolbar-title">
        <el-icon :size="18"><Document /></el-icon>
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

<script setup>
import { ref, onMounted } from 'vue'
import { Document, Refresh, TopRight } from '@element-plus/icons-vue'

/**
 * Knife4j 页面引用 /webjars、/swagger-ui 等同源根路径，
 * 开发环境由 Vite 代理到 backend:8081/api；生产由 nginx 转发
 */
const docUrl = '/doc.html'

const iframeRef = ref(null)
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
  height: calc(100vh - 120px);
  min-height: 520px;
  background: var(--el-bg-color);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.api-doc-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  flex-shrink: 0;
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
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
