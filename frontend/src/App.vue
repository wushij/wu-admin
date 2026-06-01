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

/* 全局主题变量默认值（石墨预设） */
:root {
  --theme-primary: #010710;
  --theme-primary-hover: #0f1a2e;
  --theme-primary-active: #000000;
  --theme-primary-muted: rgba(1, 7, 16, 0.08);
  --theme-primary-muted-strong: rgba(1, 7, 16, 0.14);
  --theme-logo-end: #000000;
  --theme-primary-rgb: 1, 7, 16;
  --theme-text-base: #1e293b;
  --theme-text-secondary: #64748b;
  --theme-border: #e2e8f0;
  --theme-bg: #f3f5f8;
  --theme-sidebar-bg: #ffffff;
  --admin-radius-sm: 6px;
  --admin-radius-md: 8px;
  --admin-radius-lg: 12px;
  --el-border-radius-base: 8px;
  --el-border-radius-small: 6px;
}

body {
  color: var(--theme-text-base);
  background-color: var(--theme-bg);
}

.theme-picker-popper {
  border-radius: 12px !important;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.12) !important;
  border: 1px solid var(--theme-border, #e2e8f0) !important;
}

</style>
