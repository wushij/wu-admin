<template>
  <el-dialog
    v-model="visible"
    title="上传知识文档"
    width="620px"
    align-center
    destroy-on-close
    @closed="reset"
  >
    <div
      class="upload-drop"
      :class="{ 'is-dragover': dragover }"
      role="button"
      tabindex="0"
      @click="triggerPick"
      @keydown.enter.prevent="triggerPick"
      @dragover.prevent="dragover = true"
      @dragleave.prevent="dragover = false"
      @drop.prevent="onDrop"
    >
      <div class="upload-icon-wrap">
        <svg
          v-if="!uploading"
          class="upload-svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path class="upload-arrow-stem" d="M12 19V5" />
          <path class="upload-arrow-head" d="m5 12 7-7 7 7" />
        </svg>
        <svg
          v-else
          class="upload-loading-svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
        >
          <circle cx="12" cy="12" r="9" stroke="var(--color-border)" stroke-width="2" />
          <path d="M12 3a9 9 0 0 1 9 9" stroke="var(--el-color-primary)" stroke-width="2" />
        </svg>
      </div>
      <p class="upload-title">{{ uploading ? '正在上传文档…' : '点击选择或拖拽文档到此处' }}</p>
      <p class="upload-hint">支持 PDF、Word（.doc/.docx）、Markdown（.md）、TXT，单文件不超过 20MB</p>
      <input
        ref="inputRef"
        type="file"
        class="hidden-input"
        accept=".pdf,.doc,.docx,.md,.txt"
        multiple
        @change="onPick"
      />
    </div>

    <div v-if="files.length" class="file-list">
      <div v-for="(item, index) in files" :key="`${item.file.name}-${index}`" class="file-item">
        <FileTypeIcon :file-name="item.file.name" size="sm" />
        <div class="file-info">
          <span class="file-name" :title="item.file.name">{{ item.file.name }}</span>
          <span class="file-meta">
            {{ formatSize(item.file.size) }}
            <span v-if="item.error" class="file-error">· {{ item.error }}</span>
          </span>
        </div>
        <el-button text :icon="Close" aria-label="移除文件" @click="removeAt(index)" />
      </div>
    </div>

    <InlineAlert
      v-if="resultMessage"
      :type="resultFailed ? 'warning' : 'success'"
      :title="resultMessage"
      compact
      class="upload-result"
    />

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="uploading"
        :disabled="!validFiles.length"
        @click="handleUpload"
      >
        开始上传（{{ validFiles.length }}）
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import FileTypeIcon from '@/components/ui/FileTypeIcon.vue'
import InlineAlert from '@/components/ui/InlineAlert.vue'
import { uploadKnowledgeDocuments } from '@/api/system/ai-knowledge-document'

const visible = defineModel<boolean>('visible', { default: false })
const props = defineProps<{
  knowledgeBaseId: number
}>()
const emit = defineEmits<{
  uploaded: []
}>()

const MAX_SIZE = 20 * 1024 * 1024
const ALLOWED = ['pdf', 'doc', 'docx', 'md', 'txt']

interface PendingFile {
  file: File
  error?: string
}

const inputRef = ref<HTMLInputElement | null>(null)
const dragover = ref(false)
const files = ref<PendingFile[]>([])
const uploading = ref(false)
const resultMessage = ref('')
const resultFailed = ref(false)

const validFiles = computed(() =>
  files.value.filter((item) => !item.error).map((item) => item.file),
)

function triggerPick() {
  inputRef.value?.click()
}

function onPick(event: Event) {
  const target = event.target as HTMLInputElement
  addFiles(Array.from(target.files || []))
  target.value = ''
}

function onDrop(event: DragEvent) {
  dragover.value = false
  addFiles(Array.from(event.dataTransfer?.files || []))
}

function addFiles(incoming: File[]) {
  resultMessage.value = ''
  for (const file of incoming) {
    const extension = file.name.split('.').pop()?.toLowerCase() || ''
    let error = ''
    if (!ALLOWED.includes(extension)) {
      error = '格式不支持'
    } else if (file.size > MAX_SIZE) {
      error = '超过 20MB'
    } else if (files.value.some((item) => item.file.name === file.name && item.file.size === file.size)) {
      error = '已添加'
    }
    files.value.push({ file, error })
  }
}

function removeAt(index: number) {
  files.value.splice(index, 1)
}

async function handleUpload() {
  if (!validFiles.value.length) {
    ElMessage.warning('请先选择可上传的文档')
    return
  }
  uploading.value = true
  resultMessage.value = ''
  try {
    const res = await uploadKnowledgeDocuments(props.knowledgeBaseId, validFiles.value)
    const results = res.data || []
    const succeeded = results.filter((item) => item.success).length
    const failedItems = results.filter((item) => !item.success)
    if (succeeded > 0) {
      emit('uploaded')
    }
    if (!failedItems.length && succeeded > 0) {
      ElMessage.success(`${succeeded} 篇文档已提交解析，切片与向量化将在后台自动完成`)
      visible.value = false
      return
    }
    resultFailed.value = failedItems.length > 0
    resultMessage.value = failedItems.length
      ? `${succeeded} 篇已提交，${failedItems.length} 篇失败：${failedItems
          .map((item) => `${item.fileName}（${item.message}）`)
          .join('；')}`
      : '未提交任何文档，请检查所选文件后重试'
    const failedNames = new Set(failedItems.map((item) => item.fileName))
    // 仅保留校验异常或服务端失败的文件，方便用户修正后重试
    files.value = files.value.filter((item) => item.error || failedNames.has(item.file.name))
  } finally {
    uploading.value = false
  }
}

function reset() {
  files.value = []
  resultMessage.value = ''
  resultFailed.value = false
  uploading.value = false
}

function formatSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped lang="scss">
.upload-drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: var(--space-6) var(--space-4);
  border: 1.5px dashed var(--color-border-strong);
  border-radius: var(--radius-lg);
  background: var(--color-bg-subtle);
  cursor: pointer;
  text-align: center;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out);

  &:hover,
  &.is-dragover {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);

    .upload-icon-wrap {
      background: var(--el-color-primary-light-8);
      color: var(--el-color-primary);
      transform: translateY(-2px);

      .upload-arrow-head,
      .upload-arrow-stem {
        transform: translateY(-1.5px);
      }
    }
  }
}

.upload-icon-wrap {
  width: 48px;
  height: 48px;
  margin: 0 auto var(--space-2);
  border-radius: var(--radius-circle);
  background: var(--el-fill-color);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    background-color var(--duration-normal) var(--ease-out),
    color var(--duration-normal) var(--ease-out),
    transform var(--duration-normal) var(--ease-out);

  .upload-svg {
    width: 24px;
    height: 24px;
    display: block;

    .upload-arrow-head,
    .upload-arrow-stem {
      transition: transform var(--duration-normal) var(--ease-out);
    }
  }

  .upload-loading-svg {
    width: 24px;
    height: 24px;
    display: block;
    animation: upload-spin 0.8s linear infinite;
  }
}

@keyframes upload-spin {
  to {
    transform: rotate(360deg);
  }
}

.upload-title {
  margin: 0;
  font-size: var(--text-body);
  font-weight: 600;
  color: var(--color-text-primary);
}

.upload-hint {
  margin: 0;
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}

.hidden-input {
  display: none;
}

.file-list {
  margin-top: var(--space-3);
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 220px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.file-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-name {
  font-size: 13px;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.file-error {
  color: var(--color-danger);
}

.upload-result {
  margin-top: var(--space-3);
}
</style>
