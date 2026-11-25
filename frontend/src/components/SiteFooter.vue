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

      <span v-if="displayCopyright && showIcp" class="site-footer__divider" aria-hidden="true">|</span>

      <a
        v-if="showIcp"
        class="site-footer__icp-link"
        :href="targetIcpUrl"
        target="_blank"
        rel="noopener noreferrer"
        title="点击跳转至工业和信息化部政务服务平台"
      >
        {{ icpNumber }}
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
  },
)

const showIcp = computed(() => props.icpEnabled !== false && Boolean(props.icpNumber))
const displayCopyright = computed(() => (props.copyright ? props.copyright.trim() : ''))
const targetIcpUrl = computed(() => (props.icpUrl ? props.icpUrl.trim() : 'https://beian.miit.gov.cn/'))
const hasContent = computed(() => Boolean(displayCopyright.value || showIcp.value))
</script>

<style scoped>
.site-footer {
  width: 100%;
  padding: 12px 20px;
  box-sizing: border-box;
  text-align: center;
  font-size: 13px;
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
  opacity: 0.45;
  font-weight: 400;
}

.site-footer__icp-link {
  text-decoration: none;
  transition: opacity var(--duration-fast) var(--ease-out);
  display: inline-flex;
  align-items: center;
  letter-spacing: 0.02em;
  font-weight: 500;
  cursor: pointer;
}

.site-footer__icp-link:hover {
  opacity: 0.8;
}

/* 登录/注册页深空背景 */
.site-footer--transparent,
.site-footer--transparent .site-footer__copyright,
.site-footer--transparent .site-footer__divider,
.site-footer--transparent .site-footer__icp-link {
  color: rgba(248, 250, 252, 0.88);
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.72);
}

/* 管理后台内部随主题语义变量自适应亮暗 */
.site-footer--dark,
.site-footer--light,
.site-footer--dark .site-footer__copyright,
.site-footer--dark .site-footer__divider,
.site-footer--dark .site-footer__icp-link,
.site-footer--light .site-footer__copyright,
.site-footer--light .site-footer__divider,
.site-footer--light .site-footer__icp-link {
  color: var(--color-text-secondary);
}

.site-footer--inline {
  padding: 10px 0 4px;
}
</style>
