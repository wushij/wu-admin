<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list job-page">
    <ModuleHero theme="job" title="定时任务" :subtitle="heroSubtitle">
      <template v-if="mode === 'job' && overview" #extra>
        <view class="job-hero-mini">
          <text>{{ overview.runningJobs ?? 0 }} 运行</text>
          <text class="job-hero-mini__dot">·</text>
          <text>{{ overview.pausedJobs ?? 0 }} 暂停</text>
        </view>
      </template>
    </ModuleHero>

    <view v-if="mode === 'job' && overview" class="job-metrics">
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

    <view class="job-mode-tabs">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="job-mode-tabs__item"
        :class="{ 'job-mode-tabs__item--active': mode === tab.key }"
        @tap="switchMode(tab.key as 'job' | 'log')"
      >
        <text class="job-mode-tabs__label">{{ tab.label }}</text>
      </view>
    </view>

    <scroll-view
      scroll-y
      class="page-list__scroll"
      :class="{ 'page-list__scroll--filter': mode === 'job', 'page-list__scroll--job-log': mode === 'log' }"
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
        <view class="job-log-panel card--elevated">
          <view class="job-log-panel__head">
            <view class="job-log-panel__head-main">
              <view class="job-log-panel__title-wrap">
                <text class="job-log-panel__title">执行记录</text>
                <text class="job-log-panel__count">{{ logTotal || logList.length }}</text>
              </view>
              <text v-if="canDelete" class="job-log-panel__clear" @tap="onCleanLogs">清空</text>
            </view>
            <view class="job-log-panel__filters">
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
          </view>

          <view v-if="overview && !logFilter" class="job-log-panel__stats">
            <view class="job-log-panel__stat job-log-panel__stat--ok">
              <text class="job-log-panel__stat-value">{{ overview.successCount ?? 0 }}</text>
              <text class="job-log-panel__stat-label">成功</text>
            </view>
            <view class="job-log-panel__stat job-log-panel__stat--fail">
              <text class="job-log-panel__stat-value">{{ overview.failCount ?? 0 }}</text>
              <text class="job-log-panel__stat-label">失败</text>
            </view>
            <view class="job-log-panel__stat">
              <text class="job-log-panel__stat-value">{{ overview.successRate ?? 100 }}%</text>
              <text class="job-log-panel__stat-label">成功率</text>
            </view>
          </view>

          <view v-if="logFilter?.jobName" class="job-log-panel__chip">
            <text class="job-log-panel__chip-label">{{ logFilter.jobName }}</text>
            <text class="job-log-panel__chip-clear" @tap="clearLogFilter">查看全部</text>
          </view>

          <view v-if="logList.length" class="job-log-timeline">
            <view
              v-for="(item, index) in logList"
              :key="item.id"
              class="job-log-timeline__item"
              @tap="showLogDetail(item)"
            >
              <view class="job-log-timeline__axis">
                <view
                  class="job-log-timeline__dot"
                  :class="item.status === 0 ? 'job-log-timeline__dot--ok' : 'job-log-timeline__dot--fail'"
                />
                <view v-if="index < logList.length - 1" class="job-log-timeline__line" />
              </view>
              <view class="job-log-timeline__body">
                <view class="job-log-timeline__row">
                  <text class="job-log-timeline__name">{{ item.jobName }}</text>
                  <text
                    class="job-log-timeline__status"
                    :class="item.status === 0 ? 'job-log-timeline__status--ok' : 'job-log-timeline__status--fail'"
                  >
                    {{ jobLogStatusLabel(item.status) }}
                  </text>
                </view>
                <view class="job-log-timeline__meta">
                  <text>{{ formatDateTime(item.startTime) }}</text>
                  <text class="job-log-timeline__sep">·</text>
                  <text>耗时 {{ formatJobDuration(item) }}</text>
                </view>
                <text v-if="logPreview(item)" class="job-log-timeline__msg">{{ logPreview(item) }}</text>
              </view>
            </view>
          </view>
          <view v-else-if="!currentLoading" class="job-log-panel__empty">
            <text class="job-log-panel__empty-text">暂无执行记录</text>
          </view>
        </view>
      </template>

      <EmptyState v-if="currentEmpty && mode === 'job'" :title="emptyTitle" icon="clock-o" />
      <ListFooter
        v-else-if="mode === 'job' ? !currentEmpty : logList.length"
        :loading="currentLoading"
        :finished="currentFinished"
        :empty="false"
      />
    </scroll-view>

    <FabButton v-if="mode === 'job' && canAdd" @click="goCreate" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleHero from '@/components/common/ModuleHero/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
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
import { formatJobDuration, jobLogStatusLabel } from '@/utils/job-log'
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

function switchMode(next: 'job' | 'log') {
  if (mode.value === next) return
  mode.value = next
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
        await changeJobStatus(item.id!, item.status === 1 ? 0 : 1)
        uni.showToast({ title: '状态已更新', icon: 'success' })
        await Promise.all([jobs.refresh(), loadOverview()])
      } else if (action === '执行一次') {
        await runJob(item.id!)
        uni.showToast({ title: '已触发执行', icon: 'success' })
        setTimeout(() => loadOverview(), 1000)
      } else if (action === '删除') {
        uni.showModal({
          title: '删除任务',
          content: `确定删除「${item.jobName}」？`,
          confirmColor: '#f56c6c',
          success: async (r) => {
            if (!r.confirm || !item.id) return
            await deleteJob(item.id)
            uni.showToast({ title: '已删除', icon: 'success' })
            await Promise.all([jobs.refresh(), loadOverview()])
          },
        })
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

function onCleanLogs() {
  uni.showModal({
    title: '清空日志',
    content: '确定清空所有执行日志？',
    success: async (res) => {
      if (!res.confirm) return
      await cleanJobLogs()
      uni.showToast({ title: '已清空', icon: 'success' })
      await Promise.all([logs.refresh(), loadOverview()])
    },
  })
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

function showLogDetail(item: SysJobLog) {
  const lines = [
    `结果：${jobLogStatusLabel(item.status)}`,
    item.startTime ? `开始：${formatDateTime(item.startTime)}` : '',
    item.stopTime ? `结束：${formatDateTime(item.stopTime)}` : '',
    `耗时：${formatJobDuration(item)}`,
    item.jobMessage ? `信息：${item.jobMessage}` : '',
    item.exceptionInfo ? `异常：${item.exceptionInfo}` : '',
  ].filter(Boolean)
  uni.showModal({
    title: item.jobName || '执行详情',
    content: lines.join('\n'),
    showCancel: false,
  })
}

function logPreview(item: SysJobLog) {
  const text = item.status === 1 ? item.exceptionInfo : item.jobMessage
  if (!text) return ''
  return text.length > 48 ? `${text.slice(0, 48)}…` : text
}

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
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.job-hero-mini {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: rgba(255, 255, 255, 0.92);
}

.job-hero-mini__dot {
  opacity: 0.6;
}

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

.job-mode-tabs {
  display: flex;
  align-items: center;
  gap: 32rpx;
  margin-bottom: 20rpx;
  padding: 0 8rpx;
  border-bottom: 1px solid $color-border-light;
}

.job-mode-tabs__item {
  position: relative;
  padding: 16rpx 4rpx 20rpx;

  &--active::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 4rpx;
    border-radius: 4rpx 4rpx 0 0;
    background: linear-gradient(90deg, #14b8a6, #2dd4bf);
  }
}

.job-mode-tabs__label {
  font-size: $font-size-md;
  color: $color-text-secondary;
}

.job-mode-tabs__item--active .job-mode-tabs__label {
  color: #0f766e;
  font-weight: $font-weight-semibold;
}

.page-list__scroll--job-log {
  height: calc(100vh - 400rpx);
}

.job-log-panel {
  overflow: hidden;
  padding: 0 0 8rpx;
}

.job-log-panel__head {
  padding: 24rpx 28rpx 20rpx;
  border-bottom: 1px solid $color-border-light;
}

.job-log-panel__head-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.job-log-panel__filters {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 18rpx;
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

.job-log-panel__title-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
  min-width: 0;
}

.job-log-panel__title {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.job-log-panel__count {
  padding: 4rpx 14rpx;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: #0d9488;
  background: rgba(20, 184, 166, 0.12);
}

.job-log-panel__clear {
  flex-shrink: 0;
  font-size: $font-size-sm;
  color: $color-danger;
  padding: 8rpx 4rpx;
}

.job-log-panel__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  padding: 20rpx 28rpx;
  border-bottom: 1px solid $color-border-light;
  background: linear-gradient(180deg, rgba(20, 184, 166, 0.06), transparent);
}

.job-log-panel__stat {
  padding: 16rpx 12rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  text-align: center;
}

.job-log-panel__stat-value {
  display: block;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.2;
}

.job-log-panel__stat-label {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.job-log-panel__stat--ok .job-log-panel__stat-value {
  color: $color-success;
}

.job-log-panel__stat--fail .job-log-panel__stat-value {
  color: $color-danger;
}

.job-log-panel__chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin: 16rpx 28rpx 0;
  padding: 14rpx 18rpx;
  border-radius: $radius-md;
  background: rgba(20, 184, 166, 0.1);
  border: 1px solid rgba(20, 184, 166, 0.18);
}

.job-log-panel__chip-label {
  flex: 1;
  min-width: 0;
  font-size: $font-size-sm;
  color: #0f766e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.job-log-panel__chip-clear {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.job-log-timeline {
  padding: 20rpx 28rpx 12rpx;
}

.job-log-timeline__item {
  display: flex;
  gap: 20rpx;

  &:active .job-log-timeline__body {
    opacity: 0.88;
  }

  & + & {
    margin-top: 4rpx;
  }
}

.job-log-timeline__axis {
  position: relative;
  width: 24rpx;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.job-log-timeline__dot {
  width: 18rpx;
  height: 18rpx;
  margin-top: 10rpx;
  border-radius: 50%;
  border: 4rpx solid $color-bg-card;
  box-shadow: 0 0 0 2rpx currentColor;
  flex-shrink: 0;
  z-index: 1;

  &--ok {
    color: $color-success;
    background: $color-success;
  }

  &--fail {
    color: $color-danger;
    background: $color-danger;
  }
}

.job-log-timeline__line {
  flex: 1;
  width: 2rpx;
  min-height: 48rpx;
  margin-top: 6rpx;
  background: $color-border-light;
}

.job-log-timeline__body {
  flex: 1;
  min-width: 0;
  padding-bottom: 24rpx;
}

.job-log-timeline__row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.job-log-timeline__name {
  flex: 1;
  min-width: 0;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  line-height: 1.45;
}

.job-log-timeline__status {
  flex-shrink: 0;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  padding: 4rpx 12rpx;
  border-radius: $radius-full;

  &--ok {
    color: $color-success;
    background: rgba(103, 194, 58, 0.12);
  }

  &--fail {
    color: $color-danger;
    background: rgba(245, 108, 108, 0.12);
  }
}

.job-log-timeline__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 10rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.4;
}

.job-log-timeline__sep {
  color: $color-border;
}

.job-log-timeline__msg {
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

.job-log-panel__empty {
  padding: 48rpx 28rpx 56rpx;
  text-align: center;
}

.job-log-panel__empty-text {
  font-size: $font-size-sm;
  color: $color-text-secondary;
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
