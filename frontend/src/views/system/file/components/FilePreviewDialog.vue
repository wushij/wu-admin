<template>
  <el-dialog
    :model-value="visible"
    title="文件预览"
    :width="dialogWidth"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
    @closed="$emit('closed')"
  >
    <div class="preview-container">
      <div v-if="loading" class="preview-loading">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <span>加载中…</span>
      </div>
      <img
        v-if="isImage"
        :src="blobUrl"
        class="preview-media"
        alt="预览"
        @load="$emit('media-loaded')"
      />
      <video
        v-else-if="isVideo"
        :src="blobUrl"
        class="preview-media"
        controls
        preload="metadata"
        playsinline
        @loadeddata="$emit('media-loaded')"
        @error="$emit('media-error')"
      />
      <audio
        v-else-if="isAudio"
        :src="blobUrl"
        controls
        preload="metadata"
        style="width: 100%"
        @loadeddata="$emit('media-loaded')"
        @error="$emit('media-error')"
      />
      <iframe v-else-if="isPdf" :src="blobUrl" class="preview-iframe" />
      <div v-else-if="isText" class="preview-text-wrap">
        <pre class="preview-text">{{ text }}</pre>
      </div>
      <iframe
        v-else-if="isOffice"
        :src="officeUrl"
        class="preview-iframe"
        frameborder="0"
        allowfullscreen
      />
      <div v-else class="preview-other">
        <el-icon :size="64"><Document /></el-icon>
        <p>{{ file?.originalName }}</p>
        <p class="preview-tip">该文件类型暂不支持预览</p>
        <el-button v-if="file" type="primary" @click="$emit('download', file)">下载文件</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { Document, Loading } from '@element-plus/icons-vue'
import type { FileRecord } from '@/api/system/file'

defineProps<{
  visible: boolean
  dialogWidth: string
  file: FileRecord | null
  blobUrl: string
  text: string
  loading: boolean
  isImage: boolean
  isVideo: boolean
  isAudio: boolean
  isPdf: boolean
  isText: boolean
  isOffice: boolean
  officeUrl: string
}>()

defineEmits<{
  'update:visible': [value: boolean]
  closed: []
  'media-loaded': []
  'media-error': []
  download: [file: FileRecord]
}>()
</script>

<style scoped>
.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  width: 100%;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 200px;
  color: var(--el-text-color-secondary);
}

.preview-media {
  max-width: 100%;
  max-height: 70vh;
  display: block;
  margin: 0 auto;
}

.preview-iframe {
  width: 100%;
  height: 75vh;
  border: none;
}

.preview-text-wrap {
  width: 100%;
  max-height: 70vh;
  overflow: auto;
  background: #1e1e1e;
  border-radius: 4px;
  padding: 12px;
}

.preview-text {
  margin: 0;
  color: #d4d4d4;
  font-size: 13px;
  white-space: pre-wrap;
  font-family: Consolas, Monaco, monospace;
}

.preview-other {
  text-align: center;
  padding: 24px;
}

.preview-other p {
  margin: 8px 0;
}

.preview-tip {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
