<template>
  <view class="file-thumb" :class="[`file-thumb--${theme}`, { 'file-thumb--media': showMedia }]">
    <image
      v-if="isImage(file)"
      class="file-thumb__media"
      :src="mediaUrl"
      mode="aspectFill"
    />
    <view v-else-if="isVideo(file)" class="file-thumb__media-wrap file-thumb__media-wrap--video">
      <video
        v-if="mediaUrl && !videoFailed"
        class="file-thumb__media file-thumb__media--video"
        :src="mediaUrl"
        :show-center-play-btn="false"
        :show-fullscreen-btn="false"
        :show-play-btn="false"
        :controls="false"
        :muted="true"
        :autoplay="false"
        preload="metadata"
        object-fit="cover"
        @error="videoFailed = true"
      />
      <view v-else class="file-thumb__video-bg">
        <text v-if="extLabel" class="file-thumb__ext file-thumb__ext--on-dark">{{ extLabel }}</text>
      </view>
      <view class="file-thumb__play">
        <text class="file-thumb__play-icon">▶</text>
      </view>
    </view>
    <view v-else class="file-thumb__icon">
      <IconFont :name="iconName" :size="36" :color="iconColor" />
      <text v-if="extLabel" class="file-thumb__ext">{{ extLabel }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'
import type { IconName } from '@/constants/iconfont'
import type { FileRecord } from '@/api/system/file/index'
import { getStreamPreviewUrl } from '@/api/system/file/index'
import { fileIconTheme, fileSuffix, isImage, isVideo } from '@/utils/file-type'

const props = defineProps<{
  file: FileRecord
}>()

const theme = computed(() => fileIconTheme(props.file))
const mediaUrl = computed(() => getStreamPreviewUrl(props.file))
const showMedia = computed(() => isImage(props.file) || isVideo(props.file))
const videoFailed = ref(false)

watch(
  () => props.file.id ?? props.file.url,
  () => {
    videoFailed.value = false
  },
)

const extLabel = computed(() => {
  const ext = fileSuffix(props.file).replace('.', '').toUpperCase()
  if (!ext || ext.length > 5) return ''
  return ext
})

const iconName = computed((): IconName => {
  const t = theme.value
  if (t === 'pdf' || t === 'doc') return 'notes-o'
  if (t === 'archive') return 'coupon-o'
  if (t === 'audio') return 'bell'
  return 'coupon-o'
})

const iconColor = computed(() => {
  const t = theme.value
  if (t === 'pdf') return '#ef4444'
  if (t === 'doc') return '#2563eb'
  if (t === 'archive') return '#f59e0b'
  if (t === 'audio') return '#06b6d4'
  return '#64748b'
})
</script>

<style lang="scss" scoped>

.file-thumb {
  flex-shrink: 0;
  width: 88rpx;
  height: 88rpx;
  border-radius: 20rpx;
  overflow: hidden;
  background: $color-bg-muted;
}

.file-thumb__media,
.file-thumb__media-wrap {
  width: 100%;
  height: 100%;
}

.file-thumb__media-wrap--video {
  position: relative;
}

.file-thumb__video-bg {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #334155 0%, #1e293b 100%);
}

.file-thumb__ext--on-dark {
  color: rgba(255, 255, 255, 0.82);
}

.file-thumb__media-wrap {
  position: relative;
}

.file-thumb__media--video {
  pointer-events: none;
}

.file-thumb__play {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.28);
}

.file-thumb__play-icon {
  font-size: 28rpx;
  color: #fff;
}

.file-thumb__icon {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
}

.file-thumb__ext {
  font-size: 18rpx;
  font-weight: $font-weight-bold;
  color: $color-text-secondary;
  line-height: 1;
}

.file-thumb--pdf .file-thumb__icon { background: rgba(239, 68, 68, 0.1); }
.file-thumb--doc .file-thumb__icon { background: rgba(37, 99, 235, 0.1); }
.file-thumb--archive .file-thumb__icon { background: rgba(245, 158, 11, 0.12); }
.file-thumb--audio .file-thumb__icon { background: rgba(6, 182, 212, 0.12); }
.file-thumb--default .file-thumb__icon { background: rgba(100, 116, 139, 0.1); }
</style>
