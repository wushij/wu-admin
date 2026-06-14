<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <ModuleDarkHero
      title="在线用户"
      subtitle="会话管理与强退"
      icon="manager-o"
      theme="online"
      :count="filteredList.length"
      count-label="在线"
    />
    <SearchBar v-model="keyword" placeholder="搜索昵称 / 账号 / IP" @search="() => {}" />

    <ListLoading v-if="loading && !list.length" />

    <scroll-view v-else scroll-y class="page-list__scroll">
      <ListCard v-for="user in filteredList" :key="user.userId">
        <view class="online-row">
          <UserAvatar
            :src="user.avatar"
            :name="displayName(user)"
            online
            size="lg"
            class="online-row__avatar"
          />
          <view class="online-row__main" @click="goDetail(user)">
            <view class="online-row__top">
              <text class="online-row__name">{{ displayName(user) }}</text>
              <IconFont name="arrow" :size="28" color="#c0c4cc" />
            </view>
            <text class="online-row__meta">{{ user.deptName || '未分配部门' }} · {{ user.loginName || '—' }}</text>
            <text class="online-row__meta">{{ user.ipaddr || '—' }} · {{ user.loginLocation || '未知位置' }}</text>
            <text class="online-row__meta online-row__meta--muted">
              {{ user.browser || '—' }} / {{ user.os || '—' }} · {{ formatDateTime(user.loginTime) }}
            </text>
          </view>
          <view v-if="canForce" class="online-row__aside">
            <button class="outline-btn outline-btn--danger online-row__force-btn" @click.stop="onForce(user)">
              强退
            </button>
          </view>
        </view>
      </ListCard>
      <EmptyState v-if="!loading && !filteredList.length" title="暂无在线用户" icon="manager-o" />
    </scroll-view>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import IconFont from '@/components/common/IconFont/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import UserAvatar from '@/components/business/UserAvatar/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { getOnlineUserList, forceLogoutOnlineUser } from '@/api/monitor/online'
import { useModulePermission } from '@/composables/useModulePermission'
import { setOnlineUserDetail } from '@/utils/online-detail-cache'
import { showConfirm } from '@/utils/app-dialog'
import { formatDateTime } from '@/utils/format'
import type { OnlineUser } from '@/types/system'

const { allowed, hasPerm } = useModulePermission('monitor:online:list')
const list = ref<OnlineUser[]>([])
const keyword = ref('')
const loading = ref(false)
const canForce = computed(() => hasPerm('monitor:online:forceLogout'))

const filteredList = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return list.value
  return list.value.filter((u) => {
    const hay = [u.nickname, u.username, u.loginName, u.ipaddr, u.deptName].filter(Boolean).join(' ').toLowerCase()
    return hay.includes(q)
  })
})

function displayName(user: OnlineUser) {
  return user.nickname || user.username || user.loginName || '用户'
}

function goDetail(user: OnlineUser) {
  setOnlineUserDetail(user as unknown as Record<string, unknown>)
  uni.navigateTo({ url: `/pages-sub/monitor/online-detail?id=${user.userId}` })
}

async function refresh() {
  loading.value = true
  try {
    const res = await getOnlineUserList()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function onForce(user: OnlineUser) {
  const { confirmed } = await showConfirm({
    title: '强退确认',
    content: `确定强制下线「${displayName(user)}」？`,
    tone: 'danger',
    confirmText: '强退',
  })
  if (!confirmed) return
  await forceLogoutOnlineUser(user.userId)
  uni.showToast({ title: '已强退', icon: 'success' })
  await refresh()
}

onMounted(refresh)
onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.page-list__scroll {
  height: calc(100vh - 280rpx);
}

.online-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.online-row__avatar {
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 4rpx;
}

.online-row__main {
  flex: 1;
  min-width: 0;
}

.online-row__aside {
  flex-shrink: 0;
  align-self: center;
  padding-left: 20rpx;
  margin-left: 4rpx;
  border-left: 1px solid $color-border-light;
}

.online-row__force-btn {
  min-width: 96rpx;
  height: 56rpx;
  padding: 0 20rpx;
  font-size: $font-size-sm;
}

.online-row__top {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.online-row__name {
  flex: 1;
  min-width: 0;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.online-row__meta {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-sm;
  color: $color-text-regular;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &--muted {
    color: $color-text-secondary;
    font-size: $font-size-xs;
  }
}
</style>
