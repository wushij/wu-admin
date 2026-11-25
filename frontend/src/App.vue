<template>
  <div id="app">
    <router-view />
    <!-- 全局 7 种标准交互组件宿主：危险确认 AlertDialog、可逆撤销 Snackbar、媒体灯箱 Lightbox -->
    <AlertDialogHost />
    <SnackbarHost />
    <MediaLightboxHost />
  </div>
</template>

<script setup lang="ts">
import AlertDialogHost from '@/components/ui/AlertDialogHost.vue'
import SnackbarHost from '@/components/ui/SnackbarHost.vue'
import MediaLightboxHost from '@/components/ui/MediaLightboxHost.vue'
</script>

<style>
/* 全站仅 .main-content 滚动，避免 html 与内容区双滚动条及弹窗打开时抖动 */
html,
body,
#app {
  height: 100%;
  margin: 0;
  overflow: hidden;
}

body.el-popup-parent--hidden,
body.lightbox-scroll-locked {
  overflow: hidden !important;
  padding-right: 0 !important;
}

/* 弹窗打开时锁定主内容区，防止背景仍可滚动 */
body.el-popup-parent--hidden .layout-container .main-content,
body.lightbox-scroll-locked .layout-container .main-content {
  overflow: hidden !important;
}

.theme-picker-popper {
  border-radius: 16px !important;
  background: var(--color-bg-elevated, #ffffff) !important;
  box-shadow: var(--shadow-float) !important;
  border: 1px solid var(--color-border, #e4e4e7) !important;
  padding: 14px !important;
}
</style>
