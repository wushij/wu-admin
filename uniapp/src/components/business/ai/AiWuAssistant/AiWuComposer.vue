<template>
  <view class="aiwu-footer" @click="closeUploadMenu" @touchmove.stop.prevent>
    <!-- 点击抽屉内菜单外区域时收起浮层（依托 .aiwu-drawer 形成的 fixed 包含块精准覆盖抽屉区域） -->
    <view
      v-if="uploadMenuVisible"
      class="aiwu-upload-backdrop"
      @click="closeUploadMenu"
      @touchmove.stop.prevent
    />

    <view
      class="aiwu-composer"
      :class="{
        'has-attachments': draftItems.length > 0,
        'is-web-active': webSearchAvailable && webSearchEnabled,
      }"
    >
      <!-- 锚定于附件按钮正上方的轻量高质感上传浮层，彻底避免原生 showActionSheet 被抽屉遮挡与样式简陋问题 -->
      <view
        v-if="uploadMenuVisible"
        class="aiwu-upload-popover"
        role="menu"
        aria-label="选择上传附件类型"
        @click.stop
      >
        <view class="aiwu-upload-popover__header">
          <text class="aiwu-upload-popover__heading">添加附件</text>
          <text class="aiwu-upload-popover__quota">{{ draftItems.length }}/5 · 单文件 ≤ 20MB</text>
        </view>

        <view
          class="aiwu-upload-option"
          role="menuitem"
          @click="handlePickImages"
        >
          <view class="aiwu-upload-option__icon aiwu-upload-option__icon--image">
            <!-- #ifdef H5 -->
            <svg viewBox="0 0 24 24" class="aiwu-upload-option__svg" fill="none">
              <rect
                x="3.25"
                y="4.25"
                width="17.5"
                height="15.5"
                rx="3.2"
                stroke="currentColor"
                stroke-width="1.8"
              />
              <circle cx="8.6" cy="9.4" r="1.65" fill="currentColor" />
              <path
                d="M4.5 16.5L9.2 12.1a1.3 1.3 0 0 1 1.78.02l2.62 2.48 2.15-2.05a1.3 1.3 0 0 1 1.78.01l2.72 2.58"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <IconFont name="apps-o" :size="34" color="currentColor" />
            <!-- #endif -->
          </view>
          <view class="aiwu-upload-option__body">
            <text class="aiwu-upload-option__title">上传图片</text>
            <text class="aiwu-upload-option__desc">从相册选择或即时拍照识别</text>
          </view>
          <view class="aiwu-upload-option__chevron">
            <!-- #ifdef H5 -->
            <svg viewBox="0 0 16 16" class="aiwu-upload-option__chevron-svg" fill="none">
              <path
                d="M6 3.5L10.5 8L6 12.5"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <IconFont name="arrow" :size="24" color="currentColor" />
            <!-- #endif -->
          </view>
        </view>

        <view
          class="aiwu-upload-option"
          role="menuitem"
          @click="handlePickDocuments"
        >
          <view class="aiwu-upload-option__icon aiwu-upload-option__icon--doc">
            <!-- #ifdef H5 -->
            <svg viewBox="0 0 24 24" class="aiwu-upload-option__svg" fill="none">
              <path
                d="M7 3.25h6.6a1.8 1.8 0 0 1 1.27.53l3.35 3.35a1.8 1.8 0 0 1 .53 1.27V19a1.75 1.75 0 0 1-1.75 1.75H7A1.75 1.75 0 0 1 5.25 19V5A1.75 1.75 0 0 1 7 3.25Z"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linejoin="round"
              />
              <path
                d="M13.5 3.6v3.9a1 1 0 0 0 1 1h3.9"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M8.75 12.5h6.5M8.75 15.8h4.5"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
              />
            </svg>
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <IconFont name="records-o" :size="34" color="currentColor" />
            <!-- #endif -->
          </view>
          <view class="aiwu-upload-option__body">
            <text class="aiwu-upload-option__title">上传文档</text>
            <text class="aiwu-upload-option__desc">PDF / Word / 表格 / 代码文本</text>
          </view>
          <view class="aiwu-upload-option__chevron">
            <!-- #ifdef H5 -->
            <svg viewBox="0 0 16 16" class="aiwu-upload-option__chevron-svg" fill="none">
              <path
                d="M6 3.5L10.5 8L6 12.5"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <IconFont name="arrow" :size="24" color="currentColor" />
            <!-- #endif -->
          </view>
        </view>

        <!-- 指向回形针入口的底部小三角 -->
        <view class="aiwu-upload-popover__arrow" />
      </view>

      <AiWuAttachmentTray
        :items="draftItems"
        :format-file-size="formatFileSize"
        @remove="removeAttachment"
        @retry="retryAttachment"
        @preview="previewImage"
      />

      <!-- 1:1 复刻 EduMind 侧边栏 AI 一体化蓝边胶囊输入栏：[上传 + 联网罗盘 | 输入框 | 圆形发送] -->
      <view class="aiwu-input-wrap">
        <view class="aiwu-input-tools">
          <!-- 上传附件（精致线性回形针图标） -->
          <view
            class="aiwu-inline-tool"
            :class="{
              'has-badge': readyCount > 0 || hasUploading,
              'is-menu-open': uploadMenuVisible,
              'is-disabled': disabled || streaming,
            }"
            role="button"
            :aria-expanded="uploadMenuVisible"
            aria-label="上传图片或文件"
            @click.stop="toggleUploadMenu"
          >
            <!-- #ifdef H5 -->
            <svg viewBox="0 0 24 24" class="aiwu-tool-svg" fill="none">
              <path
                d="M21.44 11.05l-8.49 8.49a5.5 5.5 0 0 1-7.78-7.78l8.49-8.49a3.67 3.67 0 0 1 5.19 5.19l-8.5 8.49a1.83 1.83 0 0 1-2.59-2.59l7.78-7.78"
                stroke="currentColor"
                stroke-width="1.85"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <IconFont name="notes-o" :size="32" color="currentColor" />
            <!-- #endif -->
            <view v-if="readyCount > 0 || hasUploading" class="aiwu-inline-badge">
              <text>{{ hasUploading ? '…' : readyCount }}</text>
            </view>
          </view>

          <!-- 联网搜索切换（复刻 EduMind 圆形指南针罗盘图标，开启时亮科技蓝） -->
          <view
            class="aiwu-inline-tool aiwu-inline-tool--compass"
            :class="{
              'is-active': webSearchAvailable && webSearchEnabled,
              'is-unavailable': !webSearchAvailable,
              'is-disabled': disabled || streaming,
            }"
            role="button"
            :aria-pressed="webSearchAvailable && webSearchEnabled"
            aria-label="联网搜索开关"
            @click="handleToggleWebSearch"
          >
            <!-- #ifdef H5 -->
            <svg viewBox="0 0 24 24" class="aiwu-tool-svg" fill="none">
              <circle cx="12" cy="12" r="9.25" stroke="currentColor" stroke-width="1.8" />
              <polygon
                points="15.6,8.4 13.7,13.7 8.4,15.6 10.3,10.3"
                stroke="currentColor"
                stroke-width="1.65"
                stroke-linejoin="round"
              />
              <circle cx="12" cy="12" r="1.15" fill="currentColor" />
            </svg>
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <IconFont name="search" :size="32" color="currentColor" />
            <!-- #endif -->
          </view>
        </view>

        <textarea
          v-model="inputText"
          class="aiwu-input"
          :maxlength="4000"
          :auto-height="true"
          :show-confirm-bar="false"
          :cursor-spacing="16"
          confirm-type="send"
          :disabled="disabled"
          :placeholder="computedPlaceholder"
          placeholder-class="aiwu-input-placeholder"
          @focus="closeUploadMenu"
          @confirm="submit"
        />

        <view
          v-if="streaming"
          class="aiwu-send stop"
          role="button"
          aria-label="停止生成"
          @click="emit('stop')"
        >
          <view class="stop-square" />
        </view>
        <view
          v-else
          class="aiwu-send"
          :class="{ disabled: !canSend }"
          role="button"
          aria-label="发送提问"
          @click="submit"
        >
          <!-- #ifdef H5 -->
          <svg viewBox="0 0 24 24" class="aiwu-send-svg" fill="none">
            <path
              d="M21 3L10 14"
              stroke="#ffffff"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <path
              d="M21 3L14.2 21L10 14L3 9.8L21 3Z"
              stroke="#ffffff"
              stroke-width="1.9"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <!-- #endif -->
          <!-- #ifndef H5 -->
          <text class="aiwu-send-glyph">➤</text>
          <!-- #endif -->
        </view>
      </view>
    </view>

    <text class="aiwu-disclaimer">内容由 AI 生成，仅作操作指引参考，请以系统实际页面为准</text>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import IconFont from '@/components/base/IconFont/index.vue'
import AiWuAttachmentTray from './AiWuAttachmentTray.vue'
import { useAiWuAttachments } from '@/composables/aiWu/useAiWuAttachments'
import type { AiWuAttachment } from '@/store/aiWu'

const props = withDefaults(
  defineProps<{
    modelValue: string
    streaming?: boolean
    disabled?: boolean
    placeholder?: string
    webSearchAvailable?: boolean
    webSearchEnabled?: boolean
  }>(),
  {
    streaming: false,
    disabled: false,
    placeholder: '输入问题…',
    webSearchAvailable: false,
    webSearchEnabled: false,
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
  chooseImages,
  chooseDocuments,
  retry,
  remove,
  clear,
  formatFileSize,
} = useAiWuAttachments()

const inputText = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value),
})

const computedPlaceholder = computed(() => {
  if (hasUploading.value) return '附件解析中，可先输入问题…'
  if (props.webSearchAvailable && props.webSearchEnabled) {
    return '已开启联网检索，输入你的问题…'
  }
  return props.placeholder
})

const canSend = computed(
  () =>
    Boolean(props.modelValue.trim() || readyCount.value > 0) &&
    !props.streaming &&
    !props.disabled &&
    !hasUploading.value,
)

const uploadMenuVisible = ref(false)

watch(
  () => [props.disabled, props.streaming],
  ([disabled, streaming]) => {
    if (disabled || streaming) {
      uploadMenuVisible.value = false
    }
  },
)

function closeUploadMenu() {
  uploadMenuVisible.value = false
}

function toggleUploadMenu() {
  if (props.disabled || props.streaming) return
  uploadMenuVisible.value = !uploadMenuVisible.value
}

function handlePickImages() {
  uploadMenuVisible.value = false
  // 保持在点击事件同步帧内调起系统选择器，避免移动端浏览器拦截异步弹窗
  void chooseImages()
}

function handlePickDocuments() {
  uploadMenuVisible.value = false
  void chooseDocuments()
}

function handleToggleWebSearch() {
  closeUploadMenu()
  emit('toggle-web-search')
}

function removeAttachment(index: number) {
  remove(index)
}

async function retryAttachment(index: number) {
  await retry(index)
}

function previewImage(url: string) {
  if (!url) return
  const urls = previewImages.value.length > 0 ? previewImages.value : [url]
  uni.previewImage({ urls, current: url })
}

function submit() {
  closeUploadMenu()
  if (!canSend.value) return
  const text = props.modelValue.trim()
  emit('send', { text, attachments: readyAttachments.value })
  emit('update:modelValue', '')
  clear()
}

defineExpose({
  clear,
  closeUploadMenu,
})
</script>
