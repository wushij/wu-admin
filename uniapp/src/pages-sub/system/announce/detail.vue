<template>
  <view class="announce-detail-page" :class="{ 'has-page-footer': showFooter }">
    <ListLoading v-if="loading" />

    <template v-else-if="detail">
      <view class="announce-hero card--elevated">
        <view class="announce-hero__top">
          <DictTag :label="detail.status === 1 ? '已发布' : '草稿'" :effect="detail.status === 1 ? 'success' : 'warning'" />
          <DictTag :label="noticeTypeLabel(detail.noticeType)" effect="primary" />
        </view>
        <text class="announce-hero__title">{{ detail.title }}</text>
        <text class="announce-hero__meta">{{ detail.createName || '—' }} · {{ formatDateTime(detail.createTime, true) }}</text>
      </view>

      <view class="section card--elevated">
        <text class="section__title">基本信息</text>
        <view class="info-grid">
          <view class="info-item info-item--full">
            <text class="info-item__label">通知对象</text>
            <text class="info-item__value">{{ targetTypeLabel }}</text>
          </view>
          <view v-if="targetNames" class="info-item info-item--full">
            <text class="info-item__label">发送范围</text>
            <text class="info-item__value">{{ targetNames }}</text>
          </view>
          <view class="info-item info-item--full">
            <text class="info-item__label">创建时间</text>
            <text class="info-item__value">{{ formatDateTime(detail.createTime, true) }}</text>
          </view>
        </view>
      </view>

      <view class="section card--elevated">
        <text class="section__title">通知内容</text>
        <view class="content-box">
          <text class="content-box__text">{{ detail.content || '—' }}</text>
        </view>
      </view>
    </template>

    <EmptyState v-else-if="!loading" title="通知不存在或已删除" icon="bell" />

    <PageFooter v-if="showFooter">
      <button v-if="canUpdate" class="page-footer__btn page-footer__btn--ghost" @click="goEdit">编辑</button>
      <button v-if="canPublish && detail?.status !== 1" class="page-footer__btn" @click="onPublish">发布</button>
      <button v-if="canDelete" class="page-footer__btn page-footer__btn--danger" @click="onDelete">删除</button>
    </PageFooter>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { deleteAnnounce, getAnnounceAdminDetail, publishAnnounce } from '@/api/message'
import { getUserList } from '@/api/system/user'
import { getDeptTree } from '@/api/system/dept'
import { useModulePermission } from '@/composables/useModulePermission'
import { buildDeptLabels, buildDeptNodeMaps } from '@/utils/dept-tree'
import { formatDateTime } from '@/utils/format'
import { showConfirm } from '@/utils/app-dialog'
import type { AnnounceVO } from '@/types/message'

const loading = ref(true)
const detail = ref<AnnounceVO | null>(null)
const targetNames = ref('')
const announceId = ref(0)

const { hasPerm } = useModulePermission('system:announce:list')
const canUpdate = computed(() => hasPerm('system:announce:update'))
const canPublish = computed(() => hasPerm('system:announce:publish'))
const canDelete = computed(() => hasPerm('system:announce:delete'))
const showFooter = computed(() => detail.value && (canUpdate.value || canPublish.value || canDelete.value))

const targetTypeLabel = computed(() => {
  const type = detail.value?.targetType
  if (type === 1) return '指定用户'
  if (type === 2) return '指定部门'
  return '全体人员'
})

function noticeTypeLabel(type?: number) {
  return type === 2 ? '公告' : '通知'
}

async function resolveTargetNames(row: AnnounceVO) {
  const ids = row.targetIds || []
  if (row.targetType === 1) {
    const res = await getUserList()
    const users = res.data || []
    targetNames.value = ids
      .map((id) => users.find((u) => u.id === id)?.nickname || users.find((u) => u.id === id)?.username)
      .filter(Boolean)
      .join('、')
    return
  }
  if (row.targetType === 2) {
    const res = await getDeptTree({ status: 1 })
    const maps = buildDeptNodeMaps(res.data || [])
    targetNames.value = buildDeptLabels(ids, maps.idToNode).join('、')
    return
  }
  targetNames.value = ''
}

async function loadDetail(id: number) {
  loading.value = true
  try {
    const res = await getAnnounceAdminDetail(id)
    detail.value = res.data || null
    if (detail.value) {
      await resolveTargetNames(detail.value)
      uni.setNavigationBarTitle({ title: '通知详情' })
    }
  } finally {
    loading.value = false
  }
}

function goEdit() {
  uni.navigateTo({ url: `/pages-sub/system/announce/form?id=${announceId.value}` })
}

async function onPublish() {
  if (!announceId.value) return
  const { confirmed } = await showConfirm({
    title: '发布确认',
    content: '确定发布该通知？',
    confirmText: '发布',
  })
  if (!confirmed) return
  await publishAnnounce(announceId.value)
  uni.showToast({ title: '已发布', icon: 'success' })
  await loadDetail(announceId.value)
}

async function onDelete() {
  if (!detail.value) return
  const { confirmed } = await showConfirm({
    title: '删除通知',
    content: `确定删除「${detail.value.title}」？`,
    tone: 'danger',
    confirmText: '删除',
  })
  if (!confirmed) return
  await deleteAnnounce(announceId.value)
  uni.showToast({ title: '已删除', icon: 'success' })
  setTimeout(() => uni.navigateBack(), 400)
}

onLoad((options) => {
  announceId.value = Number(options?.id)
  if (announceId.value) loadDetail(announceId.value)
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.announce-detail-page {
  min-height: 100vh;
  padding: $page-padding-y $page-padding-x;
  box-sizing: border-box;
}

.announce-hero {
  padding: 28rpx 32rpx;
  margin-bottom: $section-gap;
}

.announce-hero__top {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.announce-hero__title {
  display: block;
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.4;
}

.announce-hero__meta {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.section {
  padding: 28rpx 32rpx;
  margin-bottom: $section-gap;
}

.section__title {
  display: block;
  margin-bottom: 24rpx;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 24rpx 16rpx;
}

.info-item {
  width: calc(50% - 8rpx);
  min-width: 0;
}

.info-item--full {
  width: 100%;
}

.info-item__label {
  display: block;
  margin-bottom: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.info-item__value {
  display: block;
  font-size: $font-size-base;
  color: $color-text-primary;
  line-height: 1.45;
  word-break: break-all;
}

.content-box {
  padding: 20rpx 24rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
}

.content-box__text {
  font-size: $font-size-base;
  color: $color-text-primary;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
