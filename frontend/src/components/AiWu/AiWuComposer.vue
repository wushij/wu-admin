<template>
  <footer class="aiwu-footer">
    <div
      class="aiwu-composer"
      :class="{
        'is-expanded': isMultiline || draftItems.length > 0,
        'is-dragging': isDragging,
      }"
      @dragover.prevent="handleDragOver"
      @dragleave="handleDragLeave"
      @drop.prevent="handleDrop"
    >
      <input
        ref="fileInputRef"
        class="aiwu-file-input"
        type="file"
        multiple
        :accept="AI_WU_ATTACHMENT_ACCEPT"
        @change="handleFilesSelected"
      />

      <AiWuAttachmentTray
        :items="draftItems"
        @remove="removeAttachment"
        @retry="retryAttachment"
        @preview="openImagePreview"
      />

      <div class="aiwu-composer-main" :class="{ 'is-multi': isMultiline }">
        <textarea
          ref="textareaRef"
          class="aiwu-textarea"
          :class="{ 'is-multi': isMultiline }"
          :value="modelValue"
          :rows="isMultiline ? 2 : 1"
          :placeholder="placeholder"
          :disabled="disabled"
          maxlength="4000"
          @input="handleInput"
          @keydown="handleKeydown"
          @paste="handlePaste"
        />

        <!-- 单行模式：发送/停止位于输入框右侧 -->
        <div v-if="!isMultiline" class="aiwu-composer-actions">
          <el-tooltip v-if="streaming" content="停止生成" placement="top" :show-after="1000">
            <button class="aiwu-stop-btn" type="button" aria-label="停止生成" @click="emit('stop')">
              <el-icon :size="14"><VideoPause /></el-icon>
              <span>停止</span>
            </button>
          </el-tooltip>
          <el-tooltip
            v-else
            :content="sendTooltip"
            placement="top"
            :show-after="1000"
          >
            <button
              class="aiwu-send-btn"
              type="button"
              :disabled="!canSend"
              aria-label="发送提问"
              @click="submit"
            >
              <el-icon :size="15"><Promotion /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </div>

      <!-- 底部工具栏：上传 / 联网 / 快捷键提示 / 多行模式发送 -->
      <div class="aiwu-composer-toolbar">
        <div class="aiwu-toolbar-left">
          <el-tooltip
            content="上传资料或图片（支持图片、PDF、Word、Excel、PPT、Markdown 与代码）"
            placement="top"
            :show-after="1000"
          >
            <button
              class="aiwu-tool-btn"
              type="button"
              :disabled="disabled || streaming"
              @click="triggerUpload"
            >
              <el-icon :size="14"><Paperclip /></el-icon>
              <span>上传文件</span>
            </button>
          </el-tooltip>

          <el-tooltip :content="webSearchTooltip" placement="top" :show-after="1000">
            <button
              class="aiwu-tool-btn"
              :class="{
                'is-active': webSearchAvailable && webSearchEnabled,
                'is-unavailable': !webSearchAvailable,
              }"
              type="button"
              :disabled="disabled || streaming"
              :aria-pressed="webSearchAvailable && webSearchEnabled"
              @click="emit('toggle-web-search')"
            >
              <el-icon :size="14"><Compass /></el-icon>
              <span>联网</span>
            </button>
          </el-tooltip>
        </div>

        <div class="aiwu-toolbar-right">
          <span class="aiwu-shortcut-hint">Enter 发送 · Shift+Enter 换行</span>

          <el-tooltip v-if="modelValue.trim() && !streaming" content="清空输入" placement="top" :show-after="1000">
            <button
              class="aiwu-composer-clear"
              type="button"
              aria-label="清空输入"
              @click="emit('update:modelValue', '')"
            >
              <el-icon :size="14"><CircleClose /></el-icon>
            </button>
          </el-tooltip>

          <template v-if="isMultiline">
            <el-tooltip v-if="streaming" content="停止生成" placement="top" :show-after="1000">
              <button class="aiwu-stop-btn is-pill" type="button" aria-label="停止生成" @click="emit('stop')">
                <el-icon :size="14"><VideoPause /></el-icon>
                <span>停止</span>
              </button>
            </el-tooltip>
            <el-tooltip v-else :content="sendTooltip" placement="top" :show-after="1000">
              <button
                class="aiwu-send-btn is-pill"
                type="button"
                :disabled="!canSend"
                aria-label="发送提问"
                @click="submit"
              >
                <el-icon :size="15"><Promotion /></el-icon>
                <span>发送</span>
              </button>
            </el-tooltip>
          </template>
        </div>
      </div>

      <ElImageViewer
        v-if="previewVisible"
        :url-list="previewImages"
        :initial-index="previewIndex"
        teleported
        hide-on-click-modal
        @close="previewVisible = false"
      />
    </div>

    <p class="aiwu-disclaimer">内容由 AI 生成，仅作操作指引参考，请以系统实际页面为准</p>
  </footer>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElImageViewer } from 'element-plus'
import {
  CircleClose,
  Compass,
  Paperclip,
  Promotion,
  VideoPause,
} from '@element-plus/icons-vue'
import type { AiWuAttachment } from '@/store/aiWu'
import {
  AI_WU_ATTACHMENT_ACCEPT,
  useAiWuAttachments,
} from '@/composables/aiWu/useAiWuAttachments'
import AiWuAttachmentTray from './AiWuAttachmentTray.vue'

const props = withDefaults(
  defineProps<{
    modelValue: string
    streaming?: boolean
    disabled?: boolean
    placeholder?: string
    webSearchAvailable?: boolean
    webSearchEnabled?: boolean
    webSearchEngineSummary?: string
  }>(),
  {
    streaming: false,
    disabled: false,
    placeholder: '请输入你的问题，Enter 发送，Shift+Enter 换行',
    webSearchAvailable: false,
    webSearchEnabled: false,
    webSearchEngineSummary: '',
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'send', payload: { text: string; attachments: AiWuAttachment[] }): void
  (e: 'stop'): void
  (e: 'toggle-web-search'): void
}>()

const {
  items: draftItems,
  hasUploading,
  readyCount,
  readyAttachments,
  previewImages,
  addFiles,
  retry,
  remove,
  clear,
  extractClipboardImages,
} = useAiWuAttachments()

const textareaRef = ref<HTMLTextAreaElement>()
const fileInputRef = ref<HTMLInputElement>()
const isDragging = ref(false)
const previewVisible = ref(false)
const previewIndex = ref(0)

const isMultiline = computed(
  () => props.modelValue.includes('\n') || props.modelValue.length > 36,
)

const canSend = computed(
  () =>
    Boolean(props.modelValue.trim() || readyCount.value > 0) &&
    !props.streaming &&
    !props.disabled &&
    !hasUploading.value,
)

const sendTooltip = computed(() => {
  if (hasUploading.value) return '附件正在解析中，请稍候'
  if (!props.modelValue.trim() && readyCount.value === 0) return '请输入问题或添加附件后发送'
  return '发送提问 (Enter)'
})

const webSearchTooltip = computed(() => {
  if (!props.webSearchAvailable) {
    return '联网检索未配置（请在系统配置 → 第三方配置中启用）'
  }
  const engine = props.webSearchEngineSummary ? `：${props.webSearchEngineSummary}` : ''
  return props.webSearchEnabled ? `已开启实时联网检索${engine}` : `点击开启实时联网检索${engine}`
})

function adjustHeight() {
  nextTick(() => {
    const el = textareaRef.value
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${Math.min(Math.max(el.scrollHeight, 32), 160)}px`
  })
}

function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
  adjustHeight()
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter') return
  if (event.isComposing) return
  if (event.shiftKey) return
  event.preventDefault()
  submit()
}

function triggerUpload() {
  if (props.disabled || props.streaming) return
  fileInputRef.value?.click()
}

async function handleFilesSelected(event: Event) {
  const target = event.target as HTMLInputElement
  if (!target.files || target.files.length === 0) return
  await addFiles(Array.from(target.files))
  target.value = ''
}

async function handlePaste(event: ClipboardEvent) {
  const images = extractClipboardImages(event)
  if (images.length === 0) return
  event.preventDefault()
  await addFiles(images)
}

function handleDragOver() {
  if (props.disabled || props.streaming) return
  isDragging.value = true
}

function handleDragLeave() {
  isDragging.value = false
}

async function handleDrop(event: DragEvent) {
  isDragging.value = false
  if (props.disabled || props.streaming) return
  const files = event.dataTransfer?.files
  if (!files || files.length === 0) return
  await addFiles(Array.from(files))
}

function removeAttachment(index: number) {
  remove(index)
}

async function retryAttachment(index: number) {
  await retry(index)
}

function openImagePreview(url: string) {
  const index = previewImages.value.indexOf(url)
  previewIndex.value = index >= 0 ? index : 0
  previewVisible.value = true
}

function submit() {
  if (!canSend.value) return
  const text = props.modelValue.trim()
  const attachments = readyAttachments.value
  emit('send', { text, attachments })
  emit('update:modelValue', '')
  clear()
  nextTick(adjustHeight)
}

watch(
  () => props.modelValue,
  () => adjustHeight(),
)

onMounted(adjustHeight)

defineExpose({ focus: () => textareaRef.value?.focus({ preventScroll: true }) })
</script>
