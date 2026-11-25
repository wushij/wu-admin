<template>
  <span class="motion-tabular">{{ formatted }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useCountUp } from '@/composables/useMotion'

const props = withDefaults(
  defineProps<{
    value: number
    duration?: number
    suffix?: string
  }>(),
  {
    duration: 520,
    suffix: '',
  },
)

const { displayValue } = useCountUp(() => Number(props.value) || 0, {
  duration: props.duration,
})

const formatted = computed(() => `${displayValue.value}${props.suffix}`)
</script>
