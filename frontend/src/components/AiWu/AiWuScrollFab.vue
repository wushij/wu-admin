<template>
  <transition name="aiwu-step-detail">
    <el-tooltip v-if="visible" content="查看最新回复" placement="left" :show-after="1000">
      <button
        class="aiwu-scroll-fab"
        type="button"
        aria-label="回到底部"
        @click="emit('click')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="12" r="10" />
          <path d="M12 7.5v6.5" />
          <path d="M8.8 11.8 12 15 15.2 11.8" />
        </svg>
      </button>
    </el-tooltip>
  </transition>
</template>

<script setup lang="ts">
defineProps<{ visible: boolean }>()
const emit = defineEmits<{ (e: 'click'): void }>()
</script>
