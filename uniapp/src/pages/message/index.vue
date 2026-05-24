<template>
  <PageTabShell>
    <view v-if="availableTabs.length" class="message-hub">
      <SegmentTabs v-model="mode" :tabs="availableTabs" />

      <view v-if="sectionHint" class="message-hub__head">
        <text class="message-hub__hint">{{ sectionHint }}</text>
        <text
          v-if="canMarkAllRead && hasUnread"
          class="message-hub__read-all"
          @click="onReadAll"
        >
          全部已读
        </text>
      </view>

      <ListLoading v-if="loading" variant="message" />

      <view v-else class="message-list">
        <template v-if="mode === 'announce'">
          <view
            v-for="item in announceList"
            :key="item.id"
            class="message-item"
            :class="{ 'message-item--unread': item.isRead === 0 }"
            @click="goAnnounceDetail(item.id)"
          >
            <MessageListIcon icon="bell" theme="notice" />
            <view class="message-item__body">
              <view class="message-item__top">
                <text class="message-item__title">{{ item.title }}</text>
                <text class="message-item__time">{{ formatListTime(item.createTime) }}</text>
              </view>
              <text class="message-item__desc">{{ announceDesc(item) }}</text>
            </view>
          </view>
          <EmptyState v-if="!announceList.length" title="暂无公告" icon="bell" />
        </template>

        <template v-else-if="mode === 'inbox'">
          <view
            v-for="item in inboxList"
            :key="item.id"
            class="message-item"
            :class="{ 'message-item--unread': item.readStatus === 0 }"
            @click="onInboxTap(item)"
            @longpress="onInboxLongPress(item)"
          >
            <MessageListIcon icon="notes-o" theme="inbox" />
            <view class="message-item__body">
              <view class="message-item__top">
                <text class="message-item__title">{{ item.title }}</text>
                <view class="message-item__meta">
                  <text v-if="inboxBizLabel(item.bizType)" class="message-item__tag">
                    {{ inboxBizLabel(item.bizType) }}
                  </text>
                  <text class="message-item__time">{{ formatListTime(item.createTime) }}</text>
                </view>
              </view>
              <text class="message-item__desc">{{ inboxDesc(item) }}</text>
            </view>
          </view>
          <EmptyState v-if="!inboxList.length" title="暂无业务消息" icon="notes-o" />
        </template>

        <template v-else>
          <view
            v-for="item in chatSessions"
            :key="item.key"
            class="message-item message-item--chat"
            @click="goChat(item)"
          >
            <ChatAvatar
              v-if="item.type === 'user'"
              :src="item.avatar"
              :name="item.title"
              :online="item.online"
            />
            <ChatAvatar v-else :name="item.title" :group-name="item.title" />
            <view class="message-item__body">
              <view class="message-item__top">
                <text class="message-item__title">{{ item.title }}</text>
                <text class="message-item__time">{{ formatListTime(item.time) }}</text>
              </view>
              <text class="message-item__desc">{{ chatDesc(item) }}</text>
            </view>
            <view v-if="item.badge" class="message-item__badge" :class="{ 'message-item__badge--at': item.atMe }">
              {{ item.atMe ? '@' : (item.badge > 99 ? '99+' : item.badge) }}
            </view>
          </view>
          <EmptyState v-if="!chatSessions.length" title="暂无会话" icon="chat-o" />
        </template>
      </view>

      <view v-if="hasPreview && !loading" class="message-hub__more card--elevated" @click="goList">
        <ModuleIcon :icon="moreIcon" :theme="moreTheme" size="ml" />
        <view class="message-hub__more-body">
          <text class="message-hub__more-title">{{ moreTitle }}</text>
          <text class="message-hub__more-sub">{{ moreSub }}</text>
        </view>
        <IconFont name="arrow" :size="28" color="#94a3b8" />
      </view>
    </view>

    <EmptyState
      v-else
      title="暂无消息入口"
      description="请联系管理员分配消息相关权限"
      icon="chat-o"
    />
    <AppDialogHost />
  </PageTabShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import PageTabShell from '@/components/common/PageTabShell/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import IconFont from '@/components/common/IconFont/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import MessageListIcon from '@/components/business/MessageListIcon/index.vue'
import ChatAvatar from '@/components/business/ChatAvatar/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useMessageTab, type ChatSessionPreview } from '@/composables/useMessageTab'
import { useTabBarPage } from '@/composables/useTabBarPage'
import { openInboxItem, inboxBizLabel } from '@/utils/inbox-nav'
import { showConfirm } from '@/utils/app-dialog'
import { deleteNotice } from '@/api/system/notice'
import type { IconName } from '@/constants/iconfont'
import type { NoticeVO } from '@/types/message'

useTabBarPage(2)

const {
  mode,
  loading,
  availableTabs,
  announceList,
  inboxList,
  chatSessions,
  canMarkAllRead,
  listPath,
  refresh,
  markAllRead,
  announceDesc,
  inboxDesc,
  chatDesc,
  formatListTime,
} = useMessageTab()

const sectionHint = computed(() => {
  if (mode.value === 'announce') return '平台公告与通知'
  if (mode.value === 'inbox') return '工单 / 审批等业务提醒'
  return '最近会话'
})

const hasPreview = computed(() => {
  if (mode.value === 'announce') return announceList.value.length > 0
  if (mode.value === 'inbox') return inboxList.value.length > 0
  return chatSessions.value.length > 0
})

const hasUnread = computed(() => {
  if (mode.value === 'announce') return announceList.value.some((item) => item.isRead === 0)
  if (mode.value === 'inbox') return inboxList.value.some((item) => item.readStatus === 0)
  return false
})

const moreTitle = computed(() => {
  if (mode.value === 'announce') return '查看全部公告'
  if (mode.value === 'inbox') return '查看全部业务消息'
  return '进入企业 IM'
})

const moreSub = computed(() => {
  if (mode.value === 'announce') return '平台通知与系统公告'
  if (mode.value === 'inbox') return '工单、审批等业务提醒'
  return '查看完整会话列表'
})

const moreIcon = computed((): IconName => {
  if (mode.value === 'announce') return 'bell'
  if (mode.value === 'inbox') return 'notes-o'
  return 'chat-o'
})

const moreTheme = computed(() => {
  if (mode.value === 'announce') return 'notice'
  if (mode.value === 'inbox') return 'inbox'
  return 'monitor'
})

function goAnnounceDetail(id: number) {
  uni.navigateTo({ url: `/pages-sub/msg/announce/detail?id=${id}` })
}

function onInboxTap(item: NoticeVO) {
  openInboxItem(item)
}

async function onInboxLongPress(item: NoticeVO) {
  const { confirmed } = await showConfirm({
    title: '删除消息',
    content: '确定删除该条业务消息？',
    tone: 'danger',
    confirmText: '删除',
  })
  if (!confirmed) return
  await deleteNotice(item.id)
  uni.showToast({ title: '已删除', icon: 'success' })
  await refresh()
}

function goChat(item: ChatSessionPreview) {
  const onlineQuery = item.type === 'user' && item.online ? '&online=1' : ''
  uni.navigateTo({
    url: `/pages-sub/msg/chat/detail?type=${item.type}&id=${item.id}&name=${encodeURIComponent(item.title)}${onlineQuery}`,
  })
}

function goList() {
  uni.navigateTo({ url: listPath.value })
}

async function onReadAll() {
  await markAllRead()
  uni.showToast({ title: '已全部标记已读', icon: 'success' })
}

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>

.message-hub__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20rpx 4rpx 16rpx;
}

.message-hub__hint {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.message-hub__read-all {
  font-size: $font-size-sm;
  color: $color-primary;
  font-weight: $font-weight-semibold;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding: 28rpx 24rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
  box-shadow: $shadow-card;
}

.message-item--unread {
  background: rgba(79, 70, 229, 0.06);
  border: 1px solid rgba(79, 70, 229, 0.12);
}

.message-item--chat {
  align-items: center;
  min-height: 144rpx;
}

.message-item__body {
  flex: 1;
  min-width: 0;
}

.message-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.message-item__meta {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-shrink: 0;
}

.message-item__title {
  flex: 1;
  min-width: 0;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-item__time {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.message-item__desc {
  display: block;
  margin-top: 10rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.message-item__tag {
  display: inline-block;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  font-size: $font-size-xs;
  color: $color-primary;
  background: $color-primary-muted;
  line-height: 1.4;
  white-space: nowrap;
}

.message-item__badge {
  flex-shrink: 0;
  min-width: 36rpx;
  height: 36rpx;
  padding: 0 10rpx;
  border-radius: 18rpx;
  background: $color-danger;
  color: #fff;
  font-size: 22rpx;
  line-height: 36rpx;
  text-align: center;
}

.message-item__badge--at {
  min-width: 40rpx;
  font-size: 24rpx;
  font-weight: $font-weight-bold;
}

.message-hub__more {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-top: 24rpx;
  padding: 24rpx 28rpx;
}

.message-hub__more-body {
  flex: 1;
  min-width: 0;
}

.message-hub__more-title {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.message-hub__more-sub {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}
</style>
