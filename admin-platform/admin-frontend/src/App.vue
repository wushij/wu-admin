<template>
  <div id="app">
    <router-view />
  </div>
</template>

<style>
/* 修复对话框打开时页面抖动问题 */
/* Element Plus对话框打开时会添加overflow: hidden到body，导致滚动条消失页面抖动 */
/* 解决方案：让滚动条始终占位 */
html {
  overflow-y: scroll !important;
}

/* 覆盖Element Plus对话框锁定滚动的行为 */
body.el-popup-parent--hidden {
  overflow-y: scroll !important;
  padding-right: 0 !important;
}

/* 确保对话框内容区域滚动正常 */
.el-dialog__body {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

/* 防止遮罩层导致的抖动 */
.el-overlay {
  overflow-y: scroll !important;
}

/* 全局主题变量默认值 */
:root {
  --theme-primary: #111827;
  --theme-primary-hover: #000000;
  --theme-primary-active: #374151;
  --theme-text-base: #1F2937;
  --theme-text-secondary: #6B7280;
  --theme-border: #E5E7EB;
  --theme-bg: #F9FAFB;
}

/* 使用主题变量的通用样式 */
body {
  color: var(--theme-text-base, #1F2937);
  background-color: var(--theme-bg, #F9FAFB);
}

</style>
