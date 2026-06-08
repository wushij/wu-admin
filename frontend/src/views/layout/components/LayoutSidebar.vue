<template>
  <div class="sidebar" :class="{ 'is-collapse': isCollapse }">
    <div class="logo">
      <div class="logo-icon">
        <el-icon :size="28"><component :is="ElementPlusIconsVue.Management" /></el-icon>
      </div>
      <div v-show="!isCollapse" class="logo-text">
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
        @select="(index: string) => $emit('menuSelect', index)"
      >
        <el-menu-item index="/dashboard" class="menu-item-dashboard">
          <span class="dashboard-menu-icon">
            <el-icon><component :is="ElementPlusIconsVue.Odometer" /></el-icon>
          </span>
          <template #title><span>工作台</span></template>
        </el-menu-item>

        <template v-for="menu in userMenus" :key="menu.id">
          <el-sub-menu
            v-if="menu.children && menu.children.length > 0"
            :index="String(menu.id)"
            class="menu-group"
            @open="$emit('subMenuOpen', menu)"
            @close="$emit('subMenuClose', menu)"
          >
            <template #title>
              <el-icon :class="{ 'is-spin-once': isSystemMenu(menu) && menuIconSpinKey === systemMenuId }">
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
              <template #title><span>{{ child.name }}</span></template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="resolveMenuIndex(menu)" class="menu-item">
            <el-icon><component :is="getIconComponent(menu.icon)" /></el-icon>
            <template #title><span>{{ menu.name }}</span></template>
          </el-menu-item>
        </template>
      </el-menu>
    </div>
  </div>
</template>

<script setup lang="ts">
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import type { Component } from 'vue'
import type { MenuNode } from '@/utils/menu-tree'

defineProps<{
  isCollapse: boolean
  sitePlatformName: string
  sitePlatformSubtitle: string
  activeMenu: string
  userMenus: MenuNode[]
  menuIconSpinKey: string
  systemMenuId: string
  getIconComponent: (iconName?: string) => Component
  isSystemMenu: (menu: MenuNode) => boolean
  resolveMenuIndex: (menu: MenuNode) => string
}>()

defineEmits<{
  menuSelect: [index: string]
  subMenuOpen: [menu: MenuNode]
  subMenuClose: [menu: MenuNode]
}>()
</script>

<style scoped src="./layout-sidebar.css"></style>
