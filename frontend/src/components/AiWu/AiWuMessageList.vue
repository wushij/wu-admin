<template>
  <div class="aiwu-messages-wrap">
    <div ref="viewportRef" class="aiwu-messages" @scroll.passive="handleScroll">
      <!-- 空状态：同心轨道 + 快捷探索推荐 -->
      <div v-if="messages.length === 0 && !streaming" class="aiwu-empty">
        <div class="aiwu-empty-illustration" aria-hidden="true">
          <div class="aiwu-empty-orbit aiwu-empty-orbit--outer" />
          <div class="aiwu-empty-orbit aiwu-empty-orbit--inner" />
          <div class="aiwu-empty-core">
            <AiCompassIcon :size="30" :dark="true" :spin="true" />
          </div>
        </div>
        <h3 class="aiwu-empty-title">今天想了解系统的哪部分？</h3>
        <p class="aiwu-empty-subtitle">
          已接入系统功能知识库与当前账号权限，为你提供功能指引、操作路径与角色信息解答
        </p>

        <div class="aiwu-empty-presets">
          <div class="aiwu-presets-head">
            <span class="aiwu-presets-tag">
              <el-icon><StarFilled /></el-icon>
              快捷探索推荐
            </span>
            <span class="aiwu-presets-hint">点击即问</span>
          </div>
          <div class="aiwu-presets-list">
            <button
              v-for="(question, index) in questions"
              :key="index"
              class="aiwu-preset-pill"
              type="button"
              :title="question.text"
              :disabled="modelsLoaded && !hasModels"
              @click="emit('send', question.text)"
            >
              <el-icon class="pill-star"><StarFilled /></el-icon>
              <span class="pill-text">{{ question.text }}</span>
              <span class="pill-arrow">↗</span>
            </button>
          </div>
        </div>

        <p v-if="modelsLoaded && !hasModels" class="aiwu-empty-subtitle" style="margin-top: 12px">
          暂无可用模型，请联系管理员在「AI 模型配置」中启用并设为默认模型
        </p>
      </div>

      <div v-else class="aiwu-message-stream">
        <AiWuMessageItem
          v-for="(msg, index) in messages"
          :key="msg.id"
          :msg="msg"
          :streaming="streaming"
          :user-avatar="userAvatar"
          :user-fallback="userFallback"
          :can-regenerate="index === messages.length - 1"
          :question="questionFor(index)"
          @copy="emit('copy', $event)"
          @regenerate="emit('regenerate')"
          @delete="emit('delete', $event)"
        />
      </div>

      <div class="aiwu-bottom-spacer" aria-hidden="true" />
    </div>

    <AiWuScrollFab :visible="showFab" @click="scrollToBottom(true, true)" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { StarFilled } from '@element-plus/icons-vue'
import type { AiWuMessage } from '@/store/aiWu'
import { AI_WU_QUICK_QUESTIONS } from '@/constants/ai-wu'
import AiCompassIcon from './AiCompassIcon.vue'
import AiWuMessageItem from './AiWuMessageItem.vue'
import AiWuScrollFab from './AiWuScrollFab.vue'

const props = withDefaults(
  defineProps<{
    messages: AiWuMessage[]
    streaming?: boolean
    modelsLoaded?: boolean
    hasModels?: boolean
    userAvatar?: string
    userFallback?: string
  }>(),
  {
    streaming: false,
    modelsLoaded: false,
    hasModels: true,
    userAvatar: '',
    userFallback: '我',
  },
)

const emit = defineEmits<{
  (e: 'send', text: string): void
  (e: 'copy', content: string): void
  (e: 'regenerate'): void
  (e: 'delete', id: number): void
}>()

const questions = AI_WU_QUICK_QUESTIONS
const viewportRef = ref<HTMLElement>()
const showFab = ref(false)
const stickToBottom = ref(true)

/** 向上查找当前助手回答对应的用户问题，供思考步骤生成中文摘要 */
function questionFor(index: number): string {
  for (let i = index; i >= 0; i--) {
    if (props.messages[i].role === 'user') {
      return props.messages[i].content
    }
  }
  return ''
}

function distanceToBottom(el: HTMLElement): number {
  return el.scrollHeight - el.scrollTop - el.clientHeight
}

function handleScroll() {
  const el = viewportRef.value
  if (!el) return
  const distance = distanceToBottom(el)
  stickToBottom.value = distance < 120
  showFab.value = distance > 180
}

function scrollToBottom(force = false, smooth = false) {
  nextTick(() => {
    const el = viewportRef.value
    if (!el) return
    if (!force && !stickToBottom.value) return
    el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
    showFab.value = false
  })
}

const lastMessage = () => props.messages[props.messages.length - 1]

watch(
  [
    () => props.messages.length,
    () => lastMessage()?.content,
    () => lastMessage()?.reasoning,
    () => lastMessage()?.tools?.length,
  ],
  () => {
    // 流式增量：仅当用户停留在底部附近时跟随，避免打断向上翻阅
    if (props.streaming) {
      scrollToBottom(false)
    } else {
      scrollToBottom(true)
    }
  },
)

defineExpose({ scrollToBottom })
</script>
