<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ 'is-collapse': isCollapse }">
      <div class="logo">
        <div class="logo-icon">
          <el-icon :size="28"><component :is="ElementPlusIconsVue.Management" /></el-icon>
        </div>
        <div class="logo-text" v-show="!isCollapse">
          <span class="logo-title">管理系统</span>
          <span class="logo-subtitle">Management System</span>
        </div>
      </div>
      
      <div class="menu-wrapper">
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          :collapse="isCollapse"
          :collapse-transition="true"
          router
        >
          <el-menu-item index="/dashboard" class="menu-item-dashboard">
            <el-icon><component :is="ElementPlusIconsVue.HomeFilled" /></el-icon>
            <template #title>
              <span>工作台</span>
            </template>
          </el-menu-item>
          
          <!-- 动态菜单：根据用户权限显示 -->
          <template v-for="menu in userMenus" :key="menu.id">
            <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="String(menu.id)" class="menu-group">
              <template #title>
                <el-icon><component :is="getIconComponent(menu.icon)" /></el-icon>
                <span>{{ menu.name }}</span>
              </template>
              <el-menu-item 
                v-for="child in menu.children" 
                :key="child.id" 
                :index="child.path"
                class="menu-item"
              >
                <el-icon><component :is="getIconComponent(child.icon)" /></el-icon>
                <template #title>
                  <span>{{ child.name }}</span>
                </template>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="menu.path" class="menu-item">
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
          <el-popover trigger="click" placement="bottom-end" :width="340">
            <template #reference>
              <el-badge :value="unreadCount" :hidden="!unreadCount" class="notice-badge">
                <el-icon class="notice-icon" :size="20"><component :is="ElementPlusIconsVue.Bell" /></el-icon>
              </el-badge>
            </template>
            <div class="notice-panel">
              <div class="notice-panel-header">
                <span>站内消息</span>
                <el-button link type="primary" @click="handleReadAllNotice">全部已读</el-button>
              </div>
              <div class="notice-list" v-if="noticeList.length">
                <div
                  class="notice-item"
                  :class="{ unread: item.readStatus === 0 }"
                  v-for="item in noticeList"
                  :key="item.id"
                  @click="handleReadNotice(item)"
                >
                  <div class="notice-title">{{ item.title }}</div>
                  <div class="notice-content">{{ item.content }}</div>
                  <div class="notice-time">{{ item.createTime }}</div>
                </div>
              </div>
              <el-empty v-else description="暂无消息" :image-size="60" />
            </div>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { themePresets, applyTheme, saveTheme, getCurrentTheme, adjustColor } from '@/utils/theme'
import { ElMessage } from 'element-plus'
import { getMyNoticeList, getUnreadNoticeCount, readAllNotice, readNotice } from '@/api/system/notice'

// 图标转换函数：将数据库中的图标名称转换为 Element Plus 图标组件
const getIconComponent = (iconName) => {
  if (!iconName) return ElementPlusIconsVue['Menu']
  return ElementPlusIconsVue[iconName] || ElementPlusIconsVue['Menu']
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const activeMenu = computed(() => route.path)

// 主题色配置
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
const unreadCount = ref(0)
const noticeList = ref([])

// 主题色切换
function handleColorChange(color) {
  if (color) {
    currentColor.value = color
    // 查找对应的主题配置
    const themeEntry = Object.entries(themePresets).find(([_, config]) => config.primaryColor === color)
    if (themeEntry) {
      const [themeName, themeConfig] = themeEntry
      applyTheme(themeConfig)
      saveTheme(themeConfig)
    } else {
      // 自定义颜色
      const customTheme = {
        primaryColor: color,
        primaryColorHover: adjustColor(color, 30),
        primaryColorActive: adjustColor(color, -30),
        textColorBase: '#1F2937',
        textColor2: '#6B7280',
        borderColor: '#E5E7EB',
        bgColor: '#F9FAFB'
      }
      applyTheme(customTheme)
      saveTheme(customTheme)
    }
  }
}

// 获取用户有权限的菜单
const userMenus = computed(() => {
  const menus = userStore.menus || []
  // 过滤按钮类型，只保留目录和菜单
  const filteredMenus = menus.filter(menu => menu.type !== 3)
  // 构建树形结构
  const menuMap = {}
  const rootMenus = []
  
  // 先建立映射
  filteredMenus.forEach(menu => {
    menuMap[menu.id] = {
      ...menu,
      children: []
    }
  })
  
  // 构建树
  filteredMenus.forEach(menu => {
    const node = menuMap[menu.id]
    if (menu.parentId === 0 || !menu.parentId) {
      // 根节点
      rootMenus.push(node)
    } else if (menuMap[menu.parentId]) {
      // 添加到父节点
      menuMap[menu.parentId].children.push(node)
    }
  })
  
  // 移除空的 children 数组
  const cleanChildren = (menus) => {
    menus.forEach(menu => {
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

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

const loadNotices = async () => {
  try {
    const [countRes, listRes] = await Promise.all([getUnreadNoticeCount(), getMyNoticeList()])
    unreadCount.value = countRes.data || 0
    noticeList.value = listRes.data || []
  } catch (error) {
    console.error('加载站内消息失败', error)
  }
}

const handleReadNotice = async (item) => {
  if (item.readStatus === 0) {
    await readNotice(item.id)
    item.readStatus = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  if (item.bizType === 'TICKET' && item.bizId) {
    router.push({ path: '/system/ticket', query: { ticketId: item.bizId } })
    return
  }
  if (item.bizType === 'APPROVAL' && item.bizId) {
    router.push({ path: '/system/approval', query: { approvalId: item.bizId } })
  }
}

const handleReadAllNotice = async () => {
  await readAllNotice()
  ElMessage.success('已全部标记为已读')
  unreadCount.value = 0
  noticeList.value = noticeList.value.map(item => ({ ...item, readStatus: 1 }))
}

// 初始化获取用户信息
onMounted(async () => {
  // 应用保存的主题色
  handleColorChange(currentColor.value)
  
  if (!userStore.menus || userStore.menus.length === 0) {
    try {
      await userStore.getUserInfo()
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }
  loadNotices()
})
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
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

/* 主题色选择器样式 */
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
  padding: 20px;
  background: #f0f2f5;
  overflow: auto;
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
</style>
