<template>
  <view class="aiwu-row" :class="isUser ? 'is-user' : 'is-ai'">
    <view v-if="!isUser" class="aiwu-avatar ai">
      <AiCompassIcon :size="18" :dark="true" :spin="false" />
    </view>

    <view class="aiwu-msg-col" :class="{ 'is-user': isUser }">
      <!-- 用户消息附件：图片缩略卡可全屏预览，文档为可打开/复制链接的文件卡 -->
      <view v-if="isUser && msg.attachments?.length" class="aiwu-user-attachments">
        <view
          v-for="(attachment, index) in msg.attachments"
          :key="`${attachment.attachmentId || attachment.fileName}-${index}`"
          class="aiwu-user-attachment-item"
        >
          <view
            v-if="attachment.isImage && attachmentUrl(attachment)"
            class="aiwu-user-image-card"
            @click.stop="previewImage(attachmentUrl(attachment))"
          >
            <image
              :src="attachmentUrl(attachment)"
              mode="aspectFill"
              class="aiwu-user-image-thumb"
            />
          </view>
          <AiWuFileCard
            v-else
            :file-name="attachment.fileName"
            :file-size="attachment.fileSize"
            :file-type="attachment.fileType"
            :file-url="attachment.fileUrl"
          />
        </view>
      </view>

      <view
        v-if="showBubble"
        class="aiwu-bubble"
        :class="{ 'is-error': msg.error, 'is-streaming': !isUser && msg.streaming }"
      >
        <!-- 深度思考卡片：嵌入助手气泡内部上方，默认折叠为脉冲横条 -->
        <AiWuThinking
          v-if="!isUser && !msg.error && (msg.streaming || hasThinking)"
          :reasoning="msg.reasoning"
          :tools="msg.tools"
          :active="Boolean(msg.streaming)"
          :has-answer-body="hasAnswerBody"
          :reasoning-streaming="msg.reasoningStreaming"
          :started-at="msg.thinkingStartedAt"
          :finished-at="msg.thinkingFinishedAt"
        />

        <text v-if="msg.error" class="aiwu-bubble__text">{{ msg.content }}</text>
        <view v-else-if="msg.streaming && !msg.content && !hasThinking" class="aiwu-typing">
          <view class="dot" />
          <view class="dot" />
          <view class="dot" />
        </view>
        <!-- 用户消息保持纯文本，避免 Markdown 样式与深色气泡冲突 -->
        <text v-else-if="isUser" class="aiwu-bubble__text">{{ msg.content }}</text>
        <template v-else>
          <!-- #ifdef H5 -->
          <view
            class="aiwu-markdown"
            :class="{ 'is-streaming': msg.streaming }"
            v-html="renderMarkdown(msg.content)"
          />
          <!-- #endif -->
          <!-- #ifndef H5 -->
          <text class="aiwu-bubble__text">{{ msg.content }}</text>
          <!-- H5 端已由 .aiwu-markdown.is-streaming 末尾伪元素渲染唯一光标，仅非 H5 端补充视图光标 -->
          <view v-if="msg.streaming && hasAnswerBody" class="aiwu-stream-cursor" />
          <!-- #endif -->
        </template>

        <!-- 联网搜索参考来源托盘：回答完成后显示在气泡内底部，默认折叠 -->
        <AiWuCitationTray
          v-if="!isUser && !msg.streaming && !msg.error"
          :citations="msg.citations || []"
        />
      </view>

      <view v-if="!msg.streaming" class="aiwu-msg-actions" :class="{ 'is-user': isUser }">
        <view v-if="msg.content" class="aiwu-act-chip" @click.stop="emit('copy', msg.content)">
          <text>复制</text>
        </view>
        <view
          v-if="!isUser && canRegenerate && !msg.error"
          class="aiwu-act-chip"
          @click.stop="emit('regenerate')"
        >
          <text>重新生成</text>
        </view>
        <view
          class="aiwu-act-chip aiwu-act-chip--danger"
          @click.stop="emit('delete', msg.id)"
        >
          <text>删除</text>
        </view>
      </view>
    </view>

    <image
      v-if="isUser && userAvatar"
      class="aiwu-avatar user-img"
      :src="userAvatar"
      mode="aspectFill"
    />
    <view v-else-if="isUser" class="aiwu-avatar user">
      <text>{{ initials }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AiCompassIcon from './AiCompassIcon.vue'
import AiWuThinking from './AiWuThinking.vue'
import AiWuCitationTray from './AiWuCitationTray.vue'
import AiWuFileCard from './AiWuFileCard.vue'
import { renderMarkdown } from '@/utils/display/chat-markdown'
import type { AiWuMessage } from '@/store/aiWu'

const props = withDefaults(
  defineProps<{
    msg: AiWuMessage
    streaming?: boolean
    userAvatar?: string
    userFallback?: string
    canRegenerate?: boolean
  }>(),
  {
    streaming: false,
    userAvatar: '',
    userFallback: '我',
    canRegenerate: false,
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
const initials = computed(() => (props.userFallback || '我').slice(0, 1))

/** 统一大卡片：流式期或存在正文/思考/报错时始终渲染助手气泡 */
const showBubble = computed(() => {
  if (isUser.value) return true
  return Boolean(props.msg.streaming || props.msg.error || hasAnswerBody.value || hasThinking.value)
})

/** 历史回放优先使用持久文件地址，本轮上传的 base64 仅作兜底 */
function attachmentUrl(attachment: { fileUrl?: string; imageUrl?: string }): string {
  return attachment.fileUrl || attachment.imageUrl || ''
}

function previewImage(url: string) {
  if (!url) return
  const urls = (props.msg.attachments || [])
    .filter((attachment) => attachment.isImage && attachmentUrl(attachment))
    .map((attachment) => attachmentUrl(attachment))
  uni.previewImage({ urls: urls.length > 0 ? urls : [url], current: url })
}
</script>
