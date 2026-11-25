<template>
  <teleport to="body">
    <transition name="aiwu-drawer">
      <div
        v-if="visible"
        class="ai-wu-drawer-overlay"
        @click.self="emit('close')"
        @wheel.self.prevent
        @touchmove.self.prevent
      >
        <div class="ai-wu-drawer-panel" :class="{ 'is-wide-panel': wide }" @wheel.stop>
          <div class="aiwu-glow-border" aria-hidden="true" />
          <slot />
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  wide: boolean
}>()

const emit = defineEmits<{ (e: 'close'): void }>()
</script>
