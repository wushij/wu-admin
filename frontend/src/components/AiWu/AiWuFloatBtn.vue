<template>
  <div class="ai-wu-float">
    <transition name="ai-slide-panel">
      <AiWuChatPanel v-if="aiWuStore.panelVisible" />
    </transition>

    <button
      class="ai-wu-ball"
      :class="{ 'is-open': aiWuStore.panelVisible, 'is-streaming': aiWuStore.streaming }"
      type="button"
      :title="aiWuStore.panelVisible ? '收起 AI wu助手' : 'AI wu助手'"
      @click="aiWuStore.togglePanel()"
    >
      <span class="ball-halo" />
      <el-icon v-if="aiWuStore.panelVisible" class="ball-icon" :size="22"><Close /></el-icon>
      <el-icon v-else class="ball-icon" :size="24"><MagicStick /></el-icon>
    </button>
  </div>
</template>

<script setup lang="ts">
import { MagicStick, Close } from '@element-plus/icons-vue'
import { useAiWuStore } from '@/store/aiWu'
import AiWuChatPanel from './AiWuChatPanel.vue'

const aiWuStore = useAiWuStore()
</script>

<style scoped lang="scss">
.ai-wu-ball {
  position: fixed;
  right: 32px;
  bottom: 32px;
  z-index: 2001;
  width: 56px;
  height: 56px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--theme-primary, #6366f1) 0%, var(--theme-logo-end, var(--theme-primary, #8b5cf6)) 100%);
  box-shadow: 0 8px 24px rgba(var(--theme-primary-rgb, 99, 102, 241), 0.4);
  transition: transform 0.25s, box-shadow 0.25s;

  &:hover {
    transform: translateY(-3px) scale(1.05);
    box-shadow: 0 12px 30px rgba(var(--theme-primary-rgb, 99, 102, 241), 0.5);
  }

  &:active {
    transform: scale(0.95);
  }

  &.is-open {
    background: linear-gradient(135deg, var(--theme-primary, #475569) 0%, var(--theme-logo-end, #1e293b) 100%);
    box-shadow: 0 8px 24px rgba(var(--theme-primary-rgb, 15, 23, 42), 0.35);
  }
}

.ball-icon {
  position: relative;
  z-index: 1;
}

/* 待机呼吸光环；流式回答中加速脉冲 */
.ball-halo {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: inherit;
  animation: ai-wu-pulse 2.6s ease-out infinite;
  pointer-events: none;
}

.ai-wu-ball.is-open .ball-halo {
  animation: none;
}

.ai-wu-ball.is-streaming .ball-halo {
  animation-duration: 1.2s;
}

@keyframes ai-wu-pulse {
  0% {
    transform: scale(1);
    opacity: 0.55;
  }
  70% {
    transform: scale(1.55);
    opacity: 0;
  }
  100% {
    transform: scale(1.55);
    opacity: 0;
  }
}

/* 面板滑入滑出 */
.ai-slide-panel-enter-active,
.ai-slide-panel-leave-active {
  transition: opacity 0.28s ease, transform 0.28s cubic-bezier(0.34, 1.3, 0.64, 1);
}

.ai-slide-panel-enter-from,
.ai-slide-panel-leave-to {
  opacity: 0;
  transform: translateY(24px) scale(0.96);
}
</style>
