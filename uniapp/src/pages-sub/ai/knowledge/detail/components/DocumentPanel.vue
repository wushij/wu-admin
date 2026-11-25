<template>
  <view class="doc-panel">
    <view class="doc-toolbar">
      <view class="doc-toolbar__search">
        <input
          v-model="keyword"
          class="doc-search"
          placeholder="搜索文档名称"
          placeholder-class="doc-search__placeholder"
          confirm-type="search"
          @confirm="applyFilters"
        />
        <view class="doc-search__btn" @click="applyFilters">
          <text>搜索</text>
        </view>
      </view>
      <view class="doc-filter-pill" @click="pickParseStatus">
        <text>{{ parseLabel }}</text>
        <text class="doc-filter-pill__arrow">›</text>
      </view>
    </view>

    <ListLoading v-if="loading && !list.length" />

    <scroll-view v-else scroll-y class="doc-list" @scrolltolower="loadMore">
      <view v-for="item in list" :key="item.id" class="doc-card card--elevated">
        <view class="doc-card__head">
          <view class="doc-card__title-wrap">
            <text class="doc-card__name">{{ item.fileName }}</text>
            <view class="doc-card__tags">
              <view class="tag" :class="parseMeta(item.parseStatus).cls">
                <text>{{ parseMeta(item.parseStatus).label }}</text>
              </view>
              <view class="tag" :class="indexMeta(item.indexStatus).cls">
                <text>{{ indexMeta(item.indexStatus).label }}</text>
              </view>
              <view v-if="item.status === 0" class="tag is-idle">
                <text>已停用</text>
              </view>
            </view>
          </view>
        </view>

        <view class="doc-card__meta">
          <text class="doc-card__meta-item">{{ formatSize(item.fileSize) }}</text>
          <text class="doc-card__meta-item">{{ item.charCount ?? 0 }} 字符</text>
          <text class="doc-card__meta-item">{{ item.chunkCount ?? 0 }} 切片</text>
          <text class="doc-card__meta-item">{{ formatTime(item.createTime) }}</text>
        </view>

        <view v-if="item.errorMsg" class="doc-card__error">
          <text>{{ item.errorMsg }}</text>
        </view>

        <view class="doc-card__foot">
          <view class="action-btn" @click.stop="handleReparse(item)">
            <text>重新解析</text>
          </view>
          <view class="action-btn" @click.stop="handleReindex(item)">
            <text>重建向量</text>
          </view>
          <view class="action-btn" @click.stop="handleToggleStatus(item)">
            <text>{{ item.status === 1 ? '停用' : '启用' }}</text>
          </view>
          <view class="action-btn action-btn--danger" @click.stop="handleDelete(item)">
            <text>删除</text>
          </view>
        </view>
      </view>

      <EmptyState
        v-if="empty"
        title="暂无文档"
        description="上传 PDF、Word、Markdown 或代码文件，系统会自动解析并切片"
        icon="notes-o"
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
import {
  pageKnowledgeDocument,
  reparseKnowledgeDocument,
  reindexKnowledgeDocument,
  updateKnowledgeDocumentStatus,
  deleteKnowledgeDocument,
  type KnowledgeDocumentVO,
} from '@/api/system/ai-knowledge-document'
import { showConfirm, showActionSheet } from '@/store/dialog'
import { formatDateTime } from '@/utils/display/format'

const props = defineProps<{
  knowledgeBaseId: number
}>()

const emit = defineEmits<{
  (e: 'changed'): void
}>()

const keyword = ref('')
const parseStatus = ref('')
const parseOptions: { label: string; value: string }[] = [
  { label: '全部解析状态', value: '' },
  { label: '待解析', value: 'PENDING' },
  { label: '解析中', value: 'PARSING' },
  { label: '解析成功', value: 'SUCCESS' },
  { label: '解析失败', value: 'FAILED' },
]
const parseLabel = computed(
  () => parseOptions.find((item) => item.value === parseStatus.value)?.label || '全部解析状态',
)

const { list, loading, finished, empty, refresh, loadMore } = usePageList<KnowledgeDocumentVO>(
  async (pageNo, pageSize) => {
    const res = await pageKnowledgeDocument({
      pageNo,
      pageSize,
      knowledgeBaseId: props.knowledgeBaseId,
      fileName: keyword.value.trim() || undefined,
      parseStatus: parseStatus.value || undefined,
    })
    return { list: res.data?.list || [], total: res.data?.total || 0 }
  },
)

function applyFilters() {
  void refresh()
}

async function pickParseStatus() {
  try {
    const index = await showActionSheet({
      title: '解析状态',
      items: parseOptions.map((item) => ({ label: item.label })),
    })
    if (index < 0) return
    parseStatus.value = parseOptions[index].value
    applyFilters()
  } catch {
    /* 用户取消，忽略 */
  }
}

function parseMeta(value?: string): { label: string; cls: string } {
  switch (value) {
    case 'SUCCESS':
      return { label: '已解析', cls: 'is-ready' }
    case 'PARSING':
      return { label: '解析中', cls: 'is-running' }
    case 'FAILED':
      return { label: '解析失败', cls: 'is-failed' }
    default:
      return { label: '待解析', cls: 'is-idle' }
  }
}

function indexMeta(value?: string): { label: string; cls: string } {
  switch (value) {
    case 'INDEXED':
      return { label: '已向量化', cls: 'is-ready' }
    case 'INDEXING':
      return { label: '向量化中', cls: 'is-running' }
    case 'PARTIAL':
      return { label: '部分失败', cls: 'is-warn' }
    case 'FAILED':
      return { label: '向量化失败', cls: 'is-failed' }
    default:
      return { label: '待向量化', cls: 'is-idle' }
  }
}

function formatSize(bytes?: number): string {
  if (typeof bytes !== 'number' || !Number.isFinite(bytes) || bytes <= 0) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatTime(value?: string): string {
  return value ? formatDateTime(value) : '—'
}

async function handleReparse(item: KnowledgeDocumentVO) {
  const res = await showConfirm({
    title: '重新解析文档',
    content: `将重新解析「${item.fileName}」并重建切片，原有切片会被替换。`,
    confirmText: '重新解析',
  })
  if (!res.confirmed) return
  uni.showLoading({ title: '解析中…', mask: true })
  try {
    await reparseKnowledgeDocument(item.id)
    uni.showToast({ title: '已提交解析任务', icon: 'success' })
    void refresh({ silent: true })
    emit('changed')
  } finally {
    uni.hideLoading()
  }
}

async function handleReindex(item: KnowledgeDocumentVO) {
  const res = await showConfirm({
    title: '重建文档向量',
    content: `将重新切片并向量化「${item.fileName}」，耗时取决于文档长度。`,
    confirmText: '开始重建',
  })
  if (!res.confirmed) return
  uni.showLoading({ title: '向量化中…', mask: true })
  try {
    await reindexKnowledgeDocument(item.id)
    uni.showToast({ title: '已提交向量化任务', icon: 'success' })
    void refresh({ silent: true })
    emit('changed')
  } finally {
    uni.hideLoading()
  }
}

async function handleToggleStatus(item: KnowledgeDocumentVO) {
  const nextStatus = item.status === 1 ? 0 : 1
  try {
    await updateKnowledgeDocumentStatus(item.id, nextStatus)
    uni.showToast({ title: nextStatus === 1 ? '已启用' : '已停用', icon: 'success' })
    void refresh({ silent: true })
  } catch {
    /* 错误提示由请求层统一处理 */
  }
}

async function handleDelete(item: KnowledgeDocumentVO) {
  const res = await showConfirm({
    title: '删除文档',
    content: `确定删除「${item.fileName}」吗？该文档的全部切片与向量将一并永久删除。`,
    confirmText: '永久删除',
    tone: 'danger',
  })
  if (!res.confirmed) return
  await deleteKnowledgeDocument(item.id)
  uni.showToast({ title: '已删除', icon: 'success' })
  void refresh({ silent: true })
  emit('changed')
}

onMounted(() => {
  void refresh()
})

defineExpose({ refresh })
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.doc-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.doc-toolbar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin: 16rpx 0;
}

.doc-toolbar__search {
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

.doc-search {
  flex: 1;
  min-width: 0;
  font-size: 25rpx;
  color: $color-text-primary;
}

.doc-search__placeholder {
  color: $color-text-placeholder;
}

.doc-search__btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 56rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(99, 102, 241, 0.08);

  text {
    font-size: 22rpx;
    font-weight: 600;
    color: #6366f1;
  }

  &:active {
    background: rgba(99, 102, 241, 0.16);
  }
}

.doc-filter-pill {
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

.doc-filter-pill__arrow {
  font-size: 26rpx;
  line-height: 1;
  color: $color-text-placeholder;
}

.doc-list {
  flex: 1;
  min-height: 0;
}

.doc-card {
  margin-bottom: 16rpx;
  padding: 24rpx;
  border-radius: 22rpx;
  background: $color-bg-card;
  border: 1rpx solid #eef2f8;
}

.doc-card__head {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}

.doc-card__title-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.doc-card__name {
  font-size: 27rpx;
  font-weight: 700;
  color: $color-text-primary;
  word-break: break-all;
}

.doc-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.tag {
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

  &.is-running {
    background: rgba(217, 119, 6, 0.12);

    text {
      color: #d97706;
    }
  }

  &.is-warn {
    background: rgba(234, 88, 12, 0.1);

    text {
      color: #ea580c;
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

.doc-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 12rpx;
}

.doc-card__meta-item {
  font-size: 20rpx;
  color: $color-text-placeholder;
}

.doc-card__error {
  margin-top: 12rpx;
  padding: 12rpx 16rpx;
  border-radius: $radius-sm;
  background: rgba(220, 38, 38, 0.06);

  text {
    font-size: 21rpx;
    line-height: 1.5;
    color: #dc2626;
  }
}

.doc-card__foot {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12rpx;
  margin-top: 16rpx;
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

  &--danger {
    border-color: rgba(239, 68, 68, 0.22);
    background: rgba(239, 68, 68, 0.04);

    text {
      color: #ef4444;
    }
  }
}
</style>
