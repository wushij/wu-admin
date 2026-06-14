<template>
  <HomeActionRow
    v-if="(count ?? 0) > 0"
    :title="'待我审批'"
    subtitle="点击进入审批中心"
    icon="completed"
    theme="approval"
    :badge="count"
    @click="emit('click')"
  />
</template>

<script setup lang="ts">
import HomeActionRow from '@/components/business/HomeActionRow/index.vue'

defineProps<{ count?: number }>()
const emit = defineEmits<{ click: [] }>()
</script>
