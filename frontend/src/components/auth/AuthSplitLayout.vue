<template>
  <div class="auth-page">
    <AuthParticleBackground />
    <div class="auth-container auth-split">
      <AuthBrandPanel :title="title" :tagline="tagline" />
      <div class="auth-split-line" />
      <div class="auth-form-pane">
        <slot />
      </div>
      <div class="auth-footer-container">
        <SiteFooter
          :copyright="siteStore.siteConfig.copyright"
          :icp-enabled="siteStore.siteConfig.icpEnabled"
          :icp-number="siteStore.siteConfig.icpNumber"
          :icp-url="siteStore.siteConfig.icpUrl"
          theme="transparent"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AuthParticleBackground from './AuthParticleBackground.vue'
import AuthBrandPanel from './AuthBrandPanel.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import { useSiteStore } from '@/store/site'

const siteStore = useSiteStore()

defineProps<{
  title: string
  tagline: string
}>()

onMounted(() => {
  siteStore.ensureConfigLoaded()
})
</script>

<style scoped>
.auth-page {
  height: 100vh;
  height: 100dvh;
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-width: none;
}

.auth-page::-webkit-scrollbar {
  display: none;
}

.auth-container.auth-split {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1140px;
  margin: 0 auto;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: clamp(16px, 2.5vh, 32px) clamp(20px, 4vw, 40px) clamp(44px, 6vh, 56px);
  box-sizing: border-box;
}

.auth-split-line {
  position: absolute;
  top: 18%;
  bottom: 18%;
  left: 46%;
  width: 1px;
  background: linear-gradient(
    to bottom,
    transparent 0%,
    rgba(255, 255, 255, 0.1) 25%,
    rgba(255, 255, 255, 0.1) 75%,
    transparent 100%
  );
  z-index: 2;
  pointer-events: none;
}

.auth-form-pane {
  flex: 1 1 54%;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(12px, 2.5vw, 32px);
  border-left: none;
  animation: authFormPaneIn 0.85s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

@keyframes authFormPaneIn {
  from {
    opacity: 0;
    transform: translateX(28px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-form-pane {
    animation: none !important;
  }
}

@media (max-width: 960px) {
  .auth-container.auth-split {
    flex-direction: column;
    max-width: 480px;
    align-items: stretch;
    padding-bottom: 24px;
  }

  .auth-split-line {
    display: none;
  }

  .auth-form-pane {
    flex: none;
    width: 100%;
    border-left: none;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    padding: 24px 0 12px;
  }
}

.auth-footer-container {
  position: fixed;
  bottom: 14px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 1140px;
  z-index: 99;
  pointer-events: none;
  display: flex;
  justify-content: flex-start;
  padding: 0 clamp(20px, 4vw, 40px);
  box-sizing: border-box;
}

.auth-footer-container :deep(.site-footer) {
  pointer-events: auto;
  margin-left: 46%;
  transform: translateX(-50%);
  padding: 0;
  width: auto;
}

@media (max-width: 960px) {
  .auth-footer-container {
    position: static;
    transform: none;
    margin-top: 12px;
    justify-content: center;
    padding: 0;
  }

  .auth-footer-container :deep(.site-footer) {
    margin-left: 0;
    transform: none;
  }
}
</style>
