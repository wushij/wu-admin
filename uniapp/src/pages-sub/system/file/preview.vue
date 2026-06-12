<template>
  <view class="media-preview">
    <view v-if="loading" class="media-preview__state">
      <text class="media-preview__hint">加载中…</text>
    </view>

    <view v-else-if="error" class="media-preview__state">
      <text class="media-preview__hint">{{ error }}</text>
      <view class="media-preview__actions">
        <button class="media-preview__btn" @click="loadMedia">重试</button>
        <button class="media-preview__btn media-preview__btn--ghost" @click="goBack">返回</button>
      </view>
    </view>

    <view v-else class="media-preview__player-wrap">
      <text v-if="mediaType === 'audio'" class="media-preview__name">{{ mediaName }}</text>
      <video
        id="previewVideo"
        class="media-preview__player"
        :class="{ 'media-preview__player--audio': mediaType === 'audio' }"
        :src="mediaSrc"
        controls
        playsinline
        webkit-playsinline
        show-center-play-btn
        enable-play-gesture
        object-fit="contain"
        @fullscreenchange="onFullscreenChange"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { onLoad, onBackPress, onUnload } from '@dcloudio/uni-app'
import { getPreviewApiUrl } from '@/api/system/file/index'
import { getToken } from '@/utils/auth'

const FILE_LIST_URL = '/pages-sub/system/file/index'
const VIDEO_ID = 'previewVideo'

const mediaType = ref<'video' | 'audio'>('video')
const mediaName = ref('媒体文件')
const mediaSrc = ref('')
const fileId = ref(0)
const loading = ref(true)
const error = ref('')
const isFullscreen = ref(false)
let blobUrl: string | null = null
let videoCtx: UniApp.VideoContext | null = null
let leaving = false

function authHeader(): Record<string, string> {
  const token = getToken()
  return token ? { Authorization: token } : {}
}

function clearBlobUrl() {
  if (blobUrl) {
    URL.revokeObjectURL(blobUrl)
    blobUrl = null
  }
}

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

async function loadMediaH5(id: number) {
  const url = getPreviewApiUrl(id)
  const res = await fetch(url, { headers: authHeader() })
  if (!res.ok) throw new Error(res.status === 404 ? '文件不存在' : '加载失败')
  const blob = await res.blob()
  clearBlobUrl()
  blobUrl = URL.createObjectURL(blob)
  mediaSrc.value = blobUrl
}

async function loadMediaNative(id: number) {
  const url = getPreviewApiUrl(id)
  const res = await new Promise<UniApp.DownloadSuccessData>((resolve, reject) => {
    uni.downloadFile({
      url,
      header: authHeader(),
      success: (r) => {
        if (r.statusCode && r.statusCode >= 400) reject(new Error('load failed'))
        else resolve(r)
      },
      fail: reject,
    })
  })
  mediaSrc.value = res.tempFilePath
}

async function loadMedia() {
  if (!fileId.value) {
    error.value = '文件信息无效'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  mediaSrc.value = ''
  clearBlobUrl()
  try {
    // #ifdef H5
    await loadMediaH5(fileId.value)
    // #endif
    // #ifndef H5
    await loadMediaNative(fileId.value)
    // #endif
  } catch {
    error.value = '加载失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
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

  // 非全屏时交给系统默认返回，避免原生 video 拦截 navigateBack
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
  clearBlobUrl()
})

onUnmounted(() => {
  stopPlayback()
  clearBlobUrl()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

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
