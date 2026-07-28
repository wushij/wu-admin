<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="ailog-page">
    <ModuleDarkHero
      title="AI 对话日志"
      :subtitle="`问答记录审计 · 共 ${total} 条`"
      icon="records-o"
      theme="ai"
    >
      <template v-if="canDelete" #extra>
        <view class="module-dark-hero__chip" @click.stop="handleClean">
          <text>清空日志</text>
        </view>
      </template>
    </ModuleDarkHero>

    <SegmentTabs v-model="statusMode" :tabs="statusTabs" />
    <SearchBar v-model="keyword" placeholder="搜索用户名" @search="onSearch" />

    <ListLoading v-if="loading && !list.length" />

    <scroll-view
      v-else
      scroll-y
      class="ailog-page__scroll"
      @scrolltolower="loadMore"
    >
      <view
        v-for="item in list"
        :key="item.id"
        class="ailog-card card--elevated"
        @click="goDetail(item)"
        @longpress="onItemMenu(item)"
      >
        <view class="ailog-card__head">
          <view class="user-pill">
            <text class="user-icon">👤</text>
            <text class="user-name">{{ item.username }}</text>
          </view>
          <text class="model-pill">{{ providerMeta(item.provider).label }} · {{ item.modelName }}</text>
          <view
            class="status-pill"
            :class="item.chatStatus === 1 ? 'is-success' : item.chatStatus === 2 ? 'is-warn' : 'is-error'"
          >
            <text>{{ statusLabel(item.chatStatus) }}</text>
          </view>
        </view>

        <view class="ailog-card__qa-box">
          <view class="qa-item">
            <text class="qa-tag qa-tag--q">Q</text>
            <text class="qa-text qa-text--q is-collapsed">{{ item.question }}</text>
          </view>
          <view class="detail-link">
            <text>查看完整明细详情</text>
            <text class="arrow">→</text>
          </view>
        </view>

        <view class="ailog-card__meta">
          <text class="meta-pill">{{ item.totalTokens }} Tokens</text>
          <text class="meta-pill">{{ formatDuration(item.durationMs) }}</text>
          <text class="meta-pill">{{ item.source === 'mobile' ? '移动端' : 'PC端' }}</text>
          <text class="meta-time">{{ item.createTime || '' }}</text>
        </view>
      </view>

      <EmptyState v-if="empty" title="暂无对话日志" icon="records-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>
    <AiWuAssistant />
    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
import { pageAiChatLog, deleteAiChatLog, cleanAiChatLog, type AiChatLogVO } from '@/api/system/ai-log'
import { providerMeta } from '@/constants/aiProviders'
import { showConfirm } from '@/store/dialog'

const { allowed, hasPerm } = useModulePermission('system:ai-log:list')
const canDelete = computed(() => hasPerm('system:ai-log:delete'))

async function handleClean() {
  const res = await showConfirm({
    title: '清空日志',
    content: '确定要清空所有 AI 对话日志数据？此操作不可撤销！',
    confirmText: '清空',
    cancelText: '取消',
    tone: 'danger',
  })
  if (!res.confirmed) return
  await cleanAiChatLog()
  uni.showToast({ title: '已清空', icon: 'success' })
  await refresh({ silent: true })
}

const keyword = ref('')
const total = ref(0)
const statusMode = ref('all')
const statusTabs = [
  { key: 'all', label: '全部' },
  { key: '1', label: '成功' },
  { key: '0', label: '失败' },
  { key: '2', label: '中断' },
]
/** 当前展开完整问答的日志 id */
const expandedId = ref<number | null>(null)

const { list, loading, refreshing, finished, empty, refresh, loadMore } = usePageList<AiChatLogVO>(
  async (pageNo, pageSize) => {
    const res = await pageAiChatLog({
      pageNo,
      pageSize,
      username: keyword.value.trim() || undefined,
      chatStatus: statusMode.value === 'all' ? undefined : Number(statusMode.value),
    })
    total.value = res.data?.total || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

function statusLabel(status: number) {
  if (status === 1) return '成功'
  if (status === 2) return '中断'
  return '失败'
}

function formatDuration(ms: number) {
  if (!ms) return '0s'
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)}s` : `${ms}ms`
}

function goDetail(item: AiChatLogVO) {
  uni.setStorageSync('ai_log_detail_cache', item)
  uni.navigateTo({ url: '/pages-sub/ai/log/detail' })
}

function onItemMenu(item: AiChatLogVO) {
  if (!canDelete.value) return
  uni.showActionSheet({
    itemList: ['删除该记录'],
    success: async () => {
      const res = await showConfirm({
        title: '删除日志',
        content: '确定要删除这条对话记录？此操作不可撤销！',
        confirmText: '删除',
        cancelText: '取消',
        tone: 'danger',
      })
      if (!res.confirmed) return
      await deleteAiChatLog(item.id)
      uni.showToast({ title: '已删除', icon: 'success' })
      await refresh({ silent: true })
    },
  })
}

function onSearch() {
  refresh({ silent: true })
}

async function onRefresh() {
  await refresh()
}

watch(statusMode, () => refresh({ silent: true }))

onMounted(() => refresh())
onPullDownRefresh(async () => {
  try {
    await refresh()
  } finally {
    uni.stopPullDownRefresh()
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/mine.scss' as *;

.ailog-page {
  @include mine-page-bg;
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* #ifdef H5 */
  height: calc(100vh - var(--window-top, 0px));
  /* #endif */
  box-sizing: border-box;
  overflow: hidden;
  padding: $page-padding-y $page-padding-x 0;
}

.ailog-page__scroll {
  flex: 1;
  min-height: 0;
  margin-top: 8rpx;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }
}

.ailog-card {
  padding: 28rpx 24rpx;
  margin-bottom: $card-gap;
  border-radius: 24rpx;
  background: #ffffff;
  border: 1rpx solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 4rpx 20rpx rgba(15, 23, 42, 0.04);
}

.ailog-card__head {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
  background: #f1f5f9;
  flex-shrink: 0;

  .user-icon {
    font-size: 20rpx;
  }

  .user-name {
    font-size: 24rpx;
    font-weight: 700;
    color: #0f172a;
  }
}

.model-pill {
  flex: 1;
  min-width: 0;
  font-size: 22rpx;
  color: #6366f1;
  font-family: monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-pill {
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 600;
  flex-shrink: 0;

  &.is-success {
    background: #f0fdf4;
    color: #16a34a;
    border: 1rpx solid #bbf7d0;
  }

  &.is-warn {
    background: #fffbeb;
    color: #d97706;
    border: 1rpx solid #fef3c7;
  }

  &.is-error {
    background: #fef2f2;
    color: #dc2626;
    border: 1rpx solid #fecaca;
  }
}

.ailog-card__qa-box {
  margin-top: 18rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  border: 1rpx solid rgba(226, 232, 240, 0.8);
}

.qa-item {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}

.qa-tag {
  width: 32rpx;
  height: 32rpx;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 800;
  flex-shrink: 0;
  margin-top: 4rpx;

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

.qa-text {
  flex: 1;
  min-width: 0;
  font-size: 25rpx;
  line-height: 1.6;
  word-break: break-word;

  &--q {
    color: #0f172a;
    font-weight: 600;
  }

  &--a {
    color: #475569;

    &.is-error {
      color: #dc2626;
    }
  }

  &.is-collapsed {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
  }
}

.detail-link {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6rpx;
  margin-top: 6rpx;

  text {
    font-size: 21rpx;
    color: #6366f1;
    font-weight: 600;
  }

  .arrow {
    font-size: 24rpx;
  }
}

.ailog-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12rpx;
  margin-top: 18rpx;
}

.meta-pill {
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  font-size: 21rpx;
  background: #f1f5f9;
  color: #64748b;
}

.meta-time {
  margin-left: auto;
  font-size: 21rpx;
  color: #94a3b8;
}
</style>
