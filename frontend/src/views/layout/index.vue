<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ 'is-collapse': isCollapse }">
      <div class="logo">
        <div class="logo-icon">
          <el-icon :size="28"><component :is="ElementPlusIconsVue.Management" /></el-icon>
        </div>
        <div class="logo-text" v-show="!isCollapse">
          <span class="logo-title">{{ sitePlatformName }}</span>
          <span class="logo-subtitle">{{ sitePlatformSubtitle }}</span>
        </div>
      </div>

      <div class="menu-wrapper">
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          :collapse="isCollapse"
          :collapse-transition="true"
          router
          @select="handleMenuSelect"
        >
          <el-menu-item index="/dashboard" class="menu-item-dashboard">
            <el-icon><component :is="ElementPlusIconsVue.HomeFilled" /></el-icon>
            <template #title>
              <span>工作台</span>
            </template>
          </el-menu-item>

          <!-- 动态菜单：根据用户权限显示 -->
          <template v-for="menu in userMenus" :key="menu.id">
            <el-sub-menu
              v-if="menu.children && menu.children.length > 0"
              :index="String(menu.id)"
              class="menu-group"
              @open="handleSubMenuOpen(menu)"
              @close="handleSubMenuClose(menu)"
            >
              <template #title>
                <el-icon :class="{ 'is-spin-once': isSystemMenu(menu) && menuIconSpinKey === SYSTEM_MENU_ID }">
                  <component :is="getIconComponent(menu.icon)" />
                </el-icon>
                <span>{{ menu.name }}</span>
              </template>
              <el-menu-item
                v-for="child in menu.children"
                :key="child.id"
                :index="resolveMenuIndex(child)"
                class="menu-item"
              >
                <el-icon><component :is="getIconComponent(child.icon)" /></el-icon>
                <template #title>
                  <span>{{ child.name }}</span>
                </template>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="resolveMenuIndex(menu)" class="menu-item">
              <el-icon><component :is="getIconComponent(menu.icon)" /></el-icon>
              <template #title>
                <span>{{ menu.name }}</span>
              </template>
            </el-menu-item>
          </template>
        </el-menu>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-container">
      <!-- 顶部导航 -->
      <div class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <component :is="isCollapse ? ElementPlusIconsVue.Expand : ElementPlusIconsVue.Fold" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-popover trigger="click" placement="bottom-end" :width="340" @show="loadMessages">
            <template #reference>
              <el-badge :value="messageStore.totalUnread" :hidden="!messageStore.totalUnread" class="notice-badge">
                <el-icon class="notice-icon" :size="20"><component :is="ElementPlusIconsVue.Bell" /></el-icon>
              </el-badge>
            </template>
            <el-tabs v-model="messageTab" class="message-tabs">
              <el-tab-pane name="inbox">
                <template #label>
                  <span>业务消息</span>
                  <el-badge v-if="messageStore.inboxCount" :value="messageStore.inboxCount" class="tab-badge" />
                </template>
                <div class="notice-panel-header">
                  <span>工单 / 审批等业务提醒</span>
                  <el-button link type="primary" @click="handleReadAllInbox">全部已读</el-button>
                </div>
                <div class="notice-list" v-if="inboxList.length">
                  <div
                    class="notice-item"
                    :class="{ unread: item.readStatus === 0 }"
                    v-for="item in inboxList"
                    :key="item.id"
                    @click="handleReadInbox(item)"
                  >
                    <div class="notice-title">{{ item.title }}</div>
                    <div class="notice-content">{{ item.content }}</div>
                    <div class="notice-time">{{ item.createTime }}</div>
                  </div>
                </div>
                <el-empty v-else description="暂无业务消息" :image-size="60" />
              </el-tab-pane>
              <el-tab-pane name="announce">
                <template #label>
                  <span>系统通知</span>
                  <el-badge v-if="messageStore.announceCount" :value="messageStore.announceCount" class="tab-badge" />
                </template>
                <div class="notice-panel-header">
                  <span>平台公告与通知</span>
                  <el-button link type="primary" @click="handleReadAllAnnounce">全部已读</el-button>
                </div>
                <div class="notice-list" v-if="announceList.length">
                  <div
                    class="notice-item"
                    :class="{ unread: !item.isRead }"
                    v-for="item in announceList"
                    :key="item.id"
                    @click="handleReadAnnounce(item)"
                  >
                    <div class="notice-title">{{ item.title }}</div>
                    <div class="notice-content">{{ item.content }}</div>
                    <div class="notice-time">{{ item.createTime }}</div>
                  </div>
                </div>
                <el-empty v-else description="暂无系统通知" :image-size="60" />
                <div class="panel-footer">
                  <el-button link type="primary" @click="router.push('/message/notice')">管理通知</el-button>
                </div>
              </el-tab-pane>
              <el-tab-pane name="chat">
                <template #label>
                  <span>聊天</span>
                  <el-badge v-if="messageStore.chatCount" :value="messageStore.chatCount" class="tab-badge" />
                </template>
                <div class="chat-tab-body">
                  <p class="chat-hint">即时聊天未读 {{ messageStore.chatCount }} 条</p>
                  <el-button type="primary" @click="router.push('/message/chat')">进入聊天</el-button>
                </div>
              </el-tab-pane>
            </el-tabs>
          </el-popover>
          <!-- 主题色选择器 -->
          <el-popover
            trigger="click"
            placement="bottom-end"
            :width="280"
            :show-arrow="false"
          >
            <template #reference>
              <el-icon class="theme-icon" :size="20"><component :is="ElementPlusIconsVue.Brush" /></el-icon>
            </template>
            <div class="theme-picker-content">
              <div class="theme-picker-title">主题色</div>
              <div class="preset-colors">
                <div
                  v-for="item in presetColors"
                  :key="item.name"
                  class="preset-color"
                  :class="{ active: currentColor === item.color }"
                  :style="{ backgroundColor: item.color }"
                  :title="item.label"
                  @click="handleColorChange(item.color)"
                />
              </div>
              <el-color-picker
                v-model="currentColor"
                show-alpha
                size="default"
                @change="handleColorChange"
              />
            </div>
          </el-popover>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :icon="ElementPlusIconsVue.UserFilled" />
              <span class="username">{{ userStore.userInfo.nickname || '管理员' }}</span>
              <el-icon><component :is="ElementPlusIconsVue.ArrowDown" /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 内容区 -->
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
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { themePresets, applyTheme, saveTheme, getCurrentTheme, adjustColor } from '@/utils/theme'
import { resolveMenuIcon } from '@/utils/menu-icon'
import { ElMessage } from 'element-plus'
import { getMyNoticeList, readAllNotice, readNotice } from '@/api/system/notice'
import { getMyAnnounce, readAnnounce, readAllAnnounce } from '@/api/message/index'
import { useMessageStore, type InboxNoticeItem } from '@/store/message'
import type { AnnounceMyVO } from '@/types/message'
import MessageNotification from '@/components/MessageNotification.vue'
import { getConfig } from '@/api/system/auth'
import type { Component } from 'vue'
import type { MenuNode } from '@/types/menu'
import { preloadDicts } from '@/composables/useDict'
import { COMMON_DICT_TYPES } from '@/constants/dict'

const getIconComponent = (iconName?: string): Component => resolveMenuIcon(iconName) as Component

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

const isCollapse = ref(false)
const activeMenu = computed(() => route.path)
const messageTab = ref('inbox')
const inboxList = ref<InboxNoticeItem[]>([])
const announceList = ref<AnnounceMyVO[]>([])
/** 仅「系统管理」一级菜单点击时图标转一圈 */
const SYSTEM_MENU_ID = '1'
const menuIconSpinKey = ref('')
let menuIconSpinTimer: ReturnType<typeof window.setTimeout> | null = null
const MENU_ICON_SPIN_MS = 520

const isSystemMenu = (menu: MenuNode) =>
  String(menu?.id) === SYSTEM_MENU_ID || menu?.name === '系统管理'

const presetColors = [
  { name: 'default', color: '#111827', label: '深灰' },
  { name: 'blue', color: '#1890ff', label: '蓝色' },
  { name: 'green', color: '#52c41a', label: '绿色' },
  { name: 'orange', color: '#fa8c16', label: '橙色' },
  { name: 'pink', color: '#eb2f96', label: '粉色' },
  { name: 'purple', color: '#722ed1', label: '紫色' },
  { name: 'cyan', color: '#13c2c2', label: '青色' },
  { name: 'gold', color: '#faad14', label: '金色' },
  { name: 'red', color: '#f5222d', label: '红色' },
  { name: 'violet', color: '#6932c7', label: '紫罗兰' },
]

const currentColor = ref(getCurrentTheme().primaryColor)
const sitePlatformName = ref('Admin Platform')
const sitePlatformSubtitle = ref('Management System')

async function loadSiteConfig() {
  try {
    const res = await getConfig()
    if (res.data?.site) {
      if (res.data.site.platformName) sitePlatformName.value = res.data.site.platformName
      if (res.data.site.platformSubtitle) sitePlatformSubtitle.value = res.data.site.platformSubtitle
    }
  } catch {
    /* 使用默认值 */
  }
}

function handleColorChange(color: string | null) {
  if (color) {
    currentColor.value = color
    const themeEntry = Object.entries(themePresets).find(([_, config]) => config.primaryColor === color)
    if (themeEntry) {
      const [, themeConfig] = themeEntry
      applyTheme(themeConfig)
      saveTheme(themeConfig)
    } else {
      const customTheme = {
        primaryColor: color,
        primaryColorHover: adjustColor(color, 30),
        primaryColorActive: adjustColor(color, -30),
        textColorBase: '#1F2937',
        textColor2: '#6B7280',
        borderColor: '#E5E7EB',
        bgColor: '#F9FAFB',
      }
      applyTheme(customTheme)
      saveTheme(customTheme)
    }
  }
}

const userMenus = computed<MenuNode[]>(() => {
  const menus = userStore.menus || []
  const filteredMenus = menus.filter(menu => menu.type !== 3)
  const menuMap: Record<number, MenuNode> = {}
  const rootMenus: MenuNode[] = []

  filteredMenus.forEach(menu => {
    menuMap[menu.id] = { ...menu, children: [] }
  })

  filteredMenus.forEach(menu => {
    const node = menuMap[menu.id]
    if (menu.parentId === 0 || !menu.parentId) {
      rootMenus.push(node)
    } else if (menuMap[menu.parentId]) {
      menuMap[menu.parentId].children!.push(node)
    }
  })

  const cleanChildren = (items: MenuNode[]) => {
    items.forEach(menu => {
      if (menu.children && menu.children.length === 0) {
        delete menu.children
      } else if (menu.children) {
        cleanChildren(menu.children)
      }
    })
  }
  cleanChildren(rootMenus)

  return rootMenus
})

function resolveMenuIndex(menu: MenuNode) {
  const comp = menu?.component?.trim()
  if (comp && /^https?:\/\//i.test(comp)) {
    return `external:${comp}`
  }
  return menu.path || String(menu.id)
}

function findTopMenuSpinKey(index: string) {
  const key = String(index)
  if (key === '/dashboard') return '/dashboard'
  for (const menu of userMenus.value) {
    if (String(menu.id) === key) return String(menu.id)
    if (resolveMenuIndex(menu) === key) return String(menu.id)
    if (menu.children?.length) {
      for (const child of menu.children) {
        if (resolveMenuIndex(child) === key) return String(menu.id)
      }
    }
  }
  return key
}

function triggerSystemMenuIconSpin() {
  if (menuIconSpinTimer) {
    window.clearTimeout(menuIconSpinTimer)
    menuIconSpinTimer = null
  }
  menuIconSpinKey.value = ''
  requestAnimationFrame(() => {
    menuIconSpinKey.value = SYSTEM_MENU_ID
    menuIconSpinTimer = window.setTimeout(() => {
      if (menuIconSpinKey.value === SYSTEM_MENU_ID) {
        menuIconSpinKey.value = ''
      }
      menuIconSpinTimer = null
    }, MENU_ICON_SPIN_MS)
  })
}

function handleSubMenuOpen(menu: MenuNode) {
  if (isSystemMenu(menu)) {
    triggerSystemMenuIconSpin()
  }
}

function handleSubMenuClose(menu: MenuNode) {
  if (isSystemMenu(menu)) {
    triggerSystemMenuIconSpin()
  }
}

function handleMenuSelect(index: string) {
  if (findTopMenuSpinKey(index) === SYSTEM_MENU_ID) {
    triggerSystemMenuIconSpin()
  }
  const key = String(index)
  if (key.startsWith('external:')) {
    window.open(key.slice('external:'.length), '_blank')
  }
}

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await userStore.logoutAction()
    router.push('/login')
  }
}

const loadAnnounceList = async () => {
  try {
    const res = await getMyAnnounce({ pageNo: 1, pageSize: 20 })
    announceList.value = res.data?.list || []
  } catch (error) {
    console.error('加载系统通知失败', error)
  }
}

const loadMessages = async () => {
  try {
    const [inboxRes] = await Promise.all([
      getMyNoticeList(),
      loadAnnounceList(),
    ])
    inboxList.value = inboxRes.data || []
    await messageStore.refreshSummary()
  } catch (error) {
    console.error('加载消息失败', error)
  }
}

const handleReadInbox = async (item: InboxNoticeItem) => {
  if (item.readStatus === 0) {
    await readNotice(item.id)
    item.readStatus = 1
    messageStore.inboxCount = Math.max(0, messageStore.inboxCount - 1)
  }
  if (item.bizType === 'TICKET' && item.bizId) {
    router.push({ path: '/system/ticket', query: { ticketId: item.bizId } })
    return
  }
  if (item.bizType === 'APPROVAL' && item.bizId) {
    router.push({ path: '/system/approval', query: { approvalId: item.bizId } })
  }
}

const handleReadAnnounce = async (item: AnnounceMyVO) => {
  if (!item.isRead) {
    await readAnnounce(item.id)
    item.isRead = 1
    messageStore.announceCount = Math.max(0, messageStore.announceCount - 1)
  }
}

const handleReadAllInbox = async () => {
  await readAllNotice()
  ElMessage.success('业务消息已全部已读')
  inboxList.value = inboxList.value.map(item => ({ ...item, readStatus: 1 }))
  await messageStore.refreshSummary()
}

const handleReadAllAnnounce = async () => {
  await readAllAnnounce()
  ElMessage.success('系统通知已全部已读')
  announceList.value = announceList.value.map(item => ({ ...item, isRead: 1 }))
  await messageStore.refreshSummary()
}

onMounted(async () => {
  handleColorChange(currentColor.value)

  if (!userStore.menus || userStore.menus.length === 0) {
    try {
      await userStore.getUserInfo()
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }
  loadMessages()
  messageStore.initWebSocket()
  loadSiteConfig()
  preloadDicts(COMMON_DICT_TYPES).catch(() => {})
})

watch(() => messageStore.announceListTick, () => {
  loadAnnounceList()
})

watch(messageTab, (tab) => {
  if (tab === 'announce') loadAnnounceList()
})

onUnmounted(() => {
  if (menuIconSpinTimer) {
    window.clearTimeout(menuIconSpinTimer)
    menuIconSpinTimer = null
  }
  messageStore.destroyWebSocket()
})
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.sidebar {
  width: 220px;
  background: linear-gradient(180deg, #ffffff 0%, #f8f9fa 100%);
  border-right: 1px solid #e8eaed;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
}

.sidebar.is-collapse {
  width: 72px;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, var(--theme-primary-active, #374151) 100%);
  gap: 12px;
  position: relative;
  overflow: hidden;
}

.logo::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  pointer-events: none;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  backdrop-filter: blur(10px);
  flex-shrink: 0;
  transition: all 0.3s;
}

.logo:hover .logo-icon {
  transform: rotate(360deg);
  background: rgba(255, 255, 255, 0.3);
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
  white-space: nowrap;
}

.logo-title {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.logo-subtitle {
  color: rgba(255, 255, 255, 0.7);
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 0.3px;
}

.menu-wrapper {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px;
}

.menu-wrapper::-webkit-scrollbar {
  width: 4px;
}

.menu-wrapper::-webkit-scrollbar-track {
  background: transparent;
}

.menu-wrapper::-webkit-scrollbar-thumb {
  background: #d0d5dd;
  border-radius: 4px;
  transition: background 0.3s;
}

.menu-wrapper::-webkit-scrollbar-thumb:hover {
  background: #98a2b3;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 204px;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  margin-right: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notice-badge {
  cursor: pointer;
}

.notice-icon {
  color: var(--theme-text-base, #1F2937);
}

.message-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.tab-badge {
  margin-left: 6px;
}

.panel-footer {
  text-align: center;
  padding-top: 8px;
}

.chat-tab-body {
  text-align: center;
  padding: 24px 12px;
}

.chat-hint {
  color: #666;
  margin-bottom: 12px;
}

.notice-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: 600;
}

.notice-list {
  max-height: 360px;
  overflow: auto;
}

.notice-item {
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #f8f9fb;
  cursor: pointer;
}

.notice-item.unread {
  background: #eef5ff;
}

.notice-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.notice-content {
  font-size: 13px;
  color: #606266;
}

.notice-time {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.theme-icon {
  cursor: pointer;
  color: var(--theme-text-base, #1F2937);
  transition: color 0.3s;
}

.theme-icon:hover {
  color: var(--theme-primary, #111827);
}

.theme-picker-content {
  padding: 10px 0;
}

.theme-picker-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding: 0 12px;
}

.preset-colors {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  padding: 0 12px;
  margin-bottom: 12px;
}

.preset-color {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.preset-color:hover {
  transform: scale(1.1);
}

.preset-color.active {
  border-color: #303133;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin: 0 8px;
}

.main-content {
  flex: 1;
  min-height: 0;
  padding: 20px;
  background: var(--theme-bg, #f0f2f5);
  overflow: auto;
  scrollbar-gutter: stable;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

:deep(.el-menu) {
  border-right: none;
  background: transparent;
}

:deep(.el-menu--inline) {
  background: #f5f7fa;
  border-radius: 8px;
  margin: 4px 0;
}

:deep(.el-sub-menu__title),
:deep(.el-menu-item) {
  color: #475467;
  border-radius: 8px;
  margin: 2px 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 500;
}

:deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
}

:deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
  margin-left: 8px !important;
}

:deep(.el-menu-item-dashboard) {
  margin: 8px 0 16px 0;
  height: 48px;
  line-height: 48px;
  font-weight: 600;
}

:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:hover) {
  background: rgba(17, 24, 39, 0.04);
  color: var(--theme-primary, #111827);
}

:deep(.el-menu-item.is-active) {
  color: #fff;
  background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, var(--theme-primary-hover, #374151) 100%);
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(17, 24, 39, 0.15);
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: var(--theme-primary, #111827);
  background: rgba(17, 24, 39, 0.06);
}

:deep(.el-menu--collapse .el-menu-item),
:deep(.el-menu--collapse .el-sub-menu__title) {
  margin-left: 0 !important;
  justify-content: center;
}

:deep(.sidebar-menu .el-icon.is-spin-once) {
  animation: sidebar-menu-icon-spin 0.52s ease;
}

@keyframes sidebar-menu-icon-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
