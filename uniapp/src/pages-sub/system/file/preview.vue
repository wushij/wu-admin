<template>
  <view class="media-preview">
    <view v-if="error" class="media-preview__state">
      <text class="media-preview__hint">{{ error }}</text>
      <view class="media-preview__actions">
        <button class="media-preview__btn" @click="loadMedia">重试</button>
        <button class="media-preview__btn media-preview__btn--ghost" @click="goBack">返回</button>
      </view>
    </view>

    <view v-else class="media-preview__player-wrap">
      <text v-if="mediaType === 'audio'" class="media-preview__name">{{ mediaName }}</text>
      <view v-if="buffering" class="media-preview__buffer">
        <text class="media-preview__hint">缓冲中…</text>
      </view>
      <video
        v-if="mediaSrc"
        id="previewVideo"
        class="media-preview__player"
        :class="{ 'media-preview__player--audio': mediaType === 'audio' }"
        :src="mediaSrc"
        controls
        playsinline
        webkit-playsinline
        show-center-play-btn
        enable-play-gesture
        preload="metadata"
        object-fit="contain"
        @loadedmetadata="onMediaReady"
        @canplay="onMediaReady"
        @waiting="buffering = true"
        @playing="buffering = false"
        @error="onMediaError"
        @fullscreenchange="onFullscreenChange"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { onLoad, onBackPress, onUnload } from '@dcloudio/uni-app'
import { getPreviewApiUrl } from '@/api/system/file/index'

const FILE_LIST_URL = '/pages-sub/system/file/index'
const VIDEO_ID = 'previewVideo'

const mediaType = ref<'video' | 'audio'>('video')
const mediaName = ref('媒体文件')
const mediaSrc = ref('')
const fileId = ref(0)
const buffering = ref(true)
const error = ref('')
const isFullscreen = ref(false)
let videoCtx: UniApp.VideoContext | null = null
let leaving = false

function ensureVideoCtx() {
  if (!videoCtx) {
    videoCtx = uni.createVideoContext(VIDEO_ID)
  }
  return videoCtx
}

function stopPlayback() {
  try {
    const ctx = ensureVideoCtx()
    ctx?.pause()
    if (isFullscreen.value) {
      ctx?.exitFullScreen()
    }
  } catch {
    /* ignore */
  }
}

function onFullscreenChange(e: UniHelper.VideoOnFullscreenchangeEvent) {
  isFullscreen.value = !!e.detail?.fullScreen
}

function onMediaReady() {
  buffering.value = false
  error.value = ''
}

function onMediaError() {
  error.value = '加载失败，请检查网络后重试'
  buffering.value = false
  mediaSrc.value = ''
}

/** 直连预览 URL，依赖 HTTP Range 分段加载，避免整文件下载 */
function loadMedia() {
  if (!fileId.value) {
    error.value = '文件信息无效'
    buffering.value = false
    return
  }
  error.value = ''
  buffering.value = true
  mediaSrc.value = getPreviewApiUrl(fileId.value)
}

function goBack() {
  if (leaving) return
  leaving = true

  if (isFullscreen.value) {
    leaving = false
    ensureVideoCtx()?.exitFullScreen()
    return
  }

  stopPlayback()

  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack({
      fail: () => {
        leaving = false
        uni.redirectTo({ url: FILE_LIST_URL })
      },
    })
    return
  }

  uni.redirectTo({
    url: FILE_LIST_URL,
    fail: () => {
      leaving = false
      uni.reLaunch({ url: FILE_LIST_URL })
    },
  })
}

onBackPress(() => {
  if (isFullscreen.value) {
    ensureVideoCtx()?.exitFullScreen()
    return true
  }

  stopPlayback()

  const pages = getCurrentPages()
  if (pages.length <= 1) {
    goBack()
    return true
  }

  return false
})

onLoad((options) => {
  mediaType.value = options?.type === 'audio' ? 'audio' : 'video'
  fileId.value = Number(options?.id) || 0
  mediaName.value = options?.name ? decodeURIComponent(options.name) : '媒体文件'
  uni.setNavigationBarTitle({ title: mediaName.value })
  loadMedia()
})

onUnload(() => {
  stopPlayback()
})

onUnmounted(() => {
  stopPlayback()
})
</script>

<style lang="scss" scoped>
.media-preview {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #111;
  padding: 24rpx;
  box-sizing: border-box;
}

.media-preview__state {
  width: 100%;
  padding: 48rpx 32rpx;
  text-align: center;
}

.media-preview__hint {
  display: block;
  font-size: $font-size-base;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.6;
}

.media-preview__buffer {
  margin-bottom: 16rpx;
  text-align: center;
}

.media-preview__actions {
  display: flex;
  justify-content: center;
  gap: 24rpx;
  margin-top: 40rpx;
}

.media-preview__btn {
  min-width: 200rpx;
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 32rpx;
  border-radius: $radius-md;
  background: $color-primary;
  color: #fff;
  font-size: $font-size-base;

  &--ghost {
    background: rgba(255, 255, 255, 0.12);
    color: #fff;
  }
}

.media-preview__player-wrap {
  width: 100%;
}

.media-preview__name {
  display: block;
  margin-bottom: 24rpx;
  text-align: center;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: rgba(255, 255, 255, 0.88);
  word-break: break-all;
}

.media-preview__player {
  width: 100%;
  max-height: 80vh;
}

.media-preview__player--audio {
  max-height: 120rpx;
}
</style>
