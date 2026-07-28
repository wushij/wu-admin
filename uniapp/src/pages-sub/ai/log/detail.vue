<template>
  <view class="ailog-detail-page">
    <ModuleDarkHero
      title="对话日志详情"
      subtitle="问答记录与 Token 消耗审计明细"
      icon="records-o"
      theme="ai"
    />

    <view v-if="detail" class="detail-body">
      <!-- 基本信息面板 -->
      <MonitorPanel title="基本信息">
        <DetailRow label="操作用户" :value="detail.username" />
        <DetailRow label="供应商" :value="providerMeta(detail.provider).label" />
        <DetailRow label="模型名称" :value="detail.modelName" />
        <DetailRow label="对话状态">
          <template #value>
            <text class="status-pill" :class="detail.chatStatus === 1 ? 'is-success' : detail.chatStatus === 2 ? 'is-warn' : 'is-error'">
              {{ statusLabel(detail.chatStatus) }}
            </text>
          </template>
        </DetailRow>
        <DetailRow label="Tokens消耗" :value="`${detail.totalTokens || 0} Tokens (${detail.promptTokens || 0} + ${detail.completionTokens || 0})`" />
        <DetailRow label="响应耗时" :value="formatDuration(detail.durationMs)" />
        <DetailRow label="请求来源" :value="detail.source === 'mobile' ? '移动端' : 'PC端'" />
        <DetailRow label="会话 ID" :value="detail.conversationId || '—'" />
        <DetailRow label="创建时间" :value="detail.createTime || '—'" />
      </MonitorPanel>

      <!-- 提问内容 -->
      <MonitorPanel title="提问内容 (Question)">
        <view class="qa-block qa-block--q">
          <view class="qa-block__head">
            <text class="qa-badge qa-badge--q">Q</text>
            <text class="qa-title">提问文本</text>
            <view class="copy-btn" @click="copyText(detail.question, '提问内容已复制')">
              <text>复制问题</text>
            </view>
          </view>
          <text class="qa-content" user-select>{{ detail.question }}</text>
        </view>
      </MonitorPanel>

      <!-- 回答内容 -->
      <MonitorPanel title="回答内容 (Answer)">
        <view class="qa-block qa-block--a" :class="{ 'is-error': detail.chatStatus === 0 }">
          <view class="qa-block__head">
            <text class="qa-badge qa-badge--a" :class="{ 'is-error': detail.chatStatus === 0 }">A</text>
            <text class="qa-title">{{ detail.chatStatus === 0 ? '错误信息' : '回答文本' }}</text>
            <view v-if="detail.answer || detail.errorMsg" class="copy-btn" @click="copyText(detail.chatStatus === 0 ? (detail.errorMsg || '') : detail.answer, '回答内容已复制')">
              <text>复制回答</text>
            </view>
          </view>
          <view
            v-if="isH5 && detail.chatStatus !== 0 && detail.answer"
            class="qa-content markdown-body"
            v-html="renderMarkdown(detail.answer)"
          />
          <text v-else class="qa-content" :class="{ 'is-error': detail.chatStatus === 0 }" user-select>
            {{ detail.chatStatus === 0 && detail.errorMsg ? detail.errorMsg : cleanMarkdownText(detail.answer) || '（无回复内容）' }}
          </text>
        </view>
      </MonitorPanel>
    </view>

    <EmptyState v-else title="日志信息不存在或已失效" icon="records-o" />

    <PageFooter v-if="detail && canDelete">
      <button class="page-footer__btn page-footer__btn--danger" @click="handleDelete">
        删除记录
      </button>
    </PageFooter>
    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import MonitorPanel from '@/components/common/MonitorPanel/index.vue'
import DetailRow from '@/components/common/DetailRow/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { providerMeta } from '@/constants/aiProviders'
import { deleteAiChatLog, type AiChatLogVO } from '@/api/system/ai-log'
import { useModulePermission } from '@/composables/useModulePermission'
import { renderMarkdown } from '@/utils/chat-markdown'
import { cleanMarkdownText } from '@/utils/format'
import { showConfirm } from '@/store/dialog'

let isH5 = false
// #ifdef H5
isH5 = true
// #endif

const { hasPerm } = useModulePermission('system:ai-log:list')
const canDelete = computed(() => hasPerm('system:ai-log:delete'))
const detail = ref<AiChatLogVO | null>(null)

onLoad(() => {
  try {
    const cached = uni.getStorageSync('ai_log_detail_cache')
    if (cached && typeof cached === 'object') {
      detail.value = cached as AiChatLogVO
    }
  } catch {
    detail.value = null
  }
})

function statusLabel(status: number) {
  if (status === 1) return '成功'
  if (status === 2) return '中断'
  return '失败'
}

function formatDuration(ms: number) {
  if (!ms) return '0s'
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)}s` : `${ms}ms`
}

function copyText(text: string, toastText: string) {
  if (!text) return
  uni.setClipboardData({
    data: text,
    success: () => {
      uni.showToast({ title: toastText, icon: 'success' })
    },
  })
}

async function handleDelete() {
  if (!detail.value) return
  const res = await showConfirm({
    title: '删除日志',
    content: '确定要删除这条对话记录？此操作不可撤销！',
    confirmText: '删除',
    cancelText: '取消',
    tone: 'danger',
  })
  if (!res.confirmed || !detail.value) return
  await deleteAiChatLog(detail.value.id)
  uni.showToast({ title: '已删除', icon: 'success' })
  setTimeout(() => {
    uni.navigateBack()
  }, 500)
}
</script>

<!-- v-html 产物不带 data-v 属性，uni-app 会给组件内所有 style 块强制加 scoped，
     .markdown-body 正文样式统一挂在 App.vue 引入的 chat-markdown-global.scss 全局样式中 -->
<style lang="scss" scoped>
@use '@/styles/mine.scss' as *;

.ailog-detail-page {
  @include mine-page-bg;
  min-height: 100vh;
  padding: $page-padding-y $page-padding-x calc(160rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.status-pill {
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 600;

  &.is-success {
    background: #f0fdf4;
    color: #16a34a;
  }

  &.is-warn {
    background: #fffbeb;
    color: #d97706;
  }

  &.is-error {
    background: #fef2f2;
    color: #dc2626;
  }
}

.qa-block {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  border: 1rpx solid rgba(226, 232, 240, 0.8);

  &.is-error {
    background: #fff5f5;
    border-color: #fecaca;
  }
}

.qa-block__head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.qa-badge {
  width: 36rpx;
  height: 36rpx;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 800;

  &--q {
    background: rgba(99, 102, 241, 0.1);
    color: #6366f1;
  }

  &--a {
    background: rgba(16, 185, 129, 0.1);
    color: #059669;

    &.is-error {
      background: rgba(239, 68, 68, 0.1);
      color: #dc2626;
    }
  }
}

.qa-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #0f172a;
}

.copy-btn {
  margin-left: auto;
  padding: 4rpx 14rpx;
  border-radius: 10rpx;
  background: #ffffff;
  border: 1rpx solid #e2e8f0;

  text {
    font-size: 22rpx;
    color: #6366f1;
    font-weight: 500;
  }
}

.qa-content {
  font-size: 26rpx;
  line-height: 1.65;
  color: #334155;
  word-break: break-word;

  &.is-error {
    color: #dc2626;
  }
}

.page-footer__btn--danger {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
  border: 1rpx solid rgba(239, 68, 68, 0.2);
}
</style>
