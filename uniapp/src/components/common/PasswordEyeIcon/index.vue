<template>
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
</template>

<script setup lang="ts">
defineProps<{
  /** 密码已明文显示时显示斜线 */
  slashed?: boolean
}>()
</script>

<style scoped>
.password-eye-icon {
  width: 100%;
  height: 100%;
  display: block;
  color: #909399;
}
</style>
