<template>
  <div class="aiwu-message-row" :class="isUser ? 'is-user' : 'is-assistant'">
    <div v-if="!isUser" class="aiwu-msg-avatar is-ai" aria-hidden="true">
      <AiCompassIcon :size="22" :dark="true" :spin="false" />
    </div>

    <div class="aiwu-message-col">
      <!-- 用户消息附件：图片缩略卡可全屏预览，文档为可下载文件卡 -->
      <div v-if="isUser && msg.attachments?.length" class="aiwu-user-attachments">
        <div
          v-for="(attachment, index) in msg.attachments"
          :key="`${attachment.attachmentId || attachment.fileName}-${index}`"
          class="aiwu-user-attachment-item"
        >
          <div
            v-if="attachment.isImage && (attachment.fileUrl || attachment.imageUrl)"
            class="aiwu-user-image-card"
            title="点击放大查看原图"
            @click="openImagePreview(attachmentFileUrl(attachment))"
          >
            <img
              :src="attachmentFileUrl(attachment)"
              class="aiwu-user-image-thumb"
              :alt="attachment.fileName || '用户上传图片'"
              loading="lazy"
              decoding="async"
            />
            <div class="aiwu-user-image-zoom">
              <el-icon :size="22"><ZoomIn /></el-icon>
            </div>
          </div>
          <AiWuFileCard
            v-else
            :file-name="attachment.fileName"
            :file-size="attachment.fileSize"
            :file-type="attachment.fileType"
            :file-url="attachment.fileUrl"
          />
        </div>
      </div>

      <div
        v-if="showBubble"
        class="aiwu-bubble"
        :class="[
          isUser ? 'is-user' : 'is-assistant',
          { 'is-streaming': !isUser && msg.streaming, 'is-error': msg.error },
        ]"
      >
        <!-- 流式生成头部状态条（复刻 EduMind 全局助手） -->
        <div v-if="!isUser && msg.streaming" class="aiwu-meta-header">
          <span class="sender-name">AI wu 助手</span>
          <span class="streaming-badge-tag">正在深度研读与生成解答...</span>
        </div>

        <!-- 深度思考卡片：嵌入助手气泡内部上方，默认折叠为脉冲横条 -->
        <AiWuThinking
          v-if="!isUser && !msg.error && (msg.streaming || hasThinking)"
          :reasoning="msg.reasoning"
          :tools="msg.tools"
          :active="Boolean(msg.streaming)"
          :has-answer-body="hasAnswerBody"
          :reasoning-streaming="msg.reasoningStreaming"
          :question="question"
          :started-at="msg.thinkingStartedAt"
          :finished-at="msg.thinkingFinishedAt"
        />

        <AiWuMarkdown
          v-if="!msg.error && (isUser || hasAnswerBody)"
          :content="msg.content"
          :streaming="msg.streaming"
        />
        <span v-else-if="msg.error" class="aiwu-bubble-text">{{ msg.content }}</span>

        <span v-if="msg.streaming && hasAnswerBody" class="aiwu-stream-cursor" aria-hidden="true" />

        <!-- 联网搜索参考来源托盘（复刻 EduMind：消息回复完成后显示在气泡内底部，默认折叠） -->
        <div
          v-if="!isUser && !msg.streaming && !msg.error && msg.citations?.length"
          class="aiwu-citations-tray"
        >
          <button
            class="aiwu-citations__header"
            type="button"
            :aria-expanded="citationsExpanded"
            @click="citationsExpanded = !citationsExpanded"
          >
            <span class="aiwu-citations__badge">
              <el-icon><Compass /></el-icon>
              <span>实时联网检索参考 ({{ msg.citations.length }})</span>
            </span>
            <span class="aiwu-citations__header-right">
              <span class="aiwu-citations__sub">{{ citationsExpanded ? '收起' : '展开查看' }}</span>
              <el-icon class="aiwu-citations__chevron" :class="{ 'is-expanded': citationsExpanded }">
                <ArrowDown />
              </el-icon>
            </span>
          </button>

          <div v-show="citationsExpanded" class="aiwu-citations__cards">
            <a
              v-for="(cite, idx) in msg.citations"
              :key="`${cite.url}-${idx}`"
              class="aiwu-citation-card"
              :href="cite.url"
              target="_blank"
              rel="noopener noreferrer"
              :title="`在新标签页打开：${cite.url}`"
            >
              <div class="aiwu-citation-card__top">
                <span class="aiwu-citation-card__idx">[{{ idx + 1 }}]</span>
                <span class="aiwu-citation-card__doc">
                  <span class="aiwu-citation-card__web-tag">联网</span>
                  <span class="aiwu-citation-card__title">{{ cite.title }}</span>
                  <el-icon class="aiwu-citation-card__ext"><TopRight /></el-icon>
                </span>
                <span class="aiwu-citation-card__match">{{ cite.source || '实时检索' }}</span>
              </div>
              <div class="aiwu-citation-card__excerpt">
                {{ resolveCitationPreviewText(cite.snippet, cite.url) }}
              </div>
            </a>
          </div>
        </div>
      </div>

      <div v-if="!msg.streaming" class="aiwu-message-actions" :class="{ 'is-user': isUser }">
        <el-tooltip v-if="msg.content" content="复制内容" placement="top" :show-after="1000">
          <button
            class="aiwu-action-chip"
            type="button"
            aria-label="复制内容"
            @click="emit('copy', msg.content)"
          >
            <el-icon :size="12"><CopyDocument /></el-icon>
            <span>复制</span>
          </button>
        </el-tooltip>

        <el-tooltip
          v-if="!isUser && canRegenerate && !msg.streaming && !msg.error"
          content="重新生成回答"
          placement="top"
          :show-after="1000"
        >
          <button
            class="aiwu-action-chip"
            type="button"
            aria-label="重新生成回答"
            :disabled="streaming"
            @click="emit('regenerate')"
          >
            <el-icon :size="12"><RefreshRight /></el-icon>
            <span>重新生成</span>
          </button>
        </el-tooltip>

        <el-tooltip content="删除这条消息" placement="top" :show-after="1000">
          <button
            class="aiwu-action-chip aiwu-action-chip--danger"
            type="button"
            aria-label="删除这条消息"
            @click="emit('delete', msg.id)"
          >
            <el-icon :size="12"><Delete /></el-icon>
            <span>删除</span>
          </button>
        </el-tooltip>
      </div>
    </div>

    <div v-if="isUser" class="aiwu-msg-avatar is-user" aria-hidden="true">
      <img v-if="avatarUrl && !avatarBroken" :src="avatarUrl" alt="" @error="avatarBroken = true" />
      <span v-else>{{ avatarFallback }}</span>
    </div>

    <ElImageViewer
      v-if="previewVisible"
      :url-list="previewUrls"
      :initial-index="previewIndex"
      teleported
      hide-on-click-modal
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElImageViewer } from 'element-plus'
import { ArrowDown, Compass, CopyDocument, Delete, RefreshRight, TopRight, ZoomIn } from '@element-plus/icons-vue'
import { resolveCitationPreviewText } from '@/utils/ai-markdown/citation-preview'
import type { AiWuMessage } from '@/store/aiWu'
import AiCompassIcon from './AiCompassIcon.vue'
import AiWuFileCard from './AiWuFileCard.vue'
import AiWuMarkdown from './AiWuMarkdown.vue'
import AiWuThinking from './AiWuThinking.vue'

const props = withDefaults(
  defineProps<{
    msg: AiWuMessage
    streaming?: boolean
    userAvatar?: string
    userFallback?: string
    canRegenerate?: boolean
    /** 当前回答对应的用户问题 */
    question?: string
  }>(),
  {
    streaming: false,
    userAvatar: '',
    userFallback: '我',
    canRegenerate: false,
    question: '',
  },
)

const emit = defineEmits<{
  (e: 'copy', content: string): void
  (e: 'regenerate'): void
  (e: 'delete', id: number): void
}>()

const isUser = computed(() => props.msg.role === 'user')
const hasAnswerBody = computed(() => Boolean((props.msg.content || '').trim()))
const hasThinking = computed(
  () => Boolean((props.msg.reasoning || '').trim()) || Boolean(props.msg.tools?.length),
)

/** 统一大卡片：流式期或存在正文/思考/报错时始终渲染助手气泡 */
const showBubble = computed(() => {
  if (isUser.value) return true
  return Boolean(props.msg.streaming || props.msg.error || hasAnswerBody.value || hasThinking.value)
})

const avatarBroken = ref(false)
const citationsExpanded = ref(false)
const avatarUrl = computed(() => props.userAvatar || '')
const avatarFallback = computed(() => (props.userFallback || '我').slice(0, 1))

/** 用户消息中的图片附件地址（用于全屏预览翻页） */
const previewUrls = computed(() =>
  (props.msg.attachments || [])
    .filter((attachment) => attachment.isImage && (attachment.fileUrl || attachment.imageUrl))
    .map((attachment) => attachmentFileUrl(attachment)),
)
const previewVisible = ref(false)
const previewIndex = ref(0)

/** 历史回放优先使用持久文件地址，本轮上传的 base64 仅作兜底 */
function attachmentFileUrl(attachment: { fileUrl?: string; imageUrl?: string }): string {
  return attachment.fileUrl || attachment.imageUrl || ''
}

function openImagePreview(url?: string) {
  if (!url) return
  const index = previewUrls.value.indexOf(url)
  previewIndex.value = index >= 0 ? index : 0
  previewVisible.value = true
}

watch(
  () => props.userAvatar,
  () => {
    avatarBroken.value = false
  },
)
</script>
