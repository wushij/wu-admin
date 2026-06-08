<template>
  <div class="auth-page">
    <AuthParticleBackground />
    <div class="auth-container auth-split">
      <AuthBrandPanel :title="title" :tagline="tagline" />
      <div class="auth-form-pane">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AuthParticleBackground from './AuthParticleBackground.vue'
import AuthBrandPanel from './AuthBrandPanel.vue'

defineProps<{
  title: string
  tagline: string
}>()
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

.auth-form-pane {
  flex: 1 1 54%;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(16px, 3vw, 32px);
  border-left: 1px solid rgba(255, 255, 255, 0.12);
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
  }
  .auth-form-pane {
    flex: none;
    width: 100%;
    border-left: none;
    padding-top: 4px;
  }
}
</style>
