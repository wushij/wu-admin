<template>
  <view class="approval-page has-page-footer">
    <template v-if="approval">
      <view class="approval-hero card--elevated">
        <view class="approval-hero__top">
          <DictTag :dict-type="DICT_TYPE.APPROVAL_STATUS" :value="approval.status" />
          <DictTag :dict-type="DICT_TYPE.APPROVAL_FORM_TYPE" :value="approval.formType" />
        </view>
        <text class="approval-hero__title">{{ approval.title }}</text>
        <text class="approval-hero__no">{{ approval.formNo || '—' }}</text>
      </view>

      <view class="section card--elevated">
        <text class="section__title">基本信息</text>
        <view class="info-grid">
          <view class="info-item">
            <text class="info-item__label">申请人</text>
            <text class="info-item__value">{{ applicantDisplayName }}</text>
          </view>
          <view class="info-item">
            <text class="info-item__label">审批人</text>
            <text class="info-item__value">{{ approval.approverName || '—' }}</text>
          </view>
          <view class="info-item info-item--full">
            <text class="info-item__label">创建时间</text>
            <text class="info-item__value">{{ formatDateTime(approval.createTime, true) }}</text>
          </view>
          <view v-if="approval.resultRemark" class="info-item info-item--full">
            <text class="info-item__label">审批意见</text>
            <text class="info-item__value">{{ approval.resultRemark }}</text>
          </view>
        </view>
      </view>

      <view class="section card--elevated">
        <text class="section__title">申请内容</text>
        <view v-if="approval.formType === 'REGISTER'" class="info-grid">
          <view class="info-item">
            <text class="info-item__label">注册账号</text>
            <text class="info-item__value">{{ registerDetail.username || '—' }}</text>
          </view>
          <view class="info-item">
            <text class="info-item__label">昵称</text>
            <text class="info-item__value">{{ registerDetail.nickname || '—' }}</text>
          </view>
          <view class="info-item info-item--full">
            <text class="info-item__label">手机号</text>
            <text class="info-item__value">{{ registerDetail.mobile || '—' }}</text>
          </view>
        </view>
        <view v-else class="content-box">
          <text class="content-box__text">{{ approval.content || '—' }}</text>
        </view>
      </view>

      <view v-if="records.length" class="section card--elevated">
        <text class="section__title">审批记录</text>
        <view class="timeline">
          <view v-for="(r, index) in records" :key="r.id" class="timeline__item">
            <view class="timeline__axis">
              <view
                class="timeline__dot"
                :class="[
                  timelineActionClass(r.action),
                  { 'timeline__dot--latest': index === records.length - 1 },
                ]"
              />
              <view v-if="index < records.length - 1" class="timeline__line" />
            </view>
            <view class="timeline__body">
              <view class="timeline__head">
                <text class="timeline__headline">
                  <text class="timeline__operator">{{ operatorDisplayName(r) }}</text>
                  <text class="timeline__action" :class="timelineActionClass(r.action)">
                    {{ approvalActionLabel(r.action) }}
                  </text>
                </text>
                <text class="timeline__time">{{ formatDateTime(r.createTime, true) }}</text>
              </view>
              <text v-if="r.remark" class="timeline__remark">{{ r.remark }}</text>
            </view>
          </view>
        </view>
      </view>
    </template>

    <PageFooter v-if="showFooter">
      <button v-if="canApprove" class="page-footer__btn page-footer__btn--ghost" @tap="onReject">驳回</button>
      <button v-if="canApprove" class="page-footer__btn" @tap="onApprove">通过</button>
      <button v-if="canArchive" class="page-footer__btn" @tap="onArchive">归档</button>
      <button v-if="canDelete" class="page-footer__btn page-footer__btn--danger" @tap="onDelete">删除</button>
    </PageFooter>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAppDialogBackPress } from '@/composables/useAppDialogBackPress'
import DictTag from '@/components/common/DictTag/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import {
  getApproval,
  getApprovalRecords,
  approveApproval,
  archiveApproval,
  deleteApproval,
} from '@/api/system/approval'
import { usePermission } from '@/composables/usePermission'
import { useUserStore } from '@/store/user'
import { useMessageStore } from '@/store/message'
import { preloadDicts } from '@/composables/useDict'
import { DICT_TYPE } from '@/constants/dict'
import { formatDateTime } from '@/utils/format'
import { showConfirm } from '@/utils/app-dialog'
import {
  approvalActionLabel,
  parseRegisterApprovalContent,
  resolveApplicantDisplayName,
  resolveOperatorDisplayName,
  timelineActionClass,
} from '@/utils/approval-display'
import type { ApprovalRecordVO, ApprovalVO } from '@/types/system'

const approval = ref<ApprovalVO | null>(null)
const records = ref<ApprovalRecordVO[]>([])
const approvalId = ref(0)
const { hasPerm } = usePermission()
const userStore = useUserStore()
const messageStore = useMessageStore()

const registerDetail = computed(() =>
  approval.value?.formType === 'REGISTER'
    ? parseRegisterApprovalContent(approval.value.content)
    : {},
)

const applicantDisplayName = computed(() => resolveApplicantDisplayName(approval.value || undefined))

function operatorDisplayName(record: ApprovalRecordVO) {
  return resolveOperatorDisplayName(record, approval.value || undefined)
}

const canApprove = computed(() => {
  if (!approval.value || approval.value.status !== 'SUBMITTED') return false
  if (approval.value.formType === 'REGISTER') return hasPerm('system:approval:approve')
  const uid = userStore.userInfo.userId
  return !!uid && approval.value.approverUserId === uid
})

const canArchive = computed(() => {
  if (!approval.value) return false
  const status = approval.value.status
  if (status !== 'APPROVED' && status !== 'REJECTED') return false
  if (approval.value.formType === 'REGISTER') return hasPerm('system:approval:archive')
  const uid = userStore.userInfo.userId
  return !!uid && approval.value.applicantUserId === uid
})

const canDelete = computed(() => hasPerm('system:approval:delete'))

const showFooter = computed(() => canApprove.value || canArchive.value || canDelete.value)

useAppDialogBackPress()

async function load(id: number) {
  const [detailRes, recordRes] = await Promise.all([getApproval(id), getApprovalRecords(id)])
  approval.value = detailRes.data || null
  records.value = recordRes.data || []
  uni.setNavigationBarTitle({ title: approval.value?.title || '审批详情' })
}

async function submit(action: 'APPROVE' | 'REJECT') {
  if (!approval.value) return
  const { confirmed, content } = await showConfirm({
    title: action === 'APPROVE' ? '确认通过' : '确认驳回',
    content: action === 'APPROVE' ? '确定通过该审批单？' : '确定驳回该审批单？',
    confirmText: action === 'APPROVE' ? '通过' : '驳回',
    tone: action === 'REJECT' ? 'danger' : 'default',
    editable: true,
    placeholderText: '审批意见（可选）',
  })
  if (!confirmed) return
  await approveApproval({
    id: approval.value.id,
    action,
    remark: content?.trim() || undefined,
  })
  uni.showToast({ title: '操作成功', icon: 'success' })
  await load(approval.value.id)
}

function onApprove() {
  submit('APPROVE')
}

function onReject() {
  submit('REJECT')
}

async function onArchive() {
  if (!approval.value) return
  const { confirmed } = await showConfirm({
    title: '归档确认',
    content: '归档后审批单将移入历史记录',
    confirmText: '归档',
  })
  if (!confirmed) return
  await archiveApproval({ id: approval.value.id, remark: '归档' })
  uni.showToast({ title: '已归档', icon: 'success' })
  await load(approval.value.id)
}

async function onDelete() {
  if (!approval.value) return
  const { confirmed } = await showConfirm({
    title: '删除审批单',
    content: '确定删除该审批单？',
    tone: 'danger',
    confirmText: '删除',
  })
  if (!confirmed) return
  await deleteApproval(approval.value.id)
  await messageStore.refreshSummary()
  uni.showToast({ title: '已删除', icon: 'success' })
  setTimeout(() => uni.navigateBack(), 400)
}

onMounted(() => {
  preloadDicts([DICT_TYPE.APPROVAL_STATUS, DICT_TYPE.APPROVAL_FORM_TYPE])
})

onLoad(async (options) => {
  approvalId.value = Number(options?.id)
  if (!approvalId.value) return
  await load(approvalId.value)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.approval-page {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: $color-bg-page;
}

.approval-hero {
  padding: 32rpx;
  margin-bottom: $card-gap;
}

.approval-hero__top {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.approval-hero__title {
  display: block;
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.4;
}

.approval-hero__no {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  font-family: monospace;
}

.section {
  padding: 28rpx 32rpx 32rpx;
  margin-bottom: $card-gap;
}

.section__title {
  display: block;
  margin-bottom: 24rpx;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
}

.info-item {
  padding: 20rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
}

.info-item--full {
  grid-column: 1 / -1;
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
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  line-height: 1.45;
  word-break: break-all;
}

.content-box {
  padding: 24rpx;
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

.timeline__item {
  display: flex;
  gap: 20rpx;
}

.timeline__item + .timeline__item {
  margin-top: 8rpx;
}

.timeline__axis {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 24rpx;
  padding-top: 8rpx;
}

.timeline__dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: $color-border;
  flex-shrink: 0;
}

.timeline__dot--latest {
  box-shadow: 0 0 0 6rpx rgba(79, 70, 229, 0.12);
}

.timeline__dot.timeline--submit {
  background: $color-primary;
}

.timeline__dot.timeline--approve {
  background: $color-success;
}

.timeline__dot.timeline--reject {
  background: $color-danger;
}

.timeline__dot.timeline--archive {
  background: $color-text-secondary;
}

.timeline__line {
  flex: 1;
  width: 2rpx;
  min-height: 48rpx;
  margin-top: 8rpx;
  background: $color-border-light;
}

.timeline__body {
  flex: 1;
  min-width: 0;
  padding-bottom: 24rpx;
}

.timeline__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.timeline__headline {
  flex: 1;
  min-width: 0;
  font-size: $font-size-base;
  line-height: 1.45;
}

.timeline__operator {
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.timeline__action {
  margin-left: 8rpx;
  font-weight: $font-weight-semibold;
  color: $color-text-regular;
}

.timeline__action.timeline--approve {
  color: $color-success;
}

.timeline__action.timeline--reject {
  color: $color-danger;
}

.timeline__action.timeline--archive {
  color: $color-text-secondary;
}

.timeline__time {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.timeline__remark {
  display: block;
  margin-top: 10rpx;
  padding: 16rpx 20rpx;
  border-radius: $radius-sm;
  background: $color-bg-muted;
  font-size: $font-size-sm;
  color: $color-text-regular;
  line-height: 1.55;
}
</style>
