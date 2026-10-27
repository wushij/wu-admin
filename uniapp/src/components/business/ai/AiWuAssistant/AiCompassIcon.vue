<template>
  <svg
    viewBox="0 0 48 48"
    :width="size"
    :height="size"
    class="ai-compass-icon"
    :class="{ 'is-spinning': spin }"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <defs>
      <linearGradient :id="gradCompass" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#1677ff" />
        <stop offset="50%" stop-color="#4096ff" />
        <stop offset="100%" stop-color="#722ed1" />
      </linearGradient>
      <linearGradient :id="gradNorth" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#f59e0b" />
        <stop offset="100%" stop-color="#fbbf24" />
      </linearGradient>
      <filter :id="gradGlow" x="-20%" y="-20%" width="140%" height="140%">
        <feGaussianBlur stdDeviation="1.8" result="blur" />
        <feComposite in="SourceGraphic" in2="blur" operator="over" />
      </filter>
    </defs>

    <!-- 外层刻度圈 -->
    <circle
      cx="24"
      cy="24"
      r="20"
      :stroke="`url(#${gradCompass})`"
      stroke-width="1.8"
      stroke-dasharray="2 4"
      class="compass-dial"
    />

    <!-- 内核圆盘背景 -->
    <circle
      cx="24"
      cy="24"
      r="15"
      class="compass-inner-core"
      :fill="dark ? 'rgba(15, 23, 42, 0.94)' : 'rgba(255, 255, 255, 0.95)'"
      :stroke="dark ? 'rgba(22, 119, 255, 0.45)' : 'rgba(37, 99, 235, 0.3)'"
      stroke-width="1.2"
    />

    <!-- 罗盘四向极光星芒 -->
    <path
      d="M24 10 L26 22 L38 24 L26 26 L24 38 L22 26 L10 24 L22 22 Z"
      :fill="`url(#${gradCompass})`"
      :filter="`url(#${gradGlow})`"
      class="compass-star"
    />

    <!-- 北针高亮极星 -->
    <path d="M24 10 L26 24 L24 22 Z" fill="#ffffff" opacity="0.95" />
    <path d="M24 10 L22 24 L24 22 Z" :fill="`url(#${gradNorth})`" />

    <!-- 中心罗盘核心宝石 -->
    <circle cx="24" cy="24" r="3" fill="#ffffff" />
    <circle cx="24" cy="24" r="1.5" fill="#1677ff" />
  </svg>
</template>

<script lang="ts">
let globalUniCompassSeq = 0
</script>

<script setup lang="ts">
const id = ++globalUniCompassSeq

withDefaults(
  defineProps<{
    size?: number | string
    dark?: boolean
    spin?: boolean
  }>(),
  {
    size: 24,
    dark: true,
    spin: true,
  }
)

const gradCompass = `uniCompassCoreGrad_${id}`
const gradNorth = `uniCompassGoldGrad_${id}`
const gradGlow = `uniCompassGlow_${id}`
</script>

<style scoped lang="scss">
.ai-compass-icon {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
  overflow: visible;
}

.compass-dial {
  transform-origin: center;
}

.is-spinning .compass-dial {
  animation: dialSlowSpin 24s linear infinite;
}

.compass-star {
  transform-origin: center;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes dialSlowSpin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
