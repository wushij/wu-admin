<template>
  <scroll-view v-if="items.length" scroll-x class="aiwu-attachment-tray">
    <view class="aiwu-attachment-tray__inner">
      <view
        v-for="(item, index) in items"
        :key="item.key"
        class="aiwu-draft-card"
        :class="{ 'is-failed': item.failed }"
      >
        <view
          v-if="item.isImage && item.previewUrl"
          class="aiwu-draft-thumb"
          @click.stop="emit('preview', item.previewUrl)"
        >
          <image :src="item.previewUrl" mode="aspectFill" class="aiwu-draft-thumb__img" />
        </view>
        <view v-else class="aiwu-draft-thumb is-doc">
          <IconFont name="notes-o" :size="34" color="#6366f1" />
        </view>

        <view class="aiwu-draft-info">
          <text class="aiwu-draft-name">{{ item.name }}</text>
          <text class="aiwu-draft-meta">
            {{ item.failed ? '上传失败' : item.uploading ? '解析中…' : formatFileSize(item.size) }}
          </text>
        </view>

        <view class="aiwu-draft-actions">
          <view
            v-if="item.failed && !item.uploading"
            class="aiwu-draft-btn is-retry"
            @click.stop="emit('retry', index)"
          >
            <text>重试</text>
          </view>
          <view class="aiwu-draft-btn is-remove" @click.stop="emit('remove', index)">
            <text>移除</text>
          </view>
        </view>
      </view>
    </view>
  </scroll-view>
</template>

<script setup lang="ts">
import IconFont from '@/components/base/IconFont/index.vue'
import type { AiWuDraftAttachment } from '@/composables/aiWu/useAiWuAttachments'

defineProps<{
  items: AiWuDraftAttachment[]
  formatFileSize: (bytes: number) => string
}>()

const emit = defineEmits<{
  (e: 'remove', index: number): void
  (e: 'retry', index: number): void
  (e: 'preview', url: string): void
}>()
</script>
