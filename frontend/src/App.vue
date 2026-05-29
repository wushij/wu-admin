<template>
  <div id="app">
    <router-view />
  </div>
</template>

<style>
/* 全站仅 .main-content 滚动，避免 html 与内容区双滚动条及弹窗打开时抖动 */
html,
body,
#app {
  height: 100%;
  margin: 0;
  overflow: hidden;
}

body.el-popup-parent--hidden {
  overflow: hidden !important;
  padding-right: 0 !important;
}

/* 弹窗打开时锁定主内容区，防止背景仍可滚动 */
body.el-popup-parent--hidden .layout-container .main-content {
  overflow: hidden !important;
}

/* 对话框内容过长时在弹窗内滚动 */
.el-dialog__body {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

/* 全局主题变量默认值 */
:root {
  --theme-primary: #111827;
  --theme-primary-hover: #000000;
  --theme-primary-active: #374151;
  --theme-text-base: #1F2937;
  --theme-text-secondary: #6B7280;
  --theme-border: #E5E7EB;
  --theme-bg: #f0f2f5;
  --admin-radius-sm: 6px;
  --admin-radius-md: 8px;
  --admin-radius-lg: 12px;
  --el-border-radius-base: 8px;
  --el-border-radius-small: 6px;
}

/* 使用主题变量的通用样式 */
body {
  color: var(--theme-text-base, #1F2937);
  background-color: var(--theme-bg, #F9FAFB);
}

</style>
