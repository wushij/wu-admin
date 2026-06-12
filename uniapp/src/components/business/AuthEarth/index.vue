<template>

  <!-- #ifdef H5 -->

  <view class="auth-earth auth-earth--3d" :style="wrapStyle">

    <Earth3D

      transparent

      :show-bars="true"

      :show-stars="true"

      :bar-count="barCount"

      :enable-orbit="enableOrbit"

      :enable-zoom="enableZoom"

      :segments="segments"

    />

  </view>

  <!-- #endif -->

  <!-- #ifndef H5 -->

  <GlobeCanvas

    :size="size"

    color="#7ad4ff"

    glow-color="#38bdf8"

    :opacity="0.45"

    :speed="0.0018"

    vivid

  />

  <!-- #endif -->

</template>



<script setup lang="ts">

import { computed, defineAsyncComponent } from 'vue'

// #ifdef H5

const Earth3D = defineAsyncComponent(() => import('@/components/business/Earth3D/index.vue'))

// #endif

// #ifndef H5

const GlobeCanvas = defineAsyncComponent(() => import('@/components/business/GlobeCanvas/index.vue'))

// #endif



const props = withDefaults(

  defineProps<{

    size?: number

    barCount?: number

    enableOrbit?: boolean

    enableZoom?: boolean

    segments?: number

  }>(),

  {

    size: 190,

    barCount: 220,

    enableOrbit: true,

    enableZoom: false,

    segments: 96,

  },

)



const wrapStyle = computed(() => ({

  width: `${props.size}px`,

  height: `${props.size}px`,

}))

</script>



<style lang="scss" scoped>

.auth-earth--3d {

  margin: 0 auto;

  filter: drop-shadow(0 0 28px rgba(56, 189, 248, 0.28)) drop-shadow(0 0 48px rgba(99, 102, 241, 0.15));

}

</style>


