import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  ElMessage,
  type CheckboxValueType,
  type UploadRawFile,
  type UploadRequestOptions,
} from 'element-plus'
import {
  getFileGroupList,
  createFileGroup,
  updateFileGroup,
  deleteFileGroup,
  pageFileByGroup,
  uploadFile,
  deleteFile,
  restoreFile,
  deleteFileBatch,
  moveFiles,
  renameFile,
  getFileText,
  fetchFileBlob,
  getStreamPreviewUrl,
  getDownloadApiUrl,
  validateFileBeforeUpload,
  getFileUploadPolicy,
  setUploadPolicyFromApi,
  uploadPolicy,
  type FileGroupVO,
  type FileRecord,
  type FilePageByGroupQuery,
} from '@/api/system/file'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
import { showSnackbar } from '@/composables/ui/useSnackbar'
import { openLightbox } from '@/composables/ui/useLightbox'
import {
  formatSize,
  isImage,
  isVideo,
  isAudio,
  isPdf,
  isText,
  isOffice,
  isPreviewable,
  useStreamPreview,
} from '../utils/fileTypeHelpers'

export function useFilePage() {
  const groups = ref<FileGroupVO[]>([])
  const ungroupedCount = ref(0)
  const activeType = ref('all')
  const activeGroupId = ref<number | null>(-1)

  const viewMode = ref('grid')
  const searchName = ref('')

  const files = ref<FileRecord[]>([])
  const loading = ref(false)
  const pageNo = ref(1)
  const pageSize = ref(20)
  const total = ref(0)
  const selectedIds = ref<number[]>([])

  const groupVisible = ref(false)
  const editingGroup = ref<FileGroupVO | null>(null)
  const groupName = ref('')

  const renameVisible = ref(false)
  const renameValue = ref('')
  const renamingFile = ref<FileRecord | null>(null)

  const moveVisible = ref(false)
  const moveGroupId = ref<number | null>(null)

  const previewVisible = ref(false)
  const previewFile = ref<FileRecord | null>(null)
  const previewBlobUrl = ref('')
  const previewText = ref('')
  const previewLoading = ref(false)

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
    () => files.value.length > 0 && selectedIds.value.length === files.value.length,
  )
  const isIndeterminate = computed(
    () => selectedIds.value.length > 0 && selectedIds.value.length < files.value.length,
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
    if (!file) return ''
    const url = getStreamPreviewUrl(file)
    if (!url) return ''
    const abs = url.startsWith('http') ? url : `${window.location.origin}${url}`
    return `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(abs)}`
  })

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
    const res = await getFileGroupList(activeType.value)
    groups.value = res.data?.groups || []
    ungroupedCount.value = res.data?.ungroupedCount || 0
  }

  function switchTypeTab(type: string) {
    activeType.value = type
    pageNo.value = 1
    loadGroups()
    loadFiles()
  }

  async function loadFiles() {
    loading.value = true
    selectedIds.value = []
    try {
      const params: FilePageByGroupQuery = {
        pageNo: pageNo.value,
        pageSize: pageSize.value,
        fileCategory: activeType.value,
        originalName: searchName.value || undefined,
      }
      if (activeGroupId.value === null) {
        params.ungrouped = true
      } else if (activeGroupId.value !== -1) {
        params.groupId = activeGroupId.value
      }
      const res = await pageFileByGroup(params)
      files.value = res.data?.list || []
      total.value = Number(res.data?.total) || 0
      syncSidebarCountFromList()
    } finally {
      loading.value = false
    }
  }

  function syncSidebarCountFromList() {
    if (activeGroupId.value === null) {
      ungroupedCount.value = total.value
      return
    }
    if (typeof activeGroupId.value === 'number' && activeGroupId.value > 0) {
      const group = groups.value.find((g) => g.id === activeGroupId.value)
      if (group) group.fileCount = total.value
    }
  }

  function handlePageSizeChange() {
    pageNo.value = 1
    loadFiles()
  }

  function selectGroup(groupId: number | null) {
    activeGroupId.value = groupId
    pageNo.value = 1
    loadFiles()
  }

  function toggleSelect(file: FileRecord) {
    if (file.id == null) return
    const idx = selectedIds.value.indexOf(file.id)
    if (idx === -1) {
      selectedIds.value.push(file.id)
    } else {
      selectedIds.value.splice(idx, 1)
    }
  }

  function isFileSelected(file: FileRecord): boolean {
    return file.id != null && selectedIds.value.includes(file.id)
  }

  function handleSelectAll(val: CheckboxValueType) {
    const checked = val === true
    selectedIds.value = checked
      ? files.value.map((f) => f.id).filter((id): id is number => id != null)
      : []
  }

  function checkUploadFile(file: UploadRawFile) {
    const err = validateFileBeforeUpload(file)
    if (err) {
      ElMessage.error(err)
      return false
    }
    return true
  }

  async function handleUpload(options: UploadRequestOptions): Promise<void> {
    const file = options.file
    if (!file || !checkUploadFile(file)) return
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

  async function handleDrop(e: DragEvent) {
    isDragging.value = false
    dragCounter = 0
    const dropped = e.dataTransfer?.files
    if (!dropped?.length) return
    const groupId = getUploadGroupId()
    for (let i = 0; i < dropped.length; i++) {
      const f = dropped[i]
      if (!checkUploadFile(f as UploadRawFile)) continue
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

  function openGroupDialog(group?: FileGroupVO | null) {
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

  function handleGroupCmd(cmd: string, group: FileGroupVO) {
    if (cmd === 'edit') openGroupDialog(group)
    if (cmd === 'delete') {
      openAlertDialog({
        title: '删除文件分组',
        targetName: group.name,
        message: '分组删除后不可恢复，分组内的现有文件会自动移动至「未分组」。',
        severity: 'warning',
        confirmText: '确认删除分组',
        onConfirm: async () => {
          await deleteFileGroup(group.id)
          ElMessage.success('已删除分组')
          loadGroups()
          if (activeGroupId.value === group.id) selectGroup(-1)
        },
      })
    }
  }

  function handleRename(file: FileRecord) {
    renamingFile.value = file
    renameValue.value = file.originalName ?? ''
    renameVisible.value = true
  }

  async function saveRename() {
    if (!renameValue.value.trim()) {
      ElMessage.warning('请输入文件名')
      return
    }
    const file = renamingFile.value
    if (!file?.id) return
    await renameFile(file.id, renameValue.value)
    renameVisible.value = false
    ElMessage.success('重命名成功')
    loadFiles()
  }

  /** 单文件软删除进回收站：直接执行 + Snackbar 一键撤销 */
  async function handleDelete(file: FileRecord) {
    if (file.id == null) return
    const id = file.id
    await deleteFile(id)
    loadFiles()
    loadGroups()
    showSnackbar({
      message: `已将文件「${file.originalName}」移至回收站`,
      variant: 'info',
      actionLabel: '撤销',
      onAction: async () => {
        await restoreFile(id)
        ElMessage.success(`已恢复文件「${file.originalName}」`)
        loadFiles()
        loadGroups()
      },
    })
  }

  /** 批量删除：高影响操作走 Alert Dialog 阻断确认 */
  async function handleBatchDelete() {
    const count = selectedIds.value.length
    if (!count) return
    await openAlertDialog({
      title: '批量删除文件',
      targetName: `已选中的 ${count} 个文件`,
      message: '这些文件将全部移入回收站，业务链接依赖这些文件时将暂时无法访问。',
      severity: 'danger',
      confirmText: `确认删除 (${count})`,
      onConfirm: async () => {
        await deleteFileBatch(selectedIds.value)
        selectedIds.value = []
        ElMessage.success(`已将 ${count} 个文件移至回收站`)
        loadFiles()
        loadGroups()
      },
    })
  }

  async function saveMove() {
    const targetGroupId = moveGroupId.value && moveGroupId.value > 0 ? moveGroupId.value : null
    await moveFiles(selectedIds.value, targetGroupId)
    moveVisible.value = false
    selectedIds.value = []
    ElMessage.success('移动成功')
    loadFiles()
    loadGroups()
  }

  function revokePreviewUrl() {
    if (previewBlobUrl.value.startsWith('blob:')) {
      URL.revokeObjectURL(previewBlobUrl.value)
    }
    previewBlobUrl.value = ''
    previewLoading.value = false
  }

  function onPreviewMediaError() {
    previewLoading.value = false
    ElMessage.error('预览加载失败，请尝试下载后本地播放')
  }

  async function handlePreview(file: FileRecord) {
    // 图片类型直接接入标准 Lightbox 媒体灯箱（支持当前页画廊切换、缩放、Esc 与滚动锁定）
    if (isImage(file)) {
      const imageFiles = files.value.filter((f) => isImage(f))
      const gallery = (imageFiles.length ? imageFiles : [file]).map((img) => ({
        src: getStreamPreviewUrl(img),
        title: img.originalName || '图片预览',
      }))
      const idx = Math.max(0, imageFiles.findIndex((f) => f.id === file.id))
      openLightbox(gallery, idx)
      return
    }

    revokePreviewUrl()
    previewFile.value = file
    previewText.value = ''

    if (isText(file)) {
      if (file.id == null) return
      const res = await getFileText(file.id)
      previewText.value = res.data || ''
      previewVisible.value = true
      return
    }

    if (isOffice(file)) {
      previewVisible.value = true
      return
    }

    if (useStreamPreview(file)) {
      previewLoading.value = isVideo(file) || isAudio(file)
      previewBlobUrl.value = getStreamPreviewUrl(file)
      previewVisible.value = true
      if (isPdf(file)) {
        previewLoading.value = false
      }
      return
    }

    if (file.id == null) return
    try {
      const blob = await fetchFileBlob(`/system/file/preview/${file.id}`)
      previewBlobUrl.value = URL.createObjectURL(blob)
      previewVisible.value = true
    } catch {
      ElMessage.error('预览加载失败')
    }
  }

  function handleDownload(file: FileRecord) {
    if (file.id == null) return
    const link = document.createElement('a')
    link.href = getDownloadApiUrl(file.id)
    link.download = file.originalName ?? 'download'
    link.click()
  }

  onMounted(() => {
    loadUploadPolicy()
    loadGroups()
    loadFiles()
  })

  onUnmounted(() => revokePreviewUrl())

  return {
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
    formatSize,
    isImage,
    isVideo,
    isPreviewable,
  }
}
