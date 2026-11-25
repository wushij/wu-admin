import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadChatAttachment } from '@/api/ai'
import type { AiWuAttachment } from '@/store/aiWu'

/** 附件托盘中的草稿项（上传前/中/失败均保留，便于预览与重试） */
export interface AiWuDraftAttachment {
  key: string
  file: File
  /** 图片本地预览地址（blob URL） */
  previewUrl?: string
  /** 上传成功后的服务端元数据（含上下文块） */
  attachment?: AiWuAttachment
  uploading: boolean
  failed?: boolean
}

/** 单文件上限（最终仍以系统文件上传配置为准） */
const MAX_FILE_BYTES = 20 * 1024 * 1024
/** 单次提问最多附件数（与后端 MAX_ATTACHMENTS 对齐） */
const MAX_FILES = 5

export const AI_WU_ATTACHMENT_ACCEPT =
  'image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.markdown,.csv,.json,.xml,.yml,.yaml,.css,.js,.ts,.java,.py,.go,.sql,.c,.cpp,.h'

/**
 * AI wu 输入区附件管理：选择/粘贴/拖拽上传、失败重试、图片预览清理。
 */
export function useAiWuAttachments() {
  const items = ref<AiWuDraftAttachment[]>([])

  const hasUploading = computed(() => items.value.some((item) => item.uploading))
  const readyCount = computed(
    () => items.value.filter((item) => item.attachment && !item.uploading && !item.failed).length,
  )
  const readyAttachments = computed<AiWuAttachment[]>(() =>
    items.value
      .filter((item) => item.attachment && !item.uploading && !item.failed)
      .map((item) => item.attachment as AiWuAttachment),
  )
  const previewImages = computed(() =>
    items.value
      .filter((item) => Boolean(item.previewUrl))
      .map((item) => item.previewUrl as string),
  )

  function isImageFile(file: File): boolean {
    if (file.type && file.type.startsWith('image/')) return true
    return /\.(jpg|jpeg|png|webp|gif|bmp)$/i.test(file.name)
  }

  function formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  async function uploadItem(item: AiWuDraftAttachment) {
    item.uploading = true
    item.failed = false
    try {
      const res = await uploadChatAttachment(item.file)
      const data = res.data
      if (!data) {
        throw new Error('上传响应为空')
      }
      item.attachment = {
        ...data,
        isImage: data.isImage ?? isImageFile(item.file),
        fileName: data.fileName || item.file.name,
        fileSize: data.fileSize ?? item.file.size,
      }
      item.uploading = false
      if (!item.attachment.isImage) {
        if (item.attachment.parseStatus === 'FAILED') {
          ElMessage.warning(
            `资料「${item.file.name}」已上传，但未能解析正文：${
              item.attachment.errorMessage || '格式暂不支持'
            }`,
          )
        } else {
          ElMessage.success(`资料「${item.file.name}」解析就绪`)
        }
      }
    } catch (error) {
      item.uploading = false
      item.failed = true
      const message = error instanceof Error ? error.message : '网络异常'
      ElMessage.error(`「${item.file.name}」上传或解析失败：${message}`)
    }
  }

  async function addFiles(files: File[]) {
    const accepted: File[] = []
    for (const file of files) {
      if (items.value.length + accepted.length >= MAX_FILES) {
        ElMessage.warning(`单次提问最多添加 ${MAX_FILES} 个附件`)
        break
      }
      if (file.size > MAX_FILE_BYTES) {
        ElMessage.warning(`「${file.name}」超过 20MB 限制，无法上传`)
        continue
      }
      accepted.push(file)
    }
    if (accepted.length === 0) return

    // 使用 reactive 包裹草稿项，确保 uploadItem 异步回写 uploading/attachment 时触发视图响应式更新
    const created = accepted.map<AiWuDraftAttachment>((file) =>
      reactive<AiWuDraftAttachment>({
        key: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        file,
        previewUrl: isImageFile(file) ? URL.createObjectURL(file) : undefined,
        uploading: true,
      }),
    )
    items.value.push(...created)
    // 串行上传：避免同时解析多个大文档拖慢响应
    for (const item of created) {
      await uploadItem(item)
    }
  }

  async function retry(index: number) {
    const item = items.value[index]
    if (!item || item.uploading) return
    await uploadItem(item)
  }

  function remove(index: number) {
    const item = items.value[index]
    if (!item) return
    if (item.previewUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(item.previewUrl)
    }
    items.value.splice(index, 1)
  }

  function clear() {
    for (const item of items.value) {
      if (item.previewUrl?.startsWith('blob:')) {
        URL.revokeObjectURL(item.previewUrl)
      }
    }
    items.value = []
  }

  /** 从剪贴板提取图片（支持截图工具复制的位图） */
  function extractClipboardImages(event: ClipboardEvent): File[] {
    const clipboardData = event.clipboardData
    if (!clipboardData?.items) return []
    const images: File[] = []
    for (let i = 0; i < clipboardData.items.length; i++) {
      const item = clipboardData.items[i]
      if (!item.type || !item.type.startsWith('image/')) continue
      const file = item.getAsFile()
      if (!file) continue
      const ext = item.type.split('/')[1] || 'png'
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19)
      images.push(new File([file], `剪贴板截图_${timestamp}.${ext}`, { type: file.type }))
    }
    return images
  }

  onBeforeUnmount(clear)

  return {
    items,
    hasUploading,
    readyCount,
    readyAttachments,
    previewImages,
    addFiles,
    retry,
    remove,
    clear,
    extractClipboardImages,
    formatFileSize,
  }
}
