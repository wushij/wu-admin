<template>
  <div class="layout-container">
    <LayoutSidebar
      :is-collapse="isCollapse"
      :site-platform-name="sitePlatformName"
      :site-platform-subtitle="sitePlatformSubtitle"
      :active-menu="activeMenu"
      :user-menus="userMenus"
      :menu-icon-spin-key="menuIconSpinKey"
      :system-menu-id="SYSTEM_MENU_ID"
      :get-icon-component="getIconComponent"
      :is-system-menu="isSystemMenu"
      :resolve-menu-index="resolveMenuIndex"
      @menu-select="handleMenuSelect"
      @sub-menu-open="handleSubMenuOpen"
      @sub-menu-close="handleSubMenuClose"
    />

    <div class="main-container">
      <LayoutHeader
        v-model:message-tab="messageTab"
        :is-collapse="isCollapse"
        :current-page-title="currentPageTitle"
        :current-color="currentColor"
        :active-preset-id="activePresetId"
        :header-avatar-src="headerAvatarSrc"
        :nickname="userStore.userInfo.nickname || '管理员'"
        :inbox-list="inboxList"
        :announce-list="announceList"
        @toggle-collapse="toggleCollapse"
        @load-messages="loadMessages"
        @read-inbox="handleReadInbox"
        @read-announce="handleReadAnnounce"
        @read-all-inbox="handleReadAllInbox"
        @read-all-announce="handleReadAllAnnounce"
        @preset-select="handlePresetSelect"
        @color-change="handleColorChange"
        @user-command="handleCommand"
      />

      <LayoutTagsView />

      <div class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>

    <MessageNotification />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import MessageNotification from '@/components/MessageNotification.vue'
import LayoutSidebar from './LayoutSidebar.vue'
import LayoutHeader from './LayoutHeader.vue'
import LayoutTagsView from './LayoutTagsView.vue'
import { useLayoutSite } from '../composables/useLayoutSite'
import { useLayoutTheme } from '../composables/useLayoutTheme'
import { useLayoutMenu } from '../composables/useLayoutMenu'
import { useLayoutMessages } from '../composables/useLayoutMessages'
import { useLayoutBootstrap } from '../composables/useLayoutBootstrap'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const activeMenu = computed(() => route.path)
const currentPageTitle = computed(() => {
  const title = route.meta?.title
  return typeof title === 'string' ? title : ''
})

const { sitePlatformName, sitePlatformSubtitle, loadSiteConfig } = useLayoutSite()
const { currentColor, activePresetId, handlePresetSelect, handleColorChange, initTheme } = useLayoutTheme()
const {
  SYSTEM_MENU_ID,
  menuIconSpinKey,
  userMenus,
  getIconComponent,
  isSystemMenu,
  resolveMenuIndex,
  handleSubMenuOpen,
  handleSubMenuClose,
  handleMenuSelect,
} = useLayoutMenu()
const {
  messageTab,
  inboxList,
  announceList,
  loadMessages,
  handleReadInbox,
  handleReadAnnounce,
  handleReadAllInbox,
  handleReadAllAnnounce,
} = useLayoutMessages()

const headerAvatarSrc = computed(() => userStore.userInfo.avatar || undefined)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = async (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    await userStore.logoutAction()
    router.push('/login')
  }
}

useLayoutBootstrap({ initTheme, loadSiteConfig, loadMessages })
</script>

<style scoped src="./layout-shell.css"></style>
