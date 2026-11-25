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
        :color-mode="colorMode"
        :is-dark="isDark"
        :active-preset-id="activePresetId"
        :header-avatar-src="headerAvatarSrc"
        :nickname="userStore.userInfo.nickname || '管理员'"
        :inbox-list="inboxList"
        :announce-list="announceList"
        @toggle-collapse="toggleCollapse"
        @toggle-dark="toggleDarkMode"
        @open-command="commandVisible = true"
        @start-tour="handleStartTour"
        @load-messages="loadMessages"
        @read-inbox="handleReadInbox"
        @read-announce="handleReadAnnounce"
        @read-all-inbox="handleReadAllInbox"
        @read-all-announce="handleReadAllAnnounce"
        @preset-select="handlePresetSelect"
        @color-change="handleColorChange"
        @color-mode-change="handleColorModeChange"
        @user-command="handleCommand"
      />

      <LayoutTagsView />

      <div class="main-content">
        <div class="main-content__body">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
        <SiteFooter
          :copyright="siteCopyright"
          :icp-enabled="siteIcpEnabled"
          :icp-number="siteIcpNumber"
          :icp-url="siteIcpUrl"
          :theme="isDark ? 'dark' : 'light'"
          inline
        />
      </div>
    </div>

    <MessageNotification />
    <AiWuAssistant />
    <CommandPalette
      v-model:visible="commandVisible"
      :is-dark="isDark"
      @toggle-dark="toggleDarkMode"
      @start-tour="handleStartTour"
    />
    <AppTour />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import MessageNotification from '@/components/MessageNotification.vue'
import AiWuAssistant from '@/components/AiWu/AiWuAssistant.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import CommandPalette from '@/components/ui/CommandPalette.vue'
import AppTour from '@/components/ui/AppTour.vue'
import { useTour } from '@/composables/ui/useTour'
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
const { startTour } = useTour()

const isCollapse = ref(false)
const commandVisible = ref(false)
const activeMenu = computed(() => route.path)
const currentPageTitle = computed(() => {
  const title = route.meta?.title
  return typeof title === 'string' ? title : ''
})

const {
  sitePlatformName,
  sitePlatformSubtitle,
  siteCopyright,
  siteIcpEnabled,
  siteIcpNumber,
  siteIcpUrl,
  loadSiteConfig,
} = useLayoutSite()
const {
  currentColor,
  colorMode,
  isDark,
  activePresetId,
  handlePresetSelect,
  handleColorChange,
  handleColorModeChange,
  toggleDarkMode,
  initTheme,
} = useLayoutTheme()
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

function handleStartTour() {
  startTour([
    {
      selector: '#tour-sidebar',
      title: '功能导航栏',
      description: '按权限展示全部后台模块，支持折叠收起与当前路由高亮定位。',
      placement: 'right',
    },
    {
      selector: '#tour-command-btn',
      title: '全局快捷命令面板 (Ctrl + K)',
      description: '随时按下 Ctrl+K 或点击这里，快速检索跳转菜单页、切换主题或发起引导。',
      placement: 'bottom',
    },
    {
      selector: '#tour-dark-btn',
      title: '亮 / 暗黑模式一键切换',
      description: '全站支持亮色与深曜石暗黑双主题无缝切换，所有页面与图表同步自适应。',
      placement: 'bottom',
    },
    {
      selector: '#tour-theme-btn',
      title: '品牌主色与外观配置',
      description: '支持 8 套精选预设主色与自定义取色，实时映射整站按钮、标签与焦点状态。',
      placement: 'bottom',
    },
    {
      selector: '#tour-ai-float-btn',
      title: 'AI wu 智能副驾',
      description: '点击右下角悬浮助手，可结合本地 RAG 知识库实时答疑并查阅历史会话。',
      placement: 'left',
    },
  ])
}

useLayoutBootstrap({ initTheme, loadSiteConfig, loadMessages })
</script>

<style scoped src="./layout-shell.css"></style>
