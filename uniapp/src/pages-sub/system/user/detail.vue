<template>

  <view class="user-detail-page" :class="{ 'has-page-footer': user && canEdit }">

    <ListLoading v-if="loading" />

    <template v-else-if="user">

      <view class="user-hero card--elevated">
        <UserAvatar
          :src="user.avatar"
          :name="user.nickname || user.username"
          size="lg"
          class="user-hero__avatar-wrap"
        />
        <view class="user-hero__main">

          <view class="user-hero__top">

            <text class="user-hero__name">{{ user.nickname || user.username }}</text>

            <view class="user-hero__tags">
              <DictTag
                :label="user.status === 1 ? '启用' : '停用'"
                :effect="user.status === 1 ? 'success' : 'danger'"
              />
              <DictTag v-if="user.loginLocked" label="登录锁定" effect="warning" />
              <DictTag v-if="user.loginIpLocked" label="IP 锁定" effect="danger" />
            </view>

          </view>

          <text class="user-hero__username">@{{ user.username }}</text>

        </view>

      </view>



      <view class="section card--elevated">

        <text class="section__title">联系方式</text>

        <view class="info-grid">

          <view class="info-item">

            <text class="info-item__label">手机号</text>

            <text class="info-item__value">{{ user.mobile || '—' }}</text>

          </view>

          <view class="info-item">

            <text class="info-item__label">邮箱</text>

            <text class="info-item__value">{{ user.email || '—' }}</text>

          </view>

        </view>

      </view>



      <view class="section card--elevated">

        <text class="section__title">组织与权限</text>

        <view class="info-grid">

          <view class="info-item">

            <text class="info-item__label">部门</text>

            <text class="info-item__value">{{ user.deptName || '—' }}</text>

          </view>

          <view class="info-item">

            <text class="info-item__label">岗位</text>

            <text class="info-item__value">{{ user.postNames || '—' }}</text>

          </view>

          <view class="info-item info-item--full">

            <text class="info-item__label">角色</text>

            <text class="info-item__value">{{ roleLabel }}</text>

          </view>

        </view>

      </view>



      <view class="section card--elevated user-lock">

        <text class="section__title">登录安全</text>

        <view class="info-grid user-lock__grid">

          <view class="info-item info-item--full user-lock__row">

            <text class="info-item__label">账号锁定</text>

            <view class="user-lock__value">

              <DictTag v-if="user.loginLocked" label="登录锁定" effect="warning" />

              <DictTag v-else label="未锁定" effect="success" />

            </view>

          </view>

          <view v-if="user.loginLocked" class="info-item info-item--full user-lock__row">

            <text class="info-item__label">账号剩余</text>

            <text class="info-item__value">{{ lockRemainText }}</text>

          </view>

          <view v-if="(user.loginFailCount ?? 0) > 0" class="info-item info-item--full user-lock__row">

            <text class="info-item__label">账号失败</text>

            <text class="info-item__value">

              {{ user.loginLocked ? '锁定前累计' : '已累计' }}失败 {{ user.loginFailCount }} 次

            </text>

          </view>

          <view class="info-item info-item--full user-lock__row">

            <text class="info-item__label">最近登录 IP</text>

            <text class="info-item__value">{{ user.loginRecentIp || '—' }}</text>

          </view>

          <view class="info-item info-item--full user-lock__row">

            <text class="info-item__label">IP 锁定</text>

            <view class="user-lock__value">

              <DictTag v-if="user.loginIpLocked" label="IP 锁定" effect="danger" />

              <DictTag v-else label="未锁定" effect="success" />

            </view>

          </view>

          <view v-if="user.loginIpLocked" class="info-item info-item--full user-lock__row">

            <text class="info-item__label">IP 剩余</text>

            <text class="info-item__value">{{ ipLockRemainText }}</text>

          </view>

          <view class="info-item info-item--full user-lock__row user-lock__hint">

            <text class="info-item__label">说明</text>

            <text class="info-item__value">登录密码错误过多时会临时锁定账号或 IP</text>

          </view>

        </view>

      </view>



      <view class="section card--elevated">

        <text class="section__title">其他信息</text>

        <view class="info-grid">

          <view class="info-item info-item--full">

            <text class="info-item__label">创建时间</text>

            <text class="info-item__value">{{ formatDateTime(user.createTime, true) }}</text>

          </view>

          <view v-if="user.remark" class="info-item info-item--full">

            <text class="info-item__label">备注</text>

            <text class="info-item__value info-item__value--multiline">{{ user.remark }}</text>

          </view>

        </view>

      </view>

      <view v-if="canEdit || canDelete" class="section card--elevated user-actions">
        <text class="section__title">管理操作</text>
        <view class="user-actions__list">
          <button
            v-if="canEdit && canUnlock"
            class="outline-btn outline-btn--warning user-actions__btn"
            @click="onUnlockLogin"
          >
            {{ unlockButtonText }}
          </button>
          <button v-if="canEdit" class="outline-btn outline-btn--primary user-actions__btn" @click="onResetPassword">重置密码</button>
          <button v-if="canDelete" class="outline-btn outline-btn--danger user-actions__btn" @click="onDelete">删除用户</button>
        </view>
      </view>

    </template>

    <EmptyState v-else title="用户不存在" icon="friends-o" />

    <PageFooter v-if="user && canEdit">
      <button class="page-footer__btn" @click="goEdit">编辑用户</button>
    </PageFooter>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import DictTag from '@/components/common/DictTag/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import UserAvatar from '@/components/business/UserAvatar/index.vue'
import { getUser, getUserRoleIds, resetUserPassword, deleteUser, unlockUserLogin } from '@/api/system/user'
import { getRoleList } from '@/api/system/role'
import { useModulePermission } from '@/composables/useModulePermission'
import { formatDateTime } from '@/utils/format'
import { formatLoginLockRemain, buildUnlockLoginConfirm, canUnlockLoginLock, hasAccountLoginLock, hasIpLoginLock } from '@/utils/login-lock'
import { showConfirm } from '@/utils/app-dialog'
import type { UserVO } from '@/types/user'
import { appendNavFromParam } from '@/utils/nav-from'
import { registerPageShallowFallback, installH5ShallowStackTrapIfNeeded } from '@/utils/navigate-back'
import { pinNavParent } from '@/utils/nav-history'
import { scheduleSyncH5BackButton } from '@/store/h5-back-button'

const USER_LIST_URL = '/pages-sub/system/user/index'

const user = ref<UserVO | null>(null)

const roleLabel = ref('—')

const loading = ref(false)

const userId = ref(0)

const { hasPerm } = useModulePermission('system:user:list')

const canEdit = computed(() => hasPerm('system:user:update'))

const canDelete = computed(() => hasPerm('system:user:delete'))

const lockRemainText = computed(() => formatLoginLockRemain(user.value?.loginLockRemainSeconds))

const ipLockRemainText = computed(() => formatLoginLockRemain(user.value?.loginIpLockRemainSeconds))

const canUnlock = computed(() => canUnlockLoginLock(user.value))

const unlockButtonText = computed(() => {
  if (hasAccountLoginLock(user.value) && hasIpLoginLock(user.value)) return '解除登录锁定'
  if (hasIpLoginLock(user.value)) return '解除 IP 锁定'
  return '解除登录锁定'
})

function goEdit() {
  uni.navigateTo({
    url: appendNavFromParam(`/pages-sub/system/user/edit?id=${userId.value}`),
  })
}



async function onUnlockLogin() {
  if (!user.value || !canUnlockLoginLock(user.value)) return
  const { title, content } = buildUnlockLoginConfirm({
    username: user.value.username,
    nickname: user.value.nickname,
    loginLocked: user.value.loginLocked,
    loginIpLocked: user.value.loginIpLocked,
    loginRecentIp: user.value.loginRecentIp,
  })
  const { confirmed } = await showConfirm({
    title,
    content,
    confirmText: '解除锁定',
  })
  if (!confirmed) return
  try {
    await unlockUserLogin(userId.value)
    uni.showToast({ title: '已解除登录锁定', icon: 'success' })
    await loadUserDetail(userId.value, { silent: true })
  } catch (e) {
    console.error(e)
  }
}

async function onResetPassword() {
  const { confirmed, content } = await showConfirm({
    title: '重置密码',
    content: `为用户「${user.value?.nickname || user.value?.username}」设置新密码`,
    confirmText: '确定',
    editable: true,
    inputType: 'password',
    placeholderText: '请输入新密码（至少 6 位）',
  })
  if (!confirmed) return
  const password = String(content || '').trim()
  if (password.length < 6) {
    uni.showToast({ title: '密码至少 6 位', icon: 'none' })
    return
  }
  try {
    await resetUserPassword(userId.value, password)
    uni.showToast({ title: '密码已重置', icon: 'success' })
  } catch (e) {
    console.error(e)
  }
}

async function onDelete() {
  const { confirmed } = await showConfirm({
    title: '删除用户',
    content: `确定删除用户「${user.value?.nickname || user.value?.username}」？`,
    confirmText: '删除',
    tone: 'danger',
  })
  if (!confirmed) return
  try {
    await deleteUser(userId.value)
    uni.showToast({ title: '已删除', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 400)
  } catch (e) {
    console.error(e)
  }
}



async function loadUserDetail(id: number, options?: { silent?: boolean }) {
  if (!options?.silent) loading.value = true

  try {
    const [userRes, roleIdsRes, roleListRes] = await Promise.all([
      getUser(id),
      getUserRoleIds(id),
      getRoleList({ status: 1 }),
    ])

    user.value = userRes.data || null

    const roleIds = roleIdsRes.data || user.value?.roleIds || []
    const roleMap = new Map((roleListRes.data || []).map((r) => [r.id, r.name]))
    const names = roleIds.map((rid) => roleMap.get(rid)).filter(Boolean)

    roleLabel.value = names.length ? names.join('、') : '未分配'
  } finally {
    if (!options?.silent) loading.value = false
  }
}

const skipNextShowRefresh = ref(true)

function syncUserIdFromRoute() {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1] as { options?: Record<string, string> } | undefined
  const id = Number(page?.options?.id)
  if (id > 0) userId.value = id
}

onLoad(async (options) => {
  userId.value = Number(options?.id)
  registerPageShallowFallback(USER_LIST_URL)
  pinNavParent(USER_LIST_URL)
  if (!userId.value) return
  await loadUserDetail(userId.value)
})

onShow(async () => {
  registerPageShallowFallback(USER_LIST_URL)
  pinNavParent(USER_LIST_URL)
  installH5ShallowStackTrapIfNeeded()
  scheduleSyncH5BackButton()
  syncUserIdFromRoute()
  if (skipNextShowRefresh.value) {
    skipNextShowRefresh.value = false
    if (userId.value && !user.value) {
      await loadUserDetail(userId.value)
    }
    return
  }
  if (!userId.value) return
  await loadUserDetail(userId.value, { silent: true })
})

</script>



<style lang="scss" scoped>


@use '@/styles/common.scss' as *;



.user-detail-page {

  min-height: 100vh;

  padding: 24rpx;

  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));

  box-sizing: border-box;

  background: $color-bg-page;



  &.has-page-footer {

    padding-bottom: calc(260rpx + env(safe-area-inset-bottom));

  }

}



.user-hero {

  display: flex;

  align-items: center;

  gap: 24rpx;

  padding: 32rpx;

  margin-bottom: $card-gap;

}



.user-hero__avatar-wrap {
  flex-shrink: 0;
}

.user-hero__avatar-wrap :deep(.user-avatar--lg.chat-avatar) {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  box-shadow: 0 8rpx 24rpx rgba(99, 102, 241, 0.18);
}

.user-hero__main {

  flex: 1;

  min-width: 0;

}



.user-hero__top {

  display: flex;

  align-items: center;

  flex-wrap: wrap;

  gap: 12rpx;

  margin-bottom: 10rpx;

}



.user-hero__name {

  font-size: $font-size-xl;

  font-weight: $font-weight-bold;

  color: $color-text-primary;

  line-height: 1.35;

}

.user-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}

.user-hero__username {

  font-size: $font-size-sm;

  color: $color-text-secondary;

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

  min-width: 0;

  padding: 20rpx 22rpx;

  border-radius: $radius-md;

  background: $color-bg-muted;



  &--full {

    grid-column: 1 / -1;

  }

}



.info-item__label {

  display: block;

  margin-bottom: 8rpx;

  font-size: $font-size-xs;

  color: $color-text-secondary;

}



.info-item__value {

  display: block;

  font-size: $font-size-sm;

  color: $color-text-primary;

  line-height: 1.45;

  word-break: break-all;



  &--multiline {

    white-space: pre-wrap;

  }

}

.user-actions {
  padding: 28rpx 32rpx;
}

.user-actions__list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.user-actions__btn {
  width: 100%;
  height: 80rpx;
}

.user-lock__row {
  display: flex;
  align-items: center;
  gap: 20rpx;

  .info-item__label {
    margin-bottom: 0;
    flex-shrink: 0;
  }

  .info-item__value {
    flex: 1;
    min-width: 0;
  }
}

.user-lock__value {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8rpx;
}

.user-lock__hint .info-item__value {
  color: $color-text-secondary;
  font-size: $font-size-xs;
}

</style>

