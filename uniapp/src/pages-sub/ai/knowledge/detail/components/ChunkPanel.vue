<template>
  <view class="chunk-panel">
    <view class="chunk-toolbar">
      <view class="chunk-toolbar__search">
        <input
          v-model="keyword"
          class="chunk-search"
          placeholder="搜索切片内容或标题"
          placeholder-class="chunk-search__placeholder"
          confirm-type="search"
          @confirm="applyFilters"
        />
        <view class="chunk-search__btn" @click="applyFilters">
          <text>搜索</text>
        </view>
      </view>
      <view class="chunk-filter-pill" @click="pickEmbedStatus">
        <text>{{ embedLabel }}</text>
        <text class="chunk-filter-pill__arrow">›</text>
      </view>
    </view>

    <ListLoading v-if="loading && !list.length" />

    <scroll-view v-else scroll-y class="chunk-list" @scrolltolower="loadMore">
      <view v-for="item in list" :key="item.id" class="chunk-card card--elevated">
        <view class="chunk-card__head">
          <view class="chunk-card__title-wrap">
            <text class="chunk-card__doc">{{ item.documentName || '未命名文档' }}</text>
            <text v-if="item.heading" class="chunk-card__heading">{{ item.heading }}</text>
          </view>
          <view class="tag" :class="embedMeta(item.embedStatus).cls">
            <text>{{ embedMeta(item.embedStatus).label }}</text>
          </view>
        </view>

        <text class="chunk-card__content" :class="{ 'is-expanded': expandedIds.has(item.id) }">
          {{ item.content }}
        </text>
        <view class="chunk-card__expand" @click.stop="toggleExpand(item.id)">
          <text>{{ expandedIds.has(item.id) ? '收起' : '展开全文' }}</text>
        </view>

        <view class="chunk-card__meta">
          <text class="chunk-card__meta-item">#{{ item.chunkIndex + 1 }}</text>
          <text class="chunk-card__meta-item">{{ item.charCount ?? 0 }} 字符</text>
          <text class="chunk-card__meta-item">≈{{ item.tokenEstimate ?? 0 }} tokens</text>
          <text v-if="item.embeddingDim" class="chunk-card__meta-item">{{ item.embeddingDim }} 维</text>
        </view>

        <view v-if="item.errorMsg" class="chunk-card__error">
          <text>{{ item.errorMsg }}</text>
        </view>

        <view class="chunk-card__foot">
          <view class="action-btn" @click.stop="handleReindex(item)">
            <text>重建向量</text>
          </view>
        </view>
      </view>

      <EmptyState
        v-if="empty"
        title="暂无切片"
        description="上传文档后系统会自动切片，切片是知识检索的最小单位"
        icon="records-o"
      />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ListFooter from '@/components/base/ListFooter/index.vue'
import ListLoading from '@/components/base/ListLoading/index.vue'
import EmptyState from '@/components/base/EmptyState/index.vue'
import { usePageList } from '@/composables/base/usePageList'
import { pageKnowledgeChunk, reindexKnowledgeChunk, type KnowledgeChunkVO } from '@/api/system/ai-knowledge-chunk'
import { showConfirm, showActionSheet } from '@/store/dialog'

const props = defineProps<{
  knowledgeBaseId: number
}>()

const emit = defineEmits<{
  (e: 'changed'): void
}>()

const keyword = ref('')
const embedStatus = ref('')
const expandedIds = ref<Set<number>>(new Set())

const embedOptions: { label: string; value: string }[] = [
  { label: '全部向量状态', value: '' },
  { label: '待向量化', value: 'PENDING' },
  { label: '已向量化', value: 'INDEXED' },
  { label: '向量化失败', value: 'FAILED' },
]
const embedLabel = computed(
  () => embedOptions.find((item) => item.value === embedStatus.value)?.label || '全部向量状态',
)

const { list, loading, finished, empty, refresh, loadMore } = usePageList<KnowledgeChunkVO>(
  async (pageNo, pageSize) => {
    const res = await pageKnowledgeChunk({
      pageNo,
      pageSize,
      knowledgeBaseId: props.knowledgeBaseId,
      keyword: keyword.value.trim() || undefined,
      embedStatus: embedStatus.value || undefined,
    })
    return { list: res.data?.list || [], total: res.data?.total || 0 }
  },
)

function applyFilters() {
  void refresh()
}

async function pickEmbedStatus() {
  try {
    const index = await showActionSheet({
      title: '向量状态',
      items: embedOptions.map((item) => ({ label: item.label })),
    })
    if (index < 0) return
    embedStatus.value = embedOptions[index].value
    applyFilters()
  } catch {
    /* 用户取消，忽略 */
  }
}

function embedMeta(value?: string): { label: string; cls: string } {
  switch (value) {
    case 'INDEXED':
      return { label: '已向量化', cls: 'is-ready' }
    case 'FAILED':
      return { label: '向量化失败', cls: 'is-failed' }
    default:
      return { label: '待向量化', cls: 'is-idle' }
  }
}

function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
}

async function handleReindex(item: KnowledgeChunkVO) {
  const res = await showConfirm({
    title: '重建切片向量',
    content: `将重新计算该切片的向量（#${item.chunkIndex + 1}）。`,
    confirmText: '重建',
  })
  if (!res.confirmed) return
  uni.showLoading({ title: '计算中…', mask: true })
  try {
    await reindexKnowledgeChunk(item.id)
    uni.showToast({ title: '已重建向量', icon: 'success' })
    void refresh({ silent: true })
    emit('changed')
  } finally {
    uni.hideLoading()
  }
}

onMounted(() => {
  void refresh()
})

defineExpose({ refresh })
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.chunk-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chunk-toolbar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin: 16rpx 0;
}

.chunk-toolbar__search {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
  height: 72rpx;
  padding: 0 8rpx 0 24rpx;
  border-radius: 999rpx;
  border: 1rpx solid #e8edf5;
  background: $color-bg-card;
}

.chunk-search {
  flex: 1;
  min-width: 0;
  font-size: 25rpx;
  color: $color-text-primary;
}

.chunk-search__placeholder {
  color: $color-text-placeholder;
}

.chunk-search__btn {
  flex-shrink: 0;
  padding: 8rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(99, 102, 241, 0.1);

  text {
    font-size: 22rpx;
    color: #6366f1;
    font-weight: 600;
  }
}

.chunk-filter-pill {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4rpx;
  height: 72rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  border: 1rpx solid #e8edf5;
  background: $color-bg-card;

  text {
    font-size: 21rpx;
    color: $color-text-secondary;
  }
}

.chunk-filter-pill__arrow {
  font-size: 26rpx;
  line-height: 1;
  color: $color-text-placeholder;
}

.chunk-list {
  flex: 1;
  min-height: 0;
}

.chunk-card {
  margin-bottom: 16rpx;
  padding: 24rpx;
  border-radius: 22rpx;
  background: $color-bg-card;
  border: 1rpx solid #eef2f8;
}

.chunk-card__head {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}

.chunk-card__title-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.chunk-card__doc {
  font-size: 25rpx;
  font-weight: 600;
  color: $color-text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chunk-card__heading {
  font-size: 22rpx;
  color: #6366f1;
}

.chunk-card__content {
  display: block;
  margin-top: 12rpx;
  font-size: 23rpx;
  line-height: 1.6;
  color: $color-text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;

  &.is-expanded {
    display: block;
    -webkit-line-clamp: unset;
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.chunk-card__expand {
  margin-top: 8rpx;
  text-align: right;

  text {
    font-size: 21rpx;
    color: #6366f1;
  }
}

.chunk-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 10rpx;
}

.chunk-card__meta-item {
  font-size: 20rpx;
  color: $color-text-placeholder;
}

.chunk-card__error {
  margin-top: 10rpx;
  padding: 10rpx 14rpx;
  border-radius: $radius-sm;
  background: rgba(220, 38, 38, 0.06);

  text {
    font-size: 20rpx;
    color: #dc2626;
  }
}

.chunk-card__foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 14rpx;
}

.tag {
  flex-shrink: 0;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;

  text {
    font-size: 19rpx;
    font-weight: 600;
  }

  &.is-ready {
    background: rgba(22, 163, 74, 0.1);

    text {
      color: #16a34a;
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

.action-btn {
  display: inline-flex;
  align-items: center;
  height: 56rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  border: 1rpx solid #e2e8f2;
  background: $color-bg-card;

  &:active {
    background: $color-bg-page;
  }

  text {
    font-size: 21rpx;
    color: $color-text-secondary;
    line-height: 1;
  }
}
</style>
