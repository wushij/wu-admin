<template>
  <view class="group-detail page-padded">
    <ListLoading v-if="loading" />
    <template v-else-if="group">
      <SegmentTabs v-model="tab" :tabs="tabs" compact />

      <view v-if="tab === 'info'" class="panel card">
        <view class="field">
          <text class="field__label">群名称</text>
          <input v-model="form.name" class="field__input" :disabled="!canEditGroup" placeholder="群名称" />
        </view>
        <view class="field field--column">
          <text class="field__label">群公告</text>
          <textarea
            v-model="form.announcement"
            class="field__textarea"
            :disabled="!canEditGroup"
            placeholder="暂无公告"
          />
        </view>
        <view class="field field--switch">
          <text class="field__label">消息免打扰</text>
          <switch :checked="form.notifyMuted" color="#07c160" @change="toggleNotifyMuted" />
        </view>
        <button v-if="canEditGroup" class="btn btn--primary" :loading="saving" @click="saveInfo">保存修改</button>
      </view>

      <view v-else-if="tab === 'members'" class="panel">
        <view v-if="canEditGroup && availableAddUsers.length" class="add-box card">
          <text class="add-box__title">添加成员</text>
          <view class="add-list">
            <view
              v-for="user in availableAddUsers"
              :key="user.id"
              class="add-item"
              @click="toggleAddMember(user.id)"
            >
              <UserAvatar :src="user.avatar" :name="user.nickname || user.username" size="sm" />
              <text class="add-item__name">{{ user.nickname || user.username }}</text>
              <text class="add-item__check">{{ addMemberIds.includes(user.id) ? '✓' : '' }}</text>
            </view>
          </view>
          <button
            v-if="addMemberIds.length"
            class="btn btn--ghost"
            @click="submitAddMembers"
          >
            添加已选成员
          </button>
        </view>

        <view class="member-list card">
          <view v-for="m in members" :key="m.userId" class="member-row">
            <UserAvatar :src="m.avatar" :name="memberName(m)" size="sm" />
            <view class="member-row__body">
              <text class="member-row__name">{{ memberName(m) }}</text>
              <view class="member-row__tags">
                <text v-if="roleLabel(m.role)" class="tag">{{ roleLabel(m.role) }}</text>
                <text v-if="m.muted" class="tag tag--danger">禁言</text>
              </view>
            </view>
            <text
              v-if="canManageMember(m)"
              class="member-row__action"
              @click="openMemberActions(m)"
            >
              管理
            </text>
          </view>
          <EmptyState v-if="!membersLoading && !members.length" title="暂无成员" icon="friends-o" />
        </view>
      </view>

      <view v-else class="panel">
        <view class="log-list card">
          <view v-for="log in logs" :key="log.id" class="log-item">
            <text class="log-item__time">{{ formatDateTime(log.createTime, true) }}</text>
            <text class="log-item__content">{{ log.content || '—' }}</text>
          </view>
          <ListLoading v-if="logsLoading" />
          <EmptyState v-else-if="!logs.length" title="暂无群聊日志" icon="notes-o" />
        </view>
      </view>

      <view class="group-detail__footer">
        <button v-if="isGroupOwner" class="btn btn--danger" @click="handleDissolve">解散群组</button>
        <button v-else class="btn btn--warn" @click="handleQuit">退出群组</button>
      </view>
    </template>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import UserAvatar from '@/components/business/UserAvatar/index.vue'
import { useGroupDetail } from '@/composables/useGroupDetail'
import { formatDateTime } from '@/utils/format'

const {
  loading,
  saving,
  membersLoading,
  logsLoading,
  tab,
  group,
  members,
  logs,
  form,
  addMemberIds,
  availableAddUsers,
  isGroupOwner,
  canEditGroup,
  memberName,
  roleLabel,
  canManageMember,
  init,
  saveInfo,
  toggleNotifyMuted,
  toggleAddMember,
  submitAddMembers,
  openMemberActions,
  handleQuit,
  handleDissolve,
} = useGroupDetail()

const tabs = computed(() => [
  { key: 'info', label: '基本信息' },
  { key: 'members', label: '成员管理' },
  { key: 'logs', label: '群聊日志' },
])

onLoad((options) => {
  const id = Number(options?.id)
  if (!id) return
  init(id)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.group-detail {
  min-height: 100vh;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.panel {
  margin-top: 20rpx;
}

.field {
  display: flex;
  align-items: center;
  padding: 24rpx 28rpx;
  border-bottom: 1px solid $color-border-light;
}

.field--column {
  flex-direction: column;
  align-items: stretch;
}

.field--switch {
  justify-content: space-between;
  border-bottom: none;
}

.field__label {
  width: 160rpx;
  flex-shrink: 0;
  font-size: $font-size-base;
  color: $color-text-regular;
}

.field--column .field__label {
  width: auto;
}

.field__input {
  flex: 1;
  text-align: right;
  font-size: $font-size-base;
}

.field__textarea {
  width: 100%;
  min-height: 160rpx;
  margin-top: 12rpx;
  padding: 16rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  font-size: $font-size-sm;
  box-sizing: border-box;
}

.btn {
  margin: 24rpx 28rpx 8rpx;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: $radius-md;
  font-size: $font-size-base;
}

.btn--primary {
  background: #07c160;
  color: #fff;
}

.btn--ghost {
  background: $color-primary-muted;
  color: $color-primary;
}

.btn--warn {
  background: #fff7e6;
  color: #d48806;
}

.btn--danger {
  background: #fff1f0;
  color: $color-danger;
}

.add-box {
  margin-bottom: 16rpx;
  padding-bottom: 8rpx;
}

.add-box__title {
  display: block;
  padding: 20rpx 28rpx 8rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.add-item,
.member-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 28rpx;
  border-bottom: 1px solid $color-border-light;
}

.add-item__name,
.member-row__name {
  flex: 1;
  font-size: $font-size-base;
}

.add-item__check {
  color: $color-primary;
  font-size: 32rpx;
}

.member-row__body {
  flex: 1;
  min-width: 0;
}

.member-row__tags {
  display: flex;
  gap: 8rpx;
  margin-top: 6rpx;
}

.tag {
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  font-size: $font-size-xs;
  color: $color-primary;
  background: $color-primary-muted;
}

.tag--danger {
  color: $color-danger;
  background: rgba(245, 108, 108, 0.12);
}

.member-row__action {
  flex-shrink: 0;
  font-size: $font-size-sm;
  color: $color-primary;
}

.log-item {
  padding: 24rpx 28rpx;
  border-bottom: 1px solid $color-border-light;
}

.log-item__time {
  display: block;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.log-item__content {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-base;
  color: $color-text-primary;
  line-height: 1.55;
}

.group-detail__footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid $color-border-light;
}
</style>
