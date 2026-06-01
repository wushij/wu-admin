<template>
  <svg v-if="name === 'emoji'" class="chat-toolbar-icon" viewBox="0 0 24 24" aria-hidden="true">
    <circle cx="12" cy="12" r="9" />
    <circle class="chat-toolbar-icon-fill" cx="9" cy="10" r="1.15" />
    <circle class="chat-toolbar-icon-fill" cx="15" cy="10" r="1.15" />
    <path d="M8.2 14.4c1.1 1.2 2.5 1.8 3.8 1.8s2.7-.6 3.8-1.8" />
  </svg>
  <svg v-else-if="name === 'image'" class="chat-toolbar-icon" viewBox="0 0 24 24" aria-hidden="true">
    <rect x="4" y="5" width="16" height="14" rx="2" />
    <circle cx="9" cy="10" r="1.5" />
    <path d="M4 16l4.5-4.5 3.5 3.5 2.5-2.5L20 16" />
  </svg>
  <svg v-else-if="name === 'file'" class="chat-toolbar-icon" viewBox="0 0 24 24" aria-hidden="true">
    <path d="M16 6.5V16a4 4 0 0 1-8 0V7a2.5 2.5 0 0 1 5 0v8a1.5 1.5 0 0 1-3 0V8" />
  </svg>
</template>

<script setup lang="ts">
defineProps<{
  name: 'emoji' | 'image' | 'file'
}>()
</script>

<style scoped>
.chat-toolbar-icon {
  width: 20px;
  height: 20px;
  stroke: currentColor;
  fill: none;
  stroke-width: 1.75;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.chat-toolbar-icon-fill {
  fill: currentColor;
  stroke: none;
}
</style>
