<template>
  <view class="detail-page">
    <view class="detail-hero-wrap">
      <ModuleDarkHero
        :title="detail?.name || '知识库详情'"
        :subtitle="heroSubtitle"
        icon="notes-o"
        theme="ai"
      >
        <template #aside>
          <view class="hero-status" :class="indexMeta.cls">
            <view class="hero-status__dot" />
            <text class="hero-status__text">{{ indexMeta.label }}</text>
          </view>
        </template>

        <template #extra>
          <view class="hero-spec">
            <IconFont name="cluster-o" :size="22" color="rgba(226, 232, 240, 0.88)" />
            <text class="hero-spec__model">{{ modelNameText }}</text>
            <text class="hero-spec__divider" aria-hidden="true">·</text>
            <text class="hero-spec__chunk">{{ chunkSpecText }}</text>
          </view>
        </template>
      </ModuleDarkHero>
    </view>

    <view class="detail-overview card--elevated">
      <view class="overview-metrics">
        <view class="overview-metric">
          <text class="overview-metric__num">{{ detail?.documentCount ?? 0 }}</text>
          <text class="overview-metric__label">文档</text>
        </view>
        <view class="overview-metric">
          <text class="overview-metric__num">{{ detail?.chunkCount ?? 0 }}</text>
          <text class="overview-metric__label">切片</text>
        </view>
        <view class="overview-metric">
          <text class="overview-metric__num is-accent">{{ detail?.indexedChunkCount ?? 0 }}</text>
          <text class="overview-metric__label">已向量化</text>
        </view>
        <view class="overview-metric">
          <text class="overview-metric__num">{{ formattedTokens }}</text>
          <text class="overview-metric__label">Token 估算</text>
        </view>
      </view>

      <view class="overview-actions">
        <view
          class="action-pill action-pill--primary"
          :class="{ 'is-loading': uploading }"
          @click="pickAndUpload"
        >
          <IconFont name="notes-o" :size="26" color="#ffffff" />
          <text class="action-pill__text">{{ uploading ? '上传处理中…' : '上传文档' }}</text>
        </view>

        <view
          class="action-pill action-pill--secondary"
          :class="{ 'is-loading': reindexing }"
          @click="handleReindexAll"
        >
          <IconFont name="chart-trending-o" :size="24" color="#475569" />
          <text class="action-pill__text">{{ reindexing ? '重建中…' : '重建索引' }}</text>
        </view>

        <view
          class="action-more"
          role="button"
          aria-label="更多操作"
          @click="openMoreActions"
        >
          <view class="action-more__dots">
            <view class="action-more__dot" />
            <view class="action-more__dot" />
            <view class="action-more__dot" />
          </view>
        </view>
      </view>
    </view>

    <view class="detail-tabs-wrap">
      <SegmentTabs v-model="activeTab" :tabs="tabs" compact />
    </view>

    <DocumentPanel
      v-if="activeTab === 'document'"
      ref="documentPanelRef"
      :knowledge-base-id="knowledgeBaseId"
      @changed="reloadDetail"
    />
    <ChunkPanel
      v-else-if="activeTab === 'chunk'"
      :knowledge-base-id="knowledgeBaseId"
      @changed="reloadDetail"
    />
    <RetrievalPanel v-else :knowledge-base-id="knowledgeBaseId" />

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/layout/ModuleDarkHero/index.vue'
import IconFont from '@/components/base/IconFont/index.vue'
import SegmentTabs from '@/components/base/SegmentTabs/index.vue'
import AppDialogHost from '@/components/base/AppDialogHost/index.vue'
import DocumentPanel from './components/DocumentPanel.vue'
import ChunkPanel from './components/ChunkPanel.vue'
import RetrievalPanel from './components/RetrievalPanel.vue'
import {
  getKnowledgeBase,
  getKnowledgeBaseChunkStats,
  deleteKnowledgeBase,
  type KnowledgeBaseVO,
  type KnowledgeChunkStatsVO,
} from '@/api/system/ai-knowledge-base'
import { reindexKnowledgeBase, uploadKnowledgeDocument } from '@/api/system/ai-knowledge-document'
import { showActionSheet, showConfirm } from '@/store/dialog'

const knowledgeBaseId = ref(0)
const detail = ref<KnowledgeBaseVO | null>(null)
const chunkStats = ref<KnowledgeChunkStatsVO | null>(null)
const activeTab = ref('document')
const uploading = ref(false)
const reindexing = ref(false)
const initialized = ref(false)
const documentPanelRef = ref<InstanceType<typeof DocumentPanel>>()

const tabs = [
  { key: 'document', label: '文档' },
  { key: 'chunk', label: '切片' },
  { key: 'retrieval', label: '检索测试' },
]

/**
 * 副标题仅展示用户自定义描述。
 * 空描述不展示，避免迁移脚本写入的技术说明占据 Hero 版面。
 */
const heroSubtitle = computed(() => detail.value?.description?.trim() || undefined)

const modelNameText = computed(
  () => detail.value?.embeddingModelName || '默认向量模型',
)

const chunkSpecText = computed(() => {
  const size = detail.value?.chunkSize ?? '—'
  const overlap = detail.value?.chunkOverlap ?? '—'
  return `切片 ${size} / 重叠 ${overlap}`
})

const formattedTokens = computed(() => {
  const value = chunkStats.value?.totalTokens ?? 0
  return value.toLocaleString('en-US')
})

const indexMeta = computed(() => {
  const status = detail.value?.indexStatus
  if (status === 'INDEXED') return { label: '已就绪', cls: 'is-ready' }
  if (status === 'INDEXING') return { label: '索引中', cls: 'is-running' }
  if (status === 'FAILED') return { label: '索引异常', cls: 'is-failed' }
  return { label: '待索引', cls: 'is-idle' }
})

onLoad((query) => {
  knowledgeBaseId.value = Number(query?.id || 0)
  if (!knowledgeBaseId.value) {
    uni.showToast({ title: '参数缺失', icon: 'none' })
    return
  }
  initialized.value = true
  void reloadDetail()
})

onShow(() => {
  if (!initialized.value || !knowledgeBaseId.value) return
  void reloadDetail()
})

async function reloadDetail() {
  try {
    const [detailRes, statsRes] = await Promise.all([
      getKnowledgeBase(knowledgeBaseId.value),
      getKnowledgeBaseChunkStats(knowledgeBaseId.value),
    ])
    detail.value = detailRes.data || null
    chunkStats.value = statsRes.data || null
  } catch {
    /* 错误提示由请求层统一处理 */
  }
}

function navigateBackFallback() {
  uni.navigateBack({
    fail: () => uni.redirectTo({ url: '/pages-sub/ai/knowledge/index' }),
  })
}

async function openMoreActions() {
  try {
    const index = await showActionSheet({
      title: detail.value?.name || '知识库操作',
      items: [
        { label: '编辑配置' },
        { label: '删除知识库', danger: true },
      ],
    })
    if (index === 0) {
      uni.navigateTo({ url: `/pages-sub/ai/knowledge/form?id=${knowledgeBaseId.value}` })
    } else if (index === 1) {
      await handleDelete()
    }
  } catch {
    /* 用户取消操作面板 */
  }
}

/** 选择文档并逐个上传（服务端自动解析、切片、向量化） */
async function pickAndUpload() {
  if (uploading.value) return

  const picked: Array<{ path: string; name: string }> = []

  // #ifdef H5
  try {
    const res = await uni.chooseFile({ count: 10, type: 'all' })
    const files = (res.tempFiles || []) as Array<{
      path?: string
      name?: string
      file?: File
    }>
    for (const file of files) {
      const path = (file.file || file.path || '') as unknown as string
      const name = file.name || file.file?.name || path.split('/').pop() || '未命名文档'
      picked.push({ path, name })
    }
  } catch {
    return
  }
  // #endif

  // #ifdef MP-WEIXIN
  try {
    const res = (await (uni as unknown as {
      chooseMessageFile: (options: {
        count: number
        type: string
      }) => Promise<{ tempFiles: Array<{ path: string; name: string }> }>
    }).chooseMessageFile({ count: 10, type: 'file' })) as {
      tempFiles: Array<{ path: string; name: string }>
    }
    for (const file of res.tempFiles || []) {
      picked.push({ path: file.path, name: file.name })
    }
  } catch {
    return
  }
  // #endif

  if (picked.length === 0) return

  uploading.value = true
  let successCount = 0
  const failures: string[] = []
  for (const file of picked) {
    try {
      const res = await uploadKnowledgeDocument(knowledgeBaseId.value, file.path)
      const result = res.data?.[0]
      if (result?.success === false) {
        failures.push(`${file.name}：${result.message || '处理失败'}`)
      } else {
        successCount += 1
      }
    } catch (error) {
      failures.push(`${file.name}：${error instanceof Error ? error.message : '上传失败'}`)
    }
  }
  uploading.value = false

  if (failures.length > 0) {
    const summary = `成功 ${successCount} 个，失败 ${failures.length} 个\n${failures.slice(0, 3).join('\n')}`
    await showConfirm({
      title: '部分文档处理失败',
      content: summary,
      confirmText: '知道了',
      showCancel: false,
    })
  } else {
    uni.showToast({ title: `已上传 ${successCount} 个文档`, icon: 'success' })
  }

  activeTab.value = 'document'
  void reloadDetail()
  await nextTick()
  documentPanelRef.value?.refresh({ silent: true })
}

async function handleReindexAll() {
  if (reindexing.value) return
  const res = await showConfirm({
    title: '重建整个知识库索引',
    content: '将重新切片并向量化该知识库下的全部文档，耗时较长，期间检索结果可能不完整。',
    confirmText: '开始重建',
  })
  if (!res.confirmed) return
  reindexing.value = true
  try {
    await reindexKnowledgeBase(knowledgeBaseId.value)
    uni.showToast({ title: '已提交重建任务', icon: 'success' })
    void reloadDetail()
  } finally {
    reindexing.value = false
  }
}

async function handleDelete() {
  const res = await showConfirm({
    title: '删除知识库',
    content: `确定删除「${detail.value?.name || '该知识库'}」吗？全部文档与向量切片将一并永久删除，且无法恢复。`,
    confirmText: '永久删除',
    tone: 'danger',
  })
  if (!res.confirmed) return
  await deleteKnowledgeBase(knowledgeBaseId.value)
  uni.showToast({ title: '已删除', icon: 'success' })
  setTimeout(() => navigateBackFallback(), 400)
}
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.detail-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* #ifdef H5 */
  height: calc(100vh - var(--window-top, 0px));
  /* #endif */
  box-sizing: border-box;
  overflow: hidden;
  padding: $page-padding-y $page-padding-x 0;
  background: $color-bg-page;
}

.detail-hero-wrap {
  flex-shrink: 0;

  :deep(.module-dark-hero) {
    margin-bottom: 16rpx;
  }

  :deep(.module-dark-hero__body) {
    padding: 26rpx 24rpx;
    gap: 20rpx;
  }

  :deep(.module-dark-hero__sub) {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.hero-status {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  height: 44rpx;
  padding: 0 16rpx;
  border-radius: $radius-full;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);

  &__dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    background: #94a3b8;
  }

  &__text {
    font-size: 20rpx;
    font-weight: $font-weight-semibold;
    color: #e2e8f0;
    line-height: 1;
    white-space: nowrap;
  }

  &.is-ready {
    background: rgba(34, 197, 94, 0.16);
    border-color: rgba(74, 222, 128, 0.3);

    .hero-status__dot {
      background: #4ade80;
    }

    .hero-status__text {
      color: #bbf7d0;
    }
  }

  &.is-running {
    background: rgba(245, 158, 11, 0.18);
    border-color: rgba(251, 191, 36, 0.3);

    .hero-status__dot {
      background: #fbbf24;
    }

    .hero-status__text {
      color: #fde68a;
    }
  }

  &.is-failed {
    background: rgba(239, 68, 68, 0.18);
    border-color: rgba(248, 113, 113, 0.3);

    .hero-status__dot {
      background: #f87171;
    }

    .hero-status__text {
      color: #fecaca;
    }
  }
}

.hero-spec {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  max-width: 100%;
  min-width: 0;
  padding: 6rpx 16rpx;
  box-sizing: border-box;
  border-radius: $radius-full;
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(255, 255, 255, 0.12);

  &__model {
    min-width: 0;
    font-size: 20rpx;
    color: rgba(226, 232, 240, 0.86);
    line-height: 1.2;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__divider {
    flex-shrink: 0;
    font-size: 20rpx;
    color: rgba(226, 232, 240, 0.5);
    line-height: 1.2;
  }

  &__chunk {
    flex-shrink: 0;
    font-size: 20rpx;
    color: rgba(226, 232, 240, 0.86);
    line-height: 1.2;
    white-space: nowrap;
  }
}

.detail-overview {
  flex-shrink: 0;
  margin-bottom: 16rpx;
  padding: 20rpx 20rpx 18rpx;
  background: $color-bg-card;
}

.overview-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  align-items: center;
  padding-bottom: 18rpx;
  border-bottom: 1px solid $color-border-light;
}

.overview-metric {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  padding: 2rpx 6rpx;

  &:not(:last-child)::after {
    content: '';
    position: absolute;
    right: 0;
    top: 16%;
    bottom: 16%;
    width: 1px;
    background: $color-border-light;
  }
}

.overview-metric__num {
  font-size: 32rpx;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;

  &.is-accent {
    color: $color-primary;
  }
}

.overview-metric__label {
  font-size: 20rpx;
  color: $color-text-secondary;
  line-height: 1.2;
}

.overview-actions {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
}

.action-pill {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  height: 68rpx;
  padding: 0 24rpx;
  border-radius: $radius-full;
  box-sizing: border-box;
  transition: transform 150ms cubic-bezier(0.16, 1, 0.3, 1), opacity 150ms ease;

  &:active {
    transform: scale(0.98);
    opacity: 0.9;
  }

  &.is-loading {
    opacity: 0.65;
  }

  &__text {
    font-size: 24rpx;
    font-weight: $font-weight-semibold;
    line-height: 1;
    white-space: nowrap;
  }

  &--primary {
    flex: 1.3;
    background: linear-gradient(135deg, $color-primary 0%, #6366f1 100%);
    box-shadow: 0 8rpx 20rpx rgba(79, 70, 229, 0.24);

    .action-pill__text {
      color: $color-text-inverse;
    }
  }

  &--secondary {
    flex: 1;
    background: $color-bg-muted;
    border: 1px solid $color-border;

    .action-pill__text {
      color: $color-text-primary;
    }
  }
}

.action-more {
  flex-shrink: 0;
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background: $color-bg-muted;
  border: 1px solid $color-border;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  transition: transform 150ms cubic-bezier(0.16, 1, 0.3, 1), background-color 150ms ease;

  &:active {
    transform: scale(0.96);
    background: $color-border-light;
  }

  &__dots {
    display: flex;
    align-items: center;
    gap: 5rpx;
  }

  &__dot {
    width: 6rpx;
    height: 6rpx;
    border-radius: 50%;
    background: $color-text-secondary;
  }
}

.detail-tabs-wrap {
  flex-shrink: 0;
}

@media (prefers-reduced-motion: reduce) {
  .action-pill,
  .action-more {
    transition: none;
  }
}
</style>
