<template>
  <view class="ticket-page" :class="{ 'has-page-footer': showFooter, 'has-comment-bar': !!ticket }">
    <template v-if="ticket">
      <view class="ticket-hero card--elevated">
        <view class="ticket-hero__top">
          <DictTag :dict-type="DICT_TYPE.TICKET_STATUS" :value="ticket.status" />
          <DictTag v-if="ticket.priority" :dict-type="DICT_TYPE.TICKET_PRIORITY" :value="ticket.priority" />
        </view>
        <text class="ticket-hero__title">{{ ticket.title }}</text>
        <text class="ticket-hero__no">{{ ticket.ticketNo || '—' }}</text>
      </view>

      <view class="section card--elevated">
        <text class="section__title">基本信息</text>
        <view class="info-grid">
          <view class="info-item">
            <text class="info-item__label">发起人</text>
            <text class="info-item__value">{{ ticket.creatorName || '—' }}</text>
          </view>
          <view class="info-item">
            <text class="info-item__label">处理人</text>
            <text class="info-item__value">{{ ticket.assigneeName || '未分配' }}</text>
          </view>
          <view class="info-item">
            <text class="info-item__label">优先级</text>
            <text class="info-item__value">{{ priorityLabel }}</text>
          </view>
          <view class="info-item">
            <text class="info-item__label">截止时间</text>
            <text class="info-item__value" :class="{ 'info-item__value--danger': isOverdue }">
              {{ ticket.deadline ? formatDateTime(ticket.deadline, true) : '—' }}
            </text>
          </view>
          <view class="info-item info-item--full">
            <text class="info-item__label">创建时间</text>
            <text class="info-item__value">{{ formatDateTime(ticket.createTime, true) }}</text>
          </view>
        </view>
      </view>

      <view v-if="ticket.description" class="section card--elevated">
        <text class="section__title">问题描述</text>
        <view class="content-box">
          <text class="content-box__text">{{ ticket.description }}</text>
        </view>
      </view>

      <view class="section card--elevated">
        <text class="section__title">评论 ({{ comments.length }})</text>
        <view v-if="comments.length" class="timeline">
          <view v-for="(c, index) in comments" :key="c.id" class="timeline__item">
            <view class="timeline__axis">
              <view class="timeline__dot" :class="{ 'timeline__dot--latest': index === comments.length - 1 }" />
              <view v-if="index < comments.length - 1" class="timeline__line" />
            </view>
            <view class="timeline__body">
              <view class="timeline__head">
                <text class="timeline__operator">{{ c.username || '用户' }}</text>
                <text class="timeline__time">{{ formatDateTime(c.createTime, true) }}</text>
              </view>
              <text class="timeline__remark">{{ c.content }}</text>
            </view>
          </view>
        </view>
        <text v-else class="timeline__empty">暂无评论，在下方写下第一条</text>
      </view>
    </template>

    <view v-if="ticket" class="ticket-comment-bar" :class="{ 'ticket-comment-bar--with-footer': showFooter }">
      <input
        v-model="commentText"
        class="ticket-comment-bar__input"
        placeholder="写下评论…"
        confirm-type="send"
        @confirm="submitComment"
      />
      <button class="ticket-comment-bar__send" :loading="commentSaving" @click="submitComment">发送</button>
    </view>

    <PageFooter v-if="showFooter">
      <button v-if="canTransition" class="page-footer__btn" @tap="onTransition">变更状态</button>
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
  getTicket,
  getTicketComments,
  transitionTicket,
  createTicketComment,
  deleteTicket,
} from '@/api/system/ticket'
import { usePermission } from '@/composables/usePermission'
import { useUserStore } from '@/store/user'
import { getDictLabel, getDictOptions, preloadDicts } from '@/composables/useDict'
import { showConfirm, showActionSheet } from '@/utils/app-dialog'
import { DICT_TYPE } from '@/constants/dict'
import { formatDateTime } from '@/utils/format'
import type { TicketCommentVO, TicketVO } from '@/types/system'

const ticket = ref<TicketVO | null>(null)
const comments = ref<TicketCommentVO[]>([])
const commentText = ref('')
const commentSaving = ref(false)
const ticketId = ref(0)
const { hasPerm } = usePermission()
const userStore = useUserStore()

const priorityLabel = computed(() =>
  ticket.value?.priority
    ? getDictLabel(DICT_TYPE.TICKET_PRIORITY, ticket.value.priority)
    : '—',
)

const isOverdue = computed(() => {
  const t = ticket.value
  if (!t?.deadline || t.status === 'CLOSED' || t.status === 'RESOLVED') return false
  return new Date(t.deadline).getTime() < Date.now()
})

const canTransition = computed(() => {
  if (!ticket.value) return false
  if (hasPerm('system:ticket:transition')) return true
  const uid = userStore.userInfo.userId
  return !!uid && ticket.value.assigneeUserId === uid
})

const canDelete = computed(() => hasPerm('system:ticket:delete'))

const showFooter = computed(() => canTransition.value || canDelete.value)

useAppDialogBackPress()

async function load(id: number) {
  const [detailRes, commentRes] = await Promise.all([getTicket(id), getTicketComments(id)])
  ticket.value = detailRes.data || null
  comments.value = commentRes.data || []
  uni.setNavigationBarTitle({ title: ticket.value?.title || '工单详情' })
}

async function submitComment() {
  const content = commentText.value.trim()
  if (!content || !ticketId.value) return
  commentSaving.value = true
  try {
    await createTicketComment({ ticketId: ticketId.value, content })
    commentText.value = ''
    uni.showToast({ title: '评论已发送', icon: 'success' })
    const commentRes = await getTicketComments(ticketId.value)
    comments.value = commentRes.data || []
  } catch (e) {
    console.error(e)
  } finally {
    commentSaving.value = false
  }
}

async function onTransition() {
  if (!ticket.value) return
  const current = String(ticket.value.status ?? '')
  const candidates = getDictOptions(DICT_TYPE.TICKET_STATUS).filter(
    (o) => String(o.value) !== current,
  )
  if (!candidates.length) {
    uni.showToast({ title: '暂无可切换状态', icon: 'none' })
    return
  }
  try {
    const index = await showActionSheet({
      title: '变更状态',
      items: candidates.map((o) => ({ label: o.label })),
    })
    const status = String(candidates[index].value)
    await transitionTicket({ id: ticket.value!.id, status })
    uni.showToast({ title: '状态已更新', icon: 'success' })
    await load(ticket.value!.id)
  } catch {
    /* cancelled */
  }
}

async function onDelete() {
  const { confirmed } = await showConfirm({
    title: '删除工单',
    content: '确定删除该工单？删除后不可恢复。',
    tone: 'danger',
    confirmText: '删除',
  })
  if (!confirmed) return
  await deleteTicket(ticketId.value)
  uni.showToast({ title: '已删除', icon: 'success' })
  setTimeout(() => uni.navigateBack(), 400)
}

onMounted(() => {
  preloadDicts([DICT_TYPE.TICKET_STATUS, DICT_TYPE.TICKET_PRIORITY])
})

onLoad(async (options) => {
  ticketId.value = Number(options?.id)
  if (!ticketId.value) return
  await preloadDicts([DICT_TYPE.TICKET_STATUS, DICT_TYPE.TICKET_PRIORITY])
  await load(ticketId.value)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.ticket-page {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: $color-bg-page;

  &.has-page-footer {
    padding-bottom: calc(260rpx + env(safe-area-inset-bottom));
  }
}

.ticket-hero {
  padding: 32rpx;
  margin-bottom: $card-gap;
}

.ticket-hero__top {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.ticket-hero__title {
  display: block;
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.4;
}

.ticket-hero__no {
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

  &--danger {
    color: $color-danger;
  }
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
  background: $color-primary;
  flex-shrink: 0;
}

.timeline__dot--latest {
  box-shadow: 0 0 0 6rpx rgba(20, 184, 166, 0.12);
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

.timeline__operator {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
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
  word-break: break-word;
}

.timeline__empty {
  display: block;
  padding: 8rpx 4rpx 4rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.ticket-comment-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 12rpx 24rpx calc(12rpx + env(safe-area-inset-bottom));
  background: $color-bg-card;
  border-top: 1px solid $color-border-light;
  box-sizing: border-box;

  &--with-footer {
    bottom: calc(120rpx + env(safe-area-inset-bottom));
    padding-bottom: 12rpx;
  }
}

.ticket-comment-bar__input {
  flex: 1;
  height: 72rpx;
  padding: 0 20rpx;
  border-radius: $radius-full;
  background: $color-bg-muted;
  font-size: $font-size-base;
}

.ticket-comment-bar__send {
  flex-shrink: 0;
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 28rpx;
  margin: 0;
  border-radius: $radius-full;
  background: $color-primary;
  color: $color-text-inverse;
  font-size: $font-size-base;
}
</style>
