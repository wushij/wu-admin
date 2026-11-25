<template>
  <header class="aiwu-header">
    <div class="aiwu-header-left">
      <div class="aiwu-avatar" aria-hidden="true">
        <AiCompassIcon :size="26" :dark="true" :spin="true" />
      </div>
      <div class="aiwu-header-info">
        <div class="aiwu-title-row">
          <h2 class="aiwu-title">AI wu助手</h2>
          <span class="aiwu-badge">系统知识增强</span>
        </div>
        <p class="aiwu-subtitle">熟悉系统各功能模块与操作路径，随时为你答疑指引</p>
      </div>
    </div>

    <div class="aiwu-header-actions">
      <div class="aiwu-toolbar" role="toolbar" aria-label="助手面板操作">
        <el-tooltip :content="wide ? '恢复标准侧边栏 (640px)' : '展开宽屏研读工作台 (920px)'" placement="bottom" :show-after="1000">
          <button
            class="aiwu-header-btn aiwu-wide-toggle"
            :class="{ 'is-active': wide }"
            type="button"
            :aria-label="wide ? '恢复标准侧边栏' : '展开宽屏工作台'"
            @click="emit('toggle-wide')"
          >
            <el-icon :size="15"><FullScreen v-if="!wide" /><Aim v-else /></el-icon>
          </button>
        </el-tooltip>

        <span class="aiwu-toolbar-sep" aria-hidden="true" />

        <el-tooltip :content="historyOpen ? '收起历史会话' : '历史对话记录'" placement="bottom" :show-after="1000">
          <button
            class="aiwu-header-btn"
            :class="{ 'is-active': historyOpen }"
            type="button"
            data-aiwu-history-trigger
            aria-label="历史对话记录"
            @click="emit('toggle-history')"
          >
            <el-icon :size="15"><Clock /></el-icon>
          </button>
        </el-tooltip>

        <span class="aiwu-toolbar-sep" aria-hidden="true" />

        <el-tooltip content="开启新会话" placement="bottom" :show-after="1000">
          <button
            class="aiwu-header-btn"
            type="button"
            aria-label="开启新会话"
            @click="emit('new-session')"
          >
            <el-icon :size="15"><Plus /></el-icon>
          </button>
        </el-tooltip>

        <span class="aiwu-toolbar-sep aiwu-toolbar-sep--clear" aria-hidden="true" />

        <el-tooltip content="清空当前对话" placement="bottom" :show-after="1000">
          <button
            class="aiwu-header-btn aiwu-clear-btn"
            type="button"
            aria-label="清空当前对话"
            :disabled="!hasMessages"
            @click="emit('clear')"
          >
            <el-icon :size="15"><Delete /></el-icon>
          </button>
        </el-tooltip>

        <span class="aiwu-toolbar-sep" aria-hidden="true" />

        <el-tooltip content="关闭面板 (Esc)" placement="bottom" :show-after="1000">
          <button
            class="aiwu-header-btn aiwu-header-btn--close"
            type="button"
            aria-label="关闭面板"
            @click="emit('close')"
          >
            <el-icon :size="15"><Close /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { Aim, Clock, Close, Delete, FullScreen, Plus } from '@element-plus/icons-vue'
import AiCompassIcon from './AiCompassIcon.vue'

defineProps<{
  wide: boolean
  historyOpen: boolean
  hasMessages: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle-wide'): void
  (e: 'toggle-history'): void
  (e: 'new-session'): void
  (e: 'clear'): void
  (e: 'close'): void
}>()
</script>
