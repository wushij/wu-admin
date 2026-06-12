<template>
  <view class="file-uploader" @click="pick">
    <slot>
      <view class="file-uploader__default">
        <text class="file-uploader__plus">+</text>
        <text class="file-uploader__text">{{ label }}</text>
      </view>
    </slot>
  </view>
</template>

<script setup lang="ts">
import { useFileUpload } from '@/composables/useFileUpload'

const props = withDefaults(
  defineProps<{
    url: string
    label?: string
    accept?: 'image' | 'file'
  }>(),
  { label: '上传文件', accept: 'image' },
)

const emit = defineEmits<{
  success: [data: unknown]
  error: [err: unknown]
}>()

const { upload } = useFileUpload()

async function pick() {
  try {
    let filePath = ''
    if (props.accept === 'image') {
      const choose = await uni.chooseImage({ count: 1, sizeType: ['compressed'] })
      filePath = choose.tempFilePaths?.[0] || ''
    } else {
      const choose = await new Promise<UniApp.ChooseFileSuccessCallbackResult>((resolve, reject) => {
        uni.chooseFile({ count: 1, success: resolve, fail: reject })
      })
      filePath = choose.tempFilePaths?.[0] || (choose.tempFiles as { path?: string }[])?.[0]?.path || ''
    }
    if (!filePath) return
    uni.showLoading({ title: '上传中' })
    const res = await upload({ url: props.url, filePath })
    emit('success', res.data)
  } catch (e) {
    emit('error', e)
    uni.showToast({ title: '上传失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.file-uploader__default {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  min-height: 160rpx;
  border: 2rpx dashed #dcdfe6;
  border-radius: 16rpx;
  background: #fafafa;
}

.file-uploader__plus {
  font-size: 48rpx;
  color: #909399;
  line-height: 1;
}

.file-uploader__text {
  font-size: 26rpx;
  color: #909399;
}
</style>
