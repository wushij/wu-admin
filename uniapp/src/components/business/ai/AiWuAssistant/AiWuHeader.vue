<template>
  <view class="aiwu-header" @touchmove.stop.prevent>
    <view class="aiwu-header__left">
      <view class="aiwu-header__avatar">
        <AiCompassIcon :size="24" :dark="true" :spin="true" />
      </view>
      <view class="aiwu-header__title">
        <text class="title">AI wu助手</text>
        <text class="subtitle">系统智能副驾 · 随时答疑</text>
      </view>
    </view>
    <view class="aiwu-header__actions">
      <view class="header-toolbar" role="toolbar">
        <view
          class="header-btn"
          :class="{ 'is-active': historyVisible }"
          title="历史对话"
          @click.stop="emit('toggle-history')"
        >
          <IconFont name="clock-o" :size="26" color="#f1f5f9" />
        </view>
        <view class="header-toolbar-sep" />
        <view class="header-btn" title="新对话" @click.stop="emit('new-session')">
          <svg viewBox="0 0 1024 1024" width="13" height="13" fill="#f1f5f9">
            <path d="M480 480V208a32 32 0 1 1 64 0v272h272a32 32 0 1 1 0 64H544v272a32 32 0 1 1-64 0V544H208a32 32 0 1 1 0-64h272z" />
          </svg>
        </view>
        <view class="header-toolbar-sep" />
        <view class="header-btn header-btn--close" title="关闭" @click.stop="emit('close')">
          <svg viewBox="0 0 1024 1024" width="12" height="12" fill="#f1f5f9">
            <path d="M292.7 236.1a32 32 0 0 0-45.3 45.3L466.7 512 247.4 731.3a32 32 0 0 0 45.3 45.3L512 557.3l219.3 219.3a32 32 0 0 0 45.3-45.3L557.3 512l219.3-219.3a32 32 0 0 0-45.3-45.3L512 466.7 292.7 236.1z" />
          </svg>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import IconFont from '@/components/base/IconFont/index.vue'
import AiCompassIcon from './AiCompassIcon.vue'

defineProps<{
  historyVisible?: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle-history'): void
  (e: 'new-session'): void
  (e: 'close'): void
}>()
</script>
