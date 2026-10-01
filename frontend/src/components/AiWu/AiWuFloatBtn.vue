<template>
  <div v-if="siteStore.aiAssistantEnabled" class="ai-wu-float">
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
      <!-- 极光脉冲光环 -->
      <span class="aurora-pulse-ring" />

      <!-- 面板展开时显示关闭图标，否则显示科技罗盘 SVG -->
      <el-icon v-if="aiWuStore.panelVisible" class="ball-icon-close" :size="20"><Close /></el-icon>
      <AiCompassIcon v-else :size="34" :dark="true" :spin="true" class="ball-compass" />

      <!-- 流式回答跳动徽标 -->
      <span v-if="aiWuStore.streaming" class="streaming-ping" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { Close } from '@element-plus/icons-vue'
import { useAiWuStore } from '@/store/aiWu'
import { useSiteStore } from '@/store/site'
import AiWuChatPanel from './AiWuChatPanel.vue'
import AiCompassIcon from './AiCompassIcon.vue'

const aiWuStore = useAiWuStore()
const siteStore = useSiteStore()
</script>

<style scoped lang="scss">
.ai-wu-float {
  position: relative;
  z-index: 2000;
}

.ai-wu-ball {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 2001;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #101c38, #0c1222) !important;
  border: 1.6px solid rgba(22, 119, 255, 0.5) !important;
  box-shadow: 0 6px 24px rgba(22, 119, 255, 0.35), 0 0 16px rgba(114, 46, 209, 0.25);
  color: #fff;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: visible;

  &:hover {
    transform: translateY(-3px) scale(1.08);
    border-color: rgba(64, 150, 255, 0.9) !important;
    box-shadow: 0 10px 32px rgba(22, 119, 255, 0.5), 0 0 24px rgba(114, 46, 209, 0.4);

    :deep(.compass-star) {
      transform: rotate(45deg);
    }
  }

  &:active {
    transform: scale(0.95);
  }

  &.is-open {
    border-color: #4096ff !important;
    box-shadow: 0 0 24px rgba(22, 119, 255, 0.55);
    background: linear-gradient(145deg, #0e172a, #020617) !important;
  }
}

.ball-compass {
  position: relative;
  z-index: 1;
}

.ball-icon-close {
  position: relative;
  z-index: 1;
  color: #93c5fd;
  transition: transform 0.25s ease;
}

.ai-wu-ball.is-open:hover .ball-icon-close {
  transform: rotate(90deg);
  color: #ffffff;
}

/* 极光环境脉冲光环 */
.aurora-pulse-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 1px solid rgba(64, 150, 255, 0.55);
  animation: auroraPulse 2.8s cubic-bezier(0.4, 0, 0.6, 1) infinite;
  pointer-events: none;
}

.ai-wu-ball.is-open .aurora-pulse-ring {
  animation: none;
  opacity: 0;
}

.streaming-ping {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #52c41a;
  box-shadow: 0 0 10px #52c41a;
  animation: pingDot 1.2s ease-in-out infinite alternate;
  z-index: 2;
}

@keyframes auroraPulse {
  0% {
    transform: scale(0.95);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.18);
    opacity: 0;
  }
  100% {
    transform: scale(0.95);
    opacity: 0;
  }
}

@keyframes pingDot {
  from {
    opacity: 0.4;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1.2);
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

