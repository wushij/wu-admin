<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list job-page">
    <ModuleDarkHero
      title="定时任务"
      :subtitle="heroSubtitle"
      icon="clock-o"
      theme="job"
      :count="mode === 'log' ? (logTotal || logList.length) : undefined"
      count-label="日志"
    >
      <template v-if="mode === 'job' && overview" #aside>
        <view class="module-dark-hero__stats-row">
          <view class="module-dark-hero__mini-stat">
            <text class="module-dark-hero__mini-stat-num">{{ overview.runningJobs ?? 0 }}</text>
            <text class="module-dark-hero__mini-stat-label">运行</text>
          </view>
          <view class="module-dark-hero__mini-stat">
            <text class="module-dark-hero__mini-stat-num">{{ overview.pausedJobs ?? 0 }}</text>
            <text class="module-dark-hero__mini-stat-label">暂停</text>
          </view>
        </view>
      </template>
    </ModuleDarkHero>

    <view v-if="overview" class="job-metrics">
      <view class="job-metrics__item">
        <text class="job-metrics__value">{{ overview.totalCount ?? 0 }}</text>
        <text class="job-metrics__label">累计执行</text>
      </view>
      <view class="job-metrics__item job-metrics__item--ok">
        <text class="job-metrics__value">{{ overview.successCount ?? 0 }}</text>
        <text class="job-metrics__label">成功</text>
      </view>
      <view class="job-metrics__item job-metrics__item--fail">
        <text class="job-metrics__value">{{ overview.failCount ?? 0 }}</text>
        <text class="job-metrics__label">失败</text>
      </view>
      <view class="job-metrics__item job-metrics__item--rate">
        <text class="job-metrics__value">{{ overview.successRate ?? 100 }}%</text>
        <text class="job-metrics__label">成功率</text>
      </view>
    </view>

    <SegmentTabs v-model="mode" :tabs="tabs" theme="teal" />

    <view v-if="mode === 'log'" class="job-log-bar">
      <view class="job-log-bar__inner card--elevated">
        <view class="job-log-bar__filters">
          <text
            v-for="tab in logStatusTabs"
            :key="tab.key"
            class="job-log-filter"
            :class="{ 'job-log-filter--active': logStatusMode === tab.key }"
            @tap="setLogStatus(tab.key as 'all' | '0' | '1')"
          >
            {{ tab.label }}
          </text>
        </view>
        <view v-if="canDelete" class="job-log-bar__actions">
          <button
            v-if="logFilter?.jobName"
            class="outline-btn outline-btn--danger job-log-bar__clear"
            @tap="onCleanScopedLogs"
          >
            清空本任务
          </button>
          <button
            class="outline-btn job-log-bar__clear-all"
            :class="logFilter?.jobName ? '' : 'outline-btn--danger'"
            @tap="onCleanAllLogs"
          >
            清空全部
          </button>
        </view>
      </view>
    </view>

    <view v-if="mode === 'log' && logFilter?.jobName" class="job-log-chip">
      <text class="job-log-chip__label">{{ logFilter.jobName }}</text>
      <text class="job-log-chip__clear" @tap="clearLogFilter">查看全部日志</text>
    </view>

    <scroll-view
      scroll-y
      class="page-list__scroll"
      :class="mode === 'log' ? 'page-list__scroll--job-log' : 'page-list__scroll--job'"
      @scrolltolower="onLoadMore"
    >
      <template v-if="mode === 'job'">
        <ListCard v-for="item in jobList" :key="item.id" @click="onJobTap(item)">
          <view class="list-card__top">
            <text class="list-card__title">{{ item.jobName }}</text>
            <DictTag
              :label="item.status === 1 ? '运行' : '暂停'"
              :effect="item.status === 1 ? 'success' : 'warning'"
            />
          </view>
          <view class="job-card__row">
            <DictTag :label="item.jobGroup || 'DEFAULT'" effect="default" />
            <text class="job-card__cron">{{ item.cronHint || item.cronExpression }}</text>
          </view>
          <text v-if="item.status === 1 && item.nextFireTime" class="list-card__sub">
            下次 {{ formatDateTime(item.nextFireTime) }}
          </text>
        </ListCard>
      </template>

      <template v-else>
        <ListCard v-for="item in logList" :key="item.id" @click="showLogDetail(item)">
          <view class="list-card__top">
            <text class="list-card__title">{{ item.jobName }}</text>
            <DictTag
              :label="jobLogStatusLabel(item.status)"
              :effect="jobLogStatusEffect(item.status)"
            />
          </view>
          <view class="job-log-card__meta">
            <text>{{ formatDateTime(item.startTime) }}</text>
            <text class="job-log-card__sep">·</text>
            <text>耗时 {{ formatJobDuration(item) }}</text>
          </view>
          <text v-if="logPreview(item)" class="job-log-card__msg">{{ logPreview(item) }}</text>
        </ListCard>
      </template>

      <EmptyState
        v-if="(currentEmpty && mode === 'job') || (mode === 'log' && !logList.length && !currentLoading)"
        :title="emptyTitle"
        icon="clock-o"
      />
      <ListFooter
        v-else-if="mode === 'job' ? !currentEmpty : logList.length"
        :loading="currentLoading"
        :finished="currentFinished"
        :empty="false"
      />
    </scroll-view>

    <FabButton v-if="mode === 'job' && canAdd" @click="goCreate" />

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useAppDialogBackPress } from '@/composables/useAppDialogBackPress'
import { usePageList } from '@/composables/usePageList'
import { useListPageShowRefresh } from '@/composables/useListPageShowRefresh'
import { useModulePermission } from '@/composables/useModulePermission'
import { showConfirm } from '@/utils/app-dialog'
import { showSuccessToast } from '@/utils/app-toast'
import {
  getJobPage,
  getJobLogPage,
  getJobOverview,
  deleteJob,
  changeJobStatus,
  runJob,
  cleanJobLogs,
} from '@/api/monitor/job'
import { formatDateTime } from '@/utils/format'
import { formatJobDuration, jobLogStatusEffect, jobLogStatusLabel } from '@/utils/job-log'
import type { JobOverview, SysJob, SysJobLog } from '@/types/system'

const overview = ref<JobOverview | null>(null)

async function loadOverview() {
  try {
    const res = await getJobOverview()
    overview.value = res.data || null
  } catch {
    overview.value = null
  }
}

const { allowed, hasPerm } = useModulePermission('monitor:job:list')
const canAdd = computed(() => hasPerm('monitor:job:add'))
const canEdit = computed(() => hasPerm('monitor:job:edit'))
const canDelete = computed(() => hasPerm('monitor:job:delete'))

const tabs = [
  { key: 'job', label: '任务' },
  { key: 'log', label: '日志' },
]
const logStatusTabs = [
  { key: 'all', label: '全部' },
  { key: '0', label: '成功' },
  { key: '1', label: '失败' },
]

const mode = ref<'job' | 'log'>('job')
const logStatusMode = ref<'all' | '0' | '1'>('all')
const logFilter = ref<{ jobName?: string; jobGroup?: string } | null>(null)
const jobTotal = ref(0)
const logTotal = ref(0)

const heroSubtitle = computed(() => {
  if (mode.value === 'log') {
    if (logFilter.value?.jobName) return `${logFilter.value.jobName} · 执行日志`
    return '调度执行记录'
  }
  return 'Quartz 任务调度管理'
})

const emptyTitle = computed(() => (mode.value === 'job' ? '暂无任务' : '暂无日志'))

const jobs = usePageList<SysJob>(async (pageNo, pageSize) => {
  const res = await getJobPage({ pageNo, pageSize })
  jobTotal.value = res.data?.total || 0
  return { list: res.data?.list || [], total: jobTotal.value }
})

const logs = usePageList<SysJobLog>(async (pageNo, pageSize) => {
  const res = await getJobLogPage({
    pageNo,
    pageSize,
    jobName: logFilter.value?.jobName,
    jobGroup: logFilter.value?.jobGroup,
    status: logStatusMode.value === 'all' ? undefined : Number(logStatusMode.value),
  })
  logTotal.value = res.data?.total || 0
  return { list: res.data?.list || [], total: logTotal.value }
})

const jobList = computed(() => jobs.list.value)
const logList = computed(() => logs.list.value)
const currentLoading = computed(() => (mode.value === 'job' ? jobs.loading.value : logs.loading.value))
const currentFinished = computed(() => (mode.value === 'job' ? jobs.finished.value : logs.finished.value))
const currentEmpty = computed(() => (mode.value === 'job' ? jobs.empty.value : logs.empty.value))

function openJobLogs(item: SysJob) {
  logFilter.value = { jobName: item.jobName, jobGroup: item.jobGroup }
  logStatusMode.value = 'all'
  mode.value = 'log'
  logs.refresh()
}

function clearLogFilter() {
  logFilter.value = null
  logs.refresh()
}

function onLogStatusChange() {
  logs.refresh()
}

function setLogStatus(next: 'all' | '0' | '1') {
  if (logStatusMode.value === next) return
  logStatusMode.value = next
  onLogStatusChange()
}

function onJobTap(item: SysJob) {
  const actions: string[] = ['查看日志']
  if (canEdit.value) {
    actions.push('编辑', item.status === 1 ? '暂停' : '恢复', '执行一次')
  }
  if (canDelete.value) actions.push('删除')
  uni.showActionSheet({
    itemList: actions,
    success: async (res) => {
      const action = actions[res.tapIndex]
      if (action === '查看日志') openJobLogs(item)
      else if (action === '编辑') goEdit(item)
      else if (action === '暂停' || action === '恢复') {
        const isPause = action === '暂停'
        const { confirmed } = await showConfirm({
          title: isPause ? '暂停任务' : '恢复任务',
          content: isPause
            ? `确定暂停「${item.jobName}」？`
            : `确定恢复「${item.jobName}」？`,
          confirmText: isPause ? '暂停' : '恢复',
          tone: isPause ? 'danger' : 'default',
        })
        if (!confirmed) return
        await changeJobStatus(item.id!, item.status === 1 ? 0 : 1)
        showSuccessToast('状态已更新')
        await Promise.all([jobs.refresh(), loadOverview()])
      } else if (action === '执行一次') {
        const { confirmed } = await showConfirm({
          title: '立即执行',
          content: `确定立即执行一次「${item.jobName}」？`,
          confirmText: '执行',
        })
        if (!confirmed) return
        await runJob(item.id!)
        showSuccessToast('已触发执行')
        setTimeout(() => loadOverview(), 1000)
      } else if (action === '删除') {
        const { confirmed } = await showConfirm({
          title: '删除任务',
          content: `确定删除「${item.jobName}」？`,
          tone: 'danger',
          confirmText: '删除',
        })
        if (!confirmed || !item.id) return
        await deleteJob(item.id)
        showSuccessToast('已移至回收中心')
        await Promise.all([jobs.refresh(), loadOverview()])
      }
    },
  })
}

function goCreate() {
  uni.navigateTo({ url: '/pages-sub/monitor/job-form?mode=create' })
}

function goEdit(item: SysJob) {
  uni.setStorageSync('job_edit_cache', JSON.stringify(item))
  uni.navigateTo({ url: `/pages-sub/monitor/job-form?id=${item.id}` })
}

async function onCleanScopedLogs() {
  const name = logFilter.value?.jobName
  if (!name) return
  const { confirmed } = await showConfirm({
    title: '清空本任务日志',
    content: `确定清空「${name}」的全部执行日志？`,
    tone: 'danger',
    confirmText: '清空',
  })
  if (!confirmed) return
  await cleanJobLogs({
    jobName: logFilter.value?.jobName,
    jobGroup: logFilter.value?.jobGroup,
  })
  showSuccessToast('本任务日志已清空')
  await Promise.all([logs.refresh(), loadOverview()])
}

async function onCleanAllLogs() {
  const { confirmed } = await showConfirm({
    title: '清空全部日志',
    content: logFilter.value?.jobName
      ? '将清空所有任务的执行日志（不仅当前筛选），是否继续？'
      : '确定清空所有任务的执行日志？',
    tone: 'danger',
    confirmText: '清空全部',
  })
  if (!confirmed) return
  await cleanJobLogs()
  showSuccessToast('全部日志已清空')
  await Promise.all([logs.refresh(), loadOverview()])
}

async function onRefresh() {
  if (mode.value === 'job') {
    await Promise.all([jobs.refresh(), loadOverview()])
  } else {
    await logs.refresh()
  }
}

function onLoadMore() {
  if (mode.value === 'job') jobs.loadMore()
  else logs.loadMore()
}

watch(mode, (m) => {
  if (m === 'job' && !jobList.value.length) jobs.refresh()
  if (m === 'log' && !logList.value.length) logs.refresh()
})

async function showLogDetail(item: SysJobLog) {
  const lines = [
    `结果：${jobLogStatusLabel(item.status)}`,
    item.startTime ? `开始：${formatDateTime(item.startTime)}` : '',
    item.stopTime ? `结束：${formatDateTime(item.stopTime)}` : '',
    `耗时：${formatJobDuration(item)}`,
    item.jobMessage ? `信息：${item.jobMessage}` : '',
    item.exceptionInfo ? `异常：${item.exceptionInfo}` : '',
  ].filter(Boolean)
  await showConfirm({
    title: item.jobName || '执行详情',
    content: lines.join('\n'),
    showCancel: false,
    contentAlign: 'left',
  })
}

function logPreview(item: SysJobLog) {
  const text = item.status === 1 ? item.exceptionInfo : item.jobMessage
  if (!text) return ''
  return text.length > 48 ? `${text.slice(0, 48)}…` : text
}

useAppDialogBackPress()

async function refreshJobsOnShow() {
  if (mode.value !== 'job') return
  await Promise.all([jobs.refresh({ silent: true }), loadOverview()])
}

useListPageShowRefresh(refreshJobsOnShow, { loading: jobs.loading, refreshing: jobs.refreshing })

onMounted(() => {
  jobs.refresh()
  loadOverview()
})

onPullDownRefresh(async () => {
  await onRefresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.job-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
  margin-bottom: $card-gap;
}

.job-metrics__item {
  padding: 20rpx 12rpx;
  border-radius: $radius-md;
  background: $color-bg-card;
  text-align: center;
  box-shadow: $shadow-card;
}

.job-metrics__value {
  display: block;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.job-metrics__label {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.job-metrics__item--ok .job-metrics__value { color: $color-success; }
.job-metrics__item--fail .job-metrics__value { color: $color-danger; }
.job-metrics__item--rate .job-metrics__value { color: $color-primary; }

.page-list__scroll--job,
.page-list__scroll--job-log {
  flex: 1;
  min-height: 0;
  width: 100%;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }
}

.job-log-bar {
  margin-bottom: 16rpx;
}

.job-log-bar__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 16rpx 20rpx;
}

.job-log-bar__filters {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  flex-wrap: wrap;
  gap: 12rpx;
  min-width: 0;
}

.job-log-bar__actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 12rpx;
}

.job-log-bar__clear,
.job-log-bar__clear-all {
  min-width: 112rpx;
  height: 56rpx;
  padding: 0 20rpx;
  font-size: $font-size-xs;
}

.job-log-filter {
  padding: 8rpx 20rpx;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  background: $color-bg-muted;

  &--active {
    color: #0f766e;
    font-weight: $font-weight-semibold;
    background: rgba(20, 184, 166, 0.14);
  }
}

.job-log-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 16rpx;
  padding: 14rpx 18rpx;
  border-radius: $radius-md;
  background: rgba(20, 184, 166, 0.1);
  border: 1px solid rgba(20, 184, 166, 0.18);
}

.job-log-chip__label {
  flex: 1;
  min-width: 0;
  font-size: $font-size-sm;
  color: #0f766e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.job-log-chip__clear {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.job-log-card__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 10rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.4;
}

.job-log-card__sep {
  color: $color-border;
}

.job-log-card__msg {
  display: block;
  margin-top: 12rpx;
  padding: 14rpx 16rpx;
  border-radius: $radius-sm;
  font-size: $font-size-xs;
  line-height: 1.55;
  color: $color-text-regular;
  background: $color-bg-muted;
  word-break: break-all;
}

.job-card__row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 12rpx;
  flex-wrap: wrap;
}

.job-card__cron {
  flex: 1;
  min-width: 0;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}
</style>
