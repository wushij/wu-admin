import { uploadFile } from '@/utils/upload'

export interface FileUploadOptions {
  url: string
  filePath: string
  name?: string
  formData?: Record<string, string>
}

export function useFileUpload() {
  async function upload<T = unknown>(options: FileUploadOptions) {
    return uploadFile<T>(options)
  }

  async function pickAndUpload<T = unknown>(options: Omit<FileUploadOptions, 'filePath'>) {
    const choose = await uni.chooseImage({ count: 1, sizeType: ['compressed'] })
    const filePath = choose.tempFilePaths?.[0]
    if (!filePath) throw new Error('未选择文件')
    return upload<T>({ ...options, filePath })
  }

  return { upload, pickAndUpload }
}
