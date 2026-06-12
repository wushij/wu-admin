<template>
  <HomeActionRow
    v-if="(openCount ?? 0) > 0"
    :title="`待处理工单 ${openCount}`"
    :subtitle="overdueCount ? `其中超时 ${overdueCount} 个` : '点击进入工单中心'"
    icon="records-o"
    theme="ticket"
    :badge="openCount"
    @click="emit('click')"
  />
</template>

<script setup lang="ts">
import HomeActionRow from '@/components/business/HomeActionRow/index.vue'

defineProps<{
  openCount?: number
  overdueCount?: number
}>()

const emit = defineEmits<{ click: [] }>()
</script>
