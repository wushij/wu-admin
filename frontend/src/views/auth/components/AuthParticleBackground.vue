<template>
  <div class="sparkles-background">
    <div class="particle-layer">
      <vue-particles id="sparkles-1" :options="authParticleOptions1" @particles-loaded="onLoaded" />
    </div>
    <div class="particle-layer">
      <vue-particles id="sparkles-2" :options="authParticleOptions2" @particles-loaded="onLoaded" />
    </div>
    <div class="particle-layer">
      <vue-particles id="sparkles-3" :options="authParticleOptions3" @particles-loaded="onLoaded" />
    </div>
    <div class="gradient-overlay" />
  </div>
</template>

<script setup lang="ts">
import {
  authParticleOptions1,
  authParticleOptions2,
  authParticleOptions3,
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
  background: #000;
  overflow: hidden;
  z-index: 0;
}

.particle-layer {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.gradient-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at center,
    transparent 42%,
    rgba(0, 0, 0, 0.22) 72%,
    rgba(0, 0, 0, 0.55) 100%
  );
  pointer-events: none;
}
</style>
