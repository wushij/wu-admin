<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list login-log-page">
    <ModuleHero theme="log" title="登录日志" subtitle="记录用户登录行为，支持按账号、IP 与状态检索">
      <template #extra>
        <view class="login-log-hero-extra">
          <text class="login-log-hero-extra__num">{{ total }}</text>
          <text class="login-log-hero-extra__label">日志总数</text>
        </view>
      </template>
    </ModuleHero>

    <scroll-view scroll-y class="page-list__scroll" @scrolltolower="loadMore">
      <FadeIn :show="hasFetched">
        <MonitorPanel title="筛选条件">
          <SearchBar
            v-model="filters.username"
            placeholder="用户名"
            @search="applyFilters"
          />
          <view class="login-log-search-gap">
            <SearchBar
              v-model="filters.ipaddr"
              placeholder="IP 地址"
              @search="applyFilters"
            />
          </view>

          <view class="filter-group">
            <text class="filter-group__label">登录状态</text>
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

        <MonitorPanel title="登录日志列表">
          <template #extra>
            <button
              v-if="canClear"
              class="login-log-clear-btn"
              @click="confirmClean"
            >
              清空日志
            </button>
          </template>

          <ListCard v-for="item in list" :key="item.id" class="login-log-card">
            <view class="login-log-card__head">
              <text class="login-log-card__user">{{ item.username || '未知用户' }}</text>
              <DictTag
                :label="loginStatusLabel(item.status)"
                :effect="loginStatusEffect(item.status)"
              />
            </view>

            <text v-if="item.msg" class="login-log-card__msg">{{ item.msg }}</text>

            <view class="login-log-card__meta">
              <text>{{ item.ipaddr || '—' }}</text>
              <text>{{ item.loginLocation || '—' }}</text>
            </view>

            <view v-if="item.browser || item.os" class="login-log-card__client">
              <text>{{ item.browser || '—' }}</text>
              <text v-if="item.browser && item.os"> · </text>
              <text>{{ item.os || '' }}</text>
            </view>

            <text class="login-log-card__time">{{ formatDateTime(item.loginTime, true) }}</text>

            <view class="login-log-card__actions">
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

          <EmptyState v-if="!loading && !list.length" title="暂无登录日志" icon="contact-o" />
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
import ModuleHero from '@/components/common/ModuleHero/index.vue'
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
  loginStatusEffect,
  loginStatusLabel,
  storeLoginLogDetail,
} from '@/composables/useLoginLog'
import { getLoginLogList, deleteLoginLog, clearLoginLog } from '@/api/system/login-log'
import { formatDateTime } from '@/utils/format'
import { showConfirm } from '@/utils/app-dialog'
import { showSuccessToast } from '@/utils/app-toast'
import type { LoginLogVO } from '@/types/system'

const { allowed, hasPerm } = useModulePermission('system:loginLog:query')
const canDelete = computed(() => hasPerm('system:loginLog:delete'))
const canClear = computed(() => hasPerm('system:loginLog:clear'))

const total = ref(0)
const hasFetched = ref(false)

const filters = ref({
  username: '',
  ipaddr: '',
  status: null as number | null,
})

const statusFilterOptions = [
  { label: '全部', value: null },
  { label: '成功', value: 0 },
  { label: '失败', value: 1 },
]

const { list, loading, finished, refresh, loadMore } = usePageList<LoginLogVO>(
  async (pageNo, pageSize) => {
    const username = filters.value.username.trim()
    const ipaddr = filters.value.ipaddr.trim()
    const res = await getLoginLogList({
      pageNo,
      pageSize,
      username: username || undefined,
      ipaddr: ipaddr || undefined,
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
    username: '',
    ipaddr: '',
    status: null,
  }
  refresh()
}

function viewDetail(row: LoginLogVO) {
  storeLoginLogDetail(row)
  uni.navigateTo({
    url: `/pages-sub/log/login-log-detail?id=${row.id}`,
  })
}

async function confirmDelete(row: LoginLogVO) {
  const { confirmed } = await showConfirm({
    title: '删除确认',
    content: '确定要删除该日志吗？',
    tone: 'danger',
    confirmText: '删除',
  })
  if (!confirmed) return
  await deleteLoginLog(row.id)
  showSuccessToast('删除成功')
  await refresh()
}

async function confirmClean() {
  const { confirmed } = await showConfirm({
    title: '清空确认',
    content: '确定要清空所有登录日志吗？此操作不可恢复。',
    tone: 'danger',
    confirmText: '清空',
  })
  if (!confirmed) return
  await clearLoginLog()
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
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.login-log-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  box-sizing: border-box;
}

.page-list__scroll {
  flex: 1;
  min-height: 0;
  padding-bottom: 32rpx;
}

.login-log-hero-extra {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4rpx;
}

.login-log-hero-extra__num {
  font-size: 40rpx;
  font-weight: $font-weight-bold;
  color: #fff;
  line-height: 1.1;
}

.login-log-hero-extra__label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.88);
}

.login-log-search-gap {
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

.login-log-clear-btn {
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

.login-log-card {
  margin-bottom: $card-gap;
}

.login-log-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.login-log-card__user {
  flex: 1;
  min-width: 0;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  word-break: break-all;
}

.login-log-card__msg {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-regular;
  line-height: 1.45;
  word-break: break-all;
}

.login-log-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx 20rpx;
  margin-top: 16rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.login-log-card__client {
  margin-top: 10rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}

.login-log-card__time {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}

.login-log-card__actions {
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

.login-log-page :deep(.monitor-panel) {
  margin-bottom: 24rpx;
}

.login-log-page :deep(.monitor-panel__head) {
  margin-bottom: 24rpx;
}

.login-log-page :deep(.search-bar) {
  margin-bottom: 0;
}
</style>
