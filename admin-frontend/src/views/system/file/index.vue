<template>
  <div class="app-container file-page">
    <div class="file-layout">
      <!-- 左侧分组 -->
      <el-card class="group-card" shadow="never">
        <template #header>
          <div class="card-header">文件分组</div>
        </template>

        <div class="type-tabs">
          <div
            v-for="tab in typeTabs"
            :key="tab.value"
            :class="['type-tab', { active: activeType === tab.value }]"
            @click="activeType = tab.value; loadFiles()"
          >
            {{ tab.label }}
          </div>
        </div>

        <div class="group-list-wrapper">
          <div class="group-list">
            <div
              :class="['group-item', { active: activeGroupId === -1 }]"
              @click="selectGroup(-1)"
            >
              <el-icon><Folder /></el-icon>
              <span class="group-name">全部</span>
            </div>
            <div
              :class="['group-item', { active: activeGroupId === null }]"
              @click="selectGroup(null)"
            >
              <el-icon><Folder /></el-icon>
              <span class="group-name">未分组</span>
              <span v-if="ungroupedCount > 0" class="group-count">{{ ungroupedCount }}</span>
            </div>
            <div
              v-for="group in groups"
              :key="group.id"
              :class="['group-item', { active: activeGroupId === group.id }]"
              @click="selectGroup(group.id)"
            >
              <el-icon><Folder /></el-icon>
              <span class="group-name">{{ group.name }}</span>
              <span v-if="group.fileCount > 0" class="group-count">{{ group.fileCount }}</span>
              <el-dropdown trigger="click" @command="(cmd) => handleGroupCmd(cmd, group)">
                <el-icon class="group-more" @click.stop><MoreFilled /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>

        <el-button class="add-group-btn" dashed block @click="openGroupDialog()">
          <el-icon><Plus /></el-icon>
          新增分组
        </el-button>
      </el-card>

      <!-- 右侧文件区 -->
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
                @click="handleBatchDelete"
              >删除</el-button>
              <el-button :disabled="!selectedIds.length" @click="moveVisible = true">移动</el-button>
            </div>
            <div class="toolbar-right">
              <el-input
                v-model="searchName"
                placeholder="请输入文件名称"
                clearable
                style="width: 200px"
                @keyup.enter="loadFiles"
              >
                <template #suffix>
                  <el-icon class="search-icon" @click="loadFiles"><Search /></el-icon>
                </template>
              </el-input>
              <el-button-group>
                <el-button :type="viewMode === 'list' ? 'primary' : 'default'" @click="viewMode = 'list'">
                  <el-icon><List /></el-icon>
                </el-button>
                <el-button :type="viewMode === 'grid' ? 'primary' : 'default'" @click="viewMode = 'grid'">
                  <el-icon><Grid /></el-icon>
                </el-button>
              </el-button-group>
            </div>
          </div>
        </template>

        <div
          class="file-manager-body"
          @dragover.prevent="handleDragOver"
          @dragleave.prevent="handleDragLeave"
          @drop.prevent="handleDrop"
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
              @change="handleSelectAll"
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

            <!-- 平铺视图 -->
            <div v-else-if="viewMode === 'grid'" class="file-grid">
              <div
                v-for="file in files"
                :key="file.id"
                :class="['file-card', { selected: selectedIds.includes(file.id) }]"
                @click="toggleSelect(file)"
              >
                <div class="file-checkbox" @click.stop>
                  <el-checkbox
                    :model-value="selectedIds.includes(file.id)"
                    @change="toggleSelect(file)"
                  />
                </div>
                <div class="file-preview" @click.stop="handlePreview(file)">
                  <img v-if="isImage(file)" :src="fileDisplayUrl(file)" alt="" />
                  <video v-else-if="isVideo(file)" :src="fileDisplayUrl(file)" muted />
                  <div v-else class="file-icon-wrap">
                    <el-icon :size="48" :color="getFileIconColor(file)">
                      <component :is="getFileIcon(file)" />
                    </el-icon>
                  </div>
                </div>
                <div class="file-name" :title="file.originalName">{{ file.originalName }}</div>
                <div class="file-actions" @click.stop>
                  <a @click="handleRename(file)">重命名</a>
                  <span>|</span>
                  <a @click="handleDownload(file)">下载</a>
                  <template v-if="isPreviewable(file)">
                    <span>|</span>
                    <a @click="handlePreview(file)">查看</a>
                  </template>
                </div>
              </div>
            </div>

            <!-- 列表视图 -->
            <div v-else class="file-list">
              <div
                v-for="file in files"
                :key="file.id"
                :class="['file-row', { selected: selectedIds.includes(file.id) }]"
                @click="toggleSelect(file)"
              >
                <div class="file-checkbox" @click.stop>
                  <el-checkbox
                    :model-value="selectedIds.includes(file.id)"
                    @change="toggleSelect(file)"
                  />
                </div>
                <div class="file-preview-small" @click.stop="handlePreview(file)">
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
                  <el-button v-if="isPreviewable(file)" type="primary" size="small" @click="handlePreview(file)">预览</el-button>
                  <el-button type="primary" size="small" @click="handleDownload(file)">下载</el-button>
                  <el-button v-permission="'sys:file:upload'" size="small" @click="handleRename(file)">重命名</el-button>
                  <el-button v-permission="'sys:file:delete'" type="danger" size="small" @click="handleDelete(file)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="pageNo"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadFiles"
            @size-change="handlePageSizeChange"
          />
        </div>
      </el-card>
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
            <el-option label="未分组" :value="null" />
            <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMove">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewVisible"
      title="文件预览"
      :width="previewDialogWidth"
      destroy-on-close
      @closed="revokePreviewUrl"
    >
      <div class="preview-container">
        <img v-if="previewIsImage" :src="previewBlobUrl" class="preview-media" alt="预览" />
        <video v-else-if="previewIsVideo" :src="previewBlobUrl" class="preview-media" controls />
        <audio v-else-if="previewIsAudio" :src="previewBlobUrl" controls style="width: 100%" />
        <iframe v-else-if="previewIsPdf" :src="previewBlobUrl" class="preview-iframe" />
        <div v-else-if="previewIsText" class="preview-text-wrap">
          <pre class="preview-text">{{ previewText }}</pre>
        </div>
        <iframe
          v-else-if="previewIsOffice"
          :src="officePreviewUrl"
          class="preview-iframe"
          frameborder="0"
          allowfullscreen
        />
        <div v-else class="preview-other">
          <el-icon :size="64"><Document /></el-icon>
          <p>{{ previewFile?.originalName }}</p>
          <p class="preview-tip">该文件类型暂不支持预览</p>
          <el-button type="primary" @click="handleDownload(previewFile)">下载文件</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Folder,
  MoreFilled,
  Plus,
  Upload,
  Search,
  List,
  Grid,
  Document,
  DocumentCopy,
  Picture,
  VideoCamera,
  Headset
} from '@element-plus/icons-vue'
import {
  getFileGroupList,
  createFileGroup,
  updateFileGroup,
  deleteFileGroup,
  pageFileByGroup,
  uploadFile,
  deleteFile,
  deleteFileBatch,
  moveFiles,
  renameFile,
  getFileText,
  fetchFileBlob,
  fileDisplayUrl,
  getDownloadApiUrl,
  validateFileBeforeUpload,
  getFileUploadPolicy,
  setUploadPolicyFromApi,
  uploadPolicy
} from '@/api/system/file'

const typeTabs = [
  { label: '图片', value: 'image' },
  { label: '视频', value: 'video' },
  { label: '文件', value: 'other' }
]

const groups = ref([])
const ungroupedCount = ref(0)
const activeType = ref('image')
const activeGroupId = ref(-1)

const viewMode = ref('grid')
const searchName = ref('')

const files = ref([])
const loading = ref(false)
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)
const selectedIds = ref([])

const groupVisible = ref(false)
const editingGroup = ref(null)
const groupName = ref('')

const renameVisible = ref(false)
const renameValue = ref('')
const renamingFile = ref(null)

const moveVisible = ref(false)
const moveGroupId = ref(null)

const previewVisible = ref(false)
const previewFile = ref(null)
const previewBlobUrl = ref('')
const previewText = ref('')

const isDragging = ref(false)
let dragCounter = 0

const uploadMaxSizeMb = computed(() => uploadPolicy.maxSizeMb)

const uploadAccept = computed(() => {
  if (activeType.value === 'image') return 'image/*'
  if (activeType.value === 'video') return 'video/*'
  return ''
})

const uploadTypeHint = computed(() => {
  if (activeType.value === 'image') return '当前筛选：图片（jpg、png、gif 等）'
  if (activeType.value === 'video') return '当前筛选：视频（mp4、mov、avi 等）'
  return '允许常见办公文档、压缩包、音视频等'
})

const isAllSelected = computed(
  () => files.value.length > 0 && selectedIds.value.length === files.value.length
)
const isIndeterminate = computed(
  () => selectedIds.value.length > 0 && selectedIds.value.length < files.value.length
)

const previewIsImage = computed(() => isImage(previewFile.value))
const previewIsVideo = computed(() => isVideo(previewFile.value))
const previewIsAudio = computed(() => isAudio(previewFile.value))
const previewIsPdf = computed(() => isPdf(previewFile.value))
const previewIsText = computed(() => isText(previewFile.value))
const previewIsOffice = computed(() => isOffice(previewFile.value))

const previewDialogWidth = computed(() => {
  if (!previewFile.value) return '800px'
  if (previewIsPdf.value || previewIsOffice.value) return '90%'
  if (previewIsText.value) return '900px'
  return '800px'
})

const officePreviewUrl = computed(() => {
  const file = previewFile.value
  if (!file?.url) return ''
  const abs = file.url.startsWith('http')
    ? file.url
    : `${window.location.origin}${file.url.startsWith('/api') ? file.url : '/api' + file.url}`
  return `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(abs)}`
})

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const s = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${s[i]}`
}

function getUploadGroupId() {
  if (typeof activeGroupId.value === 'number' && activeGroupId.value > 0) {
    return activeGroupId.value
  }
  return null
}

async function loadUploadPolicy() {
  try {
    const res = await getFileUploadPolicy()
    setUploadPolicyFromApi(res.data)
  } catch {
    /* 使用默认 50MB */
  }
}

async function loadGroups() {
  const res = await getFileGroupList()
  groups.value = res.data?.groups || []
  ungroupedCount.value = res.data?.ungroupedCount || 0
}

async function loadFiles() {
  loading.value = true
  selectedIds.value = []
  try {
    const params = {
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      fileCategory: activeType.value,
      originalName: searchName.value || undefined
    }
    if (activeGroupId.value === null) {
      params.ungrouped = true
    } else if (activeGroupId.value !== -1) {
      params.groupId = activeGroupId.value
    }
    const res = await pageFileByGroup(params)
    files.value = res.data?.list || []
    total.value = Number(res.data?.total) || 0
  } finally {
    loading.value = false
  }
}

function handlePageSizeChange() {
  pageNo.value = 1
  loadFiles()
}

function selectGroup(groupId) {
  activeGroupId.value = groupId
  pageNo.value = 1
  loadFiles()
}

function toggleSelect(file) {
  const idx = selectedIds.value.indexOf(file.id)
  if (idx === -1) {
    selectedIds.value.push(file.id)
  } else {
    selectedIds.value.splice(idx, 1)
  }
}

function handleSelectAll(checked) {
  selectedIds.value = checked ? files.value.map((f) => f.id) : []
}

function checkUploadFile(file) {
  const err = validateFileBeforeUpload(file)
  if (err) {
    ElMessage.error(err)
    return false
  }
  return true
}

async function handleUpload({ file }) {
  if (!checkUploadFile(file)) return
  try {
    await uploadFile(file, getUploadGroupId())
    ElMessage.success('上传成功')
    loadFiles()
    loadGroups()
  } catch (e) {
    console.error(e)
  }
}

function handleDragOver() {
  dragCounter++
  isDragging.value = true
}

function handleDragLeave() {
  dragCounter--
  if (dragCounter <= 0) {
    dragCounter = 0
    isDragging.value = false
  }
}

async function handleDrop(e) {
  isDragging.value = false
  dragCounter = 0
  const dropped = e.dataTransfer?.files
  if (!dropped?.length) return
  const groupId = getUploadGroupId()
  for (let i = 0; i < dropped.length; i++) {
    const f = dropped[i]
    if (!checkUploadFile(f)) continue
    try {
      await uploadFile(f, groupId)
      ElMessage.success(`${f.name} 上传成功`)
    } catch {
      ElMessage.error(`${f.name} 上传失败`)
    }
  }
  loadFiles()
  loadGroups()
}

function openGroupDialog(group) {
  editingGroup.value = group || null
  groupName.value = group?.name || ''
  groupVisible.value = true
}

async function saveGroup() {
  if (!groupName.value.trim()) {
    ElMessage.warning('请输入分组名称')
    return
  }
  if (editingGroup.value) {
    await updateFileGroup({ id: editingGroup.value.id, name: groupName.value })
    ElMessage.success('更新成功')
  } else {
    await createFileGroup({ name: groupName.value })
    ElMessage.success('创建成功')
  }
  groupVisible.value = false
  editingGroup.value = null
  groupName.value = ''
  loadGroups()
}

function handleGroupCmd(cmd, group) {
  if (cmd === 'edit') openGroupDialog(group)
  if (cmd === 'delete') {
    ElMessageBox.confirm(
      `确定要删除分组「${group.name}」吗？分组内的文件将移动到「未分组」。`,
      '提示',
      { type: 'warning' }
    )
      .then(async () => {
        await deleteFileGroup(group.id)
        ElMessage.success('删除成功')
        loadGroups()
        if (activeGroupId.value === group.id) selectGroup(-1)
      })
      .catch(() => {})
  }
}

function handleRename(file) {
  renamingFile.value = file
  renameValue.value = file.originalName
  renameVisible.value = true
}

async function saveRename() {
  if (!renameValue.value.trim()) {
    ElMessage.warning('请输入文件名')
    return
  }
  await renameFile(renamingFile.value.id, renameValue.value)
  renameVisible.value = false
  ElMessage.success('重命名成功')
  loadFiles()
}

async function handleDelete(file) {
  await ElMessageBox.confirm(`确定要删除文件「${file.originalName}」吗？`, '提示', { type: 'warning' })
  await deleteFile(file.id)
  ElMessage.success('删除成功')
  loadFiles()
  loadGroups()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个文件吗？`, '提示', {
    type: 'warning'
  })
  await deleteFileBatch(selectedIds.value)
  selectedIds.value = []
  ElMessage.success('删除成功')
  loadFiles()
  loadGroups()
}

async function saveMove() {
  await moveFiles(selectedIds.value, moveGroupId.value)
  moveVisible.value = false
  selectedIds.value = []
  ElMessage.success('移动成功')
  loadFiles()
  loadGroups()
}

function revokePreviewUrl() {
  if (previewBlobUrl.value) {
    URL.revokeObjectURL(previewBlobUrl.value)
    previewBlobUrl.value = ''
  }
}

async function handlePreview(file) {
  revokePreviewUrl()
  previewFile.value = file
  previewText.value = ''

  if (isText(file)) {
    const res = await getFileText(file.id)
    previewText.value = res.data || ''
    previewVisible.value = true
    return
  }

  if (isOffice(file)) {
    previewVisible.value = true
    return
  }

  const needPreviewApi = isPdf(file) || isVideo(file) || isAudio(file)
  if (needPreviewApi || !file.url) {
    const blob = await fetchFileBlob(`/system/file/preview/${file.id}`)
    previewBlobUrl.value = URL.createObjectURL(blob)
  } else if (file.url?.startsWith('http') || file.url?.startsWith('/')) {
    previewBlobUrl.value = fileDisplayUrl(file)
  } else {
    const blob = await fetchFileBlob(`/system/file/preview/${file.id}`)
    previewBlobUrl.value = URL.createObjectURL(blob)
  }
  previewVisible.value = true
}

function handleDownload(file) {
  const link = document.createElement('a')
  link.href = getDownloadApiUrl(file.id)
  link.download = file.originalName
  link.click()
}

function isImage(file) {
  return file?.fileType?.startsWith('image/') || false
}
function isVideo(file) {
  return file?.fileType?.startsWith('video/') || false
}
function isAudio(file) {
  return file?.fileType?.startsWith('audio/') || false
}
function isPdf(file) {
  return (
    file?.fileType === 'application/pdf' || file?.fileSuffix?.toLowerCase() === '.pdf'
  )
}
function isOffice(file) {
  const s = file?.fileSuffix?.toLowerCase() || ''
  return ['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx'].includes(s)
}
function isText(file) {
  if (!file) return false
  const textTypes = ['text/', 'application/json', 'application/xml', 'application/javascript']
  const s = file.fileSuffix?.toLowerCase() || ''
  const textSuffixes = [
    '.txt', '.md', '.json', '.xml', '.yaml', '.yml', '.ini', '.conf', '.cfg', '.properties',
    '.js', '.ts', '.vue', '.jsx', '.tsx', '.css', '.scss', '.less', '.html', '.htm', '.java',
    '.py', '.go', '.rs', '.c', '.cpp', '.h', '.hpp', '.cs', '.php', '.rb', '.swift', '.kt',
    '.sql', '.sh', '.bat', '.ps1', '.log', '.csv'
  ]
  return textTypes.some((t) => file.fileType?.startsWith(t)) || textSuffixes.includes(s)
}
function isPreviewable(file) {
  return isImage(file) || isVideo(file) || isAudio(file) || isPdf(file) || isText(file) || isOffice(file)
}

function getFileIcon(file) {
  const s = file?.fileSuffix?.toLowerCase() || ''
  if (['.doc', '.docx', '.xls', '.xlsx', '.pdf', '.txt', '.md'].includes(s)) return DocumentCopy
  if (file?.fileType?.startsWith('image/')) return Picture
  if (file?.fileType?.startsWith('video/')) return VideoCamera
  if (file?.fileType?.startsWith('audio/')) return Headset
  return Document
}

function getFileIconColor(file) {
  const s = file?.fileSuffix?.toLowerCase() || ''
  if (['.doc', '.docx'].includes(s)) return '#2b579a'
  if (['.xls', '.xlsx'].includes(s)) return '#217346'
  if (s === '.pdf') return '#f40f02'
  return '#9ca3af'
}

onMounted(() => {
  loadUploadPolicy()
  loadGroups()
  loadFiles()
})

onUnmounted(() => revokePreviewUrl())
</script>

<style scoped lang="scss">
.file-page {
  height: 100%;
}

.file-layout {
  display: flex;
  gap: 12px;
  height: calc(100vh - 140px);
  min-height: 520px;
}

.group-card {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;

  :deep(.el-card__body) {
    padding: 0 0 12px;
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: hidden;
  }
}

.card-header {
  font-size: 14px;
  font-weight: 600;
}

.type-tabs {
  display: flex;
  padding: 8px 12px;
  gap: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.type-tab {
  padding: 4px 12px;
  cursor: pointer;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  border-radius: var(--admin-radius-md, 8px);
  transition: all 0.2s;

  &:hover {
    color: var(--el-color-primary);
    background: var(--el-fill-color-light);
  }

  &.active {
    color: #fff;
    font-weight: 500;
    background: var(--el-color-primary);
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
    border-radius: var(--admin-radius-md, 8px);
  }
}

.group-list-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  gap: 8px;
  font-size: 14px;
  transition: background 0.2s;

  &:hover {
    background: var(--el-fill-color-light);
    .group-more {
      opacity: 1;
    }
  }

  &.active {
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
  }
}

.group-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.group-more {
  opacity: 0;
  transition: opacity 0.2s;
  cursor: pointer;
}

.add-group-btn {
  margin: 8px 12px 0;
  width: calc(100% - 24px);
}

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

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  width: 100%;
}

.preview-media {
  max-width: 100%;
  max-height: 70vh;
  display: block;
  margin: 0 auto;
}

.preview-iframe {
  width: 100%;
  height: 75vh;
  border: none;
}

.preview-text-wrap {
  width: 100%;
  max-height: 70vh;
  overflow: auto;
  background: #1e1e1e;
  border-radius: 4px;
  padding: 12px;
}

.preview-text {
  margin: 0;
  color: #d4d4d4;
  font-size: 13px;
  white-space: pre-wrap;
  font-family: Consolas, Monaco, monospace;
}

.preview-other {
  text-align: center;
  padding: 24px;

  p {
    margin: 8px 0;
  }

  .preview-tip {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}
</style>
