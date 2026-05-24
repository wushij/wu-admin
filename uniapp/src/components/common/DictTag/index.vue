<template>
  <text v-if="show" class="dict-tag" :class="`dict-tag--${tagEffect}`">{{ displayLabel }}</text>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import {
  getDictLabel,
  getDictListClass,
  listClassToTagType,
  useDict,
} from '@/composables/useDict'

const props = defineProps<{
  label?: string
  dictType?: string
  value?: string | number | null
  effect?: 'default' | 'success' | 'warning' | 'danger' | 'primary'
}>()

const typeRef = computed(() => props.dictType || '')
const { load } = useDict(typeRef)

const displayLabel = computed(() => {
  if (props.label) return props.label
  if (props.dictType) return getDictLabel(props.dictType, props.value)
  return '—'
})

const tagEffect = computed(() => {
  if (props.effect) return props.effect
  if (props.dictType) return listClassToTagType(getDictListClass(props.dictType, props.value))
  return 'default'
})

const show = computed(() => displayLabel.value && displayLabel.value !== '—')

watch(
  () => props.dictType,
  () => {
    if (props.dictType) load(true)
  },
)
</script>

<style lang="scss" scoped>
.dict-tag {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  line-height: 1.4;
}

.dict-tag--default {
  color: #909399;
  background: #f4f4f5;
}

.dict-tag--primary {
  color: #409eff;
  background: #ecf5ff;
}

.dict-tag--success {
  color: #67c23a;
  background: #f0f9eb;
}

.dict-tag--warning {
  color: #e6a23c;
  background: #fdf6ec;
}

.dict-tag--danger {
  color: #f56c6c;
  background: #fef0f0;
}
</style>
