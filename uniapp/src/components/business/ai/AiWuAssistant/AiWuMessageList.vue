<template>
  <scroll-view
    class="aiwu-body"
    scroll-y
    :scroll-top="scrollTop"
    :scroll-into-view="scrollIntoViewId"
    @scroll="onBodyScroll"
  >
    <!-- 欢迎屏（与 PC 对齐） -->
    <view v-if="messages.length === 0" class="welcome-box">
      <view class="welcome-hero">
        <view class="welcome-avatar-glow" />
        <view class="welcome-avatar">
          <AiCompassIcon :size="38" :dark="true" :spin="true" />
        </view>
      </view>
      <text class="welcome-title">你好，我是 AI wu助手</text>
      <text class="welcome-desc">
        {{
          modelsLoaded && !hasModels
            ? '暂无可用模型，请联系管理员配置'
            : '我是本系统的智能助手，熟悉各功能模块与操作路径，有问题尽管问我～'
        }}
      </text>

      <view class="quick-section">
        <view class="quick-header">
          <text class="quick-header-text">常见问题快速提问</text>
        </view>
        <view class="quick-list">
          <view
            v-for="(q, idx) in QUICK_QUESTIONS"
            :key="idx"
            class="quick-card"
            @click="emit('send', q.text)"
          >
            <view class="quick-card-icon">
              <IconFont :name="q.icon" :size="32" color="#0f172a" />
            </view>
            <view class="quick-card-content">
              <text class="quick-card-title">{{ q.text }}</text>
              <text class="quick-card-desc">{{ q.desc }}</text>
            </view>
            <text class="quick-card-arrow">›</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 消息列表 -->
    <AiWuMessageItem
      v-for="(msg, index) in messages"
      :id="`aiwu-msg-${msg.id}`"
      :key="msg.id"
      :msg="msg"
      :streaming="streaming"
      :user-avatar="userAvatar"
      :user-fallback="userFallback"
      :can-regenerate="index === messages.length - 1"
      @copy="emit('copy', $event)"
      @regenerate="emit('regenerate')"
      @delete="emit('delete', $event)"
    />

    <view id="aiwu-bottom-anchor" class="aiwu-anchor" />
  </scroll-view>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import IconFont from '@/components/base/IconFont/index.vue'
import AiCompassIcon from './AiCompassIcon.vue'
import AiWuMessageItem from './AiWuMessageItem.vue'
import type { AiWuMessage } from '@/store/aiWu'

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

const QUICK_QUESTIONS = [
  {
    icon: 'chat-o',
    text: '这个系统有哪些功能模块？',
    desc: '按你的权限梳理可用功能与入口',
  },
  {
    icon: 'notes-o',
    text: '怎么修改登录密码？',
    desc: '获取本系统真实操作路径指引',
  },
  {
    icon: 'clock-o',
    text: '我是什么角色，属于哪个部门？',
    desc: '查看当前账号的角色与组织信息',
  },
  {
    icon: 'cluster-o',
    text: '这个系统用了什么技术栈，有哪些核心优势？',
    desc: '了解前后端架构、安全机制与 AI 亮点',
  },
] as const

const scrollTop = ref(0)
const scrollIntoViewId = ref('')
const nearBottom = ref(true)

function scrollToBottom(force = true) {
  if (force) {
    nearBottom.value = true
  }
  nextTick(() => {
    scrollIntoViewId.value = ''
    nextTick(() => {
      scrollIntoViewId.value = 'aiwu-bottom-anchor'
    })
    scrollTop.value = scrollTop.value === 999999 ? 999998 : 999999

    // #ifdef H5
    const el = document.querySelector('.aiwu-body') as HTMLElement
    if (el && (force || nearBottom.value)) {
      el.scrollTop = el.scrollHeight
    }
    // #endif
  })
}

function onBodyScroll(e: { detail: { scrollTop: number; scrollHeight: number } }) {
  const { scrollTop: top, scrollHeight } = e.detail
  // #ifdef H5
  const el = document.querySelector('.aiwu-body') as HTMLElement
  if (el) {
    nearBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 120
    return
  }
  // #endif
  const viewH = uni.getWindowInfo().windowHeight * 0.6
  nearBottom.value = scrollHeight - top - viewH < 120
}

const lastMessage = () => props.messages[props.messages.length - 1]

// 新消息入列：强制滚底
watch(
  () => props.messages.length,
  () => scrollToBottom(true),
)

// 流式增量：仅在跟随模式下向上推动
watch(
  [
    () => lastMessage()?.content,
    () => lastMessage()?.reasoning,
    () => lastMessage()?.tools?.length,
  ],
  () => {
    if (props.streaming && nearBottom.value) {
      scrollToBottom(false)
    }
  },
)

// 流式结束：滚到完整回答底部
watch(
  () => props.streaming,
  (val) => {
    if (!val) scrollToBottom(true)
  },
)

defineExpose({ scrollToBottom })
</script>
