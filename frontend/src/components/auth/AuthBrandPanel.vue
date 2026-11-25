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
  align-self: center;
  flex: 1 1 46%;
  min-width: 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(16px, 3vw, 40px) clamp(16px, 3vw, 44px);
  animation: authBrandIn 0.75s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.auth-brand__inner {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 420px;
  transform: translateY(-12px);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.auth-brand__visual {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: clamp(220px, 28vw, 300px);
}

.auth-brand__earth3d {
  --earth-view-size: min(320px, 72vw);
  width: var(--earth-view-size);
  height: var(--earth-view-size);
  margin: 0 auto;
  flex-shrink: 0;
  background: transparent;
  pointer-events: auto;
}

.auth-brand__title {
  margin: -4px 0 6px;
  font-size: clamp(1.75rem, 3.6vw, 2.35rem);
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.2;
  color: #f8fafc;
}

.auth-brand__tagline {
  margin: 0;
  font-size: clamp(14px, 1.4vw, 16px);
  color: rgba(226, 232, 240, 0.84);
  letter-spacing: 0.1em;
  font-weight: 400;
}

@keyframes authBrandIn {
  from {
    opacity: 0;
    transform: translateX(-24px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-brand {
    animation: none !important;
  }
}

@media (max-width: 960px) {
  .auth-brand {
    position: relative;
    align-self: stretch;
    height: auto;
    flex: none;
    width: 100%;
    padding: 24px 20px 18px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }
  .auth-brand__inner {
    transform: none;
  }
  .auth-brand__visual {
    min-height: 220px;
  }
  .auth-brand__earth3d {
    --earth-view-size: min(240px, 68vw);
  }
}
</style>
