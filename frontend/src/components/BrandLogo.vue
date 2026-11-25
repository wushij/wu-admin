<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 36 36"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    class="brand-logo-svg"
    role="img"
    :aria-label="label"
  >
    <defs>
      <!-- 极简深空炭黑微渐变底座（沉稳哑光质感，无塑料高光） -->
      <linearGradient :id="bgGradId" x1="2" y1="2" x2="34" y2="34" gradientUnits="userSpaceOnUse">
        <stop offset="0%" stop-color="#070b14" />
        <stop offset="100%" stop-color="#1e293b" />
      </linearGradient>
    </defs>

    <!-- 深空哑光圆角基座与极微冷白边缘线 -->
    <rect x="0.5" y="0.5" width="35" height="35" rx="9" :fill="`url(#${bgGradId})`" />
    <rect
      x="1"
      y="1"
      width="34"
      height="34"
      rx="8.5"
      stroke="#ffffff"
      stroke-opacity="0.09"
      stroke-width="1"
    />

    <!-- 原创阶梯断层柱阵：保留三柱节律基因，以黑白灰明度差与断层切片重塑建筑秩序感 -->
    <!-- 左柱：主秩序支柱 -->
    <rect
      class="brand-bar brand-bar--left"
      x="9.5"
      y="15.2"
      width="3.8"
      height="10.8"
      rx="1"
      fill="#f8fafc"
    />

    <!-- 中柱：双段断层调度核心塔（主体 + 顶层悬浮切片） -->
    <rect
      class="brand-bar brand-bar--mid"
      x="16.1"
      y="14"
      width="3.8"
      height="12"
      rx="1"
      fill="#ffffff"
    />
    <rect
      class="brand-bar brand-bar--cap"
      x="16.1"
      y="9.2"
      width="3.8"
      height="3.2"
      rx="1"
      fill="#ffffff"
    />

    <!-- 右柱：次级均衡节点 -->
    <rect
      class="brand-bar brand-bar--right"
      x="22.7"
      y="17.8"
      width="3.8"
      height="8.2"
      rx="1"
      fill="#ffffff"
    />
  </svg>
</template>

<script lang="ts">
let globalBrandLogoSeq = 0

function nextBrandLogoSeq(): number {
  globalBrandLogoSeq += 1
  return globalBrandLogoSeq
}
</script>

<script setup lang="ts">
/**
 * 平台侧边栏专属品牌标识（极简阶梯断层柱阵）
 * 延展图腾节律基因，采用深空哑光基座搭配纯粹黑白灰阶梯断层柱体，保持克制与高辨识度。
 */
const seq = nextBrandLogoSeq()

withDefaults(
  defineProps<{
    size?: number | string
    label?: string
  }>(),
  {
    size: 36,
    label: 'Admin Platform',
  },
)

const bgGradId = `brandLogoBg_${seq}`
</script>

<style scoped>
.brand-logo-svg {
  display: block;
  flex-shrink: 0;
}

.brand-bar {
  transition:
    transform var(--duration-fast, 150ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1)),
    opacity var(--duration-fast, 150ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.brand-bar--cap {
  opacity: 0.52;
}

.brand-bar--right {
  opacity: 0.78;
}

@media (prefers-reduced-motion: reduce) {
  .brand-bar {
    transition: none !important;
  }
}
</style>
