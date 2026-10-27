<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="file-page">
    <ModuleDarkHero
      title="文件管理"
      subtitle="点击预览 · 长按更多操作"
      icon="coupon-o"
      theme="file-store"
      :count="total || list.length"
      count-label="文件"
    />

    <!-- 分组筛选：全部 / 未分组 / 自定义分组 -->
    <scroll-view scroll-x class="file-groups-scroll" :show-scrollbar="false">
      <view class="file-groups">
        <view
          class="file-group-chip"
          :class="{ 'file-group-chip--active': activeGroupId === -1 }"
          @click="selectGroup(-1)"
        >
          <text>全部</text>
        </view>
        <view
          class="file-group-chip"
          :class="{ 'file-group-chip--active': activeGroupId === null }"
          @click="selectGroup(null)"
        >
          <text>未分组</text>
          <text v-if="ungroupedCount > 0" class="file-group-chip__count">{{ ungroupedCount }}</text>
        </view>
        <view
          v-for="group in groups"
          :key="group.id"
          class="file-group-chip"
          :class="{ 'file-group-chip--active': activeGroupId === group.id }"
          @click="onGroupTap(group)"
          @longpress="onGroupLongPress(group)"
        >
          <text class="file-group-chip__name">{{ group.name }}</text>
          <text v-if="(group.fileCount ?? 0) > 0" class="file-group-chip__count">
            {{ group.fileCount }}
          </text>
          <text
            v-if="activeGroupId === group.id && canManageGroup"
            class="file-group-chip__more"
            @click.stop="openGroupMenu(group)"
          >
            ⋯
          </text>
        </view>
        <view v-if="canUpload" class="file-group-chip file-group-chip--add" @click="openCreateGroup">
          <text>＋ 分组</text>
        </view>
      </view>
    </scroll-view>

    <!-- 搜索与多选入口 -->
    <view class="file-toolbar">
      <view class="file-toolbar__search">
        <SearchBar v-model="keyword" placeholder="搜索文件名" @search="onSearch" />
      </view>
      <button
        v-if="(canDelete || canUpload) && list.length"
        class="file-toolbar__select"
        @click="toggleSelectMode"
      >
        {{ selectMode ? '完成' : '选择' }}
      </button>
    </view>

    <ListLoading v-if="loading && !list.length" />

    <scroll-view
      v-else
      scroll-y
      class="file-page__scroll"
      :class="{ 'file-page__scroll--selecting': selectMode }"
      @scrolltolower="loadMore"
    >
      <view class="file-list">
        <view
          v-for="file in list"
          :key="file.id"
          class="file-item card--elevated"
          :class="{ 'file-item--selected': selectMode && isSelected(file) }"
          @click="onFileTap(file)"
          @longpress="onFileLongPress(file)"
        >
          <view
            v-if="selectMode"
            class="file-check"
            :class="{ 'file-check--checked': isSelected(file) }"
          >
            <text v-if="isSelected(file)" class="file-check__tick">✓</text>
          </view>
          <FileThumb :file="file" />
          <view class="file-item__body">
            <text class="file-item__name">{{ file.originalName || file.name || '未命名' }}</text>
            <view class="file-item__meta-row">
              <text class="file-item__meta">
                {{ formatBytes(file.fileSize || file.size) }} · {{ formatListTime(file.createTime) }}
              </text>
              <text
                class="file-item__engine"
                :class="`file-item__engine--${file.storageType || 'local'}`"
              >
                {{ getStorageLabel(file.storageType) }}
              </text>
            </view>
            <view class="file-item__extra">
              <text v-if="groupNameOf(file)" class="file-item__group">
                {{ groupNameOf(file) }}
              </text>
              <text v-if="file.bucketName && file.storageType !== 'local'" class="file-item__bucket">
                空间: {{ file.bucketName }}
              </text>
            </view>
          </view>
          <view v-if="!selectMode" class="file-item__more" @click.stop="openSheet(file)">
            <text class="file-item__more-icon">⋯</text>
          </view>
        </view>
      </view>

      <EmptyState v-if="empty" title="暂无文件" icon="coupon-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>

    <FabButton v-if="canUpload && !selectMode" @click="onUpload" />

    <!-- 批量操作栏 -->
    <view v-if="selectMode" class="batch-bar">
      <text class="batch-bar__tip">已选 {{ selectedIds.length }} 项</text>
      <view class="batch-bar__actions">
        <button
          class="batch-bar__btn"
          :disabled="!selectedIds.length || !canUpload"
          @click="openBatchMove"
        >
          移动
        </button>
        <button
          class="batch-bar__btn batch-bar__btn--danger"
          :disabled="!selectedIds.length || !canDelete"
          @click="handleBatchDelete"
        >
          删除
        </button>
        <button class="batch-bar__btn batch-bar__btn--ghost" @click="exitSelectMode">取消</button>
      </view>
    </view>

    <FileActionSheet
      :visible="sheetVisible"
      :file-name="activeFile?.originalName || activeFile?.name"
      :show-download="!!activeFile?.id"
      :show-rename="canUpload && !!activeFile?.id"
      :show-move="canUpload && !!activeFile?.id"
      :show-delete="canDelete && !!activeFile?.id"
      @close="sheetVisible = false"
      @download="onDownload"
      @rename="onRename"
      @move="onMoveFile"
      @delete="onDelete"
    />

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/layout/ModuleDarkHero/index.vue'
import SearchBar from '@/components/base/SearchBar/index.vue'
import ListFooter from '@/components/base/ListFooter/index.vue'
import ListLoading from '@/components/base/ListLoading/index.vue'
import EmptyState from '@/components/base/EmptyState/index.vue'
import FabButton from '@/components/base/FabButton/index.vue'
import FileThumb from '@/components/business/file/FileThumb/index.vue'
import FileActionSheet from '@/components/business/file/FileActionSheet/index.vue'
import AppDialogHost from '@/components/base/AppDialogHost/index.vue'
import PermissionBlock from '@/components/base/PermissionBlock/index.vue'
import { usePageList } from '@/composables/base/usePageList'
import { useModulePermission } from '@/composables/base/useModulePermission'
import {
  pageFileByGroup,
  uploadSysFile,
  deleteFile,
  deleteFileBatch,
  moveFiles,
  renameFile,
  getFileGroupList,
  createFileGroup,
  updateFileGroup,
  deleteFileGroup,
  type FileRecord,
  type FileGroupVO,
} from '@/api/system/file'
import type { FilePageQuery } from '@/types/system'
import { formatBytes, formatListTime } from '@/utils/display/format'
import { previewFile, downloadFile, isUserCancelError } from '@/utils/display/file-preview'
import {
  showConfirm,
  showActionSheet,
  type ActionSheetItem,
} from '@/utils/app-dialog'

const { allowed, hasPerm } = useModulePermission('sys:file:list')
const canUpload = computed(() => hasPerm('sys:file:upload'))
const canDelete = computed(() => hasPerm('sys:file:delete'))
const canManageGroup = computed(() => canUpload.value || canDelete.value)

const keyword = ref('')
const total = ref(0)

/** 分组筛选：-1 全部、null 未分组、数字为分组 ID */
const activeGroupId = ref<number | null>(-1)
const groups = ref<FileGroupVO[]>([])
const ungroupedCount = ref(0)

/** 多选模式 */
const selectMode = ref(false)
const selectedIds = ref<number[]>([])

const sheetVisible = ref(false)
const activeFile = ref<FileRecord | null>(null)

const { list, loading, finished, empty, refresh, loadMore } = usePageList<FileRecord>(
  async (pageNo, pageSize) => {
    const params: FilePageQuery = {
      pageNo,
      pageSize,
      originalName: keyword.value.trim() || undefined,
    }
    if (activeGroupId.value === null) {
      params.ungrouped = true
    } else if (activeGroupId.value !== -1) {
      params.groupId = activeGroupId.value
    }
    const res = await pageFileByGroup(params)
    total.value = res.data?.total || 0
    syncActiveGroupCount()
    return { list: res.data?.list || [], total: total.value }
  },
)

/** 用当前列表总数同步当前分组角标 */
function syncActiveGroupCount() {
  if (activeGroupId.value === null) {
    ungroupedCount.value = total.value
    return
  }
  if (typeof activeGroupId.value === 'number' && activeGroupId.value > 0) {
    const group = groups.value.find((g) => g.id === activeGroupId.value)
    if (group) group.fileCount = total.value
  }
}

async function loadGroups() {
  try {
    const res = await getFileGroupList()
    groups.value = res.data?.groups || []
    ungroupedCount.value = res.data?.ungroupedCount || 0
  } catch {
    /* 分组加载失败不阻塞文件列表 */
  }
}

function selectGroup(groupId: number | null) {
  if (activeGroupId.value === groupId) return
  activeGroupId.value = groupId
  exitSelectMode()
  refresh()
}

let groupLongPressAt = 0

/** 点击分组标签切换筛选；长按打开管理菜单后抑制这次 click */
function onGroupTap(group: FileGroupVO) {
  if (Date.now() - groupLongPressAt < 600) return
  selectGroup(group.id)
}

function onGroupLongPress(group: FileGroupVO) {
  groupLongPressAt = Date.now()
  openGroupMenu(group)
}

/** 文件所属分组名称，未分组返回空字符串 */
function groupNameOf(file: FileRecord): string {
  if (!file.groupId) return ''
  return groups.value.find((g) => g.id === file.groupId)?.name || ''
}

function isSelected(file: FileRecord): boolean {
  return file.id != null && selectedIds.value.includes(file.id)
}

function toggleSelect(file: FileRecord) {
  if (file.id == null) return
  const index = selectedIds.value.indexOf(file.id)
  if (index === -1) {
    selectedIds.value.push(file.id)
  } else {
    selectedIds.value.splice(index, 1)
  }
}

function toggleSelectMode() {
  if (selectMode.value) {
    exitSelectMode()
    return
  }
  selectMode.value = true
  selectedIds.value = []
}

function exitSelectMode() {
  selectMode.value = false
  selectedIds.value = []
}

function onFileTap(file: FileRecord) {
  // 长按打开操作菜单后会继续触发 click，这里做一次抑制，避免误触预览
  if (Date.now() - longPressAt < 600) return
  if (selectMode.value) {
    toggleSelect(file)
    return
  }
  previewFile(file)
}

let longPressAt = 0

function onFileLongPress(file: FileRecord) {
  if (selectMode.value) return
  longPressAt = Date.now()
  openSheet(file)
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

/** 单文件重命名：与 PC 一致，提交完整文件名 */
async function onRename() {
  const file = activeFile.value
  if (!file?.id) return
  const { confirmed, content } = await showConfirm({
    title: '重命名',
    content: '',
    editable: true,
    inputValue: file.originalName || file.name || '',
    placeholderText: '请输入新文件名',
    confirmText: '保存',
  })
  if (!confirmed) return
  const newName = (content || '').trim()
  if (!newName) {
    uni.showToast({ title: '请输入文件名', icon: 'none' })
    return
  }
  await renameFile(file.id, newName)
  uni.showToast({ title: '重命名成功', icon: 'success' })
  await refresh()
}

function onMoveFile() {
  const file = activeFile.value
  if (!file?.id) return
  pickGroupAndMove([file.id])
}

function openBatchMove() {
  if (!selectedIds.value.length) return
  pickGroupAndMove([...selectedIds.value])
}

/** 选择目标分组并批量移动；未分组传 null */
async function pickGroupAndMove(fileIds: number[]) {
  const items: ActionSheetItem[] = [
    { label: '未分组' },
    ...groups.value.map((g) => ({ label: g.name })),
  ]
  let index = 0
  try {
    index = await showActionSheet({
      title: '移动到分组',
      items,
      scrollable: items.length > 8,
    })
  } catch {
    return
  }
  const targetGroupId = index === 0 ? null : (groups.value[index - 1]?.id ?? null)
  await moveFiles(fileIds, targetGroupId)
  uni.showToast({ title: '移动成功', icon: 'success' })
  exitSelectMode()
  await Promise.all([refresh(), loadGroups()])
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
  await Promise.all([refresh(), loadGroups()])
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  const { confirmed } = await showConfirm({
    title: '批量删除',
    content: `确定删除选中的 ${selectedIds.value.length} 个文件吗？`,
    confirmText: '删除',
    tone: 'danger',
  })
  if (!confirmed) return
  await deleteFileBatch(selectedIds.value)
  uni.showToast({ title: '已删除', icon: 'success' })
  exitSelectMode()
  await Promise.all([refresh(), loadGroups()])
}

/** 新增分组 */
async function openCreateGroup() {
  const { confirmed, content } = await showConfirm({
    title: '新增分组',
    content: '',
    editable: true,
    placeholderText: '请输入分组名称',
    confirmText: '创建',
  })
  if (!confirmed) return
  const name = (content || '').trim()
  if (!name) {
    uni.showToast({ title: '请输入分组名称', icon: 'none' })
    return
  }
  await createFileGroup({ name })
  uni.showToast({ title: '创建成功', icon: 'success' })
  await loadGroups()
}

/** 编辑分组 */
async function openEditGroup(group: FileGroupVO) {
  const { confirmed, content } = await showConfirm({
    title: '编辑分组',
    content: '',
    editable: true,
    inputValue: group.name,
    placeholderText: '请输入分组名称',
    confirmText: '保存',
  })
  if (!confirmed) return
  const name = (content || '').trim()
  if (!name) {
    uni.showToast({ title: '请输入分组名称', icon: 'none' })
    return
  }
  await updateFileGroup({ id: group.id, name })
  uni.showToast({ title: '更新成功', icon: 'success' })
  await loadGroups()
}

/** 删除分组，组内文件回到未分组 */
async function removeGroup(group: FileGroupVO) {
  const { confirmed } = await showConfirm({
    title: '删除分组',
    content: `确定删除分组「${group.name}」吗？分组内的文件将移动到「未分组」。`,
    confirmText: '删除',
    tone: 'danger',
  })
  if (!confirmed) return
  await deleteFileGroup(group.id)
  uni.showToast({ title: '删除成功', icon: 'success' })
  if (activeGroupId.value === group.id) {
    activeGroupId.value = -1
  }
  await Promise.all([loadGroups(), refresh()])
}

/** 长按或点击分组更多按钮：编辑/删除 */
async function openGroupMenu(group: FileGroupVO) {
  const items: ActionSheetItem[] = []
  if (canUpload.value) items.push({ label: '编辑分组' })
  if (canDelete.value) items.push({ label: '删除分组', danger: true })
  if (!items.length) return
  let index = 0
  try {
    index = await showActionSheet({ title: group.name, items })
  } catch {
    return
  }
  const action = items[index]?.label
  if (action === '编辑分组') await openEditGroup(group)
  if (action === '删除分组') await removeGroup(group)
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

/** 上传到当前选中的分组；全部/未分组时由后端落到未分组 */
async function onUpload() {
  try {
    const filePath = await pickLocalFile()
    const groupId =
      typeof activeGroupId.value === 'number' && activeGroupId.value > 0
        ? activeGroupId.value
        : undefined
    uni.showLoading({ title: '上传中' })
    await uploadSysFile(filePath, groupId)
    uni.showToast({ title: '上传成功', icon: 'success' })
    await Promise.all([refresh(), loadGroups()])
  } catch (err) {
    if (isUserCancelError(err)) return
    uni.showToast({ title: '上传失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function onSearch() {
  exitSelectMode()
  refresh()
}

function getStorageLabel(type?: string): string {
  switch (type?.toLowerCase()) {
    case 'minio':
      return 'MinIO'
    case 'cos':
      return 'COS'
    case 'oss':
      return 'OSS'
    default:
      return '本地'
  }
}

onMounted(async () => {
  await loadGroups()
  await refresh()
})

onPullDownRefresh(async () => {
  await Promise.all([loadGroups(), refresh()])
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.file-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* #ifdef H5 */
  height: calc(100vh - var(--window-top, 0px));
  /* #endif */
  box-sizing: border-box;
  overflow: hidden;
  padding: $page-padding-y $page-padding-x 0;
  background: $color-bg-page;
}

/* 分组筛选条 */
.file-groups-scroll {
  width: 100%;
  height: 88rpx;
  flex-shrink: 0;
  margin-bottom: 12rpx;
  white-space: nowrap;
}

.file-groups {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  padding: 4rpx 2rpx 8rpx;
  box-sizing: border-box;
}

.file-group-chip {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 8rpx;
  height: 64rpx;
  padding: 0 24rpx;
  border-radius: 32rpx;
  background: $color-bg-card;
  border: 1rpx solid $color-border-light;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  transition: all 0.2s ease;

  &--active {
    background: rgba(99, 102, 241, 0.12);
    border-color: rgba(99, 102, 241, 0.35);
    color: #4f46e5;
    font-weight: $font-weight-semibold;
  }

  &--add {
    background: transparent;
    border-style: dashed;
    color: $color-primary;
  }
}

.file-group-chip__name {
  max-width: 220rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-group-chip__count {
  font-size: 20rpx;
  color: $color-text-placeholder;
}

.file-group-chip__more {
  margin-left: 2rpx;
  font-size: 32rpx;
  line-height: 1;
  color: currentColor;
}

/* 搜索 + 选择 */
.file-toolbar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  flex-shrink: 0;
}

.file-toolbar__search {
  flex: 1;
  min-width: 0;

  :deep(.search-bar) {
    margin-bottom: 0;
  }
}

.file-toolbar__select {
  flex-shrink: 0;
  height: 72rpx;
  padding: 0 26rpx;
  line-height: 72rpx;
  border-radius: 36rpx;
  background: rgba(99, 102, 241, 0.1);
  color: #4f46e5;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  margin: 0;
}

.file-toolbar__select::after {
  border: none;
}

.file-page__scroll {
  flex: 1;
  min-height: 0;
  margin-top: 16rpx;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }

  &--selecting :deep(.uni-scroll-view-content) {
    padding-bottom: calc(240rpx + env(safe-area-inset-bottom));
  }
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: $card-gap;
  padding-bottom: 40rpx;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx;
  transition: border-color 0.2s ease, background 0.2s ease;

  &:active {
    opacity: 0.94;
  }

  &--selected {
    border: 1rpx solid rgba(99, 102, 241, 0.45);
    background: rgba(99, 102, 241, 0.06);
  }
}

/* 多选勾选圈 */
.file-check {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 2rpx solid $color-border;
  background: $color-bg-card;
  transition: all 0.2s ease;

  &--checked {
    border-color: #4f46e5;
    background: #4f46e5;
  }
}

.file-check__tick {
  font-size: 24rpx;
  line-height: 1;
  color: #ffffff;
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
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.file-item__meta-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 8rpx;
  flex-wrap: wrap;
}

.file-item__engine {
  flex-shrink: 0;
  padding: 2rpx 10rpx;
  border-radius: 999rpx;
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
  font-size: 18rpx;
  line-height: 1.7;
}

.file-item__engine--minio {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.file-item__engine--cos {
  background: rgba(14, 165, 233, 0.1);
  color: #0284c7;
}

.file-item__engine--oss {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}

.file-item__extra {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 6rpx;
  flex-wrap: wrap;
}

.file-item__group {
  padding: 2rpx 10rpx;
  border-radius: 999rpx;
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
  font-size: 18rpx;
  line-height: 1.7;
}

.file-item__bucket {
  font-size: 20rpx;
  color: $color-text-placeholder;
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

/* 批量操作栏 */
.batch-bar {
  position: fixed;
  left: $page-padding-x;
  right: $page-padding-x;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 18rpx 24rpx;
  border-radius: $radius-xl;
  background: rgba(15, 23, 42, 0.92);
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.24);
}

.batch-bar__tip {
  font-size: $font-size-sm;
  color: rgba(255, 255, 255, 0.86);
  white-space: nowrap;
}

.batch-bar__actions {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.batch-bar__btn {
  height: 64rpx;
  margin: 0;
  padding: 0 24rpx;
  line-height: 64rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;

  &::after {
    border: none;
  }

  &[disabled] {
    opacity: 0.4;
  }

  &--danger {
    background: rgba(239, 68, 68, 0.9);
  }

  &--ghost {
    background: transparent;
    color: rgba(255, 255, 255, 0.72);
    padding: 0 12rpx;
  }
}
</style>
