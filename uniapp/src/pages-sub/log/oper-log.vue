<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list oper-log-page">
    <ModuleDarkHero
      title="操作日志"
      subtitle="记录系统操作行为，支持按模块、人员与状态检索"
      icon="edit"
      theme="log"
      :count="total"
      count-label="日志"
    />

    <scroll-view scroll-y class="page-list__scroll" @scrolltolower="loadMore">
      <FadeIn :show="hasFetched">
        <MonitorPanel title="筛选条件">
          <SearchBar
            v-model="filters.title"
            placeholder="模块名称"
            @search="applyFilters"
          />
          <view class="oper-log-search-gap">
            <SearchBar
              v-model="filters.operName"
              placeholder="操作人员"
              @search="applyFilters"
            />
          </view>

          <view class="filter-group">
            <text class="filter-group__label">操作状态</text>
            <view class="filter-toolbar">
              <view class="filter-toolbar__chips">
                <text
                  v-for="item in statusFilterOptions"
                  :key="String(item.value)"
                  class="filter-chip"
                  :class="{ 'filter-chip--active': filters.status === item.value }"
                  @click="setStatusFilter(item.value)"
                >
                  {{ item.label }}
                </text>
              </view>
              <view class="filter-toolbar__actions">
                <button class="filter-actions__btn" @click="resetFilters">重置</button>
                <button class="filter-actions__btn filter-actions__btn--primary" @click="applyFilters">
                  搜索
                </button>
              </view>
            </view>
          </view>
        </MonitorPanel>

        <MonitorPanel title="操作日志列表">
          <template #extra>
            <button
              v-if="canClear"
              class="oper-log-clear-btn"
              @click="confirmClean"
            >
              清空日志
            </button>
          </template>

          <ListCard v-for="item in list" :key="item.id" class="oper-log-card">
            <view class="oper-log-card__head">
              <view class="oper-log-card__tags">
                <DictTag
                  :label="businessTypeLabel(item.businessType)"
                  :effect="businessTypeTone(item.businessType)"
                />
                <DictTag
                  :label="item.status === 0 ? '正常' : '异常'"
                  :effect="item.status === 0 ? 'success' : 'danger'"
                />
              </view>
              <text v-if="item.requestMethod" class="oper-log-card__method" :class="requestMethodClass(item.requestMethod)">
                {{ item.requestMethod }}
              </text>
            </view>

            <text class="oper-log-card__title">{{ item.title || '—' }}</text>
            <text v-if="getActionSummary(item)" class="oper-log-card__summary">{{ getActionSummary(item) }}</text>

            <view class="oper-log-card__meta">
              <text>{{ item.operName || '—' }}</text>
              <text>{{ item.operIp || '—' }}</text>
              <text>{{ item.costTime ?? 0 }}ms</text>
            </view>

            <text class="oper-log-card__time">{{ formatDateTime(item.operTime, true) }}</text>

            <view class="oper-log-card__actions">
              <button class="outline-btn outline-btn--primary" @click="viewDetail(item)">详情</button>
              <button
                v-if="canDelete"
                class="outline-btn outline-btn--danger"
                @click="confirmDelete(item)"
              >
                删除
              </button>
            </view>
          </ListCard>

          <EmptyState v-if="!loading && !list.length" title="暂无操作日志" icon="edit" />
          <ListFooter
            v-else
            :loading="loading"
            :finished="finished"
            :empty="false"
          />
        </MonitorPanel>
      </FadeIn>
    </scroll-view>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import MonitorPanel from '@/components/common/MonitorPanel/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FadeIn from '@/components/common/FadeIn/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
import {
  businessTypeLabel,
  businessTypeTone,
  requestMethodClass,
  storeOperLogDetail,
} from '@/composables/useOperLog'
import { pageOperLog, deleteOperLog, cleanOperLog } from '@/api/system/oper-log'
import { formatDateTime } from '@/utils/format'
import { showConfirm } from '@/utils/app-dialog'
import { showSuccessToast } from '@/utils/app-toast'
import type { OperLogVO } from '@/types/system'

const { allowed, hasPerm } = useModulePermission('system:operLog:query')
const canDelete = computed(() => hasPerm('system:operLog:delete'))
const canClear = computed(() => hasPerm('system:operLog:clear'))

function getActionSummary(item: OperLogVO): string {
  if (!item.operParam) return ''
  try {
    const obj = JSON.parse(item.operParam)
    if (obj && typeof obj === 'object' && obj.action) {
      return String(obj.action)
    }
  } catch {}
  return ''
}

const total = ref(0)
const hasFetched = ref(false)

const filters = ref({
  title: '',
  operName: '',
  status: null as number | null,
})

const statusFilterOptions = [
  { label: '全部', value: null },
  { label: '正常', value: 0 },
  { label: '异常', value: 1 },
]

const { list, loading, finished, refresh, loadMore } = usePageList<OperLogVO>(
  async (pageNo, pageSize) => {
    const title = filters.value.title.trim()
    const operName = filters.value.operName.trim()
    const res = await pageOperLog({
      pageNo,
      pageSize,
      title: title || undefined,
      operName: operName || undefined,
      status: filters.value.status ?? undefined,
    })
    total.value = Number(res.data?.total) || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

function setStatusFilter(value: number | null) {
  filters.value.status = value
  applyFilters()
}

function applyFilters() {
  refresh()
}

function resetFilters() {
  filters.value = {
    title: '',
    operName: '',
    status: null,
  }
  refresh()
}

function viewDetail(row: OperLogVO) {
  storeOperLogDetail(row)
  uni.navigateTo({
    url: `/pages-sub/log/oper-log-detail?id=${row.id}`,
  })
}

async function confirmDelete(row: OperLogVO) {
  const { confirmed } = await showConfirm({
    title: '删除确认',
    content: '确定要删除该日志吗？',
    tone: 'danger',
    confirmText: '删除',
  })
  if (!confirmed) return
  await deleteOperLog(row.id)
  showSuccessToast('删除成功')
  await refresh()
}

async function confirmClean() {
  const { confirmed } = await showConfirm({
    title: '清空确认',
    content: '确定要清空所有操作日志吗？此操作不可恢复。',
    tone: 'danger',
    confirmText: '清空',
  })
  if (!confirmed) return
  await cleanOperLog()
  showSuccessToast('清空成功')
  await refresh()
}

async function onRefresh() {
  await refresh()
  hasFetched.value = true
}

onMounted(onRefresh)

onPullDownRefresh(async () => {
  await onRefresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.oper-log-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* #ifdef H5 */
  height: calc(100vh - var(--window-top, 0px));
  /* #endif */
  box-sizing: border-box;
  overflow: hidden;
}

.page-list__scroll {
  flex: 1;
  min-height: 0;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }
}

.oper-log-search-gap {
  margin-top: 16rpx;
}

.filter-group {
  margin-top: 20rpx;
}

.filter-group__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.filter-toolbar {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.filter-toolbar__chips {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.filter-toolbar__actions {
  flex-shrink: 0;
  display: flex;
  gap: 12rpx;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  background: $color-bg-muted;
  border: 1px solid $color-border-light;
}

.filter-chip--active {
  color: $color-primary;
  background: rgba(99, 102, 241, 0.1);
  border-color: rgba(99, 102, 241, 0.35);
  font-weight: $font-weight-semibold;
}

.filter-actions__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 56rpx;
  padding: 0 24rpx;
  margin: 0;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  background: $color-bg-muted;
  border: none;
  border-radius: 28rpx;
  white-space: nowrap;

  &::after {
    border: none;
  }
}

.filter-actions__btn--primary {
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #818cf8);
}

.oper-log-clear-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 56rpx;
  padding: 0 24rpx;
  margin: 0;
  font-size: 22rpx;
  color: $color-danger;
  background: rgba(245, 108, 108, 0.08);
  border: 1px solid rgba(245, 108, 108, 0.28);
  border-radius: 999rpx;

  &::after {
    border: none;
  }
}

.oper-log-card {
  margin-bottom: $card-gap;
}

.oper-log-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.oper-log-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.oper-log-card__method {
  flex-shrink: 0;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  font-weight: $font-weight-bold;
  color: $color-text-secondary;
  background: $color-bg-muted;
}

.oper-log__method--get {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.12);
}

.oper-log__method--post {
  color: #409eff;
  background: rgba(64, 158, 255, 0.12);
}

.oper-log__method--put {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.12);
}

.oper-log__method--delete {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.12);
}

.oper-log-card__title {
  display: block;
  margin-top: 16rpx;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  line-height: 1.45;
}

.oper-log-card__summary {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-sm;
  color: $color-primary;
  line-height: 1.4;
  word-break: break-all;
}

.oper-log-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx 20rpx;
  margin-top: 16rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.oper-log-card__time {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}

.oper-log-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1px solid $color-border-light;

  .outline-btn {
    min-width: auto;
    height: 56rpx;
    padding: 0 24rpx;
    font-size: 22rpx;
  }
}

.oper-log-page :deep(.monitor-panel) {
  margin-bottom: 24rpx;
}

.oper-log-page :deep(.monitor-panel__head) {
  margin-bottom: 24rpx;
}

.oper-log-page :deep(.search-bar) {
  margin-bottom: 0;
}
</style>
