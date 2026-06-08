<template>
  <div class="app-container module-page file-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.file" />
            <span>文件管理</span>
          </div>
          <p class="module-hero-desc">按类型与分组管理上传文件，支持预览、移动与批量操作</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ fileTotalCount }}</div>
          <div class="stat-label">文件总数</div>
        </div>
      </div>
    </el-card>

    <div class="file-layout">
      <FileGroupSidebar
        :active-type="activeType"
        :active-group-id="activeGroupId"
        :groups="groups"
        :ungrouped-count="ungroupedCount"
        @switch-type="switchTypeTab"
        @select-group="selectGroup"
        @group-cmd="handleGroupCmd"
        @open-group-dialog="openGroupDialog()"
      />

      <FileMainPanel
        :files="files"
        :loading="loading"
        v-model:view-mode="viewMode"
        v-model:search-name="searchName"
        v-model:page-no="pageNo"
        v-model:page-size="pageSize"
        :selected-ids="selectedIds"
        :is-dragging="isDragging"
        :upload-accept="uploadAccept"
        :upload-type-hint="uploadTypeHint"
        :upload-max-size-mb="uploadMaxSizeMb"
        :is-all-selected="isAllSelected"
        :is-indeterminate="isIndeterminate"
        :total="total"
        :is-file-selected="isFileSelected"
        :handle-upload="handleUpload"
        @load-files="loadFiles"
        @page-size-change="handlePageSizeChange"
        @batch-delete="handleBatchDelete"
        @open-move="moveVisible = true"
        @drag-over="handleDragOver"
        @drag-leave="handleDragLeave"
        @drop="handleDrop"
        @select-all="handleSelectAll"
        @toggle-select="toggleSelect"
        @preview="handlePreview"
        @rename="handleRename"
        @download="handleDownload"
        @delete="handleDelete"
      />
    </div>

    <el-dialog v-model="groupVisible" :title="editingGroup ? '编辑分组' : '新增分组'" width="400px">
      <el-form label-width="80px">
        <el-form-item label="分组名称" required>
          <el-input v-model="groupName" placeholder="请输入分组名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGroup">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renameVisible" title="重命名" width="400px">
      <el-form label-width="80px">
        <el-form-item label="文件名">
          <el-input v-model="renameValue" placeholder="请输入新文件名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRename">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="moveVisible" title="移动到分组" width="400px">
      <el-form label-width="80px">
        <el-form-item label="目标分组">
          <el-select v-model="moveGroupId" placeholder="请选择分组" clearable style="width: 100%">
            <el-option label="未分组" :value="0" />
            <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMove">确定</el-button>
      </template>
    </el-dialog>

    <FilePreviewDialog
      v-model:visible="previewVisible"
      :dialog-width="previewDialogWidth"
      :file="previewFile"
      :blob-url="previewBlobUrl"
      :text="previewText"
      :loading="previewLoading"
      :is-image="previewIsImage"
      :is-video="previewIsVideo"
      :is-audio="previewIsAudio"
      :is-pdf="previewIsPdf"
      :is-text="previewIsText"
      :is-office="previewIsOffice"
      :office-url="officePreviewUrl"
      @closed="revokePreviewUrl"
      @media-loaded="previewLoading = false"
      @media-error="onPreviewMediaError"
      @download="handleDownload"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import FileGroupSidebar from './FileGroupSidebar.vue'
import FileMainPanel from './FileMainPanel.vue'
import FilePreviewDialog from './FilePreviewDialog.vue'
import { useFilePage } from '../composables/useFilePage'

const {
  groups,
  ungroupedCount,
  activeType,
  activeGroupId,
  viewMode,
  searchName,
  files,
  loading,
  pageNo,
  pageSize,
  total,
  selectedIds,
  groupVisible,
  editingGroup,
  groupName,
  renameVisible,
  renameValue,
  moveVisible,
  moveGroupId,
  previewVisible,
  previewFile,
  previewBlobUrl,
  previewText,
  previewLoading,
  isDragging,
  uploadMaxSizeMb,
  uploadAccept,
  uploadTypeHint,
  isAllSelected,
  isIndeterminate,
  previewIsImage,
  previewIsVideo,
  previewIsAudio,
  previewIsPdf,
  previewIsText,
  previewIsOffice,
  previewDialogWidth,
  officePreviewUrl,
  switchTypeTab,
  loadFiles,
  handlePageSizeChange,
  selectGroup,
  toggleSelect,
  isFileSelected,
  handleSelectAll,
  handleUpload,
  handleDragOver,
  handleDragLeave,
  handleDrop,
  openGroupDialog,
  saveGroup,
  handleGroupCmd,
  handleRename,
  saveRename,
  handleDelete,
  handleBatchDelete,
  saveMove,
  revokePreviewUrl,
  onPreviewMediaError,
  handlePreview,
  handleDownload,
} = useFilePage()

const fileTotalCount = computed(() => {
  const groupSum = groups.value.reduce((sum, g) => sum + (Number(g.fileCount) || 0), 0)
  return groupSum + (Number(ungroupedCount.value) || 0)
})
</script>

<style scoped lang="scss">
.file-page {
  height: 100%;
}

.file-layout {
  display: flex;
  gap: 12px;
  height: calc(100vh - 220px);
  min-height: 480px;
}
</style>
