<template>
  <AuthInput
    :model-value="modelValue"
    password
    icon="lock"
    v-bind="attrs"
    @update:model-value="emit('update:modelValue', $event)"
  />
</template>

<script setup lang="ts">
import { useAttrs } from 'vue'
import AuthInput from '@/components/business/AuthInput/index.vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    modelValue?: string
  }>(),
  {
    modelValue: '',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const attrs = useAttrs()
</script>
