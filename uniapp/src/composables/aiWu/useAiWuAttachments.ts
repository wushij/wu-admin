import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { uploadChatAttachment } from '@/api/ai'
import type { AiWuAttachment } from '@/store/aiWu'

/** 附件托盘中的草稿项（上传前/中/失败均保留，便于预览与重试） */
export interface AiWuDraftAttachment {
  key: string
  /** 交给 uni.uploadFile 的本地路径（H5 下可能为 File 对象） */
  uploadPath: string
  name: string
  size: number
  isImage: boolean
  /** 图片本地预览地址 */
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

const IMAGE_EXT_RE = /\.(jpg|jpeg|png|webp|gif|bmp)$/i

function isImageName(name: string): boolean {
  return IMAGE_EXT_RE.test(name)
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function toast(title: string) {
  uni.showToast({ title, icon: 'none' })
}

/**
 * AI wu 输入区附件管理（移动端）：
 * - H5：一次选择任意类型文件
 * - 微信小程序：图片走相册/拍照，文档走聊天文件
 * 上传串行执行，失败可单独重试。
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
      .filter((item) => item.isImage && Boolean(item.previewUrl))
      .map((item) => item.previewUrl as string),
  )

  async function uploadItem(item: AiWuDraftAttachment) {
    item.uploading = true
    item.failed = false
    try {
      const res = await uploadChatAttachment(item.uploadPath)
      const data = res.data
      if (!data) {
        throw new Error('上传响应为空')
      }
      item.attachment = {
        ...data,
        isImage: data.isImage ?? item.isImage,
        fileName: data.fileName || item.name,
        fileSize: data.fileSize ?? item.size,
      }
      item.uploading = false
      if (!item.attachment.isImage) {
        if (item.attachment.parseStatus === 'FAILED') {
          toast(
            `资料「${item.name}」已上传，但未能解析正文：${
              item.attachment.errorMessage || '格式暂不支持'
            }`,
          )
        } else {
          uni.showToast({ title: `资料「${item.name}」解析就绪`, icon: 'none' })
        }
      }
    } catch (error) {
      item.uploading = false
      item.failed = true
      const message = error instanceof Error ? error.message : '网络异常'
      toast(`「${item.name}」上传或解析失败：${message}`)
    }
  }

  function appendPicked(
    picked: Array<{ uploadPath: string; name: string; size: number; previewUrl?: string }>,
  ) {
    const accepted: typeof picked = []
    for (const file of picked) {
      if (items.value.length + accepted.length >= MAX_FILES) {
        toast(`单次提问最多添加 ${MAX_FILES} 个附件`)
        break
      }
      if (file.size > MAX_FILE_BYTES) {
        toast(`「${file.name}」超过 20MB 限制，无法上传`)
        continue
      }
      accepted.push(file)
    }
    if (accepted.length === 0) return

    // 使用 reactive 包裹草稿项，确保 uploadItem 异步回写 uploading/attachment 时触发视图响应式更新
    const created = accepted.map<AiWuDraftAttachment>((file) =>
      reactive<AiWuDraftAttachment>({
        key: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        uploadPath: file.uploadPath,
        name: file.name,
        size: file.size,
        isImage: isImageName(file.name),
        previewUrl: file.previewUrl,
        uploading: true,
      }),
    )
    items.value.push(...created)
    // 串行上传：避免同时解析多个大文档拖慢响应
    void (async () => {
      for (const item of created) {
        await uploadItem(item)
      }
    })()
  }

  /** 选择图片（H5 与小程序均可用，小程序直接走相册/拍照） */
  async function chooseImages() {
    const remain = MAX_FILES - items.value.length
    if (remain <= 0) {
      toast(`单次提问最多添加 ${MAX_FILES} 个附件`)
      return
    }
    try {
      const res = await uni.chooseImage({
        count: remain,
        sizeType: ['compressed', 'original'],
        sourceType: ['album', 'camera'],
      })
      const paths = (res.tempFilePaths || []) as string[]
      const files = (res.tempFiles || []) as Array<{ path?: string; size?: number }>
      const picked = paths.map((path, index) => {
        const name = path.split('/').pop() || `图片_${Date.now()}_${index}.jpg`
        return {
          uploadPath: path,
          name,
          size: files[index]?.size || 0,
          previewUrl: path,
        }
      })
      appendPicked(picked)
    } catch {
      /* 用户取消选择，忽略 */
    }
  }

  /** 选择文档（H5 任意文件；小程序从聊天记录选择） */
  async function chooseDocuments() {
    const remain = MAX_FILES - items.value.length
    if (remain <= 0) {
      toast(`单次提问最多添加 ${MAX_FILES} 个附件`)
      return
    }

    // #ifdef H5
    try {
      const res = await uni.chooseFile({ count: remain, type: 'all' })
      const tempFiles = (res.tempFiles || []) as Array<{
        path?: string
        name?: string
        size?: number
        file?: File
      }>
      const picked = tempFiles.map((file, index) => {
        const raw = file.file
        const name =
          file.name ||
          raw?.name ||
          file.path?.split('/').pop() ||
          `附件_${Date.now()}_${index}`
        const previewUrl = raw ? URL.createObjectURL(raw) : undefined
        return {
          // H5 的 uni.uploadFile 支持直接传 File 对象
          uploadPath: (raw || file.path || '') as string,
          name,
          size: file.size || raw?.size || 0,
          previewUrl: isImageName(name) ? previewUrl : undefined,
        }
      })
      appendPicked(picked)
    } catch {
      /* 用户取消选择，忽略 */
    }
    // #endif

    // #ifdef MP-WEIXIN
    try {
      const res = (await (uni as unknown as {
        chooseMessageFile: (options: {
          count: number
          type: string
          extension?: string[]
        }) => Promise<{
          tempFiles: Array<{ path: string; name: string; size: number; type?: string }>
        }>
      }).chooseMessageFile({
        count: remain,
        type: 'file',
        extension: [
          'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'md',
          'markdown', 'csv', 'json', 'xml', 'yml', 'yaml', 'css', 'js', 'ts',
          'java', 'py', 'go', 'sql', 'c', 'cpp', 'h',
        ],
      })) as { tempFiles: Array<{ path: string; name: string; size: number }> }
      const picked = (res.tempFiles || []).map((file) => ({
        uploadPath: file.path,
        name: file.name,
        size: file.size || 0,
        previewUrl: isImageName(file.name) ? file.path : undefined,
      }))
      appendPicked(picked)
    } catch {
      /* 用户取消选择，忽略 */
    }
    // #endif
  }

  async function retry(index: number) {
    const item = items.value[index]
    if (!item || item.uploading) return
    await uploadItem(item)
  }

  function remove(index: number) {
    const item = items.value[index]
    if (!item) return
    // #ifdef H5
    if (item.previewUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(item.previewUrl)
    }
    // #endif
    items.value.splice(index, 1)
  }

  function clear() {
    for (const item of items.value) {
      // #ifdef H5
      if (item.previewUrl?.startsWith('blob:')) {
        URL.revokeObjectURL(item.previewUrl)
      }
      // #endif
    }
    items.value = []
  }

  onBeforeUnmount(clear)

  return {
    items,
    hasUploading,
    readyCount,
    readyAttachments,
    previewImages,
    chooseImages,
    chooseDocuments,
    retry,
    remove,
    clear,
    formatFileSize,
  }
}
