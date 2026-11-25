<template>
  <div class="sparkles-background" aria-hidden="true">
    <!-- 16:9 融合暗夜极光、同心圆弧、等距网格与棱镜磨砂玻璃的集合底图 -->
    <div class="auth-bg-artwork" />
    <!-- 精简后的柔和呼吸星尘粒子层（动静结合，不遮挡底图几何细节） -->
    <div class="particle-layer">
      <vue-particles id="sparkles-1" :options="authParticleOptions1" @particles-loaded="onLoaded" />
    </div>
    <div class="particle-layer">
      <vue-particles id="sparkles-2" :options="authParticleOptions2" @particles-loaded="onLoaded" />
    </div>
    <!-- 轻柔暗角与表单可读性暗影罩层 -->
    <div class="gradient-overlay" />
  </div>
</template>

<script setup lang="ts">
import {
  authParticleOptions1,
  authParticleOptions2,
} from '@/constants/authParticles'

function onLoaded(container: unknown) {
  if (import.meta.env.DEV) {
    console.debug('Particles loaded', container)
  }
}
</script>

<style scoped>
.sparkles-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #060913;
  overflow: hidden;
  z-index: 0;
}

.auth-bg-artwork {
  position: absolute;
  inset: 0;
  background-color: #060913;
  background-image: url('../../assets/themes/auth-bg-aurora.webp');
  background-position: center center;
  background-size: cover;
  background-repeat: no-repeat;
  pointer-events: none;
}

.particle-layer {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.gradient-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 28% 50%, transparent 25%, rgba(6, 9, 19, 0.25) 80%),
    linear-gradient(90deg, rgba(6, 9, 19, 0.08) 0%, rgba(6, 9, 19, 0.22) 60%, rgba(6, 9, 19, 0.34) 100%);
  pointer-events: none;
}

@media (max-width: 960px) {
  .auth-bg-artwork {
    background-position: 32% center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .particle-layer {
    display: none;
  }
}
</style>
