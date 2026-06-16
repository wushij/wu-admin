<template>
  <!-- #ifdef H5 -->
  <svg
    class="password-eye-icon"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <path
      d="M2 12C2 12 5.5 5 12 5C18.5 5 22 12 22 12C22 12 18.5 19 12 19C5.5 19 2 12 2 12Z"
      stroke="currentColor"
      stroke-width="1.5"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
    <circle cx="12" cy="12" r="2.5" stroke="currentColor" stroke-width="1.5" />
    <line
      v-if="slashed"
      x1="4"
      y1="4"
      x2="20"
      y2="20"
      stroke="currentColor"
      stroke-width="1.5"
      stroke-linecap="round"
    />
  </svg>
  <!-- #endif -->
  <!-- #ifndef H5 -->
  <image
    class="password-eye-icon"
    :src="slashed ? EYE_SLASHED_ICON : EYE_OPEN_ICON"
    mode="aspectFit"
  />
  <!-- #endif -->
</template>

<script setup lang="ts">
/* #ifndef H5 */
const EYE_OPEN_ICON = '/static/icons/password-eye-open.svg'
const EYE_SLASHED_ICON = '/static/icons/password-eye-slashed.svg'
/* #endif */

defineOptions({
  // 避免小程序子组件额外包裹层导致 100% 尺寸塌陷
  virtualHost: true,
})

defineProps<{
  /** 密码已明文显示时显示斜线 */
  slashed?: boolean
}>()
</script>

<style scoped>
.password-eye-icon {
  width: 36rpx;
  height: 36rpx;
  display: block;
  flex-shrink: 0;
  color: #909399;
}
</style>
