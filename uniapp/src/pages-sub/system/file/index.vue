<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="file-page">
    <view class="file-hero">
      <view class="file-hero__main">
        <view class="file-hero__icon">
          <IconFont name="coupon-o" :size="40" color="#fff" />
        </view>
        <view class="file-hero__text">
          <text class="file-hero__title">文件管理</text>
          <text class="file-hero__sub">点击预览 · 更多操作下载或删除</text>
        </view>
      </view>
      <view class="file-hero__stat">
        <text class="file-hero__stat-num">{{ total || list.length }}</text>
        <text class="file-hero__stat-label">文件</text>
      </view>
    </view>

    <view class="file-top">
      <SearchBar v-model="keyword" placeholder="搜索文件名" @search="onSearch" />
    </view>

    <ListLoading v-if="loading && !list.length" />

    <scroll-view
      v-else
      scroll-y
      class="file-page__scroll"
      @scrolltolower="loadMore"
    >
      <view class="file-list">
        <view
          v-for="file in list"
          :key="file.id"
          class="file-item card--elevated"
          @click="onFileTap(file)"
          @longpress="openSheet(file)"
        >
          <FileThumb :file="file" />
          <view class="file-item__body">
            <text class="file-item__name">{{ file.originalName || file.name || '未命名' }}</text>
            <text class="file-item__meta">
              {{ formatBytes(file.fileSize || file.size) }} · {{ formatListTime(file.createTime) }}
            </text>
          </view>
          <view class="file-item__more" @click.stop="openSheet(file)">
            <text class="file-item__more-icon">⋯</text>
          </view>
        </view>
      </view>

      <EmptyState v-if="empty" title="暂无文件" icon="coupon-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>

    <FabButton v-if="canUpload" @click="onUpload" />

    <FileActionSheet
      :visible="sheetVisible"
      :file-name="activeFile?.originalName || activeFile?.name"
      :show-download="!!activeFile?.id"
      :show-delete="canDelete && !!activeFile?.id"
      @close="sheetVisible = false"
      @download="onDownload"
      @delete="onDelete"
    />

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import IconFont from '@/components/common/IconFont/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import FileThumb from '@/components/business/FileThumb/index.vue'
import FileActionSheet from '@/components/business/FileActionSheet/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
import { pageFileByGroup, uploadSysFile, deleteFile, type FileRecord } from '@/api/system/file'
import { formatBytes, formatListTime } from '@/utils/format'
import { previewFile, downloadFile, isUserCancelError } from '@/utils/file-preview'
import { showConfirm } from '@/utils/app-dialog'

const { allowed, hasPerm } = useModulePermission('sys:file:list')
const canUpload = computed(() => hasPerm('sys:file:upload'))
const canDelete = computed(() => hasPerm('sys:file:delete'))
const keyword = ref('')
const total = ref(0)
const sheetVisible = ref(false)
const activeFile = ref<FileRecord | null>(null)

const { list, loading, finished, empty, refresh, loadMore } = usePageList<FileRecord>(
  async (pageNo, pageSize) => {
    const res = await pageFileByGroup({
      pageNo,
      pageSize,
      originalName: keyword.value.trim() || undefined,
    })
    total.value = res.data?.total || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

function onFileTap(file: FileRecord) {
  previewFile(file)
}

function openSheet(file: FileRecord) {
  activeFile.value = file
  sheetVisible.value = true
}

function onDownload() {
  if (activeFile.value) downloadFile(activeFile.value)
}

function onDelete() {
  if (activeFile.value) confirmDelete(activeFile.value)
}

async function confirmDelete(file: FileRecord) {
  const { confirmed } = await showConfirm({
    title: '删除文件',
    content: `确定删除「${file.originalName || file.name}」？`,
    confirmText: '删除',
    tone: 'danger',
  })
  if (!confirmed || !file.id) return
  await deleteFile(file.id)
  uni.showToast({ title: '已删除', icon: 'success' })
  await refresh()
}

async function pickLocalFile(): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.chooseFile({
      count: 1,
      extension: ['.jpg', '.jpeg', '.png', '.gif', '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.txt', '.zip', '.mp4'],
      success: (res) => {
        const path = res.tempFilePaths?.[0] || (res.tempFiles as { path?: string }[])?.[0]?.path
        if (path) resolve(path)
        else reject(new Error('no file'))
      },
      fail: (err) => reject(err),
    })
  })
}

async function onUpload() {
  try {
    const filePath = await pickLocalFile()
    uni.showLoading({ title: '上传中' })
    await uploadSysFile(filePath)
    uni.showToast({ title: '上传成功', icon: 'success' })
    await refresh()
  } catch (err) {
    if (isUserCancelError(err)) return
    uni.showToast({ title: '上传失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function onSearch() {
  refresh()
}

onMounted(refresh)

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.file-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: $page-padding-y $page-padding-x;
  background: $color-bg-page;
}

.file-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 20rpx;
  padding: 28rpx 24rpx;
  border-radius: $radius-xl;
  background: linear-gradient(135deg, #7c3aed 0%, #8b5cf6 55%, #a78bfa 100%);
  box-shadow: 0 16rpx 48rpx rgba(124, 58, 237, 0.2);
}

.file-hero__main {
  display: flex;
  align-items: center;
  gap: 20rpx;
  min-width: 0;
  flex: 1;
}

.file-hero__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80rpx;
  height: 80rpx;
  border-radius: $radius-lg;
  background: rgba(255, 255, 255, 0.18);
  flex-shrink: 0;
}

.file-hero__title {
  display: block;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: #fff;
}

.file-hero__sub {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.45;
}

.file-hero__stat {
  flex-shrink: 0;
  min-width: 88rpx;
  padding: 12rpx 16rpx;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.16);
  text-align: center;
}

.file-hero__stat-num {
  display: block;
  font-size: 32rpx;
  font-weight: $font-weight-bold;
  color: #fff;
}

.file-hero__stat-label {
  display: block;
  margin-top: 4rpx;
  font-size: 18rpx;
  color: rgba(255, 255, 255, 0.82);
}

.file-top {
  margin-bottom: 16rpx;
}

.file-page__scroll {
  height: calc(100vh - 320rpx);
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: $card-gap;
  padding-bottom: 160rpx;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx;

  &:active {
    opacity: 0.94;
  }
}

.file-item__body {
  flex: 1;
  min-width: 0;
}

.file-item__name {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-item__meta {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.file-item__more {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72rpx;
  height: 72rpx;
  border-radius: $radius-md;

  &:active {
    background: $color-bg-muted;
  }
}

.file-item__more-icon {
  font-size: 44rpx;
  font-weight: 600;
  line-height: 1;
  color: $color-text-secondary;
  letter-spacing: 0;
}
</style>
