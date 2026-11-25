<template>
  <view
    class="aiwu-file-card"
    :class="{ 'is-clickable': Boolean(fileUrl) }"
    @click.stop="openFile"
  >
    <view class="aiwu-file-card__body">
      <text class="aiwu-file-card__name">{{ displayName }}</text>
      <view class="aiwu-file-card__meta">
        <text class="aiwu-file-card__size">{{ sizeLabel }}</text>
        <text v-if="fileUrl" class="aiwu-file-card__action">打开</text>
      </view>
    </view>
    <view class="aiwu-file-card__icon">
      <IconFont name="notes-o" :size="30" color="#6366f1" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import IconFont from '@/components/base/IconFont/index.vue'

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
  // #ifdef H5
  window.open(url, '_blank', 'noopener,noreferrer')
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: url,
    showToast: false,
    success: () => uni.showToast({ title: '下载链接已复制', icon: 'none' }),
  })
  // #endif
}
</script>
