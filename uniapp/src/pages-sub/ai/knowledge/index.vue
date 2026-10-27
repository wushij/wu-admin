<template>
  <PermissionBlock v-if="!canAccess" />
  <view v-else class="knowledge-page">
    <ModuleDarkHero
      title="AI 知识库"
      :subtitle="`智能问答知识条目维护 · 共 ${total} 条`"
      icon="notes-o"
      theme="ai"
      :count="stats.total"
      count-label="知识"
    />

    <StatGrid :items="statItems" :columns="4" />

    <SearchBar v-model="filters.keyword" placeholder="搜索标题或内容关键词" @search="applyFilters" />

    <view class="knowledge-filter-row">
      <view class="knowledge-filter-pill" @click="pickCategory">
        <text class="knowledge-filter-pill__label">分类</text>
        <text class="knowledge-filter-pill__value">{{ categoryFilterLabel }}</text>
        <text class="knowledge-filter-pill__arrow">›</text>
      </view>
      <view class="knowledge-filter-pill" @click="pickStatus">
        <text class="knowledge-filter-pill__label">状态</text>
        <text class="knowledge-filter-pill__value">{{ statusFilterLabel }}</text>
        <text class="knowledge-filter-pill__arrow">›</text>
      </view>
    </view>

    <view class="knowledge-toolbar">
      <text class="knowledge-toolbar__title">知识条目</text>
      <button v-if="canQuery" class="knowledge-test-btn" @click="openTest">检索演练</button>
    </view>

    <ListLoading v-if="loading && !list.length" />

    <scroll-view v-else scroll-y class="knowledge-page__scroll" @scrolltolower="loadMore">
      <view
        v-for="item in list"
        :key="item.id"
        class="knowledge-card card--elevated"
        :class="{ 'is-disabled': item.status === 0 }"
      >
        <view class="knowledge-card__head">
          <view class="knowledge-card__title-wrap">
            <text class="knowledge-card__title">{{ item.title }}</text>
            <view class="knowledge-card__tags">
              <DictTag :label="categoryLabel(item.category)" :effect="categoryTone(item.category)" />
              <DictTag :label="item.status === 1 ? '已启用' : '已停用'" :effect="item.status === 1 ? 'success' : 'default'" />
            </view>
          </view>
          <switch
            :checked="item.status === 1"
            color="#6366f1"
            :disabled="!canUpdate"
            @change="toggleStatus(item, $event)"
          />
        </view>

        <text v-if="item.keywords" class="knowledge-card__keywords">关键词：{{ item.keywords }}</text>
        <text class="knowledge-card__content">{{ contentSnippet(item.content) }}</text>

        <view class="knowledge-card__meta">
          <text>{{ item.updater || item.creator || '—' }}</text>
          <text>{{ formatDateTime(item.updateTime || item.createTime, true) }}</text>
        </view>

        <view class="knowledge-card__actions">
          <button v-if="canUpdate" class="outline-btn outline-btn--primary" @click="goEdit(item.id)">编辑</button>
          <button v-if="canDelete" class="outline-btn outline-btn--danger" @click="handleDelete(item)">删除</button>
        </view>
      </view>

      <EmptyState v-if="empty" title="暂无知识条目" icon="notes-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreate" />

    <!-- 检索演练 -->
    <view v-if="testVisible" class="knowledge-modal" @click="testVisible = false">
      <view class="knowledge-modal__card" @click.stop>
        <text class="knowledge-modal__title">知识库检索演练</text>

        <input
          v-model="testQuery"
          class="knowledge-input"
          placeholder="输入用户可能的提问，如：怎么修改密码"
          confirm-type="search"
          @confirm="runTest"
        />
        <button class="knowledge-modal__btn" :loading="testing" @click="runTest">开始检索</button>

        <template v-if="testResult">
          <view class="knowledge-test__summary">
            <text>命中方式：{{ testResult.matchMode }}</text>
            <text>召回 {{ testResult.hitList.length }} 条 · 约 {{ testResult.estimatedTokens }} Tokens</text>
          </view>
          <view v-if="testResult.searchTerms.length" class="knowledge-test__terms">
            <text v-for="term in testResult.searchTerms" :key="term" class="knowledge-test__term">{{ term }}</text>
          </view>

          <scroll-view scroll-y class="knowledge-test__hits">
            <view v-for="hit in testResult.hitList" :key="hit.id" class="knowledge-test__hit">
              <text class="knowledge-test__hit-title">{{ hit.title }}</text>
              <text class="knowledge-test__hit-content">{{ contentSnippet(hit.content, 120) }}</text>
            </view>
            <EmptyState v-if="!testResult.hitList.length" title="没有召回结果" icon="search" />
          </scroll-view>

          <text v-if="testResult.referenceBlock" class="knowledge-test__ref-label">注入提示词片段</text>
          <scroll-view v-if="testResult.referenceBlock" scroll-y class="knowledge-test__ref">
            <text>{{ testResult.referenceBlock }}</text>
          </scroll-view>
        </template>

        <view class="knowledge-modal__actions">
          <button class="knowledge-action-btn" @click="testVisible = false">关闭</button>
        </view>
      </view>
    </view>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/layout/ModuleDarkHero/index.vue'
import SearchBar from '@/components/base/SearchBar/index.vue'
import ListFooter from '@/components/base/ListFooter/index.vue'
import ListLoading from '@/components/base/ListLoading/index.vue'
import EmptyState from '@/components/base/EmptyState/index.vue'
import DictTag from '@/components/base/DictTag/index.vue'
import FabButton from '@/components/base/FabButton/index.vue'
import StatGrid from '@/components/base/StatGrid/index.vue'
import type { StatItem } from '@/components/base/StatGrid/index.vue'
import PermissionBlock from '@/components/base/PermissionBlock/index.vue'
import AppDialogHost from '@/components/base/AppDialogHost/index.vue'
import { usePageList } from '@/composables/base/usePageList'
import { useModulePermission } from '@/composables/base/useModulePermission'
import {
  pageAiKnowledge,
  deleteAiKnowledge,
  updateAiKnowledgeStatus,
  getAiKnowledgeStats,
  testRetrieveAiKnowledge,
  type AiKnowledgeVO,
  type AiKnowledgeTestRetrieveResult,
  type AiKnowledgeStats,
} from '@/api/system/ai-knowledge'
import { formatDateTime } from '@/utils/display/format'
import { showConfirm, showActionSheet } from '@/utils/app-dialog'
import { showSuccessToast } from '@/utils/app-toast'

const { allowed, hasPerm } = useModulePermission('system:ai-knowledge:list')
// 与 PC 一致：列表权限或查询权限任一命中即可进入
const canQuery = computed(() => hasPerm('system:ai-knowledge:query'))
const canAccess = computed(() => allowed.value || canQuery.value)
const canCreate = computed(() => hasPerm('system:ai-knowledge:create'))
const canUpdate = computed(() => hasPerm('system:ai-knowledge:update'))
const canDelete = computed(() => hasPerm('system:ai-knowledge:delete'))

const stats = reactive<AiKnowledgeStats>({
  total: 0,
  activeCount: 0,
  disabledCount: 0,
  totalWords: 0,
})
const statItems = computed<StatItem[]>(() => [
  { label: '知识总数', value: stats.total, tone: 'primary' },
  { label: '已启用', value: stats.activeCount, tone: 'success' },
  { label: '已停用', value: stats.disabledCount, tone: 'warning' },
  { label: '累计字数', value: stats.totalWords, tone: 'info' },
])

const filters = reactive<{
  keyword: string
  category: string
  status: number | null
}>({
  keyword: '',
  category: '',
  status: null,
})

const categoryOptions = [
  { label: '全部分类', value: '' },
  { label: '常见问题', value: 'faq' },
  { label: '操作手册', value: 'manual' },
  { label: '功能模块', value: 'module' },
  { label: '其他', value: 'other' },
]
const statusOptions: { label: string; value: number | null }[] = [
  { label: '全部状态', value: null },
  { label: '已启用', value: 1 },
  { label: '已停用', value: 0 },
]

const categoryFilterLabel = computed(
  () => categoryOptions.find((item) => item.value === filters.category)?.label || '全部分类',
)
const statusFilterLabel = computed(
  () => statusOptions.find((item) => item.value === filters.status)?.label || '全部状态',
)

const { list, loading, finished, empty, total, refresh, loadMore } = usePageList<AiKnowledgeVO>(
  async (pageNo, pageSize) => {
    const res = await pageAiKnowledge({
      pageNo,
      pageSize,
      keyword: filters.keyword.trim() || undefined,
      category: filters.category || undefined,
      status: filters.status,
    })
    return { list: res.data?.list || [], total: res.data?.total || 0 }
  },
)

const testVisible = ref(false)
const testQuery = ref('')
const testing = ref(false)
const testResult = ref<AiKnowledgeTestRetrieveResult | null>(null)

function categoryLabel(category?: string): string {
  return categoryOptions.find((item) => item.value === category)?.label || category || '未分类'
}

function categoryTone(category?: string): 'primary' | 'success' | 'warning' | 'default' {
  switch (category) {
    case 'faq':
      return 'success'
    case 'manual':
      return 'primary'
    case 'module':
      return 'warning'
    default:
      return 'default'
  }
}

function contentSnippet(content?: string, length = 80): string {
  const text = (content || '').replace(/\s+/g, ' ').trim()
  if (!text) return '暂无内容'
  return text.length > length ? `${text.slice(0, length)}…` : text
}

async function loadStats() {
  try {
    const res = await getAiKnowledgeStats()
    if (res.data) Object.assign(stats, res.data)
  } catch {
    /* 全局错误处理已提示 */
  }
}

async function loadList() {
  await refresh()
  await loadStats()
}

function applyFilters() {
  void loadList()
}

async function pickCategory() {
  try {
    const index = await showActionSheet({
      title: '选择知识分类',
      items: categoryOptions.map((item) => ({ label: item.label })),
    })
    const option = categoryOptions[index]
    if (!option) return
    filters.category = option.value
    await loadList()
  } catch {
    /* 取消选择 */
  }
}

async function pickStatus() {
  try {
    const index = await showActionSheet({
      title: '选择知识状态',
      items: statusOptions.map((item) => ({ label: item.label })),
    })
    const option = statusOptions[index]
    if (!option) return
    filters.status = option.value
    await loadList()
  } catch {
    /* 取消选择 */
  }
}

async function toggleStatus(item: AiKnowledgeVO, event: { detail: { value: boolean } }) {
  const next = event.detail.value ? 1 : 0
  try {
    await updateAiKnowledgeStatus(item.id, next)
    item.status = next
    showSuccessToast(next === 1 ? '已启用' : '已停用')
    await loadStats()
  } catch {
    /* 全局错误处理已提示 */
  }
}

async function handleDelete(item: AiKnowledgeVO) {
  const { confirmed } = await showConfirm({
    title: '删除知识',
    content: `确定删除「${item.title}」吗？`,
    confirmText: '删除',
    tone: 'danger',
  })
  if (!confirmed) return
  await deleteAiKnowledge(item.id)
  showSuccessToast('已删除')
  await loadList()
}

function goCreate() {
  uni.navigateTo({ url: '/pages-sub/ai/knowledge/edit' })
}

function goEdit(id: number) {
  uni.navigateTo({ url: `/pages-sub/ai/knowledge/edit?id=${id}` })
}

function openTest() {
  testVisible.value = true
  testResult.value = null
}

async function runTest() {
  const query = testQuery.value.trim()
  if (!query) {
    uni.showToast({ title: '请输入检索内容', icon: 'none' })
    return
  }
  testing.value = true
  try {
    const res = await testRetrieveAiKnowledge(query)
    testResult.value = res.data || null
  } finally {
    testing.value = false
  }
}

onShow(() => {
  void loadList()
})

onPullDownRefresh(async () => {
  await loadList()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.knowledge-page {
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

.knowledge-filter-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
}

.knowledge-filter-pill {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
  height: 68rpx;
  padding: 0 20rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-md;
  background: $color-bg-card;
  box-sizing: border-box;
}

.knowledge-filter-pill__label {
  flex-shrink: 0;
  font-size: 22rpx;
  color: $color-text-secondary;
}

.knowledge-filter-pill__value {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
  font-size: 24rpx;
  color: $color-text-primary;
}

.knowledge-filter-pill__arrow {
  flex-shrink: 0;
  font-size: 28rpx;
  line-height: 1;
  color: $color-text-placeholder;
}

.knowledge-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin: 18rpx 0 8rpx;
  padding: 0 8rpx;
}

.knowledge-toolbar__title {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.knowledge-test-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 52rpx;
  padding: 0 22rpx;
  margin: 0;
  font-size: 22rpx;
  color: $color-primary;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.28);
  border-radius: 999rpx;

  &::after {
    border: none;
  }
}

.knowledge-page__scroll {
  flex: 1;
  min-height: 0;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }
}

.knowledge-card {
  margin-top: 16rpx;
  padding: 24rpx;
}

.knowledge-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.knowledge-card__title-wrap {
  flex: 1;
  min-width: 0;
}

.knowledge-card__title {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  line-height: 1.45;
}

.knowledge-card__tags {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-top: 10rpx;
  flex-wrap: wrap;
}

.knowledge-card__keywords {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.knowledge-card__content {
  display: block;
  margin-top: 10rpx;
  font-size: $font-size-sm;
  color: $color-text-regular;
  line-height: 1.55;
}

.knowledge-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-top: 14rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}

.knowledge-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  margin-top: 14rpx;
}

.is-disabled {
  opacity: 0.7;
}

.knowledge-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  background: rgba(0, 0, 0, 0.45);
}

.knowledge-modal__card {
  width: 100%;
  max-height: 82vh;
  overflow-y: auto;
  padding: 32rpx 28rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
  box-sizing: border-box;
}

.knowledge-modal__title {
  display: block;
  margin-bottom: 22rpx;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  text-align: center;
  color: $color-text-primary;
}

.knowledge-input {
  width: 100%;
  height: 80rpx;
  padding: 0 20rpx;
  margin-bottom: 16rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-md;
  background: $color-bg-card;
  font-size: $font-size-sm;
  box-sizing: border-box;
}

.knowledge-modal__btn {
  height: 72rpx;
  line-height: 72rpx;
  border-radius: $radius-md;
  background: linear-gradient(135deg, #6366f1, #818cf8);
  color: #fff;
  font-size: $font-size-sm;
}

.knowledge-modal__btn::after {
  border: none;
}

.knowledge-test__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-top: 20rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.knowledge-test__terms {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 12rpx;
}

.knowledge-test__term {
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(99, 102, 241, 0.1);
  color: $color-primary;
  font-size: 20rpx;
}

.knowledge-test__hits {
  max-height: 360rpx;
  margin-top: 14rpx;
}

.knowledge-test__hit {
  padding: 16rpx 0;
  border-bottom: 1px solid $color-border-light;
}

.knowledge-test__hit-title {
  display: block;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.knowledge-test__hit-content {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.5;
}

.knowledge-test__ref-label {
  display: block;
  margin-top: 16rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.knowledge-test__ref {
  max-height: 220rpx;
  margin-top: 8rpx;
  padding: 14rpx;
  border-radius: $radius-sm;
  background: $color-bg-muted;
  font-size: 20rpx;
  color: $color-text-secondary;
  line-height: 1.5;
}

.knowledge-modal__actions {
  display: flex;
  margin-top: 24rpx;
}

.knowledge-action-btn {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  color: $color-text-regular;
  font-size: $font-size-sm;
}

.knowledge-action-btn::after {
  border: none;
}
</style>
