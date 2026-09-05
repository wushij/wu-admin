<template>
  <footer
    v-if="hasContent"
    class="site-footer"
    :class="[`site-footer--${theme}`, { 'site-footer--inline': inline }]"
  >
    <div class="site-footer__inner">
      <span v-if="displayCopyright" class="site-footer__copyright">
        {{ displayCopyright }}
      </span>

      <span v-if="displayCopyright && showIcp" class="site-footer__divider">|</span>

      <a
        v-if="showIcp"
        class="site-footer__icp-link"
        :href="targetIcpUrl"
        target="_blank"
        rel="noopener noreferrer"
        title="点击跳转至工业和信息化部政务服务平台"
      >
        {{ icpNumber || '粤ICP备2026045343号-1' }}
      </a>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    copyright?: string
    icpEnabled?: boolean
    icpNumber?: string
    icpUrl?: string
    theme?: 'dark' | 'light' | 'transparent'
    inline?: boolean
  }>(),
  {
    copyright: '',
    icpEnabled: true,
    icpNumber: '',
    icpUrl: '',
    theme: 'transparent',
    inline: false,
  }
)

const showIcp = computed(() => {
  return props.icpEnabled !== false && Boolean(props.icpNumber || '粤ICP备2026045343号-1')
})

const displayCopyright = computed(() => {
  return props.copyright ? props.copyright.trim() : ''
})

const targetIcpUrl = computed(() => {
  return props.icpUrl ? props.icpUrl.trim() : 'https://beian.miit.gov.cn/'
})

const hasContent = computed(() => {
  return Boolean(displayCopyright.value || showIcp.value)
})
</script>

<style scoped>
.site-footer {
  width: 100%;
  padding: 14px 20px;
  box-sizing: border-box;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.6;
  z-index: 10;
}

.site-footer__inner {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px 14px;
}

.site-footer__divider {
  opacity: 0.5;
  font-weight: 400;
}

.site-footer__icp-link {
  text-decoration: none !important;
  border-bottom: none !important;
  transition: opacity 0.2s ease;
  display: inline-flex;
  align-items: center;
  letter-spacing: 0.02em;
  font-weight: 500;
  cursor: pointer;
}

.site-footer__icp-link:hover {
  text-decoration: none !important;
  border-bottom: none !important;
  opacity: 0.8;
}

/* 登录/注册页（星空黑色背景）：使用清晰明亮的白字，无横线 */
.site-footer--transparent {
  color: #ffffff;
}

.site-footer--transparent .site-footer__copyright,
.site-footer--transparent .site-footer__divider,
.site-footer--transparent .site-footer__icp-link {
  color: #ffffff;
  text-decoration: none !important;
  border-bottom: none !important;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.8);
}

.site-footer--transparent .site-footer__icp-link:hover {
  color: #ffffff;
  opacity: 0.85;
}

/* 暗色主题：白色字体，无横线 */
.site-footer--dark {
  color: #ffffff;
}

.site-footer--dark .site-footer__copyright,
.site-footer--dark .site-footer__divider,
.site-footer--dark .site-footer__icp-link {
  color: #ffffff;
  text-decoration: none !important;
  border-bottom: none !important;
}

/* 管理后台浅色主题：黑色字体，无横线 */
.site-footer--light {
  color: #303133;
}

.site-footer--light .site-footer__copyright,
.site-footer--light .site-footer__divider,
.site-footer--light .site-footer__icp-link {
  color: #303133;
  text-decoration: none !important;
  border-bottom: none !important;
}

.site-footer--inline {
  padding: 10px 0;
}
</style>
