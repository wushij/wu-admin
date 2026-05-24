<template>
  <aside class="auth-brand">
    <div class="auth-brand__inner">
      <div class="auth-brand__visual">
        <div class="auth-brand__earth3d" role="img" :aria-label="`3D 地球 · ${title}`">
          <Earth3D transparent :show-stars="false" />
        </div>
      </div>
      <h1 class="auth-brand__title">{{ title }}</h1>
      <p class="auth-brand__tagline">{{ tagline }}</p>
    </div>
  </aside>
</template>

<script setup lang="ts">
import Earth3D from '@/components/earth/Earth3D.vue'

defineProps<{
  title: string
  tagline: string
}>()
</script>

<style scoped>
.auth-brand {
  position: sticky;
  top: 0;
  align-self: flex-start;
  flex: 1 1 46%;
  min-width: 0;
  height: 100vh;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(24px, 4vw, 56px) clamp(20px, 3vw, 48px);
  animation: authBrandIn 0.85s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.auth-brand__visual {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: clamp(280px, 38vw, 400px);
}

.auth-brand__earth3d {
  --earth-view-size: min(420px, 88vw);
  width: var(--earth-view-size);
  height: var(--earth-view-size);
  margin: 0 auto;
  flex-shrink: 0;
  background: transparent;
  pointer-events: auto;
  filter: drop-shadow(0 0 36px rgba(56, 189, 248, 0.22));
}

.auth-brand__inner {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 420px;
  transform: translateY(-58px);
}

.auth-brand__title {
  margin: -10px 0 2px;
  font-size: clamp(1.75rem, 3.8vw, 2.5rem);
  font-weight: 800;
  letter-spacing: 0.04em;
  line-height: 1.2;
  color: #fff;
  text-shadow: 0 0 28px rgba(255, 255, 255, 0.2), 0 4px 20px rgba(0, 0, 0, 0.45);
}

.auth-brand__tagline {
  margin: 0;
  font-size: clamp(15px, 1.6vw, 17px);
  color: rgba(230, 228, 250, 0.92);
  letter-spacing: 0.12em;
  font-weight: 500;
}

@keyframes authBrandIn {
  from { opacity: 0; transform: translateX(-28px); }
  to { opacity: 1; transform: translateX(0); }
}

@media (prefers-reduced-motion: reduce) {
  .auth-brand { animation: none !important; }
}

@media (max-width: 960px) {
  .auth-brand {
    position: relative;
    align-self: stretch;
    height: auto;
    flex: none;
    width: 100%;
    padding: 28px 20px 22px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }
  .auth-brand__inner { transform: translateY(-48px); }
  .auth-brand__visual { min-height: 300px; }
  .auth-brand__earth3d { --earth-view-size: min(340px, 82vw); }
}

@media (max-width: 480px) {
  .auth-brand__visual { min-height: 260px; }
  .auth-brand__earth3d { --earth-view-size: min(300px, 90vw); }
}
</style>
