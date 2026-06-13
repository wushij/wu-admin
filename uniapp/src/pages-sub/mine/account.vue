<template>
  <view class="account-page">
    <ListLoading v-if="loading" />

    <template v-else-if="profile">
      <view class="account-hero">
        <view class="account-hero__pattern" />
        <view class="account-hero__glow" />
        <view class="account-hero__body">
          <view class="account-hero__avatar">
            <UserAvatar :src="profile.avatar" :name="displayName" size="lg" />
          </view>
          <view class="account-hero__info">
            <view class="account-hero__meta">
              <text class="account-hero__name">{{ displayName }}</text>
              <text class="account-hero__username">@{{ profile.username }}</text>
              <view class="account-hero__status" :class="statusClass">
                {{ formatUserStatus(profile.status) }}
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="account-section card--elevated">
        <view class="account-section__head">
          <ModuleIcon icon="contact-o" theme="indigo" size="sm" />
          <text class="account-section__title">账号信息</text>
        </view>
        <view class="account-grid">
          <view v-for="item in accountItems" :key="item.label" class="account-grid__item">
            <text class="account-grid__label">{{ item.label }}</text>
            <text class="account-grid__value">{{ item.value }}</text>
          </view>
        </view>
      </view>

      <view class="account-section card--elevated">
        <view class="account-section__head">
          <ModuleIcon icon="cluster-o" theme="dept" size="sm" />
          <text class="account-section__title">组织信息</text>
        </view>
        <view class="account-rows">
          <view v-for="item in orgItems" :key="item.label" class="account-row">
            <text class="account-row__label">{{ item.label }}</text>
            <view v-if="item.tags.length" class="account-row__tags">
              <text v-for="tag in item.tags" :key="tag" class="account-row__tag">{{ tag }}</text>
            </view>
            <text v-else class="account-row__value">{{ item.value }}</text>
          </view>
        </view>
      </view>

      <view class="account-section card--elevated">
        <view class="account-section__head">
          <ModuleIcon icon="clock-o" theme="cyan" size="sm" />
          <text class="account-section__title">登录信息</text>
        </view>
        <view class="account-rows">
          <view v-for="item in loginItems" :key="item.label" class="account-row">
            <text class="account-row__label">{{ item.label }}</text>
            <text class="account-row__value">{{ item.value }}</text>
          </view>
        </view>
      </view>

      <view class="account-section account-section--tips card--elevated">
        <view class="account-section__head">
          <ModuleIcon icon="shield-o" theme="amber" size="sm" />
          <text class="account-section__title">安全提示</text>
        </view>
        <view class="account-tips">
          <view v-for="(tip, index) in securityTips" :key="index" class="account-tips__item">
            <text class="account-tips__dot">·</text>
            <text class="account-tips__text">{{ tip }}</text>
          </view>
        </view>
      </view>
    </template>

    <EmptyState v-else title="加载失败" icon="contact-o" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import UserAvatar from '@/components/business/UserAvatar/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { getProfile } from '@/api/system/profile'
import { formatDateTime, formatUserStatus } from '@/utils/format'
import type { UserProfile } from '@/types/profile'

const loading = ref(true)
const profile = ref<UserProfile | null>(null)

const displayName = computed(() => profile.value?.nickname || profile.value?.username || '—')
const postText = computed(() => profile.value?.postNames?.join('、') || '—')
const roleText = computed(() => profile.value?.roleNames?.join('、') || '—')
const roleTags = computed(() => profile.value?.roleNames || [])

const statusClass = computed(() => {
  if (profile.value?.status === 0) return 'account-hero__status--disabled'
  return 'account-hero__status--active'
})

const accountItems = computed(() => {
  const p = profile.value
  if (!p) return []
  return [
    { label: '账号 ID', value: String(p.userId) },
    { label: '登录账号', value: p.username },
    { label: '昵称', value: p.nickname || '—' },
    { label: '手机号', value: p.mobile || '未绑定' },
    { label: '邮箱', value: p.email || '未填写' },
    { label: '账号状态', value: formatUserStatus(p.status) },
  ]
})

const orgItems = computed(() => {
  const p = profile.value
  if (!p) return []
  const deptTags = p.deptName ? [p.deptName] : []
  const postTags = p.postNames?.length ? p.postNames : postText.value !== '—' ? [postText.value] : []
  return [
    { label: '部门', value: p.deptName || '—', tags: deptTags },
    { label: '岗位', value: postText.value, tags: postTags },
    { label: '角色', value: roleText.value, tags: roleTags.value },
  ]
})

const loginItems = computed(() => {
  const p = profile.value
  if (!p) return []
  return [
    { label: '最近登录', value: formatDateTime(p.lastLoginTime, true) },
    { label: '登录 IP', value: p.lastLoginIp || '—' },
    { label: '登录地点', value: p.lastLoginLocation || '—' },
    { label: '注册时间', value: formatDateTime(p.createTime, true) },
    { label: '资料更新', value: formatDateTime(p.updateTime, true) },
  ]
})

const securityTips = [
  '请勿将账号密码告知他人或在公共设备勾选「记住我」。',
  '发现陌生登录记录时，请立即修改密码并联系管理员。',
  '部门、岗位、角色由管理员分配，如需调整请联系系统管理员。',
]

onMounted(async () => {
  try {
    const res = await getProfile()
    profile.value = res.data || null
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/mine.scss';

.account-page {
  @include mine-page-bg;
  min-height: 100vh;
  box-sizing: border-box;
  padding: $page-padding-y $page-padding-x 48rpx;
}

.account-hero {
  position: relative;
  margin-bottom: $section-gap;
  border-radius: $radius-xl;
  overflow: hidden;
  background: linear-gradient(135deg, #010710 0%, #0f1a2e 55%, #1a1040 100%);
  box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.14);
}

.account-hero__pattern {
  position: absolute;
  inset: 0;
  opacity: 0.07;
  background-image: radial-gradient(rgba(255, 255, 255, 0.8) 1px, transparent 1px);
  background-size: 32rpx 32rpx;
  pointer-events: none;
}

.account-hero__glow {
  position: absolute;
  top: -30%;
  right: -8%;
  width: 260rpx;
  height: 260rpx;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.25) 0%, transparent 70%);
  pointer-events: none;
}

.account-hero__body {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 28rpx;
  padding: 40rpx 32rpx;
}

.account-hero__avatar {
  flex-shrink: 0;
  padding: 4rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.8), rgba(251, 191, 36, 0.6));
}

.account-hero__info {
  flex: 1;
  min-width: 0;
}

.account-hero__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  min-width: 0;
}

.account-hero__name {
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: #fff;
  line-height: 1.3;
  flex-shrink: 0;
}

.account-hero__username {
  font-size: $font-size-sm;
  color: rgba(255, 255, 255, 0.58);
  line-height: 1.3;
  flex-shrink: 0;
}

.account-hero__status {
  display: inline-flex;
  align-items: center;
  padding: 6rpx 20rpx;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  flex-shrink: 0;
}

.account-hero__status--active {
  color: #86efac;
  background: rgba(34, 197, 94, 0.16);
}

.account-hero__status--disabled {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.16);
}

.account-section {
  padding: 28rpx 28rpx 24rpx;
  margin-bottom: $card-gap;
}

.account-section__head {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.account-section__title {
  font-size: $font-size-md;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.account-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.account-grid__item {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  padding: 20rpx;
  border-radius: $radius-md;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.04) 0%, rgba(99, 102, 241, 0.02) 100%);
}

.account-grid__label {
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.account-grid__value {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  word-break: break-all;
  line-height: 1.4;
}

.account-rows {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.account-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  padding: 20rpx 0;
  border-bottom: 1px solid $color-border-light;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.account-row__label {
  flex-shrink: 0;
  width: 160rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.account-row__value {
  flex: 1;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  text-align: right;
  word-break: break-all;
  line-height: 1.45;
}

.account-row__tags {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12rpx;
  min-width: 0;
}

.account-row__tag {
  padding: 8rpx 20rpx;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: $color-primary;
  background: $color-primary-muted;
}

.account-section--tips {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.06) 0%, $color-bg-card 40%);
}

.account-tips {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.account-tips__item {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
}

.account-tips__dot {
  flex-shrink: 0;
  font-size: $font-size-lg;
  line-height: 1.2;
  color: #f59e0b;
  font-weight: $font-weight-bold;
}

.account-tips__text {
  flex: 1;
  font-size: $font-size-sm;
  color: $color-text-regular;
  line-height: 1.55;
}
</style>
