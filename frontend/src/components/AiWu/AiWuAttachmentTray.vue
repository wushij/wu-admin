<template>
  <div v-if="items.length > 0" class="aiwu-attachment-tray">
    <div
      v-for="(item, index) in items"
      :key="item.key"
      class="aiwu-attachment-item"
    >
      <!-- 图片：小方形缩略图 + 悬浮放大镜 + 右上角删除钮 -->
      <div
        v-if="item.previewUrl"
        class="aiwu-image-attachment"
        :class="{ 'is-failed': item.failed, 'is-uploading': item.uploading }"
        :title="item.uploading ? '图片上传中' : item.failed ? '上传失败，点击重试' : '点击放大预览图片'"
        @click="handleImageClick(item, index)"
      >
        <img :src="item.previewUrl" class="aiwu-thumb-img" :alt="item.file.name" />
        <div v-if="item.uploading" class="aiwu-thumb-mask is-uploading">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
        <div v-else-if="item.failed" class="aiwu-thumb-mask is-failed">
          <el-icon><Warning /></el-icon>
        </div>
        <div v-else class="aiwu-thumb-zoom">
          <el-icon><ZoomIn /></el-icon>
        </div>
        <button
          type="button"
          class="aiwu-thumb-remove"
          title="移除图片"
          aria-label="移除图片"
          @click.stop="emit('remove', index)"
        >
          ×
        </button>
      </div>

      <!-- 文档/代码：紧凑文件卡片 -->
      <div
        v-else
        class="aiwu-file-attachment"
        :class="{ 'is-failed': item.failed, 'is-uploading': item.uploading }"
        :title="item.failed ? '上传或解析失败，点击重试' : item.file.name"
        @click="handleFileClick(item, index)"
      >
        <el-icon v-if="item.uploading" class="aiwu-file-state is-loading"><Loading /></el-icon>
        <el-icon v-else-if="item.failed" class="aiwu-file-state is-danger"><Warning /></el-icon>
        <FileTypeIcon
          v-else
          :file-name="item.file.name"
          :file-type="fileSuffix(item.file.name)"
          size="xs"
        />
        <span class="aiwu-file-name">{{ item.file.name }}</span>
        <span class="aiwu-file-size">({{ formatFileSize(item.file.size) }})</span>
        <button
          type="button"
          class="aiwu-file-remove"
          title="移除该附件"
          aria-label="移除该附件"
          @click.stop="emit('remove', index)"
        >
          ×
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Loading, Warning, ZoomIn } from '@element-plus/icons-vue'
import FileTypeIcon from '@/components/ui/FileTypeIcon.vue'
import type { AiWuDraftAttachment } from '@/composables/aiWu/useAiWuAttachments'

defineProps<{
  items: AiWuDraftAttachment[]
}>()

const emit = defineEmits<{
  (e: 'remove', index: number): void
  (e: 'retry', index: number): void
  (e: 'preview', url: string): void
}>()

function handleImageClick(item: AiWuDraftAttachment, index: number) {
  if (item.uploading) return
  if (item.failed) {
    emit('retry', index)
    return
  }
  if (item.previewUrl) {
    emit('preview', item.previewUrl)
  }
}

function handleFileClick(item: AiWuDraftAttachment, index: number) {
  if (!item.uploading && item.failed) {
    emit('retry', index)
  }
}

function fileSuffix(fileName: string): string {
  const dot = fileName.lastIndexOf('.')
  return dot < 0 ? '' : fileName.slice(dot + 1).toLowerCase()
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
</script>
