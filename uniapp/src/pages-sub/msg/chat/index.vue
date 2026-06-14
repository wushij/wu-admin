<template>
  <view class="page-padded page-list">
    <view class="chat-page__head">
      <SegmentTabs v-model="mode" :tabs="tabs" />
    </view>

    <view class="chat-page__search-row">
      <view class="chat-page__search-wrap">
        <SearchBar v-model="keyword" placeholder="搜索会话" />
      </view>
      <view
        v-if="mode === 'group' && canCreate"
        class="chat-page__create-btn"
        @click="goCreateGroup"
      >
        <text class="chat-page__create-plus">+</text>
        <text class="chat-page__create-label">建群</text>
      </view>
    </view>

    <scroll-view scroll-y class="page-list__scroll">
      <template v-if="mode === 'private'">
        <ListRow
          v-for="user in filteredUsers"
          :key="user.id"
          class="list-row--chat"
          :title="user.nickname || user.username || '用户'"
          :desc="user.lastMessage || '暂无消息'"
          :time="formatListTime(user.lastMessageTime)"
          :badge="user.unreadCount || 0"
          @click="goChat('user', user.id, user.nickname || user.username || '', user.online)"
        >
          <template #lead>
            <ChatAvatar :src="user.avatar" :name="user.nickname || user.username" :online="user.online" />
          </template>
        </ListRow>
        <EmptyState v-if="!loading && !filteredUsers.length" title="暂无联系人" icon="friends-o" />
      </template>

      <template v-else>
        <ListRow
          v-for="group in filteredGroups"
          :key="group.id"
          class="list-row--chat"
          :title="group.name"
          :desc="group.lastMessage || `${group.memberCount || 0} 人`"
          :time="formatListTime(group.lastMessageTime)"
          :badge="groupUnread(group.id) || group.unreadCount || 0"
          @click="goChat('group', group.id, group.name)"
        >
          <template #lead>
            <ChatAvatar :name="group.name" :group-name="group.name" />
          </template>
        </ListRow>
        <EmptyState v-if="!loading && !filteredGroups.length" title="暂无群聊" icon="chat-o" />
      </template>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { canCreateChatGroup } from '@/api/message'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListRow from '@/components/common/ListRow/index.vue'
import ChatAvatar from '@/components/business/ChatAvatar/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { useChatSessions } from '@/composables/useChatSessions'
import { useMessageStore } from '@/store/message'
import { formatListTime } from '@/utils/format'

const tabs = [
  { key: 'private', label: '私聊' },
  { key: 'group', label: '群聊' },
]

const { mode, keyword, filteredUsers, filteredGroups, loading, refresh } = useChatSessions()
const messageStore = useMessageStore()
const canCreate = ref(false)

function groupUnread(groupId: number) {
  return messageStore.getGroupUnread(groupId)
}

function goChat(type: 'user' | 'group', id: number, name: string, online?: boolean) {
  const onlineQuery = type === 'user' && online ? '&online=1' : ''
  uni.navigateTo({
    url: `/pages-sub/msg/chat/detail?type=${type}&id=${id}&name=${encodeURIComponent(name)}${onlineQuery}`,
  })
}

function goCreateGroup() {
  uni.navigateTo({ url: '/pages-sub/msg/chat/group-form?mode=create' })
}

onMounted(async () => {
  const res = await canCreateChatGroup()
  canCreate.value = !!res.data
  await refresh()
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>

.chat-page__head {
  margin-bottom: 20rpx;
}

.chat-page__search-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.chat-page__search-wrap {
  flex: 1;
  min-width: 0;
}

.chat-page__search-wrap :deep(.search-bar) {
  margin-bottom: 0;
}

.chat-page__create-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  height: 72rpx;
  padding: 0 22rpx;
  border-radius: 36rpx;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  box-shadow: 0 8rpx 20rpx rgba(79, 70, 229, 0.22);
}

.chat-page__create-plus {
  font-size: 32rpx;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.chat-page__create-label {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: #fff;
  white-space: nowrap;
}

.page-list {
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.page-list__scroll {
  flex: 1;
  min-height: 0;
}

.page-list__scroll :deep(.list-row--chat) {
  min-height: 144rpx;
  padding: 24rpx 28rpx;
}
</style>
