<template>
  <el-card class="file-list-card" shadow="never">
    <template #header>
      <div class="toolbar">
        <div class="toolbar-left">
          <el-upload
            v-permission="'sys:file:upload'"
            :show-file-list="false"
            :http-request="handleUpload"
            :accept="uploadAccept"
            multiple
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              上传
            </el-button>
          </el-upload>
          <el-button
            v-permission="'sys:file:delete'"
            :disabled="!selectedIds.length"
            @click="$emit('batch-delete')"
          >删除</el-button>
          <el-button :disabled="!selectedIds.length" @click="$emit('open-move')">移动</el-button>
        </div>
        <div class="toolbar-right">
          <el-input
            :model-value="searchName"
            placeholder="请输入文件名称"
            clearable
            style="width: 200px"
            @update:model-value="$emit('update:searchName', $event)"
            @keyup.enter="$emit('load-files')"
          >
            <template #suffix>
              <el-icon class="search-icon" @click="$emit('load-files')"><Search /></el-icon>
            </template>
          </el-input>
          <el-button-group>
            <el-button :type="viewMode === 'list' ? 'primary' : 'default'" @click="$emit('update:viewMode', 'list')">
              <el-icon><List /></el-icon>
            </el-button>
            <el-button :type="viewMode === 'grid' ? 'primary' : 'default'" @click="$emit('update:viewMode', 'grid')">
              <el-icon><Grid /></el-icon>
            </el-button>
          </el-button-group>
        </div>
      </div>
    </template>

    <div
      class="file-manager-body"
      @dragover.prevent="$emit('drag-over')"
      @dragleave.prevent="$emit('drag-leave')"
      @drop.prevent="$emit('drop', $event)"
    >
      <transition name="fade">
        <div v-if="isDragging" class="drag-overlay">
          <div class="drag-content">
            <el-icon :size="64" color="#fff"><Upload /></el-icon>
            <h3>松开鼠标上传文件</h3>
            <p>支持多文件同时上传</p>
          </div>
        </div>
      </transition>

      <div class="select-all-bar">
        <el-checkbox
          :model-value="isAllSelected"
          :indeterminate="isIndeterminate"
          @change="$emit('select-all', $event)"
        >
          全选
        </el-checkbox>
      </div>

      <div v-loading="loading" class="file-content-wrapper">
        <div v-if="!loading && files.length === 0" class="empty-state">
          <el-empty description="暂无数据">
            <template #description>
              <p class="upload-hint">
                <el-icon><Upload /></el-icon>
                支持拖拽上传；{{ uploadTypeHint }}
              </p>
              <p class="upload-hint sub">不支持 exe、bat 等可执行文件，单文件最大 {{ uploadMaxSizeMb }}MB</p>
            </template>
          </el-empty>
        </div>

        <div v-else-if="viewMode === 'grid'" class="file-grid">
          <div
            v-for="file in files"
            :key="file.id"
            :class="['file-card', { selected: isFileSelected(file) }]"
            @click="$emit('toggle-select', file)"
          >
            <div class="file-checkbox" @click.stop>
              <el-checkbox :model-value="isFileSelected(file)" @change="$emit('toggle-select', file)" />
            </div>
            <div class="file-preview" @click.stop="$emit('preview', file)">
              <img v-if="isImage(file)" :src="fileDisplayUrl(file)" alt="" />
              <video v-else-if="isVideo(file)" :src="fileDisplayUrl(file)" muted preload="metadata" playsinline />
              <div v-else class="file-icon-wrap">
                <el-icon :size="48" :color="getFileIconColor(file)">
                  <component :is="getFileIcon(file)" />
                </el-icon>
              </div>
            </div>
            <div class="file-name" :title="file.originalName">{{ file.originalName }}</div>
            <div class="file-actions" @click.stop>
              <a @click="$emit('rename', file)">重命名</a>
              <span>|</span>
              <a @click="$emit('download', file)">下载</a>
              <template v-if="isPreviewable(file)">
                <span>|</span>
                <a @click="$emit('preview', file)">查看</a>
              </template>
            </div>
          </div>
        </div>

        <div v-else class="file-list">
          <div
            v-for="file in files"
            :key="file.id"
            :class="['file-row', { selected: isFileSelected(file) }]"
            @click="$emit('toggle-select', file)"
          >
            <div class="file-checkbox" @click.stop>
              <el-checkbox :model-value="isFileSelected(file)" @change="$emit('toggle-select', file)" />
            </div>
            <div class="file-preview-small" @click.stop="$emit('preview', file)">
              <img v-if="isImage(file)" :src="fileDisplayUrl(file)" alt="" />
              <el-icon v-else :size="32" :color="getFileIconColor(file)">
                <component :is="getFileIcon(file)" />
              </el-icon>
            </div>
            <div class="file-info">
              <div class="file-name">{{ file.originalName }}</div>
              <div class="file-meta">
                <span>{{ formatSize(file.fileSize) }}</span>
                <span>{{ file.createTime }}</span>
              </div>
            </div>
            <div class="file-row-actions action-buttons" @click.stop>
              <el-button v-if="isPreviewable(file)" type="primary" size="small" @click="$emit('preview', file)">预览</el-button>
              <el-button type="primary" size="small" @click="$emit('download', file)">下载</el-button>
              <el-button v-permission="'sys:file:upload'" size="small" @click="$emit('rename', file)">重命名</el-button>
              <el-button v-permission="'sys:file:delete'" type="danger" size="small" @click="$emit('delete', file)">删除</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-bar">
      <el-pagination
        :current-page="pageNo"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @update:current-page="$emit('update:pageNo', $event)"
        @update:page-size="$emit('update:pageSize', $event)"
        @current-change="$emit('load-files')"
        @size-change="$emit('page-size-change')"
      />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { CheckboxValueType, UploadRequestOptions } from 'element-plus'
import { Upload, Search, List, Grid } from '@element-plus/icons-vue'
import { fileDisplayUrl, type FileRecord } from '@/api/system/file'
import {
  formatSize,
  isImage,
  isVideo,
  isPreviewable,
  getFileIcon,
  getFileIconColor,
} from '../utils/fileTypeHelpers'

defineProps<{
  files: FileRecord[]
  loading: boolean
  viewMode: string
  searchName: string
  selectedIds: number[]
  isDragging: boolean
  uploadAccept: string
  uploadTypeHint: string
  uploadMaxSizeMb: number
  isAllSelected: boolean
  isIndeterminate: boolean
  pageNo: number
  pageSize: number
  total: number
  isFileSelected: (file: FileRecord) => boolean
  handleUpload: (options: UploadRequestOptions) => Promise<void>
}>()

defineEmits<{
  'update:searchName': [value: string]
  'update:viewMode': [mode: string]
  'update:pageNo': [page: number]
  'update:pageSize': [size: number]
  'load-files': []
  'page-size-change': []
  'batch-delete': []
  'open-move': []
  'drag-over': []
  'drag-leave': []
  drop: [event: DragEvent]
  'select-all': [val: CheckboxValueType]
  'toggle-select': [file: FileRecord]
  preview: [file: FileRecord]
  rename: [file: FileRecord]
  download: [file: FileRecord]
  delete: [file: FileRecord]
}>()
</script>

<style scoped lang="scss">
.file-list-card {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;

  :deep(.el-card__body) {
    padding: 0;
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: hidden;
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-icon {
  cursor: pointer;
}

.file-manager-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  min-height: 0;
}

.select-all-bar {
  padding: 8px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.file-content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-height: 200px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
}

.upload-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-top: 8px;

  &.sub {
    font-size: 12px;
    margin-top: 4px;
    opacity: 0.85;
  }
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.file-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--admin-radius-lg, 12px);
  padding: 12px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;

  &:hover {
    border-color: var(--el-color-primary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  &.selected {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}

.file-card .file-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 1;
}

.file-preview {
  width: 100%;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  border-radius: var(--admin-radius-md, 8px);
  overflow: hidden;
  margin-bottom: 8px;

  img,
  video {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
  }
}

.file-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.file-card .file-name {
  font-size: 13px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-card .file-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 12px;

  a {
    color: var(--el-color-primary);
    cursor: pointer;
  }

  span {
    color: var(--el-text-color-placeholder);
  }
}

.file-list {
  display: flex;
  flex-direction: column;
}

.file-row {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  gap: 12px;
  transition: background 0.2s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.selected {
    background: var(--el-color-primary-light-9);
  }
}

.file-preview-small {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  border-radius: var(--admin-radius-md, 8px);
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-row .file-name {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.file-row-actions {
  flex-shrink: 0;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-bar {
  padding: 12px 16px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--el-border-color-lighter);
}

.drag-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
}

.drag-content {
  text-align: center;
  color: #fff;
  padding: 40px;
  border: 2px dashed rgba(255, 255, 255, 0.35);
  border-radius: 16px;

  h3 {
    margin: 12px 0 4px;
    font-size: 18px;
  }

  p {
    margin: 0;
    font-size: 14px;
    opacity: 0.85;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
