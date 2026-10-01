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
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.auth-container.auth-split {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  justify-content: center;
  min-height: 100vh;
  padding: clamp(16px, 4vw, 40px);
  box-sizing: border-box;
}

.auth-split-line {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 46%;
  width: 1px;
  background: rgba(255, 255, 255, 0.12);
  z-index: 2;
  pointer-events: none;
}

.auth-form-pane {
  flex: 1 1 54%;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(16px, 3vw, 32px);
  border-left: none;
  animation: authFormPaneIn 0.85s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

@keyframes authFormPaneIn {
  from { opacity: 0; transform: translateX(28px); }
  to { opacity: 1; transform: translateX(0); }
}

@media (prefers-reduced-motion: reduce) {
  .auth-form-pane { animation: none !important; }
}

@media (max-width: 960px) {
  .auth-container.auth-split {
    flex-direction: column;
    max-width: 460px;
    align-items: stretch;
    padding-bottom: 60px;
  }
  .auth-split-line {
    display: none;
  }
  .auth-form-pane {
    flex: none;
    width: 100%;
    border-left: none;
    border-top: 1px solid rgba(255, 255, 255, 0.12);
    padding-top: 20px;
  }
}

.auth-footer-container {
  position: fixed;
  bottom: 18px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 1120px;
  z-index: 99;
  pointer-events: none;
  display: flex;
  justify-content: flex-start;
  padding: 0 clamp(16px, 4vw, 40px);
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
    justify-content: center;
  }
  .auth-footer-container :deep(.site-footer) {
    margin-left: 0;
    transform: none;
  }
}
</style>
