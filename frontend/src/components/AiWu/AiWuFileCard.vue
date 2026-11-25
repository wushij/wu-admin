<template>
  <div
    class="aiwu-file-card"
    :class="{ 'is-clickable': Boolean(fileUrl) }"
    :role="fileUrl ? 'button' : undefined"
    :tabindex="fileUrl ? 0 : undefined"
    :title="fileUrl ? `点击打开 ${displayName}` : displayName"
    @click.stop="openFile"
    @keydown.enter.prevent.stop="openFile"
    @keydown.space.prevent.stop="openFile"
  >
    <div class="aiwu-file-card__body">
      <div class="aiwu-file-card__name">{{ displayName }}</div>
      <div class="aiwu-file-card__meta">
        <span class="aiwu-file-card__size">{{ sizeLabel }}</span>
        <span v-if="fileUrl" class="aiwu-file-card__action">
          <el-icon><Download /></el-icon>
          <span>打开</span>
        </span>
      </div>
    </div>
    <FileTypeIcon
      :file-name="displayName"
      :file-type="fileType"
      size="sm"
      class="aiwu-file-card__icon"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Download } from '@element-plus/icons-vue'
import FileTypeIcon from '@/components/ui/FileTypeIcon.vue'

const props = withDefaults(
  defineProps<{
    fileName?: string
    fileSize?: number
    fileType?: string
    fileUrl?: string
  }>(),
  {
    fileName: '',
    fileSize: undefined,
    fileType: '',
    fileUrl: '',
  },
)

const displayName = computed(() => props.fileName?.trim() || '未命名文档')

const sizeLabel = computed(() => {
  if (typeof props.fileSize === 'number' && Number.isFinite(props.fileSize) && props.fileSize >= 0) {
    return formatFileSize(props.fileSize)
  }
  if (props.fileType) {
    return `${props.fileType.toUpperCase()} 文档`
  }
  return '文档附件'
})

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function openFile() {
  const url = props.fileUrl?.trim()
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}
</script>
