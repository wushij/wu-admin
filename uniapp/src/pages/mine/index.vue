<template>
  <PageTabShell>
    <MineHero
      :nickname="nickname"
      :avatar="avatar"
      :roles="roles"
      :dept-name="deptName"
      :post-names="postNames"
    />

    <MenuSection>
      <MenuCell icon="contact-o" label="编辑资料" desc="昵称、邮箱、头像" theme="indigo" @click="go('/pages-sub/mine/profile')" />
      <MenuCell icon="clock-o" label="登录记录" desc="最近登录活动" theme="cyan" @click="go('/pages-sub/mine/login-logs')" />
      <MenuCell icon="shield-o" label="修改密码" desc="密码与短信重置" theme="violet" @click="go('/pages-sub/mine/password')" />
      <MenuCell
        icon="info-o"
        label="关于应用"
        :desc="`版本 ${versionName}`"
        theme="slate"
        @click="go('/pages-sub/mine/about')"
      />
    </MenuSection>

    <button class="logout-btn" @click="onLogout">
      <text>退出登录</text>
    </button>

    <AppDialogHost />
  </PageTabShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import PageTabShell from '@/components/common/PageTabShell/index.vue'
import MineHero from '@/components/business/MineHero/index.vue'
import MenuSection from '@/components/common/MenuSection/index.vue'
import MenuCell from '@/components/common/MenuCell/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useUserStore } from '@/store/user'
import { useTabBarPage } from '@/composables/useTabBarPage'
import { useMinePage } from '@/composables/useMinePage'
import { showConfirm } from '@/utils/app-dialog'
import { getAppVersionName } from '@/utils/app-version'

useTabBarPage(3)

const userStore = useUserStore()
const { profile, nickname, avatar, roles } = useMinePage()
const versionName = getAppVersionName()

const deptName = computed(() => profile.value?.deptName || '')
const postNames = computed(() => profile.value?.postNames?.join('、') || '')

function go(url: string) {
  uni.navigateTo({ url })
}

async function onLogout() {
  const { confirmed } = await showConfirm({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    confirmText: '退出',
    cancelText: '取消',
    tone: 'danger',
  })
  if (!confirmed) return
  await userStore.logoutAction()
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 88rpx;
  margin-top: 8rpx;
  background: transparent;
  color: $color-danger;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  border: none;
}

.logout-btn::after {
  border: none;
}
</style>
