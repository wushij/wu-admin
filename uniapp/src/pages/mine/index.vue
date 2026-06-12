<template>
  <PageTabShell>
    <MineHero
      :nickname="nickname"
      :avatar="avatar"
      :roles="roles"
      :dept-name="deptName"
      :post-names="postNames"
      :last-login-time="profile?.lastLoginTime"
      :last-login-ip="profile?.lastLoginIp"
      :status="profile?.status"
      @click="go('/pages-sub/mine/profile')"
    />

    <MenuSection
      v-for="group in menuGroups"
      :key="group.key"
      :title="group.title"
    >
      <MenuCell
        v-for="item in group.items"
        :key="item.key"
        :icon="item.icon"
        :label="item.label"
        :desc="item.desc"
        :theme="item.theme"
        :badge="item.badge"
        @click="go(item.path)"
      />
    </MenuSection>

    <button class="logout-btn" @click="onLogout">
      <text>退出登录</text>
    </button>

    <AppDialogHost />
  </PageTabShell>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import PageTabShell from '@/components/common/PageTabShell/index.vue'
import MineHero from '@/components/business/MineHero/index.vue'
import MenuSection from '@/components/common/MenuSection/index.vue'
import MenuCell from '@/components/common/MenuCell/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useUserStore } from '@/store/user'
import { useMessageStore } from '@/store/message'
import { useTabBarPage } from '@/composables/useTabBarPage'
import { useMinePage } from '@/composables/useMinePage'
import { useMineMenu } from '@/composables/useMineMenu'
import { showConfirm } from '@/utils/app-dialog'

useTabBarPage(3)

const userStore = useUserStore()
const messageStore = useMessageStore()
const { profile, nickname, avatar, roles } = useMinePage()
const { menuGroups } = useMineMenu()

const deptName = computed(() => profile.value?.deptName || '')
const postNames = computed(() => profile.value?.postNames?.join('、') || '')

const TAB_PAGES = new Set([
  '/pages/index/index',
  '/pages/work/index',
  '/pages/message/index',
  '/pages/mine/index',
])

function go(url: string) {
  if (TAB_PAGES.has(url)) {
    uni.switchTab({ url })
    return
  }
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

onMounted(() => {
  messageStore.refreshSummary()
})

onShow(() => {
  messageStore.refreshSummary()
})
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
