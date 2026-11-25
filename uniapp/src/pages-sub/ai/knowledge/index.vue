<template>
  <PermissionBlock v-if="!canAccess" />
  <view v-else class="kb-page">
    <ModuleDarkHero
      title="AI 知识库"
      :subtitle="`共 ${total} 个知识库 · 自动切片与向量化`"
      icon="notes-o"
      theme="ai"
      :count="total"
      count-label="知识库"
    />

    <SearchBar v-model="keyword" placeholder="搜索知识库名称" @search="applyFilters" />

    <view class="kb-filter-row">
      <view class="kb-filter-pill" @click="pickStatus">
        <text class="kb-filter-pill__label">状态</text>
        <text class="kb-filter-pill__value">{{ statusLabel }}</text>
        <text class="kb-filter-pill__arrow">›</text>
      </view>
      <view class="kb-filter-pill" @click="pickIndexStatus">
        <text class="kb-filter-pill__label">索引</text>
        <text class="kb-filter-pill__value">{{ indexLabel }}</text>
        <text class="kb-filter-pill__arrow">›</text>
      </view>
    </view>

    <ListLoading v-if="loading && !list.length" />

    <scroll-view v-else scroll-y class="kb-page__scroll" @scrolltolower="loadMore">
      <view
        v-for="item in list"
        :key="item.id"
        class="kb-card card--elevated"
        :class="{ 'is-disabled': item.status === 0 }"
        @click="goDetail(item.id)"
      >
        <view class="kb-card__head">
          <view class="kb-card__title-wrap">
            <text class="kb-card__title">{{ item.name }}</text>
            <text v-if="item.description" class="kb-card__desc">{{ item.description }}</text>
          </view>
          <view class="kb-card__tags">
            <view class="status-tag" :class="item.status === 1 ? 'is-active' : 'is-inactive'">
              <text>{{ item.status === 1 ? '已启用' : '已停用' }}</text>
            </view>
            <view class="index-tag" :class="indexMeta(item.indexStatus).cls">
              <text>{{ indexMeta(item.indexStatus).label }}</text>
            </view>
          </view>
        </view>

        <view class="kb-card__stats">
          <view class="kb-stat">
            <text class="kb-stat__num">{{ item.documentCount ?? 0 }}</text>
            <text class="kb-stat__label">文档</text>
          </view>
          <view class="kb-stat">
            <text class="kb-stat__num">{{ item.chunkCount ?? 0 }}</text>
            <text class="kb-stat__label">切片</text>
          </view>
          <view class="kb-stat">
            <text class="kb-stat__num">{{ item.indexedChunkCount ?? 0 }}</text>
            <text class="kb-stat__label">已向量化</text>
          </view>
        </view>

        <view class="kb-card__meta">
          <text class="kb-card__meta-item">向量模型：{{ item.embeddingModelName || '默认向量模型' }}</text>
          <text class="kb-card__meta-item">
            切片 {{ item.chunkSize ?? '—' }} · 重叠 {{ item.chunkOverlap ?? '—' }}
          </text>
        </view>

        <view class="kb-card__foot">
          <view class="action-btn" @click.stop="goDetail(item.id)">
            <text>管理文档</text>
          </view>
          <view v-if="canUpdate" class="action-btn action-btn--primary" @click.stop="goEdit(item.id)">
            <text>编辑</text>
          </view>
          <view v-if="canDelete" class="action-btn action-btn--danger" @click.stop="handleDelete(item)">
            <text>删除</text>
          </view>
        </view>
      </view>

      <EmptyState
        v-if="empty"
        title="暂无知识库"
        description="新建知识库后即可上传文档，系统会自动解析、切片并向量化"
        icon="notes-o"
      />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreate" />
    <AiWuAssistant />
    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/layout/ModuleDarkHero/index.vue'
import SearchBar from '@/components/base/SearchBar/index.vue'
import ListFooter from '@/components/base/ListFooter/index.vue'
import ListLoading from '@/components/base/ListLoading/index.vue'
import EmptyState from '@/components/base/EmptyState/index.vue'
import FabButton from '@/components/base/FabButton/index.vue'
import PermissionBlock from '@/components/base/PermissionBlock/index.vue'
import AppDialogHost from '@/components/base/AppDialogHost/index.vue'
import AiWuAssistant from '@/components/business/ai/AiWuAssistant/index.vue'
import { usePageList } from '@/composables/base/usePageList'
import { useModulePermission } from '@/composables/base/useModulePermission'
import {
  pageKnowledgeBase,
  deleteKnowledgeBase,
  type KnowledgeBaseVO,
} from '@/api/system/ai-knowledge-base'
import { showConfirm, showActionSheet } from '@/store/dialog'

const { allowed, hasPerm } = useModulePermission('system:ai-knowledge:list')
const canQuery = computed(() => hasPerm('system:ai-knowledge:query'))
const canAccess = computed(() => allowed.value || canQuery.value)
const canCreate = computed(() => hasPerm('system:ai-knowledge:create'))
const canUpdate = computed(() => hasPerm('system:ai-knowledge:update'))
const canDelete = computed(() => hasPerm('system:ai-knowledge:delete'))

const keyword = ref('')
const status = ref<number | null>(null)
const indexStatus = ref('')

const statusOptions: { label: string; value: number | null }[] = [
  { label: '全部状态', value: null },
  { label: '已启用', value: 1 },
  { label: '已停用', value: 0 },
]
const indexOptions: { label: string; value: string }[] = [
  { label: '全部索引状态', value: '' },
  { label: '待索引', value: 'IDLE' },
  { label: '索引中', value: 'INDEXING' },
  { label: '已就绪', value: 'INDEXED' },
  { label: '索引异常', value: 'FAILED' },
]

const statusLabel = computed(
  () => statusOptions.find((item) => item.value === status.value)?.label || '全部状态',
)
const indexLabel = computed(
  () => indexOptions.find((item) => item.value === indexStatus.value)?.label || '全部索引状态',
)

const { list, loading, finished, empty, total, refresh, loadMore } = usePageList<KnowledgeBaseVO>(
  async (pageNo, pageSize) => {
    const res = await pageKnowledgeBase({
      pageNo,
      pageSize,
      name: keyword.value.trim() || undefined,
      status: status.value,
      indexStatus: indexStatus.value || undefined,
    })
    return { list: res.data?.list || [], total: res.data?.total || 0 }
  },
)

function applyFilters() {
  void refresh()
}

async function pickStatus() {
  try {
    const index = await showActionSheet({
      title: '知识库状态',
      items: statusOptions.map((item) => ({ label: item.label })),
    })
    if (index < 0) return
    status.value = statusOptions[index].value
    applyFilters()
  } catch {
    /* 用户取消，忽略 */
  }
}

async function pickIndexStatus() {
  try {
    const index = await showActionSheet({
      title: '索引状态',
      items: indexOptions.map((item) => ({ label: item.label })),
    })
    if (index < 0) return
    indexStatus.value = indexOptions[index].value
    applyFilters()
  } catch {
    /* 用户取消，忽略 */
  }
}

function indexMeta(value?: string): { label: string; cls: string } {
  switch (value) {
    case 'INDEXED':
      return { label: '已就绪', cls: 'is-ready' }
    case 'INDEXING':
      return { label: '索引中', cls: 'is-running' }
    case 'FAILED':
      return { label: '索引异常', cls: 'is-failed' }
    default:
      return { label: '待索引', cls: 'is-idle' }
  }
}

function goCreate() {
  uni.navigateTo({ url: '/pages-sub/ai/knowledge/form' })
}

function goEdit(id: number) {
  uni.navigateTo({ url: `/pages-sub/ai/knowledge/form?id=${id}` })
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages-sub/ai/knowledge/detail/index?id=${id}` })
}

async function handleDelete(item: KnowledgeBaseVO) {
  const res = await showConfirm({
    title: '删除知识库',
    content: `确定删除「${item.name}」吗？该知识库下的全部文档与向量切片将一并永久删除，且无法恢复。`,
    confirmText: '永久删除',
    tone: 'danger',
  })
  if (!res.confirmed) {
    return
  }
  await deleteKnowledgeBase(item.id)
  uni.showToast({ title: '已删除', icon: 'success' })
  void refresh({ silent: true })
}

onShow(() => {
  void refresh({ silent: true })
})

onPullDownRefresh(async () => {
  await refresh({ silent: true })
  uni.stopPullDownRefresh()
})

onMounted(() => {
  void refresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.kb-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* #ifdef H5 */
  height: calc(100vh - var(--window-top, 0px));
  /* #endif */
  box-sizing: border-box;
  overflow: hidden;
  padding: 0 $page-padding-x;
  background: $color-bg-page;
}

.kb-filter-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
}

.kb-filter-pill {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
  height: 72rpx;
  padding: 0 22rpx;
  border: 1rpx solid #e8edf5;
  border-radius: 999rpx;
  background: $color-bg-card;
  box-sizing: border-box;
}

.kb-filter-pill__label {
  flex-shrink: 0;
  font-size: 22rpx;
  color: $color-text-secondary;
}

.kb-filter-pill__value {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
  font-size: 24rpx;
  color: $color-text-primary;
}

.kb-filter-pill__arrow {
  flex-shrink: 0;
  font-size: 28rpx;
  line-height: 1;
  color: $color-text-placeholder;
}

.kb-page__scroll {
  flex: 1;
  min-height: 0;
  margin-top: 16rpx;
}

.kb-card {
  margin-bottom: 20rpx;
  padding: 28rpx;
  border-radius: 26rpx;
  background: $color-bg-card;
  border: 1rpx solid #eef2f8;

  &.is-disabled {
    opacity: 0.72;
  }
}

.kb-card__head {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}

.kb-card__title-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.kb-card__title {
  font-size: 31rpx;
  font-weight: 700;
  color: $color-text-primary;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-card__tags {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8rpx;
}

.status-tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;

  text {
    font-size: 20rpx;
    font-weight: 600;
  }

  &.is-active {
    background: rgba(22, 163, 74, 0.1);

    text {
      color: #16a34a;
    }
  }

  &.is-inactive {
    background: rgba(148, 163, 184, 0.14);

    text {
      color: #94a3b8;
    }
  }
}

.index-tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;

  text {
    font-size: 20rpx;
    font-weight: 600;
  }

  &.is-ready {
    background: rgba(22, 119, 255, 0.1);

    text {
      color: #1677ff;
    }
  }

  &.is-running {
    background: rgba(217, 119, 6, 0.12);

    text {
      color: #d97706;
    }
  }

  &.is-failed {
    background: rgba(220, 38, 38, 0.1);

    text {
      color: #dc2626;
    }
  }

  &.is-idle {
    background: rgba(100, 116, 139, 0.12);

    text {
      color: #64748b;
    }
  }
}

.kb-card__desc {
  margin-top: 2rpx;
  font-size: 23rpx;
  line-height: 1.55;
  color: $color-text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.kb-card__stats {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 22rpx;
  padding: 18rpx 0;
  border-top: 1rpx solid #eef2f8;
  border-bottom: 1rpx solid #eef2f8;
}

.kb-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
}

.kb-stat__num {
  font-size: 34rpx;
  font-weight: 700;
  color: $color-text-primary;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.kb-stat__label {
  font-size: 20rpx;
  color: $color-text-secondary;
}

.kb-card__meta {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  margin-top: 16rpx;
}

.kb-card__meta-item {
  font-size: 21rpx;
  color: $color-text-secondary;
}

.kb-card__foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12rpx;
  margin-top: 20rpx;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  height: 58rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  border: 1rpx solid #e2e8f2;
  background: $color-bg-card;

  &:active {
    background: $color-bg-page;
  }

  text {
    font-size: 22rpx;
    color: $color-text-secondary;
    line-height: 1;
  }

  &--primary {
    border-color: rgba(99, 102, 241, 0.28);
    background: rgba(99, 102, 241, 0.06);

    text {
      color: #6366f1;
      font-weight: 600;
    }
  }

  &--danger {
    border-color: rgba(239, 68, 68, 0.22);
    background: rgba(239, 68, 68, 0.04);

    text {
      color: #ef4444;
    }
  }
}
</style>
