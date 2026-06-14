<template>
  <view class="online-detail-page" :class="{ 'has-page-footer': canForce && user }">
    <ListLoading v-if="loading" />

    <template v-else-if="user">
      <view class="online-hero card--elevated">
        <UserAvatar
          :src="String(user.avatar || '')"
          :name="displayName"
          online
          size="lg"
          class="online-hero__avatar"
        />
        <view class="online-hero__main">
          <text class="online-hero__name">{{ displayName }}</text>
          <text class="online-hero__sub">@{{ user.loginName || user.username || '—' }}</text>
          <text v-if="user.deptName" class="online-hero__dept">{{ user.deptName }}</text>
        </view>
      </view>

      <view class="section card--elevated">
        <text class="section__title">登录信息</text>
        <view class="info-grid">
          <view class="info-item">
            <text class="info-item__label">登录 IP</text>
            <text class="info-item__value">{{ user.ipaddr || '—' }}</text>
          </view>
          <view class="info-item">
            <text class="info-item__label">登录地点</text>
            <text class="info-item__value">{{ user.loginLocation || '未知位置' }}</text>
          </view>
          <view class="info-item info-item--full">
            <text class="info-item__label">登录时间</text>
            <text class="info-item__value">{{ formatDateTime(String(user.loginTime || ''), true) }}</text>
          </view>
        </view>
      </view>

      <view class="section card--elevated">
        <text class="section__title">设备信息</text>
        <view class="info-grid">
          <view class="info-item">
            <text class="info-item__label">浏览器</text>
            <text class="info-item__value">{{ user.browser || '—' }}</text>
          </view>
          <view class="info-item">
            <text class="info-item__label">操作系统</text>
            <text class="info-item__value">{{ user.os || '—' }}</text>
          </view>
        </view>
      </view>
    </template>

    <EmptyState v-else-if="!loading" title="会话不存在或已失效" icon="manager-o" />

    <PageFooter v-if="user && canForce">
      <button class="page-footer__btn page-footer__btn--danger" @click="onForce">强退下线</button>
    </PageFooter>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import UserAvatar from '@/components/business/UserAvatar/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { forceLogoutOnlineUser } from '@/api/monitor/online'
import { useModulePermission } from '@/composables/useModulePermission'
import { resolveOnlineUserDetail } from '@/utils/online-detail-cache'
import { showConfirm } from '@/utils/app-dialog'
import { formatDateTime } from '@/utils/format'
import type { OnlineUser } from '@/types/system'

const user = ref<OnlineUser | null>(null)
const loading = ref(true)
const { hasPerm } = useModulePermission('monitor:online:list')
const canForce = computed(() => hasPerm('monitor:online:forceLogout'))

const displayName = computed(() => {
  const u = user.value
  if (!u) return '用户'
  return u.nickname || u.username || u.loginName || '用户'
})

async function onForce() {
  const u = user.value
  if (!u) return
  const { confirmed } = await showConfirm({
    title: '强退确认',
    content: `确定强制下线「${displayName.value}」？`,
    tone: 'danger',
    confirmText: '强退',
  })
  if (!confirmed) return
  await forceLogoutOnlineUser(u.userId)
  uni.showToast({ title: '已强退', icon: 'success' })
  setTimeout(() => uni.navigateBack(), 400)
}

onLoad(async (options) => {
  const id = Number(options?.id)
  loading.value = true
  try {
    user.value = id ? await resolveOnlineUserDetail(id) : null
  } finally {
    loading.value = false
  }
  if (user.value) {
    uni.setNavigationBarTitle({ title: '会话详情' })
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.online-detail-page {
  min-height: 100vh;
  padding: $page-padding-y $page-padding-x;
  box-sizing: border-box;
}

.online-hero {
  display: flex;
  align-items: center;
  gap: 28rpx;
  padding: 36rpx 32rpx;
  margin-bottom: $section-gap;
}

.online-hero__avatar {
  flex-shrink: 0;
}

.online-hero__main {
  flex: 1;
  min-width: 0;
}

.online-hero__name {
  display: block;
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.35;
}

.online-hero__sub {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.online-hero__dept {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-sm;
  color: $color-primary;
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
</style>
